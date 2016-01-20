############################################################
# CMAP accessibilities
############################################################

#Requires Anaconda Python 
#Ben Stabler, ben.stabler@rsginc.com, 12/15/15 
#Nested Model, Time periods then Modes

import sys, os.path, zipfile, struct, array, time
import pandas as pd, numpy as np

############################################################
# paramaters
############################################################

#settings
inputsFolder = "inputs/"
outputsFolder = "outputs/"
walkSpeed = 3 #mph
mu = 0.5 #TOD choice nest coeff
nu = 0.5 #mode choice nest coeff
NA = -9999
trace_index = 0 #maz to maz OD index offset

#inputs
modeUEC = "Accessibility_mode_UEC.csv"
sizeTermsUEC = "Accessibility_size_variables_UEC.CSV"
impedanceUEC = "Accessibility_impedance_UEC.CSV"
measuresUEC = "Accessibility_measures_UEC.csv"
mazData = "SubzoneData.csv"
mazCents = "CMAP_MAZ_cents.txt"
tapCents = "tap_attributes.csv"

#outputs
nearTaps = "taps_near.csv"
mazAccessibilities = "accessibility_maz.csv"

############################################################
# functions
############################################################

def readZMX(fileName):
    #read ZMX file

    #read header files
    z = zipfile.ZipFile(fileName, "r")
    version = z.read("_version")
    description = z.read("_description")
    name = z.read("_name")
    zoneNames = z.read("_external column numbers")
    rowZoneNames = z.read("_external row numbers")
    columns = z.read("_columns")
    rows = int(z.read("_rows"))

    #read rows, big-endian floats
    mat = []
    for i in range(1,rows+1):
        fileNameRow = "row_" + str(i)
        data = z.read(fileNameRow)
        mat.append(struct.unpack(">" + "f"*rows,data))

    #close connections
    z.close()

    #return matrix data, zone names, matrix name
    return(mat, zoneNames, name)

def addMatToDb(db, fullFileName):
    
    #dict of numpy 2D arrays
    #db['mf3175'][(1,5),(3,1)] #get OD indices 1->3 and 5->1

    #read matrix
    mat, zoneNames, name = readZMX(fullFileName)
    zoneNames = map(int, zoneNames.split(','))
    
    #add to table
    if name not in db: 
        db[name] = np.array(mat)
    print("read matrix: " + name)

    #create dataframe of zone labels and indexes
    zoneNames = pd.DataFrame({"labels":zoneNames}, index=zoneNames)
    zoneNames['index'] = range(len(zoneNames))
    return(zoneNames)

############################################################
# program entry points
############################################################

