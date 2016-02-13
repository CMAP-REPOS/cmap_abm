package com.pb.cmap.tvpb;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.newmodel.UtilityExpressionCalculator;

import org.apache.log4j.Logger;

public class MazTapTazData implements Serializable {
	
	private static Logger logger = Logger.getLogger(BestTransitPathCalculator.class);
	
    private TableDataSet maz2tapTable;
    private TableDataSet tapTable;
    private TableDataSet tapLinesTable;
    private TableDataSet mazTable;
       
    private HashMap<Integer,HashMap<Integer,Float>> omazWalkTapsDist;
    private HashMap<Integer,HashMap<Integer,Float>> dmazWalkTapsDist;
    private HashMap<Integer,HashMap<Integer,Float>> mazPnrTapsDist;
    private HashMap<Integer,HashMap<Integer,Float>> mazMazDist;
    
    private HashMap<Integer,Integer> taptaz;
    private HashMap<Integer,String> taplines;
    private HashMap<Integer,ArrayList<Integer>> taztaps;
    private HashMap<Integer,Integer> tapCanPnr;
    private HashMap<Integer,Integer> maztaz;
    private HashMap<Integer,Integer> mazclosestTap;
    private HashMap<Integer,Integer> mazclosestPnrTap;
    private HashMap<Integer,Integer> maztransit;
    
    private double maxPnrDist;
    
    private int[] uniqTazs;
    private int[] mazs;
    private int[] tazs;
    private String tapid;
    private Matrix tazDistanceMatrix;
    
    private HashMap<Integer,TapDist[]> omazWalkTapsDistSorted = new HashMap<Integer,TapDist[]>();
    private HashMap<Integer,TapDist[]> dmazWalkTapsDistSorted = new HashMap<Integer,TapDist[]>();
    private HashMap<Integer,TapDist[]> mazPnrTapsDistSorted = new HashMap<Integer,TapDist[]>();
    
    private int maxTransitStopWalkDist = 5280;
    
    private static MazTapTazData instance;
    
