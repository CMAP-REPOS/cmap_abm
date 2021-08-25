SET MKL_NUM_THREADS=1

REM goto skipmazmaztap
REM MAZ/TAP walk
cd maz_maz_tap
python.exe cmap-sp.py
cd ..

:skipmazmaztap
REM Run Activitysim
activitysim run -c configs_3_zone -o output_samp2 -d data_cmap_samp
REM Debug/test: activitysim run -c configs_3_zone -o output_ltest -d data_cmap_ltest