if __name__== "__main__":
  
  #get command line arguments
  runmode = sys.argv[1].lower()
    
  print("start run mode " + runmode + " : " + time.ctime())
  
  if runmode == 'near_taps':
    
    #read maz data
    mazs = pd.read_csv(inputsFolder + mazData, index_col=0)
    mazcents = pd.read_csv(inputsFolder + mazCents, index_col=0)
    mazs = pd.merge(mazs, mazcents, left_index=True, right_index=True)
    
    #read tap data
    taps = pd.read_csv(inputsFolder + tapCents, index_col=0)
    
    #calculate maz to tap distances
    print("calculate maz to tap distances")
    mazCoords = mazs[['X','Y']]
    mazCoords = mazCoords.append([mazCoords]*(len(taps.index)-1))
    mazCoords['mazid'] = mazCoords.index
    
    mazCoords['tapx'] = np.repeat(taps['x'].tolist(),len(mazs.index))
    mazCoords['tapy'] = np.repeat(taps['y'].tolist(),len(mazs.index))
    mazCoords['tapid'] = np.repeat(taps.index.tolist(),len(mazs.index))
    mazCoords.set_index(['mazid','tapid'],inplace=True)

    mazCoords['distance'] = mazCoords.eval("((X-tapx)**2 + (Y-tapy)**2)**0.5")

    #calculate nearest maz walk tap and walk time
    print("calculate nearest maz walk tap and walk time")
    grouped = mazCoords.groupby(level=0) #group by maz
    walktaps = grouped['distance'].idxmin()
    mazs['wt_tap'] = map(lambda x: x[1], walktaps) #(maz,tap)
    walkdists = grouped['distance'].min()
    mazs['wt_time'] = walkdists / 5280 * 60.0 / walkSpeed

    #write result
    mazs.to_csv(inputsFolder + nearTaps, columns=['wt_tap','wt_time'])

    print("end run mode " + runmode + " : " + time.ctime())

  if runmode == 'accessibilities':

    print("calculate mode utilities")

    #read mode uec
    mode_table = pd.read_csv(inputsFolder + modeUEC)
    
    #read all data into db
    db = dict()
    for index, row in mode_table.iterrows():
        if not pd.isnull(row['Files']):
            filesToRead = row['Files'].split(",")
            for fileToRead in filesToRead:

                if fileToRead.find(".zmx") > 0:
                    name = fileToRead.replace(".zmx","")
                    if name not in db:
                        addMatToDb(db, inputsFolder + fileToRead)
                if fileToRead.find(".csv") > 0:
                    name = fileToRead.replace(".csv","")
                    if name not in db: 
                        db[name] = pd.read_csv(inputsFolder + fileToRead, index_col=0)
                        print("read csv: " + name)

    #read maz data
    mazs = pd.read_csv(inputsFolder + mazData, index_col=0)
    mazneartaps = pd.read_csv(inputsFolder + nearTaps, index_col=0)
    mazs = pd.merge(mazs, mazneartaps, left_index=True, right_index=True)

    #get matrix names
    tazNames = addMatToDb(db, inputsFolder + "mf3175.zmx")
    tapNames = addMatToDb(db, inputsFolder + "mf3431.zmx")
    mazs['tazindex']  = tazNames['index'].loc[mazs['zone09']].tolist()
    mazs['wttapindex']  = tapNames['index'].loc[mazs['wt_tap']].tolist()
    mazs['mazindex']  = range(len(mazs.index))

    #create maz level lookups using array indexes, not zone labels
    o_m = np.repeat(mazs['mazindex'].tolist(),len(mazs.index))
    d_m = np.tile(mazs['mazindex'].tolist(),len(mazs.index))
    o_t = np.repeat(mazs['tazindex'].tolist(),len(mazs.index))
    d_t = np.tile(mazs['tazindex'].tolist(),len(mazs.index))
    o_wt = np.repeat(mazs['wttapindex'].tolist(),len(mazs.index))
    d_wt = np.tile(mazs['wttapindex'].tolist(),len(mazs.index))

    #solve unique expressions
    data = pd.DataFrame()
    fields = mode_table['Expressions'].unique()
    for aField in fields:
        if not pd.isnull(aField):
            print("solve expression: " + aField)
            data[aField] = eval(aField)
    
    #apply to purpose, period markets for all modes
    modes = mode_table.columns[5:len(mode_table.columns)].tolist()
    purposes = mode_table['Purpose'].unique()
    for purpose in purposes:

        #get purpose records
        purp_recs = mode_table[mode_table['Purpose'] == purpose]

        periods = purp_recs['Period'].unique()
        for period in periods:

            #get period records
            per_recs = purp_recs[purp_recs['Period'] == period]

            for mode in modes:
                
                #get expressions that apply
                per_recs.index = per_recs['Expressions']
                recs = per_recs[mode].dropna()
                if len(recs) > 0:
                    util_name = "u_" + str(purpose) + "_" + period + "_" + mode
                    data[util_name] = 0
                    print("calculate utility: " + util_name)

                    for expr in recs.index.tolist():
                        data[util_name] = data[util_name].add(data[expr].mul(recs[expr]))

                    #replace na
                    data[util_name].fillna(NA, inplace=True)

    print("calculate size terms")

    #read size terms uec
    size_terms = pd.read_csv(inputsFolder + sizeTermsUEC)
    size_terms.index = size_terms['Variable']

    #loop terms and apply coefficients
    sizevars = size_terms.columns[2:len(size_terms.columns)].tolist()
    for i in range(len(sizevars)):

        #create field for variable
        sizevar = sizevars[i]
        mazs[sizevar] = 0
        print("calculate sizevar: " + str(sizevar))

        recs = size_terms[sizevar].dropna()
        for var in recs.index.tolist():
            mazs[sizevar] = mazs[sizevar].add(mazs[var].mul(recs[var]))
    
    print("calculate impedances (i.e. logsums)")

    #read impedances uec
    imp_table = pd.read_csv(inputsFolder + impedanceUEC)
    imp_table.index = imp_table['Impedance']
    modes = imp_table.columns[5:len(imp_table.columns)].tolist()

    #loop modes, calculate logsum
    measures = imp_table['Impedance'].tolist()
    for m in measures:
        
        #run twice if daily
        period = imp_table.loc[m]['Period']
        if period == 'daily':
            periods = ['peak','offpeak']
        else:
            periods = [period]

        for p in periods:
        
            #logsum column
            purpose = imp_table.loc[m]['Purpose']
            lsColName = 'ls_' + str(m) + "_" + p
            data[lsColName] = 0
            print("calculate logsum: " + str(lsColName))

            #mode logsum
            for mode in modes:
                if not pd.isnull(imp_table.loc[m].loc[mode]):
                    colName = 'u_' + str(purpose) + "_" + p + "_" + mode
                    data[lsColName] = data[lsColName].as_matrix() + np.exp(data[colName].as_matrix() / nu )
            data[lsColName] = np.log(data[lsColName].as_matrix()) * nu
            
            #replace inf
            data[lsColName].replace([np.inf, -np.inf], NA, inplace=True)

        #tod logsum, if daily, do both
        if period == 'daily':
            
            #logsum column
            lsColName = 'ls_' + str(m) + "_" + period
            data[lsColName] = 0
            print("calculate logsum: " + str(lsColName))

            pLS = 'ls_' + str(m) + "_peak"
            opLS = 'ls_' + str(m) + "_offpeak"
            data[opLS].add(imp_table.loc[m]['Offpeak']) #offpeak constant
            data[lsColName] = np.log(np.exp(data[pLS].as_matrix() / mu) + np.exp(data[opLS].as_matrix() / mu)) * mu
        else:
            data[lsColName] = np.log(np.exp(data[lsColName].as_matrix() / mu)) * mu
            
        #replace inf
        data[lsColName].replace([np.inf, -np.inf], NA, inplace=True)
  
    #trace utilities and logsums for debugging
    data.iloc[trace_index].to_csv(outputsFolder + "trace.csv")

    print("calculate measures")

    #read measures uec
    meas_table = pd.read_csv(inputsFolder + measuresUEC)
    meas_table.index = meas_table['measure']

    #build multi index to squeeze to Os
    data.index = pd.MultiIndex.from_product([mazs.index,mazs.index])

    measures = meas_table['measure'].tolist()
    columnsToWrite = []
    for m in measures:
        size = meas_table.loc[m]['size']
        imp = meas_table.loc[m]['impedance']
        period = meas_table.loc[m]['period']
        
        #create field
        fieldName = meas_table.loc[m]['model']
        mazs[fieldName] = 0
        columnsToWrite.append(fieldName)
        print("calculate accessibility: " + fieldName)

        #measure = log( sum_over_dests[ size * exp(logsum) ] )
        d_size = np.tile(mazs['p' + str(size)].tolist(),len(mazs.index))
        fName = 'ls_' + str(imp) + '_' + period
        data['result'] = d_size * np.exp(data[fName].as_matrix())
        grouped = data['result'].groupby(level=0)
        mazs[fieldName] = grouped.sum().tolist()
        mazs[fieldName] = np.log(mazs[fieldName].as_matrix())

        #replace infinity and negatives
        mazs[fieldName].replace([np.inf, -np.inf], 0, inplace=True)

    #write result
    mazs.to_csv(outputsFolder + mazAccessibilities, columns=columnsToWrite)

    print("end run mode " + runmode + " : " + time.ctime())