    public MazTapTazData(HashMap<String, String> propertyMap) throws IOException {
    	        
    	//----------------------------------------------
    	//read maz table
    	//----------------------------------------------
    	CSVFileReader csvReader = new CSVFileReader();
        String mazTableFileName = propertyMap.get("tvpb.mazfile");
        try {
        	mazTable = csvReader.readFile( new File(mazTableFileName));
        } catch (IOException e) {
        	throw new IOException();
        }
        
        //get maz table field names
	    String maz = propertyMap.get("tvpb.mazfile.maz");
	    String taz = propertyMap.get("tvpb.mazfile.taz");	    
	    
	    mazs = mazTable.getColumnAsInt(maz);
	    tazs = mazTable.getColumnAsInt(taz);
	    
	    //create unique set of tazs
	    TreeSet<Integer> tempTazs = new TreeSet<Integer>();
    	for (int aTaz : tazs) {
    		tempTazs.add(aTaz);
    	}
        uniqTazs = new int[tempTazs.size()];
        int z=0;
        for(Integer aTaz : tempTazs) {
    		uniqTazs[z] = aTaz;
    		z++;
    	}
    	
	    //create lookup
        maztaz = new HashMap<Integer,Integer>();
	    for(int i=0; i<mazs.length; i++) {
	    	maztaz.put(mazs[i], tazs[i]);
	    }
    	
	    //----------------------------------------------
    	//read tap table
	    //----------------------------------------------
    	String tapTableFileName = propertyMap.get("tvpb.tapFile");
        try {
        	tapTable = csvReader.readFile( new File(tapTableFileName));
        } catch (IOException e) {
        	throw new IOException();
        }
        
		//get tap table field names
	    tapid = propertyMap.get("tvpb.tapFile.tapid");
	    String tazid = propertyMap.get( "tvpb.tapFile.tazid");
	    String canpnr = propertyMap.get("tvpb.tapFile.canpnr");
	    
	    int[] tapIds = tapTable.getColumnAsInt(tapid);
	    int[] tazIds = tapTable.getColumnAsInt(tazid);
	    int[] canPnr = tapTable.getColumnAsInt(canpnr);
	    
	    //create lookups
	    taztaps = new HashMap<Integer,ArrayList<Integer>>();
	    taptaz = new HashMap<Integer,Integer>();
	    tapCanPnr = new HashMap<Integer,Integer>();
	    for(int i=0; i<tapIds.length; i++) {
	    	taptaz.put(tapIds[i], tazIds[i]);
	    	tapCanPnr.put(tapIds[i], canPnr[i]);
	    	
    		//store taps for taz
    		if(taztaps.containsKey(tazIds[i])) {
    			ArrayList<Integer> updatedTaz = taztaps.get(tazIds[i]);
    			updatedTaz.add(tapIds[i]);
    			taztaps.put(tazIds[i], updatedTaz);
    		} else {
    			ArrayList<Integer> initTap = new ArrayList<Integer>();
    			initTap.add(tapIds[i]);
    			taztaps.put(tazIds[i], initTap);
    		}
	    }
	    
	    //----------------------------------------------
    	//read tap lines table
	    //----------------------------------------------
    	String tapLinesTableFileName = propertyMap.get("tvpb.tapLines");
        try {
        	tapLinesTable = csvReader.readFile( new File(tapLinesTableFileName));
        } catch (IOException e) {
        	throw new IOException();
        }
        
		//get tap lines table field names
	    String tapLinesTapId = propertyMap.get("tvpb.tapLines.tapid");
	    String lines = propertyMap.get( "tvpb.tapLines.lines");
	    int[] tapLinesTapIds = tapLinesTable.getColumnAsInt(tapLinesTapId);
	    String[] linesForTap = tapLinesTable.getColumnAsString(lines);
	    
	    //create lookups
	    taplines = new HashMap<Integer,String>();
	    for(int i=0; i<tapLinesTapIds.length; i++) {
	    	taplines.put(tapLinesTapIds[i], linesForTap[i]);
	    }
	    
        //----------------------------------------------
	    //read maz2tap table (which is also maz2maz)
	    //----------------------------------------------
        String maz2tapTableFileName = propertyMap.get("tvpb.maz2tapfile");
        try {
        	maz2tapTable = csvReader.readFile( new File(maz2tapTableFileName));
        } catch (IOException e) {
        	throw new IOException();
        }
        
        //get maz2tap table field names
	    String origin = propertyMap.get("tvpb.maz2tapfile.origin");
	    String dest = propertyMap.get("tvpb.maz2tapfile.destination");
	    String dist = propertyMap.get("tvpb.maz2tapfile.distance");
    	
	    int[] origins = maz2tapTable.getColumnAsInt(origin);
	    int[] dests = maz2tapTable.getColumnAsInt(dest);
	    double[] distances = maz2tapTable.getColumnAsDouble(dist);
	    
	    //create lookups
	    omazWalkTapsDist = new HashMap<Integer,HashMap<Integer,Float>>();
	    dmazWalkTapsDist = new HashMap<Integer,HashMap<Integer,Float>>();
	    mazMazDist = new HashMap<Integer,HashMap<Integer,Float>>();
	    
	    //create dictionaries of taps by origin and destination
	    for(int i=0; i<origins.length; i++) {
	    	
	    	//if origin is MAZ and dest is TAP
	    	if(maztaz.containsKey(origins[i]) & taptaz.containsKey(dests[i])) { 
	    		
	    		//store distances
	    		if(omazWalkTapsDist.containsKey(origins[i])) {
	    			
	    			HashMap<Integer,Float> updatedTapsDist = omazWalkTapsDist.get(origins[i]);
	    			updatedTapsDist.put(dests[i], (float)distances[i]);
	    			omazWalkTapsDist.put(origins[i], updatedTapsDist);
	    			
	    		} else {
	    			
	    			HashMap<Integer,Float> initTapDist = new HashMap<Integer,Float>();
	    			initTapDist.put(dests[i], (float)distances[i]);
	    			omazWalkTapsDist.put(origins[i], initTapDist);
	    		}
	    		
	    	} 
	    	
	    	//if origin is TAP and dest is MAZ
	    	if(taptaz.containsKey(origins[i]) & maztaz.containsKey(dests[i])) {

	    		//store distances
	    		if(dmazWalkTapsDist.containsKey(dests[i])) {
	    			
	    			HashMap<Integer,Float> updatedTapsDist = dmazWalkTapsDist.get(dests[i]);
	    			updatedTapsDist.put(origins[i], (float)distances[i]);
	    			dmazWalkTapsDist.put(dests[i], updatedTapsDist);
	    			
	    		} else {
	    			
	    			HashMap<Integer,Float> initTapDist = new HashMap<Integer,Float>();
	    			initTapDist.put(origins[i], (float)distances[i]);
	    			dmazWalkTapsDist.put(dests[i], initTapDist);
	    		}
	    		
	    	}
	    	
	    	//if origin is MAZ and dest is MAZ
	    	if(maztaz.containsKey(origins[i]) & maztaz.containsKey(dests[i])) {
	    		
	    		if(mazMazDist.containsKey(origins[i])) {
	    			
	    			HashMap<Integer,Float> updatedMazsDist = mazMazDist.get(origins[i]);
	    			updatedMazsDist.put(dests[i], (float)distances[i]);
	    			mazMazDist.put(origins[i], updatedMazsDist);
	    			
	    		} else {
	    			
	    			HashMap<Integer,Float> initMazDist = new HashMap<Integer,Float>();
	    			initMazDist.put(dests[i], (float)distances[i]);
	    			mazMazDist.put(origins[i], initMazDist);
	    		}
	    		
	    	}
	    }
	    
	    //----------------------------------------------
	    //trim the near tap set by origin/destination by only including the nearest tap 
	    //when more than one tap serves the same line.
	    //----------------------------------------------
	    int omazToOTaps = 0;
	    int trimmedOTaps = 0; 
	    for(Integer omaz : omazWalkTapsDist.keySet()) {
        	
	    	//get taps and distance and lines
	    	HashMap<Integer,Float> tapsForMazDist = omazWalkTapsDist.get(omaz);
	    	ArrayList<Maz2Tap> maz2TapData = new ArrayList<Maz2Tap>();
	    	
	    	for (Integer tapId : tapsForMazDist.keySet()) {
	    		
	    		Maz2Tap m2t = new Maz2Tap();
	    		m2t.tap = tapId;
	    		m2t.dist = tapsForMazDist.get(tapId);
	    		if(taplines.containsKey(tapId)) { //else drop tap from list
	    			m2t.lines = taplines.get(tapId).split(" ");
	    		}  
	    		maz2TapData.add(m2t);
	    		
	    	}
	    	
	    	//sort by distance and check for new lines
			Collections.sort(maz2TapData);
			HashMap<String,String> linesServed = new HashMap<String,String>();
			for (Maz2Tap m2t : maz2TapData) {
				
				//skip if no lines served
				if(m2t.lines != null) {
				
					for (int i=0; i<m2t.lines.length; i++) {
						if(linesServed.containsKey(m2t.lines[i]) == false) {
							m2t.servesNewLines = true;
							linesServed.put(m2t.lines[i], m2t.lines[i]);
						}
					}
				}
	    	}
	    	
			//remove unused taps
			for (Maz2Tap m2t : maz2TapData) {
				omazToOTaps = omazToOTaps + 1;
				if( m2t.servesNewLines == false) {
					tapsForMazDist.remove(m2t.tap);
					trimmedOTaps = trimmedOTaps + 1;
				}
			}
			omazWalkTapsDist.put(omaz, tapsForMazDist);
    		
	    }
	    
	    //destination taps
	    int dmazToDTaps = 0;
	    int trimmedDTaps = 0;
	    for(Integer dmaz : dmazWalkTapsDist.keySet()) {
        	
	    	//get taps and distance and lines
	    	HashMap<Integer,Float> tapsForMazDist = dmazWalkTapsDist.get(dmaz);
	    	ArrayList<Maz2Tap> maz2TapData = new ArrayList<Maz2Tap>();
	    	
	    	for (Integer tapId : tapsForMazDist.keySet()) {
	    		
	    		Maz2Tap m2t = new Maz2Tap();
	    		m2t.tap = tapId;
	    		m2t.dist = tapsForMazDist.get(tapId); 
	    		if(taplines.containsKey(tapId)) { //else drop tap from list
	    			m2t.lines = taplines.get(tapId).split(" ");
	    		}  
	    		maz2TapData.add(m2t);
	    		
	    	}
	    	
	    	//sort by distance and check for new lines
			Collections.sort(maz2TapData);
			HashMap<String,String> linesServed = new HashMap<String,String>();
			for (Maz2Tap m2t : maz2TapData) {
				
				//skip if no lines served
				if(m2t.lines != null) {
				
					for (int i=0; i<m2t.lines.length; i++) {
						if(linesServed.containsKey(m2t.lines[i]) == false) {
							m2t.servesNewLines = true; 
							linesServed.put(m2t.lines[i], m2t.lines[i]);
						}
					}
				}
	    	}
	    	
			//remove unused taps
			for (Maz2Tap m2t : maz2TapData) {
				dmazToDTaps = dmazToDTaps + 1;
				if( m2t.servesNewLines == false) {
					tapsForMazDist.remove(m2t.tap);
					trimmedDTaps = trimmedDTaps + 1;
				}
			}
			dmazWalkTapsDist.put(dmaz, tapsForMazDist);
    		
	    }

	    logger.info("Removed " + trimmedOTaps + " of " + omazToOTaps + " Omaz Otap pairs since servesNewLines=false");
	    logger.info("Removed " + trimmedDTaps + " of " + dmazToDTaps + " Dmaz Dtap pairs since servesNewLines=false");
	    
	    //create sorted taps by distance from maz
	    createSortedTapDists(omazWalkTapsDist, omazWalkTapsDistSorted);
	    createSortedTapDists(dmazWalkTapsDist, dmazWalkTapsDistSorted);
	    
	    //populate maz has transit vector
	    calculateMazHasTransit();
	    
    	//----------------------------------------------
    	//read TAZ distance matrix
    	//----------------------------------------------
  		logger.info("Get taz distance matrix");
  				
  		//setup uec
  		String projectDirectory = propertyMap.get("Project.Directory");
  		String uecFile = propertyMap.get("utility.bestTransitPath.uec.file");
  		maxPnrDist = Double.parseDouble(propertyMap.get("tvpb.maxpnrdist"));
  		TransitWalkAccessDMU twaDmu = new TransitWalkAccessDMU();
        UtilityExpressionCalculator uecDistance = new UtilityExpressionCalculator(
        			new File(projectDirectory + uecFile), 6, 0, propertyMap, twaDmu );
  		
  		//solve uec
        int[] nAlts = {0,1};
    	int[] tazs = getTazs();
  		Matrix tazDistance = new Matrix(tazs.length, tazs.length);
  		tazDistance.setExternalNumbersZeroBased(tazs);
  		for(int i=0; i<tazs.length; i++) {
  			for(int j=0; j<tazs.length; j++) {
  				twaDmu.setDmuIndexValues(-1, tazs[i], tazs[j]);
  				double value = uecDistance.solve(twaDmu.getDmuIndexValues(), twaDmu, nAlts)[0];
  				tazDistance.setValueAt(tazs[i], tazs[j], (float) value);
  			}
  		}
  		
  		//create pnr distance matrix
  		logger.info("Calculate pnr tap data for later use");
  		setDistanceMatrix(tazDistance);
  		calculatePnrTapData(maxPnrDist);
    }
    
