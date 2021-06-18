#######################################################
### Script for summarizing Census data used in the visualizer
### 
#######################################################

start_time <- Sys.time()


if (!"rgeos" %in% installed.packages()) install.packages("rgeos", repos='http://cran.us.r-project.org')
library(rgeos)
if (!"sp" %in% installed.packages()) install.packages("sp", repos='http://cran.us.r-project.org')
library(sp)
if (!"raster" %in% installed.packages()) install.packages("raster", repos='http://cran.us.r-project.org')
library(raster)
if (!"rgdal" %in% installed.packages()) install.packages("rgdal", repos='http://cran.us.r-project.org')
library(rgdal)
# if (!"dplyr" %in% installed.packages()) install.packages("dplyr", repos='http://cran.us.r-project.org')
# library(dplyr)
library(plyr)
if (!"foreign" %in% installed.packages()) install.packages("foreign", repos='http://cran.us.r-project.org')
library(foreign)
library(reshape)
library(data.table)

## User Inputs
inputDir       <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census"
outputDir      <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census/summarized"
taz_shp_file   <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/zone_data/TAZ_Zones.shp"
puma_shp_file  <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census/2017_puma_shapefile/tl_2017_26_puma10.shp"
hts_countyflow_file <- "E:/Projects/Clients/SEMCOG/Tasks/Task5_Visualizer/data/census/HTS_countyFlows.csv"

setwd(inputDir)

census_flows     <- read.csv("ACS_commuting_flows_2015_5yr.csv", header = TRUE)
# semcog county names and fips in ACS file
semcog_county_fips  <- data.frame(county_fips = c(9999, 163, 125, 99, 161, 115, 147, 93),
                                  name = c("Detroit", "Wayne", "Oakland", "Macomb", "Washtenaw", "Monroe", "St. Clair", "Livingston"))
# county names and code used in SEMCOG files
countynames <- data.frame(code = c(1, 2, 3, 4, 5, 6, 7, 8),
                          name = c("Detroit", "Wayne", "Oakland", "Macomb", "Washtenaw", "Monroe", "St. Clair", "Livingston"))

census_hh     <- read.csv("ACS_PUMS_2017_5yr_hh_MI.csv", header = TRUE)
land_use      <- read.csv("land_use.csv", header = TRUE)
census_auto   <- read.csv("ACSDT5Y2017_auto_ownership.csv", header = TRUE)
# census_per    <- read.csv("ACS_PUMS_2017_5yr_p_MI.csv", header = TRUE)
hts_countyflow <- read.csv(hts_countyflow_file, header = TRUE, check.names = FALSE)

taz_shp  <- shapefile(taz_shp_file)
puma_shp <- shapefile(puma_shp_file)
taz_shp  <- spTransform(taz_shp, CRS("+proj=longlat +ellps=GRS80"))
puma_shp <- spTransform(puma_shp, CRS("+proj=longlat +ellps=GRS80"))

# ct_xwalk      <- read.csv(paste(geogXWalkDir, "geo_cross_walk_tm1.csv", sep = "/"), as.is = T)

setwd(outputDir)


# -------------------------------------------
# Processing Household Vehicles
# Only keeping households from PUMAs inside the SEMCOG region

