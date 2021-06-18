######################################################################################
# Creating leaflet comparing census tract level auto ownership with model
#
######################################################################################

start_time = Sys.time()

### Read Command Line Arguments
# args                = commandArgs(trailingOnly = TRUE)
# Parameters_File     = args[1]

LOAD_PKGS_LIST = c("leaflet", "htmlwidgets", "rgdal", "rgeos", "raster", "dplyr", 
                    "stringr", "data.table", "tigris", "yaml", "sf")
lib_sink = suppressWarnings(suppressMessages(lapply(LOAD_PKGS_LIST, library, character.only = TRUE)))

settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')

Parameters_File = file.path(settings$visualizer_dir, 'runtime', 'parameters.csv' )


### Read parameters from Parameters_File

parameters              = read.csv(Parameters_File, header = TRUE)
ABM_DIR                 = trimws(paste(parameters$Value[parameters$Key=="ABM_DIR"]))
ABM_SUMMARY_DIR =  file.path(settings$visualizer_dir, 'data', 'calibration_runs', 'summarized')
#ABM_SUMMARY_DIR         = trimws(paste(parameters$Value[parameters$Key=="ABM_SUMMARY_DIR"]))
CENSUS_DIR              = trimws(paste(parameters$Value[parameters$Key=="CENSUS_DIR"]))
ZONES_DIR               = trimws(paste(parameters$Value[parameters$Key=="ZONES_DIR"]))
R_LIBRARY               = trimws(paste(parameters$Value[parameters$Key=="R_LIBRARY"]))
BUILD_SAMPLE_RATE       = trimws(paste(parameters$Value[parameters$Key=="BUILD_SAMPLE_RATE"]))
# CT_ZERO_AUTO_FILE_NAME  = trimws(paste(parameters$Value[parameters$Key=="CT_ZERO_AUTO_FILE_NAME"]))
CT_ZERO_AUTO_FILE_NAME='ct_zero_auto.shp'
SHP_FILE_NAME           = trimws(paste(parameters$Value[parameters$Key=="SHP_FILE_NAME"]))

# INPUTS
########
# CensusData      = "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census/ACS_2017_5yr_MI_CT_AutoOwn.csv"
CensusData      = file.path(settings$visualizer_summaries, 'ACS_2018_5yr_AutoOwn.csv')
# hh_file       = "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/activitySim_400k_hh/final_households.csv"
hh_file         = file.path(settings$SPA_input_dir, 'HH_SPA_INPUT.csv')

tract_to_taz_file = file.path(settings$zone_dir, 'zones17.shp')

hh = read.csv(hh_file)
census = read.csv(CensusData, stringsAsFactors = F)

zones = st_read(tract_to_taz_file)
zones_dt = setDT(copy(zones))
zones_dt[, county_shortfips := substr(county_fip, 3, 5)]
zones_dt[, state_shortfips := substr(county_fip, 1, 2)]
# USe Tigris to get census tract shapefile

il_shp = tracts(state = 17, county = zones_dt[state_shortfips == 17, county_shortfips], class = 'sp')
wi_shp = tracts(state = 55, county = zones_dt[state_shortfips == 55, county_shortfips], class = 'sp')
in_shp = tracts(state = 18, county = zones_dt[state_shortfips == 18, county_shortfips], class = 'sp')

ct_shp = rbind_tigris(il_shp, wi_shp, in_shp)
ct_shp = spTransform(ct_shp, CRS("+proj=longlat +ellps=GRS80"))
zones_proj = st_transform(zones, CRS("+proj=longlat +ellps=GRS80"))

# TAZ xwalk
# not needed, get taz from survey
ct_shp_sf = st_as_sf(ct_shp)
tazXWalk = st_join(zones_proj, ct_shp_sf)
tazXWalk = setDT(tazXWalk)

setwd(ABM_SUMMARY_DIR) # output dir


# Calculating auto availability and merging Census Tract ID

# get weights
wt_dir_cmap = settings$popsim_folder
wt_dir_nirpc = settings$popsim_folder_nirpc
trip_wt_file = file.path(settings$proj_dir, 'underreporting_correction', 'trip_weights.rds')

nirpc_weights = fread(file.path(wt_dir_nirpc, 'output/final_summary_hh_weights.csv'))
cmap_weights = fread(file.path(wt_dir_cmap, 'output/final_summary_hh_weights.csv'))

print('Applying weighting factors...')

weights = rbind(cmap_weights, nirpc_weights)
setDT(hh)

hh[weights, HHEXPFAC := i.SUBREGCluster_balanced_weight, on = .(SAMPN = sampno)]

hh$finalweight = hh$HHEXPFAC
hh$hasZeroAutos = ifelse(hh$HH_VEH_CAT==0, 1, 0)
hh$hasZeroAutosWeighted = hh$hasZeroAutos * hh$finalweight
hh$TAZ = hh$HH_ZONE_ID

# num_hh_per_taz = aggregate(hh$finalweight, by=list(Category=hh$TAZ), FUN=sum)
num_hh_per_ct = aggregate(hh$finalweight, by=list(Category=hh$HH_TRACT_FIPS), FUN=sum)
# zero_auto_hh_by_taz = aggregate(hh$hasZeroAutosWeighted, by=list(Category=hh$TAZ), FUN=sum)
zero_auto_hh_by_CT = aggregate(hh$hasZeroAutosWeighted, by=list(Category=hh$HH_TRACT_FIPS), FUN=sum)

