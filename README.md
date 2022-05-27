# cmap_abm
CMAP's ActivitySim-based Activity Based Model


*This is currently in development/transition to Activitysim*

## To run
simulation.py -c %ASIM%\configs -o %ASIM%\output -d %ASIM%\data

## Required inputs:

### ActivitySim
 - **land use** (e.g. "land_use.csv")
    - check if external zones are added (TAZ 3633-3649)
 - **households** (e.g. "households.csv")
    - "household_id": list of successive integers from 1 
 - **persons** (e.g. "persons.csv")
    - "person_id": list of successive integers from 1
 - **skims**
    - highway and transit (e.g. "taz.omx")
    - microzone (e.g. "maz_to_maz_bike.csv" and "maz_to_maz_walk.csv")
    - transit access: check if "walk_dist_xxx" columns in "land_use.csv" (populated by "cmap_maz_stop.py")