/*
 * Copyright  2007 John L. Bowman
 *  You may not use this file without a license for its use.
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pb.cmap.synpop;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.pb.models.synpopV3.PUMSAttrs;
import com.pb.models.synpopV3.PropertyParser;

/**
 * @author John L Bowman
 * <John_L_Bowman@alum.mit.edu>
 * Created on Mar 13, 2007
 * 
 * --------------------------------------------------------------------------------------------
 *
 * JLB 20070317   This program
 * 					--creates a repository for
 *                  	--data item definitions
 *                  	--data item value definitions
 *                  --retrieves PUMS data item and data value definitions from an ascii PUMS definitions file
 *                  --retrieves other data item and data value definitions from a tab delimited definitions file 
 *                  --provides methods to add individual data item and value definitions to the repository
 *                      (this feature not used on initial implementation)              
 *                
*/

public class MasterDataDictionary{
    public static final boolean DEBUG = false;
    protected static Logger logger = Logger.getLogger(MasterDataDictionary.class.getName());
    //data item name<--->data item definition object 
    public LinkedHashMap entries;

    /**
     * CONSTRUCTORS AND BASIC METHODS
     */
    
    /**
     * Constructor--construct empty dictionary
     */
    public MasterDataDictionary(){
        entries=new LinkedHashMap();
    }

    /**
     * Constructor--construct dictionary with PUMS definitions
     */
    public MasterDataDictionary(String PUMSDefFileName){
        entries=new LinkedHashMap();
       	readPUMSMasterDDEntries(PUMSDefFileName);
       	setValueIndices(entries);
    }

    /**
     * Constructor--construct dictionary with PUMS and Other definitions
     */
    public MasterDataDictionary(String PUMSDefFileName, String DefFileName){
        entries=new LinkedHashMap();
       	readPUMSMasterDDEntries(PUMSDefFileName);
       	readMasterDDEntries(DefFileName);
       	setValueIndices(entries);
    }

//  public void putEntry(String itemName, DataItemDefinition definition){
    private void putEntry(String itemName, DataItemDefinition definition){
    	entries.put(itemName, definition);
    }

      public DataItemDefinition getDataItemDefinition(String itemName){
//    private DataItemDefinition getDataItemDefinition(String itemName){
    	return (DataItemDefinition)entries.get(itemName); 
    }

    
    /**
     * DATA ITEM DEFINITION CLASS WITH CONSTRUCTORS AND BASIC METHODS
     */

    private class DataItemDefinition {
       	private String itemName;
        private String shortDescription;
    	private String longDescription;
    	private String type;
    	private int length;
    	private String primaryClass;
    	protected LinkedHashMap valueDefinitions; // <stringValue,ValueDefinition>
    	protected LinkedHashMap rangeDefinitions; // <stringRangeBegin,RangeDefinition>
    	
    	/*
    	 * Constructor for item definition
    	 *  --without supplied value definitions
    	 *  --without short description
    	 */
    	public DataItemDefinition(String itemName, 
    					          String type,
    					          int length,
    					          String primaryClass,
    					          String longDescription
    					          ) {
    		this.itemName=itemName;
    		this.type=type;
    		this.length = length;
    		this.primaryClass=primaryClass;
    		this.longDescription=longDescription;
    		shortDescription="        ";
            valueDefinitions=new LinkedHashMap();
            rangeDefinitions=new LinkedHashMap();
    	}

    	/*
    	 * Constructor for item definition
    	 *  --without supplied value definitions
    	 * 	--with shortDescription
    	 */
    	public DataItemDefinition(String itemName, 
    					          String type,
    					          int length,
    					          String primaryClass,
    					          String longDescription,
    					          String shortDescription
    					          ) {
    		this.itemName=itemName;
    		this.type=type;
    		this.length = length;
    		this.primaryClass=primaryClass;
    		this.longDescription=longDescription;
    		this.shortDescription=shortDescription;
            valueDefinitions=new LinkedHashMap();
            rangeDefinitions=new LinkedHashMap();
    	}

//    	public void putValueDefinition(String stringValue, ValueDefinition definition){
       	private void putValueDefinition(String stringValue, ValueDefinition definition){
            valueDefinitions.put(stringValue,definition);
    	}

//     	public void putRangeDefinition(String rangeBottom, RangeDefinition definition){
       	private void putRangeDefinition(String rangeBottom, RangeDefinition definition){
            rangeDefinitions.put(rangeBottom,definition);
    	}
    }
    
