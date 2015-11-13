package com.pb.models.ctrampIf.jppf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.node.protocol.Task;

import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.util.ResourceUtil;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.DestChoiceSize;
import com.pb.models.ctrampIf.HouseholdDataManagerIf;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.jppf.LocationChoiceTaskJppf;

public class UsualWorkSchoolLocationChoiceModel implements Serializable {
    
	
    private transient Logger logger = Logger.getLogger(UsualWorkSchoolLocationChoiceModel.class);

    private static final String PROPERTIES_DC_SOA_SAMPLE_SIZE        = "UsualWorkAndSchoolLocationChoice.SampleOfAlternatives.SampleSize";
    private static final String PROPERTIES_UEC_USUAL_LOCATION        = "UecFile.DestinationChoice";
    private static final String PROPERTIES_UEC_USUAL_LOCATION_SOA    = "UecFile.SampleOfAlternativesChoice";
    private static final String PROPERTIES_UEC_TOUR_MODE_CHOICE      = "UecFile.TourModeChoice";

    private static final String PROPERTIES_RESULTS_WORK_SCHOOL_LOCATION_CHOICE = "Results.UsualWorkAndSchoolLocationChoice";

    private static final String PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_PACKET_SIZE = "distributed.task.packet.size";
    
    private static int PACKET_SIZE = 0;

    private int ONE_HH_ID = -1;
    private static final String RUN_THIS_HOUSEHOLD_ONLY = "run.this.household.only";    
    
    
    // TODO: see if we can eliminate the setup synchronization issues - otherwise the number of these small
    // packets can be fine-tuned and set in properties file..
    
    private static final int NUM_WRITE_PACKETS = 1000;
    
    private String wsLocResultsFileName;


    private transient ResourceBundle resourceBundle;
    private String projectDirectory;

    private MatrixDataServerIf ms;
    private ModelStructure modelStructure;                                         
    private TazDataIf tazDataManager;
    private DestChoiceSize dcSizeObj;
    private CtrampDmuFactoryIf dmuFactory;

    private String dcUecFileName;
    private String soaUecFileName;
    private String modeChoiceUecFileName;
    private int soaSampleSize;

    private String restartModelString;
    
    private JPPFClient jppfClient = null;


    
    


    public UsualWorkSchoolLocationChoiceModel(ResourceBundle resourceBundle, String restartModelString, JPPFClient jppfClient, ModelStructure modelStructure, MatrixDataServerIf ms, TazDataIf tazDataManager, DestChoiceSize dcSizeObj, CtrampDmuFactoryIf dmuFactory ){
    	
    	// set the local variables
    	this.resourceBundle = resourceBundle;
    	this.modelStructure = modelStructure;
    	this.tazDataManager = tazDataManager;
    	this.dcSizeObj = dcSizeObj;
        this.dmuFactory = dmuFactory;
        this.ms = ms;
        this.jppfClient = jppfClient;
        this.restartModelString = restartModelString;

        try {
        	ONE_HH_ID = Integer.parseInt( resourceBundle.getString( RUN_THIS_HOUSEHOLD_ONLY ));
        } catch (MissingResourceException e) {
        	ONE_HH_ID=-1; 
        }
        if (ONE_HH_ID>=0) logger.info("UWSL run.this.household.only=" +  ONE_HH_ID); 
        
        
        try {
            PACKET_SIZE = Integer.parseInt( resourceBundle.getString( PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_PACKET_SIZE ) );
        }
        catch ( MissingResourceException e ){
            PACKET_SIZE = 0;
        }
            
        
        try {
            wsLocResultsFileName = resourceBundle.getString( PROPERTIES_RESULTS_WORK_SCHOOL_LOCATION_CHOICE );
        }
        catch (MissingResourceException e){
            wsLocResultsFileName = null;
        }


        projectDirectory = ResourceUtil.getProperty(resourceBundle, CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);

        // get the sample-of-alternatives sample size
        soaSampleSize = ResourceUtil.getIntegerProperty(resourceBundle, PROPERTIES_DC_SOA_SAMPLE_SIZE);

        // locate the UECs for destination choice, sample of alts, and mode choice
        String usualLocationUecFileName = ResourceUtil.getProperty(resourceBundle, PROPERTIES_UEC_USUAL_LOCATION);
        dcUecFileName = projectDirectory + usualLocationUecFileName;

        String usualLocationSoaUecFileName = ResourceUtil.getProperty(resourceBundle, PROPERTIES_UEC_USUAL_LOCATION_SOA);
        soaUecFileName = projectDirectory + usualLocationSoaUecFileName;

        String tourModeChoiceUecFileName = ResourceUtil.getProperty(resourceBundle, PROPERTIES_UEC_TOUR_MODE_CHOICE);
        modeChoiceUecFileName = projectDirectory + tourModeChoiceUecFileName;

    }
    


