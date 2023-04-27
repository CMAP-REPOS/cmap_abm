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
# Inputs:
#   period: the time-of-day period, one of NT, EA, AM, MM, MD, AF, PM, EV.
#   scenario: Transit assignment scenario
#   skims_only: Only run assignment for skim matrices, if True only three assignments
#       are run to generate the skim matrices for walk transit.
#       Otherwise, all 15 assignments are run to generate the total network flows.
#   num_processors: number of processors to use for the traffic assignments.
#
# Matrices:
#   All transit demand and skim matrices.
#   See list of matrices under self.skim_matrices.
#

TOOLBOX_ORDER = 21


import inro.modeller as _m
import inro.emme.core.exception as _except
import inro.emme.desktop.worksheet as worksheet
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

try:
    basestring
except NameError:
    basestring = str

EMME_OUTPUT = os.environ["EMME_OUTPUT"]
_vcr_adjustment_factor = {
    'NT': 1,
    'AM': 1.219,
    'MD': 1,
    'PM': 1.262,
}
_hours_in_period = {
    'NT': 12,  # 6:00 PM to 6:00 AM
    'AM': 3,  # 6:00 AM to 9:00 AM
    'MD': 7,  # 9:00 AM to 4:00 PM
    'PM': 2,  # 4:00 PM to 6:00 PM
}
_segment_cost_function = """
values = scenario.get_attribute_values("TRANSIT_VEHICLE", ["seated_capacity"])
network.set_attribute_values("TRANSIT_VEHICLE", ["seated_capacity"], values)

min_seat_weight = 1.0
max_seat_weight = 1.4
power_seat_weight = 2.2
min_stand_weight = 1.4
max_stand_weight = 1.6
power_stand_weight = 3.4

def calc_segment_cost(transit_volume, capacity, segment):
    if transit_volume == 0:
        return 0.0
    line = segment.line
    # need assignment period in seated_capacity calc?
    # capacity adjusted for full time period
    seated_capacity = line.vehicle.seated_capacity * {0} * 60 / line.headway
    # volume is adjusted to reflect peak period
    adj_transit_volume = transit_volume * {1}
    num_seated = min(adj_transit_volume, seated_capacity)
    num_standing = max(adj_transit_volume - seated_capacity, 0)

    # adjusting transit volume to account for peak period spreading
    vcr = adj_transit_volume / capacity
    crowded_factor = (((
        (min_seat_weight+(max_seat_weight-min_seat_weight)*(vcr)**power_seat_weight)*num_seated
        +(min_stand_weight+(max_stand_weight-min_stand_weight)*(vcr)**power_stand_weight)*num_standing
        )/(adj_transit_volume+0.01)))

    # Toronto implementation limited factor between 1.0 and 10.0

    # subtracting 1 from factor since new transit times are calculated as:
    #  trtime_new = trtime_old * (1 + crowded_factor), but Toronto was derived
    #  using trtime_new = trtime_old * (crowded_factor)
    #  (see https://app.asana.com/0/0/1185777482487173/1202006990557038/f)
    crowded_factor = max(crowded_factor - 1, 0)
    return crowded_factor
"""
_headway_cost_function = """
max_hdwy_growth = 1.5
max_hdwy = 999.98

def calc_eawt(segment, vcr, headway):
    # EAWT_AM = 0. 259625 + 1. 612019*(1/Headway) + 0.005274*(Arriving V/C) + 0. 591765*(Total Offs Share)
    # EAWT_MD = 0. 24223 + 3.40621* (1/Headway) + 0.02709*(Arriving V/C) + 0. 82747 *(Total Offs Share)
    line = segment.line
    prev_segment = line.segment(segment.number - 1)
    alightings = 0
    total_offs = 0
    all_segs = iter(line.segments(True))
    prev_seg = next(all_segs)
    for seg in all_segs:
        total_offs += prev_seg.transit_volume - seg.transit_volume + seg.transit_boardings
        if seg == segment:
            alightings = total_offs
        prev_seg = seg
    if total_offs < 0.001:
        total_offs = 9999  # added due to divide by zero error
    if headway < .01:
        headway = 9999
    eawt = 0.259625 + 1.612019*(1/headway) + 0.005274*(vcr) + 0.591765*(alightings / total_offs)
    # if mode is LRT / BRT mult eawt * 0.4, if HRT /commuter mult by 0.2
    mode_char = line{0}
    #if mode_char in ["E", "Q"]:
    #    eawt_factor = 0.4
    if mode_char in ["C", "M"]:
        eawt_factor = 0.2
    else:
        eawt_factor = 1
    return eawt * eawt_factor


def calc_adj_headway(transit_volume, transit_boardings, headway, capacity, segment):
    prev_hdwy = segment["@phdwy"]
    delta_cap = max(capacity - transit_volume + transit_boardings, 0)
    adj_hdwy = min(max_hdwy, prev_hdwy * min((transit_boardings+1) / (delta_cap+1), 1.5))
    adj_hdwy = max(headway, adj_hdwy)
    return adj_hdwy

def calc_headway(transit_volume, transit_boardings, headway, capacity, segment):
    # multipying vcr by peak period factor
    vcr = transit_volume / capacity * {1}
    # eawt = calc_eawt(segment, vcr, segment.line.headway)
    adj_hdwy = calc_adj_headway(transit_volume, transit_boardings, headway, capacity, segment)
    return adj_hdwy # + eawt

"""

