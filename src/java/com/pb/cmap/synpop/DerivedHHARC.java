/*
 * Copyright  2007 PB Americas
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

package com.pb.cmap.synpop;

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.pb.models.synpopV3.DataDictionary;
import com.pb.models.synpopV3.DerivedHH;
import com.pb.models.synpopV3.PUMSPerson;
import com.pb.models.synpopV3.PropertyParser;

/**
 * DerivedHH which includes new attributes and associated methods added by John Bowman
 *  
 * @author D. Ory
 *
 */

public class DerivedHHARC extends DerivedHH {
	
	protected int hsizecat6; // JLB 20070330
	protected int hNCcat6;  // JLB 20070330
	protected int hwrkcat5;  // JLB 20070330
	
	public DerivedHHARC(String record_raw, Vector pRecords_raw, DataDictionary dd) {
	
		// call the constructor, which calls the setDerivedAttrs() method
		super(record_raw, pRecords_raw, dd);
		logger = PropertyParser.getLogger();
		
		// set the ARC-specific derived attributes
		setArcDerivedAttrs();
	}
	  
	/**
	 * Get DerivedHH attributes specific to ARC
	 */
	public int getHHDerivedAttr(String varName){
	    int result=-1;
	    if(varName.equals("hsizecat6")){
	    	result=hsizecat6;
	    }
	    else if(varName.equals("hNCcat6")){
	    	result=hNCcat6;
	    }
	    else if(varName.equals("hwrkcat5")){
	    	result=hwrkcat5;
	    }
	    else{
	    	result=super.getHHDerivedAttr(varName);
	    }
	    
	    return result;
	        
	}
	
	public void print(){
		super.print();
		
		logger.info("hsizecat6="+hsizecat6);
		logger.info("hNCcat6="+hNCcat6);
		logger.info("hwrkcat5="+hwrkcat5);
	}
	
	private void setArcDerivedAttrs(){
		setHsizecat6();
		setHwrkrcat5();
		setHNCcat6();
		
	}
	
	/** Set the Hsizecat6 variable
	 * Bowman's function
	 *
	 */
	private void setHsizecat6(){
		
	    int hsize=((Integer)hhAttrsMap.get("PERSONS")).intValue();
	    if(hsize==0){
	      hsizecat6 = 1;
	    }else if(hsize<6){
	      hsizecat6 = hsize;
	    }else{
	      hsizecat6 = 6;
	    }
	}
	
	/**
	 * Set the hwrkcat5 variables
	 * Bowman's function
	 *
	 */
	private void setHwrkrcat5(){

	    Set keySet = pRecordsMap.keySet();
	    Iterator itr = keySet.iterator();
	    String key=new String();
	    PUMSPerson person;
	    int wrkrcount2=0;

	    while (itr.hasNext()) {
	        key = (String) itr.next();
	        person=(PUMSPerson)pRecordsMap.get(key);
	        if(person.getPAttr("ESR")==1||person.getPAttr("ESR")==2||person.getPAttr("ESR")==4||person.getPAttr("ESR")==5){
	            if(person.getPAttr("AGE")>=18) wrkrcount2++;
	        }
	    }

	    if(wrkrcount2>=4) hwrkcat5 = 4;
	    else hwrkcat5=wrkrcount2;
	  
	  
	}
	
	/**
	 * Sets the number of children in household variable
	 * Bowman's function
	 */
	private void setHNCcat6(){

	    Set keySet = pRecordsMap.keySet();
	    Iterator itr = keySet.iterator();
	    String key=new String();
	    PUMSPerson person;
	    int age;
	    while (itr.hasNext()) {
	        key = (String) itr.next();
	        person=(PUMSPerson)pRecordsMap.get(key);
	        age=person.getPAttr("AGE");

	        if(age<=17) hNCcat6++;
	    }
	    
	    if (hNCcat6>5) hNCcat6=5;

	  }
	

	  
	


}
