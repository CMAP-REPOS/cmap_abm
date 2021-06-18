# Skims distance review

library(leaflet)
library(yaml)

get_distance_meters =
  function(location_1, location_2) {
    distance_meters =
      distHaversine(
        matrix(location_1, ncol = 2),
        matrix(location_2, ncol = 2))
    return(distance_meters)
  }
settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')

data_dir = settings$data_dir
cmap_dir = file.path(data_dir, settings$cmap_folder)
nirpc_dir = file.path(data_dir, settings$nirpc_folder)
output_dir = settings$SPA_input_dir

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

zones09 = st_read(file.path(settings$zone_dir, 'Zone09_CMAP_2009.shp'))
zones09 = st_transform(zones09, st_crs("+proj=longlat +datum=WGS84 +ellps=GRS80"))
zones17 = st_read(file.path(zone_dir, "zones17.shp"))
zones17 = st_transform(zones17, st_crs("+proj=longlat +datum=WGS84"))
subzones17 = st_read(file.path(zone_dir, "subzones17.shp"))
zones17 = st_transform(zones17, st_crs("+proj=longlat +datum=WGS84"))


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

per_raw[location_raw[loctype == 2], `:=` (PER_WK_ZONE_ID = zone17, 
                                          work_lat = latitude,
                                          work_lon = longitude), on = .(perno, sampno)]


work_coords = st_as_sf(per_raw[!is.na(work_lat), .(sampno, perno, work_lon, work_lat)], 
                       coords = c(x = "work_lon", y = "work_lat"), 
                       crs = st_crs(zones09))

work_zones_09 = setDT(st_join(work_coords, zones09))

rows = nrow(per_raw)
per_raw = work_zones_09[, .(sampno, perno, work_zone_09 = zone09, work_zone_07 = ZONE07)][per_raw, 
                                                                                          on = .(sampno, perno)]

source(file.path(settings$visualizer_dir, 'scripts', 'ZMX.R'))

skim_file = settings$skims_file
skimMat = readZipMat(skim_file)
DST_SKM = setNames(reshape2::melt(skimMat), c('o', 'd', 'dist'))
setDT(DST_SKM)
skim_for_join = DST_SKM[o==d]

skim_for_join[, intrazone_dist := dist]
zones09 = merge(zones09, skim_for_join, by.x = 'zone09', by.y = 'o')
zones09$map_label = paste0('zone ', zones09$zone09, '\nintrazone distance (mi) = ', 
                           round(zones09$intrazone_dist, 2))

# sampno 30009712, perno 2
home_coords = c(-87.93101 , 41.87911)
work_coords = c(-87.93681 , 41.86407)

leaflet(zones09) %>% addPolygons(weight = 0.5, label = ~map_label) %>% 
  addCircleMarkers(lng = -87.93101, lat = 41.87911, radius = 3,
                   fillColor = 'red',
                   stroke = FALSE, 
                   label = 'Google distance ',
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -87.93681, lat = 41.86407, radius = 3,
                   fillColor = 'green',
                   stroke = FALSE, 
                   fillOpacity = 1)

leaflet(zones17) %>% addPolygons(weight = 0.5, label = ~zone17) %>% 
  addCircleMarkers(lng = -87.93101, lat = 41.87911, radius = 3,
                   fillColor = 'red',
                   stroke = FALSE, 
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -87.93681, lat = 41.86407, radius = 3,
                   fillColor = 'green',
                   stroke = FALSE, 
                   fillOpacity = 1)

# get_distance_meters(home_coords, work_coords) * 0.000621371

# sampno 70056487, perno 2
home_coords1 = c(-88.7765, 41.9434)
work_coords1 = c(-88.77423, 41.93131 )
get_distance_meters(home_coords1, work_coords1) * 0.000621371

