package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.DcSoaDMU;
import com.pb.models.ctrampIf.TazDataIf;


public class CmapDcSoaDMU extends DcSoaDMU {

	 TazDataIf tazDataManager;
    
    public CmapDcSoaDMU( TazDataIf tazDataManager ) {
        super ( tazDataManager );
        setupMethodIndexMap();
        
        this.tazDataManager = tazDataManager;
    }

    public double getOMazDMazDistanceAlt ( int alt ) {
    	int destZone = altToZone[alt];
        return tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), destZone);
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getLnWorkOcc1DcSizeAlt", 0 );
        methodIndexMap.put( "getLnWorkOcc2DcSizeAlt", 1 );
        methodIndexMap.put( "getLnWorkOcc3DcSizeAlt", 2 );
        methodIndexMap.put( "getLnWorkOcc4DcSizeAlt", 3 );
        methodIndexMap.put( "getLnWorkOcc5DcSizeAlt", 4 );
        methodIndexMap.put( "getLnWorkOcc6DcSizeAlt", 5 );
        methodIndexMap.put( "getLnWorkOcc7DcSizeAlt", 6 );
        methodIndexMap.put( "getLnWorkOcc8DcSizeAlt", 7 );
        methodIndexMap.put( "getLnWorkOcc9DcSizeAlt", 8 );
        methodIndexMap.put( "getLnWorkOcc10DcSizeAlt", 9 );
        methodIndexMap.put( "getLnWorkOcc11DcSizeAlt", 10 );
        methodIndexMap.put( "getLnWorkOcc12DcSizeAlt", 11 );

        methodIndexMap.put( "getLnUnivDcSizeAlt", 12 );
        methodIndexMap.put( "getLnSchoolPreDcSizeAlt", 13 );
        methodIndexMap.put( "getLnSchoolGradeDcSizeAlt", 14 );
        methodIndexMap.put( "getLnSchoolHighDcSizeAlt", 15 );
        methodIndexMap.put( "getLnEscortDcSizeAlt", 16 );
        methodIndexMap.put( "getLnShoppingDcSizeAlt", 17 );
        methodIndexMap.put( "getLnEatOutDcSizeAlt", 18 );
        methodIndexMap.put( "getLnOthMaintDcSizeAlt", 19 );
        methodIndexMap.put( "getLnSocialDcSizeAlt", 20 );
        methodIndexMap.put( "getLnOthDiscrDcSizeAlt", 21 );
        methodIndexMap.put( "getLnAtWorkDcSizeAlt", 22 );
        
        methodIndexMap.put( "getWorkerOccupation", 23 );
        
        methodIndexMap.put( "getOMazDMazDistanceAlt", 60 );
    }
    
    
    // DMU methods - define one of these for every @var in the mode choice control file.
    public double getLnWorkOcc1DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ1" );
    }
    
    public double getLnWorkOcc2DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ2" );
    }
    
    public double getLnWorkOcc3DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ3" );
    }
    
    public double getLnWorkOcc4DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ4" );
    }
    
    public double getLnWorkOcc5DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ5" );
    }
    
    public double getLnWorkOcc6DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ6" );
    }
    
    public double getLnWorkOcc7DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ7" );
    }
    
    public double getLnWorkOcc8DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ8" );
    }
    
    public double getLnWorkOcc9DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ9" );
    }
    
    public double getLnWorkOcc10DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ10" );
    }
    
    public double getLnWorkOcc11DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ11" );
    }
    
    public double getLnWorkOcc12DcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work_occ12" );
    }

    public double getLnUnivDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "university" );
    }

    public double getLnSchoolPreDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "school_pre" );
    }

    public double getLnSchoolGradeDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "school_grade" );
    }
    
    public double getLnSchoolHighDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "school_high" );
    }

    public double getLnEscortDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "escort" );
    }

    public double getLnShoppingDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "shop" );
    }

    public double getLnEatOutDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "eatingOut" );
    }

    public double getLnOthMaintDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "maintenance" );
    }

    public double getLnSocialDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "visiting" );
    }

    public double getLnOthDiscrDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "discretionary" );
    }

    public double getLnAtWorkDcSizeAlt( int alt ) {
        return getLnDcSizeForPurpSegAlt( alt, "work-based" );
    }

    public int getWorkerOccupation() {
        return person.getPersonWorkerOccupation();
    }
    
    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getLnWorkOcc1DcSizeAlt( arrayIndex );
            case 1: return getLnWorkOcc2DcSizeAlt( arrayIndex );
            case 2: return getLnWorkOcc3DcSizeAlt( arrayIndex );
            case 3: return getLnWorkOcc4DcSizeAlt( arrayIndex );
            case 4: return getLnWorkOcc5DcSizeAlt( arrayIndex );
            case 5: return getLnWorkOcc6DcSizeAlt( arrayIndex );
            case 6: return getLnWorkOcc7DcSizeAlt( arrayIndex );
            case 7: return getLnWorkOcc8DcSizeAlt( arrayIndex );
            case 8: return getLnWorkOcc9DcSizeAlt( arrayIndex );
            case 9: return getLnWorkOcc10DcSizeAlt( arrayIndex );
            case 10: return getLnWorkOcc11DcSizeAlt( arrayIndex );
            case 11: return getLnWorkOcc12DcSizeAlt( arrayIndex );
            
            case 12: return getLnUnivDcSizeAlt( arrayIndex );
            case 13: return getLnSchoolPreDcSizeAlt( arrayIndex );
            case 14: return getLnSchoolGradeDcSizeAlt( arrayIndex );
            case 15: return getLnSchoolHighDcSizeAlt( arrayIndex );
            case 16: return getLnEscortDcSizeAlt( arrayIndex );
            case 17: return getLnShoppingDcSizeAlt( arrayIndex );
            case 18: return getLnEatOutDcSizeAlt( arrayIndex );
            case 19: return getLnOthMaintDcSizeAlt( arrayIndex );
            case 20: return getLnSocialDcSizeAlt( arrayIndex );
            case 21: return getLnOthDiscrDcSizeAlt( arrayIndex );
            case 22: return getLnAtWorkDcSizeAlt( arrayIndex );
            
            case 23: return getWorkerOccupation( );
            
            case 60: return getOMazDMazDistanceAlt( arrayIndex );
            
            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
    }

}