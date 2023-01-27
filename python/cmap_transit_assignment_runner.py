import inro.emme.desktop.app as _app
import inro.modeller as _m
import transit_assignment.cmap_transit_assignment as cmap_transit_assignment
import network.cmap_network as cmap_network
import matrix.cmap_matrix as cmap_matrix
import sys
import os
import datetime
import traceback

print("Starting Transit Skim Process at %s"%(datetime.datetime.now()))
EMME_OUTPUT = os.environ["EMME_OUTPUT"]
ASIM_INPUT = os.environ["ASIM_INPUT"]
PROJECT = os.environ["PROJECT"]
msa_iteration = int(sys.argv[1])

desktop = _app.start_dedicated(project = PROJECT, visible = True, user_initials = "TL") 
modeller = _m.Modeller(desktop)
my_emmebank = modeller.emmebank
databank = desktop.data_explorer().active_database().core_emmebank
copy_att = _m.Modeller().tool("inro.emme.data.network.copy_attribute")
netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")
#scens = [{"periodNum": 1, "scenNum": 201, "period": "NT"}]

scens = [{"periodNum": 1, "scenNum": 201, "period": "NT"},
   {"periodNum": 3, "scenNum": 203, "period": "AM"},
   {"periodNum": 5, "scenNum": 205, "period": "MD"},
   {"periodNum": 7, "scenNum": 207, "period": "PM"}
]

data_explorer = desktop.data_explorer()
database = data_explorer.active_database()
matrix_count = 0
for s in scens:
    print("time period: " + s['period'] + " and new matrix count: " + str(matrix_count) +  "...")

    current_scenario = my_emmebank.scenario(s['scenNum'])
    data_explorer.replace_primary_scenario(database.scenario_by_number(s['scenNum']))

    # copy congested auto travel time from highway scenarios
    from_att = "timau"
    to_att = "ul2"
    from_scen = _m.Modeller().emmebank.scenario(s['periodNum'])
    if not from_scen.has_traffic_results:
        raise Exception("missing traffic assignment results for scenario %s" % (str(scen-200)))
    copy_att(from_attribute_name=from_att,
            to_attribute_name=to_att,
            from_scenario=from_scen)

    spec1 = {
        "result": "us1",
        "expression": "(us1*(ttf.eq.2))+(us1.max.ul2)*(ttf.eq.1)", #ttf1=normal, ttf2=BRT
        "selections": {
            "link": "all",
            "transit_line": "all"
        },
        "type": "NETWORK_CALCULATION"
    }
    spec2 = {
        "result": "ul2",
        "expression": "0",
        "selections": {
            "link": "all",
        },        
        "type": "NETWORK_CALCULATION"
    }    
    netcalc([spec1,spec2])

    try:
        cmap_transit_assignment.TransitAssignment().__call__(str(s['periodNum']), matrix_count, current_scenario, 
                                                            ccr_periods = "", num_processors = 27)
        if msa_iteration == 2:
            cmap_network.CMapNetwork().__call__(databank.scenario(s['scenNum']), runPrep = False, export = True, 
                                                output_directory = "%s\\scen%s" % (EMME_OUTPUT, s['scenNum']))          
        print("Export transit matrices to OMX for time period " + s['period'])      
        cmap_matrix.CMapMatrix().outputTransitSkimsToOMX(s['period'], databank.scenario(s['periodNum']), 
                                                            "%s\\taz_skims.omx" % ASIM_INPUT)
    except:
        print("There was an error in the %s period"%s['period'])
        traceback.print_exc() 
                                                                
print("Completed Transit Skim Process at %s"%(datetime.datetime.now()))
