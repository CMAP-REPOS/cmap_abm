# 
# importScenarios.py
# 
# CMAP create scenarios in new Emmebank
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
import datetime

print("Starting importing scenarios at %s"%(datetime.datetime.now()))

HSCENS = [1,2,3,4,5,6,7,8]
TSCENS = [201,203,205,207]
TPERIODS = [1,2,3,4]

WORK_FOLDER = os.environ["EMME_INPUT"] + os.sep + "netfiles"
autoFolder = WORK_FOLDER + os.sep + 'scen_auto'
transitFolder = WORK_FOLDER + os.sep + 'scen_transit'
RECIP_PROJECT = os.environ["PROJECT"]

with open(WORK_FOLDER+os.sep+"scens.json", 'r') as file:
    scen_desc = _json.load(file)

desktop = _app.start_dedicated(project=RECIP_PROJECT, visible=True, user_initials="TL")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

createScenario = _m.Modeller().tool("inro.emme.data.scenario.create_scenario")
deleteScenario = _m.Modeller().tool("inro.emme.data.scenario.delete_scenario")
impModes = _m.Modeller().tool("inro.emme.data.network.mode.mode_transaction")
impFunc = _m.Modeller().tool("inro.emme.data.function.function_transaction")
import_basenet = _m.Modeller().tool("inro.emme.data.network.base.base_network_transaction")
run_macro = _m.Modeller().tool("inro.emme.prompt.run_macro")
impVeh = _m.Modeller().tool("inro.emme.data.network.transit.vehicle_transaction")
listModes = _m.Modeller().tool("inro.emme.data.network.mode.list_modes")
import_transit = _m.Modeller().tool("inro.emme.data.network.transit.transit_line_transaction")
import_link_shape = _m.Modeller().tool("inro.emme.data.network.base.link_shape_transaction")
create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
import_attrib = _m.Modeller().tool("inro.emme.data.extra_attribute.import_extra_attributes")
importTurns = _m.Modeller().tool("inro.emme.data.network.turn.turn_transaction")

# Import functions
impFunc(transaction_file  = WORK_FOLDER + os.sep + "functions.in", throw_on_error = False)

for scen in HSCENS:
    print("Importing Highway Scenario %s"%scen)
    createScenario(scenario_id = int(scen), scenario_title = scen_desc[str(scen)], overwrite = True, set_as_primary = True)
    # Import highway mode table    
    impModes(transaction_file = autoFolder + os.sep + "modes.in", revert_on_error = False)
    # Import highway net 
    import_basenet(transaction_file = autoFolder + os.sep + "1000" + str(scen) + ".n1", revert_on_error = True)
    import_basenet(transaction_file = autoFolder + os.sep + "1000" + str(scen) + ".l1", revert_on_error = True)
    import_link_shape(transaction_file = WORK_FOLDER + os.sep + 'linkshape_100.in', revert_on_error = False)
    # import extra attributes and tolls
    node_att = ['@zone', '@atype', '@imarea']
    node_att_def = ['CMAP Zone', 'IM area flag', 'cycle length at node in minutes']
    node_labels={0: 'i_node'}
    for i, att, att_def in zip(range(len(node_att)), node_att, node_att_def):
        create_extra('NODE', att, att_def, 0.0, overwrite = True)
        node_labels[i+1] = att
    import_attrib(file_path = autoFolder + os.sep + '10000.n2',
                    field_separator=' ',
                    column_labels=node_labels,
                    revert_on_error = False)    
    link_att = ['@speed', '@width', '@parkl', '@cltl', '@toll', '@sigic', '@rrx', '@tipid']
    link_att_def = ['posted speed', 'average lane width', 'number of park lanes', '', 'auto toll (dollars)', 
                        'signal interconnect present', '', 'most recently applied tip id']
    link_labels={0: 'i_node', 1: 'j_node'}
    for i, att, att_def in zip(range(len(link_att)), link_att, link_att_def):
        create_extra('LINK', att, att_def, 0.0, overwrite = True)
        link_labels[i+2] = att
    import_attrib(file_path = autoFolder + os.sep + '10000.l2',
                    field_separator=' ',
                    column_labels=link_labels,
                    revert_on_error = False)
run_macro(macro_name = autoFolder + os.sep + "hwy_network_prep.txt")

