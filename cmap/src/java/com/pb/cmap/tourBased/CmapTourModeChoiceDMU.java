package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.cmap.tourBased.CmapModelStructure;
import com.pb.models.ctrampIf.TourModeChoiceDMU;
import com.pb.models.ctrampIf.TazDataIf;



public class CmapTourModeChoiceDMU extends TourModeChoiceDMU {

    int[] zoneTableRow;
    int[] areaType;
    float[] prkCost;
    float[] bhhden;
    float[] bempden;
    float[] pef;
    int[] ring;
    CmapModelStructure modelStructure;
    
    int[] tazForMaz;
	
	TazDataIf tazDataManager;
	
    public CmapTourModeChoiceDMU(TazDataIf tazDataManager, CmapModelStructure modelStructure){
        super( modelStructure );
        this.modelStructure = modelStructure;
        setup( tazDataManager );
        setupMethodIndexMap();
    }

    private void setup( TazDataIf tazDataManager ) {

        zoneTableRow = tazDataManager.getZoneTableRowArray();

        // the zone table columns below returned use 0-based indexing
        areaType = tazDataManager.getZonalAreaType();
        bhhden = tazDataManager.getBHHDen();
        bempden = tazDataManager.getBEmpDen();
        pef = tazDataManager.getPef();
        ring = tazDataManager.getRing();
        prkCost = tazDataManager.getZonalParkRate();
        
        tazForMaz = tazDataManager.getTazsForMazs();
        
        this.tazDataManager = tazDataManager;
        
    }
    
