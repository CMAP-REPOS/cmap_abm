#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
############################################################################# 

#Should always be placed in the same folder as SPwrapper.py

#Wrapper code that post processes the SP output

import parameters
import cmapParallelPP
if parameters.isBaseYear:
  import cmapRStoSubZone
else:
  import cmapRStoSubZone_future

cmapParallelPP.parallelPostProcess()
cmapRStoSubZone.rsToSubZone()