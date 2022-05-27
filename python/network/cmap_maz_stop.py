# -*- coding: utf-8 -*-
"""
Created on Mon Jan 17 12:06:45 2022

author: Andrew Rohne, Ted Lin
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

class CMapMazStop():
    __MODELLER_NAMESPACE__ = "cmap"
    tool_run_msg = ""
    
    def __init__(self):
        print("Preparing MAZ-MAZ and MAZ-Stop Connectors...")

    def __call__(self, parms):
        print(f"{time.ctime()} Preparing MAZ-MAZ and MAZ-Stop Connectors")
        startTime = time.time()
        asim_inputs = os.environ['ASIM_INPUTS']
        model_inputs = os.environ['INPUT_FOLDER']
        sf = os.path.join(model_inputs, parms['mmms']['shapefile_name'])
        mazfile_name = os.path.join(model_inputs, parms['mazfile_name'])
        
        max_maz_maz_walk_dist_feet = int(parms['mmms']['max_maz_maz_walk_dist_feet'])
        max_maz_maz_bike_dist_feet = int(parms['mmms']['max_maz_maz_bike_dist_feet'])
        max_maz_local_bus_stop_walk_dist_feet = int(parms['mmms']['max_maz_local_bus_stop_walk_dist_feet'])
        max_maz_express_bus_stop_walk_dist_feet = int(parms['mmms']['max_maz_express_bus_stop_walk_dist_feet']) 
        max_maz_cta_rail_stop_walk_dist_feet = int(parms['mmms']['max_maz_cta_rail_stop_walk_dist_feet']) 
        max_maz_metra_rail_stop_walk_dist_feet = int(parms['mmms']['max_maz_metra_rail_stop_walk_dist_feet'])

        max_maz_metra_rail_stop_walk_dist_feet
        walk_speed_mph = float(parms['mmms']["walk_speed_mph"])
        drive_speed_mph = float(parms['mmms']["drive_speed_mph"])
        print(f"{time.ctime()} Getting Nodes...")
        nodes = self.getNodes(gpd.read_file(sf), parms)
        print(f"{time.ctime()} Getting Links...")
        links = self.getLinks(gpd.read_file(sf), parms)
        print(f"{time.ctime()} Building MAZ Centroids...")
        centroids = self.getCentroids(gpd.read_file(mazfile_name), parms)
        print(f"{time.ctime()} Building Network...")
        net = pdna.Network(nodes["X"], nodes["Y"], links["FNODE"], links["TNODE"], links[["LENGTH"]], twoway=False)
        print(f"{time.ctime()} Assign Nearest Network Node to MAZs and stops")
        centroids["network_node_id"] = net.get_node_ids(centroids["X"], centroids["Y"])
        centroids["network_node_x"] = nodes["X"].loc[centroids["network_node_id"]].tolist()
        centroids["network_node_y"] = nodes["Y"].loc[centroids["network_node_id"]].tolist()
        stops = pd.read_csv(os.path.join(model_inputs, parms['stop_attributes']['file']))
        stops["network_node_id"] = net.get_node_ids(stops[parms['stop_attributes']['x_field']], stops[parms['stop_attributes']['y_field']])
        stops["network_node_x"] = nodes["X"].loc[stops["network_node_id"]].tolist()
        stops["network_node_y"] = nodes["Y"].loc[stops["network_node_id"]].tolist()
        # M: Metra rail, C: CTA rail, E: Express bus, L: Local bus, EL: Express bus and local bus, N: None. There should be no Ns
        stops['mode'] = np.where(stops.metra_rail==1,'M',np.where(stops.cta_rail==1,'C',
                        np.where(stops.bus_exp==1,np.where(stops.bus_local==1,'LE','E'),np.where(stops.bus_local==1,'L','N'))))
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
        maz_to_maz_walk_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0].copy()
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_maz_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_maz_walk_cost["OMAZ_NODE"], maz_to_maz_walk_cost["DMAZ_NODE"])
        maz_to_maz_walk_cost_out = maz_to_maz_walk_cost[maz_to_maz_walk_cost["DISTWALK"] <= max_maz_maz_walk_dist_feet / 5280.0]
        missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_maz_walk_cost_out['OMAZ'])]['MAZ']).rename(columns = {'MAZ': 'OMAZ'}).merge(maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']].sort_values('DISTWALK').groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index(), on = 'OMAZ', how = 'left')
        print(f"{time.ctime()} Write Results...")
        maz_to_maz_walk_cost_out[["OMAZ","DMAZ","DISTWALK"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(os.path.join(asim_inputs, parms['mmms']["maz_maz_walk_output"]), index=False)
        del(missing_maz)
        #
        # MAZ-to-MAZ Bike
        #
        print(f"{time.ctime()} Build Maz To Maz Bike Table...") # same table above
        maz_to_maz_bike_cost = maz_to_maz_cost[maz_to_maz_cost["DISTWALK"] <= max_maz_maz_bike_dist_feet / 5280.0].copy()
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_maz_bike_cost["DISTBIKE"] = net.shortest_path_lengths(maz_to_maz_bike_cost["OMAZ_NODE"], maz_to_maz_bike_cost["DMAZ_NODE"])
        maz_to_maz_bike_cost_out = maz_to_maz_bike_cost[maz_to_maz_bike_cost["DISTBIKE"] <= max_maz_maz_bike_dist_feet / 5280.0]
        missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_maz_bike_cost_out['OMAZ'])]['MAZ']).rename(columns = {'MAZ': 'OMAZ'}).merge(maz_to_maz_cost[maz_to_maz_cost['OMAZ'] != maz_to_maz_cost['DMAZ']].sort_values('DISTWALK').groupby('OMAZ').agg({'DMAZ': 'first', 'DISTWALK': 'first'}).reset_index().rename(columns = {'DISTWALK': 'DISTBIKE'}), on = 'OMAZ', how = 'left')
        print(f"{time.ctime()} Write Results...")
        maz_to_maz_bike_cost_out[["OMAZ","DMAZ","DISTBIKE"]].append(missing_maz).sort_values(['OMAZ', 'DMAZ']).to_csv(os.path.join(asim_inputs, parms['mmms']["maz_maz_bike_output"]), index=False)
        del(missing_maz)
        
        #
        # MAZ-to-stop Walk
        #
        print(f"{time.ctime()} Build Maz To stop Walk Table...")
        o_m = np.repeat(centroids['MAZ'].tolist(), len(stops))
        o_m_nn = np.repeat(centroids['network_node_id'].tolist(), len(stops))
        d_t = np.tile(stops[parms['stop_attributes']['id_field']].tolist(), len(centroids))
        d_t_nn = np.tile(stops['network_node_id'].tolist(), len(centroids))
        o_m_x = np.repeat(centroids['network_node_x'].tolist(), len(stops))
        o_m_y = np.repeat(centroids['network_node_y'].tolist(), len(stops))
        d_t_x = np.tile(stops['network_node_x'].tolist(), len(centroids))
        d_t_y = np.tile(stops['network_node_y'].tolist(), len(centroids))
        d_t_canpnr = np.tile(stops[parms['mmms']['drive_stop_field']].tolist(), len(centroids))
        mode = np.tile(stops['mode'].tolist(), len(centroids))
        maz_to_stop_cost = pd.DataFrame({"MAZ":o_m, "stop":d_t, "OMAZ_NODE":o_m_nn, "DSTOP_NODE":d_t_nn, "OMAZ_NODE_X":o_m_x, "OMAZ_NODE_Y":o_m_y, 
                                        "DSTOP_NODE_X":d_t_x, "DSTOP_NODE_Y":d_t_y, "DSTOP_CANPNR":d_t_canpnr, "MODE": mode})
        maz_to_stop_cost["DISTANCE"] = maz_to_stop_cost.eval("(((OMAZ_NODE_X-DSTOP_NODE_X)**2 + (OMAZ_NODE_Y-DSTOP_NODE_Y)**2)**0.5) / 5280.0")
        maz_to_stop_walk_cost = maz_to_stop_cost[(maz_to_stop_cost["DISTANCE"] <= max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'L') | 
                                                    (maz_to_stop_cost["DISTANCE"] <= max_maz_express_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'E') | 
                                                    (maz_to_stop_cost["DISTANCE"] <= max_maz_express_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'LE') | 
                                                    (maz_to_stop_cost["DISTANCE"] <= max_maz_cta_rail_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'C') | 
                                                    (maz_to_stop_cost["DISTANCE"] <= max_maz_metra_rail_stop_walk_dist_feet / 5280.0) & (maz_to_stop_cost['MODE'] == 'M')].copy()
        print(f"{time.ctime()} Get Shortest Path Length...")
        maz_to_stop_walk_cost["DISTWALK"] = net.shortest_path_lengths(maz_to_stop_walk_cost["OMAZ_NODE"], maz_to_stop_walk_cost["DSTOP_NODE"])
        print(f"{time.ctime()} Remove Maz Stop Pairs Beyond Max Walk Distance...")
        #maz_to_stop_walk_cost.to_csv(os.path.join(asim_inputs, "maz_to_stop_walk_cost.csv"), index=False)
        
        #maz_to_stop_walk_cost = pd.read_csv(os.path.join(asim_inputs, "maz_to_stop_walk_cost.csv"))
        maz_to_stop_walk_cost_out = maz_to_stop_walk_cost[(maz_to_stop_walk_cost["DISTANCE"] <= max_maz_local_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'L') | 
                                                            (maz_to_stop_walk_cost["DISTANCE"] <= max_maz_express_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'E') | 
                                                            (maz_to_stop_walk_cost["DISTANCE"] <= max_maz_express_bus_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'LE') | 
                                                            (maz_to_stop_walk_cost["DISTANCE"] <= max_maz_cta_rail_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'C') | 
                                                            (maz_to_stop_walk_cost["DISTANCE"] <= max_maz_metra_rail_stop_walk_dist_feet / 5280.0) & (maz_to_stop_walk_cost['MODE'] == 'M')].copy()
        land_use = pd.read_csv(os.path.join(model_inputs, parms['land_use']['file']))
        modes = {"L": "local_bus", "E": "express_bus", "C": "cta_rail", "M": "metra_rail"}
        for mode, output in modes.items():
            max_walk_dist = parms['mmms']['max_maz_' + output + '_stop_walk_dist_feet'] / 5280.0
            maz_to_stop_walk_cost_out_mode = maz_to_stop_walk_cost_out[maz_to_stop_walk_cost_out['MODE'].str.contains(mode)].copy()
            maz_to_stop_walk_cost_out_mode.loc[:, 'MODE'] = mode
            # in case straight line distance is less than max and actual distance is greater than max (e.g., street net), set actual distance to max
            maz_to_stop_walk_cost_out_mode['DISTWALK'] = maz_to_stop_walk_cost_out_mode['DISTWALK'].clip(upper=max_walk_dist)
            #maz_to_stop_walk_cost_out_mode.to_csv(os.path.join(asim_inputs, "maz_to_stop_walk_cost_out_%s.csv"%mode), index=False)
            missing_maz = pd.DataFrame(centroids[~centroids['MAZ'].isin(maz_to_stop_walk_cost_out_mode['MAZ'])]['MAZ']).merge(maz_to_stop_cost.sort_values('DISTANCE').groupby(['MAZ', 'MODE']).agg({'stop': 'first', 'DISTANCE': 'first'}).reset_index(), on = 'MAZ', how = 'left')            
            #missing_maz.to_csv(os.path.join(asim_inputs, "missing_maz_%s.csv"%mode), index=False)
            #missing_maz = pd.read_csv(os.path.join(asim_inputs, "missing_maz_%s.csv"%mode))
            maz_to_stop_walk_cost = maz_to_stop_walk_cost_out_mode.append(missing_maz.rename(columns = {'DISTANCE': 'DISTWALK'})).sort_values(['MAZ', 'stop'])            
            #maz_to_stop_walk_cost.to_csv(os.path.join(asim_inputs, "maz_to_stop_walk_cost_%s.csv"%mode), index=False)
            del(maz_to_stop_walk_cost_out_mode)
            del(missing_maz)            
            maz_stop_walk = maz_to_stop_walk_cost[maz_to_stop_walk_cost.MODE==mode].groupby('MAZ')['DISTWALK'].min().reset_index()            
            maz_stop_walk.loc[maz_stop_walk['DISTWALK'] > max_walk_dist, 'DISTWALK'] = np.nan
            #maz_stop_walk["walk_time"] = maz_stop_walk["DISTWALK"].apply(lambda x: x / parms['mmms']['walk_speed_mph'] * 60.0)                      
            maz_stop_walk['DISTWALK'].fillna(9999, inplace = True)
            maz_stop_walk.rename({'MAZ': 'maz', 'DISTWALK': 'walk_dist_' + output}, axis='columns', inplace=True)
            land_use = land_use.merge(maz_stop_walk[['maz', 'walk_dist_' + output]], how='left', on='maz')

        print(f"{time.ctime()} Write Results...")
        land_use.to_csv(os.path.join(asim_inputs, parms['land_use']['file']), index=False)
        print(f"{time.ctime()} Completed MAZ-MAZ and MAZ-stop Connectors in {time.strftime('%H:%M:%S', time.gmtime(time.time() - startTime))}")

        
    def getNodes(self, sf, parms):
        output = {}
        outputF = sf[sf[parms['mmms']["mmms_node_fc"]].isin(["3", "4", "5"])].copy()
        outputF['X'] = outputF['geometry'].apply(lambda x: x.coords[0][0])
        outputF['Y'] = outputF['geometry'].apply(lambda x: x.coords[0][1])
        outputF['X1'] = outputF['geometry'].apply(lambda x: x.coords[-1][0])
        outputF['Y1'] = outputF['geometry'].apply(lambda x: x.coords[-1][1])
        
        for i, r in outputF.iterrows():
            output[r[parms['mmms']["mmms_link_ref_id"]]] = [r['X'], r['Y']]
            output[r[parms['mmms']["mmms_link_nref_id"]]] = [r['X1'], r['Y1']]
            
        return(pd.DataFrame().from_dict(output, orient = 'index', columns = ['X', 'Y']))
        
    def getLinks(self, sf, parms):
        linksDict = {}
        outputD = []
        for idx, record in sf.iterrows():
            referenceNode = record[parms['mmms']["mmms_link_ref_id"]]
            nreferenceNode = record[parms['mmms']["mmms_link_nref_id"]]
            linkid = record[parms['mmms']["mmms_link_id"]]
            dir_travel = record[parms['mmms']["mmms_link_dirtravel"]]
            func_class = record[parms['mmms']["mmms_link_fc"]]
            length = record[parms['mmms']["mmms_link_len"]] / 5280.0
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
        

        