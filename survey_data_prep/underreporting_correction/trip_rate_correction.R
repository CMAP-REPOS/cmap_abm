
# Calculates person weights, day weights, and adjusted trip weights


library(data.table)
library(tmrtools)
library(dplyr)
library(stringr)
library(yaml)
library(readxl)
# load settings

# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')

# working directory
# Paths and other constants
project_dir = settings$proj_dir
wt_dir = file.path(project_dir, 'underreporting_correction')
data_dir = file.path(settings$SPA_input_dir, 'pre_trip_adjustment')


# read in necessary data

ex_hh     = fread(file.path(data_dir, 'HH_SPA_INPUT.csv'))
ex_person = fread(file.path(data_dir, 'PER_SPA_INPUT.csv'))
ex_trip   = fread(file.path(data_dir, 'PLACE_SPA_INPUT.csv'))

ex_person[ex_trip[PLANO > 1, .N, .(PERNO, SAMPN)], num_trips := N, on = .(PERNO, SAMPN)]


hh_weights = ex_hh[,
    .(hh_id = SAMPN,
      hh_size = HH_SIZE,
      hh_weight = HHEXPFAC,
      complete_weekdays = 1,
      puma_id = PUMA_ID,
      income_broad = HH_INC_CAT,
      num_kids = NUM_KIDS,
      res_type = RESTY,
      travel_dow = HH_DOW)]


# Calculate person weights -----------------------------------------------------

hh_weights[, person_weight := hh_weight] # For hh studies, person weight is same
# for all members of household
person_weights = merge(ex_person[, .(SAMPN, PERNO)],
                       hh_weights[, .(hh_id, person_weight)],
                       by.x ='SAMPN', by.y = 'hh_id', all.x=TRUE)

person_weights[is.na(person_weight), person_weight := 0]

stopifnot(
  sum(hh_weights[, hh_weight * hh_size]) == sum(person_weights[, person_weight])
)

# Get complete weekdays and day of week
day_complete_wkdays = 
  ex_person[, 
    .(hh_id = SAMPN, 
      person_id = PERNO, 
      day_num = 1, 
      num_trips = num_trips)]

# join weights to days for fit data
fit_dt = merge(day_complete_wkdays, hh_weights, by = 'hh_id', all=FALSE)[hh_weight > 0]

ex_trip = ex_trip[order(SAMPN, PERNO, PLANO)]
ex_trip[, o_purpose_category := shift(TPURP, type = 'lag'), by = c('SAMPN', 'PERNO')]

ex_trip = ex_trip[PLANO > 1]
ex_trip[, .N, TPURP][order(TPURP)]
ex_trip[, .N, o_purpose_category][order(o_purpose_category)]


# calculate the number of the following trips on complete days:
# a.	Home-based work/school trips (hbw)
# b.	Home-based other trips (hbo)
# c.	Non-home-based work/school trips (nhbw)
# d.  Non-home-based other trips (nhbo)

trip_data = merge(day_complete_wkdays, ex_trip,
                  by.x = c('hh_id', 'person_id'),
                  by.y = c('SAMPN', 'PERNO'), all=FALSE)

trip_data[hh_weights, travel_dow := i.travel_dow, on = .(hh_id)]

trip_data[, .N, TPURP]
trip_data[, d_purpose_category := TPURP]
pcat_work_school = c(1, 2, 3)
pcat_home = 0

trip_counts = trip_data[, 
                        .(num_trips_hbw = 
                            sum(1 * ((d_purpose_category %in% pcat_work_school & o_purpose_category == pcat_home) | 
                                       (o_purpose_category %in% pcat_work_school & d_purpose_category == pcat_home))), 
                          num_trips_hbo = 
                            sum(1 * ((d_purpose_category == pcat_home & !o_purpose_category %in% pcat_work_school) | 
                                       (o_purpose_category == pcat_home & !d_purpose_category %in% pcat_work_school))),
                          num_trips_nhbw = 
                            sum(1 * ((d_purpose_category %in% pcat_work_school & o_purpose_category != 0) | 
                                       (o_purpose_category %in% pcat_work_school & d_purpose_category != 0))),
                          num_trips_nhbo = 
                            sum(1 * (!d_purpose_category %in% 0:3 & !o_purpose_category %in% 0:3))),
                        .(hh_id, person_id, day_num)]

