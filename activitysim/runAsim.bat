SET MKL_NUM_THREADS=1

REM MAZ/TAP walk
cd maz_maz_tap
python.exe cmap-sp.py
cd ..

REM Run Activitysim
activitysim run -c configs_3_zone -o output_samp2 -d data_cmap_samp