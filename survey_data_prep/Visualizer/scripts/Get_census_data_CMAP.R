######################################################################################
# Gathering census data for visualizer
#
######################################################################################

library(data.table)
library(tidycensus)
library(stringr)
library(sf)
library(yaml)

args = commandArgs(trailingOnly = TRUE)

if(length(args) > 0){
  settings_file = args[1]
} else {
  settings_file = 'C:\\projects\\cmap_activitysim\\cmap_abm\\survey_data_prep\\cmap_inputs.yml'
}

settings = yaml.load_file(settings_file)
start_time = Sys.time()
# county_flows= fread('E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/data/census/ACS_commuting_flows_2015_5yr.csv')
# tract_to_taz = fread('E:/Projects/Clients/MWCOG/Tasks/TO3/PopulationSim/data/xwalk/tract_to_taz.csv')

census_api_key("7662f4e85f392512f29297c2627e82fdbf349bf7")

zone_dir = settings$zone_dir

zones = st_read(file.path(zone_dir, settings$zone_shp_file))

zones_dt = setDT(zones)


# Counties
counties = unique(zones_dt[, .(county_fip, 
                               county = substring(county_fip, 3, 5), 
                               state = substring(county_fip, 1, 2))])


# Census county flows
county_flows = fread(settings$county_flows_csv)
county_flows = county_flows[work_county_name != '']
county_flows[, res_county_name := gsub(' County', '', res_county_name)]
county_flows[, work_county_name := gsub(' County', '', work_county_name)]

county_flows = county_flows[order(tolower(res_county_name), tolower(work_county_name))]

#omit_counties = c('Kankakee', 'Kenosha', 'LaSalle', 'Lee', 'Ogle', 'Racine','Boone', 'Winnebago', 'Walworth' )
#county_flows = county_flows[!rownames(county_flows) %in% omit_counties, !colnames(county_flowss) %in% omit_counties]

county_flows[, fips := paste0(str_pad(res_state_fips, width = 2, side = "left", pad = "0"), str_pad(res_county_fips,  width = 3, side = "left", pad = "0"))]
county_flows[, w_fips := paste0(str_pad(work_state_fips, width = 2, side = "left", pad = "0"), str_pad(work_county_fips,  width = 3, side = "left", pad = "0"))]

county_flows[res_county_name == 'Lake', res_county_name := ifelse(res_state_fips == 17, 'Lake, IL', 'Lake, IN')]
county_flows[work_county_name == 'Lake', work_county_name := ifelse(work_state_fips == 17, 'Lake, IL', 'Lake, IN')]
county_flows = county_flows[counties, on = .(fips = county_fip), nomatch = 0]
county_flows = county_flows[w_fips %in% counties[, county_fip]]
county_flows[, workers_N := gsub(',', '', workers_N)]
county_flows[, workers_N1 := as.integer(workers_N)]

acs_mode = data.table(
  get_acs(
    geography = "county",
    table = "B08006",
    state = unique(counties$state),
    # county = counties$county,
    year = 2019
  )
) 

acs_wfh = acs_mode[acs_mode$variable == "B08006_051",]

county_flows$workers_N = unlist(mapply(function(fc, tc, inw){if(fc == tc)(return(inw - acs_wfh[acs_wfh$GEOID == fc, "estimate"]))else(return(inw))}, county_flows$fips, county_flows$w_fips, county_flows$workers_N1))


county_flows_matrix = dcast(county_flows, res_county_name ~ work_county_name , value.var = 'workers_N', fun = sum)

county_flows_matrix[, Total := rowSums(.SD), .SDcols = names(county_flows_matrix)[-1]]
county_flows_matrix = rollup(county_flows_matrix, j = lapply(.SD, sum), by = 'res_county_name', .SDcols = names(county_flows_matrix)[-1])
county_flows_matrix[is.na(res_county_name), res_county_name := 'Total']
#setcolorder(county_flows_matrix, c("res_county_name", "Boone", "Cook", "DeKalb", "DuPage", "Grundy", "Kane", "Kankakee", 
#                                    "LaPorte", "Lake, IL", "Kendall", "Kenosha", "Lake, IN", "LaSalle", "Lee", "McHenry",
#                                    "Olge", "Porter", "Racine", "Walworth", "Will", "Winnebago", "Total"))
#county_flows_matrix[, order_num := c(1:6, 8, 10, 12, 9, 7, 11, 13)]
#county_flows_matrix = county_flows_matrix[order(order_num)]
#county_flows_matrix[, order_num := NULL]
#county_flows_matrix <- cbind(county_flows_matrix, Total = rowSums(county_flows_matrix))
#county_flows_matrix <- rbind(county_flows_matrix, Total = colSums(county_flows_matrix))
setnames(county_flows_matrix, 'res_county_name', '')
county_flows_matrix = county_flows_matrix[c(1:7, 10, 12, 8, 9, 11, 13:22), c(1:8, 11, 13, 9, 10, 12, 14:23)]





