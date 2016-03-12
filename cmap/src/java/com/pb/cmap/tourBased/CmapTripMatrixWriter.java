package com.pb.cmap.tourBased;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;

import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TripMatrixWriterIf;
import com.pb.models.ctrampIf.HouseholdDataManagerIf;

/** Calculate and write trip matrices to EMME
 * @author bts
 */
public class CmapTripMatrixWriter implements TripMatrixWriterIf {
    
    private transient Logger logger = Logger.getLogger(CmapTripMatrixWriter.class);
    
    private static final String PROPERTIES_INDIV_TRIP_DATA_FILE = "Results.IndivTripDataFile";
    private static final String PROPERTIES_JOINT_TRIP_DATA_FILE = "Results.JointTripDataFile";
    
    private static final String PROPERTIES_PROJECT_DIRECTORY = "Project.Directory";
    private static final String PROPERTIES_LOWVOT_UPPERBOUND = "Results.TripMatrices.LowVOTUpperBound";
    private static final String PROPERTIES_NUMZONES = "Results.TripMatrices.NumZones";
    private static final String PROPERTIES_FOLDER = "Results.TripMatrices.Folder";
    private static final String PROPERTIES_TIMEPERIOD_UPPERBOUNDS = "Results.TripMatrices.TimePeriodUpperBounds";
    private static final String PROPERTIES_TRIPMATRICES_NUMBERS = "Results.TripMatrices.Numbers";
    private static final String PROPERTIES_TRIPMATRICES_NAMES = "Results.TripMatrices.Names";
    private static final String PROPERTIES_TRANSITMATRICES_NUMBERS = "Results.TripMatrices.TransitNumbers";
    private static final String PROPERTIES_TRANSITMATRICES_NAMES = "Results.TripMatrices.TransitNames";
    private static final String PROPERTIES_NUMTRANSITZONES = "Results.TripMatrices.NumTransitZones";
    
    private static final float hov2occupancy = 2.0f;
    private static final float hov3occupancy = 3.3f;
    
    //mode choice alt codes
    private static final int tripModeCode_DRIVEALONEFREE = 1;
    private static final int tripModeCode_DRIVEALONEPAY	 = 2;
    private static final int tripModeCode_SHARED2FREE	 = 3;
    private static final int tripModeCode_SHARED2PAY	 = 4;
    private static final int tripModeCode_SHARED3FREE	 = 5;
    private static final int tripModeCode_SHARED3PAY     = 6;
    
    private static final int tripModeCode_WALK_LOCAL	 = 9;
    private static final int tripModeCode_WALK_TRANSIT   = 10;
    private static final int tripModeCode_KNR	         = 11;
    private static final int tripModeCode_PNR            = 12;

    private ResourceBundle resourceBundle;
    private HouseholdDataManagerIf householdDataManager;
    private int iteration;
    private float iterationSampleRate;
    private TableDataSet indivTrips;
    private TableDataSet jointTrips;
    private float lowVOTUpperBound;
    private int numZones;
    private int numTransitZones;
    private String folder;
    private int[] timePeriodUpperBounds;
    private String[] matrixNums;
    private String[] matrixNames;
    private Matrix[] matrices;
    
    private HashMap<Integer,Float> vots;
    private HashMap<Integer,Integer> oldestPersonId;
    private HashMap<Integer,Integer> user_class_work_walk;
    private HashMap<Integer,Integer> user_class_work_pnr;
    private HashMap<Integer,Integer> user_class_work_knr;   
    private HashMap<Integer,Integer> user_class_non_work_walk;
    private HashMap<Integer,Integer> user_class_non_work_pnr;
    private HashMap<Integer,Integer> user_class_non_work_knr;
    
    private String[] transitMatrixNums;
    private String[] transitMatrixNames;
    private Matrix[] transitMatrices;
    
    TazDataIf tazDataHandler;
    