# # First creating puma to county crosswalk
# taz_centroids <- gCentroid(taz_shp, byid=TRUE, id=taz_shp$OBJECTID)
# 
# overlay <- function(points, polygon){
#   proj4string(points) <- proj4string(polygon) # use same projection
#   pointsDF <- over(points,polygon)
#   return(pointsDF)
# }
# 
# # tazXWalk <- select(taz_shp@data, OBJECTID) %>%
# #   mutate(TAZSEQ = OBJECTID)
# tazXWalk <- taz_shp@data
# tazTemp1 <- overlay(taz_centroids, puma_shp)
# tazXWalk$PUMA <- tazTemp1$PUMACE10
# tazXWalk$TAZ <- tazXWalk$ID
# 
# tazXWalk$COUNTY_NAME <- countynames$name[match(tazXWalk$COUNTY, countynames$code)]
# 
# puma_county_count <- count(tazXWalk, c("PUMA", "COUNTY_NAME"))
# puma_county_count$PUMA <- as.numeric(puma_county_count$PUMA)
# 
# # A PUMA can match with multiple districts, so the PUMA is matched with the district that has the most TAZs in that PUMA
# puma_county_xwalk <- puma_county_count[with(puma_county_count, ave(freq, PUMA, FUN=max) == freq),]
# 
# 
# # additional POWPUMA-COUNTY XWALK
# POWPUMA_list <- c(2900, 3000, 3200)
# COUNTY_list <- c("Oakland", "Macomb", "Wayne")
# powpuma_county_xwalk <- data.frame(PUMA = POWPUMA_list, COUNTY_NAME = COUNTY_list)
# powpuma_county_xwalk$freq <- 0
# 
# puma_county_xwalk <- rbind(puma_county_xwalk, powpuma_county_xwalk)
# 
# semcog_pumas <- unique(puma_county_xwalk$PUMA)
# 
# semcog_hh <- census_hh[census_hh$PUMA %in% semcog_pumas,]
# 
# semcog_hh$finalweight <- semcog_hh$WGTP
# 
# semcog_hh$HHVEH[semcog_hh$VEH == 0] <- 0
# semcog_hh$HHVEH[semcog_hh$VEH == 1] <- 1
# semcog_hh$HHVEH[semcog_hh$VEH == 2] <- 2
# semcog_hh$HHVEH[semcog_hh$VEH == 3] <- 3
# semcog_hh$HHVEH[semcog_hh$VEH >= 4] <- 4
# 
# autoOwnership_census <- count(semcog_hh, c("HHVEH"), "finalweight")
# write.csv(autoOwnership_census, "autoOwnershipCensus.csv", row.names = TRUE)

autoOwnership_census <- data.frame(HHVEH = seq(0,4))
autoOwnership_census$freq[autoOwnership_census$HHVEH == 0]  <- sum(census_auto$ZERO_AUTO)
autoOwnership_census$freq[autoOwnership_census$HHVEH == 1]  <- sum(census_auto$ONE_AUTO)
autoOwnership_census$freq[autoOwnership_census$HHVEH == 2]  <- sum(census_auto$TWO_AUTO)
autoOwnership_census$freq[autoOwnership_census$HHVEH == 3]  <- sum(census_auto$THREE_AUTO)
autoOwnership_census$freq[autoOwnership_census$HHVEH == 4]  <- sum(census_auto$FOUR_AUTO)
write.csv(autoOwnership_census, "autoOwnershipCensus.csv", row.names = TRUE)


# -------------------------------------------
# Creating County-to-County Flows

mi_flows <- census_flows[census_flows$R_STATE_FIPS == 26 & census_flows$W_STATE_FIPS == 26,]
semcog_flows <- mi_flows[mi_flows$R_COUNTY_FIPS %in% semcog_county_fips$county_fips & mi_flows$W_COUNTY_FIPS %in% semcog_county_fips$county_fips,]
semcog_flows$W_COMMUTING_FLOW <- as.numeric(gsub(",", "", as.character(semcog_flows$W_COMMUTING_FLOW)))
semcog_flows$HCOUNTYNAME <- semcog_county_fips$name[match(semcog_flows$R_COUNTY_FIPS, semcog_county_fips$county_fips)]
semcog_flows$WCOUNTYNAME <- semcog_county_fips$name[match(semcog_flows$W_COUNTY_FIPS, semcog_county_fips$county_fips)]

# Raw census data without Detroit splitting to compare to
no_detroit_countyflow <- xtabs(W_COMMUTING_FLOW~HCOUNTYNAME+WCOUNTYNAME, data = semcog_flows)
no_detroit_countyflow[is.na(no_detroit_countyflow)] <- 0
no_detroit_countyflow <- addmargins(as.table(no_detroit_countyflow))
no_detroit_countyflow <- as.data.frame.matrix(no_detroit_countyflow)
colnames(no_detroit_countyflow)[colnames(no_detroit_countyflow)=="Sum"] <- "Total"
rownames(no_detroit_countyflow)[rownames(no_detroit_countyflow)=="Sum"] <- "Total"
no_detroit_countyflow <- round(no_detroit_countyflow)


# Initial values for Detroit are equal to Wayne
semcog_flows$OLD_W_COMMUTING_FLOW <- semcog_flows$W_COMMUTING_FLOW

detroit_flows <- semcog_flows[semcog_flows$HCOUNTYNAME == "Wayne" | semcog_flows$WCOUNTYNAME == "Wayne",]
detroit_flows$HCOUNTYNAME[detroit_flows$HCOUNTYNAME == "Wayne"] <- "Detroit"
detroit_flows$WCOUNTYNAME[detroit_flows$WCOUNTYNAME == "Wayne"] <- "Detroit"

