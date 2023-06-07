import numpy as np
import pandas as pd
import openpyxl
import datetime
import os
import matplotlib.pyplot as plt
import seaborn as sns

sns.set_theme()
start_time = datetime.datetime.now()
# ABM Directory
abm_dir = os.environ["BASE_PATH"]
## Assignment Output Directory
board_dir = abm_dir + r"/emme_outputs"
### Count Data Directory
count_data_loc = abm_dir + r"/survey_data_prep/Visualizer/data"
### Summary Output Directory
summary_dir = abm_dir + r"/survey_data_prep/Visualizer/outputs"
### Subzone Lookup
subzone_lookup_loc = abm_dir + r"/activitysim_inputs/SubzoneData.csv"
### Assigned Volume
hwy_vol_loc = count_data_loc + r"/build/hnetcnt.csv"
### Observed VMT Data
obs_vmt_loc = count_data_loc + r"/counts/vmt_county_func_class.csv"
obs_vmt_IL_loc = count_data_loc + r"/counts/vmt_county.csv"
### Node Lookup
node_lookup_loc = count_data_loc + r"/base/node_zone_lu.csv"
### Screenline and Corridor Data
screenlines_loc = count_data_loc + r"/counts/screenlines.csv"
corridors_loc = count_data_loc + r"/counts/corridors.csv"
### Observed boardings info
metra_obs_loc = count_data_loc + r"/counts/metra_boarding_alighting.csv"
### Boardings and Trips Data
model_trips_loc = board_dir + "/trn_demand_iter2.csv"
model_boardings_loc = board_dir + "/trn_boardings_iter2.csv"
### County Name Dictionary
county = {17007: 'Boone',
          17031: 'Cook',
          17037: 'DeKalb',
          17043: 'DuPage',
          17063: 'Grundy',
          17089: 'Kane',
          17091: 'Kankakee',
          17093: 'Kendall',
          55059: 'Kenosha',
          17097: 'Lake_IL',
          18089: 'Lake_IN',
          18091: 'LaPorte',
          17099: 'LaSalle',
          17103: 'Lee',
          17111: 'McHenry',
          17141: 'Ogle',
          18127: 'Porter',
          55101: 'Racine',
          55127: 'Walworth',
          17197: 'Will',
          17201: 'Winnebago'}
### Read data
hwy_vol = pd.read_csv(hwy_vol_loc)
obs_vmt = pd.read_csv(obs_vmt_loc)
obs_vmt_IL = pd.read_csv(obs_vmt_IL_loc)
node_lookup = pd.read_csv(node_lookup_loc)
subzone_lookup = pd.read_csv(subzone_lookup_loc)
screenlines = pd.read_csv(screenlines_loc)
corridors = pd.read_csv(corridors_loc)
processed_counts = pd.read_csv(hwy_vol_loc)
metra_obs = pd.read_csv(metra_obs_loc)
model_trips = pd.read_csv(model_trips_loc)
model_boardings = pd.read_csv(model_boardings_loc)
### Add zone
hwy_vol = pd.merge(hwy_vol, 
                   node_lookup, 
                   left_on='INODE', 
                   right_on='NODE', 
                   how='left', 
                   suffixes=('', '_x'))
hwy_vol.rename({'@zone': 'zone'}, axis=1, inplace=True)
hwy_vol.drop(['NODE'], axis=1, inplace=True)
### Create zone lookup from subzone lookup
zone_lookup = pd.pivot_table(subzone_lookup, 
                             index='zone', 
                             values='county', 
                             aggfunc=np.unique).reset_index()
### Add county
hwy_vol = pd.merge(hwy_vol, 
                   zone_lookup, 
                   on='zone', 
                   how='left', 
                   suffixes=('', '_x'))
hwy_vol['county'] = hwy_vol.county.map(county)
hwy_vol.rename({'county': 'County'}, axis=1, inplace=True)
### Find Chicago Region from zones
hwy_vol['Region'] = hwy_vol['County']
hwy_vol.loc[hwy_vol['zone']<=717, 'Region'] = 'Chicago'
hwy_vol.loc[hwy_vol['Region']=='Cook', 'Region'] = 'Cook (excluding Chicago)'
### Remove links with no counts for VMT table
hwy_vol_no_cnt = hwy_vol[(hwy_vol['DY_CNT']>0)&(~hwy_vol['County'].isna())]
hwy_vol_pv_cnt = hwy_vol[(hwy_vol['DY_CNT_PV']>0)&(~hwy_vol['County'].isna())]

### Table - Counts by county/func class
### Model Crosstab
model_counts = pd.crosstab(index=hwy_vol_pv_cnt.Region, 
                           columns=hwy_vol_pv_cnt.FTYPE, 
                           values=hwy_vol_pv_cnt.DY_ASN_PV, 
                           aggfunc=np.sum, 
                           margins=True, margins_name='Total').round().fillna(0).astype(int)
### Observed Crosstab
obs_counts = pd.crosstab(index=hwy_vol_pv_cnt.Region, 
                         columns=hwy_vol_pv_cnt.FTYPE, 
                         values=hwy_vol_pv_cnt.DY_CNT_PV, 
                         aggfunc=np.sum, 
                         dropna=True,
                         margins=True, margins_name='Total').round().fillna(0).astype(int)

