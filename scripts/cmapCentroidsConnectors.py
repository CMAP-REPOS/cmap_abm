#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

#Should always be placed in the same folder as SPwrapper.py

#Generates four connectors for every MAZ centroid. The csv file is stored in ...\SP\int_output

import math
#import random
import sys
#import pickle
import csv
import os
#import pdb
from collections import defaultdict
from heapq import heapify, heappop, heappush
import parameters
#import numpy as np
import shapefile
import pickle

output = open(parameters.connectorFile, "w")
output.write('"FNODE_","TNODE_","LENGTH"\n')

#############################################################################

def getEucleadianDist(startnode, endnode):

    return math.sqrt( ((startnode.getX()-endnode.getX())*(startnode.getX()-endnode.getX())) +
                      ((startnode.getY()-endnode.getY())*(startnode.getY()-endnode.getY())) )

#############################################################################

def getNodesInsideMAZ():

    result = {}
    count1 = {}
    for i in range((parameters.maxCenID)+1):
        result[i] = []
        count1[i] = 0
    fileName = parameters.nodesinMAZFile
    line = 0
    for fields in csv.DictReader(open(fileName, "r")):
        line += 1
        mazId = int(fields["MAZ_ID_9"])
        nodes = int(fields["Nodes"])
        nodes2 = nodes

        if mazId == 0:
            continue

        result[mazId].append(nodes2)
        count1[mazId] = count1[mazId]+1

    result = dict([(k,v) for k,v in result.items() if len(v)>0])
    #print count1[35]

    return result

#############################################################################

def getCentroids():

    results = {}
    fileName = parameters.centroidsMAZFile
    for record in csv.DictReader(open(fileName, "r")):
        maz = int(record["MAZ_ID_9"])
        x = float(record["X"])
        y = float(record["Y"])
        results[maz] = (x,y)

    return results

#############################################################################

def getNodes():

    results = {}
    fileName = parameters.nodeFile_int
    for record in csv.DictReader(open(fileName, "r")):
        maz = int(record["ID"])
        x = float(record["X"])
        y = float(record["Y"])
        results[maz] = (x,y)

    return results

#############################################################################

def sortConnectorsByDist(nodes, centroids, mazId, nodesInside):

    centX, centY = centroids[mazId]
    #if mazId == 35:
       #print nodesInside

    #print "check6"
    result = []
    i = 0
    try:
        for nodeId in nodesInside:
            #print "checkNNNN"
            i = i + 1
            x,y = nodes[int(nodeId)]
            #if mazId == 35:
               #print x, y
            if sum(nodes[int(nodeId)]) != 0:
               #if mazId == 25:
                  #print 'inside loop', x, y
                dist = math.sqrt((x - centX) ** 2 + (y - centY) ** 2)
                if dist > 0:
                    result.append((dist, nodeId))
    except KeyError:
        pass

    result = sorted(result, key = lambda x:x[0])
    #print result
    #if mazId == 35:
       #print result
    return result

#############################################################################

def createNetworkWithCentroidConnectors():
    #print "check1"
    nodesInsideMAZ = getNodesInsideMAZ()
    centroids = getCentroids()
    nodes = getNodes()

    w = shapefile.Writer(shapefile.POLYLINE)

    w.field("Start", "N", 10)
    w.field("End", "N", 10)
    length = parameters.SPThreshold
    for mazId, nodesInside in nodesInsideMAZ.iteritems():
        centX, centY = centroids[mazId]

        #print "Check2"
        sortedConnectors = sortConnectorsByDist(nodes, centroids, mazId, nodesInside)

        i = 0
        for dist, nodeId in sortedConnectors:
        #print "cehck3"
            i = i + 1
            if i > 4:
                break
            x,y = nodes[int(nodeId)]

            centerline = ((centX, centY), (x, y))
            w.line(parts=[centerline])
            w.record(mazId, nodeId)
            centerline = ((x, y), (centX, centY))
            w.line(parts=[centerline])
            w.record(nodeId, mazId)
            #print "check"
            output.write("%d,%d,%f\n" % (mazId, nodeId, length))
            output.write("%d,%d,%f\n" % (nodeId, mazId, length))

    #print "done"
    #w.save("E:\\CMAP\\SHPFiles\\NavTeq_Shp\\CMAPConnectorsMin4_v2")
    output.close()
