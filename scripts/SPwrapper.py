#############################################################################
# Shortest Path code for CMAP non-motorized modes
# Roshan Kumar, kumarr1@pbworld.com, 12/20/2012
#############################################################################

#Should always be placed in the same folder as cmapPostProcess.py
#Wrapper code that distributes the shortest path computations to the different cores.
#The total # of parallel processes equal to the number of processors

import parameters, time
from multiprocessing import Process

if parameters.isBaseYear:
    import cmapShortestPath_NX
else:
    import cmapShortestPath_NX_future

################################################################

#parallel distribution of SP computation to different processors
def runSPinParallel():
    #alpha = 1
    if __name__ == '__main__':

        for num in range(parameters.number_of_cores):

            if parameters.isBaseYear:
                p = Process(target=cmapShortestPath_NX.cenToCenSP, args=(num,))
                p.start()
            else:
                p = Process(target=cmapShortestPath_NX_future.cenToCenSP, args=(num,))
                p.start()

################################################################

print "SP code start time:    ", time.time()
runSPinParallel()
