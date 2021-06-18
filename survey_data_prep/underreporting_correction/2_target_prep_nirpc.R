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
counties = zones17_dt[state  == 'IN', unique(county_fip)]
counties = str_sub(counties, 3, 5)

state_fips = 18

state = data.table(fips = state_fips, abb = 'in')

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

# DOWNLOAD FOR NIRPC
message('Loading PUMS data')
pums_hh  = fread(file = file.path(pums_dir, 'psam_h18.csv'), colClasses = c(PUMA = "character"))
pums_per = fread(file = file.path(pums_dir, 'psam_p18.csv'), colClasses = c(PUMA = "character"))

pums_hh_copy = copy(pums_hh)
pums_per_copy = copy(pums_per)

# Get shapefiles
# puma_sf = pumas(state_fips,  class='sf')
# 
# 
# 
# puma_sf[PUMACE10 %in% c(101, 102), cluster := 1]
# puma_sf[PUMACE10 == 103, cluster := 2]
# puma_sf[PUMACE10 == 104, cluster := 3]
# puma_sf[PUMACE10 == 200, cluster := 4]
# puma_sf[PUMACE10 == 300, cluster := 5]

pums_hh[PUMA %in% c('00101', '00102'), cluster := 1]
pums_hh[PUMA == '00103', cluster := 2]
pums_hh[PUMA == '00104', cluster := 3]
pums_hh[PUMA == '00200', cluster := 4]
pums_hh[PUMA == '00300', cluster := 5]

pums_per[PUMA %in% c('00101', '00102'), cluster := 1]
pums_per[PUMA == '00103', cluster := 2]
pums_per[PUMA == '00104', cluster := 3]
pums_per[PUMA == '00200', cluster := 4]
pums_per[PUMA == '00300', cluster := 5]




# FILTER TO DESIRED REGION AND PARAMETERS---------------------------------------

# Filter to exclude group quarters
# Filter to exclude vacant unit as number of persons in household is greater than 0
nrow(pums_hh)
pums_hh[, .N, TYPE]
pums_hh = pums_hh[TYPE == 1 & NP > 0] # TYPE == 1 is Housing Unit, NP = 0 is vacant
nrow(pums_hh)

# Filter to study region

pumas_in_region = c('00101', '00102','00103','00104','00200','00300' )
# View the PUMAS over the study area
# puma_region = puma_sf[as.numeric(puma_sf$PUMACE10) %in% pumas_in_region, ]

# puma_region = puma_region[puma_region$cluster > 0,]
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

pums_hh[, HHSIZEC := pmin(NP, 4)]

pums_hsize = dcast(
  pums_hh, 
  PUMA + cluster~ HHSIZEC, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_hsize,
  colnames(pums_hsize),
  c("puma_id", "cluster", "h_size_1", "h_size_2", "h_size_3", "h_size_4"))

pums_hsize[, h_total := h_size_1 + h_size_2 + h_size_3 + h_size_4]


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


# Person ethnicity ------------------------------------------------------------
pums_per_ethnicity = dcast(
  pums_person_all, 
  PUMA ~ RAC1P, 
  value.var = "WGTP",
  fun.aggregate = sum)

setnames(
  pums_per_ethnicity,
  colnames(pums_per_ethnicity),
  #  c("puma_id","p_white","p_afam","p_other_1","p_other_2","p_other_3","p_asian","p_other_4","p_other_5","p_multiple"))
  c("puma_id","p_white","p_afam","p_other_1","p_other_3", "p_other_6", "p_asian","p_other_4","p_other_5","p_multiple"))

pums_per_ethnicity[, `:=` (
  # p_other = p_other_1 + p_other_2 + p_other_3 + p_other_4 + p_other_5,
  p_other = p_other_1 + p_other_3 + p_other_4 + p_other_5 + p_asian + p_multiple + p_other_6,
  p_other_1 = NULL,
  # p_other_2 = NULL,
  p_other_3 = NULL,
  p_other_4 = NULL,
  p_other_5 = NULL,
  p_other_6 = NULL)]


pums_per_ethnicity
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

# Construct final dataset -----------------------------------------------------

setkey(pums_hsize, puma_id)

target_dt = merge(pums_hworker, pums_hsize)      # pums_hsize[pums_hworker]
target_dt = merge(target_dt, pums_hinc)          # target_dt[pums_hinc]
target_dt = merge(target_dt, pums_hveh)          # target_dt[pums_hveh]
target_dt = merge(target_dt, pums_per_gender)    # target_dt[pums_per_gender]
target_dt = merge(target_dt, pums_per_age)       # target_dt[pums_per_age]
target_dt = merge(target_dt, pums_per_commute)
target_dt = merge(target_dt, pums_per_ethnicity)



# Save file ------------------------------------------------------------------

# Check that names match survey data
hh_file = file.path(wt_dir, 'hh_survey_nirpc.rds')
per_file = file.path(wt_dir, 'p_survey_nirpc.rds')
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


filename = file.path(wt_dir, "PUMA_2018_targets_nirpc.rds")
message('Saving to targets to ', filename)
saveRDS(target_dt, 
        file = filename)

saveRDS(hh_survey, file = file.path(wt_dir, 'hh_survey_nirpc.rds'))
saveRDS(per_survey, file = file.path(wt_dir, 'p_survey_nirpc.rds'))

message('Target data created successfully')
