import inro.emme.desktop.app as _app
import inro.modeller as _m
import highway_assignment.cmap_assignment as cmap_assignment
import network.cmap_network as cmap_network
import matrix.cmap_matrix as cmap_matrix
import sys
import os
import datetime
import traceback
import openmatrix as omx

print("Starting Auto Skim Process at %s"%(datetime.datetime.now()))
EMME_OUTPUT = os.environ["BASE_PATH"] + os.sep + "emme_outputs"
ASIM_INPUTS = os.environ["ASIM_INPUTS"]
PROJECT = os.environ["EMMEBANK"]
msa_iteration = int(sys.argv[1])

desktop = _app.start_dedicated(project=PROJECT, visible = True, user_initials = "TL")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank
#scens = [{"periodNum": 1, "period": "NT"}]

scens = [{"periodNum": 1, "period": "NT"},
   {"periodNum": 2, "period": "EA"},
   {"periodNum": 3, "period": "AM"},
   {"periodNum": 4, "period": "MM"},
   {"periodNum": 5, "period": "MD"},
   {"periodNum": 6, "period": "AF"},
   {"periodNum": 7, "period": "PM"},
   {"periodNum": 8, "period": "EV"}]

# initialize omx to save auto and transit skims
taz_skims = omx.open_file('%s\\taz_skims.omx' % ASIM_INPUTS,'w')
taz_skims.close()

for s in scens:
    print("Scenario %s Auto Assignment"%s['period'])
    scenario = desktop.data_explorer().active_database().scenario_by_number(s['periodNum'])
    desktop.data_explorer().replace_primary_scenario(scenario)
    try:
        cmap_network.CMapNetwork().__call__(databank.scenario(s['periodNum']), runCapacities = True)
        cmap_assignment.TrafficAssignment().__call__(s['period'], msa_iteration, 0.001, 40, 27, databank.scenario(s['periodNum']))
        cmap_network.CMapNetwork().__call__(databank.scenario(s['periodNum']), runPrep = False, export = True, 
                                            output_directory = "%s\\scen0%s" % (EMME_OUTPUT, s['periodNum']))
        print("Export auto matrices to OMX for time period " + s['period'])
        cmap_matrix.CMapMatrix().outputAutoSkimsToOMX(s['period'], databank.scenario(s['periodNum']), 
                                                        "%s\\taz_skims.omx" % ASIM_INPUTS)
    except:
        print("There was an error in the %s period"%s['period'])
        traceback.print_exc()

# placeholder for distance, walk distance, and bike distance
taz_skims = omx.open_file('%s\\taz_skims.omx' % ASIM_INPUTS,'a')
if 'SOV_TR_M_DIST__MD' in taz_skims.list_matrices():
    taz_skims['DIST'] = taz_skims['SOV_TR_M_DIST__MD']
    taz_skims['DISTWALK'] = taz_skims['SOV_TR_M_DIST__MD']
    taz_skims['DISTBIKE'] = taz_skims['SOV_TR_M_DIST__MD']
taz_skims.close()

print("Completed Auto Skim Process at %s"%(datetime.datetime.now()))