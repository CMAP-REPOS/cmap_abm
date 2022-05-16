#
# Prepare files for ActivitySim
#

import utilities.asim_input_prep as _a
import utilities.cmap_utilities as _u
import network.cmap_maz_stop as _cmt
import os

modelYaml = os.environ['MODEL_PARAMETERS']

utils = _u.CMapUtilities()
parms = utils.readYaml(modelYaml)

#TODO: to be removed
ip = _a.ASimInputPrep()
ip(parms)

mmms = _cmt.CMapMazStop()
mmms(parms)