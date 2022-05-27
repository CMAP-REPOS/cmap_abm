# Copied from SANDAG and modified 4/15/21 Andrew Rohne, Ted Lin/RSG

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
            
    def outputAutoSkimsToOMX(self, period, scenario, outputfilename):
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
                {"name": 'SOV_NT_L', "skims": ["TIME", "DIST", "TOLLCOST.SOV"]},
                {"name": 'SOV_TR_L', "skims": ["TIME", "DIST", "TOLLCOST.SOV"]},
                {"name": 'HOV2_L', "skims": ["TIME", "DIST", "TOLLCOST.HOV2"]},
                {"name": 'HOV3_L', "skims": ["TIME", "DIST", "TOLLCOST.HOV3"]},
                {"name": 'SOV_NT_M', "skims": ["TIME", "DIST", "FTIME", "TOLLCOST.SOV"]},
                {"name": 'SOV_TR_M', "skims": ["TIME", "DIST", "TOLLCOST.SOV"]},
                {"name": 'HOV2_M', "skims": ["TIME", "DIST", "TOLLCOST.HOV2"]},
                {"name": 'HOV3_M', "skims": ["TIME", "DIST", "TOLLCOST.HOV3"]},
                {"name": 'SOV_NT_H', "skims": ["TIME", "DIST", "TOLLCOST.SOV"]},
                {"name": 'SOV_TR_H', "skims": ["TIME", "DIST", "TOLLCOST.SOV"]},
                {"name": 'HOV2_H', "skims": ["TIME", "DIST", "TOLLCOST.HOV2"]},
                {"name": 'HOV3_H', "skims": ["TIME", "DIST", "TOLLCOST.HOV3"]}
                ]
        for vehClass in classes:
            for skim in vehClass['skims']:
                skimList.append("%s_%s__%s"%(vehClass['name'], skim.split(".")[0], period))
        exportOMX(matrices=skimList, export_file=outputfilename, append_to_file=True, omx_key = "NAME")

    def outputTransitSkimsToOMX(self, period, scenario, outputfilename):
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
        skim_matrices = ["FIRSTWAIT", "XFERWAIT", "FARE", "XFERS", "ACC", "XFERWALK", "EGR", "TOTALIVTT", "CTABUSLIVTT", 
                            "PACEBUSRIVTT", "PACEBUSLIVTT", "PACEBUSEIVTT", "CTABUSEIVTT", "CTARAILIVTT", "METRARAILIVTT"]
        access_modes = ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]
        user_classes = ["L","M","H"]
        for amode in access_modes:
            for uc in user_classes:
                for skim in skim_matrices:
                    #print("%s_%s_%s_%s"%(skim, amode, uc, period))
                    skimList.append("%s_%s_%s__%s"%(skim, amode, uc, period))
        exportOMX(matrices=skimList, export_file=outputfilename, append_to_file=True, omx_key = "NAME")

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