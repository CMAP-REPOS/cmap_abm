package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.DestChoiceDMU;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.TazDataIf;


public class CmapDestChoiceDMU extends DestChoiceDMU {

    private int[] zoneTableRow;
    private int[] areaType;
    private int[] district;
    private int[] county;
    private int[] gradeschool;
    private int[] highschool;

    private int workLogsumIndex;
    private int schoolLogsumIndex;
    private int nonManLogsumIndex;
    
    TazDataIf tazDataManager;
    
    public CmapDestChoiceDMU( TazDataIf tazDataManager, ModelStructure modelStructure ) {
        super( tazDataManager, modelStructure );
        setup( tazDataManager );
        setupMethodIndexMap();
    }

    private void setup( TazDataIf tazDataManager) {
        zoneTableRow = tazDataManager.getZoneTableRowArray();

        // the zone table columns below returned us 0-based indexing
        areaType = tazDataManager.getZonalAreaType();
        district = tazDataManager.getZonalDistrict();
        county = tazDataManager.getZonalCounty();
        gradeschool = tazDataManager.getZonalHighSchool();
        highschool = tazDataManager.getZonalGradeSchool();

        workLogsumIndex = modelStructure.getPeriodCombinationIndex( modelStructure.getWorkLocationDefaultDepartPeriod(), modelStructure.getWorkLocationDefaultArrivePeriod() );
        schoolLogsumIndex = modelStructure.getPeriodCombinationIndex( modelStructure.getSchoolLocationDefaultDepartPeriod(), modelStructure.getSchoolLocationDefaultArrivePeriod() );
        nonManLogsumIndex = modelStructure.getPeriodCombinationIndex( modelStructure.getNonMandatoryLocationDefaultDepartPeriod(), modelStructure.getNonMandatoryLocationDefaultArrivePeriod() );
        
        this.tazDataManager = tazDataManager;
    }


    protected int getIncome() {
        return hh.getIncomeSegment();
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getOrigAreaType", 0 );
        methodIndexMap.put( "getOrigDistrict", 1 );
        methodIndexMap.put( "getOriginCounty", 2 );
        methodIndexMap.put( "getDestAreaTypeAlt", 3 );
        methodIndexMap.put( "getDestDistrictAlt", 4 );
        methodIndexMap.put( "getDestCountyAlt", 5 );
        
        methodIndexMap.put( "getLnWorkOcc1DcSizeAlt", 6 );
        methodIndexMap.put( "getLnWorkOcc2DcSizeAlt", 7 );
        methodIndexMap.put( "getLnWorkOcc3DcSizeAlt", 8 );
        methodIndexMap.put( "getLnWorkOcc4DcSizeAlt", 9 );
        methodIndexMap.put( "getLnWorkOcc5DcSizeAlt", 10 );
        methodIndexMap.put( "getLnWorkOcc6DcSizeAlt", 11 );
        methodIndexMap.put( "getLnWorkOcc7DcSizeAlt", 12 );
        methodIndexMap.put( "getLnWorkOcc8DcSizeAlt", 13 );
        methodIndexMap.put( "getLnWorkOcc9DcSizeAlt", 14 );
        methodIndexMap.put( "getLnWorkOcc10DcSizeAlt", 15 );
        methodIndexMap.put( "getLnWorkOcc11DcSizeAlt", 16);
        methodIndexMap.put( "getLnWorkOcc12DcSizeAlt", 17 );
        
        methodIndexMap.put( "getLnUnivDcSizeAlt", 18 );
        methodIndexMap.put( "getLnSchoolPreDcSizeAlt", 19 );
        methodIndexMap.put( "getLnSchoolGradeDcSizeAlt", 20 );
        methodIndexMap.put( "getLnSchoolHighDcSizeAlt", 21 );
        methodIndexMap.put( "getLnEscortDcSizeAlt", 22 );
        methodIndexMap.put( "getLnShoppingDcSizeAlt", 23 );
        methodIndexMap.put( "getLnEatOutDcSizeAlt", 24 );
        methodIndexMap.put( "getLnOthMaintDcSizeAlt", 25 );
        methodIndexMap.put( "getLnSocialDcSizeAlt", 26 );
        methodIndexMap.put( "getLnOthDiscrDcSizeAlt", 27 );
        methodIndexMap.put( "getLnAtWorkDcSizeAlt", 28 );
        methodIndexMap.put( "getPersonIsFullTimeWorker", 29 );
        methodIndexMap.put( "getSubtourType", 30 );
        methodIndexMap.put( "getDcSoaCorrectionsAlt", 31 );
        methodIndexMap.put( "getWorkLocationMcLogsumAlt", 32 );
        methodIndexMap.put( "getSchoolLocationMcLogsumAlt", 33 );
        methodIndexMap.put( "getNonMandatoryLocationMcLogsumAlt", 34 );
        methodIndexMap.put( "getZonalShortWalkAccessOrig", 35 );
        methodIndexMap.put( "getZonalShortWalkAccessDestAlt", 36 );
        methodIndexMap.put( "getIncome", 37 );
        methodIndexMap.put( "getAutos", 38 );
        methodIndexMap.put( "getWorkers", 39 );
        methodIndexMap.put( "getNumChildrenUnder16", 40 );
        methodIndexMap.put( "getNumChildrenUnder19", 41 );
        methodIndexMap.put( "getAge", 42 );
        methodIndexMap.put( "getFullTimeWorker", 43 );
        methodIndexMap.put( "getWorkTaz", 44 );
        methodIndexMap.put( "getWorkTourModeIsSOV", 45 );
        methodIndexMap.put( "getTourIsJoint", 46 );
        methodIndexMap.put( "getHighestEducationAttainment", 47 );
        methodIndexMap.put( "getWorkerOccupation", 48 );
        
        methodIndexMap.put( "getOriginGradeSchool", 49 );
        methodIndexMap.put( "getOriginHighSchool", 50 );
        methodIndexMap.put( "getDestGradeSchoolAlt", 51 );
        methodIndexMap.put( "getDestHighSchoolAlt", 52 );
        methodIndexMap.put( "getPersonIsFemale", 53 );
        methodIndexMap.put( "getPersonIsPartTimeWorker", 54 );
        methodIndexMap.put( "getNumberOfNonWorkingAdults", 55 );
        methodIndexMap.put( "getNumRetired", 56 );

        methodIndexMap.put( "getOMazDMazDistanceAlt", 60 );
        
        
    }
    