    public void calculatePnrTapData(double maxPnrDist) {
    	
    	//get taz distance 2D array
    	float[][] dist = tazDistanceMatrix.getValues();
    	int[] tazNums = tazDistanceMatrix.getExternalColumnNumbersZeroBased();
    	ArrayList<Integer> tazNumbers = new ArrayList<Integer>();
    	for (Integer i : tazNums) {  
    		tazNumbers.add(i);
    	}
    	
    	//for each maz, get its taz, then the closest pnr tap and all taps within threshold
    	mazclosestPnrTap = new HashMap<Integer,Integer>();
    	mazPnrTapsDist = new HashMap<Integer,HashMap<Integer,Float>>();
    	mazclosestTap = new HashMap<Integer,Integer>();
    	for(Integer maz : maztaz.keySet()) {
        	
    		//get maz's taz
    		int taz = maztaz.get(maz);
    		
    		//get the distance to each taz from the maz's taz
        	float[] distToTazs = dist[tazNumbers.indexOf(taz)];

        	double minDist = 999*5280;
        	int minDistTap = 0;
        	double minDistPnr = 999*5280;
        	int minDistPnrTap = 0;
        	for (int i=0; i<tazNums.length; i++) {
        				
    			//get taps in the taz if there are some
    			ArrayList<Integer> tapsForTaz = taztaps.get(tazNums[i]);
    			if(tapsForTaz!=null) {
    				
    				//skip unavailable tazs
    				if(distToTazs[i] <= maxPnrDist) {
    				
	    				//@todo - for now take arbitrary tap in the taz since each tap has the same taz distance
	        			if(distToTazs[i] < minDist) {
	        				minDistTap = tapsForTaz.get(0); //index 0 since ArrayList
	        				minDist = distToTazs[i];
	        			}
	        			
	        			//reduce set of taps to those that can pnr
	        			ArrayList<Integer> tapsForTazPnr = new ArrayList<Integer>();
	        			HashMap<Integer,Float> tapsForTazPnrDist = new HashMap<Integer,Float>();
	        	    	for (Integer tapId : tapsForTaz) {
	        	    		if(tapCanPnr.get(tapId)==1) {
	        	    			tapsForTazPnr.add(tapId);
	        	    			tapsForTazPnrDist.put(tapId, distToTazs[i]);
	        	    		}
	        	    	}
	        			
	        			//process pnr taps if there are some
	        			if(tapsForTazPnr.size()>0) {
	        				
	            			//put all taps in the pnr tap set for the maz
	            			if(mazPnrTapsDist.containsKey(maz)) {
	            				
	            				HashMap<Integer,Float> updatedTapsDist = mazPnrTapsDist.get(maz);
	            				updatedTapsDist.putAll(tapsForTazPnrDist);
	            				mazPnrTapsDist.put(maz, updatedTapsDist);
	            				
	            			} else {
	            				
	        	    			HashMap<Integer,Float> initTapDist = new HashMap<Integer,Float>();
	        	    			initTapDist.putAll(tapsForTazPnrDist);
	        	    			mazPnrTapsDist.put(maz, initTapDist);
	        	    			
	            			}
	            			
	            			//@todo - for now take arbitrary pnr tap in the taz since each tap has the same taz distance
	            			if(distToTazs[i] < minDistPnr) {
	            				minDistPnrTap = tapsForTazPnr.get(0); //index 0 since ArrayList
	            				minDistPnr = distToTazs[i];
	            			}
	        			
	        			}
	        			
    				}
        			
    			}
    			
        	}
    		
        	//store closest tap and pnr tap by maz
        	mazclosestTap.put(maz, minDistTap);
        	mazclosestPnrTap.put(maz, minDistPnrTap);
        }
    	
    	//----------------------------------------------
	    //trim the near tap set by origin/destination by only including the nearest tap 
	    //when more than one tap serves the same line.
	    //----------------------------------------------
    	int mazToPnrTaps = 0;
    	int trimmedPnrTaps = 0;
	    for(Integer maz : mazPnrTapsDist.keySet()) {
        	
	    	//get taps and distance and lines
	    	HashMap<Integer,Float> tapsForMazDist = mazPnrTapsDist.get(maz);
	    	ArrayList<Maz2Tap> maz2TapData = new ArrayList<Maz2Tap>();
	    	
	    	for (Integer tapId : mazPnrTapsDist.get(maz).keySet()) {
	    		
	    		Maz2Tap m2t = new Maz2Tap();
	    		m2t.tap = tapId;
	    		m2t.dist = tapsForMazDist.get(tapId);
	    		if(taplines.containsKey(tapId)) { //else drop tap from list
	    			m2t.lines = taplines.get(tapId).split(" ");
	    		}  
	    		maz2TapData.add(m2t);
	    		
	    	}
	    	
	    	//sort by distance and check for new lines
			Collections.sort(maz2TapData);
			HashMap<String,String> linesServed = new HashMap<String,String>();
			for (Maz2Tap m2t : maz2TapData) {
				
				//skip if no lines served
				if(m2t.lines != null) {
				
					for (int i=0; i<m2t.lines.length; i++) {
						if(linesServed.containsKey(m2t.lines[i]) == false) {
							m2t.servesNewLines = true;
							linesServed.put(m2t.lines[i], m2t.lines[i]);
						}
					}
				}
	    	}
	    	
			//remove unused taps
			for (Maz2Tap m2t : maz2TapData) {
				mazToPnrTaps = mazToPnrTaps + 1;
				if( m2t.servesNewLines == false) {
					tapsForMazDist.remove(m2t.tap);
					trimmedPnrTaps = trimmedPnrTaps + 1;
				}
			}
			mazPnrTapsDist.put(maz, tapsForMazDist);
    		
	    }
	    
	    logger.info("Removed " + trimmedPnrTaps + " of " + mazToPnrTaps + " maz pnr tap pairs since servesNewLines=false");
	    
	    //create sorted taps by distance from maz
	    createSortedTapDists(mazPnrTapsDist, mazPnrTapsDistSorted);

    }
   
