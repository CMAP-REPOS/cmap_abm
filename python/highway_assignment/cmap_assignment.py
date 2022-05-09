# Copied from SANDAG and modified 4/15/21 Andrew Rohne/RSG

TOOLBOX_ORDER = 2

import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import numpy
import array
import os
import json as _json
import general as gen_utils

class TrafficAssignment(_m.Tool()):
    __MODELLER_NAMESPACE__ = "cmap"
    period = _m.Attribute(unicode)
    msa_iteration = _m.Attribute(int)
    relative_gap = _m.Attribute(float)
    max_iterations = _m.Attribute(int)
    num_processors = _m.Attribute(str)
    select_link = _m.Attribute(unicode)
    raise_zero_dist = _m.Attribute(bool)
    stochastic = _m.Attribute(bool)
    input_directory = _m.Attribute(str)
    tool_run_msg = ""
    timeFactors = {}
    
    def __init__(self):
        self.msa_iteration = 1
        self.relative_gap = 0.0005
        self.max_iterations = 100
        self.num_processors = "MAX-1"
        self.raise_zero_dist = True
        self.select_link = '[]'
        self.stochastic = False
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["period", "msa_iteration", "relative_gap", "max_iterations",
                           "num_processors", "select_link", "raise_zero_dist", "stochastic", "input_directory"]
        version = os.environ.get("EMMEPATH", "")
        self._version = version[-5:] if version else ""
        self._skim_classes_separately = True  # Used for debugging only
        self._stats = {}
        
    
    def __call__(self, period, msa_iteration, relative_gap, max_iterations, num_processors, scenario, select_link=[], raise_zero_dist=True, stochastic=False, input_directory=None):        
        self._skim_classes_separately = False
        select_link = _json.loads(select_link) if isinstance(select_link, basestring) else select_link
        attrs = {
            "period": period,
            "msa_iteration": msa_iteration,
            "relative_gap": relative_gap,
            "max_iterations": max_iterations,
            "num_processors": num_processors,
            "scenario": scenario, 
            "select_link": _json.dumps(select_link),
            "raise_zero_dist": raise_zero_dist,
            "stochastic": stochastic,
            "input_directory": input_directory,
            "self": str(self)
        }
        
        self._stats = {}
        with _m.logbook_trace("Traffic assignment for period %s" % period, attributes=attrs):
            classes = [
                {   # 0
                    "name": 'SOV_NT_L', "mode": 'S', "PCE": 1, "VOT": 36.4, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 1
                    "name": 'SOV_TR_L', "mode": 'S', "PCE": 1, "VOT": 36.4, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 2
                    "name": 'HOV2_L', "mode": 'H', "PCE": 1, "VOT": 61.46, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV2", "TOLLDIST", "HOVDIST"]
                },
                {   # 3
                    "name": 'HOV3_L', "mode": 'H', "PCE": 1, "VOT": 91.17, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV3", "TOLLDIST", "HOVDIST"]
                },
                {   # 4
                    "name": 'SOV_NT_M', "mode": 'S', "PCE": 1, "VOT": 152.4, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 5
                    "name": 'SOV_TR_M', "mode": 'S', "PCE": 1, "VOT": 152.4, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 6
                    "name": 'HOV2_M', "mode": 'H', "PCE": 1, "VOT": 252.3, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV2", "TOLLDIST", "HOVDIST"]
                },
                {   # 7
                    "name": 'HOV3_M', "mode": 'H', "PCE": 1, "VOT": 380.5, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV3", "TOLLDIST", "HOVDIST"]
                },
                {   # 8
                    "name": 'SOV_NT_H', "mode": 'S', "PCE": 1, "VOT": 268.4, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 9
                    "name": 'SOV_TR_H', "mode": 'S', "PCE": 1, "VOT": 268.4, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 10
                    "name": 'HOV2_H', "mode": 'H', "PCE": 1, "VOT": 443.17, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV2", "TOLLDIST", "HOVDIST"]
                },
                {   # 11
                    "name": 'HOV3_H', "mode": 'H', "PCE": 1, "VOT": 669.29, "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV3", "TOLLDIST", "HOVDIST"]
                },
                {   # 12
                    "name": 'TRK_B', "mode": 'b', "PCE": 1.0, "VOT": 60., "toll": "@toll",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_L", "TOLLDIST"]
                },
                {   # 13
                    "name": 'TRK_L', "mode": 'l', "PCE": 1.3, "VOT": 60., "toll": "@toll2",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_L", "TOLLDIST"]
                },
                {   # 14
                    "name": 'TRK_M', "mode": 'm', "PCE": 1.5, "VOT": 60., "toll": "@toll3",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_M", "TOLLDIST"]
                },
                {   # 15
                    "name": 'TRK_H', "mode": 'h', "PCE": 2.5, "VOT": 100., "toll": "@toll4",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_H", "TOLLDIST"]
                }
                ]

            if 1 < msa_iteration < 4:
                # Link and turn flows
                link_attrs = ["auto_volume"]
                turn_attrs = ["auto_volume"]
                for traffic_class in classes:
                    link_attrs.append("@%s" % (traffic_class["name"].lower()))
                    turn_attrs.append("@p%s" % (traffic_class["name"].lower()))
                msa_link_flows = scenario.get_attribute_values("LINK", link_attrs)[1:]
                msa_turn_flows = scenario.get_attribute_values("TURN", turn_attrs)[1:]

            self.run_assignment(period, relative_gap, max_iterations, num_processors, scenario, classes, select_link)

            if 1 < msa_iteration < 4:
                link_flows = scenario.get_attribute_values("LINK", link_attrs)
                values = [link_flows.pop(0)]
                for msa_array, flow_array in zip(msa_link_flows, link_flows):
                    msa_vals = numpy.frombuffer(msa_array, dtype='float32')
                    flow_vals = numpy.frombuffer(flow_array, dtype='float32')
                    result = msa_vals + (1.0 / msa_iteration) * (flow_vals - msa_vals)
                    result_array = array.array('f')
                    result_array.fromstring(result.tostring())
                    values.append(result_array)
                scenario.set_attribute_values("LINK", link_attrs, values)

                turn_flows = scenario.get_attribute_values("TURN", turn_attrs)
                values = [turn_flows.pop(0)]
                for msa_array, flow_array in zip(msa_turn_flows, turn_flows):
                    msa_vals = numpy.frombuffer(msa_array, dtype='float32')
                    flow_vals = numpy.frombuffer(flow_array, dtype='float32')
                    result = msa_vals + (1.0 / msa_iteration) * (flow_vals - msa_vals)
                    result_array = array.array('f')
                    result_array.fromstring(result.tostring())
                    values.append(result_array)
                scenario.set_attribute_values("TURN", turn_attrs, values)
            self.calc_network_results(period, num_processors, scenario)

            if msa_iteration < 5: #FIXME: Changed from 4 to 5... why is a 4 here?
                self.prepOutputTables(period, scenario, classes)
                self.run_skims(period, num_processors, scenario, classes)
                #self.report(period, scenario, classes) #FIXME
                # Check that the distance matrix is valid (no disconnected zones)
                if raise_zero_dist:
                    name = "SOV_TR_H_DIST__%s"%period
                    dist_stats = self._stats[name]
                    if dist_stats[1] == 0:
                        zones = scenario.zone_numbers
                        matrix = scenario.emmebank.matrix(name)
                        data = matrix.get_numpy_data(scenario)
                        row, col = numpy.unravel_index(data.argmin(), data.shape)
                        row, col = zones[row], zones[col]
                        raise Exception("Disconnected zone error: 0 value found in matrix %s from zone %s to %s" % (name, row, col))
                        
    
    def run_assignment(self, period, relative_gap, max_iterations, num_processors, scenario, classes, select_link):
        """
        Assignment Model
        ----------
        period : string
            input variable description
        relative_gap: float
            Relative gap used to determine model convergence
        max_iterations: integer
            Maximum number of iterations used to determine when to stop the model
        num_processors: integer
            Number of processors to use in assignment
        scenario: Scenario
            The scenario to work on
        classes: array
            Array of vehicle class objects
        
        Returns
        -------
        Nothing
        """
        emmebank = scenario.emmebank

        modeller = _m.Modeller()
        set_extra_function_para = modeller.tool("inro.emme.traffic_assignment.set_extra_function_parameters")
        create_attribute = modeller.tool("inro.emme.data.extra_attribute.create_extra_attribute")
        
        traffic_assign = modeller.tool("inro.emme.traffic_assignment.sola_traffic_assignment")
        net_calc = gen_utils.NetworkCalculator(scenario) 
        net_calc("ul1", "@ftime", {"link": "modes=A"})
        set_extra_function_para(el2 = "@busveq")
        p = period.lower()
        assign_spec = self.base_assignment_spec(
            relative_gap, max_iterations, num_processors)
        with _m.logbook_trace("Prepare traffic data for period %s" % period):
            with _m.logbook_trace("Per-class flow attributes"):
                classIndex = 0
                for traffic_class in classes:
                    demand = "mf%s_%s"%(traffic_class["name"], period) 
                    classIndex += 1
                    
                    att_name = "@c_%s"%traffic_class["name"].lower()
                    att_des = "Cost for %s"%traffic_class["name"]
                    cost = create_attribute("LINK", att_name, att_des, 0.0, overwrite = True, scenario = scenario)
                    net_calc(att_name, "999.min.@auto_time + 60.0 / %s * %s" %(traffic_class["VOT"], traffic_class["toll"]), "modes = %s"%traffic_class["mode"])
                    link_cost = att_name
                    
                    att_name = "@%s" % (traffic_class["name"].lower())
                    att_des = "%s %s link volume" % (period, traffic_class["name"])
                    link_flow = create_attribute("LINK", att_name, att_des, 0, overwrite=True, scenario=scenario)
                    att_name = "@p%s" % (traffic_class["name"].lower())
                    att_des = "%s %s turn volume" % (period, traffic_class["name"])
                    turn_flow = create_attribute("TURN", att_name, att_des, 0, overwrite=True, scenario=scenario)

                    class_spec = {
                        "mode": traffic_class["mode"],
                        "demand": demand,
                        "generalized_cost": {
                            "link_costs": link_cost, "perception_factor": 1.0
                        },
                        "results": {
                            "link_volumes": link_flow.id, 
                            "turn_volumes": turn_flow.id,
                            "od_travel_times": None
                        }
                    }
                    assign_spec["classes"].append(class_spec)
            if select_link:
                for class_spec in assign_spec["classes"]:
                    class_spec["path_analyses"] = []
                for sub_spec in select_link:
                    expr = sub_spec["expression"]
                    suffix = sub_spec["suffix"]
                    threshold = sub_spec["threshold"]
                    if not expr and not suffix:
                        continue
                    with _m.logbook_trace("Prepare for select link analysis '%s' - %s" % (expr, suffix)):
                        slink = create_attribute("LINK", "@slink_%s" % suffix, "selected link for %s" % suffix, 0,
                                                 overwrite=True, scenario=scenario)
                        net_calc(slink.id, "1", expr)
                        with _m.logbook_trace("Initialize result matrices and extra attributes"):
                            for traffic_class, class_spec in zip(classes, assign_spec["classes"]):
                                att_name = "@sl_%s_%s" % (traffic_class["name"].lower(), suffix)
                                att_des = "%s %s '%s' sel link flow"% (period, traffic_class["name"], suffix)
                                link_flow = create_attribute("LINK", att_name, att_des, 0, overwrite=True, scenario=scenario)
                                att_name = "@psl_%s_%s" % (traffic_class["name"].lower(), suffix)
                                att_des = "%s %s '%s' sel turn flow" % (period, traffic_class["name"], suffix)
                                turn_flow = create_attribute("TURN", att_name, att_des, 0, overwrite=True, scenario=scenario)

                                name = "SELDEM_%s_%s__%s" % (traffic_class["name"], suffix, period)
                                desc = "Selected demand for %s %s %s" % (traffic_class["name"], suffix, period)
                                seldem = dem_utils.create_full_matrix(name, desc, scenario=scenario)

                                # add select link analysis
                                class_spec["path_analyses"].append({
                                    "link_component": slink.id,
                                    "turn_component": None,
                                    "operator": "+",
                                    "selection_threshold": { "lower": threshold, "upper": 999999},
                                    "path_to_od_composition": {
                                        "considered_paths": "SELECTED",
                                        "multiply_path_proportions_by": {"analyzed_demand": True, "path_value": False}
                                    },
                                    "analyzed_demand": None,
                                    "results": {
                                        "selected_link_volumes": link_flow.id,
                                        "selected_turn_volumes": turn_flow.id,
                                        "od_values": seldem.named_id
                                    }
                                })
        # Run assignment
        out = traffic_assign(assign_spec, scenario, chart_log_interval=1)
        print "Assigned %s trips"%out['initialization']['all_classes']['demand']['total']
        print "Assignment complete, ended due to %s after %s iterations and at a RG of %s" % (out['stopping_criterion'], len(out['iterations']), out['iterations'][len(out['iterations'])-1]['gaps']['relative'])
        return
        
    def run_skims(self, period, num_processors, scenario, classes):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        modeller = _m.Modeller()
        traffic_assign = modeller.tool(
            "inro.emme.traffic_assignment.sola_traffic_assignment")
        emmebank = scenario.emmebank
        p = period.lower()
        analysis_link = {
            "TIME":           "@auto_time",
            "FTIME":          "@ftime",
            "DIST":           "length",
            "HOVDIST":        "@hovdist",
            "TOLLCOST.SOV":   "@toll",
            "TOLLCOST.HOV2":  "@toll",
            "TOLLCOST.HOV3":  "@toll",
            "TOLLCOST.TRK_L": "@toll2",
            "TOLLCOST.TRK_M": "@toll3",
            "TOLLCOST.TRK_H": "@toll4",
            "TOLLDIST":       "@tolldist",
            "TOLLDIST.HOV2":  "@tolldist",
            "TOLLDIST.HOV3":  "@tolldist",
            "REL":            "@reliability_sq"
        }
        analysis_turn = {"TIME": "@auto_time_turn"}
        with self.setup_skims(period, scenario):
            skim_spec = self.base_assignment_spec(0, 0, num_processors)
            for traffic_class in classes:
                if not traffic_class["skims"]:
                    continue
                class_analysis = []
                if "GENCOST" in traffic_class["skims"]:
                    od_travel_times = 'mf"%s_%s__%s"' % (traffic_class["name"], "GENCOST", period)
                    traffic_class["skims"].remove("GENCOST")
                else:
                    od_travel_times = None
                for skim_type in traffic_class["skims"]:
                    skim_name = skim_type.split(".")[0]
                    class_analysis.append({
                        "link_component": analysis_link.get(skim_type),
                        "turn_component": analysis_turn.get(skim_type),
                        "operator": "+",
                        "selection_threshold": {"lower": None, "upper": None},
                        "path_to_od_composition": {
                            "considered_paths": "ALL",
                            "multiply_path_proportions_by":
                                {"analyzed_demand": False, "path_value": True}
                        },
                        "results": {
                            "od_values": '%s_%s__%s'%(traffic_class["name"], skim_name, period),
                            "selected_link_volumes": None,
                            "selected_turn_volumes": None
                        }
                    })
                link_cost = "@c_%s"%traffic_class["name"].lower()
                skim_spec["classes"].append({
                    "mode": traffic_class["mode"],
                    "demand": 'ms"Zero"',  # 0 demand for skim with 0 iteration and fix flow in ul2 in vdf
                    "generalized_cost": {
                        "link_costs": link_cost, "perception_factor": 1.0
                    },
                    "results": {
                        "link_volumes": None, "turn_volumes": None,
                        "od_travel_times": {"shortest_paths": od_travel_times}
                    },
                    "path_analyses": class_analysis,
                })

            # skim assignment
            if self._skim_classes_separately:
                # Debugging check
                skim_classes = skim_spec["classes"][:]
                for kls in skim_classes:
                    skim_spec["classes"] = [kls]
                    traffic_assign(skim_spec, scenario)
            else:
                traffic_assign(skim_spec, scenario)

            # compute diagonal value for TIME and DIST
            with _m.logbook_trace("Compute diagonal values for period %s" % period):
                num_cells = len(scenario.zone_numbers) ** 2
                for traffic_class in classes:
                    class_name = traffic_class["name"]
                    skims = traffic_class["skims"]
                    with _m.logbook_trace("Class %s" % class_name):
                        idx = 1
                        for skim_type in skims:
                            skim_name = skim_type.split(".")[0]
                            name = "%s_%s__%s"%(class_name, skim_name, period)
                            matrix = emmebank.matrix(name.split(".")[0])
                            data = matrix.get_numpy_data(scenario)
                            if skim_name in ["TIME", "DIST", "FTIME"]:
                                numpy.fill_diagonal(data, 999999999.0)
                                data[numpy.diag_indices_from(data)] = 0.5 * numpy.nanmin(data[::,:-17:], 1)
                                internal_data = data[:-17:, :-17:]  # Exclude the last 17 zones, external zones
                                self._stats[name] = (name, internal_data.min(), internal_data.max(), internal_data.mean(), internal_data.sum(), 0)
                            else:
                                self._stats[name] = (name, data.min(), data.max(), data.mean(), data.sum(), 0)
                                numpy.fill_diagonal(data, 0.0)
                            matrix.set_numpy_data(data, scenario)
        return
        
        
    def calc_network_results(self, period, num_processors, scenario):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        modeller = _m.Modeller()
        create_attribute = modeller.tool("inro.emme.data.extra_attribute.create_extra_attribute")
        net_calc = gen_utils.NetworkCalculator(scenario)
        emmebank = scenario.emmebank
        p = period.lower()
        with _m.logbook_trace("Calculation of attributes for skims"):
            link_attributes = [
                ("@hovdist", "distance for HOV"),
                ("@tollcost", "Toll cost for SOV autos"),
                ("@h2tollcost", "Toll cost for hov2"),
                ("@h3tollcost", "Toll cost for hov3"),
                ("@trk_ltollcost", "Toll cost for light trucks"),
                ("@trk_mtollcost", "Toll cost for medium trucks"),
                ("@trk_htollcost", "Toll cost for heavy trucks"),
                ("@mlcost", "Manage lane cost in cents"),
                ("@tolldist", "Toll distance"),
                ("@h2tolldist", "Toll distance for hov2"),
                ("@h3tolldist", "Toll distance for hov3"),
                ("@reliability", "Reliability factor"),
                ("@reliability_sq", "Reliability factor squared"),
                ("@auto_volume", "traffic link volume (volau)"),
                ("@auto_time", "traffic link time (timau)"),
            ]
            for name, description in link_attributes:
                create_attribute("LINK", name, description,
                                 0, overwrite=True, scenario=scenario)
            create_attribute("TURN", "@auto_time_turn", "traffic turn time (ptimau)", 
                             overwrite=True, scenario=scenario)
            net_calc("@tolldist", "length * @tollv") 
            net_calc("@auto_volume", "volau", {"link": "modes=A"})
            net_calc("ul1", "@ftime", {"link": "modes=A"})
            vdfs = [f for f in emmebank.functions() if f.type == "VOLUME_DELAY"]
            exf_pars = emmebank.extra_function_parameters
            
            for function in vdfs:
                expression = function.expression
                for exf_par in ["el1", "el2", "el3"]:
                    expression = expression.replace(exf_par, getattr(exf_pars, exf_par))
                # split function into time component and reliability component
                #time_expr, reliability_expr = expression.split("*(1+@sta_reliability+")
                time_expr = expression
                net_calc("@auto_time", time_expr, {"link": "vdf=%s" % function.id[2:]})
                #net_calc("@reliability", "(@sta_reliability+" + reliability_expr, {"link": "vdf=%s" % function.id[2:]})

            #net_calc("@reliability_sq", "@reliability**2", {"link": "modes=d"})
            net_calc("@auto_time_turn", "ptimau*(ptimau.gt.0)", {"incoming_link": "all", "outgoing_link": "all"})  
                     
    def base_assignment_spec(self, relative_gap, max_iterations, num_processors, background_traffic=False):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        #TODO: Changed background to False
        base_spec = {
            "type": "SOLA_TRAFFIC_ASSIGNMENT",
            "background_traffic": None,
            "classes": [],
            "stopping_criteria": {
                "max_iterations": int(max_iterations), "best_relative_gap": 0.0,
                "relative_gap": float(relative_gap), "normalized_gap": 0.0
            },
            "performance_settings": {"number_of_processors": num_processors},
        }
        if background_traffic:
            base_spec["background_traffic"] = {
                "link_component": "@busveq",     # ul2 = transit flow of the period
                "turn_component": None,
                "add_transit_vehicles": False
            }
        return base_spec

    @_context
    def setup_skims(self, period, scenario):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        emmebank = scenario.emmebank
        with _m.logbook_trace("Extract skims for period %s" % period):
            # temp_functions converts to skim-type VDFs
            with temp_functions(emmebank):
                backup_attributes = {"LINK": ["data2", "auto_volume", "auto_time", "additional_volume"]}
                with gen_utils.backup_and_restore(scenario, backup_attributes):
                    yield
    
    def report(self, period, scenario, classes):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        emmebank = scenario.emmebank
        text = ['<div class="preformat">']
        matrices = []
        for traffic_class in classes:
            matrices.extend(["%s_%s" % (traffic_class["name"], s.split(".")[0]) for s in traffic_class["skims"]])
        num_zones = len(scenario.zone_numbers)
        num_cells = num_zones ** 2
        text.append("""
            Number of zones: %s. Number of O-D pairs: %s. 
            Values outside -9999999, 9999999 are masked in summaries.<br>""" % (num_zones, num_cells))
        text.append("%-25s %9s %9s %9s %13s %9s" % ("name", "min", "max", "mean", "sum", "mask num"))
        for name in matrices:
            name = period + "_" + name
            matrix = emmebank.matrix(name)
            stats = self._stats.get(name)
            if stats is None:
                data = matrix.get_numpy_data(scenario)
                data = numpy.ma.masked_outside(data, -9999999, 9999999, copy=False)
                stats = (name, data.min(), data.max(), data.mean(), data.sum(), num_cells-data.count())
            text.append("%-25s %9.4g %9.4g %9.4g %13.7g %9d" % stats)
        text.append("</div>")
        title = 'Traffic impedance summary for period %s' % period
        report = _m.PageBuilder(title)
        report.wrap_html('Matrix details', "<br>".join(text))
        _m.logbook_write(title, report.render())
        
    
            
    def prepOutputTables(self, period, scenario, classes):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        sm = 1000 * int(scenario)
        idx = 400
        create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
        for vehClass in classes:
            for skim in vehClass['skims']:
                matrixName = "%s_%s__%s"%(vehClass['name'], skim.split(".")[0], period)
                matrixId  = "mf%s"%(sm + idx)
                create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
                idx += 1
                

        
        
@_context
def temp_functions(emmebank):
    change_function = _m.Modeller().tool(
        "inro.emme.data.function.change_function")
    orig_expression = {}
    with _m.logbook_trace("Set functions to skim parameter"):
        for func in emmebank.functions():
            if func.prefix=="fd":
                exp = func.expression
                orig_expression[func] = exp
                if "volau+volad" in exp:
                    exp = exp.replace("volau+volad", "ul2")
                    change_function(func, exp, emmebank)
    try:
        yield
    finally:
        a=1 #nothing
#    #with _m.logbook_trace("Reset functions to assignment parameters"):
#        #for func, expression in orig_expression.iteritems():
#            #change_function(func, expression, emmebank)
