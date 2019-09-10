'''
ABM Extended Transit Assignment

Description: Extended Transit Assignment with journey levels
             References transit_assignment_skimming_CT_RAMP3.mac

Created by: Brittaney Ross
Last Updated: 5/15/2018

'''
#import libraries
import os
import sys
import inro
import inro.emme.desktop.app as _app
import inro.modeller as _m
import pandas as pd
import numpy as np
import copy
import inro.emme.desktop.worksheet as _worksheet

#Connect to Emme Desktop
#EMME project file
directory = os.getcwd().replace('\\Database','')
empFile = os.path.join(directory,"CMAP-ABM.emp")
my_app = _app.start_dedicated(False, "cmap", empFile)
my_modeller = _m.Modeller(my_app)
my_emmebank = my_modeller.emmebank
data_explorer = my_app.data_explorer()
database = data_explorer.active_database()
#---------------------------------------------------------------------------------------------------------
netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")
compute_matrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")
create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
assign_transit = _m.Modeller().tool("inro.emme.transit_assignment.extended_transit_assignment")
mat_results = _m.Modeller().tool("inro.emme.transit_assignment.extended.matrix_results")
strategy = _m.Modeller().tool("inro.emme.transit_assignment.extended.strategy_based_analysis")
create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
import_values = _m.Modeller().tool("inro.emme.data.network.import_attribute_values")
default_path = os.path.dirname(my_emmebank.path).replace("\\","/")
file_path = os.path.join(default_path,"abm_ringsectors_by_zone09.in").replace("\\","/")
#----------------------- ----------------------------------------------------------------------------------
#access macro arguments
#placed within transit_assignment_skimming_CT_RAMP3.mac
#Premium
#~<run_modeller ../../scripts/abm_extended_transit_v7.py %r206% mf%r203% %rz% %y% %s% %2% %r250% %r44% %r45% %x% %r200%

vol = int(sys.argv[1])
dmd_matrix = sys.argv[2]
cost_percep = sys.argv[3]
userClass = sys.argv[4]
scen = int(sys.argv[5])
todPeriod = sys.argv[6]
matNum = int(sys.argv[7])
initialBoarding = sys.argv[8]
defaultTransfer = sys.argv[9]
iteration = int(sys.argv[10])
iterations = int(sys.argv[11])
initial = int(sys.argv[12])
#---------------------------------------------------------------------------------------------------------
modes = ["B","C","P","L","u","v","x","y","b","c","r","E","Q","M","m","w","z","t","d","a","e"]

if int(userClass) == 1:
    board_cost_per = float(cost_percep)
elif int(userClass) == 2:
    board_cost_per = float(cost_percep)
elif int(userClass) == 3:
    board_cost_per = float(cost_percep)

