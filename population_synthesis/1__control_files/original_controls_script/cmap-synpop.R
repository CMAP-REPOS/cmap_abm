
#CMAP SynPop Files
#Ben Stabler, stabler@pbworld.com, 021111

################################################################################

PopSynDir <- getwd() #"S:/AdminGroups/ResearchAnalysis/nmp/ABM/source_code/cmap_abm/population_synthesis"
setwd(file.path(PopSynDir, "2__synpop"))

################################################################################

## FUNCTIONS

#faster version of read.fwf
fast.read.fwf = function(fileName, widths) {

	tableData = scan(fileName, what="", sep="\n")
	widthssum = cumsum(widths)
	result = matrix(0, length(tableData), length(widths))
	result = as.data.frame(result)

	for(i in 1:length(widths)) {
		if(i==1) {
			start = 1
		} else {
			start = widthssum[i-1] + 1
		}
		result[,i] = substr(tableData, start, widthssum[i])
	}
	return(result)
}


# FIPS Code (15 ): State (2), County (3), Tract (6), Block Group (1), Block (3)
genFIPSCode = function(state, county, tract="", blockgroup="", block="") {
	fips = paste(	sprintf("%02.0f", as.integer(as.character(state))),
								sprintf("%03.0f", as.integer(as.character(county))),
								ifelse(tract=="", "", sprintf("%06.0f", as.integer(as.character(tract)))),
								ifelse(blockgroup=="", "", sprintf("%1.0f",  as.integer(as.character(blockgroup)))),
								ifelse(block=="", "", sprintf("%03.0f", as.integer(as.character(block)))),
							  sep="")
	return(fips)
}


# FIPS TAZ Code (11 characters): State (2), County (3), TAZ (6)
genFIPSCodeTAZ = function(state, county, taz) {
	fips = paste(	sprintf("%02.0f", as.integer(as.character(state))),
								sprintf("%03.0f", as.integer(as.character(county))),
								as.character(taz),
								sep="")
	return(fips)
}

################################################################################

#1)	Create Zone Correspondence Table – Zone, FIPS code, PUMA 1% code, PUMA 5% code
zones = read.csv("zone_puma_corresp.csv")

################################################################################

#2) Get PUMS Data and download to PUMS folder

#2.1) Determine 1% PUMS Codes to figure what states to download
puma1 = sort(unique(zones$puma1))
puma5 = sort(unique(zones$puma5))

#2.2) Look up PUMA1 codes on map - http://www2.census.gov/geo/maps/puma/puma2k --> IL, IN, WI
#Download 1% files for all states: http://www2.census.gov/census_2000/datasets/PUMS/OnePercent/Illinois/all_Illinois.zip --> pums_17.dat
#Download 5% files for all states: http://www2.census.gov/census_2000/datasets/PUMS/FivePercent/Illinois/all_Illinois.zip --> PUMS5_17.TXT

#Merge all three states into 1 PUMS.txt file

#2.3)	Copy over PUMS2000D.txt data dictionary file used to index into PUMS files

################################################################################

#3) Create geography conversion files (TAZ00_BLKGRP00.csv,TAZ00_TRACT_COUNTY.csv,BLK00_TAZ00.csv,TRACT00_PUMA5.csv,TAZ00_PUMA5.csv)
# Note LookUpTable.file (TAZ00_TRACT_COUNTY_SUPERCOUNTY_PUMA5.csv) used in validation step
# Put in conversion folder
# Create crosswalks for:
	#TAZ  -> Block
	#			-> Block Group
	#			-> PUMA 5
	#			-> Tract
	#			-> County (and SuperCounty (aka county group) for validation)
	#Tract -> PUMA 5

#3.1) Download shapefiles (Block Group, PUMA5, Tract, County, Census TAZ) --> http://www.census.gov/geo/www/cob/bdy_files.html
#3.2) Download shapefiles for Blocks --> http://arcdata.esri.com/data/tiger2000/tiger_download.cfm

#3.3) Run ArcGIS Merge operation to merge states by geography

#3.4) Run ArcGIS Define Projection to create prj for shapes (set to Geographic WGS 1984)

#3.5) Create BLK00_TAZ00.csv - create centroid for block and then assign each to a TAZ
#				Add an X and Y field (prec=10,scale=3) and then right click the field and do Calculate Geometry.  Then do Tools + Add XY Data and then export data to shapefile
#				Then spatial join to TAZ file and only takes those that fall within the TAZ to identify TAZ for each block

				####  blk00-centroid-clip.shp has adjusted centroids to ensure every block gets a TAZ

#3.6) Create TAZ00_BLKGRP00.csv - create by using ArcGIS Intersect operation --> saved to taz_bg.shp
#				Add new area field and calculate geometry to get area
#				Recalculate TAZ area and block group area into new fields and then calculate percents for TAZ to BG crosswalk

