#
# Import warm start matrices
#
# importwsmatrices.py
#
# Andrew Rohne RSG

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
computeMatrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}

finalAssnMats = {400: "SOV_NT_L", 401: "SOV_TR_L", 402: "HOV2_L", 403: "HOV3_L", 404: "SOV_NT_M", 405: "SOV_TR_M", 
                406: "HOV2_M", 407: "HOV3_M", 408: "SOV_NT_H", 409: "SOV_TR_H", 410: "HOV2_H", 411: "HOV3_H", 412: "TRK_B",
                413: "TRK_L", 414: "TRK_M", 415: "TRK_H"}

# Import Highway Warm Start
for scen in HSCENS:
    period = per[scen]
    for skid in range(400, 416) + range(201, 226) + range(362, 377):
        try:
            deleteMatrix(matrix = databank.matrix("mf%s%s"%(scen, skid)))
        except:
            pass
            
    matsToImport = {
            "SOV_NT_L_%s"%period: "mf%s%s"%(scen, 214),
            "SOV_TR_L_%s"%period: "mf%s%s"%(scen, 215),
            "HOV2_L_%s"%period: "mf%s%s"%(scen, 216), 
            "HOV3_L_%s"%period: "mf%s%s"%(scen, 217),
            "SOV_NT_M_%s"%period: "mf%s%s"%(scen, 218), 
            "SOV_TR_M_%s"%period: "mf%s%s"%(scen, 219), 
            "HOV2_M_%s"%period: "mf%s%s"%(scen, 220), 
            "HOV3_M_%s"%period: "mf%s%s"%(scen, 221), 
            "SOV_NT_H_%s"%period: "mf%s%s"%(scen, 222), 
            "SOV_TR_H_%s"%period: "mf%s%s"%(scen, 223),
            "HOV2_H_%s"%period: "mf%s%s"%(scen, 224), 
            "HOV3_H_%s"%period: "mf%s%s"%(scen, 225)
            }
            
    auxMatsToImport = {
            "%s_extAuto"%period: "mf%s%s"%(scen, 201), # -> SOV TR M
            "%s_extTrkH"%period: "mf%s%s"%(scen, 202), ## -> TRK_H
            "%s_extAP"%period: "mf%s%s"%(scen, 203), # -> SOV TR M
			"%s_AIR1_L"%period: "mf%s%s"%(scen, 204), # -> SOV TR L
			"%s_AIR1_H"%period: "mf%s%s"%(scen, 205), # -> SOV TR H
			"%s_AIR2_L"%period: "mf%s%s"%(scen, 206), # -> HOV2 L
			"%s_AIR2_H"%period: "mf%s%s"%(scen, 207), # -> HOV2 H
			"%s_AIR3_L"%period: "mf%s%s"%(scen, 208), # -> HOV3 L
			"%s_AIR3_H"%period: "mf%s%s"%(scen, 209), # -> HOV3 H
            "TRK_B_%s"%period: "mf%s%s"%(scen, 210), # As-is
            "TRK_L_%s"%period: "mf%s%s"%(scen, 211), # As-is
            "TRK_M_%s"%period: "mf%s%s"%(scen, 212), # As-is
            "TRK_H_%s"%period: "mf%s%s"%(scen, 213) #
        }   
    scenario = databank.scenario(scen)    
    for n, m in matsToImport.items():
        createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
    importOMX(file_path = "%s\\trips_%s_taz.omx"%(WORK_FOLDER, period), matrices = matsToImport, scenario = scenario)

    for n, m in auxMatsToImport.items():
        createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
    importOMX(file_path = "%s\\AUX_%s.omx"%(WORK_FOLDER, period), matrices = auxMatsToImport, scenario = scenario)
    
    for n, m in finalAssnMats.items():
        createMatrix(matrix_id = "mf%s%s"%(scen, n), matrix_name = "%s_%s"%(period, m), scenario = scenario, overwrite = True)
    
    # Prepare matrices for assignment - Low VoT
    spec1 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_SOV_NT_L"%period,
        "expression": "SOV_NT_L_%s"%period,
    }
    
    spec2 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_SOV_TR_L"%period,
        "expression": "SOV_TR_L_%s + %s_AIR1_L"%(period, period),
    }

    spec3 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_HOV2_L"%period,
        "expression": "HOV2_L_%s + %s_AIR2_L"%(period, period),
    }
    
    spec4 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_HOV3_L"%period,
        "expression": "HOV3_L_%s + %s_AIR3_L"%(period, period),
    }
    
    # Prepare matrices for assignment - Mid VoT
    spec5 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_SOV_NT_M"%period,
        "expression": "SOV_NT_M_%s"%period,
    }
    
    spec6 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_SOV_TR_M"%period,
        "expression": "SOV_TR_M_%s + %s_extAuto + %s_extAP"%(period, period, period),
    }
    
    spec7 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_HOV2_M"%period,
        "expression": "HOV2_M_%s"%period,
    }
    
    spec8 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_HOV3_M"%period,
        "expression": "HOV3_M_%s"%period,
    }
    
    # Prepare matrices for assignment - High VoT
    spec9 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_SOV_NT_H"%period,
        "expression": "SOV_NT_H_%s"%period,
    }
    
    spec10 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_SOV_TR_H"%period,
        "expression": "SOV_TR_H_%s"%period, # + %s_AIR1_H"%(period,period),
    }
    
    spec11 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_HOV2_H"%period,
        "expression": "HOV2_H_%s"%period, # + %s_AIR2_H"%(period,period),
    }
    
    spec12 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_HOV3_H"%period,
        "expression": "HOV3_H_%s"%period, # +  %s_AIR3_H"%(period,period),
    }
    
    # Prepare truck matrices
    spec13 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_B_%s"%period,
        "expression": "%s_TRK_B"%period,
    }
    
    spec14 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_L_%s"%period,
        "expression": "%s_TRK_L"%period,
    }
    
    spec15 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_M_%s"%period,
        "expression": "%s_TRK_M"%period,
    }
    
    spec16 = {
        "type": "MATRIX_CALCULATION",
        "result": "%s_TRK_H"%period,
        "expression": "TRK_H_%s + %s_extTrkH"%(period,period),
    }
    
    computeMatrix([spec1, spec2, spec3, spec4, spec5, spec6, spec7, spec8, spec9, spec10, spec11, spec12, spec13, spec14, spec15, spec16]) 

