:: This is the entire CMAP model runner script. 
:: 
:: Ted Lin, RSG

:: READ FIRST: <<readme path>> #TODO

@ECHO OFF

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
SET EMMEPY="C:\Program Files\INRO\Emme\Emme 4\Emme-4.4.5.1\Python27\python.exe"
SET PYTHONPATH="C:\Program Files\INRO\Emme\Emme 4\Emme-4.4.5.1\Python27\lib"

SET ASIM=%BASE_PATH%\activitysim
SET ASIM_INPUTS=%ASIM%\data
::#TODO: check output folder, delete all prior outputs
:: Removing old outputs
::DEL /y %SCENARIO_OUTPUT%
::DEL /y %LOG_OUT%\*

:: Prepare EMMEBank - import network; create and import warmstart matrices
::%EMMEPY% %BASE_PATH%\python\import\importScenarios.py
::%EMMEPY% %BASE_PATH%\python\import\importScalars.py
::%EMMEPY% %BASE_PATH%\python\import\importwsmatrices.py
::%EMMEPY% %BASE_PATH%\python\add_acc_egr_links.py

::%EMMEPY% %BASE_PATH%\python\rename_omx.py :: to be removed

:: Iteration 0
:: Skim highway and transit for ActivitySim
::%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 0
::%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py

:: Iteration 1
:: Run ActivitySim
runAsim.bat
:: Run Emme Assignment
::%EMMEPY% %BASE_PATH%\python\import\importMatrices.py
::%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 1
::%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py

:: Iteration 2
:: Run ActivitySim
::runAsim.bat
:: Run Emme Assignment
::%EMMEPY% %BASE_PATH%\python\import\importMatrices.py
::%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 2
::%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py

:: Iteration 3
:: Run ActivitySim
::runAsim.bat
:: Run Emme Assignment
::%EMMEPY% %BASE_PATH%\python\import\importMatrices.py
::%EMMEPY% %BASE_PATH%\python\cmap_assignment_runner.py 3
::%EMMEPY% %BASE_PATH%\python\cmap_transit_assignment_runner.py

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