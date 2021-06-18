# CMAP target prep

library(tidycensus)
library(data.table)
library(sf)
library(yaml)
library(stringr)
library(tigris)
# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')

data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
nirpc_dir = file.path(data_dir, settings$nirpc_folder)
output_dir = settings$SPA_input_dir

project_dir = settings$proj_dir
wt_dir = file.path(project_dir, "underreporting_correction")


# puma to tract
# puma_tract = fread('https://www2.census.gov/geo/docs/maps-data/data/rel/2010_Census_Tract_to_2010_PUMA.txt')

zones17 = st_read(file.path(settings$zone_dir, 'zones17.shp'))

zones17_dt = setDT(copy(zones17))
counties = zones17_dt[state  == 'IL', unique(county_fip)]
counties = str_sub(counties, 3, 5)

state_fips = 17

state = data.table(fips = state_fips, abb = 'il')

# download ACS PUMS data
# PUMS data dictionary
pums_dd_url = 'https://www2.census.gov/programs-surveys/acs/tech_docs/pums/data_dict/PUMS_Data_Dictionary_2018.csv'
pums_dd_path = file.path(wt_dir, 'PUMS_Data_Dictionary_2018.csv')
if ( !file.exists(pums_dd_path) ){
  download.file(url = pums_dd_url, destfile=pums_dd_path)
}
pums_dd = fread(pums_dd_path, sep=',', fill=TRUE, header=FALSE)
names(pums_dd) = c('name_or_value', 'variable', 'data_type', 'data_length', 'value_min', 'value_max', 'description')

# pums_url_base = 'https://www2.census.gov/programs-surveys/acs/data/pums/2018/1-Year'

pums_dir = wt_dir


# 2018 PUMS data
message('Loading PUMS data')
pums_hh  = fread(file = file.path(pums_dir, 'psam_h17.csv'), colClasses = c(PUMA = "character"))
pums_per = fread(file = file.path(pums_dir, 'psam_p17.csv'), colClasses = c(PUMA = "character"))

pums_hh_copy = copy(pums_hh)
pums_per_copy = copy(pums_per)

# Get shapefiles
puma_sf = pumas(state_fips, year = '2018', class='sf')


cluster_9 = c('03700', '02601', '03601', '03009', '02400')
cluster_10 = c('03008','03007','03005', '03208','03417')
cluster_8 = c('03209', '03202', '03203','03207', '03205', '03204')
cluster_7 = c('03306', '03307', '03308','03309','03310','03602')
cluster_11 = c('03108', '03102' , '03105', '03106', '03107')
cluster_6 = c('03414', '03410', '03412','03413','03411')
cluster_5 = c('03422', '03407', '03409', '03408')
cluster_4 = c('03401', '03418', '03419', '03415', '03420', '03421', '03416')
cluster_3 = c('03527', '03528', '03531', '03532', '03529', '03530')
cluster_2 = c('03521', '03520', '03504', '03503', '03501')
cluster_1 = c('03525', '03524', '03523', '03502', '03522', '03526')

puma_sf$cluster = fcase(puma_sf$PUMACE10  %in% (cluster_1), 1,
                        puma_sf$PUMACE10  %in% (cluster_2), 2,
                        puma_sf$PUMACE10  %in% (cluster_3), 3,
                        puma_sf$PUMACE10  %in% (cluster_4), 4, 
                        puma_sf$PUMACE10  %in% (cluster_5), 5,
                        puma_sf$PUMACE10  %in% (cluster_6), 6,
                        puma_sf$PUMACE10  %in% (cluster_7), 7,
                        puma_sf$PUMACE10  %in% (cluster_8), 8,
                        puma_sf$PUMACE10  %in% (cluster_9), 9,
                        puma_sf$PUMACE10  %in% (cluster_10), 10,
                        puma_sf$PUMACE10  %in% (cluster_11), 11,
                        default = -1)

pums_hh$cluster = fcase(pums_hh$PUMA %in% (cluster_1), 1,
                        pums_hh$PUMA %in% (cluster_2), 2,
                        pums_hh$PUMA %in% (cluster_3), 3,
                        pums_hh$PUMA %in% (cluster_4), 4, 
                        pums_hh$PUMA %in% (cluster_5), 5,
                        pums_hh$PUMA %in% (cluster_6), 6,
                        pums_hh$PUMA %in% (cluster_7), 7,
                        pums_hh$PUMA %in% (cluster_8), 8,
                        pums_hh$PUMA %in% (cluster_9), 9,
                        pums_hh$PUMA %in% (cluster_10), 10,
                        pums_hh$PUMA %in% (cluster_11), 11,
                        default = -1)
