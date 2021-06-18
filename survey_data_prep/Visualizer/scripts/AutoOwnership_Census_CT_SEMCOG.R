######################################################################################
# Creating leaflet comparing census tract level auto ownership with model
#
######################################################################################

start_time <- Sys.time()

### Read Command Line Arguments
# args                <- commandArgs(trailingOnly = TRUE)
# Parameters_File     <- args[1]
Parameters_File <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/runtime/parameters.csv"

LOAD_PKGS_LIST <- c("leaflet", "htmlwidgets", "rgdal", "rgeos", "raster", "dplyr")
lib_sink <- suppressWarnings(suppressMessages(lapply(LOAD_PKGS_LIST, library, character.only = TRUE)))

### Read parameters from Parameters_File
parameters              <- read.csv(Parameters_File, header = TRUE)
ABM_DIR                 <- trimws(paste(parameters$Value[parameters$Key=="ABM_DIR"]))
ABM_SUMMARY_DIR         <- trimws(paste(parameters$Value[parameters$Key=="ABM_SUMMARY_DIR"]))
CENSUS_DIR              <- trimws(paste(parameters$Value[parameters$Key=="CENSUS_DIR"]))
ZONES_DIR               <- trimws(paste(parameters$Value[parameters$Key=="ZONES_DIR"]))
R_LIBRARY               <- trimws(paste(parameters$Value[parameters$Key=="R_LIBRARY"]))
BUILD_SAMPLE_RATE       <- trimws(paste(parameters$Value[parameters$Key=="BUILD_SAMPLE_RATE"]))
CT_ZERO_AUTO_FILE_NAME  <- trimws(paste(parameters$Value[parameters$Key=="CT_ZERO_AUTO_FILE_NAME"]))
SHP_FILE_NAME           <- trimws(paste(parameters$Value[parameters$Key=="SHP_FILE_NAME"]))

# INPUTS
########
# CensusData      <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census/ACS_2017_5yr_MI_CT_AutoOwn.csv"
CensusData      <- "ACS_2017_5yr_MI_CT_AutoOwn.csv"
# hh_file         <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/activitySim_400k_hh/final_households.csv"
hh_file         <- "final_households.csv"
# ct_shp_file     <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census/tl_2017_26_tract/tl_2017_26_tract.shp"
ct_shp_file     <- "tl_2017_26_tract.shp"


setwd(ABM_DIR)
hh <- read.csv(hh_file)

setwd(CENSUS_DIR)
census <- read.csv(CensusData, stringsAsFactors = F)

setwd(ZONES_DIR)
taz_shp <- shapefile(SHP_FILE_NAME)
ct_shp <- shapefile(ct_shp_file)

setwd(ABM_SUMMARY_DIR) # output dir

# Need Geographic crosswalk between taz and census tract
taz_shp <- spTransform(taz_shp, CRS("+proj=longlat +ellps=GRS80"))
ct_shp <- spTransform(ct_shp, CRS("+proj=longlat +ellps=GRS80"))

taz_centroids <- gCentroid(taz_shp, byid=TRUE, id=taz_shp$OBJECTID)

overlay <- function(points, polygon){
  proj4string(points) <- proj4string(polygon) # use same projection
  pointsDF <- over(points,polygon)
  return(pointsDF)
}

tazXWalk <- select(taz_shp@data, ID) %>%
  mutate(TAZSEQ = ID)
tazTemp1 <- overlay(taz_centroids, ct_shp)
tazXWalk <- tazXWalk %>%
  mutate(COUNTYFP = tazTemp1$COUNTYFP) %>%
  mutate(CTIDFP = tazTemp1$GEOID)


# Calculating auto availability and merging Census Track ID
hh$finalweight <- 1/as.numeric(BUILD_SAMPLE_RATE)
hh$hasZeroAutos <- ifelse(hh$auto_ownership==0, 1, 0)
hh$hasZeroAutosWeighted <- hh$hasZeroAutos * hh$finalweight
num_hh_per_taz <- aggregate(hh$finalweight, by=list(Category=hh$TAZ), FUN=sum)
zero_auto_hh_by_taz <- aggregate(hh$hasZeroAutosWeighted, by=list(Category=hh$TAZ), FUN=sum)
names(zero_auto_hh_by_taz)[names(zero_auto_hh_by_taz)=="Category"] <- "TAZ"
names(zero_auto_hh_by_taz)[names(zero_auto_hh_by_taz)=="x"] <- "ZeroAutoHH"

