package com.pb.cmap.ctramp;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TourIf;

public class Person implements PersonIf, java.io.Serializable
{

    // 8 am default departure period
    //public static final int      DEFAULT_MANDATORY_START_PERIOD             = 6;
    // 11 am default departure period
    public static final int      DEFAULT_NON_MANDATORY_START_PERIOD         = 12;
    // 12 pm default departure period
    public static final int      DEFAULT_AT_WORK_SUBTOUR_START_PERIOD       = 14;
    // 5 pm default arrival period
    //public static final int      DEFAULT_MANDATORY_END_PERIOD               = 24;
    // 3 pm default arrival period
    public static final int      DEFAULT_NON_MANDATORY_END_PERIOD           = 20;
    // 2 pm default arrival period
    public static final int      DEFAULT_AT_WORK_SUBTOUR_END_PERIOD         = 18;
    
    

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

    // Employment category (1-employed FT, 2-employed PT, 3-not employed, 4-under age
    // 16)
    // Student category (1 - student in grade or high school; 2 - student in college
    // or higher; 3 - not a student)

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

    private Household            hhObj;

    private int                  persNum;
    private int                  persId;
    private int                  persAge;
    private int                  persGender;
    private int                  persPecasOccup;
    private int                  persActivityCode;
    private int                  persEmploymentCategory;
    private int                  persStudentCategory;
    private int                  personType;
    private boolean              gradeSchool;
    private boolean              highSchool;
    private boolean              highSchoolGraduate;
    private boolean              hasBachelors;
    private float                persValueOfTime;                                                          // individual
    // value-of-time
    // in
    // $/hr

    private int                  persEduc;
    private int                  persIndCen;
    private int                  persWorkerOccupation;
    
    private int workLoc;
    private int workLocSubzone;
    private float                workLocDistance;
    private float                workLocLogsum;
    private int schoolLoc;
    private int schoolLocSubzone;
    private float                schoolLocDistance;
    private float                schoolLocLogsum;

    private int workLocationPurposeIndex;
    private int universityLocationPurposeIndex;
    private int schoolLocationPurposeIndex;
    
    private byte freeParkingAvailable;
    
    private String               cdapActivity;
    private int                  imtfChoice;
    private int                  inmtfChoice;

    private int                  maxAdultOverlaps;
    private int                  maxChildOverlaps;

    private ArrayList<TourIf>      workTourArrayList;
    private ArrayList<TourIf>      schoolTourArrayList;
    private ArrayList<TourIf>      indNonManTourArrayList;
    private ArrayList<TourIf>      atWorkSubtourArrayList;

    // private Scheduler scheduler;
    // windows[] is 1s based - indexed from 1 to number of intervals.
    private byte[]               windows;

    private int                  windowBeforeFirstMandJointTour;
    private int                  windowBetweenFirstLastMandJointTour;
    private int                  windowAfterLastMandJointTour;

    private ModelStructure       modelStructure;
    
    private float walkTimeWeight;
    private float walkSpeed;
    private float maxWalk;
    
    private HashMap<String,Integer> userClass = new HashMap<String,Integer>();
    
    public Person(HouseholdIf hhObj, int persNum, ModelStructure modelStructure)
    {
        this.hhObj = (Household) hhObj;
        this.persNum = persNum;
        this.workTourArrayList = new ArrayList<TourIf>();
        this.schoolTourArrayList = new ArrayList<TourIf>();
        this.indNonManTourArrayList = new ArrayList<TourIf>();
        this.atWorkSubtourArrayList = new ArrayList<TourIf>();
        this.modelStructure = modelStructure;

        initializeWindows();
    }

    public Tour makeDefaultTour( int purposeIndex ) {
        return new Tour( this, purposeIndex );
    }
    
    public HouseholdIf getHouseholdObject()
    {
        return hhObj;
    }

    public ArrayList<TourIf> getListOfWorkTours()
    {
        return workTourArrayList;
    }

    public ArrayList<TourIf> getListOfSchoolTours()
    {
        return schoolTourArrayList;
    }

    public ArrayList<TourIf> getListOfIndividualNonMandatoryTours()
    {
        return indNonManTourArrayList;
    }

    public ArrayList<TourIf> getListOfAtWorkSubtours()
    {
        return atWorkSubtourArrayList;
    }

    public byte[] getTimeWindows()
    {
        return windows;
    }
    
    public String getTimePeriodLabel(int windowIndex){
        return modelStructure.getTimePeriodLabel(windowIndex);
    }

    public void initializeWindows()
    {
        windows = new byte[modelStructure.getNumberOfTimePeriods()+1];
    }

    public void resetTimeWindow(int startPeriod, int endPeriod)
    {
        for (int i = startPeriod; i <= endPeriod; i++)
        {
            windows[i] = 0;
        }
    }

    public void resetTimeWindow()
    {
        for (int i = 0; i < windows.length; i++)
        {
            windows[i] = 0;
        }
    }

    /**
     * code the time window array for this tour being scheduled.
     * 0: unscheduled,
     * 1: scheduled, middle of tour,
     * 2: scheduled, start of tour,
     * 3: scheduled, end of tour,
     * 4: scheduled, end of previous tour, start of current tour or
     *               end of current tour, start of subsequent tour;
     *               or current tour start/end same period.
     * @param start is the departure period index for the tour
     * @param end is the arrival period index for the tour
     */
    public void scheduleWindow(int start, int end)
    {

        /*
         * This is the logic used in ARC/MTC, but for SANDAG, we don't allow overlapping tours
         * 
         * 
        if (start == end)
        {
            windows[start] = 4;
        } else
        {
            if (windows[start] == 3) windows[start] = 4;
            else if (windows[start] == 0) windows[start] = 2;

            if (windows[end] == 2) windows[end] = 4;
            else if (windows[end] == 0) windows[end] = 3;
        }

        for (int h = start + 1; h < end; h++)
        {
            windows[h] = 1;
        }
        */
        
        for (int h=start; h <= end; h++)
        {
            windows[h] = 1;
        }
        
    }

    public boolean[] getAvailableTimeWindows(int[] altStarts, int[] altEnds)
    {

        // availability array is used by UEC based choice model, which uses 1-based
        // indexing
        boolean[] availability = new boolean[altStarts.length + 1];

        for (int i = 1; i <= altStarts.length; i++)
        {
            int start = altStarts[i - 1];
            int end = altEnds[i - 1];
            availability[i] = isWindowAvailable(start, end);
        }

        return availability;
    }

