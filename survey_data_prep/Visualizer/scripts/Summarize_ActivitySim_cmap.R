###################################################################################
### Script for summarizing MetCouncil ActivitySim Output 
### Edited: July 2021
###################################################################################

start_time <- Sys.time()

### Read Command Line Arguments
# args                <- commandArgs(trailingOnly = TRUE)
# Parameters_File     <- args[1]
# Run_switch          <- args[2]


#Run_switch <- "FULL"

SYSTEM_REPORT_PKGS <- c("data.table", "plyr", "weights", "reshape", "stringr", "foreign", "yaml")
lib_sink <- suppressWarnings(suppressMessages(lapply(SYSTEM_REPORT_PKGS, library, character.only = TRUE))) 

args = commandArgs(trailingOnly = TRUE)

if(length(args) > 0){
  settings_file = args[1]
} else {
  settings_file = 'C:\\projects\\cmap_activitysim\\cmap_abm\\survey_data_prep\\cmap_inputs.yml'
}

settings = yaml.load_file(settings_file)

# User Inputs
#-----------------------------------------------
### Read parameters from Parameters_File

ABM_DIR             <- settings$abm_dir
ABM_SUMMARY_DIR     <- settings$abm_summaries_dir
# SKIMS_FILEPATH           <- settings$skims_file
SKIMS_FILEPATH = file.path(settings$abm_inputs, settings$skims_filename)
# ZONES_DIR           <- settings$zone_dir
ZONES_DIR = settings$zone_dir # when using sandag example 
# R_LIBRARY           <- trimws(paste(parameters$Value[parameters$Key=="R_LIBRARY"]))

ABM_INPUTS = settings$abm_inputs

# .libPaths(R_LIBRARY)
library(omxr)
WD <- ABM_SUMMARY_DIR
# 
xwalk_file = file.path(ZONES_DIR, "subzones17.dbf")
xwalk <- read.dbf(xwalk_file, as.is=TRUE)

# source(file.path(settings$visualizer_dir, 'scripts', 'ZMX.R'))

# skim_file = file.path(SKIMS_DIR, "mf5183.zmx")

setwd(ABM_DIR)
print(paste("Using the files from", ABM_DIR))
hh                 <- read.csv("final_households.csv", header = TRUE)
per                <- read.csv("final_persons.csv", header = TRUE)
all_tours          <- read.csv("final_tours.csv", header = TRUE)
all_trips          <- read.csv("final_trips.csv", header = TRUE)
jtour_participants <- read.csv("final_joint_tour_participants.csv", header = TRUE)

print(paste("Model data has", nrow(hh), "households,", nrow(per), "people,", nrow(all_tours), "tours, and", nrow(all_trips), "trips."))

setwd(WD)

#-----------------------------------------------
# Define other variables
pertypeCodes <- data.frame(code = c(1,2,3,4,5,6,7,8,"All"), 
                           name = c("FT Worker", "PT Worker", "Univ Stud", "Non Worker", "Retiree", "Driv Stud", "NonDriv Stud", "Pre-School", "All"))
# ensure persontype coding already in per file is consistent with above

purposeCodes <- data.frame(code = c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                           name = c("Home", "work", "univ", "school", "escort", "shopping", "othmaint", "eatout", "social", "othdiscr", "atwork"))

# fixme: Confirm text/numeric correspond to CMAP model output modes
modeCodes <- data.frame(code = c(1, 1, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 10, 11, 11, 11),
                        name = c("DRIVEALONE", "DRIVEALONEPAY", "SHAREDRIDE2", "SHARED2PAY",
                                 "SHAREDRIDE3","SHARED3PAY", "WALK", "BIKE",
                                 "WALK_TRANSIT", "PNR_TRANSIT", "KNR_TRANSIT", "TNC_TRANSIT",
                                 "SCHBUS",                        # 9 = School Bus
                                 "TAXI", "TNC_SINGLE", "TNC_SHARED")) #10 = Ride hail

tourcatCodes <- data.frame(code = c(0,1,3),
                           name = c("mandatory", "non-mandatory", "atwork"))




county_lookup = fread(file.path(settings$proj_dir, 'county_lookup.csv'))
setDT(xwalk)

county_lookup[, county_fip := as.character(county_fip)]

xwalk[county_lookup, county_name_proper := i.county_name, on = .(county_fip)]
xwalk[county_lookup, county_name_proper := i.county_name, on = .(county_fip)]
xwalk[county_name_proper == '', county_name_proper := str_to_title(county_nam) ]
# 


districtList <- sort(unique(xwalk$county_name_proper))
setnames(xwalk, c('county_name_proper', 'county_fip', 'zone17', 'subzone17'), c('COUNTY_NAME', 'COUNTY', 'TAZ', 'MAZ'), skip_absent = TRUE)

print('Processing Distance Skim Matrix...')
# skimMat = readZipMat(SKIMS_FILEPATH)
skimMat = read_omx(SKIMS_FILEPATH, "DIST")

#zoneID_lookup <- read_lookup(skim_file, "ZoneID")
#zoneID_lookup_df <- data.frame(TAZ = zoneID_lookup$Lookup,
#                               ID =seq(2899))

DST_SKM <- melt(skimMat)
colnames(DST_SKM) <- c("o", "d", "dist")
#DST_SKM$o <- zoneID_lookup_df$TAZ[match(DST_SKM$X1,zoneID_lookup_df$ID)]
#DST_SKM$d <- zoneID_lookup_df$TAZ[match(DST_SKM$X2,zoneID_lookup_df$ID)]
#DST_SKM$dist <- DST_SKM$value




#----------------------------------------------
# Prepare files for computing summary statistics

hh$HHVEH[hh$auto_ownership == 0] <- 0
hh$HHVEH[hh$auto_ownership == 1] <- 1
hh$HHVEH[hh$auto_ownership == 2] <- 2
hh$HHVEH[hh$auto_ownership == 3] <- 3
hh$HHVEH[hh$auto_ownership >= 4] <- 4

#HH Size
hh$HHSIZE[hh$hhsize == 1] <- 1
hh$HHSIZE[hh$hhsize == 2] <- 2
hh$HHSIZE[hh$hhsize == 3] <- 3
hh$HHSIZE[hh$hhsize == 4] <- 4
hh$HHSIZE[hh$hhsize >= 5] <- 5

# calculate HH weights
hh$finalweight <- 1

### copy weight field
per$finalweight    <- hh$finalweight[match(per$household_id, hh$household_id)]
all_tours$finalweight  <- hh$finalweight[match(all_tours$household_id, hh$household_id)]
all_trips$finalweight  <- hh$finalweight[match(all_trips$household_id, hh$household_id)]

# PERTYPE needs to be in the following order: FT, PT, US, NW, RE, DS, ND, PS
per$PERTYPE <- per$ptype

# Districts are Counties
per$HDISTRICT <- xwalk$COUNTY_NAME[match(per$home_zone_id, xwalk$MAZ)]
per$WDISTRICT <- xwalk$COUNTY_NAME[match(per$workplace_zone_id, xwalk$MAZ)]

hh$HDISTRICT <- xwalk$COUNTY_NAME[match(hh$home_zone_id, xwalk$MAZ)]

#Workers in the HH
hh$WORKERS <- hh$num_workers
#Adults in the HH
hh$ADULTS <- hh$num_adults

# HHVEH X Workers CrossTab
write.csv(xtabs(finalweight~HHVEH+WORKERS, data = hh), "xtab_HHVEH_WORKERS.csv", row.names = T)

# Workers vs. employees by zone and county ####
tazdat = read.csv(file.path(ABM_INPUTS, "land_use.csv"), header = TRUE)

tazdat$WDISTRICT = xwalk$COUNTY_NAME[match(tazdat$subzone, xwalk$MAZ)]
emp_co = plyr::count(tazdat, c("WDISTRICT"), "emp")
emp_taz = plyr::count(tazdat, c("zone"), "emp")

write.csv(emp_taz, "employees_per_taz.csv")
write.csv(emp_co, "employees_per_co.csv")


per$worktaz <- xwalk$TAZ[match(per$workplace_zone_id, xwalk$MAZ)]
wrk_taz = plyr::count(per[per$workplace_zone_id > 0,], c("worktaz"), "finalweight")
wrk_co = plyr::count(per[per$workplace_zone_id > 0,], c("WDISTRICT"), "finalweight")

write.csv(wrk_taz, "workers_per_taz.csv")
write.csv(wrk_co, "workers_per_co.csv")

#--------------------------------------------------
# Compute Summary Statistics

# Auto ownership
autoOwnership <- plyr::count(hh, c("HDISTRICT", "HHVEH"), "finalweight")
autoOwnershipT = plyr::count(hh, c("HHVEH"), "finalweight")
autoOwnershipT$HDISTRICT = "Total"
autoOwnership = rbind(autoOwnership, autoOwnershipT)

write.csv(autoOwnership, "autoOwnership.csv", row.names = TRUE)

hh$COUNTY <- xwalk$COUNTY_NAME[match(hh$home_zone_id, xwalk$MAZ)]
autoOwnershipCY <- plyr::count(hh, c("COUNTY", "HHVEH"), "finalweight")
autoOwnershipCY <- cast(autoOwnershipCY, COUNTY~HHVEH, value = "freq", sum)
write.csv(autoOwnershipCY, "autoOwnershipCY.csv", row.names = F)

# Persons by person type
pertypeDistbn <- plyr::count(per, c("PERTYPE"), "finalweight")
write.csv(pertypeDistbn, "pertypeDistbn.csv", row.names = TRUE)


# Mandatory DC
workers <- per[per$workplace_zone_id > 0 & per$is_worker == "True",]
students <- per[per$school_zone_id > 0 & per$is_student == "True",]

# code distance bins
workers$distbin <- cut(workers$distance_to_work, breaks = c(seq(0,50, by=1), 9999), labels = F, right = F)
students$distbin <- cut(students$distance_to_school, breaks = c(seq(0,50, by=1), 9999), labels = F, right = F)

distBinCat <- data.frame(distbin = seq(1,51, by=1))

# compute TLFDs by district and total
tlfd_work <- ddply(workers[,c("HDISTRICT", "distbin", "finalweight")], c("HDISTRICT", "distbin"), summarise, work = sum(finalweight))
tlfd_work <- cast(tlfd_work, distbin~HDISTRICT, value = "work", sum)

tlfd_univ <- ddply(students[students$PERTYPE==3,c("HDISTRICT", "distbin", "finalweight")], c("HDISTRICT", "distbin"), summarise, univ = sum(finalweight))
tlfd_univ <- cast(tlfd_univ, distbin~HDISTRICT, value = "univ", sum)

tlfd_schl <- ddply(students[students$PERTYPE>=6,c("HDISTRICT", "distbin", "finalweight")], c("HDISTRICT", "distbin"), summarise, schl = sum(finalweight))
tlfd_schl <- cast(tlfd_schl, distbin~HDISTRICT, value = "schl", sum)

# all counties need to be included
unique_county_names <- unique(as.character(xwalk$COUNTY_NAME))
for(i in seq(1, length(unique_county_names))){
  if (!(unique_county_names[i] %in% colnames(tlfd_work))){
    tlfd_work[unique_county_names[i]] <- 0
  }
  if (!(unique_county_names[i] %in% colnames(tlfd_univ))){
    tlfd_univ[unique_county_names[i]] <- 0
  }
  if (!(unique_county_names[i] %in% colnames(tlfd_schl))){
    tlfd_schl[unique_county_names[i]] <- 0
  }
}

tlfd_work$Total <- rowSums(tlfd_work[,!colnames(tlfd_work) %in% c("distbin")])
tlfd_work_df <- merge(x = distBinCat, y = tlfd_work, by = "distbin", all.x = TRUE)
tlfd_work_df[is.na(tlfd_work_df)] <- 0

tlfd_univ$Total <- rowSums(tlfd_univ[,!colnames(tlfd_univ) %in% c("distbin")])
tlfd_univ_df <- merge(x = distBinCat, y = tlfd_univ, by = "distbin", all.x = TRUE)
tlfd_univ_df[is.na(tlfd_univ_df)] <- 0

tlfd_schl$Total <- rowSums(tlfd_schl[,!colnames(tlfd_schl) %in% c("distbin")])
tlfd_schl_df <- merge(x = distBinCat, y = tlfd_schl, by = "distbin", all.x = TRUE)
tlfd_schl_df[is.na(tlfd_schl_df)] <- 0

write.csv(tlfd_work_df, "workTLFD.csv", row.names = F)
write.csv(tlfd_univ_df, "univTLFD.csv", row.names = F)
write.csv(tlfd_schl_df, "schlTLFD.csv", row.names = F)

cat("\n Average distance to workplace (Total): ", weighted.mean(workers$distance_to_work, workers$finalweight, na.rm = TRUE))
cat("\n Average distance to university (Total): ", weighted.mean(students$distance_to_school[students$PERTYPE==3], students$finalweight[students$PERTYPE==3], na.rm = TRUE))
cat("\n Average distance to school (Total): ", weighted.mean(students$distance_to_school[students$PERTYPE>=6], students$finalweight[students$PERTYPE>=6], na.rm = TRUE))
cat("\n\n")

## Output avg trip lengths for visualizer
workTourLengths <- ddply(workers[,c("HDISTRICT", "distance_to_work", "finalweight")], c("HDISTRICT"), summarise, work = weighted.mean(distance_to_work, finalweight))
totalLength     <- data.frame("Total", weighted.mean(workers$distance_to_work, workers$finalweight))
colnames(totalLength) <- colnames(workTourLengths)
workTourLengths <- rbind(workTourLengths, totalLength)

univTourLengths <- ddply(students[students$PERTYPE==3,c("HDISTRICT", "distance_to_school", "finalweight")], c("HDISTRICT"), summarise, univ = weighted.mean(distance_to_school, finalweight))
totalLength     <- data.frame("Total", weighted.mean(students$distance_to_school[students$PERTYPE==3], students$finalweight[students$PERTYPE==3]))
colnames(totalLength) <- colnames(univTourLengths)
univTourLengths <- rbind(univTourLengths, totalLength)

schlTourLengths <- ddply(students[students$PERTYPE>=6,c("HDISTRICT", "distance_to_school", "finalweight")], c("HDISTRICT"), summarise, schl = weighted.mean(distance_to_school, finalweight))
totalLength     <- data.frame("Total", weighted.mean(students$distance_to_school[students$PERTYPE>=6], students$finalweight[students$PERTYPE>=6]))
colnames(totalLength) <- colnames(schlTourLengths)
schlTourLengths <- rbind(schlTourLengths, totalLength)

# mandTripLengths <- cbind(workTripLengths, univTripLengths$univ, schlTripLengths$schl)
mandTourLengths <- merge(workTourLengths, univTourLengths, by='HDISTRICT', all=TRUE)
mandTourLengths <- merge(mandTourLengths, schlTourLengths, by='HDISTRICT', all=TRUE)
colnames(mandTourLengths) <- c("District", "Work", "Univ", "Schl")
# rearranging such that the "Total" comes at the end
mandTourLengths <- rbind(mandTourLengths[!(mandTourLengths$District == "Total"),], mandTourLengths[(mandTourLengths$District == "Total"),])
write.csv(mandTourLengths, "mandTourLengths.csv", row.names = F)
# 
# # Work from home [for each district and total]
districtWorkers <- ddply(per[per$is_worker=="True",c("HDISTRICT", "finalweight")], c("HDISTRICT"), summarise, workers = sum(finalweight))
#districtWorkers$HDISTRICT = str_to_upper(districtWorkers$HDISTRICT)
districtWorkers_df <- merge(x = data.frame(HDISTRICT = districtList), y = districtWorkers, by = "HDISTRICT", all.x = TRUE)
districtWorkers_df[is.na(districtWorkers_df)] <- 0

districtWfh     <- ddply(per[per$is_worker=="True" & per$work_from_home=="True",c("HDISTRICT", "finalweight")], c("HDISTRICT"), summarise, wfh = sum(finalweight))
#districtWfh$HDISTRICT = str_to_upper(districtWfh$HDISTRICT)
districtWfh_df <- merge(x = data.frame(HDISTRICT = districtList), y = districtWfh, by = "HDISTRICT", all.x = TRUE)
districtWfh_df[is.na(districtWfh_df)] <- 0

