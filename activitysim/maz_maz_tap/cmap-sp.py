
# Calculate MAZ to MAZ and MAZ to TAP shortest paths
# Ben Stabler, ben.stabler@rsginc.com, 07/15/21

import sys
import shapefile
import pandas as pd
import pandana as pdna
import numpy as np
import time
import yaml

################################################################################
# functions from the old setup to get the allstreets network from shapefile
################################################################################

def write_nodes_from_shp(sf):

    shapes = sf.shapes()
    records = sf.records()

    output = open("nodes.csv", "w")
    output.write('"ID","X","Y"\n')

    allPoints = {}
    for shape, record in zip(shapes, records):

        func_class = record.FUNC_CLASS
        referenceNode = record.REF_IN_ID
        nreferenceNode = record.NREF_IN_ID

        ifholder = 1
        if func_class in ["3","4","5"]:
            if ifholder == 1:
                firstPoint = shape.points[0]
                lastPoint = shape.points[-1]
                allPoints[referenceNode] = firstPoint
                allPoints[nreferenceNode] = lastPoint

    for idn, point in allPoints.items():
        output.write("%d,%f,%f\n" % (idn, point[0], point[1]))

    output.close()


def write_links_from_shp(sf):

    records = sf.records()

    output = open("links.csv", "w")
    output.write('"FNODE","TNODE","LENGTH"\n')

    linksDict = {}
    for record in records:

        referenceNode = record.REF_IN_ID
        nreferenceNode = record.NREF_IN_ID
        linkid = record.LINK_ID
        dir_travel = record.DIR_TRAVEL
        func_class = record.FUNC_CLASS
        length = record.SHAPE_LENG

        flag = 0
        try:
            if linksDict[linkid] > []:
                flag = 0

        except KeyError:
            linksDict[linkid] = [referenceNode,nreferenceNode]
            flag = 1

        ifholder = 1
        if func_class in ["3","4","5"]:
            if flag == 1:
                output.write("%d,%d,%f\n" % (referenceNode, nreferenceNode, length))
                output.write("%d,%d,%f\n" % (nreferenceNode, referenceNode, length))

    output.close()

def readYaml(file):
    with open(file, "r") as stream:
        try:
            return(yaml.load(stream, Loader=yaml.SafeLoader))
        except yaml.YAMLError as exc:
            print(exc)
################################################################################
# do stuff
################################################################################

print(time.ctime(), " start")

sp_settings = readYaml("cmap-sp.yaml")


for arg in sys.argv:
  print(time.ctime(), "", arg)

shapefile_name = sp_settings['shapefile_name']
mazfile_name = sp_settings['mazfile_name']
tapfile_name = sp_settings['tapfile_name']
drive_tap_field = sp_settings['drive_tap_field']
max_maz_maz_walk_dist_feet = int(sp_settings['max_maz_maz_walk_dist_feet'])
max_maz_maz_bike_dist_feet = int(sp_settings['max_maz_maz_bike_dist_feet'])
max_maz_tap_walk_dist_feet = int(sp_settings['max_maz_tap_walk_dist_feet'])
max_maz_tap_drive_dist_feet = int(sp_settings['max_maz_tap_drive_dist_feet'])
walk_speed_mph = float(sp_settings["walk_speed_mph"])
drive_speed_mph = float(sp_settings["drive_speed_mph"])

#shapefile_name = sys.argv[1]                   # "Chicago_streets.shp"
#mazfile_name = sys.argv[2]                     # "CMAP_MAZ_cents.txt"
#tapfile_name = sys.argv[3]                     # "tap_attributes.csv"
#drive_tap_field = sys.argv[4]                  # "canpnr"
#max_maz_maz_walk_dist_feet = int(sys.argv[5])  # 5280
#max_maz_maz_bike_dist_feet = int(sys.argv[6])  # 26400
#max_maz_tap_walk_dist_feet = int(sys.argv[7])  # 5280
#max_maz_tap_drive_dist_feet = int(sys.argv[8]) # 105600

