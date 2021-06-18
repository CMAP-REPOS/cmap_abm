library(data.table)
library(readxl)

# obs data review


metra = read_xlsx('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/survey_data/Metra_2019/2019_Origin-Destination_survey_package/2019_OriginDestinationSurvey_V2_send.xlsx')
setDT(metra)
names(metra)
metra[, .N]
metra[, sum(Subregional_weight, na.rm = TRUE)]



cta = read_xlsx('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/survey_data/CTA_2017/CTA OD Final Data.xlsx')
setDT(cta)
