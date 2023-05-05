import pandas as pd, numpy as np, openmatrix as omx, os
ASIM_INPUT = os.environ["ASIM_INPUT"]

land_use = pd.read_csv(os.path.join(ASIM_INPUT, 'land_use.csv'))
taz_county_xwalk = pd.pivot_table(land_use, values=['county'], index=['zone'], margins=False)

rows = np.arange(1,len(taz_county_xwalk) + 1)
cols = np.arange(1,len(taz_county_xwalk) + 1)

data = np.empty([len(rows),len(rows)])

for i in range(len(rows)):
    data[:,i] = np.transpose(np.repeat(taz_county_xwalk.loc[i+1, 'county'], len(rows)))

skims = omx.open_file(os.path.join(ASIM_INPUT,'COUNTY.omx'), 'w')
skims['COUNTY'] = data
skims.create_mapping('ZoneID', sorted(rows))
skims.close()
