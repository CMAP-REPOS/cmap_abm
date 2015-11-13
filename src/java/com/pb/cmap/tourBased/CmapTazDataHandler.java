package com.pb.cmap.tourBased;


import com.pb.models.ctrampIf.TazDataHandler;
import com.pb.common.util.ResourceUtil;

import java.util.ResourceBundle;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;
import com.pb.cmap.tvpb.MazTapTazData;

public class CmapTazDataHandler extends TazDataHandler {
	
    private static final String ZONE_DATA_AREATYPE_FIELD_NAME = "areatype";
    private static final String ZONE_DATA_DISTRICT_FIELD_NAME = "district";
    private static final String ZONE_DATA_GRADESCHOOL_DISTRICT_FIELD_NAME = "gradeschool";
    private static final String ZONE_DATA_HIGHSCHOOL_DISTRICT__FIELD_NAME = "highschool";

    private static final int CBD_AREA_TYPE = 0;
    private static final int URBAN_AREA_TYPE = 1;
    private static final int SUBURBAN_AREA_TYPE = 2;
    private static final int RURAL_AREA_TYPE = 3;
    private static final int[][] AREA_TYPES = { {1}, {2, 3}, {4, 5, 6}, {7} };

    public static final String ZONAL_DATA_SERVER_NAME = CmapTazDataHandler.class.getCanonicalName();
    public static final String ZONAL_DATA_SERVER_ADDRESS = "192.168.1.212";
    public static final int ZONAL_DATA_SERVER_PORT = 1138;

    private MazTapTazData mttData;
    

	public CmapTazDataHandler( ResourceBundle rb, String projectDirectory, MazTapTazData mttData){
		super( rb, projectDirectory );

        tazDataAtFieldName = ZONE_DATA_AREATYPE_FIELD_NAME;
        tazDataDistFieldName = ZONE_DATA_DISTRICT_FIELD_NAME;
        tazDataGradeSchoolFieldName = ZONE_DATA_GRADESCHOOL_DISTRICT_FIELD_NAME;
        tazDataHighSchoolFieldName = ZONE_DATA_HIGHSCHOOL_DISTRICT__FIELD_NAME;

        cbdAreaTypesArrayIndex = CBD_AREA_TYPE;
        urbanAreaTypesArrayIndex = URBAN_AREA_TYPE;
        suburbanAreaTypesArrayIndex = SUBURBAN_AREA_TYPE;
        ruralAreaTypesArrayIndex = RURAL_AREA_TYPE;
        areaTypes = AREA_TYPES;

        setupTazDataManager();
        
        //set mttData
        this.mttData = mttData;

    }

     private static void usage( String[] args ) {
        System.out.println( String.format( "improper arguments." ) );
        if (args.length == 0 ) {
            System.out.println ( String.format( "no properties file specified." ) );
            System.out.println ( String.format( "a properties file base name (without .properties extension) must be specified as the first argument." ) );
        }
        else if (args.length >= 1 ) {
            System.out.println ( String.format( "improper properties file specified." ) );
            System.out.println ( String.format( "a properties file base name (without .properties extension) must be specified as the first argument." ) );
        }
    }


     public float getOMazDMazDistance(int omaz, int dmaz) {
     	return(mttData.getOMazDMazDistance(omaz, dmaz));
     }
     
     public float[] getOMazDistances(int omaz) {
     	return(mttData.getOMazDistances(omaz));
     }
     
     public float[] getAltDistances(int alt) {
     	return(mttData.getDMazDistances(alt));
     }
     
     public int getHasTransitAccess(int maz) {
      	return(mttData.getHasTransitAccess(maz));
      }
     
     public int[] getTazsForMazs() {
         return (mttData.getTazsForMazs());
     }
     
     public MazTapTazData getMttData() {
     	return mttData;
     }
}
