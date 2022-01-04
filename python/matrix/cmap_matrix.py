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

class CMapMatrix(_m.Tool()):
    __MODELLER_NAMESPACE__ = "cmap"
    period = _m.Attribute(unicode)
    input_directory = _m.Attribute(str)
    tool_run_msg = ""
    
    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["period"]
        
    def __call__(self, period):
        pass
        
    def prepTripTables(self, scenario, period):
        """
        This is a temporary subroutine to prepare trip tables for assignment. This should
        be replaced when possible with trip tables from either ASIM or warm start inputs.
        ----------
        scenario : Scenario
            The scenario to work on
        period : text (NT, EA, AM, MM, MD, AF, PM, EV)
        
        Returns
        -------
        nothing
        """
        sm = 1000 * int(scenario)
        create_matrix = _m.Modeller().tool("inro.emme.data.matrix.create_matrix")
        mNames = {
            400: "%s_SOV_NT_L"%period,
            401: "%s_SOV_TR_L"%period,
            402: "%s_HOV2_L"%period,
            403: "%s_HOV3_L"%period,
            404: "%s_SOV_NT_M"%period,
            405: "%s_SOV_TR_M"%period,
            406: "%s_HOV2_M"%period,
            407: "%s_HOV3_M"%period,
            408: "%s_SOV_NT_H"%period,
            409: "%s_SOV_TR_H"%period,
            410: "%s_HOV2_H"%period,
            411: "%s_HOV3_H"%period,
            412: "%s_TRK_L"%period,
            413: "%s_TRK_M"%period,
            414: "%s_TRK_H"%period
        }    

        # SOV - 400, 401, 404, 405, 408, 409 (from 167, 168, 169, 170)
        inMtxIN = numpy.zeros(scenario.emmebank.matrix("mf%s315"%scenario).get_numpy_data(scenario).shape)
        for m in [167, 168, 169, 170]:
            inMtxIN += scenario.emmebank.matrix("mf%s%s"%(scenario, m)).get_numpy_data(scenario)
        
        for m in [400, 401, 404, 405, 408, 409]:
            matrixName = mNames[m]
            matrixId  = "mf%s"%(sm + m)
            create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
            scenario.emmebank.matrix("mf%s%s"%(scenario, m)).set_numpy_data((1.0/6.0) * inMtxIN, scenario)
        
        # HOV2 - 402, 406, 410 (from 171, 172, 173, 174)
        inMtxIN = numpy.zeros(scenario.emmebank.matrix("mf%s315"%scenario).get_numpy_data(scenario).shape)
        for m in [171, 172, 173, 174]:
            inMtxIN += scenario.emmebank.matrix("mf%s%s"%(scenario, m)).get_numpy_data(scenario)
        
        for m in [402, 406, 410]:
            matrixName = mNames[m]
            matrixId  = "mf%s"%(sm + m)
            create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
            scenario.emmebank.matrix("mf%s%s"%(scenario, m)).set_numpy_data((1.0/3.0) * inMtxIN, scenario)
            
        # HOV3 - 403, 407, 411 (from 175, 176, 177, 178)
        inMtxIN = numpy.zeros(scenario.emmebank.matrix("mf%s315"%scenario).get_numpy_data(scenario).shape)
        for m in [175, 176, 177, 178]:
            inMtxIN += scenario.emmebank.matrix("mf%s%s"%(scenario, m)).get_numpy_data(scenario)
        
        for m in [403, 407, 411]:
            matrixName = mNames[m]
            matrixId  = "mf%s"%(sm + m)
            create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
            scenario.emmebank.matrix("mf%s%s"%(scenario, m)).set_numpy_data((1.0/3.0) * inMtxIN, scenario)
            
        # TRK_Lt - 412 (from 179, 180)
        inMtxIN = numpy.zeros(scenario.emmebank.matrix("mf%s315"%scenario).get_numpy_data(scenario).shape)
        for m in [179, 180]:
            inMtxIN += scenario.emmebank.matrix("mf%s%s"%(scenario, m)).get_numpy_data(scenario)
        
        for m in [412]:
            matrixName = mNames[m]
            matrixId  = "mf%s"%(sm + m)
            create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
            scenario.emmebank.matrix("mf%s%s"%(scenario, m)).set_numpy_data(inMtxIN, scenario)
            
        # TRK_Md - 413 (from 331-332)
        inMtxIN = numpy.zeros(scenario.emmebank.matrix("mf%s315"%scenario).get_numpy_data(scenario).shape)
        for m in range(331, 333):
            inMtxIN += scenario.emmebank.matrix("mf%s%s"%(scenario, m)).get_numpy_data(scenario)
        
        for m in [413]:
            matrixName = mNames[m]
            matrixId  = "mf%s"%(sm + m)
            create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
            scenario.emmebank.matrix("mf%s%s"%(scenario, m)).set_numpy_data(inMtxIN, scenario)
            
        # TRK_Hvy - 413 (from 333-334)
        inMtxIN = numpy.zeros(scenario.emmebank.matrix("mf%s315"%scenario).get_numpy_data(scenario).shape)
        for m in range(333, 335):
            inMtxIN += scenario.emmebank.matrix("mf%s%s"%(scenario, m)).get_numpy_data(scenario)
        
        for m in [414]:
            matrixName = mNames[m]
            matrixId  = "mf%s"%(sm + m)
            create_matrix(matrix_id = matrixId, matrix_name = matrixName, matrix_description = "", scenario = scenario, overwrite = True)
            scenario.emmebank.matrix("mf%s%s"%(scenario, m)).set_numpy_data(inMtxIN, scenario)
            
    def outputSkimsToOMX(self, period, scenario, outputfilename):
        """
        Description #TODO
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        exportOMX = _m.Modeller().tool("inro.emme.data.matrix.export_to_omx")
        skimList = []
        #TODO: below is a copy/paste from the assignment script, it would be better to set
        # this up somewhere else so it's not here twice
        classes = [
                {   # 0
                    "name": 'SOV_NT_L', "mode": 'S', "PCE": 1, "VOT": 36.4, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 1
                    "name": 'SOV_TR_L', "mode": 'S', "PCE": 1, "VOT": 36.4, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 2
                    "name": 'HOV2_L', "mode": 'H', "PCE": 1, "VOT": 61.46, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV2", "TOLLDIST", "HOVDIST"]
                },
                {   # 3
                    "name": 'HOV3_L', "mode": 'H', "PCE": 1, "VOT": 91.17, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV3", "TOLLDIST", "HOVDIST"]
                },
                {   # 4
                    "name": 'SOV_NT_M', "mode": 'S', "PCE": 1, "VOT": 152.4, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 5
                    "name": 'SOV_TR_M', "mode": 'S', "PCE": 1, "VOT": 152.4, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 6
                    "name": 'HOV2_M', "mode": 'H', "PCE": 1, "VOT": 252.3, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV2", "TOLLDIST", "HOVDIST"]
                },
                {   # 7
                    "name": 'HOV3_M', "mode": 'H', "PCE": 1, "VOT": 380.5, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV3", "TOLLDIST", "HOVDIST"]
                },
                {   # 8
                    "name": 'SOV_NT_H', "mode": 'S', "PCE": 1, "VOT": 268.4, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 9
                    "name": 'SOV_TR_H', "mode": 'S', "PCE": 1, "VOT": 268.4, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV", "TOLLDIST"]
                },
                {   # 10
                    "name": 'HOV2_H', "mode": 'H', "PCE": 1, "VOT": 443.17, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV2", "TOLLDIST", "HOVDIST"]
                },
                {   # 11
                    "name": 'HOV3_H', "mode": 'H', "PCE": 1, "VOT": 669.29, "cost": '@toll',
                    "toll": "@toll",
                    "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.HOV3", "TOLLDIST", "HOVDIST"]
                },
                {   # 12
                    "name": 'TRK_L', "mode": 'l', "PCE": 1.3, "VOT": 60., "cost": '@toll2',
                    "toll": "@toll2",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_L"]
                },
                {   # 13
                    "name": 'TRK_M', "mode": 'm', "PCE": 1.5, "VOT": 60., "cost": '@toll3',
                    "toll": "@toll3",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_M"]
                },
                {   # 14
                    "name": 'TRK_H', "mode": 'h', "PCE": 2.5, "VOT": 100., "cost": '@toll4',
                    "toll": "@toll4",
                    "skims": ["TIME", "DIST", "TOLLCOST.TRK_H"]
                }
                ]
        for vehClass in classes:
            for skim in vehClass['skims']:
                skimList.append("%s_%s_%s"%(period, vehClass['name'], skim.split(".")[0]))
        exportOMX(matrices=skimList, export_file=outputfilename, append_to_file=False, omx_key = "NAME")

    def outputTripTablesToOMX(self, period, scenario, outputfilename):
        """
        Writes the trip tables to OMX - this is tempoarary code
        ----------
        inputvar : Type
            input variable description
        
        Returns
        -------
        int_var_name : type
        """
        exportOMX = _m.Modeller().tool("inro.emme.data.matrix.export_to_omx")
        sm = 1000 * int(scenario)
        mList = []
        for m in range(400, 415):
            mList.append("mf%s"%(sm+m))
        exportOMX(matrices=mList, export_file=outputfilename, append_to_file=False)