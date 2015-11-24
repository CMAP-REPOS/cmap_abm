package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.StopLocationDMU;
import com.pb.models.ctrampIf.TazDataIf;


public class CmapStopLocationDMU extends StopLocationDMU {

	TazDataIf tazDataManager;
	
    public CmapStopLocationDMU( TazDataIf tazDataManager, ModelStructure modelStructure ) {
        super ( tazDataManager, modelStructure );
        setupMethodIndexMap();
        
        this.tazDataManager = tazDataManager;
    }

    public double getOMazDMazDistanceAlt ( int alt ) {
    	int destZone = altToZone[alt];
        return tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), destZone);
    }
    
    public double getAltDMazDistanceAlt ( int alt ) {
    	int altZone = altToZone[alt];
        return tazDataManager.getOMazDMazDistance(altZone, dmuIndex.getDestZone());
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getDcSoaCorrectionsAlt", 0 );
        methodIndexMap.put( "getTripModeChoiceLogsumOrigToStopAlt", 1 );
        methodIndexMap.put( "getTripModeChoiceLogsumStopAltToDest", 2 );
        methodIndexMap.put( "getStopDestAreaTypeAlt", 3 );
        methodIndexMap.put( "getStopPurposeIsWork", 4 );
        methodIndexMap.put( "getStopPurposeIsEscort", 5 );
        methodIndexMap.put( "getStopPurposeIsShopping", 6 );
        methodIndexMap.put( "getStopPurposeIsEatOut", 7 );
        methodIndexMap.put( "getStopPurposeIsOthMaint", 8 );
        methodIndexMap.put( "getStopPurposeIsSocial", 9 );
        methodIndexMap.put( "getStopPurposeIsOthDiscr", 10 );
        methodIndexMap.put( "getTourOriginZone", 11 );
        methodIndexMap.put( "getTourDestZone", 12 );
        methodIndexMap.put( "getTourMode", 13 );
        methodIndexMap.put( "getStopNumber", 14 );
        methodIndexMap.put( "getInboundStop", 15 );
        methodIndexMap.put( "getTourIsJoint", 16 );
        methodIndexMap.put( "getLnStopDcSizeAlt", 17 );
        
        methodIndexMap.put( "getOMazDMazDistanceAlt", 20 );
        methodIndexMap.put( "getAltDMazDistanceAlt", 21 );
    }
    
    



    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getDcSoaCorrectionsAlt( arrayIndex );
            case 1: return getTripModeChoiceLogsumOrigToStopAlt( arrayIndex );
            case 2: return getTripModeChoiceLogsumStopAltToDest( arrayIndex );
            case 3: return getStopDestAreaTypeAlt( arrayIndex );
            case 4: return getStopPurposeIsWork();
            case 5: return getStopPurposeIsEscort();
            case 6: return getStopPurposeIsShopping();
            case 7: return getStopPurposeIsEatOut();
            case 8: return getStopPurposeIsOthMaint();
            case 9: return getStopPurposeIsSocial();
            case 10: return getStopPurposeIsOthDiscr();
            case 11: return getTourOriginZone();
            case 12: return getTourDestZone();
            case 13: return getTourMode();
            case 14: return getStopNumber();
            case 15: return getInboundStop();
            case 16: return getTourIsJoint();
            case 17: return getLnStopDcSizeAlt( arrayIndex );
            
            case 20: return getOMazDMazDistanceAlt( arrayIndex );
            case 21: return getAltDMazDistanceAlt( arrayIndex );

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
    }

}