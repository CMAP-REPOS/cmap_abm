package com.pb.models.ctrampIf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * Holds the tour purpose list as well as the market segments for each tour.
 * 
 * @author D. Ory
 * 
 */
public abstract class ModelStructure implements Serializable
{


    public static final String[] DC_SIZE_AREA_TYPE_BASED_SEGMENTS  = {"CBD", "URBAN", "SUBURBAN", "RURAL" };

    public static final String MANDATORY_CATEGORY                = "MANDATORY";
    public static final String JOINT_NON_MANDATORY_CATEGORY      = "JOINT_NON_MANDATORY";
    public static final String INDIVIDUAL_NON_MANDATORY_CATEGORY = "INDIVIDUAL_NON_MANDATORY";
    public static final String AT_WORK_CATEGORY                  = "AT_WORK";
    public static final String[] TOUR_CATEGORY_LABELS = { "", MANDATORY_CATEGORY, JOINT_NON_MANDATORY_CATEGORY, INDIVIDUAL_NON_MANDATORY_CATEGORY, AT_WORK_CATEGORY };

    //public final String[] WORK_PURPOSE_SEGMENT_NAMES = { "low", "med", "high", "very high", "part time" };
    public final String[] WORK_PURPOSE_SEGMENT_NAMES = { "OCC1", "OCC2", "OCC3", "OCC4", "OCC5", "OCC6", "OCC7", "OCC8", "OCC9", "OCC10", "OCC11", "OCC12" };
    
    public static final int MANDATORY_CATEGORY_INDEX = 1;
    public static final int JOINT_NON_MANDATORY_CATEGORY_INDEX = 2;
    public static final int INDIVIDUAL_NON_MANDATORY_CATEGORY_INDEX = 3;
    public static final int AT_WORK_CATEGORY_INDEX = 4;
    
    public static final String                          MANDATORY_PATTERN                 = "M";
    public static final String                          NONMANDATORY_PATTERN              = "N";
    public static final String                          HOME_PATTERN                      = "H";

    public static final int                             FIRST_DEPART_HOUR                 = 4;
    public static final int                             LAST_DEPART_HOUR                  = 24;
    public static final int                             FIRST_TOD_INTERVAL_HOUR           = 430;
    public static final int                             LAST_TOD_INTERVAL_HOUR            = 2400;
    protected String[]                                  TIME_PERIOD_LABELS;
    
    private HashMap<Integer,Integer>                    indexTimePeriodMap;
    private HashMap<Integer,Integer>                    timePeriodIndexMap;

    protected int[] incomeSegmentDollarLimits;                    // upper limits of income ranges; 0-based; length of array gives the number of income segments;
    
    protected HashMap<String,Integer> segmentedPurposeIndexMap;   // segment names/indices map for purposes assigned to tours
    protected HashMap<Integer,String> segmentedIndexPurposeMap;   // segment indices/names map for purposes assigned to tours
    
    private HashMap<String, Integer>                    primaryTourPurposeNameIndexMap    = new HashMap<String, Integer>();
    private HashMap<Integer, String>                    indexPrimaryTourPurposeNameMap    = new HashMap<Integer, String>();

    public static final String                          WORK_PRIMARY_PURPOSE_NAME         = "Work";
    public static final String                          UNIVERSITY_PRIMARY_PURPOSE_NAME   = "University";
    public static final String                          SCHOOL_PRIMARY_PURPOSE_NAME       = "School";
    public static final String                          ESCORT_PRIMARY_PURPOSE_NAME       = "Escort";
    public static final String                          SHOPPING_PRIMARY_PURPOSE_NAME     = "Shop";
    public static final String                          OTH_MAINT_PRIMARY_PURPOSE_NAME    = "Maintenance";
    public static final String                          EAT_OUT_PRIMARY_PURPOSE_NAME      = "EatingOut";
    public static final String                          VISITING_PRIMARY_PURPOSE_NAME     = "Visiting";
    public static final String                          OTH_DISCR_PRIMARY_PURPOSE_NAME    = "Discretionary";
    public static final String                          WORK_BASED_PRIMARY_PURPOSE_NAME   = "Work-Based";

    public static final int                             WORK_PRIMARY_PURPOSE_INDEX        = 1;
    public static final int                             UNIVERSITY_PRIMARY_PURPOSE_INDEX  = 2;
    public static final int                             SCHOOL_PRIMARY_PURPOSE_INDEX      = 3;
    public static final int                             ESCORT_PRIMARY_PURPOSE_INDEX      = 4;
    public static final int                             SHOPPING_PRIMARY_PURPOSE_INDEX    = 5;
    public static final int                             OTH_MAINT_PRIMARY_PURPOSE_INDEX   = 6;
    public static final int                             EAT_OUT_PRIMARY_PURPOSE_INDEX     = 7;
    public static final int                             VISITING_PRIMARY_PURPOSE_INDEX    = 8;
    public static final int                             OTH_DISCR_PRIMARY_PURPOSE_INDEX   = 9;
    public static final int                             WORK_BASED_PRIMARY_PURPOSE_INDEX  = 10;
    public static final int                             NUM_PRIMARY_PURPOSES              = 10;

    public static final int                             WORKS_AT_HOME_ALTERNATUVE_INDEX  = 2;
    public static final int                             WORKS_AT_HOME_LOCATION_INDICATOR = 99999;
    public static final int                             NOT_ENROLLED_SEGMENT_INDEX       = 88888;
    
