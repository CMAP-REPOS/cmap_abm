#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

#Should always be placed in the same folder as SPwrapper.py

#install required Python libraries in the installers folder:
# 1) pyshp
#			copy shapefile.py from pyshp-1.1.4-py2.6.egg folder to:
#     C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.1\Python26\Lib\site-packages
# 2) matplotlib
#			copy contents of matplotlib-1.1.1.win32-py2.6 folder to:
#			C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.1\Python26\Lib\site-packages
# 3) networkX
#			copy contents of networkX1.7 folder to:
#			C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.1\Python26\Lib\site-packages
#			install networkX >>python setup.py install

#############################################################################
#Computes the MAZ-MAZ and MAZ-TAP shortest paths. Generates CSV files equal to the #processors since SPs are computed in parallel

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
import shapefile
#import gc
import networkx as nx
import time
import pickle

# function retunrs euclidean distance between two points, inputs are the X and Y co-ordinates of the start and end points
def getEucleadianDist(startnodeX, startnodeY, endnodeX, endnodeY):

    return math.sqrt( ((startnodeX-endnodeX)*(startnodeX-endnodeX)) +
                      ((startnodeY-endnodeY)*(startnodeY-endnodeY)) )

#############################################################################

#this function uses networkX to read in the network. Only link start and end nodes file is required to define the network


def pickleNetwork():
    G=nx.DiGraph()
    count_ = 1

    fLink = open(parameters.linkFile, "rb")
    for record in csv.DictReader(fLink):
        fNode = int(record["FNODE_"])
        tNode = int(record["TNODE_"])
        length = float(record["LENGTH"])
        try:
           if G[fNode][tNode]['cost'] > 0:
              dupLinks.write("%d,%d\n" % (fNode, tNode))
              count_ = count_+1
        except KeyError:
              pass

        G.add_edge(fNode,tNode, cost = length)

    fLink.close()

    #pickle network
    pickle.dump(G, open(os.path.join(parameters.root, parameters.int_output, "saveG.p"),"wb"))

#############################################################################

#The function below reads in the X and Y co-ordinates f the nodes from the node co-ordinates file
def getNodeCoords():
    nodeCoordsX = {}
    nodeCoordsY = {}
    for record in csv.DictReader(open(parameters.nodeFile, "r")):
        id_ = int(record["ID"])
        x = float(record["X"])
        y = float(record["Y"])
        nodeCoordsX[id_] = x
        nodeCoordsY[id_] = y
    return nodeCoordsX, nodeCoordsY

#############################################################################

