library(data.table)
library(yaml)
# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')


project_dir = settings$proj_dir
wt_dir = file.path(project_dir, "underreporting_correction")
target_data      = readRDS(file = file.path(wt_dir,"PUMA_2018_targets_day_adjustments.rds"))
target_region      = readRDS(file = file.path(wt_dir,"region_targets.rds"))
p_survey_data  = readRDS(file = file.path(wt_dir,"p_survey_day_adjustments.rds"))
h_survey_data  = readRDS(file = file.path(wt_dir,"hh_survey.rds"))

hhsize = melt(h_survey_data, id.vars = c('sampno' ,'cluster' , 'puma_id', 'initial_expansion_factor', 'h_total', 'n_metra', 'n_pace', 'n_cta_bus', 'n_cta_rail'), measure.vars = c('h_size_1', 'h_size_2', 'h_size_3', 'h_size_4', 'h_size_5'))[value == 1]
hhsize[, h_size := gsub('h_size_', '', variable)]

hhsize[, c('variable', 'value') := NULL]

lifecycle = melt(h_survey_data, id.vars = c('sampno' ), measure.vars = names(h_survey_data)[names(h_survey_data) %like% 'lifecycle'])[value == 1]
lifecycle[, h_lifecycle := gsub('h_lifecycle_', '', variable)]
lifecycle[, c('variable', 'value') := NULL]

workers = melt(h_survey_data, id.vars = c('sampno' ), measure.vars = names(h_survey_data)[names(h_survey_data) %like% 'h_worker'])[value == 1]
workers[, h_worker := gsub('h_worker_', '', variable)]
workers[, c('variable', 'value') := NULL]


autosuff = melt(h_survey_data, id.vars = c('sampno' ), measure.vars = names(h_survey_data)[names(h_survey_data) %like% 'veh'])[value == 1]
autosuff[variable == 'h_zero_veh', h_veh := 'none']
autosuff[variable!= 'h_zero_veh', h_veh := gsub('h_veh_', '', variable)]
autosuff[, c('variable', 'value') := NULL]

setnames(target_data, 'h_zero_veh', 'h_veh_none')


income = melt(h_survey_data, id.vars = c('sampno'), measure.vars = names(h_survey_data)[names(h_survey_data) %like% 'inc'])[value == 1]
income[, h_inc := gsub('h_inc_', '', variable)]
income[, c('variable', 'value') := NULL]

setkey(hhsize, sampno)
h_popsim = Reduce(merge, list(hhsize, lifecycle, workers, autosuff, income))

names(p_survey_data)

age = melt(p_survey_data, id.vars = c('sampno', 'perno', 'cluster' , 'puma_id', 'initial_expansion_factor', 'p_total', 'p_made_mandatory_trips',
                                      'p_made_nonmandatory_only', 'p_made_no_trips', 'p_made_not_applicable'),
           measure.vars = names(p_survey_data)[names(p_survey_data) %like% 'p_age'])[value == 1]
age[, p_age := gsub('p_age_', '', variable)]
age[, c('variable', 'value') := NULL]

commute = melt(p_survey_data, id.vars = c('sampno', 'perno'), measure.vars = names(p_survey_data)[names(p_survey_data) %like% 'p_commute'])[value == 1]
commute[, p_commute := gsub('p_commute_', '', variable)]
commute[, c('variable', 'value') := NULL]


flow = melt(p_survey_data, id.vars = c('sampno', 'perno'), 
            measure.vars = names(p_survey_data)[names(p_survey_data) %in% c('p_to_loop', 'p_from_loop', 'p_intra_district', 'p_d2d_not_loop', 'p_not_worker')])[value == 1]
flow[, p_flow := gsub('p_', '', variable)]
flow[, c('variable', 'value') := NULL]

setnames(target_data, c('p_to_loop', 'p_from_loop', 'p_intra_district', 'p_d2d_not_loop'),
         c('p_flow_to_loop', 'p_flow_from_loop', 'p_flow_intra_district', 'p_flow_d2d_not_loop'))


