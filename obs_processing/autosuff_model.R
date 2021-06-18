# Estimate model to use for metra auto sufficiency

library(data.table)
library(dplyr)
library(stringr)
library(yaml)
library(readxl)
library(nnet)
# load settings


settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')

# working directory
# Paths and other constants
project_dir = settings$proj_dir
data_dir = file.path(settings$SPA_input_dir)
wt_dir = file.path(project_dir, 'underreporting_correction')

cmap_weights = fread(file.path(settings$popsim_folder, 'output/final_summary_hh_weights.csv'))

nirpc_hh_weights = fread(file.path(settings$data_dir, settings$nirpc_folder, 'weights', 'ipf', 'household_weights.csv'))
nirpc_per_weights = fread(file.path(settings$data_dir, settings$nirpc_folder, 'weights', 'ipf', 'person_weights.csv'))

# read in necessary data

hh     = fread(file.path(data_dir, 'HH_SPA_INPUT.csv'))
person = fread(file.path(data_dir, 'PER_SPA_INPUT.csv'))
trip   = fread(file.path(data_dir, 'PLACE_SPA_INPUT.csv'))
metra_data = read_xlsx(file.path(data_dir, 'Metra_2019/2019_Origin-Destination_survey_package/2019_OriginDestinationSurvey_V2_send.xlsx'))
setDT(metra_data)
# some pre processing

hh[trip, has_pnr := ifelse(i.MODE == 8, 1, 0), on = .(SAMPN)]
hh[, autosuff := fcase(HH_VEH_CAT == 0, 0,
                       HH_VEH_CAT < HH_WORKERS_CAT, 1,
                       HH_VEH_CAT >= HH_WORKERS_CAT, 2)]

imputed_income = readRDS(file.path(wt_dir, 'imputed_income.rds'))
hh[, HH_INC_CAT_orig := HH_INC_CAT]
hh[imputed_income, HH_INC_CAT := ifelse(HH_INC_CAT_orig < 0, income_imputed_aggregate, HH_INC_CAT_orig), on = .(SAMPN = sampno)]

# person = person[hh[, .(HH_SIZE_CAT, HH_WORKERS_CAT, HH_DRIVERS, HHEXPFAC, HH_INC_CAT, SAMPN, autosuff)], on = .(SAMPN)]
# label
hh[, hh_inc_fac := factor(HH_INC_CAT, levels = c(1:5), labels = c('0-30k', '30-60k', '60-100k', '100-150k', '150kp'))]
hh[SAMPN %in% cmap_weights$sampno, study_cmap := 1]
hh[, hh_workers_fac := as.factor(HH_WORKERS_CAT)]
hh[, hh_drivers_fac := as.factor(HH_DRIVERS)]
hh[, hh_size_fac := as.factor(HH_SIZE_CAT)]


hh_cmap = hh[study_cmap == 1]

model_rhs = 
  ~ hh_inc_fac + 
  hh_size_fac +
  hh_workers_fac +
  has_pnr
  
hh_cmap[, weight_small := HHEXPFAC/hh_cmap[, mean(HHEXPFAC)]]

set.seed(413)
model = multinom(
  update(model_rhs, autosuff ~ .),
  data = hh_cmap,
  weights = hh_cmap[,weight_small],
  model = TRUE, Hess = TRUE)

summary(model)

options(scipen = 999)
tidy.opts = options(scipen = 99)
print(broom::tidy(model, exponentiate = FALSE), n = 400)

DescTools::PseudoR2(model) 

# fit model over metra data

metra_data[, HH_People_orig := HH_People]
metra_data[, hh_inc_cat := fcase(HH_Income_Code %in% c(1,2, 3), 1,
                                 HH_Income_Code %in% c(4), 2,
                                 HH_Income_Code %in% c(5,6), 3,
                                 HH_Income_Code %in% c(7, 8), 4,
                                 HH_Income_Code %in% c(11:13), -9,
                                 default = 5)]

# simple hh inc imputation
inc_dists = metra_data[hh_inc_cat >0, .(prob = .N/metra_data[hh_inc_cat >0, .N]), hh_inc_cat][order(hh_inc_cat)]
inc_dists[, prob_cumul := cumsum(prob)]
inc_dists[, prev_prob := shift(prob_cumul, type = 'lag')]
inc_dists[is.na(prev_prob), prev_prob := 0]

