#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
############################################################################# 

#Should always be placed in the same folder as SPwrapper.py

#This script reads in the navTeq shp file and generates one csv link file and a csv node file. CSVs are stored in ...\SP\int_output

import pdb
from itertools import izip
import shapefile
import math
import parameters
#sf = shapefile.Reader("tl_2010_abm_roads_5c_cleaned")
sf = shapefile.Reader(parameters.navteqShpFile)   #reads in the shapefile
#sf = shapefile.Reader(r"Y:\Projects\11387A-MAG ABM DEVELOPMENT PH 2&3\NavTeq\PAG\PAG_Street Network.shp")
shapes = sf.shapes()
records = sf.records()
fields = sf.fields

#pdb.set_trace()

#pdb.set_trace()
#print "done reading the shapefiles" 
linksDict = {}
#generates links csv file
def getLinkDirectionality(): 
    linksDict = {}
    travel = set() 

    output = open(parameters.linkFile_int, "w") 
    output.write('"FNODE_","TNODE_","LENGTH"\n')
    w = shapefile.Writer(shapefile.POLYLINE)
    #w.field("ID", "N", 10)
    w.field("Start", "N", 10)
    w.field("End", "N", 10)

    for shape, record in izip(shapes, records):
        
        #pdb.set_trace()
        firstPoint = shape.points[0]
        lastPoint = shape.points[-1]
        referenceNode = record[21]
        nreferenceNode = record[22]
        linkid = record[1]
                
        #ref = fields[21]
        #nref = fields[22]
        #pdb.set_trace()
        dir_travel = record[33]
        func_class = record[24]
        ar_pedest = record[45]
        plot_road = record[88]
        speed_cat = record[25]
        flag = 0
        try:
           if linksDict[linkid] > []:
              flag = 0
           
              
        except KeyError:
              linksDict[linkid] = [referenceNode,nreferenceNode]
              flag = 1
              
        #print dir_travel
        #if func_class == "1":
           #print func_class
        #length = float(record[-1])
        length = math.sqrt(((firstPoint[0]-lastPoint[0])*(firstPoint[0]-lastPoint[0])) + ((firstPoint[1]-lastPoint[1])*(firstPoint[1]-lastPoint[1])))
        ifholder = 1
        if func_class == "3" or func_class == "4" or func_class == "5": 
        #if ar_pedest == "Y" and plot_road == "N" and speed_cat != "2" and speed_cat != "3":
           if flag == 1:
              output.write("%d,%d,%f\n" % (referenceNode, nreferenceNode, length))
              output.write("%d,%d,%f\n" % (nreferenceNode, referenceNode, length))
                            
    output.close()
    #w.save("E:\CMAP\cmap_NavTechLinks_no_freeway_pedest_v2")

#############################################################################

#generates nodes csv file
def createAllPoints():
    
    allPoints = {}
    
    #pdb.set_trace()
    
    output = open(parameters.nodeFile_int , "w") 
    output.write('"ID","X","Y"\n')
    for shape, record in izip(shapes, records):
	func_class = record[24]
	ar_pedest = record[45]
	plot_road = record[88]
        speed_cat = record[25]
	fromNode = record[21]
	#print fromNode
        toNode = record[22]
        ifholder = 1
        #print toNode
        if func_class == "3" or func_class == "4" or func_class == "5":
           #if ar_pedest == "Y" and plot_road == "N" and speed_cat != "2" and speed_cat != "3":
           if ifholder == 1:
              firstPoint = shape.points[0]
              lastPoint = shape.points[-1]
              allPoints[fromNode] = firstPoint
              allPoints[toNode] = lastPoint 

    
    #pdb.set_trace()
    
    w = shapefile.Writer(shapefile.POINT)
    w.field("ID")
    for id_, point in allPoints.iteritems():
        w.point(point[0], point[1])
        w.record(id_)
        output.write("%d,%f,%f\n" % (id_, point[0], point[1]))
    
    output.close()
    #w.save(r"E:\CMAP\cmap_NavTechNodes_no_freeway_pedest_v3")
    #output.close()
    #print "done"

createAllPoints()
getLinkDirectionality()

