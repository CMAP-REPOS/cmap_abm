
rem ############  PARAMETERS  ############
set RUNTIME=Y:/{{{TEMPLATE}}}/model
set JAVA_PATH=C:/Program Files/Java/jdk1.7.0_13

rem ##########  Run CTRAMP  ##############
"%JAVA_PATH%\bin\java" -Xmx64000m -cp "%RUNTIME%;%RUNTIME%/exec/jxl.jar;%RUNTIME%/exec/jppf-5.0.4-lib/*;%RUNTIME%/exec/*;config" -Dlog4j.configuration=log4j.xml -server -Djppf.config=jppf-guiLocal.properties com.pb.cmap.tourBased.CmapTourBasedModel "cmap" -iteration 1 -sampleRate %1 -sampleSeed 0

rem ##########  Run CTRAMP Remote Debug ##
rem "%JAVA_PATH%\bin\java" -Xdebug -Xrunjdwp:transport=dt_socket,address=1044,server=y,suspend=y -Xmx120000m -cp "%RUNTIME%;%RUNTIME%/exec/jxl.jar;%RUNTIME%/exec/jppf-5.0.4-lib/*;%RUNTIME%/exec/*;config" -Dlog4j.configuration=log4j.xml -server -Djppf.config=jppf-guiLocal.properties com.pb.cmap.tourBased.CmapTourBasedModel "cmap" -iteration 1 -sampleRate %1 -sampleSeed 0