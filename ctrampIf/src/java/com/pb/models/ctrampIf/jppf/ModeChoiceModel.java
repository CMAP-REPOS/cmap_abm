package com.pb.models.ctrampIf.jppf;

import java.io.Serializable;
import java.util.*;

import com.pb.cmap.tvpb.TapPair;
import com.pb.cmap.tvpb.TransitVirtualPathBuilder;
import com.pb.cmap.tvpb.Trip;
import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TourModeChoiceDMU;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TourIf;
import com.pb.common.newmodel.ChoiceModelApplication;
import org.apache.log4j.Logger;
import com.pb.common.util.ResourceUtil;

public class ModeChoiceModel implements Serializable {
    
    private transient Logger logger = Logger.getLogger(ModeChoiceModel.class);
    private transient Logger tourMCManLogger = Logger.getLogger( "tourMcMan" );
    private transient Logger tourMCNonManLogger = Logger.getLogger( "tourMcNonMan" );
    

    public static final int MC_DATA_SHEET = 0;
    public static final String PROPERTIES_UEC_TOUR_MODE_CHOICE = "UecFile.TourModeChoice";

    
    // A MyChoiceModelApplication object and modeAltsAvailable[] is needed for each purpose
    private ChoiceModelApplication mcModel[];
    private TourModeChoiceDMU mcDmuObject;

    private String tourCategory;
    private String[] tourPurposeList;

    private HashMap<String, Integer> purposeModelIndexMap;

    private String[][] modeAltNames;

    private boolean saveUtilsProbsFlag=false;
    
//    private long[][] filterCount = null;
//    private long[][] expressionCount = null;
//    private long[][] coeffCount = null;
    private long cmUecTime;
    private long cmOtherTime;
    private long mcLsTotalTime;
    
    TransitVirtualPathBuilder tvpb;
    