### Table - VMT on all links by county/func class
### Rename Lake county in obs data
obs_vmt.loc[obs_vmt['Region']=='Lake', 'Region'] = 'Lake_IL'
obs_vmt.loc[obs_vmt['Region']=='Sub Cook', 'Region'] = 'Cook (excluding Chicago)'
### Set index for observed vmt
obs_vmt.set_index('Region', inplace=True)
### Restructure observed vmt
obs_vmt['Freeway'] = obs_vmt['Interstate'] + obs_vmt['Freeway']
obs_vmt['Arterial'] = obs_vmt['Other Princ Arterials'] + obs_vmt['Minor Arterials'] + obs_vmt['Major Collectors'] + obs_vmt['Minor Collectors']
obs_vmt['Local'] = obs_vmt['Local Roads/Streets']
obs_vmt = obs_vmt[['Arterial', 'Freeway', 'Local']]
obs_vmt['Total'] = obs_vmt[['Arterial', 'Freeway', 'Local']].sum(axis=1)
### Model Crosstab
model_vmt = pd.crosstab(index=hwy_vol.Region, 
                       columns=hwy_vol.FTYPE, 
                       values=hwy_vol.DY_ASN*hwy_vol.LENGTH, 
                       aggfunc=np.sum, 
                       margins=True, margins_name='Total').round().fillna(0).astype(int)
### Only keep regions/counties from observed data
model_vmt = model_vmt[model_vmt.index.isin(list(obs_vmt.index))]
model_vmt.loc['Total'] = model_vmt.iloc[:-1].sum(axis=0)

### Table - VMT on links with counts by county/func class
### Model Crosstab
model_vmt_no_cnt = pd.crosstab(index=hwy_vol_pv_cnt.Region, 
                               columns=hwy_vol_pv_cnt.FTYPE, 
                               values=hwy_vol_pv_cnt.DY_ASN*hwy_vol_pv_cnt.LENGTH, 
                               aggfunc=np.sum, 
                               margins=True, margins_name='Total').round().fillna(0).astype(int)
### Model Crosstab
obs_vmt_no_cnt = pd.crosstab(index=hwy_vol_pv_cnt.Region, 
                             columns=hwy_vol_pv_cnt.FTYPE, 
                             values=hwy_vol_pv_cnt.DY_CNT*hwy_vol_pv_cnt.LENGTH, 
                             aggfunc=np.sum, 
                             margins=True, margins_name='Total').round().fillna(0).astype(int)

### Table - VMT on links in Illinois by county
### Rename Lake county in obs data
obs_vmt_IL.loc[obs_vmt_IL['County']=='Lake', 'County'] = 'Lake_IL'
### Set index for observed vmt
obs_vmt_IL.set_index('County', inplace=True)
obs_vmt_IL
### Model Crosstab
model_vmt_IL = pd.crosstab(index=hwy_vol.County, 
                           columns=hwy_vol.FTYPE, 
                           values=hwy_vol.DY_ASN*hwy_vol.LENGTH, 
                           aggfunc=np.sum, 
                           margins=True, margins_name='Total').round().fillna(0).astype(int)
### Only keep regions/counties from observed data
model_vmt_IL = model_vmt_IL[['Total']]
model_vmt_IL = model_vmt_IL.loc[list(obs_vmt_IL.index)]
model_vmt_IL.loc['Total'] = model_vmt_IL.iloc[:-1].sum(axis=0)

### Table - Counts by TOD/veh class
### Model table
hwy_vol_no_tp_cnt = hwy_vol_no_cnt[hwy_vol_no_cnt['NT_CNT']>0]
tod_list = ['NT', 'EA', 'AM', 'MM', 'MD', 'AF', 'PM', 'EV', 'Total']
veh_dic = {'PV': 'Passenger Vehicle and Light Truck', 'MH_TRK': 'Medium to Heavy Truck'}
model_counts_tod_veh = pd.DataFrame({'TOD': tod_list, 
                                     'Passenger Vehicle and Light Truck': [None]*len(tod_list), 
                                     'Medium to Heavy Truck': [None]*len(tod_list), 
                                     'Total': [None]*len(tod_list)})
model_counts_tod_veh.set_index('TOD', inplace=True)
for veh in veh_dic.keys():
    for tp in tod_list[:-1]:
        model_counts_tod_veh.loc[tp, veh_dic[veh]] = hwy_vol_no_tp_cnt[tp+'_ASN_'+veh].sum()
model_counts_tod_veh.loc['Total'] = model_counts_tod_veh.iloc[:-1].sum(axis=0)
model_counts_tod_veh['Total'] = model_counts_tod_veh[['Passenger Vehicle and Light Truck', 'Medium to Heavy Truck']].sum(axis=1)
model_counts_tod_veh = model_counts_tod_veh.astype(float).round().fillna(0).astype(int)
### Observed table
obs_counts_tod_veh = pd.DataFrame({'TOD': tod_list, 
                                   'Passenger Vehicle and Light Truck': [None]*len(tod_list), 
                                   'Medium to Heavy Truck': [None]*len(tod_list), 
                                   'Total': [None]*len(tod_list)})
obs_counts_tod_veh.set_index('TOD', inplace=True)
for veh in veh_dic.keys():
    for tp in tod_list[:-1]:
        obs_counts_tod_veh.loc[tp, veh_dic[veh]] = hwy_vol_no_tp_cnt[tp+'_CNT_'+veh].sum()
obs_counts_tod_veh.loc['Total'] = obs_counts_tod_veh.iloc[:-1].sum(axis=0)
obs_counts_tod_veh['Total'] = obs_counts_tod_veh[['Passenger Vehicle and Light Truck', 'Medium to Heavy Truck']].sum(axis=1)
obs_counts_tod_veh = obs_counts_tod_veh.astype(float).round().fillna(0).astype(int)

