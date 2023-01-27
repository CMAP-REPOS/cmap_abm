::@ECHO OFF
set ITERATION=%1
SETLOCAL EnableDelayedExpansion
::SET ANACONDA=C:\Users\%USERNAME%\.conda\envs\cmapasim\python.exe
SET ANACONDA_DIR=C:\ProgramData\Anaconda3
SET PATH=%ANACONDA_DIR%\Library\bin;%PATH%
::SET PYTHONPATH="C:\Users\%USERNAME%\.conda\envs\cmapasim\lib"

:: setup paths to Python application, Conda script, etc.
SET CONDA_ACT=%ANACONDA_DIR%\scripts\activate.bat
SET CONDA_DEA=%ANACONDA_DIR%\Scripts\deactivate.bat

:: Run ActivitySim
ECHO Activate ActivitySim Environment....
::CD /d %ANACONDA_DIR%\Scripts
CALL %CONDA_ACT% cmapasim

set MKL_NUM_THREADS=1

:: Prepare files for ActivitySim
IF %ITERATION% EQU 1 (
    python python\prepAsim.py
)

:: Crop files for testing ActivitySim
python python\crop.py little

:: Run ActivitySim
python %ASIM%\simulation.py -c %ASIM%\configs -o %ASIM_OUTPUT% -d %ASIM_INPUT%