journeyLevels =  [
                    {   #Never Boarded 0
                        "description": "Never Boarded",
                        "destinations_reachable": True,
                        "transition_rules": [
                            {
                                "mode": "B",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "C",
                                "next_journey_level": 2
                            },
                            {
                                "mode": "E",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "L",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "M",
                                "next_journey_level": 1
                            },
                            {
                                "mode": "P",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "Q",
                                "next_journey_level": 4
                            }
                        ],
                        "boarding_time": {
                            "at_nodes": {
                                "penalty": "@timbf",
                                "perception_factor": "@perbf"
                            },
                            "on_lines": {
                                    "penalty": "@easbp",
                                    "perception_factor": 1
                                },
                            "on_segments": None
                        },
                        "boarding_cost": {
                            "global": None,
                            "at_nodes": None,
                            "on_lines": {
                                "penalty": "ut1",
                                "perception_factor": board_cost_per
                            }
                        },
                        "waiting_time": {
                            "headway_fraction": .5,
                            "effective_headways": "@hdwef",
                            "spread_factor": 1,
                            "perception_factor": "@wconf"
                        }
                    },
                    {   #Metra Only 1
                        "description": "Metra Only",
                        "destinations_reachable": True,
                        "transition_rules": [
                            {
                                "mode": "B",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "C",
                                "next_journey_level": 2
                            },
                            {
                                "mode": "E",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "L",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "M",
                                "next_journey_level": 1
                            },
                            {
                                "mode": "P",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "Q",
                                "next_journey_level": 4
                            }
                        ],
                        "boarding_time": {
                            "at_nodes": {
                                "penalty": "@timbf",
                                "perception_factor": "@perbf"
                            },
                            "on_lines": {
                                    "penalty": "@easbp",
                                    "perception_factor": 1
                                },
                            "on_segments": None
                        },
                        "boarding_cost": {
                            "global": None,
                            "at_nodes": None,
                            "on_lines": {
                                "penalty": "ut1",
                                "perception_factor": 1
                            }
                        },
                        "waiting_time": {
                            "headway_fraction": .5,
                            "effective_headways": "@hdwef",
                            "spread_factor": 5,
                            "perception_factor": "@wconf"
                        }
                    },
                    {   #CTA Rail 2
                        "description": "CTA, no Pace",
                        "destinations_reachable": True,
                        "transition_rules": [
                            {
                                "mode": "B",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "C",
                                "next_journey_level": 2
                            },
                            {
                                "mode": "E",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "L",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "M",
                                "next_journey_level": 2
                            },
                            {
                                "mode": "P",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "Q",
                                "next_journey_level": 5
                            }
                        ],
                        "boarding_time": {
                            "at_nodes": {
                                "penalty": "@timbf",
                                "perception_factor": "@perbf"
                            },
                            "on_lines": {
                                    "penalty": "@easbp",
                                    "perception_factor": 1
                                },
                            "on_segments": None
                        },
                        "boarding_cost": {
                            "global": {
                                "penalty": 25,
                                "perception_factor": 1.25
                            },
                            "at_nodes": None,
                            "on_lines": None,
                        },
                        "waiting_time": {
                            "headway_fraction": 1,
                            "effective_headways": "@hdwef",
                            #Reduction in spread factor to reduce options
                            "spread_factor": 1,
                            "perception_factor": "@wconf"
                        }
                    },
                    {   #CTA, No Pace 3
                        "description": "CTA, no Pace",
                        "destinations_reachable": True,
                        "transition_rules": [
                            {
                                "mode": "B",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "C",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "E",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "L",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "M",
                                "next_journey_level": 3
                            },
                            {
                                "mode": "P",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "Q",
                                "next_journey_level": 5
                            }
                        ],
                        "boarding_time": {
                            "at_nodes": {
                                "penalty": "@timbf",
                                "perception_factor": "@perbf"
                            },
                            "on_lines": {
                                    "penalty": "@easbp",
                                    "perception_factor": 1
                                },
                            "on_segments": None
                        },
                        "boarding_cost": {
                            "global": {
                                "penalty": 25,
                                "perception_factor": 1
                            },
                            "at_nodes": None,
                            "on_lines": None,
                        },
                        "waiting_time": {
                            "headway_fraction": 1,
                            "effective_headways": "@hdwef",
                            #Reduction in spread factor to reduce options
                            "spread_factor": 1,
                            "perception_factor": "@wconf"
                        }
                    },
                    {   #Pace No CTA 4
                        "description": "Pace, no CTA",
                        "destinations_reachable": True,
                        "transition_rules": [
                            {
                                "mode": "B",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "C",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "E",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "L",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "M",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "P",
                                "next_journey_level": 4
                            },
                            {
                                "mode": "Q",
                                "next_journey_level": 4
                            }
                        ],
                        "boarding_time": {
                            "at_nodes": {
                                "penalty": "@timbf",
                                "perception_factor": "@perbf"
                            },
                            "on_lines": {
                                    "penalty": "@easbp",
                                    "perception_factor": 1
                                },
                            "on_segments": None
                        },
                        "boarding_cost": {
                            "global": None,
                            "at_nodes": None,
                            "on_lines": {
                                "penalty": "ut1",
                                "perception_factor": board_cost_per
                            }
                        },
                        "waiting_time": {
                            "headway_fraction": 1,
                            "effective_headways": "@hdwef",
                            #Increase Spread Factor (Transit Options)
                            "spread_factor": 1,
                            "perception_factor": "@wconf"
                        }
                    },
                    {   #CTA and Pace 5
                        "description": "Boarded CTA and Pace",
                        "destinations_reachable": True,
                        "transition_rules": [
                            {
                                "mode": "B",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "C",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "E",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "L",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "M",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "P",
                                "next_journey_level": 5
                            },
                            {
                                "mode": "Q",
                                "next_journey_level": 5
                            }
                        ],
                        "boarding_time": {
                            "at_nodes": {
                                "penalty": "@timbf",
                                "perception_factor": "@perbf"
                            },
                            "on_lines": {
                                    "penalty": "@easbp",
                                    "perception_factor": 1
                                },
                            "on_segments": None
                        },
                        "boarding_cost": {
                            "global": None,
                            "at_nodes": None,
                            "on_lines": {
                                "penalty": "ut1",
                                "perception_factor": board_cost_per
                            }
                        },
                        "waiting_time": {
                            "headway_fraction": 1,
                            "effective_headways": "@hdwef",
                            "spread_factor": 1,
                            "perception_factor": "@wconf"
                        }
                    },
                ]