wfh_summary     <- cbind(districtWorkers_df, districtWfh_df$wfh)
colnames(wfh_summary) <- c("District", "Workers", "WFH")
totalwfh        <- data.frame("Total", sum((per$is_worker=="True")*per$finalweight), sum((per$is_worker=="True" & per$work_from_home=="True")*per$finalweight))
colnames(totalwfh) <- colnames(wfh_summary)
wfh_summary <- rbind(wfh_summary, totalwfh)
write.csv(wfh_summary, "wfh_summary.csv", row.names = F)

# Telecommute Frequency
telecommuteFrequency <- count(per[!is.na(per$telecommute_frequency),], c("telecommute_frequency"), "finalweight")
# #drop the empty rows count
telecommuteFrequency <- telecommuteFrequency[-c(1), ]
write.csv(telecommuteFrequency, "telecommuteFrequency.csv", row.names = TRUE)

# County-County Flows
countyFlows <- xtabs(finalweight~HDISTRICT+WDISTRICT, data = workers[levels(workers$work_from_home)[workers$work_from_home] == 'False',])
countyFlows[is.na(countyFlows)] <- 0
#countyFlows <- addmargins(as.table(countyFlows))
countyFlows <- as.data.frame.matrix(countyFlows)
#omit_counties = c('Kankakee', 'Kenosha', 'Lasalle', 'Lee', 'Ogle', 'Racine','Boone', 'Winnebago', 'Walworth' , 'Sum')
#countyFlows = countyFlows[!rownames(countyFlows) %in% omit_counties, !colnames(countyFlows) %in% omit_counties]
#setcolorder(countyFlows, c("Cook",     "DeKalb",   "DuPage",   
#                                   "Grundy" ,  "Kane" ,  "Kendall", "Lake, IL",
#             "McHenry"  , "Will"   , "Lake, IN",  "LaPorte",   "Porter"))

countyFlows = countyFlows[c(1:6, 7, 10, 12, 8, 9, 11, 13:21), c(1:6, 7, 10, 12, 8, 9, 11, 13:21)]

#countyFlows <- addmargins(countyFlows, FUN = sum)
countyFlows <- cbind(countyFlows, Total = rowSums(countyFlows))
countyFlows <- rbind(countyFlows, Total = colSums(countyFlows))
#countyFlows[, order_num := c(1:6, 8, 10, 12, 9, 7, 11, 13)]
#countyFlows = countyFlows[order(order_num)]
#countyFlows[, order_num := NULL]
# setnames(countyFlows, 'res_county_name', '')

#fwrite(county_flows_matrix, file.path(settings$visualizer_summaries, 'countyFlowsCensus.csv'))
#colnames(countyFlows)[colnames(countyFlows)=="sum"] <- "Total"
#rownames(countyFlows)[rownames(countyFlows)=="sum"] <- "Total"
write.csv(countyFlows, "countyFlows.csv", row.names = T)



#--------------------------------------------------------
# Process Tour file
all_tours$TOURPURP <- purposeCodes$code[match(all_tours$primary_purpose, purposeCodes$name)]
all_tours$TOURMODE <- modeCodes$code[match(all_tours$tour_mode, modeCodes$name)]
all_tours$TOURCAT <- tourcatCodes$code[match(all_tours$tour_category, tourcatCodes$name)]



all_tours$PERTYPE <- per$PERTYPE[match(all_tours$person_id, per$person_id)]
all_tours$HHVEH <- hh$HHVEH[match(all_tours$household_id, hh$household_id)]
all_tours$ADULTS <- hh$ADULTS[match(all_tours$household_id, hh$household_id)]

all_tours$WORKERS <- hh$WORKERS[match(all_tours$household_id, hh$household_id)]
all_tours$AUTOSUFF[all_tours$HHVEH == 0] <- 0
all_tours$AUTOSUFF[all_tours$HHVEH < all_tours$WORKERS & all_tours$HHVEH > 0] <- 1
all_tours$AUTOSUFF[all_tours$HHVEH >= all_tours$WORKERS & all_tours$HHVEH > 0] <- 2

all_tours$num_ob_stops <- sapply(strsplit(as.character(all_tours$stop_frequency), "out_"), function(x) (as.numeric(x[1])))
all_tours$num_ib_stops <- sapply(strsplit(as.character(all_tours$stop_frequency), "out_"), function(x) (as.numeric(str_replace(x[2],"in",""))))

all_tours$num_tot_stops <- all_tours$num_ob_stops + all_tours$num_ib_stops

all_tours$OTAZ <- xwalk$TAZ[match(all_tours$origin, xwalk$MAZ)]
all_tours$DTAZ <- xwalk$TAZ[match(all_tours$destination, xwalk$MAZ)]
all_tours$OCOUNTY <- xwalk$COUNTY[match(all_tours$OTAZ, xwalk$TAZ)]
all_tours$DCOUNTY <- xwalk$COUNTY[match(all_tours$DTAZ, xwalk$TAZ)]

all_tours$SKIMDIST <- DST_SKM$dist[match(paste(all_tours$OTAZ, all_tours$DTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]

all_tours$tour_composition <- 0
all_tours$tour_composition[all_tours$composition == "adults"]   <- 1
all_tours$tour_composition[all_tours$composition == "children"] <- 2
all_tours$tour_composition[all_tours$composition == "mixed"]    <- 3

all_tours$start_hour <- all_tours$start
all_tours$end_hour <- all_tours$end

#compute duration
all_tours$tourdur <- all_tours$duration

all_tours$finalweight[is.na(all_tours$finalweight)] <- 0



#--------------------------------------------------
# Process Trips file
all_trips$TOURPURP <- purposeCodes$code[match(all_trips$primary_purpose, purposeCodes$name)]
all_trips$TRIPPURP <- purposeCodes$code[match(all_trips$purpose, purposeCodes$name)]

all_trips$TRIPMODE <- modeCodes$code[match(all_trips$trip_mode, modeCodes$name)]
all_trips$TOURMODE <- all_tours$TOURMODE[match(all_trips$tour_id, all_tours$tour_id)]

all_trips$tour_category <- all_tours$tour_category[match(all_trips$tour_id, all_tours$tour_id)]
all_trips$TOURCAT <- tourcatCodes$code[match(all_trips$tour_category, tourcatCodes$name)]

all_trips$PERTYPE <- per$PERTYPE[match(all_trips$person_id, per$person_id)]
all_trips$HHVEH <- hh$HHVEH[match(all_trips$household_id, hh$household_id)]
all_trips$ADULTS <- hh$ADULTS[match(all_trips$household_id, hh$household_id)]

all_trips$WORKERS <- hh$WORKERS[match(all_trips$household_id, hh$household_id)]
all_trips$AUTOSUFF[all_trips$HHVEH == 0] <- 0
all_trips$AUTOSUFF[all_trips$HHVEH < all_trips$WORKERS & all_trips$HHVEH > 0] <- 1
all_trips$AUTOSUFF[all_trips$HHVEH >= all_trips$WORKERS & all_trips$HHVEH > 0] <- 2

all_trips$OTAZ <- all_trips$origin
all_trips$DTAZ <- all_trips$destination

all_trips$OTAZ <- xwalk$TAZ[match(all_trips$origin, xwalk$MAZ)]
all_trips$DTAZ <- xwalk$TAZ[match(all_trips$destination, xwalk$MAZ)]
all_trips$OCOUNTY <- xwalk$COUNTY[match(all_trips$OTAZ, xwalk$TAZ)]
all_trips$DCOUNTY <- xwalk$COUNTY[match(all_trips$DTAZ, xwalk$TAZ)]

all_trips$TOUROTAZ <- all_tours$OTAZ[match(all_trips$tour_id, all_tours$tour_id)]
all_trips$TOURDTAZ <- all_tours$DTAZ[match(all_trips$tour_id, all_tours$tour_id)]

all_trips$od_dist <- DST_SKM$dist[match(paste(all_trips$OTAZ, all_trips$DTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]
all_trips$od_dist[is.na(all_trips$od_dist)] <- 0

all_trips$depart_hour <- all_trips$depart

all_trips$num_participants <- all_tours$number_of_participants[match(all_trips$tour_id, all_tours$tour_id)]

# Set Origin and destination trip purposes
all_trips$DPURP <- all_trips$TRIPPURP
nr <- nrow(all_trips)
all_trips$prev_trip_id <- 0
all_trips$prev_trip_id[2:nr] <- all_trips$trip_id[1:nr-1]
all_trips$OPURP <- 0 # Start at home as default
all_trips$OPURP <- all_trips$TRIPPURP[match(all_trips$prev_trip_id, all_trips$trip_id)]

#Mark stops
all_trips$inb_next <- 0
all_trips$inbound <- ifelse(all_trips$outbound == "False", 1, 0)
all_trips$inb_next[1:nr-1] <- all_trips$inbound[2:nr]
all_trips$stops[all_trips$DPURP>0 & ((all_trips$inbound==0 & all_trips$inb_next==0) | (all_trips$inbound==1 & all_trips$inb_next==1))] <- 1
all_trips$stops[is.na(all_trips$stops)] <- 0


all_trips$finalweight[is.na(all_trips$finalweight)] <- 0


# Recode Duration Bins

all_tours$tourdur[all_tours$tourdur<=1] = 1


# Recode time bin windows

bin_xwalk = data.frame(bin48 = seq(1,48), 
                       bin23 = seq(1,48))
                         
                         
                         
                         # c(rep(5,3),
                         #         rep(6:22, each = 1),
                         #         rep(23, 4)))


# cap tour duration at 20 hours
all_tours$tourdur[all_tours$tourdur>=20] = 20

all_tours = merge(all_tours, bin_xwalk, by.x = 'start_hour', by.y = 'bin48', all.x = T)
names(all_tours)[names(all_tours)=="bin23"]   = 'ANCHOR_DEPART_BIN_RECODE'

all_tours = merge(all_tours, bin_xwalk, by.x = 'end', by.y = 'bin48', all.x = T)
names(all_tours)[names(all_tours)=="bin23"]   = 'ANCHOR_ARRIVE_BIN_RECODE'

all_tours$TOUR_DUR_BIN_RECODE = all_tours$duration + 1 #all_tours$ANCHOR_ARRIVE_BIN_RECODE - all_tours$ANCHOR_DEPART_BIN_RECODE + 1

all_tours$tourdur_orig = all_tours$tourdur

all_tours$tourdur = all_tours$TOUR_DUR_BIN_RECODE
all_tours$start_hour_orig = all_tours$start_hour
all_tours$end_hour_orig = all_tours$end_hour

all_tours$start_hour = all_tours$ANCHOR_DEPART_BIN_RECODE
all_tours$end_hour = all_tours$ANCHOR_ARRIVE_BIN_RECODE

all_trips = merge(all_trips, bin_xwalk, by.x = 'depart', by.y = 'bin48', all.x = T)
names(all_trips)[names(all_trips)=="bin23"]   = 'DEP_BIN_RECODE'

all_trips$depart_hour_orig = all_trips$depart_hour

all_trips$depart_hour = all_trips$DEP_BIN_RECODE



# Separate all_trips and all_tours into indiv and joint
# ----------------------------------------------------
print("Splitting trips and tours into individual and joint trips and tours...")

unique_joint_tours <- all_tours[all_tours$tour_category == "joint",]
tours <- all_tours[all_tours$tour_category != "joint",]

jtrips <- all_trips[all_trips$tour_category == "joint",]
trips <- all_trips[all_trips$tour_category != "joint",]

unique_joint_tours$JOINT_PURP <- unique_joint_tours$TOURPURP


# Creating individual columns for up to 12 persons in each joint tour: PER1, PER2,... and PTYPE1, PTYPE2,....
max_participant_num <- max(jtour_participants$participant_num)
if (!"reshape2" %in% installed.packages()) install.packages("reshape2", repos='http://cran.us.r-project.org')
participant_pivot <- reshape2::dcast(jtour_participants, tour_id ~ participant_num, value.var = "person_id")

for(i in seq(1, 12)){
  perid_colname <- paste("PER", i, sep="")
  ifelse (i <= max_participant_num,
    colnames(participant_pivot)[colnames(participant_pivot)== i] <- perid_colname,
    participant_pivot[[perid_colname]] <- 0)
  pertype_colname <- paste("PTYPE", i, sep="")
  participant_pivot[[pertype_colname]] <- per$PERTYPE[match(participant_pivot[[perid_colname]], per$person_id)]
}
participant_pivot[is.na(participant_pivot)] <- 0

unique_joint_tours <- merge(unique_joint_tours, participant_pivot, by = c("tour_id"))


unique_joint_tours$NUMBER_HH <- (unique_joint_tours$PER1>0) + 
  (unique_joint_tours$PER2>0) + 
  (unique_joint_tours$PER3>0) + 
  (unique_joint_tours$PER4>0) + 
  (unique_joint_tours$PER5>0) + 
  (unique_joint_tours$PER6>0) + 
  (unique_joint_tours$PER7>0) + 
  (unique_joint_tours$PER8>0) + 
  (unique_joint_tours$PER9>0) + 
  (unique_joint_tours$PER10>0) + 
  (unique_joint_tours$PER11>0) + 
  (unique_joint_tours$PER12>0)


#create person level file for joint tours
jtours_per <- melt(unique_joint_tours[,c("household_id","tour_id", "PER1","PER2","PER3","PER4","PER5","PER6","PER7","PER8", "PER9", "PER10", "PER11", "PER12")], 
                   id = c("household_id","tour_id"))
jtours_per$value <- as.numeric(jtours_per$value)
colnames(jtours_per) <- c("household_id","tour_id","variable","person_id")
jtours_per$variable <- NULL
jtours_per <- jtours_per[jtours_per$person_id>0,]
jtours_per$finalweight <- hh$finalweight[match(jtours_per$household_id, hh$household_id)]

# create a combined temp tour file for creating stop freq model summary
temp_tour1 <- tours[,c("TOURPURP","num_ob_stops","num_ib_stops", "finalweight")]
temp_tour2 <- unique_joint_tours[,c("TOURPURP","num_ob_stops","num_ib_stops", "finalweight")]
colnames(temp_tour2) <- colnames(temp_tour1)
temp_tour <- rbind(temp_tour1,temp_tour2)

# code stop frequency model alternatives
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops==0] <- 1
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops==1] <- 2
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops==2] <- 3
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops>=3] <- 4
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops==0] <- 5
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops==1] <- 6
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops==2] <- 7
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops>=3] <- 8
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops==0] <- 9
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops==1] <- 10
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops==2] <- 11
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops>=3] <- 12
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops==0] <- 13
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops==1] <- 14
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops==2] <- 15
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops>=3] <- 16
temp_tour$STOP_FREQ_ALT[is.na(temp_tour$STOP_FREQ_ALT)] <- 0

stopFreqModel_summary <- xtabs(finalweight~STOP_FREQ_ALT+TOURPURP, data = temp_tour[temp_tour$TOURPURP<=10,])
write.csv(stopFreqModel_summary, "stopFreqModel_summary.csv", row.names = T)


# Create Stops tables
#------------------
stops <- trips[trips$stops==1,]

stops$finaldestTAZ[stops$inbound==0] <- stops$TOURDTAZ[stops$inbound==0]
stops$finaldestTAZ[stops$inbound==1] <- stops$TOUROTAZ[stops$inbound==1]


