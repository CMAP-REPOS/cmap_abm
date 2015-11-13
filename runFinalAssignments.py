#############################################################################
# Run Final Assignments
# Ben Stabler, stabler@pbworld.com, 02/18/13
# "C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.4\Python26\python.exe" runFinalAssignments.py 1,1,1,1,1,1,1,1
#############################################################################

#EMME project file
empFile = "CMAP-ABM/CMAP-ABM.emp"

#scenarios
highwayScenarios = [1,2,3,4,5,6,7,8]
transitScenarios = [101,102,103,104,105,106,107,108]
tods = [1,2,3,4,5,6,7,8]

#settings
runTransitOnly = False
transitImport = 100
trnAssignIters = 3
matNumConvDemand = 467
matNumPremDemand = 468
bypassConventionalTransit = True

#macros - relative to the databank location
hwySkimMacro = "../../scripts/CT_RAMP_skim3.mac"
transitSkimMacro = "../../scripts/Transit_assignment_skimming_CT_RAMP3.mac"

############################################################################

#load libraries
import inro.modeller as m
import inro.emme.desktop.app as d
import inro.emme.prompt as p
import os, csv, math, pickle, datetime, sys
from scripts import EMXtoZMX

#start EMME desktop and attach a modeller session
desktop = d.start_dedicated(True, "bts", empFile) 
m = m.Modeller(desktop)

#location to write matrices
emxFolder = os.path.dirname(m.emmebank.path) + "\\emmemat"

#get time periods to run, item 2 
#1,1,0,0,0,0,0,0 #for example runs periods 1 and 2
runPeriods = map(int, sys.argv[1].split(","))

############################################################################

#loop by time-of-day
for i in range(len(tods)):
	
	#run only specified periods
	runPeriod = runPeriods[i]
	if runPeriod:
		scen = highwayScenarios[i]
		tranScen = transitScenarios[i]
		tod = tods[i]
	
		#read taz and tap demand matrices and write to bank
		matNames = ["mf%i123","mf%i124","mf%i125","mf%i126","mf%i127","mf%i128","mf%i129","mf%i130","mf%i131","mf%i132","mf%i133","mf%i134"] #taz
		mfs_tod = []
		for aMat in matNames:
		  mfs_tod.append(aMat % (tod))
	
		for mf in mfs_tod:
			mat, zoneNames, name = EMXtoZMX.readZMX("outputs\\" + mf + ".zmx")
			EMXtoZMX.setMf(m.emmebank, mat, name)
			print("Load highway matrix into databank %s" % (name))
		
		if not runTransitOnly:
			#highwy assign and skim with new demand
			#%1%       /0 - initialize split matrices mf131-mf174, 1 - start with the previous set 
			#%2%       /MSA factor for averaging matrices 0-1 (0-no update, 1.0 full update)
			#%3%       /0 - skip final assign., 1 - implement final assign. (last global iter.)
			#%4%       /base scenario for assignment (9999 - for skimming)
			#%5%       /number of assignment iterations
			#%6%       /0 - include auto split, 1 - exclude (when applied with CT-RAMP)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 1.00, 0, scen, 25, 1), m.emmebank.path, scen)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 0.75, 0, scen, 50, 1), m.emmebank.path, scen)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 0.50, 0, scen, 75, 1), m.emmebank.path, scen)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 0.25, 1, scen, 100, 1), m.emmebank.path, scen)
		
		#read taz and tap demand matrices and write to bank
		matNames = ["mf%i271","mf%i272","mf%i273","mf%i274","mf%i275","mf%i276"] #tap
		mfs_tod = []
		for aMat in matNames:
		  mfs_tod.append(aMat % (tod))
	
		for mf in mfs_tod:
			mat, zoneNames, name = EMXtoZMX.readZMX("outputs\\" + mf + ".zmx")
			EMXtoZMX.setMf(m.emmebank, mat, name)
			print("Load transit matrix into databank %s" % (name))
		
		#transit skimming	
		#%1%       /transit scenario 100 for base year 
		#%2%       /time-of-day period and highway network 1-8 
		#%3%       /number of iterations for transit assignment equilibration
		#%4%       /matrix number for conventional transit demand for initial assignment
		#%5%       /matrix number for premium transit demand for initial assignment
		#%6%       /1=bypass conventional transit assignment and skims; 0=skim both conv and prem
		#%7%       /1=Bypass create matrix segmentation by class; 0=create demand matrices by class
		mfConvDem = tod*1000 + matNumConvDemand
		mfPremDem = tod*1000 + matNumPremDemand
		p.run_macro("~< %s %i %i %i %i %i %i %i" % (transitSkimMacro, transitImport, scen, trnAssignIters, mfConvDem, mfPremDem, int(bypassConventionalTransit), 1), m.emmebank.path, tranScen)
		
		#log results
		print("Time-of-day %i Complete %s" % (tod, datetime.datetime.now()))

