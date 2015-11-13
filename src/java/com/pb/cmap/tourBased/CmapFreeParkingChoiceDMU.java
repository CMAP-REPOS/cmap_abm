package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.FreeParkingChoiceDMU;


public class CmapFreeParkingChoiceDMU extends FreeParkingChoiceDMU {


    
    public CmapFreeParkingChoiceDMU() {
        super();
        setupMethodIndexMap();
    }

    
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getHHIncome", 0 );
        methodIndexMap.put( "getAutoOwnership", 1 );
        methodIndexMap.put( "getFtwkPersons", 2 );
        methodIndexMap.put( "getPtwkPersons", 3 );
        methodIndexMap.put( "getSize", 4 );
    }
    
    

    public int getHHIncome() {
        return hh.getIncomeSegment();
    }


    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getHHIncome();
            case 1: return getAutoOwnership();
            case 2: return getFtwkPersons();
            case 3: return getPtwkPersons();
            case 4: return getSize();

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
    }

}