print(time.ctime(), " read shapefile and write network")

shp = shapefile.Reader(shapefile_name)
write_nodes_from_shp(shp)
write_links_from_shp(shp)

print(time.ctime(), " read network, mazs, taps")

nodes = pd.read_csv("nodes.csv")
nodes.set_index('ID', inplace= True)

links = pd.read_csv("links.csv")

mazs = pd.read_csv(mazfile_name)
taps = pd.read_csv(tapfile_name)

print(time.ctime(), " construct pandanas network")

net = pdna.Network(nodes["X"], nodes["Y"], links["FNODE"], links["TNODE"], links[["LENGTH"]] / 5280.0, twoway=False)

print(time.ctime(), " assign nearest network node to mazs and taps")

mazs["network_node_id"] = net.get_node_ids(mazs["X"], mazs["Y"])
mazs["network_node_x"] = nodes["X"].loc[mazs["network_node_id"]].tolist()
mazs["network_node_y"] = nodes["Y"].loc[mazs["network_node_id"]].tolist()
taps["network_node_id"] = net.get_node_ids(taps["x_coord"], taps["y_coord"])
taps["network_node_x"] = nodes["X"].loc[taps["network_node_id"]].tolist()
taps["network_node_y"] = nodes["Y"].loc[taps["network_node_id"]].tolist()

print(time.ctime(), " build maz to maz walk table")

o_m = np.repeat(mazs['MAZ_ID_9'].tolist(), len(mazs))
d_m = np.tile(mazs['MAZ_ID_9'].tolist(), len(mazs))
o_m_nn = np.repeat(mazs['network_node_id'].tolist(), len(mazs))
d_m_nn = np.tile(mazs['network_node_id'].tolist(), len(mazs))
o_m_x = np.repeat(mazs['network_node_x'].tolist(), len(mazs))
o_m_y = np.repeat(mazs['network_node_y'].tolist(), len(mazs))
d_m_x = np.tile(mazs['network_node_x'].tolist(), len(mazs))
d_m_y = np.tile(mazs['network_node_y'].tolist(), len(mazs))

maz_to_maz_cost = pd.DataFrame({"OMAZ":o_m, "DMAZ":d_m, "OMAZ_NODE":o_m_nn, "DMAZ_NODE":d_m_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, "DMAZ_NODE_X":d_m_x, "DMAZ_NODE_Y":d_m_y})
maz_to_maz_cost["DISTWALK"] = maz_to_maz_cost.eval("((OMAZ_NODE_X-DMAZ_NODE_X)**2 + (OMAZ_NODE_Y-DMAZ_NODE_Y)**2)**0.5")
maz_to_maz_cost = maz_to_maz_cost[maz_to_maz_cost["OMAZ"] != maz_to_maz_cost["DMAZ"]]

print(time.ctime(), " remove maz maz pairs beyond max walk distance")

maz_to_maz_walk_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0].copy()

print(time.ctime(), " get shortest path length")

maz_to_maz_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_maz_walk_cost["OMAZ_NODE"], maz_to_maz_walk_cost["DMAZ_NODE"])
maz_to_maz_walk_cost = maz_to_maz_walk_cost[maz_to_maz_walk_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0]

print(time.ctime(), " write results")

maz_to_maz_walk_cost[["OMAZ","DMAZ","DISTWALK"]].to_csv(sp_settings["maz_maz_walk_output"], index=False)

print(time.ctime(), " build maz to maz bike table") # same table above

print(time.ctime(), " remove maz maz pairs beyond max bike distance")

maz_to_maz_bike_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_bike_dist_feet / 5280.0].copy()

print(time.ctime(), " get shortest path length")

maz_to_maz_bike_cost["DISTBIKE"] = net.shortest_path_lengths(maz_to_maz_bike_cost["OMAZ_NODE"], maz_to_maz_bike_cost["DMAZ_NODE"])
maz_to_maz_bike_cost = maz_to_maz_bike_cost[maz_to_maz_bike_cost["DISTBIKE"] <= max_maz_maz_bike_dist_feet / 5280.0]

