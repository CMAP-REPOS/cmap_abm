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
TSCENS = [201,202,203,204,205,206,207,208]

WORK_FOLDER = os.environ["WARM_START"] + os.sep + "wsmatrices"
PROJECT = os.environ["EMMEBANK"]

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

createMatrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
importOMX = _m.Modeller().tool("inro.emme.data.matrix.import_from_omx")
deleteMatrix = _m.Modeller().tool("inro.emme.data.matrix.delete_matrix")

per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}

# Import Highway Warm Start
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
    #importOMX(file_path = "%s\\trips_%s_taz.omx"%(WORK_FOLDER, period), matrices = matsToImport, scenario = scenario)

# Import Transit Warm Start    
for scen in TSCENS:
    scenario = databank.scenario(scen)
    period = per[scen-200]
    for vclass, mn in zip(['Low', 'Mid', 'Hi'], [362, 363, 364]):
        #matsToImport = {"TRNPerTrp_%s_%s"%(period, vclass[0:1]): "mf%s%s"%(scen-200, mn)}
        matsToImport = {"TRN_%s_%s"%(vclass.upper(), period): "mf%s%s"%(scen-200, mn)}
        for n, m in matsToImport.items():
            createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
        #importOMX(file_path = "%s\\trips_%s_%s_tap.omx"%(WORK_FOLDER, period, vclass), matrices = matsToImport, scenario = scenario)

# Delete any old skims
for scen in HSCENS:
    for skid in range(600, 660):
        try:
            deleteMatrix(matrix = databank.matrix("mf%s%s"%(scen, skid)))
        except:
            pass