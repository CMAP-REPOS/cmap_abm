# Preparing survey data for reweighting


library(data.table)
library(yaml)
library(readxl)
# load settings

# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')

# Combine data from CMAP main and NIRPC

data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
nirpc_dir = file.path(data_dir, settings$nirpc_folder)
output_dir = settings$SPA_input_dir

project_dir = settings$proj_dir
wt_dir = file.path(project_dir, "underreporting_correction")




# read in necessary data
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
place_raw[mode!=401 & (payf_o %like% "school bus" | payf_o %like% "public school"),mode:=401]
#
### --- "Other" mode --- ###
place_raw[study == 'nirpc' & mode==997 & mode_o=="RV" & perno==1,mode:=202]                                                                           ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o=="RV" & perno>1,mode:=203]                                                                                            ## -- auto/van/truck passenger
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("DROVE PRIVATE VEHICLE","DROVE",
                                                       "DRIVE MY CAR","DROVE SELF","CAR",
                                                       "GOLF CART","GARBAGE TRUCK","TRACKTOR","TACKTOR") & party==1,mode:=202]               ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("DROVE"),mode:=202]                                                                                             ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("CAR"),mode:=202]                                                                                                  ## -- auto/van/truck driver
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("NEIGHBOR DROVE ME.","FRIEND"),mode:=203]    ## -- auto/van/truck passenger
place_raw[study == 'nirpc' & mode==997 & mode_o %in% c("WENT FOR A RUN"),mode:=101]                                             ## -- walk 

tpurp_edit = rbindlist(list(purpose_edits, purpose_edits_nirpc), fill = TRUE, use.names = TRUE)

place_raw[, tpurp_orig := tpurp]
place_raw[tpurp_edit, tpurp := new_tpurp, on = .(sampno, perno, placeno, traveldayno, locno)]

place_raw[, mode_orig := mode]
place_raw[mode_edits, mode := i.new_mode, on = .(sampno, perno, placeno, traveldayno, locno)]

# CMAP target data prep

per_hh_data = hh_raw[per_raw, on = .(sampno)]


# assign puma cluster

cluster_9 = c('03700', '02601', '03601', '03009', '02400')
cluster_10 = c('03008','03007','03005', '03208','03417')
cluster_8 = c('03209', '03202', '03203','03207', '03205', '03204')
cluster_7 = c('03306', '03307', '03308','03309','03310','03602')
cluster_11 = c('03108', '03102' , '03105', '03106', '03107')
cluster_6 = c('03414', '03410', '03412','03413','03411')
cluster_5 = c('03422', '03407', '03409', '03408')
cluster_4 = c('03401', '03418', '03419', '03415', '03420', '03421', '03416')
cluster_3 = c('03527', '03528', '03531', '03532', '03529', '03530')
cluster_2 = c('03521', '03520', '03504', '03503', '03501')
cluster_1 = c('03525', '03524', '03523', '03502', '03522', '03526')



# puma_tract = fread('https://www2.census.gov/geo/docs/maps-data/data/rel/2010_Census_Tract_to_2010_PUMA.txt')

names(location_raw)
per_hh_data[location_raw[loctype == 1], puma_id := puma10, on = .(sampno)]
per_hh_data[, cluster := fcase(puma_id %in% as.numeric(cluster_1), 1,
                          puma_id %in%  as.numeric(cluster_2), 2,
                          puma_id %in%  as.numeric(cluster_3), 3,
                          puma_id %in%  as.numeric(cluster_4), 4, 
                          puma_id %in%  as.numeric(cluster_5), 5,
                          puma_id %in%  as.numeric(cluster_6), 6,
                          puma_id %in%  as.numeric(cluster_7), 7,
                          puma_id %in%  as.numeric(cluster_8), 8,
                          puma_id %in%  as.numeric(cluster_9), 9,
                          puma_id %in%  as.numeric(cluster_10), 10,
                          puma_id %in%  as.numeric(cluster_11), 11)]


# hhsize

per_hh_data[, h_size_1 := ifelse(hhsize == 1, 1, 0)]
per_hh_data[, h_size_2 := ifelse(hhsize == 2, 1, 0)]
per_hh_data[, h_size_3 := ifelse(hhsize == 3, 1, 0)]
per_hh_data[, h_size_4 := ifelse(hhsize == 4, 1, 0)]
per_hh_data[, h_size_5 := ifelse(hhsize >= 5, 1, 0)]

# income

imputed_income = readRDS(file.path(wt_dir, 'imputed_income.rds'))

per_hh_data[imputed_income, income_aggregate := i.income_imputed_aggregate, on = .(sampno)]
per_hh_data[income_aggregate == 5, income_aggregate := 4] # aggregated wrong before saving...

per_hh_data[, h_inc_35 := ifelse(income_aggregate == 1, 1, 0)]
per_hh_data[, h_inc_60 := ifelse(income_aggregate == 2, 1, 0)]
per_hh_data[, h_inc_100 := ifelse(income_aggregate == 3, 1, 0)]
per_hh_data[, h_inc_100p := ifelse(income_aggregate == 4, 1, 0)]

