library(data.table)
# library(tmrtools)
library(nnet) # for the multinomial regression function
# source('../for_tmrtools.R')
library(yaml)
library(readxl)
library(geosphere)
# working directory
# Paths and other constants

get_distance_meters =
  function(location_1, location_2) {
    distance_meters =
      distHaversine(
        matrix(location_1, ncol = 2),
        matrix(location_2, ncol = 2))
    return(distance_meters)
  }

# load settings

# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm_lf/survey_data_prep/cmap_inputs.yml')


data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
output_dir = settings$SPA_input_dir

project_dir = settings$proj_dir
wt_dir = file.path(project_dir, "underreporting_correction")


# # Read codebook
# codebook_path = file.path(project_dir, '4.Deliverables/Final Data Deliverable/Data_Deliverable_Codebook.xlsx')
# var_labels = read_codebook(codebook_path, varvals=FALSE, sheet = 'summary')
# val_labels = read_codebook(codebook_path)



# read in necessary data
studies = c('cmap')
folders = c(cmap_dir)

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

hh_raw = read_and_bind(folders, studies, 'household.csv', 'hh')
per_raw = read_and_bind(folders, studies, 'person.csv', 'person')
place_raw = read_and_bind(folders, studies, 'place.csv', 'place')
transit_raw = read_and_bind(folders, studies, 'place_transit.csv', 'place')
vehicle_raw = read_and_bind(folders, studies, 'vehicle.csv', 'vehicle')
location_raw = read_and_bind(folders, studies, 'location_wZones.csv', 'location')




# Weights: CMAP already has weight attached (wthhfin, wtperfin)

# purpose edits

place_raw[location_raw[loctype == 3], `:=` (school_lat = latitude, school_lon = longitude), on = .(sampno, perno)]
place_raw[location_raw[loctype == 2], `:=` (work_lat = latitude, work_lon = longitude), on = .(sampno, perno)]
place_raw[location_raw, `:=` (lat = latitude, lon = longitude), on = .(sampno, perno, locno)]

place_raw[, dist_to_school := get_distance_meters(c(lon, lat), c(school_lon, school_lat))]
place_raw[, dist_to_work := get_distance_meters(c(lon, lat), c(work_lon, work_lat))]

place_raw[, assume_school := ifelse(dist_to_school <= 100 & actdur >= (4 * 60), 1, 0)]
place_raw[, assume_work := ifelse(dist_to_work <= 100 & actdur >= (4 * 60), 1, 0)]
place_raw[is.na(assume_school), assume_school := 0]
place_raw[is.na(assume_work), assume_work := 0]


# Adjust weights for more meaningful t-statistics
hh_raw[, wthhfin_orig := wthhfin]
per_raw[, wtperfin_orig := wtperfin]
hh_raw[, wthhfin := wthhfin_orig/hh_raw[, mean(wthhfin_orig)]]
per_raw[, wtperfin := wtperfin_orig/hh_raw[, mean(wthhfin_orig)]]

hh_data = hh_raw[!is.na(wthhfin)]
person_data = per_raw[sampno %in% hh_data$sampno]
trip_data = place_raw[sampno %in% hh_data$sampno]

trip_data[person_data, wtperfin := i.wtperfin, on = .(sampno, perno)]
trip_data = trip_data[placeno > 1]

person_data[trip_data[, .(num_trips = .N), .(sampno, perno)], num_trips := i.num_trips, on = .(sampno, perno)]
person_data[hh_data, travel_dow := travday - 1, on = .(sampno)]

person_data[is.na(num_trips), num_trips := 0]
# calculate the number of mandatory trips and non-mandatory trips

trip_data[, .N, tpurp]

purpose_dt = trip_data[, .(sampno, perno, tpurp, assume_school, assume_work)]

purpose_codes_mandatory = c(3, 6)

