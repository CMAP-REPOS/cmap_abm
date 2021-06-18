# Preparing survey data for reweighting


library(data.table)
library(yaml)
library(readxl)
library(tigris)
library(sf)
# load settings


# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')

# Combine data from CMAP main and NIRPC

data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
nirpc_dir = file.path(data_dir, settings$nirpc_folder)
output_dir = settings$SPA_input_dir

project_dir = settings$proj_dir
wt_dir = file.path(project_dir, "underreporting_correction")




# read in necessary data
studies = c('cmap', 'nirpc')
folders = c(cmap_dir, nirpc_dir)

read_and_bind = function(folder_list, study_list, data_file_name, data_level) {
  dt_full = data.table()
  for(index in 1:length(study_list)){
    folder = folder_list[index]
    study = study_list[index]
    dt = fread(file.path(folder, data_file_name))
    dt[, study := study]
    cat(nrow(dt), 'records in', study, data_level, 'file\n')
    dt_full = rbindlist(list(dt_full, dt), fill = TRUE, use.names = TRUE)
  }
  
  cat(nrow(dt_full), 'total', data_level, 'records\n')
  
  return(dt_full)
}

join_spatial =
  function(
    x,
    y,
    id_col,
    lon_col,
    lat_col,
    crs_lonlat = 4326,
    crs_equal_area = 5070,
    largest = FALSE){
    
    n_x = x[, .N]
    
    x_spatial =
      x[!is.na(get(lat_col)), c(id_col, lon_col, lat_col), with=FALSE] %>%
      st_as_sf(coords = c(lon_col, lat_col), crs = crs_lonlat) %>%
      st_transform(crs = crs_equal_area)
    
    y_with_id =
      y %>%
      st_transform(crs = crs_equal_area) %>%
      st_join(x_spatial, ., largest = largest) %>%
      st_drop_geometry() %>%
      data.table()
    
    x_with_y = merge(x, y_with_id, by = id_col, all.x = TRUE)
    
    if (largest) {
      stopifnot(n_x == x_with_y[, .N])
    }
    
    return(x_with_y)
  }
hh_raw = read_and_bind(folders, studies, 'household.csv', 'hh')
per_raw = read_and_bind(folders, studies, 'person.csv', 'person')
place_raw = read_and_bind(folders, studies, 'place.csv', 'place')
transit_raw = read_and_bind(folders, studies, 'place_transit.csv', 'place')
vehicle_raw = read_and_bind(folders, studies, 'vehicle.csv', 'vehicle')
location_raw = read_and_bind(folders, studies, 'location_wZones.csv', 'location')




# Weights: CMAP already has weight attached (wthhfin, wtperfin)
# Read in and attach IPF weight to nirpc

nirpc_hh_weights = fread(file.path(nirpc_dir, 'weights', 'ipf', 'household_weights.csv'))
nirpc_per_weights = fread(file.path(nirpc_dir, 'weights', 'ipf', 'person_weights.csv'))
nirpc_strata = fread(file.path(nirpc_dir, 'weights', 'standard', 'household_weights.csv'))

hh_raw[nirpc_hh_weights, wthhfin_nirpc := i.wthhfin, on = .(sampno)]
per_raw[nirpc_per_weights, wtperfin_nirpc := i.wtperfin, on = .(sampno, perno)]

hh_raw[, wthhfin := fcoalesce(wthhfin, wthhfin_nirpc)]
per_raw[, wtperfin := fcoalesce(wtperfin, wtperfin_nirpc)]

# # remove sunday travel dates?
# hh_raw = hh_raw[wday(travdate) != 1]
# per_raw = per_raw[sampno %in% hh_raw$sampno]
# place_raw = place_raw[sampno %in% hh_raw$sampno]

# Read mode/purpose edits
purpose_edits = setDT(read_xlsx(file.path(data_dir, 'tpurp_update.xlsx')))
purpose_edits_nirpc = setDT(read_xlsx(file.path(data_dir, 'tpurp_update_nirpc.xlsx')))
mode_edits = setDT(read_xlsx(file.path(data_dir, 'mode_update.xlsx')))