    public void runSchoolAndLocationChoiceModel (HouseholdDataManagerIf householdDataManager) {

        //runConstrainedLocationChoiceModelOriginal ( householdDataManager);
        runConstrainedLocationChoiceModel ( householdDataManager);

    }



    /**
     * 
     * JPPF framework based method
     * @param <T>
     */
    private <T> void runConstrainedLocationChoiceModel (HouseholdDataManagerIf householdDataManager) {

        // dimension an array to accumulate chosen long term model choices for use in shadow price adjustments
        String[] tourPurposeList = modelStructure.getDcModelPurposeList( ModelStructure.MANDATORY_CATEGORY );


        dcSizeObj.setupDestChoiceSize( resourceBundle, projectDirectory, ModelStructure.MANDATORY_CATEGORY );

        // calculate the size terms
        if ( ! dcSizeObj.getDcSizeCalculated() )
            dcSizeObj.calculateDcSize();


        int[][][] originLocationsByHomeZone = householdDataManager.getTourPurposePersonsByHomeZone( tourPurposeList );

        // balance the size variables
        int start = modelStructure.getDcSizeArrayCategoryIndexOffset( ModelStructure.MANDATORY_CATEGORY );
        int end = start + modelStructure.getNumDcSizeArrayCategorySegments( ModelStructure.MANDATORY_CATEGORY );
        dcSizeObj.balanceSizeVariables(originLocationsByHomeZone, tourPurposeList, start, end);
       
        
        HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(resourceBundle);
        
        


        if ( PACKET_SIZE == 0 )
            PACKET_SIZE = householdDataManager.getNumHouseholds();


    
        
        // set current iteration either to 0, or if a previously written shadow price file is specified,
        // to the iteration number set in that file name +1.
        int currentIter = 0;
        String fileName = propertyMap.get( CtrampApplication.PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE );
        if ( fileName != null ) {
            dcSizeObj.restoreShadowPricingInfo( projectDirectory + fileName );
            int underScoreIndex = fileName.lastIndexOf('_');
            int dotIndex = fileName.lastIndexOf('.');
            currentIter = Integer.parseInt( fileName.substring( underScoreIndex+1, dotIndex ) );
            currentIter++;
        }
        
        
//        String restartFlag = propertyMap.get( CtrampApplication.PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER );
//        if ( restartFlag == null )
//            restartFlag = "none";
//        if ( restartFlag.equalsIgnoreCase("none") )
//            currentIter = 0;

        
        
        long initTime = System.currentTimeMillis();

        
        // shadow pricing iterations
        for( int iter=0; iter < dcSizeObj.getMaxShadowPriceIterations(); iter++ ) {
           
            logger.info( "start of shadow pricing iter " + iter + ", free memory = " + Runtime.getRuntime().freeMemory() + ", total memory = " + Runtime.getRuntime().totalMemory() );

            //logger.info( String.format( "Size of Household[] in bytes = %d.", householdDataManager.getBytesUsedByHouseholdArray() ) );

            try {
                JPPFJob job = new JPPFJob();

                ArrayList<int[]> startEndTaskIndicesList = getTaskHouseholdRanges(householdDataManager, householdDataManager.getNumHouseholds() );
                
                DataProvider dataProvider = new MemoryMapDataProvider();
                dataProvider.setParameter("propertyMap", propertyMap);
                dataProvider.setParameter("ms", ms);
                dataProvider.setParameter("modelStructure", modelStructure);
                dataProvider.setParameter("tourCategory", ModelStructure.MANDATORY_CATEGORY);
                dataProvider.setParameter("tazDataManager", tazDataManager);
                dataProvider.setParameter("householdDataManager", householdDataManager);
                dataProvider.setParameter("dcSizeObj", dcSizeObj); 
                dataProvider.setParameter("dcUecFileName", dcUecFileName);
                dataProvider.setParameter("soaUecFileName", soaUecFileName);
                dataProvider.setParameter("soaSampleSize", soaSampleSize);
                dataProvider.setParameter("modeChoiceUecFileName", modeChoiceUecFileName);
                dataProvider.setParameter("dmuFactory", dmuFactory);
                dataProvider.setParameter("restartModelString", restartModelString);
                
                job.setDataProvider(dataProvider);
                
                int startIndex = 0;
                int endIndex = 0;
                int taskIndex = 1;
                LocationChoiceTaskJppf myTask = null;
                logger.info( "before creating tasks:  free memory = " + Runtime.getRuntime().freeMemory() + ", total memory = " + Runtime.getRuntime().totalMemory() );
                for ( int[] startEndIndices : startEndTaskIndicesList ) {
                    startIndex = startEndIndices[0];
                    endIndex = startEndIndices[1];

//                    myTask = new LocationChoiceTaskJppf( propertyMap, ms, householdDataManager, modelStructure, ModelStructure.MANDATORY_CATEGORY, tazDataManager, dcSizeObj,
//                            dcUecFileName, soaUecFileName, soaSampleSize, modeChoiceUecFileName, dmuFactory, startIndex, endIndex );
                    myTask = new LocationChoiceTaskJppf( taskIndex, startIndex, endIndex, currentIter );

                    
                    job.add ( myTask );
                    taskIndex++;
                }

                
                logger.info( "after creating tasks, free memory = " + Runtime.getRuntime().freeMemory() + ", total memory = " + Runtime.getRuntime().totalMemory() );

                List<Task<?>> results = jppfClient.submitJob(job);

                logger.info( "after getting results, free memory = " + Runtime.getRuntime().freeMemory() + ", total memory = " + Runtime.getRuntime().totalMemory() );

                for (Task<?> task : results) {
                    if (task.getException() != null) throw task.getException();

                    try {                       
                        String stringResult = (String)task.getResult();
                        logger.info( stringResult );
                    }
                    catch (Exception e) {
                        logger.error( "", e );
                        throw new RuntimeException();
                    }

                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            logger.info( "after results returned, free memory = " + Runtime.getRuntime().freeMemory() + ", total memory = " + Runtime.getRuntime().totalMemory() );
            
            
            // sum the chosen destinations by purpose, dest zone and subzone for shadow pricing adjustment
            double[][][] finalModeledDestChoiceLocationsByDestZone = householdDataManager.getMandatoryToursByDestZoneSubZone();

            double[] numChosenDests = new double[tourPurposeList.length];
            for( int i=0; i < tourPurposeList.length; i++) {
                String purposeString = tourPurposeList[i];
                int dcModelIndex = modelStructure.getDcModelPurposeIndex(purposeString);
                int dcSizeArrayIndex = modelStructure.getDcSizeArrayIndexFromDcModelIndex(dcModelIndex); 
                for( int k=1; k < finalModeledDestChoiceLocationsByDestZone[dcSizeArrayIndex].length; k++ ) {
                    for( int l=0; l < finalModeledDestChoiceLocationsByDestZone[dcSizeArrayIndex][k].length; l++ ) {
                        numChosenDests[i] += finalModeledDestChoiceLocationsByDestZone[dcSizeArrayIndex][k][l];
                    }
                }
            }
            
            logger.info( String.format("Usual work/school location choice tasks completed for shadow price iteration %d.", currentIter) );
            logger.info( String.format("Chosen dests by purpose:") );
            double total = 0;
            for( int i=0; i < tourPurposeList.length; i++) {
                String purposeString = tourPurposeList[i];
                logger.info( String.format("\t%-15s = %.0f", purposeString, numChosenDests[i]) );
                total += numChosenDests[i];
            }
            logger.info( String.format("\t%-15s = %.0f", "total", total) );
                
            
            logger.info( "Usual work and school location choices elapsed time " + ((System.currentTimeMillis() - initTime) / 1000) + " seconds."); ;
            
            
            // apply the shadow price adjustments
            dcSizeObj.reportMaxDiff(currentIter, finalModeledDestChoiceLocationsByDestZone, tourPurposeList);
            dcSizeObj.updateShadowPrices(finalModeledDestChoiceLocationsByDestZone, tourPurposeList);
            dcSizeObj.updateSizeVariables(tourPurposeList, start, end);
            dcSizeObj.updateShadowPricingInfo(currentIter, originLocationsByHomeZone, finalModeledDestChoiceLocationsByDestZone, tourPurposeList, start, end);
           
            householdDataManager.setUwslRandomCount( currentIter );

            logger.info( "end of shadow pricing iter " + iter + ", free memory = " + Runtime.getRuntime().freeMemory() + ", total memory = " + Runtime.getRuntime().totalMemory() );
            
            currentIter++;

        } // iter

        
        /*
        // done iterating, clear the dcModel objects following the final iteration.
        try {
            JPPFJob job = new JPPFJob();
            for ( int i=0; i < 100; i++ ){
                job.addTask( new LocationChoiceCleanupTaskJppf() );
            }
            List<JPPFTask> results = client.submit(job);
            for (JPPFTask task : results) {
                if (task.getException() != null) throw task.getException();

                try {                       
                    task.getResult();
                }
                catch (Exception e) {
                    logger.error( "", e );
                    throw new RuntimeException();
                }

            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        */
        
        
        //jppfClient.close();
        

        
        logger.info( "Usual work and school location choices computed in " + ((System.currentTimeMillis() - initTime) / 1000) + " seconds."); ;

   }



    /**
     * Loops through the households in the HouseholdDataManager, gets the households and persons
     *  and writes a row with detail on each of these in a file.
     *
     * @param householdDataManager is the object from which the array of household objects can be retrieved.
     * @param projectDirectory is the root directory for the output file named
     */
    public void saveResults( HouseholdDataManagerIf householdDataManager, String projectDirectory, int globalIteration ){

        FileWriter writer;
        PrintWriter outStream = null;

        if ( wsLocResultsFileName != null ) {

            // insert '_' and the global iteration number at end of filename or before '.' if there is a file extension in the name.
            int dotIndex = wsLocResultsFileName.indexOf('.');
            if ( dotIndex < 0 ) {
                wsLocResultsFileName = String.format( "%s_%d", wsLocResultsFileName, globalIteration );
            }
            else {
                String base = wsLocResultsFileName.substring( 0, dotIndex );
                String extension = wsLocResultsFileName.substring( dotIndex );
                wsLocResultsFileName = String.format( "%s_%d%s", base, globalIteration, extension );
            }
            
            
            
            wsLocResultsFileName = projectDirectory + wsLocResultsFileName;

            try {
                writer = new FileWriter(new File(wsLocResultsFileName));
                outStream = new PrintWriter (new BufferedWriter( writer ) );
            }
            catch(IOException e){
                logger.fatal( String.format( "Exception occurred opening wsLoc results file: %s.", wsLocResultsFileName) );
                throw new RuntimeException(e);
            }

            
            // write header
            outStream.println( "HHID,HomeTAZ,HomeSubZone,PersonID,PersonNum,PersonWorkOcc,PersonType,PersonAge,EmploymentCategory,StudentCategory,WorkLocIndex,WorkLocation,WorkSubZone,SchlLocIndex,SchoolLocation,SchoolSubZone");
            
            
            ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager, householdDataManager.getNumHouseholds() );

            for ( int[] startEndIndices : startEndTaskIndicesList ) {
            
                int startIndex = startEndIndices[0];
                int endIndex = startEndIndices[1];

                // get the array of households
                HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );

                for(int i=0; i < householdArray.length; ++i){

                    HouseholdIf household = householdArray[i];
                    
                    int hhId = household.getHhId();
                    int homeTaz = household.getHhTaz();
                    int homeSubzone = household.getHhWalkSubzone(); 

                    PersonIf[] personArray = household.getPersons();

                    for (int j = 1; j < personArray.length; ++j) {

                        PersonIf person = personArray[j];

                        int personId = person.getPersonId();
                        int personNum = person.getPersonNum();
                        String personType = person.getPersonType(); 
                        int personAge = person.getAge();
                        String employmentCategory = person.getPersonEmploymentCategory();
                        String studentCategory = person.getPersonStudentCategory();
                        int workLocIndex = person.getWorkLocationPurposeIndex();
                        int schlLocIndex = person.getSchoolLocationPurposeIndex();
                        int personWorkOcc = person.getPersonWorkerOccupation();
                        
                        int workLocation = person.getUsualWorkLocation();
                        int workSubZone = person.getPersonWorkLocationSubZone();

                        int schoolLocation = person.getUsualSchoolLocation();
                        int schoolSubZone = person.getPersonSchoolLocationSubZone();

                        // write data record
                        outStream.println( String.format("%d,%d,%d,%d,%d,%d,%s,%d,%s,%s,%d,%d,%d,%d,%d,%d" , 
                                    hhId, homeTaz, homeSubzone, personId, personNum, personWorkOcc, personType, personAge,
                                    employmentCategory, studentCategory,
                                    workLocIndex, workLocation, workSubZone, schlLocIndex, schoolLocation, schoolSubZone));

                    }

                }

            }
            

            outStream.close();
        }

    }