day_class_dt = purpose_dt[, 
                          .(num_mandatory_trips     = sum(1 * (tpurp %in% purpose_codes_mandatory | assume_school == 1 | assume_work == 1)), 
                            num_non_mandatory_trips = sum(1 * (!tpurp %in% purpose_codes_mandatory & assume_work == 0 & assume_school == 0)),
                            num_school_trips = sum(1 * (tpurp == 6 | assume_school == 1))),
                          by=.(sampno, perno)]
nrow(day_class_dt)

fit_dt = merge(person_data[, .(sampno, perno, travel_dow, num_trips, wtperfin)], day_class_dt, by=c('perno', 'sampno'), all.x=TRUE)
stopifnot(
  nrow(fit_dt[, .N, .(sampno, perno)][N > 1]) == 0
)

# some qaqc
fit_dt[, .N, travel_dow]

# should return 0 rows:
stopifnot(
  fit_dt[, .N, .(num_trips, num_trips_check = num_mandatory_trips + num_non_mandatory_trips)
         ][num_trips != num_trips_check, .N] == 0,
  fit_dt[num_mandatory_trips + num_non_mandatory_trips != num_trips, .N] == 0
  
)

fit_dt[is.na(num_trips), num_trips := 0]
fit_dt[is.na(num_mandatory_trips), num_mandatory_trips := 0]
fit_dt[is.na(num_non_mandatory_trips), num_non_mandatory_trips := 0]
fit_dt[is.na(num_school_trips), num_school_trips := 0]

# qaqc
if ( fit_dt[, .N, .(num_trips, num_trip_checks = num_mandatory_trips + num_non_mandatory_trips)
            ][num_trips != num_trip_checks, .N] > 0 ){
  
  # update num_trips if there is a discrepancy
  warning('num_trips does not equal mandatory and non-mandatory trips')
  fit_dt[, num_trips := num_mandatory_trips + num_non_mandatory_trips]
  
}


# identify the 3 classifications
# (1) make no trips 
# (2) make mandatory (work or school) trips (and possibly other trips) 
# (3) make non-mandatory trips only.

fit_dt[, day_trip_class := NA_character_]
fit_dt[num_trips == 0, day_trip_class := 'none']
fit_dt[num_non_mandatory_trips != num_trips & num_trips > 0, day_trip_class := 'mandatory']
fit_dt[num_non_mandatory_trips == num_trips & num_trips > 0, day_trip_class := 'non-mandatory']
fit_dt[, day_trip_class := factor(day_trip_class, levels=c('none', 'mandatory', 'non-mandatory'))]

fit_dt[, .N, day_trip_class][order(day_trip_class)]

# filter which data from the person table we want to use
# add smartphone_type to person data
person_data =
  person_data[age >= 5 | aage >= 1 ,
              .(sampno, perno, age, aage, sex, stude, emply_ask, smrtphn, proxy, retmode, smode, schol, sloc)]

fit_dt_old = copy(fit_dt)
fit_dt = merge(fit_dt, person_data, by=c('sampno', 'perno')) # Reduces nrow

hh_data = hh_data[, .(sampno, hhveh, hhinc, hhinc2, study)]


fit_dt_old = copy(fit_dt)
fit_dt = merge(fit_dt, hh_data, by='sampno')


# # our model is going to be weighted by day weights
# # let's normalize so the standard errors are meaningful
# 
fit_dt[, estimation_weight := wtperfin]
# stopifnot(all.equal(fit_dt[, sum(estimation_weight)], nrow(fit_dt)))

# create some useful binary variables for the model
# Online vs rMove
fit_dt[, online_data := 1 * ((!retmode  %in% c(1:4) & study == 'cmap') | (retmode != 1 & study == 'nirpc'))]
fit_dt[, app_data := 1 * ((retmode  %in% c(1:4) & study == 'cmap') | (retmode == 1 & study == 'nirpc'))]

# Age groups