### Table - Counts by Veh class
### Model table
veh_dic_model = {'DY_ASN_PV': 'Passenger Vehicle and Light Truck', 'DY_ASN_MH_TRK': 'Medium to Heavy Truck', 'DY_ASN': 'Total'}
model_counts_veh = hwy_vol_no_cnt[veh_dic_model.keys()].sum(axis=0).to_frame().rename({0: 'Total'}, axis=1).round().astype(int)
model_counts_veh['Vehicle Class'] = veh_dic_model.values()
model_counts_veh.set_index('Vehicle Class', inplace=True)
### Observed table
veh_dic_obs = {'DY_CNT_PV': 'Passenger Vehicle and Light Truck', 'DY_CNT_MH_TRK': 'Medium to Heavy Truck', 'DY_CNT': 'Total'}
obs_counts_veh = hwy_vol_no_cnt[veh_dic_obs.keys()].sum(axis=0).to_frame().rename({0: 'Total'}, axis=1).round().astype(int)
obs_counts_veh['Vehicle Class'] = veh_dic_obs.values()
obs_counts_veh.set_index('Vehicle Class', inplace=True)

### Table - Screenlines Volume and Corridor VMT Summaries
### Merge counts to screenlines
screenlines = pd.merge(screenlines, 
                       processed_counts[['INODE', 'JNODE', 'DY_ASN', 'DY_CNT']], 
                       left_on=['Anode', 'Bnode'], 
                       right_on=['INODE', 'JNODE'], 
                       how='left')
### Remove screenlines without observed counts
screenlines = screenlines[screenlines['DY_CNT']>0]
### Summarize and save data
volume_summary = pd.pivot_table(screenlines, 
                             values=['DY_ASN', 'DY_CNT'], 
                             index=['X_ID'], 
                             aggfunc=np.sum, 
                             fill_value=0).round(0).reset_index()
volume_summary = volume_summary.rename({'DY_ASN': 'Modeled', 'DY_CNT': 'Observed'}, axis=1)
volume_summary['Diff'] = volume_summary['Modeled']-volume_summary['Observed']
volume_summary['Percent Diff'] = volume_summary['Diff']/volume_summary['Observed']*100
volume_summary['Percent Diff'] = volume_summary['Percent Diff'].round(2)
### Merge counts to screenlines
corridors = pd.merge(corridors, 
                       processed_counts[['INODE', 'JNODE', 'LENGTH', 'DY_ASN', 'DY_CNT']], 
                       left_on=['Anode', 'Bnode'], 
                       right_on=['INODE', 'JNODE'], 
                       how='left')
### Remove screenlines without observed counts
corridors = corridors[corridors['DY_CNT']>0]
corridors['VMT_Modeled'] = corridors['LENGTH']*corridors['DY_ASN']
corridors['VMT_Observed'] = corridors['LENGTH']*corridors['DY_CNT']
### Summarize and save data
vmt_summary = pd.pivot_table(corridors, 
                             values=['VMT_Modeled', 'VMT_Observed'], 
                             index=['RDNM_TOLL'], 
                             aggfunc=np.sum, 
                             fill_value=0).round(0).reset_index()
# vmt_summary = vmt_summary.rename({'ID': 'Corridor_ID'}, axis=1)
vmt_summary['Diff'] = vmt_summary['VMT_Modeled']-vmt_summary['VMT_Observed']
vmt_summary['Percent Diff'] = vmt_summary['Diff']/vmt_summary['VMT_Observed']*100
vmt_summary['Percent Diff'] = vmt_summary['Percent Diff'].round(2)

### Table - Transit Boarding Summary by Operator and Type
### Time_Period Factor - factor to obtain boardings in one particular period from daily boardings
tp_fac = {'AM': 0.25, 
          'MD': 0.4, 
          'PM': 0.25, 
          'NT': 0.1}
tp = "AM"
boards = pd.read_csv(board_dir+"/boardings_by_line_"+tp+"_iter2.csv", sep="\t")
boards['time_period'] = tp
for tp in ["MD", "NT", "PM"]:
    df = pd.read_csv(board_dir+"/boardings_by_line_"+tp+"_iter2.csv", sep="\t")
    df['time_period'] = tp
    boards = pd.concat([boards, df])
boards = boards.drop(['c '], axis=1)
### Metra Rail Data
metra_boards = boards[boards['mode']=='M'].reset_index(drop=True)
metra_boards[['1', '2']] = metra_boards['description'].str.split(' - ',expand=True)
metra_boards[['3', '4', '5', '6']] = metra_boards['1'].str.split(' ',expand=True)
metra_boards['direction'] = 'Outbound'
metra_boards.loc[metra_boards['3'].astype(int) % 2 == 0, 'direction'] = 'Inbound'
metra_boards = metra_boards.rename({'4': 'line_code'}, axis=1).drop(['1', '2', '3', '5', '6'], axis=1)
### CTA Rail Data
cta_boards = boards[boards['mode']=='C'].reset_index(drop=True)
cta_boards[['1', '2', '3']] = cta_boards['description'].str.split('-',expand=True)
cta_boards = cta_boards.rename({'1': 'line_code'}, axis=1).drop(['2', '3'], axis=1)
cta_boards['direction'] = 'NA'
### CTA Bus Data
cta_bus_boards = boards[boards['mode'].isin(['B', 'E'])].reset_index(drop=True)
cta_bus_boards[['1', '2', '3', '4', '5']] = cta_bus_boards['description'].str.split(' ',expand=True)
cta_bus_boards = cta_bus_boards.rename({'1': 'line_code'}, axis=1).drop(['2', '3', '4', '5'], axis=1)
cta_bus_boards['direction'] = 'NA'
### PACE Bus Data
pace_boards = boards[boards['mode'].isin(['P', 'L', 'Q'])].reset_index(drop=True)
pace_boards[['1', '2', '3', '4', '5', '6']] = pace_boards['description'].str.split(' ',expand=True)
pace_boards = pace_boards.rename({'1': 'line_code'}, axis=1).drop(['2', '3', '4', '5', '6'], axis=1)
pace_boards['direction'] = 'NA'
### CTA Data
### CTA Model Data Summarize
cta_board_summary = pd.pivot_table(cta_boards, 
                                    values='total_boardings', 
                                    index=['line_code', 'time_period', 'direction', 'mode_description'], 
                                    aggfunc=np.sum, 
                                    fill_value=0).round(0).reset_index()
