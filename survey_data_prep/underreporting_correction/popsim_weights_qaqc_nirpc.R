library(data.table)
library(yaml)
# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')
popsim_folder = settings$popsim_folder_nirpc

project_dir = settings$proj_dir
wt_dir = file.path(project_dir, "underreporting_correction")
h_survey_data  = readRDS(file = file.path(wt_dir,"hh_survey_nirpc.rds"))
p_survey_data  = readRDS(file = file.path(wt_dir,"p_survey_day_adjustments_nirpc.rds"))
h_initial  = fread(file = file.path(settings$data_dir,settings$nirpc_folder, 'weights', 'ipf', 'household_weights.csv'))
target_data      = readRDS(file = file.path(wt_dir,"PUMA_2018_targets_day_adjustments_nirpc.rds"))

popsim_weights = fread(file.path(popsim_folder, 'output', 'final_summary_hh_weights.csv'))

names(popsim_weights) = c('sampno', 'final_weight')

library(ggplot2)

weights = merge(popsim_weights, h_survey_data, by = 'sampno')
weights = merge(weights, h_initial[, .(sampno, wthhfin)], by = 'sampno')
p_weights = merge(popsim_weights, p_survey_data, by = c('sampno'))
p_weights = merge(p_weights, h_initial[, .(sampno, wthhfin)], on = 'sampno')

ggplot(data = weights, aes(x = final_weight, y= wthhfin, color = as.factor(cluster))) + geom_point() + 
  xlab('PopSim Weight') + ylab('Previous CMAP Weight')
ggplot(data = weights, aes(x = final_weight, y= initial_expansion_factor, color = as.factor(cluster))) + geom_point() + 
  xlab('PopSim Weight') + ylab('Initial expansion factor')

ggplot(data = weights, aes(x = wthhfin, y= initial_expansion_factor, color = as.factor(cluster))) + geom_point() + 
  xlab('Previous CMAP Weight') + ylab('Initial expansion factor')


sum(weights$wthhfin)
sum(weights$final_weight)
cor(weights$wthhfin, weights$final_weight)
summary(lm(weights$final_weight ~ weights$wthhfin))
# histogram of ratio of popsim/initial weight

ggplot(data = weights, aes(x = final_weight/initial_expansion_factor)) + geom_histogram() + 
  xlab('Ratio of PopSim Weight:Initial expansion factor')

ggplot(data = weights, aes(x = wthhfin/initial_expansion_factor)) + geom_histogram() + 
  xlab('Ratio of Previous Weight:Initial expansion factor') + xlim(0, 3.5)

# Compare totals

library(magrittr)
colnames_hh = names(weights)[names(weights) %like% 'h_' | names(weights) %like% '^n_']
summary_hh_weights = t(weights[, lapply(.SD, function(x) sum(x * final_weight)), .SDcols = colnames_hh])

colnames_per = names(target_data)[names(target_data) %like% 'p_'& names(target_data) %in% names(p_weights)]
summary_per_weights = t(p_weights[, lapply(.SD, function(x) sum(x * final_weight)), .SDcols = colnames_per])

summary_weights = rbind(summary_hh_weights, summary_per_weights)
colorder = c('h_total',
             colnames_hh[colnames_hh %like% 'h_size'],
             colnames_hh[colnames_hh %like% 'h_worker'],
             colnames_hh[colnames_hh %like% 'veh'],
             colnames_hh[colnames_hh %like% 'inc'],
             colnames_per[colnames_per %like% 'male'],
             colnames_per[colnames_per %like% 'age'],
             #colnames_hh[colnames_hh %like% 'lifecycle'],
             #colnames_per[colnames_per %like% 'loop' | colnames_per == 'p_not_worker' | colnames_per == 'p_intra_district'],
             #colnames_hh[colnames_hh %like% 'n_'],
             # colnames_per[colnames_per %like% 'commute'],
             colnames_per[colnames_per %like% 'p_made']
             )
summary_weights = as.data.table(summary_weights[match(colorder, rownames(summary_weights)),], keep.rownames = TRUE)

setnames(target_data,
         c('p_flow_to_loop', 'p_flow_from_loop', 'p_flow_intra_district', 'p_flow_d2d_not_loop'),
         c('p_to_loop', 'p_from_loop', 'p_intra_district', 'p_d2d_not_loop'), skip_absent = TRUE)

setnames(target_data,  'h_veh_none','h_zero_veh', skip_absent = TRUE)

summary_targets = t(target_data[, lapply(.SD, function(x) sum(x)), .SDcols = c(colnames_per, colnames_hh[colnames_hh %in% names(target_data)])])

summary_targets = rbind(summary_targets, t(target_region))
summary_targets = as.data.table(summary_targets[match(colorder, rownames(summary_targets)),], keep.rownames = TRUE)
summary_targets =summary_targets[!is.na(V1)]

summary = summary_weights[summary_targets, on = .(V1)]
summary[, pct_diff := round((V2 - i.V2)/V2 * 100, 2)]


write.table(summary, 'clipboard', row.names = F, sep = '\t')