library(foreign)
taz_bg = read.dbf("shapefiles/taz_bg.dbf")
taz_bg$bg = paste("A", genFIPSCode(substr(taz_bg$COUNTY,0,2), substr(taz_bg$COUNTY,3,9999), taz_bg$TRACT, taz_bg$BLKGROUP), sep="")
taz_area = tapply(taz_bg$newarea, taz_bg$zone09, sum)
bg_area = tapply(taz_bg$newarea, taz_bg$bg, sum)

#function to calculate percent by taz and by block group
addPercents = function(x) {
	x$taz_pct = x$newarea / sum(x$newarea)
	x$bg_pct = x$newarea / bg_area[match(x$bg, names(bg_area))]
	return(x)
}

taz2 = by(taz_bg, taz_bg$zone09, function(x) x)
taz2 = lapply(taz2, addPercents)
taz2 = do.call(rbind, taz2)

taz3 = taz2[,c("zone09","taz_pct","bg","bg_pct")]
colnames(taz3) = c("TAZ","TAZPc","BLKGRP","BlkGrpPc")
write.csv(taz3,"conversion/TAZ00_BLKGRP00.csv",row.names=F)

#3.7) Create TAZ to PUMA5 --> from zone_puma_corresp.csv file

#3.8) Create TAZ00_TRACT_COUNTY.csv --> already have taz to county from zone_puma_corresp.csv file, just need taz to tract
#		There are multiple tracts per TAZ.  Tracts are only used for summaries/validation
#   since SF1 is block and SF3 is block group
#		CTPP data can be at the TAZ level, depending on if the agency submitted their TAZs to census
#   So will not actually use this conversion for CMAP, but need to fake it to get it to run so
#     just use a 1 to 1 mapping from CTPP zone (TAZ_CTPP_S) to TAZ (Centroid)

#   Create a TAZ centroid (like for the blocks above) and spatial join to tag a TRACT (taz_tr.dbf)

taz_tr = read.dbf("shapefiles/taz_tr.dbf")
taz_tr$new_tract = genFIPSCode(taz_tr$STATE, taz_tr$COUNTY_1, taz_tr$TRACT)
write.csv(data.frame(TAZ=taz_tr$zone09, TRACT=taz_tr$new_tract), "temp.csv", row.names=F)

#3.9) Create TRACT00_PUMA5.csv by creating a tract centroid and spatial join to tag a PUMA5 (tr_p.dbf)

tr_p = read.dbf("shapefiles/tr_p.dbf")
tr_p$new_tract = genFIPSCode(tr_p$STATE, tr_p$COUNTY, tr_p$TRACT)
write.csv(data.frame(TRACT=tr_p$new_tract, PUMA5=tr_p$PUMA5), "temp.csv", row.names=F)

#3.10) Create taz00_tract_county_supercounty_puma5.csv from the other files with super county as state code

#3.11) Create a Census TAZ (tz_d00.shp) to CMAP TAZ mapping in order to convert ctpp data since CTPP data
#		is not available at the block group leve (but rather the Census taz level, which is not the same zone system for CMAP)
#		Note that Census TAZs are not available for the entire CMAP area so need to use Census Tracts as well
#
#   a) Create by using ArcGIS Intersect operation --> saved to tz_taz.shp
#				Add new area field and calculate geometry to get area
#				Recalculate TAZ area and block group area into new fields and then calculate percents for TAZ to BG crosswalk

tz_taz = read.dbf("shapefiles/tz_taz.dbf")
tz_taz$census_taz = paste("A", genFIPSCodeTAZ(tz_taz$STATE, tz_taz$COUNTY, tz_taz$TAZ), sep="")
tz_area = tapply(tz_taz$newarea, tz_taz$census_taz, sum)
zone09_area = tapply(tz_taz$newarea, tz_taz$zone09, sum)

#function to calculate percent by census taz and cmap taz
addPercents = function(x) {
	x$taz_pct = x$newarea / sum(x$newarea)
	x$zone09_pct = x$newarea / zone09_area[match(x$zone09, names(zone09_area))]
	return(x)
}

taz2 = by(tz_taz, tz_taz$census_taz, function(x) x)
taz2 = lapply(taz2, addPercents)
taz2 = do.call(rbind, taz2)

taz3 = taz2[,c("census_taz","taz_pct","zone09","zone09_pct")]
colnames(taz3) = c("TAZ_CTPP","TAZ_CTPPPc","CENTROID","CENTROIDPC") #CENTROID=CMAP ZONE NUMBER

#
#		b) Create tract to CMAP TAZ as well where each TRACT is like a CENSUS TAZ --> tz_tr.shp

