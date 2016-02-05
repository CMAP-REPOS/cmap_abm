
rem ############  PARAMETERS  ############
set RUNTIME=Y:/{{{TEMPLATE}}}/cmap_abm
set JAVA_PATH=C:/Program Files/Java/jdk1.7.0_13
rem ############  NODE 1  ############
cd..
java -server -Xmx128m -cp "%RUNTIME%;%RUNTIME%/exec/jxl.jar;%RUNTIME%/exec/jppf-5.0.4-lib/*;%RUNTIME%/exec/*;config" -Dlog4j.configuration=log4j-node1.xml -Djppf.config=jppf-node1.properties org.jppf.node.NodeLauncher
