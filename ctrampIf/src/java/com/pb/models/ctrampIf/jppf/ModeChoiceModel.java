package com.pb.models.ctrampIf.jppf;

import java.io.Serializable;
import java.util.*;

import com.pb.cmap.tvpb.TransitDriveAccessDMU;
import com.pb.cmap.tvpb.BestTransitPathCalculator;
import com.pb.cmap.tvpb.TransitWalkAccessDMU;
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
    
    BestTransitPathCalculator tvpb;
    
    public ModeChoiceModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory, CtrampDmuFactoryIf dmuFactory, TazDataIf tazDataManager){

        this.tourCategory =  tourCategory;
        mcDmuObject = dmuFactory.getModeChoiceDMU();
        setupModeChoiceModelApplicationArray( propertyMap, modelStructure, tourCategory );
        
        tvpb = new BestTransitPathCalculator(propertyMap);
    }

    public ModeChoiceModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory, TourModeChoiceDMU mcDmuObject, TazDataIf tazDataManager){

        this.tourCategory =  tourCategory;
        this.mcDmuObject = mcDmuObject;
        setupModeChoiceModelApplicationArray( propertyMap, modelStructure, tourCategory );
        
        tvpb = new BestTransitPathCalculator(propertyMap);
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
        setTVPBValues(mcDmuObject, tour.getTourPrimaryPurpose(), true, household.getDebugChoiceModels());
        
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
    
    public void setTVPBValues(TourModeChoiceDMU mcDmuObject, String purpose, boolean recalcPersonUtil, boolean debug) {

    	int NA_VALUE = 0;
    	
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu = new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	TransitDriveAccessDMU knrDmu  = new TransitDriveAccessDMU();
    	walkDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	driveDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	knrDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	
    	if(purpose.toLowerCase().startsWith("work")) {
    		if(mcDmuObject.getTourObject().getPersonObject()!=null) { //joint tours use default
    			walkDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_walk"));
        		driveDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_pnr"));
        		knrDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_knr"));
    		}
    	} else {
    		if(mcDmuObject.getTourObject().getPersonObject()!=null) { //joint tours use default
    			walkDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_walk"));
    			driveDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_pnr"));
    			knrDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_knr"));
    		}
    	}    	
    	
    	if(mcDmuObject.getTourObject().getPersonObject()!=null) { //joint tours use default
    		walkDmu.setWalkPropClass(mcDmuObject.getTourObject().getPersonObject().getWalkPropClass());
    	} else {
    		walkDmu.setWalkPropClass(walkDmu.getWalkPropClass());
    	}
    	driveDmu.setWalkPropClass(walkDmu.getWalkPropClass());
    	knrDmu.setWalkPropClass(walkDmu.getWalkPropClass());
    	
    	//inbound - check for existing best taps and calculate utilities if needed
    	double[][] bestWtwTapPairsIn = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTW, 
    			mcDmuObject.getDmuIndexValues().getDestZone(), 
    			mcDmuObject.getDmuIndexValues().getOriginZone(), 
    			mcDmuObject.getTodIn(), debug, logger);    	
    	double[][] bestWtdTapPairsIn = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTD, 
    			mcDmuObject.getDmuIndexValues().getDestZone(), 
    			mcDmuObject.getDmuIndexValues().getOriginZone(), 
    			mcDmuObject.getTodIn(), debug, logger);
    	double[][] bestWtkTapPairsIn = tvpb.getBestTapPairs(walkDmu, knrDmu, tvpb.WTK, 
    			mcDmuObject.getDmuIndexValues().getDestZone(), 
    			mcDmuObject.getDmuIndexValues().getOriginZone(), 
    			mcDmuObject.getTodIn(), debug, logger);
    	
    	//outbound - check for existing best taps and calculate utilities if needed   	
    	double[][] bestWtwTapPairsOut = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTW, 
    			mcDmuObject.getDmuIndexValues().getOriginZone(), 
    			mcDmuObject.getDmuIndexValues().getDestZone(), 
    			mcDmuObject.getTodOut(), debug, logger);    	
    	double[][] bestDtwTapPairsOut = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.DTW, 
    			mcDmuObject.getDmuIndexValues().getOriginZone(), 
    			mcDmuObject.getDmuIndexValues().getDestZone(), 
    			mcDmuObject.getTodOut(), debug, logger);
    	double[][] bestKtwTapPairsOut = tvpb.getBestTapPairs(walkDmu, knrDmu, tvpb.KTW, 
    			mcDmuObject.getDmuIndexValues().getOriginZone(), 
    			mcDmuObject.getDmuIndexValues().getDestZone(), 
    			mcDmuObject.getTodOut(), debug, logger);
    	
    	if(tvpb.getRecalcPersonAccEggUtil() & recalcPersonUtil) {
    		
    		if(mcDmuObject.getTourObject().getPersonObject()!=null) { //joint tours use default
    			walkDmu.setWalkTimeWeight(mcDmuObject.getTourObject().getPersonObject().getWalkTimeWeight());
        		walkDmu.setWalkSpeed(mcDmuObject.getTourObject().getPersonObject().getWalkSpeed());
        		walkDmu.setMaxWalk(mcDmuObject.getTourObject().getPersonObject().getMaxWalk());
        		walkDmu.setValueOfTime(mcDmuObject.getTourObject().getPersonObject().getValueOfTime());
        		
        		driveDmu.setAge(mcDmuObject.getTourObject().getPersonObject().getAge());
        		driveDmu.setCars(mcDmuObject.getTourObject().getPersonObject().getHouseholdObject().getAutoOwnershipModelResult());
        		driveDmu.setWalkTimeWeight(walkDmu.getWalkTimeWeight());
        		driveDmu.setWalkSpeed(walkDmu.getWalkSpeed());
        		driveDmu.setMaxWalk(walkDmu.getMaxWalk());
        		driveDmu.setValueOfTime(walkDmu.getValueOfTime());
        		
        		knrDmu.setAge(mcDmuObject.getTourObject().getPersonObject().getAge());
        		knrDmu.setCars(mcDmuObject.getTourObject().getPersonObject().getHouseholdObject().getAutoOwnershipModelResult());
        		knrDmu.setWalkTimeWeight(walkDmu.getWalkTimeWeight());
        		knrDmu.setWalkSpeed(walkDmu.getWalkSpeed());
        		knrDmu.setMaxWalk(walkDmu.getMaxWalk());
        		knrDmu.setValueOfTime(walkDmu.getValueOfTime());
    		}
    		
    		if(bestWtwTapPairsIn[0] != null) {
    			bestWtwTapPairsIn = tvpb.calcPersonSpecificUtilities(bestWtwTapPairsIn, walkDmu, driveDmu, tvpb.WTW, 
        				mcDmuObject.getDmuIndexValues().getDestZone(),
        				mcDmuObject.getDmuIndexValues().getOriginZone(), 
        				mcDmuObject.getTodIn(), debug, logger);
    		}
    		
    		if(bestWtdTapPairsIn[0] != null) {
    			bestWtdTapPairsIn = tvpb.calcPersonSpecificUtilities(bestWtdTapPairsIn, walkDmu, driveDmu, tvpb.WTD, 
        				mcDmuObject.getDmuIndexValues().getDestZone(), 
        				mcDmuObject.getDmuIndexValues().getOriginZone(), 
        				mcDmuObject.getTodIn(), debug, logger);

    		}
    		
    		if(bestWtkTapPairsIn[0] != null) {
    			bestWtkTapPairsIn = tvpb.calcPersonSpecificUtilities(bestWtkTapPairsIn, walkDmu, knrDmu, tvpb.WTK, 
        				mcDmuObject.getDmuIndexValues().getDestZone(), 
        				mcDmuObject.getDmuIndexValues().getOriginZone(), 
        				mcDmuObject.getTodIn(), debug, logger);
    		}
    		
    		if(bestWtwTapPairsOut[0] != null) {
    			bestWtwTapPairsOut = tvpb.calcPersonSpecificUtilities(bestWtwTapPairsOut, walkDmu, driveDmu, tvpb.WTW, 
        				mcDmuObject.getDmuIndexValues().getOriginZone(), 
        				mcDmuObject.getDmuIndexValues().getDestZone(), 
        				mcDmuObject.getTodOut(), debug, logger);
    		}
    		
    		if(bestDtwTapPairsOut[0] != null) {
    			bestDtwTapPairsOut = tvpb.calcPersonSpecificUtilities(bestDtwTapPairsOut, walkDmu, driveDmu, tvpb.DTW, 
        				mcDmuObject.getDmuIndexValues().getOriginZone(), 
        				mcDmuObject.getDmuIndexValues().getDestZone(), 
        				mcDmuObject.getTodOut(), debug, logger);
    		}
    		
    		if(bestKtwTapPairsOut[0] != null) {
    			bestKtwTapPairsOut = tvpb.calcPersonSpecificUtilities(bestKtwTapPairsOut, walkDmu, knrDmu, tvpb.KTW, 
        				mcDmuObject.getDmuIndexValues().getOriginZone(), 
        				mcDmuObject.getDmuIndexValues().getDestZone(), 
        				mcDmuObject.getTodOut(), debug, logger);
    		}
    	
    	}
    		
    	//update inbound dmu @variables
    	if (bestWtwTapPairsIn[0] == null) {
			mcDmuObject.setOtapWT_In( NA_VALUE );
			mcDmuObject.setDtapWT_In( NA_VALUE );
			mcDmuObject.setGenCostWT_In( NA_VALUE );
        } else {
			mcDmuObject.setOtapWT_In( (int) bestWtwTapPairsIn[0][0] );
			mcDmuObject.setDtapWT_In( (int) bestWtwTapPairsIn[0][1] );
			mcDmuObject.setGenCostWT_In( (float) bestWtwTapPairsIn[0][3] );
        }
    	
    	if (bestWtdTapPairsIn[0] == null) {
			mcDmuObject.setOtapPNR_In( NA_VALUE );
			mcDmuObject.setDtapPNR_In( NA_VALUE );
			mcDmuObject.setGenCostPNR_In( NA_VALUE );
        } else {
			mcDmuObject.setOtapPNR_In( (int) bestWtdTapPairsIn[0][0] );
			mcDmuObject.setDtapPNR_In( (int) bestWtdTapPairsIn[0][1] );
			mcDmuObject.setGenCostPNR_In( (float) bestWtdTapPairsIn[0][3] );
        }
    	
    	if (bestWtkTapPairsIn[0] == null) {
			mcDmuObject.setOtapKNR_In( NA_VALUE );
			mcDmuObject.setDtapKNR_In( NA_VALUE );
			mcDmuObject.setGenCostKNR_In( NA_VALUE );
        } else {
			mcDmuObject.setOtapKNR_In( (int) bestWtkTapPairsIn[0][0] );
			mcDmuObject.setDtapKNR_In( (int) bestWtkTapPairsIn[0][1] );
			mcDmuObject.setGenCostKNR_In( (float) bestWtkTapPairsIn[0][3] );
        }

    	//update outbound dmu @variables
    	if (bestWtwTapPairsOut[0] == null) {
			mcDmuObject.setOtapWT_Out( NA_VALUE );
			mcDmuObject.setDtapWT_Out( NA_VALUE );
			mcDmuObject.setGenCostWT_Out( NA_VALUE );
        } else {
			mcDmuObject.setOtapWT_Out( (int) bestWtwTapPairsOut[0][0] );
			mcDmuObject.setDtapWT_Out( (int) bestWtwTapPairsOut[0][1] );
			mcDmuObject.setGenCostWT_Out( (float) bestWtwTapPairsOut[0][3] );
        }
    	
    	if (bestDtwTapPairsOut[0] == null) {
			mcDmuObject.setOtapPNR_Out( NA_VALUE );
			mcDmuObject.setDtapPNR_Out( NA_VALUE );
			mcDmuObject.setGenCostPNR_Out( NA_VALUE );
        } else {
			mcDmuObject.setOtapPNR_Out( (int) bestDtwTapPairsOut[0][0] );
			mcDmuObject.setDtapPNR_Out( (int) bestDtwTapPairsOut[0][1] );
			mcDmuObject.setGenCostPNR_Out( (float) bestDtwTapPairsOut[0][3] );
        }
    	
    	if (bestKtwTapPairsOut[0] == null) {
			mcDmuObject.setOtapKNR_Out( NA_VALUE );
			mcDmuObject.setDtapKNR_Out( NA_VALUE );
			mcDmuObject.setGenCostKNR_Out( NA_VALUE );
        } else {
			mcDmuObject.setOtapKNR_Out( (int) bestKtwTapPairsOut[0][0] );
			mcDmuObject.setDtapKNR_Out( (int) bestKtwTapPairsOut[0][1] );
			mcDmuObject.setGenCostKNR_Out( (float) bestKtwTapPairsOut[0][3] );
        }

    }
    
    public void setTourTaps(TourIf tour, TourModeChoiceDMU mcDmuObject, int chosenMode) {
    	
    	//set taps for transit tour mode
        if(chosenMode==ModelStructure.WT_ALT) {
        	tour.setTourBTapIn(mcDmuObject.getOtapWT_In());
        	tour.setTourATapIn(mcDmuObject.getDtapWT_In());
        	tour.setTourBTapOut(mcDmuObject.getOtapWT_Out());
        	tour.setTourATapOut(mcDmuObject.getDtapWT_Out());
        } else if (chosenMode==ModelStructure.KNR_ALT) {
        	tour.setTourBTapIn(mcDmuObject.getOtapKNR_In());
        	tour.setTourATapIn(mcDmuObject.getDtapKNR_In());
        	tour.setTourBTapOut(mcDmuObject.getOtapKNR_Out());
        	tour.setTourATapOut(mcDmuObject.getDtapKNR_Out());
        }  else if (chosenMode==ModelStructure.PNR_ALT) {
	    	tour.setTourBTapIn(mcDmuObject.getOtapPNR_In());
	    	tour.setTourATapIn(mcDmuObject.getDtapPNR_In());
	    	tour.setTourBTapOut(mcDmuObject.getOtapPNR_Out());
	    	tour.setTourATapOut(mcDmuObject.getDtapPNR_Out());
        }  
    }
    
    
}