    public CmapTripMatrixWriter( ResourceBundle resourceBundle, HouseholdDataManagerIf householdDataManager,  TazDataIf tazDataHandler, int iteration, float iterationSampleRate) {

        this.resourceBundle = resourceBundle;
        this.householdDataManager = householdDataManager;
        this.iterationSampleRate = iterationSampleRate;
        this.iteration = iteration;
        this.tazDataHandler = tazDataHandler;
        
        lowVOTUpperBound = Float.parseFloat(resourceBundle.getString(PROPERTIES_LOWVOT_UPPERBOUND));
        numZones = Integer.parseInt(resourceBundle.getString(PROPERTIES_NUMZONES));
        folder = ResourceUtil.getProperty(resourceBundle, PROPERTIES_PROJECT_DIRECTORY) + "/" + 
        		ResourceUtil.getProperty(resourceBundle, PROPERTIES_FOLDER);
        timePeriodUpperBounds = ResourceUtil.getIntegerArray(resourceBundle, PROPERTIES_TIMEPERIOD_UPPERBOUNDS);
        matrixNums = ResourceUtil.getArray(resourceBundle, PROPERTIES_TRIPMATRICES_NUMBERS);
        matrixNames = ResourceUtil.getArray(resourceBundle, PROPERTIES_TRIPMATRICES_NAMES);
        numTransitZones = Integer.parseInt(resourceBundle.getString(PROPERTIES_NUMTRANSITZONES));
        transitMatrixNums = ResourceUtil.getArray(resourceBundle, PROPERTIES_TRANSITMATRICES_NUMBERS);
        transitMatrixNames = ResourceUtil.getArray(resourceBundle, PROPERTIES_TRANSITMATRICES_NAMES);
    }
    
    public void writeMatrices() {

    	logger.info("Writing CMAP trip matrices to EMME databanks");
    	
    	try {
    		
    		//get person VOTs and user classes
    		getPersonVOTsAndUserClasses();
    		
    		//read output files in order to get create trip matrices
        	readIndivTrips();
        	readJointTrips();
        	
        	//loop through time periods and create and write matrices
        	for(int i=0; i<timePeriodUpperBounds.length; i++) {
        		calculateMatrices(i+1);
            	writeMatricesToFile(i+1, matrices, transitMatrices, folder);	
        	}
        	
        } catch (IOException e) {            
        	logger.error ( "IOException writing trip matrices", e );
        }	
    }
    