# some qaqc
trip_counts

trip_counts[, hh_id := as.numeric(hh_id)]
stopifnot(
  merge(fit_dt, trip_counts, by = c('hh_id', 'person_id')
  )[, .N, .(d_num_trips = num_trips, num_trips_check = num_trips_hbw + num_trips_hbo + num_trips_nhbw + num_trips_nhbo)
  ][d_num_trips != num_trips_check, .N] == 0
)

# join trips to days
fit_dt_old = copy(fit_dt)
fit_dt = merge(fit_dt, trip_counts,
               by = c('hh_id', 'person_id', 'day_num'), all.x=TRUE)
fit_dt[is.na(num_trips_hbw), num_trips_hbw := 0]
fit_dt[is.na(num_trips_hbo), num_trips_hbo := 0]
fit_dt[is.na(num_trips_nhbw), num_trips_nhbw := 0]
fit_dt[is.na(num_trips_nhbo), num_trips_nhbo := 0]


# qaqc
stopifnot(
  fit_dt[, .N,
         .(num_trips,
           num_derived_trips = num_trips_hbw +
             num_trips_hbo +
             num_trips_nhbw +
             num_trips_nhbo)][num_trips != num_derived_trips, .N] == 0
)


# filter which data from the person table we want to use
age_adult = 4

person_data = ex_person[
  AGE_CAT >= age_adult, 
  .(hh_id = SAMPN, 
    person_id = PERNO, 
    age = AGE_CAT, 
    gender = SEX, 
    student = STUDE, 
    employment = EMPLY, 
    num_jobs = NJOBS,
    workplace_type = WPLACE, 
    education = EDUC,
    smartphone_type = SMARTPHONE,
    survey_mode = SURVEY_MODE)]

# join person data to days
fit_dt_old = copy(fit_dt)
fit_dt = merge(fit_dt, person_data, by = c('hh_id', 'person_id'), all=FALSE)

# our model is going to weighted by day weights
# let's normalize so the standard errors are meaningful

fit_dt[, estimation_weight := hh_weight  / mean(hh_weight )]

stopifnot(
  round(fit_dt[, sum(estimation_weight)],0) ==nrow(fit_dt)
)

# create some useful binary variables for the model
fit_dt[, online_data := 1 * (survey_mode == 2)]

fit_dt[, age_under_25 := 1 * (age %in% 1:4)]
fit_dt[, age_25_45 := 1 * (age %in% c(5))]
fit_dt[, age_45_65 := 1 * (age %in% c(6))]


fit_dt[, employed := 1 * (employment %in% c(1, 2))] # ft, pt, self-employed, volunteer
fit_dt[, employed_ft := 1 * (employment == 1)]
fit_dt[, employed_pt := 1 * (employment == 2)]
# fit_dt[, employed_self := 1 * (employment == 3)]

fit_dt[, is_student := 1 * (student %in% c(1))]

fit_dt[, is_male := 1 * (gender == 1)]

fit_dt[, income_under_60k := 1 * (income_broad %in% 1:2)]
fit_dt[, income_60k_to_100k := 1 * (income_broad %in% 3)]

fit_dt[, higher_ed := 1 * (education >= 4)]
# fit_dt[, graduate_degree := 1 * (education == 6)]

fit_dt[, two_plus_jobs := 1 * (num_jobs >=2)]
fit_dt[num_jobs <0, two_plus_jobs := 0]

