/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.models.ctrampIf.IndividualMandatoryTourFrequencyDMU;
import com.pb.models.ctrampIf.TazDataIf;

/**
 * ArcIndividualMandatoryTourFrequencyDMU is a class that ...
 *
 * @author Kimberly Grommes
 * @version 1.0, Jul 17, 2008
 *          Created by IntelliJ IDEA.
 */
public class CmapIndividualMandatoryTourFrequencyDMU extends IndividualMandatoryTourFrequencyDMU {

	TazDataIf tazDataManager;

    public CmapIndividualMandatoryTourFrequencyDMU(TazDataIf tazDataManager){
    	super();
        setupMethodIndexMap();
       
        this.tazDataManager = tazDataManager;
    }
    

    public int getIncomeHigherThan50k(){
    	if(household.getIncomeSegment()>2) return (1);
    	return(0);
    }
    
    public int getOmazOtaz() {
    	return(tazDataManager.getTazsForMazs()[dmuIndex.getOriginZone()-1]);
    }
    
    public int getDmazDtazUsualWorkLocation() {
    	int workZone = person.getUsualWorkLocation();
    	if(workZone>0) {
    		return(tazDataManager.getTazsForMazs()[person.getUsualWorkLocation()-1]);
    	} else {
    		return 0;	
    	}
    }
    
    public int getDmazDtazUsualSchoolLocation() {
    	int schoolZone = person.getUsualSchoolLocation();
    	if(schoolZone>0) {
    		return(tazDataManager.getTazsForMazs()[person.getUsualSchoolLocation()-1]);
    	} else {
    		return 0;
    	}
    }
    
    public float getOMazDMazDistanceUsualWorkLocation() {
    	int workZone = person.getUsualWorkLocation();
    	if(workZone>0) {
    		return(tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), workZone));
    	} else {
    		return 9999;	
    	}
    }
    
    public float getOMazDMazDistanceUsualSchoolLocation() {
    	int schoolZone = person.getUsualSchoolLocation();
    	if(schoolZone>0) {
    		return(tazDataManager.getOMazDMazDistance(dmuIndex.getOriginZone(), schoolZone));
    	} else {
    		return 9999;
    	}
    }

    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getFullTimeWorker", 0 );
        methodIndexMap.put( "getPartTimeWorker", 1 );
        methodIndexMap.put( "getUniversityStudent", 2 );
        methodIndexMap.put( "getNonWorkingAdult", 3 );
        methodIndexMap.put( "getRetired", 4 );
        methodIndexMap.put( "getDrivingAgeSchoolChild", 5 );
        methodIndexMap.put( "getPreDrivingAgeSchoolChild", 6 );
        methodIndexMap.put( "getFemale", 7 );
        methodIndexMap.put( "getAge", 8 );
        methodIndexMap.put( "getStudentIsEmployed", 9 );
        methodIndexMap.put( "getNonStudentGoesToSchool", 10 );
        methodIndexMap.put( "getAutos", 11 );
        methodIndexMap.put( "getDrivers", 12 );
        methodIndexMap.put( "getPreschoolChildren", 13 );
        methodIndexMap.put( "getNonWorkers", 14 );
        methodIndexMap.put( "getIncomeHigherThan50k", 15 );
        methodIndexMap.put( "getNonFamilyHousehold", 16 );
        methodIndexMap.put( "getChildrenUnder16NotAtSchool", 17 );
        methodIndexMap.put( "getAreaType", 18 );
        methodIndexMap.put( "getUsualWorkLocation", 19 );
        methodIndexMap.put( "getUsualSchoolLocation", 20 );
        
        methodIndexMap.put( "getOmazOtaz", 30 );
        methodIndexMap.put( "getDmazDtazUsualWorkLocation", 31 );
        methodIndexMap.put( "getDmazDtazUsualSchoolLocation", 32 );
        methodIndexMap.put( "getOMazDMazDistanceUsualWorkLocation", 33 );
        methodIndexMap.put( "getOMazDMazDistanceUsualSchoolLocation", 34 );
                
    }
    


    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getFullTimeWorker();
            case 1: return getPartTimeWorker();
            case 2: return getUniversityStudent();
            case 3: return getNonWorkingAdult();
            case 4: return getRetired();
            case 5: return getDrivingAgeSchoolChild();
            case 6: return getPreDrivingAgeSchoolChild();
            case 7: return getFemale();
            case 8: return getAge();
            case 9: return getStudentIsEmployed();
            case 10: return getNonStudentGoesToSchool();
            case 11: return getAutos();
            case 12: return getDrivers();
            case 13: return getPreschoolChildren();
            case 14: return getNonWorkers();
            case 15: return getIncomeHigherThan50k();
            case 16: return getNonFamilyHousehold();
            case 17: return getChildrenUnder16NotAtSchool();
            case 18: return getAreaType();
            case 19: return getUsualWorkLocation();
            case 20: return getUsualSchoolLocation();
            
            case 30: return getOmazOtaz();
            case 31: return getDmazDtazUsualWorkLocation();
            case 32: return getDmazDtazUsualSchoolLocation();
            case 33: return getOMazDMazDistanceUsualWorkLocation();
            case 34: return getOMazDMazDistanceUsualSchoolLocation();

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
        
    }


}