#---------------------------------------------------------------------------------------------------------
def transit_volsboard(uc):
    if uc == 1:
        pass_ = {
            "type": "NETWORK_CALCULATION",
            "expression": "voltr",
            "result" : "@pass"+str(uc),
            "selections": {
                "link": "all",
                "transit_line": "all"
                    }
                }
        boar = {
            "type": "NETWORK_CALCULATION",
            "expression": "board",
            "result" : "@boar"+str(uc),
            "selections": {
                "link": "all",
                "transit_line": "all"
                    }
                }
    elif uc == 2:
        pass_ = {
            "type": "NETWORK_CALCULATION",
            "expression": "voltr-@pass"+str(1),
            "result" : "@pass"+str(uc),
            "selections": {
                "link": "all",
                "transit_line": "all"
                    }
                }
        boar = {
            "type": "NETWORK_CALCULATION",
            "expression": "board-@boar"+str(1),
            "result" : "@boar"+str(uc),
            "selections": {
                "link": "all",
                "transit_line": "all"
                    }
                }
    elif uc == 3:
        pass_ = {
            "type": "NETWORK_CALCULATION",
            "expression": "voltr-@pass"+str(1) + "-@pass"+str(2),
            "result" : "@pass"+str(uc),
            "selections": {
                "link": "all",
                "transit_line": "all"
                    }
                }
        boar = {
            "type": "NETWORK_CALCULATION",
            "expression": "board-@boar"+str(1) + "-@boar"+str(2),
            "result" : "@boar"+str(uc),
            "selections": {
                "link": "all",
                "transit_line": "all"
                    }
                }
    report = netcalc([pass_,boar])
#---------------------------------------------------------------------------------------------------------
#collect values for extended transit assignment

current_scenario = my_emmebank.scenario(scen)
tranScen = database.scenario_by_number(scen)
data_explorer.replace_primary_scenario(tranScen)

segCostPerception = create_extra(extra_attribute_type = "TRANSIT_SEGMENT",
                        extra_attribute_name="@boardp%sm" % (userClass),
                        extra_attribute_description="board cost percep",
                        overwrite=True)

segCostP_calc =  {
        "result": "@boardp%sm" % (userClass),
        "expression": cost_percep,
        "aggregation": None,
        "selections": {
            "link": "all",
            "transit_line": "all"
        },
        "type": "NETWORK_CALCULATION"
    }
report = netcalc(segCostP_calc)

#Pace Bus
Pace_InVeh =  {
        "result": "@ivtf%s" % (userClass),
        "expression": "@ivtf%s" % (userClass) + "*8",
        "aggregation": None,
        "selections": {
            "link": "all",
            "transit_line": "line=p_____ | q_____ "
        },
        "type": "NETWORK_CALCULATION"
    }

metra_InVeh =  {
        "result": "@ivtf%s" % (userClass),
        "expression": "@ivtf%s" % (userClass) + "*4.5",
        "aggregation": None,
        "selections": {
            "link": "@zone=89,309",
            "transit_line": "line=m_____"
        },
        "type": "NETWORK_CALCULATION"
    }

metra_InVeh_noCook =  {
        "result": "@ivtf%s" % (userClass),
        "expression": "@ivtf%s" % (userClass) + "*.10",
        "aggregation": None,
        "selections": {
            "link": "@zone=310,1711",
            "transit_line": "line=m_____"
        },
        "type": "NETWORK_CALCULATION"
    }

