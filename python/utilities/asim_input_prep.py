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
        asim_inputs = os.environ['ASIM_INPUTS']
        model_inputs = os.environ['INPUT_FOLDER']
        mazfile_name = parms['mazfile_name']
        mazid = parms['maz_shape_maz_id']
        tazid = parms['maz_shape_taz_id']
        mazshapeSf = gpd.read_file(os.path.join(model_inputs, mazfile_name))
        mazshapeSf.rename(columns = {mazid: "MAZ", tazid: "TAZ"}, inplace = True)
        mazshapeSf[['MAZ', 'TAZ']].to_csv(os.path.join(asim_inputs, "MAZ.csv"), index = False)
        tazList = mazshapeSf['TAZ'].unique()
        tazList.sort()
        pd.DataFrame(tazList, columns = ['TAZ']).to_csv(os.path.join(asim_inputs, "TAZ.csv"), index = False)
        