package com.pb.models.ctrampIf;

import java.util.ArrayList;
import org.apache.log4j.Logger;

public interface PersonIf
{

    public static final int      MIN_ADULT_AGE                              = 19;
    public static final int      MIN_STUDENT_AGE                            = 5;
    // person type strings used for data summaries
    public static final String   PERSON_TYPE_FULL_TIME_WORKER_NAME          = "Full-time worker";
    public static final String   PERSON_TYPE_PART_TIME_WORKER_NAME          = "Part-time worker";
    public static final String   PERSON_TYPE_UNIVERSITY_STUDENT_NAME        = "University student";
    public static final String   PERSON_TYPE_NON_WORKER_NAME                = "Non-worker";
    public static final String   PERSON_TYPE_RETIRED_NAME                   = "Retired";
    public static final String   PERSON_TYPE_STUDENT_DRIVING_NAME           = "Student of driving age";
    public static final String   PERSON_TYPE_STUDENT_NON_DRIVING_NAME       = "Student of non-driving age";
    public static final String   PERSON_TYPE_PRE_SCHOOL_CHILD_NAME          = "Child too young for school";
    public static final String[] personTypeNameArray                        = {
            PERSON_TYPE_FULL_TIME_WORKER_NAME, PERSON_TYPE_PART_TIME_WORKER_NAME,
            PERSON_TYPE_UNIVERSITY_STUDENT_NAME, PERSON_TYPE_NON_WORKER_NAME,
            PERSON_TYPE_RETIRED_NAME, PERSON_TYPE_STUDENT_DRIVING_NAME,
            PERSON_TYPE_STUDENT_NON_DRIVING_NAME, PERSON_TYPE_PRE_SCHOOL_CHILD_NAME};
    public static final String   EMPLOYMENT_CATEGORY_FULL_TIME_WORKER_NAME  = "Full-time worker";
    public static final String   EMPLOYMENT_CATEGORY_PART_TIME_WORKER_NAME  = "Part-time worker";
    public static final String   EMPLOYMENT_CATEGORY_NOT_EMPLOYED_NAME      = "Not employed";
    public static final String   EMPLOYMENT_CATEGORY_UNDER_AGE_16_NAME      = "Under age 16";
    public static final String[] employmentCategoryNameArray                = {
            EMPLOYMENT_CATEGORY_FULL_TIME_WORKER_NAME, EMPLOYMENT_CATEGORY_PART_TIME_WORKER_NAME,
            EMPLOYMENT_CATEGORY_NOT_EMPLOYED_NAME, EMPLOYMENT_CATEGORY_UNDER_AGE_16_NAME};
    public static final String   STUDENT_CATEGORY_GRADE_OR_HIGH_SCHOOL_NAME = "Grade or high school";
    public static final String   STUDENT_CATEGORY_COLLEGE_OR_HIGHER_NAME    = "College or higher";
    public static final String   STUDENT_CATEGORY_NOT_STUDENT_NAME          = "Not student";
    public static final String[] studentCategoryNameArray                   = {
            STUDENT_CATEGORY_GRADE_OR_HIGH_SCHOOL_NAME, STUDENT_CATEGORY_COLLEGE_OR_HIGHER_NAME,
            STUDENT_CATEGORY_NOT_STUDENT_NAME                               };

    public abstract TourIf makeDefaultTour( int purposeIndex );
    
    public abstract HouseholdIf getHouseholdObject();

    public abstract ArrayList<TourIf> getListOfWorkTours();

    public abstract ArrayList<TourIf> getListOfSchoolTours();

    public abstract ArrayList<TourIf> getListOfIndividualNonMandatoryTours();

    public abstract ArrayList<TourIf> getListOfAtWorkSubtours();

    public abstract byte[] getTimeWindows();

    public abstract void initializeWindows();

    public abstract void resetTimeWindow(int startHour, int endHour);
    public void resetTimeWindow();


    // code the timw window array for this tour being scheduled.
    // 0: unscheduled, 1: scheduled, middle of tour, 2: scheduled, start of tour,, 3: scheduled, end of tour,
    // 4: scheduled, end of previous tour, start of current tour or end of current tour, start of subsequent tour;
    //    or current tour start/end same hour.
    public abstract void scheduleWindow(int start, int end);

    public abstract boolean[] getAvailableTimeWindows(int[] altStarts, int[] altEnds);

