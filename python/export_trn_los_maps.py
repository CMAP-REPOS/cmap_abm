import os
import inro.modeller as _m
import inro.emme.desktop.app as _app
import inro.emme.desktop.worksheet as _worksheet

DIR = os.environ["BASE_PATH"] + os.sep + "CMAP-ABM"
RECIP_PROJECT = os.environ["PROJECT"]
desktop = _app.start_dedicated(project=RECIP_PROJECT, visible=True, user_initials="ASR")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank
change_scenario = _m.Modeller().tool("inro.emme.data.scenario.change_primary_scenario")

#data_explorer = desktop.data_explorer()
#db = data_explorer.active_database()
#sc = db.scenario_by_number(scenario.number)
#data_explorer.replace_primary_scenario(sc)

TSCENS = [203]#[201,203,205,207]
#for scen in TSCENS: 
#    change_scenario(scenario=scen)
#root = desktop.root_worksheet_folder()
time_skim = ["TOTALIVTT", "CTARAILIVTT", "METRARAILIVTT", "ACC", "EGR"]
fare_skim = ["FARE"]
xfer_skim = ["XFERS"]
access_mode = ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]
user_class = ["L", "M", "H"]
period = ["AM"] #["NT", "AM", "MD", "PM"]
time_matrix_list = []
fare_matrix_list = []
xfer_matrix_list = []
for amode in access_mode:
    for uc in user_class:
        for per in period:
            for time in time_skim:
                time_matrix_list.append(time + "_" + amode + "_" + uc + "__" + per)
            for fare in fare_skim:
                fare_matrix_list.append(fare + "_" + amode + "_" + uc + "__" + per)
            for xfer in xfer_skim:
                xfer_matrix_list.append(xfer + "_" + amode + "_" + uc + "__" + per)     

ws_dir = DIR + os.sep + "Worksheets\\Travel_time_contour.emw"
ws = desktop.open_worksheet(ws_dir)
# TODO make ws template for fare and xfer
#view_dir = DIR + os.sep + "Views\\Region.emv"
root_view_f = desktop.root_view_folder()
region_view = root_view_f.find_item(["Region"])
#cta_rail_view = root_view_f.find_item(["CTA_Rail"])
_worksheet.set_worksheet_view(ws, region_view)
OD_layer = ws.layer(layer_name="O-D pair values")
'''
style_legend = OD_layer.style_legend()
print(style_legend.use_breaks.get())
print(style_legend.style.get())
style_list = style_legend.style.listval
print(len(style_list))
'''
for matrix in time_matrix_list:
    OD_layer.par("Value").set("mf" + matrix)
    print(OD_layer.par("Value").get())
    cta_rail_layer = ws.layer(layer_name="CTA Rail Lines")
    if "METRARAIL" in matrix:
        cta_rail_layer.par("SFlag").set(False)
    else:
        cta_rail_layer.par("SFlag").set(True)
    metra_rail_layer = ws.layer(layer_name="Metra Rail Lines")
    if "CTARAIL" in matrix:        
        metra_rail_layer.par("SFlag").set(False)
    else:
        metra_rail_layer.par("SFlag").set(True)
    legend_layer = ws.layer(layer_name="Legend")
    title = "Scenario %<$ScenarioNumber>% \nTime To %<SR:SelectedNode>% \n" + matrix
    legend_layer.par("TextString").set(title)
    ws_export = DIR + os.sep + "Media" + os.sep + matrix + "_contour.pdf"
    ws.save_as_pdf(ws_export)

ws_dir = DIR + os.sep + "Worksheets\\Fare_contour.emw"
ws = desktop.open_worksheet(ws_dir)
root_view_f = desktop.root_view_folder()
region_view = root_view_f.find_item(["Region"])
_worksheet.set_worksheet_view(ws, region_view)
OD_layer = ws.layer(layer_name="O-D pair values")    
for matrix in fare_matrix_list:
    OD_layer.par("Value").set("mf" + matrix)
    print(OD_layer.par("Value").get())
    legend_layer = ws.layer(layer_name="Legend")
    title = "Scenario %<$ScenarioNumber>% \nTime To %<SR:SelectedNode>% \n" + matrix
    legend_layer.par("TextString").set(title)
    ws_export = DIR + os.sep + "Media" + os.sep + matrix + "_contour.pdf"
    ws.save_as_pdf(ws_export)  

ws_dir = DIR + os.sep + "Worksheets\\Xfer_contour.emw"
ws = desktop.open_worksheet(ws_dir)
root_view_f = desktop.root_view_folder()
region_view = root_view_f.find_item(["Region"])
_worksheet.set_worksheet_view(ws, region_view)
OD_layer = ws.layer(layer_name="O-D pair values")
for matrix in xfer_matrix_list:
    OD_layer.par("Value").set("mf" + matrix + "-(mfTOTALIVTT" + matrix[5:] + "==0)*100")
    print(OD_layer.par("Value").get())
    legend_layer = ws.layer(layer_name="Legend")
    title = "Scenario %<$ScenarioNumber>% \nTime To %<SR:SelectedNode>% \n" + matrix
    legend_layer.par("TextString").set(title)
    ws_export = DIR + os.sep + "Media" + os.sep + matrix + "_contour.pdf"
    ws.save_as_pdf(ws_export)      
