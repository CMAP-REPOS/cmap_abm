package com.pb.cmap.tvpb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ForkJoinPool;

import com.pb.cmap.tvpb.MazTapTazData.TapDist;
import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.calculator.VariableTable;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.ctrampIf.MatrixDataServer;
import com.pb.models.ctrampIf.MatrixDataServerRmi;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.datafile.TableDataSet;

import com.pb.sawdust.calculator.Function1;
import com.pb.sawdust.util.concurrent.DnCFunctionalTask;
import com.pb.sawdust.util.concurrent.ForkJoinPoolFactory;

import org.apache.log4j.Logger;

public class TransitVirtualPathBuilder implements Serializable {
	
    private static Logger logger = Logger.getLogger(TransitVirtualPathBuilder.class);
    private transient Logger tvpbLogger = Logger.getLogger("tvpbLog");
    private HashMap<String, String> propertyMap;
    
    private static final String TVPB_UEC = "tvpb.uec";
    private static final int DATA_SHEET = 0;
    private static final int MAZ2TAP_SHEET = 1;
    private static final int OTAP_SHEET = 2;
    private static final int TAP2TAP_SHEET = 3;
    private static final int DTAP_SHEET = 4;
    private static final int TAP2MAZ_SHEET = 5;
    private static final int TAZDISTANCE_SHEET = 6;
    private static final int forkJoinMinCalcSize = 10;
    
    private ThreadLocal<UtilityExpressionCalculator[]> threadSafeUECs; 
    private UtilityExpressionCalculator[] uecs;
    private Trip[] trips;
    private MazTapTazData mttData;
    private MatrixDataServerIf ms;
    private String uecFileName;
    private boolean runThreaded;
    private double maxWalkDist;
    private double maxTotalWalkDist;
    private double maxPnrDist;
    private int alts;
    private int[] nAlts = new int[alts+1];
    private boolean trace;
    
    private int maxWalkPaths;
    private int maxKNRPaths;
    private int maxPNRPaths;
    
    private int NA_VALUE = 0; //since using gen cost
    
    public TransitVirtualPathBuilder(HashMap<String, String> propertyMap) {
    	this.propertyMap = propertyMap;
    }
    
	public static void main(String[] args) {
        
		// get the properties file
		logger.info("Running Transit Virtual Path Builder");
        ResourceBundle resourceBundle = ResourceUtil.getResourceBundle("cmap");
        HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(resourceBundle);     
		TransitVirtualPathBuilder tvpb = new TransitVirtualPathBuilder(propertyMap);
		
		//setup and run
		tvpb.setupTVPB();
		tvpb.setupModels();
		tvpb.readTripData();
		tvpb.createMazTapTazData();
		tvpb.calculatePaths();
		tvpb.writeOutputData();
		
		//all done
		logger.info("Transit Virtual Path Builder Done");
	}
	
    public void setupTVPB() {
        
		//setup
		connectToMatrixServer();
		getProperties();
	}
	
	public void getProperties() {
			
        // get properties
		String projectDirectory = propertyMap.get("Project.Directory");
        String uecFile = propertyMap.get(TVPB_UEC);
        uecFileName = projectDirectory + uecFile;
        runThreaded = Boolean.parseBoolean(propertyMap.get("tvpb.runthreaded"));
        maxWalkDist = Double.parseDouble(propertyMap.get("tvpb.maxwalkdist"));
        maxTotalWalkDist = Double.parseDouble(propertyMap.get("tvpb.maxtotalwalkdist"));
        maxPnrDist = Double.parseDouble(propertyMap.get("tvpb.maxpnrdist"));
        alts = Integer.parseInt(propertyMap.get("tvpb.alts"));
        trace = Boolean.parseBoolean(propertyMap.get("tvpb.trace"));
        
		//path set trimming settings
		maxWalkPaths = Integer.parseInt(propertyMap.get("tvpb.maxpaths.walk"));
		maxKNRPaths = Integer.parseInt(propertyMap.get("tvpb.maxpaths.knr"));
		maxPNRPaths = Integer.parseInt(propertyMap.get("tvpb.maxpaths.pnr"));
		
	}
	