pums_per$cluster = fcase(pums_per$PUMA %in% (cluster_1), 1,
                         pums_per$PUMA %in% (cluster_2), 2,
                         pums_per$PUMA %in% (cluster_3), 3,
                         pums_per$PUMA %in% (cluster_4), 4, 
                         pums_per$PUMA %in% (cluster_5), 5,
                         pums_per$PUMA %in% (cluster_6), 6,
                         pums_per$PUMA %in% (cluster_7), 7,
                         pums_per$PUMA %in% (cluster_8), 8,
                         pums_per$PUMA %in% (cluster_9), 9,
                         pums_per$PUMA %in% (cluster_10), 10,
                         pums_per$PUMA %in% (cluster_11), 11,
                        default = -1)

# FILTER TO DESIRED REGION AND PARAMETERS---------------------------------------

# Filter to exclude group quarters
# Filter to exclude vacant unit as number of persons in household is greater than 0
nrow(pums_hh)
pums_hh[, .N, TYPE]
pums_hh = pums_hh[TYPE == 1 & NP > 0] # TYPE == 1 is Housing Unit, NP = 0 is vacant
nrow(pums_hh)

# Filter to study region

pumas_in_region = c(cluster_1, cluster_2, cluster_3, cluster_4, cluster_5, cluster_6, cluster_7, cluster_8, cluster_9, cluster_10, cluster_11)

# View the PUMAS over the study area
puma_region = puma_sf[as.numeric(puma_sf$PUMACE10) %in% pumas_in_region, ]

puma_region = puma_region[puma_region$cluster > 0,]
new_weights = pums_per[, .(new_WGTP = mean(PWGTP)), .(SERIALNO)] 
pums_hh = new_weights[pums_hh, on = .(SERIALNO)]
pums_hh[, WGTP_orig := WGTP]
pums_hh[, WGTP := new_WGTP]

## TODO: Ask whether to do person:person and hh:hh hor use the same scaling for persons/hhs
pums_hh = pums_hh[cluster >0]
pums_per = pums_per[cluster >0]
pums_person_all = merge(pums_per, pums_hh, by = c('PUMA', 'SERIALNO'))

# Review variable names, if needed
pums_dd[name_or_value == 'NAME', .(variable, value_min)]

names(pums_person_all)
nrow(pums_person_all)

### hh and person weights are not consistent! this is a known issue.
pums_hh[, sum(NP * WGTP)]
pums_per[SERIALNO %in% pums_hh[, SERIALNO], sum(PWGTP)]



# DERIVE NEW VARIABLES----------------------------------------------------------

# TODO: data breaks and categories should not be hard-coded.  
# They need to match cutpoints in the survey data script
warning('Hardcoded cutpoints should match survey data')



# Household size -------------------------------------------------------------

pums_hh[, HHSIZEC := pmin(NP, 5)]

pums_hsize = dcast(
  pums_hh, 
  PUMA + cluster~ HHSIZEC, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_hsize,
  colnames(pums_hsize),
  c("puma_id", "cluster", "h_size_1", "h_size_2", "h_size_3", "h_size_4", "h_size_5"))

pums_hsize[, h_total := h_size_1 + h_size_2 + h_size_3 + h_size_4 + h_size_5]


# Household workers -----------------------------------------------------------

pums_person_all[, 
                EMPLC := 
                  ifelse(
                    ESR %in% c(1, 2, 4, 5) & WKHP >= 35, 1,
                    ifelse(
                      ESR %in% c(1, 2, 4, 5), 2, 3))]

pums_person_all[, FTWORKER := ifelse(EMPLC == 1, 1, 0)]
pums_person_all[, PTWORKER := ifelse(EMPLC == 2, 1, 0)]

#Aggregate the data
hh_sum = pums_person_all[,
                         lapply(.SD, sum, na.rm = TRUE), .SDcols = c("FTWORKER","PTWORKER"),
                         .(SERIALNO)]    

pums_hh_prev = copy(pums_hh)
pums_hh = merge(pums_hh, hh_sum, by = 'SERIALNO')
stopifnot(nrow(pums_hh) == nrow(pums_hh_prev))