stops$od_dist <- DST_SKM$dist[match(paste(stops$OTAZ, stops$finaldestTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]
stops$os_dist <- DST_SKM$dist[match(paste(stops$OTAZ, stops$DTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]
stops$sd_dist <- DST_SKM$dist[match(paste(stops$DTAZ, stops$finaldestTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]

stops$out_dir_dist <- stops$os_dist + stops$sd_dist - stops$od_dist									


jstops <- jtrips[jtrips$stops==1,]

jstops$finaldestTAZ[jstops$inbound==0] <- jstops$TOURDTAZ[jstops$inbound==0]
jstops$finaldestTAZ[jstops$inbound==1] <- jstops$TOUROTAZ[jstops$inbound==1]

jstops$od_dist <- DST_SKM$dist[match(paste(jstops$OTAZ, jstops$finaldestTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]
jstops$os_dist <- DST_SKM$dist[match(paste(jstops$OTAZ, jstops$DTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]
jstops$sd_dist <- DST_SKM$dist[match(paste(jstops$DTAZ, jstops$finaldestTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]

jstops$out_dir_dist <- jstops$os_dist + jstops$sd_dist - jstops$od_dist		


#---------------------------------------------------------------------------

# Recode workrelated tours which are not at work subtour as work tour
#tours$TOURPURP[tours$TOURPURP == 10 & tours$IS_SUBTOUR == 0] <- 1

workCounts   <- count(tours, c("person_id"), "TOURPURP == 1") #[excluding at work subtours]
atWorkCounts <- count(tours, c("person_id"), "TOURPURP == 10")
schlCounts   <- count(tours, c("person_id"), "TOURPURP == 2 | TOURPURP == 3")
inmCounts    <- count(tours, c("person_id"), "TOURPURP>=4 & TOURPURP<=9")
itourCounts  <- count(tours, c("person_id"), "TOURPURP <= 9")  #number of tours per person [excluding at work subtours]
jtourCounts  <- count(jtours_per, c("person_id"), "tour_id>=0") 

# -----------------------
# added for calibration by nagendra.dhakar@rsginc.com
# for indivudal NM tour generation
tours_temp <- tours[,c("household_id", "person_id", "TOURPURP", "finalweight")]
tours_temp$FULLY_JOINT <- 0
jtours_temp <- jtours_per[,c("household_id", "person_id", "tour_id", "finalweight")]
# colnames(jtours_temp)[colnames(jtours_temp)=="perno"] <- "person_num"

jtours_temp$TOURPURP <- unique_joint_tours$JOINT_PURP[match(jtours_temp$tour_id, unique_joint_tours$JOINT_PURP)]

# jtours_temp$TOURPURP <- unique_joint_tours$JOINT_PURP[match(paste(jtours_temp$household_id, jtours_temp$tour_id), 
#                                                             paste(unique_joint_tours$household_id, unique_joint_tours$tour_id))]

jtours_temp$FULLY_JOINT <- 1
jtours_temp <- jtours_temp[,!(colnames(jtours_temp) %in% c("tour_id"))]

all_tours_temp <- rbind(tours_temp, jtours_temp)
#all_tours_temp$cdap <- hh$cdap_pattern[match(all_tours_temp$household_id, hh$household_id)]

workCounts_temp <- count(all_tours_temp, c("person_id"), "TOURPURP == 1")
schlCounts_temp <- count(all_tours_temp, c("person_id"), "TOURPURP == 2 | TOURPURP == 3")
inmCounts_temp <- count(all_tours_temp, c("person_id"), "TOURPURP>=4 & TOURPURP<=9 & FULLY_JOINT==0")  
atWorkCounts_temp <- count(all_tours_temp, c("person_id"), "TOURPURP == 10")
jToursCounts_temp <- count(all_tours_temp, c("person_id"), "FULLY_JOINT==1")  


colnames(workCounts_temp)[2] <- "freq_work"
colnames(schlCounts_temp)[2] <- "freq_schl"
colnames(inmCounts_temp)[2] <- "freq_inm"
colnames(atWorkCounts_temp)[2] <- "freq_atwork"
colnames(jToursCounts_temp)[2] <- "freq_jtours"

temp <- merge(workCounts_temp, schlCounts_temp, by = c("person_id"))
temp1 <- merge(temp, inmCounts_temp, by = c("person_id"))
temp1$freq_m <- temp1$freq_work + temp1$freq_schl
temp1$freq_itours <- temp1$freq_m+temp1$freq_inm

#joint tours
##identify persons that made joint tour
#temp_joint <- melt(unique_joint_tours[,c("household_id","tour_id" ,"PER1", "PER2", "PER3", "PER4", "PER5", "PER6", "PER7", "PER8")], id = c("household_id","tour_id"))
#colnames(temp_joint) <- c("household_id", "tour_id", "var", "person_num")
#temp_joint <- as.data.frame(temp_joint)
#temp_joint$person_num <- as.integer(temp_joint$person_num)
#temp_joint$joint<- 0
#temp_joint$joint[temp_joint$person_num>0] <- 1
#
#temp_joint <- temp_joint[temp_joint$joint==1,]
#person_unique_joint <- aggregate(joint~household_id+person_num, temp_joint, sum)

temp2 <- merge(temp1, jToursCounts_temp, by = c("person_id"), all = T)
temp2 <- merge(temp2, atWorkCounts_temp, by = c("person_id"), all = T)
temp2[is.na(temp2)] <- 0

#add number of joint tours to non-mandatory
temp2$freq_nm <- temp2$freq_inm + temp2$freq_jtours

#get person type
temp2$PERTYPE <- per$PERTYPE[match(temp2$person_id, per$person_id)]
#get finalweight
temp2$finalweight <- per$finalweight[match(temp2$person_id, per$person_id)]

#total tours
temp2$total_tours <- temp2$freq_nm+temp2$freq_m+temp2$freq_atwork

persons_mand <- temp2[temp2$freq_m>0,]   #persons with atleast 1 mandatory tours
persons_nomand <- temp2[temp2$freq_m==0,] #active persons with 0 mandatory tours

# joint tours counted as iNM for calibraiton purpose [model does not allow 0 iNM and >0 JT]

freq_nmtours_mand <- count(persons_mand, c("PERTYPE","freq_nm"), "finalweight")
freq_nmtours_nomand <- count(persons_nomand, c("PERTYPE","freq_nm"), "finalweight")
test <- count(temp2, c("PERTYPE","freq_inm","freq_m","freq_nm","freq_atwork"), "finalweight")
write.csv(test, "tour_rate_debug.csv", row.names = F)
write.csv(temp2,"temp2.csv", row.names = F)

# write.table("Non-Mandatory Tours for Persons with at-least 1 Mandatory Tour", "indivNMTourFreq.csv", sep = ",", row.names = F, append = F)
# write.table(freq_nmtours_mand, "indivNMTourFreq.csv", sep = ",", row.names = F, append = T)
# write.table("Non-Mandatory Tours for Active Persons with 0 Mandatory Tour", "indivNMTourFreq.csv", sep = ",", row.names = F, append = T)
# write.table(freq_nmtours_nomand, "indivNMTourFreq.csv", sep = ",", row.names = F, append = TRUE)

# end of addition for calibration
# -----------------------

joint5 <- count(unique_joint_tours, c("household_id"), "JOINT_PURP==5")
joint6 <- count(unique_joint_tours, c("household_id"), "JOINT_PURP==6")
joint7 <- count(unique_joint_tours, c("household_id"), "JOINT_PURP==7")
joint8 <- count(unique_joint_tours, c("household_id"), "JOINT_PURP==8")
joint9 <- count(unique_joint_tours, c("household_id"), "JOINT_PURP==9")

hh$joint5 <- joint5$freq[match(hh$household_id, joint5$household_id)]
hh$joint6 <- joint6$freq[match(hh$household_id, joint6$household_id)]
hh$joint7 <- joint7$freq[match(hh$household_id, joint7$household_id)]
hh$joint8 <- joint8$freq[match(hh$household_id, joint8$household_id)]
hh$joint9 <- joint9$freq[match(hh$household_id, joint9$household_id)]
hh$jtours <- hh$joint5+hh$joint6+hh$joint7+hh$joint8+hh$joint9

hh$joint5[is.na(hh$joint5)] <- 0
hh$joint6[is.na(hh$joint6)] <- 0
hh$joint7[is.na(hh$joint7)] <- 0
hh$joint8[is.na(hh$joint8)] <- 0
hh$joint9[is.na(hh$joint9)] <- 0
hh$jtours[is.na(hh$jtours)] <- 0

#joint tour indicator
hh$JOINT <- ifelse(hh$num_hh_joint_tours > 0, 1, 0)

# code JTF category
hh$jtf[hh$jtours==0] <- 1 
hh$jtf[hh$joint5==1] <- 2
hh$jtf[hh$joint6==1] <- 3
hh$jtf[hh$joint7==1] <- 4
hh$jtf[hh$joint8==1] <- 5
hh$jtf[hh$joint9==1] <- 6

hh$jtf[hh$joint5>=2] <- 7
hh$jtf[hh$joint6>=2] <- 8
hh$jtf[hh$joint7>=2] <- 9
hh$jtf[hh$joint8>=2] <- 10
hh$jtf[hh$joint9>=2] <- 11

hh$jtf[hh$joint5>=1 & hh$joint6>=1] <- 12
hh$jtf[hh$joint5>=1 & hh$joint7>=1] <- 13
hh$jtf[hh$joint5>=1 & hh$joint8>=1] <- 14
hh$jtf[hh$joint5>=1 & hh$joint9>=1] <- 15

hh$jtf[hh$joint6>=1 & hh$joint7>=1] <- 16
hh$jtf[hh$joint6>=1 & hh$joint8>=1] <- 17
hh$jtf[hh$joint6>=1 & hh$joint9>=1] <- 18

hh$jtf[hh$joint7>=1 & hh$joint8>=1] <- 19
hh$jtf[hh$joint7>=1 & hh$joint9>=1] <- 20

hh$jtf[hh$joint8>=1 & hh$joint9>=1] <- 21

per$workTours   <- workCounts$freq[match(per$person_id, workCounts$person_id)]
per$atWorkTours <- atWorkCounts$freq[match(per$person_id, atWorkCounts$person_id)]
per$schlTours   <- schlCounts$freq[match(per$person_id, schlCounts$person_id)]
per$inmTours    <- inmCounts$freq[match(per$person_id, inmCounts$person_id)]
per$inmTours[is.na(per$inmTours)] <- 0
per$inumTours <- itourCounts$freq[match(per$person_id, itourCounts$person_id)]
per$inumTours[is.na(per$inumTours)] <- 0
per$jnumTours <- jtourCounts$freq[match(per$person_id, jtourCounts$person_id)]
per$jnumTours[is.na(per$jnumTours)] <- 0
per$numTours <- per$inmTours + per$jnumTours

per$workTours[is.na(per$workTours)] <- 0
per$schlTours[is.na(per$schlTours)] <- 0
per$atWorkTours[is.na(per$atWorkTours)] <- 0

# Total tours by person type
per$numTours[is.na(per$numTours)] <- 0
toursPertypeDistbn <- count(tours[tours$PERTYPE>0 & tours$TOURPURP!=10,], c("PERTYPE"), "finalweight")
write.csv(toursPertypeDistbn, "toursPertypeDistbn.csv", row.names = TRUE)

# count joint tour fr each person type
temp_joint <- melt(unique_joint_tours[, c("household_id","tour_id","PTYPE1","PTYPE2","PTYPE3","PTYPE4","PTYPE5","PTYPE6","PTYPE7","PTYPE8","finalweight")], id = c("household_id", "tour_id", "finalweight"))
names(temp_joint)[names(temp_joint)=="value"] <- "PERTYPE"
jtoursPertypeDistbn <- count(temp_joint[temp_joint$PERTYPE>0,], c("PERTYPE"), "finalweight")
jtoursPertypeDistbn = merge(jtoursPertypeDistbn, pertypeCodes[pertypeCodes$code != 'All',], by.x = 'PERTYPE', by.y = 'code', all = TRUE)
jtoursPertypeDistbn$name = NULL
jtoursPertypeDistbn[is.na(jtoursPertypeDistbn)] = 0

# Total tours by person type for visualizer
totaltoursPertypeDistbn <- toursPertypeDistbn
totaltoursPertypeDistbn$freq <- totaltoursPertypeDistbn$freq + jtoursPertypeDistbn$freq
write.csv(totaltoursPertypeDistbn, "total_tours_by_pertype_vis.csv", row.names = F)


# Total indi NM tours by person type and purpose
tours_pertype_purpose <- count(tours[tours$TOURPURP>=4 & tours$TOURPURP<=9,], c("PERTYPE", "TOURPURP"), "finalweight")
write.csv(tours_pertype_purpose, "tours_pertype_purpose.csv", row.names = TRUE)

tours_pertype_purpose <- xtabs(freq~PERTYPE+TOURPURP, tours_pertype_purpose)
tours_pertype_purpose[is.na(tours_pertype_purpose)] <- 0
tours_pertype_purpose <- addmargins(as.table(tours_pertype_purpose))
tours_pertype_purpose <- as.data.frame.matrix(tours_pertype_purpose)

totalPersons <- sum(pertypeDistbn$freq)
totalPersons_DF <- data.frame("Total", totalPersons)
colnames(totalPersons_DF) <- colnames(pertypeDistbn)
pertypeDF <- rbind(pertypeDistbn, totalPersons_DF)
nm_tour_rates <- tours_pertype_purpose/pertypeDF$freq
nm_tour_rates$pertype <- row.names(nm_tour_rates)
nm_tour_rates <- melt(nm_tour_rates, id = c("pertype"))
colnames(nm_tour_rates) <- c("pertype", "tour_purp", "tour_rate")
nm_tour_rates$pertype <- as.character(nm_tour_rates$pertype)
nm_tour_rates$tour_purp <- as.character(nm_tour_rates$tour_purp)
nm_tour_rates$pertype[nm_tour_rates$pertype=="Sum"] <- "All"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp=="Sum"] <- "All"
nm_tour_rates$pertype <- pertypeCodes$name[match(nm_tour_rates$pertype, pertypeCodes$code)]

nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==4] <- "Escorting"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==5] <- "Shopping"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==6] <- "Maintenance"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==7] <- "EatingOut"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==8] <- "Visiting"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==9] <- "Discretionary"

write.csv(nm_tour_rates, "nm_tour_rates.csv", row.names = F)

# Total tours by purpose X tourtype
t1 <- wtd.hist(tours$TOURPURP[tours$TOURPURP<10], breaks = seq(1,10, by=1), freq = NULL, right=FALSE, weight=tours$finalweight[tours$TOURPURP<10])
t3 <- wtd.hist(unique_joint_tours$JOINT_PURP, breaks = seq(1,10, by=1), freq = NULL, right=FALSE, weight=unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP<10])
tours_purpose_type <- data.frame(t1$counts, t3$counts)
colnames(tours_purpose_type) <- c("indi", "joint")
write.csv(tours_purpose_type, "tours_purpose_type.csv", row.names = TRUE)

# DAP by pertype
dapSummary <- count(per, c("PERTYPE", "cdap_activity"), "finalweight")
write.csv(dapSummary, "dapSummary.csv", row.names = TRUE)

# Prepare DAP summary for visualizer
dapSummary_vis <- xtabs(freq~PERTYPE+cdap_activity, dapSummary)
dapSummary_vis <- addmargins(as.table(dapSummary_vis))
dapSummary_vis <- as.data.frame.matrix(dapSummary_vis)

dapSummary_vis$id <- row.names(dapSummary_vis)
dapSummary_vis <- melt(dapSummary_vis, id = c("id"))
colnames(dapSummary_vis) <- c("PERTYPE", "DAP", "freq")
dapSummary_vis$PERTYPE <- as.character(dapSummary_vis$PERTYPE)
dapSummary_vis$DAP <- as.character(dapSummary_vis$DAP)
dapSummary_vis <- dapSummary_vis[dapSummary_vis$DAP!="Sum",]
dapSummary_vis$PERTYPE[dapSummary_vis$PERTYPE=="Sum"] <- "Total"
write.csv(dapSummary_vis, "dapSummary_vis.csv", row.names = TRUE)

# HHSize X Joint
hhsizeJoint <- count(hh[hh$HHSIZE>=2,], c("HHSIZE", "JOINT"), "finalweight")
write.csv(hhsizeJoint, "hhsizeJoint.csv", row.names = TRUE)

#mandatory tour frequency
per$imf_choice <- 0
per$imf_choice[per$mandatory_tour_frequency == "work1"]           <- 1
per$imf_choice[per$mandatory_tour_frequency == "work2"]           <- 2
per$imf_choice[per$mandatory_tour_frequency == "school1"]         <- 3
per$imf_choice[per$mandatory_tour_frequency == "school2"]         <- 4
per$imf_choice[per$mandatory_tour_frequency == "work_and_school"] <- 5

mtfSummary <- count(per[per$imf_choice > 0,], c("PERTYPE", "imf_choice"), "finalweight")
write.csv(mtfSummary, "mtfSummary.csv")
#write.csv(tours, "tours_test.csv")

# Prepare MTF summary for visualizer
mtfSummary_vis <- xtabs(freq~PERTYPE+imf_choice, mtfSummary)
mtfSummary_vis <- addmargins(as.table(mtfSummary_vis))
mtfSummary_vis <- as.data.frame.matrix(mtfSummary_vis)

mtfSummary_vis$id <- row.names(mtfSummary_vis)
mtfSummary_vis <- melt(mtfSummary_vis, id = c("id"))
colnames(mtfSummary_vis) <- c("PERTYPE", "MTF", "freq")
mtfSummary_vis$PERTYPE <- as.character(mtfSummary_vis$PERTYPE)
mtfSummary_vis$MTF <- as.character(mtfSummary_vis$MTF)
mtfSummary_vis <- mtfSummary_vis[mtfSummary_vis$MTF!="Sum",]
mtfSummary_vis$PERTYPE[mtfSummary_vis$PERTYPE=="Sum"] <- "Total"
write.csv(mtfSummary_vis, "mtfSummary_vis.csv")

# indi NM summary
inm0Summary <- count(per[per$numTours==0,], c("PERTYPE"), "finalweight")
inm1Summary <- count(per[per$numTours==1,], c("PERTYPE"), "finalweight")
inm2Summary <- count(per[per$numTours==2,], c("PERTYPE"), "finalweight")
inm3Summary <- count(per[per$numTours>=3,], c("PERTYPE"), "finalweight")

inmSummary <- data.frame(PERTYPE = c(1,2,3,4,5,6,7,8))
inmSummary$tour0 <- inm0Summary$freq[match(inmSummary$PERTYPE, inm0Summary$PERTYPE)]
inmSummary$tour1 <- inm1Summary$freq[match(inmSummary$PERTYPE, inm1Summary$PERTYPE)]
inmSummary$tour2 <- inm2Summary$freq[match(inmSummary$PERTYPE, inm2Summary$PERTYPE)]
inmSummary$tour3pl <- inm3Summary$freq[match(inmSummary$PERTYPE, inm3Summary$PERTYPE)]

write.table(inmSummary, "innmSummary.csv", col.names=TRUE, sep=",")

# prepare INM summary for visualizer
inmSummary_vis <- melt(inmSummary, id=c("PERTYPE"))
inmSummary_vis$variable <- as.character(inmSummary_vis$variable)
inmSummary_vis$variable[inmSummary_vis$variable=="tour0"] <- "0"
inmSummary_vis$variable[inmSummary_vis$variable=="tour1"] <- "1"
inmSummary_vis$variable[inmSummary_vis$variable=="tour2"] <- "2"
inmSummary_vis$variable[inmSummary_vis$variable=="tour3pl"] <- "3pl"
inmSummary_vis <- xtabs(value~PERTYPE+variable, inmSummary_vis)
inmSummary_vis <- addmargins(as.table(inmSummary_vis))
inmSummary_vis <- as.data.frame.matrix(inmSummary_vis)

inmSummary_vis$id <- row.names(inmSummary_vis)
inmSummary_vis <- melt(inmSummary_vis, id = c("id"))
colnames(inmSummary_vis) <- c("PERTYPE", "nmtours", "freq")
inmSummary_vis$PERTYPE <- as.character(inmSummary_vis$PERTYPE)
inmSummary_vis$nmtours <- as.character(inmSummary_vis$nmtours)
inmSummary_vis <- inmSummary_vis[inmSummary_vis$nmtours!="Sum",]
inmSummary_vis$PERTYPE[inmSummary_vis$PERTYPE=="Sum"] <- "Total"
write.csv(inmSummary_vis, "inmSummary_vis.csv")

# Joint Tour Frequency and composition
jtfSummary <- count(hh[!is.na(hh$jtf),], c("jtf"), "finalweight")
jointComp <- count(unique_joint_tours, c("tour_composition"), "finalweight")
jointPartySize <- count(unique_joint_tours, c("NUMBER_HH"), "finalweight")
jointCompPartySize <- count(unique_joint_tours, c("tour_composition","NUMBER_HH"), "finalweight")

hh$jointCat[hh$jtours==0] <- 0
hh$jointCat[hh$jtours==1] <- 1
hh$jointCat[hh$jtours>=2] <- 2

jointToursHHSize <- count(hh[!is.na(hh$HHSIZE) & !is.na(hh$jointCat),], c("HHSIZE", "jointCat"), "finalweight")

# write.table(jtfSummary, "jtfSummary.csv", col.names=TRUE, sep=",")
# write.table(jointComp, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)
# write.table(jointPartySize, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)
# write.table(jointCompPartySize, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)
# write.table(jointToursHHSize, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)

#cap joint party size to 5+
jointPartySize$freq[jointPartySize$NUMBER_HH==5] <- sum(jointPartySize$freq[jointPartySize$NUMBER_HH>=5])
jointPartySize <- jointPartySize[jointPartySize$NUMBER_HH<=5, ]

jtf <- data.frame(jtf_code = seq(from = 1, to = 21), 
                  alt_name = c("No Joint Tours", "1 Shopping", "1 Maintenance", "1 Eating Out", "1 Visiting", "1 Other Discretionary", 
                               "2 Shopping", "1 Shopping / 1 Maintenance", "1 Shopping / 1 Eating Out", "1 Shopping / 1 Visiting", 
                               "1 Shopping / 1 Other Discretionary", "2 Maintenance", "1 Maintenance / 1 Eating Out", 
                               "1 Maintenance / 1 Visiting", "1 Maintenance / 1 Other Discretionary", "2 Eating Out", "1 Eating Out / 1 Visiting", 
                               "1 Eating Out / 1 Other Discretionary", "2 Visiting", "1 Visiting / 1 Other Discretionary", "2 Other Discretionary"))
jtf$freq <- jtfSummary$freq[match(jtf$jtf_code, jtfSummary$jtf)]
jtf[is.na(jtf)] <- 0

jointComp$tour_composition[jointComp$tour_composition==1] <- "All Adult"
jointComp$tour_composition[jointComp$tour_composition==2] <- "All Children"
jointComp$tour_composition[jointComp$tour_composition==3] <- "Mixed"

jointToursHHSizeProp <- xtabs(freq~jointCat+HHSIZE, jointToursHHSize[jointToursHHSize$HHSIZE>1,])
jointToursHHSizeProp <- addmargins(as.table(jointToursHHSizeProp))
jointToursHHSizeProp <- jointToursHHSizeProp[1:(nrow(jointToursHHSizeProp) - 1),]  #remove last row 
jointToursHHSizeProp <- prop.table(jointToursHHSizeProp, margin = 2)
jointToursHHSizeProp <- as.data.frame.matrix(jointToursHHSizeProp)
jointToursHHSizeProp <- jointToursHHSizeProp*100
jointToursHHSizeProp$jointTours <- row.names(jointToursHHSizeProp)
jointToursHHSizeProp <- melt(jointToursHHSizeProp, id = c("jointTours"))
colnames(jointToursHHSizeProp) <- c("jointTours", "hhsize", "freq")
jointToursHHSizeProp$hhsize <- as.character(jointToursHHSizeProp$hhsize)
jointToursHHSizeProp$hhsize[jointToursHHSizeProp$hhsize=="Sum"] <- "Total"

jointCompPartySize$tour_composition[jointCompPartySize$tour_composition==1] <- "All Adult"
jointCompPartySize$tour_composition[jointCompPartySize$tour_composition==2] <- "All Children"
jointCompPartySize$tour_composition[jointCompPartySize$tour_composition==3] <- "Mixed"

jointCompPartySizeProp <- xtabs(freq~tour_composition+NUMBER_HH, jointCompPartySize)
jointCompPartySizeProp <- addmargins(as.table(jointCompPartySizeProp))
# jointCompPartySizeProp <- jointCompPartySizeProp[1:(nrow(jointCompPartySizeProp) - 1),]  #remove last row 
# fixme: above line was removing totals in this case, which are used in the visualization - may need to edit with full sample?
jointCompPartySizeProp <- prop.table(jointCompPartySizeProp, margin = 1)
jointCompPartySizeProp <- as.data.frame.matrix(jointCompPartySizeProp)
jointCompPartySizeProp <- jointCompPartySizeProp*100
jointCompPartySizeProp$comp <- row.names(jointCompPartySizeProp)
jointCompPartySizeProp <- melt(jointCompPartySizeProp, id = c("comp"))
colnames(jointCompPartySizeProp) <- c("comp", "partysize", "freq")
jointCompPartySizeProp$comp <- as.character(jointCompPartySizeProp$comp)
jointCompPartySizeProp$comp[jointCompPartySizeProp$comp=="Sum"] <- "Total"

# Cap joint comp party size at 5
jointCompPartySizeProp <- jointCompPartySizeProp[jointCompPartySizeProp$partysize!="Sum",]
jointCompPartySizeProp$partysize <- as.numeric(as.character(jointCompPartySizeProp$partysize))
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Adult" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Adult" & jointCompPartySizeProp$partysize>=5])
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Children" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Children" & jointCompPartySizeProp$partysize>=5])
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Mixed" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Mixed" & jointCompPartySizeProp$partysize>=5])
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Total" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Total" & jointCompPartySizeProp$partysize>=5])