for scen, per in zip(TSCENS, TPERIODS):
    print("Importing Transit Scenario %s"%scen)
    createScenario(scenario_id = scen, scenario_title = scen_desc[str(scen)], overwrite = True, set_as_primary = True)
    # Import transit modes
    impModes(transaction_file = transitFolder + os.sep + "modes.in", revert_on_error = False)
    # Import transit vehicles
    impVeh(transaction_file = transitFolder + os.sep + "vehicles.in", revert_on_error = False)
    # Import transit net    
    import_basenet(transaction_file = transitFolder + os.sep + 'bus.network_' + str(per), revert_on_error = True)
    import_basenet(transaction_file = transitFolder + os.sep + 'rail.network_' + str(per), revert_on_error = True)
    import_basenet(transaction_file = transitFolder + os.sep + 'access.network_' + str(per), revert_on_error = True)
    import_transit(transaction_file = transitFolder + os.sep + 'rail.itinerary_' + str(per), revert_on_error = True)
    import_transit(transaction_file = transitFolder + os.sep + 'bus.itinerary_' + str(per), revert_on_error = True)   
    import_link_shape(transaction_file = WORK_FOLDER + os.sep + 'linkshape_100.in', revert_on_error = False)
    import_link_shape(transaction_file = transitFolder + os.sep + 'rail.linkshape', revert_on_error = False)
    create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
    create_extra('LINK', '@zfare_link', 'incremental zone fare on links', 0.0, overwrite = True)
    import_attrib(file_path = transitFolder + os.sep + 'zfare_link.csv', 
                    field_separator=',',
                    revert_on_error = False)    
    create_extra('TRANSIT_LINE', '@easeb', 'Ease of boarding 1=worst, 3=best', 0.0, overwrite = True)
    import_attrib(file_path = transitFolder + os.sep + 'boarding_ease_by_line_id.csv', 
                    field_separator=',',
                    column_labels={0: 'line', 
                                    1: '@easeb'},
                    revert_on_error = False)
    #create_extra('TRANSIT_LINE', '@relim', 'Reliability policy impact', 0.0, overwrite = True)
    #import_attrib(file_path = transitFolder + os.sep + 'relim_by_line_id.csv', 
    #                field_separator=',',
    #                column_labels={0: 'line', 
    #                                1: '@relim'},
    #                revert_on_error = False)                    
    rail_node_att = ['@rspac', '@rpcos', '@rstyp', '@rsinf', '@timbo']
    rail_node_att_def = ['parking spaces at rail station', 'rail parking cost at station', 'rail stat type 1-6', 
                        'rail stat info 1=no, 2=yes', 'Base boarding time by station type, min']
    rail_labels={0: 'i_node'}
    for i, att, att_def in zip(range(len(rail_node_att)), rail_node_att, rail_node_att_def):
        create_extra('NODE', att, att_def, 0.0, overwrite = True)
        rail_labels[i+1] = att
    import_attrib(file_path = transitFolder + os.sep + 'rail_node_extra_attributes_100.csv', 
                    field_separator=',',
                    column_labels=rail_labels,
                    revert_on_error = False)
    bus_node_att = ['@bstyp', '@bsinf', '@timbo']
    bus_node_att_def = ['bus stop type 1-6', 'bus stop info 1=no, 2=yes', 'Base boarding time by station type, min']    
    bus_labels={0: 'i_node'}
    for i, att, att_def in zip(range(len(bus_node_att)), bus_node_att, bus_node_att_def):
        create_extra('NODE', att, att_def, 0.0, overwrite = True)
        bus_labels[i+1] = att
    import_attrib(file_path = transitFolder + os.sep + 'bus_node_extra_attributes_100.csv', 
                    field_separator=',',
                    column_labels=bus_labels,
                    revert_on_error = False)
    bus_node_att = ['@pspac', '@pcost']
    bus_node_att_def = ['off-street parking spaces at station', 'avg. daily parking cost at station']
    bus_labels={0: 'i_node'}
    for i, att, att_def in zip(range(len(bus_node_att)), bus_node_att, bus_node_att_def):
        create_extra('NODE', att, att_def, 0.0, overwrite = True)
        bus_labels[i+1] = att
    import_attrib(file_path = transitFolder + os.sep + '17120001_parking.csv', 
                    field_separator=',',
                    column_labels=bus_labels,
                    revert_on_error = False)
    import_attrib(file_path = transitFolder + os.sep + 'cermakbrt_parking.csv',
                    field_separator=',',
                    column_labels=bus_labels,
                    revert_on_error = False)
                     
print("Completed importing scenarios at %s"%(datetime.datetime.now()))