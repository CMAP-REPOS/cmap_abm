import pandas as pd, numpy as np, openmatrix as omx, os
ASIM_INPUT = os.environ["ASIM_INPUT"]

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



# %%