	public void setupModels() {
		
        //create a set of UEC objects to use for the components
        if(runThreaded) {
        	threadSafeUECs = new ThreadLocal<UtilityExpressionCalculator[]>() {
            	protected UtilityExpressionCalculator[] initialValue() {
            		
            		tvpbLogger.info("Create Thread Safe UECs");
            		TVPBDMU tvpbDmu = new TVPBDMU();
            		UtilityExpressionCalculator[] uecs = new UtilityExpressionCalculator[5];
                    uecs[0] = new UtilityExpressionCalculator(new File(uecFileName), MAZ2TAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
                    uecs[1] = new UtilityExpressionCalculator(new File(uecFileName), OTAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
                    uecs[2] = new UtilityExpressionCalculator(new File(uecFileName), TAP2TAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
                    uecs[3] = new UtilityExpressionCalculator(new File(uecFileName), DTAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
                    uecs[4] = new UtilityExpressionCalculator(new File(uecFileName), TAP2MAZ_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
                    return(uecs);
            	}
             };
        } else {
        	TVPBDMU tvpbDmu = new TVPBDMU();
    		uecs = new UtilityExpressionCalculator[5];
            uecs[0] = new UtilityExpressionCalculator(new File(uecFileName), MAZ2TAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
            uecs[1] = new UtilityExpressionCalculator(new File(uecFileName), OTAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
            uecs[2] = new UtilityExpressionCalculator(new File(uecFileName), TAP2TAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
            uecs[3] = new UtilityExpressionCalculator(new File(uecFileName), DTAP_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
            uecs[4] = new UtilityExpressionCalculator(new File(uecFileName), TAP2MAZ_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
        }
         
     	//create alternatives available array
        nAlts = new int[alts+1];
        nAlts[0] = 0;
        nAlts[1] = 1;
        if(alts==2) { nAlts[2] = 1; }
        
	}
	
	public void connectToMatrixServer() {
		
        //attach process to matrix server
		tvpbLogger.info("Connect to Matrix Data Server");
        String matrixServerAddress = propertyMap.get("RunModel.MatrixServerAddress");
        int serverPort = Integer.parseInt(propertyMap.get("RunModel.MatrixServerPort"));
        ms = new MatrixDataServerRmi( matrixServerAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME );
        MatrixDataManager mdm = MatrixDataManager.getInstance();
        ms.testRemote(Thread.currentThread().getName());
        mdm.setMatrixDataServerObject( ms );
	}
	
	public void readTripData() {
		
        //read trip records
		logger.info("Read Trip Records");
      	try {
      		Trip aTripForReadingTrips = new Trip();
      		trips = aTripForReadingTrips.readTripsFromCSV(propertyMap);
      		
      		//set all trips to use the max walk threshold
      		for(int j=0; j<trips.length; j++) {
				trips[j].setMaxWalkInFeet((float)maxWalkDist);
			}
      		
        } catch (IOException e) {
           	logger.error ( "IOException reading trip file");
        }
	}
	
	
	public void createMazTapTazData() {
		
		//create maz, tap, taz data manager
		logger.info("Read maz, tap, maz2tap Input Tables");
		try {
			mttData = new MazTapTazData(propertyMap);
        } catch (IOException e) {
           	logger.error ( "IOException reading maz, tap, maz2tap tables");
        }
		
        //set indexes and calculate taz distance matrix
		logger.info("Get taz distance matrix");
				
		//set uec
		TVPBDMU tvpbDmu = new TVPBDMU();
        UtilityExpressionCalculator uecDistance = new UtilityExpressionCalculator(new File(uecFileName), TAZDISTANCE_SHEET, DATA_SHEET, propertyMap, tvpbDmu );
		
		//solve uec
        int[] nAlts = {0,1};
  		int[] tazs = mttData.getTazs();
		Matrix tazDistance = new Matrix(tazs.length, tazs.length);
		tazDistance.setExternalNumbersZeroBased(tazs);
		for(int i=0; i<tazs.length; i++) {
			for(int j=0; j<tazs.length; j++) {
				tvpbDmu.setDmuIndexValues(-1, tazs[i], tazs[j]);
				double value = uecDistance.solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts)[0];
				tazDistance.setValueAt(tazs[i], tazs[j], (float) value);
			}
		}
		
		//create pnr data and maz to maz distance matrix
		logger.info("Calculate pnr tap data for later use");
		mttData.setDistanceMatrix(tazDistance);
		mttData.calculatePnrTapData(maxPnrDist);
		logger.info("Create maz to maz distance matrix");
		mttData.createMazDistanceMatrix(); //very big matrix
		
	}
	
	public void calculatePaths() {
	
		//fork join helper function
		Function1<Trip,Void> calculatePathsForATripFJ = new Function1<Trip,Void>() {
			@Override
			public Void apply(Trip trip) {
				calculatePathsForATrip(trip, true, true, true);
				return null;
			};
		};
		
		//create fork join task and execute it
		if(runThreaded) {
			DnCFunctionalTask<Trip, Void> doDNC = new DnCFunctionalTask<Trip,Void>(trips,calculatePathsForATripFJ,forkJoinMinCalcSize);
			new ForkJoinPool().execute(doDNC);
			doDNC.getResult();
		} else {
			for(int j=0; j<trips.length; j++) {
				calculatePathsForATrip(trips[j], true, true, true);
			}
		}
		
	}
	
	public void calculatePathsForATrip(Trip trip, boolean walk, boolean pnr, boolean knr) {
		
		//todo - assumes 1 alt for now
		
		//create dmu and set tap table
		TVPBDMU tvpbDmu = new TVPBDMU();
        tvpbDmu.setTapTable(mttData.getTapTable(), mttData.getTapIdFieldName());
		
        //create tap pairs to add to
        trip.setTapPairs(new ArrayList<TapPair>());
        
		if(walk) {
			ArrayList<TapPair> tapPairs = new ArrayList<TapPair>();
			setTVPBDmuValues(tvpbDmu, trip, true, false, false);
			calculateWalkPathsForATrip(trip, tapPairs, tvpbDmu);
			trip.getTapPairs().addAll(trimPaths(trip, tapPairs, true, false, false));
		}
		if(pnr) {
			ArrayList<TapPair> tapPairs = new ArrayList<TapPair>();
			setTVPBDmuValues(tvpbDmu, trip, false, true, false);
			calculatePnrPathsForATrip(trip, tapPairs, tvpbDmu);			
			trip.getTapPairs().addAll(trimPaths(trip, tapPairs, false, true, false));
		}
		if(knr) {
			ArrayList<TapPair> tapPairs = new ArrayList<TapPair>();
			setTVPBDmuValues(tvpbDmu, trip, false, false, true);
			calculateKnrPathsForATrip(trip, tapPairs, tvpbDmu);
			trip.getTapPairs().addAll(trimPaths(trip, tapPairs, false, false, true));
		}
	}
	
	public void calculateWalkPathsForATrip(Trip trip, ArrayList<TapPair> tapPairs, TVPBDMU tvpbDmu) {
		
		//TAZ O-D trip distance for getting near taps
		float tripDistTaz = mttData.getOTazDTazDistance(trip.otaz, trip.dtaz);
		
		//walk taps
		TapDist[] omazTaps = mttData.getOmazWalkTaps(trip.getOmaz(), Math.min(tripDistTaz, trip.getMaxWalkInFeet()));
		TapDist[] dmazTaps = mttData.getDmazWalkTaps(trip.getDmaz(), Math.min(tripDistTaz, trip.getMaxWalkInFeet()));
		if(omazTaps!=null & dmazTaps!=null) {
		
			//calculate walk otaps utils
			double[] maz2tapUtils = new double[omazTaps.length];
			double[] otapUtils = new double[omazTaps.length];

			for(int j=0; j<omazTaps.length; j++) {
				int taz = mttData.getTazForTap(omazTaps[j].tap);
				tvpbDmu.setMaz2TapDistance(omazTaps[j].dist);
				maz2tapUtils[j] = calculateUtility_MAZ2TAP(tvpbDmu, trip, omazTaps[j].tap, taz, nAlts);
				if(maz2tapUtils[j] != NA_VALUE) {
					otapUtils[j] =  calculateUtility_OTAP(tvpbDmu, trip, omazTaps[j].tap, taz, nAlts);
				}
			}
			
			//calculate walk dtaps utils
			double[] dtapUtils = new double[dmazTaps.length];
			double[] tap2mazUtils = new double[dmazTaps.length];
			
			for(int j=0; j<dmazTaps.length; j++) {
				int taz = mttData.getTazForTap(dmazTaps[j].tap);
				tvpbDmu.setMaz2TapDistance(dmazTaps[j].dist);
				tap2mazUtils[j] = calculateUtility_TAP2MAZ(tvpbDmu, trip, dmazTaps[j].tap, taz, nAlts);
				if(tap2mazUtils[j] != NA_VALUE) {
					dtapUtils[j] = calculateUtility_DTAP(tvpbDmu, trip, dmazTaps[j].tap, taz, nAlts);
				}
			}
			
			//calculate tap to tap and entire path
			if(trip.getDebugRecord()) {
				tvpbLogger.info("Walk tap pairs considered for trip record " + trip.getRecid() + " = " + omazTaps.length * dmazTaps.length);
			}
			
			for(int j=0; j<omazTaps.length; j++) {
				for(int k=0; k<dmazTaps.length; k++) {
					
					//only if all other path components valid
					if(maz2tapUtils[j] != NA_VALUE & otapUtils[j] != NA_VALUE & dtapUtils[k] != NA_VALUE & tap2mazUtils[k] != NA_VALUE) {
					
						//walk (only if O + D < max total walk)
						if ((omazTaps[j].dist + dmazTaps[k].dist) <= maxTotalWalkDist) {
							
							double tap2tapUtil = calculateUtility_TAP2TAP(tvpbDmu, trip, omazTaps[j].tap, dmazTaps[k].tap, nAlts);
							
							if( tap2tapUtil != NA_VALUE) {
								
								TapPair wTapPair = new TapPair(omazTaps[j].tap, dmazTaps[k].tap, true, false, false, alts);								
								wTapPair.maz2tapUtils[0] = maz2tapUtils[j];
								wTapPair.otapUtils[0] = otapUtils[j];
								wTapPair.tap2tapUtils[0] = tap2tapUtil;
								wTapPair.dtapUtils[0] = dtapUtils[k];
								wTapPair.tap2mazUtils[0] = tap2mazUtils[k];
								tapPairs.add(wTapPair);
								
							}
		
						}
					
					}
				}
			}
		
		} else {
			if(trip.getDebugRecord()) {
				tvpbLogger.info("Walk tap pairs considered for trip record " + trip.getRecid() + " = " + 0);
			}
		}
				
	}
	
	public void calculateKnrPathsForATrip(Trip trip, ArrayList<TapPair> tapPairs, TVPBDMU tvpbDmu) {
				
		//TAZ O-D trip distance for getting near taps
		float tripDistTaz = mttData.getOTazDTazDistance(trip.otaz, trip.dtaz);
				
		//knr tap pairs, depends on trip direction
		TapDist[] omazTaps;
		TapDist[] dmazTaps;
		if(trip.getInbound()==1) {
			omazTaps = mttData.getOmazWalkTaps(trip.getOmaz(), Math.min(tripDistTaz, trip.getMaxWalkInFeet()));
			dmazTaps = mttData.getDmazWalkTaps(trip.getDmaz(), tripDistTaz);
		} else {
			omazTaps = mttData.getOmazWalkTaps(trip.getOmaz(), tripDistTaz);
			dmazTaps = mttData.getDmazWalkTaps(trip.getDmaz(), Math.min(tripDistTaz, trip.getMaxWalkInFeet()));
		}
		
		if(omazTaps!=null & dmazTaps!=null) {
			
			//calculate knr otaps utils
			double[] maz2tapUtils = new double[omazTaps.length];
			double[] otapUtils = new double[omazTaps.length];
			
			for(int j=0; j<omazTaps.length; j++) {
				int taz = mttData.getTazForTap(omazTaps[j].tap);
	        	if(tvpbDmu.getInbound()==0) {
	        		tvpbDmu.setClosestTapTaz(mttData.getClosestTapTaz(trip.getOmaz())); //for outbound case
	        	} else {
	        		tvpbDmu.setMaz2TapDistance(omazTaps[j].dist); //for inbound case
	        	}
		        maz2tapUtils[j] = calculateUtility_MAZ2TAP(tvpbDmu, trip, omazTaps[j].tap, taz, nAlts);
		        if(maz2tapUtils[j] != NA_VALUE) {
		        	otapUtils[j] = calculateUtility_OTAP(tvpbDmu, trip, omazTaps[j].tap, taz, nAlts);
		        }
			}
			
			//calculate knr dtaps utils
			double[] dtapUtils = new double[dmazTaps.length];
			double[] tap2mazUtils = new double[dmazTaps.length];
						
			for(int j=0; j<dmazTaps.length; j++) {
				int taz = mttData.getTazForTap(dmazTaps[j].tap);
				if(tvpbDmu.getInbound()==0) {
	        		tvpbDmu.setMaz2TapDistance(dmazTaps[j].dist); //for outbound case
	        	} else {
	        		tvpbDmu.setClosestTapTaz(mttData.getClosestTapTaz(trip.getDmaz())); //for inbound case
	        	}
				tap2mazUtils[j] = calculateUtility_TAP2MAZ(tvpbDmu, trip, dmazTaps[j].tap, taz, nAlts);
				if(tap2mazUtils[j] != NA_VALUE) {
					dtapUtils[j] = calculateUtility_DTAP(tvpbDmu, trip, dmazTaps[j].tap, taz, nAlts);
				}
			}
			
			//calculate tap to tap and entire path
			if(trip.getDebugRecord()) {
				tvpbLogger.info("Knr tap pairs considered for trip record " + trip.getRecid() + " = " + omazTaps.length * dmazTaps.length);
			}
			
			for(int j=0; j<omazTaps.length; j++) {
				for(int k=0; k<dmazTaps.length; k++) {
					
					//only if all other path components valid
					if(maz2tapUtils[j] != NA_VALUE & otapUtils[j] != NA_VALUE & dtapUtils[k] != NA_VALUE & tap2mazUtils[k] != NA_VALUE) {
					
						double tap2tapUtil = calculateUtility_TAP2TAP(tvpbDmu, trip, omazTaps[j].tap, dmazTaps[k].tap, nAlts);
												
						if( tap2tapUtil != NA_VALUE) {
							
							TapPair kTapPair = new TapPair(omazTaps[j].tap, dmazTaps[k].tap, false, false, true, alts);
							kTapPair.maz2tapUtils[0] = maz2tapUtils[j];
							kTapPair.otapUtils[0] = otapUtils[j];
							kTapPair.tap2tapUtils[0] = tap2tapUtil;
							kTapPair.dtapUtils[0] = dtapUtils[k];
							kTapPair.tap2mazUtils[0] = tap2mazUtils[k];							
							tapPairs.add(kTapPair);
							
						}
					}
					
				}
			}
		
		} else {
			if(trip.getDebugRecord()) {
				tvpbLogger.info("Knr tap pairs considered for trip record " + trip.getRecid() + " = " + 0);
			}
		}
		
	}

	public void calculatePnrPathsForATrip(Trip trip, ArrayList<TapPair> tapPairs, TVPBDMU tvpbDmu) {
	
		//TAZ O-D trip distance for getting near taps
		float tripDistTaz = mttData.getOTazDTazDistance(trip.otaz, trip.dtaz);
		
		//pnr taps, depends on trip direction
		TapDist[] omazTaps;
		TapDist[] dmazTaps;
		if(trip.getInbound()==1) {
			omazTaps = mttData.getOmazWalkTaps(trip.getOmaz(), Math.min(tripDistTaz, trip.getMaxWalkInFeet()));
			dmazTaps = mttData.getPnrTaps(trip.getDmaz(), tripDistTaz);
		} else {
			omazTaps = mttData.getPnrTaps(trip.getOmaz(), tripDistTaz);
			dmazTaps = mttData.getDmazWalkTaps(trip.getDmaz(), Math.min(tripDistTaz, trip.getMaxWalkInFeet()));
		}
		
		if(omazTaps!=null & dmazTaps!=null) {
			
			//calculate pnr otaps utils
			double[] maz2tapUtils = new double[omazTaps.length];
			double[] otapUtils = new double[omazTaps.length];
			
			for(int j=0; j<omazTaps.length; j++) {
				int taz = mttData.getTazForTap(omazTaps[j].tap);
	        	if(tvpbDmu.getInbound()==0) {
	        		tvpbDmu.setClosestPnrTapTaz(mttData.getClosestPnrTapTaz(trip.getOmaz())); //for outbound case
	        	} else {
	        		tvpbDmu.setMaz2TapDistance(omazTaps[j].dist); //for inbound case
			    }
				maz2tapUtils[j] = calculateUtility_MAZ2TAP(tvpbDmu, trip, omazTaps[j].tap, taz, nAlts);
				if(maz2tapUtils[j] != NA_VALUE) {
					otapUtils[j] = calculateUtility_OTAP(tvpbDmu, trip, omazTaps[j].tap, taz, nAlts);
				}
			}
			
			//calculate pnr dtaps utils
			double[] dtapUtils = new double[dmazTaps.length];
			double[] tap2mazUtils = new double[dmazTaps.length];
			
			for(int j=0; j<dmazTaps.length; j++) {
				int taz = mttData.getTazForTap(dmazTaps[j].tap);
				if(tvpbDmu.getInbound()==0) {
	        		tvpbDmu.setMaz2TapDistance(dmazTaps[j].dist); //for outbound case
	        	} else {
	        		tvpbDmu.setClosestPnrTapTaz(mttData.getClosestPnrTapTaz(trip.getDmaz())); //for inbound case
	        	}
				tap2mazUtils[j] = calculateUtility_TAP2MAZ(tvpbDmu, trip, dmazTaps[j].tap, taz, nAlts);
				if(tap2mazUtils[j] != NA_VALUE) {
					dtapUtils[j] = calculateUtility_DTAP(tvpbDmu, trip, dmazTaps[j].tap, taz, nAlts);
				}
				
			}
			
			//calculate tap to tap and entire path
			if(trip.getDebugRecord()) {
				tvpbLogger.info("Pnr tap pairs considered for trip record " + trip.getRecid() + " = " + omazTaps.length * dmazTaps.length);
			}
			
			for(int j=0; j<omazTaps.length; j++) {
				for(int k=0; k<dmazTaps.length; k++) {
					
					//only if all other path components valid
					if(maz2tapUtils[j] != NA_VALUE & otapUtils[j] != NA_VALUE & dtapUtils[k] != NA_VALUE & tap2mazUtils[k] != NA_VALUE) {
					
						double tap2tapUtil = calculateUtility_TAP2TAP(tvpbDmu, trip, omazTaps[j].tap, dmazTaps[k].tap, nAlts);
						
						if( tap2tapUtil != NA_VALUE) {
							
							TapPair pTapPair = new TapPair(omazTaps[j].tap, dmazTaps[k].tap, false, true, false, alts);
							pTapPair.maz2tapUtils[0] = maz2tapUtils[j];
							pTapPair.otapUtils[0] = otapUtils[j];
							pTapPair.tap2tapUtils[0] = tap2tapUtil;
							pTapPair.dtapUtils[0] = dtapUtils[k];
							pTapPair.tap2mazUtils[0] = tap2mazUtils[k];
							tapPairs.add(pTapPair);
						}
						
					}
					
				}
			}
		
		} else {
			if(trip.getDebugRecord()) {
				tvpbLogger.info("Pnr tap pairs considered for trip record " + trip.getRecid() + " = " + 0);
			}
		}
	
	}

	private void setTVPBDmuValues(TVPBDMU tvpbDmu, Trip trip, boolean isWalkTapPair, boolean isPnrTapPair, boolean isKnrTapPair) {

        // update dmu variables
        tvpbDmu.setAge(trip.getAge());
        tvpbDmu.setUserClass(trip.getUserClass());
        tvpbDmu.setCars(trip.getCars());
        tvpbDmu.setTod(trip.getTod());        
        tvpbDmu.setHhincome(trip.getHhincome());
        tvpbDmu.setEduc(trip.getEduc());
        tvpbDmu.setGender(trip.getGender());
        tvpbDmu.setPurpose(trip.getPurpose());
        tvpbDmu.setInbound(trip.getInbound());
        
        tvpbDmu.setWalkTimeWeight(trip.getWalkTimeWeight());
        tvpbDmu.setWalkSpeed(trip.getWalkSpeed());
        tvpbDmu.setMaxWalk(trip.getMaxWalk());
        tvpbDmu.setValueOfTime(trip.getValueOfTime());
        
        tvpbDmu.setIsWalkTapPair(isWalkTapPair == true ? 1 : 0);
        tvpbDmu.setIsPnrTapPair(isPnrTapPair == true ? 1 : 0);
        tvpbDmu.setIsKnrTapPair(isKnrTapPair == true ? 1 : 0);
        
        tvpbDmu.setUserClassByType("user_class_work_walk", trip.getUserClassByType("user_class_work_walk"));
        tvpbDmu.setUserClassByType("user_class_work_pnr", trip.getUserClassByType("user_class_work_pnr"));
        tvpbDmu.setUserClassByType("user_class_work_knr", trip.getUserClassByType("user_class_work_knr"));
        tvpbDmu.setUserClassByType("user_class_non_work_walk", trip.getUserClassByType("user_class_non_work_walk"));
        tvpbDmu.setUserClassByType("user_class_non_work_pnr", trip.getUserClassByType("user_class_non_work_pnr"));
        tvpbDmu.setUserClassByType("user_class_non_work_knr", trip.getUserClassByType("user_class_non_work_knr"));
        
	}
	
	private double calculateUtility_MAZ2TAP(TVPBDMU tvpbDmu, Trip trip, int otap, int otaz, int[] nAlts) {
       
        //set indexes and calculate maz2tap utilities
        tvpbDmu.setDmuIndexValues(-1, trip.getOtaz(), otaz);
        
        double util[] = new double[1];
        if(runThreaded) {
        	util = threadSafeUECs.get()[0].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	threadSafeUECs.get()[0].logAnswersArray(tvpbLogger, "MAZ2TAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        	
        } else {
        	util = uecs[0].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
        		uecs[0].logAnswersArray(tvpbLogger, "MAZ2TAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }

        }
        
        return(util[0]);
	}
	
	private double calculateUtility_OTAP(TVPBDMU tvpbDmu, Trip trip, int otap, int otaz, int[] nAlts) {
        
        //set indexes and calculate otap utilities
        tvpbDmu.setDmuIndexValues(otap, -1, -1);
        
        double util[] = new double[1];
        
        if(runThreaded) {
        	util = threadSafeUECs.get()[1].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	threadSafeUECs.get()[1].logAnswersArray(tvpbLogger, "OTAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        	
        } else {
        	util = uecs[1].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	uecs[1].logAnswersArray(tvpbLogger, "OTAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        }

        return(util[0]);

	}
	
	private double calculateUtility_TAP2TAP(TVPBDMU tvpbDmu, Trip trip, int otap, int dtap, int[] nAlts) {

        //set indexes and calculate tap2tap utilities
        tvpbDmu.setDmuIndexValues(-1, otap, dtap);
        
        double util[] = new double[1];
        
        if(runThreaded) {
        	util = threadSafeUECs.get()[2].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	threadSafeUECs.get()[2].logAnswersArray(tvpbLogger, "TAP2TAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        	
        } else {
        	util = uecs[2].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	uecs[2].logAnswersArray(tvpbLogger, "TAP2TAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        }
        
        return(util[0]);

	}
	
	private double calculateUtility_DTAP(TVPBDMU tvpbDmu, Trip trip, int dtap, int dtaz, int[] nAlts) {

        //set indexes and calculate dtap utilities
        tvpbDmu.setDmuIndexValues(dtap, -1, -1);
        
        double util[] = new double[1];
        
        if(runThreaded) {
        	util = threadSafeUECs.get()[3].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	threadSafeUECs.get()[3].logAnswersArray(tvpbLogger, "DTAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        	
        } else {
        	util = uecs[3].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	uecs[3].logAnswersArray(tvpbLogger, "DTAP Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        }
        
        return(util[0]);

	}
	
	private double calculateUtility_TAP2MAZ(TVPBDMU tvpbDmu, Trip trip, int dtap, int dtaz, int[] nAlts) {

		//set indexes and calculate tap2maz utilities
        tvpbDmu.setDmuIndexValues(-1, dtaz, trip.getDtaz());

        //solve
        double util[] = new double[1];
        if(runThreaded) {
        	util = threadSafeUECs.get()[4].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	threadSafeUECs.get()[4].logAnswersArray(tvpbLogger, "TAP2MAZ Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        	
        } else {
        	util = uecs[4].solve(tvpbDmu.getDmuIndexValues(), tvpbDmu, nAlts);
        	
        	if(trip.getDebugRecord() & trace) {
            	uecs[4].logAnswersArray(tvpbLogger, "TAP2MAZ Utility (isWalkTapPair=" + tvpbDmu.isWalkTapPair + ", isPnrTapPair=" + tvpbDmu.isPnrTapPair + ", isKnrTapPair=" + tvpbDmu.isKnrTapPair + ")");
            }
        }

        return(util[0]);
	}
	
	public ArrayList<TapPair> trimPaths(Trip trip, ArrayList<TapPair> tapPairs, boolean walk, boolean pnr, boolean knr) {

		//sort tapPairs by total utility 
		Collections.sort(tapPairs);
		
		//take up to n lowest total utility tap pairs
		if(trip.getDebugRecord()) {
			tvpbLogger.info("Number of initial tap pairs for trip record " + trip.getRecid() + " = " + tapPairs.size());
		}
		
		//get max paths for mode
		int maxPaths = 0;
		if(walk) {
			maxPaths = maxWalkPaths;
		} else if(pnr) {
			maxPaths = maxPNRPaths;
		} else {
			maxPaths = maxKNRPaths;
		}
		
		ArrayList<TapPair> lowestTapPairs = new ArrayList<TapPair>();
		int count = 0;
		for(TapPair aTapPair : tapPairs) {
			
			if(trip.getDebugRecord() & trace) {
				tvpbLogger.info("getTotalUtils()[0]= " + aTapPair.getTotalUtils()[0] + " isWalkTapPair=" + aTapPair.isWalkTapPair + ", isPnrTapPair=" + aTapPair.isPnrTapPair + ", isKnrTapPair=" + aTapPair.isKnrTapPair);
			}

			lowestTapPairs.add(aTapPair);
			count = count + 1;
			if(count == maxPaths) { 
				break;
			}
		}
		
		if(trip.getDebugRecord()) {
			tvpbLogger.info("Number of trimmed tap pairs for trip record " + trip.getRecid() + " = " + lowestTapPairs.size());
		}
		
		return(lowestTapPairs);
	}
	
	public void writeOutputData() {
		
		//output path file name
		logger.info("Write output file");
		String projectDirectory = propertyMap.get("Project.Directory");
		String outputPathFileName = propertyMap.get("tvpb.pathfile");
		
		PrintWriter pathWriter = null;
		try {
			pathWriter = new PrintWriter(new File(projectDirectory + outputPathFileName));             
        } catch (IOException e) {
        	logger.error ( "IOException writing path file");
        }
		
		//write output file header
		ArrayList<String> data = new ArrayList<String>();
		data.add("recid");
		data.add("hhid");
		data.add("personid");
		data.add("dayid");
		data.add("tourid");
		data.add("tripid");
		data.add("userclass");
		data.add("cars");
		data.add("age");
		data.add("hhincome");
		data.add("educ");
		data.add("gender");
		data.add("purpose");
		data.add("inbound");
		data.add("tod");
		data.add("otaz");
		data.add("dtaz");
		data.add("omaz");
		data.add("dmaz");
		
		data.add("alt"); //tapPair alt
		data.add("otap");
		data.add("dtap");
		data.add("otaptaz");
		data.add("dtaptaz");
		data.add("isWalkTapPair");
		data.add("isPnrTapPair");
		data.add("isKnrTapPair");
		
		data.add("maz2tapUtils0");
		data.add("otapUtils0");
		data.add("tap2tapUtils0");
		data.add("dtapUtils0");
		data.add("tap2mazUtils0");
		
		if(alts==2) {
			data.add("maz2tapUtils1");
			data.add("otapUtils1");
			data.add("tap2tapUtils1");
			data.add("dtapUtils1");
			data.add("tap2mazUtils1");
		}
		
		pathWriter.println(formCsvString(data));
		
		//loop through trips and tapPairs and write data; write empty tapPair data if no tapPairs
		for(int k=0; k < trips.length; k++) {
			for(int j=0; j < Math.max(1, trips[k].tapPairs.size()); j++) {
				
				ArrayList<String> row = new ArrayList<String>();				
				row.add(String.valueOf(trips[k].getRecid()));
				row.add(String.valueOf(trips[k].getHhid()));
				row.add(String.valueOf(trips[k].getPersonid()));
				row.add(String.valueOf(trips[k].getdayid()));
				row.add(String.valueOf(trips[k].getTourid()));
				row.add(String.valueOf(trips[k].getTripid()));
				row.add(String.valueOf(trips[k].getUserClass()));
				row.add(String.valueOf(trips[k].getCars()));
				row.add(String.valueOf(trips[k].getAge()));
				row.add(String.valueOf(trips[k].getHhincome()));
				row.add(String.valueOf(trips[k].getEduc()));
				row.add(String.valueOf(trips[k].getGender()));
				row.add(String.valueOf(trips[k].getPurpose()));
				row.add(String.valueOf(trips[k].getInbound()));
				row.add(String.valueOf(trips[k].getTod()));
				row.add(String.valueOf(trips[k].getOtaz()));
				row.add(String.valueOf(trips[k].getDtaz()));
				row.add(String.valueOf(trips[k].getOmaz()));
				row.add(String.valueOf(trips[k].getDmaz()));
				
				//has tapPairs
				if(trips[k].tapPairs.size() > 0) {
				
					TapPair aTapPair = trips[k].tapPairs.get(j);
					
					row.add(String.valueOf(j)); //alt
					row.add(String.valueOf(aTapPair.otap));
					row.add(String.valueOf(aTapPair.dtap));
					row.add(String.valueOf(aTapPair.otaptaz));
					row.add(String.valueOf(aTapPair.dtaptaz));
					row.add(String.valueOf(aTapPair.isWalkTapPair));
					row.add(String.valueOf(aTapPair.isPnrTapPair));
					row.add(String.valueOf(aTapPair.isKnrTapPair));
					row.add(String.valueOf(aTapPair.maz2tapUtils[0]));
					row.add(String.valueOf(aTapPair.otapUtils[0]));
					row.add(String.valueOf(aTapPair.tap2tapUtils[0]));
					row.add(String.valueOf(aTapPair.dtapUtils[0]));
					row.add(String.valueOf(aTapPair.tap2mazUtils[0]));
					
					if(alts==2) {
						row.add(String.valueOf(aTapPair.maz2tapUtils[1]));
						row.add(String.valueOf(aTapPair.otapUtils[1]));
						row.add(String.valueOf(aTapPair.tap2tapUtils[1]));
						row.add(String.valueOf(aTapPair.dtapUtils[1]));
						row.add(String.valueOf(aTapPair.tap2mazUtils[1]));
					}
					
				} else {
					
					row.add(String.valueOf(j)); //alt
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					row.add("");
					
					if(alts==2) {
						row.add("");
						row.add("");
						row.add("");
						row.add("");
						row.add("");
					}
				}
				
				pathWriter.println(formCsvString(row));
			}
		}
		pathWriter.close();
	}
	
    private String formCsvString(List<String> data)
    {
        char delimiter = ',';
        Iterator<String> it = data.iterator();
        StringBuilder sb = new StringBuilder(it.next());
        while(it.hasNext())
            sb.append(delimiter).append(it.next());
        return sb.toString();
    }
	
    public MazTapTazData getMTTData() {
    	return(mttData);
    }
    
    public void setMTTData(MazTapTazData mttData) {
    	this.mttData = mttData;
    }

}