    public void calculateMazHasTransit() {
    	
    	maztransit = new HashMap<Integer,Integer>();
    	
    	for(int i=0; i<mazs.length; i++) {
    		if(getNearTaps(omazWalkTapsDistSorted, mazs[i], maxTransitStopWalkDist) != null) {
    			maztransit.put(mazs[i], 1);
    		} else {
    			maztransit.put(mazs[i], 0);
    		}
    	}
    }
    
    public void createSortedTapDists(HashMap<Integer,HashMap<Integer,Float>> tapsListDist, HashMap<Integer,TapDist[]> tapsDistSorted) {
    	
    	//loop by maz
       	for(Integer maz : tapsListDist.keySet()) {
    			
       		//create maz2taps to sort
       		ArrayList<Maz2Tap> mt = new ArrayList<Maz2Tap>();
    		HashMap<Integer,Float> tapDists = tapsListDist.get(maz);
    			
			if(tapDists.size()>0) {
			
				//add data and sort
    			for(Integer tap : tapDists.keySet()) {
    				Maz2Tap x = new Maz2Tap();
    				x.tap = tap;
    				x.dist = tapDists.get(tap);
    				mt.add(x);
    			}
    			Collections.sort(mt);
    			
    			//convert to TapDist simple object
    			TapDist[] td = new TapDist[mt.size()];
    			for(int i=0; i<mt.size(); i++) {
    				td[i] = new TapDist();
    				td[i].tap = mt.get(i).tap;
    				td[i].dist= mt.get(i).dist;
    			}
    			
    			//add to array
    			tapsDistSorted.put(maz, td);
			}
       	}
    	
    }
    