# Import Transit Warm Start    
for scen in TSCENS:
    scenario = databank.scenario(scen)
    period = per[scen-200]

    matsToImport = {
            "TRNPerTrp_%s_L"%period: "mf%s%s"%(scen, 362),
            "TRNPerTrp_%s_M"%period: "mf%s%s"%(scen, 363),
            "TRNPerTrp_%s_H"%period: "mf%s%s"%(scen, 364), 
            "%s_TRN_WALK_L"%period: "mf%s%s"%(scen, 365),
            "%s_TRN_PNR_L"%period: "mf%s%s"%(scen, 366), 
            "%s_TRN_KNR_L"%period: "mf%s%s"%(scen, 367), 
            "%s_TRN_TNC_L"%period: "mf%s%s"%(scen, 368), 
            "%s_TRN_WALK_M"%period: "mf%s%s"%(scen, 369),
            "%s_TRN_PNR_M"%period: "mf%s%s"%(scen, 370), 
            "%s_TRN_KNR_M"%period: "mf%s%s"%(scen, 371), 
            "%s_TRN_TNC_M"%period: "mf%s%s"%(scen, 372), 
            "%s_TRN_WALK_H"%period: "mf%s%s"%(scen, 373),
            "%s_TRN_PNR_H"%period: "mf%s%s"%(scen, 374), 
            "%s_TRN_KNR_H"%period: "mf%s%s"%(scen, 375), 
            "%s_TRN_TNC_H"%period: "mf%s%s"%(scen, 376)
        }  
    for vclass, mn in zip(['Low', 'Mid', 'Hi'], [362, 363, 364]):
        #matsToImport = {"TRNPerTrp_%s_%s"%(period, vclass[0:1]): "mf%s%s"%(scen-200, mn)}
        matsToImport = {"TRN_%s_%s"%(vclass.upper(), period): "mf%s%s"%(scen-200, mn)}
        for n, m in matsToImport.items():
            createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
        importOMX(file_path = "%s\\trips_%s_%s_tap.omx"%(WORK_FOLDER, period, vclass), matrices = matsToImport, scenario = scenario)

# Delete any old skims
for scen in HSCENS:
    for skid in range(600, 660):
        try:
            deleteMatrix(matrix = databank.matrix("mf%s%s"%(scen, skid)))
        except:
            pass