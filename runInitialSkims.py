#############################################################################
# Run Initial Skimming
# Ben Stabler, stabler@pbworld.com, 02/18/13
# "C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.3\Python26\python.exe" runInitialSkims.py 1,1,1,1,1,1,1,1
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
		
		##########################################################################
		# HIGHWAY
		##########################################################################
		
		if not runTransitOnly:
		
			#highway skimming
			#%1%       /0 - initialize split matrices mf131-mf174, 1 - start with the previous set 
			#%2%       /MSA factor for averaging matrices 0-1 (0-no update, 1.0 full update)
			#%3%       /0 - skip final assign., 1 - implement final assign. (last global iter.)
			#%4%       /base scenario for assignment (9999 - for skimming)
			#%5%       /number of assignment iterations
			#%6%       /0 - include auto split, 1 - exclude (when applied with CT-RAMP)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 0, 1.00, 0, scen, 25, 0), m.emmebank.path, scen)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 0.75, 0, scen, 50, 0), m.emmebank.path, scen)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 0.50, 0, scen, 75, 0), m.emmebank.path, scen)
			p.run_macro("~< %s %i %f %i %i %i %i" % (hwySkimMacro, 1, 0.25, 0, scen, 100, 0), m.emmebank.path, scen)
		
			#convert TAZ emx skim matrices to zmx for CT-RAMP
			hwy_skims = []
			mfs_tod = []
			hwy_skims.extend(["mf%i175","mf%i176","mf%i177","mf%i179"])           #sov
			hwy_skims.extend(["mf%i185","mf%i186","mf%i187","mf%i189"])           #hov
			hwy_skims.extend(["mf%i195","mf%i196","mf%i197","mf%i199"])           #hov3
			hwy_skims.extend(["mf%i180","mf%i181","mf%i182","mf%i183","mf%i184"]) #sov pay
			hwy_skims.extend(["mf%i190","mf%i191","mf%i192","mf%i193","mf%i194"]) #hov pay
			hwy_skims.extend(["mf%i200","mf%i201","mf%i202","mf%i203","mf%i204"]) #hov3 pay
			for aMat in hwy_skims:
			  mfs_tod.append(m.emmebank.matrix(aMat % (tod)).id)
	
			#get matrices and zone names and write to ZMX format
			for mf in mfs_tod:
				mat, zoneNames = EMXtoZMX.getMf(m.emmebank, mf, scen)
				EMXtoZMX.writeZMX(emxFolder + "\\" + mf + ".zmx", zoneNames, mat)	
	
		##########################################################################
		# TRANSIT
		##########################################################################

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
		p.run_macro("~< %s %i %i %i %i %i %i %i" % (transitSkimMacro, transitImport, scen, trnAssignIters, mfConvDem, mfPremDem, int(bypassConventionalTransit), 0), m.emmebank.path, tranScen)
	
		#convert TAP emx skim matrices to zmx for CT-RAMP
		mfs_tod = []
		if not bypassConventionalTransit:
			cgen_c1 = m.emmebank.matrix("Cgen%i%i" % (tod, 1)).id
			cgen_c2 = m.emmebank.matrix("Cgen%i%i" % (tod, 2)).id
			cgen_c3 = m.emmebank.matrix("Cgen%i%i" % (tod, 3)).id
			mfs_tod.extend([cgen_c1,cgen_c2,cgen_c3])
		pgen_c1 = m.emmebank.matrix("Pgen%i%i" % (tod, 1)).id
		pgen_c2 = m.emmebank.matrix("Pgen%i%i" % (tod, 2)).id
		pgen_c3 = m.emmebank.matrix("Pgen%i%i" % (tod, 3)).id
		mfs_tod.extend([pgen_c1,pgen_c2,pgen_c3])
		
		#get matrices and zone names and write to ZMX format
		for mf in mfs_tod:
			mat, zoneNames = EMXtoZMX.getMf(m.emmebank, mf, tranScen)
			EMXtoZMX.writeZMX(emxFolder + "\\" + mf + ".zmx", zoneNames, mat)	
	
		#log results
		print("Time-of-day %i Complete %s" % (tod, datetime.datetime.now()))
	
