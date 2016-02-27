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

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;

/**
 * WalkDMU is the Decision-Making Unit class for the Walk-transit choice. The class
 * contains getter and setter methods for the variables used in the WalkPathUEC.
 */
public class TransitWalkAccessDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(TransitWalkAccessDMU.class);
    
    IndexValues dmuIndex = null;
    protected HashMap<String, Integer> methodIndexMap;

    private TableDataSet tapTable;
	private HashMap<Integer,Integer> tapToRowIndex;
	
    //person specific and joint tour defaults
    int age = 35;
	int cars = 1;

	//person variables with defaults by walk prop class
	float walkTimeWeight;
    float walkSpeed;
    float maxWalk;
    float persValueOfTime;
    int userClass = 3;  //1=avoid walking, 2=avoid transfers, 3=best route
    int walkPropClass = 3;  //1=0.0-0.3, 2=0.3-0.6, 3=0.6-1.0
    float[] walkTimeWeightWalkPropClass = {-1f,2.75f,2.00f,1.00f};
    float[] walkSpeedWalkPropClass = {-1f,1.90f,2.80f,4.00f};
    float[] maxWalkWalkPropClass = {-1f,1.25f,2.00f,3.00f};
    float[] persValueOfTimeWalkPropClass = {-1f,8f,10f,12f};
    
	//trip zonal data and TOD
	int closestPnrTapTaz;
	int closestTapTaz;
	double maz2tapDistance;
    int   tod;
    
    public TransitWalkAccessDMU()
    {
    	dmuIndex = new IndexValues();
    	setupMethodIndexMap();
    }
    
    public void setTapTable(TableDataSet tapTable, String tapFieldName) {
    	this.tapTable = tapTable;
    	int[] tapIds = tapTable.getColumnAsInt(tapFieldName);
    	
    	//create lookup
    	tapToRowIndex = new HashMap<Integer,Integer>();
	    for(int i=0; i<tapIds.length; i++) {
	    	tapToRowIndex.put(tapIds[i], i);
	    }
    	
    }
    
    public void setDmuIndexValues(int zoneId, int origTaz, int destTaz) {
        dmuIndex.setZoneIndex(zoneId);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);
    }
    
    public IndexValues getDmuIndexValues() {
        return dmuIndex; 
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
       this.age = age;
    }
    
    public int getUserClass() {
        return userClass;
    }

    public void setUserClass(int userClass) {
       this.userClass = userClass;
       
    }
    
    public void setWalkPropClass(int walkPropClass) {
    	this.walkPropClass = walkPropClass;
	    walkTimeWeight = walkTimeWeightWalkPropClass[walkPropClass];
	    walkSpeed = walkSpeedWalkPropClass[walkPropClass];
	    maxWalk = maxWalkWalkPropClass[walkPropClass];
	    persValueOfTime = persValueOfTimeWalkPropClass[walkPropClass];
    }
    
    public int getWalkPropClass() {
        return walkPropClass;
    }
    
    public int getCars() {
        return cars;
    }

    public void setCars(int cars) {
       this.cars = cars;
    }
       
    public double getMaz2TapDistance() {
        return maz2tapDistance;
    }

    public void setMaz2TapDistance(double maz2tapDistance) {
       this.maz2tapDistance = maz2tapDistance;
    }
    
    public int getClosestPnrTapTaz() {
        return closestPnrTapTaz;
    }

    public void setClosestPnrTapTaz(int closestPnrTapTaz) {
       this.closestPnrTapTaz = closestPnrTapTaz;
    }
    
    public int getClosestTapTaz() {
        return closestTapTaz;
    }

    public void setClosestTapTaz(int closestTapTaz) {
       this.closestTapTaz = closestTapTaz;
    }
       
    public float getStop_type_btap() {
    	return(tapTable.getColumnAsFloat("stop_type")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
    }
    
	public float getReltime_if_btap() {
		return(tapTable.getColumnAsFloat("reltime_if")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getF_pnr_sp_btap() {
		return(tapTable.getColumnAsFloat("f_pnr_sp")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getIf_pnr_sp_btap() {
		return(tapTable.getColumnAsFloat("if_pnr_sp")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getDpark_cost_btap() {
		return(tapTable.getColumnAsFloat("dpark_cost")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getLot_time_btap() {
		return(tapTable.getColumnAsFloat("lot_time")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getKnr_convf_btap() {
		return(tapTable.getColumnAsFloat("knr_convf")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getCrime_rate_btap() {
		return(tapTable.getColumnAsFloat("crime_rate")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getRetail_den_btap() {
		return(tapTable.getColumnAsFloat("retail_den")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getFare_1brd_btap() {
		return(tapTable.getColumnAsFloat("fare_1brd")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}

    public float getStop_type_atap() {
    	return(tapTable.getColumnAsFloat("stop_type")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
    }
    
	public float getReltime_if_atap() {
		return(tapTable.getColumnAsFloat("reltime_if")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getF_pnr_sp_atap() {
		return(tapTable.getColumnAsFloat("f_pnr_sp")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getIf_pnr_sp_atap() {
		return(tapTable.getColumnAsFloat("if_pnr_sp")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getDpark_cost_atap() {
		return(tapTable.getColumnAsFloat("dpark_cost")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getLot_time_atap() {
		return(tapTable.getColumnAsFloat("lot_time")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getKnr_convf_atap() {
		return(tapTable.getColumnAsFloat("knr_convf")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getCrime_rate_atap() {
		return(tapTable.getColumnAsFloat("crime_rate")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getRetail_den_atap() {
		return(tapTable.getColumnAsFloat("retail_den")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
	public float getFare_1brd_atap() {
		return(tapTable.getColumnAsFloat("fare_1brd")[tapToRowIndex.get(dmuIndex.getZoneIndex())]);
	}
	
    public float getWalkTimeWeight() {
    	return walkTimeWeight;
    }
    
    public void setWalkTimeWeight(float walkTimeWeight){
    	this.walkTimeWeight =walkTimeWeight;
    }
    
    public float getWalkSpeed() {
    	return walkSpeed;
    }
    
    public void setWalkSpeed(float walkSpeed) {
    	this.walkSpeed = walkSpeed;
    }
    
    public float getMaxWalk() {
    	return maxWalk;
    }
    
    public void setMaxWalk(float maxWalk) {
    	this.maxWalk = maxWalk;
    }
   
    public void setTOD(int tod) {
    	this.tod = tod;
    }
    
    public int getTOD() {
    	return tod;
    }  
    
    public void setValueOfTime(float valueOfTime) {
    	this.persValueOfTime = valueOfTime;
    }
    
    public float getValueOfTime() {
    	return persValueOfTime;
    }

    /**
     * Log the DMU values.
     * 
     * @param localLogger The logger to use.
     */
    public void logValues(Logger localLogger)
    {

        localLogger.info("");
        localLogger.info("Walk DMU Values:");
        localLogger.info("");
        localLogger.info(String.format("TOD:                   %9s", tod));
    }

    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getAge", 0 );
        methodIndexMap.put( "getUserClass", 1 );
        methodIndexMap.put( "getCars", 2 );
        methodIndexMap.put( "getTOD", 3 );
        methodIndexMap.put( "getMaz2TapDistance", 8 );
        methodIndexMap.put( "getClosestPnrTapTaz", 9 );
        methodIndexMap.put( "getClosestTapTaz", 10 );
        
        methodIndexMap.put( "getStop_type_btap", 20);
        methodIndexMap.put( "getReltime_if_btap", 21);
        methodIndexMap.put( "getF_pnr_sp_btap", 22);
        methodIndexMap.put( "getIf_pnr_sp_btap", 23);
        methodIndexMap.put( "getDpark_cost_btap", 24);
        methodIndexMap.put( "getLot_time_btap", 25);
        methodIndexMap.put( "getKnr_convf_btap", 26);
        methodIndexMap.put( "getCrime_rate_btap", 27);
        methodIndexMap.put( "getRetail_den_btap", 28);
        methodIndexMap.put( "getFare_1brd_btap", 29);
        
        methodIndexMap.put( "getStop_type_atap", 40);
        methodIndexMap.put( "getReltime_if_atap", 41);
        methodIndexMap.put( "getF_pnr_sp_atap", 42);
        methodIndexMap.put( "getIf_pnr_sp_atap", 43);
        methodIndexMap.put( "getDpark_cost_atap", 44);
        methodIndexMap.put( "getLot_time_atap", 45);
        methodIndexMap.put( "getKnr_convf_atap", 46);
        methodIndexMap.put( "getCrime_rate_atap", 47);
        methodIndexMap.put( "getRetail_den_atap", 48);
        methodIndexMap.put( "getFare_1brd_atap", 49);
        
        methodIndexMap.put( "getWalkTimeWeight", 50);
        methodIndexMap.put( "getWalkSpeed", 51);
        methodIndexMap.put( "getMaxWalk", 52);
        methodIndexMap.put( "getValueOfTime", 53);
        
        methodIndexMap.put( "getUserClass", 60);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getAge();
            case 1: return getUserClass();
            case 2: return getCars();
            case 3: return getTOD();
            case 8: return getMaz2TapDistance();
            case 9: return getClosestPnrTapTaz();
            case 10: return getClosestTapTaz();

            case 20: return getStop_type_btap();
            case 21: return getReltime_if_btap();
            case 22: return getF_pnr_sp_btap();
            case 23: return getIf_pnr_sp_btap();
            case 24: return getDpark_cost_btap();
            case 25: return getLot_time_btap();
            case 26: return getKnr_convf_btap();
            case 27: return getCrime_rate_btap();
            case 28: return getRetail_den_btap();
            case 29: return getFare_1brd_btap();
            
            case 40: return getStop_type_atap();
            case 41: return getReltime_if_atap();
            case 42: return getF_pnr_sp_atap();
            case 43: return getIf_pnr_sp_atap();
            case 44: return getDpark_cost_atap();
            case 45: return getLot_time_atap();
            case 46: return getKnr_convf_atap();
            case 47: return getCrime_rate_atap();
            case 48: return getRetail_den_atap();
            case 49: return getFare_1brd_atap();
            
            case 50: return getWalkTimeWeight();
            case 51: return getWalkSpeed();
            case 52: return getMaxWalk();
            case 53: return getValueOfTime();
            
            case 60: return getUserClass();
            
            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        }
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

}
