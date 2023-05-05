import pandas as pd, numpy as np, openmatrix as omx, os
ASIM_INPUT = os.environ["ASIM_INPUT"]

land_use = pd.read_csv(os.path.join(ASIM_INPUT, 'land_use.csv'))
taz_areatype_xwalk = pd.pivot_table(land_use, values=['areatype'], index=['zone'], margins=False).round(0)

rows = np.arange(1,len(taz_areatype_xwalk) + 1)
cols = np.arange(1,len(taz_areatype_xwalk) + 1)

data = np.empty([len(rows),len(rows)])

for i in range(len(rows)):
    data[:,i] = np.transpose(np.repeat(taz_areatype_xwalk.loc[i+1, 'areatype'], len(rows)))

skims = omx.open_file(os.path.join(ASIM_INPUT,'AREATYPE.omx'), 'w')
skims['AREATYPE'] = data
skims.create_mapping('ZoneID', sorted(rows))
skims.close()
