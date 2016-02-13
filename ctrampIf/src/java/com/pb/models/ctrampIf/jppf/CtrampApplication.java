package com.pb.models.ctrampIf.jppf;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.jppf.client.JPPFClient;

import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.Definitions;
import com.pb.models.ctrampIf.DestChoiceSize;
import com.pb.models.ctrampIf.HouseholdDataManagerIf;
import com.pb.models.ctrampIf.HouseholdDataWriter;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.MatrixDataServer;
import com.pb.models.ctrampIf.MatrixDataServerRmi;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.StopFrequencyDMU;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TourIf;
import com.pb.models.ctrampIf.TripMatrixWriterIf;
import com.pb.models.synpopV3.PopulationSynthesizer;




public class CtrampApplication implements Serializable {

     private Logger logger = Logger.getLogger(CtrampApplication.class);


     public static final String VERSION = "1.0";


     public static final int MATRIX_DATA_SERVER_PORT = 1171;



     public static final String PROPERTIES_BASE_NAME = "ctramp";
     public static final String PROPERTIES_PROJECT_DIRECTORY = "Project.Directory";
     public static final String PROPERTIES_UEC_PATH = "uec.path";
     
     public static final String SQLITE_DATABASE_FILENAME = "Sqlite.DatabaseFileName";

     public static final String PROPERTIES_RUN_POPSYN                                     = "RunModel.PopulationSynthesizer";
     public static final String PROPERTIES_RUN_WORKSCHOOL_CHOICE                          = "RunModel.UsualWorkAndSchoolLocationChoice";
     public static final String PROPERTIES_RUN_AUTO_OWNERSHIP                             = "RunModel.AutoOwnership";
     public static final String PROPERTIES_RUN_FREE_PARKING_AVAILABLE                     = "RunModel.FreeParking";
     public static final String PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN                     = "RunModel.CoordinatedDailyActivityPattern";
     public static final String PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ                  = "RunModel.IndividualMandatoryTourFrequency";
     public static final String PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR                 = "RunModel.MandatoryTourDepartureTimeAndDuration";
     public static final String PROPERTIES_RUN_MAND_TOUR_MODE_CHOICE                      = "RunModel.MandatoryTourModeChoice";
     public static final String PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ                       = "RunModel.AtWorkSubTourFrequency";
     public static final String PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE            = "RunModel.AtWorkSubTourLocationChoice";
     public static final String PROPERTIES_RUN_AT_WORK_SUBTOUR_MODE_CHOICE                = "RunModel.AtWorkSubTourModeChoice";
     public static final String PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR           = "RunModel.AtWorkSubTourDepartureTimeAndDuration";
     public static final String PROPERTIES_RUN_JOINT_TOUR_FREQ                            = "RunModel.JointTourFrequency";
     public static final String PROPERTIES_RUN_JOINT_LOCATION_CHOICE                      = "RunModel.JointTourLocationChoice";
     public static final String PROPERTIES_RUN_JOINT_TOUR_MODE_CHOICE                     = "RunModel.JointTourModeChoice";
     public static final String PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR                = "RunModel.JointTourDepartureTimeAndDuration";
     public static final String PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ              = "RunModel.IndividualNonMandatoryTourFrequency";
     public static final String PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE        = "RunModel.IndividualNonMandatoryTourLocationChoice";
     public static final String PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_MODE_CHOICE       = "RunModel.IndividualNonMandatoryTourModeChoice";
     public static final String PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR  = "RunModel.IndividualNonMandatoryTourDepartureTimeAndDuration";
     public static final String PROPERTIES_RUN_STOP_FREQUENCY                             = "RunModel.StopFrequency";
     public static final String PROPERTIES_RUN_STOP_LOCATION                              = "RunModel.StopLocation";


     public static final String PROPERTIES_UEC_AUTO_OWNERSHIP                = "UecFile.AutoOwnership";
     public static final String PROPERTIES_UEC_DAILY_ACTIVITY_PATTERN        = "UecFile.CoordinatedDailyActivityPattern";
     public static final String PROPERTIES_UEC_INDIV_MANDATORY_TOUR_FREQ     = "UecFile.IndividualMandatoryTourFrequency";
     public static final String PROPERTIES_UEC_MAND_TOUR_DEP_TIME_AND_DUR    = "UecFile.TourDepartureTimeAndDuration";
     public static final String PROPERTIES_UEC_INDIV_NON_MANDATORY_TOUR_FREQ = "UecFile.IndividualNonMandatoryTourFrequency";

     // TODO eventually move to model-specific structure object
     public static final int TOUR_MODE_CHOICE_WORK_MODEL_UEC_PAGE         = 1;
     public static final int TOUR_MODE_CHOICE_UNIVERSITY_MODEL_UEC_PAGE   = 2;
     public static final int TOUR_MODE_CHOICE_HIGH_SCHOOL_MODEL_UEC_PAGE  = 3;
     public static final int TOUR_MODE_CHOICE_GRADE_SCHOOL_MODEL_UEC_PAGE = 4;

     // TODO eventually move to model-specific model structure object
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_MODEL_UEC_PAGE     = 1;
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_DEPARTURE_UEC_PAGE = 2;
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_DURATION_UEC_PAGE  = 3;
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_WORK_ARRIVAL_UEC_PAGE   = 4;

     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_MODEL_UEC_PAGE     = 5;
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_DEPARTURE_UEC_PAGE = 6;
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_DURATION_UEC_PAGE  = 7;
     public static final int MANDATORY_TOUR_DEP_TIME_AND_DUR_SCHOOL_ARRIVAL_UEC_PAGE   = 8;

     public static final String PROPERTIES_SCHEDULING_NUMBER_OF_TIME_PERIODS = "Scheduling.NumberOfTimePeriods";
     public static final String PROPERTIES_SCHEDULING_FIRST_TIME_PERIOD = "Scheduling.FirstTimePeriod";

     static final String PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER = "RunModel.RestartWithHhServer";
     static final String PROPERTIES_REREAD_MATRIX_DATA_ON_RESTART = "RunModel.RereadMatrixDataOnRestart"; 
     
     public static final String PROPERTIES_RESULTS_AUTO_OWNERSHIP = "Results.AutoOwnership";
     public static final String PROPERTIES_RESULTS_CDAP = "Results.CoordinatedDailyActivityPattern";
     
     public static final String PROPERTIES_WRITE_DATA_TO_FILE = "Results.WriteDataToFiles";
     public static final String PROPERTIES_WRITE_DATA_TO_DATABASE = "Results.WriteDataToDatabase";
     public static final String PROPERTIES_WRITE_TRIP_MATRICES = "Results.WriteTripMatrices";

     public static final String PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS = "TourModeChoice.Save.UtilsAndProbs";

     public static final String PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE = "UsualWorkAndSchoolLocationChoice.ShadowPrice.Input.File";

     public static final String PROPERTIES_NUMBER_OF_GLOBAL_ITERATIONS = "Global.iterations";
     
     public static final String ALT_FIELD_NAME = "a";
     public static final String START_FIELD_NAME = "depart";
     public static final String END_FIELD_NAME = "arrive";

     
     private static final int NUM_WRITE_PACKETS = 2000;

     

     private boolean restartFromDiskObjectFile = false;
     
     private ResourceBundle resourceBundle;

     private MatrixDataServerIf ms;

     private ModelStructure modelStructure;
     private TazDataIf tazDataManager;
     protected String projectDirectory;
     protected String hhDiskObjectFile;
     protected String hhDiskObjectKey;
     protected String tazDiskObjectFile;
     protected String tazDiskObjectKey;