    /**
     * DATA VALUE DEFINITION CLASS WITH CONSTRUCTORS
     */

//  public class ValueDefinition {
    private class ValueDefinition {
       	private String itemName;
       	private String stringValue;
       	private int integerValue;
       	private String shortDescription;
       	private String longDescription;
       	private int valueIndex;

       	/*
       	 * Constructor for integer values
       	 */
       	public ValueDefinition(String itemName, int integerValue, String description){
       		this.itemName=itemName;
       		this.integerValue=integerValue;
       		this.stringValue=""+integerValue;
       		this.longDescription=description;
       		this.shortDescription="        ";
       		this.valueIndex=-999999;
       		
       	}

        /*
         * Constructor for string values
         */
       	public ValueDefinition(String itemName, int integerValue, String stringValue, String description){
           	this.itemName=itemName;
           	this.integerValue= integerValue;
           	this.stringValue=stringValue;
           	this.longDescription=description;
           	this.shortDescription="        ";
       		this.valueIndex=-999999;
        }

       	/*
         * Constructor for string values
         * 	--with short description
         */
       	public ValueDefinition(String itemName,
       			int integerValue,
       			String stringValue,
       			String longDescription,
       			String shortDescription){
           	this.itemName=itemName;
           	this.integerValue= integerValue;
           	this.stringValue=stringValue;
           	this.longDescription=longDescription;
           	this.shortDescription=shortDescription;
       		this.valueIndex=-999999;
        }
    
    }

    /**
     * DATA RANGE DEFINITION CLASS WITH CONSTRUCTORS
     */

    private class RangeDefinition {
       	private String itemName;
       	private int integerRangeBottom;
       	private int integerRangeTop;
       	private String stringRangeBottom;
       	private String stringRangeTop;
       	private String shortDescription;
       	private String longDescription;

       	/*
       	 * Constructor for ranges 
       	 * 	--without short description
       	 */
       	public RangeDefinition(String itemName,
       			int integerRangeBottom,
       			int integerRangeTop,
       			String stringRangeBottom,
       			String stringRangeTop,
       			String description){
       		this.itemName=itemName;
       		this.integerRangeBottom=integerRangeBottom;
       		this.integerRangeTop=integerRangeTop;
       		this.stringRangeBottom=stringRangeBottom;
       		this.stringRangeTop=stringRangeTop;
       		this.longDescription=description;
       		this.shortDescription="        ";
       	}
       	/*
       	 * Constructor for ranges 
       	 * 	--with short description
       	 */
       	public RangeDefinition(String itemName,
       			int integerRangeBottom,
       			int integerRangeTop,
       			String stringRangeBottom,
       			String stringRangeTop,
       			String longDescription,
       			String shortDescription){
       		this.itemName=itemName;
       		this.integerRangeBottom=integerRangeBottom;
       		this.integerRangeTop=integerRangeTop;
       		this.stringRangeBottom=stringRangeBottom;
       		this.stringRangeTop=stringRangeTop;
       		this.longDescription=longDescription;
       		this.shortDescription=shortDescription;
       	}
    }	

	/*
	 * METHODS THAT ARE USED TO FILL THE DATA DICTIONARY 
	 */
    
	/*
	 * Put a single data item definition into the data dictionary
	 * without accompanying value and range definitions
	 */
    public void createDataItemDefinition(
    		String itemName,
    		String type,
    		int length,
			String primaryClass,
			String longDescription,
			String shortDescription) {
    	DataItemDefinition dID = new DataItemDefinition (
    		itemName,
    		type,
    		length,
    		primaryClass,
    		longDescription,
    		shortDescription);
    	putEntry(itemName, dID);
    }

	/*
	 * Create a single value definition 
	 * and put it into its data item definition
	 * within the data dictionary, given its data item name
	 * and the other details of its defintion
	 */
    public void createValueDefinition(
    				String itemName,
           			int integerValue,
           			String stringValue,
           			String longDescription,
           			String shortDescription){
    	ValueDefinition vD = new ValueDefinition (itemName,
       			integerValue,
       			stringValue,
       			longDescription,
       			shortDescription);
    	getDataItemDefinition(itemName).putValueDefinition(stringValue, vD);
    }