    public abstract boolean isWindowAvailable(int start, int end);

    /**
     * @return true if the window for the argument is the end of a previously scheduled tour
     * and this hour does not overlap with any other tour.
     */
    public abstract boolean isPreviousArrival(int hour);

    /**
     * @return true if the window for the argument is the start of a previously scheduled tour
     * and this hour does not overlap with any other tour.
     */
    public abstract boolean isPreviousDeparture(int hour);

    public abstract boolean isPeriodAvailable(int period);

    public abstract void setPersId(int id);

    public abstract void setWorkLocationPurposeIndex(int workPurpose);

    public abstract void setUniversityLocationPurposeIndex(int universityPurpose);

    public abstract void setSchoolLocationPurposeIndex(int schoolPurpose);

    public abstract void setPersAge(int age);

    public abstract void setPersGender(int gender);

    public abstract void setPersEmploymentCategory(int category);

    public abstract void setPersStudentCategory(int category);

    public abstract void setPersonTypeCategory(int personTypeCategory);

    public abstract void setValueOfTime(float vot);

    public abstract void setWorkLoc(int loc);

    public abstract void setWorkLocSubzone(int subzone);

    public abstract void setSchoolLoc(int loc);

    public abstract void setSchoolLocSubzone(int subzone);

    public abstract void setFreeParkingAvailableResult(int fpResult);

    public abstract int getFreeParkingAvailableResult();

    public abstract void setImtfChoice(int choice);

    public abstract void setInmtfChoice(int choice);

    public abstract int getImtfChoice();

    public abstract int getInmtfChoice();

    public abstract void clearIndividualNonMandatoryToursArray();

    public abstract void createIndividualNonMandatoryTours(int numberOfTours, String purposeName, ModelStructure modelStructure);

    public abstract void createWorkTours(int numberOfTours, int startId, String tourPurpose, ModelStructure modelStructure);

    public abstract void clearAtWorkSubtours();

    public abstract void createAtWorkSubtour(int id, int choice, int workTaz, int workSubZone, String tourPurpose, ModelStructure modelStructure);

    public abstract void createSchoolTours(int numberOfTours, int startId, String tourPurpose, ModelStructure modelStructure);

    public abstract int getWorkLocationPurposeIndex();

    public abstract int getUniversityLocationPurposeIndex();

    public abstract int getSchoolLocationPurposeIndex();

    public abstract void setDailyActivityResult(String activity);

    public abstract int getPersonIsChildUnder16WithHomeOrNonMandatoryActivity();

    public abstract int getAge();

    public abstract int getHomemaker();

    public abstract int getGender();

    public abstract int getPersonIsFemale();

    public abstract int getPersonIsMale();

    public abstract int getPersonId();

    public abstract int getPersonNum();

    public abstract String getPersonType();

    public abstract int getPersonTypeNumber();

    public abstract String getPersonEmploymentCategory();

    public abstract String getPersonStudentCategory();

    public abstract float getValueOfTime();

    public abstract int getPersonWorkLocationZone();

    public abstract int getPersonWorkLocationSubZone();

    public abstract int getPersonSchoolLocationSubZone();

    public abstract int getPersonSchoolLocationZone();

    public abstract String getCdapActivity();

    public abstract int getUsualWorkLocation();

    public abstract int getUsualSchoolLocation();

    public abstract int getNumWorkTours();

    public abstract int getNumUniversityTours();

    public abstract int getNumSchoolTours();

    public abstract int getNumIndividualEscortTours();

    public abstract int getNumIndividualShoppingTours();
    
    public abstract int getNumTotalIndivTours();

    public abstract int getNumIndividualEatOutTours();

    public abstract int getNumIndividualOthMaintTours();

    public abstract int getNumIndividualSocialTours();

    public abstract int getNumIndividualOthDiscrTours();

    public abstract int getNumMandatoryTours();

    public abstract int getNumJointShoppingTours();

    public abstract int getNumJointOthMaintTours();

    public abstract int getNumJointEatOutTours();

    public abstract int getNumJointSocialTours();

    public abstract int getNumJointOthDiscrTours();

    public abstract void computeIdapResidualWindows();

    public abstract int getWindowBeforeFirstMandJointTour();

    public abstract int getWindowBetweenFirstLastMandJointTour();

    public abstract int getWindowAfterLastMandJointTour();