    public TapDist[] getNearTaps(HashMap<Integer,TapDist[]> tapsDistSorted, int maz, double maxDist) {
    	
    	//get tap distances
    	TapDist[] tapsDist = tapsDistSorted.get(maz);

       	//return array
    	if(tapsDist != null) {
    		
       		//filter on distance
    		int keepCount = 0 ;    		
			while(tapsDist[keepCount].dist <= maxDist) {
				keepCount++;
				if(keepCount == tapsDist.length) { 
					break;
				}
			}
    		
			TapDist[] ret =  new TapDist[keepCount];
    		for(int i=0; i<keepCount; i++) {
        		ret[i] = tapsDist[i];
    		}
            return(ret);
            
    	} else {
    		return(null);
    	}
    	
    }

    public TapDist[] getOmazWalkTaps(int omaz, double maxDist) {
    	return(getNearTaps(omazWalkTapsDistSorted, omaz, maxDist));
    }
    
    public TapDist[] getDmazWalkTaps(int dmaz, double maxDist) {
    	return(getNearTaps(dmazWalkTapsDistSorted, dmaz, maxDist));
    }
    
    public int[] getOmazWalkTapIds(int omaz, double maxDist) {
    	TapDist[] tapsDist = getOmazWalkTaps(omaz, maxDist);
    	if(tapsDist != null) {
    		int taps[] = new int[tapsDist.length];
        	for(int i=0; i<tapsDist.length; i++) {
        		taps[i] = tapsDist[i].tap;
        	}
        	return(taps);
    	} else {
    		return(null);
    	}
    }
    
