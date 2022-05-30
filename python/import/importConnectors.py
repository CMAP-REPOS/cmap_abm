#
# importConnectors.py
#
# add transit connectors
# if generate_connectors is False (default), only import connectors
# if generate_connectors is True, generate and import connectors (used if there are network changes)
#

import os
import inro.modeller as _m
import inro.emme.desktop.app as _app
#import count_stops
#import count_connectors
generate_connectors = False
TSCENS = [201,202,203,204,205,206,207,208]
#line_haul_modes = ["l", "e"]
line_haul_modes = ["o", "c", "m"] # other (all buses), CTA rail, METRA rail
#line_haul_mode_descr = ["local bus stops", "express bus stops"]
line_haul_mode_descr = ["bus stops", "CTA rail stations", "METRA rail stations"]
line_haul_mode_specs = ["i=5000,29999 and @num_stops_o=1,99","i=30000,39999 and @num_stops_c=1,99",
                        "i=40000,49999 and @num_stops_m=1,99"]
#transit_modes = ["BPL","EQ"] 
transit_modes = ["BEPQL","C","M"]
max_length_wlk = [0.55, 1.2, 1.2]
max_length_knr = [5, 5, 5]
max_length_pnr = [10, 15, 15]
acc_modes = ["uvw", "uw", "vw", "u", "v", "w"]

WORK_FOLDER = "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_inputs\\netfiles"
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

'''
# to be removed: export nodes to generate stop_attributes.csv
for scen in TSCENS: 
    change_scenario(scenario=scen)
    eFolder = WORK_FOLDER+os.sep+"scen"+str(scen)

    # delete regular nodes not connected to links so connectors won't be drawn to unconnected nodes
    #delete_nodes(selection = "ci=0 and @pspac=0", condition="ignore")

    for i in range(len(line_haul_modes)):
        # count number of stops at each node by line haul mode
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
    basenet_file = os.path.join(emmebank_dir, "%s_nodes.csv" % scen)
    export_basenet(selection = {"link": 'none',
                                "node": 'i=1,99999'},
                export_file = basenet_file,
                field_separator = ",")
    #export_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.export_extra_attributes")
    #export_extra(extra_attributes="NODE",field_separator=",")
'''

for scen in TSCENS: 
    change_scenario(scenario=scen)
    eFolder = WORK_FOLDER+os.sep+"scen"+str(scen)
    if generate_connectors:
        
        # delete regular nodes not connected to links so connectors won't be drawn to unconnected nodes
        #delete_nodes(selection = "ci=0 and @pspac=0", condition="ignore")

        for i in range(len(line_haul_modes)):
            # count number of stops at each node by line haul mode
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

            # create connectors for each access and line haul mode and export connectors
            create_connectors(access_modes=["u"],
                            egress_modes=["x"],
                            delete_existing=True,
                            selection={
                                "centroid":"all",
                                "node": "%s" % line_haul_mode_specs[i],
                                "only_midblock_nodes": False,
                                "link": "none",
                                "exclude_split_links": True},
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
                                "only_midblock_nodes": False,
                                "link": "none",
                                "exclude_split_links": True},
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
                                "only_midblock_nodes": False,
                                "link": "none",
                                "exclude_split_links": True},
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
                                "only_midblock_nodes": False,
                                "link": "none",
                                "exclude_split_links": True},
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
                                "only_midblock_nodes": False,
                                "link": "none",
                                "exclude_split_links": True},
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
                                "only_midblock_nodes": False,
                                "link": "none",
                                "exclude_split_links": True},
                            max_length=max_length_wlk[i],
                            max_connectors=2,
                            min_angle=0)
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = eFolder + os.sep + str(scen) + "connectors_uvw" + line_haul_modes[i] + ".out",
                        field_separator = " ")                                       
            
        # import connectors; if a connector already exists, it is skipped because the connector with the most access modes is imported first
        for line_haul in line_haul_modes:
            for acc in acc_modes:
                #print(eFolder + os.sep + str(scen) + "connectors_" + acc + line_haul + ".out")
                import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_" + acc + line_haul + ".out", revert_on_error = False)
                #import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_uvw" + line_haul_modes[i] + ".out", revert_on_error = False)
                #import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_uw" + line_haul_modes[i] + ".out", revert_on_error = False)
                #import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_vw" + line_haul_modes[i] + ".out", revert_on_error = False)
                #import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_u" + line_haul_modes[i] + ".out", revert_on_error = False)
                #import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_v" + line_haul_modes[i] + ".out", revert_on_error = False)
                #import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors_w" + line_haul_modes[i] + ".out", revert_on_error = False)
        # export all onnectors and delete individial connector files by transit mode and access mode

        export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                    "node": 'none'},
                    export_file = eFolder + os.sep + str(scen) + "connectors.out",
                    field_separator = " ")

        for line_haul in line_haul_modes:
            for acc in acc_modes:
                try:
                    os.remove(eFolder + os.sep + str(scen) + "connectors_" + acc + line_haul + ".out")
                except:
                    pass
        
    import_basenet(transaction_file = eFolder + os.sep + str(scen) + "connectors.out", revert_on_error = False)
    '''
    # to be removed: update zonal fare links to 2019 adult metra fare
    create_extra(extra_attribute_type="LINK",
                        extra_attribute_name="@zfare_link",
                        extra_attribute_description="incremental zone fare on links",
                        overwrite=False)
    spec1={
        "result": "@zfare_link",
        "expression": "@zfare",
        "aggregation": ".max.",
        "selections": {
            "link": "all",
            "transit_line": "all"
        },
        "type": "NETWORK_CALCULATION"
    }
    netcalc(spec1)
    spec1={
        "result": "@zfare_link",
        "expression": "25",
        "selections": {
            "link": "@zfare_link=18.12,18.14"
        },
        "type": "NETWORK_CALCULATION"
    }
    spec2={
        "result": "@zfare_link",
        "expression": "50",
        "selections": {
            "link": "@zfare_link=36.25"
        },
        "type": "NETWORK_CALCULATION"
    }
    spec3={
        "result": "@zfare_link",
        "expression": "75",
        "selections": {
            "link": "@zfare_link=54.38"
        },
        "type": "NETWORK_CALCULATION"
    }
    spec4={
        "result": "@zfare_link",
        "expression": "100",
        "selections": {
            "link": "@zfare_link=72.5"
        },
        "type": "NETWORK_CALCULATION"
    }
    spec5={
        "result": "@zfare_link",
        "expression": "125",
        "selections": {
            "link": "@zfare_link=90.63"
        },
        "type": "NETWORK_CALCULATION"
    }         
    netcalc([spec1,spec2,spec3,spec4,spec5]) 
    spec1={
        "result": "ut1",
        "expression": "400",
        "selections": {
            "transit_line": "line=mss___"
        },
        "type": "NETWORK_CALCULATION"
    }
    netcalc(spec1)

    spec1={
        "result": "@zfare",
        "expression": "@zfare_link",
        "selections": {
            "link": "all",
            "transit_line": "all"
        },
        "type": "NETWORK_CALCULATION"
    }
    netcalc(spec1)    

    export_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.export_extra_attributes")
    export_extra(extra_attributes = ["LINK", "TRANSIT_SEGMENT"], field_separator=" ", export_definitions="True")
    '''
print("Finished adding access and egress links")