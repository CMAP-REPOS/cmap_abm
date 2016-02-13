package com.pb.models.ctrampIf.jppf;

import com.pb.cmap.tvpb.BestTransitPathCalculator;
import com.pb.cmap.tvpb.TransitDriveAccessDMU;
import com.pb.cmap.tvpb.TransitWalkAccessDMU;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.IndexSort;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.ParkingChoiceDMU;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.StopDestChoiceSize;
import com.pb.models.ctrampIf.StopIf;
import com.pb.models.ctrampIf.StopLocationDMU;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TourIf;
import com.pb.models.ctrampIf.TourModeChoiceDMU;
import com.pb.models.ctrampIf.TripModeChoiceDMU;
import com.pb.common.newmodel.ChoiceModelApplication;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

/**
 * This class will be used for determining the number of stops
 * on individual mandatory, individual non-mandatory and joint
 * tours.
 *
 * @author Christi Willison
 * @version Nov 4, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class StopLocationModeChoiceModel implements Serializable {
    
    private transient Logger logger = Logger.getLogger(StopLocationModeChoiceModel.class);
    private transient Logger slcLogger = Logger.getLogger("slcLogger");
    private transient Logger tripDepartLogger = Logger.getLogger("tripDepartLog");
    private transient Logger tripMcLogger = Logger.getLogger("tripMcLog");
    private transient Logger parkLocLogger = Logger.getLogger("parkLocLog");

    private static final int HOME_TYPE_INDEX = 1;
    private static final int PRIM_DEST_TYPE_INDEX = 2;
    private static final int INT_STOP_TYPE_INDEX = 3;
    
    private static final String PROPERTIES_UEC_STOP_LOCATION = "UecFile.StopLocation";
    public static final String PROPERTIES_UEC_TRIP_MODE_CHOICE = "UecFile.TripModeChoice";
    private static final String PROPERTIES_UEC_PARKING_LOCATION_CHOICE = "UecFile.ParkingLocationChoice";
    
    private static final String PROPERTIES_UEC_PARKING_LOCATION_ALTERNATIVES_FILE = "CBDParkingAlternatives.file";
    
    public static final int UEC_DATA_PAGE = 0;
    private static final int MAND_FREE_PAGE = 1;
    private static final int MAND_PAID_PAGE = 2;
    private static final int NON_MAND_PAGE = 3;

    private static int PARK_TAZ_COLUMN = 2;

    
    private static final int MAX_INVALID_FIRST_ARRIVAL_COUNT = 1000;
    
    
    private StopDestinationSampleOfAlternativesModel dcSoaModel = null;
    private StopDestChoiceSize dcSizeModel = null;

    private int[] sampleValues;
    private boolean[] destinationAvailability;
    private int[] destinationSample;

    private TazDataIf tazDataManager;
    private ModelStructure modelStructure;
    
    private StopLocationDMU stopLocDmuObj;
    private TripModeChoiceDMU tripModeChoiceDmuObj;
    private ParkingChoiceDMU parkingChoiceDmuObj;
    private StopDepartArrivePeriodModel stopTodModel;
    
    private ChoiceModelApplication[] slChoiceModelApplication;
    private ChoiceModelApplication[] mcChoiceModelApplication;
    private ChoiceModelApplication mandatoryFreePc;
    private ChoiceModelApplication mandatoryPaidPc;
    private ChoiceModelApplication nonMandatoryPc;
    
    private TableDataSet cbdAltsTable;
    
    private int[] altToZone;
    private int[] altToSubZone;
    
    private float[] parkRate;
    
    public static final int NUM_CPU_TIME_VALUES = 8;
    private long soaTime;
    private long slsTime;
    private long slcTime;
    private long todTime;
    private long smcTime;
    private long plcTime;
    private long[] hhTimes = new long[NUM_CPU_TIME_VALUES];

    private String loggerSeparator = "";
    
    BestTransitPathCalculator tvpb;
    
    /**
     * Constructor that will be used to set up the ChoiceModelApplications for each
     * type of tour
     * @param projectDirectory - name of root level project directory
     * @param resourceBundle - properties file with paths identified
     * @param dmuObject - decision making unit for stop frequency
     * @param tazDataManager - holds information about TAZs in the model.
     * @param modelStructure - holds the model structure info
     */
    public StopLocationModeChoiceModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, TazDataIf tazDataManager, CtrampDmuFactoryIf dmuFactory ) {
        
        this.tazDataManager = tazDataManager;
        this.modelStructure = modelStructure;

        setupStopLocationChoiceModels( propertyMap, dmuFactory );
        setupTripDepartTimeModel(propertyMap, dmuFactory);
        setupTripModeChoiceModels( propertyMap, dmuFactory );
        setupParkingLocationModel( propertyMap, dmuFactory );
        
        //create TVPB for trip mode choice UEC 
        tvpb = new BestTransitPathCalculator(propertyMap);
    }

    private void setupStopLocationChoiceModels( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory ) {
        
        logger.info( String.format( "setting up stop location choice models." ) );
        
        altToZone = tazDataManager.getAltToZoneArray();
        altToSubZone = tazDataManager.getAltToSubZoneArray();
        parkRate = tazDataManager.getZonalParkRate();

        stopLocDmuObj = dmuFactory.getStopLocationDMU();

        
        // locate the UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );

        // locate the UEC
        String uecFileName = propertyMap.get( PROPERTIES_UEC_STOP_LOCATION );
        uecFileName = projectDirectory + uecFileName;


        // set up the stop location choice model array
        Collection<Integer> modelSheets = modelStructure.getStopLocModelSheetIndices();
        slChoiceModelApplication = new ChoiceModelApplication[modelSheets.size()+1];     // one choice model for each model sheet specified
        for ( int i : modelSheets ) {
            if ( slChoiceModelApplication[i] == null )
                slChoiceModelApplication[i] = new ChoiceModelApplication( uecFileName, i, UEC_DATA_PAGE, propertyMap, (VariableTable)stopLocDmuObj );
        }

        
        dcSizeModel = new StopDestChoiceSize(propertyMap,tazDataManager,modelStructure);
        dcSoaModel = new StopDestinationSampleOfAlternativesModel(propertyMap,tazDataManager,dcSizeModel,modelStructure,dmuFactory);

        sampleValues = new int[dcSoaModel.getSampleOfAlternativesSampleSize()];
        
        int altCount = tazDataManager.getNumberOfZones()*tazDataManager.getNumberOfSubZones();
        destinationAvailability = new boolean[altCount+1];
        destinationSample = new int[altCount+1];

    
        for (int i=0; i < 38; i++);
            loggerSeparator += "-"; 
    }

    

    private void setupTripModeChoiceModels( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory ) {
        
        logger.info( String.format( "setting up trip mode choice models." ) );
        
        // locate the UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );

        // locate the UEC
        String uecFileName = propertyMap.get( PROPERTIES_UEC_TRIP_MODE_CHOICE );
        uecFileName = projectDirectory + uecFileName;

        tripModeChoiceDmuObj = dmuFactory.getTripModeChoiceDMU();

        // keep a set of unique model sheet numbers so that we can create ChoiceModelApplication objects once for each model sheet used
        TreeSet<Integer> modelIndexSet = new TreeSet<Integer>();

        Collection<Integer> modelSheets = modelStructure.getTripModeChoiceModelSheetIndices();
        
        int maxUecIndex = 0;
        for ( int i : modelSheets ) {
            modelIndexSet.add( i );
            if ( i > maxUecIndex )
                maxUecIndex = i;
        }
        
        // set up the trip mode choice model array
        mcChoiceModelApplication = new ChoiceModelApplication[maxUecIndex+1];     // one choice model for each model sheet specified

        // for each unique model index, create the ChoiceModelApplication object and the availabilty array
        Iterator<Integer> it = modelIndexSet.iterator();
        int i = -1;
        while ( it.hasNext() ) {
            i = it.next();
            mcChoiceModelApplication[i] = new ChoiceModelApplication( uecFileName, i, UEC_DATA_PAGE, propertyMap, (VariableTable)tripModeChoiceDmuObj );
        }
    }

    

    private void setupParkingLocationModel( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory ) {
        
        logger.info ("setting up parking location choice models.");
        
        // locate the UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );

        // locate the UEC
        String uecFileName = propertyMap.get( PROPERTIES_UEC_PARKING_LOCATION_CHOICE );
        uecFileName = projectDirectory + uecFileName;

        parkingChoiceDmuObj = dmuFactory.getParkingChoiceDMU();

        mandatoryFreePc =  new ChoiceModelApplication( uecFileName, MAND_FREE_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)parkingChoiceDmuObj );
        mandatoryPaidPc =  new ChoiceModelApplication( uecFileName, MAND_PAID_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)parkingChoiceDmuObj );
        nonMandatoryPc =  new ChoiceModelApplication( uecFileName, NON_MAND_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)parkingChoiceDmuObj );


        

        // read the parking choice alternatives data file to get alternatives names
        String cbdFile = projectDirectory + (String)propertyMap.get( PROPERTIES_UEC_PARKING_LOCATION_ALTERNATIVES_FILE );

        try {
            CSVFileReader reader = new CSVFileReader();
            cbdAltsTable = reader.readFile(new File(cbdFile));
        }
        catch (IOException e) {
            logger.error ("problem reading table of cbd zones for parking location choice model.", e);
            System.exit(1);
        }

        int[] parkTazs = cbdAltsTable.getColumnAsInt( PARK_TAZ_COLUMN );
        parkingChoiceDmuObj.setParkTazArray ( parkTazs );
    }
    
    private void setupTripDepartTimeModel( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory ) {
       	stopTodModel = new StopDepartArrivePeriodModel( propertyMap, modelStructure );
    }
    
    
    public void applyModel( HouseholdIf household ) {

        soaTime = 0;
        slsTime = 0;
        slcTime = 0;
        todTime = 0;
        smcTime = 0;
        plcTime = 0;

        // get this household's person array
        PersonIf[] personArray = household.getPersons();

        // set the household id, origin taz, hh taz, and debugFlag=false in the dmus
        stopLocDmuObj.setHouseholdObject(household);
        tripModeChoiceDmuObj.setHouseholdObject(household);

        
        // loop through the person array (1-based)
        for(int j=1;j<personArray.length;++j){

            ArrayList<TourIf> tours = new ArrayList<TourIf>();

            PersonIf person = personArray[j];

            // set the person
            stopLocDmuObj.setPersonObject(person);
            tripModeChoiceDmuObj.setPersonObject(person);

            
            
            // apply stop location and mode choice for all individual tours.
            tours.addAll( person.getListOfWorkTours() );
            tours.addAll( person.getListOfSchoolTours() );
            tours.addAll( person.getListOfIndividualNonMandatoryTours() );
            tours.addAll( person.getListOfAtWorkSubtours() );

            for ( TourIf tour : tours ) {

                // set the tour object
                stopLocDmuObj.setTourObject(tour);
                tripModeChoiceDmuObj.setTourObject(tour);

                
                applyForOutboundStops( tour, person, household );
                
                applyForInboundStops( tour, person, household );

            } //tour loop

        } // j (person loop)

        
        
        // apply stop location and mode choice for all joint tours.
        TourIf[] jointTours = household.getJointTourArray();
        if ( jointTours != null ) {
        
            for ( TourIf tour : jointTours ) {

                // set the tour object
                stopLocDmuObj.setTourObject(tour);
                tripModeChoiceDmuObj.setTourObject(tour);

                // no person objects for joint tours
                applyForOutboundStops( tour, null, household );
                
                applyForInboundStops( tour, null, household );


            } //tour loop

        }

        
        household.setStlRandomCount( household.getHhRandomCount() );
        
    
    }

    
    private void applyForOutboundStops( TourIf tour, PersonIf person, HouseholdIf household ) {
        
        StopIf[] stops = tour.getOutboundStops();
        
        //select trip depart hour
        if ( stops != null) {
            long check = System.nanoTime();
            setOutboundTripDepartTimes ( stops );
            todTime += ( System.nanoTime() - check );
        }
        
        int origin = tour.getTourOrigTaz();
        int originWalkSegment = tour.getTourOrigWalkSubzone();
        int dest = tour.getTourDestTaz();
        int destWalkSegment = tour.getTourDestWalkSubzone();
        stopLocDmuObj.setInboundStop( false );
            
        tripModeChoiceDmuObj.setOrigType( HOME_TYPE_INDEX );
        tripModeChoiceDmuObj.setOrigParkRate( 0.0f );
        tripModeChoiceDmuObj.setDestType( PRIM_DEST_TYPE_INDEX );
        tripModeChoiceDmuObj.setPrimDestParkRate( parkRate[dest-1] );

        applyTripChoiceModels ( household, person, tour, stops, origin, originWalkSegment, dest, destWalkSegment, 0);

    }
    
    
    private void applyForInboundStops( TourIf tour, PersonIf person, HouseholdIf household ) {
        
        StopIf[] stops = tour.getInboundStops();
        
        //select trip arrive hour
        if ( stops != null) {
            long check = System.nanoTime();
            int lastOutboundTripDeparts = -1;
            StopIf[] obStops = tour.getOutboundStops();        
            if ( obStops == null ) {
                lastOutboundTripDeparts = tour.getTourDepartPeriod();
            }
            else {
                StopIf lastStop = obStops[obStops.length - 1];
                lastOutboundTripDeparts = lastStop.getStopPeriod();
            }
                
            setInboundTripDepartTimes ( stops, lastOutboundTripDeparts );
            todTime += ( System.nanoTime() - check );        
        }
        
        int origin = tour.getTourDestTaz();
        int originWalkSegment = tour.getTourDestWalkSubzone();
        int dest = tour.getTourOrigTaz();
        int destWalkSegment = tour.getTourOrigWalkSubzone();
        stopLocDmuObj.setInboundStop( true );
            
        tripModeChoiceDmuObj.setOrigType( PRIM_DEST_TYPE_INDEX );
        tripModeChoiceDmuObj.setOrigParkRate( parkRate[origin-1] );
        tripModeChoiceDmuObj.setDestType( HOME_TYPE_INDEX );
        tripModeChoiceDmuObj.setPrimDestParkRate( parkRate[origin-1] );
        
        applyTripChoiceModels ( household, person, tour, stops, origin, originWalkSegment, dest, destWalkSegment, 1);

    }

    
    private void applyTripChoiceModels ( HouseholdIf household, PersonIf person, TourIf tour, StopIf[] stops, int origin, int originWalkSegment, int dest, int destWalkSegment, int inbound ) {
        
        // if there are stops on this half-tour, determine their destinations, depart hours, trip modes, and parking tazs.
        if (stops != null) {

            for ( int i=0; i < stops.length; i++ ) {
                
                StopIf stop = stops[i];
                stop.setOrig(origin);
                stop.setOrigWalkSegment(originWalkSegment);
                
                stopLocDmuObj.setStopObject( stop );
                
                stopLocDmuObj.setStopNumber( i + 1 );
                stopLocDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), origin, dest );

                tripModeChoiceDmuObj.setStopObject( stop );
                tripModeChoiceDmuObj.setStopObjectIsFirst( i == 0 ? 1 : 0 );
                tripModeChoiceDmuObj.setStopObjectIsLast( i == stops.length - 1 ? 1 : 0 );
                
                int zone = -1;
                int subzone = -1;
                int choice = -1;
                // if not the last stop object, make a destination choice
                if ( i < stops.length - 1 ) {

                    tripModeChoiceDmuObj.setDestType( INT_STOP_TYPE_INDEX );

                    try {
                        long check = System.nanoTime();
                        choice = selectDestination(stop);
                        slcTime += ( System.nanoTime() - check );
                    }
                    catch ( Exception e ) {
                        logger.error ( String.format( "Exception caught processing %s stop location choice model for %s type tour %s stop:  HHID=%d, personNum=%s, stop=%d.", ( stopLocDmuObj.getInboundStop() == 1 ? "inbound" : "outbound"), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourPrimaryPurpose(), household.getHhId(), ( person == null ? "N/A" : Integer.toString(person.getPersonNum()) ), (i+1) ) );
                        throw new RuntimeException(e);
                    }

                    zone = altToZone[choice];
                    subzone = altToSubZone[choice];
                    tripModeChoiceDmuObj.setIntStopParkRate( parkRate[zone-1] );
                }
                else {
                    zone = dest;
                    subzone = destWalkSegment;
                    if ( stopLocDmuObj.getInboundStop() == 1 ) {
                        tripModeChoiceDmuObj.setDestType( HOME_TYPE_INDEX );
                    }
                    else {
                        tripModeChoiceDmuObj.setDestType( PRIM_DEST_TYPE_INDEX );
                    }
                }
                
                stop.setDest(zone);
                stop.setDestWalkSegment(subzone);
                tripModeChoiceDmuObj.setDmuIndexValues( household.getHhId(), origin, zone );
                
                //create TVPB for trip mode choice model
                setTVPBValues(tripModeChoiceDmuObj, inbound, stop.getDestPurpose(), true, household.getDebugChoiceModels());
                
                //select mode
                long check = System.nanoTime();
                choice = -1;
                try {
                    choice = selectMode( household, tour, stop );
                }
                catch ( Exception e ) {
                    logger.error ( String.format( "Exception caught processing %s trip mode choice model for %s type tour %s intermediate stop:  HHID=%d, personNum=%s, stop=%d.", ( stopLocDmuObj.getInboundStop() == 1 ? "inbound" : "outbound"), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourPrimaryPurpose(), household.getHhId(), ( person == null ? "N/A" : Integer.toString(person.getPersonNum()) ), (i+1) ) );
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                stop.setMode( choice );
                smcTime += ( System.nanoTime() - check );

                //set taps for stop
                setStopTaps(stop, tripModeChoiceDmuObj, choice);
                
                // set the next segment's origin temporarily - it might change if a parking zone is selected
                int tempOrigin = zone;
                
                check = System.nanoTime();
                float parkTot = tazDataManager.getZoneTableValue( zone, tazDataManager.getZonalParkTotFieldName() );
                
                // if the stop location is in the CBD and the mode is drive (SOV or HOV), determine the parking location zone.
                if ( modelStructure.getTripModeIsSovOrHov( choice ) && parkTot > 0 && tazDataManager.getZoneIsCbd( zone ) == 1 ) {
                    parkingChoiceDmuObj.setDmuState( household, origin, zone );
                    choice = -1;
                    try {
                        choice = selectParkingLocation ( household, tour, stop );
                    }
                    catch ( Exception e ) {
                        logger.error ( String.format( "Exception caught processing %s stop parking location choice model for %s type tour %s intermediate stop:  HHID=%d, personNum=%s, stop=%d.", ( stopLocDmuObj.getInboundStop() == 1 ? "inbound" : "outbound"), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourPrimaryPurpose(), household.getHhId(), ( person == null ? "N/A" : Integer.toString(person.getPersonNum()) ), (i+1) ) );
                        throw new RuntimeException(e);
                    }
                    stop.setPark( choice );
                    tempOrigin = choice;
                }
                plcTime += ( System.nanoTime() - check );
                
                origin = tempOrigin;
                originWalkSegment = subzone;

                tripModeChoiceDmuObj.setOrigType( INT_STOP_TYPE_INDEX );
                tripModeChoiceDmuObj.setOrigParkRate( parkRate[origin-1] );

            }

        }
        // if no stops on the half-tour, determine trip mode choice, then parking location choice at the primary destination.
        else {


            long check = System.nanoTime();
            // create a Stop object for use in applying trip mode choice for this half tour without stops
            String origStopPurpose = "";
            String destStopPurpose = "";
            if ( stopLocDmuObj.getInboundStop() == 0 ) {
                origStopPurpose = tour.getTourCategoryIsAtWork() ? "Work" : "Home";
                destStopPurpose = tour.getTourPrimaryPurpose();
            }
            else {
                origStopPurpose = tour.getTourPrimaryPurpose();
                destStopPurpose = tour.getTourCategoryIsAtWork() ? "Work" : "Home";
            }

            StopIf stop = null;
            try {
                stop = tour.createStop( modelStructure, origStopPurpose, destStopPurpose, stopLocDmuObj.getInboundStop() == 1, tour.getTourCategoryIsAtWork() );            
            }
            catch ( Exception e ){
                logger.info( "exception creating stop." );
            }
            
            stop.setStopPeriod( stopLocDmuObj.getInboundStop() == 1 ? tour.getTourArrivePeriod() : tour.getTourDepartPeriod() );
            
            stop.setOrig(origin);
            stop.setOrigWalkSegment(originWalkSegment);
            
            stopLocDmuObj.setStopObject( stop );
            
            stopLocDmuObj.setStopNumber( 1 );
            stopLocDmuObj.setDmuIndexValues( household.getHhId(), household.getHhTaz(), origin, dest );
            
            int zone = dest;
            int subzone = destWalkSegment;
            if ( stopLocDmuObj.getInboundStop() == 1 ) {
                tripModeChoiceDmuObj.setDestType( HOME_TYPE_INDEX );
            }
            else {
                tripModeChoiceDmuObj.setDestType( PRIM_DEST_TYPE_INDEX );
            }

            stop.setDest(zone);
            stop.setDestWalkSegment(subzone);

            tripModeChoiceDmuObj.setStopObject( stop );
            tripModeChoiceDmuObj.setDmuIndexValues( household.getHhId(), origin, zone );
            
            //create TVPB for trip mode choice model
            setTVPBValues(tripModeChoiceDmuObj, inbound, stop.getDestPurpose(), true, household.getDebugChoiceModels());
            
            //select mode
            int choice = -1;
            try {
                choice = selectMode( household, tour, stop );
            }
            catch ( Exception e ) {
                logger.error ( String.format( "Exception caught processing %s trip mode choice model for %s type half-tour %s stop:  HHID=%d, personNum=%s, stop=%d.", ( stopLocDmuObj.getInboundStop() == 1 ? "inbound" : "outbound"), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourPrimaryPurpose(), household.getHhId(), ( person == null ? "N/A" : Integer.toString(person.getPersonNum()) ), 1 ) );
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            stop.setMode( choice );
            smcTime += ( System.nanoTime() - check );
            check = System.nanoTime();            

            //set taps for stop
            setStopTaps(stop, tripModeChoiceDmuObj, choice);           
            
            float parkTot = tazDataManager.getZoneTableValue( zone, tazDataManager.getZonalParkTotFieldName() );
            
            // if the stop location is in the CBD and the mode is drive (SOV or HOV), determine the parking location zone.
            if ( modelStructure.getTripModeIsSovOrHov( choice ) && parkTot > 0 && tazDataManager.getZoneIsCbd( zone ) == 1 ) {
                parkingChoiceDmuObj.setDmuState( household, origin, zone );
                choice = -1;
                try {
                    choice = selectParkingLocation ( household, tour, stop );
                }
                catch ( Exception e ) {
                    logger.error ( String.format( "Exception caught processing %s stop parking location choice model for %s type half-tour %s stop:  HHID=%d, personNum=%s, stop=%d.", ( stopLocDmuObj.getInboundStop() == 1 ? "inbound" : "outbound"), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourPrimaryPurpose(), household.getHhId(), ( person == null ? "N/A" : Integer.toString(person.getPersonNum()) ), 1 ) );
                    throw new RuntimeException(e);
                }
                stop.setPark( choice );
            }
            plcTime += ( System.nanoTime() - check );
             
        }            
            
    }

    
    private int selectDestination(StopIf s) {

        Logger modelLogger = slcLogger;
        
        HouseholdIf household = s.getTour().getPersonObject().getHouseholdObject();
        TourIf tour = s.getTour();
        PersonIf person = tour.getPersonObject();
        
        if ( household.getDebugChoiceModels() ) {
            if ( s == null ) {
                household.logHouseholdObject( "Pre Stop Location Choice for tour primary destination: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId(), modelLogger );
                household.logPersonObject( "Pre Stop Location Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                household.logTourObject("Pre Stop Location Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
            }
            else {
                household.logHouseholdObject( "Pre Stop Location Choice for trip: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Stop_" + s.getStopId(), modelLogger );
                household.logPersonObject( "Pre Stop Location Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                household.logTourObject("Pre Stop Location Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
                household.logStopObject("Pre Stop Location Choice for stop " + s.getStopId(), modelLogger, s, modelStructure );
            }
        }
        

        long check = System.nanoTime();
        StopDestinationSampleOfAlternativesModel.StopSoaResult result =  dcSoaModel.computeDestinationSampleOfAlternatives(s);
        soaTime += ( System.nanoTime() - check );

        int numAltsInSample = result.getNumUniqueAltsInSample();
        int[] sample = result.getSample();
        float[] corrections = result.getCorrections();
        double[] stopLocSize = result.getStopLocationSize();

        
        int modelIndex = modelStructure.getStopLocationModelIndex( s.getTour().getTourPrimaryPurpose() );
        ChoiceModelApplication choiceModel = slChoiceModelApplication[modelIndex];

        
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        String separator = "";
        String logsumLoggingHeader = "";
        
        if ( household.getDebugChoiceModels() ) {

            choiceModelDescription = "Stop Location Choice";
            decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourPrimaryPurpose(), tour.getTourId(), s.getDestPurpose(), s.getStopId() );
            loggingHeader = String.format( "%s for %s", choiceModelDescription, decisionMakerLabel );
        
            modelLogger.info(" ");
            for (int k=0; k < loggingHeader.length(); k++)
                separator += "+";
            modelLogger.info( loggingHeader );
            modelLogger.info( separator );
            modelLogger.info( "" );
            modelLogger.info( "" );
        
        }


        stopLocDmuObj.setLogSizeTerms(stopLocSize);
        
        Arrays.fill(destinationAvailability,false);
        Arrays.fill(destinationSample,0);
        for (int i=1; i <= numAltsInSample; i++) {
            int alternative = sample[i];
            destinationAvailability[alternative] = true;
            destinationSample[alternative] = 1;

            stopLocDmuObj.setDcSoaCorrections(altToZone[alternative],altToSubZone[alternative],corrections[i]);
            
            // calculate the logsum for the stop origin to the stop destination alternative
            // stop origin has already been set; stop destionation is the stop alternative being considered for the ik logsum
            s.setDest( altToZone[alternative] );
            s.setDestWalkSegment( altToSubZone[alternative] );

            if ( household.getDebugChoiceModels() ) {
                logsumLoggingHeader = "ik mode choice logsum for slc soa sample=" + i + ", alt=" + alternative + ", taz=" + altToZone[alternative] + ", subzone=" + altToSubZone[alternative];
            }
        
            
            check = System.nanoTime();
            double ikLogsum = calculateTripModeChoiceLogsum( household, person, tour, s, modelLogger, logsumLoggingHeader );
            stopLocDmuObj.setTripMcLogsumIk( altToZone[alternative], altToSubZone[alternative], ikLogsum );
            

            
            // calculate the logsum for the stop destination alternative to the tour primary destination
            // stop destination is the tour origin or the tour primary destination, depending on whether this half-tour is inbound or outbound
            s.setDest( s.isInboundStop() ? tour.getTourOrigTaz() : tour.getTourDestTaz() );
            s.setDestWalkSegment( s.isInboundStop() ? tour.getTourOrigWalkSubzone() : tour.getTourDestWalkSubzone() );
            // stop dest attributes will be reset after stop location is made; but stop origin attributes need to be reset after logsum calculation
            int tempOrig = s.getOrig();
            int tempOrigWalkSegment = s.getOrigWalkSegment();
            // stop origin should be the stop alternative being considered for the jk logsum.
            s.setOrig( altToZone[alternative] );
            s.setOrigWalkSegment( altToSubZone[alternative] );
            
            if ( household.getDebugChoiceModels() ) {
                logsumLoggingHeader = "kj mode choice logsum for slc soa sample=" + i + ", alt=" + alternative + ", taz=" + altToZone[alternative] + ", subzone=" + altToSubZone[alternative];
            }

            
            double kjLogsum = calculateTripModeChoiceLogsum( household, person, tour, s, modelLogger, logsumLoggingHeader );
            s.setOrig( tempOrig );
            s.setOrigWalkSegment( tempOrigWalkSegment );
            stopLocDmuObj.setTripMcLogsumKj( altToZone[alternative], altToSubZone[alternative], kjLogsum );
            slsTime += ( System.nanoTime() - check );

        }

        choiceModel.computeUtilities(stopLocDmuObj,stopLocDmuObj.getDmuIndexValues(),destinationAvailability,destinationSample);

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();
        
        // if the choice model has at least one available alternative, make choice.
        int chosen = -1;
        if ( choiceModel.getAvailabilityCount() > 0 )
            chosen = choiceModel.getChoiceResult( rn );
        else {
        	
        	//set to origin or destination if no alt found, assumes 1 to 1 match alts to zone ids
        	chosen = rn < 0.5 ? s.getOrig() : s.getDest();
            logger.error (String.format( "Error for HHID=%d, PersonNum=%d, no available %s stop destination choice alternatives to choose from in choiceModelApplication so setting to orig or dest.", stopLocDmuObj.getHouseholdObject().getHhId(), stopLocDmuObj.getPersonObject().getPersonNum(), tour.getTourPrimaryPurpose()));
        }
        
        // write choice model alternative info to log file
        if ( household.getDebugChoiceModels() || chosen < 0 ) {

            if ( chosen < 0 ) {
                choiceModelDescription = "No Alternatives for Stop Location Choice";
                decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourPrimaryPurpose(), tour.getTourId(), s.getDestPurpose(), s.getStopId() );
                loggingHeader = String.format( "%s for %s", choiceModelDescription, decisionMakerLabel );
            
                modelLogger.info(" ");
                for (int k=0; k < loggingHeader.length(); k++)
                    separator += "+";
                modelLogger.info( loggingHeader );
                modelLogger.info( separator );
                modelLogger.info( "" );
                modelLogger.info( "" );
            }
            
            double[] utilities     = choiceModel.getUtilities();
            double[] probabilities = choiceModel.getProbabilities();
            boolean[] availabilities = choiceModel.getAvailabilities();

            
            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum  + ", Person type: " + personTypeString );
            modelLogger.info("Alternative             Availability           Utility       Probability           CumProb");
            modelLogger.info("---------------------   ------------       -----------    --------------    --------------");

            int numberOfSubzones = tazDataManager.getNumberOfSubZones();
            
            // copy the values of the sample to an array that can be sorted for logging purposes
            for (int i=1; i <= numAltsInSample; i++)
                sampleValues[i-1] = sample[i];
            for (int i=numAltsInSample; i < sampleValues.length; i++)
                sampleValues[i] = Integer.MAX_VALUE;
            int[] sortedSampleValueIndices = IndexSort.indexSort( sampleValues );
            
            double cumProb = 0.0;
            int selectedIndex = -1;
            for(int j=1; j <= numAltsInSample; j++){

                int k =  sortedSampleValueIndices[j-1];
                int alt = sample[k+1];

                if ( alt == chosen )
                    selectedIndex = j;
                
                int d = ( alt-1) / numberOfSubzones + 1;
                int w = alt - (d-1)*numberOfSubzones - 1;
                cumProb += probabilities[alt-1];
                String altString = String.format( "%-3d %5d %5d %5d", j, alt, d, w );
                modelLogger.info(String.format("%-21s%15s%18.6e%18.6e%18.6e", altString, availabilities[alt], utilities[alt-1], probabilities[alt-1], cumProb));
            }

            modelLogger.info(" ");
            int d = (chosen-1)/numberOfSubzones + 1;
            int w = chosen - (d-1)*numberOfSubzones - 1;
            String altString = String.format( "%-3d %5d %5d %5d", selectedIndex, chosen, d, w );
            modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );

            modelLogger.info( separator );
            modelLogger.info( "" );
            modelLogger.info( "" );
        
            
            choiceModel.logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
            choiceModel.logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, chosen );

            
            // write UEC calculation results to separate model specific log file
            choiceModel.logUECResults( modelLogger, loggingHeader );

            if ( chosen < 0 )
                throw new RuntimeException();
        }

        return chosen;
    }

    
    private int selectMode ( HouseholdIf household, TourIf tour, StopIf stop ) {

        Logger modelLogger = tripMcLogger;
        
        
        if ( household.getDebugChoiceModels() ) {
            if ( stop == null ) {
                household.logHouseholdObject( "Pre Trip Mode Choice: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId(), tripMcLogger );
                household.logPersonObject( "Pre Trip Mode Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                household.logTourObject("Pre Trip Mode Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
            }
            else {
                household.logHouseholdObject( "Pre Trip Mode Choice: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Stop_" + stop.getStopId(), tripMcLogger );
                household.logPersonObject( "Pre Trip Mode Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                household.logTourObject("Pre Trip Mode Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
                household.logStopObject("Pre Trip Mode Choice for stop " + stop.getStopId(), modelLogger, stop, modelStructure );
            }
        }
        

        
        int modelIndex = modelStructure.getTripModeChoiceModelIndex( tour.getTourPrimaryPurpose().toLowerCase() );
        ChoiceModelApplication choiceModel = mcChoiceModelApplication[modelIndex];

        
        PersonIf person = tour.getPersonObject();
        
        String choiceModelDescription = "";
        String separator = "";
        String loggerString = "";
        String decisionMakerLabel = "";

        if ( household.getDebugChoiceModels() ) {
            
            choiceModelDescription = "Trip Mode Choice Model";
            decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId() );


            modelLogger.info(" ");
            loggerString = choiceModelDescription + " for " + decisionMakerLabel + ".";
            for (int k=0; k < loggerString.length(); k++)
                separator += "+";
            modelLogger.info( loggerString );
            modelLogger.info( separator );
            modelLogger.info( "" );
            modelLogger.info( "" );
         
            choiceModel.choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
        
        }

        
        
        choiceModel.computeUtilities( tripModeChoiceDmuObj, tripModeChoiceDmuObj.getDmuIndexValues() );

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make choice.
        int chosen = -1;
        if ( choiceModel.getAvailabilityCount() > 0 )
            chosen = choiceModel.getChoiceResult( rn );

        
        
        // write choice model alternative info to log file
        if ( household.getDebugChoiceModels() || chosen < 0 ) {
            
            double[] utilities     = choiceModel.getUtilities();
            double[] probabilities = choiceModel.getProbabilities();

            String[] altNames = choiceModel.getAlternativeNames();
            
            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum  + ", Person type: " + personTypeString );
            modelLogger.info("Alternative               Utility       Probability           CumProb");
            modelLogger.info("---------------    --------------    --------------    --------------");

            double cumProb = 0.0;

            for(int k=0; k < altNames.length; k++){
                cumProb += probabilities[k];
                String altString = String.format( "%-3d  %25s", k+1, altNames[k] );
                modelLogger.info(String.format("%-30s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
            }

            
            modelLogger.info(" ");
            if ( chosen < 0 ) {
                String altString = "No alternatives available to choose from";
                modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );
                modelLogger.info("tour depart period=" + tour.getTourDepartPeriod() + " tour arrival period=" + tour.getTourArrivePeriod() + " stop period=" + stop.getStopPeriod());
                
                if ( stop == null ) {
                    household.logHouseholdObject( "Pre Trip Mode Choice: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId(), tripMcLogger );
                    household.logPersonObject( "Pre Trip Mode Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                    household.logTourObject("Pre Trip Mode Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
                }
                else {
                    household.logHouseholdObject( "Pre Trip Mode Choice: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Stop_" + stop.getStopId(), tripMcLogger );
                    household.logPersonObject( "Pre Trip Mode Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                    household.logTourObject("Pre Trip Mode Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
                    household.logStopObject("Pre Trip Mode Choice for stop " + stop.getStopId(), modelLogger, stop, modelStructure );
                }
                
            }
            else {
                String altString = String.format( "%-3d  %s", chosen, altNames[chosen-1] );
                modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );
            }

            
            modelLogger.info( separator );
            modelLogger.info( "" );
            modelLogger.info( "" );
        
            
            choiceModel.logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
            choiceModel.logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, chosen );

            
            // write UEC calculation results to separate model specific log file
            choiceModel.logUECResults( modelLogger, loggerString );

        }

        
        
        if ( chosen > 0 )
            return chosen;
        else {
            logger.error ( String.format( "HHID=%d, no available %s trip mode alternatives in tourId=%d to choose from in choiceModelApplication.", household.getHhId(), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourId() ) );
            throw new RuntimeException();
        }

    }
    
    
    
    // determine the trip mode choice logsum for the intermediate stop alternative either as an origin or a destination
    private double calculateTripModeChoiceLogsum( HouseholdIf household, PersonIf person, TourIf tour, StopIf stop, Logger modelLogger, String loggerHeader ){
        
        //determine the trip mode choice logsum for the sampled dest alt and store in stop location dmu
        tripModeChoiceDmuObj.setDmuIndexValues( household.getHhId(), stop.getOrig(), stop.getDest() );
        tripModeChoiceDmuObj.setIntStopParkRate( parkRate[stop.getDest()-1] );
        
        int mcModelIndex = modelStructure.getTripModeChoiceModelIndex( tour.getTourPrimaryPurpose().toLowerCase() );
        mcChoiceModelApplication[mcModelIndex].computeUtilities( tripModeChoiceDmuObj, tripModeChoiceDmuObj.getDmuIndexValues() );
        double logsum = mcChoiceModelApplication[mcModelIndex].getLogsum();

        if ( household.getDebugChoiceModels() ) {
            household.logStopObject(loggerHeader, modelLogger, stop, modelStructure );
            mcChoiceModelApplication[mcModelIndex].logUECResults( modelLogger, loggerHeader );
            modelLogger.info( "" );
            modelLogger.info( "calculated mc logsum: " + logsum );
        }
        
        return logsum;
        
    }
 
    
    // this method is called to determine parking location if stop location is in the CBD and chosen mode is sov or hov.
    private int selectParkingLocation ( HouseholdIf household, TourIf tour, StopIf stop ) {

        Logger modelLogger = parkLocLogger;
                
        if ( household.getDebugChoiceModels() ) {
            if ( stop == null ) {
                household.logHouseholdObject( "Pre Parking Location Choice for tour primary destination: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId(), modelLogger );
                household.logPersonObject( "Pre Parking Location Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                household.logTourObject("Pre Parking Location Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
            }
            else {
                household.logHouseholdObject( "Pre Parking Location Choice for trip: HH_" + household.getHhId() + ", Pers_" + tour.getPersonObject().getPersonNum() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Tour_" + tour.getTourId() + ", Tour Purpose_" + tour.getTourPrimaryPurpose() + ", Stop_" + stop.getStopId(), modelLogger );
                household.logPersonObject( "Pre Parking Location Choice for person " + tour.getPersonObject().getPersonNum(), modelLogger, tour.getPersonObject() );
                household.logTourObject("Pre Parking Location Choice for tour " + tour.getTourId(), modelLogger, tour.getPersonObject(), tour );
                household.logStopObject("Pre Parking Location Choice for stop " + stop.getStopId(), modelLogger, stop, modelStructure );
            }
        }
                
        ChoiceModelApplication choiceModel;
        if ( tour.getTourCategoryIsMandatory() ) {
            PersonIf person = tour.getPersonObject(); 
            switch ( person.getFreeParkingAvailableResult() ) {
                case 1:
                    choiceModel = mandatoryFreePc;
                    break;
                case 2:
                    choiceModel = mandatoryPaidPc;
                    break;
                    
                default:
                    logger.error( String.format("Free parking availability choice for hh=%d was %d, but should have been 1 or 2.", household.getHhId(), person.getFreeParkingAvailableResult()) );
                    throw new RuntimeException();
            }
        }
        else {
            choiceModel = nonMandatoryPc;
        }
        
        parkingChoiceDmuObj.setTourObject(tour);
        
        PersonIf person = tour.getPersonObject();
        
        String choiceModelDescription = "";
        String separator = "";
        String loggerString = "";
        String decisionMakerLabel = "";

        // log headers to traceLogger if the person making the destination choice is from a household requesting trace information
        if ( household.getDebugChoiceModels() ) {
            
            if ( stop == null ) {
                choiceModelDescription = "Parking Location Choice Model for tour primary destination";
                decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourPrimaryPurpose(), tour.getTourId() );
            }
            else {
                choiceModelDescription = "Parking Location Choice Model for trip";
                decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId() );
            }

            modelLogger.info(" ");
            loggerString = choiceModelDescription + " for " + decisionMakerLabel + ".";
            for (int k=0; k < loggerString.length(); k++)
                separator += "+";
            modelLogger.info( loggerString );
            modelLogger.info( separator );
            modelLogger.info( "" );
            modelLogger.info( "" );
         
            choiceModel.choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
        
        }

        
        
        choiceModel.computeUtilities ( parkingChoiceDmuObj, parkingChoiceDmuObj.getDmuIndexValues() );

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make choice.
        int chosen = -1;
        int parkTaz = 0;
        if ( choiceModel.getAvailabilityCount() > 0 ) {
            chosen = choiceModel.getChoiceResult( rn );
            
            // get the zone number associated with the chosen alternative
            parkTaz = (int)cbdAltsTable.getValueAt( chosen, PARK_TAZ_COLUMN );
        }


        
        // write choice model alternative info to log file
        if ( household.getDebugChoiceModels() || chosen < 0 ) {
            
            double[] utilities     = choiceModel.getUtilities();
            double[] probabilities = choiceModel.getProbabilities();

            int[] altTazs = cbdAltsTable.getColumnAsInt( PARK_TAZ_COLUMN );
            
            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();

            modelLogger.info("Person num: " + personNum  + ", Person type: " + personTypeString );
            modelLogger.info("Alternative               Utility       Probability           CumProb");
            modelLogger.info("---------------    --------------    --------------    --------------");

            double cumProb = 0.0;

            for(int k=0; k < altTazs.length; k++){
                int alt = altTazs[k];
                cumProb += probabilities[k];
                String altString = String.format( "%-3d  %5d", k+1, alt );
                modelLogger.info(String.format("%-15s%18.6e%18.6e%18.6e", altString, utilities[k], probabilities[k], cumProb));
            }

            modelLogger.info(" ");
            if ( chosen < 0 ) {
                modelLogger.info( String.format("No Alternatives Available For Choice !!!" ) );
            }
            else {
                String altString = String.format( "%-3d  %5d", chosen, altTazs[chosen-1] );
                modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );
            }

            modelLogger.info( separator );
            modelLogger.info( "" );
            modelLogger.info( "" );
        
            
            choiceModel.logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
            choiceModel.logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, chosen );

            
            // write UEC calculation results to separate model specific log file
            choiceModel.logUECResults( modelLogger, loggerString );

        }

        
        if ( chosen > 0 )
            return parkTaz;
        else {
            logger.error ( String.format( "Exception caught for HHID=%d, no available %s parking location alternatives in tourId=%d to choose from in choiceModelApplication.", household.getHhId(), ModelStructure.TOUR_CATEGORY_LABELS[tour.getTourCategoryIndex()], tour.getTourId() ) );
            throw new RuntimeException();
        }

    }
    
    
    private void setOutboundTripDepartTimes ( StopIf[] stops ) {
        
        // these stops are in outbound direction
        int halfTourDirection = 0;

        for ( int i=0; i < stops.length; i++ ) {
        
            // if tour depart and arrive periods are the same, set same values for the stops
            StopIf stop = stops[i];
            TourIf tour = stop.getTour();
            PersonIf person = tour.getPersonObject();
            HouseholdIf household = person.getHouseholdObject();
            if ( tour.getTourArrivePeriod() == tour.getTourDepartPeriod() ) {

                if ( household.getDebugChoiceModels() ) {
                    tripDepartLogger.info( "Trip Depart Time Model Not Run Since Tour Depart and Arrive Periods are Equal; Stop Depart Period set to Tour Depart Period = " + tour.getTourDepartPeriod() + " for outbound half-tour." );
                    tripDepartLogger.info( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getOrigPurpose(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                    tripDepartLogger.info( "" );
                }
                stop.setStopPeriod( tour.getTourDepartPeriod() );
                
            }
            else {
            
                int tripIndex = i + 1;

                if ( tripIndex == 1 ) {

                    if ( household.getDebugChoiceModels() ) {
                        tripDepartLogger.info( "Trip Depart Time Model Not Run Since Trip is first trip in sequence, departing from " + stop.getOrigPurpose() + "; Stop Depart Period set to Tour Depart Period = " + tour.getTourDepartPeriod() + " for outbound half-tour." );
                        tripDepartLogger.info( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getOrigPurpose(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                        tripDepartLogger.info( "" );
                    }
                    stop.setStopPeriod( tour.getTourDepartPeriod() );
                
                }
                else {
                
                    int tourPrimaryPurposeIndex = tour.getTourPrimaryPurposeIndex();            
        
                    int prevTripPeriod = stops[i-1].getStopPeriod();
                    
                    double[] proportions = stopTodModel.getStopTodIntervalProportions(tourPrimaryPurposeIndex, halfTourDirection, prevTripPeriod, tripIndex);           
    
                    // for inbound trips, the first trip cannot arrive earlier than the last outbound trip departs
                    // if such a case is chosen, re-select.
                    int invalidCount = 0;
                    boolean validTripDepartPeriodSet = false;
                    while ( validTripDepartPeriodSet == false ) {
                        
                        double rn = household.getHhRandom().nextDouble();
                        int choice = getMonteCarloSelection( proportions, rn );
                        
                        // check that this stop depart time departs at same time or later than the stop object preceding this one in the stop sequence.
                        if ( choice >= prevTripPeriod && choice <= tour.getTourArrivePeriod() ) {
                            validTripDepartPeriodSet = true;
                            if ( household.getDebugChoiceModels() ) {
                                tripDepartLogger.info( "Trip Depart Time Model for outbound half-tour." );
                                tripDepartLogger.info( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopOrigPurpose=%s, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getOrigPurpose(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                                tripDepartLogger.info( "tourPrimaryPurposeIndex=" + tourPrimaryPurposeIndex + ", halfTourDirection=" + halfTourDirection + ", tripIndex=" + tripIndex + ", prevTripPeriod=" + prevTripPeriod );
                                tripDepartLogger.info( "" );
                                
                                tripDepartLogger.info( loggerSeparator );
                                tripDepartLogger.info( String.format( "%-4s %-8s  %10s  %10s", "alt", "time", "prob", "cumProb" ) );
                                double cumProb = 0.0;
                                for ( int p=1; p < proportions.length; p++ ){
                                    int hr = 4 + (p/2);
                                    int min = (p%2)*30;
                                    cumProb += proportions[p];
                                    String timeString = ((hr < 10) ? ("0" + hr) : ("" + hr + ":")) + ((min == 30) ? min : "00");
                                    tripDepartLogger.info( String.format( "%-4d  %-8s  %10.8f  %10.8f", p, timeString, proportions[p], cumProb ));
                                }
                                tripDepartLogger.info( loggerSeparator );
                                tripDepartLogger.info( "rn=" + rn + ", choice=" + choice + ", try=" + invalidCount );
                                tripDepartLogger.info( "" );
                            }
                            stop.setStopPeriod( choice );

                        }
                        else {
                            invalidCount++;
                        }
                        
                        if ( invalidCount > MAX_INVALID_FIRST_ARRIVAL_COUNT ){
                            tripDepartLogger.error( "Error in Trip Depart Time Model." );
                            tripDepartLogger.error( "outbound trip depart time less than previous trip depart time or greater than tour arrive time chosen for " + invalidCount + " times." );
                            tripDepartLogger.error( "Possible infinite loop?" );
                            tripDepartLogger.error( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                            throw new RuntimeException();
                        }
                        
                    }
                    
                }
                
            }
            
        }
        
    }

    private void setInboundTripDepartTimes ( StopIf[] stops, int lastOutboundTripDeparts ) {
        
        // these stops are in inbound direction
        int halfTourDirection = 1;

        for ( int i=stops.length-1; i >= 0; i-- ) {
        
            // if tour depart and arrive periods are the same, set same values for the stops
            StopIf stop = stops[i];
            TourIf tour = stop.getTour();
            PersonIf person = tour.getPersonObject();
            HouseholdIf household = person.getHouseholdObject();
            if ( tour.getTourArrivePeriod() == tour.getTourDepartPeriod() ) {

                if ( household.getDebugChoiceModels() ) {
                    tripDepartLogger.info( "Trip Arrive Time Model Not Run Since Tour Depart and Arrive Periods are Equal; Stop Arrive Period set to Tour Arrive Period = " + tour.getTourDepartPeriod() + " for inbound half-tour." );
                    tripDepartLogger.info( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                    tripDepartLogger.info( "" );
                }
                stop.setStopPeriod( tour.getTourArrivePeriod() );
                
            }
            else {

                int tripIndex = stops.length - i;
                
                if ( tripIndex == 1 ) {

                    if ( household.getDebugChoiceModels() ) {
                        tripDepartLogger.info( "Trip Arrive Time Model Not Run Since Trip is last trip in sequence, arriving at " + stop.getDestPurpose() + "; Stop Arrive Period set to Tour Arrive Period = " + tour.getTourArrivePeriod() + " for inbound half-tour." );
                        tripDepartLogger.info( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                        tripDepartLogger.info( "" );
                    }
                    stop.setStopPeriod( tour.getTourArrivePeriod() );
                
                }
                else {

                    int tourPrimaryPurposeIndex = tour.getTourPrimaryPurposeIndex();            
        
                    int prevTripPeriod = stops[i+1].getStopPeriod();
                
                    double[] proportions = stopTodModel.getStopTodIntervalProportions(tourPrimaryPurposeIndex, halfTourDirection, prevTripPeriod, tripIndex);           
                    
                    // for inbound trips, the first trip cannot arrive earlier than the last outbound trip departs
                    // if such a case is chosen, re-select.
                    int invalidCount = 0;
                    boolean validTripArrivePeriodSet = false;
                    while ( validTripArrivePeriodSet == false ) {
                        
                        double rn = household.getHhRandom().nextDouble();
                        int choice = getMonteCarloSelection( proportions, rn );
                        
                        // check that this stop arrival time arrives at same time or earlier than the stop object following this one in the stop sequence.
                        // also check that this stop arrival is after the depart time for the last outbound stop.
                        if ( choice <= prevTripPeriod && choice >= lastOutboundTripDeparts ) {
                            validTripArrivePeriodSet = true;
                            if ( household.getDebugChoiceModels() ) {
                                tripDepartLogger.info( "Trip Arrive Time Model for inbound half-tour." );
                                tripDepartLogger.info( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                                tripDepartLogger.info( "tourPrimaryPurposeIndex=" + tourPrimaryPurposeIndex + ", halfTourDirection=" + halfTourDirection + ", tripIndex=" + tripIndex + ", prevTripPeriod=" + prevTripPeriod );
                                tripDepartLogger.info( loggerSeparator );
                                tripDepartLogger.info( "" );
                                
                                tripDepartLogger.info( String.format( "%-4s %-8s  %10s  %10s", "alt", "time", "prob", "cumProb" ) );
                                double cumProb = 0.0;
                                for ( int p=1; p < proportions.length; p++ ){
                                    int hr = 4 + (p/2);
                                    int min = (p%2)*30;
                                    cumProb += proportions[p];
                                    String timeString = ((hr < 10) ? ("0" + hr) : ("" + hr + ":")) + ((min == 30) ? min : "00");
                                    tripDepartLogger.info( String.format( "%-4d  %-8s  %10.8f  %10.8f", p, timeString, proportions[p], cumProb ));
                                }
                                tripDepartLogger.info( loggerSeparator );
                                tripDepartLogger.info( "rn=" + rn + ", choice=" + choice + ", try=" + invalidCount );
                                tripDepartLogger.info( "" );
                            }
                            stop.setStopPeriod( choice );
                        }
                        else {
                            invalidCount++;
                        }
                        
                        if ( invalidCount > MAX_INVALID_FIRST_ARRIVAL_COUNT ){
                            tripDepartLogger.error( "Error in Trip Arrive Time Model." );
                            tripDepartLogger.error( "First inbound trip arrival time less than last outbound trip depart time chosen for " + invalidCount + " times." );
                            tripDepartLogger.error( "Possible infinite loop?" );
                            tripDepartLogger.error( String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourMode=%d, TourPurpose=%s, TourId=%d, StopDestPurpose=%s, StopId=%d, inboundStopsArray Length=%d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourModeChoice(), tour.getTourPrimaryPurpose(), tour.getTourId(), stop.getDestPurpose(), stop.getStopId(), stops.length ) );
                            throw new RuntimeException();
                        }
                            
                    }

                }
            
            }
            
        }
        
    }

    /**
     * 
     * @param probabilities has 1s based indexing
     * @param randomNumber
     * @return
     */
    private int getMonteCarloSelection (double[] probabilities, double randomNumber) {

        int returnValue = 0;
        double sum = probabilities[1];
        // probabilities array passded into this method is 1s based.
        for (int i=1; i < probabilities.length-1; i++) {
            if (randomNumber <= sum) {
                returnValue = i;
                break;
            }
            else {
                sum += probabilities[i+1];
                returnValue = i+1;
            }
        }
        return returnValue;
    }
 
    public long[] getStopTimes() {
        hhTimes[0] = soaTime;
        hhTimes[1] = slsTime;
        hhTimes[2] = slcTime - (soaTime + slsTime);
        hhTimes[3] = slcTime;
        hhTimes[4] = todTime;
        hhTimes[5] = smcTime;
        hhTimes[6] = plcTime;
               
        hhTimes[7] = hhTimes[3] + todTime + smcTime + plcTime;
        
        return hhTimes;
    }
    
    public void cleanUp() {
        dcSoaModel.cleanUp();
    }
    
    private void setTVPBValues(TripModeChoiceDMU tripModeChoiceDmuObj, int inbound, String purpose, boolean recalcPersonUtil, boolean debug) {
    	
    	int NA_VALUE = 0;

    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu = new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	walkDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	driveDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	
    	if(purpose.toLowerCase().startsWith("work")) {
    		walkDmu.setUserClass(tripModeChoiceDmuObj.getTourObject().getPersonObject().getUserClass("user_class_work_walk"));
    		driveDmu.setUserClass(tripModeChoiceDmuObj.getTourObject().getPersonObject().getUserClass("user_class_work_pnr"));
    	} else {
    		walkDmu.setUserClass(tripModeChoiceDmuObj.getTourObject().getPersonObject().getUserClass("user_class_non_work_walk"));
    		driveDmu.setUserClass(tripModeChoiceDmuObj.getTourObject().getPersonObject().getUserClass("user_class_non_work_pnr"));
    	}    	
    	
    	walkDmu.setWalkPropClass(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkPropClass());
    	driveDmu.setWalkPropClass(walkDmu.getWalkPropClass());
    	
    	//check for existing best taps and calculate utilities if needed
    	int omaz = tripModeChoiceDmuObj.getDmuIndexValues().getOriginZone();
    	int dmaz = tripModeChoiceDmuObj.getDmuIndexValues().getDestZone();
    	int tod = tripModeChoiceDmuObj.getTodOut();
    	
    	//run TVPB by tour mode    	
    	tripModeChoiceDmuObj.setGenCostWT( NA_VALUE );
    	tripModeChoiceDmuObj.setOtapWT( NA_VALUE );
    	tripModeChoiceDmuObj.setDtapWT( NA_VALUE );			
    	tripModeChoiceDmuObj.setGenCostDL( NA_VALUE );
    	tripModeChoiceDmuObj.setOtapDL( NA_VALUE );
    	tripModeChoiceDmuObj.setDtapDL( NA_VALUE );
    	tripModeChoiceDmuObj.setGenCostDP( NA_VALUE );
    	tripModeChoiceDmuObj.setOtapDP( NA_VALUE );
    	tripModeChoiceDmuObj.setDtapDP( NA_VALUE );
    	
    	//walk
		if(tripModeChoiceDmuObj.getTourMode() == ModelStructure.WP_ALT) {
			
			double[][] bestWtwTapPairs = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTW, omaz, dmaz, tod, debug, logger);    	
	    	
			if(recalcPersonUtil & bestWtwTapPairs[0] != null) {
	    		walkDmu.setWalkTimeWeight(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkTimeWeight());
	    		walkDmu.setWalkSpeed(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkSpeed());
	    		walkDmu.setMaxWalk(tripModeChoiceDmuObj.getTourObject().getPersonObject().getMaxWalk());
	    		walkDmu.setValueOfTime(tripModeChoiceDmuObj.getTourObject().getPersonObject().getValueOfTime());
	    		
	    		driveDmu.setAge(tripModeChoiceDmuObj.getTourObject().getPersonObject().getAge());
	    		driveDmu.setCars(tripModeChoiceDmuObj.getHouseholdObject().getAutoOwnershipModelResult());
	    		driveDmu.setWalkTimeWeight(walkDmu.getWalkTimeWeight());
	    		driveDmu.setWalkSpeed(walkDmu.getWalkSpeed());
	    		driveDmu.setMaxWalk(walkDmu.getMaxWalk());
	    		driveDmu.setValueOfTime(walkDmu.getValueOfTime());
	    		
	    		bestWtwTapPairs = tvpb.calcPersonSpecificUtilities(bestWtwTapPairs, walkDmu, driveDmu, tvpb.WTW, omaz, dmaz, tod, false, debug, logger);
			}
			
			if (bestWtwTapPairs[0] == null) {
	    		tripModeChoiceDmuObj.setOtapWT( NA_VALUE );
	    		tripModeChoiceDmuObj.setDtapWT( NA_VALUE );
	    		tripModeChoiceDmuObj.setGenCostWT( NA_VALUE );
	        } else {
	        	tripModeChoiceDmuObj.setOtapWT( (int) bestWtwTapPairs[0][0] );
	        	tripModeChoiceDmuObj.setDtapWT( (int) bestWtwTapPairs[0][1] );
	        	tripModeChoiceDmuObj.setGenCostWT( (float) bestWtwTapPairs[0][3] );
	        }
		}
		
		//knr uses pnr for now
		if(tripModeChoiceDmuObj.getTourMode() == ModelStructure.DL_ALT) {
			
			double[][] bestDLTapPairs;
			if(inbound==1) {
				bestDLTapPairs = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTD, omaz, dmaz, tod, debug, logger);	
			} else {
				bestDLTapPairs = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.DTW, omaz, dmaz, tod, debug, logger);
			}
			
			if(recalcPersonUtil & bestDLTapPairs[0] != null) {
	    		walkDmu.setWalkTimeWeight(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkTimeWeight());
	    		walkDmu.setWalkSpeed(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkSpeed());
	    		walkDmu.setMaxWalk(tripModeChoiceDmuObj.getTourObject().getPersonObject().getMaxWalk());
	    		walkDmu.setValueOfTime(tripModeChoiceDmuObj.getTourObject().getPersonObject().getValueOfTime());
	    		
	    		driveDmu.setAge(tripModeChoiceDmuObj.getTourObject().getPersonObject().getAge());
	    		driveDmu.setCars(tripModeChoiceDmuObj.getHouseholdObject().getAutoOwnershipModelResult());
	    		driveDmu.setWalkTimeWeight(walkDmu.getWalkTimeWeight());
	    		driveDmu.setWalkSpeed(walkDmu.getWalkSpeed());
	    		driveDmu.setMaxWalk(walkDmu.getMaxWalk());
	    		driveDmu.setValueOfTime(walkDmu.getValueOfTime());
	    		
	    		if(inbound==1) {
	    			bestDLTapPairs = tvpb.calcPersonSpecificUtilities(bestDLTapPairs, walkDmu, driveDmu, tvpb.WTD, omaz, dmaz, tod, false, debug, logger);
				} else {					
					bestDLTapPairs = tvpb.calcPersonSpecificUtilities(bestDLTapPairs, walkDmu, driveDmu, tvpb.DTW, omaz, dmaz, tod, false, debug, logger);
				}	    		
	    		
			}
			
			if (bestDLTapPairs[0] == null) {
	    		tripModeChoiceDmuObj.setOtapDL( NA_VALUE );
	    		tripModeChoiceDmuObj.setDtapDL( NA_VALUE );
	    		tripModeChoiceDmuObj.setGenCostDL( NA_VALUE );
	        } else {
	        	tripModeChoiceDmuObj.setOtapDL( (int) bestDLTapPairs[0][0] );
	        	tripModeChoiceDmuObj.setDtapDL( (int) bestDLTapPairs[0][1] );
	        	tripModeChoiceDmuObj.setGenCostDL( (float) bestDLTapPairs[0][3] );
	        }
		}
		
		//pnr
		if(tripModeChoiceDmuObj.getTourMode() == ModelStructure.DP_ALT) {
			
			double[][] bestDPTapPairs;
			if(inbound==1) {
				bestDPTapPairs = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTD, omaz, dmaz, tod, debug, logger);	
			} else {
				bestDPTapPairs = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.DTW, omaz, dmaz, tod, debug, logger);
			}
	    	
			if(recalcPersonUtil & bestDPTapPairs[0] != null) {
	    		walkDmu.setWalkTimeWeight(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkTimeWeight());
	    		walkDmu.setWalkSpeed(tripModeChoiceDmuObj.getTourObject().getPersonObject().getWalkSpeed());
	    		walkDmu.setMaxWalk(tripModeChoiceDmuObj.getTourObject().getPersonObject().getMaxWalk());
	    		walkDmu.setValueOfTime(tripModeChoiceDmuObj.getTourObject().getPersonObject().getValueOfTime());
	    		
	    		driveDmu.setAge(tripModeChoiceDmuObj.getTourObject().getPersonObject().getAge());
	    		driveDmu.setCars(tripModeChoiceDmuObj.getHouseholdObject().getAutoOwnershipModelResult());
	    		driveDmu.setWalkTimeWeight(walkDmu.getWalkTimeWeight());
	    		driveDmu.setWalkSpeed(walkDmu.getWalkSpeed());
	    		driveDmu.setMaxWalk(walkDmu.getMaxWalk());
	    		driveDmu.setValueOfTime(walkDmu.getValueOfTime());
	    		
	    		if(inbound==1) {
	    			bestDPTapPairs = tvpb.calcPersonSpecificUtilities(bestDPTapPairs, walkDmu, driveDmu, tvpb.WTD, omaz, dmaz, tod, false, debug, logger);
				} else {					
					bestDPTapPairs = tvpb.calcPersonSpecificUtilities(bestDPTapPairs, walkDmu, driveDmu, tvpb.DTW, omaz, dmaz, tod, false, debug, logger);
				}	    		
	    		
			}
			
			if (bestDPTapPairs[0] == null) {
	    		tripModeChoiceDmuObj.setOtapDP( NA_VALUE );
	    		tripModeChoiceDmuObj.setDtapDP( NA_VALUE );
	    		tripModeChoiceDmuObj.setGenCostDP( NA_VALUE );
	        } else {
	        	tripModeChoiceDmuObj.setOtapDP( (int) bestDPTapPairs[0][0] );
	        	tripModeChoiceDmuObj.setDtapDP( (int) bestDPTapPairs[0][1] );
	        	tripModeChoiceDmuObj.setGenCostDP( (float) bestDPTapPairs[0][3] );
	        }
		}
		
	}
    
    public void setStopTaps(StopIf stop, TripModeChoiceDMU tripModeChoiceDmuObj, int choice) {
    	
    	//set taps for transit tour mode
        if(choice==ModelStructure.WP_ALT) {
        	stop.setBTap(tripModeChoiceDmuObj.getOtapWT());
        	stop.setATap(tripModeChoiceDmuObj.getDtapWT());
        } else if (choice==ModelStructure.DL_ALT) {
        	stop.setBTap(tripModeChoiceDmuObj.getOtapDL());
        	stop.setATap(tripModeChoiceDmuObj.getDtapDL());
        } else if (choice==ModelStructure.DP_ALT) {
        	stop.setBTap(tripModeChoiceDmuObj.getOtapDP());
        	stop.setATap(tripModeChoiceDmuObj.getDtapDP());
        }
    	
    }
    
}