/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.pb.cmap.tvpb;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.VariableTable;
import com.pb.common.util.Tracer;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.newmodel.Alternative;
import com.pb.common.newmodel.ConcreteAlternative;
import com.pb.common.newmodel.LogitModel;

import com.pb.cmap.tourBased.CmapModelStructure;

/**
 * Best Transit Path Calculator
 */
public class BestTransitPathCalculator implements Serializable
{

    private transient Logger              logger        = Logger.getLogger(BestTransitPathCalculator.class);

    //CMAP uses generalized time instead of utility
    public static final int               NA            = 0;
    public static final int               NA_TIME       = 99999;
    
    public static final int               WTW           = 0;
    public static final int               WTD           = 1;
    public static final int               DTW           = 2;
    public static final int               WTK           = 3;
    public static final int               KTW           = 4;
    public static final int[]             ACC_EGR       = {WTW,WTD,DTW,WTK,KTW};
    public static final String[]          ACC_EGR_LABELS= {"WTW","WTD","DTW","WTK","KTW"};
    public static final int               NUM_ACC_EGR   = ACC_EGR.length;

    public static final int[]             PERIODS       = CmapModelStructure.PERIODS;
    
    // seek and trace
    private boolean                       trace;
    private int[]                         traceOtaz;
    private int[]                         traceDtaz;
    protected Tracer                      tracer;

    private MazTapTazData                 mttData;

    // piece-wise utilities are being computed
    private UtilityExpressionCalculator   walkAccessUEC;
    private UtilityExpressionCalculator   walkEgressUEC;
    private UtilityExpressionCalculator   driveAccessUEC;
    private UtilityExpressionCalculator   driveEgressUEC;
    private UtilityExpressionCalculator   knrAccessUEC;
    private UtilityExpressionCalculator   knrEgressUEC;
    private UtilityExpressionCalculator   tapToTapUEC;

    // utility data cache for each transit path segment
    // encapsulates data shared by the BTPC objects created for each hh choice model object
    // note that access/egress utilities are independent of transit skim set
    private StoredUtilityData storedDataObject; 
    private float[][][] storedWalkAccessUtils;
    private float[][][] storedDriveAccessUtils;
    private float[][][] storedKnrAccessUtils;
    
    private float[][][] storedWalkEgressUtils;
    private float[][][] storedDriveEgressUtils;    
    private float[][][] storedKnrEgressUtils;
    
    private HashMap<Integer,ConcurrentHashMap<Long,float[]>> storedDepartPeriodTapTapUtils;

    // arrays storing information about the n (array length) best paths
    private double[]                      bestUtilities;
    private int[]                         bestPTap;
    private int[]                         bestATap;
    private int[]                         bestSet;   
    private int                           numSkimSets;
    private int                           numTransitAlts;
    private int                           numWalkPropClass;
    
    private double                        maxWalkDist;
    private double                        maxPnrDist;
    private int                           maxKnrTaps;
    
    private boolean                       recalcPersonTourTripMCAccEggUtil;
    private boolean                       recalcPersonTap2TapUtil;
    
    /**
     * Constructor.
     * 
     * @param rbMap HashMap<String, String>
     * @param UECFileName The path/name of the UEC containing the walk-transit model.
     * @param modelSheet The sheet (0-indexed) containing the model specification.
     * @param dataSheet The sheet (0-indexed) containing the data specification.
     */
    public BestTransitPathCalculator(HashMap<String, String> rbMap)
    {

        // read in resource bundle properties
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if ( trace )
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }
        
        String uecPath = rbMap.get("Project.Directory").toString();
  		String uecFileName = Paths.get(uecPath,rbMap.get("utility.bestTransitPath.uec.file")).toString();

        int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.data.page");
        int walkAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.walkAccess.page");
        int driveAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.driveAccess.page");
        int knrAccessPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.knrAccess.page");
        int walkEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.walkEgress.page");
        int driveEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.driveEgress.page");
        int knrEgressPage = Util.getIntegerValueFromPropertyMap(rbMap,
                "utility.bestTransitPath.knrEgress.page");
        int tapToTapPage = Util.getIntegerValueFromPropertyMap( rbMap, 
        		"utility.bestTransitPath.tapToTap.page" );
        
        File uecFile = new File(uecFileName);
        TransitWalkAccessDMU walkDmu = new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();

    	walkAccessUEC = createUEC(uecFile, walkAccessPage, dataPage, rbMap, walkDmu);
        driveAccessUEC = createUEC(uecFile, driveAccessPage, dataPage, rbMap, driveDmu);
        knrAccessUEC = createUEC(uecFile, knrAccessPage, dataPage, rbMap, driveDmu);
        walkEgressUEC = createUEC(uecFile, walkEgressPage, dataPage, rbMap, walkDmu);
        driveEgressUEC = createUEC(uecFile, driveEgressPage, dataPage, rbMap, driveDmu);
        knrEgressUEC = createUEC(uecFile, knrEgressPage, dataPage, rbMap, driveDmu);
        tapToTapUEC = createUEC(uecFile, tapToTapPage, dataPage, rbMap, walkDmu);

        //setup arrays
        numSkimSets = Util.getIntegerValueFromPropertyMap( rbMap, "utility.bestTransitPath.skim.sets" );
        numTransitAlts = Util.getIntegerValueFromPropertyMap( rbMap, "utility.bestTransitPath.alts" );
        numWalkPropClass = Util.getIntegerValueFromPropertyMap( rbMap, "utility.bestTransitPath.numWalkPropClass" );
        