    public double getOriginBHHDen() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return bhhden[index];
    }
    
    public double getDestinationBHHDen() {
    	int index = zoneTableRow[dmuIndex.getDestZone()] - 1;
        return bhhden[index];
    }
    
    public double getOrigRing() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return ring[index];
    }
    
    public double getDestRing() {
    	int index = zoneTableRow[dmuIndex.getDestZone()] - 1;
        return ring[index];
    }
    
    public double getOriginBEmpDen() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return bempden[index];
    }
    
    public double getDestinationBEmpDen() {
    	int index = zoneTableRow[dmuIndex.getDestZone()] - 1;
        return bempden[index];
    }
    
    public double getOriginPef() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return pef[index];
    }
    
    public double getDestinationPef() {
    	int index = zoneTableRow[dmuIndex.getDestZone()] - 1;
        return pef[index];
    }
    
    public double getOrigParkCost() {
    	int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return prkCost[index];
    }
    
    public double getDestParkCost() {
    	int index = zoneTableRow[dmuIndex.getDestZone()] - 1;
        return prkCost[index];
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

    public int getZonalShortWalkAccessOrig() {
        if ( tour.getTourOrigWalkSubzone() == 1 )
            return 1;
        else
            return 0;
    }

    public int getZonalShortWalkAccessDest() {
        if ( tour.getTourDestWalkSubzone() == 1 )
            return 1;
        else
            return 0;
    }

    public int getZonalLongWalkAccessOrig() {
        if ( tour.getTourOrigWalkSubzone() == 2 )
            return 1;
        else
            return 0;
    }

    public int getZonalLongWalkAccessDest() {
        if ( tour.getTourDestWalkSubzone() == 2 )
            return 1;
        else
            return 0;
    }

    // fixed Structure value -- need to update to use ModelStructure
    // TODO update to make general using ModelStructure and remove zdm
    public int getRuralDestination() {

        //TODO: Kim review with Jim.
        //int at = tazDataManager.getZonalAreaType( tour.getTourDestTaz() );
        int at = areaType[tour.getTourDestTaz() - 1];

        if ( at == 7 ) // Structure.AreaType.RURAL.ordinal() )
            return 1;
        else
            return 0;
    }



    public int getIncome() {
        return hh.getIncomeSegment();
    }
    
    
    public int getOmazOtaz() {
    	return tazForMaz[dmuIndex.getOriginZone()-1];
    }
    
    public int getDmazDtaz() {
    	return tazForMaz[dmuIndex.getDestZone()-1];
    }
    
    public double getOMazDMazDistance() {
        return tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), dmuIndex.getDestZone());
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getTodOut", 0 );
        methodIndexMap.put( "getTodIn", 1 );
        methodIndexMap.put( "getZonalShortWalkAccessOrig", 2 );
        methodIndexMap.put( "getZonalShortWalkAccessDest", 3 );
        methodIndexMap.put( "getZonalLongWalkAccessOrig", 4 );
        methodIndexMap.put( "getZonalLongWalkAccessDest", 5 );
        methodIndexMap.put( "getRuralDestination", 6 );
        methodIndexMap.put( "getHhSize", 7 );
        methodIndexMap.put( "getAutos", 8 );
        methodIndexMap.put( "getDrivers", 9 );
        methodIndexMap.put( "getAge", 10 );
        methodIndexMap.put( "getIncome", 11 );
        methodIndexMap.put( "getAgeUnder40", 12 );
        methodIndexMap.put( "getAge40to59", 13 );
        methodIndexMap.put( "getAge60to79", 14 );
        methodIndexMap.put( "getAge60plus", 15 );
        methodIndexMap.put( "getPersonIsWorker", 16 );
        methodIndexMap.put( "getPersonIsNonWorkingAdult", 17 );
        methodIndexMap.put( "getPersonIsMale", 18 );
        methodIndexMap.put( "getPreDrivingChildInHh", 19 );
        methodIndexMap.put( "getNumFemalesInHh", 20 );
        methodIndexMap.put( "getFreeParking", 23 );
        methodIndexMap.put( "getHomemaker", 24 );
        methodIndexMap.put( "getWorkers", 25 );
        methodIndexMap.put( "getSize", 26 );
        methodIndexMap.put( "getGender", 27 );
        methodIndexMap.put( "getChildunder16", 28 );
        methodIndexMap.put( "getChild16plus", 29 );
        methodIndexMap.put( "getTourCategoryJoint", 30 );
        methodIndexMap.put( "getTourPurposeEscort", 31 );
        methodIndexMap.put( "getTourPurposeShopping", 32 );
        methodIndexMap.put( "getTourPurposeEatOut", 33 );
        methodIndexMap.put( "getTourPurposeSocial", 34 );
        methodIndexMap.put( "getTourPurposeOthDiscr", 35 );
        methodIndexMap.put( "getNumberOfParticipantsInJointTour", 36 );
        methodIndexMap.put( "getWorkTourModeIsSOV", 37 );
        methodIndexMap.put( "getWorkTourModeIsBike", 38 );
        methodIndexMap.put( "getSubtourType", 39 );
        methodIndexMap.put( "getTourDepartsAfter4pm", 40 );
        methodIndexMap.put( "getTourArrivesAfter7pm", 41 );
        methodIndexMap.put( "getValueOfTime", 42 );
        
        methodIndexMap.put( "getOriginBHHDen", 43 );
        methodIndexMap.put( "getDestinationBHHDen", 44 );
        methodIndexMap.put( "getOriginBEmpDen", 45 );
        methodIndexMap.put( "getDestinationBEmpDen", 46 );
        methodIndexMap.put( "getOriginPef", 47 );
        methodIndexMap.put( "getDestinationPef", 48 );
        
        methodIndexMap.put( "getPersonIsPartTimeWorker", 49);
        methodIndexMap.put( "getNumChildrenUnder19", 50);
        methodIndexMap.put( "getNumPreschool", 51);
        methodIndexMap.put( "getNumPersons6to15", 52);
        methodIndexMap.put( "getWorkTourMode", 53);
        methodIndexMap.put( "getNumChildren", 54);
        methodIndexMap.put( "getHouseholdMaxValueOfTime", 55);
        
        methodIndexMap.put( "getOrigRing", 56 );
        methodIndexMap.put( "getDestRing", 57 );
        
        methodIndexMap.put( "getOrigParkCost", 58 );
        methodIndexMap.put( "getDestParkCost", 59 );
        
        methodIndexMap.put( "getOmazOtaz", 60 );
        methodIndexMap.put( "getDmazDtaz", 61 );
        
        methodIndexMap.put( "getGenCostWT_In", 70 );
        methodIndexMap.put( "getGenCostDL_In", 71 );
        methodIndexMap.put( "getGenCostDP_In", 72 );
        
        methodIndexMap.put( "getOtapWT_In", 73 );
        methodIndexMap.put( "getDtapWT_In", 74 );
        
        methodIndexMap.put( "getOtapDL_In", 75 );
        methodIndexMap.put( "getDtapDL_In", 76 );
        
        methodIndexMap.put( "getOtapDP_In", 77 );
        methodIndexMap.put( "getDtapDP_In", 78 );
        
        methodIndexMap.put( "getGenCostWT_Out", 80 );
        methodIndexMap.put( "getGenCostDL_Out", 81 );
        methodIndexMap.put( "getGenCostDP_Out", 82 );
        
        methodIndexMap.put( "getOtapWT_Out", 83 );
        methodIndexMap.put( "getDtapWT_Out", 84 );
        
        methodIndexMap.put( "getOtapDL_Out", 85 );
        methodIndexMap.put( "getDtapDL_Out", 86 );
        
        methodIndexMap.put( "getOtapDP_Out", 87 );
        methodIndexMap.put( "getDtapDP_Out", 88 );
        
        methodIndexMap.put( "getOMazDMazDistance", 90 );
       
    }
    
        
    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getTodOut();
            case 1: return getTodIn();
            case 2: return getZonalShortWalkAccessOrig();
            case 3: return getZonalShortWalkAccessDest();
            case 4: return getZonalLongWalkAccessOrig();
            case 5: return getZonalLongWalkAccessDest();
            case 6: return getRuralDestination();
            case 7: return getHhSize();
            case 8: return getAutos();
            case 9: return getDrivers();
            case 10: return getAge();
            case 11: return getIncome();
            case 12: return getAgeUnder40();
            case 13: return getAge40to59();
            case 14: return getAge60to79();
            case 15: return getAge60plus();
            case 16: return getPersonIsWorker();
            case 17: return getPersonIsNonWorkingAdult();
            case 18: return getPersonIsMale();
            case 19: return getPreDrivingChildInHh();
            case 20: return getNumFemalesInHh();
            case 23: return getFreeParking();
            case 24: return getHomemaker();
            case 25: return getWorkers();
            case 26: return getSize();
            case 27: return getGender();
            case 28: return getChildunder16();
            case 29: return getChild16plus();
            case 30: return getTourCategoryJoint();
            case 31: return getTourPrimaryPurposeEscort();
            case 32: return getTourPrimaryPurposeShopping();
            case 33: return getTourPrimaryPurposeEatOut();
            case 34: return getTourPrimaryPurposeSocial();
            case 35: return getTourPrimaryPurposeOthDiscr();
            case 36: return getNumberOfParticipantsInJointTour();
            case 37: return getWorkTourModeIsSOV();
            case 38: return getWorkTourModeIsBike();
            case 39: return getSubtourType();
            case 40: return getTourDepartsAfter4pm();
            case 41: return getTourArrivesAfter7pm();
            case 42: return getValueOfTime();
            
            case 43: return getOriginBHHDen();
            case 44: return getDestinationBHHDen();
            case 45: return getOriginBEmpDen();
            case 46: return getDestinationBEmpDen();
            case 47: return getOriginPef();
            case 48: return getDestinationPef();
        
            case 49: return getPersonIsPartTimeWorker();
            case 50: return getNumChildrenUnder19();
            case 51: return getNumPreschool();
            case 52: return getNumPersons6to15();
            case 53: return getWorkTourMode();
            case 54: return getNumChildren();
            case 55: return getHouseholdMaxValueOfTime();
            
            case 56: return getOrigRing();
            case 57: return getDestRing();
            
            case 58: return getOrigParkCost();
            case 59: return getDestParkCost();
            
            case 60: return getOmazOtaz();
            case 61: return getDmazDtaz();
                        
            case 70: return getGenCostWT_In();
            case 71: return getGenCostDL_In();
            case 72: return getGenCostDP_In();
            case 73: return getOtapWT_In();
            case 74: return getDtapWT_In();
            case 75: return getOtapDL_In();
            case 76: return getDtapDL_In();
            case 77: return getOtapDP_In();
            case 78: return getDtapDP_In();
            
            case 80: return getGenCostWT_Out();
            case 81: return getGenCostDL_Out();
            case 82: return getGenCostDP_Out();
            case 83: return getOtapWT_Out();
            case 84: return getDtapWT_Out();
            case 85: return getOtapDL_Out();
            case 86: return getDtapDL_Out();
            case 87: return getOtapDP_Out();
            case 88: return getDtapDP_Out();
            
            case 90: return getOMazDMazDistance();
            
            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
        
    }


}