
rem #Run CMAP Transit Virtual Path Builder
rem #Ben Stabler, stabler@pbworld.com, 02/12/13

rem ##########  Parameters  ############
set RUNTIME=%CD%
set HOST_IP=localhost

rem ##########  Run TVPB  ##############
java -Xmx40000m -cp "%RUNTIME%;%RUNTIME%/exec/jxl.jar;%RUNTIME%/exec/*;exec" -Dlog4j.configuration=config/log4j.xml -server com.pb.cmap.tvpb.TransitVirtualPathBuilder "cmap"

rem ##########  Run TVPB Remote Debug ##
rem java -Xdebug -Xrunjdwp:transport=dt_socket,address=1044,server=y,suspend=y -Xmx40000m -cp "%RUNTIME%;%RUNTIME%/exec/jxl.jar;%RUNTIME%/exec/*;exec" -Dlog4j.configuration=config/log4j.xml -server com.pb.cmap.tvpb.TransitVirtualPathBuilder "cmap"