# impute age (move to survey data weighting)
if(file.exists(file.path(wt_dir, 'imputed_ages.rds'))){
  
  imputed_ages = readRDS(file.path(wt_dir, 'imputed_ages.rds'))
  
} else {

  fit_dt[, age_imputed := fcase(aage == 1, sample(0:4, .N, replace=TRUE),
                                aage == 2, sample(5:12, .N, replace=TRUE),
                                aage == 3, sample(13:15, .N, replace=TRUE),
                                aage == 4, sample(16:17, .N, replace=TRUE),
                                aage == 5, sample(18:44, .N, replace=TRUE),
                                aage == 6, sample(45:64, .N, replace=TRUE),
                                aage == 7, sample(65:84, .N, replace=TRUE),
                                default = -1L
                                )]
  fit_dt[age_imputed == -1, age_imputed := age]


  imputed_ages = fit_dt[, .(sampno, perno, age_imputed)]

  saveRDS(imputed_ages, file = file.path(settings$proj_dir, 'underreporting_correction', 'imputed_ages.rds'))

}

fit_dt[imputed_ages, age_imputed := i.age_imputed, on = .(perno, sampno)]

fit_dt[, age_18_35 := 1 * (between(age_imputed, 18, 34))]
fit_dt[, age_35_65 := 1 * (between(age_imputed, 35, 64))]

# Employment
# assume unanswered are not employed?
fit_dt[, employed := 1 * (emply_ask == 1)] # ft, pt, self-employed, volunteer/intern

# Student
# assume unanswered are not students?
fit_dt[, is_student := 1 * (stude %in% c(1, 2))] # full-time, part-time

# Gender
if(file.exists(file.path(wt_dir, 'imputed_sex.rds'))){
  imputed_sex = readRDS(file.path(wt_dir, 'imputed_sex.rds'))
  
} else {
  fit_dt[sex < 0, imputed_sex := sample(1:2, .N, replace = TRUE)]
  fit_dt[sex > 0, imputed_sex := sex]
  imputed_sex = fit_dt[, .(sampno, perno, imputed_sex)]
  saveRDS(imputed_sex, file = file.path(wt_dir, 'imputed_sex.rds'))
  
}


fit_dt[imputed_sex, imputed_sex := i.imputed_sex, on = .(perno, sampno)]

fit_dt[, is_male := 1 * (imputed_sex == 1)]

# Owns no car
fit_dt[, zero_vehicle := 1 * (hhveh == 0)]

# Income

# impute aggregate income

hh_raw[per_raw[(lic == 1), .(num_lic = .N), .(sampno)], num_lic := i.num_lic, on = .(sampno)]

hh_raw[, autosuff := fcase(hhveh == 0, 0,
                           hhveh < num_lic, 1,
                           hhveh >= num_lic, 2,
                           is.na(num_lic), 2)] # no licensed drivers but have vehicle?