class TransitAssignment(_m.Tool()): #, gen_utils.Snapshot
    __MODELLER_NAMESPACE__ = "cmap"
    #period = _m.Attribute(str)
    #scenario =  _m.Attribute(_m.InstanceType)
    assignment_only = _m.Attribute(bool)
    skims_only = _m.Attribute(bool)
    matrix_summary = _m.Attribute(bool)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

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
        self._all_periods = ["1", "3", "5", "7"]
        self.periodLabel = {1: "NT", 3: "AM", 5: "MD", 7: "PM"}
        self.periods = self._all_periods[:]
        #self.attributes = ["components", "periods", "delete_all_existing"]
        self._matrices = {}
        self._count = {}
        self.user_classes = ["1","2","3"]
        self.user_class_labels = {1: "L", 2: "M", 3: "H"}
        self.vots = {"uc1": 22.65, "uc2": 47.37, "uc3": 125.08}  # by user class - 13.59 $/hr (22.65 cents/min), 28.43 $/hr (47.37 cents/min), 75.05 $/hr (125.08 cents/min)
        self.clean_importance = {"uc1":0.5, "uc2": 0.75, "uc3":1.0} #by user_class
        self.acc_egr_walk_percep = "2"
        self.acc_egr_drive_percep = "2"
        self.xfer_walk_percep = "2"
        self.acc_spd_fac = {"WALK": "3.0", "PNROUT": "25.0", "PNRIN": "3.0", "KNROUT": "25.0", "KNRIN": "3.0"}
        self.egr_spd_fac = {"WALK": "3.0", "PNROUT": "3.0", "PNRIN": "25.0", "KNROUT": "3.0", "KNRIN": "25.0"}
        self.xfer_penalty = "15.0"
        self.skim_matrices = ["GENCOST", "FIRSTWAIT", "XFERWAIT", "TOTALWAIT", "FARE", "XFERS", "ACC", "XFERWALK", "EGR", 
                                "TOTALAUX", "TOTALIVTT", "DWELLTIME", "CTABUSLOCIVTT", "PACEBUSLOCIVTT", "BUSEXPIVTT", "CTARAILIVTT", "METRARAILIVTT", 
                                "CROWD", "CAPPEN"] # "LINKREL", "EAWT"
        self.basematrixnumber = 500


    def __call__(self, period, matrix_count, scenario, assignment_only=False, skims_only=False, matrix_summary=True,
                 export_boardings = True, ccr_periods="AM,PM", num_processors="MAX-1"):
        attrs = {
            "period": period,
            "scenario": scenario.id,
            "assignment_only": assignment_only,
            "skims_only": skims_only,
            "matrix_summary": matrix_summary,
            "num_processors": num_processors,
            "self": str(self)
        }
        self.scenario = scenario
        print("transit assignment for period %s scenario %s" % (period, scenario))
        with self.setup(attrs):
            #gen_utils.log_snapshot("Transit assignment", str(self), attrs)
            create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
            netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")                      
            periods = self.periods
            if not period in periods:
                raise Exception('period: unknown value - specify one of %s' % periods)
            elif (ccr_periods == "ALL") | (self.periodLabel[int(period)] in ccr_periods):
                use_ccr = True
                print("use_ccr %s for %s" % (use_ccr, self.periodLabel[int(period)]))                
                #create_extra("TRANSIT_SEGMENT", "@eawt", "extra added wait time", overwrite=True, scenario=scenario)
                #create_extra("TRANSIT_SEGMENT", "@seg_rel", "link reliability on transit segment", overwrite=True, scenario=scenario)
                # create_extra("TRANSIT_SEGMENT", "@crowding_factor", "crowding factor along segments", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@capacity_penalty", "capacity penalty at boarding", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@tot_capacity", "total capacity", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@seated_capacity", "seated capacity", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@tot_vcr", "volume to total capacity ratio", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@seated_vcr", "volume to seated capacity ratio", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@ccost_skim", "copy of ccost used for skimming", overwrite=True, scenario=scenario)
                create_extra("TRANSIT_SEGMENT", "@hfrac_ccr", "Wait time func as frac of perceived hdwy", overwrite=True, scenario=scenario)
            else:
                use_ccr = False
                print("use_ccr %s for %s" % (use_ccr, self.periodLabel[int(period)]))
                delete_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.delete_extra_attribute")
                try:
                    delete_extra("@capacity_penalty", scenario=scenario)
                    delete_extra("@tot_capacity", scenario=scenario)
                    delete_extra("@seated_capacity", scenario=scenario)
                    delete_extra("@tot_vcr", scenario=scenario)
                    delete_extra("@seated_vcr", scenario=scenario)
                    delete_extra("@ccost_skim", scenario=scenario)
                    delete_extra("@hfrac_ccr", scenario=scenario)
                except:
                    pass

            num_processors = attrs["num_processors"] #dem_utils.parse_num_processors(num_processors)
            #params = self.get_perception_parameters(period)

            self.generate_matrix_list(period, self.scenario) 
            
            self.transit_skim_matrices(period, scenario, num_processors)
            network = scenario.get_network()
            #network = scenario.get_partial_network(["TRANSIT_LINE", "TRANSIT_SEGMENT"], include_attributes=True)
            
            #day_pass = 5*100/2.0  # in cents.
            regional_pass = (15.95+7)*100/2.0 # in cents; max Metra monthly $319/20 days + max CTA/Pace monthly $140/20 days


            create_extra('TRANSIT_SEGMENT', '@zfare', 'incremental zone fare', overwrite=True, scenario=scenario)
            create_extra('TRANSIT_LINE', '@xfer_fare', 'Transfer Fare', overwrite=True, scenario=scenario)
            create_extra('TRANSIT_LINE', '@xfer_fare_q', 'Transfer Fare from Pace Express', overwrite=True, scenario=scenario)
            self.calculate_fare()
            create_extra('NODE', '@xfer_pen', 'Transfer Penalty', overwrite=True, scenario=scenario)
            self.calculate_xfer_penalty()
            create_extra('TRANSIT_LINE', '@hfrac', 'Wait time function as fraction of hdwy', overwrite=True, scenario=scenario)
            create_extra('TRANSIT_SEGMENT', '@hdwef', 'Effective hdwy for capacity constraint', overwrite=True, scenario=scenario)
            self.calculate_default_eff_headway()
            create_extra('LINK', '@aperf', 'auxiliary transit perception factor', overwrite=True, scenario=scenario)
            create_extra('NODE', '@perbf', 'Final boarding time perception factor', 1.0, overwrite=True, scenario=scenario)
            create_extra('TRANSIT_LINE', '@easbp', 'Ease of boarding penalty', overwrite=True, scenario=scenario)
            create_extra('TRANSIT_LINE', '@pctab', 'boarding penalty CTA bus only transfer', overwrite=True, scenario=scenario)
            create_extra('TRANSIT_LINE', '@pctar', 'boarding penalty CTA rail only transfer', overwrite=True, scenario=scenario)            
            easbp={
                "result": "@easbp",
                "expression": "(3-@easeb).max.0",
                "aggregation": None,
                "selections": {
                    "transit_line": "all"
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc(easbp)
            # Pace boardings over-assigned; Pace travel time sometimes incorrectly coded to be faster than CTA
            # adding an extra 15 minutes of boarding penalty to Pace 
            bpPace={
                "result": "@easbp",
                "expression": "@easbp+15",
                "aggregation": None,
                "selections": {
                    "transit_line": "mode=PLQ"
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc(bpPace)
            bp={
                "result": "@pctab",
                "expression": "@easbp+5",
                "aggregation": None,
                "selections": {
                    "transit_line": "mode=CMPLQ"
                },
                "type": "NETWORK_CALCULATION"
            }
            pctab={
                "result": "@pctab",
                "expression": "@easbp",
                "aggregation": None,
                "selections": {
                    "transit_line": "mode=BE"
                },
                "type": "NETWORK_CALCULATION"
            }
            bp2={
                "result": "@pctar",
                "expression": "@easbp+5",
                "aggregation": None,
                "selections": {
                    "transit_line": "mode=BEMPLQ"
                },
                "type": "NETWORK_CALCULATION"
            }            
            pctar={
                "result": "@pctar",
                "expression": "@easbp",
                "aggregation": None,
                "selections": {
                    "transit_line": "mode=C"
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc([bp ,pctab, bp2, pctar])                    
            create_extra('NODE', '@wconf', 'Wait convenience final factor', overwrite=True, scenario=scenario)
            wconf_bus={
                "result": "@wconf",
                "expression": "2.75",
                "selections": {
                    "node": "i=5000,29999"
                },                
                "type": "NETWORK_CALCULATION"
            }
            wconf_rail={
                "result": "@wconf",
                "expression": "2.475",
                "selections": {
                    "node": "i=30000,49999"
                },                
                "type": "NETWORK_CALCULATION"
            }            
            netcalc([wconf_bus, wconf_rail])
            create_extra('TRANSIT_SEGMENT', '@ivtf', 'in-vehicle time factor', 1, overwrite=True, scenario=scenario)
            ivtf_cta={
                "result": "@ivtf",
                "expression": "0.9",
                "selections": {
                    "link": "all",
                    "transit_line": "mode=C"
                },                
                "type": "NETWORK_CALCULATION"
            }
            ivtf_metra={
                "result": "@ivtf",
                "expression": "0.65",
                "selections": {
                    "link": "all",
                    "transit_line": "mode=M"
                },                
                "type": "NETWORK_CALCULATION"
            }            
            netcalc([ivtf_cta, ivtf_metra])
            if matrix_summary:
                create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")           
                temp_matrix = create_matrix(matrix_id = "ms1", matrix_name = "TEMP_SUM", matrix_description = "temp mf sum", scenario = self.scenario, default_value = 0, overwrite = True)

            #self.calc_network_attribute(uc)

            #parameters by period and user class
            #params = self.get_perception_parameters(period, "uc%s" % (uc), amode)
            #skim_params["journey_levels"] = self.all_modes_journey_levels(params, "uc%s" % (uc))   
            print("Running transit assignment for %s" % self.periodLabel[int(period)])          
            self.run_assignment(period, network, skims_only, num_processors, use_ccr)
            
            if not assignment_only:
                for amode in ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]:
                    for uc in self.user_classes:
                        class_name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[int(uc)], self.periodLabel[int(period)])
                        #if scenario.emmebank.matrix(class_name).get_numpy_data(scenario.id).sum() == 0:
                        #    continue   # don't include if no demand
                        self.run_skims(period, amode, int(uc), regional_pass, skims_only, matrix_summary, num_processors, network, use_ccr)
            if export_boardings:
                self.output_transit_boardings(desktop = _m.Modeller().desktop, use_ccr = use_ccr, 
                                                output_location = EMME_OUTPUT, period = self.periodLabel[int(period)])

    @_context.contextmanager
    def setup(self, attrs):
        self._matrix_cache = {}  # initialize cache at beginning of run
        emmebank = self.scenario.emmebank
        period = attrs["period"]
        with _m.logbook_trace("Transit assignment for period %s" % period, attributes=attrs):
            #temp = gen_utils.temp_matrices(emmebank, "FULL", 3)
            with gen_utils.temp_matrices(emmebank, "FULL", 2) as matrices:
                matrices[0].name = "TEMP_IN_VEHICLE_COST"
                #matrices[1].name = "TEMP_LAYOVER_BOARD"
                matrices[1].name = "TEMP_PERCEIVED_FARE"
                try:
                    yield
                finally:
                    self._matrix_cache = {}  # clear cache at end of run                

    def generate_matrix_list(self, period, scenario):
        #every period - 16 skims matrices per userclass per access mode .
        self._count = {"ms": 2, "md": 100, "mo": 100, "mf": 8500 + 16*3*5} #"mf"=100

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

    def calculate_fare(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")
        # boarding fare
        Metra_fare = {
            "result": "ut1",
            "expression": "400*0.725",
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
        # zore fare
        zfare={
            "result": "@zfare",
            "expression": "@zfare_link*0.725",
            "selections": {
                "link": "all",
                "transit_line": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        netcalc(zfare)
        # transfer fare
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
        netcalc([Metra_fare, Pace_express_fare, Other_fare])
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
        netcalc([Metra_fare, Other_fare])

    def calculate_xfer_penalty(self):
        modeller = _m.Modeller()
        scenario = self.scenario
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")
        xfer_pen = {
            "result": "@xfer_pen",
            "expression": "@timbo+%s"%self.xfer_penalty,
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
        hdw_frac_1 = {
            "result": "@hfrac",
            "expression": "0.5",
            "selections": {
                "transit_line": "hdw=0,10"
            },
            "type": "NETWORK_CALCULATION"
        }
        hdw_frac_2 = {
            "result": "@hfrac",
            "expression": "((hdw-10)*.4+5)/hdw",
            "selections": {
                "transit_line": "hdw=10,20"
            },
            "type": "NETWORK_CALCULATION"
        }
        hdw_frac_3 = {
            "result": "@hfrac",
            "expression": "((hdw-20)*.35+9)/hdw",
            "selections": {
                "transit_line": "hdw=20,30"
            },
            "type": "NETWORK_CALCULATION"
        }
        hdw_frac_4 = {
            "result": "@hfrac",
            "expression": "((hdw-30)*2.5/30+12.5)/hdw",
            "selections": {
                "transit_line": "hdw=30,720"
            },
            "type": "NETWORK_CALCULATION"
        }
        eff_hdw = {
            "result": "@hdwef",
            "expression": "hdw*@hfrac",
            "selections": {
                "link": "all",
                "transit_line": "all"
            },
            "type": "NETWORK_CALCULATION"
        }
        netcalc([hdw_frac_1, hdw_frac_2, hdw_frac_3, hdw_frac_4, eff_hdw], scenario=scenario)          

    def get_perception_parameters(self, period, user_class, amode):
        access = self.acc_egr_walk_percep
        egress = self.acc_egr_walk_percep
        if amode in ["PNROUT", "KNROUT"]:
            access = self.acc_egr_drive_percep
        elif amode in ["PNRIN", "KNRIN"]:
            egress = self.acc_egr_drive_percep
        cost_percep = 1 / self.vots[user_class]
        perception_parameters = {
            "1": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "2": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "3": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "4": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "5": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "6": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "7": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            },
            "8": {
                "vot": self.vots[user_class],
                "init_wait": 1.5,
                "xfer_wait": 3,
                "access": access,
                "xfer_walk": self.xfer_walk_percep,
                "egress": egress,
                "eff_headway": "@hdwef",
                "fare_percep": cost_percep,
                "in_vehicle": 1.0,
            }
        }
        return perception_parameters[period]

    def all_modes_journey_levels(self, params, uc_name):
        board_cost_percep = 1 / self.vots[uc_name]

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
                        "penalty": "@timbo",
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
                        "perception_factor": board_cost_percep
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
                        "perception_factor": board_cost_percep
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
                        "penalty": "@pctar",
                        "perception_factor": 1
                    },
                    "on_segments": None
                },
                "boarding_cost": {
                    "global": None,
                    "at_nodes": None,
                    "on_lines": {
                        "penalty": "@xfer_fare",
                        "perception_factor": board_cost_percep
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
                        "penalty": "@pctab",
                        "perception_factor": 1
                    },
                    "on_segments": None
                },
                "boarding_cost": {
                    "global": None,
                    "at_nodes": None,
                    "on_lines": {
                        "penalty": "@xfer_fare",
                        "perception_factor": board_cost_percep
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
                        "perception_factor": board_cost_percep
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
                        "perception_factor": board_cost_percep
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
                        "perception_factor": board_cost_percep
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
                        "perception_factor": board_cost_percep
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
                    temp = self.add_matrices(int(period) * 1000 + idx, "%s_%s_%s__%s"%(mname, amode, self.user_class_labels[int(uc)], self.periodLabel[int(period)]), scenario)
                    idx += 1                    
        return(True)

    def get_strat_spec(self, components, matrix_name):
        """
        Retrieve the default specification template for strategy analysis in Emme.

        Parameters:
            - components: trip components to skim and the variable to sum. trip component
                options are: "boading", "in-vehicle", "alighting", and "aux_transit"
            - matrix_name: name of the matrix that should contain the skim
        Returns:
            - dictionary of the strategy specification
        """
        spec = {
            "trip_components": components,
            "sub_path_combination_operator": "+",
            "sub_strategy_combination_operator": "average",
            "selected_demand_and_transit_volumes": {
                "sub_strategies_to_retain": "ALL",
                "selection_threshold": {"lower": -999999, "upper": 999999}
            },
            "analyzed_demand": None,
            "constraint": None,
            "results": {"strategy_values": matrix_name},
            "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
        }
        return spec

    #@_m.logbook_trace("Transit assignment by demand set", save_arguments=True)
    def run_assignment(self, period, network, skims_only, num_processors, use_ccr):
        modeller = _m.Modeller()
        scenario = self.scenario
        emmebank = scenario.emmebank
        #assign_transit = modeller.tool("inro.emme.transit_assignment.extended_transit_assignment")
        # number of trips in the peak hour / avg number of trips per hour in period
        # factor of 1 means no adjustment
    
        base_spec = {
            "type": "EXTENDED_TRANSIT_ASSIGNMENT",
            "modes": [],
            "demand": "",
            "waiting_time": {
                "effective_headways": None, "headway_fraction": 1,
                "perception_factor": None, "spread_factor": 1.0
            },
            # Fare attributes
            "boarding_cost": {"global": None, "at_nodes": None, "on_lines": {"penalty": "ut1", "perception_factor": None}, "on_segments": None},
            "boarding_time": {"at_nodes": {"penalty": "@timbo", "perception_factor": "@perbf"}, "on_lines": {"penalty": "@easbp", "perception_factor": 1}, "on_segments": None},
            "in_vehicle_cost": {"penalty": "@zfare",
                                "perception_factor": None},
            "in_vehicle_time": {"perception_factor": "@ivtf"},
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

        #if ((amode == "WALK") and (user_class == "1")): # first assignment
        #    add_volumes = False
        #else: 
        #    add_volumes = True
        '''
        spec = _copy(base_spec)
        self.define_aux_perception(amode)
        cost_percep = 1 / params['vot']
        name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[int(user_class)], self.periodLabel[int(period)])
        ("run transit assignment for %s"%name )
        spec["modes"] = skim_parameters["modes"]
        spec["demand"] = 'mf%s' % (name)
        spec["journey_levels"] = skim_parameters["journey_levels"]        
        spec["in_vehicle_cost"]["perception_factor"] = float(cost_percep)
        spec["boarding_cost"]["on_lines"]["perception_factor"] = float(cost_percep)
        '''
        #spec["in_vehicle_time"]["perception_factor"] = "@ivtpf" #"@ivtf%s" % (user_class)
        #spec["aux_transit_time"]["perception_factor"] = "@aperf"
        #assign_transit(spec, class_name=name, add_volumes=add_volumes, scenario=self.scenario)
        mode_attr = '.mode.id' #'["#src_mode"]'
        if use_ccr:
            assign_transit = modeller.tool(
                "inro.emme.transit_assignment.capacitated_transit_assignment")
            #  assign all 3 classes of demand at the same time
            specs = []
            names = []
            #demand_matrix_template = "mfTRN_{amode}_{set}_{period}"
            for amode, parameters in skim_parameters.items():
                for uc in self.user_classes:
                    spec = _copy(base_spec)
                    spec["modes"] = parameters["modes"]
                    # name = "%s_%s%s" % (period, a_name, amode)
                    # demand_matrix = demand_matrix_template.format(
                    #     access_mode=a_name, set=_set_dict[amode], period=period)

                    #self.calc_network_attribute(uc)
                    #parameters by period and user class
                    self.define_aux_perception(amode)
                    params = self.get_perception_parameters(period, "uc%s" % (uc), amode)
                    cost_percep = 1 / params['vot']                    
                    #skim_params["journey_levels"] = self.all_modes_journey_levels(params, "uc%s" % (uc))

                    name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[int(uc)], self.periodLabel[int(period)])
                    #demand_matrix = demand_matrix_template.format(
                    #    set=_set_dict[amode], period=period)

                    #if emmebank.matrix(name).get_numpy_data(scenario.id).sum() == 0:
                    #    continue   # don't include if no demand
                    spec["demand"] = 'mf%s' % (name)
                    spec["journey_levels"] = self.all_modes_journey_levels(params, "uc%s" % (uc))
                    spec["waiting_time"]["effective_headways"] = params["eff_headway"]
                    spec["waiting_time"]["perception_factor"] = params["init_wait"]
                    spec["in_vehicle_time"]["perception_factor"] = "@ivtf"
                    spec["in_vehicle_cost"]["perception_factor"] = float(cost_percep)
                    spec["boarding_cost"]["on_lines"]["perception_factor"] = float(cost_percep)                                     
                    specs.append(spec)
                    names.append(name)
            func = {
                "segment": {
                    "type": "CUSTOM",
                    "python_function": _segment_cost_function.format(_hours_in_period[self.periodLabel[int(period)]], _vcr_adjustment_factor[self.periodLabel[int(period)]]),
                    "congestion_attribute": "us3",
                    "orig_func": False
                },
                "headway": {
                    "type": "CUSTOM",
                    "python_function": _headway_cost_function.format(mode_attr, _vcr_adjustment_factor[self.periodLabel[int(period)]]),
                },
                "assignment_period": _hours_in_period[self.periodLabel[int(period)]]
            }
            stop = {
                "max_iterations": 3,  # changed from 10 for testing
                "relative_difference": 0.01,
                "percent_segments_over_capacity": 0.01
            }
            assign_transit(specs, congestion_function=func, stopping_criteria=stop, class_names=names, scenario=scenario,
                        log_worksheets=False)
        else:
            assign_transit = modeller.tool(
                "inro.emme.transit_assignment.extended_transit_assignment")
            add_volumes = False
            for amode, parameters in skim_parameters.items():
                for uc in self.user_classes:
                    spec = _copy(base_spec)
                    self.define_aux_perception(amode)
                    params = self.get_perception_parameters(period, "uc%s" % (uc), amode)
                    cost_percep = 1 / params['vot']
                    name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[int(uc)], self.periodLabel[int(period)])
                    spec["modes"] = parameters["modes"]
                    #spec["demand"] = 'ms1' # zero demand matrix
                    spec["demand"] = 'mf%s' % (name)
                    spec["journey_levels"] = self.all_modes_journey_levels(params, "uc%s" % (uc))
                    spec["waiting_time"]["effective_headways"] = params["eff_headway"]
                    spec["waiting_time"]["perception_factor"] = params["init_wait"]
                    spec["in_vehicle_time"]["perception_factor"] = "@ivtf"
                    spec["in_vehicle_cost"]["perception_factor"] = float(cost_percep)
                    spec["boarding_cost"]["on_lines"]["perception_factor"] = float(cost_percep)
                    #print("Running transit assignment for %s"%name)
                    #print(spec)
                    assign_transit(spec, class_name=name, add_volumes=add_volumes, scenario=scenario)
                    add_volumes = True

    #@_m.logbook_trace("Extract skims", save_arguments=True)
    def run_skims(self, period, amode, uc, max_fare, skims_only, matrix_summary, num_processors, network, use_ccr):
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
        class_name = "TRN_%s_%s_%s"%(amode, self.user_class_labels[uc], self.periodLabel[int(period)])
        skim_name = "%s_%s__%s" % (amode, self.user_class_labels[uc], self.periodLabel[int(period)])
        print('skimming %s'% class_name)
        with _m.logbook_trace("First and total wait time, number of boardings, fares, total walk time"):
            # First and total wait time, number of boardings, fares, total walk time, in-vehicle time
            spec = {
                "type": "EXTENDED_TRANSIT_MATRIX_RESULTS",
                "actual_first_waiting_times": 'mf"FIRSTWAIT_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "actual_total_waiting_times": 'mf"TOTALWAIT_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "total_impedance": 'mf"GENCOST_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "by_mode_subset": {
                    "modes": [mode.id for mode in network.modes() if mode.type == "TRANSIT" or mode.type == "AUX_TRANSIT"],
                    #"modes": ["u", "x", "m", "c", "b", "r", "t", "d", "B", "P", "L", "C", "M", "E", "Q"],
                    "avg_boardings": 'mf"XFERS_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                    #"actual_in_vehicle_times": 'mf"TOTALIVTT_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                    "actual_in_vehicle_costs": 'mf"TEMP_IN_VEHICLE_COST"',
                    "actual_total_boarding_costs": 'mf"FARE_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                    "perceived_total_boarding_costs": 'mf"TEMP_PERCEIVED_FARE"',
                    "actual_aux_transit_times": 'mf"TOTALAUX_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                },
            }
            matrix_results(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
        with _m.logbook_trace("Distance and in-vehicle time by mode"):
            mode_combinations = [
                    ("CTABUSLOC", ["B"],        ["IVTT", "DIST"]),
                    ("PACEBUSLOC", ["P", "L"],  ["IVTT", "DIST"]),
                    ("BUSEXP", ["E", "Q"],      ["IVTT", "DIST"]),
                    ("CTARAIL", ["C"],          ["IVTT", "DIST"]),
                    ("METRARAIL", ["M"],        ["IVTT", "DIST"]),
                ]
            total_ivtt_expr = []

            if use_ccr:
                network = scenario.get_partial_network(["TRANSIT_LINE", "TRANSIT_SEGMENT"], include_attributes=True)
                scenario.create_extra_attribute("TRANSIT_SEGMENT", "@mode_timtr")
                try:
                    for mode_name, modes, skim_types in mode_combinations:
                        network.create_attribute("TRANSIT_SEGMENT", "@mode_timtr")
                        for line in network.transit_lines():
                            if line.mode.id in modes:
                                for segment in line.segments():
                                    # segment["@mode_timtr"] = segment['transit_time']
                                    segment["@mode_timtr"] = segment['transit_time'] - segment['@ccost']
                        mode_timtr = network.get_attribute_values("TRANSIT_SEGMENT", ["@mode_timtr"])
                        #print(mode_timtr[0])
                        network.delete_attribute("TRANSIT_SEGMENT", "@mode_timtr")
                        scenario.set_attribute_values("TRANSIT_SEGMENT", ["@mode_timtr"], mode_timtr)
                        #if "IVTT" in skim_types:
                        ivtt = 'mf"%sIVTT_%s"' % (mode_name, skim_name)
                        total_ivtt_expr.append(ivtt)
                        spec = self.get_strat_spec({"in_vehicle": "@mode_timtr"}, ivtt)
                        #print(spec)
                        strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
                finally:
                    scenario.delete_extra_attribute("@mode_timtr")
            else:
                for mode_name, modes, skim_types in mode_combinations:
                    #dist = 'mf"%sDIST_%s_%s__%s"' % (mode_name, amode, self.user_class_labels[uc], self.periodLabel[int(period)]) if "DIST" in skim_types else None
                    ivtt = 'mf"%sIVTT_%s"' % (mode_name, skim_name) if "IVTT" in skim_types else None
                    total_ivtt_expr.append(ivtt)
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
                    #    "result": 'mf"TOTTRNDIST_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                    #    "expression": ('mf"CTABUSLOCDIST_{amode}_{uc}__{period}" + mf"PACEBUSLOCDIST_{amode}_{uc}__{period}" + mf"BUSEXPDIST_{amode}_{uc}__{period}" + mf"CTARAILDIST_{amode}_{uc}__{period}"'
                    #                ' + mf"METRARAILDIST_{amode}_{uc}__{period}"').format(amode=amode, uc=self.user_class_labels[uc], period=self.periodLabel[int(period)]),
                    #}
                    #matrix_calc(spec, scenario=scenario, num_processors=num_processors)
        self.mask_highvalues_all(amode, self.user_class_labels[uc], self.periodLabel[int(period)], scenario, num_processors)
        with _m.logbook_trace("Total in-vehicle time"):
            spec = {   # sum total ivtt across all modes
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"TOTALIVTT_%s"' % skim_name,
                "expression": "+".join(total_ivtt_expr),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

        # convert number of boardings to number of transfers
        # and subtract transfers to the same line at layover points
        with _m.logbook_trace("Number of transfers and total fare"):
            #spec = {
            #    "trip_components": {"boarding": "@layover_board"},
            #    "sub_path_combination_operator": "+",
            #    "sub_strategy_combination_operator": "average",
            #    "selected_demand_and_transit_volumes": {
            #        "sub_strategies_to_retain": "ALL",
            #        "selection_threshold": {"lower": -999999, "upper": 999999}
            #    },
            #    "results": {
            #        "strategy_values": 'TEMP_LAYOVER_BOARD',
            #    },
            #    "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
            #}
            #strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)            
            #self.mask_highvalues("TEMP_LAYOVER_BOARD", amode, self.user_class_labels[uc], self.periodLabel[int(period)], scenario, num_processors)
            self.mask_highvalues("TEMP_IN_VEHICLE_COST", amode, self.user_class_labels[uc], self.periodLabel[int(period)], scenario, num_processors)
            self.mask_highvalues("FARE", amode, self.user_class_labels[uc], self.periodLabel[int(period)], scenario, num_processors)
            #export_matrix_data = _m.Modeller().tool("inro.emme.data.matrix.export_matrix_to_csv")
            #export_matrix_data(matrices=["mfTEMP_LAYOVER_BOARD", "mfTEMP_IN_VEHICLE_COST", 'mf"FARE_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)])])
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"XFERS_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                        "interval_min": 0, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"XFERS_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "expression": '(XFERS_%s_%s__%s - 1).max.0' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
            }
            matrix_calc(spec, scenario=scenario, num_processors=num_processors)

            # sum in-vehicle cost and boarding cost to get the fare paid
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"FARE_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "expression": '(FARE_%s_%s__%s + TEMP_IN_VEHICLE_COST).min.%s' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)], max_fare),
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
                    "aggregated_path_values": 'mf"ACC_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
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
                    "aggregated_path_values": 'mf"EGR_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)])
                },
                "type": "EXTENDED_TRANSIT_PATH_ANALYSIS"
            }
            path_analysis(path_spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

            spec_list = [
            {    # walk access time - convert to time
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"ACC_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "expression": '60.0 * ACC_%s_%s__%s / %s' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)], self.acc_spd_fac[amode]),
            },
            {    # walk egress time - convert to time
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"EGR_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "expression": '60.0 * EGR_%s_%s__%s / %s' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)], self.egr_spd_fac[amode]),
            },
            {   # transfer walk time = total - access - egress
                "type": "MATRIX_CALCULATION",
                "constraint": None,
                "result": 'mf"XFERWALK_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "expression": '(TOTALAUX_{amode}_{uc}__{period} - ACC_{amode}_{uc}__{period}'
                            ' - EGR_{amode}_{uc}__{period}).max.0'.format(amode=amode, uc=self.user_class_labels[uc], 
                                                                                    period=self.periodLabel[int(period)]),
            }]
            matrix_calc(spec_list, scenario=scenario, num_processors=num_processors)

        # transfer wait time
        with _m.logbook_trace("Wait time - xfer"):
            spec = {
                "type": "MATRIX_CALCULATION",
                "constraint":{
                    "by_value": {
                        "od_values": 'mf"TOTALWAIT_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                        "interval_min": 0, "interval_max": 9999999,
                        "condition": "INCLUDE"},
                },
                "result": 'mf"XFERWAIT_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                "expression": '(TOTALWAIT_{amode}_{uc}__{period} - FIRSTWAIT_{amode}_{uc}__{period}).max.0'.format(amode=amode, 
                                                                                                                uc=self.user_class_labels[uc], 
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
                        "strategy_values": 'mf"DWELLTIME_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
                    },
                    "type": "EXTENDED_TRANSIT_STRATEGY_ANALYSIS"
                }
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
        params = self.get_perception_parameters(period, "uc%s" % str(uc), amode)
        expr_params = _copy(params)
        expr_params["xfers"] = 15.0
        expr_params["amode"] = amode
        expr_params["period"] = self.periodLabel[int(period)]
        expr_params["uc"] = self.user_class_labels[uc]
        cost_percep = 1 / params['vot']        
        expr_params["fare_percep"] = float(cost_percep)
        spec = {
            "type": "MATRIX_CALCULATION",
            "constraint": None,
            "result": 'mf"GENCOST_%s_%s__%s"' % (amode, self.user_class_labels[uc], self.periodLabel[int(period)]),
            "expression": ("{xfer_wait} * TOTALWAIT_{amode}_{uc}__{period} "
                        "- ({xfer_wait} - {init_wait}) * FIRSTWAIT_{amode}_{uc}__{period} "
                        "+ 1.0 * TOTALIVTT_{amode}_{uc}__{period}"
                        "+ {fare_percep} * FARE_{amode}_{uc}__{period}"
                        "+ {xfers} *(XFERS_{amode}_{uc}__{period}.max.0) "
                        "+ {access} * ACC_{amode}_{uc}__{period} "
                        "+ {egress} * EGR_{amode}_{uc}__{period} "
                        "+ {xfer_walk} * XFERWALK_{amode}_{uc}__{period}").format(**expr_params)
        }
        matrix_calc(spec, scenario=scenario, num_processors=num_processors)
        
        if use_ccr:
            with _m.logbook_trace("Calculate CCR skims"):
                network = scenario.get_partial_network(["TRANSIT_LINE", "TRANSIT_SEGMENT"], include_attributes=True)
                attr_map = {
                    "TRANSIT_SEGMENT": ["@phdwy", "transit_volume", "transit_boardings", "@capacity_penalty",
                                        "@tot_capacity", "@seated_capacity", "@tot_vcr", "@seated_vcr", #"@seg_rel", "@eawt", 
                                        "@base_timtr", "@ccost_skim"],
                    "TRANSIT_VEHICLE": ["seated_capacity", "total_capacity"],
                    "TRANSIT_LINE": ["headway"],
                    "LINK": ["data3"],
                }
                mode_name = '.mode.id'
                for domain, attrs in attr_map.items():
                    values = scenario.get_attribute_values(domain, attrs)
                    network.set_attribute_values(domain, attrs, values)

                enclosing_scope = {"network": network, "scenario": scenario}
                # code = compile(_segment_cost_function, "segment_cost_function", "exec")
                # exec(code, enclosing_scope)
                code = compile(_headway_cost_function.format(mode_name, _vcr_adjustment_factor[self.periodLabel[int(period)]]), "headway_cost_function", "exec")
                exec(code, enclosing_scope)
                #calc_eawt = enclosing_scope["calc_eawt"]
                #hdwy_fraction = 0.5 # fixed in assignment spec
                '''
                netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")
                hdw_frac_1 = {
                    "result": "@hfrac_ccr",
                    "expression": "0.5",
                    "selections": {
                        "transit_line": "@phdwy=0,10"
                    },
                    "type": "NETWORK_CALCULATION"
                }
                hdw_frac_2 = {
                    "result": "@hfrac_ccr",
                    "expression": "((@phdwy-10)*.4+5)/@phdwy",
                    "selections": {
                        "transit_line": "@phdwy=10,20"
                    },
                    "type": "NETWORK_CALCULATION"
                }
                hdw_frac_3 = {
                    "result": "@hfrac_ccr",
                    "expression": "((@phdwy-20)*.35+9)/@phdwy",
                    "selections": {
                        "transit_line": "@phdwy=20,30"
                    },
                    "type": "NETWORK_CALCULATION"
                }
                hdw_frac_4 = {
                    "result": "@hfrac_ccr",
                    "expression": "((@phdwy-30)*2.5/30+12.5)/@phdwy",
                    "selections": {
                        "transit_line": "@phdwy=30,720"
                    },
                    "type": "NETWORK_CALCULATION"
                }
                netcalc([hdw_frac_1, hdw_frac_2, hdw_frac_3, hdw_frac_4], scenario=scenario)
                '''
                for segment in network.transit_segments():
                    line = segment.line
                    headway = line.headway
                    veh_cap = line.vehicle.total_capacity
                    seated_veh_cap = line.vehicle.seated_capacity
                    # capacity = 60.0 * veh_cap / line.headway
                    capacity = 60.0 * _hours_in_period[self.periodLabel[int(period)]] * veh_cap / line.headway
                    seated_capacity = 60.0 * _hours_in_period[self.periodLabel[int(period)]] * seated_veh_cap / line.headway
                    transit_volume = segment.transit_volume
                    vcr = transit_volume / capacity * _vcr_adjustment_factor[self.periodLabel[int(period)]]
                    seated_vcr = transit_volume / seated_capacity * _vcr_adjustment_factor[self.periodLabel[int(period)]]
                    segment["@tot_capacity"] = capacity
                    segment["@seated_capacity"] = seated_capacity
                    segment["@tot_vcr"] = vcr
                    segment["@seated_vcr"] = seated_vcr
                    segment["@hfrac_ccr"] = 0.5
                    #if segment["@phdwy"] <= 10:
                    #    segment["@hfrac_ccr"] = 0.5
                    #elif segment["@phdwy"] <= 20:
                    #    segment["@hfrac_ccr"] = ((segment["@phdwy"]-10)*.4+5)/segment["@phdwy"]
                    #elif segment["@phdwy"] <= 30:
                    #    segment["@hfrac_ccr"] = ((segment["@phdwy"]-20)*.35+9)/segment["@phdwy"]                        
                    #else:
                    #    segment["@hfrac_ccr"] = ((segment["@phdwy"]-30)*2.5/30+12.5)/segment["@phdwy"] 
                    # link reliability is calculated as link_rel_factor * base (non-crowded) transit time
                    #if segment.link is None:
                    #    # sometimes segment.link returns None. Is this due to hidden segments??
                    #    segment['@seg_rel'] = 0
                    #else:
                    #    segment["@seg_rel"] = segment.link.data3 * segment['@base_timtr']
                    #segment["@eawt"] = calc_eawt(segment, vcr, headway)
                    segment["@capacity_penalty"] = max(segment["@phdwy"] * segment["@hfrac_ccr"] - headway * line["@hfrac"], 0) # - segment["@eawt"]
                    segment['@ccost_skim'] = segment['@ccost']

                additional_attribs = ["@capacity_penalty", "@tot_vcr", "@seated_vcr", "@tot_capacity", "@seated_capacity", "@ccost_skim"] # "@eawt", "@seg_rel"
                values = network.get_attribute_values('TRANSIT_SEGMENT', additional_attribs)
                scenario.set_attribute_values('TRANSIT_SEGMENT', additional_attribs, values)

                # # Link unreliability
                #spec = self.get_strat_spec({"in_vehicle": "@seg_rel"}, "LINKREL_%s" % skim_name)
                #strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

                # Crowding penalty
                # for some unknown reason, just using ccost here will produce an all 0 matrix...
                # spec = self.get_strat_spec({"in_vehicle": "@ccost"}, "CROWD_%s" % skim_name)
                # hack to get around this was to create a new variable that is a copy of ccost and use it to skim
                spec = self.get_strat_spec({"in_vehicle": "@ccost_skim"}, "CROWD_%s" % skim_name)
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

                # skim node reliability, Extra added wait time (EAWT)
                #spec = self.get_strat_spec({"boarding": "@eawt"}, "EAWT_%s" % skim_name)
                #strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)

                # skim capacity penalty
                spec = self.get_strat_spec({"boarding": "@capacity_penalty"}, "CAPPEN_%s" % skim_name)
                strategy_analysis(spec, class_name=class_name, scenario=scenario, num_processors=num_processors)
        else:
            compute_matrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")
            # set crowd and cappen to 0 if no ccr
            reset_crowd={
                "expression": "0",
                "result": "mfCROWD_%s" % skim_name,
                "type": "MATRIX_CALCULATION"
            }
            report = compute_matrix(reset_crowd)
            reset_cappen={
                "expression": "0",
                "result": "mfCROWD_%s" % skim_name,
                "type": "MATRIX_CALCULATION"
            }
            report = compute_matrix(reset_cappen)               
        self.mask_highvalues_all(amode, self.user_class_labels[uc], self.periodLabel[int(period)], scenario, num_processors)

        if matrix_summary:
            compute_matrix = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")
            # export max, average, sum for each transit skim matrix
            data = []
            for name in self.skim_matrices:
                skim_name = "%s_%s_%s__%s" % (name, amode, self.user_class_labels[uc], self.periodLabel[int(period)])
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
            filename = "%s\\trn_skim_list_%s.csv"%(EMME_OUTPUT, datetime.date.today())
            df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')
            # export number of OD pairs with non-zero in-vehicle time by transit mode
            data = [self.periodLabel[int(period)], amode, self.user_class_labels[uc]]
            spec = "_%s_%s__%s" % (amode, self.user_class_labels[uc], self.periodLabel[int(period)])
            modes = ["bus", "CTA rail", "Metra rail", "bus and CTA rail", "bus and Metra rail", "CTA rail and Metra rail", "bus, CTA rail and Metra rail"]
            expressions = ["CTABUSLOCIVTT%s+PACEBUSLOCIVTT%s+BUSEXPIVTT%s"%(spec,spec,spec),
                            "CTARAILIVTT%s"%(spec), 
                            "METRARAILIVTT%s"%(spec), 
                            "(CTABUSLOCIVTT%s+PACEBUSLOCIVTT%s+BUSEXPIVTT%s)*CTARAILIVTT%s"%(spec,spec,spec,spec),
                            "(CTABUSLOCIVTT%s+PACEBUSLOCIVTT%s+BUSEXPIVTT%s)*METRARAILIVTT%s"%(spec,spec,spec,spec),
                            "CTARAILIVTT%s*METRARAILIVTT%s"%(spec,spec),
                            "(CTABUSLOCIVTT%s+PACEBUSLOCIVTT%s+BUSEXPIVTT%s)*CTARAILIVTT%s*METRARAILIVTT%s"%(spec,spec,spec,spec,spec)]
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
            filename = "%s\\transit_mode_OD_summary_%s.csv"%(EMME_OUTPUT, datetime.date.today())
            df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')
            # export total boardings
            data = []
            demand_name = "TRN_%s_%s_%s" % (amode, self.user_class_labels[uc], self.periodLabel[int(period)])
            metra_demand_name = "(mfXFERS_%s_%s__%s+1)*mfTRN_%s_%s_%s" % (amode, self.user_class_labels[uc], self.periodLabel[int(period)], amode, self.user_class_labels[uc], self.periodLabel[int(period)])
            spec_sum={
                "expression": metra_demand_name,
                "result": "msTEMP_SUM",
                "aggregation": {
                    "origins": "+",
                    "destinations": "+"
                },
                "type": "MATRIX_CALCULATION"
            }
            report = compute_matrix(spec_sum)
            data.append([demand_name, report["maximum"], report["maximum_at"]["origin"], report["maximum_at"]["destination"], 
                        report["average"], report["sum"]])
            df = pd.DataFrame(data, columns=['Demand', 'Max', 'Max orig', 'Max dest', 'Avg', 'Sum'])
            filename = "%s\\trn_boardings_%s.csv"%(EMME_OUTPUT, datetime.date.today())
            df.to_csv(filename, mode='a', index=False, header=not os.path.exists(filename), line_terminator='\n')
        return

    def define_aux_perception(self, amode):
        modeller = _m.Modeller()
        scenario = self.scenario
        netcalc = modeller.tool("inro.emme.network_calculation.network_calculator")
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
        netcalc([xfer_walk, acc_egr_walk])       
        if amode in ["PNROUT", "KNROUT"]: # overwrite perception on PNR/KNR access links 
            drive_access_percep = {
                "result": "@aperf" ,
                "expression": self.acc_egr_drive_percep,
                "selections": {
                    "link": "modes=vw",
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc(drive_access_percep)
        elif amode in ["PNRIN", "KNRIN"]: # overwrite perception on PNR/KNR egress links
            drive_egress_percep = {
                "result": "@aperf" ,
                "expression": self.acc_egr_drive_percep,
                "selections": {
                    "link": "modes=yz",
                },
                "type": "NETWORK_CALCULATION"
            }
            netcalc(drive_egress_percep)
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
        with _m.logbook_trace("Set high values to 0"):            
            for name in self.skim_matrices:
                self.mask_highvalues(name, amode, uc, period, scenario, num_processors)

    def mask_highvalues(self, name, amode, uc, period, scenario, num_processors):
        
        matrix_calc = _m.Modeller().tool("inro.emme.matrix_calculation.matrix_calculator")

        if "TEMP" not in name:
            name = "%s_%s_%s__%s" % (name, amode, uc, period)
        # Set high values to 0
        with _m.logbook_trace("Set high values to 0"):           
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
    def export_boardings_by_line(self, desktop, output_transit_boardings_file):
        """
        Writes out csv file containing the transit boardings by line  for use in
        validation summaries
        Parameters:
            - desktop: emme desktop object with an primary scenario that has transit
                boardings assigned
            - output_transit_boardings_file: str of file to write transit boardings to
        """
        project = desktop.project
        table = project.new_network_table("TRANSIT_LINE")
        column = worksheet.Column()

        # Creating total boardings by line table
        column.expression = "line"
        column.name = "line_name"
        table.add_column(0, column)

        column.expression = "description"
        column.name = "description"
        table.add_column(1, column)

        column.expression = "ca_board_t"
        column.name = "total_boardings"
        table.add_column(2, column)

        column.expression = "mode"
        column.name = "mode"
        table.add_column(3, column)

        column.expression = "mdesc"
        column.name = "mode_description"
        table.add_column(4, column)

        table.export(output_transit_boardings_file)
        table.close()


    def export_boardings_by_segment(self, desktop, use_ccr, output_transit_boardings_file):
        """
        Writes out csv file containing the transit boardings by segment for use in
        validation summaries
        Parameters:
            - desktop: emme desktop object with an primary scenario that has transit
                boardings assigned
            - output_transit_boardings_file: str of file to write transit boardings to
        """
        project = desktop.project
        table = project.new_network_table("TRANSIT_SEGMENT")
        column = worksheet.Column()

        col_name_dict = {
            "i": "i_node",
            "j": "j_node",
            "line": "line_name",
            "description": "description",
            "voltr": "volume",
            "length": "length",
            "board": "boardings",
            "alight": "alightings",
            "mode": "mode",
            "mdesc": "mode_description",
            "capt": "total_cap_of_line_per_hr",
            "caps": "seated_cap_of_line_per_hr",
            "xi": "i_node_x",
            "yi": "i_node_y",
            "xj": "j_node_x",
            "yj": "j_node_y",
        }
        if use_ccr: 
            col_name_dict['@tot_capacity']: 'total_capacity'
            col_name_dict['@seated_capacity']: 'seated_capacity'
            col_name_dict['@capacity_penalty']: 'capacity_penalty'
            col_name_dict['@tot_vcr']: 'tot_vcr'
            col_name_dict['@seated_vcr']: 'seated_vcr'
            col_name_dict['@ccost']: 'ccost'
            #col_name_dict['@eawt']: 'eawt'
            #col_name_dict['@seg_rel']: 'seg_rel'
        col_num = 0
        for expression, name in col_name_dict.items():
            column.expression = expression
            column.name = name
            table.add_column(col_num, column)
            col_num += 1

        table.export(output_transit_boardings_file)
        table.close()


    def export_boardings_by_station(self, output_folder, period):
        modeller = _m.Modeller()
        scenario = self.scenario
        sta2sta = modeller.tool(
            "inro.emme.transit_assignment.extended.station_to_station_analysis")
        sta2sta_spec = {
            "type": "EXTENDED_TRANSIT_STATION_TO_STATION_ANALYSIS",
            "transit_line_selections": {
                "first_boarding": "mod=M",
                "last_alighting": "mod=M"
            },
            "analyzed_demand": None,
        }

        # FIXME these expressions need to be more flexible if fares are applied
        operator_dict = {
        # mode: network_selection
            'METRARail': "mode=M",
            'CTARail': "mode=C"
        }

        with _m.logbook_trace("Writing station-to-station summaries for period %s" % period):
            for amode in ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]:
                for uc in self.user_classes:
                    for op, cut in operator_dict.items():
                        class_name = "TRN_%s_%s_%s" % (amode, self.user_class_labels[int(uc)], period)
                        demand_matrix = "mf%s" % (class_name)
                        output_file_name = "%s_station_to_station_%s_%s.txt" % (op, amode, period)
                        #print(class_name, demand_matrix, output_file_name)

                        sta2sta_spec['transit_line_selections']['first_boarding'] = cut
                        sta2sta_spec['transit_line_selections']['last_alighting'] = cut
                        sta2sta_spec['analyzed_demand'] = demand_matrix

                        output_path = os.path.join(output_folder, output_file_name)
                        sta2sta(specification=sta2sta_spec,
                                output_file=output_path,
                                append_to_output_file=False,
                                class_name=class_name)


    def output_transit_boardings(self, desktop, use_ccr, output_location, period):
        desktop.data_explorer().replace_primary_scenario(self.scenario)
        output_transit_boardings_file = os.path.join(output_location, "boardings_by_line_{}.csv".format(period))
        self.export_boardings_by_line(desktop, output_transit_boardings_file)

        output_transit_segments_file = os.path.join(output_location, "boardings_by_segment_{}.csv".format(period))
        self.export_boardings_by_segment(desktop, use_ccr, output_transit_segments_file)

        output_station_to_station_folder = os.path.join(output_location)
        self.export_boardings_by_station(output_station_to_station_folder, period)            