    public boolean isWindowAvailable(int start, int end)
    {

        /*
         * This is the logic used in ARC/MTC, but for SANDAG, we don't allow overlapping tours
         * 
         * 
        // check start period, if window is 0, it is unscheduled;
        // if window is 3, it is the last period of another tour, and available
        // as the first period of this tour.
        if (windows[start] == 1) return false;
        else if (windows[start] == 2 && start != end) return false;

        // check end period, if window is 0, it is unscheduled;
        // if window is 2, it is the first period of another tour, and available
        // as the last period of this tour.
        if (windows[end] == 1) return false;
        else if (windows[end] == 3 && start != end) return false;

        // the alternative is available if start and end are available, and all periods
        // from start+1,...,end-1 are available.
        for (int h = start + 1; h < end; h++)
        {
            if (windows[h] > 0) return false;
        }

        return true;
         */        
        
        // the alternative is available if all intervals between start and end, inclusive, are available
        for (int h=start; h <= end; h++)
        {
            if (windows[h] > 0) return false;
        }

        return true;
        
    }

    /**
     * @return true if the window for the argument is the end of a previously
     *         scheduled tour and this period does not overlap with any other tour.
     */
    public boolean isPreviousArrival(int period)
    {

        if (windows[period] == 3 || windows[period] == 4) return true;
        else return false;

    }

    /**
     * @return true if the window for the argument is the start of a previously
     *         scheduled tour and this period does not overlap with any other tour.
     */
    public boolean isPreviousDeparture(int period)
    {

        if (windows[period] == 2 || windows[period] == 4) return true;
        else return false;

    }

    public void setPersId(int id)
    {
        persId = id;
    }

    public void setPersAge(int age)
    {
        persAge = age;
    }

    public void setPersGender(int gender)
    {
        persGender = gender;
    }

    public void setPersPecasOccup(int occup)
    {
        persPecasOccup = occup;
    }

    public void setPersActivityCode(int actCode)
    {
        persActivityCode = actCode;
    }

    public void setPersEmploymentCategory(int category)
    {
        persEmploymentCategory = category;
    }

    public void setPersStudentCategory(int category)
    {
        persStudentCategory = category;
    }

    public void setPersonTypeCategory(int personTypeCategory)
    {
        personType = personTypeCategory;
    }

    public void setValueOfTime(float vot)
    {
        persValueOfTime = vot;
    }

    public void setWorkLoc(int loc)
    {
        workLoc = loc;
    }

    public void setWorkLocDistance(float distance)
    {
        workLocDistance = distance;
    }

    public void setWorkLocLogsum(float logsum)
    {
        workLocLogsum = logsum;
    }

    public void setSchoolLoc(int loc)
    {
        schoolLoc = loc;
    }

    public void setSchoolLocDistance(float distance)
    {
        schoolLocDistance = distance;
    }

    public void setSchoolLocLogsum(float logsum)
    {
        schoolLocLogsum = logsum;
    }

    public void setImtfChoice(int choice)
    {
        imtfChoice = choice;
    }

    public void setInmtfChoice(int choice)
    {
        inmtfChoice = choice;
    }

    public int getImtfChoice()
    {
        return imtfChoice;
    }

    public int getInmtfChoice()
    {
        return inmtfChoice;
    }

    public void clearIndividualNonMandatoryToursArray()
    {
        indNonManTourArrayList.clear();
    }


    public void clearAtWorkSubtours()
    {

        atWorkSubtourArrayList.clear();

    }

    public void setDailyActivityResult(String activity)
    {
        this.cdapActivity = activity;
    }

    public int getPersonIsChildUnder16WithHomeOrNonMandatoryActivity()
    {

        // check the person type
        if (persIsStudentNonDrivingAge() == 1 || persIsPreschoolChild() == 1)
        {

            // check the activity type
            if (cdapActivity.equalsIgnoreCase(ModelStructure.HOME_PATTERN)) return (1);

            if (cdapActivity.equalsIgnoreCase(ModelStructure.MANDATORY_PATTERN)) return (1);

        }

        return (0);
    }

    /**
     * @return 1 if M, 2 if N, 3 if H
     */
    public int getCdapIndex()
    {

        // return the activity type
        if (cdapActivity.equalsIgnoreCase(ModelStructure.MANDATORY_PATTERN)) return 1;

        if (cdapActivity.equalsIgnoreCase(ModelStructure.NONMANDATORY_PATTERN)) return 2;

        if (cdapActivity.equalsIgnoreCase(ModelStructure.HOME_PATTERN)) return 3;

        return (0);
    }

    public int getPersonIsChild6To18WithoutMandatoryActivity()
    {

        // check the person type
        if (persIsStudentDrivingAge() == 1 || persIsStudentNonDrivingAge() == 1)
        {

            // check the activity type
            if (cdapActivity.equalsIgnoreCase(ModelStructure.MANDATORY_PATTERN)) return 0;
            else return 1;

        }

        return 0;
    }

    // methods DMU will use to get info from household object

    public int getAge()
    {
        return persAge;
    }

    public int getHomemaker()
    {
        return persIsHomemaker();
    }

    public int getGender()
    {
        return persGender;
    }

    public int getPersonIsFemale()
    {
        if (persGender == 2) return 1;
        return 0;
    }

    public int getPersonIsMale()
    {
        if (persGender == 1) return 1;
        return 0;
    }

    public int getPersonId()
    {
        return this.persId;
    }

    public int getPersonNum()
    {
        return this.persNum;
    }

    public String getPersonType()
    {
        return personTypeNameArray[personType - 1];
    }

    public void setPersonIsHighSchool(boolean flag)
    {
        highSchool = flag;
    }

    public int getPersonIsHighSchool()
    {
        return highSchool ? 1 : 0;
    }

    public void setPersonIsGradeSchool(boolean flag)
    {
        gradeSchool = flag;
    }

    public int getPersonIsGradeSchool()
    {
        return gradeSchool ? 1 : 0;
    }

    public int getPersonIsHighSchoolGraduate()
    {
        return highSchoolGraduate ? 1 : 0;
    }

    public void setPersonIsHighSchoolGraduate(boolean hsGrad)
    {
        highSchoolGraduate = hsGrad;
    }