tz_tr = read.dbf("shapefiles/tz_tr.dbf")
tz_tr$FIPS = paste("A", genFIPSCode(substr(tz_tr$COUNTY,0,2), substr(tz_tr$COUNTY,3,9999), tz_tr$TRACT), sep="")

tr_area = tapply(tz_tr$newarea, tz_tr$FIPS, sum)
zone09_area = tapply(tz_tr$newarea, tz_tr$zone09, sum)

#function to calculate percent by taz and by tract
addPercents = function(x) {
	x$tract_pct = x$newarea / sum(x$newarea)
	x$zone09_pct = x$newarea / zone09_area[match(x$zone09, names(zone09_area))]
	return(x)
}

taz2 = by(tz_tr, tz_tr$FIPS, function(x) x)
taz2 = lapply(taz2, addPercents)
taz2 = do.call(rbind, taz2)

taz4 = taz2[,c("FIPS","tract_pct","zone09","zone09_pct")]
colnames(taz4) = c("TAZ_CTPP","TAZ_CTPPPc","CENTROID","CENTROIDPC") #using Census Tract FIPS code as a CTPP TAZ ID
taz5 = rbind(taz3,taz4)
write.csv(taz5,"conversion/CENSUS_TAZ00_TAZ.csv",row.names=F) #CTPP TAZs and CENSUS TRACTS in this file

################################################################################

#4) Get Census SF1, SF3, and CTPP files to put in Census folder to run conversion which will take census folder
#files and create TAZ versions of these that are used by PopSyn.

#4.1) SF1 (Block level) http://www2.census.gov/census_2000/datasets/Summary_File_1/
#   then download geo file (ilgeo_uf1.zip) + files il00002_uf1.zip and il00003_uf1.zip to get summaries P7,8,12,14,19,20,23,26,27
#4.2) SF3 (Block group level) http://www2.census.gov/census_2000/datasets/Summary_File_3/
#   then download geo files (ilgeo_uf3.zip) + all files  (all_Illinios.zip) and keep
#   files 1 - 7 to get summaries P12,14,18,36,43,47,52,55,76,79,87

#4.3) Get the Access template files to get the column headers
#		http://www.census.gov/support/cen2000_sf1ASCII.html --> then download the Access DB and save out the column headers for SF1 File 2 and SF1 File 3 to text files with first row as column headers
#   http://www.census.gov/support/cen2000_sf3ASCII.html --> Repeat process for SF3 column headers as well

#4.4) CTPP (hh by Workers) http://www.transtats.bts.gov/ --> CTPP2000 --> Download CTPP 2000 Part1 (Ascii File) which includes raw files + documentation
#		Download IL, IN, and WI zip file (T_CTPP2000_PART1_IL.zip for example)
#		Then unzip the CTPP_<IL>_Pt1.zip file and unzip IL_ctpp1_t047_t073.dat (tables 47-73) and tables 074_087 file
#   Also unzip Part1-Docs-SASprograms-OtherFiles.zip and pull out the ctpp1_t047_t073.loc geo file, labels047_073.prn column names file and the tables 074_087 files as well
#		Will convert this to CMAP zones in R

################################################################################

#5) Reformat Census SF1 data into SynPop format Block, Field1, Field2, etc

#5.1) Read SF1 geographic file

readGeoFile = function(filename) {

	colWidths = c(6,2,3,2,3,2,7,1,1,2,2,3,2,5,2,2,5,2,1,2,6,1,4)
	colWidths2 = cumsum(colWidths)

	geo = scan(filename, what="", sep="\n")
	geo = substr(geo,0,sum(colWidths))

	geo = data.frame(SUMLEV=substr(geo, colWidths2[2]+1, colWidths2[3]),
		STATE=substr(geo, colWidths2[10]+1, colWidths2[11]),
		COUNTY=substr(geo, colWidths2[11]+1, colWidths2[12]),
		TRACT=substr(geo, colWidths2[20]+1, colWidths2[21]),
		BLKGRP=substr(geo, colWidths2[21]+1, colWidths2[22]),
		BLOCK=substr(geo, colWidths2[22]+1, colWidths2[23]))

	geo$FIPS = genFIPSCode(geo$STATE, geo$COUNTY, geo$TRACT, substr(geo$BLOCK,1,1), substr(geo$BLOCK,2,4))
	return(geo)
}

ilgeo = readGeoFile("sf1/ilgeo.uf1")
ingeo = readGeoFile("sf1/ingeo.uf1")
wigeo = readGeoFile("sf1/wigeo.uf1")

#5.2) Read SF1 Files