if(file.exists(file.path(wt_dir, 'imputed_income.rds'))){
  
  imputed_income = readRDS(file.path(wt_dir, 'imputed_income.rds'))
  
} else {
  library(MASS)
  # 
  # 
  hh_raw[, income_aggregate := fcase(hhinc %in% c(1:3), 1,  # 0-30k
                                     hhinc2 == 1, 1,  # 0-30k
                                     hhinc %in% c(4:6), 2, # 30-60k
                                     hhinc2 == 2, 2, # 30-60k
                                     hhinc %in% c(7:8), 3,  # 60-99k
                                     hhinc2 == 3, 3,  # 60-99k
                                     hhinc == 9, 4, # 100-150k
                                     hhinc2 == 4, 4, # 100-150k
                                     hhinc == 10, 5, # 150k plus
                                     hhinc2 == 5, 5 # 150k plus
  )
  ]

  hh_raw[, income_aggregate := factor(income_aggregate)]

  hh_raw[hhinc > 0, income_detailed_f := factor(hhinc)]
  hh_raw[, .N, income_aggregate][order(income_aggregate)]

  hh_raw[, home_own_agg := fcase(homeown %in% c(1:3), 1,
                                 default = 0)]
  inc_model = polr(income_detailed_f ~ autosuff + home_own_agg,
                   data = hh_raw, subset = !is.na(hh_raw[, income_detailed_f]), Hess = TRUE)

  summary(inc_model)
  broom::tidy(inc_model, n = Inf)

  hh_raw[
    is.na(income_aggregate) | hhinc2 ==2,
    paste0("hh_imputation_", 1:10) := as.data.frame(predict(inc_model, newdata = .SD, type = "probs"))
  ]

  hh_raw[
    is.na(income_aggregate),
    .N,
    .(sum_probs = round(hh_imputation_1 +
                          hh_imputation_2 +
                          hh_imputation_3 +
                          hh_imputation_4 +
                          hh_imputation_5 +
                          hh_imputation_6 +
                          hh_imputation_7 +
                          hh_imputation_8 +
                          hh_imputation_9 +
                          hh_imputation_10, 4))]

  # get cumulative probability
  cols = paste0("hh_imputation_", 1:10)
  hh_raw[, paste0("hh_imputation_", 1:10, '_cumsum') := Reduce(`+`, .SD, accumulate = TRUE), .SDcols = cols]


  hh_raw[
    hhinc2 == 2, `:=` ( hh_imputation_4_cumsum = hh_imputation_4 / (hh_imputation_4 + hh_imputation_5 + hh_imputation_6),
                        hh_imputation_5_cumsum = (hh_imputation_4  + hh_imputation_5)/ (hh_imputation_4 + hh_imputation_5 + hh_imputation_6),
                        hh_imputation_6_cumsum = 1)]

  cumsum_cols = paste0(cols, '_cumsum')
  hh_raw[
    hhinc2 == 2, c(cumsum_cols[!cumsum_cols %in% c('hh_imputation_4_cumsum', 'hh_imputation_5_cumsum', 'hh_imputation_6_cumsum')]) := 0]


  set.seed(413)
  hh_raw[is.na(income_aggregate) | hhinc2 == 2, irand0_1 := runif(.N, 0, 1)]


  hh_raw[, income_imputed := fcase(irand0_1 <= hh_imputation_1_cumsum, 1,
                                   irand0_1 <= hh_imputation_2_cumsum, 2,
                                   irand0_1 <= hh_imputation_3_cumsum, 3,
                                   irand0_1 <= hh_imputation_4_cumsum, 4,
                                   irand0_1 <= hh_imputation_5_cumsum, 5,
                                   irand0_1 <= hh_imputation_6_cumsum, 6,
                                   irand0_1 <= hh_imputation_7_cumsum, 7,
                                   irand0_1 <= hh_imputation_8_cumsum, 8,
                                   irand0_1 <= hh_imputation_9_cumsum, 9,
                                   irand0_1 <= 1, 10
  )]


  hh_raw[!is.na(income_aggregate) & hhinc > 0, income_imputed := hhinc]
  hh_raw[, income_imputed_aggregate := fcase(income_imputed %in% c(1:4), 1,  # 0-35k
                                             income_imputed %in% c(5:6), 2, # 35-60k
                                             income_imputed %in% c(7:8), 3,  # 60-99k
                                             income_imputed == 9, 5, # 100-150k
                                             income_imputed == 10, 5 # 150k plus
  )
  ]

  hh_raw[is.na(income_imputed_aggregate), income_imputed_aggregate := hhinc2]

  imputed_income = hh_raw[, .(sampno, income_imputed, income_imputed_aggregate, irand0_1,
                              hh_imputation_1_cumsum, hh_imputation_2_cumsum,
                              hh_imputation_3_cumsum, hh_imputation_4_cumsum,
                              hh_imputation_5_cumsum, hh_imputation_6_cumsum,
                              hh_imputation_7_cumsum, hh_imputation_8_cumsum,
                              hh_imputation_9_cumsum, hh_imputation_10_cumsum)]
  saveRDS(inc_model, file = file.path(wt_dir, 'income_imputation_model.rds'))
  saveRDS(imputed_income, file = file.path(wt_dir, 'imputed_income.rds'))
}

