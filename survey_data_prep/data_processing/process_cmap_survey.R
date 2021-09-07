##########################################################################################################################
#
# Script to format CMAP raw HTS as inputs for Python based tour/trip processing tool (SPA)
#
#
# Leah Flake, March 2021
##########################################################################################################################

if (!"data.table" %in% installed.packages()) install.packages("data.table")
if (!"readxl" %in% installed.packages()) install.packages("lubridate")
if (!"magrittr" %in% installed.packages()) install.packages("lubridate")
if (!"sf" %in% installed.packages()) install.packages("lubridate")

# library(foreign)
# library(dplyr)
library(data.table)
library(lubridate)
library(readxl)
library(magrittr)
library(sf)
library(geosphere)
library(yaml)
library(stringr)
# 

get_distance_meters =
  function(location_1, location_2) {
    distance_meters =
      distHaversine(
        matrix(location_1, ncol = 2),
        matrix(location_2, ncol = 2))
    return(distance_meters)
  }


# load settings

args = commandArgs(trailingOnly = TRUE)

if(length(args) > 0){
  settings_file = args[1]
} else {
  settings_file = 'N:/Projects/CMAP_Activitysim/cmap_abm_lf/survey_data_prep/cmap_inputs.yml'
}

settings = yaml.load_file(settings_file)

# Combine data from CMAP main and NIRPC

data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
nirpc_dir = file.path(data_dir, settings$nirpc_folder)
output_dir = file.path(settings$proj_dir, 'SPA_Inputs')

# create spa input/output directories if they don't exist
dir.create(output_dir, showWarnings = FALSE)
dir.create( file.path(settings$proj_dir, 'SPA_Processed'), showWarnings = FALSE) # also create processed folder if doesn't exist

studies = c('cmap', 'nirpc')
folders = c(cmap_dir, nirpc_dir)

read_and_bind = function(folder_list, study_list, data_file_name, data_level) {
  dt_full = data.table()
  for(index in 1:length(study_list)){
    folder = folder_list[index]
    study = study_list[index]
    dt = fread(file.path(folder, data_file_name))
    dt[, study := study]
    cat(nrow(dt), 'records in', study, data_level, 'file\n')
    if('arrtime' %in% names(dt)) {
      dt[, arrtime := as.character(arrtime)]
    }
    if('deptime' %in% names(dt)) {
      dt[, deptime := as.character(deptime)]
    }
    dt_full = rbindlist(list(dt_full, dt), fill = TRUE, use.names = TRUE)
  }
  
  cat(nrow(dt_full), 'total', data_level, 'records\n')
  
  return(dt_full)
}


hh_raw = read_and_bind(folders, studies, 'household.csv', 'hh')
per_raw = read_and_bind(folders, studies, 'person.csv', 'person')
place_raw = read_and_bind(folders, studies, 'place.csv', 'place')
transit_raw = read_and_bind(folders, studies, 'place_transit.csv', 'place')
vehicle_raw = read_and_bind(folders, studies, 'vehicle.csv', 'vehicle')
location_raw = read_and_bind(folders, studies, 'location_wZones.csv', 'location')




# Weights: CMAP already has weight attached (wthhfin, wtperfin)
# Read in and attach IPF weight to nirpc

nirpc_hh_weights = fread(file.path(nirpc_dir, 'weights', 'ipf', 'household_weights.csv'))
nirpc_per_weights = fread(file.path(nirpc_dir, 'weights', 'ipf', 'person_weights.csv'))

hh_raw[nirpc_hh_weights, wthhfin_nirpc := i.wthhfin, on = .(sampno)]
per_raw[nirpc_per_weights, wtperfin_nirpc := i.wtperfin, on = .(sampno, perno)]

hh_raw[, wthhfin := fcoalesce(wthhfin, wthhfin_nirpc)]
per_raw[, wtperfin := fcoalesce(wtperfin, wtperfin_nirpc)]

# # remove sunday travel dates?
# hh_raw = hh_raw[wday(travdate) != 1]
# per_raw = per_raw[sampno %in% hh_raw$sampno]
# place_raw = place_raw[sampno %in% hh_raw$sampno]

# Read mode/purpose edits
purpose_edits = setDT(read_xlsx(file.path(data_dir, 'tpurp_update.xlsx')))
purpose_edits_nirpc = setDT(read_xlsx(file.path(data_dir, 'tpurp_update_nirpc.xlsx')))
mode_edits = setDT(read_xlsx(file.path(data_dir, 'mode_update.xlsx')))

### --- School bus incorrectly coded as transit --- ###
place_raw[mode!=401 & (payf_o %like% "school bus" | payf_o %like% "public school"), mode:=401]
#
### --- "Other" mode --- ###
place_raw[study == 'nirpc' & mode==997 & mode_o=="RV" & perno==1,mode:=202]                                                                           ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o=="RV" & perno>1,mode:=203]                                                                                            ## -- auto/van/truck passenger
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("DROVE PRIVATE VEHICLE","DROVE","DRIVE MY CAR","DROVE SELF","CAR",
                                "GOLF CART","GARBAGE TRUCK","TRACKTOR","TACKTOR") & party==1, mode:=202]               ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("DROVE"),mode:=202]                                                                                             ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("CAR"),mode:=202]                                                                                                  ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("NEIGHBOR DROVE ME.","FRIEND"),mode:=203]    ## -- auto/van/truck passenger
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("WENT FOR A RUN"),mode:=101]                                             ## -- walk 