    public void setPersonHasBachelors(boolean hasBS)
    {
        hasBachelors = hasBS;
    }

    public int getPersonTypeNumber()
    {
        return personType;
    }

    public int getPersPecasOccup()
    {
        return persPecasOccup;
    }

    public int getPersActivityCode()
    {
        return persActivityCode;
    }

    public int getPersonEmploymentCategoryIndex()
    {
        return persEmploymentCategory;
    }

    public String getPersonEmploymentCategory()
    {
        return employmentCategoryNameArray[persEmploymentCategory - 1];
    }

    public int getPersonStudentCategoryIndex()
    {
        return persStudentCategory;
    }

    public String getPersonStudentCategory()
    {
        return studentCategoryNameArray[persStudentCategory - 1];
    }

    public float getValueOfTime()
    {
        return persValueOfTime;
    }

    public int getPersonWorkLocationZone()
    {
        return workLoc;
    }

    public int getPersonSchoolLocationZone()
    {
        return schoolLoc;
    }

    public String getCdapActivity()
    {
        return cdapActivity;
    }

    public int getUsualWorkLocation()
    {
        return workLoc;
    }

    public float getWorkLocationDistance()
    {
        return workLocDistance;
    }

    public float getWorkLocationLogsum()
    {
        return workLocLogsum;
    }

    public int getUsualSchoolLocation()
    {
        return schoolLoc;
    }

    public float getSchoolLocationDistance()
    {
        return schoolLocDistance;
    }

    public float getSchoolLocationLogsum()
    {
        return schoolLocLogsum;
    }

    public int getHasBachelors()
    {
        return hasBachelors ? 1 : 0;
    }

    public int getNumWorkTours()
    {
        ArrayList<TourIf> workTours = getListOfWorkTours();
        if (workTours != null) return workTours.size();
        else return 0;
    }

    public int getNumSchoolTours()
    {
        ArrayList<TourIf> schoolTours = getListOfSchoolTours();
        if (schoolTours != null) return schoolTours.size();
        else return 0;
    }

