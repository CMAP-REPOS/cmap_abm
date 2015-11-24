package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.cmap.tourBased.CmapModelStructure;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.StopDCSoaDMU;


public class CmapStopDCSoaDMU extends StopDCSoaDMU {

	CmapModelStructure modelStructure;
    
	TazDataIf tazDataManager;
	
    public CmapStopDCSoaDMU( TazDataIf tazDataManager, CmapModelStructure modelStructure ) {
        super ( tazDataManager, modelStructure );
        this.modelStructure = modelStructure;
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
    
    public int getHasTransitAccessAlt ( int alt ) {
    	int altZone = altToZone[alt];
        return tazDataManager.getHasTransitAccess(altZone);
    }
    
    public int getTourTodOut() {
        return modelStructure.getTod( tour.getTourDepartPeriod() );
    }

    public int getTourTodIn() {
        return modelStructure.getTod( tour.getTourArrivePeriod() );
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getInbound", 0 );
        methodIndexMap.put( "getKidsPresent", 1 );
        methodIndexMap.put( "getTourModeIsWalk", 2 );
        methodIndexMap.put( "getTourModeIsBike", 3 );
        methodIndexMap.put( "getTourModeIsWalkLocal", 4 );
        methodIndexMap.put( "getTourModeIsWalkPremium", 5 );
        methodIndexMap.put( "getTourTodOut", 6 );
        methodIndexMap.put( "getTourTodIn", 7 );
        methodIndexMap.put( "getLnStopDcSizeAlt", 8 );
        
        
        methodIndexMap.put( "getTourModeIsDriveTransit", 9 );
        
        
        methodIndexMap.put( "getOMazDMazDistanceAlt", 20 );
        methodIndexMap.put( "getAltDMazDistanceAlt", 21 );
        
        methodIndexMap.put( "getHasTransitAccessAlt", 22 );

    }
    
    


    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getInbound();
            case 1: return getKidsPresent();
            case 2: return getTourModeIsWalk();
            case 3: return getTourModeIsBike();
            case 4: return getTourModeIsWalkLocal();
            case 5: return getTourModeIsWalkPremium();
            case 6: return getTourTodOut();
            case 7: return getTourTodIn();
            case 8: return getLnStopDcSizeAlt( arrayIndex );
            
            case 9: return getTourModeIsDriveTransit();
            
            case 20: return getOMazDMazDistanceAlt( arrayIndex );
            case 21: return getAltDMazDistanceAlt( arrayIndex );
            
            case 22: return getHasTransitAccessAlt( arrayIndex );

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
    }

}