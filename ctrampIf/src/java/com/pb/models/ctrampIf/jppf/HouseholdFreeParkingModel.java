package com.pb.models.ctrampIf.jppf;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.FreeParkingChoiceDMU;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.common.newmodel.ChoiceModelApplication;

public class HouseholdFreeParkingModel implements Serializable {
    
    private transient Logger logger = Logger.getLogger(HouseholdFreeParkingModel.class);

    private static final String FP_CONTROL_FILE_TARGET = "UecFile.FreeParking";
    private static final int FP_DATA_SHEET = 0;
    private static final int FP_MODEL_SHEET = 1;


    private ChoiceModelApplication fpModel;
    private FreeParkingChoiceDMU fpDmuObject;


    public HouseholdFreeParkingModel(HashMap<String,String> propertyMap, TazDataIf tazDataManager, CtrampDmuFactoryIf dmuFactory ) {
        setupFreeParkingChoiceModelApplication( propertyMap, tazDataManager, dmuFactory );
    }


    private void setupFreeParkingChoiceModelApplication(HashMap<String,String> propertyMap, TazDataIf tazDataManager, CtrampDmuFactoryIf dmuFactory) {
        logger.info( "setting up free parking choice model." );

        // locate the auto ownership UEC
        String projectDirectory = propertyMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);
        String fpUecFile = projectDirectory + propertyMap.get(FP_CONTROL_FILE_TARGET);

        // create the auto ownership choice model DMU object.
        fpDmuObject = dmuFactory.getFreeParkingChoiceDMU();

        // create the auto ownership choice model object
        fpModel = new ChoiceModelApplication(fpUecFile,FP_MODEL_SHEET,FP_DATA_SHEET,propertyMap, (VariableTable)fpDmuObject );

    }


    public void applyModel(HouseholdIf hhObject){

    	// apply model at a person level, but use the same random number
    	// for all persons so the result is consistent with the ARC legacy code
        Random hhRandom = hhObject.getHhRandom();
        double randomNumber = hhRandom.nextDouble();

    	// person array is 1-based
        PersonIf[] person = hhObject.getPersons();         
        for (int i=1; i<person.length; i++) {
            int chosen = getFreeParkingChoice(person[i], randomNumber);
            person[i].setFreeParkingAvailableResult((short)chosen);
        }

        hhObject.setFpRandomCount(hhObject.getHhRandomCount());
    }

    
    private int getFreeParkingChoice (PersonIf personObj, double randomNumber) {
    	
    	// get the corresponding household object
    	HouseholdIf hhObj = personObj.getHouseholdObject(); 

        // update the AO dmuObject for this person
        fpDmuObject.setPersonObject(personObj);
        fpDmuObject.setDmuIndexValues(hhObj.getHhId(),personObj.getUsualWorkLocation(),hhObj.getHhTaz(),personObj.getUsualWorkLocation());

        // compute utilities and choose auto ownership alternative.
        fpModel.computeUtilities (fpDmuObject,fpDmuObject.getDmuIndexValues() );

        // if the choice model has at least one available alternative, make choice.
        int chosenAlt;
        if (fpModel.getAvailabilityCount() > 0) {
            chosenAlt = fpModel.getChoiceResult(randomNumber);
        }
        else {
            String errorMessage = String.format("Exception caught for HHID=%d, PERSID=%d, no available free parking alternatives to choose from in choiceModelApplication.", hhObj.getHhId(), personObj.getPersonId());
            logger.error (errorMessage);
            throw new RuntimeException();
        }

        // write choice model alternative info to log file
        if ( hhObj.getDebugChoiceModels() ) {
            fpModel.logAlternativesInfo("Free parking Choice", String.format("PERS_%d", personObj.getPersonId()));
            fpModel.logSelectionInfo ( "Free parking Choice", String.format("PERS_%d", personObj.getPersonId()), randomNumber, chosenAlt);
        }

        return chosenAlt;
    }
    
}