print(time.ctime(), " write results")

maz_to_maz_bike_cost[["OMAZ","DMAZ","DISTBIKE"]].to_csv(sp_settings["maz_maz_bike_output"], index=False)

print(time.ctime(), " build maz to tap walk table")

o_m = np.repeat(mazs['MAZ_ID_9'].tolist(), len(taps))
o_m_nn = np.repeat(mazs['network_node_id'].tolist(), len(taps))
d_t = np.tile(taps['tap_id'].tolist(), len(mazs))
d_t_nn = np.tile(taps['network_node_id'].tolist(), len(mazs))

o_m_x = np.repeat(mazs['network_node_x'].tolist(), len(taps))
o_m_y = np.repeat(mazs['network_node_y'].tolist(), len(taps))
d_t_x = np.tile(taps['network_node_x'].tolist(), len(mazs))
d_t_y = np.tile(taps['network_node_y'].tolist(), len(mazs))

d_t_canpnr = np.tile(taps[drive_tap_field].tolist(), len(mazs))

maz_to_tap_cost = pd.DataFrame({"OMAZ":o_m, "DTAP":d_t, "OMAZ_NODE":o_m_nn, "DTAP_NODE":d_t_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, "DTAP_NODE_X":d_t_x, "DTAP_NODE_Y":d_t_y, "DTAP_CANPNR":d_t_canpnr})
maz_to_tap_cost["DISTANCE"] = maz_to_tap_cost.eval("((OMAZ_NODE_X-DTAP_NODE_X)**2 + (OMAZ_NODE_Y-DTAP_NODE_Y)**2)**0.5")

print(time.ctime(), " remove maz tap pairs beyond max walk distance")

maz_to_tap_walk_cost = maz_to_tap_cost[maz_to_tap_cost["DISTANCE"] <= max_maz_tap_walk_dist_feet / 5280.0].copy()

print(time.ctime(), " get shortest path length")

maz_to_tap_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_tap_walk_cost["OMAZ_NODE"], maz_to_tap_walk_cost["DTAP_NODE"])
maz_to_tap_walk_cost = maz_to_tap_walk_cost[maz_to_tap_walk_cost["DISTWALK"] <= max_maz_tap_walk_dist_feet / 5280.0]
maz_to_tap_walk_cost["walk_time"] = maz_to_tap_walk_cost["DISTWALK"].apply(lambda x: x / walk_speed_mph * 60.0)

print(time.ctime(), " write results")

maz_to_tap_walk_cost[["OMAZ","DTAP","DISTWALK", "walk_time"]].to_csv(sp_settings["maz_tap_walk_output"], index=False)

print(time.ctime(), " build maz to tap drive table")

maz_to_tap_drive_cost = maz_to_tap_cost[maz_to_tap_cost["DTAP_CANPNR"].astype("bool")]

print(time.ctime(), " remove maz tap pairs beyond max distance")

maz_to_tap_drive_cost = maz_to_tap_drive_cost[maz_to_tap_drive_cost["DISTANCE"] <= max_maz_tap_drive_dist_feet / 5280.0]

print(time.ctime(), " get shortest path length")

maz_to_tap_drive_cost["DIST"] = net.shortest_path_lengths(maz_to_tap_drive_cost["OMAZ_NODE"], maz_to_tap_drive_cost["DTAP_NODE"])
maz_to_tap_drive_cost = maz_to_tap_drive_cost[maz_to_tap_drive_cost["DIST"] <= max_maz_tap_drive_dist_feet / 5280.0]

print(time.ctime(), " write results")
maz_to_tap_drive_cost['drive_time'] = maz_to_tap_drive_cost["DIST"].apply(lambda x: x / drive_speed_mph * 60.0)
maz_to_tap_drive_cost[["OMAZ","DTAP","DIST"]].to_csv(sp_settings["maz_tap_drive_output"], index=False)

print(time.ctime(), " finish")