fit_dt[imputed_income, income_aggregate := i.income_imputed_aggregate, on = .(sampno)]
fit_dt[income_aggregate == 5, income_aggregate := 4]
fit_dt[, income_aggregate := as.factor(income_aggregate)]

# Proxy
fit_dt[, was_proxied := 1 * (proxy == 1)]
fit_dt[, was_proxied_online_data := was_proxied * online_data]
fit_dt[, .N, .(was_proxied, online_data)][order(was_proxied, online_data)]


fit_dt[hh_raw, auto_lt_drivers := ifelse(autosuff <2, 1, 0), on = .(sampno)]

# # Smartphone type
fit_dt[, owns_phone := 1 * (smrtphn == 1 | app_data == 1)]  # not sure why some don't own phone but participated by smartphone

# the model weighted
model_rhs = 
  ~ online_data + 
  auto_lt_drivers +
  income_aggregate +
  age_18_35 + age_35_65 +
  income_aggregate +
  employed + 
  is_student +
  was_proxied +
  owns_phone 

fit_dt_adult = copy(fit_dt[age_imputed >= 18])
fit_dt_cmap = copy(fit_dt_adult[study == 'cmap'])

model = multinom(
  update(model_rhs, day_trip_class ~ .),
  data = fit_dt_cmap,
  weights = fit_dt_cmap[,estimation_weight],
  model = TRUE, Hess = TRUE)

summary(model)

options(scipen = 999)
tidy.opts = options(scipen = 99)
print(broom::tidy(model, exponentiate = FALSE), n = 400)

t(summary(model)$coefficients / summary(model)$standard.errors)
# fitted(model)

# rho-square
DescTools::PseudoR2(model) # Ohio: 0.164; # Baton Rouge 0.247

saveRDS(broom::tidy(model, exponentiate = FALSE), file.path(wt_dir,"model_summaries","day_pattern_model_coef_new.rds"))

# saveRDS(DescTools::PseudoR2(model), file.path(wt_dir, "intermediate_files", "day_pattern_model_fit_new.rds"))


fit_dt_cmap[, 
       .(weighted_sum = sum(estimation_weight)), 
       .(day_trip_class, online_data)][order(day_trip_class, online_data)]

# checks out
all.equal(
  colSums(predict(model, newdata = fit_dt_cmap, type = "probs") * fit_dt_cmap[, estimation_weight]),
  fit_dt_cmap[, .(weighted_sum = sum(estimation_weight)), .(day_trip_class)][order(day_trip_class), weighted_sum]
)

fit_dt_cmap[, model_predict_none := predict(model, newdata = fit_dt_cmap, type = "probs")[, 'none']]
fit_dt_cmap[, model_predict_mand := predict(model, newdata = fit_dt_cmap, type = "probs")[, 'mandatory']]
fit_dt_cmap[, model_predict_non_mand := predict(model, newdata = fit_dt_cmap, type = "probs")[, 'non-mandatory']]

fit_dt_cmap[, sum(model_predict_none * estimation_weight), by = .(online_data)]

# Recalculate predicteds without bias parameters removed
# rMove people tend to record more trips

model_coefs = coef(model)

model_coefs[, "online_data"] = 0
# model_coefs[, "was_proxied_online_data"] = 0
# model_coefs[, "online_data:age_18_35"] = 0

V_none = 0
V_mandatory = model.matrix(model_rhs, fit_dt_cmap) %*% t(t(model_coefs['mandatory',]))
V_non_mand = model.matrix(model_rhs, fit_dt_cmap) %*% t(t(model_coefs['non-mandatory',]))

