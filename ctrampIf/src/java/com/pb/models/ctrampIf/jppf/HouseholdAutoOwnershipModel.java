package com.pb.models.ctrampIf.jppf;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.AutoOwnershipChoiceDMU;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TimeDMU;
import com.pb.models.ctrampIf.TourModeChoiceDMU;

import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.cmap.tvpb.TransitDriveAccessDMU;
import com.pb.cmap.tvpb.TransitWalkAccessDMU;

import com.pb.cmap.tvpb.BestTransitPathCalculator;

import org.apache.log4j.Logger;


public class HouseholdAutoOwnershipModel implements Serializable {
    
    private transient Logger logger = Logger.getLogger(HouseholdAutoOwnershipModel.class);
    private transient Logger aoLogger = Logger.getLogger("ao");
    

    private static final String AO_CONTROL_FILE_TARGET = "UecFile.AutoOwnership";
    private static final int AO_DATA_SHEET = 0;
    private static final int AO_MODEL_SHEET = 1;
    private static final int AUTO_MODEL_SHEET = 2;
    private static final int TRANSIT_MODEL_SHEET = 3;
    private static final int WALK_MODEL_SHEET = 4;


    ChoiceModelApplication aoModel;
    AutoOwnershipChoiceDMU aoDmuObject;
    private ModelStructure modelStructure;

    private AutoDependencyModel adModel;
    private TourModeChoiceDMU mcDmuObject;
    HashMap<String, String> propertyMap;
    
    UtilityExpressionCalculator[] timeUec;

    BestTransitPathCalculator tvpb;

    public HouseholdAutoOwnershipModel( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory, TazDataIf tazDataManager, ModelStructure modelStructure) {

    	// setup the auto ownership choice model objects
        setupAutoOwnershipChoiceModelApplication( propertyMap, dmuFactory, tazDataManager, modelStructure);
    	
    }

    private void setupAutoOwnershipChoiceModelApplication( HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory, TazDataIf tazDataManager, ModelStructure modelStructure ) {
        
        logger.info( "setting up AO choice model." );

        this.propertyMap = propertyMap;
        this.modelStructure = modelStructure;
        
        // locate the auto ownership UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
        String autoOwnershipUecFile = propertyMap.get( AO_CONTROL_FILE_TARGET);
        autoOwnershipUecFile = projectDirectory + autoOwnershipUecFile;

        // create the auto ownership choice model DMU object.
        aoDmuObject = dmuFactory.getAutoOwnershipDMU();
        aoDmuObject.setupTazDataManager(tazDataManager);
        
        
        // create the auto ownership choice model object
        aoModel = new ChoiceModelApplication( autoOwnershipUecFile, AO_MODEL_SHEET, AO_DATA_SHEET, propertyMap, (VariableTable)aoDmuObject );
    	
    	//create auto dependency model
        mcDmuObject = dmuFactory.getModeChoiceDMU();
        adModel = new AutoDependencyModel( propertyMap, modelStructure, ModelStructure.MANDATORY_CATEGORY, dmuFactory );
        
        TimeDMU timeDmu = new TimeDMU();
        
        // create a set of UEC objects to use to get OD times for the selected long term location
        timeUec = new UtilityExpressionCalculator[3];
        timeUec[0] = new UtilityExpressionCalculator(new File(autoOwnershipUecFile), AUTO_MODEL_SHEET, AO_DATA_SHEET, propertyMap, (VariableTable)timeDmu );
        timeUec[1] = new UtilityExpressionCalculator(new File(autoOwnershipUecFile), TRANSIT_MODEL_SHEET, AO_DATA_SHEET, propertyMap, (VariableTable)timeDmu );
        timeUec[2] = new UtilityExpressionCalculator(new File(autoOwnershipUecFile), WALK_MODEL_SHEET, AO_DATA_SHEET, propertyMap, (VariableTable)timeDmu );

        
        //create TVPB for auto dependency model 
        tvpb = new BestTransitPathCalculator(propertyMap);
    }

