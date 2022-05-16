#
# importwsmatrices.py
#
# Import warm start matrices
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
import datetime

HSCENS = [1,2,3,4,5,6,7,8]
transitImport = 200
TSCENS = [transitImport + i for i in HSCENS]
per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}

WORK_FOLDER = os.environ["WARM_START"] + os.sep + "wsmatrices"
PROJECT = os.environ["EMMEBANK"]

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

importMatrix = _m.Modeller().tool("inro.emme.data.matrix.matrix_transaction")
createMatrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
importOMX = _m.Modeller().tool("inro.emme.data.matrix.import_from_omx")
deleteMatrix = _m.Modeller().tool("inro.emme.data.matrix.delete_matrix")
computeMatrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

TODFactor_intTRK_B = [0.161, 0.054, 0.129, 0.050, 0.214, 0.132, 0.150, 0.110]
TODFactor_intTRK_L = [0.143, 0.052, 0.142, 0.066, 0.264, 0.147, 0.112, 0.074]
TODFactor_intTRK_M = [0.174, 0.049, 0.129, 0.061, 0.251, 0.139, 0.113, 0.084]
TODFactor_intTRK_H = [0.216, 0.039, 0.102, 0.059, 0.249, 0.118, 0.092, 0.125]
TODFactor_extAuto = [0.161, 0.054, 0.129, 0.050, 0.214, 0.132, 0.150, 0.110]
TODFactor_extTRK_H = [0.216, 0.039, 0.102, 0.059, 0.249, 0.118, 0.092, 0.125]
TODFactor_extAP = [0.161, 0.054, 0.129, 0.050, 0.214, 0.132, 0.150, 0.110]

finalAssnMats = {300: "SOV_NT_TOT_L", 301: "SOV_TR_TOT_L", 302: "HOV2_TOT_L", 303: "HOV3_TOT_L", 304: "SOV_NT_TOT_M", 
                305: "SOV_TR_TOT_M", 306: "HOV2_TOT_M", 307: "HOV3_TOT_M", 308: "SOV_NT_TOT_H", 309: "SOV_TR_TOT_H", 
                310: "HOV2_TOT_H", 311: "HOV3_TOT_H", 312: "TRK_TOT_B", 313: "TRK_TOT_L", 314: "TRK_TOT_M", 315: "TRK_TOT_H"}
'''
# Delete unused skims: TODO to be removed
for skim in ["CTABUSLDIST", "PACEBUSRDIST", "PACEBUSLDIST", "PACEBUSEDIST", "CTABUSEDIST", "CTARAILDIST", "METRARAILDIST"]:
    for amode in ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]: 
        for uc in ["L", "M", "H"]:
            for period in ["NT", "EA", "AM", "MM", "MD", "AF", "PM", "EV"]
                try:
                    deleteMatrix(matrix = databank.matrix("mf%s_%s_%s__%s"%(skim, amode, uc, period)))
                except:
                    pass
'''  
# Delete any old skims: TODO to be removed
for scen in HSCENS:
    for skid in range(400, 1000):
        try:
            deleteMatrix(matrix = databank.matrix("mf%s%s"%(scen, skid)))
        except:
            pass