### --- School bus incorrectly coded as transit --- ###
place_raw[mode!=401 & (payf_o %like% "school bus" | payf_o %like% "public school"),mode:=401]
#
### --- "Other" mode --- ###
place_raw[study == 'nirpc' & mode==997 & mode_o=="RV" & perno==1,mode:=202]                                                                           ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o=="RV" & perno>1,mode:=203]                                                                                            ## -- auto/van/truck passenger
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("DROVE PRIVATE VEHICLE","DROVE",
                                                       "DRIVE MY CAR","DROVE SELF","CAR",
                                                       "GOLF CART","GARBAGE TRUCK","TRACKTOR","TACKTOR") & party==1,mode:=202]               ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("DROVE"),mode:=202]                                                                                             ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("CAR"),mode:=202]                                                                                                  ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("NEIGHBOR DROVE ME.","FRIEND"),mode:=203]    ## -- auto/van/truck passenger
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("WENT FOR A RUN"),mode:=101]                                             ## -- walk 

tpurp_edit = rbindlist(list(purpose_edits, purpose_edits_nirpc), fill = TRUE, use.names = TRUE)

place_raw[, tpurp_orig := tpurp]
place_raw[tpurp_edit, tpurp := new_tpurp, on = .(sampno, perno, placeno, traveldayno, locno)]

place_raw[, mode_orig := mode]
place_raw[mode_edits, mode := i.new_mode, on = .(sampno, perno, placeno, traveldayno, locno)]

# CMAP target data prep

per_hh_data = hh_raw[per_raw, on = .(sampno)]


# assign strata
per_hh_data[nirpc_strata, stratum := varstrat, on = .(sampno)]

# get block groups
in_bgs = block_groups(state = 18)
home_locs = location_raw[state == 'IN' & loctype == 1, .(sampno, latitude, longitude)]
home_bgs = join_spatial(home_locs, in_bgs,  lon_col = 'longitude', lat_col = 'latitude', id_col = 'sampno')

per_hh_data[home_bgs, block_group := i.GEOID, on = .(sampno)]


# puma_tract = fread('https://www2.census.gov/geo/docs/maps-data/data/rel/2010_Census_Tract_to_2010_PUMA.txt')

names(location_raw)
per_hh_data[location_raw[loctype == 1], puma_id := puma10, on = .(sampno)]

per_hh_data[study=='nirpc'& puma_id %in% c(101, 102), cluster := 1]
per_hh_data[study=='nirpc'& puma_id == 103, cluster := 2]
per_hh_data[study=='nirpc'& puma_id == 104, cluster := 3]
per_hh_data[study=='nirpc'& puma_id == 200, cluster := 4]
per_hh_data[study=='nirpc'& puma_id == 300, cluster := 5]

# hhsize

per_hh_data[, h_size_1 := ifelse(hhsize == 1, 1, 0)]
per_hh_data[, h_size_2 := ifelse(hhsize == 2, 1, 0)]
per_hh_data[, h_size_3 := ifelse(hhsize == 3, 1, 0)]
per_hh_data[, h_size_4 := ifelse(hhsize >= 4, 1, 0)]


# income

imputed_income = readRDS(file.path(wt_dir, 'imputed_income.rds'))

per_hh_data[imputed_income, income_aggregate := i.income_imputed_aggregate, on = .(sampno)]
per_hh_data[income_aggregate == 5, income_aggregate := 4] # aggregated wrong before saving...

per_hh_data[, h_inc_35 := ifelse(income_aggregate == 1, 1, 0)]
per_hh_data[, h_inc_60 := ifelse(income_aggregate == 2, 1, 0)]
per_hh_data[, h_inc_100 := ifelse(income_aggregate == 3, 1, 0)]
per_hh_data[, h_inc_100p := ifelse(income_aggregate == 4, 1, 0)]

# hh workers

per_hh_data[, num_workers := sum(emply_ask == 1), by = .(sampno)]
per_hh_data[, .N, num_workers]

