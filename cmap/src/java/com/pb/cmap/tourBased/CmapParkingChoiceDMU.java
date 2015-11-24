package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.ParkingChoiceDMU;
import com.pb.models.ctrampIf.TazDataIf;



public class CmapParkingChoiceDMU extends ParkingChoiceDMU {

    
	TazDataIf tazDataManager;
	        
    public CmapParkingChoiceDMU( TazDataIf tazDataManager, ModelStructure modelStructure ){
        super( tazDataManager );
        setupMethodIndexMap( modelStructure );
        
        this.tazDataManager = tazDataManager;
    }
    
    public double getOMazDMazDistanceAlt ( int alt ) {
    	int destZone = parkTazs[alt-1];
        return tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), destZone);
    }
    
    public double getAltDMazDistanceAlt ( int alt ) {
    	int altZone = parkTazs[alt-1];
        return tazDataManager.getOMazDMazDistance(altZone, dmuIndex.getDestZone());
    }

    private void setupMethodIndexMap( ModelStructure modelStructure ) {
    
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getParkTazCbdAreaTypeAlt", 0 );
        methodIndexMap.put( "getTourIsJoint", 1 );
        methodIndexMap.put( "getParkingCostAlt", 2 );
        methodIndexMap.put( "getParkTot", 3 );
        methodIndexMap.put( "getParkTotAlt", 4 );
        
        methodIndexMap.put( "getOMazDMazDistanceAlt", 20 );
        methodIndexMap.put( "getAltDMazDistanceAlt", 21 );
        
        // populate map of hours parked per day by purpose name
        averageDailyHoursParkedMap = new HashMap<String, Float>();
        averageDailyHoursParkedMap.put( modelStructure.WORK_PURPOSE_NAME, 7.9f );
        averageDailyHoursParkedMap.put( modelStructure.UNIVERSITY_PURPOSE_NAME, 6.2f );
        averageDailyHoursParkedMap.put( modelStructure.SCHOOL_PURPOSE_NAME, 7.9f );
        averageDailyHoursParkedMap.put( modelStructure.ESCORT_PURPOSE_NAME, 1.1f );
        averageDailyHoursParkedMap.put( modelStructure.SHOPPING_PURPOSE_NAME, 1.7f );
        averageDailyHoursParkedMap.put( modelStructure.OTH_MAINT_PURPOSE_NAME, 2.8f );
        averageDailyHoursParkedMap.put( modelStructure.EAT_OUT_PURPOSE_NAME, 2.2f );
        averageDailyHoursParkedMap.put( modelStructure.SOCIAL_PURPOSE_NAME, 3.0f );
        averageDailyHoursParkedMap.put( modelStructure.OTH_DISCR_PURPOSE_NAME, 2.7f );
        averageDailyHoursParkedMap.put( modelStructure.AT_WORK_PURPOSE_NAME, 0.6f );
        
    }
    
    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
        case 0: return getParkTazCbdAreaTypeAlt( arrayIndex );
        case 1: return getTourIsJoint();
        case 2: return getParkingCostAlt( arrayIndex );
        case 3: return getParkTot();
        case 4: return getParkTotAlt( arrayIndex );
        
        case 20: return getOMazDMazDistanceAlt( arrayIndex );
        case 21: return getAltDMazDistanceAlt( arrayIndex );

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
        
    }

}