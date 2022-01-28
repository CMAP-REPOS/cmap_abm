# -*- coding: utf-8 -*-
"""
Created on Mon Jan 17 12:06:45 2022

@author: andrew.rohne
"""

import sys
import pandas as pd
import pandana as pdna
import numpy as np
import time
import yaml
import os
import geopandas as gpd
import time

class CMapMazTap():
    __MODELLER_NAMESPACE__ = "cmap"
    tool_run_msg = ""
    
    def __init__(self):
        print("Preparing MAZ-MAZ-TAP Connectors...")

    def __call__(self, parms):
        print(f"{time.ctime()} Preparing MAZ-MAZ-TAP Connectors")
        startTime = time.time()
        asim_inputs = os.environ['ASIM_INPUTS']
        model_inputs = os.environ['INPUT_FOLDER']
        sf = os.path.join(model_inputs, parms['mmt']['shapefile_name'])
        mazfile_name = os.path.join(model_inputs, parms['mazfile_name'])
        tapfile_name = os.path.join(model_inputs, parms['tap_attributes']['file'])
        drive_tap_field = parms['mmt']['drive_tap_field']
        
        max_maz_maz_walk_dist_feet = int(parms['mmt']['max_maz_maz_walk_dist_feet'])
        max_maz_maz_bike_dist_feet = int(parms['mmt']['max_maz_maz_bike_dist_feet'])
        max_maz_tap_walk_dist_feet = int(parms['mmt']['max_maz_tap_walk_dist_feet'])
        max_maz_tap_drive_dist_feet = int(parms['mmt']['max_maz_tap_drive_dist_feet'])
        walk_speed_mph = float(parms['mmt']["walk_speed_mph"])
        drive_speed_mph = float(parms['mmt']["drive_speed_mph"])
        print(f"{time.ctime()} Getting Nodes...")
        nodes = self.getNodes(gpd.read_file(sf), parms)
        print(f"{time.ctime()} Getting Links...")
        links = self.getLinks(gpd.read_file(sf), parms)
        print(f"{time.ctime()} Building MAZ Centroids...")
        centroids = self.getCentroids(gpd.read_file(mazfile_name), parms)
        print(f"{time.ctime()} Building Network...")
        net = pdna.Network(nodes["X"], nodes["Y"], links["FNODE"], links["TNODE"], links[["LENGTH"]], twoway=False)
        print(f"{time.ctime()} Assign Nearest Network Node to MAZs and TAPs")
        centroids["network_node_id"] = net.get_node_ids(centroids["X"], centroids["Y"])
        centroids["network_node_x"] = nodes["X"].loc[centroids["network_node_id"]].tolist()
        centroids["network_node_y"] = nodes["Y"].loc[centroids["network_node_id"]].tolist()
        taps = pd.read_csv(os.path.join(model_inputs, parms['tap_attributes']['file']))
        taps["network_node_id"] = net.get_node_ids(taps[parms['tap_attributes']['x_field']], taps[parms['tap_attributes']['y_field']])
        taps["network_node_x"] = nodes["X"].loc[taps["network_node_id"]].tolist()
        taps["network_node_y"] = nodes["Y"].loc[taps["network_node_id"]].tolist()
        print(f"{time.ctime()} Build MAZ to MAZ Walk Table...")
        o_m = np.repeat(centroids['MAZ'].tolist(), len(centroids))
        d_m = np.tile(centroids['MAZ'].tolist(), len(centroids))
        o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(centroids))
        d_m_nn = np.tile(centroids['network_node_id'].tolist(), len(centroids))
        o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(centroids))
        o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(centroids))
        d_m_x = np.tile(centroids['network_node_x'].tolist(), len(centroids))
        d_m_y = np.tile(centroids['network_node_y'].tolist(), len(centroids))
        #
        # MAZ-to-MAZ Walk
        #
        maz_to_maz_cost = pd.DataFrame({"OMAZ":o_m, "DMAZ":d_m, "OMAZ_NODE":o_m_nn, "DMAZ_NODE":d_m_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, "DMAZ_NODE_X":d_m_x, "DMAZ_NODE_Y":d_m_y})
        maz_to_maz_cost["DISTWALK"] = maz_to_maz_cost.eval("(((OMAZ_NODE_X-DMAZ_NODE_X)**2 + (OMAZ_NODE_Y-DMAZ_NODE_Y)**2)**0.5) / 5280.0")
        maz_to_maz_cost = maz_to_maz_cost[maz_to_maz_cost["OMAZ"] != maz_to_maz_cost["DMAZ"]]
        
        print(f"{time.ctime()} Remove MAZ to MAZ Pairs Beyond Max Walk Distance...")
        maz_to_maz_walk_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= parms['mmt']['max_maz_maz_walk_dist_feet'] / 5280.0].copy()
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_maz_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_maz_walk_cost["OMAZ_NODE"], maz_to_maz_walk_cost["DMAZ_NODE"])
        maz_to_maz_walk_cost_out = maz_to_maz_walk_cost[maz_to_maz_walk_cost["DISTWALK"] <= parms['mmt']['max_maz_maz_walk_dist_feet'] / 5280.0]
        missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_maz_walk_cost_out['OMAZ'])]['MAZ']).rename(columns = {'MAZ': 'OMAZ'}).merge(maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']].sort_values('DISTWALK').groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index(), on = 'OMAZ', how = 'left')
        print(f"{time.ctime()} Write Results...")
        maz_to_maz_walk_cost_out[["OMAZ","DMAZ","DISTWALK"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(os.path.join(asim_inputs, parms['mmt']["maz_maz_walk_output"]), index=False)
        del(missing_maz)
        #
        # MAZ-to-MAZ Bike
        #
        print(f"{time.ctime()} Build Maz To Maz Bike Table...") # same table above
        maz_to_maz_bike_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= parms['mmt']['max_maz_maz_bike_dist_feet'] / 5280.0].copy()
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_maz_bike_cost["DISTBIKE"] = net.shortest_path_lengths(maz_to_maz_bike_cost["OMAZ_NODE"], maz_to_maz_bike_cost["DMAZ_NODE"])
        maz_to_maz_bike_cost_out = maz_to_maz_bike_cost[maz_to_maz_bike_cost["DISTBIKE"] <= parms['mmt']['max_maz_maz_bike_dist_feet'] / 5280.0]
        missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_maz_bike_cost_out['OMAZ'])]['MAZ']).rename(columns = {'MAZ': 'OMAZ'}).merge(maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']].sort_values('DISTWALK').groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index().rename(columns = {'DISTWALK': 'DISTBIKE'}), on = 'OMAZ', how = 'left')
        print(f"{time.ctime()} Write Results...")
        maz_to_maz_bike_cost_out[["OMAZ","DMAZ","DISTBIKE"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(os.path.join(asim_inputs, parms['mmt']["maz_maz_bike_output"]), index=False)
        del(missing_maz)
        
        #
        # MAZ-to-TAP Walk
        #
        print(f"{time.ctime()} Build Maz To Tap Walk Table...")
        o_m = np.repeat(centroids['MAZ'].tolist(), len(taps))
        o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(taps))
        d_t = np.tile(taps[parms['tap_attributes']['id_field']].tolist(), len(centroids))
        d_t_nn = np.tile(taps['network_node_id'].tolist(), len(centroids))
        o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(taps))
        o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(taps))
        d_t_x = np.tile(taps['network_node_x'].tolist(), len(centroids))
        d_t_y = np.tile(taps['network_node_y'].tolist(), len(centroids))
        d_t_canpnr = np.tile(taps[parms['mmt']['drive_tap_field']].tolist(), len(centroids))
        maz_to_tap_cost = pd.DataFrame({"MAZ":o_m, "TAP":d_t, "OMAZ_NODE":o_m_nn, "DTAP_NODE":d_t_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, "DTAP_NODE_X":d_t_x, "DTAP_NODE_Y":d_t_y, "DTAP_CANPNR":d_t_canpnr})
        maz_to_tap_cost["DISTANCE"] = maz_to_tap_cost.eval("(((OMAZ_NODE_X-DTAP_NODE_X)**2 + (OMAZ_NODE_Y-DTAP_NODE_Y)**2)**0.5) / 5280.0")
        maz_to_tap_walk_cost = maz_to_tap_cost[maz_to_tap_cost["DISTANCE"] <= parms['mmt']['max_maz_tap_walk_dist_feet'] / 5280.0].copy()
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_tap_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_tap_walk_cost["OMAZ_NODE"], maz_to_tap_walk_cost["DTAP_NODE"])
        print(f"{time.ctime()} Remove Maz Tap Pairs Beyond Max Walk Distance...")
        maz_to_tap_walk_cost_out = maz_to_tap_walk_cost[maz_to_tap_walk_cost["DISTWALK"] <= parms['mmt']['max_maz_tap_walk_dist_feet'] / 5280.0].copy()
        missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_tap_walk_cost_out['MAZ'])]['MAZ']).merge(maz_to_tap_cost.sort_values('DISTANCE').groupby('MAZ').agg({'TAP': 'first', 'DISTANCE': 'first'}).reset_index(), on = 'MAZ', how = 'left')
        maz_to_tap_walk_cost = maz_to_tap_walk_cost_out.append(missing_maz.rename(columns = {'DISTANCE': 'DISTWALK'})).sort_values(['MAZ', 'TAP'])
        del(missing_maz)
        maz_to_tap_walk_cost["walk_time"] = maz_to_tap_walk_cost["DISTWALK"].apply(lambda x: x / parms['mmt']['walk_speed_mph'] * 60.0)
        print(f"{time.ctime()} Write Results...")
        maz_to_tap_walk_cost[["MAZ","TAP","DISTWALK", "walk_time"]].to_csv(os.path.join(asim_inputs, parms['mmt']["maz_tap_walk_output"]), index=False)

        #
        # MAZ-to-TAP Drive
        #
        print(f"{time.ctime()} Build Maz To Tap Drive Table...")
        maz_to_tap_drive_cost = maz_to_tap_cost[maz_to_tap_cost["DTAP_CANPNR"].astype("bool")]
        print(f"{time.ctime()} Remove Maz Tap Pairs Beyond Max Distance...")
        maz_to_tap_drive_cost = maz_to_tap_drive_cost[maz_to_tap_drive_cost["DISTANCE"] <= parms['mmt']['max_maz_tap_drive_dist_feet'] / 5280.0]
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_tap_drive_cost["DIST"] = net.shortest_path_lengths(maz_to_tap_drive_cost["OMAZ_NODE"], maz_to_tap_drive_cost["DTAP_NODE"])
        maz_to_tap_drive_cost = maz_to_tap_drive_cost[maz_to_tap_drive_cost["DIST"] <= parms['mmt']['max_maz_tap_drive_dist_feet'] / 5280.0]
        maz_to_tap_drive_cost['drive_time'] = maz_to_tap_drive_cost["DIST"].apply(lambda x: x / parms['mmt']['drive_speed_mph'] * 60.0)
        print(f"{time.ctime()} Write Results...")
        if 'tap_parkcost_field' in parms['mmt'].keys():
            tapPCost = dict(zip(taps['tap_id'], taps[parms['mmt']['tap_parkcost_field']]/100.0))
            maz_to_tap_drive_cost['PCOST'] = maz_to_tap_drive_cost['TAP'].map(tapPCost)
            maz_to_tap_drive_cost[["MAZ","TAP","DIST", "drive_time", "PCOST"]].to_csv(os.path.join(asim_inputs, parms['mmt']["maz_tap_drive_output"]), index=False)
        else:
            maz_to_tap_drive_cost[["MAZ","TAP","DIST", "drive_time"]].to_csv(os.path.join(asim_inputs, parms['mmt']["maz_tap_drive_output"]), index=False)
            
        print(f"{time.ctime()} Completed MAZ-MAZ-TAP Connectors in {time.strftime('%H:%M:%S', time.gmtime(time.time() - startTime))}")

        
    def getNodes(self, sf, parms):
        output = {}
        outputF = sf[sf[parms['mmt']["mmt_node_fc"]].isin(["3", "4", "5"])].copy()
        outputF['X'] = outputF['geometry'].apply(lambda x: x.coords[0][0])
        outputF['Y'] = outputF['geometry'].apply(lambda x: x.coords[0][1])
        outputF['X1'] = outputF['geometry'].apply(lambda x: x.coords[-1][0])
        outputF['Y1'] = outputF['geometry'].apply(lambda x: x.coords[-1][1])
        
        for i, r in outputF.iterrows():
            output[r[parms['mmt']["mmt_link_ref_id"]]] = [r['X'], r['Y']]
            output[r[parms['mmt']["mmt_link_nref_id"]]] = [r['X1'], r['Y1']]
            
        return(pd.DataFrame().from_dict(output, orient = 'index', columns = ['X', 'Y']))
        
    def getLinks(self, sf, parms):
        linksDict = {}
        outputD = []
        for idx, record in sf.iterrows():
            referenceNode = record[parms['mmt']["mmt_link_ref_id"]]
            nreferenceNode = record[parms['mmt']["mmt_link_nref_id"]]
            linkid = record[parms['mmt']["mmt_link_id"]]
            dir_travel = record[parms['mmt']["mmt_link_dirtravel"]]
            func_class = record[parms['mmt']["mmt_link_fc"]]
            length = record[parms['mmt']["mmt_link_len"]] / 5280.0
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
                    outputD.append({'FNODE': referenceNode, 'TNODE': nreferenceNode, 'LENGTH': length})
                    outputD.append({'FNODE': nreferenceNode, 'TNODE': referenceNode, 'LENGTH': length})
        output = pd.DataFrame(outputD)
        return(output)
        
    def getCentroids(self, sf, parms):
        outputD = []
        for idx, r in sf.iterrows():
            centroid = np.array(r.geometry.centroid.coords.xy)
            outputD.append({'MAZ': r[parms['maz_shape_maz_id']], 'X': centroid[0][0], 'Y': centroid[1][0]})
        output = pd.DataFrame(outputD)
        return(output)
        

        