cta_board_summary = cta_board_summary.rename_axis(None, axis=1)
cta_board_summary = cta_board_summary.rename({'total_boardings': 'model_boardings'}, axis=1)
## CTA Survey Data Read
cta_survey_boards = pd.read_csv(count_data_loc+"/counts/cta_boardings.csv")
### CTA Merge
cta_board_summary['line_code'] = cta_board_summary['line_code'].map(lambda x: x.rstrip(' '))
cta_board_summary = pd.merge(cta_board_summary, cta_survey_boards[['Line_longname','avg_weekday_scaled_to_annual']],
                             left_on='line_code', right_on='Line_longname', how='left')
cta_board_summary = cta_board_summary.drop('Line_longname', axis=1).rename({'avg_weekday_scaled_to_annual': 'survey_line_boardings'}, axis=1)
cta_board_summary['survey_line_boardings'] = cta_board_summary['survey_line_boardings'].replace(',','', regex=True).astype(int)
cta_board_summary['time_period_factor'] = cta_board_summary['time_period'].replace(tp_fac)
cta_board_summary['survey_boardings'] = cta_board_summary['survey_line_boardings']*cta_board_summary['time_period_factor']
cta_board_summary['diff'] = cta_board_summary['model_boardings']-cta_board_summary['survey_boardings']
cta_board_summary['percent diff'] = cta_board_summary['diff']/cta_board_summary['survey_boardings']*100
### Metra Data
### Metra Model Data Summarize
metra_board_summary = pd.pivot_table(metra_boards, 
                                     values='total_boardings', 
                                     index=['line_code', 'time_period', 'direction', 'mode_description'], 
                                     aggfunc=np.sum, 
                                     fill_value=0).round(0).reset_index()
metra_board_summary = metra_board_summary.rename_axis(None, axis=1)
metra_board_summary = metra_board_summary.rename({'total_boardings': 'model_boardings'}, axis=1)
### Metra Survey Data Read
metra_survey_boards = pd.read_csv(count_data_loc+"/counts/metra_boardings.csv").drop([4, 9]).fillna(0)
metra_survey_boards = pd.melt(metra_survey_boards, id_vars=['direction', 'period'], 
                              value_vars=['ME', 'RI', 'SWS', 'HC', 'BNSF', 'UP-W', 'MD-W', 'UP-NW', 'MD-N', 'NCS', 'UP-N'], 
                              var_name='line_code', 
                              value_name='survey_boardings').rename({'period': 'time_period'}, axis=1)
metra_survey_boards['time_period'] = metra_survey_boards['time_period'].replace({'AM-Peak': 'AM', 'Midday': 'MD', 'PM-Peak': 'PM', 'Evening': 'NT'})
### Metra Merge
metra_board_summary = pd.merge(metra_board_summary, metra_survey_boards, on=['time_period', 'direction', 'line_code'])
metra_board_summary['survey_boardings'] = metra_board_summary['survey_boardings'].replace(',','', regex=True).astype(int)
metra_board_summary['diff'] = metra_board_summary['model_boardings']-metra_board_summary['survey_boardings']
metra_board_summary['percent diff'] = metra_board_summary['diff']/metra_board_summary['survey_boardings']*100
### CTA Bus Data
### CTA Bus Model Data Summarize
cta_bus_board_summary = pd.pivot_table(cta_bus_boards, 
                                    values='total_boardings', 
                                    index=['line_code', 'time_period', 'direction', 'mode_description'], 
                                    aggfunc=np.sum, 
                                    fill_value=0).round(0).reset_index()
cta_bus_board_summary = cta_bus_board_summary.rename_axis(None, axis=1)
cta_bus_board_summary = cta_bus_board_summary.rename({'total_boardings': 'model_boardings'}, axis=1)
### CTA Bus Survey Data Read
cta_bus_survey_boards = pd.read_csv(count_data_loc+"/counts/CTA_Average_Bus_Ridership_1999_2021.csv")
cta_bus_survey_boards = cta_bus_survey_boards[(cta_bus_survey_boards['YEAR']==2019)&(cta_bus_survey_boards['DAY_TYPE']=='Weekday')]
cta_bus_survey_boards = cta_bus_survey_boards.groupby('ROUTE')['AVG_RIDES'].mean().to_frame()
cta_bus_survey_boards.reset_index(inplace=True)
### CTA Bus Merge
# cta_bus_board_summary['line_code'] = cta_bus_board_summary['line_code'].map(lambda x: x.rstrip(' '))
cta_bus_board_summary = pd.merge(cta_bus_board_summary, cta_bus_survey_boards[['ROUTE','AVG_RIDES']],
                             left_on='line_code', right_on='ROUTE', how='left')
