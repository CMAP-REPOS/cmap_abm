#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

#Should always be placed in the same folder as SPwrapper.py

#Combines the CSVs generated into one node CSV and one link CSV. Both are stored in ...\SP\final_output

import parameters
import csv

#############################################################################

def createNetworkInput():
    linkCSV = open(parameters.linkFile,"w")
    linkCSV.write('"FNODE_","TNODE_","LENGTH"\n')
    nodeCSV = open(parameters.nodeFile,"w")
    nodeCSV.write('"ID","X","Y"\n')

    for record in csv.DictReader(open(parameters.centroidsMAZFile, "r")):
        maz = int(record["MAZ_ID_9"])
        x = float(record["X"])
        y = float(record["Y"])
        nodeCSV.write("%d,%f,%f\n" % (maz, x, y))

    for record in csv.DictReader(open(parameters.tapFile, "r")):
        tap = int(record["tap_id"])
        x = float(record["x"])
        y = float(record["y"])
        nodeCSV.write("%d,%f,%f\n" % (tap, x, y))

    for record in csv.DictReader(open(parameters.nodeFile_int, "r")):
        nid = int(record["ID"])
        x = float(record["X"])
        y = float(record["Y"])
        nodeCSV.write("%d,%f,%f\n" % (nid, x, y))

    for record in csv.DictReader(open(parameters.connectorFile, "r")):
        fnode = int(record["FNODE_"])
        tnode = int(record["TNODE_"])
        length= float(record["LENGTH"])
        linkCSV.write("%d,%d,%f\n" % (fnode, tnode, length))

    for record in csv.DictReader(open(parameters.tapConnectorFile, "r")):
        fnode = int(record["FNODE_"])
        tnode = int(record["TNODE_"])
        length= float(record["LENGTH"])
        linkCSV.write("%d,%d,%f\n" % (fnode, tnode, length))

    for record in csv.DictReader(open(parameters.linkFile_int, "r")):
        fnode = int(record["FNODE_"])
        tnode = int(record["TNODE_"])
        length= float(record["LENGTH"])
        linkCSV.write("%d,%d,%f\n" % (fnode, tnode, length))

    linkCSV.close()
    nodeCSV.close()

#############################################################################
