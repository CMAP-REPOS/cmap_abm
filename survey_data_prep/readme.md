# CMAP HTS Data Processing for Model Comparisons

Steps for processing CMAP survey data and visualizing against model output.

Note: 
OBS processing and re-weighting are not included in this step-by-step, as their outputs are saved in the repo (calibration target tables from OBS processing, and weight outputs from re-weighting exercise).
The scripts for these processes are included in the repo and should be fairly easy to follow along if necessary to re-do.

## Step 0: Pre-requisites

* Create Python environment
  * The Python libraries for running ActivitySim are inclusive of those needed to run the Survey Processing App
  * Thus -- you can use the environment.yml file in the cmap_abm repo
  * If you have Conda installed -- run in the command line: conda env create -f environment.yml 
  * Troubleshooting: I had to move some conda packages into Pip in a copy of the environment.yml to install them (might have related to my version of Conda).   

* Confirm you have R installed (version 4.0.2 or higher)
  * Rstudio is also handy!

* Download or confirm you have data available
  * CMAP and NIRPC survey data files
  * CMAP updated weights
  * TAZ shapefile for the region -- 2017 zones and subzones for county crosswalks; and 2009 zones are still referenced due to previous distance skims
  * Skims matrix with distance between TAZs

* Create/edit settings file with filepaths
  * Example: survey_data_prep/cmap_settings.yaml (edit or replace this)
  * Replace file paths with file/folder paths you'll use (it's OK to add these as they come up, rather than all at once)
  * Data processing scripts read the settings file so that you don't have to change the path references in every script
	* You DO need to change the settings file path in the scripts that reference it, unless passing from the command line

## To run automatically & skip the steps 1-5:

* Data prep, SPA, and visualizer summaries:
	* https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/run_data_prep_scripts.bat
	* Edit the settings file path
	* Run via command line
* Run the visualizer (see step 6)

Otherwise -- to run each script and confirm things look good -- follow the steps below!

## Step 1: Format HTS data to be used in Survey Processing Application (SPA)

Time: <5 minutes

* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/data_processing/process_cmap_survey.R
* Inputs needed: CMAP and NIRPC HTS data; folder for SPA inputs (create the folder if you don't have one); zones shapefiles; weights files
* Ensure that the settings file path in the script is pointing to the right file
* Run the script -- if any filepaths are wrong in the settings, fix and run again. 
* What the script does:
  * Aggregates certain fields so that the SPA tool is set up to map them to the correct model format
  * Derives other fields like school/home/work zone
  * Outputs edited files
 

## Step 2: Run SPA

Time: ~1 hour (varies by machine)

* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/SPA/__init__.py
* Ensure that the settings file path is pointing to the right file
* Run the script
* Check that outputs are in the SPA Processed folder
* What the tool does:
  * Links change mode trips together
  * Processes trips into tours and joint tours
  * Re-codes modes/purposes to match model formats

## Step 3: Prepare survey data summaries for Visualizer dashboard

* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/Visualizer/scripts/CMAP_visualizer_prep.R
* Ensure that the settings file path is pointing to the right file
* Ensure you're in a brand new R environment (restart R session or restart RStudio)
* Run the script
* What the script does:
  * Creates summaries of the survey data
  * Stores summaries in specified path for use in the visualizer
  
Time: ~5 minutes

## Step 4: Prepare Census data summaries for Visualizer dashboard

* Note that these have been pushed to git, BUT you can re-create them with the scripts if needed.
* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/Visualizer/scripts/Get_census_data_CMAP.R
* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/Visualizer/scripts/AutoOwnership_Census_CMAP.R
* Edit settings & params files
* These scripts generate Visualizer summaries based on census/ACS data.

Time: ~5 minutes

## Step 5: Prepare ActivitySim output summaries for Visualizer dashboard

* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/Visualizer/scripts/Summarize_ActivitySim_cmap.R
* Ensure that the settings file path is pointing to the right file
* Run the script
* What the script does:
  * Splits tour/trip model outputs into joint tours/trips
  * Creates summaries of the model outputs for the Visualizer

Time: ~45 minutes

## Step 6: Run the visualizer

* Script: https://github.com/CMAP-REPOS/cmap_abm/blob/activitysim/survey_data_prep/Visualizer/generateDashboard_cmap_vs_model.bat
* Edit the various paths/inputs as needed -- R location, settings file, etc.
* Run via the command line