    public int[] getDmazWalkTapIds(int dmaz, double maxDist) {
    	TapDist[] tapsDist = getDmazWalkTaps(dmaz, maxDist);
    	if(tapsDist != null) {
	    	int taps[] = new int[tapsDist.length];
	    	for(int i=0; i<tapsDist.length; i++) {
	    		taps[i] = tapsDist[i].tap;
	    	}
	    	return(taps);
    	} else {
    		return(null);
    	}
    }
    
    public TapDist[] getPnrTaps(int maz, double maxDist) {
    	return(getNearTaps(mazPnrTapsDistSorted, maz, maxDist));
    }
    
    public int[] getPnrTapIds(int maz, double maxDist) {
    	TapDist[] tapsDist = getPnrTaps(maz, maxDist);
    	if(tapsDist != null) {
    		int taps[] = new int[tapsDist.length];
    		for(int i=0; i<tapsDist.length; i++) {
        		taps[i] = tapsDist[i].tap;
        	}
        	return(taps);
    	} else {
    		return(null);
    	}
    }
    
    public int getTazForTap(int tap) {
    	return(taptaz.get(tap));
    }
    
    public int getTazForMaz(int maz) {
    	if ( ! maztaz.containsKey( maz ) ) {
    		logger.error( "no taz value in maztaz hashmap for maz key=" + maz );
    		throw new RuntimeException();
    	}
    	return(maztaz.get(maz));
    }
    
