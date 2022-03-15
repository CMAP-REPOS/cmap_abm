:: This is the entire CMAP model runner script. 
:: 
:: Andrew Rohne, RSG

:: READ FIRST: <<readme path>> #TODO

@ECHO OFF

::
:: Scenario Paths - These define the inputs and outputs
SET BASE_PATH=C:\projects\cmap_activitysim\cmap_abm
SET MODEL_PARAMETERS=%BASE_PATH%\model_params.yaml
                
SET EMMEBANK=C:\projects\cmap_activitysim\cmap_abm\CMAP-ABM\CMAP-ABM.emp
SET INPUT_FOLDER=%BASE_PATH%\inputs
SET WARM_START=%BASE_PATH%\emme_inputs
SET SCENARIO_OUTPUT=%BASE_PATH%\outputs
SET LOG_OUT=%BASE_PATH%\Reports
MD %SCENARIO_OUTPUT%
MD %SCENARIO_OUTPUT%\activitysim_outputs
MD %SCENARIO_OUTPUT%\activitysim_logs


:: Python paths - EMMEPY should point to the Emme installation, ANACONDA should point to the 
:: Anaconda environment; both should point directly to the Python executable.

SET EMMEPY="C:\Program Files\INRO\Emme\Emme 4\Emme-4.4.2\Python27\python.exe"
SET ANACONDA=C:\Users\%USERNAME%\.conda\envs\cmapasim\python.exe


SET ASIM=%BASE_PATH%\activitysim
SET ASIM_INPUTS=%ASIM%\data

::#TODO: check output folder, delete all prior outputs
:: Removing old outputs
::DEL /y %ASIM%\output\*
::DEL /y %SCENARIO_OUTPUT%
::DEL /y %LOG_OUT%\*

::#TODO: initialize EMME, connect to EMMEBANK

::
:: Prepare Model
::

:: Prepare EMMEBank - rewrites warm-start matrices, removes skims
::%EMMEPY% %BASE_PATH%\python\import\importScenarios.py :: TODO: REMOVE BEFORE FLIGHT
::%EMMEPY% %BASE_PATH%\python\import\importScalars.py :: TODO: REMOVE BEFORE FLIGHT
::%EMMEPY% %BASE_PATH%\python\import\importwsmatrices.py :: TODO: REMOVE BEFORE FLIGHT

:: This is Ted's
::%EMMEPY% %BASE_PATH%\python\add_acc_egr_links.py

::#TODO: assign/skim HNET
::%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py :: TODO: REMOVE BEFORE FLIGHT
 
::%EMMEPY% %BASE_PATH%\python\copyAutoTime.py

::#TODO: assign/skim TNET
%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py

::#TODO: output skim matrices to ActivitySim folder

::
:: Prepare files for ActivitySim and run ActivitySim
::
::%ANACONDA% python\prepAsim.py
::%ANACONDA% %ASIM%\simulation.py -c %ASIM%\configs_test -c %ASIM%\configs_3_zone -o %ASIM%\output -d %ASIM%\data


::#TODO: assign HNET

::#TODO: assign TNET

::#TODO: check feedback loop, determine if to run again

::
:: OUTPUTS
::

::#TODO: Uncomment next four before flight, Remove >nul before flight next three lines
::ECHO Copying ActivitySim files to output folder... 
::COPY %ASIM%\output\*.omx %SCENARIO_OUTPUT%\activitysim_outputs > nul 
::COPY %ASIM%\output\final*.csv %SCENARIO_OUTPUT%\activitysim_outputs > nul
::COPY %ASIM%\output\*activitysim.log %SCENARIO_OUTPUT%\activitysim_logs > nul

::#TODO: copy outputs to destination folder

::#TODO: run visualizer, send to output folder

::#TODO: cleanup