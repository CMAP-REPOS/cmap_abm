:: runModel.bat
:: Written by BTS, 06/24/13
:: Modified by NMP, 05/14/14 - don't store username or password
::                  04/17/15 - log timestamps
::                  07/08/15 - new server config
:: ----------------------------------------------------------------------------
:: Run the CMAP Transit ABM.
:: Call from command line with CMAP username & password as arguments,
:: e.g "runmodel npeterson P@ssw0rd > blog.txt"

:: Parameters
SET projDir=Y:/{{{TEMPLATE}}}/model
SET sampleRate=0.05

@SET user=%1
@SET pwd=%2
@SET un=cmap\%user%
SET IP1=\\10.10.1.52
SET IP2=\\10.10.1.51
SET IP3=\\10.10.1.53
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

:: Create TAP lines file and run CT-RAMP
@ECHO Create TAP lines: %date% %time% >> model_run_timestamp.txt
%emmepy% runTapLines.py
@ECHO Run CT-RAMP: %date% %time% >> model_run_timestamp.txt
CALL runCTRAMP.bat %sampleRate%

:: Final skimming and assignments
@ECHO Run final assignments: %date% %time% >> model_run_timestamp.txt
%emmepy% runFinalAssignments.py 1,1,1,1,1,1,1,1

@ECHO ====================================================== >> model_run_timestamp.txt
@ECHO END CMAP REGIONAL MODEL RUN >> model_run_timestamp.txt
@ECHO Model Run End Time: %date% %time% >> model_run_timestamp.txt
@ECHO ====================================================== >> model_run_timestamp.txt