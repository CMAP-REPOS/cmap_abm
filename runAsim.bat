@ECHO OFF

SETLOCAL EnableDelayedExpansion
SET ANACONDA=C:\Users\%USERNAME%\.conda\envs\cmapasim\python.exe
SET ANACONDA_DIR=C:\ProgramData\Anaconda3
SET PATH=%ANACONDA_DIR%\Library\bin;%PATH%
ECHO %PATH%
SET PYTHONPATH="C:\Users\%USERNAME%\.conda\envs\cmapasim\lib"

:: setup paths to Python application, Conda script, etc.
SET CONDA_ACT=%ANACONDA_DIR%\scripts\activate.bat
ECHO CONDA_ACT: %CONDA_ACT%

SET CONDA_DEA=%ANACONDA_DIR%\Scripts\deactivate.bat
ECHO CONDA_DEA: %CONDA_DEA%

:: Run ActivitySim
ECHO Activate ActivitySim Environment....
::CD /d %ANACONDA_DIR%\Scripts
CALL %CONDA_ACT% cmapasim

set MKL_NUM_THREADS=1
echo %PYTHONPATH%

:: Removing old outputs
DEL /s /q %ASIM%\output\*
::#TODO: output skim matrices to ActivitySim folder

::
:: Prepare files for ActivitySim and run ActivitySim
::
::%ANACONDA% python\prepAsim.py
%ANACONDA% %ASIM%\simulation.py -c %ASIM%\configs -o %ASIM%\output -d %ASIM%\data