jointCompPartySizeProp <- jointCompPartySizeProp[jointCompPartySizeProp$partysize<=5,]


write.csv(jtf, "jtf.csv", row.names = F)
write.csv(jointComp, "jointComp.csv", row.names = F)
write.csv(jointPartySize, "jointPartySize.csv", row.names = F)
write.csv(jointCompPartySizeProp, "jointCompPartySize.csv", row.names = F)
write.csv(jointToursHHSizeProp, "jointToursHHSize.csv", row.names = F)

# Tour TOD Profile  ####
#------------------------------------
#work.dep <- table(cut(tours$ANCHOR_DEPART_BIN[!is.na(tours$ANCHOR_DEPART_BIN)], seq(1,48, by=1), right=FALSE))


tod1 <- wtd.hist(tours$start_hour[tours$TOURPURP==1], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1])
tod2 <- wtd.hist(tours$start_hour[tours$TOURPURP==2], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2])
tod3 <- wtd.hist(tours$start_hour[tours$TOURPURP==3], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3])
tod4 <- wtd.hist(tours$start_hour[tours$TOURPURP==4], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
todi56 <- wtd.hist(tours$start_hour[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
todi789 <- wtd.hist(tours$start_hour[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
todj56 <- wtd.hist(unique_joint_tours$start_hour[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
todj789 <- wtd.hist(unique_joint_tours$start_hour[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
tod15 <- wtd.hist(tours$start_hour[tours$TOURPURP==10], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

todDepProfile <- data.frame(tod1$counts, tod2$counts, tod3$counts, tod4$counts, todi56$counts, todi789$counts
                            , todj56$counts, todj789$counts, tod15$counts)
colnames(todDepProfile) <- c("work", "univ", "sch", "esc", "imain", "idisc", 
                             "jmain", "jdisc", "atwork")
write.csv(todDepProfile, "todDepProfile.csv")

# prepare input for visualizer
todDepProfile_vis <- todDepProfile
todDepProfile_vis$id <- row.names(todDepProfile_vis)
todDepProfile_vis <- melt(todDepProfile_vis, id = c("id"))
colnames(todDepProfile_vis) <- c("id", "purpose", "freq_dep")

todDepProfile_vis$purpose <- as.character(todDepProfile_vis$purpose)
todDepProfile_vis <- xtabs(freq_dep~id+purpose, todDepProfile_vis)
todDepProfile_vis <- addmargins(as.table(todDepProfile_vis))
todDepProfile_vis <- as.data.frame.matrix(todDepProfile_vis)
todDepProfile_vis$id <- row.names(todDepProfile_vis)
todDepProfile_vis <- melt(todDepProfile_vis, id = c("id"))
colnames(todDepProfile_vis) <- c("timebin", "PURPOSE", "freq")
todDepProfile_vis$PURPOSE <- as.character(todDepProfile_vis$PURPOSE)
todDepProfile_vis$timebin <- as.character(todDepProfile_vis$timebin)
todDepProfile_vis <- todDepProfile_vis[todDepProfile_vis$timebin!="Sum",]
todDepProfile_vis$PURPOSE[todDepProfile_vis$PURPOSE=="Sum"] <- "Total"
todDepProfile_vis$timebin <- as.numeric(todDepProfile_vis$timebin)

tod1 <- wtd.hist(tours$end_hour[tours$TOURPURP==1], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1])
tod2 <- wtd.hist(tours$end_hour[tours$TOURPURP==2], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2])
tod3 <- wtd.hist(tours$end_hour[tours$TOURPURP==3], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3])
tod4 <- wtd.hist(tours$end_hour[tours$TOURPURP==4], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
todi56 <- wtd.hist(tours$end_hour[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
todi789 <- wtd.hist(tours$end_hour[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
todj56 <- wtd.hist(unique_joint_tours$end_hour[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
todj789 <- wtd.hist(unique_joint_tours$end_hour[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
tod15 <- wtd.hist(tours$end_hour[tours$TOURPURP==10], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

todArrProfile <- data.frame(tod1$counts, tod2$counts, tod3$counts, tod4$counts, todi56$counts, todi789$counts
                            , todj56$counts, todj789$counts, tod15$counts)
colnames(todArrProfile) <- c("work", "univ", "sch", "esc", "imain", "idisc", 
                             "jmain", "jdisc", "atwork")
write.csv(todArrProfile, "todArrProfile.csv")

# prepare input for visualizer
todArrProfile_vis <- todArrProfile
todArrProfile_vis$id <- row.names(todArrProfile_vis)
todArrProfile_vis <- melt(todArrProfile_vis, id = c("id"))
colnames(todArrProfile_vis) <- c("id", "purpose", "freq_arr")

todArrProfile_vis$purpose <- as.character(todArrProfile_vis$purpose)
todArrProfile_vis <- xtabs(freq_arr~id+purpose, todArrProfile_vis)
todArrProfile_vis <- addmargins(as.table(todArrProfile_vis))
todArrProfile_vis <- as.data.frame.matrix(todArrProfile_vis)
todArrProfile_vis$id <- row.names(todArrProfile_vis)
todArrProfile_vis <- melt(todArrProfile_vis, id = c("id"))
colnames(todArrProfile_vis) <- c("timebin", "PURPOSE", "freq")
todArrProfile_vis$PURPOSE <- as.character(todArrProfile_vis$PURPOSE)
todArrProfile_vis$timebin <- as.character(todArrProfile_vis$timebin)
todArrProfile_vis <- todArrProfile_vis[todArrProfile_vis$timebin!="Sum",]
todArrProfile_vis$PURPOSE[todArrProfile_vis$PURPOSE=="Sum"] <- "Total"
todArrProfile_vis$timebin <- as.numeric(todArrProfile_vis$timebin)


tod1 <- wtd.hist(tours$tourdur[tours$TOURPURP==1], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1])
tod2 <- wtd.hist(tours$tourdur[tours$TOURPURP==2], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2])
tod3 <- wtd.hist(tours$tourdur[tours$TOURPURP==3], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3])
tod4 <- wtd.hist(tours$tourdur[tours$TOURPURP==4], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
todi56 <- wtd.hist(tours$tourdur[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
todi789 <- wtd.hist(tours$tourdur[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
todj56 <- wtd.hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
todj789 <- wtd.hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$finalweight[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
tod15 <- wtd.hist(tours$tourdur[tours$TOURPURP==10], breaks = seq(1,48, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

todDurProfile <- data.frame(tod1$counts, tod2$counts, tod3$counts, tod4$counts, todi56$counts, todi789$counts
                            , todj56$counts, todj789$counts, tod15$counts)
colnames(todDurProfile) <- c("work", "univ", "sch", "esc", "imain", "idisc", 
                             "jmain", "jdisc", "atwork")
write.csv(todDurProfile, "todDurProfile.csv")

# prepare input for visualizer
todDurProfile_vis <- todDurProfile
todDurProfile_vis$id <- row.names(todDurProfile_vis)
todDurProfile_vis <- melt(todDurProfile_vis, id = c("id"))
colnames(todDurProfile_vis) <- c("id", "purpose", "freq_dur")

todDurProfile_vis$purpose <- as.character(todDurProfile_vis$purpose)
todDurProfile_vis <- xtabs(freq_dur~id+purpose, todDurProfile_vis)
todDurProfile_vis <- addmargins(as.table(todDurProfile_vis))
todDurProfile_vis <- as.data.frame.matrix(todDurProfile_vis)
todDurProfile_vis$id <- row.names(todDurProfile_vis)
todDurProfile_vis <- melt(todDurProfile_vis, id = c("id"))
colnames(todDurProfile_vis) <- c("timebin", "PURPOSE", "freq")
todDurProfile_vis$PURPOSE <- as.character(todDurProfile_vis$PURPOSE)
todDurProfile_vis$timebin <- as.character(todDurProfile_vis$timebin)
todDurProfile_vis <- todDurProfile_vis[todDurProfile_vis$timebin!="Sum",]
todDurProfile_vis$PURPOSE[todDurProfile_vis$PURPOSE=="Sum"] <- "Total"
todDurProfile_vis$timebin <- as.numeric(todDurProfile_vis$timebin)

todDepProfile_vis <- todDepProfile_vis[order(todDepProfile_vis$timebin, todDepProfile_vis$PURPOSE), ]
todArrProfile_vis <- todArrProfile_vis[order(todArrProfile_vis$timebin, todArrProfile_vis$PURPOSE), ]
todDurProfile_vis <- todDurProfile_vis[order(todDurProfile_vis$timebin, todDurProfile_vis$PURPOSE), ]
todProfile_vis <- data.frame(todDepProfile_vis, todArrProfile_vis$freq, todDurProfile_vis$freq)
colnames(todProfile_vis) <- c("id", "purpose", "freq_dep", "freq_arr", "freq_dur")
write.csv(todProfile_vis, "todProfile_vis.csv", row.names = F)

unique_joint_tours$numberhh_wgt <- unique_joint_tours$finalweight*unique_joint_tours$NUMBER_HH

# Tour Mode X Auto Suff [person tours]
tmode1_as0 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==1 & tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1 & tours$AUTOSUFF==0])
tmode2_as0 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==2 & tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2 & tours$AUTOSUFF==0])
tmode3_as0 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==3 & tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3 & tours$AUTOSUFF==0])
tmode4_as0 <- wtd.hist(tours$TOURMODE[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==0])
tmode5_as0 <- wtd.hist(tours$TOURMODE[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==0])
tmode6_as0 <- wtd.hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==0])
tmode7_as0 <- wtd.hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==0])
tmode8_as0 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==10 & tours$AUTOSUFF==0], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10 & tours$AUTOSUFF==0])

tmodeAS0Profile <- data.frame(tmode1_as0$counts, tmode2_as0$counts, tmode3_as0$counts, tmode4_as0$counts,
                              tmode5_as0$counts, tmode6_as0$counts, tmode7_as0$counts, tmode8_as0$counts)
colnames(tmodeAS0Profile) <- c("work", "univ", "sch", "imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(tmodeAS0Profile, "tmodeAS0Profile.csv")

# Prepeare data for visualizer
tmodeAS0Profile_vis <- tmodeAS0Profile[1:11,]
tmodeAS0Profile_vis$id <- row.names(tmodeAS0Profile_vis)
tmodeAS0Profile_vis <- melt(tmodeAS0Profile_vis, id = c("id"))
colnames(tmodeAS0Profile_vis) <- c("id", "purpose", "freq_as0")

tmodeAS0Profile_vis <- xtabs(freq_as0~id+purpose, tmodeAS0Profile_vis)
tmodeAS0Profile_vis[is.na(tmodeAS0Profile_vis)] <- 0
tmodeAS0Profile_vis <- addmargins(as.table(tmodeAS0Profile_vis))
tmodeAS0Profile_vis <- as.data.frame.matrix(tmodeAS0Profile_vis)

tmodeAS0Profile_vis$id <- row.names(tmodeAS0Profile_vis)
tmodeAS0Profile_vis <- melt(tmodeAS0Profile_vis, id = c("id"))
colnames(tmodeAS0Profile_vis) <- c("id", "purpose", "freq_as0")
tmodeAS0Profile_vis$id <- as.character(tmodeAS0Profile_vis$id)
tmodeAS0Profile_vis$purpose <- as.character(tmodeAS0Profile_vis$purpose)
tmodeAS0Profile_vis <- tmodeAS0Profile_vis[tmodeAS0Profile_vis$id!="Sum",]
tmodeAS0Profile_vis$purpose[tmodeAS0Profile_vis$purpose=="Sum"] <- "Total"

tmode1_as1 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==1 & tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1 & tours$AUTOSUFF==1])
tmode2_as1 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==2 & tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2 & tours$AUTOSUFF==1])
tmode3_as1 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==3 & tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3 & tours$AUTOSUFF==1])
tmode4_as1 <- wtd.hist(tours$TOURMODE[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==1])
tmode5_as1 <- wtd.hist(tours$TOURMODE[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==1])
tmode6_as1 <- wtd.hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==1])
tmode7_as1 <- wtd.hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==1])
tmode8_as1 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==10 & tours$AUTOSUFF==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10 & tours$AUTOSUFF==1])

