#
# Import warm start matrices
#
# importwsmatrices.py
#

import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import numpy
import array
import os
import json as _json
import inro.emme.desktop.app as _app

HSCENS = [201,202,203,204,205,206,207,208]

WORK_FOLDER = "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_inputs\\wsTrnmatrixes\\"
PROJECT = "C:\\projects\\cmap_activitysim\\cmap_abm\\CMAP-ABM\\CMAP-ABM.emp"

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

createMatrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
importOMX = _m.Modeller().tool("inro.emme.data.matrix.import_from_omx")
changeMatrix = _m.Modeller().tool("inro.emme.data.matrix.change_matrix_properties")

per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}

for scen in HSCENS:
    scenario = databank.scenario(scen)
    desktop.data_explorer().replace_primary_scenario(scenario)    
    period = per[scen-200]
    matsToImport = {
            "Cl1Pre": "mf%s%s"%(scen-200, 362),
            "Cl2Pre": "mf%s%s"%(scen-200, 363), 
            "Cl3Pre": "mf%s%s"%(scen-200, 364)
        }   
    
    for n, m in matsToImport.items():
        createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
        print(m)
    importOMX(file_path = "%s\\warmstart0%s.omx"%(WORK_FOLDER, scen-200), matrices = matsToImport, scenario = scenario)
    
    matFrom = ["mf%s%s"%(scen-200, 362), "mf%s%s"%(scen-200, 363), "mf%s%s"%(scen-200, 364)]
    matTo = ["TRNPerTrp_%s_L"%period, "TRNPerTrp_%s_M"%period, "TRNPerTrp_%s_H"%period]
    for mi, mo in zip(matFrom, matTo):
        matrix_ptr = databank.matrix(mi)
        changeMatrix(matrix = matrix_ptr, matrix_name = mo)
        