:: Run data preparation scripts

SET SETTINGS_FILE="C:\projects\cmap_activitysim\cmap_abm\survey_data_prep\cmap_inputs.yml"

@echo off

SET R_SCRIPT="C:\Program Files\R\R-3.6.2\bin\rscript"
SET R_LIBRARY=C:\Users\%USERNAME%\r_library
SETLOCAL EnableDelayedExpansion
SET ANACONDA=C:\Users\%USERNAME%\.conda\envs\cmapasim\python.exe
SET ANACONDA_DIR=C:\ProgramData\Anaconda3
SET PATH=%ANACONDA_DIR%\Library\bin;%PATH%
::ECHO %PATH%
SET PYTHONPATH="C:\Users\%USERNAME%\.conda\envs\cmapasim\lib"
:: setup paths to Python application, Conda script, etc.
SET CONDA_ACT=%ANACONDA_DIR%\scripts\activate.bat
SET CONDA_DEA=%ANACONDA_DIR%\Scripts\deactivate.bat

::%R_SCRIPT% Visualizer\scripts\CMAP_RLibInst.R %SETTINGS_FILE%
%R_SCRIPT% data_processing\process_cmap_survey.R %SETTINGS_FILE%

CALL %CONDA_ACT% cmapasim
%ANACONDA% SPA\__init__.py %SETTINGS_FILE%

::Rscript Visualizer\scripts\install_packages.R
%R_SCRIPT% Visualizer\scripts\CMAP_visualizer_prep.R %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\Get_census_data_CMAP.R %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\AutoOwnership_Census_CMAP.R %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\Summarize_ActivitySim_cmap.R %SETTINGS_FILE%

call Visualizer\generateDashboard_cmap_model_vs_cmap.bat
cmd /k 
:: Then edit/run the visualizer .bat