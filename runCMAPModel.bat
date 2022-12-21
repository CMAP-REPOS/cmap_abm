:: This is the entire CMAP model runner script.
::
:: Ted Lin, RSG

:: READ FIRST: cmap_abm\README.md

@ECHO OFF
set SCEN_NAME=%1
:: Scenario Paths - These define the inputs and outputs
SET BASE_PATH=C:\projects\cmap_activitysim\cmap_abm
SET MODEL_PARAMETERS=%BASE_PATH%\model_params.yaml

SET PROJECT=%BASE_PATH%\emme\CMAP-ABM\CMAP-ABM.emp
SET TWOZONE_INPUT=%BASE_PATH%\twozone_inputs
SET EMME_INPUT=%BASE_PATH%\emme\emme_inputs
SET EMME_OUTPUT=%BASE_PATH%\emme\emme_outputs
SET ASIM=%BASE_PATH%\activitysim
SET ASIM_INPUT=%BASE_PATH%\model_runs\%SCEN_NAME%\inputs
SET ASIM_OUTPUT=%BASE_PATH%\model_runs\%SCEN_NAME%\outputs
MD %ASIM_OUTPUT%

:: Python paths - EMMEPY should point to the Emme installation
SET EMMEPY="C:\Program Files\INRO\Emme\Emme 4\Emme-4.6.1\Python37\python.exe"
SET PYTHONPATH="C:\Program Files\INRO\Emme\Emme 4\Emme-4.6.1\Python37\Lib"
::SET EMMEPY="C:\Program Files\INRO\Emme\Emme 4\Emme-4.4.5.1\Python27\python.exe"
::SET PYTHONPATH="C:\Program Files\INRO\Emme\Emme 4\Emme-4.4.5.1\Python27\Lib"

:: Prepare EMMEBank - import network; create and import warmstart matrices
%EMMEPY% %BASE_PATH%\python\import\importScenarios.py
%EMMEPY% %BASE_PATH%\python\import\importScalars.py
%EMMEPY% %BASE_PATH%\python\import\importwsmatrices.py
%EMMEPY% %BASE_PATH%\python\import\importTransitConnectors.py

:: Create an OMX stating work county for each work zone
::%EMMEPY% %BASE_PATH%\python\County_script.py

:: Iteration 0
:: Skim highway and transit for ActivitySim
%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 0
%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py 0

:: Iteration 1
:: Run ActivitySim
CALL runAsim.bat 1
copy %ASIM_INPUT%\taz_skims.omx %ASIM_INPUT%\taz_skims_iter0.omx
:: Run Emme Assignment
%EMMEPY% %BASE_PATH%\python\import\importMatrices.py 1
%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 1
%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py 1

:: Iteration 2
:: Run ActivitySim
CALL runAsim.bat 2
copy %ASIM_INPUT%\taz_skims.omx %ASIM_INPUT%\taz_skims_iter1.omx
:: Run Emme Assignment
%EMMEPY% %BASE_PATH%\python\import\importMatrices.py 2
%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 2
%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py 2

:: OUTPUTS

::#TODO: copy outputs to destination folder

::#TODO: run visualizer, send to output folder
cd survey_data_prep
CALL run_data_prep_scripts.bat
cd .. 
:: export skims maps from Emme
::%EMMEPY% %BASE_PATH%\python\export_trn_los_maps.py

::#TODO: cleanup