getSF1Fields = function(fileName, headerFileName, geoTable, fields) {

	sf1Table = read.csv(fileName, header=F) #same record order as ilgeo so can just cbind
	colnames(sf1Table) = colnames(read.csv(headerFileName))
	sf1Table = cbind(BLOCK=geoTable$FIPS, sf1Table)

	#create output block table
	resultTable = data.frame(BLOCK=sf1Table[,"BLOCK"])

	#loop through Census tables and add to result table
	for(i in 1:length(fields)) {
		tableFields = sf1Table[,grep(fields[i],colnames(sf1Table))]
		resultTable = cbind(resultTable, tableFields)
	}

	#remove non-block level records
	resultTable = resultTable[geoTable$SUMLEV==101,] #block level records only
	return(resultTable)
}

fields = c("P007","P008","P012","P014")
ilSf1_2 = getSF1Fields("sf1/il00002.uf1", "sf1/SF10002.txt", ilgeo, fields)
inSf1_2 = getSF1Fields("sf1/in00002.uf1", "sf1/SF10002.txt", ingeo, fields)
wiSf1_2 = getSF1Fields("sf1/wi00002.uf1", "sf1/SF10002.txt", wigeo, fields)

fields = c("P019","P020","P021","P023","P026","P027")
ilSf1_3 = getSF1Fields("sf1/il00003.uf1", "sf1/SF10003.txt", ilgeo, fields)
inSf1_3 = getSF1Fields("sf1/in00003.uf1", "sf1/SF10003.txt", ingeo, fields)
wiSf1_3 = getSF1Fields("sf1/wi00003.uf1", "sf1/SF10003.txt", wigeo, fields)

#merge all the tables
sf1 = cbind(rbind(ilSf1_2, inSf1_2, wiSf1_2), rbind(ilSf1_3, inSf1_3, wiSf1_3)[,-1])
sf1 = cbind(BLOCK=paste("A", sf1$BLOCK, sep=""), sf1)
colnames(sf1)[2] = "Block00"
write.csv(sf1, "sf1/sf1.csv", row.names=F)

rm(ilSf1_2, inSf1_2, wiSf1_2, ilSf1_3, inSf1_3, wiSf1_3, ilgeo, ingeo, wigeo)

#5.3) Reformat CTPP files

#table 64 (Household size by Household income in 1999)

# ctpp1_t047_t073.loc contains the file structure
ctppfieldnames = scan("ctpp/labels047_073.prn", what="", sep="\n")
ctppfieldnames = unlist(lapply(strsplit(ctppfieldnames, " +="), function(x) x[[1]]))
colWidths = c(5,3,3,3,5,3,5,4,6,1,4,4,5,6,5,4,6,8)  #location fields only

#table 64 structure
loc = 4635
cells = 130
cellsize = 9

tab64cols = c(colWidths, loc-sum(colWidths), rep(cellsize, cells))
il_tab64 = fast.read.fwf("ctpp/IL_ctpp1_t047_t073.dat", tab64cols)
in_tab64 = fast.read.fwf("ctpp/IN_ctpp1_t047_t073.dat", tab64cols)
wi_tab64 = fast.read.fwf("ctpp/WI_ctpp1_t047_t073.dat", tab64cols)

colnames(il_tab64) = colnames(in_tab64) = colnames(wi_tab64) = c(ctppfieldnames[1:length(colWidths)], "junk", ctppfieldnames[grep("tab64",ctppfieldnames)])
il_tab64 = il_tab64[,-(length(colWidths)-1)]
in_tab64 = in_tab64[,-(length(colWidths)-1)]
wi_tab64 = wi_tab64[,-(length(colWidths)-1)]

#merge states
tab64 = rbind(il_tab64, in_tab64, wi_tab64)
tab64$taz = gsub(" +", "", tab64$taz) #remove whitespace
tab64$sumlev = as.numeric(gsub(" +", "", tab64$sumlev)) #remove whitespace
tab64$state = as.numeric(gsub(" +", "", tab64$state)) #remove whitespace
tab64$county = as.numeric(gsub(" +", "", tab64$county)) #remove whitespace

#convert to integer - has "." in it since CTPP files are "." for no data
x = as.integer(as.matrix(tab64[,(length(colWidths)-1):ncol(tab64)]))
x[is.na(x)] = 0
tab64[,(length(colWidths)-1):ncol(tab64)] =x
rm(il_tab64, in_tab64, wi_tab64)

#keep TAZ and Tract records and calculate TAZ ID
tab64 = tab64[tab64$sumlev==140 | tab64$sumlev==940,] # SUMLEV (140=tract, 150=blkgrp, 795=puma5, 940=taz)
tab64$TAZ_CTPP[tab64$sumlev==140] = paste("A", genFIPSCode(tab64$state[tab64$sumlev==140], tab64$county[tab64$sumlev==140], tab64$tract[tab64$sumlev==140]), sep="")
tab64$TAZ_CTPP[tab64$sumlev==940] = paste("A", genFIPSCodeTAZ(tab64$state[tab64$sumlev==940], tab64$county[tab64$sumlev==940], tab64$taz[tab64$sumlev==940]), sep="")
tab64 = tab64[,-(1:length(colWidths))]
tab64 = tab64[,c(ncol(tab64),1:(ncol(tab64)-1))]
write.csv(tab64, "ctpp/tab64.csv", row.names=F)

