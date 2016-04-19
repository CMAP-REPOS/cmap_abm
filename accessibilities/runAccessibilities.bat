::  Requires 64-bit Anaconda from www.continuum.io/downloads in C:\Anaconda
SET conda=C:\Anaconda\python.exe
CALL ..\EmmeConfig.bat
SET emmepy="%EMMEPATH%\Python27\python.exe"

:: Copy required non-skim inputs
cd ..\inputs
copy CMAP_MAZ_cents.txt ..\accessibilities\inputs
copy SubzoneData.csv ..\accessibilities\inputs
copy tap_attributes.csv ..\accessibilities\inputs

:: Create/copy all necessary skim ZMXs
:: (mfX181-83 already created by ..\runInitialSkims.py)
cd ..\CMAP-ABM\Database\emmemat
%emmepy% ../../../scripts/EMXtoZMX.py ../../CMAP-ABM.emp 103 mf3431 mf3432 mf3433 mf3434 mf3461
%emmepy% ../../../scripts/EMXtoZMX.py ../../CMAP-ABM.emp 105 mf5431 mf5432 mf5433 mf5434 mf5461
copy mf3175.zmx ..\..\..\accessibilities\inputs
copy mf3176.zmx ..\..\..\accessibilities\inputs
copy mf3177.zmx ..\..\..\accessibilities\inputs
copy mf3431.zmx ..\..\..\accessibilities\inputs
copy mf3432.zmx ..\..\..\accessibilities\inputs
copy mf3433.zmx ..\..\..\accessibilities\inputs
copy mf3434.zmx ..\..\..\accessibilities\inputs
copy mf3461.zmx ..\..\..\accessibilities\inputs
copy mf5175.zmx ..\..\..\accessibilities\inputs
copy mf5176.zmx ..\..\..\accessibilities\inputs
copy mf5177.zmx ..\..\..\accessibilities\inputs
copy mf5431.zmx ..\..\..\accessibilities\inputs
copy mf5432.zmx ..\..\..\accessibilities\inputs
copy mf5433.zmx ..\..\..\accessibilities\inputs
copy mf5434.zmx ..\..\..\accessibilities\inputs
copy mf5461.zmx ..\..\..\accessibilities\inputs

:: Return to accessibilities dir and run processing scripts
cd ..\..\..\accessibilities
%conda% cmapaccess.py near_taps
%conda% cmapaccess.py accessibilities
