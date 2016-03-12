package com.pb.models.ctrampIf;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class TourModeChoiceDMU implements Serializable, VariableTable {

    protected transient Logger logger = Logger.getLogger(TourModeChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;
    
    protected TourIf tour;
    protected TourIf workTour;
    protected PersonIf person;
    protected HouseholdIf hh;
    protected IndexValues dmuIndex;
    
    private ModelStructure modelStructure;
    
	float genCostWT_In;
	float genCostKNR_In;
	float genCostPNR_In;
	int OtapWT_In;
	int DtapWT_In;
	int OtapKNR_In;
	int DtapKNR_In;
	int OtapPNR_In;
	int DtapPNR_In;
	
	float genCostWT_Out;
	float genCostKNR_Out;
	float genCostPNR_Out;
	int OtapWT_Out;
	int DtapWT_Out;
	int OtapKNR_Out;
	int DtapKNR_Out;
	int OtapPNR_Out;
	int DtapPNR_Out;
	
    public TourModeChoiceDMU( ModelStructure modelStructure ){
        this.modelStructure = modelStructure;
        dmuIndex = new IndexValues();
    }
    


    public void setHouseholdObject ( HouseholdIf hhObject ) {
        hh = hhObject;
    }
    
    public HouseholdIf getHouseholdObject() {
        return hh;
    }
    
    public void setPersonObject ( PersonIf personObject ) {
        person = personObject;
    }
    
    public PersonIf getPersonObject () {
        return person;
    }
    
    public void setTourObject ( TourIf tourObject ) {
        tour = tourObject;
    }
    
    public void setWorkTourObject ( TourIf tourObject ) {
        workTour = tourObject;
    }
    
    public TourIf getTourObject () {
        return tour;
    }
    
    public TourIf getWorkTourObject () {
        return workTour;
    }
    
    public float getGenCostWT_In() {
    	return genCostWT_In;
    }
    
    public float getGenCostKNR_In() {
    	return genCostKNR_In;
    }
    
    public float getGenCostPNR_In() {
    	return genCostPNR_In;
    }
    
    public int getOtapWT_In() {
    	return OtapWT_In;
    }
    
    public int getDtapWT_In() {
    	return DtapWT_In;
    }
    
    public int getOtapKNR_In() {
    	return OtapKNR_In;
    }
    
    public int getDtapKNR_In() {
    	return DtapKNR_In;
    }
    
    public int getOtapPNR_In() {
    	return OtapPNR_In;
    }
    
    public int getDtapPNR_In() {
    	return DtapPNR_In;
    }

    public void setGenCostWT_In(float genCost) {
    	this.genCostWT_In = genCost;
    }
    
    public void setGenCostKNR_In(float genCost) {
    	this.genCostKNR_In = genCost;
    }
    
    public void setGenCostPNR_In(float genCost) {
    	this.genCostPNR_In = genCost;
    }
    
    public void setOtapWT_In(int Otap) {
    	this.OtapWT_In = Otap;
    }
    
    public void setDtapWT_In(int Dtap) {
    	this.DtapWT_In = Dtap;
    }
    
    public void setOtapKNR_In(int Otap) {
    	this.OtapKNR_In = Otap;
    }
    
    public void setDtapKNR_In(int Dtap) {
    	this.DtapKNR_In = Dtap;
    }
    
    public void setOtapPNR_In(int Otap) {
    	this.OtapPNR_In = Otap;
    }
    
    public void setDtapPNR_In(int Dtap) {
    	this.DtapPNR_In = Dtap;
    }
    
    public float getGenCostWT_Out() {
    	return genCostWT_Out;
    }
    
    public float getGenCostKNR_Out() {
    	return genCostKNR_Out;
    }
    
    public float getGenCostPNR_Out() {
    	return genCostPNR_Out;
    }
    
    public int getOtapWT_Out() {
    	return OtapWT_Out;
    }
    
    public int getDtapWT_Out() {
    	return DtapWT_Out;
    }
    
    public int getOtapKNR_Out() {
    	return OtapKNR_Out;
    }
    
    public int getDtapKNR_Out() {
    	return DtapKNR_Out;
    }
    
    public int getOtapPNR_Out() {
    	return OtapPNR_Out;
    }
    
    public int getDtapPNR_Out() {
    	return DtapPNR_Out;
    }

    public void setGenCostWT_Out(float genCost) {
    	this.genCostWT_Out = genCost;
    }
    
    public void setGenCostKNR_Out(float genCost) {
    	this.genCostKNR_Out = genCost;
    }
    
    public void setGenCostPNR_Out(float genCost) {
    	this.genCostPNR_Out = genCost;
    }
    
    public void setOtapWT_Out(int Otap) {
    	this.OtapWT_Out = Otap;
    }
    
    public void setDtapWT_Out(int Dtap) {
    	this.DtapWT_Out = Dtap;
    }
    
    public void setOtapKNR_Out(int Otap) {
    	this.OtapKNR_Out = Otap;
    }
    
    public void setDtapKNR_Out(int Dtap) {
    	this.DtapKNR_Out = Dtap;
    }   
    
    public void setOtapPNR_Out(int Otap) {
    	this.OtapPNR_Out = Otap;
    }
    
    public void setDtapPNR_Out(int Dtap) {
    	this.DtapPNR_Out = Dtap;
    }   
    
    /**
     * get skim period for tour departure interval
     */
    public int getTodOut() {
        return modelStructure.getTod( tour.getTourDepartPeriod() );
    }

    /**
     * get skim period for tour arrival interval
     */
    public int getTodIn() {
        return modelStructure.getTod( tour.getTourArrivePeriod() );
    }
    
    /**
     * Set this index values for this tour mode choice DMU object.
     * 
     * @param hhIndex is the DMU household index
     * @param zoneIndex is the DMU zone index
     * @param origIndex is the DMU origin index
     * @param destIndex is the DMU desatination index
     */
    public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex, int destIndex, boolean debug)
    {
        dmuIndex.setHHIndex(hhIndex);
        dmuIndex.setZoneIndex(zoneIndex);
        dmuIndex.setOriginZone(origIndex);
        dmuIndex.setDestZone(destIndex);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (debug)
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug MC UEC");
        }

    }
    
    public IndexValues getDmuIndexValues() {
        return dmuIndex; 
    }
    
    public void setIndexDest( int d ) {
        dmuIndex.setDestZone( d );
    }

    public void setTourDestTaz( int d ) {
        tour.setTourDestTaz( d );
    }

    public void setTourDestWalkSubzone( int subzone ) {
        tour.setTourDestWalkSubzone( subzone );
    }

    public void setTourOrigTaz( int o ) {
        tour.setTourOrigTaz( o );
    }

    public void setTourDepartPeriod( int period) {
        tour.setTourDepartPeriod(period);
    }
    
    public void setTourArrivePeriod( int period) {
        tour.setTourArrivePeriod(period);
    }
    
    public void setTourOrigWalkSubzone( int subzone ) {
        tour.setTourOrigWalkSubzone( subzone );
    }
    
    public int getWorkTourModeIsSOV() {
        boolean tourModeIsSov = modelStructure.getTourModeIsSov( workTour.getTourModeChoice() );
        return tourModeIsSov ? 1 : 0;
    }

    public int getWorkTourModeIsBike() {
        boolean tourModeIsBike = modelStructure.getTourModeIsBike( workTour.getTourModeChoice() );
        return tourModeIsBike ? 1 : 0;
    }
    
    //tour mode to and from work 
    public int getWorkTourMode() {
        return workTour.getTourModeChoice();
    }


    public int getSubtourType() {
        return tour.getTourPrimaryPurposeIndex();
    }

    public int getTourCategoryJoint() {
        if ( tour.getTourCategoryIsJointNonMandatory() )
            return 1;
        else
            return 0;
    }

    public int getTourCategorySubtour() {
        if ( tour.getTourCategoryIsAtWork() )
            return 1;
        else
            return 0;
    }
    
    public int getTourPrimaryPurposeEscort() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getEscortPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeShopping() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getShoppingPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeEatOut() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getEatOutPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeSocial() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getSocialPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPrimaryPurposeOthDiscr() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getOthDiscrPurposeName() ) )
            return 1;
        else
            return 0;
    }

    
    public int getNumberOfParticipantsInJointTour() {
        byte[] participants = tour.getPersonNumArray();
        int returnValue = 0;
        if ( participants != null )
            returnValue = participants.length;
        return returnValue;
    }

    public int getAgeUnder40() {
        if ( person.getAge() < 40 )
            return 1;
        else
            return 0;
    }

    public int getAge40to59() {
        if ( person.getAge() >= 40 && person.getAge() < 60 )
            return 1;
        else
            return 0;
    }

    public int getAge60to79() {
        if ( person.getAge() >= 60 && person.getAge() < 79 )
            return 1;
        else
            return 0;
    }

    public int getAge60plus() {
        if ( person.getAge() >= 60 )
            return 1;
        else
            return 0;
    }
    
    public int getPersonIsWorker() {
        return person.getPersonIsWorker();
    }
    
    public int getPersonIsPartTimeWorker() {
    	return person.getPersonIsPartTimeWorker();
    }

    public int getPersonIsNonWorkingAdult() {
        if ( person.getPersonIsNonWorkingAdultUnder65() == 1 )
            return 1;
        else
            return 0;
    }

    public int getPersonIsMale() {
        if ( person.getPersonIsMale() == 1 )
            return 1;
        else
            return 0;
    }


    public int getPreDrivingChildInHh() {
        if ( hh.getNumChildrenUnder16() > 0 )
            return 1;
        else
            return 0;
    }

    public int getNumFemalesInHh() {
        int count = 0;
        PersonIf[] persons = hh.getPersons();
        for (int i=1; i < persons.length; i++){
            if ( persons[i].getPersonIsFemale() == 1 )
                count++;
        }
        return count;
    }

    public int getFreeParking() {
        return person.getFreeParkingAvailableResult();
    }
    
    public int getHhSize() {
        return hh.getHhSize();
    }
    
    public int getAutos() {
        return hh.getAutoOwnershipModelResult();
    }

    public int getDrivers() {
        return hh.getDrivers();
    }

    public int getAge() {
        return person.getAge();
    }

    public int getHomemaker() {
        return person.getHomemaker();
    }

    public int getWorkers() {
        return hh.getWorkers();
    }

    public int getOutboundStops() {
        return tour.getNumOutboundStops();
    }

    public int getInboundStops() {
        return tour.getNumInboundStops();
    }

    public int getSize() {
        return hh.getSize();
    }

    public int getGender() {
        return person.getGender();
    }

    public int getChildunder16() {
        return hh.getChildunder16();
    }

    public int getChild16plus() {
        return hh.getChild16plus();
    }
    
    public int getNumChildrenUnder19() {
    	return hh.getNumChildrenUnder19();
    }
    
    public int getNumChildren() {
    	return hh.getNumChildren();
    }

    public int getNumPreschool() {
    	return hh.getNumPreschool();
    }
    
    public int getNumPersons6to15() {
    	return hh.getNumPersons6to15();
    }
    
    public int getTourDepartsAfter4pm() {
        if ( tour.getTourDepartPeriod() >= modelStructure.getIntervalFor4Pm() )
            return 1;
        else
            return 0;
    }

    public int getTourArrivesAfter7pm() {
        if ( tour.getTourArrivePeriod() >= modelStructure.getIntervalFor7Pm() )
            return 1;
        else
            return 0;
    }

    public float getValueOfTime() {
    	return person.getValueOfTime();
    }
    
    public boolean getTourModeIsWalkLocal( int tourMode ) {
    	return(modelStructure.getTourModeIsWalkLocal(tourMode));
    }
    
    public boolean getTourModeIsDriveTransit( int tourMode ) {
    	return(modelStructure.getTourModeIsDriveTransit(tourMode));
    }
    
    public float getHouseholdMaxValueOfTime() {
    	
    	float max_hh_vot = 0;
        
    	PersonIf[] persons = hh.getPersons();
        for (int i=1; i < persons.length; i++){
        	float per_vot = persons[i].getValueOfTime();
        	if(per_vot > max_hh_vot) { max_hh_vot = per_vot; }
        }
        
        return max_hh_vot;
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