#table 75 (Household Size by Number of workers in household by Household income in 1999)

ctppfieldnames = scan("ctpp/labels074_087.prn", what="", sep="\n")
ctppfieldnames = unlist(lapply(strsplit(ctppfieldnames, " +="), function(x) x[[1]]))
colWidths = c(5,3,3,3,5,3,5,4,6,1,4,4,5,6,5,4,6,8)  #location fields only

#table 75 structure
loc = 1701
cells = 330
cellsize = 9

tab75cols = c(colWidths, loc-sum(colWidths), rep(cellsize, cells))
il_tab75 = fast.read.fwf("ctpp/IL_ctpp1_t074_t087.dat", tab75cols)
in_tab75 = fast.read.fwf("ctpp/IN_ctpp1_t074_t087.dat", tab75cols)
wi_tab75 = fast.read.fwf("ctpp/WI_ctpp1_t074_t087.dat", tab75cols)

colnames(il_tab75) = colnames(in_tab75) = colnames(wi_tab75) = c(ctppfieldnames[1:length(colWidths)], "junk", ctppfieldnames[grep("tab75",ctppfieldnames)])
il_tab75 = il_tab75[,-(length(colWidths)-1)]
in_tab75 = in_tab75[,-(length(colWidths)-1)]
wi_tab75 = wi_tab75[,-(length(colWidths)-1)]

#merge states
tab75 = rbind(il_tab75, in_tab75, wi_tab75)
tab75$taz = gsub(" +", "", tab75$taz) #remove whitespace
tab75$sumlev = as.numeric(gsub(" +", "", tab75$sumlev)) #remove whitespace
tab75$state = as.numeric(gsub(" +", "", tab75$state)) #remove whitespace
tab75$county = as.numeric(gsub(" +", "", tab75$county)) #remove whitespace

#convert to integer - has "." in it since CTPP files are "." for no data
x = as.integer(as.matrix(tab75[,(length(colWidths)-1):ncol(tab75)]))
x[is.na(x)] = 0
tab75[,(length(colWidths)-1):ncol(tab75)] =x
rm(il_tab75, in_tab75, wi_tab75)

#keep TAZ and Tract records and calculate TAZ ID
tab75 = tab75[tab75$sumlev==140 | tab75$sumlev==940,] # SUMLEV (140=tract, 150=blkgrp, 795=puma5, 940=taz)
tab75$TAZ_CTPP[tab75$sumlev==140] = paste("A", genFIPSCode(tab75$state[tab75$sumlev==140], tab75$county[tab75$sumlev==140], tab75$tract[tab75$sumlev==140]), sep="")
tab75$TAZ_CTPP[tab75$sumlev==940] = paste("A", genFIPSCodeTAZ(tab75$state[tab75$sumlev==940], tab75$county[tab75$sumlev==940], tab75$taz[tab75$sumlev==940]), sep="")
tab75 = tab75[,-(1:length(colWidths))]
tab75 = tab75[,c(ncol(tab75),1:(ncol(tab75)-1))]
write.csv(tab75, "ctpp/tab75.csv", row.names=F)

################################################################################

#6) Convert CTPP data to CMAP TAZ level

tz_taz = read.csv("inputs/conversion/CENSUS_TAZ00_TAZ.csv")
tab64 = read.csv("ctpp/tab64.csv")
tab64 = merge(tz_taz,tab64)
tab64[,5:ncol(tab64)] = tab64$TAZ_CTPPPc * as.matrix(tab64[,5:ncol(tab64)])
tab64_taz = data.frame(TAZ=names(tapply(tab64[,1], tab64$CENTROID, length)), apply(tab64[,5:ncol(tab64)], 2, function(x) tapply(x, tab64$CENTROID, sum)))
write.csv(tab64_taz, "ctpp/tab64_taz.csv", row.names=F)

tab75 = read.csv("ctpp/tab75.csv")
tab75 = merge(tz_taz,tab75)
tab75[,5:ncol(tab75)] = tab75$TAZ_CTPPPc * as.matrix(tab75[,5:ncol(tab75)])
tab75_taz = data.frame(TAZ=names(tapply(tab75[,1], tab75$CENTROID, length)), apply(tab75[,5:ncol(tab75)], 2, function(x) tapply(x, tab75$CENTROID, sum)))
write.csv(tab75_taz, "ctpp/tab75_taz.csv", row.names=F)

################################################################################

#7) Reformat Census SF3 data into SynPop format BlockGroup, Field1, Field2, etc

#7.1) Read SF3 geographic file