    public void applyModel( HouseholdIf hhObject ){

        if ( hhObject.getDebugChoiceModels() )
            hhObject.logHouseholdObject( "Pre AO Household " + hhObject.getHhId() + " Object", aoLogger );
        
        int chosen = getAutoOwnershipChoice( hhObject );
        hhObject.setAutoOwnershipModelResult((short)chosen);

    }
    
    
    private int getAutoOwnershipChoice ( HouseholdIf hhObj ) {

        // update the AO dmuObject for this hh
        aoDmuObject.setHouseholdObject( hhObj );
        aoDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), 0 );

        // set the travel times from home to chosen work and school locations
        double workTimeSavings = getWorkTourAutoTimeSavings( hhObj );        
        double schoolDriveTimeSavings = getSchoolDriveTourAutoTimeSavings( hhObj );        
        double schoolNonDriveTimeSavings = getSchoolNonDriveTourAutoTimeSavings( hhObj );        
        
        aoDmuObject.setWorkTourAutoTimeSavings( workTimeSavings );
        aoDmuObject.setSchoolDriveTourAutoTimeSavings( schoolDriveTimeSavings );
        aoDmuObject.setSchoolNonDriveTourAutoTimeSavings( schoolNonDriveTimeSavings );

        //calculate work and school auto dependency terms
        aoDmuObject.setWorkAutoDependency(getWorkAutoDependency(hhObj));
        aoDmuObject.setSchoolAutoDependency(getSchoolAutoDependency(hhObj));

        // compute utilities and choose auto ownership alternative.
        aoModel.computeUtilities ( aoDmuObject, aoDmuObject.getDmuIndexValues() );
        
        Random hhRandom = hhObj.getHhRandom();
        int randomCount = hhObj.getHhRandomCount();
        double rn = hhRandom.nextDouble();

        // if the choice model has at least one available alternative, make choice.
        int chosenAlt;
        if ( aoModel.getAvailabilityCount() > 0 ) {
            chosenAlt = aoModel.getChoiceResult( rn );
        }
        else {
            logger.error ( String.format( "Exception caught for HHID=%d, no available auto ownership alternatives to choose from in choiceModelApplication.", hhObj.getHhId() ) );
            throw new RuntimeException();
        }


        // write choice model alternative info to log file
        if ( hhObj.getDebugChoiceModels() ) {

            double[] utilities     = aoModel.getUtilities();
            double[] probabilities = aoModel.getProbabilities();

            aoLogger.info("Alternative                    Utility       Probability           CumProb");
            aoLogger.info("--------------------   ---------------      ------------      ------------");

            double cumProb = 0.0;
            for( int k=0; k < aoModel.getNumberOfAlternatives(); k++ ) {
                cumProb += probabilities[k];
                aoLogger.info(String.format("%-20s%18.6e%18.6e%18.6e", k + " autos", utilities[k], probabilities[k], cumProb ) );
            }

            aoLogger.info(" ");
            aoLogger.info( String.format("Choice: %s, with rn=%.8f, randomCount=%d", chosenAlt, rn, randomCount ) );

            aoLogger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            aoLogger.info("");
            aoLogger.info("");

        
            // write choice model alternative info to debug log file
            aoModel.logAlternativesInfo ( "Household Auto Ownership Choice", String.format("HH_%d", hhObj.getHhId() ) );
            aoModel.logSelectionInfo ( "Household Auto Ownership Choice", String.format("HH_%d", hhObj.getHhId() ), rn, chosenAlt );

            // write UEC calculation results to separate model specific log file
            aoModel.logUECResults( aoLogger, String.format("Household Auto Ownership Choice, HH_%d", hhObj.getHhId() ) );
     }

        hhObj.setAoRandomCount( hhObj.getHhRandomCount() );
        

        return chosenAlt;

    }

    
    private double getWorkTourAutoTimeSavings( HouseholdIf hhObj ) {

        double totalAutoSavingsRatio = 0.0;
        
        TimeDMU timeDmuObject = new TimeDMU ();
        int[] availability = new int[2];
        availability[1] = 1;

        // determine the travel time savings from home to chosen work and school locations
        // for workers and students by student category
        PersonIf[] personArray = hhObj.getPersons();
        for ( int i=1; i < personArray.length; i++ ) {

            PersonIf person = personArray[i];
            
            // if person is not a worker, skip to next person.
            if ( person.getPersonIsWorker() == 0 )
                continue;

            
            // use a time UEC to get the od times for this person's location choice
            boolean debugFlag = hhObj.getDebugChoiceModels();
            timeDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), person.getUsualWorkLocation(), debugFlag );

            double auto[]    = timeUec[0].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            double transit[] = timeUec[1].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            double walk[]    = timeUec[2].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            
            // set the minimum of walk and transit time to walk time if no transit access,
            // otherwise to the minimum of walk and transit time.
            double minWalkTransit = walk[0];
            if ( transit[0] > 0 && transit[0] < walk[0] ) {
                minWalkTransit = transit[0];
            }
            
            // set auto savings to be minimum of walk and transit time minus auto time
            double autoSavings = minWalkTransit - auto[0];
            double autoSavingsRatio = ( autoSavings < 120 ? autoSavings/120.0 : 1.0 );

            totalAutoSavingsRatio += autoSavingsRatio;

        }
    
        return totalAutoSavingsRatio;

    }
    

    private double getSchoolDriveTourAutoTimeSavings( HouseholdIf hhObj ) {

        double totalAutoSavingsRatio = 0.0;
        
        TimeDMU timeDmuObject = new TimeDMU ();
        int[] availability = new int[2];
        availability[1] = 1;

        // determine the travel time savings from home to chosen work and school locations
        // for workers and students by student category
        PersonIf[] personArray = hhObj.getPersons();
        for ( int i=1; i < personArray.length; i++ ) {

            PersonIf person = personArray[i];
            

            // if person is not a worker, skip to next person.
            if ( person.getPersonIsStudentDriving() == 0 )
                continue;

            
            // use a time UEC to get the od times for this person's location choice
            boolean debugFlag = hhObj.getDebugChoiceModels();
            timeDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), person.getUsualSchoolLocation(), debugFlag );

            double auto[]    = timeUec[0].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            double transit[] = timeUec[1].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            double walk[]    = timeUec[2].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            
            // set the minimum of walk and transit time to walk time if no transit access,
            // otherwise to the minimum of walk and transit time.
            double minWalkTransit = walk[0];
            if ( transit[0] > 0 && transit[0] < walk[0] ) {
                minWalkTransit = transit[0];
            }
            
            // set auto savings to be minimum of walk and transit time minus auto time
            double autoSavings = minWalkTransit - auto[0];
            double autoSavingsRatio = ( autoSavings < 120 ? autoSavings/120.0 : 1.0 );

            totalAutoSavingsRatio += autoSavingsRatio;

        }
    
        return totalAutoSavingsRatio;

    }
    

    private double getSchoolNonDriveTourAutoTimeSavings( HouseholdIf hhObj ) {

        double totalAutoSavingsRatio = 0.0;
        
        TimeDMU timeDmuObject = new TimeDMU ();
        int[] availability = new int[2];
        availability[1] = 1;

        // determine the travel time savings from home to chosen work and school locations
        // for workers and students by student category
        PersonIf[] personArray = hhObj.getPersons();
        for ( int i=1; i < personArray.length; i++ ) {

            PersonIf person = personArray[i];
            

            // if person is not a worker, skip to next person.
            if ( person.getPersonIsStudentNonDriving() == 0 )
                continue;

            
            // use a time UEC to get the od times for this person's location choice
            boolean debugFlag = hhObj.getDebugChoiceModels();
            timeDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), person.getUsualSchoolLocation(), debugFlag );

            double auto[]    = timeUec[0].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            double transit[] = timeUec[1].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            double walk[]    = timeUec[2].solve( timeDmuObject.getDmuIndexValues(), timeDmuObject, availability );
            
            // set the minimum of walk and transit time to walk time if no transit access,
            // otherwise to the minimum of walk and transit time.
            double minWalkTransit = walk[0];
            if ( transit[0] > 0 && transit[0] < walk[0] ) {
                minWalkTransit = transit[0];
            }
            
            // set auto savings to be minimum of walk and transit time minus auto time
            double autoSavings = minWalkTransit - auto[0];
            double autoSavingsRatio = ( autoSavings < 120 ? autoSavings/120.0 : 1.0 );

            totalAutoSavingsRatio += autoSavingsRatio;

        }
    
        return totalAutoSavingsRatio;

    }
    
    private double getWorkAutoDependency(HouseholdIf hhObj) {
    	
    	PersonIf[] persons = hhObj.getPersons();

    	double hh_auto_depend = 0;
    	
    	//for all workers
        for ( int i=1; i < persons.length; i++ ) {

            PersonIf p = persons[i];  
            
            if(p.getPersonIsWorker()==1) {
	        
		        // update the MC dmuObject for this person
		        mcDmuObject.setHouseholdObject( hhObj );
		        mcDmuObject.setPersonObject( p );
		        mcDmuObject.setTourObject( p.makeDefaultTour( modelStructure.getPrimaryPurposeIndex( ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase() ) ) ); 
		        mcDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), p.getPersonWorkLocationZone(), hhObj.getDebugChoiceModels() );
		        mcDmuObject.setTourOrigTaz( hhObj.getHhTaz() );
	            mcDmuObject.setTourOrigWalkSubzone( hhObj.getHhWalkSubzone() );
	            mcDmuObject.setTourDestTaz( p.getPersonWorkLocationZone() );
	            mcDmuObject.setTourDestWalkSubzone( p.getPersonWorkLocationSubZone() );
	            mcDmuObject.setTourDepartPeriod( modelStructure.getWorkLocationDefaultDepartPeriod() );
	            mcDmuObject.setTourArrivePeriod( modelStructure.getWorkLocationDefaultArrivePeriod() );
	            mcDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), p.getPersonWorkLocationZone(), hhObj.getDebugChoiceModels() );
	
	            //create TVPB for auto dependency model
	            setTVPBValues(mcDmuObject, ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase(), false, hhObj.getDebugChoiceModels());
	            
		        // get the choice model application
	            String choiceModelDescription = String.format ( "Auto Dependency Model for: Auto, primaryPurpose=%s, Orig=%d, OrigSubZ=%d, Dest=%d, DestSubZ=%d", 
	            			ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase(), hhObj.getHhTaz(), hhObj.getHhWalkSubzone(), p.getPersonWorkLocationZone(), p.getPersonWorkLocationSubZone() );
	            String decisionMakerLabel = String.format ( "HH=%d", p.getHouseholdObject().getHhId());
	            double auto = adModel.getLogsum(mcDmuObject, ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase(), logger, choiceModelDescription, decisionMakerLabel, true);
		        
	            choiceModelDescription = String.format ( "Auto Dependency Model for: Non-auto, primaryPurpose=%s, Orig=%d, OrigSubZ=%d, Dest=%d, DestSubZ=%d", 
            			ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase(), hhObj.getHhTaz(), hhObj.getHhWalkSubzone(), p.getPersonWorkLocationZone(), p.getPersonWorkLocationSubZone() );
	            double non_auto = adModel.getLogsum(mcDmuObject, ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase(), logger, choiceModelDescription, decisionMakerLabel, false);
		        
		        //adjust term
		        double person_auto_depend = Math.min(Math.max(auto - non_auto, 0) / 6, 1);
		        hh_auto_depend = hh_auto_depend + person_auto_depend;
            }
        }
    	
        return hh_auto_depend;

    }
    
    private double getSchoolAutoDependency(HouseholdIf hhObj) {
    	
    	PersonIf[] persons = hhObj.getPersons();
    	
    	double hh_auto_depend = 0;

        for ( int i=1; i < persons.length; i++ ) {

            PersonIf p = persons[i];
            
        	String tourPrimaryPurpose = "";
            int tourDepart = -1;
            int tourArrive = -1;
            
            //for all ptype 3 (univ) and 6 (student 16+)
            if(p.getPersonIsUniversityStudent()==1) {
            	tourPrimaryPurpose = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME.toLowerCase();
                tourDepart = modelStructure.getUniversityLocationDefaultDepartPeriod();
                tourArrive = modelStructure.getUniversityLocationDefaultArrivePeriod();
            } else if ( p.getPersonIsStudentDriving()==1 ) {
                tourPrimaryPurpose = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME.toLowerCase();
                tourDepart = modelStructure.getSchoolLocationDefaultDepartPeriod();
                tourArrive = modelStructure.getSchoolLocationDefaultArrivePeriod();
            } else {
            	continue;
            }
	        
	        // update the MC dmuObject for this person
	        mcDmuObject.setHouseholdObject( hhObj );
	        mcDmuObject.setPersonObject( p );
	        mcDmuObject.setTourObject( p.makeDefaultTour( modelStructure.getPrimaryPurposeIndex( tourPrimaryPurpose ) ) ); 
	        mcDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), p.getPersonSchoolLocationZone(), hhObj.getDebugChoiceModels() );
	        mcDmuObject.setTourOrigTaz( hhObj.getHhTaz() );
            mcDmuObject.setTourOrigWalkSubzone( hhObj.getHhWalkSubzone() );
            mcDmuObject.setTourDestTaz( p.getPersonSchoolLocationZone() );
            mcDmuObject.setTourDestWalkSubzone( p.getPersonSchoolLocationSubZone() );
            mcDmuObject.setTourDepartPeriod( tourDepart );
            mcDmuObject.setTourArrivePeriod( tourArrive );
            mcDmuObject.setDmuIndexValues( hhObj.getHhId(), hhObj.getHhTaz(), hhObj.getHhTaz(), p.getPersonSchoolLocationZone(), hhObj.getDebugChoiceModels() );

            //create TVPB for auto dependency model
            setTVPBValues(mcDmuObject, tourPrimaryPurpose, false, hhObj.getDebugChoiceModels());
            
	        // get the choice model application
            String choiceModelDescription = String.format ( "Auto Dependency Model for: Auto, primaryPurpose=%s, Orig=%d, OrigSubZ=%d, Dest=%d, DestSubZ=%d", 
            		tourPrimaryPurpose, hhObj.getHhTaz(), hhObj.getHhWalkSubzone(), p.getPersonSchoolLocationZone(), p.getPersonSchoolLocationSubZone() );
            String decisionMakerLabel = String.format ( "HH=%d", p.getHouseholdObject().getHhId());
            double auto = adModel.getLogsum(mcDmuObject, tourPrimaryPurpose, logger, choiceModelDescription, decisionMakerLabel, true);
            
            choiceModelDescription = String.format ( "Auto Dependency Model for: Non-auto, primaryPurpose=%s, Orig=%d, OrigSubZ=%d, Dest=%d, DestSubZ=%d", 
            		tourPrimaryPurpose, hhObj.getHhTaz(), hhObj.getHhWalkSubzone(), p.getPersonSchoolLocationZone(), p.getPersonSchoolLocationSubZone() );
	        double non_auto = adModel.getLogsum(mcDmuObject, tourPrimaryPurpose, logger, choiceModelDescription, decisionMakerLabel, false);
	        
	        //adjust term
	        double person_auto_depend = Math.min(Math.max(auto - non_auto, 0) / 6, 1);
	        hh_auto_depend = hh_auto_depend + person_auto_depend;
        }
    	
        return hh_auto_depend;
    }


    public void setTVPBValues(TourModeChoiceDMU mcDmuObject, String purpose, boolean recalcPersonUtil, boolean debug) {
    	
    	int NA_VALUE = 0;
    	
    	//setup best path dmu variables
    	TransitWalkAccessDMU walkDmu = new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
    	walkDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	driveDmu.setTapTable(tvpb.getMttData().getTapTable(), tvpb.getMttData().getTapIdFieldName());
    	
    	if(purpose.toLowerCase().startsWith("work")) {
    		walkDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_walk"));
    		driveDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_work_pnr"));
    	} else {
    		walkDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_walk"));
    		driveDmu.setUserClass(mcDmuObject.getTourObject().getPersonObject().getUserClass("user_class_non_work_pnr"));
    	}
    	
    	walkDmu.setWalkPropClass(mcDmuObject.getTourObject().getPersonObject().getWalkPropClass());
    	driveDmu.setWalkPropClass(walkDmu.getWalkPropClass());
    	
    	//check for existing best taps and calculate utilities if needed
    	int omaz = mcDmuObject.getDmuIndexValues().getOriginZone();
    	int dmaz = mcDmuObject.getDmuIndexValues().getDestZone();
    	
    	int tod = mcDmuObject.getTodOut();
    	double[][] bestWtwTapPairsIn = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTW, omaz, dmaz, tod, debug, aoLogger);
        double[][] bestDtwTapPairsIn = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.DTW, omaz, dmaz, tod, debug, aoLogger);
        
        tod = modelStructure.getTod(modelStructure.getNonMandatoryLocationDefaultDepartPeriod());
        double[][] bestWtwTapPairsOut = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.WTW, omaz, dmaz, tod, debug, aoLogger);
        double[][] bestDtwTapPairsOut = tvpb.getBestTapPairs(walkDmu, driveDmu, tvpb.DTW, omaz, dmaz, tod, debug, aoLogger);
        
        //use _In dmu label for peak outbound measures
        if (bestWtwTapPairsIn[0] == null) {
			mcDmuObject.setOtapWT_In( NA_VALUE );
			mcDmuObject.setDtapWT_In( NA_VALUE );
			mcDmuObject.setGenCostWT_In( NA_VALUE );
        } else {
			mcDmuObject.setOtapWT_In( (int) bestWtwTapPairsIn[0][0] );
			mcDmuObject.setDtapWT_In( (int) bestWtwTapPairsIn[0][1] );
			mcDmuObject.setGenCostWT_In( (float) bestWtwTapPairsIn[0][3] );
        }
        
        if (bestDtwTapPairsIn[0] == null) {
			mcDmuObject.setOtapPNR_In( NA_VALUE );
			mcDmuObject.setDtapPNR_In( NA_VALUE );
			mcDmuObject.setGenCostPNR_In( NA_VALUE );
        } else {
			mcDmuObject.setOtapPNR_In( (int) bestDtwTapPairsIn[0][0] );
			mcDmuObject.setDtapPNR_In( (int) bestDtwTapPairsIn[0][1] );
			mcDmuObject.setGenCostPNR_In( (float) bestDtwTapPairsIn[0][3] );
        }
        
		//use _Out dmu label for offpeak outbound measures
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

    }
}
