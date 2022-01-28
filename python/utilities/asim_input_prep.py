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
        tap_attributes = pd.read_csv(os.path.join(model_inputs, parms['tap_attributes']["file"]))
        tapSf = gpd.GeoDataFrame(tap_attributes, geometry=gpd.points_from_xy(tap_attributes[parms['tap_attributes']['x_field']], tap_attributes[parms['tap_attributes']['y_field']]))
        tap_join = gpd.sjoin(tapSf, mazshapeSf)
        tap_join.rename(columns = {parms['tap_attributes']['id_field']: "TAP"}, inplace = True)
        tap_join.sort_values(by = "TAP")[["TAP", "MAZ"]].to_csv(os.path.join(asim_inputs, "TAP.csv"), index = False)
        