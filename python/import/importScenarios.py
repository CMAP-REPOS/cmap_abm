# 
# importScenarios.py
# 
# CMAP Copy scenarios to new Emmebank
# Ted Lin - RSG - 2022-05-08

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
transitImport = 200
TSCENS = [transitImport + i for i in HSCENS]

WORK_FOLDER = "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_inputs\\netfiles"
RECIP_PROJECT = "C:\\projects\\cmap_activitysim\\cmap_abm\\CMAP-ABM\\CMAP-ABM.emp"

with open(WORK_FOLDER+os.sep+"scens.json", 'r') as file:
    scen_desc = _json.load(file)

desktop = _app.start_dedicated(project=RECIP_PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

createScenario = _m.Modeller().tool("inro.emme.data.scenario.create_scenario")
deleteScenario = _m.Modeller().tool("inro.emme.data.scenario.delete_scenario")
impModes = _m.Modeller().tool("inro.emme.data.network.mode.mode_transaction")
impFunc = _m.Modeller().tool("inro.emme.data.function.function_transaction")
import_basenet = _m.Modeller().tool("inro.emme.data.network.base.base_network_transaction")
impVeh = _m.Modeller().tool("inro.emme.data.network.transit.vehicle_transaction")
listModes = _m.Modeller().tool("inro.emme.data.network.mode.list_modes")
import_transit = _m.Modeller().tool("inro.emme.data.network.transit.transit_line_transaction")
import_attrib = _m.Modeller().tool("inro.emme.data.extra_attribute.import_extra_attributes")
importTurns = _m.Modeller().tool("inro.emme.data.network.turn.turn_transaction")

for scen in HSCENS:
    print "Importing Highway Scenario %s"%scen
    createScenario(scenario_id = int(scen), scenario_title = scen_desc[str(scen)], overwrite = True, set_as_primary = True)    
    eFolder = WORK_FOLDER+os.sep+"scen"+str(scen)
    # Import highway mode table    
    impModes(transaction_file = eFolder + os.sep + "modes" + str(scen) + ".out", revert_on_error = False)
    # Import functions
    impFunc(transaction_file  = eFolder + os.sep + "functions" + str(scen) + ".out", throw_on_error = False)
    # Import highway net 
    import_basenet(transaction_file = eFolder + os.sep + str(scen) + ".out", revert_on_error = False)
    for f in ["extra_links_%s.txt"%scen, "extra_nodes_%s.txt"%scen]:
        import_attrib(file_path = eFolder + os.sep  + f, import_definitions = True, revert_on_error = False)
    # Import Turns
    importTurns (transaction_file  = eFolder + os.sep + "turns%s.out"%scen, revert_on_error = False)
    
for scen in TSCENS:
    print "Importing Transit Scenario %s"%scen
    createScenario(scenario_id = scen, scenario_title = scen_desc[str(scen)], overwrite = True, set_as_primary = True)
    eFolder = WORK_FOLDER+os.sep+"scen"+str(scen)
    # Import transit modes
    impModes(transaction_file = eFolder + os.sep + "modes" + str(scen) + ".in", revert_on_error = False)
    # Import transit vehicles
    impVeh(transaction_file = eFolder + os.sep + "vehicles" + str(scen) + ".in", revert_on_error = False)
    # Import transit net
    import_basenet(transaction_file = eFolder + os.sep + str(scen) + ".in", revert_on_error = False)
    import_transit(transaction_file = eFolder + os.sep + str(scen) + "_transit.in", revert_on_error = False)
    for f in ["extra_links_%s.txt"%scen, "extra_nodes_%s.txt"%scen, "extra_segments_%s.txt"%scen, "extra_transit_lines_%s.txt"%scen]:
        import_attrib(file_path = eFolder + os.sep  + f, import_definitions = True, revert_on_error = False)