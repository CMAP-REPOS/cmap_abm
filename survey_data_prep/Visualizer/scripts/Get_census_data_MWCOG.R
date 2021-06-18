######################################################################################
# Gathering census data for visualizer
#
######################################################################################

library(data.table)
library(tidycensus)
library(stringr)

HTS_raw_Dir  = "E:/Projects/Clients/MWCOG/Data/FromMWCOG/2017_RTS/Interim_RTS_Data_Files"
hh_raw  = fread(paste(HTS_raw_Dir, "RTS_TPB_Households_2020-06-15.csv", sep = "/"))
county_fips_lookup = fread('E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/county_fips_lookup.csv')
county_flows= fread('E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/data/census/ACS_commuting_flows_2015_5yr.csv')
tract_to_taz = fread('E:/Projects/Clients/MWCOG/Tasks/TO3/PopulationSim/data/xwalk/tract_to_taz.csv')
bgs = fread('E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/bg_in_study.csv')
bgs[, tract := substr(as.character(bg_geoid), 1, 11)]


county_flows[, fips := paste0(str_pad(R_STATE_FIPS, width = 2, side = "left", pad = "0"), str_pad(R_COUNTY_FIPS,  width = 3, side = "left", pad = "0"))]
county_flows[, w_fips := paste0(str_pad(W_STATE_FIPS, width = 2, side = "left", pad = "0"), str_pad(W_COUNTY_FIPS,  width = 3, side = "left", pad = "0"))]

counties = unique(hh_raw[, .(HOME_STATE_COUNTY_FIPS)])

counties = counties[county_fips_lookup, on = .(HOME_STATE_COUNTY_FIPS = FIPS), nomatch = 0]
census_api_key("3b5963a87855c7861477469fd5e8e5846c9d9416")
acs_vars = load_variables(year = 2018, "acs5", cache = TRUE)

acs_veh = data.table(
  get_acs(
    geography = "tract",
    table = "B08201",
    state = hh_raw[, unique(HOME_STATE_FIPS)],
    year = 2017
  )
)

acs_veh = acs_veh[substr(GEOID, 0, 5) %in% counties[, HOME_STATE_COUNTY_FIPS]]
acs_veh[, GEOID := as.numeric(GEOID)]

acs_vars = data.table(load_variables(year = 2017, "acs5", cache = FALSE))
acs_vars_veh = acs_vars[name %like% "B08201"]

auto_ownership = dcast(acs_veh[variable %in% c('B08201_002', 'B08201_003', 'B08201_004', 'B08201_005', 'B08201_006')],
                       variable ~ '', value.var = 'estimate', fun = sum )

setnames(auto_ownership, c('variable', '.'), c('HHVEH', 'freq'))

auto_ownership[, HHVEH := as.numeric(gsub('B08201_00', '', HHVEH)) - 2]
auto_ownership = auto_ownership[HHVEH <= 4]

acs_veh_t = dcast(acs_veh, GEOID + NAME ~ variable, value.var = 'estimate')

setnames(acs_veh_t, c('GEOID', 'B08201_001', 'B08201_002', 'B08201_003', 'B08201_004', 'B08201_005', 'B08201_006'),
         c('TractID', 'Census_HH',	'Census_A0',	'Census_A1',	'Census_A2',	'Census_A3',	'Census_A4'))

acs_veh_t = acs_veh_t[, c('TractID', 'Census_HH',	'Census_A0',	'Census_A1',	'Census_A2',	'Census_A3',	'Census_A4'), with = FALSE]

fwrite(auto_ownership, 'E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/summaries/autoOwnershipCensus.csv')
fwrite(acs_veh_t, 'E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/ACS_2017_5yr_AutoOwn.csv')

## hh size

B11016

setDT(acs_veh)
hhsize = dcast(acs_veh[variable %in% c('B08201_007', 'B08201_013', 'B08201_019', 'B08201_025')],
                       variable ~ '', value.var = 'estimate', fun = sum )

setnames(hhsize, c('variable', 'Var.2'), c('HHSIZE', 'freq'))

setDT(hhsize)
hhsize[, HHSIZE := round(as.numeric(gsub('B08201_', '', HHSIZE))/7, 0)]
fwrite(hhsize, 'E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/summaries/hhSizeCensus.csv')

## County flows

counties[, HOME_STATE_COUNTY_FIPS := as.character(HOME_STATE_COUNTY_FIPS)]
county_flows[, W_COMMUTING_FLOW := as.numeric(gsub(",", "", W_COMMUTING_FLOW))]
county_flows = county_flows[counties, on = .(fips = HOME_STATE_COUNTY_FIPS), nomatch = 0]
county_flows = county_flows[w_fips %in% counties[, HOME_STATE_COUNTY_FIPS]]

county_flows[W_COUNTY_NAME == 'Fairfax city' | W_COUNTY_NAME == 'Falls Church city', W_COUNTY_NAME := 'Fairfax County']
county_flows[W_COUNTY_NAME %like% 'Manassas', W_COUNTY_NAME := 'Prince William County']
county_flows[W_COUNTY_NAME== 'District of Columbia' , W_COUNTY_NAME := 'Washington']
county_flows[R_COUNTY_NAME == 'Fairfax city' | R_COUNTY_NAME == 'Falls Church city', R_COUNTY_NAME := 'Fairfax County']
county_flows[R_COUNTY_NAME %like% 'Manassas', R_COUNTY_NAME := 'Prince William County']
county_flows[R_COUNTY_NAME== 'District of Columbia' , R_COUNTY_NAME := 'Washington']

county_flows_matrix = dcast(county_flows, R_COUNTY_NAME ~ W_COUNTY_NAME, value.var = 'W_COMMUTING_FLOW', fun = sum)
#county_flows_matrix[, V1 := NULL]
setnames(county_flows_matrix, 'R_COUNTY_NAME', '')

fwrite(county_flows_matrix, 'E:/Projects/Clients/MWCOG/Tasks/TO3/Visualizer/summaries/countyFlowsCensus.csv')
