package com.pb.models.ctrampIf;

import java.util.Random;
import org.apache.log4j.Logger;


public interface HouseholdIf
{

    /**
     * 
     * @return a 1-based array of the person objects in the household
     */
    public abstract PersonIf[] getPersons();

    public abstract void initializeWindows();

    public abstract void setDebugChoiceModels(boolean value);

    public abstract void setHhId(int id, int baseSeed);

    public abstract void setRandomObject(Random r);

    public abstract void setHhRandomCount(int count);

    // work/school location choice uses shadow pricing, so save randomCount per iteration
    public abstract void setUwslRandomCount(int iter, int count);

    public abstract void setAoRandomCount(int count);

    public abstract void setFpRandomCount(int count);

    public abstract void setCdapRandomCount(int count);

    public abstract void setImtfRandomCount(int count);

    public abstract void setImtodRandomCount(int count);

    public abstract void setAwfRandomCount(int count);

    public abstract void setAwlRandomCount(int count);

    public abstract void setAwtodRandomCount(int count);

    public abstract void setJtfRandomCount(int count);

    public abstract void setJtlRandomCount(int count);

    public abstract void setJtodRandomCount(int count);

    public abstract void setInmtfRandomCount(int count);

    public abstract void setInmtlRandomCount(int count);

    public abstract void setInmtodRandomCount(int count);

    public abstract void setStfRandomCount(int count);

    public abstract void setStlRandomCount(int count);

    public abstract void setHhTaz(int taz);

    public abstract void setHhWalkSubzone(int subzone);

    public abstract void setHhAutos(int autos);

    public abstract void setAutoOwnershipModelResult(int aoModelAlternativeChosen);

    public abstract int getAutoOwnershipModelResult();

    public abstract void setCoordinatedDailyActivityPatternResult(String pattern);

    public abstract String getCoordinatedDailyActivityPattern();

    public abstract void setJointTourFreqResult(int altIndex, String altName);

    public abstract int getJointTourFreqChosenAlt();

    public abstract String getJointTourFreqChosenAltName();

    public abstract void setHhSize(int numPersons);

    public abstract void setHhIncomeInDollars(int dollars);

    public abstract void setHhWorkers(int numWorkers);

    public abstract void setHhType(int type);

    public abstract boolean getDebugChoiceModels();

    public abstract int getHhSize();

    public abstract int getNumberOfNonWorkingAdults();

    public abstract int getIsNonFamilyHousehold();

    public abstract int getNumStudents();

    public abstract int getNumberOfChildrenUnder16WithHomeOrNonMandatoryActivity();
    
    public abstract void setHhBldgsz(int code);

    public abstract int getHhBldgsz();
    
//    /**
//     * return the number of workers this household has for the purpose index.
//     * @param purposeIndex is the DC purpose index to be compared to the usual school location index saved for this
//     * person upon reading synthetic population file.
//     * @return num, a value of the number of workers in the household for this purpose index.
//     */
//    public abstract int getNumberOfWorkersWithDcPurposeIndex(int purposeIndex);
//
//    /**
//     * return the number of university students this household has for the purpose index.
//     * @param purposeIndex is the DC purpose index to be compared to the usual school location index saved for this
//     * person upon reading synthetic population file.
//     * @return num, a value of the number of university students in the household for this purpose index.
//     */
//    public abstract int getNumberOfUniversityStudentsWithDcPurposeIndex(int purposeIndex);
//
//    /**
//     * return the number of school age students this household has for the purpose index.
//     * @param purposeIndex is the DC purpose index to be compared to the usual school location index saved for this
//     * person upon reading synthetic population file.
//     * @return num, a value of the number of school age students in the household for this purpose index.
//     */
//    public abstract int getNumberOfDrivingAgedStudentsWithDcPurposeIndex(int purposeIndex);
//
//    public abstract int getNumberOfNonDrivingAgedStudentsWithDcPurposeIndex(int purposeIndex);

    public abstract PersonIf getPerson(int persNum);

    public abstract int getHhId();

    public abstract Random getHhRandom();

    public abstract int getHhRandomCount();

    public abstract int getUwslRandomCount(int iter);

    public abstract int getAoRandomCount();

    public abstract int getFpRandomCount();

    public abstract int getCdapRandomCount();

    public abstract int getImtfRandomCount();

    public abstract int getImtodRandomCount();

    public abstract int getImmcRandomCount();

    public abstract int getJtfRandomCount();

    public abstract int getAwfRandomCount();

    public abstract int getAwlRandomCount();

