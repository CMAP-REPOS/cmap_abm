#
# outputNets.py
#
# Outputs the networks as shapefiles

import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import numpy
import array
import os
import json as _json
import general as gen_utils
import inro.emme.desktop.app as _app

#desktop = _app.start_dedicated(project="D:\\Projects\\Clients\\CMAP\\Model\\New Skims\\CMAP-ABM_TB\\CMAP-ABM.emp", visible=True, user_initials="ASR")
desktop = _app.start_dedicated(project="D:/Projects/Clients/CMAP/Model/onto2050_2019/cmap_abm/CMAP-ABM/CMAP-ABM.emp", visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank

exportOMX = _m.Modeller().tool("inro.emme.data.matrix.export_to_omx")

skimBaseNames = ["ALLPEN_FIRSTWAIT", "ALLPEN_XFERWAIT", "ALLPEN_TOTALIVTT", "ALLPEN_XFERS", "ALLPEN_FARE", "ALLPEN_XFERWALK"]

user_classes = ["1","2","3"]
for uc in user_classes:
    for scen in range(1,9):
        print "Exporting scenario %s"%scen
        actscen = 200 + scen
        scenario = databank.scenario(actscen)
        desktop.data_explorer().replace_primary_scenario(scenario)
        
        #output = "D:\\Projects\\Clients\\CMAP\\Model\\New Skims\\outputs\\orig\\scen0%s\\Ttrips0%s.omx"%(scen, scen)
        
        #mfList = []
        #for m in range(359, 365):
        #    mfList.append("mf%s%s"%(scen, m))
           
        
        #exportOMX(matrices = mfList, export_file = output, append_to_file = False, omx_key = "NAME")
        
        output = "D:\\Projects\\Clients\\CMAP\\Model\\New Skims\\outputs\\scen0%s\\Tskims0%s_uc%s.omx"%(scen, scen, uc)
        
        mfList = []
        for m in skimBaseNames:
            mfList.append("%s%s_uc%s"%(m, scen, uc))
            
        print mfList
        exportOMX(matrices = mfList, export_file = output, append_to_file = False, omx_key = "NAME")

