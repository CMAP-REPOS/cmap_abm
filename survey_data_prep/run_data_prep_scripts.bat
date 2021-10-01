:: Run data preparation scripts

SET SETTINGS_FILE="C:\projects\cmap_activitysim\cmap_abm\survey_data_prep\cmap_inputs.yml"

@echo off

SET R_SCRIPT="C:\Program Files\R\R-3.6.2\bin\rscript"
SET R_LIBRARY=C:\Users\andrew.rohne\r_library

::%R_SCRIPT% Visualizer\scripts\CMAP_RLibInst.R %SETTINGS_FILE%

::Rscript data_processing\process_cmap_survey.R %SETTINGS_FILE%
::python SPA\__init__.py %SETTINGS_FILE%

::Rscript Visualizer\scripts\install_packages.R
::%R_SCRIPT% Visualizer\scripts\CMAP_visualizer_prep.R %SETTINGS_FILE%
::Rscript Visualizer\scripts\Get_census_data_CMAP %SETTINGS_FILE%
::Rscript Visualizer\scripts\AutoOwnership_Census_CMAP %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\Summarize_ActivitySim_cmap.R %SETTINGS_FILE%

call Visualizer\generateDashboard_cmap_model_vs_cmap.bat
cmd /k 
:: Then edit/run the visualizer .bat