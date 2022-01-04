import inro.emme.desktop.app as _app
import inro.modeller as _m
import cmap_transit_assignment_v3 as cmap_transit_assignment
#import network.cmap_network as cmap_network
import sys

#desktop = _app.start_dedicated(project="D:/Projects/Clients/CMAP/Model/New Skims/CMAP-ABM_TB/CMAP-ABM.emp", visible=True, user_initials="NSD") #old model
desktop = _app.start_dedicated(project="D:/Projects/Clients/CMAP/Model/onto2050_2019/cmap_abm/CMAP-ABM/CMAP-ABM.emp", visible=True, user_initials="ASR") #new model
modeller = _m.Modeller(desktop)
my_emmebank = modeller.emmebank
databank = desktop.data_explorer().active_database().core_emmebank

#directory = os.getcwd().replace('\\Database','')
#empFile = os.path.join(directory,"CMAP-ABM.emp")
#my_app = _app.start_dedicated(False, "cmap", empFile)
#my_modeller = _m.Modeller(my_app)
#my_emmebank = my_modeller.emmebank

#transit
tods = [1,2,3,4,5,6,7,8]
#tods = [1] #for testing only
#transitImport = 100    #old model
transitImport = 200   #new model
transitScenarios = [transitImport + i for i in tods]

data_explorer = desktop.data_explorer()
database = data_explorer.active_database()
matrix_count = 0
for i in range(len(tods)):
    print("time period: " + str(tods[i]) + " and new matrix count: " + str(matrix_count) +  "...")
    transitScenario = transitScenarios[i]
    current_scenario = my_emmebank.scenario(transitScenario)
    tranScen = database.scenario_by_number(transitScenario)
    data_explorer.replace_primary_scenario(tranScen)

    matrix_count = cmap_transit_assignment.TransitAssignment().__call__(str(tods[i]), matrix_count, current_scenario)

print('Finished!')
