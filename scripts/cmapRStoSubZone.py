#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

#Should always be placed in the same folder as SPwrapper.py

#Post-processes the output to exclude all TAP-TAP paths

import math
#import random
import sys
#import pickle
import csv
import os
#import pdb
from collections import defaultdict
from heapq import heapify, heappop, heappush
from array import *
#import numpy as np
import shapefile
import parameters

#############################################################################

def rsToSubZone():
    output = open(parameters.spMAZ2TAP, "w")
    output.write('"Origin","Destin","SP","Euclid","Ratio"\n')

    szForTap = {}
    for fields in csv.DictReader(open(parameters.tapFile, "r")):
        tap_id = int(fields["tap_id"])
        sz09 = int(fields["maz09"])
        szForTap[tap_id] = sz09
    count_ = 1
    for fields in csv.DictReader(open(parameters.outputSPFile, "r")):
        origin = int(fields["Origin"])
        destin = int(fields["Destin"])
        sp = float(fields["SP"])
        euclid = float(fields["Euclid"])
        ratio = float(fields["Ratio"])
        if destin > parameters.minTAPID:
            try:
                if origin == szForTap[destin]:
                    sp = euclid
                    count_ = count_+1
            except KeyError:
                pass



        if (origin+destin<parameters.minTAPid):
            output.write("%d,%d,%f,%f,%f\n" % (origin, destin, sp, euclid, ratio))
    print count_
    output.close()

#############################################################################
