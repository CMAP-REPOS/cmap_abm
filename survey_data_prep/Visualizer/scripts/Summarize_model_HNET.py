#
# Summarize_model_HNET.py - file to do some reading and summarizing of loaded highway networks (as dbf or csv)
# 
# Andrew Rohne RSG 3/2022
#
# Reads network DBF files (AM, PM, MD, EV, ON), compiles data for ActivitySim Visualizer
# Assumes that this is run within the model stream - uses environment variables setup by
# the model batch file.
#
# NOT FULLY TESTED - Works with Metropolitan Council data, but did not test if it would 
# work with added period labels, etc.

import pandas as pd
from simpledbf import Dbf5
import os
import numpy as np

model_path = os.environ['EMME_OUTPUT']
vis_path = os.environ['WORKING_DIR'] + os.sep + "data" + os.sep + "base"
net_in_path = os.environ['WORKING_DIR'] + os.sep + "data" + os.sep + "counts"
print(f"Reading Highway Loading Results from {model_path}")

hnetfile = 'emme_links.dbf'
periods = {'0': 'DAILY', '1': 'NT', '2': 'EA', '3': 'AM', '4': 'MM', '5': 'MD', '6': 'AF', '7': 'PM', '8': 'EV'}
periods_labels = {'DAILY': 'Daily', 'NT': 'Night', 'EA': 'Early AM', 'AM': 'AM Peak', 'MM': 'Mid Morning', 'MD': 'Midday', 'AF': 'Afternoon', 'PM': 'PM Peak', 'EV': 'Evening'}
#count_fields = {'DAILY': 'COUNT', 'AM': 'AM_CNT', 'MD': 'MD_CNT', 'PM': 'PM_CNT', 'NT': 'NT_CNT'}
count_fields = {'DAILY': 'DY_CNT', 'NT': 'NT_CNT', 'EA': 'EA_CNT', 'AM': 'AM_CNT', 'MM': 'MM_CNT', 'MD': 'MD_CNT', 'AF': 'AF_CNT', 'PM': 'PM_CNT', 'EV': 'EV_CNT'}
assign_fields = {'DAILY': 'DY_ASN', 'NT': 'NT_ASN', 'EA': 'EA_ASN', 'AM': 'AM_ASN', 'MM': 'MM_ASN', 'MD': 'MD_ASN', 'AF': 'AF_ASN', 'PM': 'PM_ASN', 'EV': 'EV_ASN'}

rmseGroups = [0, 500, 1500, 2500, 3500, 4500, 5500, 7000, 8500, 10000, 12500, 15000, 17500, 20000, 25000, 35000, 55000, 75000, 120000, 250000]
rmseLimits = [200, 100, 62, 54, 48, 45, 42, 39, 36, 34, 31, 30, 28, 26, 24, 21, 18, 12, 12]

# Reading counts file from inputs
print(f"Reading traffic counts from {net_in_path}")
countsfile = "TOD2019IDOT.csv"
countsfile_Toll = "TOD2019Tollway.csv"
countsfile_AADT = "AADT2019.csv"


# NOTE: ftCol is the name of the facility type column, check code if it's "FTYPE". ftTrans needs to match _SYSTEM_VARIABLES.R hnet_ft
ftCol = "VDF"
ftTrans = {1: "Arterial", 2: "Freeway",  3: "Freeway",4: "Expressway", 5: "Freeway", 6: "Connector", 7: "Freeway", 8: "Freeway"}
countyColumn = "COUNTY"
lengthColumn = "LENGTH"
distMult = 1.0 # in case the distance needs to be converted to another unit

# each period (scenario) has a dbf network; import first network and merge other networks 
scen_path = os.path.join(model_path, "scen01")
hnet_TOD = Dbf5(os.path.join(scen_path, hnetfile)).to_dataframe()
hnet_TOD.rename(columns={'VOLAU': 'NT_ASN'}, inplace=True)
#hnet_TOD['DY_ASN'] = hnet_TOD['NT_ASN']
hnet = hnet_TOD[['ID', 'INODE', 'JNODE', 'LENGTH', 'VDF', 'NT_ASN']].copy()
hnet.fillna(0, inplace=True)
for scen_num in range(2, len(periods_labels)):
    scen_path = os.path.join(model_path, "scen0" + str(scen_num))
    if hnetfile[-4:].lower() == ".dbf":
        hnet_TOD = Dbf5(os.path.join(scen_path, hnetfile)).to_dataframe()
    elif hnetfile[-4:].lower() == ".csv":
        hnet_TOD = pd.read_csv(os.path.join(scen_path, hnetfile))
    else:
        raise ValueError('hnet_TOD File is not supported')
    hnet = pd.merge(hnet, hnet_TOD[['ID', 'INODE', 'JNODE', 'VOLAU', 'LENGTH', 'VDF']],
                 on = ['ID', 'INODE', 'JNODE', 'LENGTH', 'VDF'], how='outer')
    TOD_header = periods[str(scen_num)] + '_ASN'
    hnet.rename(columns={'VOLAU': TOD_header}, inplace=True)
    hnet.fillna(0, inplace=True)
    #hnet['DY_ASN'] += hnet[TOD_header]