    public ModeChoiceModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory, CtrampDmuFactoryIf dmuFactory, TazDataIf tazDataManager){

        this.tourCategory =  tourCategory;
        mcDmuObject = dmuFactory.getModeChoiceDMU();
        setupModeChoiceModelApplicationArray( propertyMap, modelStructure, tourCategory );
        
        //create TVPB for trip mode choice UEC 
        tvpb = new TransitVirtualPathBuilder(propertyMap);
		tvpb.setupTVPB();
		tvpb.setupModels();
		tvpb.setMTTData(tazDataManager.getMttData());
    }

    public ModeChoiceModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory, TourModeChoiceDMU mcDmuObject, TazDataIf tazDataManager){

        this.tourCategory =  tourCategory;
        this.mcDmuObject = mcDmuObject;
        setupModeChoiceModelApplicationArray( propertyMap, modelStructure, tourCategory );
        
        //create TVPB for trip mode choice UEC 
        tvpb = new TransitVirtualPathBuilder(propertyMap);
		tvpb.setupTVPB();
		tvpb.setupModels();
		tvpb.setMTTData(tazDataManager.getMttData());
    }



    private void setupModeChoiceModelApplicationArray( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory ) {

        logger.info( String.format( "setting up %s tour mode choice model.", tourCategory ) );

        // locate the individual mandatory tour mode choice model UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
        String mcUecFile = propertyMap.get( PROPERTIES_UEC_TOUR_MODE_CHOICE );
        mcUecFile = projectDirectory + mcUecFile;

        // default is to not save the tour mode choice utils and probs for each tour
        String saveUtilsProbsString = propertyMap.get( CtrampApplication.PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS );
        if ( saveUtilsProbsString != null ) {
            if ( saveUtilsProbsString.equalsIgnoreCase( "true" ) )
                saveUtilsProbsFlag = true;
        }

        
        // get the number of purposes and declare the array dimension to be this size.
        tourPurposeList = modelStructure.getDcModelPurposeList( tourCategory );
        
        // create a HashMap to map purposeName to model index
        purposeModelIndexMap = new HashMap<String, Integer>();

        // keep a set of unique model sheet numbers so that we can create ChoiceModelApplication objects once for each model sheet used
        TreeSet<Integer> modelIndexSet = new TreeSet<Integer>();
        
        int maxUecIndex = 0;
        for ( String purposeName : tourPurposeList ) {
            int uecIndex = modelStructure.getTourModeChoiceUecIndexForPurpose( purposeName );
            String primaryPurposeName = modelStructure.getPrimaryPurposeString( purposeName );
            purposeModelIndexMap.put( primaryPurposeName, uecIndex );
            modelIndexSet.add( uecIndex );
            if ( uecIndex > maxUecIndex )
                maxUecIndex = uecIndex;
        }
        
        mcModel = new ChoiceModelApplication[maxUecIndex+1];

        // declare dimensions for the array of choice alternative availability by purpose
        modeAltNames = new String[maxUecIndex+1][];

        // for each unique model index, create the ChoiceModelApplication object and the availabilty array
        Iterator<Integer> it = modelIndexSet.iterator();
        while ( it.hasNext() ) {
            int m = it.next();
            mcModel[m] = new ChoiceModelApplication ( mcUecFile, m, MC_DATA_SHEET, propertyMap, (VariableTable)mcDmuObject );
            modeAltNames[m] = mcModel[m].getAlternativeNames();

//            if (filterCount == null){
//                filterCount = new long[mcModel[m].getFilterCount().length][mcModel[m].getFilterCount()[0].length];
//                expressionCount = new long[mcModel[m].getExpressionCount().length][mcModel[m].getExpressionCount()[0].length];
//                coeffCount = new long[mcModel[m].getCoeffCount().length][mcModel[m].getCoeffCount()[0].length];
//            }

            
//            try{
//                logger.info ( "mcModel[" + m + "] size:   " + ObjectUtil.checkObjectSize(mcModel[m]) );
//            }catch(Exception e){
//                throw(new RuntimeException(e));
//            }
        }
        
    }



    /**
     * Calculate and return the tour mode choice logsum for the DMU object.
     * @param mcDmuObject is the tour mode choice model DMU object
     * @return logsum of the tour mode choice alternatives
     */
    public double getModeChoiceLogsum ( TourModeChoiceDMU mcDmuObject, String primaryPurposeName, Logger modelLogger, String choiceModelDescription, String decisionMakerLabel ) {

        long check1 = System.nanoTime();
        
        int modelIndex = purposeModelIndexMap.get( primaryPurposeName.toLowerCase() );

        HouseholdIf household = mcDmuObject.getHouseholdObject();

        
        // log headers to traceLogger
        if ( household.getDebugChoiceModels() ) {
        	mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
        }
        
        
        mcModel[modelIndex].computeUtilities( mcDmuObject, mcDmuObject.getDmuIndexValues() );
        double logsum = mcModel[modelIndex].getLogsum();

        
        // write UEC calculation results to separate model specific log file
        if( household.getDebugChoiceModels() ){
        	if (modelLogger.isDebugEnabled()) {
        		String loggingHeader = String.format( "%s   %s", choiceModelDescription, decisionMakerLabel );
        		mcModel[modelIndex].logUECResults( modelLogger, loggingHeader );
        	}
            modelLogger.info( choiceModelDescription + " Logsum value: " + logsum );
            modelLogger.info( "" );
            modelLogger.info( "" );
        }

        
        mcLsTotalTime = System.nanoTime() - check1;

        return logsum;

    }

    /**
     * Get the choice model application object
     */
    public ChoiceModelApplication getChoiceModelApplication ( String primaryPurposeName) {

        int modelIndex = purposeModelIndexMap.get( primaryPurposeName.toLowerCase() );
        return mcModel[modelIndex];
    }


    
    public int getModeChoice ( TourModeChoiceDMU mcDmuObject, String primaryPurposeName ) {

        int modelIndex = purposeModelIndexMap.get( primaryPurposeName );

        HouseholdIf household = mcDmuObject.getHouseholdObject();

        Logger modelLogger = null;
        if ( tourCategory.equalsIgnoreCase( ModelStructure.MANDATORY_CATEGORY ) )
            modelLogger = tourMCManLogger;
        else
            modelLogger = tourMCNonManLogger;
            
        String choiceModelDescription = "";
        String decisionMakerLabel = "";
        String loggingHeader = "";
        String separator = "";

        
        TourIf tour = mcDmuObject.getTourObject();
        
        if ( household.getDebugChoiceModels() ) {

        	PersonIf person;
        	if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) {
        		person = tour.getPersonObject();
        	} else {
        		person = mcDmuObject.getPersonObject();
        	}
            
            choiceModelDescription = String.format ( "%s Tour Mode Choice Model for: primaryPurpose=%s, Orig=%d, OrigSubZ=%d, Dest=%d, DestSubZ=%d", tourCategory, primaryPurposeName, household.getHhTaz(), household.getHhWalkSubzone(), tour.getTourDestTaz(), tour.getTourDestWalkSubzone() );
            decisionMakerLabel = String.format ( "HH=%d, PersonNum=%d, PersonType=%s, TourId=%d", person.getHouseholdObject().getHhId(), person.getPersonNum(), person.getPersonType(), tour.getTourId() );
            loggingHeader = String.format( "%s    %s", choiceModelDescription, decisionMakerLabel );
            
            household.logHouseholdObject( "Pre " + tourCategory + " Tour Mode Choice Model HHID=" + household.getHhId(), tourMCManLogger );               
            household.logPersonObject( decisionMakerLabel, tourMCManLogger, person );            
            
            mcModel[modelIndex].choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );


            modelLogger.info(" ");
            for (int k=0; k < loggingHeader.length(); k++)
                separator += "+";
            modelLogger.info( loggingHeader );
            modelLogger.info( separator );
         
            household.logTourObject( loggingHeader, modelLogger, person, tour );
            
        }

        mcModel[modelIndex].computeUtilities( mcDmuObject, mcDmuObject.getDmuIndexValues() );

        Random hhRandom = household.getHhRandom();
        int randomCount = household.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make choice.
        int chosen;
        if ( mcModel[modelIndex].getAvailabilityCount() > 0 )
            chosen = mcModel[modelIndex].getChoiceResult( rn );
        else {
            logger.error ( String.format( "Exception caught for HHID=%d, no available %s tour mode alternatives to choose from in choiceModelApplication.", household.getHhId(), tourCategory ) );
            throw new RuntimeException();
        }
        
        
        // debug output
        if( household.getDebugChoiceModels() ){

            double[] utilities     = mcModel[modelIndex].getUtilities();          // 0s-indexing
            double[] probabilities = mcModel[modelIndex].getProbabilities();      // 0s-indexing
            boolean[] availabilities = mcModel[modelIndex].getAvailabilities();   // 1s-indexing
            String[] altNames = mcModel[modelIndex].getAlternativeNames();        // 0s-indexing

            PersonIf person;
        	if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) {
        		person = tour.getPersonObject();
        	} else {
        		person = mcDmuObject.getPersonObject();
        	}
            
            String personTypeString = person.getPersonType();
            int personNum = person.getPersonNum();
            
            modelLogger.info("Person num: " + personNum  + ", Person type: " + personTypeString + ", Tour Id: " + tour.getTourId() );
            modelLogger.info("Alternative                    Utility       Probability           CumProb");
            modelLogger.info("--------------------    --------------    --------------    --------------");

            double cumProb = 0.0;
            for(int k=0; k < mcModel[modelIndex].getNumberOfAlternatives(); k++){
                cumProb += probabilities[k];
                String altString = String.format( "%-3d  %s", k+1, altNames[k] );
                modelLogger.info( String.format( "%-20s%15s%18.6e%18.6e%18.6e", altString, availabilities[k+1], utilities[k], probabilities[k], cumProb ) );
            }

            modelLogger.info(" ");
            String altString = String.format( "%-3d  %s", chosen, altNames[chosen-1] );
            modelLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", altString, rn, randomCount ) );

            modelLogger.info( separator );
            modelLogger.info("");
            modelLogger.info("");
            
            
            // write choice model alternative info to log file
            mcModel[modelIndex].logAlternativesInfo ( choiceModelDescription, decisionMakerLabel );
            mcModel[modelIndex].logSelectionInfo ( choiceModelDescription, decisionMakerLabel, rn, chosen );
            mcModel[modelIndex].logLogitCalculations ( choiceModelDescription, decisionMakerLabel );

            
            // write UEC calculation results to separate model specific log file
            mcModel[modelIndex].logUECResults( modelLogger, loggingHeader );
        }

        
        
        if ( saveUtilsProbsFlag ) {
            
            // get the utilities and probabilities arrays for the tour mode choice model for this tour and save them to the tour object 
            double[] dUtils     = mcModel[modelIndex].getUtilities();
            double[] dProbs = mcModel[modelIndex].getProbabilities();
            
            float[] utils = new float[dUtils.length];
            float[] probs = new float[dUtils.length];
            for ( int k=0; k < dUtils.length; k++ ) {
                utils[k] = (float)dUtils[k];
                probs[k] = (float)dProbs[k];
            }
            
            tour.setTourModalUtilities(utils);
            tour.setTourModalProbabilities(probs);
            
        }

        
        return chosen;

    }

    public void applyModel( HouseholdIf household ){

        try {
	        if (tourCategory.equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) {
	            TourIf[] jointTours = household.getJointTourArray();
	            if (jointTours!=null) {
	            	for ( int i=0; i < jointTours.length; i++ ) {
	            		TourIf tour = jointTours[i];  
	            		applyJointModel(household, tour); 
	            	}
	            }
	        }
        } catch ( Exception e ) {
            logger.error( String.format( "error in joint tour mode choice model model for hhId=%d.", household.getHhId()));
            throw new RuntimeException(e);
        }
        // get the array of persons for this household
        PersonIf[] personArray = household.getPersons();

        // loop through the persons (1-based array)
        for(int j=1;j<personArray.length;++j){

            TourIf tour = null;
            PersonIf person = personArray[j];
            
            try {
	            if (tourCategory.equalsIgnoreCase(ModelStructure.MANDATORY_CATEGORY)) {
	            	ArrayList<TourIf> workTours = person.getListOfWorkTours();
	            	for ( int i=0; i < workTours.size(); i++ ) {
	            		tour = workTours.get(i); 
	            		applyIndividualModel(household, person, tour); 
	            	}
	            	ArrayList<TourIf> schoolTours = person.getListOfSchoolTours();
	            	for (int i=0; i < schoolTours.size(); i++) {
	            		tour = schoolTours.get(i); 
	            		applyIndividualModel(household, person, tour); 
	            	}                    
	            }
	            else if (tourCategory.equalsIgnoreCase(ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY)) {
	            	ArrayList<TourIf> tours = person.getListOfIndividualNonMandatoryTours();
	            	for ( int i=0; i < tours.size(); i++ ) {
	            		tour = tours.get(i); 
	            		applyIndividualModel(household, person, tour); 
	            	}            	
	            }
	            else if (tourCategory.equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY)) {
	            	ArrayList<TourIf> tours = person.getListOfAtWorkSubtours(); 
	            	for ( int i=0; i < tours.size(); i++ ) {
	            		tour = tours.get(i); 
	            		applyIndividualModel(household, person, tour); 
	            	}            	
	            }
            }
            catch ( Exception e ) {
                logger.error( String.format( "error in individual tour mode choice model model for hhId=%d, persId=%d, persNum=%d, personType=%s.", household.getHhId(), person.getPersonId(), person.getPersonNum(), person.getPersonType() ));
                logger.error( String.format( "tour id=%d, tour orig=%d, tour dest=%d, tour purpose=%s, tour purpose index=%d.", tour.getTourId(), tour.getTourOrigTaz(), tour.getTourDestTaz(), tour.getTourPrimaryPurpose(), tour.getTourPrimaryPurposeIndex() ));
                throw new RuntimeException(e);
            }
        }
    	
    }
    

    private void applyJointModel(HouseholdIf household, TourIf tour) {
    	PersonIf person = tour.getPersonObject(); 
    	applyIndividualModel(household, person, tour); 
    }
    
    private void applyIndividualModel(HouseholdIf household, PersonIf person, TourIf tour) {
        
        // update the MC dmuObjects for this person
        mcDmuObject.setHouseholdObject(household);
        mcDmuObject.setPersonObject( person );
        mcDmuObject.setTourObject( tour );
        mcDmuObject.setDmuIndexValues( household.getHhId(), household.getHhTaz(), tour.getTourOrigTaz(), tour.getTourDestTaz(), household.getDebugChoiceModels() );
        mcDmuObject.setTourDepartPeriod( tour.getTourDepartPeriod() );
        mcDmuObject.setTourArrivePeriod( tour.getTourArrivePeriod() );
        
        if (tourCategory.equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY)) {
        	ArrayList<TourIf> workTourList = person.getListOfWorkTours();
            int workTourIndex = tour.getWorkTourIndexFromSubtourId( tour.getTourId() );
            TourIf workTour = workTourList.get( workTourIndex );            
        	mcDmuObject.setWorkTourObject(workTour); 
        }

        //create TVPB for tour mode choice model
        setTVPBValues(mcDmuObject, tour.getTourPrimaryPurpose());
        
        // use the mcModel object already setup for computing logsums and get the mode choice, where the selected
        // worklocation and subzone an departure time and duration are set for this work tour.
        int chosenMode = getModeChoice ( mcDmuObject, tour.getTourPrimaryPurpose() );
        tour.setTourModeChoice( chosenMode );
        
        //set tour taps
        setTourTaps(tour, mcDmuObject, chosenMode);

        if ( household.getDebugChoiceModels() ) {
        	Logger modelLogger = null; 
            if ( tourCategory.equalsIgnoreCase( ModelStructure.MANDATORY_CATEGORY ) )
                modelLogger = tourMCManLogger;
            else
                modelLogger = tourMCNonManLogger;
            
            modelLogger.info("Chosen mode = " + chosenMode); 
            String decisionMakerLabel = String.format ( "Final Mode Choice Person Object: HH=%d, PersonNum=%d, PersonType=%s", household.getHhId(), person.getPersonNum(), person.getPersonType() );
            household.logPersonObject( decisionMakerLabel, modelLogger, person );
        }
        
    }

    public String[] getModeAltNames( int purposeIndex ) {
        int modelIndex = purposeModelIndexMap.get( tourPurposeList[purposeIndex] );
        return modeAltNames[modelIndex];
    }
    