fwrite(county_flows_matrix, file.path(settings$visualizer_summaries, 'countyFlowsCensus.csv'))

# use this as a way to get county name table

counties[county_flows, county_name := i.res_county_name, on = .(county_fip = fips)]
fwrite(counties, file.path(settings$proj_dir, 'county_lookup.csv'))

# ACS vehicles

acs_veh = data.table(
  get_acs(
    geography = "tract",
    table = "B08201",
    state = unique(counties$state),
    # county = counties$county,
    year = 2019
  )
)

acs_veh = acs_veh[substr(GEOID, 0, 5) %in% counties[, county_fip]]
acs_veh[, GEOID := as.numeric(GEOID)]
acs_veh$COUNTY = counties$county_name[match(substr(acs_veh$GEOID, 0, 5), levels(counties$county_fip)[counties$county_fip])]
acs_veh = acs_veh[!is.na(acs_veh$COUNTY),]

acs_vars = data.table(load_variables(year = 2019, "acs5", cache = FALSE))
acs_vars_veh = acs_vars[name %like% "B08201"]

auto_ownership = dcast(acs_veh[variable %in% c('B08201_002', 'B08201_003', 'B08201_004', 'B08201_005', 'B08201_006')],
                       COUNTY + variable ~ '', value.var = 'estimate', fun = sum )

setnames(auto_ownership, c('COUNTY','variable', '.'), 
         c('COUNTY', 'HHVEH', 'freq'))

auto_ownership[, HHVEH := as.numeric(gsub('B08201_00', '', HHVEH)) - 2]
auto_ownership = auto_ownership[HHVEH <= 4]

acs_veh_t = dcast(acs_veh, GEOID + NAME ~ variable, value.var = 'estimate', fun.aggregate = sum)

setnames(acs_veh_t, c('GEOID', 'B08201_001', 'B08201_002', 'B08201_003', 'B08201_004', 'B08201_005', 'B08201_006'),
         c('TractID', 'Census_HH',	'Census_A0',	'Census_A1',	'Census_A2',	'Census_A3',	'Census_A4'))

acs_veh_t = acs_veh_t[, c('TractID', 'Census_HH',	'Census_A0',	'Census_A1',	'Census_A2',	'Census_A3',	'Census_A4'), with = FALSE]

fwrite(auto_ownership, file.path(settings$visualizer_summaries, 'autoOwnershipCensus.csv'))
fwrite(acs_veh_t, file.path(settings$visualizer_summaries, 'ACS_2019_5yr_AutoOwn.csv'))

acs_veh_t$COUNTY = counties$county_name[match(substr(acs_veh_t$TractID, 0, 5), levels(counties$county_fip)[counties$county_fip])]

hh_census_co = dcast(acs_veh_t, COUNTY ~ ., value.var = "Census_HH", fun = sum)

## hh size

#B11016

setDT(acs_veh)
hhsize = dcast(acs_veh[variable %in% c('B08201_007', 'B08201_013', 'B08201_019', 'B08201_025')],
                       variable ~ '', value.var = 'estimate', fun = sum )

setnames(hhsize, c('variable', '.'), c('HHSIZE', 'freq'))

setDT(hhsize)
hhsize[, HHSIZE := round(as.numeric(gsub('B08201_', '', HHSIZE))/7, 0)]
fwrite(hhsize, file.path(settings$visualizer_summaries, 'hhSizeCensus.csv'))

# Workplace location by county from ACS ####
#
# Note that the universe is Workers age >= 16 that did not work at home, so it doesn't need to be adjusted by wfh/tc

acs_wp = data.table(
  get_acs(
    geography = "county",
    table = "B08406",
    state = unique(counties$state),
    # county = counties$county,
    year = 2019
  )
)

acs_wp$COUNTY = counties$county_name[match(substr(acs_wp$GEOID, 0, 5), levels(counties$county_fip)[counties$county_fip])] 

acs_wp = acs_wp[!is.na(acs_wp$COUNTY) & acs_wp$variable == "B08406_001",c("COUNTY", "estimate")]

colnames(acs_wp) = c("COUNTY", "freq")

fwrite(acs_wp, file.path(settings$visualizer_summaries, 'workplaceLocationCensus.csv'))

end_time = Sys.time()
cat("\n Get_census_data_CMAP.R Script finished, run time: ", end_time - start_time, "sec \n")