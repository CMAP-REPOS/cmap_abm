#######################################################
###Script for creating updated ARC PopSyn controls using -
#   1) 2010 HH totals 2) 2007-11 ACS PUMS 3) existing controls as seed
### Author: Binny M Paul, binny.paul@rsginc.com
#######################################################
oldw <- getOption("warn")
olds <- getOption("scipen")
options(warn = -1)
options(scipen=999)

library(plyr)
library(foreign)

# User inputs
source("../get_dir.R")  # Intelligently create PopSynDir variable
RScriptDir <- file.path(PopSynDir, "1__control_files")
InDir <- file.path(PopSynDir, "1__control_files/inputs")
OutDir <- file.path(PopSynDir, "2__synpop/inputs/datatables")

# Define conversion factor to convert 2011 PUMS income dollars to 1999 dollars
# (derived from http://www.bls.gov/data/inflation_calculator.htm)
INCOME_DISCOUNT_FACTOR <- 0.74064524

# Load IPF function
setwd(RScriptDir)
source("ipf_AB20080429.R")

# Read geog correspondence files
setwd(InDir)
pumaList <- read.csv("ST_PumaList.csv")
ListST_PUMA <- as.list(pumaList$ST_PUMA)

geog <- read.csv("GEOG_IN.csv")
geog$ST[geog$state_abbr=="IL"] <- 17
geog$ST[geog$state_abbr=="IN"] <- 18
geog$ST[geog$state_abbr=="WI"] <- 55
geog$ST_PUMA[nchar(geog$puma5)==3] <- geog$ST[nchar(geog$puma5)==3] * 1000 + geog$puma5[nchar(geog$puma5)==3]
geog$ST_PUMA[nchar(geog$puma5)==4] <- geog$ST[nchar(geog$puma5)==4] * 10000 + geog$puma5[nchar(geog$puma5)==4]

# Read old controls files
seed <- read.csv("oldControls.csv")


