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

HSCENS = [1,2,3,4,5,6,7,8]
TSCENS_TO_EXPORT = [201,202,203,204,205,206,207,208]

WORK_FOLDER = "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_inputs\\wsmatrixes\\"
PROJECT = "C:\\projects\\cmap_activitysim\\cmap_abm\\CMAP-ABM\\CMAP-ABM.emp"

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

createMatrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
importOMX = _m.Modeller().tool("inro.emme.data.matrix.import_from_omx")

per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}

for scen in HSCENS:
    period = per[scen]
    matsToImport = {
            "%s_SOV_NT_L"%period: "mf%s%s"%(scen, 400),
            "%s_SOV_TR_L"%period: "mf%s%s"%(scen, 401),
            "%s_HOV2_L"%period: "mf%s%s"%(scen, 402), 
            "%s_HOV3_L"%period: "mf%s%s"%(scen, 403),
            "%s_SOV_NT_M"%period: "mf%s%s"%(scen, 404), 
            "%s_SOV_TR_M"%period: "mf%s%s"%(scen, 405), 
            "%s_HOV2_M"%period: "mf%s%s"%(scen, 406), 
            "%s_HOV3_M"%period: "mf%s%s"%(scen, 407), 
            "%s_SOV_NT_H"%period: "mf%s%s"%(scen, 408), 
            "%s_SOV_TR_H"%period: "mf%s%s"%(scen, 409),
            "%s_HOV2_H"%period: "mf%s%s"%(scen, 410), 
            "%s_HOV3_H"%period: "mf%s%s"%(scen, 411), 
            "%s_TRK_L"%period: "mf%s%s"%(scen, 412), 
            "%s_TRK_M"%period: "mf%s%s"%(scen, 413), 
            "%s_TRK_H"%period: "mf%s%s"%(scen, 414)
        }   
    scenario = databank.scenario(scen)    
    for n, m in matsToImport.items():
        createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
    importOMX(file_path = "%s\\warmstart0%s.omx"%(WORK_FOLDER, scen), matrices = matsToImport, scenario = scenario)