package com.pb.models.ctrampIf;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class TripModeChoiceDMU implements Serializable, VariableTable {

    protected transient Logger logger = Logger.getLogger(TripModeChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;
    
    protected StopIf stop;
    protected TourIf tour;
    protected PersonIf person;
    protected HouseholdIf hh;
    protected IndexValues dmuIndex;

    protected int origType;
    protected int destType;
    protected float origParkRate;
    protected float primDestParkRate;
    protected float intStopParkRate;
    
    protected int stopIsFirst;
    protected int stopIsLast;
    

    private ModelStructure modelStructure;
    
    int[] tazForMaz;
    
	float genCostWT;
	float genCostKNR;
	float genCostPNR;
	int OtapWT;
	int DtapWT;
	int OtapKNR;
	int DtapKNR;
	int OtapPNR;
	int DtapPNR;
	
	TazDataIf tazDataManager;
    
    public TripModeChoiceDMU(TazDataIf tazDataManager, ModelStructure modelStructure ){
        this.modelStructure = modelStructure;
    	dmuIndex = new IndexValues();
    	
    	tazForMaz = tazDataManager.getTazsForMazs();
        
        this.tazDataManager = tazDataManager;
    }
    


    public HouseholdIf getHouseholdObject() {
        return hh;
    }
    
    public TourIf getTourObject() {
        return tour;
    }
    
    public int getTodOut() {
        return modelStructure.getTod( stop.getStopPeriod() );
    }
    
    public void setHouseholdObject ( HouseholdIf hhObject ) {
        hh = hhObject;
    }
    
    public void setPersonObject ( PersonIf personObject ) {
        person = personObject;
    }
    
    public void setTourObject ( TourIf tourObject ) {
        tour = tourObject;
    }
    
    public void setStopObject ( StopIf stopObject ) {
        stop = stopObject;
    }
    
    public void setStopObjectIsFirst ( int flag ) {
        stopIsFirst = flag;
    }
    
    public void setStopObjectIsLast ( int flag ) {
        stopIsLast = flag;
    }
    

    
    public int getOmazOtaz() {
    	return tazForMaz[dmuIndex.getOriginZone()-1];
    }
    
    public int getDmazDtaz() {
    	return tazForMaz[dmuIndex.getDestZone()-1];
    }

    public float getGenCostWT() {
    	return genCostWT;
    }
    
    public float getGenCostKNR() {
    	return genCostKNR;
    }
    
    public float getGenCostPNR() {
    	return genCostPNR;
    }
    
    public int getOtapWT() {
    	return OtapWT;
    }
    
    public int getDtapWT() {
    	return DtapWT;
    }
    
    public int getOtapKNR() {
    	return OtapKNR;
    }
    
    public int getDtapKNR() {
    	return DtapKNR;
    }
    
    public int getOtapPNR() {
    	return OtapPNR;
    }
    
    public int getDtapPNR() {
    	return DtapPNR;
    }

    public void setGenCostWT(float genCost) {
    	this.genCostWT = genCost;
    }
    
    public void setGenCostKNR(float genCost) {
    	this.genCostKNR = genCost;
    }
    
    public void setGenCostPNR(float genCost) {
    	this.genCostPNR = genCost;
    }
    
    public void setOtapWT(int Otap) {
    	this.OtapWT = Otap;
    }
    
    public void setDtapWT(int Dtap) {
    	this.DtapWT = Dtap;
    }
    
    public void setOtapKNR(int Otap) {
    	this.OtapKNR = Otap;
    }
    
    public void setDtapKNR(int Dtap) {
    	this.DtapKNR = Dtap;
    }
    
    public void setOtapPNR(int Otap) {
    	this.OtapPNR = Otap;
    }
    
    public void setDtapPNR(int Dtap) {
    	this.DtapPNR = Dtap;
    }
    
    public double getOMazDMazDistance() {
        return tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), dmuIndex.getDestZone());
    }
    
    
    // DMU methods - define one of these for every @var in the mode choice control file.

    public void setDmuIndexValues( int hhId, int origTaz, int destTaz ) {
        dmuIndex.setHHIndex( hhId );
        dmuIndex.setZoneIndex( destTaz );
        dmuIndex.setOriginZone( origTaz );
        dmuIndex.setDestZone( destTaz );

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel ( "" );
        if ( hh.getDebugChoiceModels() ) {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel ( "Debug Trip MC UEC" );
        }

    }
    
    
    /**
     * origin type is 1 if home, 2 if primary dest, 3 if intermediate stop, for home-based tours.
     * origin type is 1 if work, 2 if primary dest, 3 if intermediate stop, for work-based tours.
     */
    public void setOrigType( int type ) {
        origType = type;
    }

    /**
     * destination type is 1 if home, 2 if primary dest, 3 if intermediate stop, for home-based tours.
     * destination type is 1 if work, 2 if primary dest, 3 if intermediate stop, for work-based tours.
     */
    public void setDestType( int type ) {
        destType = type;
    }

    public void setOrigParkRate( float cost ) {
        origParkRate = cost;
    }

    public void setIntStopParkRate( float cost ) {
        intStopParkRate = cost;
    }

    public void setPrimDestParkRate( float cost ) {
        primDestParkRate = cost;
    }


    public int getFreeParking() {
        return person.getFreeParkingAvailableResult();
    }
    
    
    public int getOrigType() {
        return origType;
    }
    
    public int getDestType() {
        return destType;
    }
    
    public float getOrigParkRate() {
        return origParkRate;
    }
    
    public float getIntStopParkRate() {
        return intStopParkRate;
    }
    
    public float getPrimDestParkRate() {
        return primDestParkRate;
    }
    
    public IndexValues getDmuIndexValues() {
        return dmuIndex; 
    }
  

    public int getZonalShortWalkAccessOrig() {
        if ( stop.getOrigWalkSegment() == 1 )
            return 1;
        else
            return 0;
    }

    public int getZonalShortWalkAccessDest() {
        if ( stop.getDestWalkSegment() == 1 )
            return 1;
        else
            return 0;
    }

    public int getZonalLongWalkAccessOrig() {
        if ( stop.getOrigWalkSegment() == 2 )
            return 1;
        else
            return 0;
    }

    public int getZonalLongWalkAccessDest() {
        if ( stop.getDestWalkSegment() == 2 )
            return 1;
        else
            return 0;
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
    
    public int getTourModeIsAuto() {
        boolean tourModeIsAuto = modelStructure.getTourModeIsSovOrHov( tour.getTourModeChoice() );
        return tourModeIsAuto ? 1 : 0;
    }

    public int getTourModeIsWalkTransit() {
        boolean tourModeIsWalkLocal = modelStructure.getTourModeIsWalkLocal( tour.getTourModeChoice() );
        boolean tourModeIsWalkPremium = modelStructure.getTourModeIsWalkPremium( tour.getTourModeChoice() );
        return tourModeIsWalkLocal || tourModeIsWalkPremium ? 1 : 0;
    }

    public int getTourModeIsDriveTransit() {
        boolean tourModeIsDriveTransit = modelStructure.getTourModeIsDriveTransit( tour.getTourModeChoice() );
        return tourModeIsDriveTransit ? 1 : 0;
    }

    public int getWorkTourModeIsSOV() {
        boolean workTourModeIsSov = modelStructure.getTourModeIsSov( tour.getTourModeChoice() );
        return workTourModeIsSov ? 1 : 0;
    }

    public int getWorkTourModeIsBike() {
        boolean tourModeIsBike = modelStructure.getTourModeIsBike( tour.getTourModeChoice() );
        return tourModeIsBike ? 1 : 0;
    }

    public int getTourModeIsSOV() {
        boolean tourModeIsSov = modelStructure.getTourModeIsSov( tour.getTourModeChoice() );
        return tourModeIsSov ? 1 : 0;
    }

    public int getTourModeIsBike() {
        boolean tourModeIsBike = modelStructure.getTourModeIsBike( tour.getTourModeChoice() );
        return tourModeIsBike ? 1 : 0;
    }

    public int getTourModeIsWalk() {
        boolean tourModeIsWalk = modelStructure.getTourModeIsWalk( tour.getTourModeChoice() );
        return tourModeIsWalk ? 1 : 0;
    }

    public int getTourMode() {
        return tour.getTourModeChoice();
    }

    public int getSubtourType() {
        return tour.getTourPrimaryPurposeIndex();
    }

    
    public int getNumberOfParticipantsInJointTour() {
        return tour.getPersonNumArray().length;
    }

    
    public boolean getModeIsWalkLocal( int mode ) {
    	return(modelStructure.getTourModeIsWalkLocal(mode));
    }
    
    public boolean getModeIsDriveTransit( int mode ) {
    	return(modelStructure.getTourModeIsDriveTransit(mode));
    }
    
    public int getStopIsFirst() {
        return stopIsFirst;
    }
    
    public int getStopIsLast() {
        return stopIsLast;
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