tmodeAS1Profile <- data.frame(tmode1_as1$counts, tmode2_as1$counts, tmode3_as1$counts, tmode4_as1$counts,
                              tmode5_as1$counts, tmode6_as1$counts, tmode7_as1$counts, tmode8_as1$counts)
colnames(tmodeAS1Profile) <- c("work", "univ", "sch", "imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(tmodeAS1Profile, "tmodeAS1Profile.csv")

# Prepeare data for visualizer 

tmodeAS1Profile_vis <- tmodeAS1Profile[1:11,]
tmodeAS1Profile_vis$id <- row.names(tmodeAS1Profile_vis)
tmodeAS1Profile_vis <- melt(tmodeAS1Profile_vis, id = c("id"))
colnames(tmodeAS1Profile_vis) <- c("id", "purpose", "freq_as1")

tmodeAS1Profile_vis <- xtabs(freq_as1~id+purpose, tmodeAS1Profile_vis)
tmodeAS1Profile_vis[is.na(tmodeAS1Profile_vis)] <- 0
tmodeAS1Profile_vis <- addmargins(as.table(tmodeAS1Profile_vis))
tmodeAS1Profile_vis <- as.data.frame.matrix(tmodeAS1Profile_vis)

tmodeAS1Profile_vis$id <- row.names(tmodeAS1Profile_vis)
tmodeAS1Profile_vis <- melt(tmodeAS1Profile_vis, id = c("id"))
colnames(tmodeAS1Profile_vis) <- c("id", "purpose", "freq_as1")
tmodeAS1Profile_vis$id <- as.character(tmodeAS1Profile_vis$id)
tmodeAS1Profile_vis$purpose <- as.character(tmodeAS1Profile_vis$purpose)
tmodeAS1Profile_vis <- tmodeAS1Profile_vis[tmodeAS1Profile_vis$id!="Sum",]
tmodeAS1Profile_vis$purpose[tmodeAS1Profile_vis$purpose=="Sum"] <- "Total"

tmode1_as2 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==1 & tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1 & tours$AUTOSUFF==2])
tmode2_as2 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==2 & tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2 & tours$AUTOSUFF==2])
tmode3_as2 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==3 & tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3 & tours$AUTOSUFF==2])
tmode4_as2 <- wtd.hist(tours$TOURMODE[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==2])
tmode5_as2 <- wtd.hist(tours$TOURMODE[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==2])
tmode6_as2 <- wtd.hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==2])
tmode7_as2 <- wtd.hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==2])
tmode8_as2 <- wtd.hist(tours$TOURMODE[tours$TOURPURP==10 & tours$AUTOSUFF==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10 & tours$AUTOSUFF==2])

tmodeAS2Profile <- data.frame(tmode1_as2$counts, tmode2_as2$counts, tmode3_as2$counts, tmode4_as2$counts,
                              tmode5_as2$counts, tmode6_as2$counts, tmode7_as2$counts, tmode8_as2$counts)
colnames(tmodeAS2Profile) <- c("work", "univ", "sch", "imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(tmodeAS2Profile, "tmodeAS2Profile.csv")

# Prepeare data for visualizer
tmodeAS2Profile_vis <- tmodeAS2Profile[1:11,]
tmodeAS2Profile_vis$id <- row.names(tmodeAS2Profile_vis)
tmodeAS2Profile_vis <- melt(tmodeAS2Profile_vis, id = c("id"))
colnames(tmodeAS2Profile_vis) <- c("id", "purpose", "freq_as2")

tmodeAS2Profile_vis <- xtabs(freq_as2~id+purpose, tmodeAS2Profile_vis)
tmodeAS2Profile_vis[is.na(tmodeAS2Profile_vis)] <- 0
tmodeAS2Profile_vis <- addmargins(as.table(tmodeAS2Profile_vis))
tmodeAS2Profile_vis <- as.data.frame.matrix(tmodeAS2Profile_vis)

tmodeAS2Profile_vis$id <- row.names(tmodeAS2Profile_vis)
tmodeAS2Profile_vis <- melt(tmodeAS2Profile_vis, id = c("id"))
colnames(tmodeAS2Profile_vis) <- c("id", "purpose", "freq_as2")
tmodeAS2Profile_vis$id <- as.character(tmodeAS2Profile_vis$id)
tmodeAS2Profile_vis$purpose <- as.character(tmodeAS2Profile_vis$purpose)
tmodeAS2Profile_vis <- tmodeAS2Profile_vis[tmodeAS2Profile_vis$id!="Sum",]
tmodeAS2Profile_vis$purpose[tmodeAS2Profile_vis$purpose=="Sum"] <- "Total"


# Combine three AS groups
tmodeProfile_vis <- data.frame(tmodeAS0Profile_vis, tmodeAS1Profile_vis$freq_as1, tmodeAS2Profile_vis$freq_as2)
colnames(tmodeProfile_vis) <- c("id", "purpose", "freq_as0", "freq_as1", "freq_as2")
tmodeProfile_vis$freq_all <- tmodeProfile_vis$freq_as0 + tmodeProfile_vis$freq_as1 + tmodeProfile_vis$freq_as2
write.csv(tmodeProfile_vis, "tmodeProfile_vis.csv", row.names = F)


# Non Mand Tour lengths
# ------------------------------------
tourdist4    <- wtd.hist(tours$SKIMDIST[tours$TOURPURP==4], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
tourdisti56  <- wtd.hist(tours$SKIMDIST[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
tourdisti789 <- wtd.hist(tours$SKIMDIST[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
tourdistj56  <- wtd.hist(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
tourdistj789 <- wtd.hist(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
tourdist10   <- wtd.hist(tours$SKIMDIST[tours$TOURPURP==10], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

tourDistProfile <- data.frame(tourdist4$counts, tourdisti56$counts, tourdisti789$counts, tourdistj56$counts, tourdistj789$counts, tourdist10$counts)

colnames(tourDistProfile) <- c("esco", "imain", "idisc", "jmain", "jdisc", "atwork")

write.csv(tourDistProfile, "nonMandTourDistProfile.csv")

#prepare input for visualizer
tourDistProfile_vis <- tourDistProfile
tourDistProfile_vis$id <- row.names(tourDistProfile_vis)
tourDistProfile_vis <- melt(tourDistProfile_vis, id = c("id"))
colnames(tourDistProfile_vis) <- c("id", "purpose", "freq")

tourDistProfile_vis <- xtabs(freq~id+purpose, tourDistProfile_vis)
tourDistProfile_vis <- addmargins(as.table(tourDistProfile_vis))
tourDistProfile_vis <- as.data.frame.matrix(tourDistProfile_vis)
tourDistProfile_vis$id <- row.names(tourDistProfile_vis)
tourDistProfile_vis <- melt(tourDistProfile_vis, id = c("id"))
colnames(tourDistProfile_vis) <- c("distbin", "PURPOSE", "freq")
tourDistProfile_vis$PURPOSE <- as.character(tourDistProfile_vis$PURPOSE)
tourDistProfile_vis$distbin <- as.character(tourDistProfile_vis$distbin)
tourDistProfile_vis <- tourDistProfile_vis[tourDistProfile_vis$distbin!="Sum",]
tourDistProfile_vis$PURPOSE[tourDistProfile_vis$PURPOSE=="Sum"] <- "Total"
tourDistProfile_vis$distbin <- as.numeric(tourDistProfile_vis$distbin)

write.csv(tourDistProfile_vis, "tourDistProfile_vis.csv", row.names = F)

cat("\n Average Tour Distance [esco]: ", weighted.mean(tours$SKIMDIST[tours$TOURPURP==4], tours$finalweight[tours$TOURPURP==4], na.rm = TRUE))
cat("\n Average Tour Distance [imain]: ", weighted.mean(tours$SKIMDIST[tours$TOURPURP>=5 & tours$TOURPURP<=6], tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6], na.rm = TRUE))
cat("\n Average Tour Distance [idisc]: ", weighted.mean(tours$SKIMDIST[tours$TOURPURP>=7 & tours$TOURPURP<=9], tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9], na.rm = TRUE))
cat("\n Average Tour Distance [jmain]: ", weighted.mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], na.rm = TRUE))
cat("\n Average Tour Distance [jdisc]: ", weighted.mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], na.rm = TRUE))
cat("\n Average Tour Distance [atwork]: ", weighted.mean(tours$SKIMDIST[tours$TOURPURP==10], tours$finalweight[tours$TOURPURP==10], na.rm = TRUE))
cat("\n\n")

## Retirees
#cat("\n Average Tour Distance [esco]: ", mean(tours$SKIMDIST[tours$TOURPURP==4 & tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [imain]: ", mean(tours$SKIMDIST[tours$TOURPURP>=5 & tours$TOURPURP<=6 & tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [idisc]: ", mean(tours$SKIMDIST[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [jmain]: ", mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [jdisc]: ", mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [atwork]: ", mean(tours$SKIMDIST[tours$TOURPURP==10 & tours$PERTYPE==5], na.rm = TRUE))
#
## Non-reitrees
#cat("\n Average Tour Distance [esco]: ", mean(tours$SKIMDIST[tours$TOURPURP==4 & tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [imain]: ", mean(tours$SKIMDIST[tours$TOURPURP>=5 & tours$TOURPURP<=6 & tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [idisc]: ", mean(tours$SKIMDIST[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [jmain]: ", mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [jdisc]: ", mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [atwork]: ", mean(tours$SKIMDIST[tours$TOURPURP==10 & tours$PERTYPE!=5], na.rm = TRUE))
#

## Output average trips lengths for visualizer

avgTourLengths <- c(weighted.mean(tours$SKIMDIST[tours$TOURPURP==4], tours$finalweight[tours$TOURPURP==4], na.rm = TRUE),
                    weighted.mean(tours$SKIMDIST[tours$TOURPURP>=5 & tours$TOURPURP<=6], tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6], na.rm = TRUE),
                    weighted.mean(tours$SKIMDIST[tours$TOURPURP>=7 & tours$TOURPURP<=9], tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9], na.rm = TRUE),
                    weighted.mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], 
                                  unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], 
                                  na.rm = TRUE),
                    weighted.mean(unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], na.rm = TRUE),
                    weighted.mean(tours$SKIMDIST[tours$TOURPURP==10], tours$finalweight[tours$TOURPURP==10], na.rm = TRUE))

# totAvgNonMand <- mean(c(tours$SKIMDIST[tours$TOURPURP %in% c(4,5,6,7,8,9,10)], 
#                             unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP %in% c(5,6,7,8,9)]), 
#                       na.rm = T)
nmSkimDist <- c(tours$SKIMDIST[tours$TOURPURP %in% c(4,5,6,7,8,9,10)],
                unique_joint_tours$SKIMDIST[unique_joint_tours$JOINT_PURP %in% c(5,6,7,8,9)])
nmWeights  <- c(tours$finalweight[tours$TOURPURP %in% c(4,5,6,7,8,9,10)],
                unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP %in% c(5,6,7,8,9)])
totAvgNonMand <- weighted.mean(nmSkimDist, nmWeights, na.rm = TRUE)