per_hh_data[, h_worker_1 := ifelse(num_workers == 1, 1, 0)]
per_hh_data[, h_worker_2 := ifelse(num_workers == 2, 1, 0)]
per_hh_data[, h_worker_3 := ifelse(num_workers >= 3, 1, 0)]
per_hh_data[, h_worker_0 := ifelse(num_workers == 0, 1, 0)]

# zero auto
per_hh_data[, autosuff := fcase(hhveh ==0, 0,
                                hhveh < num_workers, 1,
                                hhveh >= num_workers, 2)]


per_hh_data[, h_zero_veh := ifelse(autosuff == 0, 1, 0)]
per_hh_data[, h_veh_lt_wrk := ifelse(autosuff == 1, 1, 0)]
per_hh_data[, h_veh_gt_wrk := ifelse(autosuff == 2, 1, 0)]

# race

# per_data[, .N, .(race_white, race_asian, race_afam, race_hisp, race_other, race_no_answer)][order(-N)]
per_hh_data[, .N, .(race)][order(-N)]


# recode to a single variable:
# 0 = missing
# 1 = white only
# 2 = afam only
# 3 = asian only
# 4 = other only
# 5 = multiple races

per_hh_data[, race_estimation := NULL]
per_hh_data[, race_estimation := 0L]

per_hh_data[race == 1, race_estimation := 1L]
per_hh_data[race == 2, race_estimation := 2L]
per_hh_data[race_estimation == 0 & race > 0, race_estimation := 3L]

# 987 need imputation (?)

##########
#### ethnicity imputation 
#########

## add block group

per_hh_data[, bg_geoid := as.numeric(block_group)]

# ACS race data
# ACS measures it as white-only, black-only, etc.


table_code_race = 'B02001'

acs_race_table_1 = data.table(
  get_acs(
    geography = "block group",
    table = table_code_race,
    state = 18 ,
    county = 127 )
)

acs_race_table_2 = data.table(
  get_acs(
    geography = "block group",
    table = table_code_race,
    state = 18,
    county = 89
  )
)

acs_race_table_3 = data.table(
  get_acs(
    geography = "block group",
    table = table_code_race,
    state = 18,
    county = 91
  )
)

acs_race_table_4 = data.table(
  get_acs(
    geography = "block group",
    table = table_code_race,
    state = 18,
    county = 31
  )
)
acs_race_table = rbind(acs_race_table_1, acs_race_table_2, acs_race_table_3, acs_race_table_4)

setnames(acs_race_table, "GEOID", "bg_geoid")
acs_race_table = 
  dcast(
    acs_race_table,
    bg_geoid ~ variable,
    value.var = "estimate")
acs_vars = load_variables(year = 2018, "acs5", cache = TRUE)
setDT(acs_vars)
acs_vars[str_detect(name, table_code_race)]

acs_race_table[, frace_white_only :=
                 ifelse(B02001_001 != 0,
                        round(B02001_002 / B02001_001, 5), 0)]

acs_race_table[, frace_afam_only :=
                 ifelse(B02001_001 != 0,
                        round(B02001_003 / B02001_001, 5), 0)]


acs_race_table[, frace_other :=
                 ifelse(B02001_001 != 0,
                        1 - (frace_white_only + frace_afam_only), 0)]

acs_race_table[, bg_geoid := as.numeric(bg_geoid)]

# # Get race distribution for entire study geography
# Get block groups for study region from what was shared with MSG

acs_race_table_aggregate = acs_race_table[bg_geoid %in% per_hh_data[study=='nirpc',bg_geoid]]

acs_race_table_aggregate[, `:=` (percent_white = sum(B02001_002) / sum(B02001_001),
                                 percent_afam = sum(B02001_003) / sum(B02001_001),
                                 percent_other = 1 - (sum(B02001_002) + sum(B02001_003)) /sum(B02001_001))]

acs_race_table_aggregate = unique(acs_race_table_aggregate[,.(percent_white, percent_afam, percent_other)])

frace_df = 
  acs_race_table[, 
                 .(bg_geoid, 
                   frace_white_only,
                   frace_afam_only,
                   frace_other)]

per_hh_data_prev = copy(per_hh_data)
per_hh_data = merge(per_hh_data[study == 'nirpc'], frace_df, by = 'bg_geoid', all.x=TRUE)
stopifnot(nrow(per_hh_data_prev[study=='nirpc']) == nrow(per_hh_data[study=='nirpc']))