## Run the commented portion once to generate PUMA level distributions
#raw_hh_il <- read.csv("ss11hil.csv")   # <http://www2.census.gov/acs2011_5yr/pums/csv_hil.zip>
#raw_per_il <- read.csv("ss11pil.csv")  # <http://www2.census.gov/acs2011_5yr/pums/csv_pil.zip>
#raw_hh_in <- read.csv("ss11hin.csv")   # <http://www2.census.gov/acs2011_5yr/pums/csv_hin.zip>
#raw_per_in <- read.csv("ss11pin.csv")  # <http://www2.census.gov/acs2011_5yr/pums/csv_pin.zip>
#raw_hh_wi <- read.csv("ss11hwi.csv")   # <http://www2.census.gov/acs2011_5yr/pums/csv_hwi.zip>
#raw_per_wi <- read.csv("ss11pwi.csv")  # <http://www2.census.gov/acs2011_5yr/pums/csv_pwi.zip>
#
#raw_hh_il$ST_PUMA[nchar(raw_hh_il$PUMA)==3] <- raw_hh_il$ST[nchar(raw_hh_il$PUMA)==3] * 1000 + raw_hh_il$PUMA[nchar(raw_hh_il$PUMA)==3]
#raw_hh_il$ST_PUMA[nchar(raw_hh_il$PUMA)==4] <- raw_hh_il$ST[nchar(raw_hh_il$PUMA)==4] * 10000 + raw_hh_il$PUMA[nchar(raw_hh_il$PUMA)==4]
#
#raw_hh_in$ST_PUMA[nchar(raw_hh_in$PUMA)==3] <- raw_hh_in$ST[nchar(raw_hh_in$PUMA)==3] * 1000 + raw_hh_in$PUMA[nchar(raw_hh_in$PUMA)==3]
#raw_hh_in$ST_PUMA[nchar(raw_hh_in$PUMA)==4] <- raw_hh_in$ST[nchar(raw_hh_in$PUMA)==4] * 10000 + raw_hh_in$PUMA[nchar(raw_hh_in$PUMA)==4]
#
#raw_hh_wi$ST_PUMA[nchar(raw_hh_wi$PUMA)==3] <- raw_hh_wi$ST[nchar(raw_hh_wi$PUMA)==3] * 1000 + raw_hh_wi$PUMA[nchar(raw_hh_wi$PUMA)==3]
#raw_hh_wi$ST_PUMA[nchar(raw_hh_wi$PUMA)==4] <- raw_hh_wi$ST[nchar(raw_hh_wi$PUMA)==4] * 10000 + raw_hh_wi$PUMA[nchar(raw_hh_wi$PUMA)==4]
#
#raw_per_il$ST_PUMA[nchar(raw_per_il$PUMA)==3] <- raw_per_il$ST[nchar(raw_per_il$PUMA)==3] * 1000 + raw_per_il$PUMA[nchar(raw_per_il$PUMA)==3]
#raw_per_il$ST_PUMA[nchar(raw_per_il$PUMA)==4] <- raw_per_il$ST[nchar(raw_per_il$PUMA)==4] * 10000 + raw_per_il$PUMA[nchar(raw_per_il$PUMA)==4]
#
#raw_per_in$ST_PUMA[nchar(raw_per_in$PUMA)==3] <- raw_per_in$ST[nchar(raw_per_in$PUMA)==3] * 1000 + raw_per_in$PUMA[nchar(raw_per_in$PUMA)==3]
#raw_per_in$ST_PUMA[nchar(raw_per_in$PUMA)==4] <- raw_per_in$ST[nchar(raw_per_in$PUMA)==4] * 10000 + raw_per_in$PUMA[nchar(raw_per_in$PUMA)==4]
#
#raw_per_wi$ST_PUMA[nchar(raw_per_wi$PUMA)==3] <- raw_per_wi$ST[nchar(raw_per_wi$PUMA)==3] * 1000 + raw_per_wi$PUMA[nchar(raw_per_wi$PUMA)==3]
#raw_per_wi$ST_PUMA[nchar(raw_per_wi$PUMA)==4] <- raw_per_wi$ST[nchar(raw_per_wi$PUMA)==4] * 10000 + raw_per_wi$PUMA[nchar(raw_per_wi$PUMA)==4]
#
#hh_il <- raw_hh_il[raw_hh_il$ST_PUMA %in% ListST_PUMA,]
#per_il <- raw_per_il[raw_per_il$ST_PUMA %in% ListST_PUMA,]
#
#hh_in <- raw_hh_in[raw_hh_in$ST_PUMA %in% ListST_PUMA,]
#per_in <- raw_per_in[raw_per_in$ST_PUMA %in% ListST_PUMA,]
#
#hh_wi <- raw_hh_wi[raw_hh_wi$ST_PUMA %in% ListST_PUMA,]
#per_wi <- raw_per_wi[raw_per_wi$ST_PUMA %in% ListST_PUMA,]
#
## Combine dataset
#hh <- rbind(hh_il, hh_in, hh_wi)
#per <- rbind(per_il, per_in, per_wi)
#hh[is.na(hh)] <- 0
#per[is.na(per)] <- 0
#
## Remove GQs
#per$TYPE <- hh$TYPE[match(per$SERIALNO, hh$SERIALNO)]
#per$NP <- hh$NP[match(per$SERIALNO, hh$SERIALNO)]
#per <- per[per$TYPE==1 & per$NP>0,]
#hh <- hh[hh$TYPE==1 & hh$NP>0,]
#
## Code workers
#per$employed[per$ESR %in% c(1,2,4,5)] <- 1
#per$employed[is.na(per$employed)] <- 0
#
## Get number of workers in HH
#hhWorker <- count(per, c("SERIALNO"), "employed")
#hh$workers <- hhWorker$freq[match(hh$SERIALNO, hhWorker$SERIALNO)]
#hh$workers[is.na(hh$workers)] <- 0
#hh$workers[hh$workers == 0 & hh$FES > 0] <- 0
#hh$workers[hh$workers == 1 & hh$FES > 0] <- 1
#hh$workers[hh$workers >= 2 & hh$FES > 0] <- 2
#hh$workers[hh$FES == 0] <- 3
#
## Householder age ~ Max age of HH
#hhAge <- aggregate(per$AGEP, list(SERIALNO = per$SERIALNO), max)
#hh$HHAGE <- hhAge$x[match(hh$SERIALNO, hhAge$SERIALNO)]
#hh$HHAGECAT <- 1
#hh$HHAGECAT[hh$HHAGE>=65] <- 2
#
## HH Size
#hh$HHSIZE[hh$NP == 1] <- 1
#hh$HHSIZE[hh$NP == 2] <- 2
#hh$HHSIZE[hh$NP == 3] <- 3
#hh$HHSIZE[hh$NP == 4] <- 4
#hh$HHSIZE[hh$NP >= 5] <- 5
#
## HH Income (use 1999 dollars)
#hh$HINC <- hh$HINCP * (hh$ADJINC / 1000000) * INCOME_DISCOUNT_FACTOR
#hh$hhinccat[hh$HINC < 40000] <- 1
#hh$hhinccat[hh$HINC >= 40000 & hh$HINC < 60000] <- 2
#hh$hhinccat[hh$HINC >= 60000 & hh$HINC < 100000] <- 3
#hh$hhinccat[hh$HINC >= 100000] <- 4
#
## Summarize PUMS data
#hhAgeCounts <- count(hh, c("HHAGECAT", "ST_PUMA"), "WGTP")
#hhincCounts <- count(hh, c("hhinccat", "ST_PUMA"), "WGTP")
#hhsizeCounts <- count(hh, c("HHSIZE", "ST_PUMA"), "WGTP")
#hhworkersCounts <- count(hh, c("workers", "ST_PUMA"), "WGTP")
#
## create PUMS summaries (ST_PUMA level) data frame
#countsPUMS <- data.frame(hhAgeCounts[hhAgeCounts$HHAGECAT==1,2:3], hhAgeCounts[hhAgeCounts$HHAGECAT==2,3],
#					   hhincCounts[hhincCounts$hhinccat==1,3], hhincCounts[hhincCounts$hhinccat==2,3], hhincCounts[hhincCounts$hhinccat==3,3], hhincCounts[hhincCounts$hhinccat==4,3],
#					   hhsizeCounts[hhsizeCounts$HHSIZE==1,3], hhsizeCounts[hhsizeCounts$HHSIZE==2,3], hhsizeCounts[hhsizeCounts$HHSIZE==3,3], hhsizeCounts[hhsizeCounts$HHSIZE==4,3], hhsizeCounts[hhsizeCounts$HHSIZE==5,3],
#					   hhworkersCounts[hhworkersCounts$workers==0,3], hhworkersCounts[hhworkersCounts$workers==1,3], hhworkersCounts[hhworkersCounts$workers==2,3], hhworkersCounts[hhworkersCounts$workers==3,3])
#colnames(countsPUMS) <- c("ST_PUMA", "hhage0064", "hhage65up", "hhinc1", "hhinc2", "hhinc3", "hhinc4", "hhsize1", "hhsize2",
#						"hhsize3", "hhsize4", "hhsize5", "hwork0", "hwork1", "hwork2", "hworkna")
#
#distPUMS <- countsPUMS
#distPUMS[,(2:ncol(distPUMS))] <- 0
#
## compute ST_PUMA level distributions
##Age
#distPUMS[,2] <- countsPUMS$hhage0064/(countsPUMS$hhage0064+countsPUMS$hhage65u)
#distPUMS[,3] <- countsPUMS$hhage65u/(countsPUMS$hhage0064+countsPUMS$hhage65u)
##HHinc
#distPUMS[,4] <- countsPUMS$hhinc1/(countsPUMS$hhinc1+countsPUMS$hhinc2+countsPUMS$hhinc3+countsPUMS$hhinc4)
#distPUMS[,5] <- countsPUMS$hhinc2/(countsPUMS$hhinc1+countsPUMS$hhinc2+countsPUMS$hhinc3+countsPUMS$hhinc4)
#distPUMS[,6] <- countsPUMS$hhinc3/(countsPUMS$hhinc1+countsPUMS$hhinc2+countsPUMS$hhinc3+countsPUMS$hhinc4)
#distPUMS[,7] <- countsPUMS$hhinc4/(countsPUMS$hhinc1+countsPUMS$hhinc2+countsPUMS$hhinc3+countsPUMS$hhinc4)
##hhsize
#distPUMS[,8] <- countsPUMS$hhsize1/(countsPUMS$hhsize1+countsPUMS$hhsize2+countsPUMS$hhsize3+countsPUMS$hhsize4+countsPUMS$hhsize5)
#distPUMS[,9] <- countsPUMS$hhsize2/(countsPUMS$hhsize1+countsPUMS$hhsize2+countsPUMS$hhsize3+countsPUMS$hhsize4+countsPUMS$hhsize5)
#distPUMS[,10] <- countsPUMS$hhsize3/(countsPUMS$hhsize1+countsPUMS$hhsize2+countsPUMS$hhsize3+countsPUMS$hhsize4+countsPUMS$hhsize5)
#distPUMS[,11] <- countsPUMS$hhsize4/(countsPUMS$hhsize1+countsPUMS$hhsize2+countsPUMS$hhsize3+countsPUMS$hhsize4+countsPUMS$hhsize5)
#distPUMS[,12] <- countsPUMS$hhsize5/(countsPUMS$hhsize1+countsPUMS$hhsize2+countsPUMS$hhsize3+countsPUMS$hhsize4+countsPUMS$hhsize5)
##hworkers
#distPUMS[,13] <- countsPUMS$hwork0/(countsPUMS$hwork0+countsPUMS$hwork1+countsPUMS$hwork2+countsPUMS$hworkna)
#distPUMS[,14] <- countsPUMS$hwork1/(countsPUMS$hwork0+countsPUMS$hwork1+countsPUMS$hwork2+countsPUMS$hworkna)
#distPUMS[,15] <- countsPUMS$hwork2/(countsPUMS$hwork0+countsPUMS$hwork1+countsPUMS$hwork2+countsPUMS$hworkna)
#distPUMS[,16] <- countsPUMS$hworkna/(countsPUMS$hwork0+countsPUMS$hwork1+countsPUMS$hwork2+countsPUMS$hworkna)
#
##write down ST_PUMA level counts and distribution to CSVs for future use
#write.csv(countsPUMS, "countsPUMS.csv")
#write.csv(distPUMS, "distPUMS.csv")