    public void setMcLogsum( int index, int zone, int subzone, double logsum ){
        modeChoiceLogsums[index][zone][subzone] = logsum;
    }

    
    public double getWorkLocationMcLogsumAlt ( int alt ) {
        int zone = altToZone[alt];
        int subZone = altToSubZone[alt];
        return getMcLogsumDestAlt( workLogsumIndex, zone, subZone );
    }
    
    public double getSchoolLocationMcLogsumAlt ( int alt ) {
        int zone = altToZone[alt];
        int subZone = altToSubZone[alt];
        return getMcLogsumDestAlt( schoolLogsumIndex, zone, subZone );
    }
    
    public double getNonMandatoryLocationMcLogsumAlt ( int alt ) {
        int zone = altToZone[alt];
        int subZone = altToSubZone[alt];
        return getMcLogsumDestAlt( nonManLogsumIndex, zone, subZone );
    }
    
    
    public int getOrigAreaType() {
        int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return areaType[index];
    }

    public int getOrigDistrict() {
        int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return district[index];
    }

    public int getOriginCounty() {
        int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return county[index];
    }
    
    public int getOriginGradeSchool() {
        int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return gradeschool[index];
    }
    
    public int getOriginHighSchool() {
        int index = zoneTableRow[dmuIndex.getOriginZone()] - 1;
        return highschool[index];
    }

    public int getDestAreaTypeAlt( int alt ) {
        int destZone = altToZone[alt];
        int index = zoneTableRow[destZone] - 1;
        return areaType[index];
    }

    public int getDestDistrictAlt( int alt ) {
        int destZone = altToZone[alt];
        int index = zoneTableRow[destZone] - 1;
        return district[index];
    }

    public int getDestCountyAlt( int alt ) {
        int destZone = altToZone[alt];
        int index = zoneTableRow[destZone] - 1;
        return county[index];
    }
    
    public int getDestGradeSchoolAlt( int alt ) {
        int destZone = altToZone[alt];
        int index = zoneTableRow[destZone] - 1;
        return gradeschool[index];
    }
    