per_hh_data[, 
            race_estimation_f := factor(race_estimation, levels = c(0:3), labels = c("Missing", "White", "African American", "Other"))]



per_hh_data[, num_people_cat := hhsize]

per_hh_data[num_people_cat > 5, num_people_cat := 5]


per_hh_data[, college_educated := 0]
per_hh_data[educ == 5, college_educated := 1]
per_hh_data[educ == 6, college_educated := 2]

per_hh_data[, employed := 0]
per_hh_data[emply_ask == 1, employed := 1]


per_hh_data[, has_license := 0]
per_hh_data[lic == 1, has_license := 1]

per_hh_data[, is_student := 0]
per_hh_data[stude %in% 1:2, is_student := 1]

race_counts = dcast(
  per_hh_data[, .N, .(sampno, race_estimation_f)],
  sampno ~ race_estimation_f, value.var='N',
  fill = 0)

setnames(race_counts, "Missing",  "n_race_missing")
setnames(race_counts, "White",    "n_race_white")
setnames(race_counts, "African American",    "n_race_afam")
setnames(race_counts, "Other",    "n_race_other")

per_hh_data = merge(per_hh_data, race_counts, by = 'sampno')

per_hh_data[, .N, .(hhsize, num_people_check = n_race_missing + n_race_white + n_race_afam + n_race_other)]

per_hh_data[hhsize == n_race_missing, .N]

per_hh_data[, perc_race_white := n_race_white / hhsize]
per_hh_data[, perc_race_afam := n_race_afam / hhsize]
per_hh_data[, perc_race_other := n_race_other / hhsize]

#### ethnicity model
ethnicity_model_1 = nnet::multinom(race_estimation_f ~ 
                                     perc_race_white + 
                                     perc_race_afam + 
                                     perc_race_other, 
                                   data = per_hh_data,
                                   subset = per_hh_data[, race_estimation_f] != 'Missing',
                                   Hess = TRUE, maxit = 500)

ethnicity_model_2 = nnet::multinom(race_estimation_f ~ 
                                     frace_white_only +
                                     frace_afam_only +
                                     frace_other +
                                     factor(college_educated) + 
                                     factor(employed) +
                                     factor(num_people_cat) +
                                     is_student +
                                     factor(has_license) +
                                     h_inc_35 +
                                     h_inc_60 +
                                     h_inc_100 +
                                     h_inc_100p,
                                   data = per_hh_data,
                                   subset = per_hh_data[, race_estimation_f] != 'Missing',
                                   Hess = TRUE, maxit = 500)

summary(ethnicity_model_1)
summary(ethnicity_model_2)
broom::tidy(ethnicity_model_1, exponentiate = FALSE) %>% print(n = 400)
broom::tidy(ethnicity_model_2, exponentiate = FALSE) %>% print(n = 400)

# DescTools::PseudoR2(ethnicity_model_1)
# DescTools::PseudoR2(ethnicity_model_2)

saveRDS(broom::tidy(ethnicity_model_1, exponentiate = FALSE) %>% print(n = 400), file.path(wt_dir, "ethnicity_imputation_model_1_coef.rds"))
saveRDS(broom::tidy(ethnicity_model_2, exponentiate = FALSE) %>% print(n = 400), file.path(wt_dir, "ethnicity_imputation_model_2_coef.rds"))

# saveRDS(DescTools::PseudoR2(ethnicity_model_1), file.path(wt_dir, "intermediate_files", "ethnicity_imputation_model_1_fit.rds"))
# saveRDS(DescTools::PseudoR2(ethnicity_model_2), file.path(wt_dir, "intermediate_files", "ethnicity_imputation_model_2_fit.rds"))


per_hh_data[
  race_estimation_f == 'Missing' & hhsize != n_race_missing, 
  .N,
  .(race)]

# we are estimating two models. one is to be used where we have some
# information on race within the household (model_1)
# the second model (model_2) is to be used when we have no information
# on race in the household

