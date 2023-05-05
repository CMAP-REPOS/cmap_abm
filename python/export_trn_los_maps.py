import os
import inro.modeller as _m
import inro.emme.desktop.app as _app
import inro.emme.desktop.worksheet as _worksheet

DIR = os.environ["BASE_PATH"] + os.sep + "emme" + os.sep + "CMAP-ABM"
RECIP_PROJECT = os.environ["PROJECT"]
desktop = _app.start_dedicated(project=RECIP_PROJECT, visible=True, user_initials="TL")
modeller = _m.Modeller(desktop)
databank = desktop.data_explorer().active_database().core_emmebank
change_scenario = _m.Modeller().tool("inro.emme.data.scenario.change_primary_scenario")

TSCENS = [203]#[201,203,205,207]
period = ["AM"]#["NT", "AM", "MD", "PM"]
print('Exporting transit LOS maps')
for scen, per in zip(TSCENS,period): 
    change_scenario(scenario=scen)
    #root = desktop.root_worksheet_folder()
    time_skim = ["TOTALIVTT", "CTARAILIVTT", "METRARAILIVTT", "ACC", "EGR"]
    fare_skim = ["FARE"]
    xfer_skim = ["XFERS"]
    access_mode = ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]
    user_class = ["L", "M", "H"]
    time_matrix_list = []
    fare_matrix_list = []
    xfer_matrix_list = []
    for amode in access_mode:
        for uc in user_class:
            for time in time_skim:
                time_matrix_list.append(time + "_" + amode + "_" + uc + "__" + per)
            for fare in fare_skim:
                fare_matrix_list.append(fare + "_" + amode + "_" + uc + "__" + per)
            for xfer in xfer_skim:
                xfer_matrix_list.append(xfer + "_" + amode + "_" + uc + "__" + per)     

    ws_dir = DIR + os.sep + "Worksheets\\Travel_time_contour.emw"
    ws = desktop.open_worksheet(ws_dir)
    #view_dir = DIR + os.sep + "Views\\Region.emv"
    root_view_f = desktop.root_view_folder()
    region_view = root_view_f.find_item(["Region"])
    #cta_rail_view = root_view_f.find_item(["CTA_Rail"])
    _worksheet.set_worksheet_view(ws, region_view)
    OD_layer = ws.layer(layer_name="O-D pair values")

    for matrix in time_matrix_list:
        OD_layer.par("Value").set("mf" + matrix)
        #print(OD_layer.par("Value").get())
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
        #print(OD_layer.par("Value").get())
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
        #print(OD_layer.par("Value").get())
        legend_layer = ws.layer(layer_name="Legend")
        title = "Scenario %<$ScenarioNumber>% \nTime To %<SR:SelectedNode>% \n" + matrix
        legend_layer.par("TextString").set(title)
        ws_export = DIR + os.sep + "Media" + os.sep + matrix + "_contour.pdf"
        ws.save_as_pdf(ws_export)      
print('Finished exporting maps')
