:: runModel.bat
:: Written by BTS, 06/24/13
:: Modified by NMP, 04/14/16 - run accessibilities after skimming
:: ----------------------------------------------------------------------------
:: Run the CMAP ABM from start to finish.
:: Call from command line, e.g "runModel.bat > blog.txt"

:: Parameters
SET projDir=Y:/{{{TEMPLATE}}}/cmap_abm
SET sampleRate=0.50

:: Set Python paths
CALL EmmeConfig.bat
@ECHO on
SET emmepy="%EMMEPATH%\Python27\python.exe"
SET py64="C:\Python27\python.exe"

:: Initialize timestamp log
if exist model_run_timestamp.txt (del model_run_timestamp.txt /Q)
@ECHO ====================================================== >> model_run_timestamp.txt
@ECHO BEGIN CMAP REGIONAL MODEL RUN - SAMPLE RATE %sampleRate% >> model_run_timestamp.txt
@ECHO Model Run Start Time: %date% %time% >> model_run_timestamp.txt
@ECHO ====================================================== >> model_run_timestamp.txt

:: Create MAZ skims
@ECHO Create MAZ skims: %date% %time% >> model_run_timestamp.txt
%py64% runMAZSkimsInitial.py
%py64% scripts/SPwrapper.py
%py64% scripts/cmapPostProcess.py

:: Create networks and skim
@ECHO Build networks: %date% %time% >> model_run_timestamp.txt
%emmepy% runBuildNetworks.py 1,1,1,1,1,1,1,1
@ECHO Run initial skims: %date% %time% >> model_run_timestamp.txt
%emmepy% runInitialSkims.py 1,1,1,1,1,1,1,1

:: Run accessibilities script
@ECHO Calculate accessibilities: %date% %time% >> model_run_timestamp.txt
cd accessibilities
CALL runAccessibilities.bat
copy outputs\accessibility_maz.csv ..\inputs
cd ..

:: Create TAP lines file and run CT-RAMP
@ECHO Create TAP lines: %date% %time% >> model_run_timestamp.txt
%emmepy% runTapLines.py
@ECHO Run CT-RAMP: %date% %time% >> model_run_timestamp.txt
CALL runCTRAMP-SingleProcess.bat %sampleRate%

:: Final skimming and assignments
@ECHO Run final assignments: %date% %time% >> model_run_timestamp.txt
%emmepy% runFinalAssignments.py 1,1,1,1,1,1,1,1

@ECHO ====================================================== >> model_run_timestamp.txt
@ECHO END CMAP REGIONAL MODEL RUN >> model_run_timestamp.txt
@ECHO Model Run End Time: %date% %time% >> model_run_timestamp.txt
@ECHO ====================================================== >> model_run_timestamp.txt