# names(zero_auto_hh_by_taz)[names(zero_auto_hh_by_taz)=="Category"] = "TAZ"
# names(zero_auto_hh_by_taz)[names(zero_auto_hh_by_taz)=="x"] = "ZeroAutoHH"
# 
zero_auto_hh_by_CT$HH = num_hh_per_ct$x
# setDT(zero_auto_hh_by_taz)
# zero_auto_hh_by_taz[tazXWalk, CTIDFP := i.GEOID, on = .(TAZ = zone17)]
# 
# tazXWalk[, county_fips := county_fip]
# 
# zero_auto_hh_by_taz$COUNTYFP = tazXWalk$county_fips[match(zero_auto_hh_by_taz$TAZ, tazXWalk$TAZ)]
# zero_auto_hh_by_taz$COUNTYFP = tazXWalk$county_fips[match(zero_auto_hh_by_taz$TAZ, tazXWalk$TAZ)]
# 
# COUNTYFPs = unique(zero_auto_hh_by_taz$COUNTYFP)
# COUNTYFPs = COUNTYFPs[!is.na(COUNTYFPs)]

zero_auto_hh_by_CT = zero_auto_hh_by_CT %>%
  group_by(CTIDFP= Category) %>%
  #summarise(HH = sum(zero_auto_hh_by_taz$HH)) %>%
  #summarise(ZeroAutoHH = sum(zero_auto_hh_by_taz$ZeroAutoHH))
  summarise_at(vars(HH, ZeroAutoHH = x), list(sum))

zero_auto_hh_by_CT = zero_auto_hh_by_CT[!is.na(zero_auto_hh_by_CT$CTIDFP),]

model = zero_auto_hh_by_CT


# ct_shp = ct_shp[ct_shp$COUNTYFP %in% COUNTYFPs,]
ct_shp$GEOID = as.numeric(ct_shp$GEOID)

# Create DF
names(model)[names(model)=="HH"] = "Model_HH"
names(model)[names(model)=="ZeroAutoHH"] = "Model_A0"

census$Census_Auto0Prop = (census$Census_A0/census$Census_HH)*100
census[is.na(census)] = 0

setDT(model)
model[, CTIDFP := as.numeric(CTIDFP)]

model$Model_Auto0Prop = (model$Model_A0/model$Model_HH)*100
model[is.na(model)] = 0

df = census %>%
  left_join(model, by = c("TractID"="CTIDFP")) %>%
  mutate(Diff_ZeroAuto = Model_Auto0Prop - Census_Auto0Prop)
df[is.na(df)] = 0

#Copy plot variable to SHP
ct_shp@data$GEOID = as.numeric(ct_shp@data$GEOID)
ct_shp@data = ct_shp@data %>%
  left_join(df, by = c("GEOID"="TractID"))

ct_shp@data[is.na(ct_shp@data)] = 0

# Create Map
#ct_shp@data = ct_shp@data[!is.na(ct_shp@data$Diff_ZeroAuto),]
ct_shp@data$textComment1 = paste("Total Census HH: ", ct_shp@data$Census_HH, sep = "")
ct_shp@data$textComment2 = ifelse(ct_shp@data$Diff_ZeroAuto<0,'Model under predicts by',
                                     ifelse(ct_shp@data$Diff_ZeroAuto==0,"Model correct",'Model over predicts by'))

writeOGR(ct_shp, ABM_SUMMARY_DIR, 
         sub(".shp", "", CT_ZERO_AUTO_FILE_NAME), driver="ESRI Shapefile", check_exists = TRUE, overwrite_layer = TRUE)

# labels = sprintf(
#   "<strong>%s</strong><br/><strong>%s %.2f %s</strong><br/> %s",
#   ct_shp@data$CensusTract, ct_shp@data$textComment2, ct_shp@data$Diff_ZeroAuto, "%", ct_shp@data$textComment1
# ) %>% lapply(htmltools::HTML)
# 
# 
# bins = c(-Inf, -100, -75, -50, -25, -5, 5, 25, 50, 75, 100, Inf)
# pal = colorBin("PiYG", domain = ct_shp@data$Diff_ZeroAuto, na.color="transparent", bins = bins)
# 
# m = leaflet(data = ct_shp)%>%
#   addTiles() %>%
#   addProviderTiles(providers$OpenStreetMap, group = "Background Map") %>%
#   addLayersControl(
#     overlayGroups = "Background Map", options = layersControlOptions(collapsed = FALSE)
#   ) %>%
#   addPolygons(group='ZeroCarDiff',
#               fillColor = ~pal(Diff_ZeroAuto),
#               weight = 0.2,
#               opacity = 1,
#               color = "gray",
#               stroke=T,
#               dashArray = "5, 1",
#               fillOpacity = 0.7,
#               highlight = highlightOptions(
#                 weight = 1,
#                 color = "blue",
#                 dashArray = "",
#                 fillOpacity = 0.7,
#                 bringToFront = TRUE),
#               label = labels,
#               labelOptions = labelOptions(
#                 style = list("font-weight" = "normal", padding = "3px 8px"),
#                 textsize = "15px",
#                 direction = "auto")) %>%
#   addLegend(pal = pal, values = ~density, opacity = 0.7, title = "Estimated(%) - Observed(%) Bins",
#             position = "bottomright")
# 
# 
# # Output HTML
# saveWidget(m, file=paste(Out_Dir, "CT_ZeroAutoDiff_Census_vs_Model.html", sep = "/"), selfcontained = TRUE)
# 
# 
# # Write tabular CSV
# write.csv(df, paste(Out_Dir, "Data_CT_ZeroAutoDiff_Census_vs_Model.csv", sep = "/"), row.names = F)
# 
# print("Map Created!")

end_time = Sys.time()
end_time - start_time
cat("\n Script finished, run time: ", end_time - start_time, "sec \n")