    public int getNumIndividualEscortTours()
    {
        int num = 0;
        for (TourIf tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.ESCORT_PURPOSE_NAME)) num++;
        return num;
    }

    public int getNumIndividualShoppingTours()
    {
        int num = 0;
        for (TourIf tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.SHOPPING_PURPOSE_NAME))
                num++;
        return num;
    }

    public int getNumIndividualEatOutTours()
    {
        int num = 0;
        for (TourIf tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.EAT_OUT_PURPOSE_NAME)) num++;
        return num;
    }

    public int getNumIndividualOthMaintTours()
    {
        int num = 0;
        for (TourIf tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.OTH_MAINT_PURPOSE_NAME))
                num++;
        return num;
    }

    public int getNumIndividualSocialTours()
    {
        int num = 0;
        for (TourIf tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.SOCIAL_PURPOSE_NAME)) num++;
        return num;
    }

    public int getNumIndividualOthDiscrTours()
    {
        int num = 0;
        for (TourIf tour : getListOfIndividualNonMandatoryTours())
            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(modelStructure.OTH_DISCR_PURPOSE_NAME))
                num++;
        return num;
    }

    public int getNumMandatoryTours()
    {
        int numTours = 0;
        ArrayList<TourIf> workTours = getListOfWorkTours();
        if (workTours != null) numTours += workTours.size();

        ArrayList<TourIf> schoolTours = getListOfSchoolTours();
        if (schoolTours != null) numTours += schoolTours.size();

        return numTours;
    }

    public int getNumNonMandatoryTours()
    {
        ArrayList<TourIf> nonMandTours = getListOfIndividualNonMandatoryTours();
        if (nonMandTours == null) return 0;
        else return nonMandTours.size();
    }

    public int getNumSubtours()
    {
        ArrayList<TourIf> subtours = getListOfAtWorkSubtours();
        if (subtours == null) return 0;
        else return subtours.size();
    }

    public int getNumTotalIndivTours()
    {
        return getNumMandatoryTours() + getNumNonMandatoryTours() + getNumSubtours();
    }

    public int getNumJointShoppingTours()
    {
        return getNumJointToursForPurpose(modelStructure.SHOPPING_PURPOSE_NAME);
    }

    public int getNumJointOthMaintTours()
    {
        return getNumJointToursForPurpose(modelStructure.OTH_MAINT_PURPOSE_NAME);
    }

    public int getNumJointEatOutTours()
    {
        return getNumJointToursForPurpose(modelStructure.EAT_OUT_PURPOSE_NAME);
    }

    public int getNumJointSocialTours()
    {
        return getNumJointToursForPurpose(modelStructure.SOCIAL_PURPOSE_NAME);
    }

    public int getNumJointOthDiscrTours()
    {
        return getNumJointToursForPurpose(modelStructure.OTH_DISCR_PURPOSE_NAME);
    }

    private int getNumJointToursForPurpose(String purposeName)
    {
        int count = 0;
        TourIf[] jt = hhObj.getJointTourArray();
        if (jt == null) return count;

        for (int i = 0; i < jt.length; i++)
            if (jt[i].getTourPrimaryPurpose().equalsIgnoreCase(purposeName)) count++;

        return count;
    }

    public void computeIdapResidualWindows()
    {

        // find the start of the earliest mandatory or joint tour for this person
        // and end of last one.
        int firstTourStart = 9999;
        int lastTourEnd = -1;
        int firstTourEnd = 0;
        int lastTourStart = 0;

        // first check mandatory tours
        for (TourIf tour : workTourArrayList)
        {
            int tourDeparts = tour.getTourDepartPeriod();
            int tourArrives = tour.getTourArrivePeriod();

            if (tourDeparts < firstTourStart)
            {
                firstTourStart = tourDeparts;
                firstTourEnd = tourArrives;
            }

            if (tourArrives > lastTourEnd)
            {
                lastTourStart = tourDeparts;
                lastTourEnd = tourArrives;
            }
        }

        for (TourIf tour : schoolTourArrayList)
        {
            int tourDeparts = tour.getTourDepartPeriod();
            int tourArrives = tour.getTourArrivePeriod();

            if (tourDeparts < firstTourStart)
            {
                firstTourStart = tourDeparts;
                firstTourEnd = tourArrives;
            }

            if (tourArrives > lastTourEnd)
            {
                lastTourStart = tourDeparts;
                lastTourEnd = tourArrives;
            }
        }

        // now check joint tours
        TourIf[] jointTourArray = hhObj.getJointTourArray();
        if (jointTourArray != null)
        {
            for (TourIf tour : jointTourArray)
            {

                if (tour == null) continue;

                // see if this person is in the joint tour or not
                if (tour.getPersonInJointTour(this))
                {

                    int tourDeparts = tour.getTourDepartPeriod();
                    int tourArrives = tour.getTourArrivePeriod();

                    if (tourDeparts < firstTourStart)
                    {
                        firstTourStart = tourDeparts;
                        firstTourEnd = tourArrives;
                    }

                    if (tourArrives > lastTourEnd)
                    {
                        lastTourStart = tourDeparts;
                        lastTourEnd = tourArrives;
                    }

                }

            }
        }

        if (firstTourStart > modelStructure.getNumberOfTimePeriods() - 1 && lastTourEnd < 0)
        {
            int numPeriods = windows.length;
            windowBeforeFirstMandJointTour = numPeriods;
            windowAfterLastMandJointTour = numPeriods;
            windowBetweenFirstLastMandJointTour = numPeriods;
        } else
        {

            // since first tour first period and last tour last period are available,
            // account for them.
            windowBeforeFirstMandJointTour = firstTourStart + 1;
            windowAfterLastMandJointTour = modelStructure.getNumberOfTimePeriods() - lastTourEnd;

            // find the number of unscheduled periods between end of first tour and
            // start of last tour
            windowBetweenFirstLastMandJointTour = 0;
            for (int i = firstTourEnd; i <= lastTourStart; i++)
            {
                if (isPeriodAvailable(i)) windowBetweenFirstLastMandJointTour++;
            }
        }

    }

    public int getWindowBeforeFirstMandJointTour()
    {
        return windowBeforeFirstMandJointTour;
    }

    public int getWindowBetweenFirstLastMandJointTour()
    {
        return windowBetweenFirstLastMandJointTour;
    }

    public int getWindowAfterLastMandJointTour()
    {
        return windowAfterLastMandJointTour;
    }

    // public int getNumberOfMandatoryWorkTours( String workPurposeName ){
    //
    // int numberOfTours = 0;
    // for(int i=0;i<tourArrayList.size();++i){
    // if(tourArrayList.get(i).getTourPrimaryPurposeString().equalsIgnoreCase(
    // workPurposeName ))
    // numberOfTours++;
    // }
    //
    // return(numberOfTours);
    // }
    //
    // public int getNumberOfMandatorySchoolTours( String schoolPurposeName ){
    //
    // int numberOfTours = 0;
    // for(int i=0;i<tourArrayList.size();++i){
    // if(tourArrayList.get(i).getTourPrimaryPurposeString().equalsIgnoreCase(
    // schoolPurposeName ))
    // numberOfTours++;
    // }
    //
    // return(numberOfTours);
    // }
    //
    // public int getNumberOfMandatoryWorkAndSchoolTours( String
    // workAndschoolPurposeName ){
    //
    // int numberOfTours = 0;
    // for(int i=0;i<tourArrayList.size();++i){
    // if(tourArrayList.get(i).getTourPrimaryPurposeString().equalsIgnoreCase(
    // workAndschoolPurposeName ))
    // numberOfTours++;
    // }
    //
    // return(numberOfTours);
    // }

    /**
     * determine if person is a worker (indepdent of person type).
     * 
     * @return 1 if worker, 0 otherwise.
     */
    public int getPersonIsWorker()
    {
        return persIsWorker();
    }

    /**
     * Determine if person is a student (of any age, independent of person type)
     * 
     * @return 1 if student, 0 otherwise
     */
    public int getPersonIsStudent()
    {
        return persIsStudent();
    }

    public int getPersonIsUniversityStudent()
    {
        return persIsUniversity();
    }

    public int getPersonIsTypicalUniversityStudent()
    {
        if (persIsUniversity() == 1) if (persAge < 30) return 1;
        else return 0;
        else return 0;
    }

    public int getPersonIsStudentDriving()
    {
        return persIsStudentDrivingAge();
    }

    public int getPersonIsStudentNonDriving()
    {
        return persIsStudentNonDrivingAge();
    }

    /**
     * Determine if person is a full-time worker (independent of person type)
     * 
     * @return 1 if full-time worker, 0 otherwise
     */
    public int getPersonIsFullTimeWorker()
    {
        return persIsFullTimeWorker();
    }

    /**
     * Determine if person is a part-time worker (indepdent of person type)
     */
    public int getPersonIsPartTimeWorker()
    {
        return persIsPartTimeWorker();
    }

    public int getPersonTypeIsFullTimeWorker()
    {
        return persTypeIsFullTimeWorker();
    }

    public int getPersonTypeIsPartTimeWorker()
    {
        return persTypeIsPartTimeWorker();
    }

    public int getPersonIsNonWorkingAdultUnder65()
    {
        return persIsNonWorkingAdultUnder65();
    }

    public int getPersonIsNonWorkingAdultOver65()
    {
        return persIsNonWorkingAdultOver65();
    }

    public int getPersonIsPreschoolChild()
    {
        return persIsPreschoolChild();
    }

    public int getPersonIsAdult()
    {
        if (persIsPreschoolChild() == 1 || getPersonIsStudentNonDriving() == 1) return 0;
        else return 1;
    }

    private int persIsHomemaker()
    {
        if (persAge >= MIN_ADULT_AGE
                && persEmploymentCategory == EmployStatus.NOT_EMPLOYED.ordinal()) return 1;
        else return 0;
    }

    public int notEmployed()
    {
        if (persEmploymentCategory == EmployStatus.NOT_EMPLOYED.ordinal()) return 1;
        else return 0;
    }

    private int persIsWorker()
    {
        if (persEmploymentCategory == EmployStatus.FULL_TIME.ordinal()
                || persEmploymentCategory == EmployStatus.PART_TIME.ordinal()) return 1;
        else return 0;
    }

    private int persIsStudent()
    {
        if (persStudentCategory == StudentStatus.STUDENT_HIGH_SCHOOL_OR_LESS.ordinal()
                || persStudentCategory == StudentStatus.STUDENT_COLLEGE_OR_HIGHER.ordinal())
        {
            return 1;
        } else
        {
            return 0;
        }
    }

    private int persIsFullTimeWorker()
    {
        if (persEmploymentCategory == EmployStatus.FULL_TIME.ordinal()) return 1;
        else return 0;
    }

    private int persIsPartTimeWorker()
    {
        if (persEmploymentCategory == EmployStatus.PART_TIME.ordinal()) return 1;
        else return 0;
    }

    private int persTypeIsFullTimeWorker()
    {
        if (personType == PersonType.FT_worker_age_16plus.ordinal()) return 1;
        else return 0;
    }

    private int persTypeIsPartTimeWorker()
    {
        if (personType == PersonType.PT_worker_nonstudent_age_16plus.ordinal()) return 1;
        else return 0;
    }

    private int persIsUniversity()
    {
        if (personType == PersonType.University_student.ordinal()) return 1;
        else return 0;
    }

    private int persIsStudentDrivingAge()
    {
        if (personType == PersonType.Student_age_16_19_not_FT_wrkr_or_univ_stud.ordinal()) return 1;
        else return 0;
    }

    private int persIsStudentNonDrivingAge()
    {
        if (personType == PersonType.Student_age_6_15_schpred.ordinal()) return 1;
        else return 0;
    }

    private int persIsPreschoolChild()
    {
        if (personType == PersonType.Preschool_under_age_6.ordinal()) return 1;
        else return 0;

    }

    private int persIsNonWorkingAdultUnder65()
    {
        if (personType == PersonType.Nonworker_nonstudent_age_16_64.ordinal()) return 1;
        else return 0;
    }

    private int persIsNonWorkingAdultOver65()
    {
        if (personType == PersonType.Nonworker_nonstudent_age_65plus.ordinal())
        {
            return 1;
        } else
        {
            return 0;
        }
    }

    /**
     * return maximum periods of overlap between this person and other adult persons in
     * the household.
     * 
     * @return the most number of periods mutually available between this person and
     *         other adult household members
     */
    public int getMaxAdultOverlaps()
    {
        return maxAdultOverlaps;
    }

    /**
     * set maximum periods of overlap between this person and other adult persons in
     * the household.
     * 
     * @param overlaps are the most number of periods mutually available between this
     *            person and other adult household members
     */
    public void setMaxAdultOverlaps(int overlaps)
    {
        maxAdultOverlaps = overlaps;
    }

    /**
     * return maximum periods of overlap between this person and other children in the
     * household.
     * 
     * @return the most number of periods mutually available between this person and
     *         other child household members
     */
    public int getMaxChildOverlaps()
    {
        return maxChildOverlaps;
    }

    /**
     * set maximum periods of overlap between this person and other children in the
     * household.
     * 
     * @param overlaps are the most number of periods mutually available between this
     *            person and other child household members
     */
    public void setMaxChildOverlaps(int overlaps)
    {
        maxChildOverlaps = overlaps;
    }

    /**
     * return available time window for this person in the household.
     * 
     * @return the total number of periods available for this person
     */
    public int getAvailableWindow()
    {
        int numPeriodsAvailable = 0;
        for (int i = 1; i < windows.length; i++)
            if (windows[i] != 1) numPeriodsAvailable++;

        return numPeriodsAvailable;
    }

    /**
     * determine the maximum consecutive available time window for the person
     * 
     * @return the length of the maximum available window in units of time intervals
     */
    public int getMaximumContinuousAvailableWindow()
    {
        int maxWindow = 0;
        int currentWindow = 0;
        for (int i = 1; i < windows.length; i++){
            if (windows[i] == 0){
                currentWindow++;
            }
            else{
                if (currentWindow > maxWindow)
                    maxWindow = currentWindow;
                currentWindow = 0;
            }
        }
        if (currentWindow > maxWindow)
            maxWindow = currentWindow;

        return maxWindow;
    }

    /**
     * determine the maximum consecutive pairwise available time window for this person
     * and the person for which a window was passed
     * 
     * @return the length of the maximum pairwise available window in units of time intervals
     */
    public int getMaximumContinuousPairwiseAvailableWindow( byte[] otherWindow )
    {
        int maxWindow = 0;
        int currentWindow = 0;
        for (int i = 1; i < windows.length; i++){
            if (windows[i] == 0 && otherWindow[i] == 0){
                currentWindow++;
            }
            else{
                if (currentWindow > maxWindow)
                    maxWindow = currentWindow;
                currentWindow = 0;
            }
        }
        if (currentWindow > maxWindow)
            maxWindow = currentWindow;

        return maxWindow;
    }

    public void setTimeWindows(byte[] win)
    {
        windows = win;
    }

    public void initializeForAoRestart()
    {

        cdapActivity = "-";
        imtfChoice = 0;
        inmtfChoice = 0;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;

        workTourArrayList.clear();
        schoolTourArrayList.clear();
        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        initializeWindows();

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    public void initializeForImtfRestart()
    {

        imtfChoice = 0;
        inmtfChoice = 0;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;

        workTourArrayList.clear();
        schoolTourArrayList.clear();
        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        initializeWindows();

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    /**
     * initialize the person attributes and tour objects for restarting the model at
     * joint tour frequency
     */
    public void initializeForJtfRestart()
    {

        inmtfChoice = 0;

        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            TourIf t = workTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            TourIf t = schoolTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    /**
     * initialize the person attributes and tour objects for restarting the model at
     * individual non-mandatory tour frequency.
     */
    public void initializeForInmtfRestart()
    {

        inmtfChoice = 0;

        indNonManTourArrayList.clear();
        atWorkSubtourArrayList.clear();

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            TourIf t = workTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            TourIf t = schoolTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }

        windowBeforeFirstMandJointTour = 0;
        windowBetweenFirstLastMandJointTour = 0;
        windowAfterLastMandJointTour = 0;

    }

    /**
     * initialize the person attributes and tour objects for restarting the model at
     * at-work sub-tour frequency.
     */
    public void initializeForAwfRestart()
    {

        atWorkSubtourArrayList.clear();

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            TourIf t = workTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            TourIf t = schoolTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }
        for (int i = 0; i < indNonManTourArrayList.size(); i++)
        {
            TourIf t = indNonManTourArrayList.get(i);
            scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
            t.clearStopModelResults();
        }

    }

    /**
     * initialize the person attributes and tour objects for restarting the model at
     * stop frequency.
     */
    public void initializeForStfRestart()
    {

        for (int i = 0; i < workTourArrayList.size(); i++)
        {
            TourIf t = workTourArrayList.get(i);
            t.clearStopModelResults();
        }
        for (int i = 0; i < schoolTourArrayList.size(); i++)
        {
            TourIf t = schoolTourArrayList.get(i);
            t.clearStopModelResults();
        }
        for (int i = 0; i < atWorkSubtourArrayList.size(); i++)
        {
            TourIf t = atWorkSubtourArrayList.get(i);
            t.clearStopModelResults();
        }
        for (int i = 0; i < indNonManTourArrayList.size(); i++)
        {
            TourIf t = indNonManTourArrayList.get(i);
            t.clearStopModelResults();
        }

    }

    public void logPersonObject(Logger logger, int totalChars)
    {
        
        Household.logHelper(logger, "persNum: ", persNum, totalChars);
        Household.logHelper(logger, "persId: ", persId, totalChars);
        Household.logHelper(logger, "persAge: ", persAge, totalChars);
        Household.logHelper(logger, "persGender: ", persGender, totalChars);
        Household.logHelper(logger, "persEmploymentCategory: ", persEmploymentCategory, totalChars);
        Household.logHelper(logger, "persStudentCategory: ", persStudentCategory, totalChars);
        Household.logHelper(logger, "personType: ", personType, totalChars);
        Household.logHelper(logger, "workLoc: ", workLoc, totalChars);
        Household.logHelper(logger, "schoolLoc: ", schoolLoc, totalChars);
        Household.logHelper(logger, "cdapActivity: ", cdapActivity, totalChars);
        Household.logHelper(logger, "imtfChoice: ", imtfChoice, totalChars);
        Household.logHelper(logger, "inmtfChoice: ", inmtfChoice, totalChars);
        Household.logHelper(logger, "maxAdultOverlaps: ", maxAdultOverlaps, totalChars);
        Household.logHelper(logger, "maxChildOverlaps: ", maxChildOverlaps, totalChars);
        Household.logHelper(logger, "windowBeforeFirstMandJointTour: ",
                windowBeforeFirstMandJointTour, totalChars);
        Household.logHelper(logger, "windowBetweenFirstLastMandJointTour: ",
                windowBetweenFirstLastMandJointTour, totalChars);
        Household.logHelper(logger, "windowAfterLastMandJointTour: ", windowAfterLastMandJointTour,
                totalChars);

        String header1 = "      Index:     |";
        String header2 = "     Period:     |";
        String windowString = "     Window:     |";
        String periodString = "";
        for (int i = 1; i < windows.length; i++)
        {
            header1 += String.format(" %2d |", i);
            header2 += String.format("%4s|", modelStructure.getTimePeriodLabel(i));
            switch (windows[i])
            {
                case 0:
                    periodString = "    ";
                    break;
                case 1:
                    periodString = "XXXX";
                    break;
            }
            windowString += String.format("%4s|", periodString);
        }

        logger.info(header1);
        logger.info(header2);
        logger.info(windowString);

        if (workTourArrayList.size() > 0)
        {
            for (TourIf tour : workTourArrayList)
            {
                int id = tour.getTourId();
                logger.info(tour.getTourWindow(String.format("W%d", id)));
            }
        }
        if (atWorkSubtourArrayList.size() > 0)
        {
            for (TourIf tour : atWorkSubtourArrayList)
            {
                int id = tour.getTourId();
                String alias = "";
                String purposeName = tour.getSubTourPurpose();
                if (purposeName.equalsIgnoreCase(modelStructure.AT_WORK_BUSINESS_PURPOSE_NAME)) alias = "aB";
                else if (purposeName.equalsIgnoreCase(modelStructure.AT_WORK_EAT_PURPOSE_NAME)) alias = "aE";
                else if (purposeName.equalsIgnoreCase(modelStructure.AT_WORK_MAINT_PURPOSE_NAME))
                    alias = "aM";
                logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
            }
        }
        if (schoolTourArrayList.size() > 0)
        {
            for (TourIf tour : schoolTourArrayList)
            {
                int id = tour.getTourId();
                String alias = "S";
                logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
            }
        }
        if (hhObj.getJointTourArray() != null && hhObj.getJointTourArray().length > 0)
        {
            for (TourIf tour : hhObj.getJointTourArray())
            {
                if (tour == null) continue;

                // log this persons time window if they are in the joint tour party.
                byte[] persNumArray = tour.getPersonNumArray();
                if (persNumArray != null)
                {
                    for (int num : persNumArray)
                    {
                        if (num == persNum)
                        {

                            Person person = (Person)hhObj.getPersons()[num];
                            tour.setPersonObject(person);

                            int id = tour.getTourId();
                            String alias = "";
                            if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                                    ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME)) alias = "jE";
                            else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                                    ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)) alias = "jS";
                            else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                                    ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) alias = "jM";
                            else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                                    ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)) alias = "jV";
                            else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                                    ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) alias = "jD";
                            logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
                        }
                    }
                }
            }
        }
        if (indNonManTourArrayList.size() > 0)
        {
            for (TourIf tour : indNonManTourArrayList)
            {
                int id = tour.getTourId();
                String alias = "";
                if (tour.getTourPrimaryPurpose().equalsIgnoreCase(ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME)) alias = "ie";
                else if (tour.getTourPrimaryPurpose()
                        .equalsIgnoreCase(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME)) alias = "iE";
                else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                        ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)) alias = "iS";
                else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                        ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) alias = "iM";
                else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                        ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)) alias = "iV";
                else if (tour.getTourPrimaryPurpose().equalsIgnoreCase(
                        ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) alias = "iD";
                logger.info(tour.getTourWindow(String.format("%s%d", alias, id)));
            }
        }

    }

    public void logTourObject(Logger logger, int totalChars, TourIf tour)
    {
        tour.logTourObject(logger, totalChars);
    }

    public void logEntirePersonObject(Logger logger)
    {

        int totalChars = 60;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        Household.logHelper(logger, "persNum: ", persNum, totalChars);
        Household.logHelper(logger, "persId: ", persId, totalChars);
        Household.logHelper(logger, "persAge: ", persAge, totalChars);
        Household.logHelper(logger, "persGender: ", persGender, totalChars);
        Household.logHelper(logger, "persEmploymentCategory: ", persEmploymentCategory, totalChars);
        Household.logHelper(logger, "persStudentCategory: ", persStudentCategory, totalChars);
        Household.logHelper(logger, "personType: ", personType, totalChars);
        Household.logHelper(logger, "workLoc: ", workLoc, totalChars);
        Household.logHelper(logger, "schoolLoc: ", schoolLoc, totalChars);
        Household.logHelper(logger, "cdapActivity: ", cdapActivity, totalChars);
        Household.logHelper(logger, "imtfChoice: ", imtfChoice, totalChars);
        Household.logHelper(logger, "inmtfChoice: ", inmtfChoice, totalChars);
        Household.logHelper(logger, "maxAdultOverlaps: ", maxAdultOverlaps, totalChars);
        Household.logHelper(logger, "maxChildOverlaps: ", maxChildOverlaps, totalChars);
        Household.logHelper(logger, "windowBeforeFirstMandJointTour: ",
                windowBeforeFirstMandJointTour, totalChars);
        Household.logHelper(logger, "windowBetweenFirstLastMandJointTour: ",
                windowBetweenFirstLastMandJointTour, totalChars);
        Household.logHelper(logger, "windowAfterLastMandJointTour: ", windowAfterLastMandJointTour,
                totalChars);

        String header = "     Period:     |";
        String windowString = "     Window:     |";
        for (int i = 1; i < windows.length; i++)
        {
            header += String.format("%4s|", modelStructure.getTimePeriodLabel(i));
            windowString += String.format("%4s|", windows[i] == 0 ? "    " : "XXXX");
        }

        logger.info(header);
        logger.info(windowString);

        if (workTourArrayList.size() > 0)
        {
            for (TourIf tour : workTourArrayList)
            {
                int id = tour.getTourId();
                logger.info(tour.getTourWindow(String.format("W%d", id)));
            }
        }
        if (schoolTourArrayList.size() > 0)
        {
            for (TourIf tour : schoolTourArrayList)
            {
                logger.info(tour
                        .getTourWindow(tour.getTourPrimaryPurpose().equalsIgnoreCase("university") ? "U"
                                : "S"));
            }
        }
        if (indNonManTourArrayList.size() > 0)
        {
            for (TourIf tour : indNonManTourArrayList)
            {
                logger.info(tour.getTourWindow("N"));
            }
        }
        if (atWorkSubtourArrayList.size() > 0)
        {
            for (TourIf tour : atWorkSubtourArrayList)
            {
                logger.info(tour.getTourWindow("A"));
            }
        }
        if (hhObj.getJointTourArray() != null && hhObj.getJointTourArray().length > 0)
        {
            for (TourIf tour : hhObj.getJointTourArray())
            {
                if (tour != null) logger.info(tour.getTourWindow("J"));
            }
        }

        logger.info(separater);

        logger.info("Work Tours:");
        if (workTourArrayList.size() > 0)
        {
            for (TourIf tour : workTourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No work tours");
        }

        logger.info("School Tours:");
        if (schoolTourArrayList.size() > 0)
        {
            for (TourIf tour : schoolTourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No school tours");
        }

        logger.info("Individual Non-mandatory Tours:");
        if (indNonManTourArrayList.size() > 0)
        {
            for (TourIf tour : indNonManTourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No individual non-mandatory tours");
        }

        logger.info("Work based subtours Tours:");
        if (atWorkSubtourArrayList.size() > 0)
        {
            for (TourIf tour : atWorkSubtourArrayList)
            {
                tour.logEntireTourObject(logger);
            }
        } else
        {
            logger.info("     No work based subtours");
        }

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public enum EmployStatus
    {
        nul, FULL_TIME, PART_TIME, NOT_EMPLOYED, UNDER16
    }

    public enum StudentStatus
    {
        nul, STUDENT_HIGH_SCHOOL_OR_LESS, STUDENT_COLLEGE_OR_HIGHER, NON_STUDENT
    }

    public enum PersonType
    {
        nul, FT_worker_age_16plus, PT_worker_nonstudent_age_16plus, University_student, Nonworker_nonstudent_age_16_64, Nonworker_nonstudent_age_65plus, Student_age_16_19_not_FT_wrkr_or_univ_stud, Student_age_6_15_schpred, Preschool_under_age_6
    }




    public void setFreeParkingAvailableResult( int fpResult ) {
        freeParkingAvailable = (byte)fpResult;
    }
    
    public int getFreeParkingAvailableResult() {
        return freeParkingAvailable;
    }
    
    
    public void setWorkLocSubzone( int subzone ) {
        workLocSubzone = subzone;
    }
    
    public void setSchoolLocSubzone( int subzone ) {
        schoolLocSubzone = subzone;
    }

    public void setWorkLocationPurposeIndex( int workPurpose ) {
        workLocationPurposeIndex = workPurpose;
    }

    public void setUniversityLocationPurposeIndex( int universityPurpose ) {
        universityLocationPurposeIndex = universityPurpose;
    }

    public void setSchoolLocationPurposeIndex( int schoolPurpose ) {
        schoolLocationPurposeIndex = schoolPurpose;
    }

    public int getPersonSchoolLocationSubZone() {
        return schoolLocSubzone;
    }

    public int getPersonWorkLocationSubZone() {
        return workLocSubzone;
    }

    public int getSchoolLocationPurposeIndex() {
        return schoolLocationPurposeIndex;
    }

    public int getUniversityLocationPurposeIndex() {
        return universityLocationPurposeIndex;
    }

    public int getWorkLocationPurposeIndex() {
        return workLocationPurposeIndex;
    }

    public int getNumUniversityTours() {
        int num = 0;
        for ( TourIf tour : getListOfSchoolTours() )
            if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_NAME ) )
                num++;
        return num;
    }

    public void createIndividualNonMandatoryTours( int numberOfTours, String purposeName, ModelStructure modelStructure ){

        // if purpose is escort, need to determine if household has kids or not
        if ( purposeName.equalsIgnoreCase( ModelStructure.ESCORT_PURPOSE_NAME ) & modelStructure.getNumEscortSegments() > 0 ) {
            if ( hhObj.getNumChildrenUnder19() > 0 )
                purposeName += "_" + modelStructure.ESCORT_SEGMENT_NAMES[0];
            else
                purposeName += "_" + modelStructure.ESCORT_SEGMENT_NAMES[1];
        }
        
        int primaryPurposeIndex = modelStructure.getPrimaryPurposeIndex( modelStructure.getPrimaryPurposeString(purposeName) );
          
        int startId = indNonManTourArrayList.size();
        
        for(int i=0;i<numberOfTours;i++){
            
            int id = startId + i;
            TourIf tempTour = new Tour( id, this.hhObj, this, purposeName, ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY, primaryPurposeIndex );

            tempTour.setTourOrigTaz(this.hhObj.getHhTaz());
            tempTour.setTourOrigWalkSubzone(this.hhObj.getHhWalkSubzone());

            tempTour.setTourDestTaz(-1);
            tempTour.setTourDestWalkSubzone(-1);

            indNonManTourArrayList.add(tempTour);
            
            modelStructure.setSegmentedIndexPurpose( modelStructure.getDcModelPurposeIndex(purposeName), purposeName );
            modelStructure.setSegmentedPurposeIndex( purposeName, modelStructure.getDcModelPurposeIndex(purposeName) );

            
        }

    }
    
    public void createAtWorkSubtour(int id, int choice, int workTaz, int workSubZone, String subtourPurpose, ModelStructure modelStructure)
    {

        String segmentedPurpose = ModelStructure.AT_WORK_PURPOSE_NAME + "_" + subtourPurpose;
        int primaryPurposeIndex = modelStructure.getPrimaryPurposeIndex( modelStructure.getPrimaryPurposeString(segmentedPurpose) );
        
        TourIf tempTour = new Tour(id, this.hhObj, this, segmentedPurpose, ModelStructure.AT_WORK_CATEGORY, primaryPurposeIndex);

        tempTour.setTourOrigTaz(workTaz);
        tempTour.setTourOrigWalkSubzone(workSubZone);
        tempTour.setTourDestTaz(0);
        tempTour.setTourDestWalkSubzone(0);

        tempTour.setTourPurpose(segmentedPurpose);
        tempTour.setSubTourPurpose(subtourPurpose);

        tempTour.setTourDepartPeriod(DEFAULT_AT_WORK_SUBTOUR_START_PERIOD);
        tempTour.setTourArrivePeriod(DEFAULT_AT_WORK_SUBTOUR_END_PERIOD);
        
        modelStructure.setSegmentedIndexPurpose( modelStructure.getDcModelPurposeIndex(segmentedPurpose), segmentedPurpose );
        modelStructure.setSegmentedPurposeIndex( segmentedPurpose, modelStructure.getDcModelPurposeIndex(segmentedPurpose) );

        atWorkSubtourArrayList.add(tempTour);

    }

    public void createSchoolTours( int numberOfTours, int startId, String tourPurpose, ModelStructure modelStructure ){
        
        schoolTourArrayList.clear();
        
        int primaryPurposeIndex = tourPurpose == ModelStructure.UNIVERSITY_PURPOSE_NAME ? ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX : ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX;
        
        for(int i=0;i<numberOfTours;i++){
            int id = startId + i;
            TourIf tempTour = new Tour(this, id, primaryPurposeIndex);

            tempTour.setTourOrigTaz(this.hhObj.getHhTaz());
            tempTour.setTourOrigWalkSubzone(this.hhObj.getHhWalkSubzone());
            
            tempTour.setTourDestTaz(schoolLoc);
            tempTour.setTourDestWalkSubzone(schoolLocSubzone);

            tempTour.setTourPurpose( tourPurpose );

            modelStructure.setSegmentedIndexPurpose( modelStructure.getDcModelPurposeIndex(tourPurpose), tourPurpose );
            modelStructure.setSegmentedPurposeIndex( tourPurpose, modelStructure.getDcModelPurposeIndex(tourPurpose) );

            schoolTourArrayList.add(tempTour);
        }
    }

    public void createWorkTours( int numberOfTours, int startId, String tourPurpose, ModelStructure modelStructure ){

        workTourArrayList.clear();
        
        for(int i=0;i<numberOfTours;i++){
            int id = startId + i;
            TourIf tempTour = new Tour(this, id, ModelStructure.WORK_PRIMARY_PURPOSE_INDEX);

            tempTour.setTourOrigTaz(this.hhObj.getHhTaz());
            tempTour.setTourOrigWalkSubzone(this.hhObj.getHhWalkSubzone());

            tempTour.setTourDestTaz(workLoc);
            tempTour.setTourDestWalkSubzone(workLocSubzone);

            tempTour.setTourPurpose( tourPurpose );

            modelStructure.setSegmentedIndexPurpose( modelStructure.getDcModelPurposeIndex(tourPurpose), tourPurpose );
            modelStructure.setSegmentedPurposeIndex( tourPurpose, modelStructure.getDcModelPurposeIndex(tourPurpose) );

            workTourArrayList.add(tempTour);
        }

    }

    public boolean isPeriodAvailable( int index ){

        if ( windows[index] == 1 )
            return false;
        else
            return true;
    }
    
    public int getPersonEducAttainment() {
    	return persEduc;
    }
    
    public void setPersonEducAttainment(int educAttainCode) {
    	persEduc = educAttainCode;
    }
    
    public int getPersonIndustryCensus() {
    	return persIndCen;
    }
    
    public void setPersonIndustryCensus(int indCensusCode) {
    	persIndCen = indCensusCode;
    }
    
    public int getPersonWorkerOccupation() {
    	return persWorkerOccupation;
    }
    
    public void setPersonWorkerOccupation(int workerOccupation) {
    	persWorkerOccupation = workerOccupation;
    }
    
    public float getWalkTimeWeight() {
    	return walkTimeWeight;
    }
    
    public void setWalkTimeWeight(float walkTimeWeight){
    	this.walkTimeWeight =walkTimeWeight;
    }
    
    public float getWalkSpeed() {
    	return walkSpeed;
    }
    
    public int getUserClass(String type) {
    	return userClass.get(type);
    }
    
    public void setWalkSpeed(float walkSpeed) {
    	this.walkSpeed = walkSpeed;
    }
    
    public float getMaxWalk() {
    	return maxWalk;
    }
    
    public void setMaxWalk(float maxWalk) {
    	this.maxWalk = maxWalk;
    }
    
    public void setUserClass(String type, int userClass) {
    	this.userClass.put(type, userClass);
    }

}
