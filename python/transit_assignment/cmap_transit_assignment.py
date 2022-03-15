#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// transit_assignment.py                                                 ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# The Transit assignment tool runs the transit assignment and skims for each
# period on the current primary scenario.
#
# The Build transit network tool must be run first to prepare the scenario for
# assignment. Note that this tool must be run with the Transit database
# (under the Database_transit directory) open (as the active database in the
# Emme desktop).
#
#
# Inputs:
#   period: the time-of-day period, one of EA, AM, MD, PM, EV.
#   scenario: Transit assignment scenario
#   skims_only: Only run assignment for skim matrices, if True only two assignments
#       are run to generate the skim matrices for the BUS and ALL skim classes.
#       Otherwise, all 15 assignments are run to generate the total network flows.
#   num_processors: number of processors to use for the traffic assignments.
#
# Matrices:
#   All transit demand and skim matrices.
#   See list of matrices under report method.
#
# Script example:
#
# Updates (_v3) - 2021-10-22:
#     skimming by three income classes

TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from copy import deepcopy as _copy
from collections import defaultdict as _defaultdict, OrderedDict
import contextlib as _context
import numpy
import general as gen_utils
#import demand as dem_utils

import os
import sys
import math

#TODO - createa utilities scripts for these. Presently, required definitions are added to the bottom of the script
#gen_utils = _m.Modeller().module("sandag.utilities.general")
#dem_utils = _m.Modeller().module("sandag.utilities.demand")

