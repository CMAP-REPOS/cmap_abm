:: Run data preparation scripts

SET SETTINGS_FILE="D:\Projects\Clients\CMAP\Tasks\asim_setup\cmap_abm\survey_data_prep\cmap_inputs.yml"

@echo off

SET R_SCRIPT="C:\Program Files\R\R-4.0.3\bin\x64\Rscript"
SET R_LIBRARY=C:\Users\andrew.rohne\r_library

::%R_SCRIPT% Visualizer\scripts\CMAP_RLibInst.R %SETTINGS_FILE%

::Rscript data_processing\process_cmap_survey.R %SETTINGS_FILE%
::python SPA\__init__.py %SETTINGS_FILE%

::Rscript Visualizer\scripts\install_packages.R
::%R_SCRIPT% Visualizer\scripts\CMAP_visualizer_prep.R %SETTINGS_FILE%
::Rscript Visualizer\scripts\Get_census_data_CMAP %SETTINGS_FILE%
::Rscript Visualizer\scripts\AutoOwnership_Census_CMAP %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\Summarize_ActivitySim_cmap.R %SETTINGS_FILE%


cmd /k 
:: Then edit/run the visualizer .bat