    public static final int WORK_PURPOSE_INDEX = 1;
    public static final int UNIVERSITY_PURPOSE_INDEX = 2;
    public static final int SCHOOL_PURPOSE_INDEX = 3;
    public static final int ESCORT_PURPOSE_INDEX = 4;
    public static final int SHOPPING_PURPOSE_INDEX = 5;
    public static final int OTH_MAINT_PURPOSE_INDEX = 6;
    public static final int EAT_OUT_PURPOSE_INDEX = 7;
    public static final int SOCIAL_PURPOSE_INDEX = 8;
    public static final int OTH_DISCR_PURPOSE_INDEX = 9;
    public static final int AT_WORK_PURPOSE_INDEX = 10;
    
    public static final String WORK_PURPOSE_NAME = "work";
    public static final String UNIVERSITY_PURPOSE_NAME = "university";
    public static final String SCHOOL_PURPOSE_NAME = "school";
    public static final String ESCORT_PURPOSE_NAME = "escort";
    public static final String SHOPPING_PURPOSE_NAME = "shop";
    public static final String EAT_OUT_PURPOSE_NAME = "eatingout";
    public static final String OTH_MAINT_PURPOSE_NAME = "maintenance";
    public static final String SOCIAL_PURPOSE_NAME = "visiting";
    public static final String OTH_DISCR_PURPOSE_NAME = "discretionary";
    public static final String AT_WORK_PURPOSE_NAME = "work-based";
    public static final String AT_WORK_EAT_PURPOSE_NAME = "eat";
    public static final String AT_WORK_BUSINESS_PURPOSE_NAME = "business";
    public static final String AT_WORK_MAINT_PURPOSE_NAME = "maint";

    public int                                          AT_WORK_PURPOSE_INDEX_EAT;
    public int                                          AT_WORK_PURPOSE_INDEX_BUSINESS;
    public int                                          AT_WORK_PURPOSE_INDEX_MAINT;

    public String[]                                     ESCORT_SEGMENT_NAMES;
    public String[]                                     AT_WORK_SEGMENT_NAMES;

    // TODO: Determine which of the following can be eliminated
    protected HashMap<String, Integer>                  dcSoaUecIndexMap;
    protected HashMap<String, Integer>                  dcUecIndexMap;
    protected HashMap<String, Integer>                  tourModeChoiceUecIndexMap;

    protected HashMap<String, String>                   dcSizeDcModelPurposeMap;
    protected HashMap<String, String>                   dcModelDcSizePurposeMap;

    protected HashMap<String, Integer>                  dcModelPurposeIndexMap;                                            // segments
    // for
    // which
    // dc
    // soa alternative models
    // are applied
    protected HashMap<Integer, String>                  dcModelIndexPurposeMap;                                            // segments
    // for
    // which
    // dc
    // soa alternative models
    // are applied

    protected HashMap<String, Integer>                  dcSizeSegmentIndexMap;                                             // segments
    // for
    // which
    // separate dc size
    // coefficients are
    // specified
    protected HashMap<Integer, String>                  dcSizeIndexSegmentMap;
    protected HashMap<String, Integer>                  dcSizeArrayPurposeIndexMap;                                        // segments
    // for
    // which
    // dc
    // size terms are stored
    protected HashMap<Integer, String>                  dcSizeArrayIndexPurposeMap;
    protected HashMap<String, HashMap<String, Integer>> dcSizePurposeSegmentMap;

    private String                                      dcSizeCoeffPurposeFieldName       = "purpose";
    private String                                      dcSizeCoeffSegmentFieldName       = "segment";

    // TODO meld with what jim is doing on this front
    protected String[]                                  mandatoryDcModelPurposeNames;
    protected String[]                                  jointDcModelPurposeNames;
    protected String[]                                  nonMandatoryDcModelPurposeNames;
    protected String[]                                  atWorkDcModelPurposeNames;

    protected String                                    workPurposeName;
    protected String                                    universityPurposeName;
    protected String                                    schoolPurposeName;

    protected String[]                                  workPurposeSegmentNames;
    protected String[]                                  universityPurposeSegmentNames;
    protected String[]                                  schoolPurposeSegmentNames;

    protected HashMap<String, Integer>                  stopFreqUecIndexMap;
    protected HashMap<String, Integer>                  stopLocUecIndexMap;
    protected HashMap<String, Integer>                  tripModeChoiceUecIndexMap;

    protected String[]                                  jtfAltLabels;
    protected String[]                                  awfAltLabels;
    
   
    /**
     * Assume name of the columns in the destination size coefficients file that
     * contain the purpose strings is "purpose" and the column that contains the
     * segment strings is "segment"
     */
    public ModelStructure()
    {

        dcModelPurposeIndexMap = new HashMap<String, Integer>();
        dcModelIndexPurposeMap = new HashMap<Integer, String>();
        dcSoaUecIndexMap = new HashMap<String, Integer>();
        dcUecIndexMap = new HashMap<String, Integer>();
        tourModeChoiceUecIndexMap = new HashMap<String, Integer>();
        stopFreqUecIndexMap = new HashMap<String, Integer>();
        stopLocUecIndexMap = new HashMap<String, Integer>();
        tripModeChoiceUecIndexMap = new HashMap<String, Integer>();

        segmentedPurposeIndexMap = new HashMap<String,Integer>();
        segmentedIndexPurposeMap = new HashMap<Integer,String>();

        
        // create a mapping between primary purpose names and purpose indices
        createPrimaryPurposeMappings();

        createIndexTimePeriodMap();
        
    }


    // a derived class must implement these methods to retrieve purpose names for
    // various personTypes making mandatory tours.
    abstract public String getUniversityPurpose();

    abstract public String getSchoolPurpose(int age);

    abstract public boolean getTourModeIsSov(int tourMode);

    abstract public boolean getTourModeIsSovOrHov(int tourMode);