ilgeo = readGeoFile("sf3/ilgeo.uf3")
ingeo = readGeoFile("sf3/ingeo.uf3")
wigeo = readGeoFile("sf3/wigeo.uf3")

#7.2) Read SF3 Files

getSF3Fields = function(fileName, headerFileName, geoTable, fields) {

	sf3Table = read.csv(fileName, header=F) #same record order as ilgeo so can just cbind
	colnames(sf3Table) = colnames(read.csv(headerFileName))
	sf3Table = cbind(BLKGRP=geoTable$FIPS, sf3Table)

	#create output block table
	resultTable = data.frame(BLKGRP=sf3Table[,"BLKGRP"])

	#loop through Census tables and add to result table
	for(i in 1:length(fields)) {
		tableFields = sf3Table[,grep(fields[i],colnames(sf3Table))]
		resultTable = cbind(resultTable, tableFields)
	}

	#remove non-block group level records
	resultTable = resultTable[geoTable$SUMLEV==150,] #block group level records only
	return(resultTable)
}

fields = c("P012","P014")
ilSf3_1 = getSF3Fields("sf3/il00001.uf3", "sf3/SF30001.txt", ilgeo, fields)
inSf3_1 = getSF3Fields("sf3/in00001.uf3", "sf3/SF30001.txt", ingeo, fields)
wiSf3_1 = getSF3Fields("sf3/wi00001.uf3", "sf3/SF30001.txt", wigeo, fields)

fields = c("P018")
ilSf3_2 = getSF3Fields("sf3/il00002.uf3", "sf3/SF30002.txt", ilgeo, fields)
inSf3_2 = getSF3Fields("sf3/in00002.uf3", "sf3/SF30002.txt", ingeo, fields)
wiSf3_2 = getSF3Fields("sf3/wi00002.uf3", "sf3/SF30002.txt", wigeo, fields)

fields = c("P036")
ilSf3_3 = getSF3Fields("sf3/il00003.uf3", "sf3/SF30003.txt", ilgeo, fields)
inSf3_3 = getSF3Fields("sf3/in00003.uf3", "sf3/SF30003.txt", ingeo, fields)
wiSf3_3 = getSF3Fields("sf3/wi00003.uf3", "sf3/SF30003.txt", wigeo, fields)

fields = c("P043")
ilSf3_4 = getSF3Fields("sf3/il00004.uf3", "sf3/SF30004.txt", ilgeo, fields)
inSf3_4 = getSF3Fields("sf3/in00004.uf3", "sf3/SF30004.txt", ingeo, fields)
wiSf3_4 = getSF3Fields("sf3/wi00004.uf3", "sf3/SF30004.txt", wigeo, fields)

fields = c("P047")
ilSf3_5 = getSF3Fields("sf3/il00005.uf3", "sf3/SF30005.txt", ilgeo, fields)
inSf3_5 = getSF3Fields("sf3/in00005.uf3", "sf3/SF30005.txt", ingeo, fields)
wiSf3_5 = getSF3Fields("sf3/wi00005.uf3", "sf3/SF30005.txt", wigeo, fields)

fields = c("P052","P055")
ilSf3_6 = getSF3Fields("sf3/il00006.uf3", "sf3/SF30006.txt", ilgeo, fields)
inSf3_6 = getSF3Fields("sf3/in00006.uf3", "sf3/SF30006.txt", ingeo, fields)
wiSf3_6 = getSF3Fields("sf3/wi00006.uf3", "sf3/SF30006.txt", wigeo, fields)

fields = c("P076","P079","P087")
ilSf3_7 = getSF3Fields("sf3/il00007.uf3", "sf3/SF30007.txt", ilgeo, fields)
inSf3_7 = getSF3Fields("sf3/in00007.uf3", "sf3/SF30007.txt", ingeo, fields)
wiSf3_7 = getSF3Fields("sf3/wi00007.uf3", "sf3/SF30007.txt", wigeo, fields)

#merge all the tables
sf3 = cbind(
	rbind(ilSf3_1, inSf3_1, wiSf3_1),
	rbind(ilSf3_2, inSf3_2, wiSf3_2)[,-1],
	rbind(ilSf3_3, inSf3_3, wiSf3_3)[,-1],
	rbind(ilSf3_4, inSf3_4, wiSf3_4)[,-1],
	rbind(ilSf3_5, inSf3_5, wiSf3_5)[,-1],
	rbind(ilSf3_6, inSf3_6, wiSf3_6)[,-1],
	rbind(ilSf3_7, inSf3_7, wiSf3_7)[,-1])
sf3$BLKGRP = paste("A", sf3$BLKGRP, sep="")
write.csv(sf3, "sf3/sf3.csv", row.names=F)