zero_auto_hh_by_taz$HH <- num_hh_per_taz$x
zero_auto_hh_by_taz$CTIDFP <- tazXWalk$CTIDFP[match(zero_auto_hh_by_taz$TAZ, tazXWalk$TAZSEQ)]
zero_auto_hh_by_taz$CTIDFP <- as.numeric(zero_auto_hh_by_taz$CTIDFP)
zero_auto_hh_by_taz$COUNTYFP <- tazXWalk$COUNTYFP[match(zero_auto_hh_by_taz$TAZ, tazXWalk$TAZSEQ)]

COUNTYFPs <- unique(zero_auto_hh_by_taz$COUNTYFP)
COUNTYFPs <- COUNTYFPs[!is.na(COUNTYFPs)]

zero_auto_hh_by_CT <- zero_auto_hh_by_taz %>%
  group_by(CTIDFP) %>%
  #summarise(HH = sum(zero_auto_hh_by_taz$HH)) %>%
  #summarise(ZeroAutoHH = sum(zero_auto_hh_by_taz$ZeroAutoHH))
  summarise_at(vars(HH, ZeroAutoHH), funs(sum))

zero_auto_hh_by_CT <- zero_auto_hh_by_CT[!is.na(zero_auto_hh_by_CT$CTIDFP),]

model <- zero_auto_hh_by_CT


ct_shp <- ct_shp[ct_shp$COUNTYFP %in% COUNTYFPs,]
ct_shp$GEOID <- as.numeric(ct_shp$GEOID)

# Create DF
names(model)[names(model)=="HH"] <- "Model_HH"
names(model)[names(model)=="ZeroAutoHH"] <- "Model_A0"

census$Census_Auto0Prop <- (census$Census_A0/census$Census_HH)*100
census[is.na(census)] <- 0

model$Model_Auto0Prop <- (model$Model_A0/model$Model_HH)*100
model[is.na(model)] <- 0

df <- census %>%
  left_join(model, by = c("TractID"="CTIDFP")) %>%
  mutate(Diff_ZeroAuto = Model_Auto0Prop - Census_Auto0Prop)
df[is.na(df)] <- 0

#Copy plot variable to SHP
ct_shp@data <- ct_shp@data %>%
  left_join(df, by = c("GEOID"="TractID"))
ct_shp@data[is.na(ct_shp@data)] <- 0

# Create Map
ct_shp <- ct_shp[!is.na(ct_shp@data$Diff_ZeroAuto),]
#ct_shp <- ct_shp[!is.na(ct_shp$Diff_ZeroAuto),]
ct_shp@data$textComment1 <- paste("Total Census HH: ", ct_shp$Census_HH, sep = "")
ct_shp@data$textComment2 <- ifelse(ct_shp@data$Diff_ZeroAuto<0,'Model under predicts by',
                                     ifelse(ct_shp@data$Diff_ZeroAuto==0,"Model correct",'Model over predicts by'))

writeOGR(ct_shp, ABM_SUMMARY_DIR, sub(".shp", "", CT_ZERO_AUTO_FILE_NAME), driver="ESRI Shapefile", check_exists = TRUE, overwrite_layer = TRUE)

# labels <- sprintf(
#   "<strong>%s</strong><br/><strong>%s %.2f %s</strong><br/> %s",
#   ct_shp@data$CensusTract, ct_shp@data$textComment2, ct_shp@data$Diff_ZeroAuto, "%", ct_shp@data$textComment1
# ) %>% lapply(htmltools::HTML)
# 
# 
# bins <- c(-Inf, -100, -75, -50, -25, -5, 5, 25, 50, 75, 100, Inf)
# pal <- colorBin("PiYG", domain = ct_shp@data$Diff_ZeroAuto, na.color="transparent", bins = bins)
# 
# m <- leaflet(data = ct_shp)%>%
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

end_time <- Sys.time()
end_time - start_time
cat("\n Script finished, run time: ", end_time - start_time, "sec \n")
