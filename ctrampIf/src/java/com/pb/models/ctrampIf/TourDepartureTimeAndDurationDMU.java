package com.pb.models.ctrampIf;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.jppf.HouseholdIndividualMandatoryTourFrequencyModel;


public class TourDepartureTimeAndDurationDMU implements Serializable, VariableTable {
	
	protected transient Logger logger = Logger.getLogger(TourDepartureTimeAndDurationDMU.class);

	private static int JOINT_TOUR_COMPOSITION_CHILDREN_ONLY = 2;
	
    protected HashMap<String, Integer> methodIndexMap;

    
    protected IndexValues dmuIndex;
    
    protected PersonIf person;
    protected HouseholdIf household;
    protected TourIf tour;

    protected double[] modeChoiceLogsums;

    private int[] altStarts;
    private int[] altEnds;
    
    protected int originAreaType, destinationAreaType;

    protected int tourNumber;
    
    protected int firstTour;
    protected int subsequentTour;
    protected int endOfPreviousScheduledTour;
    protected int subsequentTourIsWork;
    protected int subsequentTourIsSchool;
    
    
    protected ModelStructure modelStructure;
	
	public TourDepartureTimeAndDurationDMU( ModelStructure modelStructure ){
	    this.modelStructure = modelStructure;
		dmuIndex = new IndexValues();
	}
	
	public void setPerson(PersonIf passedInPerson){
		person = passedInPerson;
	}
	
    public void setHousehold(HouseholdIf passedInHousehold){
    	household = passedInHousehold;

    	// set the origin and zone indices
        dmuIndex.setZoneIndex(household.getHhTaz());
        dmuIndex.setHHIndex(household.getHhId());

        // set the debug flag that can be used in the UEC
        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel ( "" );
        if ( household.getDebugChoiceModels() ) {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel ( "Debug DepartTime UEC" );
        }

    }

	public void setTour(TourIf passedInTour){
		tour = passedInTour;
	}
	
	public void setOriginZone(int zone){
		dmuIndex.setOriginZone(zone);
	}
	
	public void setDestinationZone(int zone){
		dmuIndex.setDestZone(zone);
	}
	
	public void setOriginAreaType(int areaType){
		originAreaType = areaType;
	}
	
	public void setDestinationAreaType(int areaType){
		destinationAreaType = areaType;
	}
	
    public void setFirstTour(int trueOrFalse)
    {
        firstTour = trueOrFalse;
    }

    public void setSubsequentTour(int trueOrFalse)
    {
        subsequentTour = trueOrFalse;
    }

    public void setSubsequentTourIsWork(int trueOrFalse)
    {
        subsequentTourIsWork = trueOrFalse;
    }

    public void setSubsequentTourIsSchool(int trueOrFalse)
    {
        subsequentTourIsSchool = trueOrFalse;
    }

    /**
     * Set the sequence number of this tour among all scheduled 
     * @param tourNum
     */
    public void setTourNumber( int tourNum ){
        tourNumber = tourNum;
    }

    public void setEndOfPreviousScheduledTour ( int endHr ){
        endOfPreviousScheduledTour = endHr;    
    }
    
    public void setModeChoiceLogsums( double[] logsums ) {
        modeChoiceLogsums = logsums;
    }

    public void setTodAlts( int[] altStarts, int[] altEnds ){
        this.altStarts = altStarts;
        this.altEnds = altEnds;
    }
    
    public IndexValues getIndexValues(){
		return(dmuIndex);
	}

    public HouseholdIf getDmuHouseholdObject() {
        return household;
    }

    public int getOriginZone(){
		return(dmuIndex.getOriginZone());
	}
	
	public int getDestinationZone(){
		return(dmuIndex.getDestZone());
	}
	                                                         	
    public int getOriginAreaType(){
    	return(originAreaType);
    }
    
    public int getDestinationAreaType(){
    	return(destinationAreaType);
    }
    
    public int getPersonIsAdult() {
        return person.getPersonIsAdult();
    }
    
    public int getPreDrivingAgeChild()
    {
        return (person.getPersonIsStudentNonDriving() == 1 || person.getPersonIsPreschoolChild() == 1 ) ? 1 : 0;
    }
    