V = cbind(V_none=V_none, V_mand=V_mandatory, V_non_mand=V_non_mand)
colnames(V) = c('none', 'mandatory', 'non-mandatory')
P = exp(V)

P = P/rowSums(P)

# Expect fewer "none" trips in model with bias parameters removed.
colSums(P)
colSums(predict(model, newdata = fit_dt_cmap, type = "probs"))


fit_dt_cmap[, new_model_predict_none := P[, 'none']]
fit_dt_cmap[, new_model_predict_mand := P[, 'mandatory']]
fit_dt_cmap[, new_model_predict_non_mand := P[, 'non-mandatory']]

# does it impact online only
fit_dt_cmap[, 
       .(wbias = sum(model_predict_none * estimation_weight), 
         wobias = sum(new_model_predict_none * estimation_weight)), 
       by=.(online_data)][order(online_data)]


# hh_weights[fit_dt, on = .(hh_id)]

# fit_dt_adult[, 
#        .(made_no_trips = sum(day_weight * new_model_predict_none),  
#          made_mandatory_trips = sum(day_weight * new_model_predict_mand),
#          made_nonmandatory_only = sum(day_weight * new_model_predict_non_mand)), 
#        .(puma_id)]

(day_category_without_bias = fit_dt_cmap[, 
                                    .(made_no_trips = sum(estimation_weight * new_model_predict_none),  
                                      made_mandatory_trips = sum(estimation_weight * new_model_predict_mand),
                                      made_nonmandatory_only = sum(estimation_weight * new_model_predict_non_mand)), 
                                    .(online_data)][order(online_data)])

(day_category_with_bias = fit_dt_cmap[, 
                                 .(made_no_trips = sum(estimation_weight * model_predict_none),  
                                   made_mandatory_trips = sum(estimation_weight * model_predict_mand),
                                   made_nonmandatory_only = sum(estimation_weight * model_predict_non_mand)), 
                                 .(online_data)][order(online_data)])


saveRDS(day_category_without_bias, file.path(wt_dir,"model_summaries","day_category_without_bias.rds"))
saveRDS(day_category_with_bias, file.path(wt_dir,"model_summaries","day_category_with_bias.rds"))


#-----------------------------------------------------------------------------
#-- Update survey data and target data input files
#-----------------------------------------------------------------------------
targets      = readRDS(file = file.path(wt_dir,"PUMA_2018_targets.rds"))
survey_data  = readRDS(file = file.path(wt_dir,"p_survey.rds"))

# Calculate number of people in each household falling into each day trip class

# Survey_data

# Average over days to get days per adult
person_predict_cmap = fit_dt_cmap[,
                        .(p_made_no_trips = mean(new_model_predict_none),
                          p_made_mandatory_trips = mean(new_model_predict_mand),
                          p_made_nonmandatory_only = mean(new_model_predict_non_mand)),
                        .(sampno, perno)]

# Average over person to get average probability per household-day (for adults)

person_predict_hh_cmap = person_predict_cmap[, 
                                               .(p_made_no_trips          = mean(p_made_no_trips),
                                                 p_made_mandatory_trips   = mean(p_made_mandatory_trips),
                                                 p_made_nonmandatory_only = mean(p_made_nonmandatory_only)),
                                               .(sampno)]

# Make sure that p_made_no_trips + p_made_mandatory_trips + p_made_non... = 1


survey_day_adj = merge(survey_data, person_predict_cmap, by = c('sampno', 'perno'), all = TRUE)