fit_dt[, work_loc_varies := 1 * (workplace_type == 3)]
fit_dt[is.na(work_loc_varies), work_loc_varies := 0]

fit_dt[, has_kids := 1 * (num_kids > 0)]

fit_dt[, sf_home := 1 * (res_type == 1)]

fit_dt[, has_smartphone := 1 * (smartphone_type ==1)]

# Fit models ===================================================================

fmla_rhs =  ~ online_data + 
  age_under_25 + age_25_45 + age_45_65 + 
  employed_ft + employed_pt +
  higher_ed + 
  is_student + 
  work_loc_varies + 
  has_kids + 
  two_plus_jobs + 
  sf_home + 
  has_smartphone


#----------------------------
# home based work model
#----------------------------
model_hbw = glm(
  update(fmla_rhs, num_trips_hbw ~ .),
  data = fit_dt, 
  weights = fit_dt[, estimation_weight],
  family = "poisson")

summary(model_hbw)
broom::tidy(model_hbw)
broom::tidy(model_hbw) %>% write.table('clipboard', sep = '\t', row.names = FALSE)
range(fitted(model_hbw))

# rho-square
# DescTools::PseudoR2(model_hbw)

saveRDS(broom::tidy(model_hbw), file.path(wt_dir, "model_summaries","model_hbw_coef.rds"))
# saveRDS(DescTools::PseudoR2(model_hbw), file.path(wt_dir, "intermediate_files", "model_hbw_fit.rds"))


#--------------------------
# home based other model
#--------------------------
model_hbo = glm(
  update(fmla_rhs, num_trips_hbo ~ .),
  data = fit_dt, 
  weights = fit_dt[, estimation_weight],
  family = "poisson")

summary(model_hbo)
broom::tidy(model_hbo)
broom::tidy(model_hbo) %>% write.table('clipboard', sep = '\t', row.names = FALSE)
range(fitted(model_hbo))

# rho-square
# DescTools::PseudoR2(model_hbo)

saveRDS(broom::tidy(model_hbo), file.path(wt_dir,"model_summaries", "model_hbo_coef.rds"))
# saveRDS(DescTools::PseudoR2(model_hbo), file.path(wt_dir, "intermediate_files", "model_hbo_fit.rds"))


#--------------------------
# not home based work model
#--------------------------
model_nhbw = glm(
  update(fmla_rhs, num_trips_nhbw ~ .),
  data = fit_dt, 
  weights = fit_dt[, estimation_weight],
  family = "poisson")

summary(model_nhbw)
broom::tidy(model_nhbw)
broom::tidy(model_nhbw) %>% write.table('clipboard', sep = '\t', row.names = FALSE)
range(fitted(model_nhbw))

# rho-square
# DescTools::PseudoR2(model_nhbw)

saveRDS(broom::tidy(model_nhbw), file.path(wt_dir,"model_summaries","model_nhbw_coef.rds"))
# saveRDS(DescTools::PseudoR2(model_nhbw), file.path(wt_dir, "intermediate_files", "model_nhbw_fit.rds"))


#--------------------------------
# not home based other
#--------------------------------
model_nhbo = glm(
  update(fmla_rhs, num_trips_nhbo ~ .),
  data = fit_dt, 
  weights = fit_dt[,estimation_weight],
  family = "poisson")

summary(model_nhbo)
broom::tidy(model_nhbo)
broom::tidy(model_nhbo)%>% write.table('clipboard', sep = '\t', row.names = FALSE)
# rho-square
DescTools::PseudoR2(model_nhbo)

saveRDS(broom::tidy(model_nhbo), file.path(wt_dir,"model_summaries", "model_nhbo_coef.rds"))
# saveRDS(DescTools::PseudoR2(model_nhbo), file.path(wt_dir, "intermediate_files", "model_nhbo_fit.rds"))


