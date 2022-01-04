import inro.emme.desktop.app as _app
import inro.modeller as _m
import highway_assignment.cmap_assignment as cmap_assignment
import network.cmap_network as cmap_network
import matrix.cmap_matrix as cmap_matrix
import sys
import datetime
import traceback

print("Starting Skim Process at %s"%(datetime.datetime.now()))

desktop = _app.start_dedicated(project="C:/projects/cmap_activitysim/cmap_abm/CMAP-ABM/CMAP-ABM.emp", visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

scens = [{"periodNum": 1, "period": "NT"},
   {"periodNum": 2, "period": "EA"},
   {"periodNum": 3, "period": "AM"},
   {"periodNum": 4, "period": "MM"},
   {"periodNum": 5, "period": "MD"},
   {"periodNum": 6, "period": "AF"},
   {"periodNum": 7, "period": "PM"},
   {"periodNum": 8, "period": "EV"}]

for s in scens:
    print "Scenario %s Assignment"%s['period']
    scenario = desktop.data_explorer().active_database().scenario_by_number(s['periodNum'])
    desktop.data_explorer().replace_primary_scenario(scenario)
    try:
        #cmap_matrix.CMapMatrix().prepTripTables(databank.scenario(s['periodNum']), s['period'])
        cmap_network.CMapNetwork().__call__(databank.scenario(s['periodNum']), runCapacities = True)
        cmap_assignment.TrafficAssignment().__call__(s['period'], 1, 0.001, 40, 27, databank.scenario(s['periodNum']))
        cmap_network.CMapNetwork().__call__(databank.scenario(s['periodNum']), runPrep = False, export = True, output_directory = "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_outputs\\scen0%s" % s['periodNum'])
        cmap_matrix.CMapMatrix().outputSkimsToOMX(s['period'], databank.scenario(s['periodNum']), "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_outputs\\scen0%sskims.omx" % (s['periodNum']))
    except:
        print "There was an error in the %s period"%s['period']
        traceback.print_exc() 

print("Completed Skim Process at %s"%(datetime.datetime.now()))