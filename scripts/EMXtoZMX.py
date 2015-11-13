
#Read/write EMME matrices from/to zmx files
#Ben Stabler, stabler@pbworld.com, 11/11/12
#Arguments: emme_project scenario mat_1<.zmx> mat_2<.zmx> mat_X<.zmx>
#Example call to write zmxs: python.exe EMXtoZMX.py "JEMnR.emp" 1000 mf1 mf2
#Example call to read zmxs: python.exe EMXtoZMX.py "JEMnR.emp" 1000 mf1.zmx mf2.zmx
######################################################################

#load libraries
import inro.modeller as m
import inro.emme.desktop.app as d
import sys, os.path, zipfile, struct, array

#write ZMX file
def writeZMX(fileName, zoneNames, mat):

	#calculate numZones to remove trailing zeros
	numZones = len(zoneNames)

	#write header files
	z = zipfile.ZipFile(fileName, "w")
	z.writestr("_version", str(2))
	z.writestr("_description", fileName)
	z.writestr("_name", os.path.splitext(os.path.basename(fileName))[0])
	z.writestr("_external column numbers", ",".join(zoneNames))
	z.writestr("_external row numbers", ",".join(zoneNames))
	z.writestr("_columns", str(numZones))
	z.writestr("_rows", str(numZones))
	
	#write rows, trim unused zones, big-endian floats
	for i in range(1,numZones+1):
		fileNameRow = "row_" + str(i)
		data = struct.pack(">" + "f"*numZones,*mat[i-1][0:numZones])
		z.writestr(fileNameRow, data)

	#close connections
	z.close()

#read ZMX file
def readZMX(fileName):

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

#get matrix and zone names
def getMf(bank, matName, scenarioNum):

	#get matrix as array of floats (dimensioned to num max bank centroids)
	mat = bank.matrix(matName).raw_data

	#get centroid numbers for scenario for matrix trailing zeros
	centroids = bank.scenario(scenarioNum).get_network().centroids()
	names = []
	for i in centroids:
		names.append(i.id)

	#return matrix data and zone names
	return(mat, names)

#set matrix
def setMf(bank, mat, MatrixName):

	#bank max centroids
	maxCents = bank.dimensions["centroids"]

	#convert to array, pad zeros by adding columns then rows
	numZones = len(mat[0])
	missingZones = maxCents - numZones
	for i in range(numZones):
		mat[i] = array.array("f",mat[i])
		mat[i].extend([0]*missingZones)			
	for i in range(missingZones):
		mat.append(array.array("f",[0]*maxCents))
		
	#post matrix to bank
	bank.matrix(MatrixName).raw_data = mat

######################################################################

#run command line version
if __name__ == "__main__":

	#start EMME desktop and attach a modeller session	
	empFile = sys.argv[1]
	scenarioNum = sys.argv[2]
	desktop = d.start_dedicated(False, "bts", empFile)
	m = m.Modeller(desktop)
	
	#determine if export matrices or importing based on presence of zmx extension
	if sys.argv[3].find("zmx") < 0:
		export = True
	else:
		export = False
	
	#get location of emx files
	emxFolder = os.path.dirname(m.emmebank.path) + "\\emmemat"
	
	#get matrix names from command line argument
	for i in range(3,len(sys.argv)):
		matName = sys.argv[i]
	
		#export matrices
		if export:
			
			#write zmx file
			mat, names = getMf(m.emmebank, matName, scenarioNum)
			fileName = emxFolder + "\\" + matName + ".zmx"
			writeZMX(fileName, names, mat)
			print(emxFolder + "\\" + matName + ".emx -> " + fileName)
	
		#else import
		else:
			
			#read zmx file from emx folder and post to bank
			mat, zoneNames, name = readZMX(emxFolder + "\\" + matName)
			setMf(m.emmebank, mat, name)
			print(emxFolder + "\\" + matName + " -> " + emxFolder + "\\" + name + ".emx")
	
