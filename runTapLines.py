#############################################################################
# Create lines by tap file for CT-RAMP
# Ben Stabler, stabler@pbworld.com, 02/18/13
# "C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.3\Python26\python.exe" runTapLines.py
#############################################################################

#load libraries
import inro.modeller as m
import inro.emme.desktop.app as d
import inro.emme.prompt as p
import os, csv, math, pickle, datetime, sys

#EMME project file
empFile = "CMAP-ABM/CMAP-ABM.emp"

#scenarios
transitImport = int(sys.argv[1])  #100
tods = [1,2,3,4,5,6,7,8]
highwayScenarios = [i for i in tods]
transitScenarios = [transitImport + i for i in tods]

#settings
tapFile = "inputs/tap_attributes.csv"
tapLinks = "inputs/tap2node.csv"
tapLinesOutFileName = "outputs/tapLines.csv"

############################################################################

#start EMME desktop and attach a modeller session
desktop = d.start_dedicated(True, "cmap", empFile)
m = m.Modeller(desktop)

############################################################################

#create tap virtual zones
taps = []
with open(tapFile, 'rb') as csvfile:
    tapreader = csv.reader(csvfile, skipinitialspace=True)
    for row in tapreader:
        taps.append(row)
taps_col_names = taps.pop(0)

#create access links
access_links = []
with open(tapLinks, 'rb') as csvfile:
    tapreader = csv.reader(csvfile, skipinitialspace=True)
    for row in tapreader:
        access_links.append(row)
access_links_col_names = access_links.pop(0)

############################################################################

#generates lines by tap file to trim tap set for use in CT-RAMP

#get all transit nodes from EMME
linesByNode = dict()
for tranScen in transitScenarios:

    #get transit segments for scenario
    network = m.emmebank.scenario(tranScen).get_network()
    tsegs = network.transit_segments()

    for tseg in tsegs:
        line = tseg.line.id
        from_node = tseg.i_node.id

        if from_node not in linesByNode:
            linesByNode[from_node] = set()
        linesByNode[from_node].add(line)

        to_node = tseg.j_node
        if to_node != None:
            to_node = tseg.j_node.id
            if to_node not in linesByNode:
                linesByNode[to_node] = set()
            linesByNode[to_node].add(line)

#get nodes connected to each tap
nodesByTap = dict()
for linkRow in access_links:
    tap = linkRow[0]
    node = linkRow[1]

    if tap not in nodesByTap:
        nodesByTap[tap] = set()
    nodesByTap[tap].add(node)

#get lines for each tap
linesByTap = dict()
for tapRow in taps:
    tap = tapRow[0]
    nodes = nodesByTap[tap]

    if tap not in linesByTap:
        linesByTap[tap] = set()
    for node in nodes:
        if node in linesByNode:
            lines = linesByNode[node]
            for line in lines:
                linesByTap[tap].add(line)

#write out tapLines file for CT-RAMP
f = file(tapLinesOutFileName,"wt")
f.write("TAP,LINES\n")
for tap in linesByTap.keys():
    lines = " ".join(list(linesByTap[tap]))
    if lines != "":
        f.write("%s,%s\n" % (tap,lines))
f.close()

#log results
print("TapLines Complete %s" % (datetime.datetime.now()))