hnet['DY_ASN'] = 0
for scen_num in range(1, len(periods_labels)):
    hnet['DY_ASN'] += hnet[periods[str(scen_num)] + '_ASN']
#hnet = pd.read_csv("hnet.csv")

if countsfile[-4:].lower() == ".dbf":
    counts = Dbf5(os.path.join(net_in_path, countsfile)).to_dataframe()
elif countsfile[-4:].lower() == ".csv":
    counts = pd.read_csv(os.path.join(net_in_path, countsfile))
else:
    raise ValueError('Counts file is not supported')
if countsfile_Toll[-4:].lower() == ".dbf":
    counts_Toll = Dbf5(os.path.join(net_in_path, countsfile_Toll)).to_dataframe()
elif countsfile_Toll[-4:].lower() == ".csv":
    counts_Toll = pd.read_csv(os.path.join(net_in_path, countsfile_Toll), encoding='cp1252')
else:
    raise ValueError('counts_Toll file is not supported')
if countsfile_AADT[-4:].lower() == ".dbf":
    counts_AADT = Dbf5(os.path.join(net_in_path, countsfile_AADT)).to_dataframe()
elif countsfile_AADT[-4:].lower() == ".csv":
    counts_AADT = pd.read_csv(os.path.join(net_in_path, countsfile_AADT), encoding='cp1252')
else:
    raise ValueError('counts_AADT file is not supported')

for per in range(1,len(periods_labels)):
    TOD_counts = counts[counts["TOD"] == per].copy()
    TOD_counts.rename(columns={'Anode': 'INODE', 'Bnode': 'JNODE'}, inplace=True)
    #hnet = hnet.merge(TOD_counts[['INODE', 'JNODE', 'AADT']], on = ['INODE', 'JNODE'], how = 'left')
    #hnet.rename(columns={'AADT': 'COUNTS_' + periods[str(per)]}, inplace=True)
    TOD_counts_Toll = counts_Toll[counts_Toll["TOD"] == per].copy()
    TOD_counts_Toll.rename(columns={'Anode': 'INODE', 'Bnode': 'JNODE'}, inplace=True)
    TOD_counts_Toll['AADT'] = TOD_counts_Toll['AADT'] * TOD_counts_Toll['tier1_percent']
    TOD_counts_all = pd.concat([TOD_counts[['INODE', 'JNODE', 'AADT']], TOD_counts_Toll[['INODE', 'JNODE', 'AADT']]])
    hnet = hnet.merge(TOD_counts_all[['INODE', 'JNODE', 'AADT']], on = ['INODE', 'JNODE'], how = 'left')
    hnet.rename(columns={'AADT': periods[str(per)] + '_CNT'}, inplace=True)
hnet['DY_CNT'] = 0
for per in range(1, len(periods_labels)):
    hnet['DY_CNT'] += hnet[ periods[str(per)] + '_CNT']
counts_AADT.rename(columns={'Anode': 'INODE', 'Bnode': 'JNODE'}, inplace=True)
counts_AADT['AADT_CNT'] = (counts_AADT['AADT19adj'] - counts_AADT['HCAADT19adj']).clip(lower=0)
hnet = hnet.merge(counts_AADT[['INODE', 'JNODE', 'AADT_CNT']], on = ['INODE', 'JNODE'], how = 'left')
hnet['DY_CNT'] = np.where(np.isnan(hnet['DY_CNT']), hnet['AADT_CNT'], hnet['DY_CNT'])
hnet.drop(columns=['AADT_CNT'], inplace=True)

print(f"Counted Daily VMT: {(hnet[count_fields['DAILY']] * hnet_TOD[lengthColumn] * distMult).sum()}")

print(f"Assigned Daily VMT: {(hnet[hnet[count_fields['DAILY']] > 0][assign_fields['DAILY']] * hnet[hnet[count_fields['DAILY']] > 0][lengthColumn] * distMult).sum()}")