#-------------------------------------
# Calculate trip factors
#-------------------------------------
fit_dt[,  num_trips_hbw_pred1 := predict(model_hbw, newdata = fit_dt, type = "response")]
fit_dt[,  num_trips_hbo_pred1 := predict(model_hbo, newdata = fit_dt, type = "response")]
fit_dt[, num_trips_nhbw_pred1 := predict(model_nhbw, newdata = fit_dt, type = "response")]
fit_dt[, num_trips_nhbo_pred1 := predict(model_nhbo, newdata = fit_dt, type = "response")]

model_hbw_bias_removed = model_hbw
model_hbo_bias_removed = model_hbo
model_nhbw_bias_removed = model_nhbw
model_nhbo_bias_removed = model_nhbo

# Need to do this for online/loaner/proxied?
model_hbw_bias_removed$coefficients['online_data'] = 0
model_hbo_bias_removed$coefficients['online_data'] = 0
model_nhbw_bias_removed$coefficients['online_data'] = 0
model_nhbo_bias_removed$coefficients['online_data'] = 0

fit_dt[,  num_trips_hbw_pred2 := predict(model_hbw_bias_removed, newdata = fit_dt, type = "response")]
fit_dt[,  num_trips_hbo_pred2 := predict(model_hbo_bias_removed, newdata = fit_dt, type = "response")]
fit_dt[, num_trips_nhbw_pred2 := predict(model_nhbw_bias_removed, newdata = fit_dt, type = "response")]
fit_dt[, num_trips_nhbo_pred2 := predict(model_nhbo_bias_removed, newdata = fit_dt, type = "response")]

fit_dt[, hbw_trip_rate_factor := 1]
fit_dt[, hbo_trip_rate_factor := 1]
fit_dt[, nhbw_trip_rate_factor := 1]
fit_dt[, nhbo_trip_rate_factor := 1]

fit_dt[survey_mode != 1,  hbw_trip_rate_factor := num_trips_hbw_pred2 / num_trips_hbw_pred1]
fit_dt[survey_mode != 1,  hbo_trip_rate_factor := num_trips_hbo_pred2 / num_trips_hbo_pred1]
fit_dt[survey_mode != 1, nhbw_trip_rate_factor := num_trips_nhbw_pred2 / num_trips_nhbw_pred1]
fit_dt[survey_mode != 1, nhbo_trip_rate_factor := num_trips_nhbo_pred2 / num_trips_nhbo_pred1]

# putting some bounds

fit_dt[hbw_trip_rate_factor > 2, hbw_trip_rate_factor := 2]
fit_dt[hbo_trip_rate_factor > 2, hbo_trip_rate_factor := 2]
fit_dt[nhbw_trip_rate_factor > 2, nhbw_trip_rate_factor := 2]
fit_dt[nhbo_trip_rate_factor > 2, nhbo_trip_rate_factor := 2]

fit_dt[hbw_trip_rate_factor < 1, hbw_trip_rate_factor := 1]
fit_dt[hbo_trip_rate_factor < 1, hbo_trip_rate_factor := 1]
fit_dt[nhbw_trip_rate_factor < 1, nhbw_trip_rate_factor := 1]
fit_dt[nhbo_trip_rate_factor < 1, nhbo_trip_rate_factor := 1]


fit_dt[, .N, .(survey_mode, hbw_trip_rate_factor = round(hbw_trip_rate_factor, 3))]
fit_dt[, .N, .(survey_mode, hbo_trip_rate_factor = round(hbo_trip_rate_factor, 3))]
fit_dt[, .N, .(survey_mode, nhbw_trip_rate_factor = round(nhbw_trip_rate_factor, 3))]
fit_dt[, .N, .(survey_mode, nhbo_trip_rate_factor = round(nhbo_trip_rate_factor, 3))]


#-------------------------------------------------------------------
# Join trip rate factors to trip table and calculate trip weights
#-------------------------------------------------------------------