report = netcalc([Pace_InVeh, metra_InVeh,metra_InVeh_noCook])

metra_electric =  {
        "result": "@ivtf%s" % (userClass),
        "expression": "@ivtf%s" % (userClass) + "*1.4",
        "aggregation": None,
        "selections": {
            "link": "all",
            "transit_line": "line=mme___"
        },
        "type": "NETWORK_CALCULATION"
    }
metra_hc =  {
        "result": "@ivtf%s" % (userClass),
        "expression": "@ivtf%s" % (userClass) + "*5",
        "aggregation": None,
        "selections": {
            "link": "all",
            "transit_line": "line=mh____"
        },
        "type": "NETWORK_CALCULATION"
    }

cta_red =  {
        "result": "@ivtf%s" % (userClass),
        "expression": "@ivtf%s" % (userClass) + "*.3",
        "aggregation": None,
        "selections": {
            "link": "all",
            "transit_line": "line=cr____"
        },
        "type": "NETWORK_CALCULATION"
    }

report = netcalc([metra_electric, cta_red,metra_hc ])

if initial > 0:
    #Pace Bus
    Pace_InVeh =  {
            "result": "@ivtf%s" % (userClass),
            "expression": "@ivtf%s" % (userClass) + "*10",
            "aggregation": None,
            "selections": {
                "link": "all",
                "transit_line": "line=p_____ | q_____ "
            },
            "type": "NETWORK_CALCULATION"
        }

    report = netcalc([Pace_InVeh])



#---------------------------------------------------------------------------------------------------------
spec = {
    "modes": modes,
    "demand": None,
    "waiting_time": {
        #Headway fraction (already accounted in effective headway)
        "headway_fraction": 1,
        #Effective headway multiplied by the fraction to get wait time
        "effective_headways": "@hdwef",
        #Spread factor (mutiplied on perception factor)
        "spread_factor": 1,
        #Wait time perception factor
        "perception_factor": "@wconf"
    },
    "boarding_time": {
        "at_nodes": {
            "penalty": "@timbf",
            "perception_factor": "@perbf"
        },
        "on_lines": {
            "penalty":"@easbp",
            "perception_factor": 1
        },
        "on_segments": None
    },
    "boarding_cost": {
        "global": None,
        "at_nodes": None,
        "on_lines": {
            "penalty": "ut1",
            "perception_factor": None
        },
        "on_segments": None,
    },
    "in_vehicle_time": {
        "perception_factor": None
    },
    "in_vehicle_cost": {
        "penalty":"@zfare",
        "perception_factor": None
    },
    "aux_transit_time": {
        "perception_factor": "@pefli"
    },
    "aux_transit_cost": {
        "penalty": "ul1",
        "perception_factor": 0
    },
    #Distribute flow between connectors - transit time (logit)
    "flow_distribution_at_origins": {
        "choices_at_origins": {
            "choice_points": "ALL_ORIGINS",
            "choice_set": "ALL_CONNECTORS",
            "logit_parameters": {
                "scale": 0.2,
                "truncation": 0.05
            }
        },
        "fixed_proportions_on_connectors": None
    },
    "flow_distribution_at_regular_nodes_with_aux_transit_choices": {
        "choices_at_regular_nodes":"OPTIMAL_STRATEGY"
    },
    #Distribute flow between attractive lines at stops by frequency and transit time to destination
    "flow_distribution_between_lines": {
        "consider_total_impedance": True
    },
    #Handle connector-to-connector path - prohibit
    "connector_to_connector_path_prohibition": {
        #Prohibit connector-to-connector path everwhere
        "at_nodes": "ALL",
        #assign to another path
        "reassign_demand_to_alternate_path" : True
        },
    "od_results": None,
    "journey_levels": journeyLevels,
    "performance_settings": {
        "number_of_processors": "max"
    },
    "type": "EXTENDED_TRANSIT_ASSIGNMENT"
}

sys.stdout.write("       Extended Transit Assignment User Class %s\n" % (userClass))
sys.stdout.flush()

