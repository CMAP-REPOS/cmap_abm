package com.pb.models.ctrampIf;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class DcSoaDMU implements SoaDMU, Serializable, VariableTable  {

    protected transient Logger logger = Logger.getLogger(DcSoaDMU.class);

    protected HashMap<String, Integer> methodIndexMap;
    

    protected HouseholdIf hh;
    protected PersonIf person;
    protected IndexValues dmuIndex = null;
    protected String dmuLabel = "Origin Location";
    
    protected DestChoiceSize dcSizeObj;

    protected int[] altToZone;
    protected int[] altToSubZone;

    
    public DcSoaDMU( TazDataIf tazDataManager ){
        altToZone = tazDataManager.getAltToZoneArray();
        altToSubZone = tazDataManager.getAltToSubZoneArray();

        dmuIndex = new IndexValues();
    }


    
    public void setDmuIndexValues( int hhId, int zoneId, int origTaz, int destTaz ) {
        dmuIndex.setHHIndex( hhId );
        dmuIndex.setZoneIndex( zoneId );
        dmuIndex.setOriginZone( origTaz );
        dmuIndex.setDestZone( destTaz );

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel ( "" );
        if ( hh.getDebugChoiceModels() ) {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel ( "Debug DC SOA UEC" );
        }

    }
    
    public void setHouseholdObject ( HouseholdIf hhObject ) {
        hh = hhObject;
    }

    public void setPersonObject ( PersonIf personObject ) {
        person = personObject;
    }
    
    public void setDestChoiceSizeObject ( DestChoiceSize dcSizeObj ) {
        this.dcSizeObj = dcSizeObj;
    }

    public IndexValues getDmuIndexValues() {
        return dmuIndex; 
    }
    
    public HouseholdIf getHouseholdObject() {
        return hh;
    }

    protected double getLnDcSizeForPurpSegAlt( int alt, String purposeString ){
        
        int zone = altToZone[alt];
        int subzone = altToSubZone[alt];
        
        int purposeIndex = dcSizeObj.getDcSizeArrayPurposeIndex(purposeString);
        double size = dcSizeObj.getDcSize(purposeIndex, zone, subzone);

        double logSize = 0.0;
        if ( size > 0 )
            logSize = Math.log(size + 1);
        
        return logSize;
    	
    }

    public String getDmuLabel() {
        return dmuLabel;
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