pums_hh[, HHWORKRC := pmin(FTWORKER + PTWORKER, 3)]


pums_hworker = dcast(
  pums_hh, 
  PUMA ~ HHWORKRC, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_hworker,
  colnames(pums_hworker),
  c("puma_id","h_worker_0","h_worker_1","h_worker_2","h_worker_3"))



# Household income ------------------------------------------------------------

pums_hh[, 
        HHINCC := ifelse(
                HINCP >= 100000,  4,
                ifelse(
                  HINCP >= 60000,  3,
                  ifelse(
                    HINCP >= 35000,  2, 1)))]

pums_hinc = dcast(
  pums_hh, 
  PUMA ~ HHINCC, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_hinc,
  colnames(pums_hinc),
  c("puma_id","h_inc_35","h_inc_60","h_inc_100", "h_inc_100p"))


# Household vehicles --------------------------------------------------------
pums_hh[, AUTOSUFF := fcase(VEH == 0, 0,
                           VEH < (FTWORKER + PTWORKER), 1,
                           VEH >= (FTWORKER + PTWORKER), 2)]


pums_hveh = dcast(
  pums_hh, 
  PUMA ~  AUTOSUFF, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_hveh,
  colnames(pums_hveh),
  c("puma_id","h_zero_veh", "h_veh_lt_wrk", "h_veh_gt_wrk"))


# Lifecycle --------------------------------------------------


#Aggregate the data
hh_max = pums_person_all[,
                         lapply(.SD, max),
                         .SDcols = "AGEP",
                         .(SERIALNO)]   

setnames(hh_max, 'AGEP', 'AGE_MAX')

hh_min = pums_person_all[,
                         lapply(.SD, min),
                         .SDcols = "AGEP",
                         .(SERIALNO)]   
setnames(hh_min, 'AGEP', 'AGE_MIN')

pums_hh_prev = copy(pums_hh)
pums_hh = merge(pums_hh, hh_max, by = 'SERIALNO')
pums_hh = merge(pums_hh, hh_min, by = 'SERIALNO')
stopifnot(nrow(pums_hh) == nrow(pums_hh_prev))

# calculate lifecycle

pums_hh[, LIFECYCLE := fcase(AGE_MIN < 6, 1,
                             AGE_MIN %in% 6:17, 2,
                             AGE_MIN >= 18 & HHSIZEC == 1 & AGE_MAX < 35, 3,
                             AGE_MIN >= 18 & HHSIZEC == 1 & AGE_MAX %in% 35:64, 4,
                             AGE_MIN >= 18 & HHSIZEC == 1 & AGE_MAX >= 65, 5,                             
                             AGE_MIN >= 18 & HHSIZEC > 1 & AGE_MAX < 35, 6,
                             AGE_MIN >= 18 & HHSIZEC > 1 & AGE_MAX %in% 35:64, 7,
                             AGE_MIN >= 18 & HHSIZEC > 1 & AGE_MAX >= 65, 8)
                             ]

pums_lifecycle = dcast(
  pums_hh, 
  PUMA ~ LIFECYCLE, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_lifecycle,
  colnames(pums_lifecycle),
  c("puma_id","h_lifecycle_prek","h_lifecycle_k6_17","h_lifecycle_ad1_lt35", "h_lifecycle_ad1_35to64", "h_lifecycle_ad1_gt64",
    "h_lifecycle_ad2_lt35", "h_lifecycle_ad2_35to64", "h_lifecycle_ad2_gt64" ))




# Person gender --------------------------------------------------------------

pums_per_gender = dcast(
  pums_person_all, 
  PUMA ~ SEX, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_per_gender,
  colnames(pums_per_gender),
  c("puma_id","p_male","p_female"))

pums_per_gender[, p_total := p_male + p_female]



# Person age -----------------------------------------------------------------

pums_person_all[, 
                AGEC := ifelse(
                  AGEP %in% (5:14),  2,
                  ifelse(
                    AGEP %in% (15:17), 3,
                    ifelse(
                      AGEP %in% (18:34), 4,
                      ifelse(
                        AGEP %in% (35:64), 5,
                          ifelse(
                            AGEP >= 65, 6, 1)))))]