     private HashMap<Integer,HashMap<String,Integer>> cdapByHhSizeAndPattern;
     private HashMap<String,HashMap<String,Integer>> cdapByPersonTypeAndActivity;

     
     
     
     
     public CtrampApplication( ResourceBundle rb ){
         resourceBundle = rb;
         projectDirectory = ResourceUtil.getProperty(resourceBundle, PROPERTIES_PROJECT_DIRECTORY);
     }



     public void setupModels( ModelStructure modelStructure, TazDataIf tazDataManager ){

         this.modelStructure = modelStructure;
         this.tazDataManager = tazDataManager;

     }

     
     public void runPopulationSynthesizer( PopulationSynthesizer populationSynthesizer ){

         // run population synthesizer
         boolean runModelPopulationSynthesizer = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_POPSYN);
         if(runModelPopulationSynthesizer){
             populationSynthesizer.runPopulationSynthesizer();
         }

     }

     //run models without a trip matrix writer
     public void runModels( HouseholdDataManagerIf householdDataManager, CtrampDmuFactoryIf dmuFactory, int globalIterationNumber) {
    	 runModels(householdDataManager, dmuFactory, globalIterationNumber, null);
     }

     public void runModels( HouseholdDataManagerIf householdDataManager, CtrampDmuFactoryIf dmuFactory, int globalIterationNumber, TripMatrixWriterIf tripMatrixWriter ){

         logger.info("Running JPPF based CtrampApplication.runModels() with " + householdDataManager.getNumHouseholds() + " households.");
         
         String matrixServerAddress = "";
         int serverPort = 0;
         try {
             // get matrix server address.  if "none" is specified, no server will be started, and matrix io will ocurr within the current process.
             matrixServerAddress = resourceBundle.getString( "RunModel.MatrixServerAddress" );
             try {
                 // get matrix server port.
                 serverPort = Integer.parseInt( resourceBundle.getString( "RunModel.MatrixServerPort" ) );
             }
             catch ( MissingResourceException e ) {
                 // if no matrix server address entry is found, leave undefined -- it's eithe not needed or show could create an error.
             }
         }
         catch ( MissingResourceException e ) {
             // if no matrix server address entry is found, set to localhost, and a separate matrix io process will be started on localhost.
             matrixServerAddress = "localhost";
             serverPort = MATRIX_DATA_SERVER_PORT;
         }


         MatrixDataServer matrixServer = null;

         try {

             if ( ! matrixServerAddress.equalsIgnoreCase("none") ) {

                 if ( matrixServerAddress.equalsIgnoreCase("localhost") ) {
                     matrixServer = startMatrixServerProcess( matrixServerAddress, serverPort );
                     ms = matrixServer;
                 }
                 else {
                     MatrixDataServerRmi mds = new MatrixDataServerRmi( matrixServerAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME );
                     ms = mds;

                     boolean rereadMatrixDataOnRestart = ResourceUtil.getBooleanProperty( resourceBundle, PROPERTIES_REREAD_MATRIX_DATA_ON_RESTART, true);
                     if (rereadMatrixDataOnRestart) ms.clear();
                     ms.start32BitMatrixIoServer( MatrixType.TPPLUS );
                     
                     MatrixDataManager mdm = MatrixDataManager.getInstance();
                     mdm.setMatrixDataServerObject( ms );
                     
                 }

             }


         }
         catch ( Exception e ) {

             if ( matrixServerAddress.equalsIgnoreCase("localhost") ) {
                 matrixServer.stop32BitMatrixIoServer();
             }
             logger.error ( String.format( "exception caught running ctramp model components -- exiting." ), e );
             throw new RuntimeException();

         }
         
         
         

         // run core activity based model for the specified iteration
         runIteration( globalIterationNumber, householdDataManager, dmuFactory, tripMatrixWriter);

         
         
         // if a separate process for running matrix data mnager was started, we're done with it, so close it. 
         if ( matrixServerAddress.equalsIgnoreCase("localhost") ) {
             matrixServer.stop32BitMatrixIoServer();
         }
         else {
             if ( ! matrixServerAddress.equalsIgnoreCase("none") )
                 ms.stop32BitMatrixIoServer();
         }

     }



     private void runIteration( int iteration, HouseholdDataManagerIf householdDataManager, CtrampDmuFactoryIf dmuFactory, TripMatrixWriterIf tripMatrixWriter) {
         
         String restartModel = "";
         if ( hhDiskObjectKey != null && ! hhDiskObjectKey.equalsIgnoreCase("none") ) {
             /*
             String doFileName = hhDiskObjectFile + "_" + hhDiskObjectKey;
             householdDataManager.createHhArrayFromSerializedObjectInFile( doFileName, hhDiskObjectKey );
             restartModel = hhDiskObjectKey;
             restartModels ( householdDataManager );
             */
         }
         else {
             restartModel = ResourceUtil.getProperty( resourceBundle, PROPERTIES_RESTART_WITH_HOUSEHOLD_SERVER );
             if ( restartModel == null )
                 restartModel = "none";
             if ( ! restartModel.equalsIgnoreCase("none") )
                 restartModels ( householdDataManager );
         }



         JPPFClient jppfClient = new JPPFClient();
         
         boolean runUsualWorkSchoolChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_WORKSCHOOL_CHOICE);
         if(runUsualWorkSchoolChoiceModel){

             // create an object for calculating destination choice attraction size terms and managing shadow price calculations.
             DestChoiceSize dcSizeObj = new DestChoiceSize( modelStructure, tazDataManager );
             
             // new the usual school and location choice model object
             UsualWorkSchoolLocationChoiceModel usualWorkSchoolLocationChoiceModel = new UsualWorkSchoolLocationChoiceModel(resourceBundle, restartModel, jppfClient, modelStructure, ms, tazDataManager, dcSizeObj, dmuFactory );
    
             // run the model
             logger.info ( "starting usual work and school location choice.");
             usualWorkSchoolLocationChoiceModel.runSchoolAndLocationChoiceModel(householdDataManager);
             logger.info ( "finished with usual work and school location choice.");
             
             logger.info ( "writing work/school location choice results file; may take a few minutes ..." );
             usualWorkSchoolLocationChoiceModel.saveResults( householdDataManager, projectDirectory, iteration );
             logger.info ( String.format("finished writing results file.") );

             usualWorkSchoolLocationChoiceModel = null;
             dcSizeObj = null;

         }
      
         

         boolean runAutoOwnershipChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AUTO_OWNERSHIP );
         boolean runFreeParkingChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_FREE_PARKING_AVAILABLE );
         boolean runCoordinatedDailyActivityPatternChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN );
         boolean runMandatoryTourFreqChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ );
         boolean runMandatoryTourTimeOfDayChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR );
         boolean runMandatoryTourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_MAND_TOUR_MODE_CHOICE );
         boolean runJointTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_TOUR_FREQ );
         boolean runJointTourLocationChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_LOCATION_CHOICE );
         boolean runJointTourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_TOUR_MODE_CHOICE );
         boolean runJointTourDepartureTimeAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR );
         boolean runIndivNonManTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ );
         boolean runIndivNonManTourLocationChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE );
         boolean runIndivNonManTourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_MODE_CHOICE );
         boolean runIndivNonManTourDepartureTimeAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR );
         boolean runAtWorkSubTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ );
         boolean runAtWorkSubtourLocationChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE );
         boolean runAtWorkSubtourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_MODE_CHOICE );
         boolean runAtWorkSubtourDepartureTimeAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR );
         boolean runStopFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_STOP_FREQUENCY );
         boolean runStopLocationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_STOP_LOCATION );


         boolean runHouseholdModels = false;
         if (
             runAutoOwnershipChoiceModel
             || runFreeParkingChoiceModel
             || runCoordinatedDailyActivityPatternChoiceModel
             || runMandatoryTourFreqChoiceModel
             || runMandatoryTourModeChoiceModel
             || runMandatoryTourTimeOfDayChoiceModel
             || runJointTourFrequencyModel 
             || runJointTourLocationChoiceModel
             || runJointTourModeChoiceModel
             || runJointTourDepartureTimeAndDurationModel
             || runIndivNonManTourFrequencyModel 
             || runIndivNonManTourLocationChoiceModel
             || runIndivNonManTourModeChoiceModel
             || runIndivNonManTourDepartureTimeAndDurationModel
             || runAtWorkSubTourFrequencyModel
             || runAtWorkSubtourLocationChoiceModel
             || runAtWorkSubtourModeChoiceModel
             || runAtWorkSubtourDepartureTimeAndDurationModel
             || runStopFrequencyModel
             || runStopLocationModel
                 )
                     runHouseholdModels = true;
         
         
         
         // disk object file is labeled with the next component eligible to be run if model restarted
         String lastComponent = "uwsl";
         String nextComponent = "ao";
         
         if( runHouseholdModels ) {
             
             logger.info ( "starting HouseholdChoiceModelRunner." );     
             HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(resourceBundle);
             HouseholdChoiceModelRunner runner = new HouseholdChoiceModelRunner( propertyMap, jppfClient, restartModel, householdDataManager, ms, modelStructure, tazDataManager, dmuFactory );
             runner.runHouseholdChoiceModels();
    
             if( runAutoOwnershipChoiceModel ){
                 saveAoResults( householdDataManager, projectDirectory );
                 logAoResults( householdDataManager );
                 lastComponent = "ao";
                 nextComponent = "fp";
             }
             
             if( runFreeParkingChoiceModel ){
                 logFpResults( householdDataManager );
                 lastComponent = "fp";
                 nextComponent = "cdap";
             }
             
             if( runCoordinatedDailyActivityPatternChoiceModel ){
                 saveCdapResults( householdDataManager, projectDirectory );
                 logCdapResults( householdDataManager );
                 lastComponent = "cdap";
                 nextComponent = "imtf";
             }
             
             if( runMandatoryTourFreqChoiceModel ){
                 logImtfResults( householdDataManager );
                 lastComponent = "imtf";
                 nextComponent = "imtod";
             }
             
             if( runMandatoryTourTimeOfDayChoiceModel || runMandatoryTourModeChoiceModel ){
                 lastComponent = "imtod";
                 nextComponent = "jtf";
             }
             
             if( runJointTourFrequencyModel ){
                 logJointModelResults( householdDataManager );
                 lastComponent = "jtf";
                 nextComponent = "jtl";
             }             
             
             if( runJointTourLocationChoiceModel ){
                 lastComponent = "jtl";
                 nextComponent = "jtod";
             }             
             
             if( runJointTourDepartureTimeAndDurationModel || runJointTourModeChoiceModel ){
                 lastComponent = "jtod";
                 nextComponent = "inmtf";
             }             
             
             if( runIndivNonManTourFrequencyModel ){
                 lastComponent = "inmtf";
                 nextComponent = "inmtl";
             }             
             
             if( runIndivNonManTourLocationChoiceModel ){
                 lastComponent = "inmtl";
                 nextComponent = "inmtod";
             }             
             
             if( runIndivNonManTourDepartureTimeAndDurationModel || runIndivNonManTourModeChoiceModel ){
                 lastComponent = "inmtod";
                 nextComponent = "awf";
             }             
             
             if( runAtWorkSubTourFrequencyModel ){
                 logAtWorkSubtourFreqResults( householdDataManager );
                 lastComponent = "awf";
                 nextComponent = "awl";
             }
             
             if( runAtWorkSubtourLocationChoiceModel ){
                 lastComponent = "awl";
                 nextComponent = "awtod";
             }
             
             if( runAtWorkSubtourDepartureTimeAndDurationModel || runAtWorkSubtourModeChoiceModel ){
                 lastComponent = "awtod";
                 nextComponent = "stf";
             }
             
             if( runStopFrequencyModel ){
                 logStfResults( householdDataManager, true);  //individual
                 //logStfResults( householdDataManager, false ); //joint
                 lastComponent = "stf";
                 nextComponent = "stl";
             }             
             
             if( runStopLocationModel ){
                 lastComponent = "stl";
                 nextComponent = "done";
             }             
             
             
             
             // write a disk object fle for the householdDataManager, in case we want to restart from the next step.
             if ( hhDiskObjectFile != null && ! lastComponent.equalsIgnoreCase("uwsl") ) {
                 /*
                 logger.info ( String.format("writing household disk object file after %s choice model; may take a long time ...", lastComponent) );
                 String hhFileName = hhDiskObjectFile + "_" + nextComponent;
                 householdDataManager.createSerializedHhArrayInFileFromObject( hhFileName, nextComponent );
                 logger.info ( String.format("finished writing household disk object file = %s.", hhFileName) );
                 */
             }
             
             logger.info ( "finished with HouseholdChoiceModelRunner." );         

         }
         

         
         
         
         boolean writeTextFileFlag = false;
         boolean writeSqliteFlag = false;
         boolean writeTripMatricesFlag = false;
         try {
             writeTextFileFlag = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_WRITE_DATA_TO_FILE);
         }
         catch ( MissingResourceException e ) {
             // if exception is caught while getting property file value, then boolean flag remains false
         }
         try {
             writeSqliteFlag = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_WRITE_DATA_TO_DATABASE);
         }
         catch ( MissingResourceException e ) {
             // if exception is caught while getting property file value, then boolean flag remains false
         }
         try {
        	 writeTripMatricesFlag = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_WRITE_TRIP_MATRICES);
         }
         catch ( MissingResourceException e ) {
             // if exception is caught while getting property file value, then boolean flag remains false
         }

         //write output text files and/or SQLite database
         HouseholdDataWriter dataWriter = null;
         if ( writeTextFileFlag || writeSqliteFlag ) {
             dataWriter = new HouseholdDataWriter( resourceBundle, modelStructure, tazDataManager, dmuFactory, iteration );

             if ( writeTextFileFlag )
                dataWriter.writeDataToFiles(householdDataManager);
             
             if ( writeSqliteFlag ) {
                 String dbFilename = "";
                 try {
                     String baseDir = resourceBundle.getString(PROPERTIES_PROJECT_DIRECTORY);
                     dbFilename = baseDir + resourceBundle.getString(SQLITE_DATABASE_FILENAME) + "_" + iteration;
                     dataWriter.writeDataToDatabase(householdDataManager, dbFilename);
                 }
                 catch ( MissingResourceException e ) {
                     // if exception is caught while getting property file value, then boolean flag remains false
                 }
             }
         }
         
         //write trip matrices
         if(writeTripMatricesFlag) {
        	 tripMatrixWriter.writeMatrices();
         }
         
     }
     
     
     
     
     public String getProjectDirectoryName() {
        return projectDirectory;
     }


     private MatrixDataServer startMatrixServerProcess( String serverAddress, int serverPort ) {

         String className = MatrixDataServer.MATRIX_DATA_SERVER_NAME;

         MatrixDataServer matrixServer = new MatrixDataServer();

         try {

             // create the concrete data server object
             matrixServer.start32BitMatrixIoServer( MatrixType.TPPLUS );

         }
         catch ( RuntimeException e ) {
             matrixServer.stop32BitMatrixIoServer();
             logger.error ( "RuntimeException caught in com.pb.models.ctramp.MatrixDataServer.main() -- exiting.", e );
         }

         // bind this concrete object with the cajo library objects for managing RMI
         try {
             Remote.config( serverAddress, serverPort, null, 0 );
         }
         catch ( UnknownHostException e ) {
             logger.error ( String.format( "UnknownHostException. serverAddress = %s, serverPort = %d -- exiting.", serverAddress, serverPort ), e );
             matrixServer.stop32BitMatrixIoServer();
             throw new RuntimeException();
         }

         try {
             ItemServer.bind( matrixServer, className );
         }
         catch ( RemoteException e ) {
             logger.error ( String.format( "RemoteException. serverAddress = %s, serverPort = %d -- exiting.", serverAddress, serverPort ), e );
             matrixServer.stop32BitMatrixIoServer();
             throw new RuntimeException();
         }

         return matrixServer;

     }

     
     public boolean restartFromDiskObjectFile() {
         return restartFromDiskObjectFile;
     }


     public void restartModels ( HouseholdDataManagerIf householdDataManager ) {

         // if no filename was specified for the previous shadow price info, restartIter == -1.
         // also, random counts will be reset to 1.
         int restartIter = -1;
         String fileName = ResourceUtil.getProperty( resourceBundle, PROPERTIES_WORK_SCHOOL_LOCATION_CHOICE_SHADOW_PRICE_INPUT_FILE );
         if ( fileName != null ) {
             fileName = projectDirectory + fileName;
             int underScoreIndex = fileName.lastIndexOf('_');
             int dotIndex = fileName.lastIndexOf('.');
             restartIter = Integer.parseInt( fileName.substring( underScoreIndex+1, dotIndex ) );
         }
         
         boolean runUsualWorkSchoolChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_WORKSCHOOL_CHOICE);
         if ( runUsualWorkSchoolChoiceModel ) {
             // the input file name should have indicated the iteration number prior to the iteration number desired to run.
             // e.g. if the filename contains "..._4.csv", then the model should restart with uwsl iteration 5, using the random
             // number count set at the end of iteration 4, stored in the HashMap with key of 4.
             householdDataManager.resetUwslRandom( restartIter );
         }
         else {
        	 boolean runAutoOwnershipModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AUTO_OWNERSHIP);
        	 if ( runAutoOwnershipModel ) {
        		 // if restarting with ao, meaning not running uwsl, the shadow price input file should also be set to indicate
        		 // the last shadow price iteration number from which the random number count should be taked to reset the ao count.
        		 // e.g. if the filename contains "..._5.csv", then the model should reset the ar random count to that following uwsl iteration 5
        		 householdDataManager.resetAoRandom( restartIter );
        	 }
        	 else {
        		 boolean runFreeParkingAvailableModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_FREE_PARKING_AVAILABLE);
        		 if ( runFreeParkingAvailableModel  ) {
        			 householdDataManager.resetFpRandom();
        		 }
        		 else {
        			 boolean runCoordinatedDailyActivityPatternModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_DAILY_ACTIVITY_PATTERN);
        			 if ( runCoordinatedDailyActivityPatternModel  ) {
        				 householdDataManager.resetCdapRandom();
        			 }
        			 else {
        				 boolean runIndividualMandatoryTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_MANDATORY_TOUR_FREQ);
        				 if ( runIndividualMandatoryTourFrequencyModel  ) {
        					 householdDataManager.resetImtfRandom();
        				 }
        				 else {
        					 boolean runIndividualMandatoryTourDepartureAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_MAND_TOUR_DEP_TIME_AND_DUR);
        					 if ( runIndividualMandatoryTourDepartureAndDurationModel  ) {
        						 householdDataManager.resetImtodRandom();
        					 }

        					 else {
        						 boolean runIndividualMandatoryTourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_MAND_TOUR_MODE_CHOICE);
        						 if ( runIndividualMandatoryTourModeChoiceModel  ) {
        							 householdDataManager.resetImmcRandom();
        						 }
        						 else {
        							 boolean runJointTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_TOUR_FREQ);
        							 if ( runJointTourFrequencyModel  ) {
        								 householdDataManager.resetJtfRandom();
        							 }
        							 else {
        								 boolean runJointTourLocationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_LOCATION_CHOICE);
        								 if ( runJointTourLocationModel  ) {
        									 householdDataManager.resetJtlRandom();
        								 }
        								 else {
        									 boolean runJointTourDepartureAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_TOUR_DEP_TIME_AND_DUR);
        									 if ( runJointTourDepartureAndDurationModel  ) {
        										 householdDataManager.resetJtodRandom();
        									 }
        									 else {
        										 boolean runJointTourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_JOINT_TOUR_MODE_CHOICE);
        										 if ( runJointTourModeChoiceModel  ) {
        											 householdDataManager.resetJmcRandom();
        										 }
        										 else {
        											 boolean runIndividualNonMandatoryTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_FREQ);
        											 if ( runIndividualNonMandatoryTourFrequencyModel  ) {
        												 householdDataManager.resetInmtfRandom();
        											 }
        											 else {
        												 boolean runIndividualNonMandatoryTourLocationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_LOCATION_CHOICE);
        												 if ( runIndividualNonMandatoryTourLocationModel  ) {
        													 householdDataManager.resetInmtlRandom();
        												 }
        												 else {
        													 boolean runIndividualNonMandatoryTourDepartureAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_DEP_TIME_AND_DUR);
        													 if ( runIndividualNonMandatoryTourDepartureAndDurationModel  ) {
        														 householdDataManager.resetInmtodRandom();
        													 }

        													 else {
        														 boolean runIndividualNonMandatoryModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_INDIV_NON_MANDATORY_TOUR_MODE_CHOICE);
        														 if ( runIndividualNonMandatoryModeChoiceModel  ) {
        															 householdDataManager.resetInmmcRandom();
        														 }
        														 else {
        															 boolean runAtWorkSubTourFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_FREQ);
        															 if ( runAtWorkSubTourFrequencyModel  ) {
        																 householdDataManager.resetAwfRandom();
        															 }
        															 else {
        																 boolean runAtWorkSubtourLocationChoiceModel = ResourceUtil.getBooleanProperty( resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_LOCATION_CHOICE );
        																 if ( runAtWorkSubtourLocationChoiceModel  ) {
        																	 householdDataManager.resetAwlRandom();
        																 }
        																 else {
        																	 boolean runAtWorkSubtourDepartureTimeAndDurationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_DEP_TIME_AND_DUR);
        																	 if ( runAtWorkSubtourDepartureTimeAndDurationModel  ) {
        																		 householdDataManager.resetAwtodRandom();
        																	 }
        																	 else {
        																		 boolean runAtWorkSubtourModeChoiceModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_AT_WORK_SUBTOUR_MODE_CHOICE);
        																		 if ( runAtWorkSubtourModeChoiceModel  ) {
        																			 householdDataManager.resetAwmcRandom();
        																		 }
        																		 else {
        																			 boolean runStopFrequencyModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_STOP_FREQUENCY);
        																			 if ( runStopFrequencyModel  ) {
        																				 householdDataManager.resetStfRandom();
        																			 }
        																			 else {
        																				 boolean runStopLocationModel = ResourceUtil.getBooleanProperty(resourceBundle, PROPERTIES_RUN_STOP_LOCATION);
        																				 if ( runStopLocationModel  ) {
        																					 householdDataManager.resetStlRandom();
        																				 }
        																			 }
        																		 }
        																	 }
        																 }
        															 }
        														 }
        													 }
        												 }
        											 }
        										 }
        									 }
        								 }
        							 }
        						 }
        					 }
        				 }
        			 }
        		 }
        	 }
         }
     }


     /**
      * Loops through the households in the HouseholdDataManager, gets the auto ownership
      * result for each household, and writes a text file with hhid and auto ownership.
      *
      * @param householdDataManager is the object from which the array of household objects can be retrieved.
      * @param projectDirectory is the root directory for the output file named
      */
     private void saveAoResults(HouseholdDataManagerIf householdDataManager, String projectDirectory){

         String aoResultsFileName;
         try {
             aoResultsFileName = resourceBundle.getString( PROPERTIES_RESULTS_AUTO_OWNERSHIP );
         }
         catch ( MissingResourceException e ) {
             // if filename not specified in properties file, don't need to write it.
             return;
         }

         
         FileWriter writer;
         PrintWriter outStream = null;
         if ( aoResultsFileName != null ) {

             aoResultsFileName = projectDirectory + aoResultsFileName;

             try {
                 writer = new FileWriter(new File(aoResultsFileName));
                 outStream = new PrintWriter (new BufferedWriter( writer ) );
             }
             catch(IOException e){
                 logger.fatal( String.format( "Exception occurred opening AO results file: %s.", aoResultsFileName ) );
                 throw new RuntimeException(e);
             }


             outStream.println ( "HHID,AO" );
             
             
             ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

             for ( int[] startEndIndices : startEndTaskIndicesList ) {
             
                 int startIndex = startEndIndices[0];
                 int endIndex = startEndIndices[1];

             
                 // get the array of households
                 HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
        
                 for(int i=0; i < householdArray.length; ++i){
        
                     HouseholdIf household = householdArray[i];
                     int hhid = household.getHhId();
                     int ao = household.getAutoOwnershipModelResult();
        
                     outStream.println( String.format( "%d,%d", hhid, ao ) );
        
                 }
        
             }
             
             outStream.close();

         }

     }


     private void logAoResults( HouseholdDataManagerIf householdDataManager ){
         
         String[] aoCategoryLabel = { "0 autos", "1 auto", "2 autos", "3 autos", "4 or more autos" };
         
         logger.info( "" );
         logger.info( "" );
         logger.info( "Auto Ownership Model Results" );
         logger.info( String.format("%-16s  %10s", "Category", "Num HHs" ));
         
         
         
         // track the results
         int[] hhsByAutoOwnership;
         hhsByAutoOwnership = new int[aoCategoryLabel.length];


         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];

         
             // get the array of households
             HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
    
             for(int i=0; i < householdArray.length; ++i){
    
                 HouseholdIf household = householdArray[i];
                 int ao = household.getAutoOwnershipModelResult();
                 
                 if ( ao >= aoCategoryLabel.length )
                     ao = aoCategoryLabel.length - 1;
                 
                 hhsByAutoOwnership[ao]++;
    
             }
    
    
         }
         
         
         int total = 0;
         for (int i=0; i < hhsByAutoOwnership.length; i++) {
             logger.info( String.format("%-16s  %10d", aoCategoryLabel[i], hhsByAutoOwnership[i] ));
             total += hhsByAutoOwnership[i];
         }
         logger.info( String.format("%-16s  %10d", "Total", total ));
         
     }


     private void logFpResults( HouseholdDataManagerIf householdDataManager ){
         
         String[] fpCategoryLabel = { "Free Available", "Must Pay" };
         
         logger.info( "" );
         logger.info( "" );
         logger.info( "Free Parking Model Results" );
         logger.info( String.format("%-16s  %10s", "Category", "Num Persons" ));
         
         
         
         // track the results
         int[] personsByFreeParking;
         personsByFreeParking = new int[fpCategoryLabel.length];


         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];

         
             // get the array of households
             HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
    
             for(int i=0; i < householdArray.length; ++i){
    
            	 // note that person is a 1-based array
                 HouseholdIf household = householdArray[i];
                 PersonIf[] person = household.getPersons(); 
                 for (int j=1; j<person.length; j++) {
                     int fp = person[j].getFreeParkingAvailableResult() - 1;
                     
                     if ( fp >= 0 ) {
                         personsByFreeParking[fp]++;
                     }
                     
                 }
             }
    
    
         }
         
         
         int total = 0;
         for (int i=0; i < personsByFreeParking.length; i++) {
             logger.info( String.format("%-16s  %10d", fpCategoryLabel[i], personsByFreeParking[i] ));
             total += personsByFreeParking[i];
         }
         logger.info( String.format("%-16s  %10d", "Total", total ));
         
     }


     /**
      * Records the coordinated daily activity pattern model results to the logger. A household-level
      * summary simply records each pattern type and a person-level summary summarizes the activity
      * choice by person type (full-time worker, university student, etc). 
      *
      */
     public void logCdapResults( HouseholdDataManagerIf householdDataManager ){                

         String[] activityNameArray = { Definitions.MANDATORY_PATTERN, Definitions.NONMANDATORY_PATTERN, Definitions.HOME_PATTERN };

         getLogReportSummaries( householdDataManager );
         
         
         
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info("Coordinated Daily Activity Pattern Model Results");
         
         // count of activities by person type
         logger.info(" ");
         logger.info("CDAP Results: Count of activities by person type");
         String firstHeader  = "Person type                    ";
         String secondHeader = "-----------------------------  ";
         for(int i=0;i<activityNameArray.length;++i){
             firstHeader  += "        " + activityNameArray[i] + " ";
             secondHeader += "--------- ";
         }

         firstHeader  += "    Total";
         secondHeader += "---------";

         logger.info(firstHeader);
         logger.info(secondHeader);

         int[] columnTotals = new int[activityNameArray.length];

         for(int i=0;i<PersonIf.personTypeNameArray.length;++i){
             String personType = PersonIf.personTypeNameArray[i];
             String stringToLog  = String.format("%-30s", personType);
             int lineTotal = 0;

             if ( cdapByPersonTypeAndActivity.containsKey(personType) ) {
                 
                 for(int j=0;j<activityNameArray.length;++j){
                     int count = 0;
                     if( cdapByPersonTypeAndActivity.get(personType).containsKey(activityNameArray[j]) ) {
                         count = cdapByPersonTypeAndActivity.get(personType).get(activityNameArray[j]);
                     }
                     stringToLog += String.format("%10d",count);

                     lineTotal += count;
                     columnTotals[j] += count;
                 } // j
                 
             } // if key
             
             stringToLog += String.format("%10d",lineTotal);
             logger.info(stringToLog);
             
         } // i

         logger.info(secondHeader);

         String stringToLog  = String.format("%-30s", "Total");
         int lineTotal = 0;
         for(int j=0;j<activityNameArray.length;++j){
             stringToLog += String.format("%10d",columnTotals[j]);
             lineTotal += columnTotals[j];
         } // j

         stringToLog += String.format("%10d",lineTotal);
         logger.info(stringToLog);


         // count of patterns
         logger.info(" ");
         logger.info(" ");
         logger.info("CDAP Results: Count of patterns");
         logger.info("Pattern                Count");
         logger.info("------------------ ---------");
         
         // sort the map by hh size first
         Set<Integer> hhSizeKeySet = cdapByHhSizeAndPattern.keySet();
         Integer[] hhSizeKeyArray = new Integer[hhSizeKeySet.size()];
         hhSizeKeySet.toArray(hhSizeKeyArray);
         Arrays.sort(hhSizeKeyArray);

         int total = 0;
         for(int i=0;i<hhSizeKeyArray.length;++i){
             
             // sort the patterns alphabetically
             HashMap<String,Integer> patternMap = cdapByHhSizeAndPattern.get(hhSizeKeyArray[i]);
             Set<String> patternKeySet = patternMap.keySet();
             String[] patternKeyArray = new String[patternKeySet.size()];
             patternKeySet.toArray(patternKeyArray);
             Arrays.sort(patternKeyArray);
             for(int j=0;j<patternKeyArray.length;++j){
                 int count = patternMap.get(patternKeyArray[j]);
                 total += count;
                 logger.info(String.format("%-18s%10d",patternKeyArray[j],count));
             }
             
         }
         
         logger.info("------------------ ---------");
         logger.info(String.format("%-18s%10d","Total",total));
         logger.info(" ");

         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info(" ");
         logger.info(" ");
         
     }

     
     /**
      * Logs the results of the individual mandatory tour frequency model.
      *
      */
     public void logImtfResults( HouseholdDataManagerIf householdDataManager ){
         
         String[] choiceResults = {"1 Work", "2 Work", "1 School", "2 School", "Wrk & Schl"};
         
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info("Individual Mandatory Tour Frequency Model Results");
         
         // count of model results
         logger.info(" ");
         String firstHeader  = "Person type                   ";
         String secondHeader = "-----------------------------  ";
         
         
         // summarize results
         HashMap<String,int[]> countByPersonType = new HashMap<String,int[]>();
         
         
         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];

         
             // get the array of households
             HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
    
             for(int i=0; i < householdArray.length; ++i){
    
                 PersonIf[] personArray = householdArray[i].getPersons();
                 for(int j=1; j < personArray.length; j++){
                 
                     // only summarize persons with mandatory pattern
                     String personActivity = personArray[j].getCdapActivity();
                     if ( personActivity != null && personArray[j].getCdapActivity().equalsIgnoreCase("M") ) {
                     
                         String personTypeString = personArray[j].getPersonType();
                         int choice = personArray[j].getImtfChoice();
                         
                         // count the results
                         if(countByPersonType.containsKey(personTypeString)){
    
                             int[] counterArray = countByPersonType.get(personTypeString);
                             counterArray[choice-1]++;
                             countByPersonType.put(personTypeString, counterArray);
    
                         }
                         else{
    
                             int[] counterArray = new int[choiceResults.length];
                             counterArray[choice-1]++;
                             countByPersonType.put(personTypeString, counterArray);
    
                         }
                     }
                     
                 }    
    
             }
    
    
         }
         
         
         
         for(int i=0;i<choiceResults.length;++i){
             firstHeader  += String.format("%12s",choiceResults[i]);
             secondHeader += "----------- ";
         }
         
         firstHeader  += String.format("%12s","Total");
         secondHeader += "-----------";

         logger.info(firstHeader);
         logger.info(secondHeader);

         int[] columnTotals = new int[choiceResults.length];


         int lineTotal = 0;
         for(int i=0;i<PersonIf.personTypeNameArray.length;++i){
             String personTypeString = PersonIf.personTypeNameArray[i];
             String stringToLog  = String.format("%-30s", personTypeString);

             if(countByPersonType.containsKey(personTypeString)){
                 
                 lineTotal = 0;
                 int[] countArray = countByPersonType.get(personTypeString);
                 for(int j=0;j<choiceResults.length;++j){
                     stringToLog += String.format("%12d",countArray[j]);
                     columnTotals[j] += countArray[j];
                     lineTotal += countArray[j];
                 } // j
             } // if key
             else{
                 
                 // log zeros
                 lineTotal = 0;
                 for(int j=0;j<choiceResults.length;++j){
                     stringToLog += String.format("%12d",0);
                 }
             }

             stringToLog += String.format("%12d",lineTotal);

             logger.info(stringToLog);
             
         } // i
         
         String stringToLog  = String.format("%-30s", "Total");
         lineTotal = 0;
         for(int j=0;j<choiceResults.length;++j){
             stringToLog += String.format("%12d",columnTotals[j]);
             lineTotal += columnTotals[j];
         } // j

         logger.info(secondHeader);
         stringToLog += String.format("%12d",lineTotal);
         logger.info(stringToLog);
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info(" ");
         logger.info(" ");

     }

     
     private void logJointModelResults( HouseholdDataManagerIf householdDataManager ) {

         String[] altLabels = modelStructure.getJtfAltLabels();

         // this is the first index in the summary array for choices made by eligible households
         int indexOffset = 4;
         int[] jointTourChoiceFreq = new int[altLabels.length + indexOffset + 1];
     
         
         TreeMap<String, Integer> partySizeFreq = new TreeMap<String, Integer>();

         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];


             // get the array of households
             HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
    
             for(int i=0; i < householdArray.length; ++i){

                 TourIf[] jt = householdArray[i].getJointTourArray();
                 int jtfAlt = householdArray[i].getJointTourFreqChosenAlt();
                 
                 if ( jt == null ) {

                     int index = 0;
                     if ( jtfAlt > 1 ) {
                         logger.error ( String.format( "HHID=%d, joint tour array is null, but a valid alternative=%d is recorded for the household.", householdArray[i].getHhId(), jtfAlt ) );
                         throw new RuntimeException();
                     }
                     else if ( jtfAlt == 1 ) {
                         index = indexOffset + jtfAlt;
                     }
                     else {
                         index = -jtfAlt; 
                     }

                     jointTourChoiceFreq[index]++;
                 }
                 else {
                     

                     if ( jtfAlt <= 1 ) {
                         logger.error ( String.format( "HHID=%d, joint tour array is not null, but an invalid alternative=%d is recorded for the household.", householdArray[i].getHhId(), jtfAlt ) );
                         throw new RuntimeException();
                     }

                     int index = indexOffset + jtfAlt;
                     jointTourChoiceFreq[index]++;


                     // determine party size frequency for joint tours generated
                     PersonIf[] persons = householdArray[i].getPersons();
                     for ( int j=0; j < jt.length; j++ ) {

                         int compAlt = jt[j].getJointTourComposition();

                         // determine number of children and adults in tour
                         int adults = 0;
                         int children = 0;
                         byte[] participants = jt[j].getPersonNumArray();
                         for ( int k=0; k < participants.length; k++ ) {
                             index = participants[k];
                             PersonIf person = persons[index];
                             if ( person.getPersonIsAdult() == 1 )
                                 adults++;
                             else
                                 children++;
                         }
                         
                         // create a key to use for a frequency map for "JointTourPurpose_Composition_NumAdults_NumChildren"
                         String key = String.format( "%s_%d_%d_%d", jt[j].getTourPrimaryPurpose(), compAlt, adults, children );

                         int value = 0;
                         if ( partySizeFreq.containsKey( key ) )
                             value = partySizeFreq.get( key );
                         partySizeFreq.put( key, ++value );

                     }
                     
                 }

             }
    
         }

         
         
         
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info("Joint Tour Frequency and Joint Tour Composition Model Results");


         logger.info(" ");
         logger.info( "Frequency Table of Households by Joint Tour Frequency Choice" );
         logger.info ( String.format( "%-5s   %-26s   %12s", "Alt", "Alt Name", "Households" ) );


         // treat the first few rows of the table differently - no joint tours chosen and therefore no composition chosen.
         // so we just want to print the total households chhosing no joint tours in the "total" column.
         // later we'll add this total to the tanle total in the last row.
         logger.info ( String.format( "%-5s   %-26s   %12d", "-", "single person hh", jointTourChoiceFreq[2] ) );
         logger.info ( String.format( "%-5s   %-26s   %12d", "-", "less than 2 persons travel", jointTourChoiceFreq[3] ) );
         logger.info ( String.format( "%-5s   %-26s   %12d", "-", "only pre-schoolers travel", jointTourChoiceFreq[4] ) );


         int rowTotal = jointTourChoiceFreq[2] + jointTourChoiceFreq[3] + jointTourChoiceFreq[4];
         for ( int i=1; i < altLabels.length; i++ ) {
             int index = indexOffset + i;
             logger.info ( String.format( "%-5d   %-26s   %12d", i, altLabels[i], jointTourChoiceFreq[index] ) );
             rowTotal += jointTourChoiceFreq[index];
         }
         logger.info ( String.format( "%-34s   %12d", "Total Households", rowTotal ) );


         logger.info(" ");
         logger.info(" ");
         logger.info(" ");

         logger.info( "Frequency Table of Joint Tours by All Parties Generated" );
         logger.info ( String.format( "%-5s   %-10s   %-10s   %10s   %10s   %10s", "N", "Purpose", "Type", "Adults", "Children", "Freq" ) );


         int count = 1;
         for ( String key : partySizeFreq.keySet() ) {

             int start = 0;
             int end = 0;
             int compIndex = 0;
             int adults = 0;
             int children = 0;
             String indexString = "";
             String purpose = "";

             start = 0;
             end = key.indexOf( '_', start );
             purpose = key.substring( start, end );

             start = end+1;
             end = key.indexOf( '_', start );
             indexString = key.substring( start, end );
             compIndex = Integer.parseInt ( indexString );

             start = end+1;
             end = key.indexOf( '_', start );
             indexString = key.substring( start, end );
             adults = Integer.parseInt ( indexString );

             start = end+1;
             indexString = key.substring( start );
             children = Integer.parseInt ( indexString );

             logger.info ( String.format( "%-5d   %-10s   %-10s   %10d   %10d   %10d", count++, purpose, JointTourModels.JOINT_TOUR_COMPOSITION_NAMES[compIndex], adults, children, partySizeFreq.get(key) ) );
         }


         logger.info(" ");
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info(" ");

     }

     /**
      * Logs the results of the individual/joint tour stop frequency model.
      *
      */
     public void logStfResults( HouseholdDataManagerIf householdDataManager, Boolean isIndividual){
         
    	 logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info((isIndividual ? "Individual" : "Joint") + " Tour Stop Frequency Model Results");
         
         // count of model results
         logger.info(" ");
         String firstHeader  = "Tour Purpose     ";
         String secondHeader = "---------------   ";
         
         int[] obStopsAlt = StopFrequencyDMU.NUM_OB_STOPS_FOR_ALT;
         int[] ibStopsAlt = StopFrequencyDMU.NUM_IB_STOPS_FOR_ALT;
         
         //hash to accumulate results
         HashMap<String, int[]> chosen = new HashMap<String, int[]>();
          
         
         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];

         
             // get the array of households
             HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
    
             for(int i=0; i < householdArray.length; ++i){
    
            	 if(isIndividual) {
            	 
            		 //individual tours
	                 PersonIf[] personArray = householdArray[i].getPersons();
	                 for(int j=1; j < personArray.length; j++){
	                 
	                     List<TourIf> tourList = new ArrayList<TourIf>();
	                     
	                     // apply stop frequency for all person tours
	                     tourList.addAll( personArray[j].getListOfWorkTours() );
	                     tourList.addAll( personArray[j].getListOfSchoolTours() );
	                     tourList.addAll( personArray[j].getListOfIndividualNonMandatoryTours() );
	                     tourList.addAll( personArray[j].getListOfAtWorkSubtours() );
	
	                     for ( TourIf t : tourList ) {

	                    	 String purpose = t.getTourPurpose().toLowerCase();
	                    	 int choice = t.getStopFreqChoice();
	                    	 
	                    	 if(chosen.containsKey(purpose)) {
	                    		 int[] countArray = chosen.get(purpose);
	                    		 countArray[choice] = countArray[choice] + 1;
	                    		 chosen.put(purpose, countArray);
	                    		 
	                    	 } else {
	                    		 int[] countArray = new int[obStopsAlt.length];
	                    		 countArray[choice] = countArray[choice] + 1;
		                         chosen.put(purpose, countArray);             
	                    	 }
	                     }
	                     
	                 } 
	                 
            	 } else {
	                	
            		 //joint tours
            		 TourIf[] jointTourArray = householdArray[i].getJointTourArray(); 
            		 if (jointTourArray!=null) {
	                     for (int j=0; j<jointTourArray.length; j++) {
	                         
	                         String purpose = jointTourArray[j].getTourPurpose().toLowerCase();
	                         int choice = jointTourArray[j].getStopFreqChoice();
	                         
	                         if(chosen.containsKey(purpose)) {
	                    		 int[] countArray = chosen.get(purpose);
	                    		 countArray[choice] = countArray[choice] + 1;
	                    		 chosen.put(purpose, countArray);
	                    		 
	                    	 } else {
	                    		 int[] countArray = new int[obStopsAlt.length];
	                    		 countArray[choice] = countArray[choice] + 1;
		                         chosen.put(purpose, countArray);             
	                    	 }
	                         
	                     }
            		 }
            		 
	             }
    
             }
    
    
         }
         
         //purpose column headers
         Object[] purposes = chosen.keySet().toArray();
         for(int i=0;i<purposes.length;++i){
        	 firstHeader  += String.format("%18s", purposes[i].toString() );
             secondHeader += "  --------------- ";
        	 
         }
         
         firstHeader  += String.format("%18s","Total");
         secondHeader += "  --------------- ";

         logger.info(firstHeader);
         logger.info(secondHeader);

         //accumulate column totals
         int[] columnTotals = new int[purposes.length];

         //print each alt by purpose total
         int lineTotal = 0;
         for(int i=1;i<obStopsAlt.length;++i){
        	 
        	 //print alt label as first column
             String stringToLog  = String.format("%d out, %d in      ", obStopsAlt[i], ibStopsAlt[i] );

             //loop through purposes
             lineTotal = 0;
             for(int j=0;j<purposes.length;j++){
            	 int[] countArray = chosen.get(purposes[j]);
            	 stringToLog += String.format("%18d",countArray[i]);
                 columnTotals[j] += countArray[i];
                 lineTotal += countArray[i];
             } // j

             stringToLog += String.format("%18d",lineTotal);
             logger.info(stringToLog);
             
         } // i
         
         //print column totals
         String stringToLog  = String.format("%-17s", "Total");
         lineTotal = 0;
         for(int j=0;j<columnTotals.length;++j){
             stringToLog += String.format("%18d",columnTotals[j]);
             lineTotal += columnTotals[j];
         } // j

         logger.info(secondHeader);
         stringToLog += String.format("%18d",lineTotal);
         logger.info(stringToLog);
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info(" ");
         logger.info(" ");

     }
     

     

     
     private void getLogReportSummaries( HouseholdDataManagerIf householdDataManager ) {

         
         // summary collections
         cdapByHhSizeAndPattern = new HashMap<Integer,HashMap<String,Integer>>();
         cdapByPersonTypeAndActivity = new HashMap<String,HashMap<String,Integer>>();

         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];
         
             // get the array of households
             HouseholdIf[] partialHhArray = householdDataManager.getHhArray( startIndex, endIndex );
    
             for ( HouseholdIf hhObject : partialHhArray ) {
                 
                 // get the household's activity pattern choice
                 String pattern = hhObject.getCoordinatedDailyActivityPattern();

                 PersonIf[] personArray = hhObject.getPersons();
                 for( int j=1; j < personArray.length; j++ ) {
                     
                     // get person's activity string 
                     String activityString = personArray[j].getCdapActivity();

                     // get the person type to simmarize results by
                     String personTypeString = personArray[j].getPersonType();

                     // check if the person type is in the map
                     if( cdapByPersonTypeAndActivity.containsKey( personTypeString )){

                         HashMap<String,Integer> activityCountMap = cdapByPersonTypeAndActivity.get( personTypeString );

                         // check if the activity is in the activity map
                         int currentCount = 1;
                         if( activityCountMap.containsKey(activityString) )
                             currentCount = activityCountMap.get(activityString) + 1;
                             
                         activityCountMap.put(activityString, currentCount);
                         cdapByPersonTypeAndActivity.put(personTypeString, activityCountMap);


                     }
                     else{
                         
                         HashMap<String,Integer> activityCountMap = new HashMap<String,Integer>();
                         activityCountMap.put(activityString, 1);
                         cdapByPersonTypeAndActivity.put(personTypeString, activityCountMap);

                     } // is personType in map if

                 } // j (person loop)


                 // count each type of pattern string by hhSize
                 if( cdapByHhSizeAndPattern.containsKey(pattern.length()) ) {
                     
                     HashMap<String,Integer> patternCountMap = cdapByHhSizeAndPattern.get( pattern.length() );
                     
                     int currentCount = 1;
                     if( patternCountMap.containsKey(pattern) )
                         currentCount = patternCountMap.get(pattern) + 1;
                     patternCountMap.put(pattern, currentCount);
                     cdapByHhSizeAndPattern.put(pattern.length(), patternCountMap);
                     
                 }
                 else {
                     
                     HashMap<String,Integer> patternCountMap = new HashMap<String,Integer>();
                     patternCountMap.put(pattern, 1);
                     cdapByHhSizeAndPattern.put(pattern.length(), patternCountMap);

                 } // is personType in map if

             }
    
         }

     }
     
         
     /**
      * Loops through the households in the HouseholdDataManager, gets the coordinated daily
      * activity pattern for each person in the household, and writes a text file with hhid,
      * personid, persnum, and activity pattern.
      *
      * @param householdDataManager
      */
     public void saveCdapResults( HouseholdDataManagerIf householdDataManager, String projectDirectory ){

         String cdapResultsFileName;
         try {
             cdapResultsFileName = resourceBundle.getString( PROPERTIES_RESULTS_CDAP );
         }
         catch ( MissingResourceException e ) {
             // if filename not specified in properties file, don't need to write it.
             return;
         }
         
         
         FileWriter writer;
         PrintWriter outStream = null;
         if ( cdapResultsFileName != null ) {

             cdapResultsFileName = projectDirectory + cdapResultsFileName;

             try {
                 writer = new FileWriter(new File(cdapResultsFileName));
                 outStream = new PrintWriter (new BufferedWriter( writer ) );
             }
             catch(IOException e){
                 logger.fatal( String.format( "Exception occurred opening CDAP results file: %s.", cdapResultsFileName ) );
                 throw new RuntimeException(e);
             }


             outStream.println( "HHID,PersonID,PersonNum,PersonType,ActivityString" );


             ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

             for ( int[] startEndIndices : startEndTaskIndicesList ) {
             
                 int startIndex = startEndIndices[0];
                 int endIndex = startEndIndices[1];

             
                 // get the array of households
                 HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
        
                 for(int i=0; i < householdArray.length; ++i){

                     HouseholdIf household = householdArray[i];
                     int hhid = household.getHhId();
    
                     // get the pattern for each person
                     PersonIf[] personArray = household.getPersons();
                     for( int j=1; j < personArray.length; j++ ) {
    
                         PersonIf person = personArray[j];
                         
                         int persId = person.getPersonId();
                         int persNum = person.getPersonNum();
                         int persType = person.getPersonTypeNumber();
                         String activityString = person.getCdapActivity();
    
                         outStream.println( String.format("%d,%d,%d,%d,%s", hhid, persId, persNum, persType, activityString ));

                     } // j (person loop)
                     
                 }

             }

             outStream.close();

         }

     }



     
     /**
      * Logs the results of the model.
      *
      */
     public void logAtWorkSubtourFreqResults( HouseholdDataManagerIf householdDataManager ){
         
         String[] alternativeNames = modelStructure.getAwfAltLabels();
         HashMap<String,int[]> awfByPersonType = new HashMap<String,int[]>();
         
         ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges( householdDataManager.getNumHouseholds() );

         for ( int[] startEndIndices : startEndTaskIndicesList ) {
         
             int startIndex = startEndIndices[0];
             int endIndex = startEndIndices[1];
         
             // get the array of households
             HouseholdIf[] householdArray = householdDataManager.getHhArray( startIndex, endIndex );
             for(int i=0; i < householdArray.length; ++i){

                 // get this household's person array
                 PersonIf[] personArray = householdArray[i].getPersons();
                 
                 // loop through the person array (1-based)
                 for(int j=1;j<personArray.length;++j){

                     PersonIf person = personArray[j];
                     
                     // loop through the work tours for this person
                     ArrayList<TourIf> tourList = person.getListOfWorkTours();
                     if ( tourList == null || tourList.size() == 0 )
                         continue;
                     
                     // count the results by person type
                     String personTypeString = person.getPersonType();


                     for ( TourIf workTour : tourList ) {

                         int choice = 0;
                         if ( person.getListOfAtWorkSubtours().size() == 0 )
                             choice = 1;
                         else {
                             choice = workTour.getSubtourFreqChoice();
                             if ( choice == 0 )
                                 choice++;
                         }
                         
                         
                         // count the results by person type
                         if( awfByPersonType.containsKey(personTypeString)){
                             int[] counterArray = awfByPersonType.get(personTypeString);
                             counterArray[choice-1]++;
                             awfByPersonType.put( personTypeString, counterArray );

                         }
                         else{
                             int[] counterArray = new int[alternativeNames.length];
                             counterArray[choice-1]++;
                             awfByPersonType.put(personTypeString, counterArray);
                         }

                     }
                         

                 }

             }
    
         }

         
         
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info("At-Work Subtour Frequency Model Results");
         
         // count of model results
         logger.info(" ");
         String firstHeader  = "Person type                 ";
         String secondHeader = "---------------------------     ";
         
         
         for(int i=0;i<alternativeNames.length;++i){
             firstHeader  += String.format("%16s",alternativeNames[i]);
             secondHeader += "------------    ";
         }
         
         firstHeader  += String.format("%16s","Total");
         secondHeader += "------------";

         logger.info(firstHeader);
         logger.info(secondHeader);

         int[] columnTotals = new int[alternativeNames.length];


         int lineTotal = 0;
         for(int i=0;i<PersonIf.personTypeNameArray.length;++i){
             String personTypeString = PersonIf.personTypeNameArray[i];
             String stringToLog  = String.format("%-28s", personTypeString);

             if(awfByPersonType.containsKey(personTypeString)){
                 
                 lineTotal = 0;
                 int[] countArray = awfByPersonType.get(personTypeString);
                 for(int j=0;j<alternativeNames.length;++j){
                     stringToLog += String.format("%16d",countArray[j]);
                     columnTotals[j] += countArray[j];
                     lineTotal += countArray[j];
                 } // j

             } // if key
             else{
                 
                 // log zeros
                 lineTotal = 0;
                 for(int j=0;j<alternativeNames.length;++j){
                     stringToLog += String.format("%16d",0);
                 }
             }

             stringToLog += String.format("%16d",lineTotal);

             logger.info(stringToLog);
             
         } // i
         
         String stringToLog  = String.format("%-28s", "Total");
         lineTotal = 0;
         for(int j=0;j<alternativeNames.length;++j){
             stringToLog += String.format("%16d",columnTotals[j]);
             lineTotal += columnTotals[j];
         } // j

         logger.info(secondHeader);
         stringToLog += String.format("%16d",lineTotal);
         logger.info(stringToLog);
         logger.info(" ");
         logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
         logger.info(" ");
         logger.info(" ");

     }

     
     
     
     
     
     
     
     protected ArrayList<int[]> getWriteHouseholdRanges( int numberOfHouseholds ) {
         
         ArrayList<int[]> startEndIndexList = new ArrayList<int[]>(); 

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

}
