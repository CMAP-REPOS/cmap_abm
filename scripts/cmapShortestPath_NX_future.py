import math
import random
import sys
import pickle
import csv
import os
import pdb
from collections import defaultdict
from heapq import heapify, heappop, heappush
import parameters
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import shapefile
import gc
import networkx as nx
import time
from multiprocessing import Process
import cmapMAZLUShare


mazLandUseShare = cmapMAZLUShare.getLandUseShare()
nodeMazDict = cmapMAZLUShare.getMAZaroundNodes()

def getEucleadianDist(startnodeX, startnodeY, endnodeX, endnodeY):

    return math.sqrt( ((startnodeX-endnodeX)*(startnodeX-endnodeX)) +
                      ((startnodeY-endnodeY)*(startnodeY-endnodeY)) )

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
#print net.number_of_edges()
#print net.number_of_nodes()

nodeDistHwy = {}
for entry in csv.DictReader(open(parameters.nodeDistFromHwy, "r")):
    node_id6 = int(entry["ID"])
    nodeDistHwy[node_id6] = float(entry["hwyDistance"])

def ShortestPath(net, sourceNode, nodeCoordsX, nodeCoordsY, fileName):
    output = open(fileName, "a")
    predDict = {}
    costLabel = {}
    markedNodes = {}
    for node in net.nodes():
        costLabel[node] = sys.maxint
        predDict[node] = None
        markedNodes[node] = 0

    nodesToExamine = []
    costLabel[sourceNode] = 0
    heappush(nodesToExamine, (costLabel[sourceNode], sourceNode))

    while len(nodesToExamine) != 0:
        popped = heappop(nodesToExamine)
        minCostNode = popped[1]
        minCost = popped[0]

        sourceNodeX = nodeCoordsX[sourceNode]
        sourceNodeY = nodeCoordsY[sourceNode]
        minCostNodeX = nodeCoordsX[minCostNode]
        minCostNodeY = nodeCoordsY[minCostNode]
        edist = getEucleadianDist(sourceNodeX, sourceNodeY, minCostNodeX, minCostNodeY)


        if minCost > parameters.nineMiles:
            break
        elif minCostNode<parameters.maxMAZID:
            predNode = predDict[minCostNode]
            if predNode != None:
                predOfPred = predDict[predNode]

                spCost = minCost - parameters.connectorEDist
                firstNode = minCostNode
                secondNode = minCostNode
                nearHwy = 0
                nearHwyDist = 0
                while firstNode != sourceNode:
                    secondNode = firstNode
                    firstNode = predDict[firstNode]

                currentSpNode = minCostNode
                nextSpNode = minCostNode
                prevSpNode = minCostNode
                total_arc_weight = 0
                luPerNode = [0.000000 for x in range(parameters.landUseTypes)]
                pathWeight = [0.000000 for x in range(parameters.landUseTypes)]

                while currentSpNode != sourceNode:
                    node_weight = 0
                    arc_weight = 0
                    nextSpNode = currentSpNode
                    currentSpNode = predDict[currentSpNode]
                    prevSpNode = predDict[currentSpNode]

                    if currentSpNode != None:
                        for node in net.successors(currentSpNode):
                            if nextSpNode == node:
                                node_weight = node_weight+net[currentSpNode][node]['cost']

                    if prevSpNode != None:
                        for node in net.successors(prevSpNode):
                            if currentSpNode == node:
                                node_weight = node_weight+net[prevSpNode][node]['cost']
                    try:
                        if nodeDistHwy[currentSpNode] > 0:
                            nearHwy = 1
                            nearHwyDist = nodeDistHwy[currentSpNode]
                    except KeyError:
                        pass

                    arc_weight = node_weight/2

                    try:
                        currNodeMaz = nodeMazDict[currentSpNode]

                        if len(currNodeMaz) > 0:

                            if currentSpNode != None:
                                for mazS in currNodeMaz:
                                    if sum(mazLandUseShare[mazS])>0:
                                        for luTypes in range(parameters.landUseTypes):
                                            luPerNode[luTypes] = (mazLandUseShare[mazS][luTypes])*arc_weight + luPerNode[luTypes]
                                        total_arc_weight = total_arc_weight+arc_weight
                    except KeyError:
                        pass

                if sum(luPerNode)>0:
                    pathWeight = [x/total_arc_weight for x in luPerNode]

                edist2 = getEucleadianDist(nodeCoordsX[minCostNode], nodeCoordsY[minCostNode], nodeCoordsX[predDict[minCostNode]], nodeCoordsY[predDict[minCostNode]] )
                edist3 = getEucleadianDist(nodeCoordsX[firstNode], nodeCoordsY[firstNode], nodeCoordsX[secondNode], nodeCoordsY[secondNode])
                spCost2 = spCost+edist2+edist3

                if (spCost2-edist)>=0:# and predOfPred>parameters.maxMAZID:

                    ratioSpEdist = spCost2/edist
                    if spCost2<parameters.threeMiles:
                        output.write("%d,%d,%f,%f,%f" % (sourceNode, minCostNode, spCost2, edist, ratioSpEdist))
                        for item in pathWeight:
                            output.write(",%f" %item)
                        output.write("\n")

        for downNode in net.successors(minCostNode):

            tmpCost = minCost + net[minCostNode][downNode]['cost']

            if costLabel[downNode] > tmpCost:
                costLabel[downNode] = tmpCost
                predDict[downNode] = minCostNode
                if markedNodes[downNode] == 0:
                    heappush(nodesToExamine, (tmpCost, downNode))
                    markedNodes[downNode] = 1
    output.close()

def cenToCenSP(iter_num):
    start_time = time.time()
    centroids = {}
    cenIter = 1
    net = pickle.load(open(os.path.join(parameters.root, parameters.int_output, "saveG.p"), "rb"))
    nodeCoordsX, nodeCoordsY = getNodeCoords()
    for cen in net.nodes():
        if cen < parameters.maxMAZID:
            centroids[cenIter] = cen
            cenIter = cenIter+1

    iternum = "%s_" % (iter_num+1)
    minCent = (iter_num*(len(centroids)))/parameters.number_of_cores
    maxCent = ((iter_num+1)*(len(centroids)))/parameters.number_of_cores
    fileN = iternum+"_test_parallel_SP_future.txt"
    outputFile = os.path.join(parameters.root, parameters.int_output, fileN)

    fileName2 = open(outputFile, "w")


    print "running sp...","    ", (iter_num+1)
    for cen in centroids:
        if cen < maxCent and cen>=minCent:
            ShortestPath(net, centroids[cen], nodeCoordsX, nodeCoordsY, outputFile)

    elapsed_time = time.time() - start_time
    print "Elapsed Time for core", " ", (iter_num+1), "=", "    ", elapsed_time


if __name__ == '__main__':

    for num in range(parameters.number_of_cores):

        p = Process(target=cenToCenSP, args=(num,))
        p.start()
