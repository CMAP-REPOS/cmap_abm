#############################################################################
# Run MAZ Skimming Initial Setup
# Ben Stabler, stabler@pbworld.com, 10/29/12
# Roshan Kumar, kumarr1@pbworld.com
# "C:\Python26\P64\python.exe" runMAZSkimsInitial
#############################################################################

#inputs
isBaseYear = True
navteqShpFile = "inputs/Chicago_streets.shp"
#SPThreshold = 15*5280 #max dist in feet
SPThreshold = 5*5280 #max dist in feet
maxMAZID = 70000
minTAPID = 60000
maxCenID = 16819
rsThreshold = 9
landUseTypes = 12
centroidsMAZFile = "inputs/CMAP_MAZ_cents.txt"
rsToSZInput = "inputs/cmapRStoSubZone.txt" 
tapFile = "inputs/tap_attributes.csv"
spMAZ2TAP = "outputs/SP_MAZ_to_MAZ_and_TAP.txt"
nodesinMAZFile = "inputs/nodes_in_MAZ_CMAP.txt"

#additional parameters for future year maz skims
nodeDistFromHwy = "inputs/cmapDistFromHwy.txt" #for future build MAZs
mazLUFile = "inputs/cmapMAZLandUseShare.txt" #for future build MAZs
spInsideRS8 = "outputs/SP_with_TAP_clean_RS8_LU_Share_Dist.txt" #for future build MAZs
spOutsideRS8 = "outputs/SP_with_TAP_clean_outside_RS8_LU_Share_Dist.txt" #for future build MAZs

############################################################################

#intermediate settings
import os, csv, math, shutil
root = os.getcwd()
int_output = "outputs"
connectorEDist = 2 * SPThreshold
minTAPid = 2 * minTAPID
SPThresholdConn  = 3 * SPThreshold
SPThresholdAct = SPThreshold 
number_of_cores = int(os.environ["NUMBER_OF_PROCESSORS"])
linkFile_int = "outputs/cmap_NavTechLinks_no_freeway.txt" #without connectors
nodeFile_int = "outputs/cmap_NavTechNodes_no_freeway.txt" #without centroids
outputSPFile = "outputs/centToCentDist.txt"
tapConnectorFile = "outputs/tapConnectors.txt"
connectorFile = "outputs/CMAP_connectors.txt"
linkFile = "outputs/linkFile_Final.txt" #with connectors 
nodeFile = "outputs/nodeFile_Final.txt" #with centroids

############################################################################

def main():

  #copy itself to the scripts folder for other scripts to reference
  shutil.copyfile("runMAZSkimsInitial.py", "scripts/parameters.py")

  print("run initial network build procedures")
  from scripts import cmapTransportationNetwork
  from scripts import cmapTapConnector
  from scripts import cmapCentroidsConnectors
  from scripts import cmapInputFileGen
  if isBaseYear:
    from scripts import cmapShortestPath_NX
  else:
    from scripts import cmapShortestPath_NX_future
  
  print("read shapefile and generate csv file of navteq links")
  cmapTransportationNetwork.getLinkDirectionality()
  print("read shapefile and generate csv file of navteq nodes")
  cmapTransportationNetwork.createAllPoints()
  print("create MAZ centroid connectors")
  cmapCentroidsConnectors.createNetworkWithCentroidConnectors()
  print("create TAP connectors")
  cmapTapConnector.createNetworkWithCentroidConnectors()
  print("merge node and link files to one node and one link file containing all elements")
  cmapInputFileGen.createNetworkInput()
  if isBaseYear:
    print("pickle network for later use")
    cmapShortestPath_NX.pickleNetwork()
  else:
    print("pickle network for later use")
    cmapShortestPath_NX_future.pickleNetwork()
  print("initial shortest path network build complete")

if __name__ == '__main__':
    main()