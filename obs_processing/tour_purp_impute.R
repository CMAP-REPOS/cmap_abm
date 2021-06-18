# Estimate models for imputing tour purpose

library(data.table)
library(dplyr)
library(stringr)
library(yaml)
library(readxl)
library(nnet)
# load settings
# settings = yaml.load_file('N:/Projects/CMAP_Activitysim/cmap_abm/survey_data_prep/cmap_inputs.yml')
settings = yaml.load_file('C:/Users/leah.flake/OneDrive - Resource Systems Group, Inc/Git Repos/cmap_model_update/cmap_inputs.yml')
# working directory
# Paths and other constants
project_dir = settings$proj_dir
data_dir = file.path(settings$SPA_output_dir)


tours = fread(file.path(data_dir, 'tours.csv'))
trips = fread(file.path(data_dir, 'trips.csv'))

trips[, origDepHr9AMTo3PM  := ifelse(ORIG_DEP_HR %in% c(9:14), 1, 0)]
trips[, origDepHr3PMTo7PM   := ifelse(ORIG_DEP_HR %in% c(15:18), 1, 0)]
trips[, origDepHr7PMTo10PM   := ifelse(ORIG_DEP_HR %in% c(19:21), 1, 0)]
trips[, Disc := ifelse(DEST_PURP %in% c(7:9), 1, 0)]
trips[, Maint := ifelse(DEST_PURP %in% c(5:6), 1, 0)]
trips[, KNR := ifelse(TRIPMODE %in% c(7, 9), 1, 0)]
trips[, PNR := ifelse(TRIPMODE == 8, 1, 0)]
trips[, tour_purp_work := ifelse(TOURPURP == 1, 1, 0)]

model_rhs = ~ origDepHr9AMTo3PM +
  origDepHr3PMTo7PM +
  origDepHr7PMTo10PM +
  Disc +
  KNR +
  PNR


set.seed(413)
model_work = multinom(
  update(model_rhs, tour_purp_work ~ .),
  data = trips,
  model = TRUE, Hess = TRUE)

summary(model_work)

options(scipen = 999)
tidy.opts = options(scipen = 99)
print(broom::tidy(model_work, exponentiate = FALSE), n = 400)

DescTools::PseudoR2(model_work) 

trips[, depHr10AMTo2PM  := ifelse(ORIG_DEP_HR %in% c(10:14), 1, 0)]
trips[, noon  := ifelse(ORIG_DEP_HR  == 12, 1, 0)]
trips[, origOrDestIsWork  := ifelse(ORIG_PURP  == 1 | DEST_PURP == 1, 1, 0)]
trips[tours, tour_purp_worksub := ifelse(IS_SUBTOUR == 1, 1, 0), on = .(PER_ID, HH_ID, TOUR_ID)]

model_rhs = ~depHr10AMTo2PM +
  noon +
  origOrDestIsWork 

model_worksub = multinom(
  update(model_rhs, tour_purp_worksub ~ .),
  data = trips,
  model = TRUE, Hess = TRUE)

summary(model_worksub)

options(scipen = 999)
tidy.opts = options(scipen = 99)
print(broom::tidy(model_worksub, exponentiate = FALSE), n = 400)

DescTools::PseudoR2(model_worksub) 


trips[tours, is_fully_joint := ifelse(FULLY_JOINT == 1, 1, 0), on = .(PER_ID, HH_ID, TOUR_ID)]

model_rhs = ~origDepHr9AMTo3PM +
  origDepHr3PMTo7PM +
  origDepHr7PMTo10PM +
  Disc + 
  Maint



model_fj = multinom(
  update(model_rhs, is_fully_joint ~ .),
  data = trips,
  model = TRUE, Hess = TRUE)

summary(model_fj)

options(scipen = 999)
tidy.opts = options(scipen = 99)
print(broom::tidy(model_fj, exponentiate = FALSE), n = 400)

DescTools::PseudoR2(model_fj) 