    abstract public boolean getTourModeIsS2(int tourMode);

    abstract public boolean getTourModeIsS3(int tourMode);

    abstract public boolean getTourModeIsHov(int tourMode);

    abstract public boolean getTourModeIsNonMotorized(int tourMode);

    abstract public boolean getTourModeIsBike(int tourMode);

    abstract public boolean getTourModeIsWalk(int tourMode);

    abstract public boolean getTourModeIsWalkLocal(int tourMode);

    abstract public boolean getTourModeIsWalkPremium(int tourMode);

    abstract public boolean getTourModeIsTransit(int tourMode);

    abstract public boolean getTourModeIsDriveTransit(int tourMode);

    abstract public boolean getTourModeIsPnr(int tourMode);

    abstract public boolean getTourModeIsKnr(int tourMode);

    abstract public boolean getTourModeIsSchoolBus(int tourMode);

    abstract public boolean getTripModeIsSovOrHov(int tripMode);

    abstract public double[][] getCdap6PlusProps();

    abstract public int getWorkLocationDefaultDepartPeriod();
    abstract public int getWorkLocationDefaultArrivePeriod();
    abstract public int getSchoolLocationDefaultDepartPeriod();
    abstract public int getSchoolLocationDefaultArrivePeriod();
    abstract public int getUniversityLocationDefaultDepartPeriod();
    abstract public int getUniversityLocationDefaultArrivePeriod();
    abstract public int getNonMandatoryLocationDefaultDepartPeriod();
    abstract public int getNonMandatoryLocationDefaultArrivePeriod();
    
    abstract public String getPeriodLabel(int period);

    abstract public int getIntervalFor4Pm();
    abstract public int getIntervalFor7Pm();
    
    abstract public int getPeriodIndex(int period);

    abstract public int[] getPeriodCombinationIndices();

    abstract public int getPeriodCombinationIndex(int startPeriod, int endPeriod);

    abstract public void setIncomeSegments( int[] incomeBreakPoints );
    
    abstract public int getIncomeSegment( int hhIncomeInDollars );

    abstract public int getMaxTourModeIndex();

    abstract public HashMap<String, HashMap<String, Integer>> getDcSizePurposeSegmentMap();
    
    abstract public String[] getJtfAltLabels();
    abstract public void setJtfAltLabels( String[] labels );        