# Read cmap codebook since it's in excel format
codebook = readxl::read_xlsx(file.path(cmap_dir, 'data_dictionary.xlsx'), sheet = 'Value Lookup')
setDT(codebook)
codebook[, NAME := tolower(NAME)]

# Read zones file

# location file has 2017 zones. join to 2009 zones to use existing skims file in visualization.

zones09 = st_read(file.path(settings$zone_dir, 'Zone09_CMAP_2009.shp'))
zones09 = st_transform(zones09, st_crs("+proj=longlat +ellps=GRS80"))

# Checking Value Counts
#----------------------------
print("Household size value counts: ")
hh_raw[, .N, hhsize]

print("Age value counts:")
per_raw[, .N, age][order(-N)]
per_raw[codebook[NAME == 'aage', .(VALUE = as.integer(VALUE), LABEL)], on = .(aage = VALUE)] %>% 
  .[, .N, .(LABEL, aage)] %>%
  .[order(aage)]

print("Trip Purpose value counts:")
place_raw[codebook[NAME == 'tpurp' & TABLE == 'PLACE', .(VALUE = as.integer(VALUE), LABEL)], on = .(tpurp = VALUE)] %>%
  .[, .N, by = .(tpurp, LABEL)] %>% 
  .[order(tpurp)]



# Processing HH
#---------------------------

# codebook[NAME == 'hhinc']

hh_raw[, HH_INC_CAT := fcase(hhinc %in% c(1:3), 1,  # 0-30k
                             hhinc2 == 1, 1,  # 0-30k
                             hhinc %in% c(4:6), 2, # 30-60k
                             hhinc2 == 2, 2, # 30-60k
                             hhinc %in% c(7:8), 3,  # 60-99k
                             hhinc2 == 3, 3,  # 60-99k
                             hhinc == 9, 4, # 100-150k
                             hhinc2 == 4, 4, # 75-100k
                             hhinc == 10, 5, # 150k plus
                             hhinc == 5, 5, # 150k plus
                             default = -9)
       ]  


# Add num workers
hh_raw[, HH_WORKERS := 0]
hh_raw[per_raw[wkstat == 0 | emply_ask == 1, .N, .(sampno)],  HH_WORKERS := i.N, on = .(sampno)]

# add num drivers
hh_raw[, HH_DRIVERS := 0]
hh_raw[per_raw[lic == 1, .N, .(sampno)],  HH_DRIVERS := i.N, on = .(sampno)]

# join hh loc zone

hh_raw[location_raw[loctype == 1], `:=` (HH_ZONE_ID = zone17,
                                          home_lat = latitude,
                                          home_lon = longitude,
                                         home_tract_fips = paste0(state_fips, str_pad(county_fips, width = 3, pad = '0'),
                                                                  str_pad(tract_fips, width = 6, pad = '0'))), on = .(sampno)]
# geocode 09 zone 
home_coords = st_as_sf(hh_raw[, .(sampno, home_lon, home_lat)], 
                      coords = c(x = "home_lon", y = "home_lat"), 
                      crs = st_crs(zones09))

home_zones_09 = setDT(st_join(home_coords, zones09))

rows = nrow(hh_raw)
hh_raw = hh_raw[home_zones_09[, .(sampno, zone09, ZONE07)], on = .(sampno)]

stopifnot('bad join to geocoded 09 zones!' = rows == nrow(hh_raw))


na_zones_17 = unique(hh_raw[is.na(zone09) & HH_ZONE_ID != -9999,HH_ZONE_ID])
lookup = hh_raw[HH_ZONE_ID %in% na_zones_17 & !is.na(zone09), .N, .(zone09, HH_ZONE_ID)]

hh_raw[lookup, zone09 := ifelse(is.na(zone09), i.zone09, zone09), on = .(HH_ZONE_ID)]

# Add counts of EV, hybrids per HH
# codebook[NAME == 'fuel'

hh_raw[, `:=` (numveh_ev = 0, numveh_hybrid = 0)]
hh_raw[vehicle_raw[fuel == 4, .N, .(sampno)], numveh_ev := i.N, on = .(sampno)]
hh_raw[vehicle_raw[fuel == 3, .N, .(sampno)], numveh_hybrid := i.N, on = .(sampno)]

HH = hh_raw[, .(SAMPN = sampno,
                HH_DOW = travday,
                HH_SIZE = hhsize,
                HH_SIZE_CAT = fifelse(hhsize >= 5, 5, hhsize),
                HH_VEH = hhveh,
                HH_VEH_CAT = fifelse(hhveh >= 4, 4, hhveh),
                HH_VEH_EV = numveh_ev,
                HH_VEH_HYBRID = numveh_hybrid,
                HH_INC_CAT,
                HH_WORKERS,
                HH_WORKERS_CAT = fifelse(HH_WORKERS >= 3, 3, HH_WORKERS),
                HH_DRIVERS,
                HH_DRIVERS_CAT = fifelse(HH_DRIVERS >= 3, 3, HH_DRIVERS),
                HHEXPFAC = wthhfin, 
                HH_UNO = 1,  
                HH_ZONE_ID = zone09,
                HH_ZONE_17 = HH_ZONE_ID,
                HH_TRACT_FIPS = home_tract_fips,
                AREA = 0)]
  


# Processing PER
#---------------------------

# Person Variables
#------------------