map = leaflet(zones09) %>% addTiles() %>%
  addPolygons(weight = 0.5, label = ~map_label) %>% 
  addCircleMarkers(lng =-88.7765, lat = 41.9434, radius = 3,
                   fillColor = 'red',
                   stroke = FALSE, 
                   label = 'Google distance 1.3 miles\nsampno 70056487',
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -88.77423, lat = 41.93131, radius = 3,
                   fillColor = 'green',
                   stroke = FALSE, 
                   label = 'Google distance 1.3 miles\nsampno 70056487',
                   fillOpacity = 1) %>%
  addCircleMarkers(lng =-87.058, lat =  41.60583, radius = 3,
                   fillColor = 'red',
                   stroke = FALSE, 
                   label = 'Google distance 0.6 miles\nsampno 50000088',
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -87.05573, lat = 41.59815, radius = 3,
                   label = 'Google distance 0.6 miles\nsampno 50000088',
                   fillColor = 'green',
                   stroke = FALSE, 
                   fillOpacity = 1)%>%
  addCircleMarkers(lng =-87.47122, lat =  41.37291, radius = 3,
                   fillColor = 'red',
                   label = 'Google distance 0.2 miles\nsampno 50000822',
                   
                   stroke = FALSE, 
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -87.46947, lat = 41.37082, radius = 3,
                   fillColor = 'green',
                   label = 'Google distance 0.2 miles\nsampno 50000822',
                   
                   stroke = FALSE, 
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -88.41719, lat =   41.36454 , radius = 3,
                   fillColor = 'red',
                   label = 'Google distance 0.7 miles\nsampno 70056423',
                   
                   stroke = FALSE, 
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -88.42363, lat = 41.36024, radius = 3,
                   fillColor = 'green',
                   stroke = FALSE, 
                   label = 'Google distance 0.7 miles\nsampno 70056423',
                   
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -88.75616, lat =  41.96713  , radius = 3,
                   fillColor = 'red',
                   label = 'Google distance 2.4 miles\nsampno 70056842',
                   
                   stroke = FALSE, 
                   fillOpacity = 1) %>%
  addCircleMarkers(lng = -88.72239, lat = 41.96141 , radius = 3,
                   fillColor = 'green',
                   label = 'Google distance 2.4 miles\nsampno 70056842',
                   
                   stroke = FALSE, 
                   fillOpacity = 1)

# 50000822 2
# work_lat  work_lon
# 1: 41.37082 -87.46947
# home_lat  home_lon
# 1: 41.37291 -87.47122

# 70056423 1
# home_lat  home_lon
# 1: 41.36454 -88.41719
# work_lat  work_lon
# 1: 41.36024 -88.42363

# 70056842 1   
# work_lat  work_lon
# 1: 41.96141 -88.72239
# home_lat  home_lon
# 1: 41.96713 -88.75616

folder  = 'N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/qaqc'
library(htmlwidgets)
saveWidget(map, file.path(folder, 'zone09_skim_dist_map.html'))
# 
# hhtaz PER_WK_ZONE_ID WDIST PERNO    SAMPN
# 1:  1229           1233  2.95     1 30009467
# 2:  1229           1233  2.95     2 30009467
# 3:  1437           1443  2.99     2 30009712
# 4:  1884           1884  2.99     1 50000088
# 5:  1877           1877  2.99     2 50000822
# ---                                          
#   151:  1717           1717  2.99     1 70056423
# 152:  1740           1740  2.99     2 70056487
# 153:  1740           1740  2.99     1 70056737
# 154:  1739           1739  2.99     2 70056842
# 155:  1717           1717  2.99     1 70100562


per_raw[hh_raw, `:=` (home_lat = i.home_lat, home_lon = i.home_lon,
                       home_zone09 = i.zone09), on = .(sampno)]
zones09_dt = setDT(zones09)
# DST_SKM[zones09_dt, nearest_zone := nearest_poly_index, on = .(o = zone09)]
DST_SKM[zones09_dt, area := AREA* 0.00000038610, on = .(o = zone09)]
DST_SKM[, dist_from_skims:= dist]
# DST_SKM[DST_SKM[, .(i_d = d, i_dist = dist, i_o = o)], dist_to_nearest_zone := i_dist, on = .(o = i_o, nearest_zone = i_d)]
# 
# DST_SKM[o==d, dist := dist_to_nearest_zone/2]
DST_SKM[o == d, dist := sqrt(area)/2]
DST_SKM[, recalculated_dist := dist]

per_raw = DST_SKM[, .(home_zone_09 = o, work_zone_09 = d, dist_from_skims, recalculated_dist)][per_raw, on = .(home_zone_09 = home_zone09 , work_zone_09)]

maz_lookup = fread('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/zone_data/SP_MAZ_to_MAZ_and_TAP.txt')

per_raw[(sampno == 50000822 & perno == 2) | (sampno == 70056842 & perno == 2) |
        (sampno == 70056423 & perno == 1) |
        (sampno == 70056487 & perno == 2) |
        (sampno == 50000088 & perno == 1), .(sampno, perno, home_zone_09, work_zone_09,
                                             dist_from_skims, recalculated_dist, 
                                             google_dist = fcase(sampno == 50000822, 0.2,
                                                                 sampno == 70056842, 2.4,
                                                                 sampno == 70056423,0.7 ,
                                                                 sampno == 70056487, 1.3,
                                                                 sampno == 50000088, 0.6))
          ]