rm(ilSf3_1, inSf3_1, wiSf3_1,
	ilSf3_2, inSf3_2, wiSf3_2,
	ilSf3_3, inSf3_3, wiSf3_3,
	ilSf3_4, inSf3_4, wiSf3_4,
	ilSf3_5, inSf3_5, wiSf3_5,
	ilSf3_6, inSf3_6, wiSf3_6,
	ilSf3_7, inSf3_7, wiSf3_7,
	ilgeo, ingeo, wigeo)

################################################################################

#8)	Update DataDefinitionInput.txt to use CMAP zones

#9) Create census folder and move in Sf1 and Sf3 files to run conversion and update "census directory and data" of properties file
	#Rename sf1.csv and sf3.csv to sf1all.csv and sf3all.csv since they need a "table" name as well
	#These will be converted with RunConversion=true later

#10) Update "converted directory and data" section of properties file

	#Add sf1, sf3, and tab 64 and tab 65 ctpp tables to the "converted.tables"

################################################################################

#11) Update "design directory and data" section of properties file and create all design folder inputs
	#Copy over tab62 ctpp table to DataTables folder and rename ctpp00162.csv

#11.1) Create TAZIndexTable table

#11.2) Copy over hh cat file - HHCatFile.csv

#11.3) PopSyn User Control Inputs 88s categories.xls is a wizard to create a number of the files
		#Since we're using the ARC 88 control setup, we don't have to change many of these files.
		#Copy over baseIncidence.csv, baseIncidenceLastRow.csv, baseMetaIncidence.csv, baseMetaIncidenceLastRow.csv,
		#  ForecastControlManager.csv, futureIncidence.csv, futureIncidenceLastRow.csv, metaTargetGrouping.csv

#11.4) Create BaseYearSourceData.csv

		#From the labels*****.prn files, translate the table field names to the control file
		#For example: CTPP Tab 64, HH income 0-20K, hh size 1	person (SIZE=2 variable): ctpp, tab64, tab64x28+tab64x29+tab64x30+tab64x31+tab64x32+tab64x33

#11.5) Create BaseYearCensusVStatistics.csv and updated universeIDs in properties file since some IDs where dropped and the IDs renumbered from the ARC version
		#and change NoValidationStatistics to 102

		#This needs to be changed back to 107 in order to work!!!!

#11.5) Create PUMASimilarityTable.csv - similar PUMAS to a PUMA, used when drawing HHs - based on PUMA centroid spatial proximity
			# There are no repeat PUMA IDs in the CMAP region across states

			#Start with p.shp, clip to cmap zones, and do ArcGIS Dissolve based on PUMA5 field to get single part to get 1 polygon per puma
			#Then calculate X and Y centroid fields as done for other shapes above

library(foreign)
p = read.dbf("shapefiles/p_clip_dissolve.dbf")

distanceMatrix = as.matrix(dist(cbind(p$X,p$Y)))
rownames(distanceMatrix) = colnames(distanceMatrix) = p$PUMA5
puma_dist = data.frame(PUMA_1=rownames(distanceMatrix),PUMA_2=rep(rownames(distanceMatrix),each=nrow(distanceMatrix)),DISTANCE=as.vector(distanceMatrix))
puma_dist = puma_dist[order(puma_dist$PUMA_1, puma_dist$DISTANCE),] #needs unique IDs
puma_dist$SIMILARRAN = unlist(tapply(puma_dist$PUMA_1, puma_dist$PUMA_1, function(x) 1:length(x)))
write.csv(puma_dist, "puma_dist.csv", row.names=F)

		#Then open in Excel, drop distance column, and save as PUMASimilarityTable.csv in Design folder

#11.6) Create FutureYearCensusVStatistics.csv	#### STILL TO DO

#11.7) Adds PUMS WIF attribute to print.HHAttrs	property and pums.hhattrs property

################################################################################

#12) Clip block and block group files

sf1 = read.csv("inputs/census/sf1all.csv")
sf3 = read.csv("inputs/census/sf3all.csv")

blk_taz = read.csv("inputs/conversion/BLK00_TAZ00.csv")
blkgrp_taz = read.csv("inputs/conversion/TAZ00_BLKGRP00.csv")

sf1 = sf1[sf1$BLOCK %in% blk_taz$BLOCK,]
sf3 = sf3[sf3$BLKGRP %in% blkgrp_taz$BLKGRP,]

write.csv(sf1, "inputs/census/sf1all.csv", row.names=F)
write.csv(sf3, "inputs/census/sf3all.csv", row.names=F)

################################################################################

#13)	Create model folder at same level as inputs
	#Copy over cmap.base.properties
	#Copy over log4j.xml
	#Copy over RunCMAPPopSynBase.bat --> set java xmx to 3GB
	#Copy over cmap.jar

	#Run bat file

#14) RunConversion=true and then stop and turn off conversion and re-run it

 ######### Code change - read totals HHs as file+field combination ###########