pums_per_age = dcast(
  pums_person_all, 
  PUMA ~ AGEC, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(pums_per_age,
         colnames(pums_per_age),
         c("puma_id","p_age_lt_5","p_age_5_14","p_age_15_17",
           "p_age_18_34","p_age_35_64", "p_age_gt_65"))



# Person commute mode (variable JWTR) -----------------------------------------
pums_dd[name_or_value == 'VAL' & variable == 'JWTR', .(value_min, desc = str_trunc(description, 20))]

pums_per_commute = dcast(
  pums_person_all,  
  PUMA ~ JWTR,
  value.var = 'WGTP',
  fun.aggregate=sum)

names(pums_per_commute)

commute_labels = paste0(
  'p_commute_', c(
    'none',
    'car',
    'bus',
    'streetcar',
    'subway',
    'railroad',
    'ferry',
    'taxi',
    'motorcycle',
    'bicycle',
    'walk',
    'home',
    'other'
  )
)

names(pums_per_commute) = c('puma_id', commute_labels)

# Variable JWRIP codes the number of people in a carpool
# Check that the following values match counts of JWTR == 1

# A = pums_per_commute[, .(PUMA=puma_id, car = p_commute_car)]
# setkey(A, 'PUMA')
# B = pums_person_all[!is.na(JWRIP), .(car = sum(WGTP)), by=PUMA]
# stopifnot(all.equal(A, B))


pums_per_commute = pums_per_commute[, .(puma_id,
                                        p_commute_none = p_commute_none + p_commute_home,
                                        p_commute_car = p_commute_car ,
                                        p_commute_walkbike = p_commute_walk + p_commute_bicycle,
                                        p_commute_other = 
                                          p_commute_bus +
                                           p_commute_railroad +
                                          p_commute_ferry +
                                          p_commute_taxi +
                                          p_commute_streetcar +
                                          p_commute_subway +
                                           p_commute_motorcycle +
                                          p_commute_other)]

# Add commute pattern
# remotes::install_github("gregmacfarlane/ctpp_flows")
rm(ctpp_flows)
library(ctppflows)
ctpp_flows_backup = copy(ctpp_flows)
ctpp_flows = setDT(copy(ctpp_flows))
ctpp_flows = ctpp_flows[substr(residence, 1, 5) %in% zones17_dt[, county_fip]]

puma_tract[, tract_fips :=paste0( str_pad(STATEFP, 2, pad = '0'), str_pad(COUNTYFP, 3, pad = '0'), str_pad(TRACTCE, 6,pad = '0'))]

ctpp_flows[puma_tract, PUMA5CE := i.PUMA5CE, on = .(residence = tract_fips)]
ctpp_flows = ctpp_flows[PUMA5CE %in% as.numeric(pumas_in_region) & residence %like% '^17']

# location_raw = fread(file.path(settings$data_dir, settings$cmap_folder, 'location_wZones.csv'))
# location_raw[zones17_dt, cbd := i.cbd, on = .(zone17)]

tracts_shp = tracts(state = 'IL', county = 'Cook', year = 2018)

zones17_copy= st_transform(zones17, st_crs(tracts_shp))
cook_tracts = st_join(zones17_copy, tracts_shp)
setDT(cook_tracts)
cook_tracts[, .N, cbd]

cbd_tracts = cook_tracts[cbd == 1, unique(TRACTCE)]
chicago_tracts = cook_tracts[chicago == 1, unique(TRACTCE)]

ctpp_flows[, residence_county := substr(residence, 0, 5)]
ctpp_flows[, work_county := substr(workplace, 0, 5)]

county_flows = fread(file.path(wt_dir, 'county_county_2016.csv'))
county_flows[, res_county_name := toupper(gsub(' County, Illinois', '', RESIDENCE))]
county_flows[, work_county_name := toupper(gsub(' County, Illinois', '', WORKPLACE))]
county_flows[zones17_dt[state == 'IL'], res_county := county_fip, on = .(res_county_name = county_nam)]
county_flows[zones17_dt[state == 'IL'], work_county := county_fip, on = .(work_county_name = county_nam)]

county_flows[, workers := as.numeric(gsub(',', '', `Workers 16 and Over`))]

county_flows_agg = county_flows[!is.na(workers) & res_county %in% paste0('17', counties) & Output == 'Estimate', .(res_county, work_county, workers, res_county_name, work_county_name)]
county_flows_tract = ctpp_flows[residence_county  %in% paste0('17', counties)][, .(flow = sum(flow)), .(residence_county, work_county)]

county_flows_factor = county_flows_tract[county_flows_agg, on = .(residence_county = res_county, work_county)]
county_flows_factor[, factor := workers/flow]

ctpp_flows[county_flows_factor, factor := i.factor, on = .(residence_county, work_county)]
ctpp_flows[is.na(factor), factor := mean(county_flows_factor[, factor], na.rm = TRUE)]


ctpp_flows = puma_tract[ctpp_flows, on = .(tract_fips = residence)]

setnames(ctpp_flows, 'PUMA5CE', 'puma_id')
ctpp_flows[, work_tract_shortfips := substr(workplace, 6, 11)]

ctpp_flows[, `:=` (not_loop_to_loop = 0,
                   loop_to_not_loop = 0,
                   intra_district = 0,
                   d2d_not_loop = 0)]

district_counties = zones17_dt[state == 'IL' & county_nam %in% c('COOK', 'DUPAGE', 'KANE', 'KENDALL', 'LAKE', 'MCHENRY', 'WILL', 'GRUNDY', 'DEKALB'), county_fip]

ctpp_flows[TRACTCE %in% as.numeric(cbd_tracts) & !work_tract_shortfips %in% as.numeric(cbd_tracts), loop_to_not_loop := 1]
ctpp_flows[!TRACTCE %in% as.numeric(cbd_tracts) & work_tract_shortfips %in% as.numeric(cbd_tracts), not_loop_to_loop := 1]
ctpp_flows[TRACTCE %in% as.numeric(chicago_tracts) & work_tract_shortfips %in% as.numeric(chicago_tracts) & loop_to_not_loop + not_loop_to_loop ==0, intra_district := 1]
ctpp_flows[residence_county == work_county & loop_to_not_loop + not_loop_to_loop ==0 & residence_county %in% district_counties, intra_district := 1]
ctpp_flows[not_loop_to_loop + loop_to_not_loop + intra_district + d2d_not_loop == 0, d2d_not_loop := 1]


pums_per_flows = ctpp_flows[puma_id %in% as.numeric(pumas_in_region), .(p_to_loop = sum(not_loop_to_loop * flow * factor),
                                p_from_loop = sum(loop_to_not_loop* flow * factor),
                                p_intra_district = sum(intra_district* flow * factor),
                                p_d2d_not_loop = sum(d2d_not_loop* flow * factor)), by = .(puma_id)]

pums_per_flows[, puma_id := str_pad(puma_id, 5, pad = '0')]
stop()

workers = pums_person_all[, .(p_worker = sum(WGTP * (FTWORKER + PTWORKER))), PUMA]
pums_per_flows[, p_workers := p_to_loop + p_from_loop + p_intra_district + p_d2d_not_loop]
pums_per_flows = pums_per_flows[workers, on = .(puma_id = PUMA)]
# scale to pums workers
pums_per_flows[, worker_factor := p_worker/p_workers]

cols= c('p_to_loop', 'p_from_loop', 'p_intra_district', 'p_d2d_not_loop')
pums_per_flows[, (cols) := lapply(.SD, function(x) x * worker_factor), .SDcols = cols]
pums_per_flows[pums_per_gender, p_total := p_total, on = .(puma_id)]
pums_per_flows[, p_not_worker := p_total - (p_to_loop + p_from_loop + p_intra_district + p_d2d_not_loop)]

# scale to match WSP
frac_to_loop = 566527/(566527 + 50699 + 2201803 + 1237335)
frac_from_loop = 50699/(566527 + 50699 + 2201803 + 1237335)
frac_intra_district = 2201803/(566527 + 50699 + 2201803 + 1237335)
frac_d2d = 1237335/(566527 + 50699 + 2201803 + 1237335)

pums_per_flows[, p_to_loop := ]

# transit boardings

cta_bus_boardings = fread(file.path(wt_dir, 'CTA_Average_Bus_Ridership_1999_2021.csv'))
cta_bus_boardings = cta_bus_boardings[YEAR == 2018 & DAY_TYPE == 'Weekday']
cta_bus_boardings_n = cta_bus_boardings[, .(mean(AVG_RIDES)), ROUTE][, sum(V1)]

cta_rail_boardings = fread(file.path(wt_dir, 'CTA_Average_Rail_Station_Ridership_1999_2021.csv'))
cta_rail_boardings = cta_rail_boardings[YEAR == 2018 & DAY_TYPE == 'Weekday']
cta_rail_boardings_n = cta_rail_boardings[, .(mean(DAILY_AVG_RIDES)), NAME][, sum(V1)]
cta_rail_boardings_n = 728643 # from annual report; RTAMS dataset numbers don't match...

PACE_boardings = fread(file.path(wt_dir, 'Pace_Monthly_Ridership_2003_2021_0.csv'))
PACE_boardings = PACE_boardings[YEAR == 2018 & DAY_TYPE == 'Weekday']
PACE_boardings_n = PACE_boardings[, .(mean(AVG_RIDES)), ROUTE][, sum(V1)]

metra_boardings = readxl::read_xlsx(file.path(settings$data_dir, 'Metra_2019/2019_Origin-Destination_survey_package/2019_OriginDestinationSurvey_V2_send.xlsx'))
setDT(metra_boardings)
metra_rtams = fread(file.path(wt_dir, 'Metra_Ridership_by_Station_Boarding_Alighting Survey_1979_2018.csv'))

metra_shp = st_read(file.path(settings$data_dir, 'Metra_2019/2019_Origin-Destination_survey_package/Origin_Destination_shapefiles/Origins2019.shp'))
metra_shp = st_transform(metra_shp, st_crs(puma_sf))
metra_to_puma = st_join(metra_shp, puma_sf)

metra_to_puma = setDT(metra_to_puma)
metra_boardings[metra_to_puma, puma_id := PUMACE10, on = .(Serial_ID = SERIAL_ID)]

pums_metra = metra_boardings[!is.na(puma_id), .(n_metra = sum(ON_Weight, na.rm = TRUE)), .(puma_id)]
metra_boardings[, sum(ON_Weight, na.rm= TRUE)]

pums_metra[, n_metra := n_metra * (metra_rtams[YEAR == '2018', sum(BOARDS)]/metra_boardings[!is.na(puma_id), sum(ON_Weight, na.rm= TRUE)])] 

# Construct final dataset -----------------------------------------------------

setkey(pums_hsize, puma_id)

target_dt = merge(pums_hworker, pums_hsize)      # pums_hsize[pums_hworker]
target_dt = merge(target_dt, pums_hinc)          # target_dt[pums_hinc]
target_dt = merge(target_dt, pums_hveh)          # target_dt[pums_hveh]
target_dt = merge(target_dt, pums_lifecycle)         # target_dt[pums_hhead]
target_dt = merge(target_dt, pums_per_gender)    # target_dt[pums_per_gender]
target_dt = merge(target_dt, pums_per_age)       # target_dt[pums_per_age]
target_dt = merge(target_dt, pums_per_commute)
target_dt = merge(target_dt, pums_per_flows)
target_dt = merge(target_dt, pums_metra)



# Save file ------------------------------------------------------------------

# Check that names match survey data
hh_file = file.path(wt_dir, 'hh_survey.rds')
per_file = file.path(wt_dir, 'p_survey.rds')
hh_survey = readRDS(hh_file)
per_survey = readRDS(per_file)


setdiff(c(names(hh_survey), names(per_survey)), names(target_dt))

# set up initial expansion factor
cluster_totals = target_dt[, .(h_total = sum(h_total)), cluster]
survey_totals = hh_survey[, .(h_total = .N), cluster]
hh_survey[survey_totals, h_total:= i.h_total, on = .(cluster)]
hh_survey[cluster_totals, h_total_pums:= i.h_total, on = .(cluster)]

#survey[, .(h_total_pums/h_total, initial_weight)]


hh_survey[, initial_expansion_factor := h_total_pums/h_total]
per_survey[hh_survey, initial_expansion_factor := i.initial_expansion_factor, on = .(sampno)]
hh_survey[, h_total_pums := NULL]
hh_survey[, h_total := 1]
total_survey_exp = hh_survey[, sum(initial_expansion_factor)]
total_pums = target_dt[, sum(h_total)]

target_region = data.table(n_cta_bus = cta_bus_boardings_n[[1]], n_cta_rail = cta_rail_boardings_n[[1]], n_pace = PACE_boardings_n[[1]])

filename = file.path(wt_dir, "PUMA_2018_targets.rds")
message('Saving to targets to ', filename)
saveRDS(target_dt, 
        file = filename)

saveRDS(hh_survey, file = file.path(wt_dir, 'hh_survey.rds'))
saveRDS(per_survey, file = file.path(wt_dir, 'p_survey.rds'))
saveRDS(target_region, file = file.path(wt_dir, 'region_targets.rds'))

message('Target data created successfully')