fit_dt = fit_dt[,.(hh_id, 
  person_id, hbw_trip_rate_factor, 
  hbo_trip_rate_factor, nhbw_trip_rate_factor, nhbo_trip_rate_factor)]

# Get trip rate factors into the trip table
trip_data = merge(ex_trip, fit_dt, by.x =c('SAMPN', 'PERNO'), by.y = c('hh_id', 'person_id'), all.x=TRUE)

stopifnot(nrow(ex_trip) == nrow(trip_data))

trip_data[is.na(hbw_trip_rate_factor), hbw_trip_rate_factor := 1]
trip_data[is.na(hbo_trip_rate_factor), hbo_trip_rate_factor := 1]
trip_data[is.na(nhbw_trip_rate_factor), nhbw_trip_rate_factor := 1]
trip_data[is.na(nhbo_trip_rate_factor), nhbo_trip_rate_factor := 1]

trip_data[, d_purpose_category := TPURP]
# apply trip rate factors to day_weight by trip type
trip_data[, trip_rate_factor := case_when(
  # home based work trips
  (d_purpose_category %in% pcat_work_school &
     o_purpose_category == pcat_home) | 
    (o_purpose_category %in% pcat_work_school & 
       d_purpose_category == pcat_home) ~ hbw_trip_rate_factor,
  # home based other trips
  (d_purpose_category == pcat_home & 
     !o_purpose_category %in% pcat_work_school) | 
    (o_purpose_category == pcat_home &
       !d_purpose_category %in% pcat_work_school) ~ hbo_trip_rate_factor,
  # not home based work trips
  (d_purpose_category %in% pcat_work_school & 
     o_purpose_category != pcat_home) |
    (o_purpose_category %in% pcat_work_school & 
       d_purpose_category != pcat_home) ~ nhbw_trip_rate_factor,
  # not home based other trips
  !d_purpose_category %in% c(pcat_home, pcat_work_school) & 
    !o_purpose_category %in% c(pcat_home, pcat_work_school) ~ nhbo_trip_rate_factor)]

# Get day weights into the trip table
trip_data_prev = copy(trip_data)
trip_data = merge(trip_data, hh_weights[, .(hh_id, person_weight)],
                  by.x = 'SAMPN', by.y = 'hh_id')

stopifnot(nrow(ex_trip) == nrow(trip_data))

trip_data[, trip_weight := person_weight * trip_rate_factor]

trip_weights = trip_data[,.(SAMPN, PERNO,  PLANO, person_weight,
                            trip_rate_factor, trip_weight)]
trip_weights = trip_weights[order(SAMPN, PERNO, PLANO)]
trip_weights[is.na(trip_weight), trip_weight := 0]


# Check weight totals
# hh_weight should equal population size in households
# person_weight should equal population size in people
# sum(day_weight) should equal sum(person_weight) = sum(hh_weight * hh_size)
# sum(unadjusted trip_weight) should equal sum(day_weight * num_trips)
# sum(adjusted trip_weight) > sum(unadjusted trip_weight)

person_sum_hh = hh_weights[, sum(hh_weight * hh_size)]
person_sum = person_weights[, sum(person_weight)]


stopifnot(
  person_sum_hh == person_sum
)


# -------------------------------------------------------------------
# Save day-level and trip-level weights
# -------------------------------------------------------------------

message('Writing weight RDS files to ', wt_dir)

saveRDS(hh_weights[, .(sampno = hh_id, hh_weight)],
        file = file.path(wt_dir, 'hh_weights.rds'))

saveRDS(person_weights[, .(sampno = SAMPN, perno = PERNO, person_weight)],
        file = file.path(wt_dir, 'person_weights.rds'))

saveRDS(trip_weights[, .(sampno = SAMPN, perno = PERNO, PLANO, person_weight, trip_rate_factor, trip_weight)],
        file = file.path(wt_dir, "trip_weights.rds"))

message('Finished writing weight RDS files')
