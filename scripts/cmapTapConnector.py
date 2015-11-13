#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
############################################################################# 

#Should always be placed in the same folder as SPwrapper.py

#Generates one connector for every TAP -- from TAP to nearest "regular" node. The csv file is stored in ...\SP\int_output

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


output = open(parameters.tapConnectorFile, "w")
output.write('"FNODE_","TNODE_","LENGTH"\n')

############################################################################# 

def getEucleadianDist(startnode, endnode):

    return math.sqrt( ((startnode.getX()-endnode.getX())*(startnode.getX()-endnode.getX())) +
                      ((startnode.getY()-endnode.getY())*(startnode.getY()-endnode.getY())) )

############################################################################# 

def getNodesInsideMAZ():
    
    result = {}
    count1 = {}
    #for i in range(16820):
    	#result[i] = []
    	#count1[i] = 0
    fileName = parameters.tapFile
    line = 0
    for fields in csv.DictReader(open(fileName, "r")):
        line += 1
        mazId = int(fields["tap_id"])
        nodes = int(fields["node_closest"])
        
        
        result[mazId] = nodes
    
    
    return result

############################################################################# 

def getCentroids():
                                                           
    results = {}
    fileName = parameters.tapFile
    for record in csv.DictReader(open(fileName, "r")):
        maz = int(record["tap_id"])
        x = float(record["x"])
        y = float(record["y"])
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

        result = []
        i = 0
        try:
            for nodeId in nodesInside:

                i = i + 1
                x,y = nodes[int(nodeId)]
	        if sum(nodes[int(nodeId)]) > 0:
	           dist = math.sqrt((x - centX) ** 2 + (y - centY) ** 2)
                   result.append((dist, nodeId))
        except KeyError:
               pass

        result = sorted(result, key = lambda x:x[0])
        return result
 
############################################################################# 

def createNetworkWithCentroidConnectors():

    nodesInsideMAZ = getNodesInsideMAZ()
    centroids = getCentroids()
    nodes = getNodes() 

    w = shapefile.Writer(shapefile.POLYLINE) 
    
    w.field("Start", "N", 10)
    w.field("End", "N", 10)
    w.field("Length","C", 20)
    length = parameters.SPThreshold
    for mazId, nodesInside in nodesInsideMAZ.iteritems():
        centX, centY = centroids[mazId]


        #sortedConnectors = sortConnectorsByDist(nodes, centroids, mazId, nodesInside) 
        
        x,y = nodes[nodesInside]

        centerline = ((centX, centY), (x, y)) 
        dist = math.sqrt((x - centX) ** 2 + (y - centY) ** 2)
        w.line(parts=[centerline])
        w.record(mazId, nodesInside,dist)
        centerline = ((x, y), (centX, centY))
        w.line(parts=[centerline])            
        w.record(nodesInside, mazId,dist)
        output.write("%d,%d,%f\n" % (mazId, nodesInside, length))
        output.write("%d,%d,%f\n" % (nodesInside, mazId, length))
    #print "done" 
    #w.save("E:\\CMAP\\SHPFiles\\NavTeq_Shp\\CMAP_TAPConnectorsMin_v2")
    output.close()