cta_bus_board_summary = cta_bus_board_summary.drop('ROUTE', axis=1).rename({'AVG_RIDES': 'survey_line_boardings'}, axis=1)
# cta_bus_board_summary['survey_line_boardings'] = cta_bus_board_summary['survey_line_boardings'].replace(',','', regex=True).astype(int)
cta_bus_board_summary['time_period_factor'] = cta_bus_board_summary['time_period'].replace(tp_fac)
cta_bus_board_summary['survey_boardings'] = cta_bus_board_summary['survey_line_boardings']*cta_bus_board_summary['time_period_factor']
cta_bus_board_summary['diff'] = cta_bus_board_summary['model_boardings']-cta_bus_board_summary['survey_boardings']
cta_bus_board_summary['percent diff'] = cta_bus_board_summary['diff']/cta_bus_board_summary['survey_boardings']*100
### PACE Data
### Pace Model Data Summarize
pace_board_summary = pd.pivot_table(pace_boards, 
                                    values='total_boardings', 
                                    index=['line_code', 'time_period', 'direction', 'mode_description'], 
                                    aggfunc=np.sum, 
                                    fill_value=0).round(0).reset_index()
pace_board_summary = pace_board_summary.rename_axis(None, axis=1)
pace_board_summary = pace_board_summary.rename({'total_boardings': 'model_boardings'}, axis=1)
# pace_board_summary['mode'] = 'PACE'
### Pace Survey Data Read
pace_survey_boards = pd.read_csv(count_data_loc+"/counts/Pace_Monthly_Ridership_2003_2021.csv")
pace_survey_boards = pace_survey_boards[(pace_survey_boards['YEAR']==2019)&(pace_survey_boards['MONTH']==9)&(pace_survey_boards['DAY_TYPE']=='Weekday')]
pace_survey_boards['ROUTE'] = pace_survey_boards['ROUTE'].astype(str)
pace_total = pace_survey_boards['AVG_RIDES'].sum()
### PACE Merge
pace_board_summary = pd.merge(pace_board_summary, pace_survey_boards[['ROUTE','AVG_RIDES']],
                              left_on='line_code', right_on='ROUTE', how='outer')
pace_board_summary = pace_board_summary.drop('ROUTE', axis=1).rename({'AVG_RIDES': 'survey_line_boardings'}, axis=1)
pace_board_summary['time_period_factor'] = pace_board_summary['time_period'].replace(tp_fac)
pace_board_summary['survey_boardings'] = pace_board_summary['survey_line_boardings']*pace_board_summary['time_period_factor']
pace_board_summary['diff'] = pace_board_summary['model_boardings']-pace_board_summary['survey_boardings']
pace_board_summary['percent diff'] = pace_board_summary['diff']/pace_board_summary['survey_boardings']*100
## Merge data from all modes
columns_to_concat = ['mode_description', 
                     'time_period', 
                     'direction', 
                     'line_code', 
                     'model_boardings',
                     'survey_boardings', 
                     'diff', 
                     'percent diff']
boarding_summary = pd.concat([pace_board_summary[columns_to_concat], 
                              cta_bus_board_summary[columns_to_concat], 
                              metra_board_summary[columns_to_concat], 
                              cta_board_summary[columns_to_concat]], ignore_index=True)
#boarding_summary.to_csv(summary_dir + '/boarding_summary_by_line_TOD.csv', index=False)
daily_boarding_summary = pd.pivot_table(boarding_summary, 
                                      values=['model_boardings', 'survey_boardings'], 
                                      index=['mode_description', 'line_code'], 
                                      aggfunc=np.sum, 
                                      fill_value=0).round(0).reset_index()
daily_boarding_summary['diff'] = daily_boarding_summary['model_boardings']-daily_boarding_summary['survey_boardings']
daily_boarding_summary['percent diff'] = round(daily_boarding_summary['diff']/daily_boarding_summary['survey_boardings']*100, 3)
daily_boarding_summary.to_csv(summary_dir + '/boarding_summary_by_line.csv', index=False)
#daily_boarding_summary
agg_boarding_summary = pd.pivot_table(boarding_summary, 
                                      values=['model_boardings', 'survey_boardings'], 
                                      index=['mode_description'], 
                                      aggfunc=np.sum, 
                                      fill_value=0).round(0).reset_index()
# correct Pace routes boardings not matched to modelled route numbers
PACERegBus_survey_boardings = pace_total - agg_boarding_summary.at[4,'survey_boardings'] - agg_boarding_summary.at[5,'survey_boardings']
agg_boarding_summary.at[6,'survey_boardings'] = PACERegBus_survey_boardings
agg_boarding_summary['diff'] = agg_boarding_summary['model_boardings']-agg_boarding_summary['survey_boardings']
agg_boarding_summary['percent diff'] = round(agg_boarding_summary['diff']/agg_boarding_summary['survey_boardings']*100, 3)

### Table - Metra Daily Trip Length Frequency Distribution
### Read observed data files
for board in ['in_on', 'in_off', 'out_on', 'out_off']: 
    metra_obs['Daily_' + board] = metra_obs['AM_' + board] + metra_obs['MD_' + board] + metra_obs['PM_' + board] + metra_obs['NT_' + board]
metra_obs['Daily_total'] = metra_obs['Daily_in_on'] + metra_obs['Daily_out_off']
metra_obs['AM_total'] = metra_obs['AM_in_on'] + metra_obs['AM_out_off']
metra_obs['MD_total'] = metra_obs['MD_in_on'] + metra_obs['MD_out_off']
metra_obs['PM_total'] = metra_obs['PM_in_on'] + metra_obs['PM_out_off']
metra_obs['NT_total'] = metra_obs['NT_in_on'] + metra_obs['NT_out_off']
### Combine all time periods
tp = "AM"
boards = pd.read_csv(board_dir+"/boardings_by_segment_"+tp+"_iter2.csv", sep="\t")
boards['time_period'] = tp
for tp in ["MD", "NT", "PM"]:
    df = pd.read_csv(board_dir+"/boardings_by_segment_"+tp+"_iter2.csv", sep="\t")
    df['time_period'] = tp
    boards = pd.concat([boards, df])