        //MAZ, TAP, TAZ Data Manager
        mttData = MazTapTazData.getInstance(rbMap);
        walkDmu.setTapTable(mttData.getTapTable(), mttData.getTapIdFieldName());
    	driveDmu.setTapTable(mttData.getTapTable(), mttData.getTapIdFieldName());
        
        // these arrays are shared by the BestTransitPathCalculator objects created for each hh choice model object
        storedDataObject = StoredUtilityData.getInstance( mttData.getNumMazs(), mttData.getNumTaps(), mttData.getNumTazs(), ACC_EGR, PERIODS, numWalkPropClass, mttData.getTapIds());
        storedWalkAccessUtils = storedDataObject.getStoredWalkAccessUtils();
        storedDriveAccessUtils = storedDataObject.getStoredDriveAccessUtils();
        storedKnrAccessUtils = storedDataObject.getStoredKnrAccessUtils();
        storedWalkEgressUtils = storedDataObject.getStoredWalkEgressUtils();
        storedDriveEgressUtils = storedDataObject.getStoredDriveEgressUtils();
        storedKnrEgressUtils = storedDataObject.getStoredKnrEgressUtils();
        storedDepartPeriodTapTapUtils = storedDataObject.getStoredDepartPeriodTapTapUtils();
        
        bestUtilities = new double[numTransitAlts];
        bestPTap = new int[numTransitAlts];
        bestATap = new int[numTransitAlts];
        bestSet = new int[numTransitAlts];
        
        maxWalkDist = (double) Util.getFloatValueFromPropertyMap(rbMap,"tvpb.maxwalkdist");
        maxPnrDist = (double) Util.getFloatValueFromPropertyMap(rbMap,"tvpb.maxpnrdist");
        maxKnrTaps = (int) Util.getFloatValueFromPropertyMap(rbMap,"tvpb.maxKnrTaps");
        
