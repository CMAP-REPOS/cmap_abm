#
# Prepare ASim files
#
import os
import pandas as pd
import geopandas as gpd

class ASimInputPrep():
    __MODELLER_NAMESPACE__ = "cmap"
    tool_run_msg = ""
    
    def __init__(self):
        print("Preparing Inputs for ActivitySim...")

    def __call__(self, parms):
        asim_inputs = os.environ['ASIM_INPUT']
        twozone_input = os.environ['TWOZONE_INPUT']
        mazfile_name = parms['mazfile_name']
        mazid = parms['maz_shape_maz_id']
        tazid = parms['maz_shape_taz_id']
        mazshapeSf = gpd.read_file(os.path.join(twozone_input, mazfile_name), engine = 'pyogrio')
        mazshapeSf.rename(columns = {mazid: "MAZ", tazid: "TAZ"}, inplace = True)
        mazList = mazshapeSf[['MAZ', 'TAZ']]
        maxMAZ = mazList['MAZ'].max()
        maxTAZ = mazList['TAZ'].max()
        for i in range(maxTAZ + 1, maxTAZ + parms['land_use']['numExtTAZ'] + 1):
            #externalList = {'MAZ': i + (maxMAZ - maxTAZ), 'TAZ': i}
            #mazList = mazList.append(externalList, ignore_index = True) # add external MAZs and TAZs
            externalList = pd.DataFrame([[i + (maxMAZ - maxTAZ), i]], columns=['MAZ', 'TAZ'])
            mazList = pd.concat([mazList, externalList], ignore_index = True)
        mazList.to_csv(os.path.join(asim_inputs, "maz.csv"), index = False)
        #tazList = mazshapeSf['TAZ'].unique()
        #tazList.sort()
        #pd.DataFrame(tazList, columns = ['TAZ']).to_csv(os.path.join(asim_inputs, "taz.csv"), index = False)
        