# In Baton Rouge, model is binary so predict produces a vector
# Make it into a data.frame

per_hh_data[
  race_estimation_f == 'Missing' & hhsize != n_race_missing, 
  c("ethnicity_imputation_1", "ethnicity_imputation_2","ethnicity_imputation_3") :=
    data.frame(White=predict(ethnicity_model_1, newdata=.SD, type='probs')[, "White"],
               African_American = predict(ethnicity_model_1, newdata = .SD, type = 'probs')[, "African American"],
               Other = predict(ethnicity_model_1, newdata = .SD, type = 'probs')[, "Other"])]

per_hh_data[
  race_estimation_f == 'Missing' & hhsize == n_race_missing, 
  c("ethnicity_imputation_1", "ethnicity_imputation_2","ethnicity_imputation_3") := 
    data.frame(White=predict(ethnicity_model_2, newdata=.SD, type='probs')[, "White"],
               African_American = predict(ethnicity_model_2, newdata = .SD, type = 'probs')[, "African American"],
               Other = predict(ethnicity_model_2, newdata = .SD, type = 'probs')[, "Other"])]
per_hh_data[, 
            .N, 
            .(ethnicity_imputation_1 + ethnicity_imputation_2 + ethnicity_imputation_3 )]


# assign probability of 1 for those who reported ethnicity
per_hh_data[race_estimation_f != 'Missing', 
            c("ethnicity_imputation_1", 
              "ethnicity_imputation_2",
              "ethnicity_imputation_3") := 0]

per_hh_data[race_estimation == 1, ethnicity_imputation_1 := 1]
per_hh_data[race_estimation == 2, ethnicity_imputation_2 := 1]
per_hh_data[race_estimation == 3, ethnicity_imputation_3 := 1]

per_hh_data[, 
            .N, 
            .(ethnicity_imputation_1 + ethnicity_imputation_2 + ethnicity_imputation_3)]

per_hh_data[, 
            .(.N, 
              mean(ethnicity_imputation_1), 
              mean(ethnicity_imputation_2),
              mean(ethnicity_imputation_3)),
            .(race_estimation_f)][order(race_estimation_f)]

per_hh_data[, p_race_white := as.numeric(race == 1)]
per_hh_data[, p_race_afam := as.numeric(race == 2)]
per_hh_data[, p_race_other := as.numeric(race > 2)]
per_hh_data[race < 0, p_race_white := ethnicity_imputation_1]
per_hh_data[race < 0, p_race_afam := ethnicity_imputation_2]
per_hh_data[race < 0, p_race_other := ethnicity_imputation_3]

# age
imputed_ages = readRDS(file.path(wt_dir, 'imputed_ages.rds'))

per_hh_data[imputed_ages, age_imputed := i.age_imputed, on = .(perno, sampno)]
per_hh_data[is.na(age_imputed), age_imputed := age]

per_hh_data[, p_age_lt_5 := ifelse(age_imputed < 5, 1, 0)]
per_hh_data[, p_age_5_14 := ifelse(age_imputed %in% 5:14, 1, 0)]
per_hh_data[, p_age_15_17 := ifelse(age_imputed %in% 15:17, 1, 0)]
per_hh_data[, p_age_18_34 := ifelse(age_imputed %in% 18:34, 1, 0)]
per_hh_data[, p_age_35_64 := ifelse(age_imputed %in% 35:64, 1, 0)]
per_hh_data[, p_age_gt_65 := ifelse(age_imputed >=65, 1, 0)]

# lifecycle
per_hh_data[, min_age := min(age_imputed), by = .(sampno) ]
per_hh_data[, max_age := max(age_imputed), by = .(sampno)]
per_hh_data[, num_adults := sum(age_imputed >= 18), by = .(sampno)]
per_hh_data[, num_kids := sum(age_imputed < 18), by = .(sampno)]

