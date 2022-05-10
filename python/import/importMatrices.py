#
# importMatrices.py
#
# Import matrices from ActivitySim
#

import inro.modeller as _m
import os
import inro.emme.desktop.app as _app
import datetime

HSCENS = [1,2,3,4,5,6,7,8]
transitImport = 200
TSCENS = [transitImport + i for i in HSCENS]

WORK_FOLDER = os.environ["WARM_START"] + os.sep + "wsmatrices"
PROJECT = os.environ["EMMEBANK"]

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

importOMX = _m.Modeller().tool("inro.emme.data.matrix.import_from_omx")

per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}

# Import Highway Trip Matrices
for scen in HSCENS:
    scenario = databank.scenario(scen)
    desktop.data_explorer().replace_primary_scenario(scenario)
    period = per[scen]
    importOMX(file_path = "%s\\trips_%s_taz.omx"%(WORK_FOLDER, period), matrices = autoMatsToImport, scenario = scenario)

# Import Transit Trip Matrices    
for scen in TSCENS:
    scenario = databank.scenario(scen)
    desktop.data_explorer().replace_primary_scenario(scenario)
    period = per[scen]
    #importOMX(file_path = "%s\\trn_%s_taz.omx"%(WORK_FOLDER, period), matrices = matsToImport, scenario = scenario)
print("Completed importing matrices at %s"%(datetime.datetime.now()))