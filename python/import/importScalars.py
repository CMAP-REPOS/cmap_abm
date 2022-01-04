#
# Import warm start matrices
#
# importwsmatrices.py
#

import inro.modeller as _m
import inro.emme.core.exception as _except
import traceback as _traceback
from contextlib import contextmanager as _context
import numpy
import array
import os
import json as _json
import inro.emme.desktop.app as _app

WORK_FOLDER = "C:\\projects\\cmap_activitysim\\cmap_abm\\emme_inputs\\scalarmatrices\\cmap_scalars2.out"
PROJECT = "C:\\projects\\cmap_activitysim\\cmap_abm\\CMAP-ABM\\CMAP-ABM.emp"

desktop = _app.start_dedicated(project=PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank
scenario = databank.scenario(1)
desktop.data_explorer().replace_primary_scenario(scenario)

processMatrix = _m.Modeller().tool("inro.emme.data.matrix.matrix_transaction")

processMatrix(transaction_file = WORK_FOLDER)
