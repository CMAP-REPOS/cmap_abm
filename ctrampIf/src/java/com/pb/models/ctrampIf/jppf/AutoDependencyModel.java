package com.pb.models.ctrampIf.jppf;

import java.io.Serializable;
import java.util.*;

import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.TourModeChoiceDMU;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TourIf;
import com.pb.common.newmodel.ChoiceModelApplication;

import org.apache.log4j.Logger;

public class AutoDependencyModel implements Serializable {
    
    private transient Logger logger = Logger.getLogger(AutoDependencyModel.class);
    private transient Logger aoLogger = Logger.getLogger("ao");
    
    public static final String PROPERTIES_UEC_AUTO_DEPENDENCY = "UecFile.AutoDependency";
    private static final int DATA_SHEET               = 0;
    private static final int WORK_MODEL_SHEET         = 1;
    private static final int UNIVERSITY_MODEL_SHEET   = 2;
    private static final int SCHOOL_MODEL_SHEET       = 3;
    private static final int[] MODEL_SHEETS = {WORK_MODEL_SHEET, UNIVERSITY_MODEL_SHEET, SCHOOL_MODEL_SHEET};
    private String[] tourPurposeList = new String[MODEL_SHEETS.length];
    private static final int[] NON_AUTO_ALTS = {3,5,8,10};  //1-based
    
    // A MyChoiceModelApplication object and modeAltsAvailable[] is needed for each purpose
    private ChoiceModelApplication adModel[];
    private TourModeChoiceDMU mcDmuObject;

    private String tourCategory;
    private HashMap<String, Integer> purposeModelIndexMap;
    
    
    public AutoDependencyModel( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory, CtrampDmuFactoryIf dmuFactory ){

        this.tourCategory =  tourCategory;
        mcDmuObject = dmuFactory.getModeChoiceDMU();
        setupAutoDependencyModelApplicationArray( propertyMap, modelStructure, tourCategory );
    }

    private void setupAutoDependencyModelApplicationArray( HashMap<String, String> propertyMap, ModelStructure modelStructure, String tourCategory ) {

        logger.info( String.format( "setting up %s auto dependency model.", tourCategory ) );

        // locate the auto dependency model UEC
        String projectDirectory = propertyMap.get( CtrampApplication.PROPERTIES_PROJECT_DIRECTORY );
        String adUecFile = propertyMap.get( PROPERTIES_UEC_AUTO_DEPENDENCY );
        adUecFile = projectDirectory + adUecFile;

        //get tour purpose names
        tourPurposeList[0] = ModelStructure.WORK_PRIMARY_PURPOSE_NAME.toLowerCase();
        tourPurposeList[1] = ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME.toLowerCase();
        tourPurposeList[2] = ModelStructure.SCHOOL_PRIMARY_PURPOSE_NAME.toLowerCase();
                
        // create a HashMap to map purposeName to model index
        purposeModelIndexMap = new HashMap<String, Integer>();

        // keep a set of unique model sheet numbers so that we can create ChoiceModelApplication objects once for each model sheet used
        TreeSet<Integer> modelIndexSet = new TreeSet<Integer>();
        
        for ( int i=0; i<tourPurposeList.length; i++ ) {
            purposeModelIndexMap.put( tourPurposeList[i], MODEL_SHEETS[i] );
            modelIndexSet.add( MODEL_SHEETS[i] );
        }
        
        adModel = new ChoiceModelApplication[MODEL_SHEETS.length + 1]; //+1 since model sheet indexing starts at 1
        
        // for each unique model index, create the ChoiceModelApplication object and the availability array
        Iterator<Integer> it = modelIndexSet.iterator();
        while ( it.hasNext() ) {
            int m = it.next();
            adModel[m] = new ChoiceModelApplication ( adUecFile, m, DATA_SHEET, propertyMap, (VariableTable)mcDmuObject );
        }
    }

    /**
     * Calculate and return the logsum for the DMU object.
     * @param mcDmuObject is the tour mode choice model DMU object
     * @param solveForAutoModesOnly if true sets non-auto modes to not avail
     * @return logsum
     */
    public double getLogsum ( TourModeChoiceDMU mcDmuObject, String primaryPurposeName, Logger modelLogger, String choiceModelDescription, String decisionMakerLabel, boolean solveForAutoModesOnly) {
        
        int modelIndex = purposeModelIndexMap.get( primaryPurposeName.toLowerCase() );

        HouseholdIf household = mcDmuObject.getHouseholdObject();
        
        // log headers to traceLogger
        if ( household.getDebugChoiceModels() ) {
        	adModel[modelIndex].choiceModelUtilityTraceLoggerHeading( choiceModelDescription, decisionMakerLabel );
        }
        
        //set availabilities, calculate utilities and get logsum
        boolean[] avail = adModel[modelIndex].getAvailabilities(); //index=0 is empty since array starts at 1
        int[] sample = new int[avail.length];
        
        if(solveForAutoModesOnly) {

        	//set all available
        	for(int i=0; i<avail.length; i++) {
    			avail[i] = true;
    			sample[i] = 1;
            }
        	
        	//set non-auto to not available
    		for(int j=0; j<NON_AUTO_ALTS.length; j++) {
    			int alt = NON_AUTO_ALTS[j];
    			avail[alt] = false;
    			sample[alt] = 0;
    		}
    		        	
        	adModel[modelIndex].computeUtilities( mcDmuObject, mcDmuObject.getDmuIndexValues(), avail, sample);
        	
        } else {
        
        	//set all not available
        	for(int i=0; i<avail.length; i++) {
    			avail[i] = false;
    			sample[i] = 0;
            }
        
        	//set non-auto to available
    		for(int j=0; j<NON_AUTO_ALTS.length; j++) {
    			int alt = NON_AUTO_ALTS[j];
    			avail[alt] = true;
    			sample[alt] = 1;
    		}
        	
        	adModel[modelIndex].computeUtilities( mcDmuObject, mcDmuObject.getDmuIndexValues(), avail, sample);
        	
        }
        double logsum = adModel[modelIndex].getLogsum();

        
        // write UEC calculation results to separate model specific log file
        if( household.getDebugChoiceModels() ){
        	String loggingHeader = String.format( "%s   %s", choiceModelDescription, decisionMakerLabel );
        	adModel[modelIndex].logUECResults( aoLogger, loggingHeader );
        	aoLogger.info( choiceModelDescription + " Logsum value: " + logsum );
        	aoLogger.info( "" );
        	aoLogger.info( "" );
        }

        return logsum;

    }

    /**
     * Get the choice model application object
     */
    public ChoiceModelApplication getChoiceModelApplication ( String primaryPurposeName) {

        int modelIndex = purposeModelIndexMap.get( primaryPurposeName.toLowerCase() );
        return adModel[modelIndex];
    }

}