sex = melt(p_survey_data, id.vars = c('sampno', 'perno'), 
            measure.vars = names(p_survey_data)[names(p_survey_data) %in% c('p_male', 'p_female')])[value == 1]
sex[, p_sex := gsub('p_', '', variable)]
sex[, c('variable', 'value') := NULL]

setkey(age, sampno, perno)
setkey(flow, sampno, perno)
setkey(commute, sampno, perno)
setkey(sex, sampno, perno)
p_popsim = Reduce(merge, list(age, commute,flow, sex))

setnames(h_popsim, 'cluster', 'SUBREGCluster')
setnames(p_popsim, 'cluster', 'SUBREGCluster')
setnames(target_data, 'cluster', 'SUBREGCluster')

h_popsim[, Region := 1]
p_popsim[, Region := 1]
target_region[, Region := 1]
target_data[, Region := 1]


# 
# p_popsim[, p_grouped := .N, .(sampno)]
# p_popsim[, h_total := 1/p_grouped]
# p_popsim[, p_grouped := NULL]

target_region[, h_total := target_data[, sum(h_total)]]
# target_region = target_region[rep(seq_len(nrow(target_region)), each = nrow(target_dt)), ]
popsim_folder = settings$popsim_folder

cols = names(target_data)[!names(target_data) %in% c('Region', 'SUBREGCluster', 'puma_id')]
target_data = target_data[, lapply(.SD, sum), .SDcols = cols, by = .(SUBREGCluster, Region)]
setcolorder(target_data, c('Region', 'SUBREGCluster', 'h_total'))
setcolorder(target_region, c('Region',  'h_total'))
write.csv(h_popsim, file.path(popsim_folder, 'data/seed_households.csv'), row.names = FALSE)
write.csv(p_popsim, file.path(popsim_folder, 'data/seed_persons.csv'), row.names = FALSE)
write.csv(target_data, file.path(popsim_folder, 'data/control_totals_subcluster.csv'), row.names = FALSE)
write.csv(target_region, file.path(popsim_folder, 'data/control_totals_region.csv'), row.names = FALSE)

geo_crosswalk = target_data[, .(Region, SUBREGCluster, SUBREGClusterDummy = SUBREGCluster)]
geo_crosswalk = geo_crosswalk[order(Region, SUBREGCluster)]
write.csv(geo_crosswalk, file.path(popsim_folder, 'data/geo_cross_walk.csv'), row.names = FALSE)
####
# nirpc
####

target_data_nirpc      = readRDS(file = file.path(wt_dir,"PUMA_2018_targets_day_adjustments_nirpc.rds"))

p_survey_data_nirpc  = readRDS(file = file.path(wt_dir,"p_survey_day_adjustments_nirpc.rds"))
h_survey_data_nirpc  = readRDS(file = file.path(wt_dir,"hh_survey_nirpc.rds"))

hhsize = melt(h_survey_data_nirpc,
              id.vars = c('sampno' ,'cluster' , 'puma_id', 'initial_expansion_factor', 'h_total'),
              measure.vars = c('h_size_1', 'h_size_2', 'h_size_3', 'h_size_4'))[value == 1]
hhsize[, h_size := gsub('h_size_', '', variable)]

hhsize[, c('variable', 'value') := NULL]

workers = melt(h_survey_data_nirpc, id.vars = c('sampno' ), 
               measure.vars = names(h_survey_data_nirpc)[names(h_survey_data_nirpc) %like% 'h_worker'])[value == 1]
workers[, h_worker := gsub('h_worker_', '', variable)]
workers[, c('variable', 'value') := NULL]


autosuff = melt(h_survey_data_nirpc, id.vars = c('sampno' ), 
                measure.vars = names(h_survey_data_nirpc)[names(h_survey_data_nirpc) %like% 'veh'])[value == 1]
autosuff[variable == 'h_zero_veh', h_veh := 'none']
autosuff[variable!= 'h_zero_veh', h_veh := gsub('h_veh_', '', variable)]
autosuff[, c('variable', 'value') := NULL]

