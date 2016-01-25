HOW TO RUN TAP ACCESSIBILITIES CALCULATIONS FROM A *COMPLETED* MODEL RUN
------------------------------------------------------------------------

1.  Copy the following files from ..\inputs to inputs:

        - CMAP_MAZ_cents.txt
        - SubzoneData.csv
        - tap_attributes.csv

        
2.  Connect to an Emme license and launch Emme Shell. From Emme Shell, navigate
    to ..\CMAP-ABM\Database\emmemat and submit the following commands to create
    the necessary ZMX-formatted input matrices:
    
        > python ../../../scripts/EMXtoZMX.py ../../CMAP-ABM.emp 103 mf3431 mf3432 mf3433 mf3434 mf3461
        > python ../../../scripts/EMXtoZMX.py ../../CMAP-ABM.emp 105 mf5431 mf5432 mf5433 mf5434 mf5461
        
    Disconnect from the Emme license.
        

3.  Copy the following files from ..\CMAP-ABM\Database\emmemat to inputs:

        - mf3175.zmx
        - mf3176.zmx
        - mf3177.zmx
        - mf3431.zmx
        - mf3432.zmx
        - mf3433.zmx
        - mf3434.zmx
        - mf3461.zmx
        - mf5175.zmx
        - mf5176.zmx
        - mf5177.zmx
        - mf5431.zmx
        - mf5432.zmx
        - mf5433.zmx
        - mf5434.zmx
        - mf5461.zmx
        
    Note: be sure to copy the ZMX files, instead of the EMX files.
        

4.  Launch runAccessibilities.bat. Python scripts will run automatically,
    and will take approximately 20 minutes and 100+ GB RAM.