avgTourLengths <- c(avgTourLengths, totAvgNonMand)

nonMandTourPurpose <- c("esco", "imain", "idisc", "jmain", "jdisc", "atwork", "Total")

nonMandTourLengths <- data.frame(purpose = nonMandTourPurpose, avgTourLength = avgTourLengths)

write.csv(nonMandTourLengths, "nonMandTourLengths.csv", row.names = F)



# Stop Frequency
#----------------------------------------
#Outbound
stopfreq1 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP==1], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1])
stopfreq2 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP==2], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2])
stopfreq3 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP==3], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3])
stopfreq4 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP==4], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
stopfreqi56 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
stopfreqi789 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
stopfreqj56 <- wtd.hist(unique_joint_tours$num_ob_stops[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
stopfreqj789 <- wtd.hist(unique_joint_tours$num_ob_stops[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
stopfreq10 <- wtd.hist(tours$num_ob_stops[tours$TOURPURP==10], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopFreqOutProfile.csv")

# prepare stop frequency input for visualizer
stopFreqout_vis <- stopFreq
stopFreqout_vis$id <- row.names(stopFreqout_vis)
stopFreqout_vis <- melt(stopFreqout_vis, id = c("id"))
colnames(stopFreqout_vis) <- c("id", "purpose", "freq")

stopFreqout_vis <- xtabs(freq~purpose+id, stopFreqout_vis)
stopFreqout_vis <- addmargins(as.table(stopFreqout_vis))
stopFreqout_vis <- as.data.frame.matrix(stopFreqout_vis)
stopFreqout_vis$id <- row.names(stopFreqout_vis)
stopFreqout_vis <- melt(stopFreqout_vis, id = c("id"))
colnames(stopFreqout_vis) <- c("purpose", "nstops", "freq")
stopFreqout_vis$purpose <- as.character(stopFreqout_vis$purpose)
stopFreqout_vis$nstops <- as.character(stopFreqout_vis$nstops)
stopFreqout_vis <- stopFreqout_vis[stopFreqout_vis$nstops!="Sum",]
stopFreqout_vis$purpose[stopFreqout_vis$purpose=="Sum"] <- "Total"

#Inbound
stopfreq1 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP==1], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1])
stopfreq2 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP==2], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2])
stopfreq3 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP==3], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3])
stopfreq4 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP==4], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
stopfreqi56 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
stopfreqi789 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
stopfreqj56 <- wtd.hist(unique_joint_tours$num_ib_stops[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
stopfreqj789 <- wtd.hist(unique_joint_tours$num_ib_stops[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
stopfreq10 <- wtd.hist(tours$num_ib_stops[tours$TOURPURP==10], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopFreqInbProfile.csv")

# prepare stop frequency input for visualizer
stopFreqinb_vis <- stopFreq
stopFreqinb_vis$id <- row.names(stopFreqinb_vis)
stopFreqinb_vis <- melt(stopFreqinb_vis, id = c("id"))
colnames(stopFreqinb_vis) <- c("id", "purpose", "freq")

stopFreqinb_vis <- xtabs(freq~purpose+id, stopFreqinb_vis)
stopFreqinb_vis <- addmargins(as.table(stopFreqinb_vis))
stopFreqinb_vis <- as.data.frame.matrix(stopFreqinb_vis)
stopFreqinb_vis$id <- row.names(stopFreqinb_vis)
stopFreqinb_vis <- melt(stopFreqinb_vis, id = c("id"))
colnames(stopFreqinb_vis) <- c("purpose", "nstops", "freq")
stopFreqinb_vis$purpose <- as.character(stopFreqinb_vis$purpose)
stopFreqinb_vis$nstops <- as.character(stopFreqinb_vis$nstops)
stopFreqinb_vis <- stopFreqinb_vis[stopFreqinb_vis$nstops!="Sum",]
stopFreqinb_vis$purpose[stopFreqinb_vis$purpose=="Sum"] <- "Total"


stopfreqDir_vis <- data.frame(stopFreqout_vis, stopFreqinb_vis$freq)
colnames(stopfreqDir_vis) <- c("purpose", "nstops", "freq_out", "freq_inb")
write.csv(stopfreqDir_vis, "stopfreqDir_vis.csv", row.names = F)


#Total
stopfreq1 <- wtd.hist(tours$num_tot_stops[tours$TOURPURP==1], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==1])
stopfreq2 <- wtd.hist(tours$num_tot_stops[tours$TOURPURP==2], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==2])
stopfreq3 <- wtd.hist(tours$num_tot_stops[tours$TOURPURP==3], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==3])
stopfreq4 <- wtd.hist(tours$num_tot_stops[tours$TOURPURP==4], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==4])
stopfreqi56  <- wtd.hist(tours$num_tot_stops[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=5 & tours$TOURPURP<=6])
stopfreqi789 <- wtd.hist(tours$num_tot_stops[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP>=7 & tours$TOURPURP<=9])
stopfreqj56 <- wtd.hist(unique_joint_tours$num_tot_stops[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6])
stopfreqj789 <- wtd.hist(unique_joint_tours$num_tot_stops[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = unique_joint_tours$numberhh_wgt[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9])
stopfreq10 <- wtd.hist(tours$num_tot_stops[tours$TOURPURP==10], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE, weight = tours$finalweight[tours$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopFreqTotProfile.csv")

# prepare stop frequency input for visualizer
stopFreq_vis <- stopFreq
stopFreq_vis$id <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("id"))
colnames(stopFreq_vis) <- c("id", "purpose", "freq")

stopFreq_vis <- xtabs(freq~purpose+id, stopFreq_vis)
stopFreq_vis <- addmargins(as.table(stopFreq_vis))
stopFreq_vis <- as.data.frame.matrix(stopFreq_vis)
stopFreq_vis$id <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("id"))
colnames(stopFreq_vis) <- c("purpose", "nstops", "freq")
stopFreq_vis$purpose <- as.character(stopFreq_vis$purpose)
stopFreq_vis$nstops <- as.character(stopFreq_vis$nstops)
stopFreq_vis <- stopFreq_vis[stopFreq_vis$nstops!="Sum",]
stopFreq_vis$purpose[stopFreq_vis$purpose=="Sum"] <- "Total"

write.csv(stopFreq_vis, "stopfreq_total_vis.csv", row.names = F)

jstops$nump_wgt <- jstops$finalweight*jstops$num_participants

#Stop purpose X TourPurpose
stopfreq1 <- wtd.hist(stops$DPURP[stops$TOURPURP==1], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==1])
stopfreq2 <- wtd.hist(stops$DPURP[stops$TOURPURP==2], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==2])
stopfreq3 <- wtd.hist(stops$DPURP[stops$TOURPURP==3], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==3])
stopfreq4 <- wtd.hist(stops$DPURP[stops$TOURPURP==4], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==4])
stopfreqi56 <-  wtd.hist(stops$DPURP[stops$TOURPURP>=5 & stops$TOURPURP<=6], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP>=5 & stops$TOURPURP<=6])
stopfreqi789 <- wtd.hist(stops$DPURP[stops$TOURPURP>=7 & stops$TOURPURP<=9], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP>=7 & stops$TOURPURP<=9])
stopfreqj56  <- wtd.hist(jstops$DPURP[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = jstops$finalweight[jstops$TOURPURP>=5 & jstops$TOURPURP<=6])
stopfreqj789 <- wtd.hist(jstops$DPURP[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = jstops$finalweight[jstops$TOURPURP>=7 & jstops$TOURPURP<=9])
stopfreq10 <- wtd.hist(stops$DPURP[stops$TOURPURP==10], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopPurposeByTourPurpose.csv")

# prepare stop frequency input for visualizer
stopFreq_vis <- stopFreq
stopFreq_vis$id <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("id"))
colnames(stopFreq_vis) <- c("stop_purp", "purpose", "freq")

stopFreq_vis <- xtabs(freq~purpose+stop_purp, stopFreq_vis)
stopFreq_vis <- addmargins(as.table(stopFreq_vis))
stopFreq_vis <- as.data.frame.matrix(stopFreq_vis)
stopFreq_vis$purpose <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("purpose"))
colnames(stopFreq_vis) <- c("purpose", "stop_purp", "freq")
stopFreq_vis$purpose <- as.character(stopFreq_vis$purpose)
stopFreq_vis$stop_purp <- as.character(stopFreq_vis$stop_purp)
stopFreq_vis <- stopFreq_vis[stopFreq_vis$stop_purp!="Sum",]
stopFreq_vis$purpose[stopFreq_vis$purpose=="Sum"] <- "Total"

write.csv(stopFreq_vis, "stoppurpose_tourpurpose_vis.csv", row.names = F)

#Out of direction - Stop Location
stopfreq1 <- wtd.hist(stops$out_dir_dist[stops$TOURPURP==1], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==1])
stopfreq2 <- wtd.hist(stops$out_dir_dist[stops$TOURPURP==2], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==2])
stopfreq3 <- wtd.hist(stops$out_dir_dist[stops$TOURPURP==3], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==3])
stopfreq4 <- wtd.hist(stops$out_dir_dist[stops$TOURPURP==4], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==4])
stopfreqi56  <- wtd.hist(stops$out_dir_dist[stops$TOURPURP>=5 & stops$TOURPURP<=6], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP>=5 & stops$TOURPURP<=6])
stopfreqi789 <- wtd.hist(stops$out_dir_dist[stops$TOURPURP>=7 & stops$TOURPURP<=9], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP>=7 & stops$TOURPURP<=9])
stopfreqj56  <- wtd.hist(jstops$out_dir_dist[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = jstops$finalweight[jstops$TOURPURP>=5 & jstops$TOURPURP<=6])
stopfreqj789 <- wtd.hist(jstops$out_dir_dist[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = jstops$finalweight[jstops$TOURPURP>=7 & jstops$TOURPURP<=9])
stopfreq10 <- wtd.hist(stops$out_dir_dist[stops$TOURPURP==10], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopOutOfDirectionDC.csv")

# prepare stop location input for visualizer
stopDC_vis <- stopFreq
stopDC_vis$id <- row.names(stopDC_vis)
stopDC_vis <- melt(stopDC_vis, id = c("id"))
colnames(stopDC_vis) <- c("id", "purpose", "freq")

stopDC_vis <- xtabs(freq~id+purpose, stopDC_vis)
stopDC_vis <- addmargins(as.table(stopDC_vis))
stopDC_vis <- as.data.frame.matrix(stopDC_vis)
stopDC_vis$id <- row.names(stopDC_vis)
stopDC_vis <- melt(stopDC_vis, id = c("id"))
colnames(stopDC_vis) <- c("distbin", "PURPOSE", "freq")
stopDC_vis$PURPOSE <- as.character(stopDC_vis$PURPOSE)
stopDC_vis$distbin <- as.character(stopDC_vis$distbin)
stopDC_vis <- stopDC_vis[stopDC_vis$distbin!="Sum",]
stopDC_vis$PURPOSE[stopDC_vis$PURPOSE=="Sum"] <- "Total"
stopDC_vis$distbin <- as.numeric(stopDC_vis$distbin)

write.csv(stopDC_vis, "stopDC_vis.csv", row.names = F)

# compute average out of dir distance for visualizer
avgDistances <- c(weighted.mean(stops$out_dir_dist[stops$TOURPURP==1], weight = stops$finalweight[stops$TOURPURP==1], na.rm = TRUE),
                  weighted.mean(stops$out_dir_dist[stops$TOURPURP==2], weight = stops$finalweight[stops$TOURPURP==2], na.rm = TRUE),
                  weighted.mean(stops$out_dir_dist[stops$TOURPURP==3], weight = stops$finalweight[stops$TOURPURP==3], na.rm = TRUE),
                  weighted.mean(stops$out_dir_dist[stops$TOURPURP==4], weight = stops$finalweight[stops$TOURPURP==4], na.rm = TRUE),
                  weighted.mean(stops$out_dir_dist[stops$TOURPURP>=5 & stops$TOURPURP<=6], weight = stops$finalweight[stops$TOURPURP>=5 & stops$TOURPURP<=6], na.rm = TRUE),
                  weighted.mean(stops$out_dir_dist[stops$TOURPURP>=7 & stops$TOURPURP<=9], weight = stops$finalweight[stops$TOURPURP>=7 & stops$TOURPURP<=9], na.rm = TRUE),
                  weighted.mean(jstops$out_dir_dist[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], weight = jstops$finalweight[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], na.rm = TRUE),
                  weighted.mean(jstops$out_dir_dist[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], weight = jstops$finalweight[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], na.rm = TRUE),
                  weighted.mean(stops$out_dir_dist[stops$TOURPURP==10], weight = stops$finalweight[stops$TOURPURP==10], na.rm = TRUE))

purp <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork", "total")

###
stopsDist <- c(stops$out_dir_dist[stops$TOURPURP %in% c(1, 2, 3,4,5,6,7,8,9, 10)], 
                              jstops$out_dir_dist[jstops$TOURPURP %in% c(5,6,7,8,9)])
stopsWeights  <- c(stops$finalweight[stops$TOURPURP %in% c(1, 2, 3,4,5,6,7,8,9, 10)], 
                                  jstops$finalweight[jstops$TOURPURP %in% c(5,6,7,8,9)])

totAvgStopDist <- weighted.mean(stopsDist, stopsWeights, na.rm = TRUE)
# stopsDist <- c(stops$out_dir_dist[stops$TOURPURP %in% c(1, 2, 3,4,5,6,7,8,9, 10)], 
#                jstops$out_dir_dist[jstops$TOURPURP %in% c(5,6,7,8,9)])
# 
# totAvgStopDist <- mean(stopsDist, na.rm = TRUE)

avgDistances <- c(avgDistances, totAvgStopDist)

###

avgStopOutofDirectionDist <- data.frame(purpose = purp, avgDist = avgDistances)

write.csv(avgStopOutofDirectionDist, "avgStopOutofDirectionDist_vis.csv", row.names = F)

#Stop Departure Time
stopfreq1 <- wtd.hist(stops$depart_hour[stops$TOURPURP==1], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==1])
stopfreq2 <- wtd.hist(stops$depart_hour[stops$TOURPURP==2], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==2])
stopfreq3 <- wtd.hist(stops$depart_hour[stops$TOURPURP==3], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==3])
stopfreq4 <- wtd.hist(stops$depart_hour[stops$TOURPURP==4], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==4])
stopfreqi56  <- wtd.hist(stops$depart_hour[stops$TOURPURP>=5 & stops$TOURPURP<=6], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP>=5 & stops$TOURPURP<=6])
stopfreqi789 <- wtd.hist(stops$depart_hour[stops$TOURPURP>=7 & stops$TOURPURP<=9], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP>=7 & stops$TOURPURP<=9])
stopfreqj56 <- wtd.hist(jstops$depart_hour[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = jstops$finalweight[jstops$TOURPURP>=5 & jstops$TOURPURP<=6])
stopfreqj789 <- wtd.hist(jstops$depart_hour[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = jstops$finalweight[jstops$TOURPURP>=7 & jstops$TOURPURP<=9])
stopfreq10 <- wtd.hist(stops$depart_hour[stops$TOURPURP==10], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = stops$finalweight[stops$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopDeparture.csv")

# prepare stop departure input for visualizer
stopDep_vis <- stopFreq
stopDep_vis$id <- row.names(stopDep_vis)
stopDep_vis <- melt(stopDep_vis, id = c("id"))
colnames(stopDep_vis) <- c("id", "purpose", "freq_stop")

stopDep_vis$purpose <- as.character(stopDep_vis$purpose)
stopDep_vis <- xtabs(freq_stop~id+purpose, stopDep_vis)
stopDep_vis <- addmargins(as.table(stopDep_vis))
stopDep_vis <- as.data.frame.matrix(stopDep_vis)
stopDep_vis$id <- row.names(stopDep_vis)
stopDep_vis <- melt(stopDep_vis, id = c("id"))
colnames(stopDep_vis) <- c("timebin", "PURPOSE", "freq")
stopDep_vis$PURPOSE <- as.character(stopDep_vis$PURPOSE)
stopDep_vis$timebin <- as.character(stopDep_vis$timebin)
stopDep_vis <- stopDep_vis[stopDep_vis$timebin!="Sum",]
stopDep_vis$PURPOSE[stopDep_vis$PURPOSE=="Sum"] <- "Total"
stopDep_vis$timebin <- as.numeric(stopDep_vis$timebin)

#Trip Departure Time
stopfreq1 <- wtd.hist(trips$depart_hour[trips$TOURPURP==1], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP==1])
stopfreq2 <- wtd.hist(trips$depart_hour[trips$TOURPURP==2], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP==2])
stopfreq3 <- wtd.hist(trips$depart_hour[trips$TOURPURP==3], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP==3])
stopfreq4 <- wtd.hist(trips$depart_hour[trips$TOURPURP==4], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP==4])
stopfreqi56 <- wtd.hist(trips$depart_hour[trips$TOURPURP>=5 & trips$TOURPURP<=6], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP>=5 & trips$TOURPURP<=6])
stopfreqi789 <- wtd.hist(trips$depart_hour[trips$TOURPURP>=7 & trips$TOURPURP<=9], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP>=7 & trips$TOURPURP<=9])
stopfreqj56 <- wtd.hist(jtrips$depart_hour[jtrips$TOURPURP>=5 & jtrips$TOURPURP<=6], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = jtrips$finalweight[jtrips$TOURPURP>=5 & jtrips$TOURPURP<=6])
stopfreqj789 <- wtd.hist(jtrips$depart_hour[jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = jtrips$finalweight[jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9])
stopfreq10 <- wtd.hist(trips$depart_hour[trips$TOURPURP==10], breaks =  c(seq(1,48, by=1), 9999), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TOURPURP==10])

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "tripDeparture.csv")