    public void calculateMatrices(int timePeriod) {
    	  	
    	//set start and end period values for the time period
    	int start_period = -1;
		int end_period = timePeriodUpperBounds[0];
		int maxBound = timePeriodUpperBounds[timePeriodUpperBounds.length-1];
    	if(timePeriod>1) {
    		start_period = timePeriodUpperBounds[timePeriod-2];
    		end_period = timePeriodUpperBounds[timePeriod-1];
    	} 
    	
    	//create output taz matrices for time period
    	matrices = new Matrix[matrixNames.length];
    	for(int i=0; i<matrices.length; i++) {
    		matrices[i] = new Matrix("mf" + timePeriod + matrixNums[i], matrixNames[i], numZones, numZones);
    	}
    	
    	//get tap ids
    	String tapIDFieldName = tazDataHandler.getMttData().getTapIdFieldName();
    	int[] tapIds = tazDataHandler.getMttData().getTapTable().getColumnAsInt(tapIDFieldName);
	    
    	//create output transit tap matrices for time period
    	transitMatrices = new Matrix[transitMatrixNames.length];
    	for(int i=0; i<transitMatrixNames.length; i++) {
    		transitMatrices[i] = new Matrix("mf" + timePeriod + transitMatrixNums[i], transitMatrixNames[i], numTransitZones, numTransitZones);
    		transitMatrices[i].setExternalNumbersZeroBased(tapIds);
    	}
    	
    	//get indivTrips fields
    	int[] hh_id = indivTrips.getColumnAsInt("hh_id");
    	int[] person_id = indivTrips.getColumnAsInt("person_id");
    	int[] orig_taz = indivTrips.getColumnAsInt("orig_taz");
    	int[] dest_taz = indivTrips.getColumnAsInt("dest_taz");
    	int[] parking_taz = indivTrips.getColumnAsInt("parking_taz");
    	int[] stop_period = indivTrips.getColumnAsInt("stop_period");
    	int[] trip_mode = indivTrips.getColumnAsInt("trip_mode");
    	int[] inbound = indivTrips.getColumnAsInt("inbound");          
    	
    	int[] btap = indivTrips.getColumnAsInt("board_tap");
    	int[] atap = indivTrips.getColumnAsInt("alight_tap");
    	String[] purpose = indivTrips.getColumnAsString("dest_purpose");
    	
    	for(int i=0; i<orig_taz.length; i++) {
    		
    		if((stop_period[i] > start_period & stop_period[i] <= end_period) | (stop_period[i] > maxBound & timePeriod==1)) { 
    			
    			//use parking taz instead for destination
    			if(parking_taz[i] > 0) {
    				dest_taz[i] = parking_taz[i];
    			}
    			
    			//low or high income matrices
    			if(vots.get(Integer.valueOf(person_id[i])) <= lowVOTUpperBound) {
    				
        			if(trip_mode[i] == tripModeCode_DRIVEALONEFREE) {
        				float value = matrices[0].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[0].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_DRIVEALONEPAY) {
        				float value = matrices[1].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[1].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2FREE) {
        				float value = matrices[4].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[4].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov2occupancy);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2PAY) {
        				float value = matrices[5].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[5].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov2occupancy);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED3FREE) {
        				float value = matrices[8].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[8].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov3occupancy);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED3PAY) {
        				float value = matrices[9].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[9].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov3occupancy);
        			}
    			
    			} else {
    			
        			if(trip_mode[i] == tripModeCode_DRIVEALONEFREE) { 
        				float value = matrices[2].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[2].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_DRIVEALONEPAY) { 
        				float value = matrices[3].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[3].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2FREE) { 
        				float value = matrices[6].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[6].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov2occupancy);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2PAY) { 
        				float value = matrices[7].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[7].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov2occupancy);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED3FREE) { 
        				float value = matrices[10].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[10].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov3occupancy);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED3PAY) { 
        				float value = matrices[11].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[11].setValueAt(orig_taz[i], dest_taz[i], value + 1/hov3occupancy);
        			}	
    			}
    			    			
    			//transit matrices
				if(trip_mode[i] == tripModeCode_WALK_LOCAL) { 
    				
					if(purpose[i].toLowerCase().startsWith("work")) { 
					
						if(user_class_work_walk.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[0].getValueAt(btap[i], atap[i]);
							transitMatrices[0].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_walk.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[1].getValueAt(btap[i], atap[i]);
							transitMatrices[1].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_walk.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[2].getValueAt(btap[i], atap[i]);
							transitMatrices[2].setValueAt(btap[i], atap[i], value + 1);
						}
					 
					} else {
						
						if(user_class_non_work_walk.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[0].getValueAt(btap[i], atap[i]);
							transitMatrices[0].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_walk.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[1].getValueAt(btap[i], atap[i]);
							transitMatrices[1].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_walk.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[2].getValueAt(btap[i], atap[i]);
							transitMatrices[2].setValueAt(btap[i], atap[i], value + 1);
						}
						
					}
				
    			}
				
				if(trip_mode[i] == tripModeCode_WALK_TRANSIT) { 
    				
					if(purpose[i].toLowerCase().startsWith("work")) { 
					
						if(user_class_work_walk.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
							transitMatrices[3].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_walk.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
							transitMatrices[4].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_walk.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
							transitMatrices[5].setValueAt(btap[i], atap[i], value + 1);
						}
					
					} else {
						
						if(user_class_non_work_walk.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
							transitMatrices[3].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_walk.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
							transitMatrices[4].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_walk.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
							transitMatrices[5].setValueAt(btap[i], atap[i], value + 1);
						}
						
					}
    			}
				    			
    			if(trip_mode[i] == tripModeCode_KNR) { 
    				
    				if(purpose[i].startsWith("work")) { 
					
	    				if(user_class_work_knr.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
							transitMatrices[3].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_knr.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
							transitMatrices[4].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_knr.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
							transitMatrices[5].setValueAt(btap[i], atap[i], value + 1);
						}
						
    				} else {
    					
    					if(user_class_non_work_knr.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
							transitMatrices[3].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_knr.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
							transitMatrices[4].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_knr.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
							transitMatrices[5].setValueAt(btap[i], atap[i], value + 1);
						}
    					
    				}
    			}
    			
    			if(trip_mode[i] == tripModeCode_PNR) { 
    				
    				if(purpose[i].startsWith("work")) { 
					
	    				if(user_class_work_pnr.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
							transitMatrices[3].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_pnr.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
							transitMatrices[4].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_work_pnr.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
							transitMatrices[5].setValueAt(btap[i], atap[i], value + 1);
						}
						
    				} else {
    					
    					if(user_class_non_work_pnr.get(Integer.valueOf(person_id[i])) == 1) { 
							float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
							transitMatrices[3].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_pnr.get(Integer.valueOf(person_id[i])) == 2) { 
							float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
							transitMatrices[4].setValueAt(btap[i], atap[i], value + 1);
						}
						
						if(user_class_non_work_pnr.get(Integer.valueOf(person_id[i])) == 3) { 
							float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
							transitMatrices[5].setValueAt(btap[i], atap[i], value + 1);
						}
    					
    				}
    			}
        			    			
    		}
    	}
    	
    	//get jointTrips fields
    	hh_id = indivTrips.getColumnAsInt("hh_id");
    	orig_taz = jointTrips.getColumnAsInt("orig_taz");
    	dest_taz = jointTrips.getColumnAsInt("dest_taz");
    	parking_taz = jointTrips.getColumnAsInt("parking_taz");
    	stop_period = jointTrips.getColumnAsInt("stop_period");
    	trip_mode = jointTrips.getColumnAsInt("trip_mode");
    	inbound = jointTrips.getColumnAsInt("inbound");
    	int[] num_participants = jointTrips.getColumnAsInt("num_participants");  //for transit trips
    	
    	btap = jointTrips.getColumnAsInt("board_tap");
    	atap = jointTrips.getColumnAsInt("alight_tap");
    	purpose = indivTrips.getColumnAsString("dest_purpose");
    	    	
    	for(int i=0; i<orig_taz.length; i++) {
    		
    		if((stop_period[i] > start_period & stop_period[i] <= end_period) | (stop_period[i] > maxBound & timePeriod==1)) {
    			
    		    //get hh level attributes
    		    float hh_vot = vots.get(oldestPersonId.get(Integer.valueOf(hh_id[i])));
    		    int hh_user_class_non_work_walk = user_class_non_work_walk.get(oldestPersonId.get(Integer.valueOf(hh_id[i])));
    		    int hh_user_class_non_work_pnr = user_class_non_work_pnr.get(oldestPersonId.get(Integer.valueOf(hh_id[i])));
    		    int hh_user_class_non_work_knr = user_class_non_work_knr.get(oldestPersonId.get(Integer.valueOf(hh_id[i])));
    		    
    			//use parking taz instead for destination
    			if(parking_taz[i] > 0) {
    				dest_taz[i] = parking_taz[i];
    			}
    			
    			//low or high income matrices
    			if(hh_vot <= lowVOTUpperBound) {

        			if(trip_mode[i] == tripModeCode_DRIVEALONEFREE) { 
        				float value = matrices[0].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[0].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_DRIVEALONEPAY) { 
        				float value = matrices[1].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[1].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2FREE) { 
        				float value = matrices[4].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[4].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2PAY) { 
        				float value = matrices[5].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[5].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			 
        			if(trip_mode[i] == tripModeCode_SHARED3FREE) { 
        				float value = matrices[8].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[8].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED3PAY) { 
        				float value = matrices[9].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[9].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
    			
    			} else {
    				
        			if(trip_mode[i] == tripModeCode_DRIVEALONEFREE) { 
        				float value = matrices[2].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[2].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_DRIVEALONEPAY) { 
        				float value = matrices[3].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[3].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2FREE) { 
        				float value = matrices[6].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[6].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED2PAY) { 
        				float value = matrices[7].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[7].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        		
        			if(trip_mode[i] == tripModeCode_SHARED3FREE) { 
        				float value = matrices[10].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[10].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
        			
        			if(trip_mode[i] == tripModeCode_SHARED3PAY) { 
        				float value = matrices[11].getValueAt(orig_taz[i], dest_taz[i]);
        				matrices[11].setValueAt(orig_taz[i], dest_taz[i], value + 1);
        			}
    			}
    			
   				//transit matrices
    			if(trip_mode[i] == tripModeCode_WALK_LOCAL) { 
    				
					if(hh_user_class_non_work_walk == 1) { 
						float value = transitMatrices[0].getValueAt(btap[i], atap[i]);
	    				transitMatrices[0].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_walk == 2) { 
						float value = transitMatrices[1].getValueAt(btap[i], atap[i]);
	    				transitMatrices[1].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_walk == 3) { 
						float value = transitMatrices[2].getValueAt(btap[i], atap[i]);
	    				transitMatrices[2].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
    				
    			}
    			
    			if(trip_mode[i] == tripModeCode_WALK_TRANSIT ) { 
    				
					if(hh_user_class_non_work_walk == 1) { 
						float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
	    				transitMatrices[3].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_walk == 2) { 
						float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
	    				transitMatrices[4].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_walk == 3) { 
						float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
	    				transitMatrices[5].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
    				
    			}
    			    			
    			if(trip_mode[i] == tripModeCode_KNR) { 
					
    				if(hh_user_class_non_work_knr == 1) { 
						float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
	    				transitMatrices[3].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_knr == 2) { 
						float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
	    				transitMatrices[4].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_knr == 3) { 
						float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
	    				transitMatrices[5].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
    			}
    			
    			if(trip_mode[i] == tripModeCode_PNR) { 
					
    				if(hh_user_class_non_work_pnr == 1) { 
						float value = transitMatrices[3].getValueAt(btap[i], atap[i]);
	    				transitMatrices[3].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_pnr == 2) { 
						float value = transitMatrices[4].getValueAt(btap[i], atap[i]);
	    				transitMatrices[4].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
					
					if(hh_user_class_non_work_pnr == 3) { 
						float value = transitMatrices[5].getValueAt(btap[i], atap[i]);
	    				transitMatrices[5].setValueAt(btap[i], atap[i], value + num_participants[i]);
					}
    			}
    			
    		}
    	}
    	
    	//scale matrices by sample rate
    	for(int i=0; i<matrices.length; i++) {
    		matrices[i].scale(1/iterationSampleRate);
    	}
    	
    	//scale transitMatrices by sample rate
    	for(int i=0; i<transitMatrices.length; i++) {
    		transitMatrices[i].scale(1/iterationSampleRate);
    	}
    }
    
    public void writeMatricesToFile(int timePeriod, Matrix[] matrices, Matrix[] transitMatrices, String folder) {
    
		//highway
    	for(int i=0; i<matrices.length; i++) {
    		
    		//zmx files
    		String matName = matrices[i].getName() + ".zmx";
	    	MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(folder + "/" + matName));
    		logger.info("Writing " + matName + " " + matrices[i].getDescription() + " to " + matName + ", sum " + matrices[i].getSum());
    		mw.writeMatrix(matName, matrices[i]);
    	}
    	
    	//transit
    	for(int i=0; i<transitMatrices.length; i++) {
    		
    		//zmx files
    		String matName = transitMatrices[i].getName() + ".zmx";
    		MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP, new File(folder + "/" + matName));
    		logger.info("Writing " + matName + " " + transitMatrices[i].getDescription() + " to " + matName + ", sum " + transitMatrices[i].getSum());
    		mw.writeMatrix(matName, transitMatrices[i]);
    	}
    	   	
    }
    
    public void getPersonVOTsAndUserClasses() throws IOException {
    	
    	//create person VOT hash map
    	vots = new HashMap<Integer,Float>(); 
    	oldestPersonId = new HashMap<Integer,Integer>();
    	
    	//create user class hash maps
        user_class_work_walk = new HashMap<Integer,Integer>();
        user_class_work_pnr = new HashMap<Integer,Integer>();
        user_class_work_knr = new HashMap<Integer,Integer>();
        user_class_non_work_walk = new HashMap<Integer,Integer>();
        user_class_non_work_pnr = new HashMap<Integer,Integer>();
        user_class_non_work_knr = new HashMap<Integer,Integer>();

    	//loop through households and add person measures to hash maps
    	int numhhs = householdDataManager.getNumHouseholds();
    	for (int i=0; i<numhhs; i++) {

            HouseholdIf[] householdArray = householdDataManager.getHhArray(i,i);
            PersonIf[] personArray = householdArray[0].getPersons();
            oldestPersonId.put(Integer.valueOf(householdArray[0].getHhId()), Integer.valueOf(householdArray[0].getOldestPerson().getPersonId()));
                        
            for (int j=1; j<personArray.length; j++) { //index 0 is null
            
            	//get vot
            	int per_id = personArray[j].getPersonId();
            	float per_vot = personArray[j].getValueOfTime();
            	vots.put(Integer.valueOf(per_id), Float.valueOf(per_vot));
            	
            	//get user class attributes
            	user_class_work_walk.put(Integer.valueOf(per_id), personArray[j].getUserClass("user_class_work_walk"));
            	user_class_work_pnr.put(Integer.valueOf(per_id), personArray[j].getUserClass("user_class_work_pnr"));
            	user_class_work_knr.put(Integer.valueOf(per_id), personArray[j].getUserClass("user_class_work_knr"));
            	
            	user_class_non_work_walk.put(Integer.valueOf(per_id), personArray[j].getUserClass("user_class_non_work_walk"));
            	user_class_non_work_pnr.put(Integer.valueOf(per_id), personArray[j].getUserClass("user_class_non_work_pnr"));
            	user_class_non_work_knr.put(Integer.valueOf(per_id), personArray[j].getUserClass("user_class_non_work_knr"));
            	
            }
    	}
    }

    public void readIndivTrips() throws IOException {
    	 	
    	String[] colsToRead = {"hh_id","person_id","orig_taz","dest_taz","parking_taz","stop_period","trip_mode","inbound","board_tap","alight_tap","dest_purpose"};
    	String fileName = formFileName(resourceBundle.getString(PROPERTIES_INDIV_TRIP_DATA_FILE), iteration);
    	
        CSVFileReader csvReader = new CSVFileReader();
        indivTrips = new TableDataSet();
        try {
        	logger.info("Reading " + fileName);
        	indivTrips = csvReader.readFile( new File(fileName), colsToRead);
        } catch (IOException e) {
            logger.error ( "IOException reading file: " + PROPERTIES_INDIV_TRIP_DATA_FILE, e );
            throw new IOException();
        }
    }
    
    public void readJointTrips() throws IOException {
    	
    	String[] colsToRead = {"hh_id","orig_taz","dest_taz","parking_taz","stop_period","trip_mode","inbound","num_participants","board_tap","alight_tap","dest_purpose"};
    	String fileName = formFileName(resourceBundle.getString(PROPERTIES_JOINT_TRIP_DATA_FILE), iteration);
    	
    	CSVFileReader csvReader = new CSVFileReader();
        jointTrips = new TableDataSet();
        try {
        	logger.info("Reading " + fileName);
        	jointTrips = csvReader.readFile( new File(fileName), colsToRead);
        } catch (IOException e) {
            logger.error ( "IOException reading file: " + PROPERTIES_JOINT_TRIP_DATA_FILE, e );
            throw new IOException();
        }
    }
    
    private String formFileName( String originalFileName, int iteration ) {
        int lastDot = originalFileName.lastIndexOf('.');
        
        String returnString = "";
        if ( lastDot > 0 ) {
            String base = originalFileName.substring( 0, lastDot );
            String ext = originalFileName.substring( lastDot );
            returnString = String.format( "%s_%d%s", base, iteration, ext );
        }
        else {
            returnString = String.format( "%s_%d.csv", originalFileName, iteration );
        }
        return returnString;
    }
    
}
