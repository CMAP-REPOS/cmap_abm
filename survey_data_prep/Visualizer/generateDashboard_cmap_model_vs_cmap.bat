:: ############################################################################
:: # Batch file to generate HTML Visualizer
:: #
:: # 1. User should specify the path to the base and build summaries,
:: #    the specified directory should have all the files listed in
:: #    /templates/summaryFilesNames.csv
:: #
:: # 2. User should also specify the name of the base and build scenario if the
:: #    base/build scenario is specified as CHTS, scenario names are replaced
:: #    with appropriate Census sources names wherever applicable
:: ############################################################################
@ECHO off
SET WORKING_DIR= "C:/projects/cmap_activitysim/cmap_abm/survey_data_prep/Visualizer"

:: User Inputs
:: ###########
:: Set up dependencies
:: ###################
SET R_SCRIPT="C:\Program Files\R\R-3.6.2\bin\Rscript"
SET R_LIBRARY=C:\Users\%USERNAME%\r_library
:: Set PANDOC path
SET RSTUDIO_PANDOC=C:\projects\cmap_activitysim\pandoc-2.14.2
:: Parameters file
SET PARAMETERS_FILE=C:/projects/cmap_activitysim/cmap_abm/survey_data_prep/Visualizer\runtime\parameters.csv
SET SETTINGS_FILE=C:/projects/cmap_activitysim/cmap_abm/survey_data_prep\cmap_inputs.yml

SET FULL_HTML_NAME=CMAP_visualizer

SET BASE_SCENARIO_NAME=Survey
SET BUILD_SCENARIO_NAME=ASim
:: for survey base legend names are different [Yes/No]
SET IS_BASE_SURVEY=Yes
SET MAX_ITER=1
SET BASE_SAMPLE_RATE=1.0
SET BUILD_SAMPLE_RATE=0.2761234

SET CT_ZERO_AUTO_FILE_NAME=ct_zero_auto.shp


:: Set paths
:: Commented out -- get from R script that uses settings file instead
:: SET PROJECT_DIR= "N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer"
:: REM SET ABM_DIR=E:\Projects\Clients\SEMCOG\SNABM\2015_TM151_PPA_V1
:: SET ABM_SUMMARY_DIR="N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer\data\calibration_runs\summarized"
:: REM SET BASE_SUMMARY_DIR_SUBSET=E:\Projects\Clients\SEMCOG\Data\CHTS\CHTS_Summaries_TM1format_SN
:: SET BASE_SUMMARY_DIR="N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer\summaries"
:: SET BUILD_SUMMARY_DIR="N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer\data\calibration_runs\summarized"
:: SET CENSUS_DIR="N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer\data\census"
:: SET CENSUS_SUMMARY_DIR="N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer\data\census\summarized"
:: SET CALIBRATION_DIR="N:\Projects\CMAP_Activitysim\cmap_abm_lf\survey_data_prep\Visualizer\data\calibration_targets"
:: SET SHP_FILE_NAME=zones17.shp



ECHO Key,Value > %PARAMETERS_FILE%
REM ECHO WORKING_DIR,%WORKING_DIR% >> %PARAMETERS_FILE%
REM ECHO PROJECT_DIR,%PROJECT_DIR% >> %PARAMETERS_FILE%
REM ECHO ABM_SUMMARY_DIR,%ABM_SUMMARY_DIR% >> %PARAMETERS_FILE%
REM ECHO CALIBRATION_DIR,%CALIBRATION_DIR% >> %PARAMETERS_FILE%
REM ECHO BASE_SUMMARY_DIR,%BASE_SUMMARY_DIR% >> %PARAMETERS_FILE%
REM ECHO BUILD_SUMMARY_DIR,%BUILD_SUMMARY_DIR% >> %PARAMETERS_FILE%
REM ECHO BASE_SUMMARY_DIR_SUBSET,%BASE_SUMMARY_DIR_SUBSET% >> %PARAMETERS_FILE%
ECHO BASE_SCENARIO_NAME,%BASE_SCENARIO_NAME% >> %PARAMETERS_FILE%
ECHO BUILD_SCENARIO_NAME,%BUILD_SCENARIO_NAME% >> %PARAMETERS_FILE%
ECHO BASE_SAMPLE_RATE,%BASE_SAMPLE_RATE% >> %PARAMETERS_FILE%
ECHO BUILD_SAMPLE_RATE,%BUILD_SAMPLE_RATE% >> %PARAMETERS_FILE%
ECHO MAX_ITER,%MAX_ITER% >> %PARAMETERS_FILE%
ECHO R_LIBRARY,%R_LIBRARY% >> %PARAMETERS_FILE%
ECHO RSTUDIO_PANDOC, %RSTUDIO_PANDOC% >> %PARAMETERS_FILE%
REM ECHO SUBSET_HTML_NAME,%SUBSET_HTML_NAME% >> %PARAMETERS_FILE%
ECHO FULL_HTML_NAME,%FULL_HTML_NAME% >> %PARAMETERS_FILE%
REM ECHO SHP_FILE_NAME,%SHP_FILE_NAME% >> %PARAMETERS_FILE%
ECHO CT_ZERO_AUTO_FILE_NAME,%CT_ZERO_AUTO_FILE_NAME% >> %PARAMETERS_FILE%
ECHO IS_BASE_SURVEY,%IS_BASE_SURVEY% >> %PARAMETERS_FILE%

%R_SCRIPT% scripts\settings_to_parameters_csv.R %SETTINGS_FILE%

:: Create calibration output directory
:: ############################################################################
REM mkdir %CALIBRATION_DIR%\
REM mkdir %CALIBRATION_DIR%\ABM_Summaries
REM mkdir %CALIBRATION_DIR%\ABM_Summaries_subset
REM mkdir %ABM_DIR%\main_subset

:::: Call the R Script to generate Jobs vs Workers Summary
:::: #######################################
:::: ECHO %startTime%%Time%: Running R script to generate Jobs vs Workers Summary...
:::: %R_SCRIPT% %WORKING_DIR%\scripts\workersByMAZ.R %PARAMETERS_FILE%


REM :: Call the R script to copy CTRAMP outputs to calibration directory and create subset files
REM :: ############################################################################
REM ECHO %startTime%%Time%: Copy CTRAMP outputs to calibration sub-directory and create subset sample for Solano-Napa Counties...
REM %R_SCRIPT% %WORKING_DIR%\scripts\subset_CTRAMP_outputs.R %PARAMETERS_FILE%
REM IF %ERRORLEVEL% NEQ 0 GOTO MODEL_ERROR
REM
REM :: Call the R script to generate CTRAMP summaries for visualizer
REM :: ############################################################################
REM ECHO %startTime%%Time%: Create CTRAMP summary for full visualizer...
REM SET SWITCH=FULL
REM %R_SCRIPT% %WORKING_DIR%\scripts\SummarizeABM_county_HHwgt.R %PARAMETERS_FILE% %SWITCH%
REM IF %ERRORLEVEL% NEQ 0 GOTO MODEL_ERROR



:: Call the master R script to generate full visualizer
:: #####################################################
ECHO %startTime%%Time%: Running R script to generate visualizer...
SET SWITCH=FULL
%R_SCRIPT% scripts\Master.R %PARAMETERS_FILE% %SWITCH%
IF %ERRORLEVEL% EQU 11 (
   ECHO File missing error. Check error file in outputs.
   EXIT /b %errorlevel%
)
IF %ERRORLEVEL% NEQ 0 GOTO MODEL_ERROR


ECHO %startTime%%Time%: Dashboard creation complete...
GOTO END

:MODEL_ERROR
ECHO Model Failed


:END

