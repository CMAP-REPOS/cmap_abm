######################################################################################
# Gathering census data for visualizer
#
######################################################################################

library(data.table)
library(tidycensus)
library(stringr)
library(sf)
library(yaml)
settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')

# county_flows= fread('E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/data/census/ACS_commuting_flows_2015_5yr.csv')
# tract_to_taz = fread('E:/Projects/Clients/MWCOG/Tasks/TO3/PopulationSim/data/xwalk/tract_to_taz.csv')

zone_dir = settings$zone_dir

zones = st_read(file.path(zone_dir, 'zones17.shp'))

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

omit_counties = c('Kankakee', 'Kenosha', 'LaSalle', 'Lee', 'Ogle', 'Racine','Boone', 'Winnebago', 'Walworth' )
county_flows = county_flows[!work_county_name %in% omit_counties & !res_county_name %in% omit_counties]

county_flows[, fips := paste0(str_pad(res_state_fips, width = 2, side = "left", pad = "0"), str_pad(res_county_fips,  width = 3, side = "left", pad = "0"))]
county_flows[, w_fips := paste0(str_pad(work_state_fips, width = 2, side = "left", pad = "0"), str_pad(work_county_fips,  width = 3, side = "left", pad = "0"))]

county_flows[res_county_name == 'Lake', res_county_name := ifelse(res_state_fips == 17, 'Lake, IL', 'Lake, IN')]
county_flows[work_county_name == 'Lake', work_county_name := ifelse(work_state_fips == 17, 'Lake, IL', 'Lake, IN')]
county_flows = county_flows[counties, on = .(fips = county_fip), nomatch = 0]
county_flows = county_flows[w_fips %in% counties[, county_fip]]
county_flows[, workers_N := gsub(',', '', workers_N)]
county_flows[, workers_N := as.integer(workers_N)]

county_flows_matrix = dcast(county_flows, res_county_name ~ work_county_name , value.var = 'workers_N', fun = sum)
#county_flows_matrix[, V1 := NULL]


county_flows_matrix[, Total := rowSums(.SD), .SDcols = names(county_flows_matrix)[-1]]
county_flows_matrix = rollup(county_flows_matrix, j = lapply(.SD, sum), by = 'res_county_name', .SDcols = names(county_flows_matrix)[-1])
county_flows_matrix[is.na(res_county_name), res_county_name := 'Total']
setcolorder(county_flows_matrix, c("res_county_name", "Cook",     "DeKalb",   "DuPage",   
                                   "Grundy" ,  "Kane" ,  "Kendall", "Lake, IL",
             "McHenry"  , "Will"   , "Lake, IN",  "LaPorte",   "Porter" , "Total"   ))
county_flows_matrix[, order_num := c(1:6, 8, 10, 12, 9, 7, 11, 13)]
county_flows_matrix = county_flows_matrix[order(order_num)]
county_flows_matrix[, order_num := NULL]

setnames(county_flows_matrix, 'res_county_name', '')

fwrite(county_flows_matrix, file.path(settings$visualizer_summaries, 'countyFlowsCensus.csv'))

# use this as a way to get county name table

counties[county_flows, county_name := i.res_county_name, on = .(county_fip = fips)]
fwrite(counties, file.path(settings$proj_dir, 'county_lookup.csv'))

# ACS vehicles

census_api_key("3b5963a87855c7861477469fd5e8e5846c9d9416")

acs_veh = data.table(
  get_acs(
    geography = "tract",
    table = "B08201",
    state = unique(counties$state),
    # county = counties$county,
    year = 2017
  )
)

acs_veh = acs_veh[substr(GEOID, 0, 5) %in% counties[, county_fip]]
acs_veh[, GEOID := as.numeric(GEOID)]

acs_vars = data.table(load_variables(year = 2017, "acs5", cache = FALSE))
acs_vars_veh = acs_vars[name %like% "B08201"]

auto_ownership = dcast(acs_veh[variable %in% c('B08201_002', 'B08201_003', 'B08201_004', 'B08201_005', 'B08201_006')],
                       variable ~ '', value.var = 'estimate', fun = sum )

setnames(auto_ownership, c('variable', '.'), c('HHVEH', 'freq'))

auto_ownership[, HHVEH := as.numeric(gsub('B08201_00', '', HHVEH)) - 2]
auto_ownership = auto_ownership[HHVEH <= 4]

acs_veh_t = dcast(acs_veh, GEOID + NAME ~ variable, value.var = 'estimate', fun.aggregate = sum)

setnames(acs_veh_t, c('GEOID', 'B08201_001', 'B08201_002', 'B08201_003', 'B08201_004', 'B08201_005', 'B08201_006'),
         c('TractID', 'Census_HH',	'Census_A0',	'Census_A1',	'Census_A2',	'Census_A3',	'Census_A4'))

acs_veh_t = acs_veh_t[, c('TractID', 'Census_HH',	'Census_A0',	'Census_A1',	'Census_A2',	'Census_A3',	'Census_A4'), with = FALSE]

fwrite(auto_ownership, file.path(settings$visualizer_summaries, 'autoOwnershipCensus.csv'))
fwrite(acs_veh_t, file.path(settings$visualizer_summaries, 'ACS_2018_5yr_AutoOwn.csv'))

## hh size

#B11016

setDT(acs_veh)
hhsize = dcast(acs_veh[variable %in% c('B08201_007', 'B08201_013', 'B08201_019', 'B08201_025')],
                       variable ~ '', value.var = 'estimate', fun = sum )

setnames(hhsize, c('variable', '.'), c('HHSIZE', 'freq'))

setDT(hhsize)
hhsize[, HHSIZE := round(as.numeric(gsub('B08201_', '', HHSIZE))/7, 0)]
fwrite(hhsize, file.path(settings$visualizer_summaries, 'hhSizeCensus.csv'))

