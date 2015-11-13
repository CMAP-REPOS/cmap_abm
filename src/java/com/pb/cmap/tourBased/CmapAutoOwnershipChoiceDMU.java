package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.AutoOwnershipChoiceDMU;


public class CmapAutoOwnershipChoiceDMU extends AutoOwnershipChoiceDMU {


    public CmapAutoOwnershipChoiceDMU() {
        super ();
        setupMethodIndexMap();
    }
    
    public int getHhIncome() {
        return hh.getIncomeSegment();
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getSize", 0 );
        methodIndexMap.put( "getNumChildrenUnder16", 1 );
        methodIndexMap.put( "getDrivers", 2 );
        methodIndexMap.put( "getWorkers", 3 );
        methodIndexMap.put( "getStudents", 4 );
        methodIndexMap.put( "getNumPersons16to17", 5 );
        methodIndexMap.put( "getNumPersons18to24", 6 );
        methodIndexMap.put( "getHhIncome", 7 );
        methodIndexMap.put( "getWorkAutoDependency", 8 );
        methodIndexMap.put( "getSchoolAutoDependency", 9 );
        methodIndexMap.put( "getHhBldgsz", 10 );
        methodIndexMap.put( "getAccess17", 11 );
        methodIndexMap.put( "getAccess18", 12 );
        methodIndexMap.put( "getNumPersons65to79", 13 );
        methodIndexMap.put( "getNumPersons80plus", 14 );
        
    }
    
    

    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getSize();
            case 1: return getNumChildrenUnder16();
            case 2: return getDrivers();
            case 3: return getWorkers();
            case 4: return getStudents();
            case 5: return getNumPersons16to17();
            case 6: return getNumPersons18to24();
            case 7: return getHhIncome();
            case 8: return getWorkAutoDependency();
            case 9: return getSchoolAutoDependency();
            case 10: return getHhBldgsz();
            case 11: return getAccess17();
            case 12: return getAccess18();
            case 13: return getNumPersons65to79();
            case 14: return getNumPersons80plus();
            
            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
    }

}