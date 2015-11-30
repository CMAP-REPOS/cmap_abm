# cmap_abm
CMAP's Activity Based Model

#How To Build
1. Download Eclipse Luna
2. Import cmap_abm\cmap as an Eclipse project
3. Import cmap_abm\ctrampIf as an Eclipse project
4. Update cmap\.classpath references to external required jars
5. Update ctrampIf\.classpath references to external required jars
6. Make sure jxl jar referenced before common-base jar in the .classpath files (in order to avoid Worksheet GC disabled inconsistency error)
7. Change circular dependencies from errors to warnings: Window -> Preferences -> Java -> Compiler -> Building -> Circular Dependencies
8. Export ctrampIf.jar via File -> Export and use the ctrampIf\build.jardesc
9. Export cmap.jar via File -> Export and use the cmap\build.jardesc
10. Copy ctrampIf.jar and cmap.jar into the EXEC folder when running the model