# prepare stop departure input for visualizer
tripDep_vis <- stopFreq
tripDep_vis$id <- row.names(tripDep_vis)
tripDep_vis <- melt(tripDep_vis, id = c("id"))
colnames(tripDep_vis) <- c("id", "purpose", "freq_trip")

tripDep_vis$purpose <- as.character(tripDep_vis$purpose)
tripDep_vis <- xtabs(freq_trip~id+purpose, tripDep_vis)
tripDep_vis <- addmargins(as.table(tripDep_vis))
tripDep_vis <- as.data.frame.matrix(tripDep_vis)
tripDep_vis$id <- row.names(tripDep_vis)
tripDep_vis <- melt(tripDep_vis, id = c("id"))
colnames(tripDep_vis) <- c("timebin", "PURPOSE", "freq")
tripDep_vis$PURPOSE <- as.character(tripDep_vis$PURPOSE)
tripDep_vis$timebin <- as.character(tripDep_vis$timebin)
tripDep_vis <- tripDep_vis[tripDep_vis$timebin!="Sum",]
tripDep_vis$PURPOSE[tripDep_vis$PURPOSE=="Sum"] <- "Total"
tripDep_vis$timebin <- as.numeric(tripDep_vis$timebin)

stopTripDep_vis <- data.frame(stopDep_vis, tripDep_vis$freq)
colnames(stopTripDep_vis) <- c("id", "purpose", "freq_stop", "freq_trip")
write.csv(stopTripDep_vis, "stopTripDep_vis.csv", row.names = F)

# Trip Mode Summary
# ---------------------------------
#Work
tripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==1])
tripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==2])
tripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==3])
tripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==4])
tripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==5])
tripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==6])
tripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==7])
tripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==8])
tripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==9])
tripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==10])
tripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==11])
# tripmode12 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==12], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==12])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts,  tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", 
                               "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_Work.csv")

# Prepeare data for visualizer
tripModeProfile1_vis <- tripModeProfile[1:11,]
tripModeProfile1_vis$id <- row.names(tripModeProfile1_vis)
tripModeProfile1_vis <- melt(tripModeProfile1_vis, id = c("id"))
colnames(tripModeProfile1_vis) <- c("id", "purpose", "freq1")

tripModeProfile1_vis <- xtabs(freq1~id+purpose, tripModeProfile1_vis)
tripModeProfile1_vis[is.na(tripModeProfile1_vis)] <- 0
tripModeProfile1_vis <- addmargins(as.table(tripModeProfile1_vis))
tripModeProfile1_vis <- as.data.frame.matrix(tripModeProfile1_vis)

tripModeProfile1_vis$id <- row.names(tripModeProfile1_vis)
tripModeProfile1_vis <- melt(tripModeProfile1_vis, id = c("id"))
colnames(tripModeProfile1_vis) <- c("id", "purpose", "freq1")
tripModeProfile1_vis$id <- as.character(tripModeProfile1_vis$id)
tripModeProfile1_vis$purpose <- as.character(tripModeProfile1_vis$purpose)
tripModeProfile1_vis <- tripModeProfile1_vis[tripModeProfile1_vis$id!="Sum",]
tripModeProfile1_vis$purpose[tripModeProfile1_vis$purpose=="Sum"] <- "Total"


#University
tripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==1])
tripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==2])
tripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==3])
tripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==4])
tripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==5])
tripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==6])
tripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==7])
tripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==8])
tripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==9])
tripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==10])
tripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts, 
                              tripmode10$counts,tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", 
                               "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_Univ.csv")

tripModeProfile2_vis <- tripModeProfile[1:11,]
tripModeProfile2_vis$id <- row.names(tripModeProfile2_vis)
tripModeProfile2_vis <- melt(tripModeProfile2_vis, id = c("id"))
colnames(tripModeProfile2_vis) <- c("id", "purpose", "freq2")

tripModeProfile2_vis <- xtabs(freq2~id+purpose, tripModeProfile2_vis)
tripModeProfile2_vis[is.na(tripModeProfile2_vis)] <- 0
tripModeProfile2_vis <- addmargins(as.table(tripModeProfile2_vis))
tripModeProfile2_vis <- as.data.frame.matrix(tripModeProfile2_vis)

tripModeProfile2_vis$id <- row.names(tripModeProfile2_vis)
tripModeProfile2_vis <- melt(tripModeProfile2_vis, id = c("id"))
colnames(tripModeProfile2_vis) <- c("id", "purpose", "freq2")
tripModeProfile2_vis$id <- as.character(tripModeProfile2_vis$id)
tripModeProfile2_vis$purpose <- as.character(tripModeProfile2_vis$purpose)
tripModeProfile2_vis <- tripModeProfile2_vis[tripModeProfile2_vis$id!="Sum",]
tripModeProfile2_vis$purpose[tripModeProfile2_vis$purpose=="Sum"] <- "Total"

#School
tripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==1])
tripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==2])
tripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==3])
tripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==4])
tripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==5])
tripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==6])
tripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==7])
tripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==8])
tripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==9])
tripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==10])
tripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts, tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6",
                               "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_Schl.csv")

tripModeProfile3_vis <- tripModeProfile[1:11,]
tripModeProfile3_vis$id <- row.names(tripModeProfile3_vis)
tripModeProfile3_vis <- melt(tripModeProfile3_vis, id = c("id"))
colnames(tripModeProfile3_vis) <- c("id", "purpose", "freq3")

tripModeProfile3_vis <- xtabs(freq3~id+purpose, tripModeProfile3_vis)
tripModeProfile3_vis[is.na(tripModeProfile3_vis)] <- 0
tripModeProfile3_vis <- addmargins(as.table(tripModeProfile3_vis))
tripModeProfile3_vis <- as.data.frame.matrix(tripModeProfile3_vis)

tripModeProfile3_vis$id <- row.names(tripModeProfile3_vis)
tripModeProfile3_vis <- melt(tripModeProfile3_vis, id = c("id"))
colnames(tripModeProfile3_vis) <- c("id", "purpose", "freq3")
tripModeProfile3_vis$id <- as.character(tripModeProfile3_vis$id)
tripModeProfile3_vis$purpose <- as.character(tripModeProfile3_vis$purpose)
tripModeProfile3_vis <- tripModeProfile3_vis[tripModeProfile3_vis$id!="Sum",]
tripModeProfile3_vis$purpose[tripModeProfile3_vis$purpose=="Sum"] <- "Total"

#iMain
tripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==1])
tripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==2])
tripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==3])
tripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==4])
tripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==5])
tripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==6])
tripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==7])
tripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==8])
tripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==9])
tripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==10])
tripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts, tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", 
                               "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_iMain.csv")

tripModeProfile4_vis <- tripModeProfile[1:11,]
tripModeProfile4_vis$id <- row.names(tripModeProfile4_vis)
tripModeProfile4_vis <- melt(tripModeProfile4_vis, id = c("id"))
colnames(tripModeProfile4_vis) <- c("id", "purpose", "freq4")

tripModeProfile4_vis <- xtabs(freq4~id+purpose, tripModeProfile4_vis)
tripModeProfile4_vis[is.na(tripModeProfile4_vis)] <- 0
tripModeProfile4_vis <- addmargins(as.table(tripModeProfile4_vis))
tripModeProfile4_vis <- as.data.frame.matrix(tripModeProfile4_vis)

tripModeProfile4_vis$id <- row.names(tripModeProfile4_vis)
tripModeProfile4_vis <- melt(tripModeProfile4_vis, id = c("id"))
colnames(tripModeProfile4_vis) <- c("id", "purpose", "freq4")
tripModeProfile4_vis$id <- as.character(tripModeProfile4_vis$id)
tripModeProfile4_vis$purpose <- as.character(tripModeProfile4_vis$purpose)
tripModeProfile4_vis <- tripModeProfile4_vis[tripModeProfile4_vis$id!="Sum",]
tripModeProfile4_vis$purpose[tripModeProfile4_vis$purpose=="Sum"] <- "Total"

#iDisc
tripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==1])
tripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==2])
tripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==3])
tripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==4])
tripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==5])
tripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==6])
tripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==7])
tripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==8])
tripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==9])
tripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==10])
tripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts, tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", 
                               "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_iDisc.csv")

tripModeProfile5_vis <- tripModeProfile[1:11,]
tripModeProfile5_vis$id <- row.names(tripModeProfile5_vis)
tripModeProfile5_vis <- melt(tripModeProfile5_vis, id = c("id"))
colnames(tripModeProfile5_vis) <- c("id", "purpose", "freq5")

tripModeProfile5_vis <- xtabs(freq5~id+purpose, tripModeProfile5_vis)
tripModeProfile5_vis[is.na(tripModeProfile5_vis)] <- 0
tripModeProfile5_vis <- addmargins(as.table(tripModeProfile5_vis))
tripModeProfile5_vis <- as.data.frame.matrix(tripModeProfile5_vis)

tripModeProfile5_vis$id <- row.names(tripModeProfile5_vis)
tripModeProfile5_vis <- melt(tripModeProfile5_vis, id = c("id"))
colnames(tripModeProfile5_vis) <- c("id", "purpose", "freq5")
tripModeProfile5_vis$id <- as.character(tripModeProfile5_vis$id)
tripModeProfile5_vis$purpose <- as.character(tripModeProfile5_vis$purpose)
tripModeProfile5_vis <- tripModeProfile5_vis[tripModeProfile5_vis$id!="Sum",]
tripModeProfile5_vis$purpose[tripModeProfile5_vis$purpose=="Sum"] <- "Total"

jtrips$numpart_wgt <- jtrips$finalweight*jtrips$num_participants

#jMain
tripmode1 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==1])
tripmode2 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==2])
tripmode3 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==3])
tripmode4 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==4])
tripmode5 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==5])
tripmode6 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==6])
tripmode7 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==7])
tripmode8 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==8])
tripmode9 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==9])
tripmode10 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==10])
tripmode11 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts, tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", 
                               "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_jMain.csv")

tripModeProfile6_vis <- tripModeProfile[1:11,]
tripModeProfile6_vis$id <- row.names(tripModeProfile6_vis)
tripModeProfile6_vis <- melt(tripModeProfile6_vis, id = c("id"))
colnames(tripModeProfile6_vis) <- c("id", "purpose", "freq6")

tripModeProfile6_vis <- xtabs(freq6~id+purpose, tripModeProfile6_vis)
tripModeProfile6_vis[is.na(tripModeProfile6_vis)] <- 0
tripModeProfile6_vis <- addmargins(as.table(tripModeProfile6_vis))
tripModeProfile6_vis <- as.data.frame.matrix(tripModeProfile6_vis)

tripModeProfile6_vis$id <- row.names(tripModeProfile6_vis)
tripModeProfile6_vis <- melt(tripModeProfile6_vis, id = c("id"))
colnames(tripModeProfile6_vis) <- c("id", "purpose", "freq6")
tripModeProfile6_vis$id <- as.character(tripModeProfile6_vis$id)
tripModeProfile6_vis$purpose <- as.character(tripModeProfile6_vis$purpose)
tripModeProfile6_vis <- tripModeProfile6_vis[tripModeProfile6_vis$id!="Sum",]
tripModeProfile6_vis$purpose[tripModeProfile6_vis$purpose=="Sum"] <- "Total"

#jDisc
tripmode1 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==1])
tripmode2 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==2])
tripmode3 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==3])
tripmode4 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==4])
tripmode5 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==5])
tripmode6 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==6])
tripmode7 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==7])
tripmode8 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==8])
tripmode9 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==9])
tripmode10 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==10])
tripmode11 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts, tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", 
                               "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_jDisc.csv")

tripModeProfile7_vis <- tripModeProfile[1:11,]
tripModeProfile7_vis$id <- row.names(tripModeProfile7_vis)
tripModeProfile7_vis <- melt(tripModeProfile7_vis, id = c("id"))
colnames(tripModeProfile7_vis) <- c("id", "purpose", "freq7")

tripModeProfile7_vis <- xtabs(freq7~id+purpose, tripModeProfile7_vis)
tripModeProfile7_vis[is.na(tripModeProfile7_vis)] <- 0
tripModeProfile7_vis <- addmargins(as.table(tripModeProfile7_vis))
tripModeProfile7_vis <- as.data.frame.matrix(tripModeProfile7_vis)

tripModeProfile7_vis$id <- row.names(tripModeProfile7_vis)
tripModeProfile7_vis <- melt(tripModeProfile7_vis, id = c("id"))
colnames(tripModeProfile7_vis) <- c("id", "purpose", "freq7")
tripModeProfile7_vis$id <- as.character(tripModeProfile7_vis$id)
tripModeProfile7_vis$purpose <- as.character(tripModeProfile7_vis$purpose)
tripModeProfile7_vis <- tripModeProfile7_vis[tripModeProfile7_vis$id!="Sum",]
tripModeProfile7_vis$purpose[tripModeProfile7_vis$purpose=="Sum"] <- "Total"

#At work
tripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==1])
tripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==2])
tripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==3])
tripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==4])
tripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==5])
tripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==6])
tripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==7])
tripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==8])
tripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==9])
tripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==10])
tripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==11])

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, 
                              tripmode9$counts, tripmode10$counts, tripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", 
                               "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_AtWork.csv")

tripModeProfile8_vis <- tripModeProfile[1:11,]
tripModeProfile8_vis$id <- row.names(tripModeProfile8_vis)
tripModeProfile8_vis <- melt(tripModeProfile8_vis, id = c("id"))
colnames(tripModeProfile8_vis) <- c("id", "purpose", "freq8")

tripModeProfile8_vis <- xtabs(freq8~id+purpose, tripModeProfile8_vis)
tripModeProfile8_vis[is.na(tripModeProfile8_vis)] <- 0
tripModeProfile8_vis <- addmargins(as.table(tripModeProfile8_vis))
tripModeProfile8_vis <- as.data.frame.matrix(tripModeProfile8_vis)

tripModeProfile8_vis$id <- row.names(tripModeProfile8_vis)
tripModeProfile8_vis <- melt(tripModeProfile8_vis, id = c("id"))
colnames(tripModeProfile8_vis) <- c("id", "purpose", "freq8")
tripModeProfile8_vis$id <- as.character(tripModeProfile8_vis$id)
tripModeProfile8_vis$purpose <- as.character(tripModeProfile8_vis$purpose)
tripModeProfile8_vis <- tripModeProfile8_vis[tripModeProfile8_vis$id!="Sum",]
tripModeProfile8_vis$purpose[tripModeProfile8_vis$purpose=="Sum"] <- "Total"