wayne_to_detroit_flow <- semcog_flows[semcog_flows$HCOUNTYNAME == "Wayne" & semcog_flows$WCOUNTYNAME == "Wayne",]
wayne_to_detroit_flow$WCOUNTYNAME <- "Detroit"

detroit_to_wayne_flow <- semcog_flows[semcog_flows$HCOUNTYNAME == "Wayne" & semcog_flows$WCOUNTYNAME == "Wayne",]
detroit_to_wayne_flow$HCOUNTYNAME <- "Detroit"

semcog_flows <- rbind(semcog_flows, detroit_flows, wayne_to_detroit_flow, detroit_to_wayne_flow)


# Warning!!!! hts_countyflow table must have "X" as the column name above the Home county names!!
hts_countyflow_df <- melt(hts_countyflow, id="X")
hts_countyflow_df[is.na(hts_countyflow_df)] <- 0
hts_countyflow_df$HCOUNTY <- hts_countyflow_df$X
hts_countyflow_df$WCOUNTY <- hts_countyflow_df$variable
hts_countyflow_df$PER_OF_HCOUNTY_TOT <- 0


# Using HTS flow percentages to split Census Wayne into Detroit and Other Wayne
# Row normalizing the hts_countyflow in order to use the percentage split between wayne and detroit
for (home_county in countynames$name){
  for (work_county in countynames$name){
    home_to_dest_count <- hts_countyflow_df$value[hts_countyflow_df$HCOUNTY == home_county & hts_countyflow_df$WCOUNTY == work_county]
    total_dest_count <- hts_countyflow_df$value[hts_countyflow_df$HCOUNTY == home_county & hts_countyflow_df$WCOUNTY == 'Total']
    if (total_dest_count == 0){
      per_of_hcounty_tot <- 0
    }else {
      per_of_hcounty_tot <-  home_to_dest_count / total_dest_count
    }
    hts_countyflow_df$PER_OF_HCOUNTY_TOT[hts_countyflow_df$HCOUNTY == home_county & hts_countyflow_df$WCOUNTY == work_county] <- per_of_hcounty_tot
  }
}

# Splitting Wayne into Detroit and West Wayne split in the HTS
for (home_county in countynames$name){
  for (work_county in countynames$name){
    # Splitting Census Wayne home county into detroit and west wayne
    if (home_county == 'Detroit' | home_county == 'Wayne'){
      detroit_hts_per <- hts_countyflow_df$PER_OF_HCOUNTY_TOT[hts_countyflow_df$HCOUNTY == 'Detroit' & hts_countyflow_df$WCOUNTY == work_county]
      wayne_hts_per <- hts_countyflow_df$PER_OF_HCOUNTY_TOT[hts_countyflow_df$HCOUNTY == 'Wayne' & hts_countyflow_df$WCOUNTY == work_county]
      detroit_factor <- detroit_hts_per / (detroit_hts_per + wayne_hts_per)
      wayne_factor <- wayne_hts_per / (detroit_hts_per + wayne_hts_per)
      
      if (is.na(detroit_factor) | is.na(wayne_factor)){
        wayne_factor <- 1   # if no counts exist in hts, just keep all in wayne 
        detroit_factor <- 0
      }
      
      if (home_county == 'Detroit'){
        flow_factor <- detroit_factor
      } else {
        flow_factor <- wayne_factor
      }
      
      new_flow <- semcog_flows$OLD_W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == home_county & semcog_flows$WCOUNTYNAME == work_county] * flow_factor
      semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == home_county & semcog_flows$WCOUNTYNAME == work_county] <- new_flow
    }
    
    # Splitting Census Wayne work county into detroit and west wayne
    if (work_county == 'Detroit' | work_county == 'Wayne'){
      detroit_hts_per <- hts_countyflow_df$PER_OF_HCOUNTY_TOT[hts_countyflow_df$HCOUNTY == home_county & hts_countyflow_df$WCOUNTY == 'Detroit']
      wayne_hts_per <- hts_countyflow_df$PER_OF_HCOUNTY_TOT[hts_countyflow_df$HCOUNTY == home_county & hts_countyflow_df$WCOUNTY == 'Wayne']
      detroit_factor <- detroit_hts_per / (detroit_hts_per + wayne_hts_per)
      wayne_factor <- wayne_hts_per / (detroit_hts_per + wayne_hts_per)
      
      if (is.na(detroit_factor) | is.na(wayne_factor)){
        wayne_factor <- 1    # if no counts exist in hts, just keep all in wayne
        detroit_factor <- 0
      }
      
      if (work_county == 'Detroit'){
        flow_factor <- detroit_factor
      } else {
        flow_factor <- wayne_factor
      }
      
      new_flow <- semcog_flows$OLD_W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == home_county & semcog_flows$WCOUNTYNAME == work_county] * flow_factor
      semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == home_county & semcog_flows$WCOUNTYNAME == work_county] <- new_flow
    }
  }
}

