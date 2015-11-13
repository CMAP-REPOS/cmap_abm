package com.pb.cmap.tvpb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public class Trip {
	
	int recid;
	
	int hhid;
	int personid;
	int dayid;
	int tripid;
	int tourid;
	
	int otaz;
	int dtaz;
	int omaz;
	int dmaz;
	
	int age;
	int userClass;
	
	int cars;
	int tod;
	int inbound;
	
	int hhincome;
	int educ;
	int gender;
	int purpose;
	
	float walkTimeWeight;
    float walkSpeed;
    float maxWalk;
    float persValueOfTime;
	
    private HashMap<String,Integer> userClassByType = new HashMap<String,Integer>();
    
	boolean debugRecord = false;
	
	ArrayList<TapPair> tapPairs;
   
	public Trip() { }
	
	public int getHhid() {
        return hhid;
    }

    public void setHhid(int hhid) {
       this.hhid = hhid;
    }
    
	public int getPersonid() {
        return personid;
    }

    public void setPersonid(int personid) {
       this.personid = personid;
    }
	
	public int getdayid() {
        return dayid;
    }

    public void setDayid(int dayid) {
       this.dayid = dayid;
    }
    
	public int getTripid() {
        return tripid;
    }

    public void setTripid(int tripid) {
       this.tripid = tripid;
    }
    
    public int getTourid() {
        return tourid;
    }

    public void setTourid(int tourid) {
       this.tourid = tourid;
    }
    
	public int getOtaz() {
        return otaz;
    }

    public void setOtaz(int otaz) {
       this.otaz = otaz;
    }
    
	public int getDtaz() {
        return dtaz;
    }

    public void setDtaz(int dtaz) {
       this.dtaz = dtaz;
    }
    
	public int getOmaz() {
        return omaz;
    }

    public void setOmaz(int omaz) {
       this.omaz = omaz;
    }
    
    public int getDmaz() {
        return dmaz;
    }

    public void setDmaz(int dmaz) {
       this.dmaz = dmaz;
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
       
    public boolean getDebugRecord() {
        return debugRecord;
    }

    public void setDebugRecord(boolean debugRecord) {
       this.debugRecord = debugRecord;
    }
    
    public int getRecid() {
        return recid;
    }

    public void setRecid(int recid) {
       this.recid = recid;
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
    
    public float getMaxWalkInFeet() {
    	return maxWalk*5280;
    }
    
    public void setMaxWalk(float maxWalk) {
    	this.maxWalk = maxWalk;
    }
    
    public void setMaxWalkInFeet(float maxWalk) {
    	this.maxWalk = maxWalk/5280;
    }
    
    public float getValueOfTime()
    {
        return persValueOfTime;
    }
    
    public void setValueOfTime(float vot)
    {
        persValueOfTime = vot;
    }
    
    public ArrayList<TapPair> getTapPairs() {
    	return(tapPairs);
    }
    
    public ArrayList<TapPair> getWalkTapPairs() {
    	
    	ArrayList<TapPair> outTapPairs = new ArrayList<TapPair>();
        for(TapPair aTapPair : tapPairs) {    			
			if(aTapPair.isWalkTapPair) {
				outTapPairs.add(aTapPair);
			}
    	}
    	return(outTapPairs);
    }
    
    public ArrayList<TapPair> getDriveTapPairs() {
    	
    	ArrayList<TapPair> outTapPairs = new ArrayList<TapPair>();
        for(TapPair aTapPair : tapPairs) {    			
			if(aTapPair.isKnrTapPair | aTapPair.isPnrTapPair) {
				outTapPairs.add(aTapPair);
			}
    	}
    	return(outTapPairs);
    }
    
    public ArrayList<TapPair> getPnrTapPairs() {
    	
    	ArrayList<TapPair> outTapPairs = new ArrayList<TapPair>();
        for(TapPair aTapPair : tapPairs) {    			
			if(aTapPair.isPnrTapPair) {
				outTapPairs.add(aTapPair);
			}
    	}
    	return(outTapPairs);
    }

	public ArrayList<TapPair> getKnrTapPairs() {
	
		ArrayList<TapPair> outTapPairs = new ArrayList<TapPair>();
	    for(TapPair aTapPair : tapPairs) {    			
			if(aTapPair.isKnrTapPair) {
				outTapPairs.add(aTapPair);
			}
		}
		return(outTapPairs);
	}
    
    public void setTapPairs(ArrayList<TapPair> tapPairs) {
    	this.tapPairs = tapPairs;
    }
    
	public Trip[] readTripsFromCSV(HashMap<String, String> propertyMap ) throws IOException {
		
		// get properties
		String tripfile = propertyMap.get("tvpb.tripfile");
		String hhid = propertyMap.get("tvpb.tripfile.hhid");
		String personid = propertyMap.get( "tvpb.tripfile.personid");
		String dayid = propertyMap.get( "tvpb.tripfile.dayid");
		String tourid = propertyMap.get( "tvpb.tripfile.tourid");
		String tripid = propertyMap.get( "tvpb.tripfile.tripid");
		String userclass = propertyMap.get( "tvpb.tripfile.userclass");
		String cars = propertyMap.get( "tvpb.tripfile.cars");
		String age = propertyMap.get( "tvpb.tripfile.age");
		String inbound = propertyMap.get( "tvpb.tripfile.inbound");
		String hhincome = propertyMap.get( "tvpb.tripfile.hhincome");
		String educ = propertyMap.get( "tvpb.tripfile.educ");
		String gender = propertyMap.get( "tvpb.tripfile.gender");
		String purpose = propertyMap.get( "tvpb.tripfile.purpose");
		String tod = propertyMap.get( "tvpb.tripfile.tod");
		String otaz = propertyMap.get( "tvpb.tripfile.otaz");
		String dtaz = propertyMap.get( "tvpb.tripfile.dtaz");
		String omaz = propertyMap.get( "tvpb.tripfile.omaz");
		String dmaz = propertyMap.get( "tvpb.tripfile.dmaz");
		
		String debugRecord = propertyMap.get( "tvpb.tripfile.debugRecord");
				
		//read CSV
		CSVFileReader csvReader = new CSVFileReader();
	    TableDataSet trips;
        try {
        	trips = csvReader.readFile( new File(tripfile));
        } catch (IOException e) {
            throw new IOException();
        }
        
        //build array of Trip records
        Trip[] outTrips = new Trip[trips.getRowCount()];
        for (int i=0; i<outTrips.length; i++) {
        	outTrips[i] = new Trip();
        	outTrips[i].setRecid(i);
        	outTrips[i].setHhid((int)trips.getValueAt(i+1, hhid));
        	outTrips[i].setPersonid((int)trips.getValueAt(i+1, personid));
        	outTrips[i].setDayid((int)trips.getValueAt(i+1, dayid));
        	outTrips[i].setTourid((int)trips.getValueAt(i+1, tourid));
        	outTrips[i].setTripid((int)trips.getValueAt(i+1, tripid));
        	outTrips[i].setUserClass((int)trips.getValueAt(i+1, userclass));
        	outTrips[i].setCars((int)trips.getValueAt(i+1, cars));
        	outTrips[i].setAge((int)trips.getValueAt(i+1, age));
        	outTrips[i].setInbound((int)trips.getValueAt(i+1, inbound));
        	outTrips[i].setHhincome((int)trips.getValueAt(i+1, hhincome));
        	outTrips[i].setEduc((int)trips.getValueAt(i+1, educ));
        	outTrips[i].setGender((int)trips.getValueAt(i+1, gender));
        	outTrips[i].setPurpose((int)trips.getValueAt(i+1, purpose));
        	outTrips[i].setTod((int)trips.getValueAt(i+1, tod));
        	outTrips[i].setOtaz((int)trips.getValueAt(i+1, otaz));
        	outTrips[i].setDtaz((int)trips.getValueAt(i+1, dtaz));
        	outTrips[i].setOmaz((int)trips.getValueAt(i+1, omaz));
        	outTrips[i].setDmaz((int)trips.getValueAt(i+1, dmaz));
        	outTrips[i].setDebugRecord(i == Integer.parseInt(debugRecord) ? true : false);
        }
        
        return(outTrips);
	}
	
}
