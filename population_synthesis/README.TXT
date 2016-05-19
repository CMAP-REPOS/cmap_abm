HOW TO RUN ABM POPULATION SYNTHESIS
-----------------------------------

0.  OPTIONAL: If control totals are being run updated with new PUMS data for
    the first time, uncomment the large section of code in 1__control_files/
    createNewControls.R that creates countsPUMS.csv and distPUMS.csv in
    1__control_files/inputs. These new CSVs can then be reused for any future
    population synthesis based on the same PUMS data. (NOTE: the 2007-2011 ACS
    was the last one where all of the PUMAs match the 2000 PUMAs that the
    current model was estimated with, so it is not recommended to use newer
    PUMS data without first updating the PopSyn model itself.)

1.  a) FOR BASE YEAR (2010) POPSYN:
    Update any inputs in 1__control_files (replacing oldControls.csv with the
    previous version of forecastControls.csv from 2__synpop/inputs/datatables),
    then submit update_controls.bat. The output file will be saved to
    2__synpop/inputs/datatables/forecastControls.csv.
    
    b) FOR FUTURE YEAR POPSYN:
    Apply base year control variables' rates to future year's household counts,
    and save the results to 2__synpop/inputs/datatables/forecastControls.csv.

2.  Submit 2__synpop/model/RunCMAPPopSynForecast.bat. This requires the Java
    Runtime Environment to be installed, so it is recommended that this be done
    from one of the modeling servers that have it already (e.g. cmap-mod04).

3.  Replace the GEOG_IN.TXT and HH_IN.TXT files in 3__hh_taz_to_maz/inputs if
    they are out of date. These are taken directly from the 4-step Trip
    Generation model.

4.  Submit 3__hh_taz_to_maz/distribute_hh_to_maz.bat. This will assign the
    zone-level households produced by ARCPopSyn to subzones.

5.  Copy the following CSVs to the main ABM inputs folder for new model runs:
        - 2__synpop/outputs/ForecastPersonFile.csv
        - 3__hh_taz_to_maz/outputs/ForecastHHFile_MAZ.csv
