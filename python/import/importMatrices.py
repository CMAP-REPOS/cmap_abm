#
# importMatrices.py
#
# Import matrices from ActivitySim
#

import inro.modeller as _m
import sys
import os
import inro.emme.desktop.app as _app
import datetime
import pandas as pd

HSCENS = [1,2,3,4,5,6,7,8]
TSCENS = [201,203,205,207]

ASIM_OUTPUT = os.environ["ASIM_OUTPUT"]
EMME_OUTPUT = os.environ["EMME_OUTPUT"]
PROJECT = os.environ["PROJECT"]
msa_iteration = int(sys.argv[1])

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="TL")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

importOMX = _m.Modeller().tool("inro.emme.data.matrix.import_from_omx")
computeMatrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

per = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}
summary = True
if summary:
    create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")           
    temp_matrix = create_matrix(matrix_id = "ms1", matrix_name = "TEMP_SUM", matrix_description = "temp mf sum", default_value = 0, overwrite = True)

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
    importOMX(file_path = "%s\\trips_%s_taz.omx"%(ASIM_OUTPUT, period), 
                matrices = autoMatsToImport,
                zone_mapping='TAZ',
                scenario = scenario)

    spec1 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_NT_TOT_L_%s"%period,
        "expression": "SOV_NT_L_%s"%period,
    }
    
    spec2 = {
        "type": "MATRIX_CALCULATION",
        "result": "SOV_TR_TOT_L_%s"%period,
        "expression": "SOV_TR_L_%s+0.1*extAuto_%s+0.1*extAP_%s"%(period, period, period),
    }

    spec3 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV2_TOT_L_%s"%period,
        "expression": "HOV2_L_%s"%period,
    }
    
    spec4 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV3_TOT_L_%s"%period,
        "expression": "HOV3_L_%s"%period,
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
        "expression": "SOV_TR_M_%s+0.45*extAuto_%s+0.45*extAP_%s"%(period, period, period),
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
        "expression": "SOV_TR_H_%s+0.45*extAuto_%s+0.45*extAP_%s"%(period, period, period),
    }
    
    spec11 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV2_TOT_H_%s"%period,
        "expression": "HOV2_H_%s"%period,
    }
    
    spec12 = {
        "type": "MATRIX_CALCULATION",
        "result": "HOV3_TOT_H_%s"%period,
        "expression": "HOV3_H_%s"%period,
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
        "expression": "TRK_M_%s*2"%period,
    }
    
    spec16 = {
        "type": "MATRIX_CALCULATION",
        "result": "TRK_TOT_H_%s"%period,
        "expression": "(TRK_H_%s + extTRK_H_%s)*3"%(period,period),
    }
    
    computeMatrix([spec1, spec2, spec3, spec4, spec5, spec6, spec7, spec8, 
                    spec9, spec10, spec11, spec12, spec13, spec14, spec15, spec16])
    
    if summary:
        # export max, average, sum for each transit skim matrix
        data = []
        matrices = ["SOV_NT", "SOV_TR", "HOV2", "HOV3"]
        for V in ['L', 'M', 'H']:        
            for name in matrices:
                demand_name = "%s_%s_%s" % (name, V, period)
                spec_sum={
                    "expression": demand_name,
                    "result": "msTEMP_SUM",
                    "aggregation": {
                        "origins": "+",
                        "destinations": "+"
                    },
                    "type": "MATRIX_CALCULATION"
                }
                report = computeMatrix(spec_sum) 
                data.append([demand_name, report["maximum"], report["maximum_at"]["origin"], report["maximum_at"]["destination"], 
                            report["average"], report["sum"]])
        nonres_matrices = ["TRK_B", "TRK_L", "TRK_M", "TRK_H", "extAuto", "extTRK_H", "extAP"]       
        for name in nonres_matrices:
            demand_name = "%s_%s" % (name, period)
            spec_sum={
                "expression": demand_name,
                "result": "msTEMP_SUM",
                "aggregation": {
                    "origins": "+",
                    "destinations": "+"
                },
                "type": "MATRIX_CALCULATION"
            }
            report = computeMatrix(spec_sum) 
            data.append([demand_name, report["maximum"], report["maximum_at"]["origin"], report["maximum_at"]["destination"], 
                        report["average"], report["sum"]])                            
        df = pd.DataFrame(data, columns=['Demand', 'Max', 'Max orig', 'Max dest', 'Avg', 'Sum'])
        filename = "%s\\auto_matrix_list_iter%s_%s.csv"%(EMME_OUTPUT, msa_iteration, datetime.date.today())
        df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')

        VMT = []        
        matrices = ["SOV_NT", "SOV_TR", "HOV2", "HOV3"]
        for V in ['L', 'M', 'H']:        
            for name in matrices:
                vmt_name = "%s_%s_%s*mfSOV_TR_M_DIST__MD" % (name, V, period)
                spec_sum={
                    "expression": vmt_name,
                    "result": "msTEMP_SUM",
                    "aggregation": {
                        "origins": "+",
                        "destinations": "+"
                    },
                    "type": "MATRIX_CALCULATION"
                }
                report = computeMatrix(spec_sum) 
                VMT.append([vmt_name, report["maximum"], report["maximum_at"]["origin"], report["maximum_at"]["destination"], 
                            report["average"], report["sum"]])                           
        df = pd.DataFrame(VMT, columns=['VMT', 'Max', 'Max orig', 'Max dest', 'Avg', 'Sum'])
        filename = "%s\\auto_vmt_iter%s_%s.csv"%(EMME_OUTPUT, msa_iteration, datetime.date.today())
        df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')        

# Import Transit Trip Matrices    
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
        
        importOMX(file_path = "%s\\trn_%s_taz.omx"%(ASIM_OUTPUT, period), 
                    matrices = trnMatsToImport, 
                    zone_mapping='TAZ',
                    scenario = scenario)
        
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
            "result": "TRN_TNCOUT_%s_%s"%(V,period),
            "expression": "0",
        }
        spec4 = {
            "type": "MATRIX_CALCULATION",
            "result": "TRN_TNCIN_%s_%s"%(V,period),
            "expression": "0",
        }        
        computeMatrix([spec1, spec2, spec3, spec4])
        
        if summary:
            # export max, average, sum for each transit skim matrix
            data = []
            matrices = ["TOT", "WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN", "TNCOUT", "TNCIN"]
            for name in matrices:
                demand_name = "TRN_%s_%s_%s" % (name, V, period)
                spec_sum={
                    "expression": demand_name,
                    "result": "msTEMP_SUM",
                    "aggregation": {
                        "origins": "+",
                        "destinations": "+"
                    },
                    "type": "MATRIX_CALCULATION"
                }
                report = computeMatrix(spec_sum) 
                data.append([demand_name, report["maximum"], report["maximum_at"]["origin"], report["maximum_at"]["destination"], 
                            report["average"], report["sum"]])
            df = pd.DataFrame(data, columns=['Demand', 'Max', 'Max orig', 'Max dest', 'Avg', 'Sum'])
            filename = "%s\\trn_matrix_list_iter%s_%s.csv"%(EMME_OUTPUT, msa_iteration, datetime.date.today())
            df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')
print("Completed importing matrices at %s"%(datetime.datetime.now()))