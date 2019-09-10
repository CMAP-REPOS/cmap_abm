:: The following code is taken directly from %EMMEPATH%\programs\Emme-cmd.bat
:: (only code after "@echo off", up to and including the "@set path" command
:: was copied). The alternative, according to INRO, is to reinstall Emme and
:: check the box to make its Python the system/default Python.
@set DYNAMEQ=
@for /F "tokens=2* usebackq" %%a in (
`%comspec% /C reg QUERY "HKEY_LOCAL_MACHINE\SOFTWARE\INRO\Dynameq" /v CurrentPath 2^>nul`
) do (
    if %errorlevel%==1 set DYNAMEQ=
    if %errorlevel%==0 set DYNAMEQ=%%b
)
IF NOT EXIST "%DYNAMEQ%" set DYNAMEQ=
@set EMMEPATH=C:\Program Files\INRO\Emme\Emme 4\Emme-4.3.6
@set INRO_HOST=EMMEPATH
@set TOOLBOX_PATH=%EMMEPATH%
@set MODELLER_PYTHON=%EMMEPATH%\Python27\
@set currentdir=%CD%
@IF NOT EXIST "%APPDATA%\INRO\Emme" @goto endloop
@cd "%APPDATA%\INRO\Emme\"
@IF EXIST lastworkspace_4.3.6-64bit (
@For /F "tokens=1* delims==" %%A IN (lastworkspace_4.3.6-64bit) DO ( @IF /I "%%A"=="ModellerPython " @set MODELLER_PYTHON=%%B ) )
@cd "%currentdir%"
@IF  "X%MODELLER_PYTHON: =%"=="X" @set MODELLER_PYTHON=%EMMEPATH%\Python27\
@For /f "tokens=* delims= " %%a in ("%MODELLER_PYTHON%") do @set MODELLER_PYTHON=%%a
@IF /I "%MODELLER_PYTHON:~0,13%"=="%%<$EmmePath>%%" @set MODELLER_PYTHON=%EMMEPATH%%MODELLER_PYTHON:~13%
@IF NOT "x%MODELLER_PYTHON:<=%"=="x%MODELLER_PYTHON%" @set MODELLER_PYTHON=%EMMEPATH%\Python27\
@IF NOT "x%MODELLER_PYTHON:>=%"=="x%MODELLER_PYTHON%" @set MODELLER_PYTHON=%EMMEPATH%\Python27\
@set MODELLER_PYTHON=%MODELLER_PYTHON%##
@set MODELLER_PYTHON=%MODELLER_PYTHON:                ##=##%
@set MODELLER_PYTHON=%MODELLER_PYTHON:        ##=##%
@set MODELLER_PYTHON=%MODELLER_PYTHON:    ##=##%
@set MODELLER_PYTHON=%MODELLER_PYTHON:  ##=##%
@set MODELLER_PYTHON=%MODELLER_PYTHON: ##=##%
@set MODELLER_PYTHON=%MODELLER_PYTHON:##=%
@IF NOT "%MODELLER_PYTHON:~-4%"==".exe" @goto endloop
@set var1=%MODELLER_PYTHON%
@set var2=%var1%
@set i=0
@set j=1
:loopprocess
@for /F "tokens=1* delims=\" %%A in ( "%var1%" ) do (
@set /A i+=1
@set var1=%%B
@goto loopprocess )
:loopprocess2
@for /F "tokens=%j% delims=\" %%G in ( "%var2%" ) do (
@set /A j+=1
@IF %i%==%j% @goto endloop
@IF %j%==1 @set MODELLER_PYTHON=%%G\
@IF NOT %j%==1 @set "MODELLER_PYTHON=%MODELLER_PYTHON%%%G\"
@goto loopprocess2 )
:endloop
@IF NOT EXIST "%MODELLER_PYTHON%" @set MODELLER_PYTHON=%EMMEPATH%\Python27\
@IF NOT "%MODELLER_PYTHON:~-1%"=="\" @set MODELLER_PYTHON=%MODELLER_PYTHON%\
@set path=%EMMEPATH%\programs;%MODELLER_PYTHON%;%PATH%