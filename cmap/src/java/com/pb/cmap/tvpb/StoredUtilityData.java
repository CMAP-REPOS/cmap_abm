package com.pb.cmap.tvpb;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.HashMap;


public class StoredUtilityData implements Serializable
{

    private static StoredUtilityData objInstance = null;
    public static final float		 default_utility = 0;

    //arrays are shared by multiple BestTransitPathCalculator objects 
    //in a distributed computing environment
    private float[][][] storedWalkAccessUtils;	// MAZ, TAP, WalkPropClass
    private float[][][] storedDriveAccessUtils; // TAZ, TAP, WalkPropClass
    private float[][][] storedKnrAccessUtils; // TAZ, TAP, WalkPropClass
    
    private float[][][] storedWalkEgressUtils; 	// TAP, MAZ, WalkPropClass
    private float[][][] storedDriveEgressUtils; // TAP, TAZ, WalkPropClass
    private float[][][] storedKnrEgressUtils; // TAP, TAZ, WalkPropClass
    
    // TOD period number -> pTAP*100000+aTAP -> [util array for each skim set]
    private HashMap<Integer,ConcurrentHashMap<Long,float[]>> storedDepartPeriodTapTapUtils;
    
    //TAP ID indexes
    private int[] tapIndexes;
    
    private StoredUtilityData(){
    }
    
    public static synchronized StoredUtilityData getInstance( int numMgra, int numTap, int numTaz, int[] accEgrSegments, int[] periods, int numWalkPropClass, int[] tapsIds)
    {
        if (objInstance == null) {
            objInstance = new StoredUtilityData();
            objInstance.setupStoredDataArrays( numMgra, numTap, numTaz, accEgrSegments, periods, numWalkPropClass, tapsIds);
            return objInstance;
        }
        else {
            return objInstance;
        }
    }
    
    private void setupStoredDataArrays( int numMgra, int numTap, int numTaz, int[] accEgrSegments, int[] periods, int numWalkPropClass, int[] tapsIds){        
    	
    	//build TAP index lookup since TAP IDs are not 1 to N
    	int maxTapId = -1;
    	for (int i=0; i<tapsIds.length; i++) {
    		if(tapsIds[i] > maxTapId) {
    			maxTapId = tapsIds[i];
    		}
    	}
    	tapIndexes = new int[maxTapId+1];
    	for (int i=0; i<tapsIds.length; i++) {
    		tapIndexes[tapsIds[i]] = i;
    	}
    	
    	// dimension the arrays - MAZ and TAZ are 1 to N      
    	storedWalkAccessUtils = new float[numMgra+1][numTap][numWalkPropClass+1];
        storedDriveAccessUtils = new float[numTaz+1][numTap][numWalkPropClass+1];
        storedKnrAccessUtils = new float[numTaz+1][numTap][numWalkPropClass+1];
        
        storedWalkEgressUtils = new float[numTap][numMgra+1][numWalkPropClass+1];
        storedDriveEgressUtils = new float[numTap][numTaz+1][numWalkPropClass+1];
        storedKnrEgressUtils = new float[numTap][numTaz+1][numWalkPropClass+1];
        
        // assign default values to array elements
        for (int i=0; i<=numMgra; i++)
        	for (int j=0; j<numTap; j++) {
        		for (int k=0; k<=numWalkPropClass; k++) {
        			storedWalkAccessUtils[i][j][k] = default_utility;
        			storedWalkEgressUtils[j][i][k] = default_utility;
        		}
        	}
        // assign default values to array elements
        for (int i=0; i<=numTaz; i++)
        	for (int j=0; j<numTap; j++) {
        		for (int k=0; k<=numWalkPropClass; k++) {
        			storedDriveAccessUtils[i][j][k] = default_utility;
        			storedDriveEgressUtils[j][i][k] = default_utility;
        			storedKnrAccessUtils[i][j][k] = default_utility;
        			storedKnrEgressUtils[j][i][k] = default_utility;
        		}
        	}
        
        //put skim set utility array into concurrent hashmap
        storedDepartPeriodTapTapUtils = new HashMap<Integer,ConcurrentHashMap<Long,float[]>>();
    	for(int j=0; j<periods.length; j++) {
    		storedDepartPeriodTapTapUtils.put(periods[j], new ConcurrentHashMap<Long,float[]>()); //key method paTapKey below
    	}
    }
    
    public float[][][] getStoredWalkAccessUtils() {
        return storedWalkAccessUtils;
    }
    
    public float[][][] getStoredDriveAccessUtils() {
        return storedDriveAccessUtils;
    }
    
    public float[][][] getStoredKnrAccessUtils() {
        return storedKnrAccessUtils;
    }
    
    public float[][][] getStoredWalkEgressUtils() {
        return storedWalkEgressUtils;
    }
    
    public float[][][] getStoredDriveEgressUtils() {
        return storedDriveEgressUtils;
    }
    
    public float[][][] getStoredKnrEgressUtils() {
        return storedKnrEgressUtils;
    }
    
    public HashMap<Integer,ConcurrentHashMap<Long,float[]>> getStoredDepartPeriodTapTapUtils() {
        return storedDepartPeriodTapTapUtils;
    }
    
    //create p to a hash key - up to 99,999 
    public long paTapKey(int p, int a) {
    	return(p * 100000 + a);
    }
    
    //convert double array to float array
    public float[] d2f(double[] d) {
    	float[] f = new float[d.length];
    	for(int i=0; i<d.length; i++) {
    		f[i] = (float)d[i];
    	}
    	return(f);
    }
    
    //get TAP index
    public int getTapIndex(int tapId) { 
    	return(tapIndexes[tapId]);
    } 
    
}