spec_class = copy.deepcopy(spec)
spec_class["demand"] = dmd_matrix
spec_class["in_vehicle_cost"]["perception_factor"] = "@boardp%sm" % (userClass)
spec_class["boarding_cost"]["on_lines"]["perception_factor"] = float(cost_percep)
spec_class["in_vehicle_time"]["perception_factor"] = "@ivtf%s" % (userClass)

if int(userClass) > 1:
    saveScenario = True
else:
    saveScenario = False

report_assignment_slice = assign_transit(
                            specification=spec_class,
                            save_strategies=True,
                            scenario=current_scenario,
                            add_volumes=saveScenario,
                            class_name="EXTs%i_%s" % (scen,userClass))

if iteration == iterations:
    sys.stdout.write("        Extended Transit Analysis User Class %s\n" % (userClass))
    sys.stdout.flush()

    className = "EXTs%i_%s" % (scen,userClass)
#---------------------------------------------------------------------------------------------------------
    
    sys.stdout.write("    Matrix to hold total transit impedance for P&R convolution \n")
    sys.stdout.flush()
    Pgen = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pgen%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Gencost_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pgen = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "total_impedance": "mf%i" % (matNum),
        "actual_first_waiting_times": None,
        "actual_total_waiting_times": None,
    }
    report = mat_results(spec_Pgen,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold in-vehicle time (total) \n")
    sys.stdout.flush()
    Pivt = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pivt%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pivt = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": modes,
            "actual_in_vehicle_times": "mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pivt,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold auxiliary time \n")
    sys.stdout.flush()
    Pwlk = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pwlk%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Walk_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pwlk = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": modes,
            "actual_aux_transit_times": "mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pwlk,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold first waiting time \n")
    sys.stdout.flush()
    Pwa1 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pwa1%s%s" % (todPeriod,userClass),
            matrix_description="Prem_1Wait_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pwa1 = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "actual_first_waiting_times": "mf%i" % (matNum),
    }
    report = mat_results(spec_Pwa1,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold second waiting time (total wait initially, then first wait subtracted) \n")
    sys.stdout.flush()
    Pwa2 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pwa2%s%s" % (todPeriod,userClass),
            matrix_description="Prem_2Wait_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pwa2 = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "actual_total_waiting_times": "mf%i" % (matNum)
    }
    report = mat_results(spec_Pwa2,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold number of boardings \n")
    sys.stdout.flush()
    Pboa = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pboa%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pboa = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": modes,
            "avg_boardings":"mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pboa,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold initial boarding fares \n")
    sys.stdout.flush()
    Pifa = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pifa%s%s" % (todPeriod,userClass),
            matrix_description="Prem_InFa_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pifa = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": modes,
            "actual_first_boarding_costs": "mf%i" % (matNum),
        }
    }
    report = mat_results(spec_Pifa,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold in-vehicle cost (incremental zone transit fares) \n")
    sys.stdout.flush()
    Pzfa = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pzfa%s%s" % (todPeriod,userClass),
            matrix_description="Prem_ZoFa_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pzfa = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": modes,
            "actual_in_vehicle_costs": "mf%i" % (matNum),
        }
    }
    report = mat_results(spec_Pzfa,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Matrix to hold auxiliary cost (transfer link fare discounts) \n")
    sys.stdout.flush()
    Ptdi = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Ptdi%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Disc_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Ptdi = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": modes,
            "actual_aux_transit_costs": "mf%i" % (matNum),
        }
    }
    report = mat_results(spec_Ptdi,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Skims of other user-defined attributes that include all conventional modes \n")
    sys.stdout.flush()
    Pivs = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pivs%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_mult_Seat_Prob_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pivs = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": "@ivtsp",
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pivs,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Piv1 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Piv1%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_mult_Prop1_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Piv1 = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": "@ivtc1",
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Piv1,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Piv2 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Piv2%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_mult_Prop2_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Piv2 = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": "@ivtc2",
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Piv2,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Piv3 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Piv3%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_mult_Prop3_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Piv3 = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": "@ivtc3",
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Piv3,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Picl = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Picl%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_mult_Clean_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Picl = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": "@ivtcl",
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Picl,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pipr = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pipr%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVT_mult_Prod_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pipr = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": "@ivpr%s" % (userClass),
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pipr,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pbpr = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pbpr%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Seat_Prob_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pbpr = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@stpro",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pbpr,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Peb1 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Peb1%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Ease1_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Peb1 = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@easb1",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Peb1,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Peb2 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Peb2%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Ease2_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Peb2 = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@easb2",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Peb2,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Peb3 = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Peb3%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Ease3_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Peb3 = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@easb3",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Peb3,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Phmi = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Phmi%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Headway_Min_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Phmi = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@hdway",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Phmi,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Phma = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Phma%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Headway_Max_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Phma = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@hdway",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": ".max.",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Phma,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pbtr = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pbtr%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Trav_Max_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pbtr = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@timbo",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pbtr,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Patr = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Patr%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Aligh_Trav_Max_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Patr = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": None,
            "alighting": "@timbo",
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Patr,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pewt = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pewt%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Extra_Wait_Max_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pewt = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@eavwt",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pewt,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pbcl = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pbcl%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Board_Stat_Clean_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pbcl = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": "@clnst",
            "in_vehicle": None,
            "alighting": None,
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pbcl,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pacl = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pacl%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Alight_Stat_Clean_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pacl = {
        "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS",
        "trip_components": {
            "boarding": None,
            "in_vehicle": None,
            "alighting": "@clnst",
            "aux_transit": None,
        },
        "sub_path_combination_operator": "+",
        "sub_strategy_combination_operator": "average",
        "selected_demand_and_transit_volumes": {
            "sub_strategies_to_retain": "ALL",
            "selection_threshold": {
                "lower": 0,
                "upper": 999999
            }
        },
        "analyzed_demand": "mf%i" % (matNum),
        "constraint": None,
        "results": {
            "strategy_values": None,
            "selected_demand": None,
            "transit_volumes": None,
            "aux_transit_volumes": None,
            "total_boardings": None,
            "total_alightings": None
        },
    }
    report = strategy(spec_Pacl,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    In-vehicle time and number of boardings for bus modes only \n")
    sys.stdout.flush()
    Pivb = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pivb%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVTb_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    busModes = ["B","P","L","E","Q"]
    spec_Pivb = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": busModes,
            "actual_in_vehicle_times": "mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pivb,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    Pbob = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pbob%s%s" % (todPeriod,userClass),
            matrix_description="Prem_Boab_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    spec_Pbob = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": busModes,
            "avg_boardings":"mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pbob,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    In-vehicle time for Premium (Express bus and Metra rail) \n")
    sys.stdout.flush()
    Pivp = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pivp%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVTp_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    premModes = ["M","E","Q"]
    spec_Pivp = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": premModes,
            "actual_in_vehicle_times": "mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pivp,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    In-vehicle time for Local bus \n")
    sys.stdout.flush()
    Pivl = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pivl%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVTlb_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    localModes = ["B","P","L"]
    spec_Pivl = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": localModes,
            "actual_in_vehicle_times": "mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pivl,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    In-vehicle time for CTA train \n")
    sys.stdout.flush()
    Pivc = create_matrix(
            matrix_id="mf%i" % (matNum),
            matrix_name="Pivc%s%s" % (todPeriod,userClass),
            matrix_description="Prem_IVTc_Per%s_Class%s" % (todPeriod,userClass),
            default_value="0",
            overwrite=True,
            )
    ctaRailMode = ["C"]
    spec_Pivc = {
        "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
        "by_mode_subset": {
            "modes": ctaRailMode,
            "actual_in_vehicle_times": "mf%i" % (matNum)
        }
    }
    report = mat_results(spec_Pivc,class_name=className)
    matNum+=1
