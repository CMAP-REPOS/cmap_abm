# translate settings to parameters file so that we can avoid re-stating paths in visualizer batch file

library(yaml)

args = commandArgs(trailingOnly = TRUE)

if(length(args) > 0){
  settings_file = args[1]
} else {
  settings_file = 'N:/Projects/CMAP_Activitysim/cmap_abm_lf/survey_data_prep/cmap_inputs.yml'
}

print(paste("Using Settings file at", settings_file))

settings = yaml.load_file(settings_file)

parameters_path = file.path(settings$visualizer_dir, 'runtime', 'parameters.csv')

parameters = read.csv(parameters_path)


working_dir = data.frame(Key = 'WORKING_DIR', Value = settings$visualizer_dir)
project_dir = data.frame(Key = 'PROJECT_DIR', Value = settings$visualizer_dir)
abm_summaries_dir = data.frame(Key = 'ABM_SUMMARY_DIR', Value = settings$abm_summaries_dir)
calib_dir = data.frame(Key = 'CALIBRATION_DIR', Value = file.path(settings$visualizer_dir, 'calibration_targets'))
base_summary_dir = data.frame(Key = 'BASE_SUMMARY_DIR', Value = file.path(settings$visualizer_dir, 'summaries'))
build_summary_dir = data.frame(Key = 'BUILD_SUMMARY_DIR', Value = file.path(settings$abm_summaries_dir))
shp_file_name = data.frame(Key = 'SHP_FILE_NAME', Value = file.path(settings$zone_shp_file))

parameters = rbind(parameters, working_dir, project_dir, abm_summaries_dir, calib_dir, base_summary_dir,
                   build_summary_dir, shp_file_name)

write.csv(parameters, parameters_path, row.names = FALSE)