per_raw[, AGE_CAT := fcase(age < 5 | aage == 1, 1, # <5
                       age %in% c(5:15) | aage %in% c(2:3), 2, # 5-15
                       age %in% c(16:17) | aage == 4, 3, # 16-17
                       age %in% c(18:24), 4, # 18-24
                       age %in% c(25:44) | aage == 5, 5, # 25-44
                       age %in% c(45:64) | aage == 6, 6, # 25-44
                       age >= 65 | aage == 7, 7, # 25-44,
                       default = -9)]
                       

per_raw[, SCHOL := fcase(stude == 3 & (age %in% c(0:5) | aage == 1), 1, # not a student and age <=5: nanny/babysitter, 
                         schol == 2 & (age %in% c(0:6) | aage == 1), 2, # nursery/preschool, <age 6y
                         schol == 1 & (age %in% c(0:6) | aage == 1) , 1, # daycare
                         schol == 3, 3, # k-8
                         (age %in% c(7:12) | aage %in% c(2:3)) & schol %in% c(1, 2), 3, # k-8 if school is daycare/preschool but are 7-12
                         (age %in% c(6:12) | aage %in% c(2:3)) & schol < 0, 3, # k-8 if missing schol but age 6-12
                         schol == 4, 4, # 9-12
                         (age %in% c(13:18) | aage %in% c(3:4)) & schol %in% c(1, 2), 4, #9-12
                         schol == 5, 5, #vocational/technical
                         schol == 6, 6, #  2 yr
                         schol == 7, 7, #  4 yr
                         schol == 8, 8, #  grad
                         schol == 97, 97, #  other
                         schol %in% c(-7, -8, 1, 2) & (age >= 18 | aage > 4), 7, # 4 year if missing or daycare/preschool and 18+
                         default = -9 # None/NA
                         )]

per_raw[, PER_EMPLY_LOC_TYPE := fcase(wplace == 1, 1, # one work location
                                      wplace == 2, 2, # wfh
                                      wplace == 3, 3, # wplace varies
                                      wplace %in% c(-7, -8), -9, # refuse/dk
                                      default = 0)]

per_raw[, EMPLY := fcase(emply_ask == 1 & wrkhrs <= 35, 2,
                         emply_ask == 1, 1, # yes
                         emply_ask == 2, 3, # no
                         default = 3 # unemployed
                         )]

per_raw[, STUDE := fcase(stude == 1, 1, # yes (ft)
                         stude == 2, 1, # yes (pt)
                         stude == 3, 0, # no
                         stude < 0 & age < 17, 1, # student if 16 or younger and no answer
                         default = -9 # not answered
)]

per_raw[location_raw[loctype == 3], `:=` (PER_SCHL_ZONE_ID = zone17,
                                          school_lat = latitude,
                                          school_lon = longitude), on = .(perno, sampno)]
per_raw[location_raw[loctype == 2], `:=` (PER_WK_ZONE_ID = zone17, 
                                          work_lat = latitude,
                                          work_lon = longitude), on = .(perno, sampno)]

# geocode 09 zone 
school_coords = st_as_sf(per_raw[!is.na(school_lat), .(sampno, perno, school_lon, school_lat)], 
                       coords = c(x = "school_lon", y = "school_lat"), 
                       crs = st_crs(zones09))

school_zones_09 = setDT(st_join(school_coords, zones09))

rows = nrow(per_raw)
per_raw = school_zones_09[, .(sampno, perno, school_zone_09 = zone09, school_zone_07 = ZONE07)][per_raw, on = .(sampno, perno)]

stopifnot('bad join to geocoded school 09 zones!' = rows == nrow(per_raw))


na_zones_17 = unique(per_raw[is.na(school_zone_09) & PER_SCHL_ZONE_ID != -9999, PER_SCHL_ZONE_ID])
lookup = per_raw[PER_SCHL_ZONE_ID %in% na_zones_17 & !is.na(school_zone_09), .N, .(school_zone_09, PER_SCHL_ZONE_ID)]

per_raw[lookup, school_zone_09 := ifelse(is.na(school_zone_09), i.school_zone_09, school_zone_09), on = .(PER_SCHL_ZONE_ID)]
per_raw[is.na(school_zone_09) & !is.na(school_lat), school_zone_09 := -9999]


work_coords = st_as_sf(per_raw[!is.na(work_lat), .(sampno, perno, work_lon, work_lat)], 
                         coords = c(x = "work_lon", y = "work_lat"), 
                         crs = st_crs(zones09))

work_zones_09 = setDT(st_join(work_coords, zones09))

rows = nrow(per_raw)
per_raw = work_zones_09[, .(sampno, perno, work_zone_09 = zone09, work_zone_07 = ZONE07)][per_raw, 
                  on = .(sampno, perno)]

stopifnot('bad join to geocoded school 09 zones!' = rows == nrow(per_raw))


na_zones_17 = unique(per_raw[is.na(work_zone_09) & PER_WK_ZONE_ID != -9999, PER_WK_ZONE_ID])
lookup = per_raw[PER_WK_ZONE_ID %in% na_zones_17 & !is.na(work_zone_09), .N,
                 .(work_zone_09, PER_WK_ZONE_ID)]

per_raw[lookup, work_zone_09 := ifelse(is.na(work_zone_09), i.work_zone_09, work_zone_09), 
        on = .(PER_WK_ZONE_ID)]
per_raw[is.na(work_zone_09) & !is.na(work_lat), work_zone_09 := -9999]


