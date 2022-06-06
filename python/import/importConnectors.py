#
# importConnectors.py
#
# import transit connectors
# if generate_connectors is False (default), only import connectors
# if generate_connectors is True, generate and import connectors (used if there are network changes)
#

import os
import inro.modeller as _m
import inro.emme.desktop.app as _app

generate_connectors = False

TSCENS = [201,202,203,204,205,206,207,208]
line_haul_modes_bus = ["bl", "be"] # bus local and bus express
line_haul_mode_bus_descr = ["local bus stops", "express bus stops"]
transit_modes_bus = ["BPL","EQ"] 
line_haul_modes = ["b", "c", "m"] # all buses (local + express), CTA rail, METRA rail
line_haul_mode_descr = ["bus stops", "CTA rail stations", "METRA rail stations"]
transit_modes = ["BEPQL","C","M"]
line_haul_mode_specs = ["i=5000,29999 and @num_stops_b=1,99","i=30000,39999 and @num_stops_c=1,99",
                        "i=40000,49999 and @num_stops_m=1,99"]
max_length_wlk = [0.55, 1.2, 1.2] # length in miles
max_length_knr = [5, 5, 5]
max_length_pnr = [10, 15, 15]
acc_modes = ["uvw", "uw", "vw", "u", "v", "w"]

WORK_FOLDER = os.environ["BASE_PATH"] + os.sep + "emme_inputs\\netfiles"
PROJECT = os.environ["EMMEBANK"]

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="TL")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank
emmebank_dir = os.path.dirname(_m.Modeller().emmebank.path)

create_connectors = _m.Modeller().tool("inro.emme.data.network.base.create_connectors")
change_scenario = _m.Modeller().tool("inro.emme.data.scenario.change_primary_scenario")
#delete_nodes = _m.Modeller().tool("inro.emme.data.network.base.delete_nodes")
create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")
export_basenet = _m.Modeller().tool("inro.emme.data.network.base.export_base_network")
import_basenet = _m.Modeller().tool("inro.emme.data.network.base.base_network_transaction")

for scen in TSCENS: 
    change_scenario(scenario=scen)
    eFolder = WORK_FOLDER+os.sep+"scen"+str(scen)

    if generate_connectors:
        
        # delete regular nodes not connected to links so connectors won't be drawn to unconnected nodes
        #delete_nodes(selection = "ci=0 and @pspac=0", condition="ignore")

        for i in range(len(line_haul_modes_bus)):
            # count number of stops at each node by bus mode (local and express)
            create_extra(extra_attribute_type="NODE",
                                extra_attribute_name="@num_stops_%s" % line_haul_modes_bus[i],
                                extra_attribute_description="number of %s" % line_haul_mode_bus_descr[i],
                                overwrite=True)
            spec1={
                "result": "@num_stops_%s" % line_haul_modes_bus[i],
                "expression": "(noboa==0).or.(noali==0)",
                "aggregation": "+",
                "selections": {
                    "link": "all",
                    "transit_line": "mode=%s" % transit_modes_bus[i]
                },
                "type": "NETWORK_CALCULATION"
            }
            spec2={
                "result": "@num_stops_%sj" % line_haul_modes_bus[i],
                "expression": "(noboan==0).or.(noalin==0)",
                "aggregation": "+",
                "selections": {
                    "link": "all",
                    "transit_line": "mode=%s" % transit_modes_bus[i]
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc([spec1,spec2])

        # calcuate total number of bus stops (local + express)
        create_extra(extra_attribute_type="NODE",
                            extra_attribute_name="@num_stops_b",
                            extra_attribute_description="number of bus stops",
                            overwrite=True)
        spec1={
            "result": "@num_stops_b",
            "expression": "@num_stops_bl + @num_stops_be",
            "type": "NETWORK_CALCULATION",
            "selections" : {
                    "node" : "all"}
        }
        netcalc(spec1)

        for i in range(1,len(line_haul_modes)):
            # count number of stops at each node by rail mode (CTA and Metra)
            create_extra(extra_attribute_type="NODE",
                                extra_attribute_name="@num_stops_%s" % line_haul_modes[i],
                                extra_attribute_description="number of %s" % line_haul_mode_descr[i],
                                overwrite=True)
            spec1={
                "result": "@num_stops_%s" % line_haul_modes[i],
                "expression": "(noboa==0).or.(noali==0)",
                "aggregation": "+",
                "selections": {
                    "link": "all",
                    "transit_line": "mode=%s" % transit_modes[i]
                },
                "type": "NETWORK_CALCULATION"
            }
            spec2={
                "result": "@num_stops_%sj" % line_haul_modes[i],
                "expression": "(noboan==0).or.(noalin==0)",
                "aggregation": "+",
                "selections": {
                    "link": "all",
                    "transit_line": "mode=%s" % transit_modes[i]
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc([spec1,spec2])       

        for i in range(len(line_haul_modes)):
            # create connectors for each access and line haul mode and export connectors
            create_connectors(access_modes=["u"],
                            egress_modes=["x"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False},
                            max_length=max_length_wlk[i],
                            max_connectors=10,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_u" + line_haul_modes[i] + ".out",
                        field_separator = " ")
            create_connectors(access_modes=["v"],
                            egress_modes=["y"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s and @pspac=1,5000" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False},
                            max_length=max_length_pnr[i],
                            max_connectors=2,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_v" + line_haul_modes[i] + ".out",
                        field_separator = " ")                     
            create_connectors(access_modes=["w"],
                            egress_modes=["z"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False},
                            max_length=max_length_knr[i],
                            max_connectors=2,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_w" + line_haul_modes[i] + ".out",
                        field_separator = " ")                     
            create_connectors(access_modes=["u", "w"],
                            egress_modes=["x", "z"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False},
                            max_length=max_length_wlk[i],
                            max_connectors=2,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_uw" + line_haul_modes[i] + ".out",
                        field_separator = " ")                       
            create_connectors(access_modes=["v", "w"],
                            egress_modes=["y", "z"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s and @pspac=1,5000" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False},
                            max_length=max_length_knr[i],
                            max_connectors=2,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_vw" + line_haul_modes[i] + ".out",
                        field_separator = " ")     
            create_connectors(access_modes=["u", "v", "w"],
                            egress_modes=["x", "y", "z"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s and @pspac=1,5000" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False},
                            max_length=max_length_wlk[i],
                            max_connectors=2,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_uvw" + line_haul_modes[i] + ".out",
                        field_separator = " ")                                       
            
        # import connectors; if a connector already exists, it's skipped because the connector with the most access modes is imported first
        for line_haul in line_haul_modes:
            for acc in acc_modes:
                import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_" + acc + line_haul + ".out", revert_on_error = False)
        # export all onnectors
        export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                    "node": 'none'},
                    export_file = eFolder + os.sep + str(scen) + "connectors.out",
                    field_separator = " ")
        # delete individial connector files by transit mode and access mode
        for line_haul in line_haul_modes:
            for acc in acc_modes:
                try:
                    os.remove(eFolder + os.sep + str(scen) + "connectors_" + acc + line_haul + ".out")
                except:
                    pass
        
    import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors.out", revert_on_error = False)
print("Finished adding access and egress links")