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
import openmatrix as omx

class CMapMatrix(_m.Tool()):
    __MODELLER_NAMESPACE__ = "cmap"
    period = _m.Attribute(str)
    input_directory = _m.Attribute(str)
    tool_run_msg = ""
    
    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.attributes = ["period"]
        
    def __call__(self, period):
        pass
        
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
        skim_matrices = ["FIRSTWAIT", "XFERWAIT", "FARE", "XFERS", "ACC", "XFERWALK", "EGR", 
                            "CTABUSLOCIVTT", "PACEBUSLOCIVTT", "BUSEXPIVTT", "CTARAILIVTT", "METRARAILIVTT", "CROWD", "CAPPEN"]
        access_modes = ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]
        user_classes = ["L","M","H"]
        for amode in access_modes:
            for uc in user_classes:
                for skim in skim_matrices:
                    #print("%s_%s_%s_%s"%(skim, amode, uc, period))
                    skimList.append("%s_%s_%s__%s"%(skim, amode, uc, period))
        exportOMX(matrices=skimList, export_file=outputfilename, append_to_file=True, omx_key = "NAME")
        if period == "NT":
            taz_skims = omx.open_file(outputfilename,'a')
            for m in skimList:
                new_skim = m[:-2] + "EV"
                taz_skims[new_skim] = taz_skims[m]
            taz_skims.close()
        elif period == "AM":
            taz_skims = omx.open_file(outputfilename,'a')
            for m in skimList:
                new_skim = m[:-2] + "EA"
                taz_skims[new_skim] = taz_skims[m]
            taz_skims.close()
        elif period == "MD":
            taz_skims = omx.open_file(outputfilename,'a')
            for m in skimList:
                new_skim = m[:-2] + "MM"
                new_skim2 = m[:-2] + "AF"
                taz_skims[new_skim] = taz_skims[m]
                taz_skims[new_skim2] = taz_skims[m]
            taz_skims.close()

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