package com.pb.cmap.tvpb;

import java.util.ArrayList;
import java.util.HashMap;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import org.apache.log4j.Logger;
import com.pb.common.datafile.TableDataSet;

public class TVPBDMU implements VariableTable {

	IndexValues dmuIndex = null;
	protected HashMap<String, Integer> methodIndexMap;
	protected transient Logger logger = Logger.getLogger(TVPBDMU.class);
	
	int hhincome;
	int educ;
	int gender;
	int purpose;
	int age;
	int userClass;
	int cars;
	int tod;
	int inbound;
	int isWalkTapPair;
	int isPnrTapPair;
	int isKnrTapPair;
	double maz2tapDistance;
	int closestTapTaz;
	int closestPnrTapTaz;
	
	float walkTimeWeight;
    float walkSpeed;
    float maxWalk;
    float persValueOfTime;
    
    private HashMap<String,Integer> userClassByType = new HashMap<String,Integer>();
	
	private TableDataSet tapTable;
	private HashMap<Integer,Integer> tapToRowIndex;
    
    public TVPBDMU () {
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
    
    public int getHhincome() {
        return hhincome;
    }

    public void setHhincome(int hhincome) {
       this.hhincome = hhincome;
    }

    public int getEduc() {
        return educ;
    }

    public void setEduc(int educ) {
       this.educ = educ;
    }
    
    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
       this.gender = gender;
    }
    
    public int getPurpose() {
        return purpose;
    }

    public void setPurpose(int purpose) {
       this.purpose = purpose;
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
    
    public int getUserClassByType(String type) {
    	return userClassByType.get(type);
    }
    
    public void setUserClassByType(String type, int userClass) {
    	this.userClassByType.put(type, userClass);
    }
    
    
    public int getCars() {
        return cars;
    }

    public void setCars(int cars) {
       this.cars = cars;
    }
    
    public int getTod() {
        return tod;
    }

    public void setTod(int tod) {
       this.tod = tod;
    }
    
    public int getInbound() {
        return inbound;
    }

    public void setInbound(int inbound) {
       this.inbound = inbound;
    }
    
    
    public int getIsWalkTapPair() {
        return isWalkTapPair;
    }

    public void setIsWalkTapPair(int isWalkTapPair) {
       this.isWalkTapPair = isWalkTapPair;
    }
    
    public int getIsPnrTapPair() {
        return isPnrTapPair;
    }

    public void setIsPnrTapPair(int isPnrTapPair) {
       this.isPnrTapPair = isPnrTapPair;
    }
    
    public int getIsKnrTapPair() {
        return isKnrTapPair;
    }

    public void setIsKnrTapPair(int isKnrTapPair) {
       this.isKnrTapPair = isKnrTapPair;
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
    
    public float getValueOfTime()
    {
        return persValueOfTime;
    }
    
    public void setValueOfTime(float vot)
    {
        persValueOfTime = vot;
    }
    
    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getAge", 0 );
        methodIndexMap.put( "getUserClass", 1 );
        methodIndexMap.put( "getCars", 2 );
        methodIndexMap.put( "getTod", 3 );
        methodIndexMap.put( "getInbound", 4 );
        methodIndexMap.put( "getIsWalkTapPair", 5 );
        methodIndexMap.put( "getIsPnrTapPair", 6 );
        methodIndexMap.put( "getIsKnrTapPair", 7 );
        methodIndexMap.put( "getMaz2TapDistance", 8 );
        methodIndexMap.put( "getClosestPnrTapTaz", 9 );
        methodIndexMap.put( "getClosestTapTaz", 10 );
        methodIndexMap.put( "getHhincome", 11 );
        methodIndexMap.put( "getEduc", 12 );
        methodIndexMap.put( "getGender", 13 );
        methodIndexMap.put( "getPurpose", 14 );
        
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
        
        methodIndexMap.put( "getUserClassWorkWalk", 60);
        methodIndexMap.put( "getUserClassWorkPnr", 61);
        methodIndexMap.put( "getUserClassWorkKnr", 62);
        
        methodIndexMap.put( "getUserClassNonWorkWalk", 63);
        methodIndexMap.put( "getUserClassNonWorkPnr", 64);
        methodIndexMap.put( "getUserClassNonWorkKnr", 65);
        
    }
    
    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getAge();
            case 1: return getUserClass();
            case 2: return getCars();
            case 3: return getTod();
            case 4: return getInbound();
            case 5: return getIsWalkTapPair();
            case 6: return getIsPnrTapPair();
            case 7: return getIsKnrTapPair();
            case 8: return getMaz2TapDistance();
            case 9: return getClosestPnrTapTaz();
            case 10: return getClosestTapTaz();
            case 11: return getHhincome();
            case 12: return getEduc();
            case 13: return getGender();
            case 14: return getPurpose();
                        
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
            
            case 60: return getUserClassByType("user_class_work_walk");
            case 61: return getUserClassByType("user_class_work_pnr");
            case 62: return getUserClassByType("user_class_work_knr");
            case 63: return getUserClassByType("user_class_non_work_walk");
            case 64: return getUserClassByType("user_class_non_work_pnr");
            case 65: return getUserClassByType("user_class_non_work_knr");
            
            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        }
    }
    
    public int getIndexValue(String variableName) {
        return methodIndexMap.get(variableName);
    }
    
    public int getAssignmentIndexValue(String variableName) {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex) {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue) {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue) {
        throw new UnsupportedOperationException();
    }
    
	
}
