#Allocate SynPop HHs to MAZs
#Ben Stabler, stabler@pbworld.com, 05/24/13
#Revised to work with TG files by NMP, 5/19/16

source("../get_dir.R")  # Intelligently create PopSynDir variable
inDir1 = file.path(PopSynDir, "2__synpop/outputs")
inDir2 = file.path(PopSynDir, "3__hh_taz_to_maz/inputs")
outDir = file.path(PopSynDir, "3__hh_taz_to_maz/outputs")

#Read input files
setwd(inDir1)
hh = read.csv("ForecastHHFile.csv")
setwd(inDir2)
#mazs = read.csv("SubzoneData.csv")
hh_in = read.table("HH_IN.TXT", header=FALSE, sep=",",
                   col.names=c("subzone09","hshld","ahh","whh","chh",
                               "iqi","ahi","automs","pef"))
hh_in = subset(hh_in, select=c("subzone09","hshld"))

geog_in = read.table(file="GEOG_IN.TXT", header=FALSE, sep=",",
                     col.names=c("subzone09","cofips","coname","state","puma1",
                                 "puma5","zone09","chi","cbd","rowcol","sqmi"))
geog_in = subset(geog_in, select=c("subzone09","zone09"))

mazs = merge(geog_in, hh_in)

#Get mazs (and their num hhs) by taz
mazsByTaz = tapply(mazs$subzone09, mazs$zone09, function(x) x)
mazHHsByTaz = tapply(mazs$hshld, mazs$zone09, function(x) x)

#Randomly assign an MAZ to a TAZ using the MAZ num HHs as the probability weight
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