    public abstract int getAwtodRandomCount();

    public abstract int getAwmcRandomCount();

    public abstract int getJtlRandomCount();

    public abstract int getJtodRandomCount();

    public abstract int getJmcRandomCount();

    public abstract int getInmtfRandomCount();

    public abstract int getInmtlRandomCount();

    public abstract int getInmtodRandomCount();

    public abstract int getInmmcRandomCount();

    public abstract int getStfRandomCount();

    public abstract int getStlRandomCount();

    public abstract int getHhTaz();

    public abstract int getHhWalkSubzone();

    //TODO suggest getting rid of hhIncome groupings, or moving them down to the project level.
    //these vary for different regions, and it can be a source of error to use it here. 
    //the values here are for ARC only. 
    public abstract int getIncomeSegment();

    public abstract int getIncomeInDollars();

    public abstract int getWorkers();

    public abstract int getDrivers();

    public abstract int getSize();

    public abstract int getChildunder16();

    public abstract int getChild16plus();

    public abstract int getNumChildrenUnder16();
    
    public abstract int getNumChildren();

    public abstract int getNumChildrenUnder19();

    public abstract int getNumPersons0to4();

    public abstract int getNumPersons5to15();
    
    public abstract int getNumPersons6to15();

    public abstract int getNumPersons16to17();

    public abstract int getNumPersons16plus();

    public abstract int getNumPersons18to24();
    
    public abstract int getNumPersons65to79();
    
    public abstract int getNumPersons80plus();

    public abstract int getNumFtWorkers();

    public abstract int getNumPtWorkers();

    public abstract int getNumUnivStudents();

    public abstract int getNumNonWorkAdults();

    public abstract int getNumRetired();

    public abstract int getNumDrivingStudents();

    public abstract int getNumNonDrivingStudents();

    public abstract int getNumPreschool();
    
    public abstract int getHighestEducationAttainment();


    public void calculateTimeWindowOverlaps();
    
    /**
     * joint tour frequency choice is not applied to a household unless it has:
     * 2 or more persons, each with at least one out-of home activity, and at least 1 of the persons not a pre-schooler.
     * */
    public abstract int getValidHouseholdForJointTourFrequencyModel();

    /**
     * return maximum hours of overlap between this person and other adult persons in the household.
     * @return the most number of hours mutually available between this person and other adult household members
     */
    public abstract int getMaxAdultOverlaps();

    /**
     * return maximum hours of overlap between this person and other children in the household.
     * @return the most number of hours mutually available between this person and other child household members
     */
    public abstract int getMaxChildOverlaps();    
    
    /**
     * determine maximum number of time consecutive time intervals available to everyone in joint party. 
     * @param t, the joint tour
     * @return maximunm joint continous time window
     */
    public abstract int getMaxJointTimeWindow( TourIf t );
    
    /**
     * return maximum hours of overlap between this person(adult/child) and other persons(child/adult) in the household.
     * @return the most number of hours mutually available between this person and other type household members
     */
    public abstract int getMaxAdultChildOverlaps();

    /**
     * @return number of adults in household with "M" or "N" activity pattern - that is, traveling adults.
     */
    public abstract int getTravelActiveAdults();

    /**
     * @return number of children in household with "M" or "N" activity pattern - that is, traveling children.
     */
    public abstract int getTravelActiveChildren();


    public abstract boolean[] getAvailableJointTourTimeWindows(TourIf t, int[] altStarts, int[] altEnds);

    public abstract void scheduleJointTourTimeWindows(TourIf t, int start, int end);

    
    public abstract void createJointTourArray();

    public abstract void createJointTourArray(Object[] t1Data);

    public abstract void createJointTourArray(Object[] t1Data, Object[] t2Data);

    public abstract TourIf[] getJointTourArray();

    public abstract void initializeForAoRestart();

    public abstract void initializeForImtfRestart();

    public abstract void initializeForJtfRestart();

    public abstract void initializeForInmtfRestart();

    public abstract void initializeForAwfRestart();

    public abstract void initializeForStfRestart();

    public abstract void logHouseholdObject(String titleString, Logger logger);

    public abstract void logPersonObject(String titleString, Logger logger, PersonIf person);

    public abstract void logTourObject(String titleString, Logger logger, PersonIf person, TourIf tour);

    public abstract void logStopObject(String titleString, Logger logger, StopIf stop,
            ModelStructure modelStructure);

    public abstract long getHouseholdObjectSize();
    
    public abstract PersonIf getOldestPerson();
    
}