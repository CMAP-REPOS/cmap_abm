:: Run data preparation scripts

SET SETTINGS_FILE=%BASE_PATH%\survey_data_prep\cmap_inputs.yml

@echo off

SET R_SCRIPT="C:\Program Files\R\R-3.6.2\bin\rscript"
SET R_LIBRARY=C:\Users\%USERNAME%\r_library
SETLOCAL EnableDelayedExpansion
SET ANACONDA_DIR=C:\ProgramData\Anaconda3
SET PATH=%ANACONDA_DIR%\Library\bin;%PATH%

:: setup paths to Python application, Conda script, etc.
SET CONDA_ACT=%ANACONDA_DIR%\scripts\activate.bat
SET CONDA_DEA=%ANACONDA_DIR%\Scripts\deactivate.bat

::%R_SCRIPT% Visualizer\scripts\CMAP_RLibInst.R %SETTINGS_FILE%
%R_SCRIPT% data_processing\process_cmap_survey.R %SETTINGS_FILE%

CALL %CONDA_ACT% cmapasim12
python SPA\__init__.py %SETTINGS_FILE%

::Rscript Visualizer\scripts\install_packages.R
%R_SCRIPT% Visualizer\scripts\CMAP_visualizer_prep.R %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\Get_census_data_CMAP.R %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\AutoOwnership_Census_CMAP.R %SETTINGS_FILE%
%R_SCRIPT% Visualizer\scripts\Summarize_ActivitySim_cmap.R %SETTINGS_FILE%

call Visualizer\generateDashboard_cmap_model_vs_cmap.bat
cd .. 
python python\assignment_summary.py