# hh workers

per_hh_data[, num_workers := sum(emply_ask == 1), by = .(sampno)]
per_hh_data[, .N, num_workers]

per_hh_data[, h_worker_1 := ifelse(num_workers == 1, 1, 0)]
per_hh_data[, h_worker_2 := ifelse(num_workers == 2, 1, 0)]
per_hh_data[, h_worker_3 := ifelse(num_workers >= 3, 1, 0)]
per_hh_data[, h_worker_0 := ifelse(num_workers == 0, 1, 0)]

# zero auto
per_hh_data[, autosuff := fcase(hhveh ==0, 0,
                                hhveh < num_workers, 1,
                                hhveh >= num_workers, 2)]


per_hh_data[, h_zero_veh := ifelse(autosuff == 0, 1, 0)]
per_hh_data[, h_veh_lt_wrk := ifelse(autosuff == 1, 1, 0)]
per_hh_data[, h_veh_gt_wrk := ifelse(autosuff == 2, 1, 0)]

# age
imputed_ages = readRDS(file.path(wt_dir, 'imputed_ages.rds'))

per_hh_data[imputed_ages, age_imputed := i.age_imputed, on = .(perno, sampno)]
per_hh_data[is.na(age_imputed), age_imputed := age]
set.seed(413)
per_hh_data[age_imputed == -7, age_imputed := sample(18:100, .N, replace = TRUE)]



per_hh_data[, p_age_lt_5 := ifelse(age_imputed < 5, 1, 0)]
per_hh_data[, p_age_5_14 := ifelse(age_imputed %in% 5:14, 1, 0)]
per_hh_data[, p_age_15_17 := ifelse(age_imputed %in% 15:17, 1, 0)]
per_hh_data[, p_age_18_34 := ifelse(age_imputed %in% 18:34, 1, 0)]
per_hh_data[, p_age_35_64 := ifelse(age_imputed %in% 35:64, 1, 0)]
per_hh_data[, p_age_gt_65 := ifelse(age_imputed >=65, 1, 0)]

# lifecycle
per_hh_data[, min_age := min(age_imputed), by = .(sampno) ]
per_hh_data[, max_age := max(age_imputed), by = .(sampno)]
per_hh_data[, num_adults := sum(age_imputed >= 18), by = .(sampno)]
per_hh_data[, num_kids := sum(age_imputed < 18), by = .(sampno)]

per_hh_data[, h_lifecycle_prek := fifelse(min_age < 6, 1, 0)]
per_hh_data[, h_lifecycle_k6_17 := fifelse(min_age %in% 6:17, 1, 0)]
per_hh_data[, h_lifecycle_ad1_lt35 := fifelse(min_age >= 18 & max_age <35 & hhsize == 1, 1, 0)]
per_hh_data[, h_lifecycle_ad1_35to64 := fifelse(min_age >= 18 & hhsize == 1 & max_age %in% c(35:64), 1, 0)]
per_hh_data[, h_lifecycle_ad1_gt64 := fifelse(min_age >= 18 & hhsize == 1 & max_age >=65, 1, 0)]
per_hh_data[, h_lifecycle_ad2_lt35 := fifelse(min_age >= 18 & max_age <35 & hhsize >= 2, 1, 0)]
per_hh_data[, h_lifecycle_ad2_35to64 := fifelse(min_age >= 18 & hhsize >= 2 & max_age %in% c(35:64), 1, 0)]
per_hh_data[, h_lifecycle_ad2_gt64 := fifelse(min_age >= 18 & hhsize >= 2 & max_age >=65, 1, 0)]


transit_raw[place_raw, distance := i.distance, on = .(sampno, perno, placeno)]

# transit ridership
transit_boardings = transit_raw[study == 'cmap', .(n_cta_bus = sum(agency_name == 'Chicago Transit Authority' & mode == 'BUS'),
                                    n_cta_rail = sum(agency_name == 'Chicago Transit Authority' & mode == 'SUBWAY'),
                                n_metra = sum(agency_name == 'Metra'),
                                n_pace = sum(agency_name == 'PACE')), 
                            .(sampno)]


# metra is too high compared to weighting memo...
# scale down?
metra_wt_memo = 300724
metra_wt_sum = sum(transit_raw[hh_raw, on = .(sampno)][agency_name == 'Metra', sum(wthhfin)])
metra_factor = metra_wt_memo/metra_wt_sum
transit_boardings[, n_metra := n_metra * metra_factor]
# everything else is pretty close to the memo

per_hh_data = transit_boardings[per_hh_data, on = .(sampno)]
per_hh_data[is.na(n_cta_bus), n_cta_bus := 0]
per_hh_data[is.na(n_cta_rail), n_cta_rail := 0]
per_hh_data[is.na(n_metra), n_metra  := 0]
per_hh_data[is.na(n_pace), n_pace  := 0]

# gender
imputed_sex = readRDS(file.path(wt_dir, 'imputed_sex.rds'))

per_hh_data[imputed_sex, imputed_sex := i.imputed_sex, on = .(perno, sampno)]
per_hh_data[is.na(imputed_sex), imputed_sex := sex]