# Need to intra-wayne commuting explicitly
# intra-wayne is split into 4 categories using the same split percentage as in the HTS
hts_detroit_to_detroit <- hts_countyflow_df$value[hts_countyflow_df$HCOUNTY == "Detroit" & hts_countyflow_df$WCOUNTY == 'Detroit']
hts_detroit_to_wayne   <- hts_countyflow_df$value[hts_countyflow_df$HCOUNTY == "Detroit" & hts_countyflow_df$WCOUNTY == 'Wayne']
hts_wayne_to_detroit   <- hts_countyflow_df$value[hts_countyflow_df$HCOUNTY == "Wayne" & hts_countyflow_df$WCOUNTY == 'Detroit']
hts_wayne_to_wayne     <- hts_countyflow_df$value[hts_countyflow_df$HCOUNTY == "Wayne" & hts_countyflow_df$WCOUNTY == 'Wayne']
hts_total_intra_wayne <- hts_detroit_to_detroit + hts_detroit_to_wayne + hts_wayne_to_detroit + hts_wayne_to_wayne

census_intra_wayne <- semcog_flows$OLD_W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == 'Wayne' & semcog_flows$WCOUNTYNAME == 'Wayne']

census_detroit_to_detroit <- census_intra_wayne * (hts_detroit_to_detroit / hts_total_intra_wayne)
census_detroit_to_wayne   <- census_intra_wayne * (hts_detroit_to_wayne   / hts_total_intra_wayne)
census_wayne_to_detroit   <- census_intra_wayne * (hts_wayne_to_detroit   / hts_total_intra_wayne)
census_wayne_to_wayne     <- census_intra_wayne * (hts_wayne_to_wayne     / hts_total_intra_wayne)

semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == 'Detroit' & semcog_flows$WCOUNTYNAME == 'Detroit'] <- census_detroit_to_detroit
semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == 'Detroit' & semcog_flows$WCOUNTYNAME == 'Wayne']   <- census_detroit_to_wayne
semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == 'Wayne' & semcog_flows$WCOUNTYNAME == 'Detroit']   <- census_wayne_to_detroit
semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == 'Wayne' & semcog_flows$WCOUNTYNAME == 'Wayne']     <- census_wayne_to_wayne

land_use$COUNTY_NAME <- countynames$name[match(land_use$COUNTY, countynames$code)]

land_use_county_employment <- count(land_use, vars='COUNTY_NAME', wt_var='tot_emp')
colnames(land_use_county_employment)[colnames(land_use_county_employment)=="freq"] <- "tot_emp"
land_use_county_employment$tot_emp <- as.numeric(as.character(land_use_county_employment$tot_emp))
tot_emp_row <- data.frame(COUNTY_NAME = c("Total"), tot_emp = c(sum(land_use_county_employment$tot_emp)))
land_use_county_employment <- rbind(land_use_county_employment, tot_emp_row)

land_use_county_workers <- count(land_use, vars='COUNTY_NAME', wt_var='workers')
colnames(land_use_county_workers)[colnames(land_use_county_workers)=="freq"] <- "workers"
land_use_county_workers$workers <- as.numeric(as.character(land_use_county_workers$workers))
tot_workers_row <- data.frame(COUNTY_NAME = c("Total"), workers = c(sum(land_use_county_workers$workers)))
land_use_county_workers <- rbind(land_use_county_workers, tot_workers_row)

# land_use_by_county <- cbind(land_use_county_employment, land_use_county_workers)
land_use_by_county <- merge(land_use_county_employment, land_use_county_workers, by="COUNTY_NAME", sort=FALSE)

write.csv(land_use_by_county, "land_use_by_county.csv")


# manually adding  a detroit county based on land use data from SEMCOG

