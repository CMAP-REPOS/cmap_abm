# review gps trip/tour rates

library(data.table)
library(yaml)
settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')

spa_trip = fread(file.path(settings$SPA_output_dir, 'trips.csv'))
spa_tour = fread(file.path(settings$SPA_output_dir, 'tours.csv'))
spa_per = fread(file.path(settings$SPA_output_dir, 'persons.csv'))
spa_hh = fread(file.path(settings$SPA_output_dir, 'households.csv'))



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
data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
nirpc_dir = file.path(data_dir, settings$nirpc_folder)

studies = c('cmap', 'nirpc')
folders = c(cmap_dir, nirpc_dir)

place_raw = read_and_bind(folders, studies, 'place.csv', 'place')
per_raw = read_and_bind(folders, studies, 'person.csv', 'person')
gps_raw = fread('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/survey_data/MDT_CMAP_Research_Dataset_Lean/Data/gps_place.csv')
gps_day_raw = fread('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/survey_data/MDT_CMAP_Research_Dataset_Lean/Data/gps_day.csv')

spa_trip[place_raw, study := i.study, on = .(HH_ID = sampno, PER_ID = perno, ORIG_PLACENO = placeno )]
spa_per[per_raw, study := i.study, on = .(HH_ID = sampno, PER_ID = perno)]

spa_trip[gps_raw, in_gps := 1, on = .(HH_ID = sampno, PER_ID = perno, ORIG_PLACENO = placeno )]

n_gps_trips = spa_trip[in_gps == 1, .(n_gps_trips = .N), .(HH_ID, PER_ID)]
n_non_gps_trips = spa_trip[is.na(in_gps), .(non_gps_trips = .N), .(HH_ID, PER_ID)]

n_gps_tours = spa_trip[in_gps == 1, .(.N), .(HH_ID, PER_ID, TOUR_ID)]
n_non_gps_tours = spa_trip[is.na(in_gps), .(.N), .(HH_ID, PER_ID, TOUR_ID)]
n_non_gps_tours = n_non_gps_tours[!n_gps_tours, on = .(HH_ID, PER_ID, TOUR_ID)]

n_gps_tours = n_gps_tours[, .(n_gps_tours = .N), .(HH_ID, PER_ID)]
n_non_gps_tours = n_non_gps_tours[, .(non_gps_tours = .N), .(HH_ID, PER_ID)]



spa_per[, non_gps_trips := 0]
spa_per[, gps_trips := 0]

spa_per[, non_gps_tours := 0]
spa_per[, gps_tours := 0]

spa_per[n_gps_trips, gps_trips := n_gps_trips, on = .(HH_ID, PER_ID)]
spa_per[n_non_gps_trips, non_gps_trips := i.non_gps_trips, on = .(HH_ID, PER_ID)]
spa_per[n_gps_tours, gps_tours := n_gps_tours, on = .(HH_ID, PER_ID)]
spa_per[n_non_gps_tours, non_gps_tours := i.non_gps_tours, on = .(HH_ID, PER_ID)]

spa_per[, total_trips := non_gps_trips + gps_trips]
spa_per[, total_tours := non_gps_tours + gps_tours]
spa_per[gps_trips > 0 & study == 'cmap' & AGE_CAT > 1, mean(total_trips)]
spa_per[study == 'cmap' & AGE_CAT > 1, mean(total_trips)]
spa_per[gps_tours > 0 & study == 'cmap' & AGE_CAT > 1, mean(total_tours)]
spa_per[study == 'cmap' & AGE_CAT > 1, mean(total_tours)]

spa_per[AGE_CAT > 1, mean(total_trips), by = study]
spa_per[AGE_CAT > 1, mean(total_tours), by = study]

spa_per[AGE_CAT > 1, .(avg_trips = mean(total_trips),
                       avg_tours = mean(total_tours), 
                       N = .N), by = .(has_gps_trip = gps_trips > 0,
                                       study)]

write.table(spa_per[AGE_CAT > 1, .(avg_trips = mean(total_trips),
                       avg_tours = mean(total_tours), 
                       N = .N), by = .(has_gps_trip = gps_trips > 0,
                                       study)], 'clipboard', sep = '\t', row.names = FALSE)