class TransitAssignment(_m.Tool()): #, gen_utils.Snapshot
    __MODELLER_NAMESPACE__ = "cmap"
    period = _m.Attribute(unicode)
    scenario =  _m.Attribute(_m.InstanceType)
    #data_table_name = _m.Attribute(unicode)
    assignment_only = _m.Attribute(bool)
    skims_only = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    #@_m.method(return_type=unicode)
    #def tool_run_msg_status(self):
    #    return self.tool_run_msg

    def __init__(self):
        self.assignment_only = False
        self.skims_only = False
        self.scenario = _m.Modeller().scenario
        self.num_processors = "MAX-1"
        self.attributes = [
            "period", "scenario", "assignment_only", "skims_only",  "num_processors"]
        self._dt_db = _m.Modeller().desktop.project.data_tables()
        self._matrix_cache = {}  # used to hold data for reporting and post-processing of skims
        #for initializing matrices
        self._all_periods = ["1", "2", "3", "4", "5", "6", "7", "8"]
        self.periodLabel = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}
        self.periods = self._all_periods[:]
        self.attributes = ["components", "periods", "delete_all_existing"]
        self._matrices = {}
        self._count = {}
        self.user_classes = ["1","2","3"]
        self.user_class_labels = {1: "L", 2: "M", 3: "H"}
        self.cost_percep = {"uc1": 0.12, "uc2": 0.06, "uc3": 0.04} # by user class
        self.vots = {"uc1": 3.83, "uc2": 12, "uc3": 40}  # by user class - 2.3 $/hr (3.83 cents/min), 7.2 $/hr (12 cents/min), 24.0 $/hr (40 cents/min)
        self.clean_importance = {"uc1":0.5, "uc2": 0.75, "uc3":1.0} #by user_class
        self.board_cost_per = self.cost_percep  #TODO - does it need to be by user class?
        self._all_components = [
            #"traffic_demand",
            "transit_demand",
            #"traffic_skims",
            "transit_skims",
            #"external_internal_model",
            #"external_external_model",
            #"truck_model",
            #"commercial_vehicle_model",
        ]   
        self.skim_matrices = ["GENCOST", "FIRSTWAIT", "XFERWAIT", "TOTALWAIT", "FARE", "XFERS", "ACCWALK", "XFERWALK", "EGRWALK", "TOTALWALK", "TOTALIVTT", "DWELLTIME", "CTABUSLIVTT", "PACEBUSRIVTT", "PACEBUSLIVTT", "PACEBUSEIVTT", "CTABUSEIVTT", "CTARAILIVTT", "METRARAILIVTT", "CTABUSLDIST", "PACEBUSRDIST", "PACEBUSLDIST", "PACEBUSEDIST", "CTABUSEDIST", "CTARAILDIST", "METRARAILDIST", "TOTTRNDIST"]
        self.basematrixnumber = 700


    def __call__(self, period, matrix_count, scenario, assignment_only=False, skims_only=False,
                 num_processors="MAX-1"):
        attrs = {
            "period": period,
            "scenario": scenario.id,
            "assignment_only": assignment_only,
            "skims_only": skims_only,
            "num_processors": num_processors,
            "self": str(self)
        }
        self.scenario = scenario
        #if not scenario.has_traffic_results:
        #    raise Exception("missing traffic assignment results for period %s scenario %s" % (period, scenario))
        #emmebank = scenario.emmebank
        print("transit assignment for period %s scenario %s" % (period, scenario))
        with self.setup(attrs):
            #gen_utils.log_snapshot("Transit assignment", str(self), attrs)
            
            periods = self.periods
            if not period in periods:
                raise Exception('period: unknown value - specify one of %s' % periods)
            
            num_processors = attrs["num_processors"] #dem_utils.parse_num_processors(num_processors)
            #params = self.get_perception_parameters(period)

            self.generate_matrix_list(period, self.scenario)
            
            print("transit skim matrices")
            self.transit_skim_matrices(period, scenario, num_processors)

            network = scenario.get_partial_network(
                element_types=["TRANSIT_LINE"], include_attributes=True)
            
            
            #coaster_mode = network.mode("c")
            #params["coaster_fare_percep"] = 0
            #for line in list(network.transit_lines()):
            #    # get the coaster fare perception for use in journey levels
            #    if line.mode == coaster_mode:
            #        params["coaster_fare_percep"] = line[params["fare"]]
            #        break

            #transit_passes = gen_utils.DataTableProc("%s_transit_passes" % data_table_name)
            #transit_passes = {row["pass_type"]: row["cost"] for row in transit_passes}
            #day_pass = float(transit_passes["day_pass"]) / 2.0
            #regional_pass = float(transit_passes["regional_pass"]) / 2.0

            day_pass = 5*100  # in cents. #TODO - temp value to get the process working
            regional_pass = 10*100 #in cents. #TODO - temp value to get the process working. in cents.

            for uc in self.user_classes:
                print("calculate network attributes")
                self.calc_network_attribute(uc)

                #parameters by period and user class
                params = self.get_perception_parameters(period, "uc%s" % (uc))
                       
                print("run transit assignment for period=%s, uc=%s"%(period, uc))
                self.run_assignment(period, uc, params, network, skims_only, num_processors)
                
                if not assignment_only:
                    a=1
                    #max_fare = day_pass for local bus and regional_pass for premium modes
                    #self.run_skims("BUS", period, params, day_pass, num_processors, network)
                    #self.run_skims("PREM", period, params, regional_pass, num_processors, network)
                    ###
                    #for a_name in ["WLK", "PNR", "KNR"]:
                    #name = "%s_TRN_%s_%s"%(self.periodLabel[int(period)], a_name, self.user_class_labels[int(user_class)])
                    #name is [NT_TRN_WALK_L, NT_TRN_PNR_L, NT_TRN_KNR_L]
                    #run_skims(self, name, period, user_class, params, max_fare, num_processors, network)
                    ###
                    self.run_skims("ALLPEN", period, uc, params, regional_pass, num_processors, network)
                    #self.mask_allpen(period)  #needed only when three sets are used. TODO - for later.
                    #self.report(period)  #use sandag specific utilites. TODO - for later.

            self.mask_highvalues(self._matrices["transit_skims"][period], scenario, num_processors)
                
            return(self._count['mf']-8700)

    @_context.contextmanager
    def setup(self, attrs):
        self._matrix_cache = {}  # initialize cache at beginning of run
        emmebank = self.scenario.emmebank
        period = attrs["period"]
        with _m.logbook_trace("Transit assignment for period %s" % period, attributes=attrs):
            #temp = gen_utils.temp_matrices(emmebank, "FULL", 3)
            #print(len(temp))
            with gen_utils.temp_matrices(emmebank, "FULL", 3) as matrices:
                matrices[0].name = "TEMP_IN_VEHICLE_COST"
                matrices[1].name = "TEMP_LAYOVER_BOARD"
                matrices[2].name = "TEMP_PERCEIVED_FARE"
                try:
                    yield
                finally:
                    self._matrix_cache = {}  # clear cache at end of run

    def generate_matrix_list(self, period, scenario):
        self._matrices = dict(
            (name, dict((k, []) for k in self._all_periods + ["ALL"]))
            for name in self._all_components)

        #every period - 30 demand matrices (27 skims + 3 demand) per userclass. 3 user classes.
        self._count = {"ms": 2, "md": 100, "mo": 100, "mf": 8700 + 30*3*(int(period)-1)} #"mf"=100

        #for component in self._all_components:
        #    fcn = getattr(self, component)
        #    fcn()
        # check dimensions can fit full set of matrices
        type_names = [
            ('mf', 'full_matrices'),
            ('mo', 'origin_matrices'),
            ('md', 'destination_matrices'),
            ('ms', 'scalar_matrices')]
        dims = scenario.emmebank.dimensions
        for prefix, name in type_names:
            if self._count[prefix] > dims[name]:
                raise Exception("emmebank capacity error, increase %s to at least %s" % (name, self._count[prefix]))

    def get_perception_parameters(self, period, user_class):

        perception_parameters = {
            "1": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "2": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "3": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "4": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "5": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "6": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "7": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            },
            "8": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "walk": 2,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": None,
            }
        }
        return perception_parameters[period]

    def group_modes_by_fare(self, network, day_pass_cost):
        # Identify all the unique boarding fare values
        fare_set = {mode.id: _defaultdict(lambda:0)
                    for mode in network.modes()
                    if mode.type == "TRANSIT"}
        for line in network.transit_lines():
            fare_set[line.mode.id][line["@fare"]] += 1
        del fare_set['c']  # remove coaster mode, this fare is handled separately
        # group the modes relative to day_pass
        mode_groups = {
            "bus": [],       # have a bus fare, less than 1/2 day pass
            "day_pass": [],  # boarding fare is the same as 1/2 day pass
            "premium": []    # special premium services not covered by day pass
        }
        for mode_id, fares in fare_set.items():
            try:
                max_fare = max(fares.keys())
            except ValueError:
                continue  # an empty set means this mode is unused in this period
            if numpy.isclose(max_fare, day_pass_cost, rtol=0.0001):
                mode_groups["day_pass"].append((mode_id, fares))
            elif max_fare < day_pass_cost:
                mode_groups["bus"].append((mode_id, fares))
            else:
                mode_groups["premium"].append((mode_id, fares))
        return mode_groups

    #@_m.logbook_trace("Generate network attributes", save_arguments=True)
    def calc_network_attribute(self, user_class):
        modeller = _m.Modeller()
        scenario = self.scenario
        #emmebank = scenario.emmebank

        network_calc = modeller.tool("inro.emme.network_calculation.network_calculator")
        #create_extra_attribute = modeller.tool("inro.emme.data.extra_attribute.create_extra_attribute")

        #current_scenario = my_emmebank.scenario(scen)
        #tranScen = database.scenario_by_number(scen)
        #data_explorer.replace_primary_scenario(tranScen)

        network = scenario.get_network()  #TODO: do this in base scenario. in a build_transit_scenario script.
        new_attrs = [
            #("TRANSIT_LINE", "@xfer_from_day", "Fare for xfer from daypass/trolley"),
            #("TRANSIT_LINE", "@xfer_from_premium", "Fare for first xfer from premium"),
            #("TRANSIT_LINE", "@xfer_from_coaster", "Fare for first xfer from coaster"),
            #("TRANSIT_LINE", "@xfer_regional_pass", "0-fare for regional pass"),
            #("TRANSIT_SEGMENT", "@xfer_from_bus", "Fare for first xfer from bus"),
            #("TRANSIT_SEGMENT", "@headway_seg", "Headway adj for special xfers"),
            #("TRANSIT_SEGMENT", "@transfer_penalty_s", "Xfer pen adj for special xfers"),
            ("TRANSIT_SEGMENT", "@layover_board", "Boarding cost adj for special xfers"),
            ("TRANSIT_SEGMENT", "@boardpm", "Boarding cost percep"),
            ("TRANSIT_SEGMENT", "@ivtf", "In-vehicle time factor"),
            #("NODE", "@network_adj", "Model: 1=TAP adj, 2=circle, 3=timedxfer"),
            #("NODE", "@network_adj_src", "Orig src node for timedxfer splits"),
        ]
        #TODO - @layover_board needs to be populated by transit line and node. for now, 0
        for elem, name, desc in new_attrs:
            if not scenario.extra_attribute(name):
                attr = scenario.create_extra_attribute(elem, name)
                attr.description = desc

        #segCostP_calc = {
        #    "result": "@boardpm",
        #    "expression": str(self.cost_percep['uc1']),
        #    "aggregation": None,
        #    "selections": {
        #        "link": "all",
        #        "transit_line": "all"
        #    },
        #    "type": "NETWORK_CALCULATION"
        #}
        #network_calc(segCostP_calc, scenario=scenario) # num_processors=self.num_processors

        #calculate @ivtf by userclass        
        uc_name = "uc%s" % (user_class)
        segCostP_calc = {
            "result": "@ivtf%s" % (user_class),
            "expression": "@ivtpf*(1+@soba%s+@crowf+@clnob*%s+@prof%s*(@pseat+0.001)/(@pseat+@pstan+0.001))" % (user_class, self.clean_importance[uc_name], user_class),
            "aggregation": None,
            "selections": {
                "link": "all",
                "transit_line": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        #print(segCostP_calc)
        network_calc(segCostP_calc, scenario=scenario)

        #new - calculate @ivtf as average of @ivtf by user class. TODO - create one attribute. 
        #segCostP_calc = {
        #    "result": "@ivtf",
        #    "expression": "(@ivtf1 + @ivtf2 + @ivtf3)/3",
        #    "aggregation": None,
        #    "selections": {
        #        "link": "all",
        #        "transit_line": "all"
        #    },
        #    "type": "NETWORK_CALCULATION"
        #}
        #network_calc(segCostP_calc, scenario=scenario) # num_processors=self.num_processors

        # Pace Bus
        Pace_InVeh = {
            "result": "@ivtf%s" %(user_class),
            "expression": "@ivtf%s" %(user_class) + "*8",
            "aggregation": None,
            "selections": {
                "link": "all",
                "transit_line": "line=p_____ | q_____ "
            },
            "type": "NETWORK_CALCULATION"
        }

        metra_InVeh = {
            "result": "@ivtf%s" %(user_class),
            "expression": "@ivtf%s" %(user_class) + "*4.5",
            "aggregation": None,
            "selections": {
                "link": "@zone=89,309",
                "transit_line": "line=m_____"
            },
            "type": "NETWORK_CALCULATION"
        }

        metra_InVeh_noCook = {
            "result": "@ivtf%s" %(user_class),
            "expression": "@ivtf%s" %(user_class) + "*.10",
            "aggregation": None,
            "selections": {
                "link": "@zone=310,1711",
                "transit_line": "line=m_____"
            },
            "type": "NETWORK_CALCULATION"
        }

        network_calc([Pace_InVeh, metra_InVeh, metra_InVeh_noCook], scenario=scenario)

        metra_electric = {
            "result": "@ivtf%s" %(user_class),
            "expression": "@ivtf%s" %(user_class) + "*1.4",
            "aggregation": None,
            "selections": {
                "link": "all",
                "transit_line": "line=mme___"
            },
            "type": "NETWORK_CALCULATION"
        }
        metra_hc = {
            "result": "@ivtf%s" %(user_class),
            "expression": "@ivtf%s" %(user_class) + "*5",
            "aggregation": None,
            "selections": {
                "link": "all",
                "transit_line": "line=mh____"
            },
            "type": "NETWORK_CALCULATION"
        }

        cta_red = {
            "result": "@ivtf%s" %(user_class),
            "expression": "@ivtf%s" %(user_class) + "*.3",
            "aggregation": None,
            "selections": {
                "link": "all",
                "transit_line": "line=cr____"
            },
            "type": "NETWORK_CALCULATION"
        }

        network_calc([metra_electric, cta_red, metra_hc], scenario=scenario)

        initial = 1 #TODO - not sure what this is. Need to look into it.
        if initial > 0:
            # Pace Bus
            Pace_InVeh = {
                "result": "@ivtf%s" %(user_class),
                "expression": "@ivtf%s" %(user_class) + "*10",
                "aggregation": None,
                "selections": {
                    "link": "all",
                    "transit_line": "line=p_____ | q_____ "
                },
                "type": "NETWORK_CALCULATION"
            }

            network_calc([Pace_InVeh], scenario=scenario)

    def all_modes_journey_levels(self, params, uc_name):
        board_cost_per = float(self.cost_percep[uc_name])

        journey_levels = [
            {  # Never Boarded 0
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
            {  # Metra Only 1
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
            {  # CTA Rail 2
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
                    # Reduction in spread factor to reduce options
                    "spread_factor": 1,
                    "perception_factor": "@wconf"
                }
            },
            {  # CTA, No Pace 3
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
                    # Reduction in spread factor to reduce options
                    "spread_factor": 1,
                    "perception_factor": "@wconf"
                }
            },
            {  # Pace No CTA 4
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
                    # Increase Spread Factor (Transit Options)
                    "spread_factor": 1,
                    "perception_factor": "@wconf"
                }
            },
            {  # CTA and Pace 5
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

        return journey_levels

    #not used currently. will need this if segmented in three sets.
    def filter_journey_levels_by_mode(self, modes, journey_levels):
        # remove rules for unused modes from provided journey_levels
        # (restrict to provided modes)
        journey_levels = _copy(journey_levels)
        for level in journey_levels:
            rules = level["transition_rules"]
            rules = [r for r in rules if r["mode"] in modes]
            level["transition_rules"] = rules
        # count level transition rules references to find unused levels
        num_levels = len(journey_levels)
        level_count = [0] * len(journey_levels)

        def follow_rule(next_level):
            level_count[next_level] += 1
            if level_count[next_level] > 1:
                return
            for rule in journey_levels[next_level]["transition_rules"]:
                follow_rule(rule["next_journey_level"])

        follow_rule(0)
        # remove unreachable levels
        # and find new index for transition rules for remaining levels
        level_map = {i:i for i in range(num_levels)}
        for level_id, count in reversed(list(enumerate(level_count))):
            if count == 0:
                for index in range(level_id, num_levels):
                    level_map[index] -= 1
                del journey_levels[level_id]
        # re-index remaining journey_levels
        for level in journey_levels:
            for rule in level["transition_rules"]:
                next_level = rule["next_journey_level"]
                rule["next_journey_level"] = level_map[next_level]
        return journey_levels

    #@_m.logbook_trace("Transit demand matrices initialize", save_arguments=True)
    def transit_demand_matrices(self, period, scenario, num_processors):
        modeller = _m.Modeller()
        matrix_calc = modeller.tool("inro.emme.matrix_calculation.matrix_calculator")
        #add new matrices - for skimming
        tmplt_matrices = [
            #("BUS",  "local bus demand"),
            #("PREM", "Premium modes demand"),
            ("ALLPEN",  "all modes xfer pen demand"),
        ]

        #TEMP - aggregate the existing demand matrices into all
        #TODO - these demand matrices will be generated by trip building step
        #conventional
        
        #demand factor
        #demand_factor = {"WLK": 0.7, "PNR": 0.2, "KNR": 0.1}
        demand_factor = {"WLK": 1.0, "PNR": 0, "KNR": 0} #for testing

        #conventional
        #mat_class1 = int(period) * 1000 + 359
        #mat_class2 = int(period) * 1000 + 360
        #mat_class3 = int(period) * 1000 + 361

        #premium
        mat_class1 = int(period) * 1000 + 362
        mat_class2 = int(period) * 1000 + 363
        mat_class3 = int(period) * 1000 + 364

        mat_class = {"uc1": mat_class1, "uc2": mat_class2, "uc3": mat_class3}
                        
        #print('add matrices')
        #user_classes = ["1","2","3"]
        idx = self.basematrixnumber
        for uc in self.user_classes:
            for a_name in ["WALK", "PNR", "KNR"]:
                for mname in self.skim_matrices:
                    temp = self.add_matrices(int(period) * 1000 + idx, "%s_%s_%s_%s"%(mname, a_name, self.user_class_labels[int(uc)], self.periodLabel[int(period)]), scenario)
                    idx += 1
             

            #sum the demand to all users (clas1+class2+class3) and factor by access mode
            #spec_list = [
            #    {  # demand aggregation and factoring
            #        "type": "MATRIX_CALCULATION",
            #        "constraint": None,
            #        "result": 'mf"%s_%s%s"' % (period, a_name, name),
            #        "expression": '%s * (mf%s + mf%s + mf%s)' % (demand_factor[a_name], mat_class1, mat_class2, mat_class3),
            #    }]
            #print(spec_list)
            #matrix_calc(spec_list, scenario=scenario, num_processors=num_processors)

        #sum the demand to all users (clas1+class2+class3) and factor by access mode
        #print(temp)
        #emmebank = scenario.emmebank
        #for matrix in emmebank.matrices():
        #    print(matrix.name)
        
        #delete existing - run only when needed
        #emmebank.delete_matrix(emmebank.matrix("mf66"))

        #print('aggregate demand')
	#ex. demand matrix = mfWLK_ALLPEN1
		
        # for uc in self.user_classes:
            # for a_name in ["WALK", "PNR", "KNR"]:
                # for name, desc in tmplt_matrices:
                    # spec_list = [
                        # {  # demand aggregation and factoring
                                # "type": "MATRIX_CALCULATION",
                                # "constraint": None,
                                # "result": '%s_%s%s_uc%s' % (a_name, name, period, uc),
                                # #"expression": '%s' % (demand_factor[a_name]),
                                # "expression": '%s * (mf%s)' % (demand_factor[a_name], mat_class["uc%s" %uc]),
                        # }]
                    
                    #print(spec_list)    
                    #matrix_calc(spec_list, scenario=scenario, num_processors=num_processors)


    #@_m.logbook_trace("Transit skim matrices initialize", save_arguments=True)
    def transit_skim_matrices(self, period, scenario, num_processors):
        idx = self.basematrixnumber
        for uc in self.user_classes:
            for a_name in ["WALK", "PNR", "KNR"]:
                for mname in self.skim_matrices:
                    temp = self.add_matrices(int(period) * 1000 + idx, "%s_%s_%s_%s"%(mname, a_name, self.user_class_labels[int(uc)], self.periodLabel[int(period)]), scenario)
                    idx += 1
        return(True)
        """
        print(scenario)
        scenario = self.scenario
        tmplt_matrices = [
            ("GENCOST",    "total impedance"),
            ("FIRSTWAIT",  "first wait time"),
            ("XFERWAIT",   "transfer wait time"),
            ("TOTALWAIT",  "total wait time"),
            ("FARE",       "fare"),
            ("XFERS",      "num transfers"),
            ("ACCWALK",    "access walk time"),
            ("XFERWALK",   "transfer walk time"),
            ("EGRWALK",    "egress walk time"),
            ("TOTALWALK",  "total walk time"),
            ("TOTALIVTT",  "in-vehicle time"),
            ("DWELLTIME",  "dwell time"),
            ("CTABUSLIVTT",    "CTA bus in-vehicle time"),
            ("PACEBUSRIVTT", "CTA bus in-vehicle time"),
            ("PACEBUSLIVTT", "CTA bus in-vehicle time"),
            ("PACEBUSEIVTT", "CTA bus in-vehicle time"),
            ("CTABUSEIVTT", "CTA bus in-vehicle time"),
            ("CTARAILIVTT", "CTA bus in-vehicle time"),
            ("METRARAILIVTT", "CTA bus in-vehicle time"),
            ("CTABUSLDIST", "CTA bus in-vehicle distance"),
            ("PACEBUSRDIST", "CTA bus in-vehicle distance"),
            ("PACEBUSLDIST", "CTA bus in-vehicle distance"),
            ("PACEBUSEDIST", "CTA bus in-vehicle distance"),
            ("CTABUSEDIST", "CTA bus in-vehicle distance"),
            ("CTARAILDIST", "CTA bus in-vehicle distance"),
            ("METRARAILDIST", "CTA bus in-vehicle distance"),
            ("TOTTRNDIST",    "Total transit distance")
        ]
        skim_sets = [
            #("BUS",    "Local bus only"),
            #("PREM",   "Premium modes only"),
            ("ALLPEN", "All w/ xfer pen")
        ]

        user_classes = ["WALK","KNR","PNR"]
        for uc in self.user_classes:
           for set_name, set_desc in skim_sets:
               #add_matrices(self, mid, mname, scenario, desc = "")
               self.add_matrices("transit_skims", period,
                                 [("mf", set_name + "_" + name + "_" + period + "_" + uc,
                                   set_desc + ": " + desc + " " + period + " " + uc)
                                  for name, desc in tmplt_matrices], scenario)
    """

    #@_m.logbook_trace("Transit assignment by demand set", save_arguments=True)
    def run_assignment(self, period, user_class, params, network, skims_only, num_processors):
        modeller = _m.Modeller()
        scenario = self.scenario
        emmebank = scenario.emmebank
        assign_transit = modeller.tool("inro.emme.transit_assignment.extended_transit_assignment")

        walk_modes = ["m", "c", "u", "x", "v", "y", "w", "z", "b", "r", "t", "d"] # "a", "e"]
        local_bus_mode = ["B", "P", "L"]
        premium_modes = ["C", "M", "E", "Q"]
        #walk_modes_accessegress = ["a","e"]
        #walk_modes_bus = ["u","x"] #Bus
        #walk_modes_prem = ["v", "y", "w", "z"] #Met and CTA
        #walk_modes_xfer = ["m", "c", "b", "r", "t", "d"] #xfer between Bus, Met, and CTA
        #busModes = ["B", "P", "L", "E", "Q"] #all bus modes
        #premModes = ["C", "M", "E", "Q"] #M-Metra Rail #C-CTA Rail
        #localModes = ["B", "P", "L"]
        #ctaRailMode = ["C"]

        # get the generic all-modes journey levels table
        journey_levels = self.all_modes_journey_levels(params, "uc%s" % (user_class))
        #local_bus_journey_levels = self.filter_journey_levels_by_mode(local_bus_mode, journey_levels)
        #premium_modes_journey_levels = self.filter_journey_levels_by_mode(premium_modes, journey_levels)

        base_spec = {
            "type": "EXTENDED_TRANSIT_ASSIGNMENT",
            "modes": [],
            "demand": "",
            "waiting_time": {
                "effective_headways": params["eff_headway"], "headway_fraction": 1,
                "perception_factor": params["init_wait"], "spread_factor": 1.0
            },
            # Fare attributes
            "boarding_cost": {"global": None, "at_nodes": None, "on_lines": {"penalty": "ut1", "perception_factor": None}, "on_segments": None},
            "boarding_time": {"at_nodes": {"penalty": "@timbf", "perception_factor": "@perbf"}, "on_lines": {"penalty": "@easbp", "perception_factor": 1}, "on_segments": None},
            "in_vehicle_cost": {"penalty": "@zfare",
                                "perception_factor": None},
            "in_vehicle_time": {"perception_factor": params["in_vehicle"]},
            "aux_transit_time": {"perception_factor": params["walk"]},
            "aux_transit_cost": {"penalty": "ul1", "perception_factor": 0},
            "journey_levels": [],
            #SANDAG - "consider_total_impedance": False
            "flow_distribution_between_lines": {"consider_total_impedance": True},
            # Distribute flow between connectors - transit time (logit)
            "flow_distribution_at_origins": {"choices_at_origins": {"choice_points": "ALL_ORIGINS", "choice_set": "ALL_CONNECTORS",
                                                                    "logit_parameters": {"scale": 0.2,"truncation": 0.05}},
                                             "fixed_proportions_on_connectors": None},
            #SANDAG - optimal strategy
            #"flow_distribution_at_origins": {"fixed_proportions_on_connectors": None, "choices_at_origins": "OPTIMAL_STRATEGY"},

            "flow_distribution_at_regular_nodes_with_aux_transit_choices": {
                "choices_at_regular_nodes": "OPTIMAL_STRATEGY"
            },
            # Handle connector-to-connector path - prohibit
            "connector_to_connector_path_prohibition": {
                # Prohibit connector-to-connector path everywhere
                "at_nodes": "ALL",
                # assign to another path
                "reassign_demand_to_alternate_path": True
            },
            #SANDAG - None
            #"connector_to_connector_path_prohibition": None,
            "od_results": {"total_impedance": None},
            "performance_settings": {"number_of_processors": num_processors}
        }
        #SANDAG - three sets: BUS only, PREM only, and Transfers
        #for now, do only one set. ALLPEN
        skim_parameters = OrderedDict([
            #("BUS", {
            #    "modes": walk_modes + local_bus_mode,
            #    "journey_levels": local_bus_journey_levels
            #}),
            #("PREM", {
            #    "modes": walk_modes + premium_modes,
            #    "journey_levels": premium_modes_journey_levels
            #}),
            ("ALLPEN", {
                "modes": walk_modes + local_bus_mode + premium_modes,
                "journey_levels": journey_levels
            }),
        ])

        if skims_only:
            access_modes = ["WALK"]
        else:
            access_modes = ["WALK", "PNR", "KNR"]
        
        add_volumes = False
        for a_name in access_modes:
            for mode_name, parameters in skim_parameters.iteritems():
                spec = _copy(base_spec)
                name = "%s_TRN_%s_%s"%(self.periodLabel[int(period)], a_name, self.user_class_labels[int(user_class)])
                spec["modes"] = parameters["modes"]
                spec["demand"] = 'mf%s' % (name)  #TODO - demand matrix by user class
                spec["journey_levels"] = parameters["journey_levels"]
                spec["in_vehicle_cost"]["perception_factor"] = "@boardp%sm" % (user_class)
                spec["boarding_cost"]["on_lines"]["perception_factor"] = float(self.cost_percep["uc%s" % (user_class)])
                spec["in_vehicle_time"]["perception_factor"] = "@ivtf%s" % (user_class)
                assign_transit(spec, class_name=name, add_volumes=add_volumes, scenario=self.scenario)
                add_volumes = True

    #@_m.logbook_trace("Extract skims", save_arguments=True)
    def run_skims(self, name, period, user_class, params, max_fare, num_processors, network):
        modeller = _m.Modeller()
        scenario = self.scenario
        #emmebank = scenario.emmebank
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator")
        #network_calc = modeller.tool(
        #    "inro.emme.network_calculation.network_calculator")
        matrix_results = modeller.tool(
            "inro.emme.transit_assignment.extended.matrix_results")
        path_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.path_based_analysis")
        strategy_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.strategy_based_analysis")
        
        class_name = "%s_TRN_%s_%s"%(self.periodLabel[int(period)], "WALK", self.user_class_labels[int(user_class)])
        skim_name = "%s" % (name)
        #self.run_skims.logbook_cursor.write(name="Extract skims for %s, using assignment class %s" % (name, class_name))
        print("Extract skims for %s, using assignment class %s" % (name, class_name))
        with _m.logbook_trace("First and total wait time, number of boardings, fares, total walk time, in-vehicle time"):
            # First and total wait time, number of boardings, fares, total walk time, in-vehicle time
            spec = {
                "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                #FIRSTWAIT_<AMODE>_<VOT>_<PER>
                "actual_first_waiting_times": 'mf"FIRSTWAIT_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "actual_total_waiting_times": 'mf"TOTALWAIT_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "total_impedance": 'mf"GENCOST_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "by_mode_subset": {
                    "modes": [mode.id for mode in network.modes() if mode.type == "TRANSIT" or mode.type == "AUX_TRANSIT"],
                    "avg_boardings": 'mf"XFERS_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                    "actual_in_vehicle_times": 'mf"TOTALIVTT_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                    "actual_in_vehicle_costs": 'mf"TEMP_IN_VEHICLE_COST"',
                    "actual_total_boarding_costs": 'mf"FARE_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                    "perceived_total_boarding_costs": 'mf"TEMP_PERCEIVED_FARE"',
                    "actual_aux_transit_times": 'mf"TOTALWALK_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                },
            }
            matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
        with _m.logbook_trace("Distance and in-vehicle time by mode"):
            mode_combinations = [
                ("CTABUSL", ["B"],       ["IVTT", "DIST"]),
                ("PACEBUSR", ["P"],      ["IVTT", "DIST"]),
                ("PACEBUSL", ["L"],      ["IVTT", "DIST"]),
                ("PACEBUSE", ["Q"],      ["IVTT", "DIST"]),
                ("CTABUSE", ["E"],       ["IVTT", "DIST"]),
                ("CTARAIL", ["C"],       ["IVTT", "DIST"]),
                ("METRARAIL", ["M"],     ["IVTT", "DIST"]),
            ]
            for mode_name, modes, skim_types in mode_combinations:
                dist = 'mf"%sDIST_%s_%s_%s"' % (mode_name, "WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]) if "DIST" in skim_types else None
                ivtt = 'mf"%sIVTT_%s_%s_%s"' % (mode_name, "WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]) if "IVTT" in skim_types else None
                spec = {
                    "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                    "by_mode_subset": {
                        "modes": modes,
                        "distance": dist,
                        "actual_in_vehicle_times": ivtt,
                    },
                }
                matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
            # Sum total distance
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"TOTTRNDIST_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": ('mf"CTABUSLDIST_{amode}_{uc}_{period}" + mf"PACEBUSRDIST_{amode}_{uc}_{period}" + mf"PACEBUSLDIST_{amode}_{uc}_{period}"'
                               ' + mf"PACEBUSEDIST_{amode}_{uc}_{period}" + mf"CTABUSEDIST_{amode}_{uc}_{period}"  + mf"CTARAILDIST_{amode}_{uc}_{period}" + mf"METRARAILDIST_{amode}_{uc}_{period}"').format(amode = "WALK", period=self.periodLabel[int(period)], uc=self.user_class_labels[int(user_class)])
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # convert number of boardings to number of transfers
        # and subtract transfers to the same line at layover points
        with _m.logbook_trace("Number of transfers and total fare"):
            spec = {
                "trip_components": {"boarding": "@layover_board"},
                "sub_path_combination_operator": "+",
                "sub_strategy_combination_operator": "average",
                "selected_demand_and_transit_volumes": {
                    "sub_strategies_to_retain": "ALL",
                    "selection_threshold": {"lower": -999999, "upper": 999999}
                },
                "results": {
                    "strategy_values": 'TEMP_LAYOVER_BOARD',
                },
                "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
            }
            strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"XFERS_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                        "interval_min": 1, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"XFERS_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": '(XFERS_%s_%s_%s - 1 - TEMP_LAYOVER_BOARD).max.0' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

            # sum in-vehicle cost and boarding cost to get the fare paid
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"FARE_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": '(FARE_%s_%s_%s + TEMP_IN_VEHICLE_COST).min.%s' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)], max_fare),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # walk access time - get distance and convert to time with 3 miles / hr
        with _m.logbook_trace("Walk time access, egress and xfer"):
            path_spec = {
                "portion_of_path": "ORIGIN_TO_INITIAL_BOARDING",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"ACCWALK_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            # walk egress time - get distance and convert to time with 3 miles/ hr
            path_spec = {
                "portion_of_path": "FINAL_ALIGHTING_TO_DESTINATION",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"EGRWALK_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)])
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            spec_list = [
            {    # walk access time - convert to time with 3 miles/ hr
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"ACCWALK_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": '60.0 * ACCWALK_%s_%s_%s / 3.0' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
            },
            {    # walk egress time - convert to time with 3 miles/ hr
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"EGRWALK_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": '60.0 * EGRWALK_%s_%s_%s / 3.0' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
            },
            {   # transfer walk time = total - access - egress
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"XFERWALK_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": '(TOTALWALK_{amode}_{uc}_{period} - ACCWALK_{amode}_{uc}_{period} - EGRWALK_{amode}_{uc}_{period}).max.0'.format(amode = "WALK", period=self.periodLabel[int(period)], uc=self.user_class_labels[int(user_class)]),
            }]
            matrix_calc(spec_list, scenario=scenario, num_processors=num_processors)

        # transfer wait time
        with _m.logbook_trace("Wait time - xfer"):
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"TOTALWAIT_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                        "interval_min": 0, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"XFERWAIT_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                "expression": '(TOTALWAIT_{amode}_{uc}_{period} - FIRSTWAIT_{amode}_{uc}_{period}).max.0'.format(amode = "WALK", uc = self.user_class_labels[int(user_class)], period = self.periodLabel[int(period)]),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        with _m.logbook_trace("Calculate dwell time"):
            with gen_utils.temp_attrs(scenario, "TRANSIT_SEGMENT", ["@dwt_for_analysis"]):
                values = scenario.get_attribute_values("TRANSIT_SEGMENT", ["dwell_time"])
                scenario.set_attribute_values("TRANSIT_SEGMENT", ["@dwt_for_analysis"], values)

                spec = {
                    "trip_components": {"in_vehicle": "@dwt_for_analysis"},
                    "sub_path_combination_operator": "+",
                    "sub_strategy_combination_operator": "average",
                    "selected_demand_and_transit_volumes": {
                        "sub_strategies_to_retain": "ALL",
                        "selection_threshold": {"lower": -999999, "upper": 999999}
                    },
                    "results": {
                        "strategy_values": 'mf"DWELLTIME_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
                    },
                    "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
                }
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

        expr_params = _copy(params)
        expr_params["xfers"] = 15.0
        expr_params["name"] = skim_name
        expr_params["period"] = period
        expr_params["user_class"] = user_class
        expr_params["uc"] = self.user_class_labels[int(user_class)]
        expr_params["init_wait"] = 0.5 #TODO - cmap has a network attribute
        expr_params["perception_factor"] = 1.0 #TODO - cmap has a network attribute
        expr_params["walk"] = 2.0 #TODO - cmap has a network attribute
        expr_params["amode"] = "WALK"
        
        print("{xfer_wait} * TOTALWAIT_{amode}_{uc}_{period} - ({xfer_wait} - {init_wait}) * FIRSTWAIT_{amode}_{uc}_{period} + 1.0 * TOTALIVTT_{amode}_{uc}_{period} + (1 / {vot}) * (TEMP_PERCEIVED_FARE + {perception_factor} * TEMP_IN_VEHICLE_COST) + {xfers} *(XFERS_{amode}_{uc}_{period}.max.0) + {walk} * TOTALWALK_{amode}_{uc}_{period}").format(**expr_params)
        spec = {
            "type": "MATRIX_CALCULATION",
            "constraint": None,
            "result": 'mf"GENCOST_%s_%s_%s"' % ("WALK", self.user_class_labels[int(user_class)], self.periodLabel[int(period)]),
            "expression": ("{xfer_wait} * TOTALWAIT_{amode}_{uc}_{period} "
                           "- ({xfer_wait} - {init_wait}) * FIRSTWAIT_{amode}_{uc}_{period} "
                           "+ 1.0 * TOTALIVTT_{amode}_{uc}_{period}"
                           "+ (1 / {vot}) * (TEMP_PERCEIVED_FARE + {perception_factor} * TEMP_IN_VEHICLE_COST)"
                           "+ {xfers} *(XFERS_{amode}_{uc}_{period}.max.0) "
                           "+ {walk} * TOTALWALK_{amode}_{uc}_{period}").format(**expr_params)
        }
        matrix_calc(spec, scenario=scenario, num_processors=num_processors) #TODO: not calculated correctly. need to see calculations.
        return

    #NOT USED - from SANDAG
    def mask_allpen(self, period):
        # Reset skims to 0 if not both local and premium
        skims = [
            "FIRSTWAIT", "TOTALWAIT", "DWELLTIME", "BUSIVTT", "XFERS", "TOTALWALK",
            "LRTIVTT", "CMRIVTT", "EXPIVTT", "LTDEXPIVTT", "BRTREDIVTT", "BRTYELIVTT", "TIER1IVTT",
            "GENCOST", "XFERWAIT", "FARE",
            "ACCWALK", "XFERWALK", "EGRWALK", "TOTALIVTT",
            "BUSDIST", "LRTDIST", "CMRDIST", "EXPDIST", "BRTDIST" , "TIER1DIST"]
        localivt_skim = self.get_matrix_data("ALLPEN_BUSIVTT" + period)
        totalivt_skim = self.get_matrix_data("ALLPEN_TOTALIVTT" + period)
        has_premium = numpy.greater((totalivt_skim - localivt_skim), 0)
        has_both = numpy.greater(localivt_skim, 0) * has_premium
        for skim in skims:
            mat_name = "ALLPEN_" + skim + period
            data = self.get_matrix_data(mat_name)
            self.set_matrix_data(mat_name, data * has_both)

    #NOT USED
    def mask_highvalues(self, period, matrices):
        # Reset skims to 0 if value higher than 999999 (no path)
        for ident, name, desc in matrices:
            skim_data = self.get_matrix_data(name)
            has_less = numpy.less(skim_data, 999999)
            #data = self.get_matrix_data(mat_name)
            self.set_matrix_data(mat_name, skim_data * has_less)

    def get_matrix_data(self, name):
        data = self._matrix_cache.get(name)
        if data is None:
            matrix = self.scenario.emmebank.matrix(name)
            data = matrix.get_numpy_data(self.scenario)
            self._matrix_cache[name] = data
        return data

    def set_matrix_data(self, name, data):
        matrix = self.scenario.emmebank.matrix(name)
        self._matrix_cache[name] = data
        matrix.set_numpy_data(data, self.scenario)

    def report(self, period):
        text = ['<div class="preformat">']
        init_matrices = _m.Modeller().tool("sandag.initialize.initialize_matrices")
        matrices = init_matrices.get_matrix_names("transit_skims", [period], self.scenario)
        num_zones = len(self.scenario.zone_numbers)
        num_cells = num_zones ** 2
        text.append(
            "Number of zones: %s. Number of O-D pairs: %s. "
            "Values outside -9999999, 9999999 are masked in summaries.<br>" % (num_zones, num_cells))
        text.append("%-25s %9s %9s %9s %13s %9s" % ("name", "min", "max", "mean", "sum", "mask num"))
        for name in matrices:
            data = self.get_matrix_data(name)
            data = numpy.ma.masked_outside(data, -9999999, 9999999, copy=False)
            stats = (name, data.min(), data.max(), data.mean(), data.sum(), num_cells-data.count())
            text.append("%-25s %9.4g %9.4g %9.4g %13.7g %9d" % stats)
        text.append("</div>")
        title = 'Transit impedance summary for period %s' % period
        report = _m.PageBuilder(title)
        report.wrap_html('Matrix details', "<br>".join(text))
        _m.logbook_write(title, report.render())

    def add_matrices(self, mid, mname, scenario, desc = ""):
        scenario = self.scenario
        create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")           
        matrices_out = create_matrix(matrix_id = "mf%s"%mid, matrix_name = mname, matrix_description = desc, scenario = scenario, default_value = 0, overwrite = True)
        return matrices_out

    #set high values (>999999) to 0 - no path
    def mask_highvalues(self, matrices, scenario, num_processors):
        
        #print(matrices)
        matrix_calc = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

        # Set high values to 0
        with _m.logbook_trace("Set high values to 0"):            
            for ident, name, desc in matrices:
                spec = {
                    "type": "MATRIX_CALCULATION",
                    "constraint":{
                        "by_value": {
                            "od_values": name,
                            "interval_min": -999999, "interval_max": 999999,
                            "condition": "EXCLUDE"},
                    },
                    "result": name,
                    "expression": '0',
                }
                matrix_calc(spec, scenario=scenario, num_processors=num_processors)


    #TODO - make the below definitions part of a utilities script. taken from general.py and demand.py
    def add_select_processors(tool_attr_name, pb, tool):
        max_processors = _multiprocessing.cpu_count()
        tool._max_processors = max_processors
        options = [("MAX-1", "Maximum available - 1"), ("MAX", "Maximum available")]
        options.extend([(n, "%s processors" % n) for n in range(1, max_processors + 1)])
        pb.add_select(tool_attr_name, options, title="Number of processors:")

    def parse_num_processors(value):
        max_processors = _multiprocessing.cpu_count()
        if isinstance(value, int):
            return value
        if isinstance(value, basestring):
            if value == "MAX":
                return max_processors
            if _re.match("^[0-9]+$", value):
                return int(value)
            result = _re.split("^MAX[\s]*-[\s]*", value)
            if len(result) == 2:
                return max(max_processors - int(result[1]), 1)
        if value:
            return int(value)
        return value

    def temp_attrs(scenario, attr_type, idents, default_value=0.0):
        attrs = []
        try:
            for ident in idents:
                attrs.append(scenario.create_extra_attribute(attr_type, ident, default_value))
            yield attrs[:]
        finally:
            for attr in attrs:
                scenario.delete_extra_attribute(attr)

    def temp_matrices(emmebank, mat_type, total=1, default_value=0.0):
        matrices = []
        try:
            while len(matrices) != int(total):
                try:
                    ident = emmebank.available_matrix_identifier(mat_type)
                except _except.CapacityError:
                    raise _except.CapacityError(
                        "Insufficient room for %s required temp matrices." % total)
                matrices.append(emmebank.create_matrix(ident, default_value))
            yield matrices[:]
        finally:
            for matrix in matrices:
                # In case of transient file conflicts and lag in windows file handles over the network
                # attempt to delete file 10 times with increasing delays 0.05, 0.2, 0.45, 0.8 ... 5
                remove_matrix = lambda: emmebank.delete_matrix(matrix)
                retry(remove_matrix)