	/*
	 * Create a single range definition
	 * and put it into its data item definition
	 * within the data dictionary, given its data item name
	 * and the other details of its defintion
	 */
    public void createRangeDefinition(
           		String itemName,
   				int integerRangeBottom,
   	            int integerRangeTop,
   	            String stringRangeBottom,
   	            String stringRangeTop,
   	            String longDescription,
   	            String shortDescription){
    	RangeDefinition rD = new RangeDefinition (itemName,
    			integerRangeBottom,
    			integerRangeTop,
    			stringRangeBottom,
    			stringRangeTop,
    			longDescription,
    			shortDescription);
    	getDataItemDefinition(itemName).putRangeDefinition(stringRangeBottom, rD);
    }
    
	/*
	 * Read all the PUMS data definitions from an ascii file and put those
	 * that have been selected in the Properties file into the dictionary,
	 * including data item definitions and all accompanying value and
	 * range definitions. 
	 */
    private void readPUMSMasterDDEntries(String fileName) {
        int tokenCount;
        int RECTYPECount = 0;
        String primaryClass = null;
        //HH attributes, from property file
        Vector hhAttrs;
        //person attributes, from property file
        Vector personAttrs;
        String itemName = null;
        int length=0;
        String itemDescription;
        int integerRangeBottom;
        int integerRangeTop;
        String stringRangeBottom;
        String stringRangeTop;
        String stringValue;
        int integerValue;
        String description;
        DataItemDefinition dataItemDefinition = null;
        ValueDefinition valueDefinition = null;
        RangeDefinition rangeDefinition = null;
        boolean matchPUMSList = false;
        int ii=0;
        //get HH attribute names
        hhAttrs=PUMSAttrs.getHHAttrs();
        //get HH attribute names
        personAttrs=PUMSAttrs.getPersonAttrs();

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String s = new String();
            // skip the first record (header) which defines fields
            s = in.readLine(); ii=1;
            while ((s = in.readLine()) != null) {
            	ii=ii+1;
                if (s.length() > 0) {
                    String[] result = s.split(" ",2);
                    String[] result2 = result[1].split(" ",0);
                    if (result[0].trim().equalsIgnoreCase("D")) {
                    	matchPUMSList=false;
                		length = Integer.valueOf(result2[1]);
                    	if (result2[0].trim().equalsIgnoreCase("RECTYPE")){
                            RECTYPECount++;
                    	}
                    	else {
                    		switch(RECTYPECount){
                    			case 1: {
                    				for(int i = 0; i<hhAttrs.size();i++){
                    					if (hhAttrs.get(i).equals(result2[0].trim())){
                    						matchPUMSList=true;
                    						itemName=result2[0].trim();
                    						primaryClass="PUMSHH";
                    					}
                    				}
                        		}break;
                    			case 2: {
                    				for(int i = 0; i<personAttrs.size();i++){
                    					if (personAttrs.get(i).equals(result2[0].trim())){
                    						if(!result2[0].trim().equals("SERIALNO"))matchPUMSList=true;
                    						itemName=result2[0].trim();
                    						primaryClass="PUMSPerson";
                    					}
                    				}
                        		}break;
                    		}
                    	}
                    }
                    else if (matchPUMSList){
                    	if (result[0].trim().equalsIgnoreCase("T")) {
                    		itemDescription=result[1].trim();
                    		dataItemDefinition=new DataItemDefinition(
                    			itemName,
                    			"int",
                    			length,
                    			primaryClass,
                    			itemDescription);
                    		putEntry(itemName, dataItemDefinition);
                    	}
                    	else {
                    		if (result[0].trim().equalsIgnoreCase("V")){
                                StringTokenizer stV = new StringTokenizer(result[1],".");
                                tokenCount = stV.countTokens();
                    			if (tokenCount>=1) stringValue=""+stV.nextToken().trim();
                    			else stringValue = "-99999";
                    			if ((stringValue.equalsIgnoreCase("blank")) 
                    			|| (stringValue.substring(stringValue.length()-1).equalsIgnoreCase("+")))
                    				integerValue = -99999;
                    			else if (stringValue.substring(0,1).matches("\\D"))  //non-digit is interpreted as minus sign
                    				integerValue = (-1)* Integer.valueOf(stringValue.substring(1));
                    			else if (stringValue.length()>2){ 
                   					if (stringValue.substring(2,3).equalsIgnoreCase(" ")) integerValue = -99999;
                            		else integerValue = Integer.valueOf(stringValue);
                   				}
                    			else integerValue = Integer.valueOf(stringValue);
                    			if (tokenCount>=2) description=stV.nextToken().trim(); 
                    			else description = "";
                    			valueDefinition = new ValueDefinition(itemName, integerValue, stringValue, description);
                    			dataItemDefinition.putValueDefinition(stringValue, valueDefinition);
                    		}
                    		else if (result[0].trim().equalsIgnoreCase("R")){
                                StringTokenizer stR = new StringTokenizer(result[1],".");
                                tokenCount = stR.countTokens();
                    			if (tokenCount>=1){
                    				stringRangeBottom=""+stR.nextToken().trim();
                                	if (stringRangeBottom.substring(0,1).matches("\\D"))  //non-digit is interpreted as minus sign
                                		integerRangeBottom = (-1)* Integer.valueOf(stringRangeBottom.substring(1));
                                	else if (stringRangeBottom.length()>2){
                                		if (stringRangeBottom.substring(2,3).equalsIgnoreCase(" ")) integerRangeBottom = -99999;
                                		else integerRangeBottom = Integer.valueOf(stringRangeBottom);
                                	}
                                	else integerRangeBottom = Integer.valueOf(stringRangeBottom);
                    			}else{
                    				stringRangeBottom="-99999";
                    				integerRangeBottom=-99999;
                    			}
                    			if (tokenCount>=2){
                    				stringRangeTop=""+stR.nextToken().trim();
                                	if (stringRangeTop.substring(0,1).matches("\\D"))  //non-digit is interpreted as minus sign
                                		integerRangeTop = (-1)* Integer.valueOf(stringRangeTop.substring(1));
                                	else if (stringRangeTop.length()>2){
                                		if (stringRangeTop.substring(2,3).equalsIgnoreCase(" ")) integerRangeTop = -99999;
                                		else integerRangeTop = Integer.valueOf(stringRangeTop);
                                	}
                                	else integerRangeTop = Integer.valueOf(stringRangeTop);
                    			}else{
                    				stringRangeTop="-99999";
                    				integerRangeTop=-99999;
                    			}
                    			if (integerRangeBottom > integerRangeTop){
                    				int tempInt=integerRangeBottom; 
                    				integerRangeBottom=integerRangeTop; 
                    				integerRangeTop=tempInt;
                    				String tempString = stringRangeBottom;
                    				stringRangeBottom = stringRangeTop;
                    				stringRangeTop = tempString;
                    			}
                    			if (tokenCount>=3)description=stR.nextToken().trim();
                    			else description = "";
                    			rangeDefinition = new RangeDefinition(itemName,
                    					integerRangeBottom,
                    					integerRangeTop,
                    					stringRangeBottom,
                    					stringRangeTop,
                    					description);
                    			dataItemDefinition.putRangeDefinition(stringRangeBottom, rangeDefinition);
                    		}
                    	}
                    }
                }
        	}
        } catch (Exception e) {
            logger.fatal(
                "IO Exception caught reading PUMS data defs file: " +fileName+" line "+ii);
            e.printStackTrace();
        }
    }

    
    
    private void readMasterDDEntries(String fileName) {
        int ii=0;
        String itemName = null;
        String primaryClass = null;
        int length = 0;
        String type = null;
        String shortDescription = null;
        String longDescription = null;
        int integerRangeTop=0;
        String stringRangeTop=null;
        String stringValue;
        int integerValue;
        DataItemDefinition dataItemDefinition = null;
        ValueDefinition valueDefinition = null;
        RangeDefinition rangeDefinition = null;
        
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String s = new String();
            // skip the first record (header)
            s = in.readLine();ii++;
            while ((s = in.readLine()) != null) {
                ii++;
            	if (s.length() > 0) {
//                    String[] result = s.split(",",-2);
            		String[] result = s.split("\t",-2);
                    if (result.length>=9){
        				itemName=result[1].trim();
        				primaryClass=result[2].trim();
        				length=(Integer.valueOf(result[3].trim()));
        				if (result[4].trim().equalsIgnoreCase("I")) type="int";
        				else if (result[4].trim().equalsIgnoreCase("S")) type="String";
        				else {
        					type = "String";
        					logger.warn("Assumed String for undefined type code in def input line "+ ii);
        				}
        				longDescription = result[7].trim();
        				shortDescription = result[8].trim();
                    	if (result[0].trim().equalsIgnoreCase("I")) {
                    		dataItemDefinition=new DataItemDefinition(
                        			itemName,
                        			type,
                        			length,
                        			primaryClass,
                        			longDescription,
                        			shortDescription);
                        		putEntry(itemName, dataItemDefinition);
                        }
                    	else if ((result[0].trim().equalsIgnoreCase("V"))
                    			||(result[0].trim().equalsIgnoreCase("R"))){
                    		stringValue=result[5].trim();
                    		if (!type.equalsIgnoreCase("int")){
                    			integerValue = 0;
                    			integerRangeTop = 0;
                    		}
                    		else{
                    			if (stringValue.length() > 1){
                    				if (stringValue.substring(0,1).matches("\\D"))  //non-digit is interpreted as minus sign
                    					integerValue = (-1)* Integer.valueOf(stringValue.substring(1));
                    				else integerValue = Integer.valueOf(stringValue);
                    			}
                    			else integerValue = Integer.valueOf(stringValue);
                				if (result[0].trim().equalsIgnoreCase("R")){
                    				stringRangeTop=result[6].trim();
                        			if (stringRangeTop.length() > 1){
                        				if (stringRangeTop.substring(0,1).matches("\\D"))  //non-digit is interpreted as minus sign
                        					integerRangeTop = (-1)* Integer.valueOf(stringRangeTop.substring(1));
                        				else integerRangeTop = Integer.valueOf(stringRangeTop);
                        			}	
                        			else integerRangeTop = Integer.valueOf(stringRangeTop);
                       			}
                			}
                    		if (result[0].trim().equalsIgnoreCase("V")){
                    			valueDefinition = new ValueDefinition(
                    					itemName,
                    					integerValue,
                    					stringValue,
                    					longDescription,
                    					shortDescription);
                    			dataItemDefinition.putValueDefinition(stringValue, valueDefinition);
                    		}
                    		if (result[0].trim().equalsIgnoreCase("R")){
                    			rangeDefinition = new RangeDefinition(
                					itemName,
                					integerValue,
                					integerRangeTop,
                					stringValue,
                					stringRangeTop,
                					longDescription,
                					shortDescription);
                			dataItemDefinition.putRangeDefinition(stringValue, rangeDefinition);
                    		}
                    	}
                    }else logger.warn("Incomplete data definition input at line "+ ii);
            	} else logger.info("Empty data definition input line "+ ii);
            }
        } catch (Exception e) {
            logger.fatal(
                "IO Exception caught reading data defs file: " +fileName+" line "+ii);
            e.printStackTrace();
        	}
    }

    // Read all values of all data items and assign 0-based index to all values of each data item
    // This was created so that Aggregator could use the 0-based index as array index, rather than actual values
    private void setValueIndices(LinkedHashMap entries){
    	int mddSize = entries.size();
    	String [] dKeys;
//    	= new String[mddSize];
    	dKeys=(String[])entries.keySet().toArray(new String[mddSize]);
        for(int i=0; i<mddSize; i++){
        	int vDSize = ((DataItemDefinition)entries.get(dKeys[i])).valueDefinitions.size();
        	String [] vDKeys;
        	//    	= new String[vDSize];
        	//     	String [] vDKeys = new String[vDSize];
        	vDKeys=(String[])((DataItemDefinition)entries.get(dKeys[i])).valueDefinitions.keySet().toArray(new String[vDSize]);
        	//     	vDKeys=(String[])((DataItemDefinition)entries.get(dKeys[i])).valueDefinitions.keySet().toArray();
        	for(int j=0; j<vDSize; j++){
        		((ValueDefinition)((DataItemDefinition)entries.get(dKeys[i])).valueDefinitions.get(vDKeys[j])).valueIndex=j;
        	}
        }
 

    }
	/*
	 * METHODS THAT ARE USED TO RETRIEVE DATA DICTIONARY INFORMATION 
	 */
    //get number fo data values for a data item
    public int getNumberOfValues(String itemName){
    	if(entries.containsKey(itemName)) return ((DataItemDefinition)entries.get(itemName)).valueDefinitions.size();
    	else return 0;
    }
    
    public int getValueIndex(String itemName, String valueKey){
    	int returnValue = -999999;
    	if (entries.containsKey(itemName)){
    		if (((DataItemDefinition)entries.get(itemName)).valueDefinitions.containsKey(valueKey)){
    			returnValue = ((ValueDefinition)((DataItemDefinition)entries.
    					get(itemName)).valueDefinitions.get(valueKey)).valueIndex;
    		}
    	}
    	return returnValue;
    }
    
    public String getStringValueFromValueIndex(String itemName,int valueIndex){
    	String returnValue = null;
    	int vDSize = ((DataItemDefinition)entries.get(itemName)).valueDefinitions.size();
    	String [] vDKeys;
    	vDKeys=(String[])((DataItemDefinition)entries.get(itemName)).valueDefinitions.keySet().toArray(new String[vDSize]);
    	for(int j=0; j<vDSize; j++){
    		if(((ValueDefinition)((DataItemDefinition)entries.get(itemName)).valueDefinitions.get(vDKeys[j])).valueIndex==valueIndex){
    			returnValue=vDKeys[j];
    		}
    	}
    	return returnValue;
    }
 
    
    
    
	//for testing purpose only
    /*
  public static void main(String [] args){
    	MasterDataDictionary mdd;
    	String key;
    	String key2;
    	String key3;
    	String itemName;
        PropertyParser propertyParser = new PropertyParser(args[0]);  
        mdd=new MasterDataDictionary("c:/PopSyn/PUMS2000Dict.txt", "c:/PopSyn/DataDefinitionInput.txt");
    	logger.info("Number of Master Data Dictionary entries: "+mdd.entries.size());

    	for(Iterator mddIterator = mdd.entries.keySet().iterator();mddIterator.hasNext();){
        	key=(String)mddIterator.next();
        	DataItemDefinition itemDef=(DataItemDefinition)mdd.entries.get(key);
        	logger.info("");
        	logger.info("itemKey=          "+ key);
        	logger.info("itemName=         "+ itemDef.itemName);
        	logger.info("longDescription=  "+ itemDef.longDescription);
        	logger.info("shortDescription= "+ itemDef.shortDescription);
        	logger.info("type=             "+ itemDef.type);
        	logger.info("length=           "+ itemDef.length);
        	logger.info("primaryClass=     "+ itemDef.primaryClass);
        	
        	for(Iterator vdIterator = itemDef.valueDefinitions.keySet().iterator();vdIterator.hasNext();){
            	key2=(String)vdIterator.next();
            	ValueDefinition vD=(ValueDefinition)itemDef.valueDefinitions.get(key2);
        			logger.info("  valueKey=        "+ key2);
           			logger.info("  itemName=        "+ vD.itemName);
        			logger.info("  integerValue=    "+ vD.integerValue);
        			logger.info("  stringValue=     "+ vD.stringValue);
        			logger.info("  longDescription= "+ vD.longDescription);
        			logger.info("  shortDescription="+ vD.shortDescription);
        			logger.info("  valueIndex=      "+ vD.valueIndex);
         		}
        		
            for(Iterator rdIterator = itemDef.rangeDefinitions.keySet().iterator();rdIterator.hasNext();){
               	key3=(String)rdIterator.next();
           		RangeDefinition rD=(RangeDefinition)itemDef.rangeDefinitions.get(key3);
           		logger.info("   rangeKey=          "+ key3);
           		logger.info("   itemName=          "+ rD.itemName);
          		logger.info("   integerRangeBottom="+ rD.integerRangeBottom);
           		logger.info("   integerRangeTop=   "+ rD.integerRangeTop);
          		logger.info("   stringRangeBottom= "+ rD.stringRangeBottom);
           		logger.info("   stringRangeTop=    "+ rD.stringRangeTop);
           		logger.info("   longDescription=   "+ rD.longDescription);
           		logger.info("   shortDescription=  "+ rD.shortDescription);
           	}
        }
      	logger.info("");
      	logger.info("ok, I am done.");
    }
    */
}

