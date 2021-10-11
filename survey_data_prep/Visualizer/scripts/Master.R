#############################################################################################################################
# Master script to render final HTML file from R Markdown file
# Loads all required packages from the dependencies folder
#
# Make sure the 'plyr' is not loaded after 'dplyr' library in the same R session
# Under such case, the group_by features of dplyr library does not work. Restart RStudio and make sure
# plyr library is not loaded while generating dashboard
# For more info on this issue:
# https://stackoverflow.com/questions/26923862/why-are-my-dplyr-group-by-summarize-not-working-properly-name-collision-with
#
#############################################################################################################################
library(Rcpp)
### Read Command Line Arguments
#FIXME: Redo this, it should be reading the commandArgs, ASR commented to run in RStudio
#args                <- commandArgs(trailingOnly = TRUE)
#Parameters_File     <- args[1]
Parameters_File <- "C:\\projects\\cmap_activitysim\\cmap_abm\\survey_data_prep\\Visualizer\\runtime\\parameters.csv"
#Run_switch          <- "FULL"

### Read parameters from Parameters_File
parameters          <- read.csv(Parameters_File, header = TRUE)
WORKING_DIR         <- trimws(paste(parameters$Value[parameters$Key=="WORKING_DIR"]))
BASE_SUMMARY_DIR    <- trimws(paste(parameters$Value[parameters$Key=="BASE_SUMMARY_DIR"]))
CENSUS_SUMMARY_DIR  <- trimws(paste(parameters$Value[parameters$Key=="CENSUS_SUMMARY_DIR"]))
ABM_SUMMARY_DIR     <- trimws(paste(parameters$Value[parameters$Key=="ABM_SUMMARY_DIR"]))
CALIBRATION_DIR     <- trimws(paste(parameters$Value[parameters$Key=="CALIBRATION_DIR"]))
BASE_SCENARIO_NAME  <- trimws(paste(parameters$Value[parameters$Key=="BASE_SCENARIO_NAME"]))
BUILD_SCENARIO_NAME <- trimws(paste(parameters$Value[parameters$Key=="BUILD_SCENARIO_NAME"]))
BASE_SAMPLE_RATE    <- as.numeric(trimws(paste(parameters$Value[parameters$Key=="BASE_SAMPLE_RATE"])))
BUILD_SAMPLE_RATE   <- as.numeric(trimws(paste(parameters$Value[parameters$Key=="BUILD_SAMPLE_RATE"])))
R_LIBRARY           <- trimws(paste(parameters$Value[parameters$Key=="R_LIBRARY"]))
RSTUDIO_PANDOC_path <- trimws(paste(parameters$Value[parameters$Key=="RSTUDIO_PANDOC"]))
SUBSET_HTML_NAME    <- trimws(paste(parameters$Value[parameters$Key=="SUBSET_HTML_NAME"]))
FULL_HTML_NAME      <- trimws(paste(parameters$Value[parameters$Key=="FULL_HTML_NAME"]))
SHP_FILE_NAME       <- trimws(paste(parameters$Value[parameters$Key=="SHP_FILE_NAME"]))
CT_ZERO_AUTO_FILE_NAME       <- trimws(paste(parameters$Value[parameters$Key=="CT_ZERO_AUTO_FILE_NAME"]))
IS_BASE_SURVEY      <- trimws(paste(parameters$Value[parameters$Key=="IS_BASE_SURVEY"]))
BUILD_SUMMARY_DIR      <- trimws(paste(parameters$Value[parameters$Key=="BUILD_SUMMARY_DIR"]))
# BUILD_SUMMARY_DIR   <- ifelse(Run_switch=="SN",
#                               file.path(CALIBRATION_DIR, "ABM_Summaries_subset"),
#                               file.path(CALIBRATION_DIR, "ABM_Summaries"))
OUTPUT_HTML_NAME    <- FULL_HTML_NAME

print(paste("ASRDBG Working directory:", WORKING_DIR))
### Initialization
# Load global variables
.libPaths(R_LIBRARY)
Sys.getenv("RSTUDIO_PANDOC")
Sys.setenv(RSTUDIO_PANDOC=RSTUDIO_PANDOC_path)
cat("Using the R packages found in ", .libPaths(), "\n")
# cat("Pandoc version: ", pandoc_available())
# cat(installed.packages())
source(paste(WORKING_DIR, "scripts/_SYSTEM_VARIABLES.R", sep = "/"))

### Copy summary CSVs
# base_csv_list <- ifelse(IS_BASE_SURVEY=="Yes", "summaryFilesNames_survey_SEMCOG.csv", "summaryFilesNames_survey_SEMCOG.csv")
base_csv_list <- "summaryFilesNames_survey_CMAP.csv"
summaryFileList_base <- read.csv(paste(SYSTEM_TEMPLATES_PATH, base_csv_list, sep = "/"), as.is = T)
summaryFileList_base <- as.list(summaryFileList_base$summaryFile)
#retVal <- copyFile(summaryFileList_base, sourceDir = BASE_SUMMARY_DIR, targetDir = BASE_DATA_PATH)
#if(retVal) q(save = "no", status = 11)

