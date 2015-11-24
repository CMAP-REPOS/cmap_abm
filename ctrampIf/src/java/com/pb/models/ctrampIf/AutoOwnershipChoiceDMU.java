package com.pb.models.ctrampIf;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class AutoOwnershipChoiceDMU implements Serializable, VariableTable {

    protected transient Logger logger = Logger.getLogger(AutoOwnershipChoiceDMU.class);
    
    protected HashMap<String, Integer> methodIndexMap;

    
    protected HouseholdIf hh;
    private IndexValues dmuIndex;

    private double workTourAutoTimeSavings;
    private double schoolDriveTourAutoTimeSavings;
    private double schoolNonDriveTourAutoTimeSavings;
    
    //TODO - fix these defaults
    private double workAutoDependency = 0;   
	private double schoolAutoDependency = 0; 
	private TazDataIf tazDataManager;

	private int[] zoneTableRow;
    private float[] access17;
    private float[] access18;
    
    public AutoOwnershipChoiceDMU (){
    	dmuIndex = new IndexValues();
    }
    
    
    public void setHouseholdObject ( HouseholdIf hhObject ) {
        hh = hhObject;
    }
    
    
    
    // DMU methods - define one of these for every @var in the mode choice control file.

    public void setDmuIndexValues( int hhId, int zoneId, int origTaz, int destTaz ) {
        dmuIndex.setHHIndex( hhId );
        dmuIndex.setZoneIndex( zoneId );
        dmuIndex.setOriginZone( origTaz );
        dmuIndex.setDestZone( destTaz );

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel ( "" );
        if ( hh.getDebugChoiceModels() ) {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel ( "Debug AO UEC" );
        }

    }
    
    public IndexValues getDmuIndexValues() {
        return dmuIndex; 
    }

    
    public HouseholdIf getHouseholdObject() {
        return hh;
    }
    
    public void setupTazDataManager(TazDataIf tazDataManager) {
    	this.tazDataManager = tazDataManager;
    	zoneTableRow = tazDataManager.getZoneTableRowArray();

        // the zone table columns below returned us 0-based indexing
        access17 = tazDataManager.getAccess17Accessibity();
        access18 = tazDataManager.getAccess18Accessibity();
    	
    }
    
    public double getAccess17() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return access17[index];
    }
    
    public double getAccess18() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return access18[index];
    }
    
    public int getSize() {
        return hh.getSize();
    }
    
    public int getNumChildrenUnder16() {
        return hh.getNumChildrenUnder16();
    }
    
    public int getHhBldgsz() {
    	return hh.getHhBldgsz();
    }
    
    public int getDrivers() {
        return hh.getDrivers();
    }
    
    public int getWorkers() {
        return hh.getWorkers();
    }

    public int getStudents() {
        return hh.getNumStudents();
    }

    public int getNumPersons16to17() {
        return hh.getNumPersons16to17();
    }
    
    public int getNumPersons18to24() {
        return hh.getNumPersons18to24();
    }
    
    public int getNumPersons65to79() {
        return hh.getNumPersons65to79();
    }
    
    public int getNumPersons80plus() {
        return hh.getNumPersons80plus();
    }    
    
    public void setWorkTourAutoTimeSavings( double value ) {
        workTourAutoTimeSavings = value;
    }
    
    public double getWorkTourAutoTimeSavings() {
        return workTourAutoTimeSavings;
    }
    
    public void setSchoolDriveTourAutoTimeSavings( double value ) {
        schoolDriveTourAutoTimeSavings = value;
    }
    
    public double getSchoolDriveTourAutoTimeSavings() {
        return schoolDriveTourAutoTimeSavings;
    }
    
    public void setSchoolNonDriveTourAutoTimeSavings( double value ) {
        schoolNonDriveTourAutoTimeSavings = value;
    }
    
    public double getSchoolNonDriveTourAutoTimeSavings() {
        return schoolNonDriveTourAutoTimeSavings;
    }
    
    

    public int getIndexValue(String variableName) {
        return methodIndexMap.get(variableName);
    }

    
	public double getWorkAutoDependency() {
		return workAutoDependency;
	}
	public double getSchoolAutoDependency() {
		return schoolAutoDependency;
	}
	
	public void setWorkAutoDependency(double workAutoDependency) {
		this.workAutoDependency = workAutoDependency;
	}
	public void setSchoolAutoDependency(double schoolAutoDependency) {
		this.schoolAutoDependency = schoolAutoDependency;
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