PER = per_raw[, .(PERNO = perno,
                  SAMPN = sampno,
                  PER_WEIGHT = wtperfin,
                  AGE_CAT,
                  PER_GENDER = sex,  # 1 is male, 2 is female; does SPA care?
                  PER_EMPLY_LOC_TYPE,
                  EMPLY,
                  STUDE,
                  SCHOL,
                  PER_WFH = fifelse(PER_EMPLY_LOC_TYPE == 2, 1, 0),
                  PER_SCHL_ZONE_ID = school_zone_09,
                  PER_WK_ZONE_ID = work_zone_09,
                  PER_SCHL_ZONE_17 = PER_SCHL_ZONE_ID,
                  PER_WK_ZONE_17 = PER_WK_ZONE_ID,
                  PEREXPFAC = wtperfin
                  )]


# print(str(PER))

PER = PER[order(SAMPN, PERNO)]

PER[is.na(PER)] = 0

# Processing PLACE
#---------------------------

place_raw = place_raw[order(sampno, perno, placeno)]

# Assign depart/arrive hour and minute
place_raw[, deptime_hhmm := as_datetime(deptime)]
place_raw[, arrtime_hhmm := as_datetime(arrtime)]
place_raw[, `:=` (DEP_HR = hour(deptime_hhmm),
                  DEP_MIN = minute(deptime_hhmm),
                  ARR_HR = hour(arrtime_hhmm),
                  ARR_MIN = minute(arrtime_hhmm))]

place_raw[DEP_HR == 3 & DEP_MIN == 0, `:=` (DEP_HR = 2, DEP_MIN = 59)]

place_raw = place_raw[order(sampno, perno, placeno)]


# Derive trips for kids < 5

under_5_per = per_raw[age < 5 | aage == 1]

under_5_per = under_5_per[!place_raw, on = .(sampno, perno)]
# under_5_per[, .(min(perno), max(perno))]
under_5_record = data.table()
for(per_num in 1:11){
  field = paste0('perno_', per_num)
  place_raw[, perno_join := get(field)]
  
  new_records = under_5_per[, .(sampno, kid_perno = perno)][place_raw, on = .(sampno, kid_perno == perno_join), nomatch = 0]
  
  under_5_record = rbindlist(list(under_5_record, new_records), fill = TRUE, use.names = TRUE)
}

under_5_record = under_5_record[order(sampno, kid_perno, deptime_hhmm, arrtime_hhmm)]

