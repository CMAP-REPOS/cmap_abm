
#Allocate SynPop HHs to MAZs and create maz access file
#Ben Stabler, stabler@pbworld.com, 05/24/13

source("../get_dir.R")  # Intelligently create PopSynDir variable
inDir1 = file.path(PopSynDir, "2__synpop/outputs")
inDir2 = file.path(PopSynDir, "3__hh_taz_to_maz/inputs")
outDir = file.path(PopSynDir, "3__hh_taz_to_maz/outputs")

#Read input files
setwd(inDir1)
hh = read.csv("ForecastHHFile.csv")
setwd(inDir2)
mazs = read.csv("SubzoneData.csv")

#Get mazs (and their num hhs) by taz
mazsByTaz = tapply(mazs$subzone09, mazs$zone09, function(x) x)
mazHHsByTaz = tapply(mazs$hshld, mazs$zone09, function(x) x)

#Randomly assign an MAZ to a TAZ using the MAZ num hhs as the probability weight
assignMaz = function(taz) {
  mazs = mazsByTaz[taz][[1]]
  hhs = mazHHsByTaz[taz][[1]]
  ifelse(length(mazs)>1, sample(mazs, 1, T, hhs), mazs)
}

#Get hh tazs and assign mazs
tazs = as.character(hh$TAZ)
mazs = sapply(tazs, assignMaz)

#Write result
hh$maz = mazs
setwd(outDir)
write.csv(hh, "ForecastHHFile_maz.csv", quote=F, row.names=F)

#Create maz level accessibility csv file as well
#mazs = read.csv("SubzoneData.csv")
#access = read.csv("accessibility.csv")
#
#mazs$autoPeakRetail = access$autoPeakRetail[match(mazs$zone09, access$taz)]
#mazs$autoPeakTotal = access$autoPeakTotal[match(mazs$zone09, access$taz)]
#mazs$autoOffPeakRetail = access$autoOffPeakRetail[match(mazs$zone09, access$taz)]
#mazs$autoOffPeakTotal = access$autoOffPeakTotal[match(mazs$zone09, access$taz)]
#mazs$nonMotorizedRetail = access$nonMotorizedRetail[match(mazs$zone09, access$taz)]
#mazs$nonMotorizedTotal = access$nonMotorizedTotal[match(mazs$zone09, access$taz)]
#mazs$transitPeakRetail = access$transitPeakRetail[match(mazs$zone09, access$taz)]
#mazs$transitPeakTotal = access$transitPeakTotal[match(mazs$zone09, access$taz)]
#mazs$transitOffPeakRetail = access$transitOffPeakRetail[match(mazs$zone09, access$taz)]
#mazs$transitOffPeakTotal = access$transitOffPeakTotal[match(mazs$zone09, access$taz)]
#mazs$access17 = access$access17[match(mazs$zone09, access$taz)]
#mazs$access18 = access$access18[match(mazs$zone09, access$taz)]
#
#outFields = c("subzone09","autoPeakRetail", "autoPeakTotal", "autoOffPeakRetail",
#"autoOffPeakTotal", "nonMotorizedRetail", "nonMotorizedTotal",
#"transitPeakRetail", "transitPeakTotal", "transitOffPeakRetail",
#"transitOffPeakTotal", "access17", "access18")
#write.csv(mazs[,outFields], "accessibility_maz.csv", quote=F, row.names=F)