'''
# Import truck and external trips
dailyMatrices = ["truck_trip_matrices.in", "poe_trip_matrices.in"]
for m in dailyMatrices: 
    importMatrix(transaction_file = WORK_FOLDER + os.sep + m, throw_on_error = False)
'''
# Import Highway Warm Start
for scen in HSCENS:
    scenario = databank.scenario(scen)
    desktop.data_explorer().replace_primary_scenario(scenario)
    period = per[scen]
    for skid in range(300, 316) + range(101, 126) + range(262, 283):
        try:
            deleteMatrix(matrix = databank.matrix("mf%s%s"%(scen, skid)))
        except:
            pass
            
    autoMatsToImport = {
            "SOV_NT_L_%s"%period: "mf%s%s"%(scen, 101),
            "SOV_TR_L_%s"%period: "mf%s%s"%(scen, 102),
            "HOV2_L_%s"%period: "mf%s%s"%(scen, 103), 
            "HOV3_L_%s"%period: "mf%s%s"%(scen, 104),
            "SOV_NT_M_%s"%period: "mf%s%s"%(scen, 105), 
            "SOV_TR_M_%s"%period: "mf%s%s"%(scen, 106), 
            "HOV2_M_%s"%period: "mf%s%s"%(scen, 107), 
            "HOV3_M_%s"%period: "mf%s%s"%(scen, 108), 
            "SOV_NT_H_%s"%period: "mf%s%s"%(scen, 109), 
            "SOV_TR_H_%s"%period: "mf%s%s"%(scen, 110),
            "HOV2_H_%s"%period: "mf%s%s"%(scen, 111), 
            "HOV3_H_%s"%period: "mf%s%s"%(scen, 112)
        }
            
    auxMatsToImport = {
            "TRK_B_%s"%period: "mf%s%s"%(scen, 113), # As-is
            "TRK_L_%s"%period: "mf%s%s"%(scen, 114), # As-is
            "TRK_M_%s"%period: "mf%s%s"%(scen, 115), # As-is
            "TRK_H_%s"%period: "mf%s%s"%(scen, 116), #            
            "extAuto_%s"%period: "mf%s%s"%(scen, 117), # -> SOV TR M
            "extTRK_H_%s"%period: "mf%s%s"%(scen, 118), ## -> TRK_H
            "extAP_%s"%period: "mf%s%s"%(scen, 119), # -> SOV TR M
			"AIR1_L_%s"%period: "mf%s%s"%(scen, 120), # -> SOV TR L
			"AIR1_H_%s"%period: "mf%s%s"%(scen, 121), # -> SOV TR H
			"AIR2_L_%s"%period: "mf%s%s"%(scen, 122), # -> HOV2 L
			"AIR2_H_%s"%period: "mf%s%s"%(scen, 123), # -> HOV2 H
			"AIR3_L_%s"%period: "mf%s%s"%(scen, 124), # -> HOV3 L
			"AIR3_H_%s"%period: "mf%s%s"%(scen, 125) # -> HOV3 H
        }          
    scenario = databank.scenario(scen)    
    for n, m in autoMatsToImport.items():
        createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
    importOMX(file_path = "%s\\trips_%s_taz.omx"%(WORK_FOLDER, period), matrices = autoMatsToImport, scenario = scenario)

    for n, m in auxMatsToImport.items():
        createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
    importOMX(file_path = "%s\\AUX_%s.omx"%(WORK_FOLDER, period), matrices = auxMatsToImport, scenario = scenario)

    for n, m in finalAssnMats.items():
        createMatrix(matrix_id = "mf%s%s"%(scen, n), matrix_name = "%s_%s"%(m, period), scenario = scenario, overwrite = True)
    '''
    # matrix calc mfx113-x119 using mf4-10 and TOD factors
    spec1 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfTRK_B_%s"%(period),
        "expression": "mfbcvtr*%f"%(TODFactor_intTRK_B[scen-1]),
    }
    spec2 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfTRK_L_%s"%(period),
        "expression": "mflcvtr*%f"%(TODFactor_intTRK_L[scen-1]),
    }
    spec3 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfTRK_M_%s"%(period),
        "expression": "mfmcvtr*%f"%(TODFactor_intTRK_M[scen-1]),
    }
    spec4 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfTRK_H_%s"%(period),
        "expression": "mfhcvtr*%f"%(TODFactor_intTRK_H[scen-1]),
    }
    spec5 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfextAuto_%s"%(period),
        "expression": "mfpoeaut*%f"%(TODFactor[scen-1]),
    }
    spec6 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfextTRK_H_%s"%(period),
        "expression": "mfpoetrk*%f"%(TODFactor[scen-1]),
    }
    spec7 = {
        "type": "MATRIX_CALCULATION",
        "result": "mfextAP_%s"%(period),
        "expression": "mfpoeair*%f"%(TODFactor[scen-1]),
    }        
    computeMatrix([spec1,spec2,spec3,spec4,spec5,spec6,spec7]) 
    '''
    # Prepare matrices for assignment - Low VoT
    spec1 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_NT_TOT_L_%s"%period,
        "expression": "SOV_NT_L_%s"%period,
    }
    
    spec2 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_TR_TOT_L_%s"%period,
        "expression": "SOV_TR_L_%s + AIR1_L_%s"%(period, period),
    }

    spec3 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV2_TOT_L_%s"%period,
        "expression": "HOV2_L_%s + AIR2_L_%s"%(period, period),
    }
    
    spec4 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV3_TOT_L_%s"%period,
        "expression": "HOV3_L_%s + AIR3_L_%s"%(period, period),
    }
    
    # Prepare matrices for assignment - Mid VoT
    spec5 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_NT_TOT_M_%s"%period,
        "expression": "SOV_NT_M_%s"%period,
    }
    
    spec6 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_TR_TOT_M_%s"%period,
        "expression": "SOV_TR_M_%s + extAuto_%s + extAP_%s"%(period, period, period),
    }
    
    spec7 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV2_TOT_M_%s"%period,
        "expression": "HOV2_M_%s"%period,
    }
    
    spec8 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV3_TOT_M_%s"%period,
        "expression": "HOV3_M_%s"%period,
    }
    
    # Prepare matrices for assignment - High VoT
    spec9 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_NT_TOT_H_%s"%period,
        "expression": "SOV_NT_H_%s"%period,
    }
    
    spec10 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_TR_TOT_H_%s"%period,
        "expression": "SOV_TR_H_%s + AIR1_H_%s"%(period,period),
    }
    
    spec11 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV2_TOT_H_%s"%period,
        "expression": "HOV2_H_%s + AIR2_H_%s"%(period,period),
    }
    
    spec12 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV3_TOT_H_%s"%period,
        "expression": "HOV3_H_%s + AIR3_H_%s"%(period,period),
    }
    
    # Prepare truck matrices
    spec13 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_TOT_B_%s"%period,
        "expression": "TRK_B_%s"%period,
    }
    
    spec14 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_TOT_L_%s"%period,
        "expression": "TRK_L_%s"%period,
    }
    
    spec15 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_TOT_M_%s"%period,
        "expression": "TRK_M_%s"%period,
    }
    
    spec16 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_TOT_H_%s"%period,
        "expression": "TRK_H_%s + extTRK_H_%s"%(period,period),
    }
    
    computeMatrix([spec1, spec2, spec3, spec4, spec5, spec6, spec7, spec8, 
                    spec9, spec10, spec11, spec12, spec13, spec14, spec15, spec16]) 