# detroit_land_use   <- land_use[land_use$COUNTY == 1,]
# num_detroit_employment <- sum(detroit_land_use$tot_emp)
# num_detroit_workers    <- sum(detroit_land_use$workers)
# 
# wayne_land_use   <- land_use[land_use$COUNTY == 2,]
# num_wayne_employment <- sum(wayne_land_use$tot_emp)
# num_wayne_workers    <- sum(wayne_land_use$workers)
# 
# percentage_detroit_employment <- num_detroit_employment / (num_detroit_employment + num_wayne_employment)
# percentage_wayne_employment <- 1 - percentage_detroit_employment
# 
# percentage_detroit_workers <- num_detroit_workers / (num_detroit_workers + num_wayne_workers)
# percentage_wayne_workers <- 1 - percentage_detroit_workers
# 
# detroit_flows <- semcog_flows[semcog_flows$HCOUNTYNAME == "Wayne" | semcog_flows$WCOUNTYNAME == "Wayne",]
# detroit_flows$HCOUNTYNAME[detroit_flows$HCOUNTYNAME == "Wayne"] <- "Detroit"
# detroit_flows$WCOUNTYNAME[detroit_flows$WCOUNTYNAME == "Wayne"] <- "Detroit"
# 
# detroit_flows$W_COMMUTING_FLOW[detroit_flows$HCOUNTYNAME == "Detroit"] <- detroit_flows$OLD_W_COMMUTING_FLOW[detroit_flows$HCOUNTYNAME == "Detroit"] * percentage_detroit_workers
# detroit_flows$W_COMMUTING_FLOW[detroit_flows$WCOUNTYNAME == "Detroit"] <- detroit_flows$OLD_W_COMMUTING_FLOW [detroit_flows$WCOUNTYNAME == "Detroit"]* percentage_detroit_employment
# detroit_flows$W_COMMUTING_FLOW[detroit_flows$WCOUNTYNAME == "Detroit" & detroit_flows$HCOUNTYNAME == "Detroit"] <- detroit_flows$OLD_W_COMMUTING_FLOW[detroit_flows$WCOUNTYNAME == "Detroit" & detroit_flows$HCOUNTYNAME == "Detroit"] * percentage_detroit_employment * percentage_detroit_workers
# 
# semcog_flows$W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == "Wayne"] <- semcog_flows$OLD_W_COMMUTING_FLOW[semcog_flows$HCOUNTYNAME == "Wayne"] * percentage_wayne_workers
# semcog_flows$W_COMMUTING_FLOW[semcog_flows$WCOUNTYNAME == "Wayne"] <- semcog_flows$OLD_W_COMMUTING_FLOW[semcog_flows$WCOUNTYNAME == "Wayne"] * percentage_wayne_employment
# semcog_flows$W_COMMUTING_FLOW[semcog_flows$WCOUNTYNAME == "Wayne" & semcog_flows$HCOUNTYNAME == "Wayne"] <- semcog_flows$OLD_W_COMMUTING_FLOW[semcog_flows$WCOUNTYNAME == "Wayne" & semcog_flows$HCOUNTYNAME == "Wayne"] * percentage_wayne_employment * percentage_wayne_workers
# 
# wayne_to_detroit_flow <- semcog_flows[semcog_flows$HCOUNTYNAME == "Wayne" & semcog_flows$WCOUNTYNAME == "Wayne",]
# wayne_to_detroit_flow$WCOUNTYNAME <- "Detroit"
# wayne_to_detroit_flow$W_COMMUTING_FLOW <- wayne_to_detroit_flow$OLD_W_COMMUTING_FLOW * percentage_wayne_workers * percentage_detroit_employment
# 
# detroit_to_wayne_flow <- semcog_flows[semcog_flows$HCOUNTYNAME == "Wayne" & semcog_flows$WCOUNTYNAME == "Wayne",]
# detroit_to_wayne_flow$HCOUNTYNAME <- "Detroit"
# detroit_to_wayne_flow$W_COMMUTING_FLOW <- detroit_to_wayne_flow$OLD_W_COMMUTING_FLOW * percentage_detroit_workers * percentage_wayne_employment
# 
# semcog_flows <- rbind(semcog_flows, detroit_flows, wayne_to_detroit_flow, detroit_to_wayne_flow)


countyflow <- xtabs(W_COMMUTING_FLOW~HCOUNTYNAME+WCOUNTYNAME, data = semcog_flows)
countyflow[is.na(countyflow)] <- 0

countyflow <- addmargins(as.table(countyflow))
countyflow <- as.data.frame.matrix(countyflow)
colnames(countyflow)[colnames(countyflow)=="Sum"] <- "Total"
rownames(countyflow)[rownames(countyflow)=="Sum"] <- "Total"
countyflow <- round(countyflow)

write.csv(countyflow, "countyFlowsCensus.csv")


#--------------------------------------------------------
# Creating Auto Ownership map


#finish

end_time <- Sys.time()
end_time - start_time
cat("\n Script finished, run time: ", end_time - start_time, "sec \n")