metra_data[hh_inc_cat <0, irand0_1 := runif(.N, 0, 1)]
metra_data[, rand2 := irand0_1]
setkey(metra_data,  irand0_1, rand2)
setkey(inc_dists,  prev_prob, prob_cumul)
imputed_inc = foverlaps(metra_data[hh_inc_cat <0], inc_dists, by.x = c('irand0_1', 'rand2'), by.y = c('prev_prob', 'prob_cumul'))
imputed_inc[, i.hh_inc_cat := NULL]
metra_data[imputed_inc, hh_inc_cat := ifelse(hh_inc_cat <0, i.hh_inc_cat, hh_inc_cat), on = 'Serial_ID']

# simple hh workers imputation
metra_data[, hh_workers_fac := factor(ifelse(HH_Employed >=3, 3, HH_Employed))]

wkr_dists = metra_data[!is.na(hh_workers_fac), .(prob = .N/metra_data[!is.na(hh_workers_fac), .N]), 
                       hh_workers_fac][order(hh_workers_fac)]
wkr_dists[, prob_cumul := cumsum(prob)]
wkr_dists[, prev_prob := shift(prob_cumul, type = 'lag')]
wkr_dists[is.na(prev_prob), prev_prob := 0]

set.seed(412)
metra_data[is.na(hh_workers_fac), irand0_1 := runif(.N, 0, 1)]
metra_data[, rand2 := irand0_1]
setkey(metra_data,  irand0_1, rand2)
setkey(wkr_dists,  prev_prob, prob_cumul)
imputed_wkr = foverlaps(metra_data[is.na(hh_workers_fac)], wkr_dists, by.x = c('irand0_1', 'rand2'), by.y = c('prev_prob', 'prob_cumul'))
imputed_wkr[, i.hh_workers_fac := NULL]
metra_data[imputed_wkr, hh_workers_cat := i.hh_workers_fac, on = 'Serial_ID']
metra_data[, hh_workers_cat := as.factor(hh_workers_cat)]
metra_data[, hh_workers_fac := fcoalesce(hh_workers_fac, hh_workers_cat)]
metra_data[as.numeric(hh_workers_fac) > HH_People & !is.na(HH_People), hh_workers_fac := factor(HH_People)]

metra_data[, hh_inc_fac := factor(hh_inc_cat, levels = c(1:5), labels = c('0-30k', '30-60k', '60-100k', '100-150k', '150kp'))]

metra_data[is.na(HH_People) | HH_People == 0, HH_People := as.numeric(hh_workers_fac) + fcoalesce(HH_Children, 0)]
metra_data[HH_People == 0, HH_People := 1]
metra_data[, hh_size_fac := factor(ifelse(HH_People >=5, 5, HH_People))]

metra_data[, has_pnr := ifelse(Mode_Access_Code %in% c(2, 4) | Mode_Egress_Code %in% c(11, 12), 1, 0)]

metra_data[, model_predict_noveh := predict(model, newdata = metra_data, type = "probs")[, '0']]
metra_data[, model_predict_vehltwrk := predict(model, newdata = metra_data, type = "probs")[, '1']]
metra_data[, model_predict_vehgtewrk := predict(model, newdata = metra_data, type = "probs")[, '2']]

set.seed(411)
metra_data[, auto_rand := runif(.N, 0, 1)]

metra_data[, autosuff := fcase(auto_rand <= model_predict_noveh, 0,
                               auto_rand > model_predict_noveh & auto_rand <= model_predict_noveh + model_predict_vehltwrk, 1,
                               auto_rand > model_predict_noveh + model_predict_vehltwrk, 2)]

metra_data[, HH_People := HH_People_orig]
metra_data[, c('HH_People_orig', 'auto_rand', 'hh_workers_cat', 
               'irand0_1', 'rand2'):= NULL]

write.csv(metra_data, file.path(settings$data_dir, 'Metra_2019/2019_Origin-Destination_survey_package/metra_with_autosuff.csv'), row.names = FALSE)