    private void createPrimaryPurposeMappings()
    {

        primaryTourPurposeNameIndexMap.put(WORK_PRIMARY_PURPOSE_NAME.toLowerCase(), WORK_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(WORK_PRIMARY_PURPOSE_INDEX, WORK_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(UNIVERSITY_PRIMARY_PURPOSE_NAME.toLowerCase(), UNIVERSITY_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(UNIVERSITY_PRIMARY_PURPOSE_INDEX, UNIVERSITY_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(SCHOOL_PRIMARY_PURPOSE_NAME.toLowerCase(), SCHOOL_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(SCHOOL_PRIMARY_PURPOSE_INDEX, SCHOOL_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(ESCORT_PRIMARY_PURPOSE_NAME.toLowerCase(), ESCORT_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(ESCORT_PRIMARY_PURPOSE_INDEX, ESCORT_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(SHOPPING_PRIMARY_PURPOSE_NAME.toLowerCase(), SHOPPING_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(SHOPPING_PRIMARY_PURPOSE_INDEX, SHOPPING_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(OTH_MAINT_PRIMARY_PURPOSE_NAME.toLowerCase(), OTH_MAINT_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(OTH_MAINT_PRIMARY_PURPOSE_INDEX, OTH_MAINT_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(EAT_OUT_PRIMARY_PURPOSE_NAME.toLowerCase(), EAT_OUT_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(EAT_OUT_PRIMARY_PURPOSE_INDEX, EAT_OUT_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(VISITING_PRIMARY_PURPOSE_NAME.toLowerCase(), VISITING_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(VISITING_PRIMARY_PURPOSE_INDEX, VISITING_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(OTH_DISCR_PRIMARY_PURPOSE_NAME.toLowerCase(), OTH_DISCR_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(OTH_DISCR_PRIMARY_PURPOSE_INDEX, OTH_DISCR_PRIMARY_PURPOSE_NAME.toLowerCase());
        primaryTourPurposeNameIndexMap.put(WORK_BASED_PRIMARY_PURPOSE_NAME.toLowerCase(), WORK_BASED_PRIMARY_PURPOSE_INDEX);
        indexPrimaryTourPurposeNameMap.put(WORK_BASED_PRIMARY_PURPOSE_INDEX, WORK_BASED_PRIMARY_PURPOSE_NAME.toLowerCase());

    }

    // return the primary purpose string for the purposeName argument
    public String getPrimaryPurposeString( String purposeName ) {
        return getPurposeString( purposeName );
    }
    
    public int getPrimaryPurposeIndex (String primaryPurposeName ){
        return primaryTourPurposeNameIndexMap.get( primaryPurposeName ); 
    }

    // determine if the purpose string is a work purpose
    private String getPurposeString( String purposeName ) {
        String purpose = "";
        int index = purposeName.indexOf( '_' );

        // if there's no '_', the purposeName has no segments.
        if ( index < 0 ) {
            purpose = purposeName;
        }
        // if there is a '_', the purpose is the substring preceding it.
        else {
            purpose = purposeName.substring(0, index);
        }
        return purpose;
    }

    public String getPrimaryPurposeForIndex( int index ) {
        return indexPrimaryTourPurposeNameMap.get( index );
    }
    
    
    /**
     * @return the HashMap<String,Integer> object that maps primary tour purpose
     *         names common to all CTRAMP implementations to indices (1-10).
     */
    public HashMap<String, Integer> getPrimaryPurposeNameIndexMap()
    {
        return primaryTourPurposeNameIndexMap;
    }

    /**
     * @return the HashMap<Integer,String> object that maps indices (1-10) to primary
     *         tour purpose names common to all CTRAMP implementations.
     */
    public HashMap<Integer, String> getIndexPrimaryPurposeNameMap()
    {
        return indexPrimaryTourPurposeNameMap;
    }

    /**
     * @param purposeKey is the "purpose" name used as a key for the map to get the
     *            associated UEC tab number.
     * @return the tab number of the UEC control file for the purpose
     */
    public int getSoaUecIndexForPurpose(String purposeKey)
    {
        return dcSoaUecIndexMap.get(purposeKey);
    }

    /**
     * @param purposeKey is the "purpose" name used as a key for the map to get the
     *            associated UEC tab number.
     * @return the tab number of the UEC control file for the purpose
     */
    public int getDcUecIndexForPurpose(String purposeKey)
    {
        return dcUecIndexMap.get(purposeKey);
    }

    /**
     * @param purposeKey is the "purpose" name used as a key for the map to get the
     *            associated UEC tab number.
     * @return the tab number of the UEC control file for the purpose
     */
    public int getTourModeChoiceUecIndexForPurpose(String purposeKey)
    {
        return tourModeChoiceUecIndexMap.get(purposeKey);
    }

    public String[] getDcModelPurposeList(String tourCategory)
    {
        if (tourCategory.equalsIgnoreCase(MANDATORY_CATEGORY)) return mandatoryDcModelPurposeNames;
        else if (tourCategory.equalsIgnoreCase(JOINT_NON_MANDATORY_CATEGORY)) return jointDcModelPurposeNames;
        else if (tourCategory.equalsIgnoreCase(INDIVIDUAL_NON_MANDATORY_CATEGORY)) return nonMandatoryDcModelPurposeNames;
        else if (tourCategory.equalsIgnoreCase(AT_WORK_CATEGORY)) return atWorkDcModelPurposeNames;
        else return null;
    }

    public String getDcSizeCoeffPurposeFieldName()
    {
        return dcSizeCoeffPurposeFieldName;
    }

    public String getDcSizeCoeffSegmentFieldName()
    {
        return this.dcSizeCoeffSegmentFieldName;
    }

    public String getAtWorkEatPurposeName()
    {
        return AT_WORK_EAT_PURPOSE_NAME;
    }

    public String[] getAtWorkSegmentNames()
    {
        return AT_WORK_SEGMENT_NAMES;
    }

    public String getAtWorkBusinessPurposeName()
    {
        return AT_WORK_BUSINESS_PURPOSE_NAME;
    }

    public String getAtWorkMaintPurposeName()
    {
        return AT_WORK_MAINT_PURPOSE_NAME;
    }

    public int getAtWorkEatPurposeIndex()
    {
        return AT_WORK_PURPOSE_INDEX_EAT;
    }

    public int getAtWorkBusinessPurposeIndex()
    {
        return AT_WORK_PURPOSE_INDEX_BUSINESS;
    }

    public int getAtWorkMaintPurposeIndex()
    {
        return AT_WORK_PURPOSE_INDEX_MAINT;
    }


    /*
     * // The following are used to navigate the dcSize array public int
     * getDcSizeArrayCategoryIndexOffset(String category){ if (
     * category.equalsIgnoreCase( MANDATORY_CATEGORY ) ) return
     * getDcSizeArrayMandatoryCategoryIndexOffset(); else if (
     * category.equalsIgnoreCase( JOINT_NON_MANDATORY_CATEGORY ) ) return
     * getDcSizeArrayJointCategoryIndexOffset(); else if ( category.equalsIgnoreCase(
     * INDIVIDUAL_NON_MANDATORY_CATEGORY ) ) return
     * getDcSizeArrayIndivNonManCategoryIndexOffset(); else if (
     * category.equalsIgnoreCase( AT_WORK_CATEGORY ) ) return
     * getDcSizeArrayAtWorkCategoryIndexOffset(); else return -1; } public int
     * getNumDcSizeArrayCategorySegments(String category){ if (
     * category.equalsIgnoreCase( MANDATORY_CATEGORY ) ) return
     * getNumDcSizeArrayMandatorySegments(); else if ( category.equalsIgnoreCase(
     * JOINT_NON_MANDATORY_CATEGORY ) ) return getNumDcSizeArrayJointSegments(); else
     * if ( category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) ) return
     * getNumDcSizeArrayIndivNonManSegments(); else if ( category.equalsIgnoreCase(
     * AT_WORK_CATEGORY ) ) return getNumDcSizeArrayAtWorkSegments(); else return -1;
     * } private int getDcSizeArrayMandatoryCategoryIndexOffset() { return 0; }
     * private int getNumDcSizeArrayMandatorySegments() { return
     * mandatoryDcModelPurposeNames.length; } private int
     * getDcSizeArrayJointCategoryIndexOffset() { return
     * mandatoryDcModelPurposeNames.length + ESCORT_SEGMENT_NAMES.length; } private
     * int getNumDcSizeArrayJointSegments() { return
     * nonMandatoryDcModelPurposeNames.length - ESCORT_SEGMENT_NAMES.length; }
     * private int getDcSizeArrayIndivNonManCategoryIndexOffset() { return
     * mandatoryDcModelPurposeNames.length; } private int
     * getNumDcSizeArrayIndivNonManSegments() { return
     * nonMandatoryDcModelPurposeNames.length; } private int
     * getDcSizeArrayAtWorkCategoryIndexOffset() { return
     * mandatoryDcModelPurposeNames.length + nonMandatoryDcModelPurposeNames.length;
     * } private int getNumDcSizeArrayAtWorkSegments() { return
     * dcSizeArrayIndexPurposeMap.size() - getDcSizeArrayAtWorkCategoryIndexOffset();
     * } // The following are used to navigate the dcSize segments calculated public
     * int getDcSizeSegmentCategoryIndexOffset(String category){ if (
     * category.equalsIgnoreCase( MANDATORY_CATEGORY ) ) return
     * getDcSizeSegmentMandatoryCategoryIndexOffset(); else if (
     * category.equalsIgnoreCase( JOINT_NON_MANDATORY_CATEGORY ) ) return
     * getDcSizeSegmentJointCategoryIndexOffset(); else if (
     * category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) ) return
     * getDcSizeSegmentIndivNonManCategoryIndexOffset(); else if (
     * category.equalsIgnoreCase( AT_WORK_CATEGORY ) ) return
     * getDcSizeSegmentAtWorkCategoryIndexOffset(); else return -1; } public int
     * getNumDcSizeSegmentCategorySegments(String category){ if (
     * category.equalsIgnoreCase( MANDATORY_CATEGORY ) ) return
     * getNumDcSizeSegmentMandatorySegments(); else if ( category.equalsIgnoreCase(
     * JOINT_NON_MANDATORY_CATEGORY ) ) return getNumDcSizeSegmentJointSegments();
     * else if ( category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) )
     * return getNumDcSizeSegmentIndivNonManSegments(); else if (
     * category.equalsIgnoreCase( AT_WORK_CATEGORY ) ) return
     * getNumDcSizeSegmentAtWorkSegments(); else return -1; } private int
     * getDcSizeSegmentMandatoryCategoryIndexOffset() { return 0; } private int
     * getNumDcSizeSegmentMandatorySegments() { return
     * mandatoryDcModelPurposeNames.length; } private int
     * getDcSizeSegmentJointCategoryIndexOffset() { return
     * mandatoryDcModelPurposeNames.length + ESCORT_SEGMENT_NAMES.length; } private
     * int getNumDcSizeSegmentJointSegments() { return
     * nonMandatoryDcModelPurposeNames.length - ESCORT_SEGMENT_NAMES.length; }
     * private int getDcSizeSegmentIndivNonManCategoryIndexOffset() { return
     * mandatoryDcModelPurposeNames.length; } private int
     * getNumDcSizeSegmentIndivNonManSegments() { return
     * nonMandatoryDcModelPurposeNames.length; } private int
     * getDcSizeSegmentAtWorkCategoryIndexOffset() { return
     * mandatoryDcModelPurposeNames.length + nonMandatoryDcModelPurposeNames.length;
     * } private int getNumDcSizeSegmentAtWorkSegments() { return
     * dcSizeIndexSegmentMap.size() - getDcSizeSegmentAtWorkCategoryIndexOffset(); }
     * public boolean isValidDcSizePurposeSegment ( String purposeName, String
     * segmentName ){ boolean returnValue = false; String purpKey =
     * purposeName.toLowerCase(); if ( dcSizePurposeSegmentMap.containsKey( purpKey )
     * ) { HashMap<String, Integer> segmentNamesMap = dcSizePurposeSegmentMap.get(
     * purpKey ); if ( segmentNamesMap.size() > 0 ) { String segKey =
     * segmentName.toLowerCase(); if ( segmentNamesMap.containsKey( segKey ) )
     * returnValue = true; } } return returnValue; } public int
     * getDcSizeArrayPurposeIndex( String purpose ) { return
     * dcSizeArrayPurposeIndexMap.get(purpose); } public String
     * getDcSizeArrayIndexPurpose( int index ) { return
     * dcSizeArrayIndexPurposeMap.get(index); } public int getDcSizeSegmentIndex(
     * String purpose ) { return dcSizeSegmentIndexMap.get(purpose); } public String
     * getDcSizeIndexSegment( int index ) { return dcSizeIndexSegmentMap.get(index);
     * } public String[] getDcSizeArrayPurposeStrings() { String[] names = new
     * String[dcSizeArrayIndexPurposeMap.size()]; for ( Integer i :
     * dcSizeArrayIndexPurposeMap.keySet() ) names[i] =
     * dcSizeArrayIndexPurposeMap.get(i); return names; } public String[]
     * getDcSizeSegmentStrings() { String[] names = new
     * String[dcSizeIndexSegmentMap.size()]; for ( Integer i :
     * dcSizeIndexSegmentMap.keySet() ) names[i] = dcSizeIndexSegmentMap.get(i);
     * return names; } public int getDcModelPurposeIndex( String purposeName ) {
     * return dcModelPurposeIndexMap.get( purposeName.toLowerCase() ); } public
     * String getDcModelIndexPurpose( int purposeIndex ) { return
     * dcModelIndexPurposeMap.get( purposeIndex ); } // determine if the purpose
     * string is a work purpose public boolean getDcModelPurposeIsWorkPurpose( String
     * purposeName ) { String purpose = getPurposeString( purposeName ); return
     * purpose.equalsIgnoreCase( workPurposeName ); } // determine if the purpose
     * string is a university purpose public boolean
     * getDcModelPurposeIsUniversityPurpose( String purposeName ) { String purpose =
     * getPurposeString( purposeName ); return purpose.equalsIgnoreCase(
     * universityPurposeName ); } // determine if the purpose string is a school
     * purpose public boolean getDcModelPurposeIsSchoolPurpose( String purposeName )
     * { String purpose = getPurposeString( purposeName ); return
     * purpose.equalsIgnoreCase( schoolPurposeName ); } // determine if the purpose
     * string is a work purpose private String getPurposeString( String purposeName )
     * { String purpose = ""; int index = purposeName.indexOf( '_' ); // if there's
     * no '_', the purposeName has no segments. if ( index < 0 ) { purpose =
     * purposeName; } // if there is a '_', the purpose is the substring preceding
     * it. else { purpose = purposeName.substring(0, index); } return purpose; }
     * public String getEscortPurposeName() { return ESCORT_PURPOSE_NAME; } public
     * String[] getEscortSegmentNames() { return ESCORT_SEGMENT_NAMES; } public
     * String getShoppingPurposeName() { return SHOPPING_PURPOSE_NAME; } public
     * String getEatOutPurposeName() { return EAT_OUT_PURPOSE_NAME; } public String
     * getOthMaintPurposeName() { return OTH_MAINT_PURPOSE_NAME; } public String
     * getSocialPurposeName() { return SOCIAL_PURPOSE_NAME; } public String
     * getOthDiscrPurposeName() { return OTH_DISCR_PURPOSE_NAME; } public int
     * getStopFrequencyModelIndex( String tourPurposeName ){ return
     * stopFreqUecIndexMap.get( tourPurposeName ); } public TreeSet<Integer>
     * getStopFreqModelSheetIndices() { TreeSet<Integer> set = new
     * TreeSet<Integer>(); for ( int el : stopFreqUecIndexMap.values() ) set.add( el
     * ); return set; } public int getStopLocationModelIndex( String stopPurposeName
     * ){ return stopLocUecIndexMap.get( stopPurposeName ); } public
     * Collection<Integer> getStopLocModelSheetIndices() { return
     * stopLocUecIndexMap.values(); } public int getTripModeChoiceModelIndex( String
     * stopPurposeName ){ return tripModeChoiceUecIndexMap.get( stopPurposeName ); }
     * public Collection<Integer> getTripModeChoiceModelSheetIndices() { return
     * tripModeChoiceUecIndexMap.values(); } public Set<String>
     * getTripModeChoiceModelPurposes() { return tripModeChoiceUecIndexMap.keySet();
     * } public int getDcSizeArrayIndexFromDcModelIndex( int dcModelIndex ){ String
     * dcModelPurposeString = dcModelIndexPurposeMap.get( dcModelIndex ); String
     * dcSizePurposeString = dcModelDcSizePurposeMap.get(dcModelPurposeString); int
     * dcSizeArrayIndex = getDcSizeArrayPurposeIndex( dcSizePurposeString ); return
     * dcSizeArrayIndex; } public String[] getJtfAltLabels() { return jtfAltLabels; }
     * public String[] getAwfAltLabels() { return awfAltLabels; }
     */
    
    public String getEscortPurposeName() {
        return ESCORT_PURPOSE_NAME;
    }

    public String[] getEscortSegmentNames() {
        return ESCORT_SEGMENT_NAMES;
    }

    public String getShoppingPurposeName() {
        return SHOPPING_PURPOSE_NAME;
    }

    public String getEatOutPurposeName() {
        return EAT_OUT_PURPOSE_NAME;
    }

    public String getOthMaintPurposeName() {
        return OTH_MAINT_PURPOSE_NAME;
    }

    public String getSocialPurposeName() {
        return SOCIAL_PURPOSE_NAME;
    }

    public String getOthDiscrPurposeName() {
        return OTH_DISCR_PURPOSE_NAME;
    }



    private void createIndexTimePeriodMap(){
        indexTimePeriodMap = new HashMap<Integer,Integer>();
        timePeriodIndexMap = new HashMap<Integer,Integer>();
        
        int numHours = LAST_DEPART_HOUR - FIRST_DEPART_HOUR;
        int numHalfHours = numHours*2;
        
        TIME_PERIOD_LABELS = new String[numHalfHours+1];
        
        for (int i=1; i <= numHalfHours; i++){
            int time = ((int)(i/2) + FIRST_DEPART_HOUR)*100 + (i%2)*30;
            indexTimePeriodMap.put(i, time);
            timePeriodIndexMap.put(time, i);
            TIME_PERIOD_LABELS[i] = Integer.toString(time);
        }
    }
    
    public String[] getTimePeriodLabelArray(){
        return TIME_PERIOD_LABELS;
    }
    
    public String getTimePeriodLabel(int timePeriodIndex){
        return TIME_PERIOD_LABELS[timePeriodIndex]; 
    }
    
    // time argument is specified as: 500 for 5 am, 530 for 5:30 am, 1530 for 3:30 pm, etc.
    public int getTimePeriodIndexForTime(int time){
        return timePeriodIndexMap.get(time);
    }

    public int getNumberOfTimePeriods(){
        return TIME_PERIOD_LABELS.length - 1;
    }
    
    /**
     * return the tod skim period
     */
    public int getTod(int period) {

        int modelPeriod = 0;

        if ( period >= 1 && period < 4 )
            modelPeriod = 1;
        else if ( period < 6 )
            modelPeriod = 2;
        else if ( period < 10 )
            modelPeriod = 3;
        else if ( period < 12 )
            modelPeriod = 4;
        else if ( period < 20 )
            modelPeriod = 5;
        else if ( period < 24 )
            modelPeriod = 6;
        else if ( period < 28 )
            modelPeriod = 7;
        else if ( period < 32 )
            modelPeriod = 8;
        else
            modelPeriod = 1;
        
        return(modelPeriod);
    }
    
    public String[] getAwfAltLabels()
    {
        return awfAltLabels;
    }
    

    // determine if the purpose string is a work purpose
    public boolean getDcModelPurposeIsWorkPurpose( String purposeName ) {
        String purpose = getPurposeString( purposeName );
        return purpose.equalsIgnoreCase( workPurposeName );
    }

    // determine if the purpose string is a university purpose
    public boolean getDcModelPurposeIsUniversityPurpose( String purposeName ) {
        String purpose = getPurposeString( purposeName );
        return purpose.equalsIgnoreCase( universityPurposeName );
    }

    // determine if the purpose string is a school purpose
    public boolean getDcModelPurposeIsSchoolPurpose( String purposeName ) {
        String purpose = getPurposeString( purposeName );
        return purpose.equalsIgnoreCase( schoolPurposeName );
    }

    
    public int getDcModelPurposeIndex( String purposeName ) {
        return dcModelPurposeIndexMap.get( purposeName.toLowerCase() );
    }

    public int getStopFrequencyModelIndex( String tourPurposeName ){
        return stopFreqUecIndexMap.get( tourPurposeName );
    }
    
    public TreeSet<Integer> getStopFreqModelSheetIndices() {
        TreeSet<Integer> set = new TreeSet<Integer>();
        for ( int el : stopFreqUecIndexMap.values() )
            set.add( el );
        return set;
    }
    
    public int getStopLocationModelIndex( String stopPurposeName ){
        return stopLocUecIndexMap.get( stopPurposeName );
    }
    
    public Collection<Integer> getStopLocModelSheetIndices() {
        return stopLocUecIndexMap.values();
    }
    
    public int getTripModeChoiceModelIndex( String tourPurposeName ){
        return tripModeChoiceUecIndexMap.get( tourPurposeName );
    }
    
    public Collection<Integer> getTripModeChoiceModelSheetIndices() {
        return tripModeChoiceUecIndexMap.values();
    }
    
    public Set<String> getTripModeChoiceModelPurposes() {
        return tripModeChoiceUecIndexMap.keySet();
    }
    
    public boolean isValidDcSizePurposeSegment ( String purposeName, String segmentName ){
        boolean returnValue = false;
        String purpKey = purposeName.toLowerCase();
        if ( dcSizePurposeSegmentMap.containsKey( purpKey ) ) {
            HashMap<String, Integer> segmentNamesMap = dcSizePurposeSegmentMap.get( purpKey );
            if ( segmentNamesMap.size() > 0 ) {
                String segKey = segmentName.toLowerCase();
                if ( segmentNamesMap.containsKey( segKey ) )
                    returnValue = true;
            }
        }
        return returnValue; 
    }

    public String[] getDcSizeArrayPurposeStrings() {
        String[] names = new String[dcSizeArrayIndexPurposeMap.size()];
        for ( Integer i : dcSizeArrayIndexPurposeMap.keySet() )
            names[i] = dcSizeArrayIndexPurposeMap.get(i);
        return names;
    }
    
    // The following are used to navigate the dcSize array
    
    public int getDcSizeArrayCategoryIndexOffset(String category){
        if ( category.equalsIgnoreCase( MANDATORY_CATEGORY ) )
            return getDcSizeArrayMandatoryCategoryIndexOffset();
        else if ( category.equalsIgnoreCase( JOINT_NON_MANDATORY_CATEGORY ) )
            return getDcSizeArrayJointCategoryIndexOffset();
        else if ( category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) )
            return getDcSizeArrayIndivNonManCategoryIndexOffset();
        else if ( category.equalsIgnoreCase( AT_WORK_CATEGORY ) )
            return getDcSizeArrayAtWorkCategoryIndexOffset();
        else
            return -1;
    }
    
    public int getNumDcSizeArrayCategorySegments(String category){
        if ( category.equalsIgnoreCase( MANDATORY_CATEGORY ) )
            return getNumDcSizeArrayMandatorySegments();
        else if ( category.equalsIgnoreCase( JOINT_NON_MANDATORY_CATEGORY ) )
            return getNumDcSizeArrayJointSegments();
        else if ( category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) )
            return getNumDcSizeArrayIndivNonManSegments();
        else if ( category.equalsIgnoreCase( AT_WORK_CATEGORY ) )
            return getNumDcSizeArrayAtWorkSegments();
        else
            return -1;
    }
    
    private int getDcSizeArrayMandatoryCategoryIndexOffset() {
        return 0;
    }
    
    private int getNumDcSizeArrayMandatorySegments() {
        return mandatoryDcModelPurposeNames.length;
    }
    
    private int getDcSizeArrayJointCategoryIndexOffset() {
        return mandatoryDcModelPurposeNames.length + ESCORT_SEGMENT_NAMES.length;
    }
    
    private int getNumDcSizeArrayJointSegments() {
        return nonMandatoryDcModelPurposeNames.length - ESCORT_SEGMENT_NAMES.length;
    }
    
    private int getDcSizeArrayIndivNonManCategoryIndexOffset() {
        return mandatoryDcModelPurposeNames.length;
    }
    
    private int getNumDcSizeArrayIndivNonManSegments() {
        return nonMandatoryDcModelPurposeNames.length;
    }
    
    private int getDcSizeArrayAtWorkCategoryIndexOffset() {
        return mandatoryDcModelPurposeNames.length + nonMandatoryDcModelPurposeNames.length;
    }
    
    private int getNumDcSizeArrayAtWorkSegments() {
        return dcSizeArrayIndexPurposeMap.size() - getDcSizeArrayAtWorkCategoryIndexOffset();
    }




    // The following are used to navigate the dcSize segments calculated
    
    public int getDcSizeSegmentCategoryIndexOffset(String category){
        if ( category.equalsIgnoreCase( MANDATORY_CATEGORY ) )
            return getDcSizeSegmentMandatoryCategoryIndexOffset();
        else if ( category.equalsIgnoreCase( JOINT_NON_MANDATORY_CATEGORY ) )
            return getDcSizeSegmentJointCategoryIndexOffset();
        else if ( category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) )
            return getDcSizeSegmentIndivNonManCategoryIndexOffset();
        else if ( category.equalsIgnoreCase( AT_WORK_CATEGORY ) )
            return getDcSizeSegmentAtWorkCategoryIndexOffset();
        else
            return -1;
    }
    
    public int getNumDcSizeSegmentCategorySegments(String category){
        if ( category.equalsIgnoreCase( MANDATORY_CATEGORY ) )
            return getNumDcSizeSegmentMandatorySegments();
        else if ( category.equalsIgnoreCase( JOINT_NON_MANDATORY_CATEGORY ) )
            return getNumDcSizeSegmentJointSegments();
        else if ( category.equalsIgnoreCase( INDIVIDUAL_NON_MANDATORY_CATEGORY ) )
            return getNumDcSizeSegmentIndivNonManSegments();
        else if ( category.equalsIgnoreCase( AT_WORK_CATEGORY ) )
            return getNumDcSizeSegmentAtWorkSegments();
        else
            return -1;
    }
    
    private int getDcSizeSegmentMandatoryCategoryIndexOffset() {
        return 0;
    }
    
    private int getNumDcSizeSegmentMandatorySegments() {
        return mandatoryDcModelPurposeNames.length;
    }
    
    private int getDcSizeSegmentJointCategoryIndexOffset() {
        return mandatoryDcModelPurposeNames.length + ESCORT_SEGMENT_NAMES.length;
    }
    
    private int getNumDcSizeSegmentJointSegments() {
        return nonMandatoryDcModelPurposeNames.length - ESCORT_SEGMENT_NAMES.length;
    }
    
    private int getDcSizeSegmentIndivNonManCategoryIndexOffset() {
        return mandatoryDcModelPurposeNames.length;
    }
    
    private int getNumDcSizeSegmentIndivNonManSegments() {
        return nonMandatoryDcModelPurposeNames.length;
    }
    
    private int getDcSizeSegmentAtWorkCategoryIndexOffset() {
        return mandatoryDcModelPurposeNames.length + nonMandatoryDcModelPurposeNames.length;
    }
    
    private int getNumDcSizeSegmentAtWorkSegments() {
        return dcSizeIndexSegmentMap.size() - getDcSizeSegmentAtWorkCategoryIndexOffset();
    }
    
    public int getNumEscortSegments() {
    	return ESCORT_SEGMENT_NAMES.length;
    }
    

    public String[] getDcSizeSegmentStrings() {
        String[] names = new String[dcSizeIndexSegmentMap.size()];
        for ( Integer i : dcSizeIndexSegmentMap.keySet() )
            names[i] = dcSizeIndexSegmentMap.get(i);
        return names;
    }
    
    public int getDcSizeArrayIndexFromDcModelIndex( int dcModelIndex ){
        String dcModelPurposeString = dcModelIndexPurposeMap.get( dcModelIndex );
        String dcSizePurposeString = dcModelDcSizePurposeMap.get(dcModelPurposeString);
        int dcSizeArrayIndex = getDcSizeArrayPurposeIndex( dcSizePurposeString );
        return dcSizeArrayIndex;
    }

    public int getDcSizeArrayPurposeIndex( String purpose ) {
        return dcSizeArrayPurposeIndexMap.get(purpose);
    }
    
    public String getDcSizeArrayIndexPurpose( int index ) {
        return dcSizeArrayIndexPurposeMap.get(index);
    }
    
    public String getWorkPurposeFromIncomeInDollars( int hhIncomeInDollars ) {
        return getWorkPurposeFromIncomeInDollars( false, hhIncomeInDollars );
    }

    public String getWorkPurposeFromIncomeInDollars( boolean isPtWorker, int hhIncomeInDollars ) {
        if ( isPtWorker ) {
            return (workPurposeName + "_" + WORK_PURPOSE_SEGMENT_NAMES[WORK_PURPOSE_SEGMENT_NAMES.length-1]).toLowerCase();
        } else {
            int incomeSegment = getIncomeSegment(hhIncomeInDollars); 
            return (workPurposeName + "_" + WORK_PURPOSE_SEGMENT_NAMES[incomeSegment-1]).toLowerCase();              
        }
    }

    public String getWorkPurposeFromOccupation( int occupation ) {
       	return (workPurposeName + "_" + WORK_PURPOSE_SEGMENT_NAMES[occupation-1]).toLowerCase();            
    }
    
    public void setSegmentedPurposeIndex( String purpose, int index ) {
        segmentedPurposeIndexMap.put( purpose, index );
    }
    
    public void setSegmentedIndexPurpose( int index, String purpose ) {
        segmentedIndexPurposeMap.put( index, purpose );
    }
    
    public int getSegmentedIndexForPurpose( String purpose ) {
        return segmentedPurposeIndexMap.get( purpose );
    }
    
    public String getSegmentedPurposeForIndex( int index ) {
        return segmentedIndexPurposeMap.get( index );
    }
    
    public HashMap<String,Integer> getSegmentedPurposeIndexMap() {
        return segmentedPurposeIndexMap;
    }

    public HashMap<Integer,String> getSegmentedIndexPurposeMap() {
        return segmentedIndexPurposeMap;
    }

    public int getNumIncomeSegments() {
        return incomeSegmentDollarLimits.length;
    }
    

    
}
