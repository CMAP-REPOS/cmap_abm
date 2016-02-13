package com.pb.models.ctrampIf.jppf;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.TourModeChoiceDMU;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TourDepartureTimeAndDurationDMU;
import com.pb.models.ctrampIf.TourIf;
import com.pb.common.newmodel.ChoiceModelApplication;

/**
 * Created by IntelliJ IDEA.
 * User: Jim
 * Date: Jul 11, 2008
 * Time: 9:25:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class HouseholdIndividualMandatoryTourDepartureAndDurationTime implements Serializable {


    private transient Logger logger = Logger.getLogger( HouseholdIndividualMandatoryTourDepartureAndDurationTime.class );
    private transient Logger todLogger = Logger.getLogger( "todLogger" );
    private transient Logger tourMCManLogger = Logger.getLogger( "tourMcMan" );

    private static final String PROPERTIES_UEC_MAND_TOUR_DEP_TIME_AND_DUR = "UecFile.TourDepartureTimeAndDuration";


    // all the other page numbers are passed in
    private static final int UEC_DATA_PAGE = 0;
    private static final int UEC_WORK_MODEL_PAGE    = 1;
    private static final int UEC_UNIV_MODEL_PAGE    = 2;
    private static final int UEC_SCHOOL_MODEL_PAGE    = 3;

    //work tod alts to log for the UEC logging
    private static final int workAltsToLog = 300;

    private int[] areaType;
    private int[] zoneTableRow;

    private int[] workTourDepartureTimeChoiceSample;
    private int[] schoolTourDepartureTimeChoiceSample;


    // DMU for the UEC
    private TourDepartureTimeAndDurationDMU imtodDmuObject;
    private TourModeChoiceDMU mcDmuObject;
    
    // model structure to compare the .properties time of day with the UECs
    private ModelStructure modelStructure;
    private String tourCategory = ModelStructure.MANDATORY_CATEGORY;

    private ChoiceModelApplication workTourChoiceModel;
    private ChoiceModelApplication schoolTourChoiceModel;
    private ChoiceModelApplication univTourChoiceModel;
    private ModeChoiceModel mcModel;


    private boolean[] needToComputeLogsum;
    private double[] modeChoiceLogsums;

    private int[] altStarts;
    private int[] altEnds;
    
    private int noAvailableWorkWindowCount = 0;
    private int noAvailableSchoolWindowCount = 0;

    private int noUsualWorkLocationForMandatoryActivity = 0;
    private int noUsualSchoolLocationForMandatoryActivity = 0;




    public HouseholdIndividualMandatoryTourDepartureAndDurationTime( HashMap<String, String> propertyMap, TazDataIf tazDataManager, ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory, ModeChoiceModel mcModel ){

        setupHouseholdIndividualMandatoryTourDepartureAndDurationTime ( propertyMap, tazDataManager, modelStructure, dmuFactory, mcModel );

    }


    private void setupHouseholdIndividualMandatoryTourDepartureAndDurationTime( HashMap<String, String> propertyMap, TazDataIf tazDataManager, ModelStructure modelStructure, CtrampDmuFactoryIf dmuFactory, ModeChoiceModel mcModel ){

        logger.info( String.format( "setting up %s time-of-day choice model.", tourCategory ) );
        
        // set the model structure
        this.modelStructure = modelStructure;
        this.mcModel = mcModel;
        

        zoneTableRow = tazDataManager.getZoneTableRowArray();

        // the zone table columns below returned use 0-based indexing
        areaType = tazDataManager.getZonalAreaType();

        
        // locate the individual mandatory tour frequency choice model UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
        String imtodUecFile = propertyMap.get( PROPERTIES_UEC_MAND_TOUR_DEP_TIME_AND_DUR );
        imtodUecFile = projectDirectory + imtodUecFile;

        // get the dmu objects from the factory
        imtodDmuObject = dmuFactory.getTourDepartureTimeAndDurationDMU();
        mcDmuObject = dmuFactory.getModeChoiceDMU();
        

        // set up the models
        workTourChoiceModel = new ChoiceModelApplication( imtodUecFile, UEC_WORK_MODEL_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)imtodDmuObject );
        schoolTourChoiceModel = new ChoiceModelApplication( imtodUecFile, UEC_SCHOOL_MODEL_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable)imtodDmuObject );
        univTourChoiceModel = new ChoiceModelApplication(imtodUecFile, UEC_UNIV_MODEL_PAGE, UEC_DATA_PAGE, propertyMap, (VariableTable) imtodDmuObject);
        
        // get the alternatives table from the work tod UEC.
        TableDataSet altsTable = workTourChoiceModel.getUEC().getAlternativeData();
        altStarts = altsTable.getColumnAsInt( CtrampApplication.START_FIELD_NAME );
        altEnds = altsTable.getColumnAsInt( CtrampApplication.END_FIELD_NAME );
        altsTable = null;

        imtodDmuObject.setTodAlts(altStarts, altEnds);
        
        
        int numWorkDepartureTimeChoiceAlternatives = workTourChoiceModel.getNumberOfAlternatives();
        workTourDepartureTimeChoiceSample = new int[numWorkDepartureTimeChoiceAlternatives+1];
        Arrays.fill(workTourDepartureTimeChoiceSample, 1);

        int numSchoolDepartureTimeChoiceAlternatives = schoolTourChoiceModel.getNumberOfAlternatives();
        schoolTourDepartureTimeChoiceSample = new int[numSchoolDepartureTimeChoiceAlternatives+1];
        Arrays.fill(schoolTourDepartureTimeChoiceSample, 1);

        
        
        int numLogsumIndices = modelStructure.getPeriodCombinationIndices().length;
        needToComputeLogsum = new boolean[numLogsumIndices];
        
        modeChoiceLogsums = new double[numLogsumIndices];
        
        
        String[] tourPurposeList = modelStructure.getDcModelPurposeList( tourCategory );
        int workPurposeIndex = -1;
        int univPurposeIndex = -1;
        int schlPurposeIndex = -1;
        for ( int i=0; i < tourPurposeList.length; i++ ) {
            if ( workPurposeIndex < 0 && modelStructure.getDcModelPurposeIsWorkPurpose( tourPurposeList[i] ))
                workPurposeIndex = i;
            else if ( univPurposeIndex < 0 && modelStructure.getDcModelPurposeIsUniversityPurpose( tourPurposeList[i] ))
                univPurposeIndex = i;
            else if ( schlPurposeIndex < 0 && modelStructure.getDcModelPurposeIsSchoolPurpose( tourPurposeList[i] ))
                schlPurposeIndex = i;
        }

    }


    public void applyModel( HouseholdIf household, boolean runModeChoice ){

        Logger modelLogger = todLogger;
        if ( household.getDebugChoiceModels() ) {
            household.logHouseholdObject( "Pre Individual Mandatory Departure Time Choice Model HHID=" + household.getHhId(), modelLogger );
            household.logHouseholdObject( "Pre Individual Mandatory Tour Mode Choice Model HHID=" + household.getHhId(), tourMCManLogger );
        }
        
        // set the household id, origin taz, hh taz, and debugFlag=false in the dmu
        imtodDmuObject.setHousehold(household);



        // get the array of persons for this household
        PersonIf[] personArray = household.getPersons();

        // loop through the persons (1-based array)
        for(int j=1;j<personArray.length;++j){

            PersonIf person = personArray[j];
            person.resetTimeWindow();


            if ( household.getDebugChoiceModels() ) {
                String decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s", household.getHhId(), person.getPersonNum(), person.getPersonType() );
                household.logPersonObject( decisionMakerLabel, modelLogger, person );
                household.logPersonObject( decisionMakerLabel, tourMCManLogger, person );
            }
            

            // mandatory tour departure time and dureation choice models for each worker/student require a specific order:
            // 1. Work tours made by workers, school/university tours made by students.
            // 2. Work tours made by students, school/university tours made by workers.
            //TODO: check consistency of these definitions -
            //TODO: workers can also be students (school-age and university)?, non-driving students can be workers?,
            //TODO: cannot be school-age student and university? etc...

            try {

                if ( person.getPersonIsWorker() == 1 ) {
                    applyDepartureTimeChoiceForWorkTours( person, runModeChoice );
                    if ( person.getListOfSchoolTours().size() > 0 ){
                        if(person.getPersonIsUniversityStudent() == 1) {
                            applyDepartureTimeChoiceForUnivTours(person, runModeChoice);
                        }
                        else {
                            applyDepartureTimeChoiceForSchoolTours(person, runModeChoice);
                        }
                    }
                }
                else if ( person.getPersonIsStudent() == 1 || person.getPersonIsPreschoolChild() == 1 ) {
                    if(person.getPersonIsUniversityStudent() == 1) {
                        applyDepartureTimeChoiceForUnivTours(person, runModeChoice);
                    }
                    else {
                        applyDepartureTimeChoiceForSchoolTours(person, runModeChoice);
                    }
                    if (person.getListOfWorkTours().size() > 0)
                        applyDepartureTimeChoiceForWorkTours(person, runModeChoice);
                }
                else {
                    if ( person.getListOfWorkTours().size() > 0 || person.getListOfSchoolTours().size() > 0 ) {
                        logger.error( String.format( "error mandatory departure time choice model for j=%d, hhId=%d, persNum=%d, personType=%s.", j, person.getHouseholdObject().getHhId(), person.getPersonNum(), person.getPersonType() ));
                        logger.error( String.format( "person with type other than worker or student has %d work tours and %d school tours.", person.getListOfWorkTours().size(), person.getListOfSchoolTours().size() ) );
                        throw new RuntimeException();
                    }
                }

            }
            catch ( Exception e ) {
                logger.error( String.format( "error mandatory departure time choice model for j=%d, hhId=%d, persId=%d, persNum=%d, personType=%s.", j, person.getHouseholdObject().getHhId(), person.getPersonId(), person.getPersonNum(), person.getPersonType() ));
                throw new RuntimeException();
            }

        }

        
        household.setImtodRandomCount( household.getHhRandomCount() );

        
    }


    /**
     *
     * @param person object for which time choice should be made
     * @return the number of work tours this person had scheduled.
     */
    private int applyDepartureTimeChoiceForWorkTours( PersonIf person, boolean runModeChoice ) {

        Logger modelLogger = todLogger;
        
        // set the dmu object
        imtodDmuObject.setPerson(person);

        HouseholdIf household = person.getHouseholdObject();        
        
        ArrayList<TourIf> workTours = person.getListOfWorkTours();

        for ( int i=0; i < workTours.size(); i++ ) {

            TourIf t = workTours.get(i);

            // dest taz was set from result of usual school location choice when tour object was created in mandatory tour frequency model.
            //TODO: if the destinationTaz value is -1, then this mandatory tour was created for a non-student (retired probably)
            //TODO: and we have to resolve this somehow - either genrate a work/school location for retired, or change activity type for person.
            //TODO: for now, we'll just skip the tour, and keep count of them.
            int destinationTaz = t.getTourDestTaz();
            if ( destinationTaz <= 0 ) {
                noUsualWorkLocationForMandatoryActivity++;
                continue;
            }

            
            
            // write debug header
            String separator = "";
            String choiceModelDescription = "" ;
            String decisionMakerLabel = "";
            String loggingHeader = "";
            if( household.getDebugChoiceModels() ) {

                choiceModelDescription = String.format ( "Individual Mandatory Work Tour Departure Time Choice Model for: Purpose=%s", t.getTourPrimaryPurpose() );
                decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person.getPersonNum(), person.getPersonType(), t.getTourId(), workTours.size() );
                
                workTourChoiceModel.choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
                    
                modelLogger.info(" ");
                String loggerString = "Individual Mandatory Work Tour Departure Time Choice Model: Debug Statement for Household ID: " + household.getHhId() + ", Person Num: " + person.getPersonNum() + ", Person Type: " + person.getPersonType() + ", Work Tour Id: " + t.getTourId() + " of " + workTours.size() + " work tours.";
                for (int k=0; k < loggerString.length(); k++)
                    separator += "+";
                modelLogger.info( loggerString );
                modelLogger.info( separator );
                modelLogger.info( "" );
                modelLogger.info( "" );
             
                loggingHeader = String.format( "%s    %s", choiceModelDescription, decisionMakerLabel );
                
            }


            
            
            imtodDmuObject.setDestinationZone( destinationTaz );

            // set the dmu object
            imtodDmuObject.setTour( t );


            int originTaz = t.getTourOrigTaz();
            imtodDmuObject.setOriginZone( originTaz );


            // get and set the area types for the tour origin and usual work location zones
            int tableRow = zoneTableRow[originTaz];
            imtodDmuObject.setOriginAreaType( areaType[tableRow-1] );

            tableRow = zoneTableRow[destinationTaz];
            imtodDmuObject.setDestinationAreaType( areaType[tableRow-1] );

            

            
            
            
            // set the choice availability and initialize sample array -
            // choicemodelapplication will change sample[] according to availability[]
            boolean[] departureTimeChoiceAvailability = person.getAvailableTimeWindows(altStarts, altEnds);
            Arrays.fill(workTourDepartureTimeChoiceSample, 1);

            if (departureTimeChoiceAvailability.length != workTourDepartureTimeChoiceSample.length)
            {
                logger.error( String.format(
                    "error in work departure time choice model for hhId=%d, persId=%d, persNum=%d, work tour %d of %d.",
                    person.getHouseholdObject().getHhId(), person.getPersonId(), person.getPersonNum(), i, workTours.size()) );
                logger.error(String .format(
                    "length of the availability array determined by the number of alternatiuves set in the person scheduler=%d", departureTimeChoiceAvailability.length) );
                logger.error(String.format(
                    "does not equal the length of the sample array determined by the number of alternatives in the work tour UEC=%d.", workTourDepartureTimeChoiceSample.length) );
                throw new RuntimeException();
            }

            // if no time window is available for the tour, make the first and last alternatives available
            // for that alternative, and keep track of the number of times this condition occurs.
            boolean noAlternativeAvailable = true;
            for (int a = 0; a < departureTimeChoiceAvailability.length; a++)
            {
                if (departureTimeChoiceAvailability[a])
                {
                    noAlternativeAvailable = false;
                    break;
                }
            }

            if (noAlternativeAvailable)
            {
                noAvailableWorkWindowCount++;
                departureTimeChoiceAvailability[1] = true;
                departureTimeChoiceAvailability[departureTimeChoiceAvailability.length - 1] = true;
            }

            
            // check for multiple tours for this person
            // set the first or second switch if multiple tours for person
            if ( workTours.size() == 1 && person.getListOfSchoolTours().size() == 0 ) {
                // not a multiple tour pattern
                imtodDmuObject.setFirstTour( 0 );
                imtodDmuObject.setSubsequentTour( 0 );
                imtodDmuObject.setTourNumber( 1 );
                imtodDmuObject.setEndOfPreviousScheduledTour( 0 );
                imtodDmuObject.setSubsequentTourIsWork(0);
                imtodDmuObject.setSubsequentTourIsSchool(0);
            }
            else if ( workTours.size() > 1 && person.getListOfSchoolTours().size() == 0 ) {
                // Two work tour multiple tour pattern
                if ( i == 0 ) {
                    // first of 2 work tours
                    imtodDmuObject.setFirstTour( 1 );
                    imtodDmuObject.setSubsequentTour( 0 );
                    imtodDmuObject.setTourNumber( i + 1 );
                    imtodDmuObject.setEndOfPreviousScheduledTour( 0 );
                    imtodDmuObject.setSubsequentTourIsWork(1);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                }
                else {
                    // second of 2 work tours
                    imtodDmuObject.setFirstTour( 0 );
                    imtodDmuObject.setSubsequentTour( 1 );
                    imtodDmuObject.setTourNumber( i + 1 );
                    int otherTourArrivePeriod = workTours.get(0).getTourArrivePeriod();
                    imtodDmuObject.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                    
                    // block alternatives for this second work tour with depart <= first work tour departure AND arrive >= first work tour arrival. 
                    for (int a = 1; a <= altStarts.length; a++)
                    {
                        // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
                        if ( ! departureTimeChoiceAvailability[a] )
                            continue;
                        
                        int startPeriod = altStarts[a - 1];
                        int endPeriod = altEnds[a - 1];

                        if ( startPeriod <= workTours.get(0).getTourDepartPeriod() && endPeriod >= workTours.get(0).getTourArrivePeriod() )
                            departureTimeChoiceAvailability[a] = false;
                    }
                }
            }
            else if ( workTours.size() == 1 && person.getListOfSchoolTours().size() == 1 ) {
                // One work tour, one school tour multiple tour pattern
                if ( person.getPersonIsWorker() == 1 ) {
                    // worker, so work tour is first scheduled, school tour comes later.
                    imtodDmuObject.setFirstTour( 1 );
                    imtodDmuObject.setSubsequentTour( 0 );
                    imtodDmuObject.setTourNumber( 1 );
                    imtodDmuObject.setEndOfPreviousScheduledTour( 0 );
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(1);
                }
                else {
                    // student, so school tour was already scheduled, this work tour is the second.
                    imtodDmuObject.setFirstTour( 0 );
                    imtodDmuObject.setSubsequentTour( 1 );
                    imtodDmuObject.setTourNumber( i + 1 );
                    int otherTourArrivePeriod = person.getListOfSchoolTours().get(0).getTourArrivePeriod();
                    imtodDmuObject.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(0);

                    // block alternatives for this work tour with depart <= first school tour departure AND arrive >= first school tour arrival. 
                    for (int a = 1; a <= altStarts.length; a++)
                    {
                        // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
                        if ( ! departureTimeChoiceAvailability[a] )
                            continue;
                        
                        int startPeriod = altStarts[a - 1];
                        int endPeriod = altEnds[a - 1];

                        if ( startPeriod <= person.getListOfSchoolTours().get(0).getTourDepartPeriod() && endPeriod >= person.getListOfSchoolTours().get(0).getTourArrivePeriod() )
                            departureTimeChoiceAvailability[a] = false;
                    }
                }
            }

            
            // calculate and store the mode choice logsum for the usual work location
            // for this worker at the various
            // departure time and duration alternativees
            setWorkTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(person, t, departureTimeChoiceAvailability);

            
            
            if( household.getDebugChoiceModels() ){
                household.logTourObject( loggingHeader, modelLogger, person, t );
            }


            workTourChoiceModel.computeUtilities ( imtodDmuObject, imtodDmuObject.getIndexValues(), departureTimeChoiceAvailability, workTourDepartureTimeChoiceSample );


            Random hhRandom = imtodDmuObject.getDmuHouseholdObject().getHhRandom();
            int randomCount = household.getHhRandomCount();
            double rn = hhRandom.nextDouble();


            // if the choice model has no available alternatives, choose between the first and last alternative.
            int chosen;
            if ( workTourChoiceModel.getAvailabilityCount() > 0 )
                chosen = workTourChoiceModel.getChoiceResult( rn );
            else
                chosen = rn < 0.5 ? 1 : altStarts.length;



            // schedule the chosen alternative
            int chosenStartPeriod = altStarts[chosen-1];
            int chosenEndPeriod = altEnds[chosen-1];
            person.scheduleWindow( chosenStartPeriod, chosenEndPeriod );

            t.setTourDepartPeriod( chosenStartPeriod );
            t.setTourArrivePeriod( chosenEndPeriod );

            
            // debug output
            if( household.getDebugChoiceModels() ){

                double[] utilities     = workTourChoiceModel.getUtilities();
                double[] probabilities = workTourChoiceModel.getProbabilities();
                boolean[] availabilities = workTourChoiceModel.getAvailabilities();

                String personTypeString = person.getPersonType();
                int personNum = person.getPersonNum();
                modelLogger.info("Person num: " + personNum  + ", Person type: " + personTypeString + ", Tour Id: " + t.getTourId() );
                modelLogger.info("Alternative            Availability           Utility       Probability           CumProb");
                modelLogger.info("--------------------   ------------    --------------    --------------    --------------");

                double cumProb = 0.0;
                for(int k=0; k < workTourChoiceModel.getNumberOfAlternatives(); k++){
                    cumProb += probabilities[k];
                    String altString = String.format( "%-3d out=%-3d, in=%-3d", k+1, altStarts[k], altEnds[k] );
                    modelLogger.info( String.format( "%-20s%15s%18.6e%18.6e%18.6e", altString, availabilities[k+1], utilities[k], probabilities[k], cumProb ) );
                }

                modelLogger.info(" ");
                String altString = String.format( "%-3d out=%-3d, in=%-3d", chosen, altStarts[chosen-1], altEnds[chosen-1] );
                modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );

                modelLogger.info( separator );
                modelLogger.info("");
                modelLogger.info("");


                // write choice model alternative info to debug log file
                workTourChoiceModel.logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
                workTourChoiceModel.logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, chosen );

                // write UEC calculation results to separate model specific log file
                loggingHeader = String.format("%s  %s", choiceModelDescription, decisionMakerLabel );
                workTourChoiceModel.logUECResults( modelLogger, loggingHeader, workAltsToLog );
                
            }


            if (runModeChoice)
            {
    
                // set the mode choice attributes needed by @variables in the UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, t, chosenStartPeriod, chosenEndPeriod);
                
                // use the mcModel object already setup for computing logsums and get
                // the mode choice, where the selected
                // worklocation and subzone an departure time and duration are set
                // for this work tour.
                int chosenMode = mcModel.getModeChoice(mcDmuObject, t.getTourPrimaryPurpose());
                t.setTourModeChoice(chosenMode);
                
                //set tour taps
                mcModel.setTourTaps(t, mcDmuObject, chosenMode);
    
            }

        }
        
        if ( household.getDebugChoiceModels() ) {
            String decisionMakerLabel = String.format ( "Final Work Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s", household.getHhId(), person.getPersonNum(), person.getPersonType() );
            household.logPersonObject( decisionMakerLabel, modelLogger, person );
        }
        
        return workTours.size();

    }


    private void setWorkTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(PersonIf person, TourIf tour, boolean[] altAvailable)
    {

        HouseholdIf household = person.getHouseholdObject();

        Arrays.fill(needToComputeLogsum, true);
        Arrays.fill(modeChoiceLogsums, -999);

        Logger modelLogger = todLogger;
        String choiceModelDescription = String.format( "Work Tour Mode Choice Logsum calculation for %s Departure Time Choice", tour.getTourPrimaryPurpose() );
        String decisionMakerLabel = String.format( "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourId(), person.getListOfWorkTours().size() );
        String loggingHeader = String.format("%s    %s", choiceModelDescription, decisionMakerLabel);

        for (int a = 1; a <= altStarts.length; a++)
        {

            // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
            if ( ! altAvailable[a] )
                continue;
            
            int startPeriod = altStarts[a - 1];
            int endPeriod = altEnds[a - 1];

            int index = modelStructure.getPeriodCombinationIndex(startPeriod, endPeriod);
            if (needToComputeLogsum[index])
            {

                String periodString = modelStructure.getPeriodLabel(modelStructure.getPeriodIndex(startPeriod)) + " to " + modelStructure.getPeriodLabel(modelStructure.getPeriodIndex(endPeriod));

                // set the mode choice attributes needed by @variables in the UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, tour, startPeriod, endPeriod);

                if (household.getDebugChoiceModels())
                    household.logTourObject(loggingHeader + ", " + periodString, modelLogger, person, mcDmuObject.getTourObject());

                try
                {
                    modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(mcDmuObject, tour.getTourPrimaryPurpose(), modelLogger, choiceModelDescription, decisionMakerLabel + ", " + periodString);
                } catch(Exception e)
                {
                    logger.fatal( "exception caught applying mcModel.getModeChoiceLogsum() for " + periodString + " work tour." );
                    logger.fatal( "choiceModelDescription = " + choiceModelDescription );
                    logger.fatal( "decisionMakerLabel = " + decisionMakerLabel );
                    throw new RuntimeException(e);
                }
                needToComputeLogsum[index] = false;
            }

        }

        imtodDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

        mcDmuObject.getTourObject().setTourDepartPeriod(0);
        mcDmuObject.getTourObject().setTourArrivePeriod(0);
    }

    
    private void setSchoolTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(PersonIf person, TourIf tour, boolean[] altAvailable)
    {

        HouseholdIf household = person.getHouseholdObject();

        Arrays.fill(needToComputeLogsum, true);
        Arrays.fill(modeChoiceLogsums, -999);

        Logger modelLogger = todLogger;
        String choiceModelDescription = String.format( "School Tour Mode Choice Logsum calculation for %s Departure Time Choice", tour.getTourPrimaryPurpose() );
        String decisionMakerLabel = String.format( "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourId(), person.getListOfSchoolTours().size() );
        String loggingHeader = String.format("%s    %s", choiceModelDescription, decisionMakerLabel);

        for (int a = 1; a <= altStarts.length; a++)
        {

            // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
            if ( ! altAvailable[a] )
                continue;
            
            int startPeriod = altStarts[a - 1];
            int endPeriod = altEnds[a - 1];

            int index = modelStructure.getPeriodCombinationIndex(startPeriod, endPeriod);
            if (needToComputeLogsum[index])
            {

                String periodString = modelStructure.getPeriodLabel(modelStructure.getPeriodIndex(startPeriod)) + " to " + modelStructure.getPeriodLabel(modelStructure.getPeriodIndex(endPeriod));

                // set the mode choice attributes needed by @variables in the UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, tour, startPeriod, endPeriod);

                if (household.getDebugChoiceModels())
                    household.logTourObject(loggingHeader + ", " + periodString, modelLogger,
                            person, mcDmuObject.getTourObject());

                try
                {
                    modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(mcDmuObject, tour.getTourPrimaryPurpose(), modelLogger, choiceModelDescription, decisionMakerLabel + ", " + periodString);
                } catch(Exception e)
                {
                    logger.error(e);
                    logger.fatal( "exception caught applying mcModel.getModeChoiceLogsum() for " + periodString + " school tour." );
                    logger.fatal( "choiceModelDescription = " + choiceModelDescription );
                    logger.fatal( "decisionMakerLabel = " + decisionMakerLabel );
                    throw new RuntimeException();
                }
                needToComputeLogsum[index] = false;
            }

        }

        imtodDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

    }



    /**
     *
     * @param person object for which time choice should be made
     * @return the number of school tours this person had scheduled.
     */
    private int applyDepartureTimeChoiceForSchoolTours( PersonIf person, boolean runModeChoice ) {

        Logger modelLogger = todLogger;
        
        // set the dmu object
        imtodDmuObject.setPerson(person);

        HouseholdIf household = person.getHouseholdObject();        
        
        ArrayList<TourIf> schoolTours = person.getListOfSchoolTours();

        for ( int i=0; i < schoolTours.size(); i++ ) {

            TourIf t = schoolTours.get(i);

            // dest taz was set from result of usual school location choice when tour object was created in mandatory tour frequency model.
            //TODO: if the destinationTaz value is -1, then this mandatory tour was created for a non-student (retired probably)
            //TODO: and we have to resolve this somehow - either genrate a work/school location for retired, or change activity type for person.
            //TODO: for now, we'll just skip the tour, and keep count of them.
            int destinationTaz = t.getTourDestTaz();
            if ( destinationTaz <= 0 ) {
                noUsualSchoolLocationForMandatoryActivity++;
                continue;
            }

            
            
            // write debug header
            String separator = "";
            String choiceModelDescription = "" ;
            String decisionMakerLabel = "";
            String loggingHeader = "";
            if( household.getDebugChoiceModels() ) {

                choiceModelDescription = String.format ( "Individual Mandatory School Tour Departure Time Choice Model for: Purpose=%s", t.getTourPrimaryPurpose() );
                decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person.getPersonNum(), person.getPersonType(), t.getTourId(), schoolTours.size() );
                
                schoolTourChoiceModel.choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
                    
                modelLogger.info(" ");
                String loggerString = "Individual Mandatory School Tour Departure Time Choice Model: Debug Statement for Household ID: " + household.getHhId() + ", Person Num: " + person.getPersonNum() + ", Person Type: " + person.getPersonType() + ", Tour Id: " + t.getTourId() + " of " + schoolTours.size() + " school tours.";
                for (int k=0; k < loggerString.length(); k++)
                    separator += "+";
                modelLogger.info( loggerString );
                modelLogger.info( separator );
                modelLogger.info( "" );
                modelLogger.info( "" );

            }
            

            
            imtodDmuObject.setDestinationZone( destinationTaz );

            // set the dmu object
            imtodDmuObject.setTour( t );

            int originTaz = t.getTourOrigTaz();
            imtodDmuObject.setOriginZone( originTaz );


            // get and set the area types for the tour origin and usual school location zones
            int tableRow = zoneTableRow[originTaz];
            imtodDmuObject.setOriginAreaType( areaType[tableRow-1] );

            tableRow = zoneTableRow[destinationTaz];
            imtodDmuObject.setDestinationAreaType( areaType[tableRow-1] );


            // set the choice availability and sample
            boolean[] departureTimeChoiceAvailability = person.getAvailableTimeWindows(altStarts,
                    altEnds);
            Arrays.fill(schoolTourDepartureTimeChoiceSample, 1);

            if (departureTimeChoiceAvailability.length != schoolTourDepartureTimeChoiceSample.length)
            {
                logger.error(String.format(
                    "error in school departure time choice model for hhId=%d, persId=%d, persNum=%d, school tour %d of %d.",
                    person.getHouseholdObject().getHhId(), person.getPersonId(), person.getPersonNum(), i, schoolTours.size()) );
                logger.error(String.format(
                    "length of the availability array determined by the number of alternatiuves set in the person scheduler=%d", departureTimeChoiceAvailability.length) );
                logger.error(String.format(
                    "does not equal the length of the sample array determined by the number of alternatives in the school tour UEC=%d.", schoolTourDepartureTimeChoiceSample.length) );
                throw new RuntimeException();
            }

            // if no time window is available for the tour, make the first and last
            // alternatives available
            // for that alternative, and keep track of the number of times this
            // condition occurs.
            boolean noAlternativeAvailable = true;
            for (int a = 0; a < departureTimeChoiceAvailability.length; a++)
            {
                if (departureTimeChoiceAvailability[a])
                {
                    noAlternativeAvailable = false;
                    break;
                }
            }

            if (noAlternativeAvailable)
            {
                noAvailableSchoolWindowCount++;
                departureTimeChoiceAvailability[1] = true;
                schoolTourDepartureTimeChoiceSample[1] = 1;
                departureTimeChoiceAvailability[departureTimeChoiceAvailability.length - 1] = true;
                schoolTourDepartureTimeChoiceSample[schoolTourDepartureTimeChoiceSample.length - 1] = 1;
            }

            // check for multiple tours for this person
            // set the first or second switch if multiple tours for person
            if (schoolTours.size() == 1 && person.getListOfWorkTours().size() == 0)
            {
                // not a multiple tour pattern
                imtodDmuObject.setFirstTour(0);
                imtodDmuObject.setSubsequentTour(0);
                imtodDmuObject.setTourNumber(1);
                imtodDmuObject.setEndOfPreviousScheduledTour(0);
                imtodDmuObject.setSubsequentTourIsWork(0);
                imtodDmuObject.setSubsequentTourIsSchool(0);
            } else if (schoolTours.size() > 1 && person.getListOfWorkTours().size() == 0)
            {
                // Two school tour multiple tour pattern
                if (i == 0)
                {
                    // first of 2 school tours
                    imtodDmuObject.setFirstTour(1);
                    imtodDmuObject.setSubsequentTour(0);
                    imtodDmuObject.setTourNumber(i + 1);
                    imtodDmuObject.setEndOfPreviousScheduledTour(0);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(1);
                } else
                {
                    // second of 2 school tours
                    imtodDmuObject.setFirstTour(0);
                    imtodDmuObject.setSubsequentTour(1);
                    imtodDmuObject.setTourNumber(i + 1);
                    int otherTourArrivePeriod = schoolTours.get(0).getTourArrivePeriod();
                    imtodDmuObject.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                    
                    // block alternatives for this 2nd school tour with depart <= first school tour departure AND arrive >= first school tour arrival. 
                    for (int a = 1; a <= altStarts.length; a++)
                    {
                        // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
                        if ( ! departureTimeChoiceAvailability[a] )
                            continue;
                        
                        int startPeriod = altStarts[a - 1];
                        int endPeriod = altEnds[a - 1];

                        if ( startPeriod <= schoolTours.get(0).getTourDepartPeriod() && endPeriod >= schoolTours.get(0).getTourArrivePeriod() )
                            departureTimeChoiceAvailability[a] = false;
                    }
                }
            } else if (schoolTours.size() == 1 && person.getListOfWorkTours().size() == 1)
            {
                // One school tour, one work tour multiple tour pattern
                if (person.getPersonIsStudent() == 1)
                {
                    // student, so school tour is first scheduled, work comes later.
                    imtodDmuObject.setFirstTour(1);
                    imtodDmuObject.setSubsequentTour(0);
                    imtodDmuObject.setTourNumber(1);
                    imtodDmuObject.setEndOfPreviousScheduledTour(0);
                    imtodDmuObject.setSubsequentTourIsWork(1);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                } else
                {
                    // worker, so work tour was already scheduled, this school tour is the second.
                    imtodDmuObject.setFirstTour(0);
                    imtodDmuObject.setSubsequentTour(1);
                    imtodDmuObject.setTourNumber(i + 1);
                    int otherTourArrivePeriod = person.getListOfWorkTours().get(0).getTourArrivePeriod();
                    imtodDmuObject.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                    
                    // block alternatives for this 2nd school tour with depart <= first work tour departure AND arrive >= first work tour arrival. 
                    for (int a = 1; a <= altStarts.length; a++)
                    {
                        // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
                        if ( ! departureTimeChoiceAvailability[a] )
                            continue;
                        
                        int startPeriod = altStarts[a - 1];
                        int endPeriod = altEnds[a - 1];

                        if ( startPeriod <= person.getListOfWorkTours().get(0).getTourDepartPeriod() && endPeriod >= person.getListOfWorkTours().get(0).getTourArrivePeriod() )
                            departureTimeChoiceAvailability[a] = false;
                    }
                }
            }

            // calculate and store the mode choice logsum for the usual school location for this student at the various
            // departure time and duration alternativees
            setSchoolTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives( person, t, departureTimeChoiceAvailability );


            if( household.getDebugChoiceModels() ) {
                household.logTourObject( loggingHeader, modelLogger, person, t );
            }


            schoolTourChoiceModel.computeUtilities ( imtodDmuObject, imtodDmuObject.getIndexValues(), departureTimeChoiceAvailability, schoolTourDepartureTimeChoiceSample );


            Random hhRandom = imtodDmuObject.getDmuHouseholdObject().getHhRandom();
            int randomCount = household.getHhRandomCount();
            double rn = hhRandom.nextDouble();

            // if the choice model has no available alternatives, choose between the first and last alternative.
            int chosen;
            if ( schoolTourChoiceModel.getAvailabilityCount() > 0 )
                chosen = schoolTourChoiceModel.getChoiceResult( rn );
            else
                chosen = rn < 0.5 ? 1 : altStarts.length;



            // schedule the chosen alternative
            int chosenStartPeriod = altStarts[chosen - 1];
            int chosenEndPeriod = altEnds[chosen - 1];
            try {
                person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
            }
            catch( Exception e ){
                logger.error("exception caught updating school tour TOD choice time windows.");
                throw new RuntimeException(); 
            }

            t.setTourDepartPeriod( chosenStartPeriod );
            t.setTourArrivePeriod( chosenEndPeriod );

            
            // debug output
            if( household.getDebugChoiceModels() ){

                double[] utilities     = schoolTourChoiceModel.getUtilities();
                double[] probabilities = schoolTourChoiceModel.getProbabilities();
                boolean[] availabilities = schoolTourChoiceModel.getAvailabilities();

                String personTypeString = person.getPersonType();
                int personNum = person.getPersonNum();
                modelLogger.info("Person num: " + personNum  + ", Person type: " + personTypeString + ", Tour Id: " + t.getTourId() );
                modelLogger.info("Alternative            Availability           Utility       Probability           CumProb");
                modelLogger.info("--------------------   ------------    --------------    --------------    --------------");

                double cumProb = 0.0;
                for(int k=0; k < schoolTourChoiceModel.getNumberOfAlternatives(); k++){
                    cumProb += probabilities[k];
                    String altString = String.format( "%-3d out=%-3d, in=%-3d", k+1, altStarts[k], altEnds[k] );
                    modelLogger.info( String.format( "%-20s%15s%18.6e%18.6e%18.6e", altString, availabilities[k+1], utilities[k], probabilities[k], cumProb ) );
                }

                modelLogger.info(" ");
                String altString = String.format( "%-3d out=%-3d, in=%-3d", chosen, altStarts[chosen-1], altEnds[chosen-1] );
                modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );

                modelLogger.info( separator );
                modelLogger.info("");
                modelLogger.info("");


                // write choice model alternative info to debug log file
                schoolTourChoiceModel.logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
                schoolTourChoiceModel.logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, chosen );

                // write UEC calculation results to separate model specific log file
                loggingHeader = String.format("%s  %s", choiceModelDescription, decisionMakerLabel );
                schoolTourChoiceModel.logUECResults( modelLogger, loggingHeader, 20 );
                
            }


            if (runModeChoice)
            {

                // set the mode choice attributes needed by @variables in the UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, t, chosenStartPeriod, chosenEndPeriod);
                
                // use the mcModel object already setup for computing logsums and get
                // the mode choice, where the selected
                // worklocation and subzone an departure time and duration are set
                // for this work tour.
                int chosenMode = mcModel.getModeChoice(mcDmuObject, t.getTourPrimaryPurpose());
                t.setTourModeChoice(chosenMode);
                
                //set tour taps
                mcModel.setTourTaps(t, mcDmuObject, chosenMode);

            }

            
        }

        if ( household.getDebugChoiceModels() ) {
            String decisionMakerLabel = String.format ( "Final School Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s", household.getHhId(), person.getPersonNum(), person.getPersonType() );
            household.logPersonObject( decisionMakerLabel, modelLogger, person );
        }
        
        return schoolTours.size();

    }

    private void setUnivTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(PersonIf person, TourIf tour, boolean[] altAvailable)
    {

        HouseholdIf household = person.getHouseholdObject();

        Arrays.fill(needToComputeLogsum, true);
        Arrays.fill(modeChoiceLogsums, -999);

        Logger modelLogger = todLogger;
        String choiceModelDescription = String.format( "University Tour Mode Choice Logsum calculation for %s Departure Time Choice", tour.getTourPrimaryPurpose() );
        String decisionMakerLabel = String.format( "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourId(), person.getListOfSchoolTours().size() );
        String loggingHeader = String.format("%s    %s", choiceModelDescription, decisionMakerLabel);

        for (int a = 1; a <= altStarts.length; a++)
        {

            // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
            if ( ! altAvailable[a] )
                continue;
            
            int startPeriod = altStarts[a - 1];
            int endPeriod = altEnds[a - 1];

            int index = modelStructure.getPeriodCombinationIndex(startPeriod, endPeriod);
            if (needToComputeLogsum[index])
            {

                String periodString = modelStructure.getPeriodLabel(modelStructure.getPeriodIndex(startPeriod)) + " to " + modelStructure.getPeriodLabel(modelStructure.getPeriodIndex(endPeriod));

                // set the mode choice attributes needed by @variables in the UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, tour, startPeriod, endPeriod);

                if (household.getDebugChoiceModels())
                    household.logTourObject(loggingHeader + ", " + periodString, modelLogger, person, mcDmuObject.getTourObject());

                try
                {
                    modeChoiceLogsums[index] = mcModel.getModeChoiceLogsum(mcDmuObject, tour.getTourPrimaryPurpose(), modelLogger, choiceModelDescription, decisionMakerLabel + ", " + periodString);
                } catch(Exception e)
                {
                    logger.error(e);
                    logger.fatal( "exception caught applying mcModel.getModeChoiceLogsum() for " + periodString + " university tour." );
                    logger.fatal( "choiceModelDescription = " + choiceModelDescription );
                    logger.fatal( "decisionMakerLabel = " + decisionMakerLabel );
                    throw new RuntimeException();
                }
                needToComputeLogsum[index] = false;
            }

        }

        imtodDmuObject.setModeChoiceLogsums(modeChoiceLogsums);

    }

    /**
     * 
     * @param person object for which time choice should be made
     * @return the number of school tours this person had scheduled.
     */
    private int applyDepartureTimeChoiceForUnivTours(PersonIf person, boolean runModeChoice)
    {

        Logger modelLogger = todLogger;

        // set the dmu object
        imtodDmuObject.setPerson(person);

        HouseholdIf household = person.getHouseholdObject();

        ArrayList<TourIf> workTours = person.getListOfWorkTours();
        ArrayList<TourIf> schoolTours = person.getListOfSchoolTours();

        for (int i = 0; i < schoolTours.size(); i++)
        {

            TourIf t = schoolTours.get(i);
            t.setTourDepartPeriod(-1);
            t.setTourArrivePeriod(-1);

            // dest taz was set from result of usual school location choice when tour
            // object was created in mandatory tour frequency model.
            // TODO: if the destMgra value is -1, then this mandatory tour was
            // created for a non-student (retired probably)
            // TODO: and we have to resolve this somehow - either genrate a
            // work/school location for retired, or change activity type for person.
            // TODO: for now, we'll just skip the tour, and keep count of them.
            int destMgra = t.getTourDestTaz();
            if (destMgra <= 0)
            {
                noUsualSchoolLocationForMandatoryActivity++;
                continue;
            }

            // write debug header
            String separator = "";
            String choiceModelDescription = "";
            String decisionMakerLabel = "";
            String loggingHeader = "";
            if (household.getDebugChoiceModels())
            {

                choiceModelDescription = String.format(
                    "Individual Mandatory University Tour Departure Time Choice Model for: Purpose=%s", t.getTourPrimaryPurpose());
                decisionMakerLabel = String.format(
                    "HH=%d, PersonNum=%d, PersonType=%s, tourId=%d of %d", household.getHhId(), person.getPersonNum(), person.getPersonType(), t.getTourId(), schoolTours.size() );
                univTourChoiceModel.choiceModelUtilityTraceLoggerHeading(choiceModelDescription, decisionMakerLabel);

                modelLogger.info(" ");
                String loggerString = "Individual Mandatory University Tour Departure Time Choice Model: Debug Statement for Household ID: " + household.getHhId() + ", Person Num: " + person.getPersonNum() + ", Person Type: " + person.getPersonType() + t.getTourId() + " of " + schoolTours.size() + " school tours.";
                for (int k = 0; k < loggerString.length(); k++)
                    separator += "+";
                modelLogger.info(loggerString);
                modelLogger.info(separator);
                modelLogger.info("");
                modelLogger.info("");

            }

            // set the dmu object
            imtodDmuObject.setTour(t);
            imtodDmuObject.setDestinationZone(destMgra);
            imtodDmuObject.setOriginZone(t.getTourOrigTaz());

            // set the choice availability and sample
            boolean[] departureTimeChoiceAvailability = person.getAvailableTimeWindows(altStarts, altEnds);
            Arrays.fill(schoolTourDepartureTimeChoiceSample, 1);

            if (departureTimeChoiceAvailability.length != schoolTourDepartureTimeChoiceSample.length)
            {
                logger.error(String.format(
                    "error in university departure time choice model for hhId=%d, persId=%d, persNum=%d, school tour %d of %d.",
                    person.getHouseholdObject().getHhId(), person.getPersonId(), person.getPersonNum(), i, schoolTours.size()) );
                logger.error(String.format(
                    "length of the availability array determined by the number of alternatives set in the person scheduler=%d", departureTimeChoiceAvailability.length) );
                logger.error(String.format(
                    "does not equal the length of the sample array determined by the number of alternatives in the university tour UEC=%d.", schoolTourDepartureTimeChoiceSample.length));
                throw new RuntimeException();
            }

            // if no time window is available for the tour, make the first and last
            // alternatives available
            // for that alternative, and keep track of the number of times this
            // condition occurs.
            boolean noAlternativeAvailable = true;
            for (int a = 0; a < departureTimeChoiceAvailability.length; a++)
            {
                if (departureTimeChoiceAvailability[a])
                {
                    noAlternativeAvailable = false;
                    break;
                }
            }

            if (noAlternativeAvailable)
            {
                noAvailableSchoolWindowCount++;
                departureTimeChoiceAvailability[1] = true;
                schoolTourDepartureTimeChoiceSample[1] = 1;
                departureTimeChoiceAvailability[departureTimeChoiceAvailability.length - 1] = true;
                schoolTourDepartureTimeChoiceSample[schoolTourDepartureTimeChoiceSample.length - 1] = 1;
            }

            // check for multiple tours for this person
            // set the first or second switch if multiple tours for person
            if (schoolTours.size() == 1 && person.getListOfWorkTours().size() == 0)
            {
                // not a multiple tour pattern
                imtodDmuObject.setFirstTour(0);
                imtodDmuObject.setSubsequentTour(0);
                imtodDmuObject.setTourNumber(1);
                imtodDmuObject.setEndOfPreviousScheduledTour(0);
                imtodDmuObject.setSubsequentTourIsWork(0);
                imtodDmuObject.setSubsequentTourIsSchool(0);
            } else if (schoolTours.size() > 1 && person.getListOfWorkTours().size() == 0)
            {
                // Two school tour multiple tour pattern
                if (i == 0)
                {
                    // first of 2 school tours
                    imtodDmuObject.setFirstTour(1);
                    imtodDmuObject.setSubsequentTour(0);
                    imtodDmuObject.setTourNumber(i + 1);
                    imtodDmuObject.setEndOfPreviousScheduledTour(0);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(1);
                } else
                {
                    // second of 2 school tours
                    imtodDmuObject.setFirstTour(0);
                    imtodDmuObject.setSubsequentTour(1);
                    imtodDmuObject.setTourNumber(i + 1);
                    int otherTourArrivePeriod = schoolTours.get(0).getTourArrivePeriod();
                    imtodDmuObject.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                    
                    // block alternatives for this 2nd school tour with depart <= first school tour departure AND arrive >= first school tour arrival. 
                    for (int a = 1; a <= altStarts.length; a++)
                    {
                        // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
                        if ( ! departureTimeChoiceAvailability[a] )
                            continue;
                        
                        int startPeriod = altStarts[a - 1];
                        int endPeriod = altEnds[a - 1];

                        if ( startPeriod <= schoolTours.get(0).getTourDepartPeriod() && endPeriod >= schoolTours.get(0).getTourArrivePeriod() )
                            departureTimeChoiceAvailability[a] = false;
                    }
                }
            } else if (schoolTours.size() == 1 && workTours.size() == 1)
            {
                // One school tour, one work tour multiple tour pattern
                if (person.getPersonIsStudent() == 1)
                {
                    // student, so school tour is first scheduled, work comes later.
                    imtodDmuObject.setFirstTour(1);
                    imtodDmuObject.setSubsequentTour(0);
                    imtodDmuObject.setTourNumber(1);
                    imtodDmuObject.setEndOfPreviousScheduledTour(0);
                    imtodDmuObject.setSubsequentTourIsWork(1);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                } else
                {
                    // worker, so work tour was already scheduled, this school tour is the second.
                    imtodDmuObject.setFirstTour(0);
                    imtodDmuObject.setSubsequentTour(1);
                    imtodDmuObject.setTourNumber(i + 1);
                    int otherTourArrivePeriod = person.getListOfWorkTours().get(0).getTourArrivePeriod();
                    imtodDmuObject.setEndOfPreviousScheduledTour(otherTourArrivePeriod);
                    imtodDmuObject.setSubsequentTourIsWork(0);
                    imtodDmuObject.setSubsequentTourIsSchool(0);
                    
                    // block alternatives for this 2nd school tour with depart <= first work tour departure AND arrive >= first work tour arrival. 
                    for (int a = 1; a <= altStarts.length; a++)
                    {
                        // if the depart/arrive alternative is unavailable, no need to check to see if a logsum has been calculated
                        if ( ! departureTimeChoiceAvailability[a] )
                            continue;
                        
                        int startPeriod = altStarts[a - 1];
                        int endPeriod = altEnds[a - 1];

                        if ( startPeriod <= workTours.get(0).getTourDepartPeriod() && endPeriod >= workTours.get(0).getTourArrivePeriod() )
                            departureTimeChoiceAvailability[a] = false;
                    }
                }
            }

            // calculate and store the mode choice logsum for the usual school
            // location for this student at the various
            // departure time and duration alternativees
            setUnivTourModeChoiceLogsumsForDepartureTimeAndDurationAlternatives(person, t, departureTimeChoiceAvailability);

            if (household.getDebugChoiceModels())
            {
                household.logTourObject(loggingHeader, modelLogger, person, t);
            }

            univTourChoiceModel.computeUtilities(imtodDmuObject, imtodDmuObject.getIndexValues(),
                    departureTimeChoiceAvailability, schoolTourDepartureTimeChoiceSample);

            Random hhRandom = imtodDmuObject.getDmuHouseholdObject().getHhRandom();
            int randomCount = household.getHhRandomCount();
            double rn = hhRandom.nextDouble();

            // if the choice model has no available alternatives, choose between the
            // first and last alternative.
            int chosen;
            if (univTourChoiceModel.getAvailabilityCount() > 0)
                chosen = univTourChoiceModel.getChoiceResult(rn);
            else
                chosen = rn < 0.5 ? 1 : altStarts.length;

            // schedule the chosen alternative
            int chosenStartPeriod = altStarts[chosen - 1];
            int chosenEndPeriod = altEnds[chosen - 1];
            try {
                person.scheduleWindow(chosenStartPeriod, chosenEndPeriod);
            }
            catch( Exception e ){
                logger.error("exception caught updating university tour TOD choice time windows.");
                throw new RuntimeException(); 
            }

            t.setTourDepartPeriod(chosenStartPeriod);
            t.setTourArrivePeriod(chosenEndPeriod);

            // debug output
            if (household.getDebugChoiceModels())
            {

                double[] utilities = univTourChoiceModel.getUtilities();
                double[] probabilities = univTourChoiceModel.getProbabilities();
                boolean[] availabilities = univTourChoiceModel.getAvailabilities();

                String personTypeString = person.getPersonType();
                int personNum = person.getPersonNum();
                modelLogger.info("Person num: " + personNum + ", Person type: " + personTypeString
                        + ", Tour Id: " + t.getTourId());
                modelLogger
                        .info("Alternative            Availability           Utility       Probability           CumProb");
                modelLogger
                        .info("--------------------   ------------    --------------    --------------    --------------");

                double cumProb = 0.0;
                for (int k = 0; k < schoolTourChoiceModel.getNumberOfAlternatives(); k++)
                {
                    cumProb += probabilities[k];
                    String altString = String.format("%-3d out=%-3d, in=%-3d", k + 1, altStarts[k],
                            altEnds[k]);
                    modelLogger.info(String.format("%-20s%15s%18.6e%18.6e%18.6e", altString,
                            availabilities[k + 1], utilities[k], probabilities[k], cumProb));
                }

                modelLogger.info(" ");
                String altString = String.format("%-3d out=%-3d, in=%-3d", chosen,
                        altStarts[chosen - 1], altEnds[chosen - 1]);
                modelLogger.info(String.format("Choice: %s, with rn=%.8f, randomCount=%d",
                        altString, rn, randomCount));

                modelLogger.info(separator);
                modelLogger.info("");
                modelLogger.info("");

                // write choice model alternative info to debug log file
                univTourChoiceModel.logAlternativesInfo(choiceModelDescription,
                        decisionMakerLabel);
                univTourChoiceModel.logSelectionInfo(choiceModelDescription, decisionMakerLabel,
                        rn, chosen);

                // write UEC calculation results to separate model specific log file
                loggingHeader = String.format("%s  %s", choiceModelDescription, decisionMakerLabel);
                univTourChoiceModel.logUECResults(modelLogger, loggingHeader, 200);

            }

            if (runModeChoice)
            {

                // set the mode choice attributes needed by @variables in the UEC spreadsheets
                setModeChoiceDmuAttributes(household, person, t, chosenStartPeriod, chosenEndPeriod);
                
                // use the mcModel object already setup for computing logsums and get
                // the mode choice, where the selected
                // school location and subzone and departure time and duration are
                // set for this school tour.
                int chosenMode = -1;
                chosenMode = mcModel.getModeChoice(mcDmuObject, t.getTourPrimaryPurpose());

                t.setTourModeChoice(chosenMode);
                
                //set tour taps
                mcModel.setTourTaps(t, mcDmuObject, chosenMode);

            }

        }

        if (household.getDebugChoiceModels())
        {
            String decisionMakerLabel = String
                    .format(
                            "Final University Departure Time Person Object: HH=%d, PersonNum=%d, PersonType=%s",
                            household.getHhId(), person.getPersonNum(), person.getPersonType());
            household.logPersonObject(decisionMakerLabel, modelLogger, person);
        }

        return schoolTours.size();

    }

    private void setModeChoiceDmuAttributes(HouseholdIf household, PersonIf person, TourIf t, int startPeriod, int endPeriod)
    {

        t.setTourDepartPeriod(startPeriod);
        t.setTourArrivePeriod(endPeriod);

        // update the MC dmuObjects for this person
        mcDmuObject.setHouseholdObject(household);
        mcDmuObject.setPersonObject(person);
        mcDmuObject.setTourObject(t);
        mcDmuObject.setDmuIndexValues(household.getHhId(), household.getHhTaz(), t.getTourOrigTaz(), t.getTourDestTaz(), household.getDebugChoiceModels());
        
        //create TVPB for tour mode choice model
        mcModel.setTVPBValues(mcDmuObject, t.getTourPrimaryPurpose(), false, household.getDebugChoiceModels());
        
    }
    
    
}