write.table(spa_per[AGE_CAT > 1 & total_trips > 0, .(avg_trips = mean(total_trips),
                                   avg_tours = mean(total_tours), 
                                   N = .N), by = .(has_gps_trip = gps_trips > 0,
                                                   study)], 'clipboard', sep = '\t', row.names = FALSE)

per_raw[spa_per, `:=` (total_trips = i.total_trips,
                       total_tours = i.total_tours), on = .(sampno = HH_ID, 
                                                            perno = PER_ID)]

per_raw[, in_gps_dataset := 0]
per_raw[gps_day_raw, in_gps_dataset := 1, on = .(sampno, perno)]

per_raw[smrtphn > 0, .(mean(total_trips)), .(smrtphn)]
per_raw[smrtphn > 0, .(mean(total_trips)), .(in_gps_dataset)]
per_raw[smrtphn > 0, .(mean(total_trips)), .(retmode_final)]

per_raw[, .(sum(wtperfin, na.rm = TRUE)/per_raw[, sum(wtperfin, na.rm = TRUE)]), .(has_trips = total_trips > 0)]

per_raw[total_trips == 0 & smrtphn > 0, .(has_smartphone_no_trips = (sum(wtperfin * (smrtphn == 1), na.rm = TRUE)/
                                                              per_raw[smrtphn == 1, sum(wtperfin, na.rm = TRUE)]),
                                          no_smartphone_no_trips = (sum(wtperfin * (smrtphn == 2), na.rm = TRUE)/
                                                             per_raw[smrtphn == 2, sum(wtperfin, na.rm = TRUE)]))]


per_raw[total_trips == 0, .(in_gps_dataset_no_trips = (sum(wtperfin * (in_gps_dataset == 1), na.rm = TRUE)/
                                                                       per_raw[in_gps_dataset == 1, sum(wtperfin, na.rm = TRUE)]),
                            not_in_gps_dataset_no_trips = (sum(wtperfin * (in_gps_dataset == 0), na.rm = TRUE)/
                                                                      per_raw[in_gps_dataset == 0, sum(wtperfin, na.rm = TRUE)]))]


per_raw[total_trips == 0 & study == 'cmap', .(smartphone_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(1:4)), na.rm = TRUE)/
                                                         per_raw[retmode %in% c(1:4), sum(wtperfin, na.rm = TRUE)]),
                            no_smarpthone_no_trips = 
                              (sum(wtperfin * (retmode %in% c(5:7)), na.rm = TRUE)/
                                                             per_raw[retmode %in% c(5:7), sum(wtperfin, na.rm = TRUE)]))]

per_raw[total_trips == 0 & study == 'cmap', .(only_smartphone_no_trips = 
                                                (sum(wtperfin * (retmode == 1), na.rm = TRUE)/
                                                   per_raw[retmode ==1, sum(wtperfin, na.rm = TRUE)]),
                                              other_mode_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(2:7)), na.rm = TRUE)/
                                                   per_raw[retmode %in% c(2:7), sum(wtperfin, na.rm = TRUE)]),
                                              missing_mode_no_trips = 
                                                (sum(wtperfin * (retmode == -1), na.rm = TRUE)/
                                                   per_raw[retmode == -1, sum(wtperfin, na.rm = TRUE)]))]

per_raw[total_trips == 0 & study == 'cmap', .(only_smartphone_no_trips = 
                                                (sum(wtperfin * (retmode == 1), na.rm = TRUE)),
                                              other_mode_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(2:7)), na.rm = TRUE)),
                                              missing_mode_no_trips = 
                                                (sum(wtperfin * (retmode == -1), na.rm = TRUE)))]

per_raw[total_trips == 0 & study == 'cmap', .(only_smartphone_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(1:4)), na.rm = TRUE)),
                                              other_mode_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(5:7)), na.rm = TRUE)),
                                              missing_mode_no_trips = 
                                                (sum(wtperfin * (retmode == -1), na.rm = TRUE)))]

per_raw[study == 'cmap', .(only_smartphone_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(1:4)), na.rm = TRUE)),
                                              other_mode_no_trips = 
                                                (sum(wtperfin * (retmode %in% c(5:7)), na.rm = TRUE)),
                                              missing_mode_no_trips = 
                                                (sum(wtperfin * (retmode == -1), na.rm = TRUE)))]

per_raw[study == 'cmap', retmode_recode2 := fcase(retmode < 0, 'missing', 
                                                            retmode %in% 1:4, 'smartphone included',
                                                            default = 'no smartphone')]