    public int getPersonType()
    {
        return person.getPersonTypeNumber();
    }
    
    // return 1 if the person is preschool
    public int getPreschool()
    {
        return person.getPersonIsPreschoolChild() == 1 ? 1 : 0;

    }

    public int getPersonAge()
    {
        return person.getAge();
    }

    public int getPersonIsFemale()
    {
        return person.getGender() == 2 ? 1 : 0;
    }

    public int getHouseholdSize()
    {
        return household.getHhSize();
    }

    public int getNumPreschoolChildrenInHh()
    {
        return household.getNumPreschool();
    }

    public int getNumNonWorkingAdultsInHh()
    {
        return household.getNumberOfNonWorkingAdults();
    }

    public int getNumChildrenUnder16InHh()
    {
        return household.getNumChildrenUnder16();
    }

    public int getNumIndivShopTours()
    {
        int count = 0;
        for ( TourIf t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPrimaryPurpose().equalsIgnoreCase(ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    public int getNumIndivMaintTours()
    {
        int count = 0;
        for ( TourIf t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPrimaryPurpose().equalsIgnoreCase(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    public int getNumIndivVisitTours()
    {
        int count = 0;
        for ( TourIf t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPrimaryPurpose().equalsIgnoreCase(ModelStructure.VISITING_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    public int getNumIndivDiscrTours()
    {
        int count = 0;
        for ( TourIf t : person.getListOfIndividualNonMandatoryTours() )
            if (t.getTourPrimaryPurpose().equalsIgnoreCase(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME)) count++;
        
        return count;
    }

    // for joint tours - if an adult is in the party, return 1.
    public int getAdultInTour() {
        return tour.getJointTourComposition() != JOINT_TOUR_COMPOSITION_CHILDREN_ONLY ? 1 : 0;
    }
    
    public int getNumChildrenInHh() {
        return household.getNumChildrenUnder19();
    }
    
    public int getFullTimeWorker(){
    	return(this.person.getPersonTypeIsFullTimeWorker());
    }
    
    public int getPartTimeWorker(){
    	return(this.person.getPersonTypeIsPartTimeWorker());
    }
    
    public int getUniversityStudent(){
    	return(this.person.getPersonIsUniversityStudent());
    }

    public int getStudentDrivingAge() {
        return(this.person.getPersonIsStudentDriving());
    }

    public int getStudentNonDrivingAge() {
        return(this.person.getPersonIsStudentNonDriving());
    }

    public int getNonWorker(){
    	return(this.person.getPersonIsNonWorkingAdultUnder65());
    }
    
    public int getRetired(){
    	return(this.person.getPersonIsNonWorkingAdultOver65());
    }

    public int getAllAdultsFullTimeWorkers() {
        PersonIf[] p = household.getPersons();
        boolean allAdultsAreFullTimeWorkers = true;
        for (int i=1; i < p.length; i++) {
            if ( p[i].getPersonIsAdult() == 1 && p[i].getPersonIsFullTimeWorker() == 0 ) {
                allAdultsAreFullTimeWorkers = false;
                break;
            }
        }

        if ( allAdultsAreFullTimeWorkers )
            return 1;
        else
            return 0;
    }

    public int getSubtourPurposeIsEatOut()
    {
        if (tour.getSubTourPurpose().equalsIgnoreCase(ModelStructure.AT_WORK_EAT_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getSubtourPurposeIsBusiness()
    {
        if (tour.getSubTourPurpose().equalsIgnoreCase(ModelStructure.AT_WORK_BUSINESS_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getSubtourPurposeIsOther()
    {
        if (tour.getSubTourPurpose().equalsIgnoreCase(ModelStructure.AT_WORK_MAINT_PURPOSE_NAME)) return 1;
        else return 0;
    }

    public int getTourPrimaryPurposeIsShopping() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.SHOPPING_PRIMARY_PURPOSE_NAME ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeIsEatOut() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeIsMaint() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_NAME ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeIsVisit() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.VISITING_PRIMARY_PURPOSE_NAME ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeIsDiscr() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME ) )
            return 1;
        else
            return 0;
    }

    public int getSubtourPurposeIndex() {
        if ( tour.getTourCategoryIsAtWork() ) {
            return tour.getTourPrimaryPurposeIndex();
        }
        else {
            return 0;
        }
    }

    public int getAdultsInTour() {

        int count = 0;
        if ( tour.getTourCategoryIsJointNonMandatory() ) {
            PersonIf[] persons = household.getPersons();

            byte[] personNums = tour.getPersonNumArray();
            for (int i=0; i < personNums.length; i++) {
                int p = personNums[i];
                if ( persons[p].getPersonIsAdult() == 1 )
                    count++;
            }
        }
        else if ( tour.getTourCategoryIsIndivNonMandatory() ) {
            if ( person.getPersonIsAdult() == 1 )
                count = 1;
        }

        return count;
    }

    public int getChildrenInTour() {

        int count = 0;
        if ( tour.getTourCategoryIsJointNonMandatory() ) {
            PersonIf[] persons = household.getPersons();

            byte[] personNums = tour.getPersonNumArray();
            for (int i=0; i < personNums.length; i++) {
                int p = personNums[i];
                if ( persons[p].getPersonIsAdult() == 0 )
                    count++;
            }
        }
        else if ( tour.getTourCategoryIsIndivNonMandatory() ) {
            if ( person.getPersonIsAdult() == 0 )
                count = 1;
        }

        return count;

    }

    // return 1 if at least one preschool or pre-driving child is in joint tour, otherwise 0.
    public int getPreschoolPredrivingInTour() {

        int count = 0;
        if ( tour.getTourCategoryIsJointNonMandatory() ) {
            PersonIf[] persons = household.getPersons();
            byte[] personNums = tour.getPersonNumArray();
            for (int i=0; i < personNums.length; i++) {
                int p = personNums[i];
                if ( persons[p].getPersonIsPreschoolChild() == 1 || persons[p].getPersonIsStudentNonDriving() == 1 )
                    return 1;
            }
        }
        else if ( tour.getTourCategoryIsIndivNonMandatory() ) {
            if ( person.getPersonIsPreschoolChild() == 1 || person.getPersonIsStudentNonDriving() == 1 )
                count = 1;
        }

        return count;

    }

    // return 1 if at least one university student is in joint tour, otherwise 0.
    public int getUnivInTour() {

        int count = 0;
        if ( tour.getTourCategoryIsJointNonMandatory() ) {
            PersonIf[] persons = household.getPersons();
            byte[] personNums = tour.getPersonNumArray();
            for (int i=0; i < personNums.length; i++) {
                int p = personNums[i];
                if ( persons[p].getPersonIsUniversityStudent() == 1 )
                    return 1;
            }
        }
        else if ( tour.getTourCategoryIsIndivNonMandatory() ) {
            if ( person.getPersonIsUniversityStudent() == 1 )
                count = 1;
        }

        return count;

    }

    // return 1 if all adults in joint tour are fulltime workers, 0 otherwise;
    public int getAllWorkFull() {

        if ( tour.getTourCategoryIsJointNonMandatory() ) {
            int adultCount = 0;
            int ftWorkerAdultCount = 0;

            PersonIf[] persons = household.getPersons();
            byte[] personNums = tour.getPersonNumArray();
            for (int i=0; i < personNums.length; i++) {
                int p = personNums[i];
                if ( persons[p].getPersonIsAdult() == 1 ) {
                    adultCount++;
                    if ( persons[p].getPersonIsFullTimeWorker() == 1 )
                        ftWorkerAdultCount++;
                }
            }

            if ( adultCount > 0 && adultCount == ftWorkerAdultCount )
                return 1;
            else
                return 0;
        }

        return 0;

    }

    public int getPartyComp() {
        if ( tour.getTourCategoryIsJointNonMandatory() ) {
            return tour.getJointTourComposition();
        }
        else {
            return 0;
        }
    }

    /**
     * @return number of individual non-mandatory tours, excluding escort, for the
     *         person
     */
    public int getPersonNonMandatoryTotalNoEscort()
    {
        int count = 0;
        for (TourIf t : person.getListOfIndividualNonMandatoryTours())
            if (!t.getTourPrimaryPurpose().startsWith("escort")) count++;
        return count;
    }

    /**
     * @return number of individual non-mandatory discretionary tours for the person
     */
    public int getPersonDiscrToursTotal()
    {
        int count = 0;
        for (TourIf t : person.getListOfIndividualNonMandatoryTours())
        {
            if (t.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME) ||
                t.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.VISITING_PRIMARY_PURPOSE_NAME) ||
                t.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME) )
                    count++;
        }
        return count;
    }

    /**
     * @return number of individual non-mandatory tours, excluding escort, for the person
     */
    public int getPersonEscortTotal()
    {
        int count = 0;
        for (TourIf t : person.getListOfIndividualNonMandatoryTours())
            if (t.getTourPrimaryPurpose().startsWith("escort")) count++;
        return count;
    }

    public int getJointTourPartySize()
    {
        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
            count = tour.getPersonNumArray().length;
        
        return count;
    }
    
    public int getKidsOnJointTour()
    {

        int count = 0;
        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY))
        {
            PersonIf[] persons = household.getPersons();

            byte[] personNums = tour.getPersonNumArray();
            for (int i = 0; i < personNums.length; i++)
            {
                int p = personNums[i];
                if ((persons[p].getPersonIsPreschoolChild() + persons[p].getPersonIsStudentNonDriving() + persons[p].getPersonIsStudentDriving()) > 0 ) count++;
            }
        }

        return count > 0 ? 1 : 0;

    }

    public int getHhJointTotal()
    {
        TourIf[] jt = household.getJointTourArray();
        if (jt == null) return 0;
        else return jt.length;
    }

    public int getPersonMandatoryTotal()
    {
        return person.getListOfWorkTours().size() + person.getListOfSchoolTours().size();
    }

    public int getPersonJointTotal()
    {
        TourIf[] jtArray = household.getJointTourArray();
        if (jtArray == null)
        {
            return 0;
        } else
        {
            int numJtParticipations = 0;
            for (TourIf jt : jtArray)
            {
                byte[] personJtIndices = jt.getPersonNumArray();
                for (int pNum : personJtIndices)
                {
                    if (pNum == person.getPersonNum())
                    {
                        numJtParticipations++;
                        break;
                    }
                }
            }
            return numJtParticipations;
        }
    }

    public int getPersonJointAndIndivDiscrToursTotal()
    {

        int totDiscr = getPersonDiscrToursTotal(); 
        
        TourIf[] jtArray = household.getJointTourArray();
        if (jtArray == null)
        {
            return totDiscr;
        } else
        {
            // count number of joint discretionary tours person participates in
            int numJtParticipations = 0;
            for (TourIf jt : jtArray)
            {
                if (jt.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME) ||
                        jt.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.VISITING_PRIMARY_PURPOSE_NAME) ||
                        jt.getTourPrimaryPurpose().equalsIgnoreCase( ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_NAME) )
                {
                    byte[] personJtIndices = jt.getPersonNumArray();
                    for (int pNum : personJtIndices)
                    {
                        if (pNum == person.getPersonNum())
                        {
                            numJtParticipations++;
                            break;
                        }
                    }
                }
            }
            return numJtParticipations + totDiscr;
        }
    }

    public int getFirstTour(){
    	return firstTour;
    }
    
    public int getSubsequentTour(){
        return subsequentTour;
    }
    
    public int getNumberOfOutboundStops(){
    	return(tour.getNumOutboundStops());
    }
    
    public int getNumberOfInboundStops(){
    	return(tour.getNumInboundStops());
    }
    
    public int getWorkAndSchoolToursByWorker(){
        int returnValue = 0;
        if( person.getPersonIsWorker() == 1 ){
            if ( person.getImtfChoice() == HouseholdIndividualMandatoryTourFrequencyModel.CHOICE_WORK_AND_SCHOOL )
                returnValue = 1;
        }
    	return returnValue;
    }
    
    public int getWorkAndSchoolToursByStudent(){
        int returnValue = 0;
        if( person.getPersonIsStudent() == 1 ){
            if ( person.getImtfChoice() == HouseholdIndividualMandatoryTourFrequencyModel.CHOICE_WORK_AND_SCHOOL )
                returnValue = 1;
        }
    	return returnValue;
    }

    public double getModeChoiceLogsumAlt (int alt) {

        int startPeriod = altStarts[alt-1];
        int endPeriod = altEnds[alt-1];

        int index = modelStructure.getPeriodCombinationIndex(startPeriod, endPeriod);
        
        return modeChoiceLogsums[index];
        
    }
    
    /**
     * get the number of tours left to be scheduled, including the current tour
     * @return number of tours left to be scheduled, including the current tour
     */
    public int getToursLeftToSchedule()
    {
        if ( tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ){
            TourIf[] jt = household.getJointTourArray();
            return jt.length - tourNumber + 1;
        }
        else
            return person.getListOfIndividualNonMandatoryTours().size() - tourNumber + 1;
    }
    
    

    public int getPrevTourEndsThisDeparturePeriodAlt (int alt) {

        // get the departure period for the current alternative
        int thisTourStartsPeriod = altStarts[alt-1];

        if ( person.isPreviousArrival( thisTourStartsPeriod ) )
            return 1;
        else
            return 0;

    }


    public int getPrevTourBeginsThisArrivalPeriodAlt (int alt) {

        // get the arrival period for the current alternative
        int thisTourEndsPeriod = altStarts[alt-1];

        if ( person.isPreviousDeparture( thisTourEndsPeriod ) )
            return 1;
        else
            return 0;

    }


    public int getAdjWindowBeforeThisPeriodAlt(int alt){

        int thisTourStartsPeriod = altStarts[alt-1];

        int numAdjacentPeriodsAvailable = 0;
        for (int i=thisTourStartsPeriod-1; i >= 0; i--) {
            if ( person.isPeriodAvailable(i) )
                numAdjacentPeriodsAvailable++;
            else
                break;
        }

        return numAdjacentPeriodsAvailable;

    }

    public int getAdjWindowAfterThisPeriodAlt(int alt){

        int thisTourEndsPeriod = altEnds[alt-1];

        int numAdjacentPeriodsAvailable = 0;
        for (int i=thisTourEndsPeriod+1; i < modelStructure.getNumberOfTimePeriods(); i++) {
            if ( person.isPeriodAvailable(i) )
                numAdjacentPeriodsAvailable++;
            else
                break;
        }

        return numAdjacentPeriodsAvailable;

    }


    public int getRemainingPeriodsAvailableAlt( int alt ){

        int periodsAvail = person.getAvailableWindow();
        
        int start = altStarts[alt-1];
        int end = altEnds[alt-1];
            
        // determine the availabilty of each period after the alternative time window is hypothetically scheduled
        // if start == end, the availability won't change, so no need to compute.
        if ( start != end ) {

            // the start and end periods will always be available after scheduling, so don't need to check them.
            // the periods between start/end must be 0 or the alternative could not have been available,
            // so count them all as unavailable after scheduling this window.
            periodsAvail -= ( end - start - 1 );

        }
                
        return periodsAvail;
        
    }


    public float getRemainingInmToursToAvailablePeriodsRatioAlt( int alt ){
        int periodsAvail = getRemainingPeriodsAvailableAlt( alt );
        if ( periodsAvail > 0 ) {
            float ratio = (float)( person.getListOfIndividualNonMandatoryTours().size() - tourNumber ) / periodsAvail;
            return ratio;
        }
        else
            return -999;
    }


    public int getMaximumAvailableTimeWindow()
    {
        return person.getMaximumContinuousAvailableWindow();
    }
    
    public int getMaxJointTimeWindow( TourIf t)
    {
        return household.getMaxJointTimeWindow(t);
    }

    public int getEndOfPreviousTour() {
        return endOfPreviousScheduledTour;
    }


    public int getTourCategoryIsJoint() {
        return tour.getTourCategoryIsJointNonMandatory() ? 1 : 0;
    }






    public int getIndexValue(String variableName) {
        return methodIndexMap.get(variableName);
    }



    public int getAssignmentIndexValue(String variableName) {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex) {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex) {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue) {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue) {
        throw new UnsupportedOperationException();
    }
    
}
