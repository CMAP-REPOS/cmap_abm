package com.pb.cmap.tourBased;

import com.pb.common.datafile.TableDataSet;
import com.pb.models.ctrampIf.ModelStructure;

import java.util.HashMap;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class CmapModelStructure extends ModelStructure {


    public final String[] MANDATORY_DC_PURPOSE_NAMES = { WORK_PURPOSE_NAME, UNIVERSITY_PURPOSE_NAME, SCHOOL_PURPOSE_NAME };
    public final String[] WORK_PURPOSE_SEGMENT_NAMES = { "OCC1", "OCC2", "OCC3", "OCC4", "OCC5", "OCC6", "OCC7", "OCC8", "OCC9", "OCC10", "OCC11", "OCC12" };
    public final String[] UNIVERSITY_PURPOSE_SEGMENT_NAMES = { };
    public final String[] SCHOOL_PURPOSE_SEGMENT_NAMES = { "PRE", "GRADE", "HIGH" };
    
    //DC school segment breaks
    public static final int PRESCHOOL_MAX_AGE = 5;
    public static final int GRADESCHOOL_MAX_AGE = 13;
    
    public final int[] CMAP_INCOME_SEGMENT_DOLLAR_LIMITS = {35000, 60000, 100000, Integer.MAX_VALUE}; 
        
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC1                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC2                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC3                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC4                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC5                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC6                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC7                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC8                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC9                = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC10               = 1;   
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC11               = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC12               = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_UNIVERSITY          = 2;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_SCHOOL_PRE          = 5;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_SCHOOL_GRADE        = 4;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_SCHOOL_HIGH         = 3;

    public final int USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK                       = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_UNIVERSITY                 = 2;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_SCHOOL_PRE                 = 5;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_SCHOOL_GRADE               = 4;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_SCHOOL_HIGH                = 3;
    
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK           = 1;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_UNIVERSITY     = 2;
    public final int USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_SCHOOL         = 3;


    public final int  MANDATORY_STOP_FREQ_UEC_INDEX_WORK                                 = 1;
    public final int  MANDATORY_STOP_FREQ_UEC_INDEX_UNIVERSITY                           = 2;
    public final int  MANDATORY_STOP_FREQ_UEC_INDEX_SCHOOL                               = 3;

    public final int  MANDATORY_STOP_LOC_UEC_INDEX_WORK                                 = 1;
    public final int  MANDATORY_STOP_LOC_UEC_INDEX_UNIVERSITY                           = 2;
    public final int  MANDATORY_STOP_LOC_UEC_INDEX_SCHOOL                               = 3;

    public final int  MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX_WORK                         = 1;
    public final int  MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX_UNIVERSITY                   = 2;
    public final int  MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX_SCHOOL                       = 3;

    
    
    public final String[] NON_MANDATORY_DC_PURPOSE_NAMES = { ESCORT_PURPOSE_NAME, SHOPPING_PURPOSE_NAME, EAT_OUT_PURPOSE_NAME, OTH_MAINT_PURPOSE_NAME, SOCIAL_PURPOSE_NAME, OTH_DISCR_PURPOSE_NAME };
    public final String[] ESCORT_PURPOSE_SEGMENT_NAMES = { }; 
    
    public final String[] SHOPPING_PURPOSE_SEGMENT_NAMES = { };
    public final String[] EAT_OUT_PURPOSE_SEGMENT_NAMES = { };
    public final String[] OTH_MAINT_PURPOSE_SEGMENT_NAMES = { };
    public final String[] SOCIAL_PURPOSE_SEGMENT_NAMES = { };
    public final String[] OTH_DISCR_PURPOSE_SEGMENT_NAMES = { };

    public final int NON_MANDATORY_SOA_UEC_INDEX_ESCORT                                  = 6;
    public final int NON_MANDATORY_SOA_UEC_INDEX_SHOPPING                                = 7;
    public final int NON_MANDATORY_SOA_UEC_INDEX_EAT_OUT                                 = 8;
    public final int NON_MANDATORY_SOA_UEC_INDEX_OTHER_MAINT                             = 9;
    public final int NON_MANDATORY_SOA_UEC_INDEX_SOCIAL                                  = 10;
    public final int NON_MANDATORY_SOA_UEC_INDEX_OTHER_DISCR                             = 11;

    public final int NON_MANDATORY_DC_UEC_INDEX_ESCORT                                   = 6;
    public final int NON_MANDATORY_DC_UEC_INDEX_SHOPPING                                 = 7;
    public final int NON_MANDATORY_DC_UEC_INDEX_EAT_OUT                                  = 8;
    public final int NON_MANDATORY_DC_UEC_INDEX_OTHER_MAINT                              = 9;
    public final int NON_MANDATORY_DC_UEC_INDEX_SOCIAL                                   = 10;
    public final int NON_MANDATORY_DC_UEC_INDEX_OTHER_DISCR                              = 11;

    public final int NON_MANDATORY_MC_UEC_INDEX_ESCORT                                   = 4;
    public final int NON_MANDATORY_MC_UEC_INDEX_SHOPPING                                 = 5;
    public final int NON_MANDATORY_MC_UEC_INDEX_EAT_OUT                                  = 6;
    public final int NON_MANDATORY_MC_UEC_INDEX_OTHER_MAINT                              = 5;
    public final int NON_MANDATORY_MC_UEC_INDEX_SOCIAL                                   = 6;
    public final int NON_MANDATORY_MC_UEC_INDEX_OTHER_DISCR                              = 6;

    public final int  NON_MANDATORY_STOP_FREQ_UEC_INDEX_ESCORT                           = 4;
    public final int  NON_MANDATORY_STOP_FREQ_UEC_INDEX_SHOPPING                         = 5;
    public final int  NON_MANDATORY_STOP_FREQ_UEC_INDEX_EAT_OUT                          = 7;
    public final int  NON_MANDATORY_STOP_FREQ_UEC_INDEX_OTHER_MAINT                      = 6;
    public final int  NON_MANDATORY_STOP_FREQ_UEC_INDEX_SOCIAL                           = 7;
    public final int  NON_MANDATORY_STOP_FREQ_UEC_INDEX_OTHER_DISCR                      = 8;

    public final int  NON_MANDATORY_STOP_LOC_UEC_INDEX_ESCORT                           = 4;
    public final int  NON_MANDATORY_STOP_LOC_UEC_INDEX_SHOPPING                         = 5;
    public final int  NON_MANDATORY_STOP_LOC_UEC_INDEX_EAT_OUT                          = 6;
    public final int  NON_MANDATORY_STOP_LOC_UEC_INDEX_OTHER_MAINT                      = 7;
    public final int  NON_MANDATORY_STOP_LOC_UEC_INDEX_SOCIAL                           = 8;
    public final int  NON_MANDATORY_STOP_LOC_UEC_INDEX_OTHER_DISCR                      = 9;

    public final int  NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX                          = 4;

    
    
    public final String[] AT_WORK_DC_PURPOSE_NAMES = { "work-based" };
    public final String[] AT_WORK_DC_SIZE_SEGMENT_NAMES = { };

    public final int AT_WORK_SOA_UEC_INDEX_EAT                                           = 12;
    public final int AT_WORK_SOA_UEC_INDEX_BUSINESS                                      = 12;
    public final int AT_WORK_SOA_UEC_INDEX_MAINT                                         = 12;
    
    public final int AT_WORK_DC_UEC_INDEX_EAT                                           = 12;
    public final int AT_WORK_DC_UEC_INDEX_BUSINESS                                      = 12;
    public final int AT_WORK_DC_UEC_INDEX_MAINT                                         = 12;
    
    public final int AT_WORK_MC_UEC_INDEX_EAT                                           = 7;
    public final int AT_WORK_MC_UEC_INDEX_BUSINESS                                      = 7;
    public final int AT_WORK_MC_UEC_INDEX_MAINT                                         = 7;

    public final int ARC_AT_WORK_PURPOSE_INDEX_EAT                                          = 1;
    public final int ARC_AT_WORK_PURPOSE_INDEX_BUSINESS                                     = 2;
    public final int ARC_AT_WORK_PURPOSE_INDEX_MAINT                                        = 3;

    public final int AT_WORK_STOP_FREQ_UEC_INDEX_EAT                                    = 9;
    public final int AT_WORK_STOP_FREQ_UEC_INDEX_BUSINESS                               = 9;
    public final int AT_WORK_STOP_FREQ_UEC_INDEX_MAINT                                  = 9;

    public final int  AT_WORK_STOP_LOC_UEC_INDEX                                        = 10;

    public final int  AT_WORK_TRIP_MODE_CHOICE_UEC_INDEX                                = 5;
    
   
    public static final int[] SOV_ALTS = { 1, 2 };
    public static final int[] HOV_ALTS = { 3, 4, 5, 6 };
    public static final int[] S2_ALTS = { 3, 4 };
    public static final int[] S3_ALTS = { 5, 6 };
    public static final int[] WALK_ALTS = { 7 };
    public static final int[] BIKE_ALTS = { 8 };
    public static final int[] NON_MOTORIZED_ALTS = { 7, 8 };
    public static final int[] TRANSIT_ALTS = { 9, 10, 11, 12 };
    public static final int[] WALK_LOCAL_ALTS = { 9 };
    public static final int[] WALK_PREMIUM_ALTS = { 10 };
    public static final int[] DRIVE_TRANSIT_ALTS = { 11, 12 };
    public static final int[] PNR_ALTS = { 11, 12 };
    public static final int[] KNR_ALTS = { 11, 12 };
    public static final int[] SCHOOL_BUS_ALTS = { 14 };
    public static final int[] TAXI_ALTS = { 13 };
    
    public static final int[] TRIP_SOV_ALTS = { 1, 2 };
    public static final int[] TRIP_HOV_ALTS = { 3, 4, 5, 6 };

    public static final int      MAXIMUM_TOUR_MODE_ALT_INDEX    = 26;


    
    private static final int DEFAULT_WORK_DEPART_INTERVAL   = 6;          //  7:00 am
    private static final int DEFAULT_WORK_ARRIVE_INTERVAL   = 28;         //  6:00 pm
    private static final int DEFAULT_SCHOOL_DEPART_INTERVAL = 8;          //  8:00 am
    private static final int DEFAULT_SCHOOL_ARRIVE_INTERVAL = 24;         //  4:00 pm
    private static final int DEFAULT_UNIVERSITY_DEPART_INTERVAL = 10;     //  9:00 am
    private static final int DEFAULT_UNIVERSITY_ARRIVE_INTERVAL = 20;     //  2:00 pm
    private static final int DEFAULT_NON_MANDATORY_DEPART_INTERVAL = 14;  // 11:00 am
    private static final int DEFAULT_NON_MANDATORY_ARRIVE_INTERVAL = 20;  //  2:00 pm
    
    public static final int      P1                  = 1;
    public static final int      P2                  = 2;
    public static final int      P3                  = 3;
    public static final int      P4                  = 4;
    public static final int      P5                  = 5;
    public static final int      P6                  = 6;
    public static final int      P7                  = 7;
    public static final int      P8                  = 8;
    public static final int[]    PERIODS        = { -1, P1, P2, P3, P4, P5, P6, P7, P8 };

    public static final int      P1_P1               = 1;
    public static final int      P1_P2               = 2;
    public static final int      P1_P3               = 3;
    public static final int      P1_P4               = 4;
    public static final int      P1_P5               = 5;
    public static final int      P1_P6               = 6;
    public static final int      P1_P7               = 7;
    public static final int      P1_P8               = 8;

    public static final int      P2_P1               = -1;
    public static final int      P2_P2               = 9;
    public static final int      P2_P3               = 10;
    public static final int      P2_P4               = 11;
    public static final int      P2_P5               = 12;
    public static final int      P2_P6               = 13;
    public static final int      P2_P7               = 14;
    public static final int      P2_P8               = 15;
    
    public static final int      P3_P1               = -1;
    public static final int      P3_P2               = -1;
    public static final int      P3_P3               = 16;
    public static final int      P3_P4               = 17;
    public static final int      P3_P5               = 18;
    public static final int      P3_P6               = 19;
    public static final int      P3_P7               = 20;
    public static final int      P3_P8               = 21;

    public static final int      P4_P1               = -1;
    public static final int      P4_P2               = -1;
    public static final int      P4_P3               = -1;
    public static final int      P4_P4               = 22;
    public static final int      P4_P5               = 23;
    public static final int      P4_P6               = 24;
    public static final int      P4_P7               = 25;
    public static final int      P4_P8               = 26;

    public static final int      P5_P1               = -1;
    public static final int      P5_P2               = -1;
    public static final int      P5_P3               = -1;
    public static final int      P5_P4               = -1;
    public static final int      P5_P5               = 27;
    public static final int      P5_P6               = 28;
    public static final int      P5_P7               = 29;
    public static final int      P5_P8               = 30;

    public static final int      P6_P1               = -1;
    public static final int      P6_P2               = -1;
    public static final int      P6_P3               = -1;
    public static final int      P6_P4               = -1;
    public static final int      P6_P5               = -1;
    public static final int      P6_P6               = 31;
    public static final int      P6_P7               = 32;
    public static final int      P6_P8               = 33;

    public static final int      P7_P1               = -1;
    public static final int      P7_P2               = -1;
    public static final int      P7_P3               = -1;
    public static final int      P7_P4               = -1;
    public static final int      P7_P5               = -1;
    public static final int      P7_P6               = -1;
    public static final int      P7_P7               = 34;
    public static final int      P7_P8               = 35;

    public static final int      P8_P1               = -1;
    public static final int      P8_P2               = -1;
    public static final int      P8_P3               = -1;
    public static final int      P8_P4               = -1;
    public static final int      P8_P5               = -1;
    public static final int      P8_P6               = -1;
    public static final int      P8_P7               = -1;
    public static final int      P8_P8               = 36;

    public static final int[]    PERIOD_COMBINATION_INDICES = {
        -1,
        P1_P1, P1_P2, P1_P3, P1_P4, P1_P5, P1_P6, P1_P7, P1_P8,
        P2_P2, P2_P3, P2_P4, P2_P5, P2_P6, P2_P7, P2_P8,
        P3_P3, P3_P4, P3_P5, P3_P6, P3_P7, P3_P8,
        P4_P4, P4_P5, P4_P6, P4_P7, P4_P8,
        P5_P5, P5_P6, P5_P7, P5_P8,
        P6_P6, P6_P7, P6_P8,
        P7_P7, P7_P8,
        P8_P8
    };
    public static final int[][]  PERIOD_COMBINATIONS = { 
        { },
        { -1, P1_P1, P1_P2, P1_P3, P1_P4, P1_P5, P1_P6, P1_P7, P1_P8 },
        { -1, P2_P1, P2_P2, P2_P3, P2_P4, P2_P5, P2_P6, P2_P7, P2_P8 },
        { -1, P3_P1, P3_P2, P3_P3, P3_P4, P3_P5, P3_P6, P3_P7, P3_P8 },
        { -1, P4_P1, P4_P2, P4_P3, P4_P4, P4_P5, P4_P6, P4_P7, P4_P8 },
        { -1, P5_P1, P5_P2, P5_P3, P5_P4, P5_P5, P5_P6, P5_P7, P5_P8 },
        { -1, P6_P1, P6_P2, P6_P3, P6_P4, P6_P5, P6_P6, P6_P7, P6_P8 },
        { -1, P7_P1, P7_P2, P7_P3, P7_P4, P7_P5, P7_P6, P7_P7, P7_P8 },
        { -1, P8_P1, P8_P2, P8_P3, P8_P4, P8_P5, P8_P6, P8_P7, P8_P8 }
    };

    public static final int UPPER_1 = 3;
    public static final int UPPER_2 = 5;
    public static final int UPPER_3 = 9;
    public static final int UPPER_4 = 11;
    public static final int UPPER_5 = 19;
    public static final int UPPER_6 = 23;
    public static final int UPPER_7 = 27;
    public static final int UPPER_8 = 31;

    public static final String[] PERIOD_LABELS = { "", "P1", "P2", "P3", "P4", "P5", "P5", "P7", "P8" };
    
    private static final int TIME_INTERVAL_FOR_4_PM         = 24;         //  time interval index associated with 4:00 pm
    private static final int TIME_INTERVAL_FOR_7_PM         = 30;         //  time interval index associated with 7:00 pm
    

    
    public final double[][]      CDAP_6_PLUS_PROPORTIONS                                            = {
            {0.0, 0.0, 0.0}, {0.79647, 0.09368, 0.10985}, {0.61678, 0.25757, 0.12565},
            {0.69229, 0.15641, 0.15130}, {0.00000, 0.67169, 0.32831}, {0.00000, 0.54295, 0.45705},
            {0.77609, 0.06004, 0.16387}, {0.68514, 0.09144, 0.22342}, {0.14056, 0.06512, 0.79432}   };

    public static final String[] JTF_ALTERNATIVE_LABELS                                             = {
            "0_tours", "1_Shop", "1_Main", "1_Eat", "1_Visit", "1_Disc", "2_SS", "2_SM", "2_SE",
            "2_SV", "2_SD", "2_MM", "2_ME", "2_MV", "2_MD", "2_EE", "2_EV", "2_ED", "2_VV", "2_VD",
            "2_DD"                                                                                  };
    public static final String[] AWF_ALTERNATIVE_LABELS                                             = {
            "0_subTours", "1_eat", "1_business", "1_other", "2_business", "2 other",
            "2_eat_business"                                                                        };
    
   
    
    public CmapModelStructure(){
		super();

	    setIncomeSegments( CMAP_INCOME_SEGMENT_DOLLAR_LIMITS );

        jtfAltLabels = JTF_ALTERNATIVE_LABELS;
        awfAltLabels = AWF_ALTERNATIVE_LABELS;

	    
        dcSizePurposeSegmentMap = new HashMap<String,HashMap<String,Integer>>();
        
        dcSizeIndexSegmentMap = new HashMap<Integer,String>();
        dcSizeSegmentIndexMap = new HashMap<String,Integer>();
        dcSizeArrayIndexPurposeMap = new HashMap<Integer,String>();
        dcSizeArrayPurposeIndexMap = new HashMap<String,Integer>();

        
        
        setMandatoryPurposeNameValues();

        setUsualWorkAndSchoolLocationSoaUecSheetIndexValues ();
        setUsualWorkAndSchoolLocationUecSheetIndexValues ();
        setUsualWorkAndSchoolLocationModeChoiceUecSheetIndexValues ();

        setMandatoryStopFreqUecSheetIndexValues();
        setMandatoryStopLocUecSheetIndexValues();
        setMandatoryTripModeChoiceUecSheetIndexValues();
        
        
        
        
        setNonMandatoryPurposeNameValues();
        
        setNonMandatoryDcSoaUecSheetIndexValues();
        setNonMandatoryDcUecSheetIndexValues();
        setNonMandatoryModeChoiceUecSheetIndexValues();

        setNonMandatoryStopFreqUecSheetIndexValues();
        setNonMandatoryStopLocUecSheetIndexValues();
        setNonMandatoryTripModeChoiceUecSheetIndexValues();
        
        
        
        
        setAtWorkPurposeNameValues();
        
        setAtWorkDcSoaUecSheetIndexValues();
        setAtWorkDcUecSheetIndexValues();
        setAtWorkModeChoiceUecSheetIndexValues();

        setAtWorkStopFreqUecSheetIndexValues();
        setAtWorkStopLocUecSheetIndexValues();        
        setAtWorkTripModeChoiceUecSheetIndexValues();
        
        createDcSizePurposeSegmentMap();        
        
        mapModelSegmentsToDcSizeArraySegments();
        

    }

    private void mapModelSegmentsToDcSizeArraySegments() {
        
        Logger logger = Logger.getLogger( this.getClass() );
        
        dcSizeDcModelPurposeMap = new HashMap<String,String>();
        dcModelDcSizePurposeMap = new HashMap<String,String>();
        
        // loop over soa model names and map top dc size array indices
        for (int i=0; i < dcModelPurposeIndexMap.size(); i++){
            String modelSegment = dcModelIndexPurposeMap.get(i);
            
            // look for this modelSegment name in the dc size array names map, with and without "_segment".
            if ( dcSizeArrayPurposeIndexMap.containsKey(modelSegment) ) {
                dcSizeDcModelPurposeMap.put(modelSegment, modelSegment);
                dcModelDcSizePurposeMap.put(modelSegment, modelSegment);
            }
            else {
                int underscoreIndex = modelSegment.indexOf('_');
                if ( underscoreIndex < 0 ) {
                    if ( dcSizeArrayPurposeIndexMap.containsKey( modelSegment + "_" + modelSegment ) ) {
                        dcSizeDcModelPurposeMap.put( modelSegment + "_" + modelSegment, modelSegment );
                        dcModelDcSizePurposeMap.put( modelSegment, modelSegment + "_" + modelSegment );
                    }
                    else {
                        logger.error( String.format("could not establish correspondence between DC SOA model purpose string = %s", modelSegment) );
                        logger.error( String.format("and a DC array purpose string:") );
                        int j=0;
                        for ( String key : dcSizeArrayPurposeIndexMap.keySet() )
                            logger.error( String.format("%-2d: %s", ++j, key) );
                        throw new RuntimeException(); 
                    }
                }
                else {
                    // all at-work size segments should map to one model segment
                    if ( modelSegment.substring(0,underscoreIndex).equalsIgnoreCase(AT_WORK_PURPOSE_NAME) ) {
                        dcSizeDcModelPurposeMap.put( AT_WORK_PURPOSE_NAME + "_" + AT_WORK_PURPOSE_NAME, modelSegment );
                        dcModelDcSizePurposeMap.put( modelSegment, AT_WORK_PURPOSE_NAME + "_" + AT_WORK_PURPOSE_NAME );
                    }
                    else {
                        logger.error( String.format("could not establish correspondence between DC SOA model purpose string = %s", modelSegment) );
                        logger.error( String.format("and a DC array purpose string:") );
                        int j=0;
                        for ( String key : dcSizeArrayPurposeIndexMap.keySet() )
                            logger.error( String.format("%-2d: %s", ++j, key) );
                        throw new RuntimeException(); 
                    }
                }
            }
            
        }

    }
    
    public void setIncomeSegments( int[] incomeBreakPoints ) {
        incomeSegmentDollarLimits = new int[incomeBreakPoints.length];
        for(int i=0; i < incomeBreakPoints.length; i++)
            incomeSegmentDollarLimits[i] = incomeBreakPoints[i];
    }
    
    public int getIncomeSegment( int hhIncomeInDollars ) {
    	for (int i=0; i< incomeSegmentDollarLimits.length; i++) {
    		if ( hhIncomeInDollars < incomeSegmentDollarLimits[i] ) {
    			return i+1; 
    		}
    	}
    	throw new RuntimeException("Invalid income segments defined in ModelStructure"); 
    }

    public String getSchoolPurpose( int age ) {

    	if ( age <= PRESCHOOL_MAX_AGE )
            return (schoolPurposeName + "_" + SCHOOL_PURPOSE_SEGMENT_NAMES[0]).toLowerCase();
        else if ( age <= GRADESCHOOL_MAX_AGE )
            return (schoolPurposeName + "_" + SCHOOL_PURPOSE_SEGMENT_NAMES[1]).toLowerCase();
        else 
        	return (schoolPurposeName + "_" + SCHOOL_PURPOSE_SEGMENT_NAMES[2]).toLowerCase();
    }

    public String getSchoolPurpose() {
        return schoolPurposeName.toLowerCase();
    }

    public String getUniversityPurpose() {
        return universityPurposeName.toLowerCase();
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
   
    
    public boolean getTourModeIsBike( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < BIKE_ALTS.length; i++ ) {
            if ( BIKE_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsWalk( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < WALK_ALTS.length; i++ ) {
            if ( WALK_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsWalkLocal( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < WALK_LOCAL_ALTS.length; i++ ) {
            if ( WALK_LOCAL_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsWalkPremium( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < WALK_PREMIUM_ALTS.length; i++ ) {
            if ( WALK_PREMIUM_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }


    public boolean getTourModeIsTransit( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < TRANSIT_ALTS.length; i++ ) {
            if ( TRANSIT_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsDriveTransit( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < DRIVE_TRANSIT_ALTS.length; i++ ) {
            if ( DRIVE_TRANSIT_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsPnr(int tourMode)
    {
        boolean returnValue = false;
        for (int i = 0; i < PNR_ALTS.length; i++)
        {
            if (PNR_ALTS[i] == tourMode)
            {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsKnr(int tourMode)
    {
        boolean returnValue = false;
        for (int i = 0; i < KNR_ALTS.length; i++)
        {
            if (KNR_ALTS[i] == tourMode)
            {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsSchoolBus( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < SCHOOL_BUS_ALTS.length; i++ ) {
            if ( SCHOOL_BUS_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }
    
    public boolean getTourModeIsTaxi( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < TAXI_ALTS.length; i++ ) {
            if ( TAXI_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsHov(int tourMode) {
        boolean returnValue = false;
        for ( int i=0; i < HOV_ALTS.length; i++ ) {
            if ( HOV_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsS2(int tourMode) {
        boolean returnValue = false;
        for ( int i=0; i < S2_ALTS.length; i++ ) {
            if ( S2_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsS3(int tourMode) {
        boolean returnValue = false;
        for ( int i=0; i < S3_ALTS.length; i++ ) {
            if ( S3_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTripModeIsSovOrHov( int tripMode ) {  

        for ( int i=0; i < TRIP_SOV_ALTS.length; i++ ) {
            if ( TRIP_SOV_ALTS[i] == tripMode )
                return true;
        }
        
        for ( int i=0; i < TRIP_HOV_ALTS.length; i++ ) {
            if ( TRIP_HOV_ALTS[i] == tripMode )
                return true;
        }
        
        return false;
    }


    public boolean getTourModeIsSov( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < SOV_ALTS.length; i++ ) {
            if ( SOV_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }

    public boolean getTourModeIsSovOrHov( int tourMode ) {
        for ( int i=0; i < SOV_ALTS.length; i++ ) {
            if ( SOV_ALTS[i] == tourMode )
                return true;
        }
        
        for ( int i=0; i < HOV_ALTS.length; i++ ) {
            if ( HOV_ALTS[i] == tourMode )
                return true;
        }
        
        return false;
    }

    public boolean getTourModeIsNonMotorized( int tourMode ) {
        boolean returnValue = false;
        for ( int i=0; i < NON_MOTORIZED_ALTS.length; i++ ) {
            if ( NON_MOTORIZED_ALTS[i] == tourMode ) {
                returnValue = true;
                break;
            }
        }
        return returnValue;
    }


    
    
    private int createPurposeIndexMaps( String purposeName, String[] segmentNames, int index, String categoryString ) {
        
        HashMap<String, Integer> segmentMap = new HashMap<String, Integer>();
        String key = "";
        if ( segmentNames.length > 0 ) {
            for ( int i=0; i < segmentNames.length; i++ ) {
                segmentMap.put ( segmentNames[i].toLowerCase(), i );
                key = purposeName.toLowerCase() + "_" + segmentNames[i].toLowerCase();
                dcSizeIndexSegmentMap.put ( index, key );
                dcSizeSegmentIndexMap.put ( key, index++ );
            }
        }
        else {
            segmentMap.put ( purposeName.toLowerCase(), 0 );
            key = purposeName.toLowerCase() + "_" + purposeName.toLowerCase();
            dcSizeIndexSegmentMap.put ( index, key );
            dcSizeSegmentIndexMap.put ( key, index++ );
        }
        dcSizePurposeSegmentMap.put( purposeName.toLowerCase(), segmentMap );
        
        return index;
        
    }
    

    /**
     * This method defines the segmentation for which destination choice size variables are calculated.
     */
    private void createDcSizePurposeSegmentMap() {

        int index = 0;
        
        // put work purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( WORK_PURPOSE_NAME, WORK_PURPOSE_SEGMENT_NAMES, index, MANDATORY_CATEGORY );
        
        // put university purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( UNIVERSITY_PURPOSE_NAME, UNIVERSITY_PURPOSE_SEGMENT_NAMES, index, MANDATORY_CATEGORY );

        // put school purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( SCHOOL_PURPOSE_NAME, SCHOOL_PURPOSE_SEGMENT_NAMES, index, MANDATORY_CATEGORY );
        
        // put escort purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( ESCORT_PURPOSE_NAME, ESCORT_PURPOSE_SEGMENT_NAMES, index, INDIVIDUAL_NON_MANDATORY_CATEGORY );
        
        // put shopping purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( SHOPPING_PURPOSE_NAME, SHOPPING_PURPOSE_SEGMENT_NAMES, index, INDIVIDUAL_NON_MANDATORY_CATEGORY );
        
        // put eat out purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( EAT_OUT_PURPOSE_NAME, EAT_OUT_PURPOSE_SEGMENT_NAMES, index, INDIVIDUAL_NON_MANDATORY_CATEGORY );
        
        // put oth main purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( OTH_MAINT_PURPOSE_NAME, OTH_MAINT_PURPOSE_SEGMENT_NAMES, index, INDIVIDUAL_NON_MANDATORY_CATEGORY );
        
        // put social purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( SOCIAL_PURPOSE_NAME, SOCIAL_PURPOSE_SEGMENT_NAMES, index, INDIVIDUAL_NON_MANDATORY_CATEGORY );
        
        // put oth discr purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( OTH_DISCR_PURPOSE_NAME, OTH_DISCR_PURPOSE_SEGMENT_NAMES, index, INDIVIDUAL_NON_MANDATORY_CATEGORY );
        
        // put at work purpose segments, by which DC Size calculations are segmented, into a map to be stored by purpose name. 
        index = createPurposeIndexMaps( AT_WORK_PURPOSE_NAME, AT_WORK_DC_SIZE_SEGMENT_NAMES, index, AT_WORK_CATEGORY );
        
    }
    
    public HashMap<String, HashMap<String, Integer>> getDcSizePurposeSegmentMap() {
        return dcSizePurposeSegmentMap;
    }
    
    
    

    private void setMandatoryPurposeNameValues() {

        int index = 0;

        int numDcSizePurposeSegments = 0;
        if ( WORK_PURPOSE_SEGMENT_NAMES.length > 0 )
            numDcSizePurposeSegments += WORK_PURPOSE_SEGMENT_NAMES.length;
        else
            numDcSizePurposeSegments += 1;
        if ( UNIVERSITY_PURPOSE_SEGMENT_NAMES.length > 0 )
            numDcSizePurposeSegments += UNIVERSITY_PURPOSE_SEGMENT_NAMES.length;
        else
            numDcSizePurposeSegments += 1;
        if ( SCHOOL_PURPOSE_SEGMENT_NAMES.length > 0 )
            numDcSizePurposeSegments += SCHOOL_PURPOSE_SEGMENT_NAMES.length;
        else
            numDcSizePurposeSegments += 1;


        mandatoryDcModelPurposeNames = new String[numDcSizePurposeSegments];

        

        workPurposeName = WORK_PURPOSE_NAME.toLowerCase();
        workPurposeSegmentNames = new String[WORK_PURPOSE_SEGMENT_NAMES.length];
        if ( workPurposeSegmentNames.length > 0 ) {
            for ( int i=0; i < WORK_PURPOSE_SEGMENT_NAMES.length; i++ ) {
                workPurposeSegmentNames[i] = WORK_PURPOSE_SEGMENT_NAMES[i].toLowerCase();
                mandatoryDcModelPurposeNames[index] = workPurposeName + "_" + workPurposeSegmentNames[i];
                dcModelPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
                dcModelIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
                
                // a separate size term is calculated for each work purpose_segment 
                dcSizeArrayIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
                dcSizeArrayPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
                index++;
            }
        }
        else {
            mandatoryDcModelPurposeNames[index] = workPurposeName;
            dcModelPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
            dcModelIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );

            // a separate size term is calculated for each work purpose_segment 
            String name = mandatoryDcModelPurposeNames[index] + "_" + mandatoryDcModelPurposeNames[index];
            dcSizeArrayIndexPurposeMap.put( index, name );
            dcSizeArrayPurposeIndexMap.put( name, index );
            index++;
        }

        universityPurposeName = UNIVERSITY_PURPOSE_NAME.toLowerCase();
        universityPurposeSegmentNames = new String[UNIVERSITY_PURPOSE_SEGMENT_NAMES.length];
        if ( universityPurposeSegmentNames.length > 0 ) {
            for ( int i=0; i < universityPurposeSegmentNames.length; i++ ) {
                universityPurposeSegmentNames[i] = UNIVERSITY_PURPOSE_SEGMENT_NAMES[i].toLowerCase();
                mandatoryDcModelPurposeNames[index] = universityPurposeName + "_" + universityPurposeSegmentNames[i];
                dcModelPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
                dcModelIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
                
                // a separate size term is calculated for each university purpose_segment 
                dcSizeArrayIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
                dcSizeArrayPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
                index++;
            }
        }
        else {
            mandatoryDcModelPurposeNames[index] = universityPurposeName;
            dcModelPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
            dcModelIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
            
            // a separate size term is calculated for each university purpose_segment 
            String name = mandatoryDcModelPurposeNames[index] + "_" + mandatoryDcModelPurposeNames[index];
            dcSizeArrayIndexPurposeMap.put( index, name );
            dcSizeArrayPurposeIndexMap.put( name, index );
            index++;
        }

        schoolPurposeName = SCHOOL_PURPOSE_NAME.toLowerCase();
        schoolPurposeSegmentNames = new String[SCHOOL_PURPOSE_SEGMENT_NAMES.length];
        if ( schoolPurposeSegmentNames.length > 0 ) {
            for ( int i=0; i < schoolPurposeSegmentNames.length; i++ ) {
                schoolPurposeSegmentNames[i] = SCHOOL_PURPOSE_SEGMENT_NAMES[i].toLowerCase();
                mandatoryDcModelPurposeNames[index] = schoolPurposeName + "_" + schoolPurposeSegmentNames[i];
                dcModelPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
                dcModelIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
                
                // a separate size term is calculated for each school purpose_segment 
                dcSizeArrayIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
                dcSizeArrayPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
                index++;
            }
        }
        else {
            mandatoryDcModelPurposeNames[index] = schoolPurposeName;
            dcModelPurposeIndexMap.put( mandatoryDcModelPurposeNames[index], index );
            dcModelIndexPurposeMap.put( index, mandatoryDcModelPurposeNames[index] );
            
            // a separate size term is calculated for each school purpose_segment 
            String name = mandatoryDcModelPurposeNames[index] + "_" + mandatoryDcModelPurposeNames[index];
            dcSizeArrayIndexPurposeMap.put( index, name );
            dcSizeArrayPurposeIndexMap.put( name, index );
        }


    }


    private void setUsualWorkAndSchoolLocationSoaUecSheetIndexValues () {
        dcSoaUecIndexMap.put( "work_occ1", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC1 );
        dcSoaUecIndexMap.put( "work_occ2", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC2 );
        dcSoaUecIndexMap.put( "work_occ3", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC3 );
        dcSoaUecIndexMap.put( "work_occ4", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC4 );
        dcSoaUecIndexMap.put( "work_occ5", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC5 );
        dcSoaUecIndexMap.put( "work_occ6", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC6 );
        dcSoaUecIndexMap.put( "work_occ7", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC7 );
        dcSoaUecIndexMap.put( "work_occ8", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC8 );
        dcSoaUecIndexMap.put( "work_occ9", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC9 );
        dcSoaUecIndexMap.put( "work_occ10", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC10 );
        dcSoaUecIndexMap.put( "work_occ11", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC11 );
        dcSoaUecIndexMap.put( "work_occ12", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_OCC12 );
        
        dcSoaUecIndexMap.put( "university", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_UNIVERSITY );
        dcSoaUecIndexMap.put( "school_pre", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_SCHOOL_PRE );
        dcSoaUecIndexMap.put( "school_grade", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_SCHOOL_GRADE );
        dcSoaUecIndexMap.put( "school_high", USUAL_WORK_AND_SCHOOL_LOCATION_SOA_UEC_INDEX_SCHOOL_HIGH );
    }


    private void setUsualWorkAndSchoolLocationUecSheetIndexValues () {
        dcUecIndexMap.put( "work_occ1", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ2", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ3", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ4", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ5", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ6", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ7", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ8", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ9", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ10", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ11", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "work_occ12", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_WORK );
        dcUecIndexMap.put( "university", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_UNIVERSITY );
        dcUecIndexMap.put( "school_pre", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_SCHOOL_PRE );
        dcUecIndexMap.put( "school_grade", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_SCHOOL_GRADE );
        dcUecIndexMap.put( "school_high", USUAL_WORK_AND_SCHOOL_LOCATION_UEC_INDEX_SCHOOL_HIGH );
    }


    private void setUsualWorkAndSchoolLocationModeChoiceUecSheetIndexValues () {
    	tourModeChoiceUecIndexMap.put( "work_occ1", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
    	tourModeChoiceUecIndexMap.put( "work_occ2", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
    	tourModeChoiceUecIndexMap.put( "work_occ3", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
    	tourModeChoiceUecIndexMap.put( "work_occ4", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
    	tourModeChoiceUecIndexMap.put( "work_occ5", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
    	tourModeChoiceUecIndexMap.put( "work_occ6", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        tourModeChoiceUecIndexMap.put( "work_occ7", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        tourModeChoiceUecIndexMap.put( "work_occ8", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        tourModeChoiceUecIndexMap.put( "work_occ9", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        tourModeChoiceUecIndexMap.put( "work_occ10", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        tourModeChoiceUecIndexMap.put( "work_occ11", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        tourModeChoiceUecIndexMap.put( "work_occ12", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_WORK );
        
        tourModeChoiceUecIndexMap.put( "university", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_UNIVERSITY );
        tourModeChoiceUecIndexMap.put( "school_pre", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_SCHOOL );
        tourModeChoiceUecIndexMap.put( "school_grade", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_SCHOOL );
        tourModeChoiceUecIndexMap.put( "school_high", USUAL_WORK_AND_SCHOOL_LOCATION_MODE_CHOICE_UEC_INDEX_SCHOOL );

    }

    
    private void setMandatoryStopFreqUecSheetIndexValues () {
    	
    	stopFreqUecIndexMap.put( "work_occ1", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ2", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ3", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ4", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ5", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ6", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ7", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ8", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ9", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ10", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ11", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
    	stopFreqUecIndexMap.put( "work_occ12", MANDATORY_STOP_FREQ_UEC_INDEX_WORK );
        
    	stopFreqUecIndexMap.put( "university", MANDATORY_STOP_FREQ_UEC_INDEX_UNIVERSITY );
    	stopFreqUecIndexMap.put( "school_pre", MANDATORY_STOP_FREQ_UEC_INDEX_SCHOOL );
    	stopFreqUecIndexMap.put( "school_grade", MANDATORY_STOP_FREQ_UEC_INDEX_SCHOOL );
    	stopFreqUecIndexMap.put( "school_high", MANDATORY_STOP_FREQ_UEC_INDEX_SCHOOL );
    }
    
    private void setMandatoryStopLocUecSheetIndexValues () {
        stopLocUecIndexMap.put( WORK_PURPOSE_NAME, MANDATORY_STOP_LOC_UEC_INDEX_WORK );
        stopLocUecIndexMap.put( UNIVERSITY_PURPOSE_NAME, MANDATORY_STOP_LOC_UEC_INDEX_UNIVERSITY );
        stopLocUecIndexMap.put( SCHOOL_PURPOSE_NAME, MANDATORY_STOP_LOC_UEC_INDEX_SCHOOL );
    }

    private void setMandatoryTripModeChoiceUecSheetIndexValues () {
        tripModeChoiceUecIndexMap.put( WORK_PURPOSE_NAME, MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX_WORK );
        tripModeChoiceUecIndexMap.put( UNIVERSITY_PURPOSE_NAME, MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX_UNIVERSITY );
        tripModeChoiceUecIndexMap.put( SCHOOL_PURPOSE_NAME, MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX_SCHOOL );
    }
    


    private void setNonMandatoryPurposeNameValues() {
       
        // initialize index to the length of the mandatory names list already developed.
        int index = dcSizeArrayPurposeIndexMap.size();


        ESCORT_SEGMENT_NAMES = ESCORT_PURPOSE_SEGMENT_NAMES;

        // ESCORT is the only non-mandatory purpose with segments
        ArrayList<String> purposeNamesList = new ArrayList<String>();
        for ( int i=0; i < NON_MANDATORY_DC_PURPOSE_NAMES.length; i++ ) {
            if ( NON_MANDATORY_DC_PURPOSE_NAMES[i].equalsIgnoreCase( ESCORT_PURPOSE_NAME ) & getNumEscortSegments() > 0 ) {
                for ( int j=0; j < ESCORT_SEGMENT_NAMES.length; j++ ) {
                    String name = (ESCORT_PURPOSE_NAME + "_" + ESCORT_SEGMENT_NAMES[j]).toLowerCase();
                    purposeNamesList.add( name );
                    dcModelPurposeIndexMap.put( name, index );
                    dcModelIndexPurposeMap.put( index, name );
                    
                    // a separate size term is calculated for each non-mandatory purpose_segment 
                    dcSizeArrayIndexPurposeMap.put( index, name );
                    dcSizeArrayPurposeIndexMap.put( name, index );
                    index++;
                }
            }
            else {
                String name = NON_MANDATORY_DC_PURPOSE_NAMES[i].toLowerCase();
                purposeNamesList.add( name );
                dcModelPurposeIndexMap.put( name, index );
                dcModelIndexPurposeMap.put( index, name );

                // a separate size term is calculated for each non-mandatory purpose_segment 
                dcSizeArrayIndexPurposeMap.put( index, name+"_"+name );
                dcSizeArrayPurposeIndexMap.put( name+"_"+name, index );
                index++;
            }
        }

        int escortOffset = ESCORT_SEGMENT_NAMES.length;

        jointDcModelPurposeNames = new String[purposeNamesList.size()-escortOffset];
        nonMandatoryDcModelPurposeNames = new String[purposeNamesList.size()];
        for ( int i=0; i < purposeNamesList.size(); i++ ) {
            nonMandatoryDcModelPurposeNames[i] = purposeNamesList.get(i);
            if ( i > escortOffset - 1 )
                jointDcModelPurposeNames[i-escortOffset] = purposeNamesList.get(i);
        }

    }


    private void setNonMandatoryDcSoaUecSheetIndexValues () {
        dcSoaUecIndexMap.put( ESCORT_PURPOSE_NAME, NON_MANDATORY_SOA_UEC_INDEX_ESCORT );
        dcSoaUecIndexMap.put( SHOPPING_PURPOSE_NAME, NON_MANDATORY_SOA_UEC_INDEX_SHOPPING );
        dcSoaUecIndexMap.put( EAT_OUT_PURPOSE_NAME, NON_MANDATORY_SOA_UEC_INDEX_EAT_OUT );
        dcSoaUecIndexMap.put( OTH_MAINT_PURPOSE_NAME, NON_MANDATORY_SOA_UEC_INDEX_OTHER_MAINT );
        dcSoaUecIndexMap.put( SOCIAL_PURPOSE_NAME, NON_MANDATORY_SOA_UEC_INDEX_SOCIAL );
        dcSoaUecIndexMap.put( OTH_DISCR_PURPOSE_NAME, NON_MANDATORY_SOA_UEC_INDEX_OTHER_DISCR );
    }


    private void setNonMandatoryDcUecSheetIndexValues () {
        dcUecIndexMap.put( ESCORT_PURPOSE_NAME, NON_MANDATORY_DC_UEC_INDEX_ESCORT );
        dcUecIndexMap.put( SHOPPING_PURPOSE_NAME, NON_MANDATORY_DC_UEC_INDEX_SHOPPING );
        dcUecIndexMap.put( EAT_OUT_PURPOSE_NAME, NON_MANDATORY_DC_UEC_INDEX_EAT_OUT );
        dcUecIndexMap.put( OTH_MAINT_PURPOSE_NAME, NON_MANDATORY_DC_UEC_INDEX_OTHER_MAINT );
        dcUecIndexMap.put( SOCIAL_PURPOSE_NAME, NON_MANDATORY_DC_UEC_INDEX_SOCIAL );
        dcUecIndexMap.put( OTH_DISCR_PURPOSE_NAME, NON_MANDATORY_DC_UEC_INDEX_OTHER_DISCR );
    }


    private void setNonMandatoryModeChoiceUecSheetIndexValues () {
        tourModeChoiceUecIndexMap.put( ESCORT_PURPOSE_NAME, NON_MANDATORY_MC_UEC_INDEX_ESCORT );
        tourModeChoiceUecIndexMap.put( SHOPPING_PURPOSE_NAME, NON_MANDATORY_MC_UEC_INDEX_SHOPPING );
        tourModeChoiceUecIndexMap.put( EAT_OUT_PURPOSE_NAME, NON_MANDATORY_MC_UEC_INDEX_EAT_OUT );
        tourModeChoiceUecIndexMap.put( OTH_MAINT_PURPOSE_NAME, NON_MANDATORY_MC_UEC_INDEX_OTHER_MAINT );
        tourModeChoiceUecIndexMap.put( SOCIAL_PURPOSE_NAME, NON_MANDATORY_MC_UEC_INDEX_SOCIAL );
        tourModeChoiceUecIndexMap.put( OTH_DISCR_PURPOSE_NAME, NON_MANDATORY_MC_UEC_INDEX_OTHER_DISCR );
    }

    private void setNonMandatoryStopFreqUecSheetIndexValues () {
        stopFreqUecIndexMap.put( ESCORT_PURPOSE_NAME, NON_MANDATORY_STOP_FREQ_UEC_INDEX_ESCORT );
        stopFreqUecIndexMap.put( SHOPPING_PURPOSE_NAME, NON_MANDATORY_STOP_FREQ_UEC_INDEX_SHOPPING );
        stopFreqUecIndexMap.put( EAT_OUT_PURPOSE_NAME, NON_MANDATORY_STOP_FREQ_UEC_INDEX_EAT_OUT );
        stopFreqUecIndexMap.put( OTH_MAINT_PURPOSE_NAME, NON_MANDATORY_STOP_FREQ_UEC_INDEX_OTHER_MAINT );
        stopFreqUecIndexMap.put( SOCIAL_PURPOSE_NAME, NON_MANDATORY_STOP_FREQ_UEC_INDEX_SOCIAL );
        stopFreqUecIndexMap.put( OTH_DISCR_PURPOSE_NAME, NON_MANDATORY_STOP_FREQ_UEC_INDEX_OTHER_DISCR );
    }

    private void setNonMandatoryStopLocUecSheetIndexValues () {
        stopLocUecIndexMap.put( ESCORT_PURPOSE_NAME, NON_MANDATORY_STOP_LOC_UEC_INDEX_ESCORT );
        stopLocUecIndexMap.put( SHOPPING_PURPOSE_NAME, NON_MANDATORY_STOP_LOC_UEC_INDEX_SHOPPING );
        stopLocUecIndexMap.put( EAT_OUT_PURPOSE_NAME, NON_MANDATORY_STOP_LOC_UEC_INDEX_EAT_OUT );
        stopLocUecIndexMap.put( OTH_MAINT_PURPOSE_NAME, NON_MANDATORY_STOP_LOC_UEC_INDEX_OTHER_MAINT );
        stopLocUecIndexMap.put( SOCIAL_PURPOSE_NAME, NON_MANDATORY_STOP_LOC_UEC_INDEX_SOCIAL );
        stopLocUecIndexMap.put( OTH_DISCR_PURPOSE_NAME, NON_MANDATORY_STOP_LOC_UEC_INDEX_OTHER_DISCR );
    }

    private void setNonMandatoryTripModeChoiceUecSheetIndexValues () {
        tripModeChoiceUecIndexMap.put( ESCORT_PURPOSE_NAME, NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX );
        tripModeChoiceUecIndexMap.put( SHOPPING_PURPOSE_NAME, NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX );
        tripModeChoiceUecIndexMap.put( EAT_OUT_PURPOSE_NAME, NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX );
        tripModeChoiceUecIndexMap.put( OTH_MAINT_PURPOSE_NAME, NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX );
        tripModeChoiceUecIndexMap.put( SOCIAL_PURPOSE_NAME, NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX );
        tripModeChoiceUecIndexMap.put( OTH_DISCR_PURPOSE_NAME, NON_MANDATORY_TRIP_MODE_CHOICE_UEC_INDEX );
    }

    
    private void setAtWorkPurposeNameValues() {
        
        AT_WORK_PURPOSE_INDEX_EAT = ARC_AT_WORK_PURPOSE_INDEX_EAT;
        AT_WORK_PURPOSE_INDEX_BUSINESS = ARC_AT_WORK_PURPOSE_INDEX_BUSINESS;
        AT_WORK_PURPOSE_INDEX_MAINT = ARC_AT_WORK_PURPOSE_INDEX_MAINT;
        
        AT_WORK_SEGMENT_NAMES = new String[3];
        AT_WORK_SEGMENT_NAMES[0] = AT_WORK_EAT_PURPOSE_NAME;
        AT_WORK_SEGMENT_NAMES[1] = AT_WORK_BUSINESS_PURPOSE_NAME;
        AT_WORK_SEGMENT_NAMES[2] = AT_WORK_MAINT_PURPOSE_NAME;
        
        
        // initialize index to the length of the home-based tour names list already developed.
        int index = dcSizeArrayPurposeIndexMap.size();

        // the same size term is used by each at-work soa model 
        dcSizeArrayIndexPurposeMap.put( index, AT_WORK_PURPOSE_NAME+"_"+AT_WORK_PURPOSE_NAME );
        dcSizeArrayPurposeIndexMap.put( AT_WORK_PURPOSE_NAME+"_"+AT_WORK_PURPOSE_NAME, index );

        ArrayList<String> purposeNamesList = new ArrayList<String>();
        for ( int j=0; j < AT_WORK_SEGMENT_NAMES.length; j++ ) {
            String name = ( AT_WORK_PURPOSE_NAME + "_" + AT_WORK_SEGMENT_NAMES[j]).toLowerCase();
            purposeNamesList.add( name );
            dcModelPurposeIndexMap.put( name, index );
            dcModelIndexPurposeMap.put( index, name );
            index++;
        }

        
        atWorkDcModelPurposeNames = new String[purposeNamesList.size()];
        for ( int i=0; i < purposeNamesList.size(); i++ ) {
            atWorkDcModelPurposeNames[i] = purposeNamesList.get(i);
        }

    }


    private void setAtWorkDcSoaUecSheetIndexValues () {
        dcSoaUecIndexMap.put( "work-based_eat", AT_WORK_SOA_UEC_INDEX_EAT );
        dcSoaUecIndexMap.put( "work-based_business", AT_WORK_SOA_UEC_INDEX_BUSINESS );
        dcSoaUecIndexMap.put( "work-based_maint", AT_WORK_SOA_UEC_INDEX_MAINT );
    }


    private void setAtWorkDcUecSheetIndexValues () {
        dcUecIndexMap.put( "work-based_eat", AT_WORK_DC_UEC_INDEX_EAT );
        dcUecIndexMap.put( "work-based_business", AT_WORK_DC_UEC_INDEX_BUSINESS );
        dcUecIndexMap.put( "work-based_maint", AT_WORK_DC_UEC_INDEX_MAINT );
    }


    private void setAtWorkModeChoiceUecSheetIndexValues () {
        tourModeChoiceUecIndexMap.put( "work-based_eat", AT_WORK_MC_UEC_INDEX_EAT );
        tourModeChoiceUecIndexMap.put( "work-based_business", AT_WORK_MC_UEC_INDEX_BUSINESS );
        tourModeChoiceUecIndexMap.put( "work-based_maint", AT_WORK_MC_UEC_INDEX_MAINT );
    }


    private void setAtWorkStopFreqUecSheetIndexValues () {
        stopFreqUecIndexMap.put( "work-based_eat", AT_WORK_STOP_FREQ_UEC_INDEX_EAT );
        stopFreqUecIndexMap.put( "work-based_business", AT_WORK_STOP_FREQ_UEC_INDEX_BUSINESS );
        stopFreqUecIndexMap.put( "work-based_maint", AT_WORK_STOP_FREQ_UEC_INDEX_MAINT );
    }

    private void setAtWorkStopLocUecSheetIndexValues () {
        stopLocUecIndexMap.put( AT_WORK_PURPOSE_NAME, AT_WORK_STOP_LOC_UEC_INDEX );
    }

    private void setAtWorkTripModeChoiceUecSheetIndexValues () {
        tripModeChoiceUecIndexMap.put( AT_WORK_PURPOSE_NAME, AT_WORK_TRIP_MODE_CHOICE_UEC_INDEX );
    }

    

    
    public double[][] getCdap6PlusProps() {
        return CDAP_6_PLUS_PROPORTIONS;
    }
    

    
    public int getWorkLocationDefaultDepartPeriod() {    
        return DEFAULT_WORK_DEPART_INTERVAL;
    }

    public int getWorkLocationDefaultArrivePeriod() {
        return DEFAULT_WORK_ARRIVE_INTERVAL;
    }

    public int getSchoolLocationDefaultDepartPeriod() {
        return DEFAULT_SCHOOL_DEPART_INTERVAL;
    }

    public int getSchoolLocationDefaultArrivePeriod() {
        return DEFAULT_SCHOOL_ARRIVE_INTERVAL;
    }

    public int getUniversityLocationDefaultDepartPeriod() {
        return DEFAULT_UNIVERSITY_DEPART_INTERVAL;
    }

    public int getUniversityLocationDefaultArrivePeriod() {
        return DEFAULT_UNIVERSITY_ARRIVE_INTERVAL;
    }

    public int getNonMandatoryLocationDefaultDepartPeriod() {
        return DEFAULT_NON_MANDATORY_DEPART_INTERVAL;
    }

    public int getNonMandatoryLocationDefaultArrivePeriod() {
        return DEFAULT_NON_MANDATORY_ARRIVE_INTERVAL;
    }


    public int[] getPeriodCombinationIndices()
    {
        return PERIOD_COMBINATION_INDICES;
    }

    public int getPeriodCombinationIndex(int departInterval, int arriveInterval)
    {

        int departPeriodIndex = getPeriodIndex(departInterval);
        int arrivePeriodIndex = getPeriodIndex(arriveInterval);

        if ( PERIOD_COMBINATIONS[departPeriodIndex][arrivePeriodIndex] < 0 ) {
            String errorString = String.format( "departInterval=%d, arriveInterval=%d, departPeriodIndex=%d, arrivePeriodIndex=%d is invalid period combination.", departInterval, arriveInterval, departPeriodIndex, arrivePeriodIndex );
            throw new RuntimeException(errorString);
        }
        else {
            return PERIOD_COMBINATIONS[departPeriodIndex][arrivePeriodIndex];
        }

    }

    /**
     * return the period index
     */
    public int getPeriodIndex(int departPeriod)
    {

        int periodIndex = 0;

        if (departPeriod <= UPPER_1)
            periodIndex = P1;
        else if (departPeriod <= UPPER_2)
            periodIndex = P2;
        else if (departPeriod <= UPPER_3)
            periodIndex = P3;
        else if (departPeriod <= UPPER_4)
            periodIndex = P4;
        else if (departPeriod <= UPPER_5)
            periodIndex = P5;
        else if (departPeriod <= UPPER_6)
            periodIndex = P6;
        else if (departPeriod <= UPPER_7)
            periodIndex = P7;
        else
            periodIndex = P8;

        return periodIndex;

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


    public int getIntervalFor4Pm() {
        return TIME_INTERVAL_FOR_4_PM;
    }

    public int getIntervalFor7Pm() {
        return TIME_INTERVAL_FOR_7_PM;
    }

    public String getPeriodLabel(int period)
    {
        return PERIOD_LABELS[period];
    }

    
    
    
    public int getMaxTourModeIndex() {
        return MAXIMUM_TOUR_MODE_ALT_INDEX;
    }

    public void setJtfAltLabels( String[] labels )
    {
        jtfAltLabels = labels;
    }

    public String[] getJtfAltLabels()
    {
        return jtfAltLabels;
    }

}
    