//    public long[][] getFilterCount() {
//        return filterCount;
//    }
//    public long[][] getExpressionCount() {
//        return expressionCount;
//    }
//    public long[][] getCoeffCount() {
//        return coeffCount;
//    }
    public long getCmUecTime() {
        return cmUecTime;
    }
    public long getCmOtherTime() {
        return cmOtherTime;
    }
    public long getLsTotalTime() {
    	return mcLsTotalTime;
    }
    
    public void setTVPBValues(TourModeChoiceDMU mcDmuObject, String purpose) {
    	
    	int NA_VALUE = 0;
    	
    	//inbound
    	Trip trip = new Trip();
    	trip.setTripid(mcDmuObject.getTourObject().getHhId());
    	trip.setOtaz(tvpb.getMTTData().getTazForMaz(mcDmuObject.getDmuIndexValues().getDestZone()));
		trip.setDtaz(tvpb.getMTTData().getTazForMaz(mcDmuObject.getDmuIndexValues().getOriginZone()));
		trip.setOmaz(mcDmuObject.getDmuIndexValues().getDestZone());
		trip.setDmaz(mcDmuObject.getDmuIndexValues().getOriginZone());
		trip.setAge(mcDmuObject.getTourObject().getPersonObject().getAge());
		trip.setCars(mcDmuObject.getAutos());
		trip.setTod(mcDmuObject.getTodIn());
		trip.setHhincome(mcDmuObject.getHouseholdObject().getIncomeInDollars());
		trip.setInbound(1);
		trip.setDebugRecord(mcDmuObject.getHouseholdObject().getDebugChoiceModels());
		
		trip.setWalkTimeWeight(mcDmuObject.getTourObject().getPersonObject().getWalkTimeWeight());
		trip.setWalkSpeed(mcDmuObject.getTourObject().getPersonObject().getWalkSpeed());
		trip.setMaxWalk(mcDmuObject.getTourObject().getPersonObject().getMaxWalk());
		trip.setValueOfTime(mcDmuObject.getTourObject().getPersonObject().getValueOfTime());
		
		trip.setPurpose(purpose.toLowerCase().startsWith("work") ? 1 : 0);
		trip.setUserClassByType("user_class_work_walk", mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_walk"));
		trip.setUserClassByType("user_class_work_pnr", mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_pnr"));
	    trip.setUserClassByType("user_class_work_knr", mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_knr"));
		trip.setUserClassByType("user_class_non_work_walk", mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_walk"));
		trip.setUserClassByType("user_class_non_work_pnr", mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_pnr"));
		trip.setUserClassByType("user_class_non_work_knr", mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_knr"));
		
		tvpb.calculatePathsForATrip(trip, true, true, true);
		
		TapPair bestWalkTapPair;
		TapPair bestKnrTapPair;
		TapPair bestPnrTapPair;
		if(trip.getWalkTapPairs().size()>0) {
			bestWalkTapPair = trip.getWalkTapPairs().get(0); //get best
			mcDmuObject.setGenCostWT_In( (float) bestWalkTapPair.getTotalUtils()[0] );
			mcDmuObject.setOtapWT_In( bestWalkTapPair.otap );
			mcDmuObject.setDtapWT_In( bestWalkTapPair.dtap );
		} else {
			mcDmuObject.setGenCostWT_In( NA_VALUE );
			mcDmuObject.setOtapWT_In( NA_VALUE );
			mcDmuObject.setDtapWT_In( NA_VALUE );
		}
		
		if(trip.getKnrTapPairs().size()>0) {
			bestKnrTapPair = trip.getKnrTapPairs().get(0); //get best
			mcDmuObject.setGenCostDL_In( (float) bestKnrTapPair.getTotalUtils()[0] );
			mcDmuObject.setOtapDL_In( bestKnrTapPair.otap );
			mcDmuObject.setDtapDL_In( bestKnrTapPair.dtap );
		} else {
			mcDmuObject.setGenCostDL_In( NA_VALUE );
			mcDmuObject.setOtapDL_In( NA_VALUE );
			mcDmuObject.setDtapDL_In( NA_VALUE );
		}
		
		if(trip.getPnrTapPairs().size()>0) {
			bestPnrTapPair = trip.getPnrTapPairs().get(0); //get best
			mcDmuObject.setGenCostDP_In( (float) bestPnrTapPair.getTotalUtils()[0] );
			mcDmuObject.setOtapDP_In( bestPnrTapPair.otap );
			mcDmuObject.setDtapDP_In( bestPnrTapPair.dtap );
		} else {
			mcDmuObject.setGenCostDP_In( NA_VALUE );
			mcDmuObject.setOtapDP_In( NA_VALUE );
			mcDmuObject.setDtapDP_In( NA_VALUE );
		}
		
		//outbound
		trip.setOtaz(tvpb.getMTTData().getTazForMaz(mcDmuObject.getDmuIndexValues().getOriginZone()));
		trip.setDtaz(tvpb.getMTTData().getTazForMaz(mcDmuObject.getDmuIndexValues().getDestZone()));
		trip.setOmaz(mcDmuObject.getDmuIndexValues().getOriginZone());
		trip.setDmaz(mcDmuObject.getDmuIndexValues().getDestZone());
    	trip.setTod(mcDmuObject.getTodOut());
		trip.setInbound(0);
		tvpb.calculatePathsForATrip(trip, true, true, true);
		
		if(trip.getWalkTapPairs().size()>0) {
			bestWalkTapPair = trip.getWalkTapPairs().get(0); //get best
			mcDmuObject.setGenCostWT_Out( (float) bestWalkTapPair.getTotalUtils()[0] );
			mcDmuObject.setOtapWT_Out( bestWalkTapPair.otap );
			mcDmuObject.setDtapWT_Out( bestWalkTapPair.dtap );
		} else {
			mcDmuObject.setGenCostWT_Out( NA_VALUE );
			mcDmuObject.setOtapWT_Out( NA_VALUE );
			mcDmuObject.setDtapWT_Out( NA_VALUE );			
		}
		
		if(trip.getKnrTapPairs().size()>0) {
			bestKnrTapPair = trip.getKnrTapPairs().get(0); //get best
			mcDmuObject.setGenCostDL_Out( (float) bestKnrTapPair.getTotalUtils()[0] );
			mcDmuObject.setOtapDL_Out( bestKnrTapPair.otap );
			mcDmuObject.setDtapDL_Out( bestKnrTapPair.dtap );
		} else {
			mcDmuObject.setGenCostDL_Out( NA_VALUE );
			mcDmuObject.setOtapDL_Out( NA_VALUE );
			mcDmuObject.setDtapDL_Out( NA_VALUE );
		}
		
		if(trip.getPnrTapPairs().size()>0) {
			bestPnrTapPair = trip.getPnrTapPairs().get(0); //get best
			mcDmuObject.setGenCostDP_Out( (float) bestPnrTapPair.getTotalUtils()[0] );
			mcDmuObject.setOtapDP_Out( bestPnrTapPair.otap );
			mcDmuObject.setDtapDP_Out( bestPnrTapPair.dtap );
		} else {
			mcDmuObject.setGenCostDP_Out( NA_VALUE );
			mcDmuObject.setOtapDP_Out( NA_VALUE );
			mcDmuObject.setDtapDP_Out( NA_VALUE );
		}

    }
    
    
    public void setTourTaps(TourIf tour, TourModeChoiceDMU mcDmuObject, int chosenMode) {
    	
    	final int WP_ALT = 10;
    	final int DL_ALT = 11;
    	final int DP_ALT = 12;
    	
    	//set taps for transit tour mode
        if(chosenMode==WP_ALT) {
        	tour.setTourBTapIn(mcDmuObject.getOtapWT_In());
        	tour.setTourATapIn(mcDmuObject.getDtapWT_In());
        	tour.setTourBTapOut(mcDmuObject.getOtapWT_Out());
        	tour.setTourATapOut(mcDmuObject.getDtapWT_Out());
        } else if (chosenMode==DL_ALT) {
        	tour.setTourBTapIn(mcDmuObject.getOtapDL_In());
        	tour.setTourATapIn(mcDmuObject.getDtapDL_In());
        	tour.setTourBTapOut(mcDmuObject.getOtapDL_Out());
        	tour.setTourATapOut(mcDmuObject.getDtapDL_Out());
        }  else if (chosenMode==DP_ALT) {
	    	tour.setTourBTapIn(mcDmuObject.getOtapDP_In());
	    	tour.setTourATapIn(mcDmuObject.getDtapDP_In());
	    	tour.setTourBTapOut(mcDmuObject.getOtapDP_Out());
	    	tour.setTourATapOut(mcDmuObject.getDtapDP_Out());
    }  
    }
    
    
}