        recalcPersonTourTripMCAccEggUtil = Util.getBooleanValueFromPropertyMap(rbMap,"utility.bestTransitPath.recalcPersonTourTripMCAccEggUtil");
        recalcPersonTap2TapUtil = Util.getBooleanValueFromPropertyMap(rbMap,"utility.bestTransitPath.recalcPersonTap2TapUtil");
        
    }
    
   

    /**
     * This is the main method that finds the best N TAP-pairs. It
     * cycles through walk TAPs at the origin end (associated with the origin MGRA)
     * and alighting TAPs at the destination end (associated with the destination
     * MGRA) and calculates a utility for every available alt for each TAP
     * pair. It stores the N origin and destination TAP that had the best utility.
     * 
     * @param pMgra The origin/production MGRA.
     * @param aMgra The destination/attraction MGRA.
     * 
     */
    public void findBestWalkTransitWalkTaps(TransitWalkAccessDMU walkDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);
        
        int userClass = walkDmu.getUserClass();
        int walkPropClass = walkDmu.getWalkPropClass();
        
        int[] pMgraSet = mttData.getOmazWalkTapIds(pMgra, maxWalkDist);
        int[] aMgraSet = mttData.getDmazWalkTapIds(aMgra, maxWalkDist);

        if (pMgraSet == null || aMgraSet == null)
        {
            return;
        }

        int pTaz = mttData.getTazForMaz(pMgra);
        int aTaz = mttData.getTazForMaz(aMgra);

        boolean writeCalculations = false;
        if ((tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz))|| debug)
        {
            writeCalculations = true;
        }

        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();
        
        for (int pTap : pMgraSet)
        {

            // Calculate the pMgra to pTap walk access utility values
            float accUtil; 
            if (storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass] == StoredUtilityData.default_utility) {
    			accUtil = calcWalkAccessUtility(walkDmu, pMgra, pTap, writeCalculations, myLogger);
    			storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass] = accUtil;
            } else {
            	accUtil = storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass];
            }

            for (int aTap : aMgraSet)
            {
                
                // Calculate the aTap to aMgra walk egress utility values
                float egrUtil;
                if (storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass] == StoredUtilityData.default_utility) {
                	egrUtil = calcWalkEgressUtility(walkDmu, aTap, aMgra, writeCalculations, myLogger);
        			storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass] = egrUtil;
                } else {
                	egrUtil = storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass];	
                }
                	
                // Calculate the pTap to aTap utility values
        		float tapTapUtil[] = new float[numSkimSets+1];
        		if(!storedDepartPeriodTapTapUtils.get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
        			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, pMgra, aMgra, writeCalculations, myLogger);
        			storedDepartPeriodTapTapUtils.get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
        		} else {
	                tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap));
            	}
        		
        		//create path for each skim set
        		if (accUtil != NA & tapTapUtil[userClass] != NA & egrUtil != NA) {
        			paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, userClass, WTW, accUtil, tapTapUtil[userClass], egrUtil));
            	} 
            }
        }
        
        //save N best paths
        trimPaths(paths);
        if (writeCalculations) {
            logBestUtilities(myLogger);
        }
    }

    public void findBestDriveTransitWalkTaps(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);
        int userClass = walkDmu.getUserClass();
        int walkPropClass = walkDmu.getWalkPropClass();        
        int pTaz = mttData.getTazForMaz(pMgra);
        int aTaz = mttData.getTazForMaz(aMgra);

        if (mttData.getPnrTaps(pMgra, maxPnrDist) == null
        		|| mttData.getDmazWalkTapIds(aMgra, maxWalkDist) == null)
                    {
                        return;
                    }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }
        
        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();

        int[] pTapArray = mttData.getPnrTapIds(pMgra, maxPnrDist);
        for ( int pTap : pTapArray )
        {
            // Calculate the pTaz to pTap drive access utility values
            float accUtil;
            if (storedDriveAccessUtils[pTaz][storedDataObject.getTapIndex(pTap)][walkPropClass] == StoredUtilityData.default_utility) {
    			accUtil = calcDriveAccessUtility(driveDmu, pMgra, pTaz, pTap, period, writeCalculations, myLogger);
    			storedDriveAccessUtils[pTaz][storedDataObject.getTapIndex(pTap)][walkPropClass] = accUtil;
            } else {
            	accUtil = storedDriveAccessUtils[pTaz][storedDataObject.getTapIndex(pTap)][walkPropClass];
            }
            
            for (int aTap : mttData.getDmazWalkTapIds(aMgra, maxWalkDist))
            {
                
                // Calculate the aTap to aMgra walk egress utility values
                float egrUtil;
                if (storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass] == StoredUtilityData.default_utility) {
        			egrUtil = calcWalkEgressUtility(walkDmu, aTap, aMgra, writeCalculations, myLogger);
        			storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass] = egrUtil;
                } else {
                	egrUtil = storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass];	
                }
                                    
                // Calculate the pTap to aTap utility values
        		float tapTapUtil[] = new float[numSkimSets+1];
        		if(!storedDepartPeriodTapTapUtils.get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
        			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, pMgra, aMgra, writeCalculations, myLogger);
        			storedDepartPeriodTapTapUtils.get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
        		} else {
	                tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap));
            	}
        		
        		//create path for each skim set
        		if (accUtil != NA & tapTapUtil[userClass] != NA & egrUtil != NA) {
        			paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, userClass, DTW, accUtil, tapTapUtil[userClass], egrUtil));
        		}
        		
            }
        }
            
        //save N best paths
        trimPaths(paths);
        if (writeCalculations) {
            logBestUtilities(myLogger);
        }
        
    }

    public void findBestKnrTransitWalkTaps(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);
        int userClass = walkDmu.getUserClass();
        int walkPropClass = walkDmu.getWalkPropClass();        
        int pTaz = mttData.getTazForMaz(pMgra);
        int aTaz = mttData.getTazForMaz(aMgra);
        
        if (mttData.getMazKnrTapIds(pMgra, maxKnrTaps) == null
        		|| mttData.getDmazWalkTapIds(aMgra, maxWalkDist) == null)
                    {
                        return;
                    }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }
        
        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();

        int[] pTapArray = mttData.getMazKnrTapIds(pMgra, maxKnrTaps);
        for ( int pTap : pTapArray )
        {
            // Calculate the pTaz to pTap knr access utility values
            float accUtil;
            if (storedKnrAccessUtils[pTaz][storedDataObject.getTapIndex(pTap)][walkPropClass] == StoredUtilityData.default_utility) {
    			accUtil = calcKnrAccessUtility(driveDmu, pMgra, pTaz, pTap, period, writeCalculations, myLogger);
    			storedKnrAccessUtils[pTaz][storedDataObject.getTapIndex(pTap)][walkPropClass] = accUtil;
            } else {
            	accUtil = storedKnrAccessUtils[pTaz][storedDataObject.getTapIndex(pTap)][walkPropClass];
            }
            
            for (int aTap : mttData.getDmazWalkTapIds(aMgra, maxWalkDist))
            {
                
                // Calculate the aTap to aMgra walk egress utility values
                float egrUtil;
                if (storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass] == StoredUtilityData.default_utility) {
        			egrUtil = calcWalkEgressUtility(walkDmu, aTap, aMgra, writeCalculations, myLogger);
        			storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass] = egrUtil;
                } else {
                	egrUtil = storedWalkEgressUtils[storedDataObject.getTapIndex(aTap)][aMgra][walkPropClass];	
                }
                                    
                // Calculate the pTap to aTap utility values
        		float tapTapUtil[] = new float[numSkimSets+1];
        		if(!storedDepartPeriodTapTapUtils.get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
        			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, pMgra, aMgra, writeCalculations, myLogger);
        			storedDepartPeriodTapTapUtils.get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
        		} else {
	                tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap));
            	}
        		
        		//create path for each skim set
        		if (accUtil != NA & tapTapUtil[userClass] != NA & egrUtil != NA) {
        			paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, userClass, KTW, accUtil, tapTapUtil[userClass], egrUtil));
        		}
        		
            }
        }
            
        //save N best paths
        trimPaths(paths);
        if (writeCalculations) {
            logBestUtilities(myLogger);
        }
        
    }
    
    public void findBestWalkTransitDriveTaps(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);
        int userClass = walkDmu.getUserClass();
        int walkPropClass = walkDmu.getWalkPropClass();
        
        int pTaz = mttData.getTazForMaz(pMgra);
        int aTaz = mttData.getTazForMaz(aMgra);

        if (mttData.getOmazWalkTapIds(pMgra, maxWalkDist) == null
                || mttData.getPnrTaps(aMgra, maxPnrDist) == null)
                    {
                        return;
                    }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }

        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();
        
        for (int pTap : mttData.getOmazWalkTapIds(pMgra, maxWalkDist))
        {
            // Calculate the pMgra to pTap walk access utility values
            float accUtil;
            if (storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass] == StoredUtilityData.default_utility) {
    			accUtil = calcWalkAccessUtility(walkDmu, pMgra, pTap, writeCalculations, myLogger);
    			storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass] = accUtil;
            } else {
            	accUtil = storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass];
            }

            for (int aTap : mttData.getPnrTapIds(aMgra, maxPnrDist))
            {

            	// Calculate the aTap to aMgra drive egress utility values
                float egrUtil;
                if (storedDriveEgressUtils[storedDataObject.getTapIndex(aTap)][aTaz][walkPropClass] == StoredUtilityData.default_utility) {
        			egrUtil = calcDriveEgressUtility(driveDmu, aTap, aTaz, aMgra, period, writeCalculations, myLogger);
        			storedDriveEgressUtils[storedDataObject.getTapIndex(aTap)][aTaz][walkPropClass] = egrUtil;
                } else {
                	egrUtil = storedDriveEgressUtils[storedDataObject.getTapIndex(aTap)][aTaz][walkPropClass];	
                }
            	
                // Calculate the pTap to aTap utility values
        		float tapTapUtil[] = new float[numSkimSets+1];
        		if(!storedDepartPeriodTapTapUtils.get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
        			
        			//loop across number of skim sets  the pTap to aTap utility values 
	            	tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, pMgra, aMgra, writeCalculations, myLogger);
        			storedDepartPeriodTapTapUtils.get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
        		} else {
	                tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap));
            	}
        		
        		//create path for each skim set
        		if (accUtil != NA & tapTapUtil[userClass] != NA & egrUtil != NA) {
            		paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, userClass, WTD, accUtil, tapTapUtil[userClass], egrUtil));
        		}
   
            }
        }
        
        //save N best paths
        trimPaths(paths);
        if (writeCalculations) {
            logBestUtilities(myLogger);
        }
    }
    
    public void findBestWalkTransitKnrTaps(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int period, int pMgra, int aMgra, boolean debug, Logger myLogger)
    {

        clearBestArrays(Double.NEGATIVE_INFINITY);
        int userClass = walkDmu.getUserClass();
        int walkPropClass = walkDmu.getWalkPropClass();
        int pTaz = mttData.getTazForMaz(pMgra);
        int aTaz = mttData.getTazForMaz(aMgra);

        if (mttData.getOmazWalkTapIds(pMgra, maxWalkDist) == null
                || mttData.getMazKnrTapIds(aMgra, maxKnrTaps) == null)
                    {
                        return;
                    }

        boolean writeCalculations = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz) && debug)
        {
            writeCalculations = true;
        }

        //create transit path collection
        ArrayList<TransitPath> paths = new ArrayList<TransitPath>();
        
        for (int pTap : mttData.getOmazWalkTapIds(pMgra, maxWalkDist))
        {
            // Calculate the pMgra to pTap walk access utility values
            float accUtil;
            if (storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass] == StoredUtilityData.default_utility) {
    			accUtil = calcWalkAccessUtility(walkDmu, pMgra, pTap, writeCalculations, myLogger);
    			storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass] = accUtil;
            } else {
            	accUtil = storedWalkAccessUtils[pMgra][storedDataObject.getTapIndex(pTap)][walkPropClass];
            }

            for (int aTap : mttData.getMazKnrTapIds(aMgra, maxKnrTaps))
            {

            	// Calculate the aTap to aMgra knr egress utility values
                float egrUtil;
                if (storedKnrEgressUtils[storedDataObject.getTapIndex(aTap)][aTaz][walkPropClass] == StoredUtilityData.default_utility) {
        			egrUtil = calcKnrEgressUtility(driveDmu, aTap, aTaz, aMgra, period, writeCalculations, myLogger);
        			storedKnrEgressUtils[storedDataObject.getTapIndex(aTap)][aTaz][walkPropClass] = egrUtil;
                } else {
                	egrUtil = storedKnrEgressUtils[storedDataObject.getTapIndex(aTap)][aTaz][walkPropClass];	
                }
            	
                // Calculate the pTap to aTap utility values
        		float tapTapUtil[] = new float[numSkimSets+1];
        		if(!storedDepartPeriodTapTapUtils.get(period).containsKey(storedDataObject.paTapKey(pTap, aTap))) {
        			
        			//loop across number of skim sets  the pTap to aTap utility values 
	            	tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, pMgra, aMgra, writeCalculations, myLogger);
        			storedDepartPeriodTapTapUtils.get(period).putIfAbsent(storedDataObject.paTapKey(pTap, aTap), tapTapUtil);
        		} else {
	                tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap));
            	}
        		
        		//create path for each skim set
        		if (accUtil != NA & tapTapUtil[userClass] != NA & egrUtil != NA) {
            		paths.add(new TransitPath(pMgra, aMgra, pTap, aTap, userClass, WTK, accUtil, tapTapUtil[userClass], egrUtil));
        		}
   
            }
        }
        
        //save N best paths
        trimPaths(paths);
        if (writeCalculations) {
            logBestUtilities(myLogger);
        }
    }
    
    public float calcWalkAccessUtility(TransitWalkAccessDMU walkDmu, int pMgra, int pTap, boolean myTrace, Logger myLogger)
    {
    	
    	//set DMU attributes
    	walkDmu.setMaz2TapDistance(mttData.getODistance(pMgra, pTap));
    	walkDmu.setDmuIndexValues(pTap, -1, -1);
        float util = (float)walkAccessUEC.solve(walkDmu.getDmuIndexValues(), walkDmu, null)[0];
        
        // logging
        if (myTrace && tracer.isTraceZone(mttData.getTazForMaz(pMgra))) {
            walkAccessUEC.logAnswersArray(myLogger, "Walk Orig Mgra=" + pMgra + ", to pTap=" + pTap + " Utility Piece");
        }
        
        return(util);
        
    }
    
    public float calcDriveAccessUtility(TransitDriveAccessDMU driveDmu, int pMgra, int pTaz, int pTap, int period, boolean myTrace, Logger myLogger)
    {
    	//set DMU attributes
    	driveDmu.setClosestPnrTapTaz(mttData.getClosestPnrTapTaz(pMgra));
    	driveDmu.setTOD(period);
    	driveDmu.setDmuIndexValues(pTap, pTaz, mttData.getTazForTap(pTap));
        float util = (float)driveAccessUEC.solve(driveDmu.getDmuIndexValues(), driveDmu, null)[0];

        // logging
        if (myTrace && tracer.isTraceZone(mttData.getTazForMaz(pMgra))) {
        	driveAccessUEC.logAnswersArray(myLogger, "Drive from Orig Taz=" + pTaz + ", to Dest pTap=" + pTap + " Utility Piece");
        }
        return(util);
    }
    
    public float calcKnrAccessUtility(TransitDriveAccessDMU driveDmu, int pMgra, int pTaz, int pTap, int period, boolean myTrace, Logger myLogger)
    {
    	//set DMU attributes
    	driveDmu.setClosestTapTaz(mttData.getClosestTapTaz(pMgra));
    	driveDmu.setTOD(period);
    	driveDmu.setDmuIndexValues(pTap, pTaz, mttData.getTazForTap(pTap));
        float util = (float)knrAccessUEC.solve(driveDmu.getDmuIndexValues(), driveDmu, null)[0];

        // logging
        if (myTrace && tracer.isTraceZone(mttData.getTazForMaz(pMgra))) {
        	knrAccessUEC.logAnswersArray(myLogger, "Knr from Orig Taz=" + pTaz + ", to Dest pTap=" + pTap + " Utility Piece");
        }
        return(util);
    }
    
    public float calcWalkEgressUtility(TransitWalkAccessDMU walkDmu, int aTap, int aMgra, boolean myTrace, Logger myLogger)
    {
    	
    	//set DMU attributes
    	walkDmu.setMaz2TapDistance(mttData.getDDistance(aMgra, aTap));
    	walkDmu.setDmuIndexValues(aTap, -1, -1);
        float util = (float)walkEgressUEC.solve(walkDmu.getDmuIndexValues(), walkDmu, null)[0];

        // logging
        if (myTrace && tracer.isTraceZone(mttData.getTazForMaz(aMgra))) {
        	walkEgressUEC.logAnswersArray(myLogger, "Walk from Orig aTap=" + aTap + ", to Dest Mgra=" + aMgra + " Utility Piece");
        }    
        return(util);
    }
    
    public float calcDriveEgressUtility(TransitDriveAccessDMU driveDmu, int aTap, int aTaz, int aMgra, int period, boolean myTrace, Logger myLogger)
    {
    	
    	//set DMU attributes
    	driveDmu.setClosestPnrTapTaz(mttData.getClosestPnrTapTaz(aMgra));
    	driveDmu.setTOD(period);
    	driveDmu.setDmuIndexValues(aTap, aTaz, mttData.getTazForTap(aTap));
        float util = (float)driveEgressUEC.solve(driveDmu.getDmuIndexValues(), driveDmu, null)[0];

        // logging
        if (myTrace && tracer.isTraceZone(mttData.getTazForMaz(aMgra))) {
            //driveEgressUEC.logAnswersArray(myLogger, "Drive Tap to Dest Taz Utility Piece");
        	driveEgressUEC.logAnswersArray(myLogger, "Drive from Orig aTap=" + aTap + ", to Dest Taz=" + aTaz + " Utility Piece");
        }
        return(util);
    }
    
    public float calcKnrEgressUtility(TransitDriveAccessDMU driveDmu, int aTap, int aTaz, int aMgra, int period, boolean myTrace, Logger myLogger)
    {
    	
    	//set DMU attributes
    	driveDmu.setClosestTapTaz(mttData.getClosestTapTaz(aMgra));
    	driveDmu.setTOD(period);
    	driveDmu.setDmuIndexValues(aTap, aTaz, mttData.getTazForTap(aTap));
        float util = (float)knrEgressUEC.solve(driveDmu.getDmuIndexValues(), driveDmu, null)[0];

        // logging
        if (myTrace && tracer.isTraceZone(mttData.getTazForMaz(aMgra))) {
            knrEgressUEC.logAnswersArray(myLogger, "Knr from Orig aTap=" + aTap + ", to Dest Taz=" + aTaz + " Utility Piece");
        }
        return(util);
    }
    
    public float[] calcUtilitiesForTapPair(TransitWalkAccessDMU walkDmu, int period, int pTap, int aTap, int origMgra, int destMgra, boolean myTrace, Logger myLogger) {
   	
        // set up the index and dmu objects
        walkDmu.setTOD(period);
        walkDmu.setDmuIndexValues(-1, pTap, aTap);
        int oldUserClass = walkDmu.getUserClass();
        
        // solve for all user classes so caching is easier
        float tapTapUtil[] = new float[numSkimSets+1];
        for (int i = 0; i < numSkimSets; i++)
        {
        	walkDmu.setUserClass(i+1);
        	tapTapUtil[i+1] = (float)tapToTapUEC.solve(walkDmu.getDmuIndexValues(), walkDmu, null)[0];
        	
        	// logging
            if (myTrace && tracer.isTraceZonePair( mttData.getTazForMaz(origMgra),  mttData.getTazForMaz(destMgra) )) {
            	tapToTapUEC.logAnswersArray(myLogger, "Transit User Class: " + walkDmu.getUserClass() + " From Orig pTap=" + pTap + " (Origin MAZ:" + origMgra +") " +  " to Dest aTap=" + aTap + " (Dest MAZ:" + destMgra +") " + " Utility Piece");
                tapToTapUEC.logResultsArray(myLogger, pTap, aTap);
            }
        }
        
        //return to existing user class
        walkDmu.setUserClass(oldUserClass);
        
        return(tapTapUtil);
    }

    
    /**
     * Trim the paths calculated for this TAP-pair to the best N.  
     * Set the bestUtilities[], bestSet[], bestPTap[] and bestATap[]
     * 
     * @param ArrayList<TransitPath> paths Collection of paths
     */
    public void trimPaths(ArrayList<TransitPath> paths)
    {

    	//sort paths by total utility in reverse order to get highest utility first
    	//Collections.sort(paths, Collections.reverseOrder());
    	
    	//sort paths by total gen time
    	Collections.sort(paths);
    	
    	//get best N paths
		int count = 0;
		for(TransitPath path : paths) {
			
			if (path.getTotalUtility() > NA) {
			
				//get data
				bestUtilities[count] = path.getTotalUtility();
	            bestPTap[count] = path.pTap;
	            bestATap[count] = path.aTap;
	            bestSet[count] = path.set;
	            
	            count = count + 1;
				if(count == numTransitAlts) { 
					break;
				}
			}
		}
    }
    
    public float calcPathUtility(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int accEgr, int period, int origMgra, int pTap, int aTap, int destMgra, int userClass, boolean calcTap2Tap, boolean myTrace, Logger myLogger) {
    	
    	float accUtil    =NA;
        float egrUtil    =NA;
        float tapTapUtil =NA;
        
    	if(accEgr==WTW) {
    		accUtil = calcWalkAccessUtility(walkDmu, origMgra, pTap, myTrace, myLogger);
            egrUtil = calcWalkEgressUtility(walkDmu, aTap, destMgra, myTrace, myLogger);            
            if (calcTap2Tap) {
            	tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, origMgra, destMgra, myTrace, myLogger)[walkDmu.getUserClass()];
            } else {
            	tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap))[walkDmu.getUserClass()];
            }
    	} else if(accEgr==WTD) {
    		int aTaz = mttData.getTazForMaz(destMgra);
    		accUtil = calcWalkAccessUtility(walkDmu, origMgra, pTap, myTrace, myLogger);
    		egrUtil = calcDriveEgressUtility(driveDmu, aTap, aTaz, destMgra, period, myTrace, myLogger);
    		if (calcTap2Tap) {
    			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, origMgra, destMgra, myTrace, myLogger)[walkDmu.getUserClass()];
            } else {
            	tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap))[walkDmu.getUserClass()];
            }
    	} else if(accEgr==DTW) {
    		int pTaz = mttData.getTazForMaz(origMgra);
    		accUtil = calcDriveAccessUtility(driveDmu, origMgra, pTaz, pTap, period, myTrace, myLogger);
    		egrUtil = calcWalkEgressUtility(walkDmu, aTap, destMgra, myTrace, myLogger);
    		if (calcTap2Tap) {
    			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, origMgra, destMgra, myTrace, myLogger)[walkDmu.getUserClass()];
            } else {
            	tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap))[walkDmu.getUserClass()];
            }
    	} else if(accEgr==WTK) {
    		int aTaz = mttData.getTazForMaz(destMgra);
    		accUtil = calcWalkAccessUtility(walkDmu, origMgra, pTap, myTrace, myLogger);
    		egrUtil = calcKnrEgressUtility(driveDmu, aTap, aTaz, destMgra, period, myTrace, myLogger);
    		if (calcTap2Tap) {
    			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, origMgra, destMgra, myTrace, myLogger)[walkDmu.getUserClass()];
            } else {
            	tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap))[walkDmu.getUserClass()];
            }
    	} else if(accEgr==KTW) {
    		int pTaz = mttData.getTazForMaz(origMgra);
    		accUtil = calcKnrAccessUtility(driveDmu, origMgra, pTaz, pTap, period, myTrace, myLogger);
    		egrUtil = calcWalkEgressUtility(walkDmu, aTap, destMgra, myTrace, myLogger);
    		if (calcTap2Tap) {
    			tapTapUtil = calcUtilitiesForTapPair(walkDmu, period, pTap, aTap, origMgra, destMgra, myTrace, myLogger)[walkDmu.getUserClass()];
            } else {
            	tapTapUtil = storedDepartPeriodTapTapUtils.get(period).get(storedDataObject.paTapKey(pTap, aTap))[walkDmu.getUserClass()];
            }
    	}
    	
    	if (accUtil != NA & tapTapUtil != NA & egrUtil != NA) {
    		return(accUtil + tapTapUtil + egrUtil);
    	} else {
    		
    		if(myTrace) {
	            myLogger.info("--------------------------------------------------------");
	            myLogger.info("TAP pair unavailable at person specific level           ");
	            myLogger.info("--------------------------------------------------------");
	    		myLogger.info("maxWalk " + walkDmu.maxWalk);
	    		myLogger.info("path attributes");
	    		myLogger.info("accEgr " + accEgr);
	    		myLogger.info("maz2TapDistance " + walkDmu.getMaz2TapDistance());
	    		myLogger.info("accUtil " + accUtil);
	        	myLogger.info("tapTapUtil " + tapTapUtil);
	            myLogger.info("egrUtil " + egrUtil);
	            myLogger.info("");
    		}
            
    		return(NA_TIME);
    	}
    }
    
    /**
     * Return the array of transit best tap pairs for the given access/egress mode, origin MGRA,
     * destination MGRA, and departure time period.
     * 
     * @param TransitWalkAccessDMU walkDmu
     * @param TransitDriveAccessDMU driveDmu
     * @param Modes.AccessMode accMode
     * @param origMgra Origin MGRA
     * @param workMgra Destination MGRA
     * @param departPeriod Departure time period
     * @param debug boolean flag to indicate if debugging reports should be logged
     * @param logger Logger to which debugging reports should be logged if debug is true
     * @return double[][] Array of best tap pair values - rows are N-path, columns are orig tap, dest tap, skim set, utility
     */
    public double[][] getBestTapPairs(TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int accMode, int origMgra, int destMgra, int departPeriod, boolean debug, Logger myLogger)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info("");
            header = ACC_EGR_LABELS[accMode] + " best tap pairs debug info for origMgra=" + origMgra
                    + ", destMgra=" + destMgra + ", period index=" + departPeriod
                    + ", period label=" + PERIODS[departPeriod];
            for (int i = 0; i < header.length(); i++)
                separator += "^";

            myLogger.info("");
            myLogger.info(separator);
            myLogger.info("Calculating " + header);
        }

        double[][] bestTaps = null;

        if(accMode==WTW) {
        	findBestWalkTransitWalkTaps(walkDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	} else if(accMode==DTW) {
    		findBestDriveTransitWalkTaps(walkDmu, driveDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	} else if(accMode==WTD) {
    		findBestWalkTransitDriveTaps(walkDmu, driveDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	} else if(accMode==KTW) {
    		findBestKnrTransitWalkTaps(walkDmu, driveDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	} else if(accMode==WTK) {
    		findBestWalkTransitKnrTaps(walkDmu, driveDmu, departPeriod, origMgra, destMgra, debug, myLogger);
    	}

        // get and log the best tap-tap utilities by alt
        double[] bestUtilities = getBestUtilities();
        bestTaps = new double[bestUtilities.length][];
        
        for (int i = 0; i < bestUtilities.length; i++)
        {
            //only initialize tap data if valid; otherwise null array
        	if (bestUtilities[i] > NA) bestTaps[i] = getBestTaps(i);
        }
        
        // log the best utilities and tap pairs for each alt
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info(separator);
        	myLogger.info(header);
        	myLogger.info("Final Best Utilities:");
        	myLogger.info("Alt, Alt, Utility, bestITap, bestJTap, bestSet");
            for (int i = 0; i < bestUtilities.length; i++)
            {
                myLogger.info(i + "," + i + "," + bestUtilities[i] + ","
                        + (bestTaps[i] == null ? "NA" : bestTaps[i][0]) + ","
                        + (bestTaps[i] == null ? "NA" : bestTaps[i][1]) + ","
                        + (bestTaps[i] == null ? "NA" : bestTaps[i][2]));
            }

            myLogger.info(separator);
        }
        return bestTaps;
    }
    
    /**
     * Calculate utilities for the best tap pairs using person specific attributes.
     * 
     * @param double[][] bestTapPairs
     * @param TransitWalkAccessDMU walkDmu
     * @param TransitDriveAccessDMU driveDmu
     * @param Modes.AccessMode accMode
     * @param origMgra Origin MGRA
     * @param workMgra Destination MGRA
     * @param departPeriod Departure time period
     * @param debug boolean flag to indicate if debugging reports should be logged
     * @param logger Logger to which debugging reports should be logged if debug is true
     * @return double[][] Array of best tap pair values - rows are N-path, columns are orig tap, dest tap, skim set, utility
     */
    public double[][] calcPersonSpecificUtilities(double[][] bestTapPairs, TransitWalkAccessDMU walkDmu, TransitDriveAccessDMU driveDmu, int accMode, int origMgra, int destMgra, int departPeriod, boolean debug, Logger myLogger, String label)
    {

        String separator = "";
        String header = "";
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info("");
            header = label + " " + ACC_EGR_LABELS[accMode] + " best tap pairs person specific utility info for origMgra=" + origMgra
                    + ", destMgra=" + destMgra + ", period index=" + departPeriod
                    + ", period label=" + PERIODS[departPeriod];
            for (int i = 0; i < header.length(); i++)
                separator += "^";

            myLogger.info("");
            myLogger.info(separator);
            myLogger.info("Calculating " + header);
            myLogger.info(separator);
        }

        //re-calculate utilities
        for (int i = 0; i < bestTapPairs.length; i++) {
            if (bestTapPairs[i] != null) {
            	int pTap = (int)bestTapPairs[i][0];
            	int aTap = (int)bestTapPairs[i][1];
            	int set  = (int)bestTapPairs[i][2];
            	double utility =  calcPathUtility(walkDmu, driveDmu, accMode, departPeriod, origMgra, pTap, aTap, destMgra, set, recalcPersonTap2TapUtil, debug, myLogger);
            	bestTapPairs[i][3] = utility;
            }
        }
        
        //sort by utility
        Arrays.sort(bestTapPairs, new java.util.Comparator<double[]>() {
            public int compare(double[] a, double[] b) {
                return Double.compare(a == null ? NA_TIME : a[3], b == null ? NA_TIME : b[3]);
            }
        });
        
        // log the best utilities and tap pairs for each alt
        if (debug)
        {
        	myLogger.info("");
        	myLogger.info(separator);
        	myLogger.info(header);
        	myLogger.info("Final Person Specific Best Utilities:");
        	myLogger.info("Alt, Alt, Utility, bestITap, bestJTap, bestSet");
            for (int i = 0; i < bestUtilities.length; i++)
            {
                myLogger.info(i + "," + i + "," 
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][3]) + ","
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][0]) + ","
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][1]) + ","
                        + (bestTapPairs[i] == null ? "NA" : bestTapPairs[i][2]));
            }

            myLogger.info(separator);
        }
        return bestTapPairs;
    }
       
    /**
     * Log the best utilities so far to the logger.
     * 
     * @param localLogger The logger to use for output.
     */
    public void logBestUtilities(Logger localLogger)
    {

        // create the header
        String header = String.format("%16s", "Alternative");
        header += String.format("%14s", "Utility");
        header += String.format("%14s", "PTap");
        header += String.format("%14s", "ATap");
        header += String.format("%14s", "Set");

        localLogger.info("Best Utility and Tap to Tap Pair");
        localLogger.info(header);

        // log the utilities and tap number for each alternative
        for (int i=0; i<numTransitAlts; i++)
        {
            header = header + String.format("  %16s", i);
        }
        for (int i=0; i<numTransitAlts; i++)
        {
            String line = String.format("%16s", i);
            line = line + String.format("  %12.4f", bestUtilities[i]);
            line = line + String.format("  %12d", bestPTap[i]);
            line = line + String.format("  %12d", bestATap[i]);
            line = line + String.format("  %12d", bestSet[i]);

            localLogger.info(line);
        }
    }

    public void setTrace(boolean myTrace)
    {
        tracer.setTrace(myTrace);
    }

    /**
     * Trace calculations for a zone pair.
     * 
     * @param itaz
     * @param jtaz
     * @return true if zone pair should be traced, otherwise false
     */
    public boolean isTraceZonePair(int itaz, int jtaz)
    {
        if (tracer.isTraceOn()) {
            return tracer.isTraceZonePair(itaz, jtaz);
        } else {
            return false;
        }
    }

    /**
     * Get the best utilities.
     * 
     * @return An array of the best utilities.
     */
    public double[] getBestUtilities()
    {
        return bestUtilities;
    }

    /**
     * Create the UEC for the main transit portion of the utility.
     * 
     * @param uecSpreadsheet The .xls workbook with the model specification.
     * @param modelSheet The sheet with model specifications.
     * @param dataSheet The sheet with the data specifications.
     * @param rb A resource bundle with the path to the skims "skims.path"
     * @param dmu The DMU class for this UEC.
     */
    public UtilityExpressionCalculator createUEC(File uecSpreadsheet, int modelSheet,
            int dataSheet, HashMap<String, String> rbMap, VariableTable dmu)
    {
        return new UtilityExpressionCalculator(uecSpreadsheet, modelSheet, dataSheet, rbMap, dmu);
    }

    /**
     * Clears the arrays. This method gets called for two different purposes. One is
     * to compare alternatives based on utilities and the other based on
     * exponentiated utilities. For this reason, the bestUtilities will be
     * initialized by the value passed in as an argument set by the calling method.
     * 
     * @param initialization value
     */
    public void clearBestArrays(double initialValue)
    {
        Arrays.fill(bestUtilities, initialValue);
        Arrays.fill(bestPTap, 0);
        Arrays.fill(bestATap, 0);
        Arrays.fill(bestSet, 0);
    }

    /**
     * Get the best ptap, atap, and skim set in an array. Only to be called after trimPaths() has been called.
     * 
     * @param alt.
     * @return element 0 = best ptap, element 1 = best atap, element 2 = set, element 3= utility
     */
    public double[] getBestTaps(int alt)
    {

    	double[] bestTaps = new double[4];

        bestTaps[0] = bestPTap[alt];
        bestTaps[1] = bestATap[alt];
        bestTaps[2] = bestSet[alt];
        bestTaps[3] = bestUtilities[alt];

        return bestTaps;
    }

    /**
     * Get the best transit alt. Returns null if no transit alt has a valid utility. 
     * Call only after calling findBestWalkTransitWalkTaps().
     * 
     * @return The best transit alt (highest utility), or null if no alt have a valid utility.
     */
    public int getBestTransitAlt()
    {

        int best = -1;
        double bestUtility = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < bestUtilities.length; ++i)
        {
            if (bestUtilities[i] > bestUtility) {
            	best = i;
                bestUtility = bestUtilities[i];
            }
        }
        
        int returnSet = best;
        if (best > -1) {
        	returnSet = best;
        }
        return returnSet;
    }
 
    public MazTapTazData getMttData() {
    	return(mttData);
    }
    
    public boolean getRecalcPersonAccEggUtil() {
    	return(recalcPersonTourTripMCAccEggUtil);
    }
   
}