boards = boards.drop(['c '], axis=1)
### Separate Metra Rail Data
metra_boards = boards[boards['mode']=='M'].reset_index(drop=True)
metra_boards[['1', '2']] = metra_boards['description'].str.split(' - ',expand=True)
metra_boards[['3', '4', '5', '6']] = metra_boards['1'].str.split(' ',expand=True)
metra_boards['direction'] = 'Outbound'
metra_boards.loc[metra_boards['3'].astype(int) % 2 == 0, 'direction'] = 'Inbound'
metra_boards = metra_boards.rename({'4': 'line_code'}, axis=1).drop(['1', '2', '3', '5', '6'], axis=1)
metra_boards_out = metra_boards[metra_boards['direction'] == 'Outbound'].copy()
metra_boards_in = metra_boards[metra_boards['direction'] == 'Inbound'].copy()
metra_boards_in = metra_boards_in.loc[::-1]
metra_boards = pd.concat([metra_boards_out, metra_boards_in])
### Calculate Cumulative Distance Travelled in each line
for i in metra_boards['description'].unique():
    metra_boards.loc[metra_boards['description']==i, 
                     'cum_length'] = metra_boards.loc[metra_boards['description']==i, 'length'].cumsum()
metra_boards['total'] = metra_boards['boardings'] * (metra_boards['direction'] == 'Inbound') + \
                        metra_boards['alightings'] * (metra_boards['direction'] == 'Outbound') 
tp = 'AM'
direction = 'Inbound'
direc_obs_dict = {'Inbound': 'in_on', 'Outbound': 'out_off', 'All': 'total'}
direc_model_dict = {'Inbound': 'boardings', 'Outbound': 'alightings', 'All': 'total'}
def plot_trip_length_distribution(tp, direction, obs_dist_col='mile_post', model_dist_col='cum_length'):
    ### Columns needed for each plot
    obs_freq_col = '_'.join([tp, direc_obs_dict[direction]])
    model_freq_col = direc_model_dict[direction]
    
    ### Process observed data
    obs_df = metra_obs[[obs_dist_col, obs_freq_col]].fillna(0)
    obs_df[obs_freq_col] = obs_df[obs_freq_col].replace(',','', regex=True).astype(int)
    obs_df['dist_bin'] = obs_df[obs_dist_col].apply(np.ceil)
    obs_dist_bin = obs_df.groupby(['dist_bin'])[obs_freq_col].agg('sum').to_frame()
    
    ### Process model data
    if tp == 'Daily' and direction == 'All':
        model_df = metra_boards[[model_dist_col, model_freq_col]]    
    elif tp == 'Daily':
        model_df = metra_boards.loc[(metra_boards['direction']==direction), 
                                    [model_dist_col, model_freq_col]]
    elif direction == 'All':
        model_df = metra_boards.loc[(metra_boards['time_period']==tp), 
                                    [model_dist_col, model_freq_col]]
    else:
        model_df = metra_boards.loc[(metra_boards['time_period']==tp)&
                                    (metra_boards['direction']==direction), 
                                    [model_dist_col, model_freq_col]]
    model_df[model_freq_col] = model_df[model_freq_col].replace(',','', regex=True).astype(int)
    print('Modeled:', model_df[model_freq_col].sum())
    model_df['dist_bin'] = model_df[model_dist_col].apply(np.ceil)
    model_dist_bin = model_df.groupby(['dist_bin'])[model_freq_col].agg('sum').to_frame()
    dist_bin = pd.merge(
        obs_dist_bin,
        model_dist_bin,
        how='outer',
        on=['dist_bin'],
    )
    dist_bin.rename({model_freq_col: 'model', obs_freq_col: 'survey'}, inplace = True)
    dist_bin.sort_values(by=['dist_bin'])#.to_csv(summary_dir + '/dist_bin_' + direction + '.csv')

    ### Plot
    fig, ax = plt.subplots(figsize=(6, 4), dpi=100)
    sns.histplot(data=obs_df, 
                 x=obs_dist_col, 
                 weights=obs_freq_col, 
                 stat='frequency', 
                 kde=True, 
                 binwidth=3, 
                 color='royalblue', element='bars', label='Observed')
    sns.histplot(data=model_df, 
                 x=model_dist_col, 
                 weights=model_freq_col, 
                 stat='frequency', 
                 kde=True, 
                 binwidth=3, 
                 color='chocolate', element='bars', label='Model')
    ax.set_xlabel('Miles', fontsize=12)
    ax.set_ylabel('Frequency', fontsize=12)
    if direction == 'All':
        ax.set_title(' '.join([tp, 'Boardings']), fontsize=16)
    else:
        ax.set_title(' '.join([tp, direction, 'Boardings']), fontsize=16)
    ax.legend()
    # ax.set_xlim([0, 100])
    plt.savefig(summary_dir + '/metra_tlfd.png')
    return fig
for tp in ['Daily']: #['AM', 'MD', 'PM', 'NT']:
    for direction in ['All']: #['Inbound', 'Outbound', 'All']:
        plot_trip_length_distribution(tp=tp, direction=direction, obs_dist_col='mile_post', model_dist_col='cum_length')    

### TABLE - Agency Transfer Rate [FIX ME - This data is currently rigid, update it if needed]
agency_transfer_rate = pd.DataFrame({'Agency': ['CTA', 'Metra', 'Pace', 'Total'], 
                                     'Boardings': [1468731, 267772, 102852, 1839355],
                                     'Trips': [1109629, 266967, 87767, 1464363],
                                     'Transfer Rate': [1.32, 1, 1.17, 1.26]})