per_hh_data[, h_lifecycle_prek := fifelse(min_age < 6, 1, 0)]
per_hh_data[, h_lifecycle_k6_17 := fifelse(min_age %in% 6:17, 1, 0)]
per_hh_data[, h_lifecycle_ad1_lt35 := fifelse(min_age >= 18 & max_age <35 & hhsize == 1, 1, 0)]
per_hh_data[, h_lifecycle_ad1_35to64 := fifelse(min_age >= 18 & hhsize == 1 & max_age %in% c(35:64), 1, 0)]
per_hh_data[, h_lifecycle_ad1_gt64 := fifelse(min_age >= 18 & hhsize == 1 & max_age >=65, 1, 0)]
per_hh_data[, h_lifecycle_ad2_lt35 := fifelse(min_age >= 18 & max_age <35 & hhsize >= 2, 1, 0)]
per_hh_data[, h_lifecycle_ad2_35to64 := fifelse(min_age >= 18 & hhsize >= 2 & max_age %in% c(35:64), 1, 0)]
per_hh_data[, h_lifecycle_ad2_gt64 := fifelse(min_age >= 18 & hhsize >= 2 & max_age >=65, 1, 0)]


transit_raw[place_raw, distance := i.distance, on = .(sampno, perno, placeno)]

# gender
imputed_sex = readRDS(file.path(wt_dir, 'imputed_sex.rds'))

per_hh_data[imputed_sex, imputed_sex := i.imputed_sex, on = .(perno, sampno)]
per_hh_data[is.na(imputed_sex), imputed_sex := sex]

per_hh_data[, p_male := 1 * (imputed_sex == 1)]
per_hh_data[, p_female := 1 * (imputed_sex == 2)]
per_hh_data[, p_total := 1]

# mode to work
# Need the following
# "p_commute_none"    
# "p_commute_home"     
# "p_commute_car_alone" 
# "p_commute_carpool"
# "p_commute_walkbike"
# "p_commute_other"

# per_hh_data[, typical_commute_mode := ]
per_hh_data[wmode == 18, typical_commute_mode := 'none']
per_hh_data[wmode <0 , typical_commute_mode := 'none']

# per_hh_data[job_type == 3, typical_commute_mode := 'home']
per_hh_data[wmode %in% c(4:6), typical_commute_mode := 'car']
per_hh_data[wmode %in% c(1, 2), typical_commute_mode := 'walkbike']
per_hh_data[wmode %in% c(7:17, 97, 3),
            typical_commute_mode := 'other']

# Commute mode dummy variables
per_hh_data[, p_commute_none := 0]
per_hh_data[typical_commute_mode == 'none', p_commute_none := 1]
# per_hh_data[, p_commute_home := 0]
# per_hh_data[typical_commute_mode == 'home', p_commute_home := 1]

per_hh_data[, p_commute_car := 0]
per_hh_data[typical_commute_mode == 'car', p_commute_car := 1]
per_hh_data[, p_commute_walkbike := 0]
per_hh_data[typical_commute_mode == 'walkbike', p_commute_walkbike := 1]

per_hh_data[, p_commute_other := 0]
per_hh_data[typical_commute_mode == 'other', p_commute_other := 1]

# select data for weighting

h_cols = names(per_hh_data)[names(per_hh_data) %like% '^h_' ]
p_cols = names(per_hh_data)[names(per_hh_data) %like% '^p_']
# transit_cols = names(per_hh_data)[names(per_hh_data) %like% '^n_']

hh_survey = per_hh_data[per_hh_data[ , .I[which.min(perno)], by = sampno]$V1][study == 'nirpc'][, c('sampno', 'cluster', 'puma_id', h_cols), with = FALSE]


p_survey = per_hh_data[, c('puma_id', 'cluster', 'perno', 'sampno', p_cols), with = FALSE]

# trip_data = per_hh_data[, lapply(.SD, min), by = .(sampno), .SDcols =transit_cols ]

# hh_survey = hh_survey[trip_data, on = .(sampno),nomatch = 0]

# limit to nirpc
hh_survey = hh_survey[sampno %in% per_hh_data[study == 'nirpc', sampno]]
p_survey = p_survey[sampno %in% per_hh_data[study == 'nirpc', sampno]]

saveRDS(hh_survey, file = file.path(wt_dir, 'hh_survey_nirpc.rds'))
saveRDS(p_survey, file = file.path(wt_dir, 'p_survey_nirpc.rds'))