    public int[] getTazs() {
    	return(uniqTazs);
    }
    
    public double getODistance(int omaz, int otap) {
    	return(omazWalkTapsDist.get(omaz).get(otap));
    }
    
    public double getDDistance(int dmaz, int dtap) {
    	return(dmazWalkTapsDist.get(dmaz).get(dtap));
    }
    
    public double getOMazDMazDistanceFromFile(int omaz, int dmaz) {
    	return(mazMazDist.get(omaz).get(dmaz));
    }
    
    public boolean checkOMazDMazIsNear(int omaz, int dmaz) {
    	HashMap<Integer,Float> omazs = mazMazDist.get(omaz);
    	if(omazs != null) {
    		Object dist = omazs.get(dmaz);
    		if(dist != null) {
    			return(true);
    		} else {
        		return(false);
        	}
    	} else {
    		return(false);
    	}
    }

    public int getClosestPnrTap(int maz) {
    	return(mazclosestPnrTap.get(maz));
    }
    
    public int getClosestTap(int maz) {
    	return(mazclosestTap.get(maz));
    }
    
    public int getClosestPnrTapTaz(int maz) {
    	return(getTazForTap(getClosestPnrTap(maz)));
    }
    
    public int getClosestTapTaz(int maz) {
    	return(getTazForTap(getClosestTap(maz)));
    }
    
    public void setDistanceMatrix(Matrix distanceMatrix) {
    	tazDistanceMatrix = distanceMatrix;
    }
    
    public float getOTazDTazDistance(int otaz, int dtaz) {
    	return(tazDistanceMatrix.getValueAt(otaz, dtaz));
    }
    
    //get maz to maz distance if near; else get omaz's taz to dmaz's taz distance
    public float getOMazDMazDistanceFromParts(int omaz, int dmaz) {    	
    	float dist = -1;
    	if(checkOMazDMazIsNear(omaz, dmaz)) {
    		dist = (float)getOMazDMazDistanceFromFile(omaz,dmaz);
    	} else {
    		dist = tazDistanceMatrix.getValueAt(getTazForMaz(omaz), getTazForMaz(dmaz));
    	}
    	return(dist);
    }
    
    //gets the maz to maz distance from parts
    public float getOMazDMazDistance(int omaz, int dmaz) {    	
    	return(getOMazDMazDistanceFromParts(omaz, dmaz));
    }
    
    public float[] getOMazDistances(int omaz) {
    	float[] dists = new float[mazs.length];
    	for(int i=0; i<mazs.length; i++) {
    		dists[i] = getOMazDMazDistance(omaz, mazs[i]);
    	}
    	return(dists);
    }
    
    public float[] getDMazDistances(int dmaz) {
    	float[] dists = new float[mazs.length];
    	for(int i=0; i<mazs.length; i++) {
    		dists[i] = getOMazDMazDistance(mazs[i], dmaz);
    	}
    	return(dists);
    }
    
    public int getHasTransitAccess(int maz) {
    	return(maztransit.get(maz));
    }

    public int[] getTazsForMazs() {
    	return(tazs);
    }
    
    public TableDataSet getTapTable() {
    	return(tapTable);
    }

    public String getTapIdFieldName(){
    	return(tapid);
    }
    
    public static MazTapTazData getInstance(HashMap<String, String> rbMap)
    {
        if (instance == null)
        {
            try {
				instance = new MazTapTazData(rbMap);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return instance;
        } else return instance;
    }
    
    public class Maz2Tap implements Comparable<Maz2Tap>, Serializable
    {
        public int maz;
        public int tap;
        public double dist;
        public String[] lines;
        public boolean servesNewLines = false;
        
    	@Override
    	public int compareTo(Maz2Tap o) {
		    if ( this.dist < o.dist ) {
		    	return -1;
		    } else if (this.dist==o.dist) {
		    	return 0;
		    } else {
		    	return 1;
		    }
    	}
    }
    
    public class TapDist implements Serializable
    {
        public int tap;
        public double dist;

    }
    
    public int getNumMazs() {
    	return(mazs.length);
    }
    
    public int getNumTazs() {
    	return(uniqTazs.length);
    }
    
    public int getNumTaps() {
    	return(tapTable.getRowCount());
    }
    
    public int[] getTapIds() {
    	return(tapTable.getColumnAsInt(tapid));
    }
}