    /**
     * determine if person is a worker (indepdent of person type).
     * @return 1 if worker, 0 otherwise.
     */
    public abstract int getPersonIsWorker();

    /**
     * Determine if person is a student (of any age, independent of person type)
     * @return 1 if student, 0 otherwise
     */
    public abstract int getPersonIsStudent();

    public abstract int getPersonIsUniversityStudent();

    public abstract int getPersonIsStudentDriving();

    public abstract int getPersonIsStudentNonDriving();

    /**
     * Determine if person is a full-time worker (independent of person type)
     * @return 1 if full-time worker, 0 otherwise
     */
    public abstract int getPersonIsFullTimeWorker();

    /**
     * Determine if person is a part-time worker (indepdent of person type)
     */
    public abstract int getPersonIsPartTimeWorker();

    public abstract int getPersonTypeIsFullTimeWorker();

    public abstract int getPersonTypeIsPartTimeWorker();

    public abstract int getPersonIsNonWorkingAdultUnder65();

    public abstract int getPersonIsNonWorkingAdultOver65();

    public abstract int getPersonIsPreschoolChild();

    public abstract int getPersonIsAdult();

    /**
     * return maximum hours of overlap between this person and other adult persons in the household.
     * @return the most number of hours mutually available between this person and other adult household members
     */
    public abstract int getMaxAdultOverlaps();

    /**
     * set maximum hours of overlap between this person and other adult persons in the household.
     * @param overlaps are the most number of hours mutually available between this person and other adult household members
     */
    public abstract void setMaxAdultOverlaps(int overlaps);

    /**
     * return maximum hours of overlap between this person and other children in the household.
     * @return the most number of hours mutually available between this person and other child household members
     */
    public abstract int getMaxChildOverlaps();

    /**
     * set maximum hours of overlap between this person and other children in the household.
     * @param overlaps are the most number of hours mutually available between this person and other child household members
     */
    public abstract void setMaxChildOverlaps(int overlaps);

    /**
     * return available time window for this person in the household.
     * @return the total number of hours available for this person
     */
    public abstract int getAvailableWindow();

    public abstract void setTimeWindows(byte[] win);

    public abstract void initializeForAoRestart();

    public abstract void initializeForImtfRestart();

    /**
     * initialize the person attributes and tour objects for restarting the model at joint tour frequency
     */
    public abstract void initializeForJtfRestart();

    /**
     * initialize the person attributes and tour objects for restarting the model at individual non-mandatory tour frequency.
     */
    public abstract void initializeForInmtfRestart();

    /**
     * initialize the person attributes and tour objects for restarting the model at at-work sub-tour frequency.
     */
    public abstract void initializeForAwfRestart();

    /**
     * initialize the person attributes and tour objects for restarting the model at stop frequency.
     */
    public abstract void initializeForStfRestart();

    public abstract void logPersonObject(Logger logger, int totalChars);

    public abstract void logTourObject(Logger logger, int totalChars, TourIf tour);

    public abstract void logEntirePersonObject(Logger logger);

    public void setPersonIsHighSchool(boolean flag);

    public int getPersonIsHighSchool();

    public void setPersonIsGradeSchool(boolean flag);

    public int getPersonIsGradeSchool();

    public int getPersonIsHighSchoolGraduate();
    
    public int getMaximumContinuousAvailableWindow();

    public int getMaximumContinuousPairwiseAvailableWindow( byte[] otherWindow );

    public int getPersonIsChild6To18WithoutMandatoryActivity();
    
    public int getPersonEducAttainment();
    
    public void setPersonEducAttainment(int educAttainCode);
    
    public int getPersonIndustryCensus();
    
    public void setPersonIndustryCensus(int indCensusCode);
    
    public int getPersonWorkerOccupation();
    
    public void setPersonWorkerOccupation(int workerOccupation);
    
    public abstract float getWalkTimeWeight();
    
    public abstract void setWalkTimeWeight(float walkTimeWeight);
    
    public abstract float getWalkSpeed();
    
    public abstract void setWalkSpeed(float walkSpeed);
    
    public abstract float getMaxWalk();
    
    public abstract void setMaxWalk(float maxWalk);
    
    public abstract int getUserClass(String type);
    
    public abstract void setUserClass(String type, int userClass);
    
    public abstract int getWalkPropClass();
    
    public abstract void setWalkPropClass(int walkPropClass);    

}