    private ArrayList<int[]> getTaskHouseholdRanges(HouseholdDataManagerIf hhDataManager, int numberOfHouseholds ) {
        
        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>(); 

        if ( ONE_HH_ID < 0 ) {
	            
	        int startIndex = 0;
	        int endIndex = 0;
	        while ( endIndex < numberOfHouseholds - 1 ) {
	            endIndex = startIndex + PACKET_SIZE - 1;
	            if ( endIndex + PACKET_SIZE > numberOfHouseholds )
	                endIndex = numberOfHouseholds - 1;
	        
	            int[] startEndIndices = new int[2];
	            startEndIndices[0] = startIndex; 
	            startEndIndices[1] = endIndex;
	            startEndIndexList.add( startEndIndices );
	            
	            startIndex += PACKET_SIZE;
	        }
	        
	        return startEndIndexList;
        }
        else {

            // create a single task packet high one household id
            int[] startEndIndices = new int[2];
            int index = hhDataManager.getArrayIndex( ONE_HH_ID );
            startEndIndices[0] = index; 
            startEndIndices[1] = index;
            startEndIndexList.add( startEndIndices );
            
            return startEndIndexList;

        }
        
    }

    private ArrayList<int[]> getWriteHouseholdRanges(HouseholdDataManagerIf hhDataManager, int numberOfHouseholds ) {
        
        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>(); 

        if ( ONE_HH_ID < 0 ) {
	            
	        int startIndex = 0;
	        int endIndex = 0;
	        
	        while ( endIndex < numberOfHouseholds - 1 ) {
	            endIndex = startIndex + NUM_WRITE_PACKETS - 1;
	            if ( endIndex + NUM_WRITE_PACKETS > numberOfHouseholds )
	                endIndex = numberOfHouseholds - 1;
	        
	            int[] startEndIndices = new int[2];
	            startEndIndices[0] = startIndex; 
	            startEndIndices[1] = endIndex;
	            startEndIndexList.add( startEndIndices );
	            
	            startIndex += NUM_WRITE_PACKETS;
	        }
	        
	        return startEndIndexList;
        }
        else {

            // create a single task packet high one household id
            int[] startEndIndices = new int[2];
            int index = hhDataManager.getArrayIndex( ONE_HH_ID );
            startEndIndices[0] = index; 
            startEndIndices[1] = index;
            startEndIndexList.add( startEndIndices );
            
            return startEndIndexList;

        }
        
    }

}
