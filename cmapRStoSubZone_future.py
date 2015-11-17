#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

import math
import random
import sys
import pickle
import csv
import os
import pdb
from collections import defaultdict
from heapq import heapify, heappop, heappush
from array import *
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import shapefile
import parameters

def rsToSubZone():
    output = open(parameters.spInsideRS8, "w")
    output.write('"Origin","Destin","SP","Euclid","Ratio","nearHwy","nearHwyDist","LU_1","LU_2","LU_3","LU_4","LU_5","LU_6","LU_7","LU_8","LU_9","LU_10","LU_11","LU_12"\n')
    output2 = open(parameters.spOutsideRS8, "w")
    output2.write('"Origin","Destin","SP","Euclid","Ratio","nearHwy","nearHwyDist","LU_1","LU_2","LU_3","LU_4","LU_5","LU_6","LU_7","LU_8","LU_9","LU_10","LU_11","LU_12"\n')
    szCorr = {}
    for entry2 in csv.DictReader(open(parameters.nodeFile, "r")):
        node_id = int(entry2["ID"])
        xcoord = float(entry2["x"])
        ycoord = float(entry2["y"])
        szCorr[node_id] = 0

    for entry in csv.DictReader(open(parameters.rsToSZInput, "r")):
        rs_id = int(entry["RS"])
        zone09 = int(entry["ZONE09"])
        subzone09 = int(entry["SUBZONE09"])
        szCorr[subzone09] = rs_id

    szForTap = {}
    for fields in csv.DictReader(open(parameters.tapFile, "r")):
        tap_id = int(fields["tap_id"])
        sz09 = int(fields["maz09"])
        szForTap[tap_id] = sz09

    for fields in csv.DictReader(open(parameters.outputSPFile, "r")):
        origin = int(fields["Origin"])
        destin = int(fields["Destin"])
        sp = float(fields["SP"])
        euclid = float(fields["Euclid"])
        ratio = float(fields["Ratio"])
        nhwy = int(fields["nearHwy"])
        nhwydist = float(fields["nearHwyDist"])
        lu1 = float(fields["LU_1"])
        lu2 = float(fields["LU_2"])
        lu3 = float(fields["LU_3"])
        lu4 = float(fields["LU_4"])
        lu5 = float(fields["LU_5"])
        lu6 = float(fields["LU_6"])
        lu7 = float(fields["LU_7"])
        lu8 = float(fields["LU_8"])
        lu9 = float(fields["LU_9"])
        lu10 = float(fields["LU_10"])
        lu11 = float(fields["LU_11"])
        lu12 = float(fields["LU_12"])
        if destin > parameters.minTAPID:
            try:
                if origin == szForTap[destin]:
                    sp = euclid
                    count_ = count_+1
            except KeyError:
                pass

        if (origin<destin):
            if (origin+destin<parameters.minTAPID):
                if ((szCorr[origin] < parameters.rsThreshold ) or (szCorr[destin] < parameters.rsThreshold)):

                    output.write("%d,%d,%f,%f,%f,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\n" % (origin, destin, sp, euclid, ratio, nhwy, nhwydist, lu1, lu2, lu3, lu4, lu5, lu6, lu7, lu8, lu9, lu10, lu11, lu12))
                else:
                    output2.write("%d,%d,%f,%f,%f,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f\n" % (origin, destin, sp, euclid, ratio, nhwy, nhwydist, lu1, lu2, lu3, lu4, lu5, lu6, lu7, lu8, lu9, lu10, lu11, lu12))
