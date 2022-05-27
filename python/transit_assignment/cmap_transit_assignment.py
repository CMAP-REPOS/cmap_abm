#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// cmap_transit_assignment.py                                            ///
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
import pandas as pd
import os
import sys
import math
import datetime

EMME_OUTPUT = os.environ["BASE_PATH"] + os.sep + "emme_outputs"

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
    summary = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    #@_m.method(return_type=unicode)
    #def tool_run_msg_status(self):
    #    return self.tool_run_msg

    def __init__(self):
        #self.assignment_only = False
        self.scenario = _m.Modeller().scenario
        self.num_processors = "MAX-1"
        #self.attributes = ["period", "scenario", "assignment_only", "skims_only",  "num_processors"]
        self._dt_db = _m.Modeller().desktop.project.data_tables()
        self._matrix_cache = {}  # used to hold data for reporting and post-processing of skims
        #for initializing matrices
        self._all_periods = ["1", "2", "3", "4", "5", "6", "7", "8"]
        self.periodLabel = {1: "NT", 2: "EA", 3: "AM", 4: "MM", 5: "MD", 6: "AF", 7: "PM", 8: "EV"}
        self.periods = self._all_periods[:]
        #self.attributes = ["components", "periods", "delete_all_existing"]
        self._matrices = {}
        self._count = {}
        self.user_classes = ["1","2","3"]
        self.user_class_labels = {1: "L", 2: "M", 3: "H"}
        self.cost_percep = {"uc1": 0.12, "uc2": 0.06, "uc3": 0.04} # by user class
        self.vots = {"uc1": 3.83, "uc2": 12, "uc3": 40}  # by user class - 2.3 $/hr (3.83 cents/min), 7.2 $/hr (12 cents/min), 24.0 $/hr (40 cents/min)
        self.clean_importance = {"uc1":0.5, "uc2": 0.75, "uc3":1.0} #by user_class
        self.board_cost_per = self.cost_percep
        self.acc_egr_walk_percep = "2"
        self.acc_egr_drive_percep = "2"
        self.xfer_walk_percep = "3"
        self.acc_spd_fac = {"WALK": "3.0", "PNROUT": "25.0", "PNRIN": "3.0", "KNROUT": "25.0", "KNRIN": "3.0"}
        self.egr_spd_fac = {"WALK": "3.0", "PNROUT": "3.0", "PNRIN": "25.0", "KNROUT": "3.0", "KNRIN": "25.0"}
        self.xfer_penalty = "15.0"
        self.skim_matrices = ["GENCOST", "FIRSTWAIT", "XFERWAIT", "TOTALWAIT", "FARE", "XFERS", "ACC", "XFERWALK", "EGR", 
                                "TOTALAUX", "TOTALIVTT", "DWELLTIME", "CTABUSLIVTT", "PACEBUSRIVTT", "PACEBUSLIVTT", 
                                "PACEBUSEIVTT", "CTABUSEIVTT", "CTARAILIVTT", "METRARAILIVTT"]
        self.basematrixnumber = 500


    def __call__(self, period, matrix_count, scenario, assignment_only=False, skims_only=False, summary=False,
                 num_processors="MAX-1"):
        attrs = {
            "period": period,
            "scenario": scenario.id,
            "assignment_only": assignment_only,
            "skims_only": skims_only,
            "summary": summary,
            "num_processors": num_processors,
            "self": str(self)
        }
        self.scenario = scenario
        print("transit assignment for period %s scenario %s" % (period, scenario))
        with self.setup(attrs):
            #gen_utils.log_snapshot("Transit assignment", str(self), attrs)
            
            periods = self.periods
            if not period in periods:
                raise Exception('period: unknown value - specify one of %s' % periods)
            
            num_processors = attrs["num_processors"] #dem_utils.parse_num_processors(num_processors)
            #params = self.get_perception_parameters(period)

            self.generate_matrix_list(period, self.scenario)
            
            self.transit_skim_matrices(period, scenario, num_processors)

            network = scenario.get_partial_network(
                element_types=["TRANSIT_LINE"], include_attributes=True)
            
            day_pass = 5*100/2.0  # in cents.
            regional_pass = 100*100/2.0 #in cents. #TODO - confirm if needed

            walk_modes = ["u", "x", "m", "c", "b", "r", "t", "d"]
            PNROUT_modes = ["v", "x", "m", "c", "b", "r", "t", "d"]
            PNRIN_modes = ["u", "y", "m", "c", "b", "r", "t", "d"]
            KNROUT_modes = ["w", "x", "m", "c", "b", "r", "t", "d"]
            KNRIN_modes = ["u", "z", "m", "c", "b", "r", "t", "d"]
            local_bus_mode = ["B", "P", "L"]
            premium_modes = ["C", "M", "E", "Q"]

            if skims_only:
                access_modes = ["WALK"]                
                skim_parameters = OrderedDict([
                    ("WALK", {
                        "modes": walk_modes + local_bus_mode + premium_modes,
                        "journey_levels": []
                    }),
                ])
            else: 
                access_modes = ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]    
                skim_parameters = OrderedDict([
                    ("WALK", {
                        "modes": walk_modes + local_bus_mode + premium_modes,
                        "journey_levels": []
                    }),
                    ("PNROUT", {
                        "modes": PNROUT_modes + local_bus_mode + premium_modes,
                        "journey_levels": []
                    }),
                    ("PNRIN", {
                        "modes": PNRIN_modes + local_bus_mode + premium_modes,
                        "journey_levels": []
                    }),
                    ("KNROUT", {
                        "modes": KNROUT_modes + local_bus_mode + premium_modes,
                        "journey_levels": []
                    }),                
                    ("KNRIN", {
                        "modes": KNRIN_modes + local_bus_mode + premium_modes,
                        "journey_levels": []
                    }),
                ])
            self.calculate_boarding_fare()
            self.calculate_zone_fare()            
            self.calculate_xfer_fare()
            self.calculate_xfer_penalty()
            self.calculate_default_eff_headway()
            if summary:
                create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")           
                temp_matrix = create_matrix(matrix_id = "ms1", matrix_name = "TEMP_SUM", matrix_description = "temp mf sum", scenario = self.scenario, default_value = 0, overwrite = True)
            for amode, skim_params in skim_parameters.iteritems():
                for uc in self.user_classes:
                    #print("calculate network attributes")
                    self.calc_network_attribute(uc)

                    #parameters by period and user class
                    params = self.get_perception_parameters(period, "uc%s" % (uc), amode)
                    skim_params["journey_levels"] = self.all_modes_journey_levels(params, "uc%s" % (uc))                    
                    #print("run transit assignment for period=%s, user class=%s"%(self.periodLabel[int(period)], self.user_class_labels[int(uc)]))
                    self.run_assignment(period, amode, uc, skim_params, params, network, skims_only, num_processors)
                    
                    if not assignment_only:
                        self.run_skims(period, amode, int(uc), params, regional_pass, skims_only, summary, num_processors, network)
                        #self.report(period)  #use sandag specific utilites.                         
                
            return(self._count['mf']-8500)

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
        #self._matrices = dict(
            #(name, dict((k, []) for k in self._all_periods + ["ALL"]))
            #for name in self._all_components)

        #every period - 30 demand matrices (27 skims + 3 demand) per userclass per access mode .
        self._count = {"ms": 2, "md": 100, "mo": 100, "mf": 8500 + 27*3*5} #"mf"=100

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

    def calculate_boarding_fare(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")            
        Metra_fare = {
            "result": "ut1",
            "expression": "400",
            "selections": {
                "transit_line": "mode=M"
            },
            "type": "NETWORK_CALCULATION"
        }
        CTARail_fare = {
            "result": "ut1",
            "expression": "250",
            "selections": {
                "transit_line": "mode=C"
            },
            "type": "NETWORK_CALCULATION"
        }
        CTABus_fare = {
            "result": "ut1",
            "expression": "225",
            "selections": {
                "transit_line": "mode=BE"
            },
            "type": "NETWORK_CALCULATION"
        }
        PaceExpBus_fare = {
            "result": "ut1",
            "expression": "450",
            "selections": {
                "transit_line": "mode=Q"
            },
            "type": "NETWORK_CALCULATION"
        }
        PaceBus_fare = {
            "result": "ut1",
            "expression": "200",
            "selections": {
                "transit_line": "mode=PL"
            },
            "type": "NETWORK_CALCULATION"
        }          
        netcalc([Metra_fare, CTARail_fare, CTABus_fare, PaceExpBus_fare, PaceBus_fare])

    def calculate_zone_fare(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")  
        zfare={
            "result": "@zfare",
            "expression": "@zfare_link",
            "selections": {
                "link": "all",
                "transit_line": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        netcalc(zfare)

    def calculate_xfer_fare(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")          
        if not scenario.extra_attribute("@xfer_fare"):
            create_extra(extra_attribute_type="TRANSIT_LINE",
                                extra_attribute_name="@xfer_fare",
                                extra_attribute_description="Transfer Fare")
        Metra_fare = {
            "result": "@xfer_fare",
            "expression": "ut1",
            "selections": {
                "transit_line": "mode=M"
            },
            "type": "NETWORK_CALCULATION"
        }
        Pace_express_fare = {
            "result": "@xfer_fare",
            "expression": "280",
            "selections": {
                "transit_line": "mode=Q"
            },
            "type": "NETWORK_CALCULATION"
        }        
        Other_fare = {
            "result": "@xfer_fare",
            "expression": "25",
            "selections": {
                "transit_line": "mode=BECPL"
            },
            "type": "NETWORK_CALCULATION"
        }        
        netcalc([Metra_fare, Pace_express_fare, Other_fare], scenario=scenario)

        if not scenario.extra_attribute("@xfer_fare_q"):
            create_extra(extra_attribute_type="TRANSIT_LINE",
                                extra_attribute_name="@xfer_fare_q",
                                extra_attribute_description="Transfer Fare from Pace Express")
        Metra_fare = {
            "result": "@xfer_fare_q",
            "expression": "ut1",
            "selections": {
                "transit_line": "mode=M"
            },
            "type": "NETWORK_CALCULATION"
        }
        Other_fare = {
            "result": "@xfer_fare_q",
            "expression": "30",
            "selections": {
                "transit_line": "mode=BECPLQ"
            },
            "type": "NETWORK_CALCULATION"
        }        
        netcalc([Metra_fare, Other_fare], scenario=scenario)            

    def calculate_xfer_penalty(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")    
        if not scenario.extra_attribute("@xfer_pen"):
            create_extra(extra_attribute_type="NODE",
                                extra_attribute_name="@xfer_pen",
                                extra_attribute_description="Transfer Penalty")
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")
        xfer_pen = {
            "result": "@xfer_pen",
            "expression": "@timbf+%s"%self.xfer_penalty,
            "selections": {
                "node": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        netcalc(xfer_pen, scenario=scenario)   

    def calculate_default_eff_headway(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")
        eff_hdw = {
            "result": "@hdwef",
            "expression": "@hdway*@hfrac",
            "selections": {
                "link": "all",
                "transit_line": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        netcalc(eff_hdw, scenario=scenario)          

    def get_perception_parameters(self, period, user_class, amode):
        access = self.acc_egr_walk_percep
        egress = self.acc_egr_walk_percep
        if amode in ["PNROUT", "KNROUT"]:
            access = self.acc_egr_drive_percep
        elif amode in ["PNRIN", "KNRIN"]:
            egress = self.acc_egr_drive_percep
        perception_parameters = {
            "1": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "2": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "3": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "4": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "5": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "6": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "7": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            },
            "8": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "xfer_headway": "@headway_op",
                "fare": self.board_cost_per[user_class],
                "in_vehicle": "@ivtpf",
            }
        }
        return perception_parameters[period]

    #@_m.logbook_trace("Generate network attributes", save_arguments=True)
    def calc_network_attribute(self, user_class):
        modeller = _m.Modeller()
        scenario = self.scenario
        #emmebank = scenario.emmebank

        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")
        #create_extra_attribute = modeller.tool("inro.emme.data.extra_attribute.create_extra_attribute")

        #current_scenario = my_emmebank.scenario(scen)
        #tranScen = database.scenario_by_number(scen)
        #data_explorer.replace_primary_scenario(tranScen)

        network = scenario.get_network()
        new_attrs = [
            #("TRANSIT_LINE", "@xfer_from_day", "Fare for xfer from daypass/trolley"),
            #("TRANSIT_LINE", "@xfer_from_premium", "Fare for first xfer from premium"),
            #("TRANSIT_LINE", "@xfer_from_coaster", "Fare for first xfer from coaster"),
            #("TRANSIT_LINE", "@xfer_regional_pass", "0-fare for regional pass"),
            #("TRANSIT_SEGMENT", "@xfer_from_bus", "Fare for first xfer from bus"),
            #("TRANSIT_SEGMENT", "@headway_seg", "Headway adj for special xfers"),
            #("TRANSIT_SEGMENT", "@transfer_penalty_s", "Xfer pen adj for special xfers"),
            ("TRANSIT_SEGMENT", "@layover_board", "Boarding cost adj for special xfers"),
            #("TRANSIT_SEGMENT", "@boardpm", "Boarding cost percep"),
            ("TRANSIT_SEGMENT", "@ivtf", "In-vehicle time factor"),
        ]
        #if there are circular lines or loops, @layover_board needs to be populated by transit line and node
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
        #netcalc(segCostP_calc, scenario=scenario) # num_processors=self.num_processors

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
        netcalc(segCostP_calc, scenario=scenario)

        #new - calculate @ivtf as average of @ivtf by user class.
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
        #netcalc(segCostP_calc, scenario=scenario) # num_processors=self.num_processors

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

        netcalc([Pace_InVeh, metra_InVeh, metra_InVeh_noCook], scenario=scenario)

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

        netcalc([metra_electric, cta_red, metra_hc], scenario=scenario)

        initial = 1
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

            netcalc([Pace_InVeh], scenario=scenario)

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
                        "next_journey_level": 6
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
                        "next_journey_level": 6
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "next_journey_level": 7
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "penalty": "@xfer_fare",
                        "perception_factor": board_cost_per
                    }
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
                        "next_journey_level": 7
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "penalty": "@xfer_fare",
                        "perception_factor": board_cost_per
                    }
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
                        "next_journey_level": 6
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "penalty": "@xfer_fare",
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
                        "next_journey_level": 7
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "penalty": "@xfer_fare",
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
            {  # Pace Express No CTA 6
                "description": "Boarded Pace Express, no CTA",
                "destinations_reachable": True,
                "transition_rules": [
                    {
                        "mode": "B",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "C",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "E",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "L",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "M",
                        "next_journey_level": 6
                    },
                    {
                        "mode": "P",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "Q",
                        "next_journey_level": 6
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "penalty": "@xfer_fare_q",
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
            {  # CTA and Pace Express 7
                "description": "Boarded CTA and Pace Express",
                "destinations_reachable": True,
                "transition_rules": [
                    {
                        "mode": "B",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "C",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "E",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "L",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "M",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "P",
                        "next_journey_level": 7
                    },
                    {
                        "mode": "Q",
                        "next_journey_level": 7
                    }
                ],
                "boarding_time": {
                    "at_nodes": {
                        "penalty": "@xfer_pen",
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
                        "penalty": "@xfer_fare_q",
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

    #@_m.logbook_trace("Transit skim matrices initialize", save_arguments=True)
    def transit_skim_matrices(self, period, scenario, num_processors):
        idx = self.basematrixnumber
        for amode in ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]:        
            for uc in self.user_classes:
                for mname in self.skim_matrices:
                    #print "%s_%s_%s__%s"%(mname, amode, self.user_class_labels[int(uc)], self.periodLabel[int(period)])
                    temp = self.add_matrices(int(period) * 1000 + idx, "%s_%s_%s__%s"%(mname, amode, self.user_class_labels[int(uc)], self.periodLabel[int(period)]), scenario)
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
            ("ACC",    "access walk time"),
            ("XFERWALK",   "transfer walk time"),
            ("EGR",    "egress walk time"),
            ("TOTALAUX",  "total walk time"),
            ("TOTALIVTT",  "in-vehicle time"),
            ("DWELLTIME",  "dwell time"),
            ("CTABUSLIVTT",    "CTA bus in-vehicle time"),
            ("PACEBUSRIVTT", "CTA bus in-vehicle time"),
            ("PACEBUSLIVTT", "CTA bus in-vehicle time"),
            ("PACEBUSEIVTT", "CTA bus in-vehicle time"),
            ("CTABUSEIVTT", "CTA bus in-vehicle time"),
            ("CTARAILIVTT", "CTA bus in-vehicle time"),
            ("METRARAILIVTT", "CTA bus in-vehicle time")
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
    def run_assignment(self, period, amode, user_class, skim_parameters, params, network, skims_only, num_processors):
        modeller = _m.Modeller()
        scenario = self.scenario
        emmebank = scenario.emmebank
        assign_transit = modeller.tool("inro.emme.transit_assignment.extended_transit_assignment")

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
            "aux_transit_time": {"perception_factor": "@aperf"},
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
        
        if ((amode == "WALK") and (user_class == "1")): # first assignment
            add_volumes = False
        else: 
            add_volumes = True
        spec = _copy(base_spec)
        self.define_aux_perception(amode)
        
        name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[int(user_class)], self.periodLabel[int(period)])
        print "run transit assignment for %s"%name 
        spec["modes"] = skim_parameters["modes"]
        #print(skim_parameters["modes"])
        spec["demand"] = 'mf%s' % (name)
        spec["journey_levels"] = skim_parameters["journey_levels"]        
        # print('boarding perception %s'%(spec["journey_levels"][4]["boarding_cost"]["on_lines"]["perception_factor"]))
        spec["in_vehicle_cost"]["perception_factor"] = float(self.cost_percep["uc%s" % (user_class)])
        spec["boarding_cost"]["on_lines"]["perception_factor"] = float(self.cost_percep["uc%s" % (user_class)])
        #spec["in_vehicle_time"]["perception_factor"] = "@ivtpf" #"@ivtf%s" % (user_class)
        #spec["aux_transit_time"]["perception_factor"] = "@aperf"
        assign_transit(spec, class_name=name, add_volumes=add_volumes, scenario=self.scenario)
        #add_volumes = True

    #@_m.logbook_trace("Extract skims", save_arguments=True)
    def run_skims(self, period, amode, user_class, params, max_fare, skims_only, summary, num_processors, network):
        modeller = _m.Modeller()
        scenario = self.scenario
        #emmebank = scenario.emmebank
        matrix_calc = modeller.tool(
            "inro.emme.matrix_calculation.matrix_calculator")
        #netcalc = modeller.tool(
        #    "inro.emme.network_calculation.network_calculator")
        matrix_results = modeller.tool(
            "inro.emme.transit_assignment.extended.matrix_results")
        path_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.path_based_analysis")
        strategy_analysis = modeller.tool(
            "inro.emme.transit_assignment.extended.strategy_based_analysis")
        class_name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[user_class], self.periodLabel[int(period)])
        #skim_name = "%s" % (name)
        #self.run_skims.logbook_cursor.write(name="Extract skims for %s, using assignment class %s" % (name, class_name))

        with _m.logbook_trace("First and total wait time, number of boardings, fares, total walk time, in-vehicle time"):
            # First and total wait time, number of boardings, fares, total walk time, in-vehicle time
            spec = {
                "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                "actual_first_waiting_times": 'mf"FIRSTWAIT_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "actual_total_waiting_times": 'mf"TOTALWAIT_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "total_impedance": 'mf"GENCOST_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "by_mode_subset": {
                    "modes": [mode.id for mode in network.modes() if mode.type == "TRANSIT" or mode.type == "AUX_TRANSIT"],
                    #"modes": ["u", "x", "m", "c", "b", "r", "t", "d", "B", "P", "L", "C", "M", "E", "Q"],
                    "avg_boardings": 'mf"XFERS_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                    "actual_in_vehicle_times": 'mf"TOTALIVTT_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                    "actual_in_vehicle_costs": 'mf"TEMP_IN_VEHICLE_COST"',
                    "actual_total_boarding_costs": 'mf"FARE_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                    "perceived_total_boarding_costs": 'mf"TEMP_PERCEIVED_FARE"',
                    "actual_aux_transit_times": 'mf"TOTALAUX_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
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
                dist = 'mf"%sDIST_%s_%s__%s"' % (mode_name, amode, self.user_class_labels[user_class], self.periodLabel[int(period)]) if "DIST" in skim_types else None
                ivtt = 'mf"%sIVTT_%s_%s__%s"' % (mode_name, amode, self.user_class_labels[user_class], self.periodLabel[int(period)]) if "IVTT" in skim_types else None
                spec = {
                    "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                    "by_mode_subset": {
                        "modes": modes,
                        #"distance": dist,
                        "actual_in_vehicle_times": ivtt,
                    },
                }
                matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
            # Sum total distance
            #spec = {
            #    "type": "MATRIX_CALCULATION",
            #    "constraint": None,
            #    "result": 'mf"TOTTRNDIST_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
            #    "expression": ('mf"CTABUSLDIST_{amode}_{uc}__{period}" + mf"PACEBUSRDIST_{amode}_{uc}__{period}" + mf"PACEBUSLDIST_{amode}_{uc}__{period}"'
            #                ' + mf"PACEBUSEDIST_{amode}_{uc}__{period}" + mf"CTABUSEDIST_{amode}_{uc}__{period}"  + mf"CTARAILDIST_{amode}_{uc}__{period}"'
            #                ' + mf"METRARAILDIST_{amode}_{uc}__{period}"').format(amode=amode, uc=self.user_class_labels[user_class], period=self.periodLabel[int(period)]),
            #}
            #matrix_calc(spec, scenario=scenario, num_processors=num_processors)

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
            self.mask_highvalues_temp("TEMP_LAYOVER_BOARD", scenario, num_processors)
            self.mask_highvalues_temp("TEMP_IN_VEHICLE_COST", scenario, num_processors)
            self.mask_highvalues("FARE", amode, self.user_class_labels[user_class], self.periodLabel[int(period)], scenario, num_processors)
            #export_matrix_data = _m.Modeller().tool("inro.emme.data.matrix.export_matrix_to_csv")
            #export_matrix_data(matrices=["mfTEMP_LAYOVER_BOARD", "mfTEMP_IN_VEHICLE_COST", 'mf"FARE_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)])])
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"XFERS_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                        "interval_min": 0, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"XFERS_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "expression": '(XFERS_%s_%s__%s - 1 - TEMP_LAYOVER_BOARD).max.0' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

            # sum in-vehicle cost and boarding cost to get the fare paid
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"FARE_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "expression": '(FARE_%s_%s__%s + TEMP_IN_VEHICLE_COST).min.%s' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)], max_fare),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # access time - get distance and convert to time with 3 miles / hr for walk, 25 miles / hr for PNR and KNR
        with _m.logbook_trace("Walk time access, egress and xfer"):
            path_spec = {
                "portion_of_path": "ORIGIN_TO_INITIAL_BOARDING",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"ACC_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            # walk egress time - get distance and convert to time with 3 miles / hr for walk, 25 miles / hr for PNR and KNR
            path_spec = {
                "portion_of_path": "FINAL_ALIGHTING_TO_DESTINATION",
                "trip_components": {"aux_transit": "length",},
                "path_operator": "+",
                "path_selection_threshold": {"lower": 0, "upper": 999999 },
                "path_to_od_aggregation": {
                    "operator": "average",
                    "aggregated_path_values": 'mf"EGR_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)])
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            spec_list = [
            {    # walk access time - convert to time
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"ACC_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "expression": '60.0 * ACC_%s_%s__%s / %s' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)], self.acc_spd_fac[amode]),
            },
            {    # walk egress time - convert to time
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"EGR_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "expression": '60.0 * EGR_%s_%s__%s / %s' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)], self.egr_spd_fac[amode]),
            },
            {   # transfer walk time = total - access - egress
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"XFERWALK_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "expression": '(TOTALAUX_{amode}_{uc}__{period} - ACC_{amode}_{uc}__{period}'
                            ' - EGR_{amode}_{uc}__{period}).max.0'.format(amode=amode, uc=self.user_class_labels[user_class], 
                                                                                    period=self.periodLabel[int(period)]),
            }]
            matrix_calc(spec_list, scenario=scenario, num_processors=num_processors)

        # transfer wait time
        with _m.logbook_trace("Wait time - xfer"):
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"TOTALWAIT_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                        "interval_min": 0, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"XFERWAIT_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                "expression": '(TOTALWAIT_{amode}_{uc}__{period} - FIRSTWAIT_{amode}_{uc}__{period}).max.0'.format(amode=amode, 
                                                                                                                uc=self.user_class_labels[user_class], 
                                                                                                                period=self.periodLabel[int(period)]),
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
                        "strategy_values": 'mf"DWELLTIME_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
                    },
                    "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
                }
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

        expr_params = _copy(params)
        expr_params["xfers"] = 15.0
        expr_params["amode"] = amode
        expr_params["period"] = self.periodLabel[int(period)]
        expr_params["uc"] = self.user_class_labels[user_class]
        expr_params["fare"] = float(self.cost_percep["uc%s" % (user_class)])
        spec = {
            "type": "MATRIX_CALCULATION",
            "constraint": None,
            "result": 'mf"GENCOST_%s_%s__%s"' % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)]),
            "expression": ("{xfer_wait} * TOTALWAIT_{amode}_{uc}__{period} "
                        "- ({xfer_wait} - {init_wait}) * FIRSTWAIT_{amode}_{uc}__{period} "
                        "+ 1.0 * TOTALIVTT_{amode}_{uc}__{period}"
                        "+ (1 / {vot}) * (TEMP_PERCEIVED_FARE + {fare} * TEMP_IN_VEHICLE_COST)" # or "(1 / {vot}) * {fare} * FARE_{amode}_{uc}__{period}"
                        "+ {xfers} *(XFERS_{amode}_{uc}__{period}.max.0) "
                        "+ {access} * ACC_{amode}_{uc}__{period} "
                        "+ {egress} * EGR_{amode}_{uc}__{period} "
                        "+ {xfer_walk} * XFERWALK_{amode}_{uc}__{period}").format(**expr_params)
        }
        matrix_calc(spec, scenario=scenario, num_processors=num_processors)
        self.mask_highvalues_all(amode, self.user_class_labels[user_class], self.periodLabel[int(period)], scenario, num_processors)
        if summary:
            compute_matrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")
            # export max, average, sum for each transit skim matrix
            data = []
            for name in self.skim_matrices:
                skim_name = "%s_%s_%s__%s" % (name, amode, self.user_class_labels[user_class], self.periodLabel[int(period)])
                spec_sum={
                    "expression": skim_name,
                    "result": "msTEMP_SUM",
                    "aggregation": {
                        "origins": "+",
                        "destinations": "+"
                    },
                    "type": "MATRIX_CALCULATION"
                }
                report = compute_matrix(spec_sum) 
                data.append([skim_name, report["maximum"], report["maximum_at"]["origin"], report["maximum_at"]["destination"], 
                            report["average"], report["sum"]])
            df = pd.DataFrame(data, columns=['Skim', 'Max', 'Max orig', 'Max dest', 'Avg', 'Sum'])
            filename = "%s\\matrix_list_%s.csv"%(EMME_OUTPUT, datetime.date.today())
            df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')
            # export number of OD pairs with non-zero in-vehicle time by transit mode
            data = [self.periodLabel[int(period)], amode, self.user_class_labels[user_class]]
            #in_veh_name = ["CTABUSLIVTT", "PACEBUSRIVTT", "PACEBUSLIVTT", "PACEBUSEIVTT", "CTABUSEIVTT", "CTARAILIVTT", "METRARAILIVTT"]
            spec = "_%s_%s__%s" % (amode, self.user_class_labels[user_class], self.periodLabel[int(period)])
            modes = ["bus", "CTA rail", "Metra rail", "bus and CTA rail", "bus and Metra rail", "CTA rail and Metra rail", "bus, CTA rail and Metra rail"]
            expressions = ["CTABUSLIVTT%s+CTABUSEIVTT%s+PACEBUSLIVTT%s+PACEBUSEIVTT%s+PACEBUSRIVTT%s"%(spec,spec,spec,spec,spec),
                            "CTARAILIVTT%s"%(spec), 
                            "METRARAILIVTT%s"%(spec), 
                            "(CTABUSLIVTT%s+CTABUSEIVTT%s+PACEBUSLIVTT%s+PACEBUSEIVTT%s+PACEBUSRIVTT%s)*CTARAILIVTT%s"%(spec,spec,spec,spec,spec,spec),
                            "(CTABUSLIVTT%s+CTABUSEIVTT%s+PACEBUSLIVTT%s+PACEBUSEIVTT%s+PACEBUSRIVTT%s)*METRARAILIVTT%s"%(spec,spec,spec,spec,spec,spec),
                            "CTARAILIVTT%s*METRARAILIVTT%s"%(spec,spec),
                            "(CTABUSLIVTT%s+CTABUSEIVTT%s+PACEBUSLIVTT%s+PACEBUSEIVTT%s+PACEBUSRIVTT%s)*CTARAILIVTT%s*METRARAILIVTT%s"%(spec,spec,spec,spec,spec,spec,spec)]
            for mode, expression in zip(modes, expressions):
                spec_sum={
                    "expression": "(%s)>0" % expression,
                    "result": "msTEMP_SUM",
                    "aggregation": {
                        "origins": "+",
                        "destinations": "+"
                    },
                    "type": "MATRIX_CALCULATION"
                }
                report = compute_matrix(spec_sum) 
                data.append(report["sum"])
            header = ["Period", "Access Mode", "User Class"]
            header.extend(modes)
            df = pd.DataFrame([data], columns=header)
            filename = "%s\\transit_skim_OD_summary_%s.csv"%(EMME_OUTPUT, datetime.date.today())
            df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')            
        return

    def define_aux_perception(self, amode):
        modeller = _m.Modeller()
        scenario = self.scenario
        #emmebank = scenario.emmebank
        create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")    
        create_extra(extra_attribute_type="LINK",
                            extra_attribute_name="@aperf",
                            extra_attribute_description="auxiliary transit perception factor",
                            overwrite=True)        
        net_calc = modeller.tool("inro.emme.network_calculation.network_calculator")
        xfer_walk={
            "result": "@aperf",
            "expression": self.xfer_walk_percep,
            "selections": {
                "link": "modes=bcmdrt",
            },
            "type": "NETWORK_CALCULATION"
        }
        acc_egr_walk={
            "result": "@aperf",
            "expression": self.acc_egr_walk_percep,
            "selections": {
                "link": "modes=ux",
            },
            "type": "NETWORK_CALCULATION"
        }
        net_calc([xfer_walk, acc_egr_walk])       
        if amode in ["PNROUT", "KNROUT"]: # overwrite perception on PNR/KNR access links 
            drive_access_percep = {
                "result": "@aperf" ,
                "expression": self.acc_egr_drive_percep,
                "selections": {
                    "link": "modes=vw",
                },
                "type": "NETWORK_CALCULATION"
            }
            net_calc(drive_access_percep)
        elif amode in ["PNRIN", "KNRIN"]: # overwrite perception on PNR/KNR egress links
            drive_egress_percep = {
                "result": "@aperf" ,
                "expression": self.acc_egr_drive_percep,
                "selections": {
                    "link": "modes=yz",
                },
                "type": "NETWORK_CALCULATION"
            }
            net_calc(drive_egress_percep)
        else: # "WALK", default setting, no changes required
            pass

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
    def mask_highvalues_all(self, amode, uc, period, scenario, num_processors):
        
        #print(matrices)
        matrix_calc = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

        # Set high values to 0
        with _m.logbook_trace("Set high values to 0"):            
            for name in self.skim_matrices:                
                spec = {
                    "type": "MATRIX_CALCULATION",
                    "constraint":{
                        "by_value": {
                            "od_values": "%s_%s_%s__%s" % (name, amode, uc, period),
                            "interval_min": -999999, "interval_max": 999999,
                            "condition": "EXCLUDE"},
                    },
                    "result": "%s_%s_%s__%s" % (name, amode, uc, period),
                    "expression": '0',
                }
                matrix_calc(spec, scenario=scenario, num_processors=num_processors)

    def mask_highvalues(self, name, amode, uc, period, scenario, num_processors):
        
        #print(matrices)
        matrix_calc = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

        # Set high values to 0
        with _m.logbook_trace("Set high values to 0"):            
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": "%s_%s_%s__%s" % (name, amode, uc, period),
                        "interval_min": -999999, "interval_max": 999999,
                        "condition": "EXCLUDE"},
                },
                "result": "%s_%s_%s__%s" % (name, amode, uc, period),
                "expression": '0',
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

    def mask_highvalues_temp(self, name, scenario, num_processors):
        
        #print(matrices)
        matrix_calc = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

        # Set high values to 0
        with _m.logbook_trace("Set high values to 0"):            
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": "%s" % name,
                        "interval_min": -999999, "interval_max": 999999,
                        "condition": "EXCLUDE"},
                },
                "result": "%s" % name,
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