# Export hnet_TOD first (for assignment summaries)
hnet['FTYPE'] = hnet[ftCol].map(ftTrans)
#if countyColumn != "COUNTY":
#	hnet_TOD.rename(columns = {countyColumn: "COUNTY"}, inplace = True) #NOTE: Untested
hnet.to_csv(os.path.join(vis_path, "hnetcnt.csv"), index = False)
#hnet.rename(columns = {assign_fields['DAILY']: 'DY_ASN', count_fields['DAILY']: 'DY_CNT', assign_fields['AM']: 'AM_ASN', count_fields['AM']: 'AM_CNT', assign_fields['MD']: 'MD_ASN', count_fields['MD']: 'MD_CNT', assign_fields['PM']: 'PM_ASN', count_fields['PM']: 'PM_CNT', assign_fields['NT']: 'NT_ASN', count_fields['NT']: 'NT_CNT'}).to_csv(os.path.join(vis_path, "hnetcnt.csv"), index = False)

asnvmt = []
for k, v in periods_labels.items():
	asnvmt.append(hnet.groupby('FTYPE').apply(lambda s: pd.Series({"Period": k, "nLinks": s.shape[0], "vmt": np.sum(s[assign_fields[k]] * s[lengthColumn] * distMult)})).reset_index())

outasnvmt = pd.concat(asnvmt)
outasnvmt.to_csv(os.path.join(vis_path, "asnvmt.csv"), index = False)

hnet = hnet[(hnet[count_fields['DAILY']] > 0) & (~hnet[count_fields['DAILY']].isna())]
hnet = hnet.reset_index()
hnet['vg'] = pd.cut(hnet['DY_CNT'], rmseGroups, right = False)
rmse = []
for k, v in periods_labels.items():
    rmse.append(hnet[(hnet[count_fields[k]] > 0) & (~hnet[count_fields[k]].isna())].groupby('vg').apply(lambda s: pd.Series({f"n_{k}": s[count_fields[k]].shape[0], f"mae_{k}": np.abs(s[count_fields[k]] - s[assign_fields[k]]).mean(), f"rmse_{k}": np.sqrt(np.power(s[count_fields[k]] - s[assign_fields[k]], 2).mean()), f"prmse_{k}": np.sqrt(np.power(s[count_fields[k]] - s[assign_fields[k]], 2).mean()) / (s[count_fields[k]].sum() / (s[count_fields[k]].count()-1)) if s[count_fields[k]].count() > 1 else 0})).reset_index())
outrmse = rmse[0]
for x in range(1, len(rmse)):
	outrmse = outrmse.merge(rmse[x], on = 'vg')
outrmse['limit'] = rmseLimits
outrmse.to_csv(os.path.join(vis_path, "hassign_vgsum.csv"), index = True, index_label="vgidx")

vmtcomp = []
for k, v in periods_labels.items():
    vmtcomp.append(hnet[(hnet[count_fields[k]].fillna(0) > 0)].groupby('FTYPE').apply(lambda s: pd.Series({f"n_{k}": s[count_fields[k]].shape[0], f"obsvmt_{k}": np.sum(s[count_fields[k]] * s[lengthColumn] * distMult), f"asnvmt_{k}": np.sum(s[assign_fields[k]] * s[lengthColumn] * distMult)})))
outvmtcomp = vmtcomp[0]
for x in range(1, len(vmtcomp)):
    outvmtcomp = outvmtcomp.merge(vmtcomp[x], on = 'FTYPE')
outvmtcomp.to_csv(os.path.join(vis_path, "hassign_vmtcomp.csv"), index = True, index_label="FTYPE")

# Overall summaries

overall = pd.DataFrame([{'cntDyVMT': (hnet[count_fields['DAILY']].fillna(0) * hnet[lengthColumn] * distMult).sum()},
{'AsnDyVMT_LWC': (hnet[hnet[count_fields['DAILY']].fillna(0) > 0][assign_fields['DAILY']] * hnet[hnet[count_fields['DAILY']].fillna(0) > 0][lengthColumn] * distMult).sum()},
{'cntDyTot': hnet[count_fields['DAILY']].fillna(0).sum()},

{'cntN': hnet[hnet[count_fields['DAILY']].fillna(0) > 0].count()}])

print(overall)


"""

{'DyRMSE': np.sqrt(np.power(s[count_fields[k]] - s[assign_fields[k]], 2).mean()) / (s[count_fields[k]].sum() / (s[count_fields[k]].count()-1))
"""