### List for time periods and access modes
tp_list = ['AM', 'MD', 'PM', 'NT']
access_list = ['WALK_L', 'WALK_M', 'WALK_H', 'PNR', 'KNR']
### Add time period and access modes columns
model_trips['time_period'] = np.nan
model_boardings['time_period'] = np.nan
model_trips['access_mode'] = np.nan
model_boardings['access_mode'] = np.nan
for i in tp_list:
    model_trips.loc[model_trips['Demand'].str.contains(i), 'time_period'] = i
    model_boardings.loc[model_boardings['Demand'].str.contains(i), 'time_period'] = i
for i in access_list:
    model_trips.loc[model_trips['Demand'].str.contains(i), 'access_mode'] = i
    model_boardings.loc[model_boardings['Demand'].str.contains(i), 'access_mode'] = i
### Summarize Boardings
model_boardings_summary = pd.crosstab(index=model_boardings.access_mode, 
                                      columns=model_boardings.time_period, 
                                      values=model_boardings.Sum, 
                                      aggfunc=np.sum, 
                                      margins=True, margins_name='Total').round().fillna(0).astype(int)
model_boardings_summary = model_boardings_summary.loc[access_list+['Total']]
model_boardings_summary = model_boardings_summary[tp_list+['Total']]
### Summarize Trips
model_trips_summary = pd.crosstab(index=model_trips.access_mode, 
                                      columns=model_trips.time_period, 
                                      values=model_trips.Sum, 
                                      aggfunc=np.sum, 
                                      margins=True, margins_name='Total').round().fillna(0).astype(int)

### Opening Excel Workbook for summaries
writer = pd.ExcelWriter(summary_dir + '/assignment_validation.xlsx', engine = 'xlsxwriter')
workbook = writer.book

### Creating cell format for 
description_format = workbook.add_format({'bold': True, 'size': 12})
### Writing counts by county/func class
worksheet = workbook.add_worksheet('counts_by_county-ftype')
writer.sheets['counts_by_county-ftype'] = worksheet

worksheet.write_string(0, 0, 'Estimated vs. Observed Passenger Vehicle and Light Truck Counts - By County and Facility Type', description_format)
worksheet.write_string(1, 0, 'Only links with observed counts are included')

start = 4
start_column = 0
worksheet.write_string(start-1, start_column, 'Estimated Counts')
model_counts.to_excel(writer, sheet_name = 'counts_by_county-ftype', startrow = start, startcol = start_column)

start_column += model_counts.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed Counts')
obs_counts.to_excel(writer, sheet_name = 'counts_by_county-ftype', startrow = start, startcol = start_column)

start_column += obs_counts.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated/Observed Ratio')
ratio_df = round(model_counts/obs_counts, 2)
ratio_df.to_excel(writer, sheet_name = 'counts_by_county-ftype', startrow = start, startcol = start_column)
### Writing counts by TOD/veh class
worksheet = workbook.add_worksheet('counts_by_tod-veh')
writer.sheets['counts_by_tod-veh'] = worksheet

worksheet.write_string(0, 0, 'Estimated vs. Observed Counts - By TOD and Vehicle Class', description_format)
worksheet.write_string(1, 0, 'Only links with observed counts by time of day are included')

start = 4
start_column = 0
worksheet.write_string(start-1, start_column, 'Estimated Counts')
model_counts_tod_veh.to_excel(writer, sheet_name = 'counts_by_tod-veh', startrow = start, startcol = start_column)

start_column += model_counts_tod_veh.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed Counts')
obs_counts_tod_veh.to_excel(writer, sheet_name = 'counts_by_tod-veh', startrow = start, startcol = start_column)

start_column += obs_counts_tod_veh.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated/Observed Ratio')
ratio_df = round(model_counts_tod_veh/obs_counts_tod_veh, 2)
ratio_df.to_excel(writer, sheet_name = 'counts_by_tod-veh', startrow = start, startcol = start_column)
### Writing counts by veh class
worksheet = workbook.add_worksheet('counts_by_veh_class')
writer.sheets['counts_by_veh_class'] = worksheet

worksheet.write_string(0, 0, 'Estimated vs. Observed Counts - By Vehicle Class', description_format)

start = 4
start_column = 0
worksheet.write_string(start-1, start_column, 'Estimated Counts')
model_counts_veh.to_excel(writer, sheet_name = 'counts_by_veh_class', startrow = start, startcol = start_column)

start_column += model_counts_veh.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed Counts')
obs_counts_veh.to_excel(writer, sheet_name = 'counts_by_veh_class', startrow = start, startcol = start_column)

start_column += obs_counts_veh.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated/Observed Ratio')
ratio_df = round(model_counts_veh/obs_counts_veh, 2)
ratio_df.to_excel(writer, sheet_name = 'counts_by_veh_class', startrow = start, startcol = start_column)
### Writing VMT only on links with counts by county/func class
worksheet = workbook.add_worksheet('vmt_with_counts_by_county-ftype')
writer.sheets['vmt_with_counts_by_county-ftype'] = worksheet

worksheet.write_string(0, 0, 'Estimated vs. Observed Passenger Vehicle and Light Truck VMT - By County and Facility Type', description_format)
worksheet.write_string(1, 0, 'Only links with observed counts are included')

start = 4
start_column = 0
worksheet.write_string(start-1, start_column, 'Estimated VMT')
model_vmt_no_cnt.to_excel(writer, sheet_name = 'vmt_with_counts_by_county-ftype', startrow = start, startcol = start_column)

start_column += model_vmt_no_cnt.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed VMT')
obs_vmt_no_cnt.to_excel(writer, sheet_name = 'vmt_with_counts_by_county-ftype', startrow = start, startcol = start_column)