census_csv_list <- "summaryFilesNames_census_CMAP.csv"
summaryFileList_census <- read.csv(paste(SYSTEM_TEMPLATES_PATH, census_csv_list, sep = '/'), as.is = T)
summaryFileList_census <- as.list(summaryFileList_census$summaryFile)
#retVal <- copyFile(summaryFileList_census, sourceDir = BASE_SUMMARY_DIR, targetDir = BASE_DATA_PATH)
#if(retVal) q(save = "no", status = 11)

# calibration_csv_list <- "summaryFilesNames_calibration_SEMCOG.csv"
# summaryFileList_calibration <- read.csv(paste(SYSTEM_TEMPLATES_PATH, calibration_csv_list, sep = '/'), as.is = T)
# summaryFileList_calibration <- as.list(summaryFileList_calibration$summaryFile)
# retVal <- copyFile(summaryFileList_calibration, sourceDir = CALIBRATION_DIR, targetDir = BASE_DATA_PATH)
# if(retVal) q(save = "no", status = 11)

if(BUILD_SCENARIO_NAME == "SEMCOG_HTS") {
  build_csv_list <- "summaryFilesNames_ActivitySim_SEMCOG.csv"
} else {
  build_csv_list <- "summaryFilesNames_ActivitySim_CMAP.csv"
}

print(paste("Copying files from", BUILD_SUMMARY_DIR, "to", BUILD_DATA_PATH))
summaryFileList_build <- read.csv(paste(SYSTEM_TEMPLATES_PATH, build_csv_list, sep = '/'), as.is = T)
summaryFileList_build <- as.list(summaryFileList_build$summaryFile)
retVal <- copyFile(summaryFileList_build, sourceDir = BUILD_SUMMARY_DIR, targetDir = BUILD_DATA_PATH)
#if(retVal) q(save = "no", status = 11)

### Copy jpegs
jpeg_list <- list.files(BUILD_SUMMARY_DIR, "*.jpeg")
jpeg_copy_retval <- file.copy(jpeg_list, SYSTEM_JPEG_PATH, overwrite = TRUE)

### Load required libraries
SYSTEM_REPORT_PKGS <- c("DT", "flexdashboard", "leaflet", "geojsonio", "htmltools", "htmlwidgets", "kableExtra", "shiny",
                        "knitr", "mapview", "plotly", "RColorBrewer", "rgdal", "rgeos", "crosstalk","treemap", "htmlTable",
                        "rmarkdown", "scales", "stringr", "jsonlite", "pander", "ggplot2", "reshape", "raster", "dplyr")

lib_inst <- suppressWarnings(suppressMessages(lapply(SYSTEM_REPORT_PKGS, function(x){if (x %in% rownames(installed.packages()) == FALSE) install.packages(x, repos='http://cran.us.r-project.org')})))
lib_sink <- suppressWarnings(suppressMessages(lapply(SYSTEM_REPORT_PKGS, library, character.only = TRUE)))

### Read Target and Output SUmmary files
currDir <- getwd()
setwd(BASE_DATA_PATH)
base_csv = list.files(pattern="*.csv")
### test
#for(i in 1:length(base_csv)){
#  file_name <- base_csv[[i]]
#  cat(file_name, "\n")
#  #t <- read.csv(file_name)
#}

base_data <- lapply(base_csv, function(x){
  #print(x)
  read.csv(x, row.names = NULL)})
base_csv_names <- unlist(lapply(base_csv, function (x) {gsub(".csv", "", x)}))


setwd(BUILD_DATA_PATH)
build_csv = list.files(pattern="*.csv")
build_csv = build_csv[!grepl('Census', build_csv)]
build_data <- lapply(build_csv, read.csv)
build_csv_names <- unlist(lapply(build_csv, function (x) {gsub(".csv", "", x)}))

### Read SHP files
setwd(SYSTEM_SHP_PATH)
zone_shp <- shapefile(SHP_FILE_NAME)
zone_shp <- spTransform(zone_shp, CRS("+proj=longlat +ellps=GRS80"))

setwd(ABM_SUMMARY_DIR)
ct_zero_auto_shp <- shapefile(CT_ZERO_AUTO_FILE_NAME)
ct_zero_auto_shp <- spTransform(ct_zero_auto_shp, CRS("+proj=longlat +ellps=GRS80"))

setwd(currDir)
print("Read specified csv files, now loading visualizer.")
### Generate dashboard
rmarkdown::render(file.path(SYSTEM_TEMPLATES_PATH, "template.Rmd"),
                  output_dir = RUNTIME_PATH,
                  intermediates_dir = RUNTIME_PATH, quiet = TRUE)
template.html <- readLines(file.path(RUNTIME_PATH, "template.html"))
idx <- which(template.html == "window.FlexDashboardComponents = [];")[1]
template.html <- append(template.html, "L_PREFER_CANVAS = true;", after = idx)
writeLines(template.html, file.path(OUTPUT_PATH, paste(OUTPUT_HTML_NAME, ".html", sep = "")))





# finish