setnames(target_data_nirpc, 'h_zero_veh', 'h_veh_none')


income = melt(h_survey_data_nirpc, id.vars = c('sampno'),
              measure.vars = names(h_survey_data_nirpc)[names(h_survey_data_nirpc) %like% 'inc'])[value == 1]
income[, h_inc := gsub('h_inc_', '', variable)]
income[, c('variable', 'value') := NULL]

setkey(hhsize, sampno)
h_popsim_nirpc = Reduce(merge, list(hhsize, workers, autosuff, income))

names(p_survey_data_nirpc)

age = melt(p_survey_data_nirpc, id.vars = c('sampno', 'perno', 'cluster' , 'puma_id', 'initial_expansion_factor', 'p_total', 'p_made_mandatory_trips',
                                      'p_made_nonmandatory_only', 'p_made_no_trips', 'p_made_not_applicable',
                                      'p_race_white', 'p_race_afam', 'p_race_other'),
           measure.vars = names(p_survey_data_nirpc)[names(p_survey_data_nirpc) %like% 'p_age'])[value == 1]
age[, p_age := gsub('p_age_', '', variable)]
age[, c('variable', 'value') := NULL]

commute = melt(p_survey_data_nirpc, id.vars = c('sampno', 'perno'), 
               measure.vars = names(p_survey_data_nirpc)[names(p_survey_data_nirpc) %like% 'p_commute'])[value == 1]
commute[, p_commute := gsub('p_commute_', '', variable)]
commute[, c('variable', 'value') := NULL]


sex = melt(p_survey_data_nirpc, id.vars = c('sampno', 'perno'), 
           measure.vars = names(p_survey_data_nirpc)[names(p_survey_data_nirpc) %in% c('p_male', 'p_female')])[value == 1]
sex[, p_sex := gsub('p_', '', variable)]
sex[, c('variable', 'value') := NULL]


setkey(age, sampno, perno)
setkey(commute, sampno, perno)
setkey(sex, sampno, perno)
p_popsim_nirpc = Reduce(merge, list(age, commute, sex))

setnames(h_popsim_nirpc, 'cluster', 'SUBREGCluster')
setnames(p_popsim_nirpc, 'cluster', 'SUBREGCluster')
setnames(target_data_nirpc, 'cluster', 'SUBREGCluster')

h_popsim_nirpc[, Region := 1]
p_popsim_nirpc[, Region := 1]
target_data_nirpc[, Region := 1]
# 
# p_popsim[, p_grouped := .N, .(sampno)]
# p_popsim[, h_total := 1/p_grouped]
# p_popsim[, p_grouped := NULL]

# target_region = target_region[rep(seq_len(nrow(target_region)), each = nrow(target_dt)), ]
popsim_folder_nirpc = settings$popsim_folder_nirpc

cols = names(target_data_nirpc)[!names(target_data_nirpc) %in% c('Region', 'SUBREGCluster', 'puma_id')]
target_data_nirpc = target_data_nirpc[, lapply(.SD, sum), .SDcols = cols, by = .(SUBREGCluster, Region)]
setcolorder(target_data_nirpc, c('Region', 'SUBREGCluster', 'h_total'))
write.csv(h_popsim_nirpc, file.path(popsim_folder_nirpc, 'data/seed_households.csv'), row.names = FALSE)
write.csv(p_popsim_nirpc, file.path(popsim_folder_nirpc, 'data/seed_persons.csv'), row.names = FALSE)
write.csv(target_data_nirpc, file.path(popsim_folder_nirpc, 'data/control_totals_subcluster.csv'), row.names = FALSE)

geo_crosswalk = target_data_nirpc[, .(Region, SUBREGCluster, SUBREGClusterDummy = SUBREGCluster)]
geo_crosswalk = geo_crosswalk[order(Region, SUBREGCluster)]
write.csv(geo_crosswalk, file.path(popsim_folder_nirpc, 'data/geo_cross_walk.csv'), row.names = FALSE)