#15) run base, but first create outputs folder

#16) run with validation and add  (WriteValidationDetails = true) to properties file

################################################################################

#17) Switched to CMAP control variables

#Base Year control data rows are transposed to the base year control incidence table
# and the base year control data table maps to the HhCatFile

#Control variable index.
# *
# * 6 control variables:
# * 1) hhagecat: household header age
# * 2) hsizecat: household size
# * 3) hfamily: 	family status
# * 4) hNOCcat: 	children status
# * 5) hwrkrcat: number of workers
# * 6) hinccat1: household income category
# *
# * Important: these names must match the column labels in HHCatFile.csv

#17.1) changed HhCatFile to use hhagecat, hsizecat, hfamily, hwrkcat, hinccat2

#17.2) updated Base Year Control data to be age, size, family, wrkcat, inccat2 categories consistent with the CMAP forecast file

#17.3) Updated base year control incidence file

#17.4) Updated base year meta targets and meta target incidence.  The first file is setup to define how two input control datas
	#of the same variable (such as SF1 HH size versus CTPP HH size) are reconciled.  The second is the mapping of the variables
	#to the base year control data table

#17.5) Create new design tables from CMAP PopSyn Inputs.xls which now has 272 controls (cells)

	#Tables must have 0s not just empty cells

#17.6) Update properties file and re-run PopSyn (do not need to re-run Census to TAZ stuff since TAZ stuff has the variables (like SF1 P21 for HH age by familytype)

	# NoHHCats				=272
	# control.variables       =hinccat2,hsizecat,hwrkrcat,hfamily,hhagecat
	# Added,  RunAggregator = false


################################################################################

#18) Run forecast year

#18.1) Convert Popsyn_Controls.csv to controls specified in base distribution
		#Merge age 0-34,35-64 to 0-64 attribute
		#Reallocate (hhinc1 0-35k and hhinc2 35-60k) to (hhinc1 0-40k and hhinc2 40-60k) based on CTPP table 64 share
		#add hhs (total hhs) field

forecastControls = read.csv("Popsyn_Controls.csv")
forecastControls$hhage0064 = forecastControls$hhage0034 + forecastControls$hhage3564

#calculate share of 35-40k of 35-60k from CTPP
tab64 = read.csv("inputs/datatables/ctpptab64.csv")
hhs3540 = (tab64$tab64x14 + tab64$tab64x15)
hhs3560 = (tab64$tab64x14 + tab64$tab64x15 + tab64$tab64x16 + tab64$tab64x17 + tab64$tab64x18 + tab64$tab64x19 + tab64$tab64x20 + tab64$tab64x21)
tab64_3540share = hhs3540 / hhs3560
tab64_3540share[is.na(tab64_3540share)] = 0

#add missing zones with regional share
regionalShare = sum(hhs3540) / sum(hhs3560)
missingZones = forecastControls$zone[!(forecastControls$zone %in% tab64$TAZ)]
tab64_3540share = c(tab64_3540share, rep(regionalShare, length(missingZones)))
names(tab64_3540share) = c(tab64$TAZ, missingZones)

index = match(forecastControls$zone, names(tab64_3540share))
forecastControls$hhinc1 = forecastControls$hhinc1 + (forecastControls$hhinc2 * tab64_3540share[index])
forecastControls$hhinc2 = forecastControls$hhinc2 - (forecastControls$hhinc2 * tab64_3540share[index])

forecastControls$hhs = forecastControls$hhage0064 + forecastControls$hhage65up

forecastControls = forecastControls[,c("zone","hhs","hhage0064","hhage65up","hhinc1","hhinc2","hhinc3","hhinc4","hhsize1","hhsize2","hhsize3","hhsize4","hhsize5","hwork0","hwork1","hwork2","hworkna")]
colnames(forecastControls)[1] = "TAZ" #taz field must be "TAZ"
write.csv(forecastControls, "forecastControls.csv", row.names=F)

#18.2) Save Forecast year control data tab to ForecastControlManager.csv,
		# Save futureIncidenceLastRow.csv and futureIncidence.csv

#18.3) Add cmap.forecast2010.properties and cmap.forecast2010.properties.
	# cmap.forecast2010.properties is cmap.base.properties + "Forecast data and directory" section (see below) except
	# ForecastControlManager.csv is renamed to FutureYearSourceData.csv in properties file and filename
			#it also needs the format of the BaseYearSourceData.csv file
			#Add forecastControls.csv file to inputs/datatables folder and to "converted.tables" in properties file
			#Update the output file names such as BalancedSeedDistribution, DiscretizedSeedDistribution, HHFile, PersonFile

	# RunCMAPPopSynForecast2010.bat is RunCMAPPopSynBase.bat but for forecast2010

#18.4) Configure "Forecast data and directory" section of forecast properties file