#---------------------------------------------------------------------------------------------------------
    sys.stdout.write("    Cleaning skim matrices of infinite values where transit is not available - User Class %s\n" % (userClass))
    sys.stdout.flush()
    
    calc_Pgen = {
        "type": "MATRIX_CALCULATION",
        "result": Pgen.id,
        "expression": "999",
        "constraint": {
            "by_zone": None,
            "by_value": {
                "interval_min": 0.001,
                "interval_max": 9999,
                "condition": "EXCLUDE",
                "od_values": Pgen.id
                }
            },
        "aggregation": {
            "origins" : None,
            "destinations": None
            }
        }
    report = compute_matrix(calc_Pgen)
#---------------------------------------------------------------------------------------------------------
    calc_Pivt = {
        "type": "MATRIX_CALCULATION",
        "result": Pivt.id,
        "expression": "0",
        "constraint": {
            "by_zone": None,
            "by_value": {
                "interval_min": 0,
                "interval_max": 9999,
                "condition": "EXCLUDE",
                "od_values": Pivt.id
                }
            },
        "aggregation": {
            "origins" : None,
            "destinations": None
            }
        }
    report = compute_matrix(calc_Pivt)
#---------------------------------------------------------------------------------------------------------
    calc_Pgen2 = {
        "type": "MATRIX_CALCULATION",
        "result": Pgen.id,
        "expression": "999",
        "constraint": {
            "by_zone": None,
            "by_value": {
                "interval_min": 0,
                "interval_max": 0,
                "condition": "INCLUDE",
                "od_values": Pivt.id
                }
            },
        "aggregation": {
            "origins" : None,
            "destinations": None
            }
        }
    report = compute_matrix(calc_Pgen2)
