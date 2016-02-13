
rem ############  PARAMETERS  ############
set RUNTIME=Y:/{{{TEMPLATE}}}/cmap_abm
set JAVA_PATH=C:/Program Files/Java/jdk1.7.0_13

rem ##########  Run CTRAMP  ##############
"%JAVA_PATH%\bin\java" -Xmx8000m -cp "%RUNTIME%;%RUNTIME%/exec/jxl.jar;%RUNTIME%/exec/jppf-5.0.4-lib/*;%RUNTIME%/exec/*;config" -Dlog4j.configuration=log4j.xml -server -Djppf.config=jppf-guiDistributed.properties com.pb.cmap.tourBased.CmapTourBasedModel "cmap" -iteration 1 -sampleRate %1 -sampleSeed 0