#iTotal
itripmode1 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==1])
itripmode2 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==2])
itripmode3 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==3])
itripmode4 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==4])
itripmode5 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==5])
itripmode6 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==6])
itripmode7 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==7])
itripmode8 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==8])
itripmode9 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==9])
itripmode10 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==10])
itripmode11 <- wtd.hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = trips$finalweight[trips$TRIPMODE>0 & trips$TOURMODE==11])

#jTotal
jtripmode1 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==1], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==1])
jtripmode2 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==2], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==2])
jtripmode3 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==3], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==3])
jtripmode4 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==4], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==4])
jtripmode5 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==5], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==5])
jtripmode6 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==6], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==6])
jtripmode7 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==7], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==7])
jtripmode8 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==8], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==8])
jtripmode9 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==9], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==9])
jtripmode10 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==10], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==10])
jtripmode11 <- wtd.hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==11], breaks = seq(1,12, by=1), freq = NULL, right=FALSE, weight = jtrips$numpart_wgt[jtrips$TRIPMODE>0 & jtrips$TOURMODE==11])

tripModeProfile <- data.frame(itripmode1$counts+jtripmode1$counts, itripmode2$counts+jtripmode2$counts, itripmode3$counts+jtripmode3$counts, itripmode4$counts+jtripmode4$counts,
                              itripmode5$counts+jtripmode5$counts, itripmode6$counts+jtripmode6$counts, itripmode7$counts+jtripmode7$counts, itripmode8$counts+jtripmode8$counts, 
                              itripmode9$counts+jtripmode9$counts, itripmode10$counts+jtripmode10$counts,
                              itripmode11$counts+jtripmode11$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", 
                               "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11")
write.csv(tripModeProfile, "tripModeProfile_Total.csv")

tripModeProfile9_vis <- tripModeProfile[1:11,]
tripModeProfile9_vis$id <- row.names(tripModeProfile9_vis)
tripModeProfile9_vis <- melt(tripModeProfile9_vis, id = c("id"))
colnames(tripModeProfile9_vis) <- c("id", "purpose", "freq9")

tripModeProfile9_vis <- xtabs(freq9~id+purpose, tripModeProfile9_vis)
tripModeProfile9_vis[is.na(tripModeProfile9_vis)] <- 0
tripModeProfile9_vis <- addmargins(as.table(tripModeProfile9_vis))
tripModeProfile9_vis <- as.data.frame.matrix(tripModeProfile9_vis)

tripModeProfile9_vis$id <- row.names(tripModeProfile9_vis)
tripModeProfile9_vis <- melt(tripModeProfile9_vis, id = c("id"))
colnames(tripModeProfile9_vis) <- c("id", "purpose", "freq9")
tripModeProfile9_vis$id <- as.character(tripModeProfile9_vis$id)
tripModeProfile9_vis$purpose <- as.character(tripModeProfile9_vis$purpose)
tripModeProfile9_vis <- tripModeProfile9_vis[tripModeProfile9_vis$id!="Sum",]
tripModeProfile9_vis$purpose[tripModeProfile9_vis$purpose=="Sum"] <- "Total"


# combine all tripmode profile for visualizer
tripModeProfile_vis <- data.frame(tripModeProfile1_vis, tripModeProfile2_vis$freq2, tripModeProfile3_vis$freq3
                                  , tripModeProfile4_vis$freq4, tripModeProfile5_vis$freq5, tripModeProfile6_vis$freq6
                                  , tripModeProfile7_vis$freq7, tripModeProfile8_vis$freq8, tripModeProfile9_vis$freq9)

colnames(tripModeProfile_vis) <- c("tripmode", "tourmode", "work", "univ", "schl", "imain", "idisc", "jmain", "jdisc", "atwork", "total")

temp <- melt(tripModeProfile_vis, id = c("tripmode", "tourmode"))
#tripModeProfile_vis <- cast(temp, tripmode+variable~tourmode)
#write.csv(tripModeProfile_vis, "tripModeProfile_vis.csv", row.names = F)
temp$grp_var <- paste(temp$variable, temp$tourmode, sep = "")

# rename tour mode to standard names
temp$tourmode[temp$tourmode=="tourmode1"] <- 'Auto SOV'
temp$tourmode[temp$tourmode=="tourmode2"] <- 'Auto 2 Person'
temp$tourmode[temp$tourmode=="tourmode3"] <- 'Auto 3+ Person'
temp$tourmode[temp$tourmode=="tourmode4"] <- 'Walk'
temp$tourmode[temp$tourmode=="tourmode5"] <- 'Bike/Moped'
temp$tourmode[temp$tourmode=="tourmode6"] <- 'Walk-Transit'
temp$tourmode[temp$tourmode=="tourmode7"] <- 'PNR-Transit'
temp$tourmode[temp$tourmode=="tourmode8"] <- 'KNR-Transit'
temp$tourmode[temp$tourmode=="tourmode9"] <- 'TNR-Transit'
temp$tourmode[temp$tourmode=="tourmode10"] <- 'School Bus'
temp$tourmode[temp$tourmode=="tourmode11"] <- 'Ride Hail'

colnames(temp) <- c("tripmode","tourmode","purpose","value","grp_var")

write.csv(temp, "tripModeProfile_vis.csv", row.names = F)

# Total number of stops, trips & tours
cat("\n Total number of stops : ", sum(stops$finalweight) + sum(jstops$nump_wgt))
cat("\n Total number of trips : ", sum(trips$finalweight) + sum(jtrips$numpart_wgt))
cat("\n Total number of tours : ", sum(tours$finalweight) + sum(unique_joint_tours$numberhh_wgt))
cat("\n")

# output total numbers in a file
total_population <- sum(pertypeDistbn$freq)
# total_households <- nrow(hh)
total_households <- sum(hh$finalweight)
total_tours <- sum(tours$finalweight) + sum(unique_joint_tours$numberhh_wgt)
total_trips <- sum(trips$finalweight) + sum(jtrips$numpart_wgt)
total_stops <- sum(stops$finalweight) + sum(jstops$nump_wgt)

trips$num_travel[trips$TRIPMODE==1] <- 1
trips$num_travel[trips$TRIPMODE==2] <- 2
trips$num_travel[trips$TRIPMODE==3] <- 3.33
trips$num_travel[trips$TRIPMODE==10] <- 2.5
trips$num_travel[is.na(trips$num_travel)] <- 0

jtrips$num_travel[jtrips$TRIPMODE==1] <- 1
jtrips$num_travel[jtrips$TRIPMODE==2] <- 2
jtrips$num_travel[jtrips$TRIPMODE==3] <- 3.33
jtrips$num_travel[jtrips$TRIPMODE==10] <- 2.5
jtrips$num_travel[is.na(jtrips$num_travel)] <- 0

trips$auto[(trips$TRIPMODE<=3) | (trips$TRIPMODE==10)] <- 1
trips$auto[is.na(trips$auto)] <- 0
jtrips$auto[(jtrips$TRIPMODE<=3) | (jtrips$TRIPMODE==10)] <- 1
jtrips$auto[is.na(jtrips$auto)] <- 0

total_vmt <- sum((trips$od_dist[(trips$auto==1)])/trips$num_travel[(trips$auto==1)]) + 
  sum((jtrips$od_dist[(jtrips$auto==1)])/jtrips$num_travel[(jtrips$auto==1)])

totals_var <- c("total_population", "total_households", "total_tours", "total_trips", "total_stops", "total_vmt")
totals_val <- c(total_population,total_households, total_tours, total_trips, total_stops, total_vmt)

totals_df <- data.frame(name = totals_var, value = totals_val)

write.csv(totals_df, "totals.csv", row.names = F)

# HH Size distribution
hhSizeDist <- plyr::count(hh[hh$HHT != 5 & hh$HHT != 7,], c("HHSIZE"), "finalweight") #only counting non-GQ households
write.csv(hhSizeDist, "hhSizeDist.csv", row.names = F)

# Active Persons by person type
actpertypeDistbn <- count(per[per$cdap_activity!="H",], c("PERTYPE"), "finalweight")
write.csv(actpertypeDistbn, "activePertypeDistbn.csv", row.names = TRUE)


# County-County trip flow by Tour Purpose and Trip Mode
trips_sample <- rbind(trips[,c("OCOUNTY", "DCOUNTY", "TRIPMODE", "primary_purpose", "num_participants", "finalweight")], 
                      jtrips[,c("OCOUNTY", "DCOUNTY", "TRIPMODE", "primary_purpose", "num_participants", "finalweight")])

tripModeNames <- c('Auto SOV','Auto 2 Person','Auto 3+ Person','Walk','Bike/Moped','Walk-Transit','PNR-Transit','KNR-Transit','School Bus')
tripModeCodes <- c(1, 2, 3, 4, 5, 6, 7, 8, 9)
tripMode_df <- data.frame(tripModeCodes, tripModeNames)

trips_sample$trip_mode <- tripMode_df$tripModeNames[match(trips_sample$TRIPMODE, tripMode_df$tripModeCodes)]
trips_sample <- trips_sample[,c("OCOUNTY", "DCOUNTY", "trip_mode", "primary_purpose", "num_participants", "finalweight")]
trips_sample <- data.table(trips_sample)

# trips_flow <- trips_sample[, .(count = sum(finalweight)), by = list(OCOUNTY, DCOUNTY, trip_mode, primary_purpose)]
trips_flow <- count(trips_sample, c("OCOUNTY", "DCOUNTY", "trip_mode", "primary_purpose"), "finalweight")
colnames(trips_flow)[colnames(trips_flow)=="freq"] <- "count"

trips_flow_total <- data.table(trips_flow[,c("OCOUNTY", "DCOUNTY", "trip_mode", "count")])
trips_flow_total <- trips_flow_total[, (tot = sum(count)), by = list(OCOUNTY, DCOUNTY, trip_mode)]
trips_flow_total$primary_purpose <- "Total"
names(trips_flow_total)[names(trips_flow_total) == "V1"] <- "count"
trips_flow <- rbind(trips_flow, trips_flow_total[,c("OCOUNTY", "DCOUNTY", "trip_mode", "primary_purpose", "count")])

trips_flow_total <- data.table(trips_flow[,c("OCOUNTY", "DCOUNTY", "primary_purpose", "count")])
trips_flow_total <- trips_flow_total[, (tot = sum(count)), by = list(OCOUNTY, DCOUNTY, primary_purpose)]
trips_flow_total$trip_mode <- "Total"
names(trips_flow_total)[names(trips_flow_total) == "V1"] <- "count"
trips_flow <- rbind(trips_flow, trips_flow_total[,c("OCOUNTY", "DCOUNTY", "trip_mode", "primary_purpose", "count")])


# write.table(trips_flow, paste(WD,"trips_flow.csv", sep = "//"), sep = ",", row.names = F)


# Transit Trips
# ---------------------------------------------------
# County-County Flow
# trips_transit <- all_trips[all_trips$TRIPMODE %in% c(6,7,8),]
# trips_transit$access_mode[trips_transit$TRIPMODE==6] <- "walk"
# trips_transit$access_mode[trips_transit$TRIPMODE==7] <- "pnr"
# trips_transit$access_mode[trips_transit$TRIPMODE==8] <- "knr"
# trips_transit$tourtype <- "Non-Mandatory"
# trips_transit$tourtype[trips_transit$TOURPURP <= 3] <- "Mandatory"
# transit_trips <- trips_transit
# trips_transit <- data.table(trips_transit[,c("OCOUNTY", "DCOUNTY", "access_mode", "finalweight")])
# 
# trips_transit_summary <- trips_transit[, .(count = sum(finalweight)), by = list(OCOUNTY, DCOUNTY, access_mode)]
# trips_transit_summary_total <- data.table(trips_transit_summary[,c("OCOUNTY", "DCOUNTY", "access_mode", "count")])
# trips_transit_summary_total <- trips_transit_summary_total[, (tot = sum(count)), by = list(OCOUNTY, DCOUNTY, access_mode)]
# #trips_transit_summary_total$AGGTOURPURP <- "Total"
# names(trips_transit_summary_total)[names(trips_transit_summary_total) == "V1"] <- "count"
# trips_transit_summary <- rbind(trips_transit_summary, trips_transit_summary_total[,c("OCOUNTY", "DCOUNTY", "access_mode", "count")])
# 
# trips_transit_summary_total <- data.table(trips_transit_summary[,c("OCOUNTY", "DCOUNTY", "count")])
# trips_transit_summary_total <- trips_transit_summary_total[, (tot = sum(count)), by = list(OCOUNTY, DCOUNTY)]
# trips_transit_summary_total$access_mode <- "Total"
# names(trips_transit_summary_total)[names(trips_transit_summary_total) == "V1"] <- "count"
# trips_transit_summary <- rbind(trips_transit_summary, trips_transit_summary_total[,c("OCOUNTY", "DCOUNTY", "access_mode", "count")])

# write.table(trips_transit_summary, paste(WD,"trips_transit_summary.csv", sep = "//"), sep = ",", row.names = F)


### Trip Length Distribution of Trasit Trips ###
# 
# # code distance bins
# transit_trips$distbin <- cut(transit_trips$od_dist, breaks = c(seq(0,50, by=1), 9999), labels = F, right = F)
# distBinCat <- data.frame(distbin = seq(1,51, by=1))
# 
# transit_trips$distbin10 <- cut(transit_trips$od_dist, breaks = c(seq(0,2, by=0.1), 9999), labels = F, right = F)
# distBinCat10 <- data.frame(distbin10 = seq(1,21, by=1))
# 
# # compute TLFDs by district and total
# tlfd_transit <- ddply(transit_trips[,c("access_mode", "distbin")], c("access_mode", "distbin"), summarise, transit = sum(!is.na(access_mode)))
# tlfd_transit <- cast(tlfd_transit, distbin~access_mode, value = "transit", sum)
# tlfd_transit$Total <- rowSums(tlfd_transit[,!colnames(tlfd_transit) %in% c("distbin")])
# tlfd_transit_df <- merge(x = distBinCat, y = tlfd_transit, by = "distbin", all.x = TRUE)
# tlfd_transit_df[is.na(tlfd_transit_df)] <- 0
# write.csv(tlfd_transit_df, "transitTLFD.csv", row.names = F)
# 
# # compute TLFDs by tenths of mile
# tlfd_transit <- ddply(transit_trips[,c("access_mode", "distbin10")], c("access_mode", "distbin10"), summarise, transit = sum(!is.na(access_mode)))
# tlfd_transit <- cast(tlfd_transit, distbin10~access_mode, value = "transit", sum)
# tlfd_transit$Total <- rowSums(tlfd_transit[,!colnames(tlfd_transit) %in% c("distbin10")])
# tlfd_transit_df <- merge(x = distBinCat10, y = tlfd_transit, by = "distbin10", all.x = TRUE)
# tlfd_transit_df[is.na(tlfd_transit_df)] <- 0
# write.csv(tlfd_transit_df, "transitTLFD10.csv", row.names = F)

# work tour going to parking constraint
# tours_transit <- tours[tours$TOURMODE %in% c(6,7,8),]
# tours_transit$access_mode[tours_transit$TOURMODE==6] <- "walk"
# tours_transit$access_mode[tours_transit$TOURMODE==7] <- "pnr"
# tours_transit$access_mode[tours_transit$TOURMODE==8] <- "knr"
# # tours_transit$dest_park <- zoneData$parkConstraint[match(tours_transit$dest_taz, zoneData$taz)]
# 
# work_Tours <- table(tours_transit$access_mode[tours_transit$dest_park==1 & tours_transit$TOURPURP==1])
# write.table("work_Tours", "work_Tours.csv", sep = ",")
# write.table(work_Tours, "work_Tours.csv", sep = ",", row.names = F, append = T)

# Number of workers with work location in parking constraint zones
# wsLoc$dest_park <- zoneData$parkConstraint[match(wsLoc$WorkLocation, zoneData$taz)]
# workersParkZone <- table(wsLoc$dest_park)
# write.table("workersParkZone", "workersParkZone.csv", sep = ",")
# write.table(workersParkZone, "workersParkZone.csv", sep = ",", row.names = F, append = T)


#finish

end_time <- Sys.time()
end_time - start_time
cat("\n Summarize_ActivitySim_cmap.R Script finished\n")
