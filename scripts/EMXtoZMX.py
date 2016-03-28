# Read/write EMME matrices from/to zmx files
# Ben Stabler, stabler@pbworld.com, 11/11/12
# Revised by Noel Peterson, 3/28/16
# Arguments: emme_project scenario mat_1<.zmx> mat_2<.zmx> mat_X<.zmx>
#
# Example call to write zmxs:
#   python.exe EMXtoZMX.py "JEMnR.emp" 1000 mf1 mf2
#
# Example call to read zmxs:
#   python.exe EMXtoZMX.py "JEMnR.emp" 1000 mf1.zmx mf2.zmx
###############################################################################

# Load libraries
import inro.modeller as m
import inro.emme.desktop.app as d
import inro.emme.matrix as _matrix
import numpy as np
import sys, os.path, zipfile, struct, array


def writeZMX(fileName, zoneNames, mat):
    ''' Write ZMX file from raw data array. '''

    # Calculate numZones to remove trailing zeros
    numZones = len(zoneNames)

    # Write header files
    z = zipfile.ZipFile(fileName, "w")
    z.writestr("_version", str(2))
    z.writestr("_description", fileName)
    z.writestr("_name", os.path.splitext(os.path.basename(fileName))[0])
    z.writestr("_external column numbers", ",".join(zoneNames))
    z.writestr("_external row numbers", ",".join(zoneNames))
    z.writestr("_columns", str(numZones))
    z.writestr("_rows", str(numZones))

    # Write rows, trim unused zones, big-endian floats
    for i in range(1, numZones+1):
        fileNameRow = "row_" + str(i)
        data = struct.pack(">" + "f"*numZones, *mat[i-1][0:numZones])
        z.writestr(fileNameRow, data)

    # Close connections
    z.close()


def readZMX(fileName):
    ''' Read ZMX file to raw data array. '''

    # Read header files
    z = zipfile.ZipFile(fileName, "r")
    version = z.read("_version")
    description = z.read("_description")
    name = z.read("_name")
    zoneNames = z.read("_external column numbers")
    rowZoneNames = z.read("_external row numbers")
    columns = z.read("_columns")
    rows = int(z.read("_rows"))

    # Read rows, big-endian floats
    mat = []
    for i in range(1, rows+1):
        fileNameRow = "row_" + str(i)
        data = z.read(fileNameRow)
        mat.append(struct.unpack(">" + "f"*rows, data))

    # Close connections
    z.close()

    # Return matrix data, zone names, matrix name
    return(mat, zoneNames, name)


def getMf(bank, matName, scenarioNum):
    ''' Get Emme matrix raw data array and zone name list. '''

    # Get matrix as array of floats (dimensioned to num max bank centroids)
    mat = bank.matrix(matName).raw_data

    # Get centroid numbers for scenario for matrix trailing zeros
    centroids = bank.scenario(scenarioNum).get_network().centroids()
    names = [i.id for i in centroids]

    # Return matrix data and zone names
    return(mat, names)


def setMf(bank, mat, MatrixName):
    ''' Write raw data array to Emme matrix. '''

    # Get matrix dimensions
    maxCents = bank.dimensions["centroids"]

    # Convert to array, pad zeros by adding columns then rows
    numZones = len(mat[0])
    missingZones = maxCents - numZones
    for i in range(numZones):
        mat[i] = array.array("f", mat[i])
        mat[i].extend([0] * missingZones)
    for i in range(missingZones):
        mat.append(array.array("f", [0] * maxCents))

    # Write data to emmebank
    bank.matrix(MatrixName).raw_data = mat
    return None


def avgMf(bank, matName1, matName2, scenarioNum):
    ''' Calculate the average OD values from two full matrices, and return them
        in the same "raw_data" format returned by getMf(). '''

    # Get matrices as numpy arrays
    mat1 = bank.matrix(matName1).get_data(scenarioNum).to_numpy()
    mat2 = bank.matrix(matName2).get_data(scenarioNum).to_numpy()

    # Calculate average cell values
    avg = np.mean(np.array([mat1, mat2]), axis=0)

    # Load averages into a new MatrixData object (NOT saved to emmebank)
    avgmat = _matrix.MatrixData([bank.scenario(scenarioNum).zone_numbers] * 2)
    avgmat.from_numpy(avg)
    mat = avgmat.raw_data

    # Get centroid numbers for scenario for matrix trailing zeros
    centroids = bank.scenario(scenarioNum).get_network().centroids()
    names = [i.id for i in centroids]

    # Return matrix data and zone names
    return (mat, names)


###############################################################################

# Run command line version
if __name__ == "__main__":

    # Start Emme desktop and attach a Modeller session
    empFile = sys.argv[1]
    scenarioNum = sys.argv[2]
    desktop = d.start_dedicated(False, "bts", empFile)
    m = m.Modeller(desktop)

    # Set export/import mode based on presence of ZMX extension
    if sys.argv[3].lower().find("zmx") < 0:
        export = True
    else:
        export = False

    # Get location of EMX files
    emxFolder = os.path.dirname(m.emmebank.path) + "\\emmemat"

    # Get matrix names from command line argument
    for i in range(3, len(sys.argv)):
        matName = sys.argv[i]

        # EXPORT MODE
        if export:

            # Write ZMX file
            mat, names = getMf(m.emmebank, matName, scenarioNum)
            fileName = emxFolder + "\\" + matName + ".zmx"
            writeZMX(fileName, names, mat)
            print(emxFolder + "\\" + matName + ".emx -> " + fileName)

        # IMPORT MODE
        else:

            # Read ZMX file from EMX folder and write to emmebank
            mat, zoneNames, name = readZMX(emxFolder + "\\" + matName)
            setMf(m.emmebank, mat, name)
            print(emxFolder + "\\" + matName + " -> " + emxFolder + "\\" + name + ".emx")