# this function computes the shortest path between MAZ centroids/TAPs. It is the classic Dijkstra's algorithm implemented
#using a heap data structure. The inputs are the source node, the node co-ordinates dict and the output filename
def ShortestPath(net, sourceNode, nodeCoordsX, nodeCoordsY, fileName):
    output = open(fileName, "a")
    predDict = {} #initialize predecessor array (dict)
    costLabel = {} #initialize node cost label
    markedNodes = {} #initialize dict which records if a node has already been reached (marked) from the source node
    for node in net.nodes():
    #initial values
        costLabel[node] = sys.maxint
        predDict[node] = None
        markedNodes[node] = 0

    nodesToExamine = [] #the list of nodes that need to be examined at a particular iteration
    costLabel[sourceNode] = 0
    heappush(nodesToExamine, (costLabel[sourceNode], sourceNode)) #heapify the nodes to be examined by cost label

    while len(nodesToExamine) != 0: #main SP loop. Continues till all nodes have been "examined"
          popped = heappop(nodesToExamine)
          minCostNode = popped[1] #pop node with least cost from heap
          minCost = popped[0]#pop least cost node's cost label too

          sourceNodeX = nodeCoordsX[sourceNode] #X and Y co-ordinates of source node and the popped node
          sourceNodeY = nodeCoordsY[sourceNode]
          minCostNodeX = nodeCoordsX[minCostNode]
          minCostNodeY = nodeCoordsY[minCostNode]
	  edist = getEucleadianDist(sourceNodeX, sourceNodeY, minCostNodeX, minCostNodeY)


	  if minCost > parameters.SPThresholdConn: #proceed only if the popped node is less than three miles from the source node
	     break
	  elif minCostNode<parameters.maxMAZID: #proceed only if popped node is an MAZ centroid or a TAP
	       predNode = predDict[minCostNode]
	       if predNode != None:
	          predOfPred = predDict[predNode]

	          spCost = minCost - parameters.connectorEDist
	          firstNode = minCostNode
	          secondNode = minCostNode

	          while firstNode != sourceNode: #prevent all paths which are centroid-node-centroid (as those paths only contain connectors)
	                secondNode = firstNode
	                firstNode = predDict[firstNode]

	          edist2 = getEucleadianDist(nodeCoordsX[minCostNode], nodeCoordsY[minCostNode], nodeCoordsX[predDict[minCostNode]], nodeCoordsY[predDict[minCostNode]] )
	          edist3 = getEucleadianDist(nodeCoordsX[firstNode], nodeCoordsY[firstNode], nodeCoordsX[secondNode], nodeCoordsY[secondNode])
	          spCost2 = spCost+edist2+edist3

	          if (spCost2-edist)>=0:# and predOfPred>parameters.maxMAZID:
	             ratioSpEdist = spCost2/edist
	             if spCost2<parameters.SPThresholdAct:
	                output.write("%d,%d,%f,%f,%f\n" % (sourceNode, minCostNode, spCost2, edist, ratioSpEdist))

                  """if (spCost-edist)<0 and predOfPred<=parameters.maxMAZID:
	             edist2 = getEucleadianDist(nodeCoordsX[minCostNode], nodeCoordsY[minCostNode], nodeCoordsX[predDict[minCostNode]], nodeCoordsY[predDict[minCostNode]] )
	             edist3 = getEucleadianDist(nodeCoordsX[firstNode], nodeCoordsY[firstNode], nodeCoordsX[secondNode], nodeCoordsY[secondNode])
	             spCost2 = spCost+edist2+edist3
	             ratioSpEdist = spCost2/edist
	             if spCost2<parameters.SPThresholdAct:
	                output.write("%d,%d,%f,%f,%f\n" % (sourceNode, minCostNode, spCost2, edist, ratioSpEdist))"""

	  for downNode in net.successors(minCostNode): #push nodes that can be reached from popped nodes into the "nodes to examine" heap

	      tmpCost = minCost + net[minCostNode][downNode]['cost']

	      if costLabel[downNode] > tmpCost:
	         costLabel[downNode] = tmpCost
	         predDict[downNode] = minCostNode
	         if markedNodes[downNode] == 0:
	            heappush(nodesToExamine, (tmpCost, downNode)) #push only nodes whose costlabel has changed for the better
	            markedNodes[downNode] = 1
    output.close()

#############################################################################

# this function runs the SP function for different MAZ centroids and TAPs
def cenToCenSP(iter_num):
    start_time = time.time()
    centroids = {} #initialize centroids dict
    cenIter = 1
    net = pickle.load(open(os.path.join(parameters.root, parameters.int_output, "saveG.p"), "rb"))
    nodeCoordsX, nodeCoordsY = getNodeCoords()
    for cen in net.nodes():
        if cen < parameters.maxMAZID:
           centroids[cenIter] = cen
           cenIter = cenIter+1

    iternum = "%s_" % (iter_num+1)
    minCent = (iter_num*(len(centroids)))/parameters.number_of_cores #the centroids that will be assigned to a particular core
    maxCent = ((iter_num+1)*(len(centroids)))/parameters.number_of_cores
    fileN = iternum+"_test_parallel_SP.txt"
    outputFile = os.path.join(parameters.root, parameters.int_output, fileN)

    fileName2 = open(outputFile, "w")


    print "running sp in processor number...","    ", (iter_num+1)
    for cen in centroids:
        if cen < maxCent and cen>=minCent:
           ShortestPath(net, centroids[cen], nodeCoordsX, nodeCoordsY, outputFile) #run SP for the centroids that are greater than minCent and lte maxCent

    elapsed_time = time.time() - start_time
    print "Elapsed Time for core", " ", (iter_num+1), "=", "    ", elapsed_time
    fileName2.close()

#############################################################################