#---------------------------------------------------------------------------------------------------------
 
    for mat in [Pwlk,Pwa1,Pwa2]:
        calc_Pwlk = {
            "type": "MATRIX_CALCULATION",
            "result": mat.id,
            "expression": "0",
            "constraint": {
                "by_zone": None,
                "by_value": {
                    "interval_min": 0,
                    "interval_max": 9999,
                    "condition": "EXCLUDE",
                    "od_values": mat.id
                    }
                },
            "aggregation": {
                "origins" : None,
                "destinations": None
                }
            }
        report = compute_matrix(calc_Pwlk)
#---------------------------------------------------------------------------------------------------------
    calc_Pwa2 = {
        "type": "MATRIX_CALCULATION",
        "result": Pwa2.id,
        "expression": "(" + Pwa2.id + "-" +  Pwa1.id + ").max.0",
        "constraint": {
            "by_zone": None,
            "by_value": None,
            },
        "aggregation": {
            "origins" : None,
            "destinations": None
            }
        }
    report = compute_matrix(calc_Pwa2)
#---------------------------------------------------------------------------------------------------------
    for mat in [Pboa,Pifa,Pzfa,Ptdi,Pivb,Pbob,Pivp,Pivl,Pivc]:
        calc_P = {
            "type": "MATRIX_CALCULATION",
            "result": mat.id,
            "expression": "0",
            "constraint": {
                "by_zone": None,
                "by_value": {
                    "interval_min": 0,
                    "interval_max": 9999,
                    "condition": "EXCLUDE",
                    "od_values": mat.id
                    }
                },
            "aggregation": {
                "origins" : None,
                "destinations": None
                }
            }
        report = compute_matrix(calc_P)
#---------------------------------------------------------------------------------------------------------
    
    for mat in [Pivs,Piv1,Piv2,Piv3,Picl,Pipr]:
        calc_Pi= {
            "type": "MATRIX_CALCULATION",
            "result": mat.id,
            "expression": "0",
            "constraint": {
                "by_zone": None,
                "by_value": {
                    "interval_min": 0,
                    "interval_max": 9999,
                    "condition": "EXCLUDE",
                    "od_values": mat.id
                    }
                },
            "aggregation": {
                "origins" : None,
                "destinations": None
                }
            }
        report = compute_matrix(calc_Pi)

        calc_Pic = {
            "type": "MATRIX_CALCULATION",
            "result": mat.id,
            "expression": mat.id + "/" + Pivt.id,
            "constraint": {
            "by_zone": None,
            "by_value": {
                "interval_min": 0,
                "interval_max": 0,
                "condition": "EXCLUDE",
                "od_values": Pivt.id
                }
            },
        "aggregation": {
            "origins" : None,
            "destinations": None
            }
        }
        report = compute_matrix(calc_Pic)