# Read PUMS files
# read and process PUMS once (above), for subsequent runs just read in distribution CSVs
countsPUMS <- read.csv("countsPUMS.csv")
distPUMS <- read.csv("distPUMS.csv")

# Read new household totals
hhTotals <- read.csv("cmap_2010hh_by_zone09.csv")
hhTotals <- hhTotals[order(hhTotals$ZONE09),]

#copy ST_PUMA IDs to seed and HH totals
seed$ST_PUMA <- geog$ST_PUMA[match(seed$taz, geog$zone09)]
hhTotals$ST_PUMA <- geog$ST_PUMA[match(hhTotals$ZONE09, geog$zone09)]

#-------------------------------------------------------------------------
# Start 2-D balancing to compute new controls

# Create empty controls table
finalControls <- seed

finalControls[,c(3:(ncol(finalControls)-1))] <- 0
finalControls <- finalControls[order(finalControls$taz),]

controlsList <- list(c("hhage0064", "hhage65up"),
					 c("hhinc1", "hhinc2", "hhinc3", "hhinc4"),
					 c("hhsize1", "hhsize2","hhsize3", "hhsize4", "hhsize5"),
					 c("hwork0", "hwork1", "hwork2", "hworkna"))

# loop through all ST_PUMAs
for(stPuma in ListST_PUMA){

	for(controlSet in controlsList){
		# get seed matrix
		seedMat <- data.matrix(seed[seed$ST_PUMA==stPuma,controlSet])
		# create margin matrix [rowMargin Col, colMargin Col]
		pumaControlDist <- distPUMS[distPUMS$ST_PUMA==stPuma,controlSet]
		pumaTotHH <- sum(hhTotals$HH[hhTotals$ST_PUMA==stPuma])
		pumaControlTotals <- pumaControlDist * pumaTotHH
		pumaMarginals <- unname(unlist(pumaControlTotals[1,]))
		marginList <- list(hhTotals$HH[hhTotals$ST_PUMA==stPuma], pumaMarginals)
		result <- ipf(marginList, seedMat)
		finalControls[finalControls$ST_PUMA==stPuma,controlSet] <- result
	}
	cat("Done with PUMA: ", stPuma, "|", "\n")
}

setwd(OutDir)
finalControls <- round(finalControls,4)
finalControls$hhs <- hhTotals$HH[match(finalControls$taz, hhTotals$ZONE09)]
finalControls$ST_PUMA <- NULL
write.csv(finalControls, "forecastControls.csv", row.names=FALSE)

# Turn back old options
options(warn = oldw)
options(scipen = olds)
