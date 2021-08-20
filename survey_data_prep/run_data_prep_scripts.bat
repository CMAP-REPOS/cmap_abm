:: Run data preparation scripts

SET SETTINGS_FILE= "N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\cmap_inputs.yml"

@echo off
::Rscript data_processing\process_cmap_survey.R %SETTINGS_FILE%
::python SPA\__init__.py %SETTINGS_FILE%

::Rscript Visualizer\scripts\install_packages.R
::Rscript Visualizer\scripts\CMAP_visualizer_prep.R %SETTINGS_FILE%
::Rscript Visualizer\scripts\Get_census_data_CMAP %SETTINGS_FILE%
::Rscript Visualizer\scripts\AutoOwnership_Census_CMAP %SETTINGS_FILE%
::Rscript Visualizer\scripts\Summarize_ActivitySim_cmap %SETTINGS_FILE%


cmd /k 
:: Then edit/run the visualizer .bat