under_5_record[, prev_deptime := shift(deptime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
under_5_record[, next_deptime := shift(deptime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
under_5_record[, prev_arrtime := shift(arrtime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
under_5_record[, next_arrtime := shift(arrtime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
# under_5_record[prev_deptime > arrtime_hhmm, .(perno, kid_perno, sampno, arrtime_hhmm, deptime_hhmm, prev_arrtime, prev_deptime)]

n_overlaps = under_5_record[prev_deptime > arrtime_hhmm, .N]
init_overlaps = 0
iter = 1
# Delete straight overlaps

cat('removing <5 kid trip overlaps - straight overlaps')

while(n_overlaps != init_overlaps) {
  cat('iteration ', iter, '\n')
  n_overlaps = under_5_record[prev_deptime > arrtime_hhmm, .N]
  init_overlaps = n_overlaps  
  under_5_record[prev_arrtime == arrtime_hhmm & prev_deptime == deptime_hhmm, delete := 1]
  under_5_record = under_5_record[is.na(delete)]
  under_5_record = under_5_record[order(sampno, kid_perno, deptime_hhmm, arrtime_hhmm)]
  under_5_record[, prev_deptime := shift(deptime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
  under_5_record[, next_deptime := shift(deptime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
  under_5_record[, prev_arrtime := shift(arrtime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
  under_5_record[, next_arrtime := shift(arrtime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
  
  n_overlaps = under_5_record[prev_deptime > arrtime_hhmm, .N]
  iter = iter + 1
}

cat('removing <5 kid trip overlaps - keep lower reporter person num')
n_overlaps = under_5_record[prev_deptime > arrtime_hhmm, .N]
init_overlaps = 0
iter = 1
while(n_overlaps != init_overlaps) {
  cat('iteration ', iter, '\n')
  n_overlaps = under_5_record[prev_deptime > arrtime_hhmm, .N]
  under_5_record[, prev_perno := shift(perno, type = 'lag'), by = .(sampno, kid_perno)]
  init_overlaps = n_overlaps  
  under_5_record[prev_deptime > arrtime_hhmm & prev_perno < perno, delete := 1]
  under_5_record = under_5_record[is.na(delete)]
  under_5_record = under_5_record[order(sampno, kid_perno, deptime_hhmm, arrtime_hhmm)]
  under_5_record[, prev_deptime := shift(deptime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
  under_5_record[, next_deptime := shift(deptime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
  under_5_record[, prev_arrtime := shift(arrtime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
  under_5_record[, next_arrtime := shift(arrtime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
  n_overlaps = under_5_record[prev_deptime > arrtime_hhmm, .N]
  iter = iter + 1
}

cat('removing <5 kid trip overlaps - keep lower reporter person num; reverse data table order')
n_overlaps = under_5_record[deptime_hhmm > next_arrtime, .N]

init_overlaps = 0
iter = 1
while(n_overlaps != init_overlaps) {
  cat('iteration ', iter, '\n')
  n_overlaps = under_5_record[deptime_hhmm > next_arrtime, .N]
  under_5_record[, next_perno := shift(perno, type = 'lead'), by = .(sampno, kid_perno)]
  init_overlaps = n_overlaps  
  under_5_record[deptime_hhmm > next_arrtime & perno > next_perno, delete := 1]
  under_5_record = under_5_record[is.na(delete)]
  under_5_record[, prev_deptime := shift(deptime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
  under_5_record[, next_deptime := shift(deptime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
  under_5_record[, prev_arrtime := shift(arrtime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
  under_5_record[, next_arrtime := shift(arrtime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
  n_overlaps = under_5_record[deptime_hhmm > next_arrtime, .N]
  iter = iter + 1
}

# these trips -- looks like per 1 reported the kid on a work trip while per 2 reported kid on errands throughout the day
under_5_record = under_5_record[!(sampno == 70003763          & kid_perno == 3 & perno == 1)]
under_5_record = under_5_record[!(sampno == 70003763          & kid_perno == 4 & perno == 1)]
under_5_record = under_5_record[!(sampno == 70021636          & kid_perno == 4 & perno == 1)]
under_5_record[, prev_deptime := shift(deptime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
under_5_record[, next_deptime := shift(deptime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]
under_5_record[, prev_arrtime := shift(arrtime_hhmm, type = 'lag'), by = .(sampno, kid_perno)]
under_5_record[, next_arrtime := shift(arrtime_hhmm, type = 'lead'), by = .(sampno, kid_perno)]

stopifnot(under_5_record[deptime_hhmm > next_arrtime, .N] == 0)
stopifnot(under_5_record[prev_deptime > arrtime_hhmm, .N] == 0)

under_5_record[, placeno := seq_len(.N), by = .(sampno, kid_perno)]
under_5_record[, perno := kid_perno]

# re-code any purposes? ask on call


place_raw = rbindlist(list(place_raw, under_5_record), use.names = TRUE, fill = TRUE)

#   
# no one < 5 in trips table. add empty record
under_5_record_missing = per_raw[!place_raw, on = .(sampno, perno),
                         .(sampno,
                           perno,
                          placeno = 1,
                          locno = 10000,
                          travtime = 0,
                          actdur = 1440,
                          distance = -1,
                          mode = -1,
                          tpurp = 1,
                          perno_1 = -1,
                          perno_2 = -1,
                          perno_3 = -1,
                          perno_4 = -1,
                          perno_5 = -1,
                          perno_6 = -1,
                          perno_7 = -1,
                          perno_8 = -1,
                          perno_9 = -1,
                          perno_10 = -1,
                          perno_11 = -1,
                          perno_12 = -1,
                          hhcount = -1,
                          nonhhcount = -1,
                          hhparty = -1,
                          party = -1,
                          transitPlaceno = -9,
                          study,
                          DEP_HR = 2,
                          DEP_MIN = 59,
                          ARR_HR = 3,
                          ARR_MIN = 0
                         )]

place_raw = rbindlist(list(place_raw, under_5_record_missing), use.names = TRUE, fill = TRUE)

place_raw[per_raw, SCHOL := i.SCHOL, on = .(sampno, perno)]
place_raw[per_raw, STUDE := i.STUDE, on = .(sampno, perno)]
place_raw[per_raw, age := i.age, on = .(sampno, perno)]

# label some data to help review
place_raw[codebook[TABLE == 'PLACE' & NAME == 'tpurp', .(VALUE = as.integer(VALUE), LABEL)], tpurp_labeled := LABEL, on = .(tpurp = VALUE)]

place_raw[codebook[TABLE == 'PLACE' & NAME == 'mode', .(VALUE = as.integer(VALUE), LABEL)], mode_labeled := LABEL, on = .(mode = VALUE)]

# check out transit
# place_raw[transit_raw, on = .(sampno, perno, placeno), .(mode_labeled, i.mode, placeno, tpurp_labeled)]
# place_raw[transit_raw, on = .(sampno, perno, placeno)][, .N, .(sampno, perno, placeno, tpurp_labeled)][N<3]

# place_raw[tpurp == 28, .N, .(mode_labeled)]
# place_raw[tpurp == 28 & mode %in% c(505:509)] # some transit change mode that will get linked

# get access mode from transit file
transit_raw[, is_last_trip := fifelse(transitno == max(transitno), 1, 0), by = .(sampno, perno, placeno)]
transit_raw[, is_first_trip := fifelse(transitno == 1, 1, 0), by = .(sampno, perno, placeno)]

place_raw[transit_raw[is_first_trip == 1], access_mode := i.mode, on = .(sampno, perno, placeno)]
place_raw[transit_raw[is_last_trip == 1], egress_mode := i.mode, on = .(sampno, perno, placeno)]

# place_raw[, .N, access_mode]
# place_raw[, .N, egress_mode]

place_raw[!is.na(access_mode), access_mode_recode := fcase(access_mode == 'WALKING', 'WALK',
                                        access_mode == 'BICYCLING', 'PNR',
                                        access_mode == 'TAXI / UBER / LYFT', 'TNR',
                                        access_mode %in% c('KISS AND RIDE', 'PRIVATE SHUTTLE OR SOMETHING ELSE'), 'KNR',
                                        access_mode == 'PARK AND RIDE', 'PNR',
                                        default = 'WALK')]

place_raw[!is.na(egress_mode), egress_mode_recode := fcase(egress_mode == 'WALKING', 'WALK',
                                        egress_mode == 'BICYCLING', 'PNR',
                                        egress_mode == 'TAXI / UBER / LYFT', 'TNR',
                                        egress_mode %in% c('KISS AND RIDE', 'PRIVATE SHUTTLE OR SOMETHING ELSE'), 'KNR',
                                        egress_mode == 'PARK AND RIDE', 'PNR',
                                        default = 'WALK')]

place_raw[, grouped_access_egress := fcase(egress_mode_recode == 'PNR' | access_mode_recode == 'PNR', 'PNR',
                                          egress_mode_recode == 'KNR' | access_mode_recode == 'KNR', 'KNR',
                                          egress_mode_recode == 'TNR' | access_mode_recode == 'TNR', 'TNR',
                                          egress_mode_recode == 'WALK' | access_mode_recode == 'WALK', 'WALK'
)]

# Join edited purposes
tpurp_edit = rbindlist(list(purpose_edits, purpose_edits_nirpc), fill = TRUE, use.names = TRUE)

place_raw[, tpurp_orig := tpurp]
place_raw[tpurp_edit, tpurp := new_tpurp, on = .(sampno, perno, placeno, traveldayno, locno)]

place_raw[, mode_orig := mode]
place_raw[mode_edits, mode := i.new_mode, on = .(sampno, perno, placeno, traveldayno, locno)]


# add some fields for review/purpose and mode assignment
place_raw[, o_purpose := data.table::shift(tpurp, type = 'lag'), by = .(sampno, perno)]
place_raw[, prev_mode := data.table::shift(mode, type = 'lag'), by = .(sampno, perno)]
place_raw[, next_mode := data.table::shift(mode, type = 'lead'), by = .(sampno, perno)]
place_raw[, prev_party := data.table::shift(party, type = 'lag'), by = .(sampno, perno)]
place_raw[, prev_hhparty := data.table::shift(hhparty, type = 'lag'), by = .(sampno, perno)]
place_raw[, next_party := data.table::shift(party, type = 'lead'), by = .(sampno, perno)]
place_raw[, next_hhparty := data.table::shift(hhparty, type = 'lead'), by = .(sampno, perno)]

place_raw[per_raw, age := i.age, on = .(sampno, perno)]
place_raw[per_raw, lic := i.lic, on = .(sampno, perno)]


# get zone 09 id
place_raw[location_raw, `:=` (lat = latitude, lon = longitude,
                              state_fips = i.state_fips,
                              county_fips = i.county_fips,
                              locname = i.locname
), on = .(sampno, locno)]
place_raw[location_raw[loctype  == 1], `:=` (home_lat = latitude, home_lon = longitude), on = .(sampno)]


# location info 
place_raw[location_raw, PLACE_ZONE_ID := zone17, on = .(sampno, locno)]
place_raw[location_raw, PLACE_NAME := locname, on = .(sampno, locno)]


place_coords = st_as_sf(location_raw[, .(sampno, locno, lon = longitude, lat = latitude)], 
                       coords = c(x = "lon", y = "lat"), 
                       crs = st_crs(zones09))

place_zones_09 = setDT(st_join(place_coords, zones09))

rows = nrow(place_raw)
place_raw = place_zones_09[, .(sampno, locno, zone_09 = zone09, zone_07 = ZONE07)][place_raw, 
                                                                                          on = .(sampno, locno)]

stopifnot('bad join to geocoded  09 zones!' = rows == nrow(place_raw))


na_zones_17 = unique(place_raw[is.na(zone_09) & PLACE_ZONE_ID != -9999, PLACE_ZONE_ID])
lookup = place_raw[PLACE_ZONE_ID %in% na_zones_17 & !is.na(zone_09), .N,
                 .(zone_09, PLACE_ZONE_ID)]
place_raw[lookup, zone_09 := ifelse(is.na(zone_09), i.zone_09, zone_09), 
        on = .(PLACE_ZONE_ID)]
place_raw[is.na(zone_09), zone_09 := -9999]

#investigate change mode purposes
union_coords = c(-87.64058666867223, 41.8831654811658) # union st
union2_coords = c(-87.64039232127566, 41.87867932294234) # union st 2
lasalle_coords = c(-87.63213647435764, 41.8753252459755)
palatine_coords = c(-88.04760361480889, 42.11226850272472)
ourbus_coords = c(-87.63950500436, 41.87725652917673)
ohare_coords = c(-87.90350705286936, 41.978477543855256) # ohare
ohare_park_coords = c(-87.88103510321113, 41.97750700656876)
midway_coords = c(-87.74175590136355, 41.788342279685494)
midway_coords_2 = c( -87.75237013661477, 41.786922859930144)
greyhound_coords = c(-88.20874, 41.77792 )
greyhound_coords_2 = c(-87.64311951666464, 41.87486439683558)


place_raw[, close_to_train_station := fcase(get_distance_meters(union_coords, c(lon, lat)) <= 200, 1,
                                           get_distance_meters(union2_coords, c(lon, lat)) <= 200, 1,
                                           get_distance_meters(lasalle_coords, c(lon, lat)) <= 200, 1,
                                           get_distance_meters(greyhound_coords, c(lon, lat)) <= 200, 1,
                                           get_distance_meters(greyhound_coords_2, c(lon, lat)) <= 200, 1,
                                           get_distance_meters(ourbus_coords, c(lon, lat)) <= 200, 1,
                                           get_distance_meters(palatine_coords, c(lon, lat)) <= 200, 1,
                                           default = 0
)]


place_raw[, close_to_airport := fcase(get_distance_meters(ohare_coords, c(lon, lat)) <= 800, 1,
                                     get_distance_meters(ohare_park_coords, c(lon, lat)) <= 800, 1,
                                     get_distance_meters(midway_coords, c(lon, lat)) <= 500, 1,
                                     get_distance_meters(midway_coords_2, c(lon, lat)) <= 500, 1,
                                     default = 0
)]


# review loop trips
place_raw[, HOME_DIST := get_distance_meters(c(home_lon, home_lat), c(lon, lat))  * 0.000621371]

place_raw[, orig_tpurp := shift(tpurp, type = 'lag'), by = .(sampno, perno)]
place_raw[, orig_name := shift(PLACE_NAME, type = 'lag'), by = .(sampno, perno)]
place_raw[, `:=` (o_lat = shift(lat, type = 'lag'),
                  o_lon = shift(lon, type = 'lag')), by = .(sampno, perno)]

place_raw[, PURPOSE := fcase(tpurp %in% c(1, 2) & HOME_DIST > 0.2 & PLACE_NAME %like% 'HOUSE', 8, # social if not at home & name is house
                             tpurp %in% c(1, 2) & HOME_DIST > 0.2 & PLACE_NAME %like% 'S HOME', 8, # social if not at home & name is 'friend's home' etc
                             tpurp %in% c(1, 2) & HOME_DIST > 0.2 & PLACE_NAME %like% 'S APARTMENT', 8, # social if not at home & name is 'friend's apartment' etc
                             tpurp %in% c(1, 2) & HOME_DIST > 0.2 & PLACE_NAME %like% 'STORE', 5, # shop if not at home & name is store
                             tpurp %in% c(1, 2) & HOME_DIST > 0.2, 6, # otherwise maintenance if not at home and says home purpose
                                      tpurp %in% c(1, 2), 0, # home + wfh
                                      tpurp == 3 , 1, # work (fixed)
                                      tpurp %in% c(6) & SCHOL %in% c(6:8), 2, # university
                                      tpurp %in% c(6) & SCHOL == -9 & age %in% c(18:24), 2, # university
                                      tpurp %in% c(6) & SCHOL %in% c(1:5, 97), 3, # school
                                      tpurp %in% c(6) & SCHOL == -9 & age <18, 3, # school
                                      tpurp %in% c(6) & SCHOL == -9 & age > 24 &
                                          (next_hhparty != hhparty) & (DEP_HR %in% c(7:9) | DEP_HR %in% c(2:4)), 4, # escort if party size changed & around school time
                                      tpurp %in% c(6) & SCHOL == -9 & age > 24, 6, # other maint
                                      tpurp %in% c(26), 4, # escort
                                      tpurp %in% c(8, 9), 5, # shop
                                      tpurp %in% c(10:15, 27, 4), 6, # other maint
                                      tpurp %in% c(18, 19), 8, # social/visit
                                      tpurp %in% c(16, 17), 7, # eat out
                                      tpurp %in% c(20:25, 7, 97), 9, # other disc
                                      tpurp == 5, 6, # work related --> other maint
                                      tpurp %in% c(28) & 
                                          (mode %in% c(500:509) | next_mode %in% c(500:509)) &
                                          actdur <= 60, 12, # change mode if transit/low dwell
                                      tpurp == 28 & get_distance_meters(c(lon, lat), c(home_lon, home_lat)) < 100, 0,
                                      tpurp == 28, 12, # otherwise leave as change mode...
                             
                             default = -9 
                                     )]
place_raw[, .N, PURPOSE][order(PURPOSE)]


# Assign SPA mode value to each column

# model modes:
SOV = 1L
HOV2	= 2L
HOV3	= 3L
WALK =	4L
BIKE	= 5L
WALK_TRANSIT	= 6L
KNR_TRANSIT	= 7L
PNR_TRANSIT	= 8L
TNC_TRANSIT	= 9L
TAXI	= 10L
TNC_REG	= 11L
TNC_POOL = 12L
SCHOOLBUS =	13L
TRANSIT = 100L
OTHER = 14L


# investigate non-students going to school


# place_raw[STUDE == 0 & tpurp == 6 & age >= 18, .(SCHOL, o_purpose, party, prev_party, next_party, mode_labeled, next_mode, arrtime, deptime)] %>%
#   .[codebook[TABLE == 'PLACE' & NAME == 'tpurp', .(opurp_labeled = LABEL, VALUE = as.integer(VALUE))], on = .(o_purpose = VALUE), nomatch = 0] %>%
#   .[codebook[TABLE == 'PLACE' & NAME == 'mode', .(next_mode_labeled = LABEL, VALUE = as.integer(VALUE))], on = .( next_mode = VALUE), nomatch = 0]
# 
# place_raw[STUDE == 0 & tpurp == 6 & age >= 18 & hhparty != prev_hhparty, .N]
# place_raw[STUDE == 0 & tpurp == 6 & age >= 18 & hhparty != next_hhparty & hhparty == prev_hhparty, .N]
# # 219 of 306 might be pu/do
# 
# place_raw[STUDE == 0 & tpurp == 6 & age >= 18 & hhparty == prev_hhparty & hhparty == next_hhparty, .(mode_labeled, arrtime, deptime, party, prev_party, next_party)]

## check change mode/lack thereof


place_raw[, MODE := fcase( mode %in% c(201:202, 301) & (party == 1 | party < 0), SOV, # sov if party is 1 or unknown
                           mode %in% c(201), SOV, # motorcycle sov no matter party
                           mode %in% c(202:203, 301) & (party == 2 | party < 0), HOV2,
                           mode %in% c(202:203, 301) & party >= 3, HOV3, # hov3
                           mode %in% c(202:203, 301) & lic == 2 & party == 1, HOV2, # hov2 if one-party passenger not licensed
                           mode %in% c(202:203, 301) & lic == 1 & party == 1, SOV, # sov if one-party,
                           mode == 101, WALK, 
                           mode %in% c(102:104), BIKE,
                           mode %in% c(701:703, 601), TAXI,
                           mode %in% c(704), TNC_REG,
                           mode %in% c(705), TNC_POOL,
                           mode == 401, SCHOOLBUS,
                           mode %in% c(502:504, # paratransit
                                       501, # bus
                                       500, # rail and bus
                                       505:509) & grouped_access_egress =='TNR', TNC_TRANSIT,
                           mode %in% c(502:504, # paratransit
                                       501, # bus
                                       500, # rail and bus
                                       505:509) & grouped_access_egress =='PNR', PNR_TRANSIT, 
                           mode %in% c(502:504, # paratransit
                                       501, # bus
                                       500, # rail and bus
                                       505:509) & grouped_access_egress =='KNR', KNR_TRANSIT, 
                           mode %in% c(502:504, # paratransit
                                       501, # bus
                                       500, # rail and bus
                                       505:509) & grouped_access_egress =='WALK', WALK_TRANSIT, 
                           mode %in% c(502:504, # paratransit
                                       501, # bus
                                       500, # rail and bus
                                       505:509) & is.na(grouped_access_egress), TRANSIT, 
                           mode < 0, -9L, # missing/drop from dataset
                           mode == 2, -9L, # missing/drop from dataset
                           default = OTHER)] # other
  
place_raw[, driver := fifelse(mode %in% c(201:202), 1, 0)]
place_raw[, driver := fifelse(MODE == SOV, 1, driver)]

# place_raw[, .N, MODE][order(MODE)]



# tottr - next 

place_raw[, TOTTR_NEXT := shift(party, n = 1L, type = 'lead'), by = .(sampno, perno)]

place_raw[is.na(TOTTR_NEXT), TOTTR_NEXT := 0]

PLACE = place_raw[MODE != -9 | placeno == 1, .(SAMPN = sampno,
                      PLANO = placeno,
                      PERNO = perno,
                      TAZ = zone_09,
                      TAZ17 = PLACE_ZONE_ID,
                      DEP_HR,
                      DEP_MIN,
                      ARR_HR,
                      ARR_MIN,
                      TPURP  = PURPOSE,
                      SURVEY_PURPOSE = tpurp,
                      PNAME = PLACE_NAME,
                      DRIVER = driver,
                      TOTTR = party,
                      TOTTR_NEXT,
                      MODE, 
                      SURVEY_MODE = mode,
                      ACCESS_MODE = grouped_access_egress,
                      PAY_TOLL =  fifelse(fcoalesce(tollways_paid, -1L) >= 1, 1, 0),
                      PER1 = fifelse(perno_1 > 0, 1, 0),
                      PER2 = fifelse(perno_2 > 0, 1, 0),
                      PER3 = fifelse(perno_3 > 0, 1, 0),
                      PER4 = fifelse(perno_4 > 0, 1, 0),
                      PER5 = fifelse(perno_5 > 0, 1, 0),
                      PER6 = fifelse(perno_6 > 0, 1, 0),
                      PER7 = fifelse(perno_7 > 0, 1, 0),
                      PER8 = fifelse(perno_8 > 0, 1, 0),
                      PER9 = fifelse(perno_9 > 0, 1, 0),
                      PER10 = fifelse(perno_10 > 0, 1, 0),
                      PER11 = fifelse(perno_11 > 0, 1, 0),
                      PER12 = fifelse(perno_12 > 0, 1, 0),
                      HHMEM = hhparty,
                      XCORD = lon,
                      YCORD = lat,
                      HOME_DIST,
                      IS_TRIP = fifelse(placeno == 1, 0, 1)
)]




#=========================================================================================================================
# CREATE INPUTS FOR Python SPA
#=========================================================================================================================


HH[is.na(HH)] = -9
PER[is.na(PER)] = -9
PLACE[is.na(PLACE)] = -9

str(HH)
str(PER)
str(PLACE)


# Checking to see if every person in PER file is in PLACE file:
# -----------------
num_persons_in_per = nrow(unique(PER[, .(SAMPN, PERNO)]))
paste0("Distinct people in PER file: ", num_persons_in_per)
num_persons_in_PLACE = nrow(unique(PLACE[, .(SAMPN, PERNO)]))
paste0("Distict people in PLACE file: ", num_persons_in_PLACE)


# Writing Output
# -----------------

HH_TEST = HH[1:400]
PER_TEST = PER[SAMPN %in% HH_TEST[, SAMPN]]
PLACE_TEST = PLACE[SAMPN %in% HH_TEST[, SAMPN]]
write.csv(HH_TEST, file.path(output_dir, "HH_SPA_INPUT.csv"), row.names = F)
write.csv(PER_TEST, file.path(output_dir, "PER_SPA_INPUT.csv"), row.names = F)
write.csv(PLACE_TEST, file.path(output_dir, "PLACE_SPA_INPUT.csv"), row.names = F)
# 
# # 
write.csv(HH, file.path(output_dir, "HH_SPA_INPUT.csv"), row.names = F)
write.csv(PER, file.path(output_dir, "PER_SPA_INPUT.csv"), row.names = F)
write.csv(PLACE, file.path(output_dir, "PLACE_SPA_INPUT.csv"), row.names = F)