per_raw[study=='cmap' & age > 5, .(sum(total_trips * wtperfin, na.rm = TRUE)), .(retmode_recode2)]

per_raw[study == 'nirpc' & retmode == 1, retmode_recode2 := 'smartphone included']
per_raw[study == 'nirpc' & retmode != 1, retmode_recode2 := 'no smartphone']
per_raw[, trip_adj_type := fcase((age %in% 5:12 | aage == 2) & retmode_recode2  == 'smartphone included' , 
                                        'Kid 5-12 proxied by smartphone',
                                     (age %in% 5:12 | aage == 2) & retmode_recode2  != 'smartphone included', 
                                          'Kid 5-12 reported online',
                                     (age %in% 13:17 | aage == 3) & proxy == 1 & 
                                       retmode_recode2 == 'smartphone included',
                                          'Kid 13-17 proxied by smartphone',
                                     (age %in% 13:17 | aage == 3) & proxy == 2 & 
                                       retmode_recode2  == 'smartphone included', 
                                        'Kid 13-17 self-reported by smartphone',
                                     (age %in% 13:17 | aage == 3) & retmode_recode2 != 'smartphone included', 
                                          'Kid 13-17 reported online',
                                     (age >= 18 | aage > 3) & retmode_recode2 == 'smartphone included', 'Adult reported by smartphone',
                                     (age >= 18 | aage > 3) & retmode_recode2 != 'smartphone included', 'Adult reported online')]
    
per_raw[!is.na(trip_adj_type), .(weighted_people = sum(wtperfin, na.rm = TRUE), 
            weighted_trips = sum(total_trips * wtperfin, na.rm = TRUE),
            zero_trips = sum((total_trips == 0) * wtperfin, na.rm = TRUE),
            unweighted_people = .N), .(trip_adj_type)]

per_raw[, has_trips := 0]
per_raw[place_raw[placeno > 1], has_trips := 1, on = .(sampno, perno)]

place_raw[per_raw, retmode := i.retmode, on = .(sampno, perno)]
per_raw[study == 'nirpc' & retmode != 1, retmode_recode2 := 'no smartphone']
per_raw[study == 'nirpc' & retmode == 1, retmode_recode2 := 'smartphone included']
### tables for memo

spa_per[per_raw, PEREXPFAC := wtperfin, on = .(HH_ID = sampno, PER_ID = perno)]
rates = spa_per[, .(`Weighted persons` = sum(PEREXPFAC, na.rm = TRUE), 
            `Trips per person` = sum(PEREXPFAC * total_trips, na.rm = TRUE)/sum(PEREXPFAC, na.rm = TRUE), 
            `Tours per person` = sum(PEREXPFAC * total_tours,na.rm = TRUE)/sum(PEREXPFAC, na.rm = TRUE)), .(PERSONTYPE)][order(PERSONTYPE)]
person_type_labels = data.table(PERSONTYPE = c(1:8), `Person type` = c("FT Worker", "PT Worker", 
                                                                       "Univ Stud", "Non-Worker", 
                                                                       "Retiree", "Driv Student", 
                                                                       "Non-DrivStudent", "Pre-Schooler"))
rates = person_type_labels[rates, on = .(PERSONTYPE)]

rates %>% write.table('clipboard', sep = '\t', row.names = FALSE)         

spa_per[per_raw, retmode_recode2 := i.retmode_recode2, on = .(HH_ID == sampno, PER_ID == perno)]
spa_per[retmode_recode2 == 'missing', retmode_recode2 := 'no smartphone']
rates2 = spa_per[, .(`Weighted persons` = sum(PEREXPFAC, na.rm = TRUE), 
                    `Trips per person` = sum(PEREXPFAC * total_trips, na.rm = TRUE)/sum(PEREXPFAC, na.rm = TRUE), 
                    `Tours per person` = sum(PEREXPFAC * total_tours,na.rm = TRUE)/sum(PEREXPFAC, na.rm = TRUE)), 
                 .(PERSONTYPE, retmode_recode2)][order(PERSONTYPE)]
rates2 = dcast(rates2, PERSONTYPE ~ retmode_recode2, value.var = c('Weighted persons', 'Trips per person', 'Tours per person'))
rates2 = person_type_labels[rates2, on = .(PERSONTYPE)]
rates2 %>% write.table('clipboard', sep = '\t', row.names = FALSE)         