# Import Transit Warm Start    
for scen in TSCENS:
    scenario = databank.scenario(scen)
    desktop.data_explorer().replace_primary_scenario(scenario)
    period = per[scen-200]
    for vot, V, vint in zip(['low','mid','hi'], ['L', 'M', 'H'], [0, 1, 2]):
        trnMatsToImport = {
                "TRN_TOT_%s_%s"%(V,period): "mf%s%s"%(scen-200, 262 + vint),
                "TRN_WALK_%s_%s"%(V,period): "mf%s%s"%(scen-200, 265 + 7 * vint),
                "TRN_PNROUT_%s_%s"%(V,period): "mf%s%s"%(scen-200, 266 + 7 * vint), 
                "TRN_PNRIN_%s_%s"%(V,period): "mf%s%s"%(scen-200, 267 + 7 * vint), 
                "TRN_KNROUT_%s_%s"%(V,period): "mf%s%s"%(scen-200, 268 + 7 * vint),                 
                "TRN_KNRIN_%s_%s"%(V,period): "mf%s%s"%(scen-200, 269 + 7 * vint), 
                "TRN_TNCOUT_%s_%s"%(V,period): "mf%s%s"%(scen-200, 270 + 7 * vint), 
                "TRN_TNCIN_%s_%s"%(V,period): "mf%s%s"%(scen-200, 271 + 7 * vint)                
            }
        
        for n, m in trnMatsToImport.items():
            createMatrix(matrix_id = m, matrix_name = n, scenario = scenario, overwrite = True)
            spec1 = {
                "type": "MATRIX_CALCULATION",
                "result": "mf%s"%n,
                "expression": "0.0001",
            }
            computeMatrix(spec1)
        #importOMX(file_path = "%s\\trn_%s_taz.omx"%(WORK_FOLDER, period), matrices = trnMatsToImport, scenario = scenario)

        # Combine KNR transit and TNC transit into KNR transit
        spec1 = {
            "type": "MATRIX_CALCULATION",
            "result": "TRN_KNROUT_%s_%s"%(V,period),
            "expression": "TRN_KNROUT_%s_%s + TRN_TNCOUT_%s_%s"%(V,period,V,period),
        }
        spec2 = {
            "type": "MATRIX_CALCULATION",
            "result": "TRN_KNRIN_%s_%s"%(V,period),
            "expression": "TRN_KNRIN_%s_%s + TRN_TNCIN_%s_%s"%(V,period,V,period),
        }
        spec3 = {
            "type": "MATRIX_CALCULATION",
            "result": "0",
            "expression": "TRN_TNCOUT_%s_%s"%(V,period),
        }
        spec4 = {
            "type": "MATRIX_CALCULATION",
            "result": "0",
            "expression": "TRN_TNCIN_%s_%s"%(V,period),
        }        
        computeMatrix([spec1, spec2])
print("Completed importing warmstart matrices at %s"%(datetime.datetime.now()))