start_column += obs_vmt_no_cnt.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated/Observed Ratio')
ratio_df = round(model_vmt_no_cnt/obs_vmt_no_cnt, 2)
ratio_df.to_excel(writer, sheet_name = 'vmt_with_counts_by_county-ftype', startrow = start, startcol = start_column)
### Writing VMT on all links in Illinois by county
worksheet = workbook.add_worksheet('vmt_in_IL_by_county')
writer.sheets['vmt_in_IL_by_county'] = worksheet

worksheet.write_string(0, 0, 'Estimated vs. Observed VMT on all links in Illinois - By County', description_format)

start = 4
start_column = 0
worksheet.write_string(start-1, start_column, 'Estimated VMT')
model_vmt_IL.to_excel(writer, sheet_name = 'vmt_in_IL_by_county', startrow = start, startcol = start_column)

start_column += model_vmt_IL.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed VMT')
obs_vmt_IL.to_excel(writer, sheet_name = 'vmt_in_IL_by_county', startrow = start, startcol = start_column)

start_column += obs_vmt_IL.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated/Observed Ratio')
ratio_df = round(model_vmt_IL/obs_vmt_IL, 2)
ratio_df.to_excel(writer, sheet_name = 'vmt_in_IL_by_county', startrow = start, startcol = start_column)
### Writing VMT on all links by county/func class
worksheet = workbook.add_worksheet('vmt_by_county-ftype')
writer.sheets['vmt_by_county-ftype'] = worksheet

worksheet.write_string(0, 0, 'Estimated vs. Observed VMT on all links - By County and Facility Type', description_format)

start = 4
start_column = 0
worksheet.write_string(start-1, start_column, 'Estimated VMT')
model_vmt.to_excel(writer, sheet_name = 'vmt_by_county-ftype', startrow = start, startcol = start_column)

start_column += model_vmt.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed VMT')
obs_vmt.to_excel(writer, sheet_name = 'vmt_by_county-ftype', startrow = start, startcol = start_column)

start_column += obs_vmt.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated/Observed Ratio')
ratio_df = round(model_vmt/obs_vmt, 2)
ratio_df.to_excel(writer, sheet_name = 'vmt_by_county-ftype', startrow = start, startcol = start_column)
### Writing Screenline Volume Summary
title = 'screenlines_volume_summary'
worksheet = workbook.add_worksheet(title)
writer.sheets[title] = worksheet

worksheet.write_string(0, 0, 'Summary of Daily Modeled and Observed traffic counts by Screenline', description_format)

start = 4
start_column = 0

volume_summary.to_excel(writer, sheet_name = title, startrow = start, startcol = start_column, index=False)

worksheet.insert_image('G5', summary_dir + '/screenlines_map_with_labels.png')
### Writing Corridor VMT Summary
title = 'corridor_vmt_summary'
worksheet = workbook.add_worksheet(title)
writer.sheets[title] = worksheet

worksheet.write_string(0, 0, 'Summary of Daily Modeled and Observed vmt in each Corridor', description_format)

start = 4
start_column = 0

vmt_summary.to_excel(writer, sheet_name = title, startrow = start, startcol = start_column, index=False)

worksheet.insert_image('G5', summary_dir + '/corridors_map_with_labels.png')
### Writing Transit Boarding Summary by Operator and Mode
title = 'boarding_summary'
worksheet = workbook.add_worksheet(title)
writer.sheets[title] = worksheet

worksheet.write_string(0, 0, 'Daily Transit Boarding Summary by Operator and Mode', description_format)

start = 4
start_column = 0
#worksheet.write_string(start-1, start_column, 'Daily Boarding Summary')
agg_boarding_summary.to_excel(writer, sheet_name = title, startrow = start, startcol = start_column, index=False)
### Writing Metra Tip Length Frequency Distribution
title = 'Metra_tlfd'
worksheet = workbook.add_worksheet(title)
writer.sheets[title] = worksheet

worksheet.write_string(0, 0, 'Metra Daily Trip Length Frequency Distribution', description_format)

worksheet.insert_image('A5', summary_dir + '/metra_tlfd.png')
### Writing Transfer Rate Tables 
title = 'transfer_rate'
worksheet = workbook.add_worksheet(title)
writer.sheets[title] = worksheet

worksheet.write_string(0, 0, 'Transfer Rate by Access Mode and Agency', description_format)

start = 4
start_column = 0
#worksheet.write_string(start-1, start_column, 'Estimated Boardings')
#model_boardings_summary.to_excel(writer, sheet_name = title, startrow = start, startcol = start_column)
#
#start_column += model_boardings_summary.shape[1] + 3
#worksheet.write_string(start-1, start_column, 'Estimated Trips')
#model_trips_summary.to_excel(writer, sheet_name = title, startrow = start, startcol = start_column)
#
#start_column += model_trips_summary.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Estimated Transfer Rate by Access Mode')
transfer_rate_df = round(model_boardings_summary/model_trips_summary, 2)
transfer_rate_df.to_excel(writer, sheet_name = title, startrow = start, startcol = start_column)


start_column += transfer_rate_df.shape[1] + 3
worksheet.write_string(start-1, start_column, 'Observed Transfer Rate by Agency')
agency_transfer_rate.set_index('Agency').to_excel(writer, sheet_name = title, startrow = start, startcol = start_column)
# writer.save()
writer.close()
os.remove(summary_dir + '/metra_tlfd.png')
end_time = datetime.datetime.now()
print("Start Time:", start_time)
print("End Time:", end_time)
print("Run Time:", round(end_time.timestamp()-start_time.timestamp(), 3), "sec")