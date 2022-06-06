# 5/28/2021 Andrew Rohne/RSG
# Module for fixing CMAP's capacities
# !important: does not actually need to be run
# unless something overwrites el1, el2, and el3

import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import numpy
import array
import os
import json as _json
import general as gen_utils

class CMapNetwork(_m.Tool()):
    __MODELLER_NAMESPACE__ = "cmap"
    period = _m.Attribute(str)
    input_directory = _m.Attribute(str)
    tool_run_msg = ""
    timeFactors = {}
    
    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["period", "input_directory"]
        version = os.environ.get("EMMEPATH", "")
        self._version = version[-5:] if version else ""
        self.timeFactors = {1: 3.75, 2: 0.75, 3: 1.5, 4: 0.75, 5: 3.0, 6: 1.5, 7: 1.5, 8: 1.5} 
        #self.timeFactors = {1: 10, 2: 1.0, 3: 2.0, 4: 1.0, 5: 4.0, 6: 2.0, 7: 2.0, 8: 2.0} 
        #TODO: ASR changed 2 and 4 to 1.0, check on these
        
    def __call__(self, scenario, input_directory=None, runCapacities = False, export = False, output_directory = None, runPrep = True):
        if runCapacities:
            self.fixCapacities(scenario)
        if runPrep:
            self.prepNetwork(scenario)
        if export:
            self.outputNet(scenario, output_directory)

    def fixCapacities(self, scenario):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        net_calc = gen_utils.NetworkCalculator(scenario) 
        net_calc("ul1", "@ftime", "modes=ASHbmh")
        net_calc("ul2", "@emcap * lanes * %s" % self.timeFactors[int(scenario)], "modes=ASHbmh")
        #net_calc("ul2", "@emcap * lanes", "modes=ASHbmh")
        net_calc("ul3", "0", "modes=ASHbmh")
        net_calc("ul3", "@cyclej * 10000 + 100*@gc", "vdf=1 or vdf=3")
                           
    def prepNetwork(self, scenario):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        netCalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")
        if not ("@sta_reliability" in scenario.attributes("LINK")):
            scenario.create_extra_attribute("LINK", "@sta_reliability", 0.0)
        if not ("@reliability_sq" in scenario.attributes("LINK")):
            scenario.create_extra_attribute("LINK", "@reliability_sq", 0.0)
        if not ("@tolldist" in scenario.attributes("LINK")):
            scenario.create_extra_attribute("LINK", "@tolldist", 0.0)
        spec = {
            "type": "NETWORK_CALCULATION",
            "expression": "length",
            "selections":{
                "link": "@tollv=1"
            },
            "result": "@tolldist"
        }
        report = netCalc(spec)
        if not ("@hovdist" in scenario.attributes("LINK")):
            scenario.create_extra_attribute("LINK", "@hovdist", 0.0) 
        if("@auto_time" in scenario.attributes("LINK")):
            scenario.delete_extra_attribute("@auto_time")
        if not ("@auto_time" in scenario.attributes("LINK")):
            scenario.create_extra_attribute("LINK", "@auto_time", 0.0)            
        spec = {
            "type": "NETWORK_CALCULATION",
            "expression": "@ftime",
            "selections":{
                "link": "all"
            },
            "result": "@auto_time"            
        }
        report = netCalc(spec)
        
        if not ("@cost_operating" in scenario.attributes("LINK")):
            scenario.create_extra_attribute("LINK", "@cost_operating", 0.0)            
        spec = {
            "type": "NETWORK_CALCULATION",
            "expression": "@ftime",
            "selections":{
                "link": "all"
            },
            "result": "@cost_operating"            
        }
        report = netCalc(spec)
        
    def outputNet(self, scenario, outputFolder):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        export_linkshape = _m.Modeller().tool("inro.emme.data.network.export_network_as_shapefile")
        if int(scenario) <= 8: # scen 1-8 are highway scenarios
            export_linkshape(export_path = outputFolder, selection = {'link': 'all'}) # default is all elements
        else: # scen 201-208 are transit scenarios
            export_linkshape(export_path = outputFolder, 
                            transit_shapes = "LINES",
                            selection = {"node": "all", 
                                        "link": "none", 
                                        "turn": "none",
                                        "transit_line": "all"} )
