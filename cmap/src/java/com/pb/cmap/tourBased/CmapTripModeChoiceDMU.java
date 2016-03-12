package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.cmap.tourBased.CmapModelStructure;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TripModeChoiceDMU;


public class CmapTripModeChoiceDMU extends TripModeChoiceDMU {

	CmapModelStructure modelStructure;
    int[] zoneTableRow;
    int[] areaType;
    float[] prkCost;

    int[] tazForMaz;
    	
	TazDataIf tazDataManager;
	
    public CmapTripModeChoiceDMU(TazDataIf tazDataManager, CmapModelStructure modelStructure){
        super(tazDataManager, modelStructure );
        this.modelStructure = modelStructure;
        setup( tazDataManager );
        setupMethodIndexMap();
    }
    

    private void setup( TazDataIf tazDataManager ) {

        zoneTableRow = tazDataManager.getZoneTableRowArray();

        // the zone table columns below returned use 0-based indexing
        areaType = tazDataManager.getZonalAreaType();
        
        tazForMaz = tazDataManager.getTazsForMazs();
        
        this.tazDataManager = tazDataManager;
        
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

    public int getHhSize() {
        return hh.getHhSize();
    }
    
    public int getAutos() {
        return hh.getAutoOwnershipModelResult();
    }

    public int getDrivers() {
        return hh.getDrivers();
    }

    public int getAge() {
        return person.getAge();
    }

    public int getIncome() {
        return hh.getIncomeSegment();
    }


    public int getAgeUnder40() {
        if ( person.getAge() < 40 )
            return 1;
        else
            return 0;
    }

    public int getAge40to59() {
        if ( person.getAge() >= 40 && person.getAge() < 60 )
            return 1;
        else
            return 0;
    }

    public int getAge60to79() {
        if ( person.getAge() >= 60 && person.getAge() < 79 )
            return 1;
        else
            return 0;
    }

    public int getAge60plus() {
        if ( person.getAge() >= 60 )
            return 1;
        else
            return 0;
    }

    public int getPersonIsWorker() {
        return person.getPersonIsWorker();
    }

    public int getPersonIsNonWorkingAdult() {
        if ( person.getPersonIsNonWorkingAdultUnder65() == 1 )
            return 1;
        else
            return 0;
    }

    public int getPersonIsMale() {
        if ( person.getPersonIsMale() == 1 )
            return 1;
        else
            return 0;
    }


    public int getPreDrivingChildInHh() {
        if ( hh.getNumChildrenUnder16() > 0 )
            return 1;
        else
            return 0;
    }

    public int getNumFemalesInHh() {
        int count = 0;
        PersonIf[] persons = hh.getPersons();
        for (int i=1; i < persons.length; i++){
            if ( persons[i].getPersonIsFemale() == 1 )
                count++;
        }
        return count;
    }

    public int getFreeParking() {
        return person.getFreeParkingAvailableResult();
    }



    public int getHomemaker() {
        return person.getHomemaker();
    }

    public int getWorkers() {
        return hh.getWorkers();
    }

    public int getOutboundStops() {
        return tour.getNumOutboundStops();
    }

    public int getInboundStops() {
        return tour.getNumInboundStops();
    }

    public int getSize() {
        return hh.getSize();
    }

    public int getGender() {
        return person.getGender();
    }

    public int getChildunder16() {
        return hh.getChildunder16();
    }

    public int getChild16plus() {
        return hh.getChild16plus();
    }


    public int getTourPurposeEscort() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getEscortPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPurposeShopping() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getShoppingPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPurposeEatOut() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getEatOutPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPurposeSocial() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getSocialPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getTourPurposeOthDiscr() {
        if ( tour.getTourPrimaryPurpose().equalsIgnoreCase( modelStructure.getOthDiscrPurposeName() ) )
            return 1;
        else
            return 0;
    }

    public int getInbound() {
    	return stop.isInboundStop() ? 1 : 0;
    }
    
    public int getTodOut() {
        return modelStructure.getTod( stop.getStopPeriod() );
    }
    
    public int getTourTodOut() {
        return modelStructure.getTod(stop.getTourDepartPeriod() );
    }

    public int getTourTodIn() {
        return modelStructure.getTod( tour.getTourArrivePeriod() );
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
        methodIndexMap.put( "getTourTodIn", 1 );
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
        methodIndexMap.put( "getOutboundStops", 26 );
        methodIndexMap.put( "getInboundStops", 27 );
        methodIndexMap.put( "getSize", 28 );
        methodIndexMap.put( "getGender", 29 );
        methodIndexMap.put( "getChildunder16", 30 );
        methodIndexMap.put( "getChild16plus", 31 );
        methodIndexMap.put( "getTourCategoryJoint", 32 );
        methodIndexMap.put( "getTourPurposeEscort", 33 );
        methodIndexMap.put( "getTourPurposeShopping", 34 );
        methodIndexMap.put( "getTourPurposeEatOut", 35 );
        methodIndexMap.put( "getTourPurposeSocial", 36 );
        methodIndexMap.put( "getTourPurposeOthDiscr", 37 );
        methodIndexMap.put( "getNumberOfParticipantsInJointTour", 38 );
        methodIndexMap.put( "getWorkTourModeIsSOV", 39 );
        methodIndexMap.put( "getWorkTourModeIsBike", 40 );
        methodIndexMap.put( "getTourMode", 41 );
        methodIndexMap.put( "getSubtourType", 42 );
        methodIndexMap.put( "getOrigType", 43 );
        methodIndexMap.put( "getDestType", 44 );
        methodIndexMap.put( "getOrigParkRate", 45 );
        methodIndexMap.put( "getIntStopParkRate", 46 );
        methodIndexMap.put( "getPrimDestParkRate", 47 );
        methodIndexMap.put( "getInbound", 48 );
        
        methodIndexMap.put( "getOmazOtaz", 60 );
        methodIndexMap.put( "getDmazDtaz", 61 );
        
        methodIndexMap.put( "getGenCostWT", 70 );
        methodIndexMap.put( "getGenCostKNR", 71 );
        methodIndexMap.put( "getGenCostPNR", 72 );
        methodIndexMap.put( "getOtapWT", 73 );
        methodIndexMap.put( "getDtapWT", 74 );
        methodIndexMap.put( "getOtapKNR", 75 );
        methodIndexMap.put( "getDtapKNR", 76 );
        methodIndexMap.put( "getOtapPNR", 77 );
        methodIndexMap.put( "getDtapPNR", 78 );
        
        methodIndexMap.put( "getOMazDMazDistance", 88 );
    }
    



    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getTodOut();
            case 1: return getTourTodIn();
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
            case 26: return getOutboundStops();
            case 27: return getInboundStops();
            case 28: return getSize();
            case 29: return getGender();
            case 30: return getChildunder16();
            case 31: return getChild16plus();
            case 32: return getTourCategoryJoint();
            case 33: return getTourPurposeEscort();
            case 34: return getTourPurposeShopping();
            case 35: return getTourPurposeEatOut();
            case 36: return getTourPurposeSocial();
            case 37: return getTourPurposeOthDiscr();
            case 38: return getNumberOfParticipantsInJointTour();
            case 39: return getWorkTourModeIsSOV();
            case 40: return getWorkTourModeIsBike();
            case 41: return getTourMode();
            case 42: return getSubtourType();
            case 43: return getOrigType();
            case 44: return getDestType();
            case 45: return getOrigParkRate();
            case 46: return getIntStopParkRate();
            case 47: return getPrimDestParkRate();
            case 48: return getInbound();
            
            case 60: return getOmazOtaz();
            case 61: return getDmazDtaz();
                        
            case 70: return getGenCostWT();
            case 71: return getGenCostKNR();
            case 72: return getGenCostPNR();
            case 73: return getOtapWT();
            case 74: return getDtapWT();
            case 75: return getOtapKNR();
            case 76: return getDtapKNR();
            case 77: return getOtapPNR();
            case 78: return getDtapPNR();
            
            case 88: return getOMazDMazDistance();

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
        
    }

}