per_hh_data[, p_male := 1 * (imputed_sex == 1)]
per_hh_data[, p_female := 1 * (imputed_sex == 2)]
per_hh_data[, p_total := 1]

# commute pattern
library(sf)
zones17 = st_read(file.path(settings$zone_dir, 'zones17.shp'))

zones17_dt = setDT(zones17)
  
location_raw[zones17_dt, cbd := i.cbd, on = .(zone17)]
location_raw[zones17_dt, chicago := chicago, on = .(zone17)]

location_raw[zone17 == 57, cbd := 1]
location_raw[zone17 %in% c(1140, 1182, 1141, 1159, 1170, 1169), chicago := 1]

per_hh_data[location_raw[loctype == 1], `:=` (home_cbd = i.cbd,
                                                home_chicago = i.chicago,
                                                home_county = i.county_fips), on = .(sampno)]

per_hh_data[location_raw[loctype == 2], `:=` (work_cbd = i.cbd,
                                                work_chicago = i.chicago,
                                                work_county = i.county_fips,
                                              work_out_region = i.out_region), on = .(sampno, perno)]


per_hh_data[is.na(work_cbd), work_cbd := -1]

per_hh_data[is.na(work_chicago), work_chicago := -1]
per_hh_data[is.na(work_county), work_county := -1]


per_hh_data[, `:=` (p_to_loop = 0,
                    p_from_loop = 0,
                    p_intra_district = 0,
                    p_d2d_not_loop = 0)]
per_hh_data[, p_to_loop := ifelse(home_cbd == 0 & work_cbd == 1 & emply_ask == 1, 1, 0)]
per_hh_data[, p_from_loop := ifelse(home_cbd == 1 & work_cbd == 0& emply_ask == 1, 1, 0)]
per_hh_data[, p_intra_district := ifelse((home_county == work_county| (home_chicago == 1 & work_chicago == 1)) & emply_ask == 1 & p_to_loop + p_from_loop == 0, 1, 0)]
per_hh_data[, p_intra_district := ifelse((home_chicago  != work_chicago), 0, p_intra_district)]
per_hh_data[, p_d2d_not_loop := ifelse(p_to_loop + p_from_loop + p_intra_district == 0 & emply_ask == 1, 1, 0)]
per_hh_data[, p_not_worker := ifelse( emply_ask == 1, 0, 1)]


# mode to work
# Need the following
# "p_commute_none"    
# "p_commute_home"     
# "p_commute_car_alone" 
# "p_commute_carpool"
# "p_commute_walkbike"
# "p_commute_other"

# per_hh_data[, typical_commute_mode := ]
per_hh_data[wmode == 18, typical_commute_mode := 'none']
per_hh_data[wmode <0 , typical_commute_mode := 'none']

# per_hh_data[job_type == 3, typical_commute_mode := 'home']
per_hh_data[wmode %in% c(4:6), typical_commute_mode := 'car']
per_hh_data[wmode %in% c(1:2), typical_commute_mode := 'walkbike']
per_hh_data[wmode %in% c(7:17, 97, 3),
            typical_commute_mode := 'other']

# Commute mode dummy variables
per_hh_data[, p_commute_none := 0]
per_hh_data[typical_commute_mode == 'none', p_commute_none := 1]
# per_hh_data[, p_commute_home := 0]
# per_hh_data[typical_commute_mode == 'home', p_commute_home := 1]

per_hh_data[, p_commute_car := 0]
per_hh_data[typical_commute_mode == 'car', p_commute_car := 1]
per_hh_data[, p_commute_walkbike := 0]
per_hh_data[typical_commute_mode == 'walkbike', p_commute_walkbike := 1]

per_hh_data[, p_commute_other := 0]
per_hh_data[typical_commute_mode == 'other', p_commute_other := 1]

# select data for weighting

h_cols = names(per_hh_data)[names(per_hh_data) %like% '^h_' ]
p_cols = names(per_hh_data)[names(per_hh_data) %like% '^p_']
transit_cols = names(per_hh_data)[names(per_hh_data) %like% '^n_']

hh_survey = per_hh_data[per_hh_data[ , .I[which.min(perno)], by = sampno]$V1][study == 'cmap'][, c('sampno', 'cluster', 'puma_id', h_cols), with = FALSE]


p_survey = per_hh_data[, c('puma_id', 'cluster', 'perno', 'sampno', p_cols), with = FALSE]

trip_data = per_hh_data[, lapply(.SD, min), by = .(sampno), .SDcols =transit_cols ]

hh_survey = hh_survey[trip_data, on = .(sampno),nomatch = 0]

# limit to cmap
hh_survey = hh_survey[sampno %in% per_hh_data[study == 'cmap', sampno]]
p_survey = p_survey[sampno %in% per_hh_data[study == 'cmap', sampno]]

saveRDS(hh_survey, file = file.path(wt_dir, 'hh_survey.rds'))
saveRDS(p_survey, file = file.path(wt_dir, 'p_survey.rds'))
