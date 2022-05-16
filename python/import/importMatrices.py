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

WORK_FOLDER = os.environ["ASIM"] + os.sep + "output"
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
    importOMX(file_path = "%s\\trips_%s_taz.omx"%(WORK_FOLDER, period), matrices = autoMatsToImport, scenario = scenario)

# Import Transit Trip Matrices    
for scen in TSCENS:
    scenario = databank.scenario(scen)
    desktop.data_explorer().replace_primary_scenario(scenario)
    period = per[scen]
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
        importOMX(file_path = "%s\\trn_%s_taz.omx"%(WORK_FOLDER, period), matrices = matsToImport, scenario = scenario)
        
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
print("Completed importing matrices at %s"%(datetime.datetime.now()))