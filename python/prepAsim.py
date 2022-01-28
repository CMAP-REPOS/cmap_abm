#
# Prepare files for ActivitySim
#

import utilities.asim_input_prep as _a
import utilities.cmap_utilities as _u
import network.cmap_maz_tap as _cmt
import os

modelYaml = os.environ['MODEL_PARAMETERS']

utils = _u.CMapUtilities()
parms = utils.readYaml(modelYaml)

#TODO: remove before flight
#ip = _a.ASimInputPrep()
#ip(parms)

mmt = _cmt.CMapMazTap()
mmt(parms)