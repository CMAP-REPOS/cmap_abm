import pandas as pd, numpy as np, openmatrix as omx, os
ASIM_INPUT = os.environ["ASIM_INPUT"]
# destination in chiago flag omx 
land_use = pd.read_csv(os.path.join(ASIM_INPUT, 'land_use.csv'), usecols=['zone'])
land_use.drop_duplicates(inplace=True)
land_use['chicago'] = np.where(land_use['zone'] <= 717, 1, 0)
land_use = land_use.set_index('zone')
rows = np.arange(1,len(land_use) + 1)
cols = np.arange(1,len(land_use) + 1)
data = np.empty([len(rows),len(rows)])
for i in range(len(rows)):
    data[:,i] = np.transpose(np.repeat(land_use.loc[i+1, 'chicago'], len(rows)))
skims = omx.open_file(os.path.join(ASIM_INPUT,'dest_chicago.omx'), 'w')
skims['dest_chicago'] = data
skims.create_mapping('ZoneID', sorted(rows))
skims.close()
# destination areatype and county omx 
land_use = pd.read_csv(os.path.join(ASIM_INPUT, 'land_use.csv'))
taz_county_xwalk = pd.pivot_table(land_use, values=['county'], index=['zone'], margins=False)
taz_areatype_xwalk = pd.pivot_table(land_use, values=['areatype'], index=['zone'], margins=False).round(0)
rows = np.arange(1,len(taz_county_xwalk) + 1)
cols = np.arange(1,len(taz_county_xwalk) + 1)
data_county = np.empty([len(rows),len(rows)])
data_areatype = np.empty([len(rows),len(rows)])
for i in range(len(rows)):
    data_county[:,i] = np.transpose(np.repeat(taz_county_xwalk.loc[i+1, 'county'], len(rows)))
    data_areatype[:,i] = np.transpose(np.repeat(taz_areatype_xwalk.loc[i+1, 'areatype'], len(rows)))
skims = omx.open_file(os.path.join(ASIM_INPUT,'COUNTY.omx'), 'w')
skims['COUNTY'] = data_county
skims.create_mapping('ZoneID', sorted(rows))
skims.close()
skims = omx.open_file(os.path.join(ASIM_INPUT,'AREATYPE.omx'), 'w')
skims['AREATYPE'] = data_areatype
skims.create_mapping('ZoneID', sorted(rows))
skims.close()