# Adjust for the fact that model is based on adults
survey_day_adj[, p_made_no_trips := p_made_no_trips * (p_total - p_age_lt_5 - p_age_5_14 - p_age_15_17)]
survey_day_adj[, p_made_mandatory_trips := p_made_mandatory_trips * (p_total - p_age_lt_5 - p_age_5_14 - p_age_15_17)]
survey_day_adj[, p_made_nonmandatory_only  := p_made_nonmandatory_only  * (p_total - p_age_lt_5 - p_age_5_14 - p_age_15_17)]
survey_day_adj[, p_made_not_applicable := p_age_lt_5 + p_age_5_14 + p_age_15_17]
survey_day_adj[is.na(survey_day_adj)]  = 0


setcolorder(survey_day_adj, c(
  'sampno', 'perno', 'puma_id',
  setdiff(names(survey_day_adj)[names(survey_day_adj) %like% '^p\\_'], 'p_total'),
  'p_total',
  'initial_expansion_factor'))

saveRDS(survey_day_adj, file.path(wt_dir, "p_survey_day_adjustments.rds"))


cmap_weights = hh_raw[study == 'cmap', .(sampno, wthhfin = wthhfin_orig)]

# Target data
survey_day_adj_wt = merge(survey_day_adj,
                          cmap_weights[, .(sampno, wthhfin)],
                          by = 'sampno', all.x=TRUE)

# Get the number of adults in each puma in each class
targets_predict = 
  survey_day_adj_wt[,
                    .(p_made_no_trips          = sum(wthhfin * p_made_no_trips),
                      p_made_mandatory_trips   = sum(wthhfin * p_made_mandatory_trips),
                      p_made_nonmandatory_only = sum(wthhfin * p_made_nonmandatory_only),
                      p_made_not_applicable    = sum(wthhfin * (p_age_lt_5+ p_age_5_14 + p_age_15_17))),
                    .(puma_id)]


targets_predict[, puma_id := str_pad(puma_id, 5, pad = '0')]

# targets_predict[targets, p_total := i.p_total, on = .(puma_id)]
# targets_predict[, factor := p_total_pums/p_total]
cols = c('p_made_no_trips', 'p_made_mandatory_trips', 'p_made_nonmandatory_only', 'p_made_not_applicable')
# targets_predict[, (cols) := lapply(.SD, function(x) x * factor), .SDcols = cols]
# targets_predict[, puma_id := sprintf('%05d', puma_id)]
targets_day_adjustment = merge(targets_predict[, c('puma_id', cols), with = FALSE], targets, by = 'puma_id', all.x=TRUE)


# Check
# Differences are due to mismatch between survey and targets for age distribution
targets_day_adjustment[
  , .(p_total,
      p_sum_trips = sum(p_made_no_trips) +
        sum(p_made_mandatory_trips) +
        sum(p_made_nonmandatory_only) +
        sum(p_made_not_applicable)),
  by = puma_id]
# Make sure that total number of adults in day trip classes matches total
# adults in p_total in each PUMA

targets_day_adjustment[, scale := sum(p_total - p_made_not_applicable) /
                         sum(p_made_no_trips + p_made_mandatory_trips + p_made_nonmandatory_only), .(puma_id)]

targets_day_adjustment[, p_made_no_trips := scale * p_made_no_trips]
targets_day_adjustment[, p_made_mandatory_trips := scale * p_made_mandatory_trips]
targets_day_adjustment[, p_made_nonmandatory_only := scale * p_made_nonmandatory_only]

# Check
targets_day_adjustment[
  , .(p_total,
      p_sum_trips = sum(p_made_no_trips) +
        sum(p_made_mandatory_trips) +
        sum(p_made_nonmandatory_only) +
        sum(p_made_not_applicable)),
  by = puma_id]


setcolorder(targets_day_adjustment,
            names(survey_day_adj)[!names(survey_day_adj) %in% c('sampno', 'perno', 'initial_expansion_factor', 'initial_expansion_factor_2019', 'final_weight', 'n_cta_bus', 'n_cta_rail', 'n_pace')])

saveRDS(targets_day_adjustment,
        file.path(wt_dir, "PUMA_2018_targets_day_adjustments.rds"))