print('Finished exporting maps')
'''
matrix_list = ["TOTALIVTT_WALK_L_AM", "TOTALIVTT_PNROUT_L_AM", "TOTALIVTT_PNRIN_L_AM", 
                "TOTALIVTT_KNROUT_L_AM", "TOTALIVTT_KNRIN_L_AM",
                "TOTALIVTT_WALK_M_AM", "TOTALIVTT_PNROUT_M_AM", "TOTALIVTT_PNRIN_M_AM", 
                "TOTALIVTT_KNROUT_M_AM", "TOTALIVTT_KNRIN_M_AM",
                "TOTALIVTT_WALK_H_AM", "TOTALIVTT_PNROUT_H_AM", "TOTALIVTT_PNRIN_H_AM", 
                "TOTALIVTT_KNROUT_H_AM", "TOTALIVTT_KNRIN_H_AM",
                "CTARAILIVTT_WALK_L_AM", "CTARAILIVTT_PNROUT_L_AM", "CTARAILIVTT_PNRIN_L_AM", 
                "CTARAILIVTT_KNROUT_L_AM", "CTARAILIVTT_KNRIN_L_AM",
                "CTARAILIVTT_WALK_M_AM", "CTARAILIVTT_PNROUT_M_AM", "CTARAILIVTT_PNRIN_M_AM", 
                "CTARAILIVTT_KNROUT_M_AM", "CTARAILIVTT_KNRIN_M_AM",
                "CTARAILIVTT_WALK_H_AM", "CTARAILIVTT_PNROUT_H_AM", "CTARAILIVTT_PNRIN_H_AM", 
                "CTARAILIVTT_KNROUT_H_AM", "CTARAILIVTT_KNRIN_H_AM",
                "METRARAILIVTT_WALK_L_AM", "METRARAILIVTT_PNROUT_L_AM", "METRARAILIVTT_PNRIN_L_AM", 
                "METRARAILIVTT_KNROUT_L_AM", "METRARAILIVTT_KNRIN_L_AM",
                "METRARAILIVTT_WALK_M_AM", "METRARAILIVTT_PNROUT_M_AM", "METRARAILIVTT_PNRIN_M_AM", 
                "METRARAILIVTT_KNROUT_M_AM", "METRARAILIVTT_KNRIN_M_AM",
                "METRARAILIVTT_WALK_H_AM", "METRARAILIVTT_PNROUT_H_AM", "METRARAILIVTT_PNRIN_H_AM", 
                "METRARAILIVTT_KNROUT_H_AM", "METRARAILIVTT_KNRIN_H_AM",
                "ACC_WALK_L_AM", "ACC_PNROUT_L_AM", "ACC_PNRIN_L_AM", 
                "ACC_KNROUT_L_AM", "ACC_KNRIN_L_AM",
                "ACC_WALK_M_AM", "ACC_PNROUT_M_AM", "ACC_PNRIN_M_AM", 
                "ACC_KNROUT_M_AM", "ACC_KNRIN_M_AM",
                "ACC_WALK_H_AM", "ACC_PNROUT_H_AM", "ACC_PNRIN_H_AM", 
                "ACC_KNROUT_H_AM", "ACC_KNRIN_H_AM",
                "EGR_WALK_L_AM", "EGR_PNROUT_L_AM", "EGR_PNRIN_L_AM", 
                "EGR_KNROUT_L_AM", "EGR_KNRIN_L_AM",
                "EGR_WALK_M_AM", "EGR_PNROUT_M_AM", "EGR_PNRIN_M_AM", 
                "EGR_KNROUT_M_AM", "EGR_KNRIN_M_AM",
                "EGR_WALK_H_AM", "EGR_PNROUT_H_AM", "EGR_PNRIN_H_AM", 
                "EGR_KNROUT_H_AM", "EGR_KNRIN_H_AM"]

                "FARE_WALK_L_AM", "FARE_PNROUT_L_AM", "FARE_PNRIN_L_AM", 
                "FARE_KNROUT_L_AM", "FARE_KNRIN_L_AM",
                "FARE_WALK_M_AM", "FARE_PNROUT_M_AM", "FARE_PNRIN_M_AM", 
                "FARE_KNROUT_M_AM", "FARE_KNRIN_M_AM",
                "FARE_WALK_H_AM", "FARE_PNROUT_H_AM", "FARE_PNRIN_H_AM", 
                "FARE_KNROUT_H_AM", "FARE_KNRIN_H_AM",
                "XFERS_WALK_L_AM", "XFERS_PNROUT_L_AM", "XFERS_PNRIN_L_AM", 
                "XFERS_KNROUT_L_AM", "XFERS_KNRIN_L_AM",
                "XFERS_WALK_M_AM", "XFERS_PNROUT_M_AM", "XFERS_PNRIN_M_AM", 
                "XFERS_KNROUT_M_AM", "XFERS_KNRIN_M_AM",
                "XFERS_WALK_H_AM", "XFERS_PNROUT_H_AM", "XFERS_PNRIN_H_AM", 
                "XFERS_KNROUT_H_AM", "XFERS_KNRIN_H_AM",
'''
#for i in desktop.windows():
    #print(i.filename())
