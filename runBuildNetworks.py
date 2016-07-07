#############################################################################
# Build CMAP ABM Databank
# Ben Stabler, stabler@pbworld.com, 02/18/13
# "C:\Program Files (x86)\INRO\Emme\Emme 4\Emme-4.0.3\Python26\python.exe" runBuildNetworks.py 1,1,1,1,1,1,1,1 100
#############################################################################

#create a new emme 4 project with the following defaults for license 24 except:
    #17 scenarios
    #40 transit vehicles
    #9999 mf, mo, md (1-1000 for generic, 1000s for tod 1, 2000s for tod 2, etc)
    #50m extra attribute size
    #0.000189 user coordinate units
    #Then in EMME desktop, add mode n (since it is not used) or Modeller won't work

#############################################################################

#EMME project file
empFile = "CMAP-ABM/CMAP-ABM.emp"

#scenarios
highwayScenarios = [1,2,3,4,5,6,7,8]
transitScenarios = [101,102,103,104,105,106,107,108]
tods = [1,2,3,4,5,6,7,8]

#settings
runTransitOnly = False
transitImport = int(sys.argv[2])  #100
previousBank = "inputs/emmebank" #previous bank with hwy matrices 1-99
matNumConvDemand = 467
matNumPremDemand = 468

#macros - relative to the databank location
hwySetupMacro = "../../scripts/hwySetup.mac"
hwyMatImportMacro = "../../scripts/HwayMatIn.mac"
todTablesMacro = "../../scripts/TOD_tables3.mac"
extraAttrsMacro = "../../scripts/extraclass.mac"
tollAttrMacro = "../../scripts/addtoll.mac"
transitSetupMacro = "../../scripts/Build_TOD_Transit_CT_RAMP3.mac"
transitMatImportMacro = "../../scripts/TranMatIn.mac"

#tap files
tapFile = "inputs/tap_attributes.csv"
tapLinks = "inputs/tap2node.csv"

############################################################################

#load libraries
import inro.modeller as m
import inro.emme.desktop.app as d
import inro.emme.prompt as p
import os, csv, math, pickle, datetime, sys
from scripts import EMXtoZMX

#start EMME desktop and attach a modeller session
desktop = d.start_dedicated(True, "cmap", empFile)
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

            #create scenario if needed (scenario 1 created when creating bank)
            if m.emmebank.scenario(scen) == None:
                m.emmebank.create_scenario(scen)
            else:
                m.emmebank.delete_scenario(scen)
                m.emmebank.create_scenario(scen)

            #modes, functions, highway network
            #%1%       /tod
            p.run_macro("~< %s %i" % (hwySetupMacro, tod), m.emmebank.path, scen)

            #input daily highway matrices 1-99
            #%1%       /previous bank with matrices
            if i==0:
                p.run_macro("~< %s %s" % (hwyMatImportMacro, os.getcwd() + "\\" + previousBank), m.emmebank.path, scen)

            #tod tables, extra attributes, and toll field (which is currently not by tod)
            #%1%       /tod
            p.run_macro("~< %s %i" % (todTablesMacro, tod), m.emmebank.path, scen)
            p.run_macro("~< %s %i" % (extraAttrsMacro, tod), m.emmebank.path, scen)
            p.run_macro("~< %s %i" % (tollAttrMacro, tod), m.emmebank.path, scen)

        ##########################################################################
        # TRANSIT
        ##########################################################################

        #create transit scenario if needed
        if m.emmebank.scenario(tranScen) == None:
            m.emmebank.create_scenario(tranScen)
        else:
            m.emmebank.delete_scenario(tranScen)
            m.emmebank.create_scenario(tranScen)

        #create transit network
        #%1%       /transit import scenario (100)
        #%2%       /hwy scenario and time-of-day for transit run times
        #%3%       /inputs folder
        p.run_macro("~< %s %i %i %s" % (transitSetupMacro, transitImport, scen, os.getcwd() + "\\inputs"), m.emmebank.path, tranScen)

        #switch from TAZ to TAPs
        #########################

        #get network for scenario
        network = m.emmebank.scenario(tranScen).get_network()

        #delete taz nodes and links
        centroids = network.centroids()
        for cent in centroids:
            network.delete_node(cent.id, cascade=True) #delete links too

        #create tap virtual zones
        taps = []
        with open(tapFile, 'rb') as csvfile:
            tapreader = csv.reader(csvfile, skipinitialspace=True)
            for row in tapreader:
                taps.append(row)
        taps_col_names = taps.pop(0)

        for tap in taps:
            cent = network.create_centroid(tap[0])
            cent.x = int(tap[1])
            cent.y = int(tap[2])

        #create access links
        access_links = []
        with open(tapLinks, 'rb') as csvfile:
            tapreader = csv.reader(csvfile, skipinitialspace=True)
            for row in tapreader:
                access_links.append(row)
        access_links_col_names = access_links.pop(0)

        acces_modes = ["a"]
        egress_modes = ["e"]
        for access_link in access_links:
            if network.node(access_link[1]) != None: #skip if node not found

                #centroid and node x,y
                cx = network.node(access_link[0]).x
                cy = network.node(access_link[0]).y
                nx = network.node(access_link[1]).x
                ny = network.node(access_link[1]).y

                #create link in zone to node direction
                link = network.create_link(access_link[0],access_link[1], acces_modes)
                link.length = 0.001
                link.type = 1
                link.num_lanes = 0
                link.volume_delay_func = 1

                #create link in node to zone direction
                link = network.create_link(access_link[1], access_link[0], egress_modes)
                link.length = 0.001
                link.type = 1
                link.num_lanes = 0
                link.volume_delay_func = 1

        #publish tap network back to bank
        m.emmebank.scenario(tranScen).publish_network(network)

        #input time period transit tap demand matrices
        p.run_macro("~< %s %i" % (transitMatImportMacro, tod), m.emmebank.path, tranScen)

        #log results
        print("Time-of-day %i Complete %s" % (tod, datetime.datetime.now()))


# Add dummy turn for SOLA assignment (quick fix until Emme 4.2.3)
SOLAFixMacro = "../../scripts/quick_update_fix.mac"
p.run_macro("~< {0} {1}".format(SOLAFixMacro, os.getcwd() + "\\inputs"), m.emmebank.path, scen)
