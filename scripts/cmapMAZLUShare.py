#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

#Should always be placed in the same folder as SPwrapper.py

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

def getLandUseShare():

    result = {}
    #count1 = {}
    for i in range(16820):
        result[i] = []
        #count1[i] = 0
    fileName = parameters.mazLUFile
    line = 0
    for fields in csv.DictReader(open(fileName, "r")):
        line += 1
        mazId = int(fields["MAZ_ID"])
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


        if mazId == 0:
            continue

        result[mazId].append(lu1)
        result[mazId].append(lu2)
        result[mazId].append(lu3)
        result[mazId].append(lu4)
        result[mazId].append(lu5)
        result[mazId].append(lu6)
        result[mazId].append(lu7)
        result[mazId].append(lu8)
        result[mazId].append(lu9)
        result[mazId].append(lu10)
        result[mazId].append(lu11)
        result[mazId].append(lu12)

        #count1[mazId] = count1[mazId]+1

    result = dict([(k,v) for k,v in result.items() if len(v)>0])

    #print count1[2504]
    #print result[2504][1]

    return result

#############################################################################

def getMAZaroundNodes():

    result = {}
    #count1 = {}
    #for i in range(95141329):
        #result[i] = []
        #count1[i] = 0
    fileName = parameters.nodesinMAZFile
    line = 0
    for fields in csv.DictReader(open(fileName, "r")):

        mazId = int(fields["MAZ_ID_9"])
        nodes = int(fields["Nodes"])
        result[nodes] = []

    for fields in csv.DictReader(open(fileName, "r")):

        mazId = int(fields["MAZ_ID_9"])
        nodes = int(fields["Nodes"])



        if mazId == 0:
            continue

        result[nodes].append(mazId)
        #count1[mazId] = count1[mazId]+1

    result = dict([(k,v) for k,v in result.items() if len(v)>0])
    #print result[904259084][1]

    return result

getLandUseShare()
getMAZaroundNodes()