    public int getDestHighSchoolAlt( int alt ) {
        int destZone = altToZone[alt];
        int index = zoneTableRow[destZone] - 1;
        return highschool[index];
    }

    public int getPersonIsFullTimeWorker() {
        return person.getPersonIsFullTimeWorker();
    }
    
    public int getPersonIsPartTimeWorker() {
        return person.getPersonIsPartTimeWorker();
    }

    public int getSubtourType() {
        if ( tour.getTourCategoryIsAtWork() )
            return tour.getTourPrimaryPurposeIndex();
        else
            return 0;
    }
    
    public int getHighestEducationAttainment() {
        return hh.getHighestEducationAttainment();
    }
    
    public int getWorkerOccupation() {
        return person.getPersonWorkerOccupation();
    }
    
    public int getNumberOfNonWorkingAdults() {
    	return hh.getNumberOfNonWorkingAdults();
    }
    
    public int getNumRetired() {
    	return hh.getNumRetired();
    }
    
    public double getOMazDMazDistanceAlt ( int alt ) {
    	int destZone = altToZone[alt];
        return tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), destZone);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getOrigAreaType();
            case 1: return getOrigDistrict();
            case 2: return getOriginCounty();
            case 3: return getDestAreaTypeAlt( arrayIndex );
            case 4: return getDestDistrictAlt( arrayIndex );
            case 5: return getDestCountyAlt( arrayIndex );
            
            case 6: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ1" );
            case 7: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ2" );
            case 8: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ3" );
            case 9: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ4" );
            case 10: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ5" );
            case 11: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ6" );
            case 12: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ7" );
            case 13: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ8" );
            case 14: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ9" );
            case 15: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ10" );
            case 16: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ11" );
            case 17: return getLnDcSizeForPurposeAlt( arrayIndex, "work_occ12" );
            
            case 18: return getLnDcSizeForPurposeAlt( arrayIndex, "university" );
            case 19: return getLnDcSizeForPurposeAlt( arrayIndex, "school_pre" );
            case 20: return getLnDcSizeForPurposeAlt( arrayIndex, "school_grade" );
            case 21: return getLnDcSizeForPurposeAlt( arrayIndex, "school_high" );
            case 22: return getLnDcSizeForPurposeAlt( arrayIndex, "escort" );
            case 23: return getLnDcSizeForPurposeAlt( arrayIndex, "shop" );
            case 24: return getLnDcSizeForPurposeAlt( arrayIndex, "eatingOut" );
            case 25: return getLnDcSizeForPurposeAlt( arrayIndex, "maintenance" );
            case 26: return getLnDcSizeForPurposeAlt( arrayIndex, "visiting" );
            case 27: return getLnDcSizeForPurposeAlt( arrayIndex, "discretionary" );
            case 28: return getLnDcSizeForPurposeAlt( arrayIndex, "work-based" );
            case 29: return getPersonIsFullTimeWorker();
            case 30: return getSubtourType();
            case 31: return getDcSoaCorrectionsAlt( arrayIndex );
            case 32: return getWorkLocationMcLogsumAlt( arrayIndex );
            case 33: return getSchoolLocationMcLogsumAlt( arrayIndex );
            case 34: return getNonMandatoryLocationMcLogsumAlt( arrayIndex );
            case 35: return getZonalShortWalkAccessOrig();
            case 36: return getZonalShortWalkAccessDestAlt( arrayIndex );
            case 37: return getIncome();
            case 38: return getAutos();
            case 39: return getWorkers();
            case 40: return getNumChildrenUnder16();
            case 41: return getNumChildrenUnder19();
            case 42: return getAge();
            case 43: return getFullTimeWorker();
            case 44: return getWorkTaz();
            case 45: return getWorkTourModeIsSOV();
            case 46: return getTourIsJoint();
            case 47: return getHighestEducationAttainment();
            case 48: return getWorkerOccupation();
            
            case 49: return getOriginGradeSchool();
            case 50: return getOriginHighSchool();
            case 51: return getDestGradeSchoolAlt( arrayIndex );
            case 52: return getDestHighSchoolAlt( arrayIndex );
            case 53: return getPersonIsFemale();
            case 54: return getPersonIsPartTimeWorker();
            case 55: return getNumberOfNonWorkingAdults();
            case 56: return getNumRetired();
            
            case 60: return getOMazDMazDistanceAlt( arrayIndex );

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
        
    }


}