#---------------------------------------------------------------------------------------------------------
    calc_Pbpr= {
        "type": "MATRIX_CALCULATION",
        "result": Pbpr.id,
        "expression": "0",
        "constraint": {
            "by_zone": None,
            "by_value": {
                "interval_min": 0,
                "interval_max": 9999,
                "condition": "EXCLUDE",
                "od_values": Pbpr.id
                }
            },
        "aggregation": {
            "origins" : None,
            "destinations": None
            }
        }
    report = compute_matrix(calc_Pbpr)

    calc_Pbpr2 = {
        "type": "MATRIX_CALCULATION",
        "result": Pbpr.id,
        "expression": Pbpr.id + "/" + Pboa.id,
        "constraint": {
        "by_zone": None,
        "by_value": {
            "interval_min": 0,
            "interval_max": 0,
            "condition": "EXCLUDE",
            "od_values": Pboa.id
            }
        },
    "aggregation": {
        "origins" : None,
        "destinations": None
        }
    }
    report = compute_matrix(calc_Pbpr2)
#---------------------------------------------------------------------------------------------------------
    for mat in [Peb1,Peb2,Peb3,Phmi]:
        calc_Pb = {
            "type": "MATRIX_CALCULATION",
            "result": mat.id,
            "expression": "0",
            "constraint": {
                "by_zone": None,
                "by_value": {
                    "interval_min": 0,
                    "interval_max": 9999,
                    "condition": "EXCLUDE",
                    "od_values": mat.id
                    }
                },
            "aggregation": {
                "origins" : None,
                "destinations": None
                }
            }
        report = compute_matrix(calc_Pb)
#---------------------------------------------------------------------------------------------------------
    calc_Phmipb = {
        "type": "MATRIX_CALCULATION",
        "result": Phmi.id,
        "expression": Phmi.id + "/" + Pboa.id,
        "constraint": {
        "by_zone": None,
        "by_value": {
            "interval_min": 0,
            "interval_max": 0,
            "condition": "EXCLUDE",
            "od_values": Pboa.id
            }
        },
    "aggregation": {
        "origins" : None,
        "destinations": None
        }
    }
    report = compute_matrix(calc_Phmipb)
#---------------------------------------------------------------------------------------------------------
    for mat in [Phma,Pbtr,Patr,Pewt,Pbcl]:
        calc_Ph = {
            "type": "MATRIX_CALCULATION",
            "result": mat.id,
            "expression": "0",
            "constraint": {
                "by_zone": None,
                "by_value": {
                    "interval_min": 0,
                    "interval_max": 9999,
                    "condition": "EXCLUDE",
                    "od_values": mat.id
                    }
                },
            "aggregation": {
                "origins" : None,
                "destinations": None
                }
            }
        report = compute_matrix(calc_Ph)
#---------------------------------------------------------------------------------------------------------
    calc_PbclPB = {
        "type": "MATRIX_CALCULATION",
        "result": Pbcl.id,
        "expression": Pbcl.id + "/" + Pboa.id,
        "constraint": {
        "by_zone": None,
        "by_value": {
            "interval_min": 0,
            "interval_max": 0,
            "condition": "EXCLUDE",
            "od_values": Pboa.id
            }
        },
    "aggregation": {
        "origins" : None,
        "destinations": None
        }
    }
    report = compute_matrix(calc_PbclPB)
#---------------------------------------------------------------------------------------------------------    
    calc_PaclPB = {
        "type": "MATRIX_CALCULATION",
        "result": Pacl.id,
        "expression": "0",
        "constraint": {
        "by_zone": None,
        "by_value": {
            "interval_min": 0,
            "interval_max": 9999,
            "condition": "EXCLUDE",
            "od_values": Pacl.id
            }
        },
    "aggregation": {
        "origins" : None,
        "destinations": None
        }
    }
    report = compute_matrix(calc_PaclPB)
#---------------------------------------------------------------------------------------------------------
    calc_PaclPB2 = {
        "type": "MATRIX_CALCULATION",
        "result": Pbcl.id,
        "expression": Pbcl.id + "/" + Pboa.id,
        "constraint": {
        "by_zone": None,
        "by_value": {
            "interval_min": 0,
            "interval_max": 0,
            "condition": "EXCLUDE",
            "od_values": Pboa.id
            }
        },
    "aggregation": {
        "origins" : None,
        "destinations": None
        }
    }
    report = compute_matrix(calc_PaclPB2)
#---------------------------------------------------------------------------------------------------------
    
