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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import static java.lang.Math.*;

import com.pb.common.matrix.NDimensionalMatrix;
import com.pb.models.synpopV3.DrawnHH;
import com.pb.models.synpopV3.PropertyParser;


/**
 * @author John L Bowman
 * <John_L_Bowman@alum.mit.edu>
 * Created on Mar 5, 2007
 * 
 * --------------------------------------------------------------------------------------------
 *
 * JLB 20070305   This program aggregates
 *                  --one or more classes specified in properties file
 *                  --in one or more categorizations specified in properties
 *                    file                 
 *                
*/

public class Aggregator {
	protected boolean logDebug=false;
	protected static Logger logger = Logger.getLogger("com.pb.arc.synpop");

    protected LinkedHashMap aggregationSpecs;
	protected int numberOfAggregations; 
    protected MasterDataDictionary mdd;
    protected String outputDirectory;

	public Aggregator(MasterDataDictionary mdd){
    	this.mdd=mdd;
		Vector dimensionVariables;
		AggregationSpec aggregationSpec;
		
		// Read aggregator parameters from properties file
		outputDirectory = PropertyParser.getPropertyByName("Aggregator.OutputDirectory");
		numberOfAggregations = new Integer(PropertyParser.getPropertyByName("Aggregator.NumberOfAggregations")).intValue();
		aggregationSpecs = new LinkedHashMap();
		
        // Loop to read parameters of each aggregation specified in properties file
        for(int i=1; i<=numberOfAggregations; i++){
        	String si = String.valueOf(i);  

        	aggregationSpec = new AggregationSpec();
        	aggregationSpec.aggregationNumber = i;
    		aggregationSpec.aggregationParameters.put("Item", "DerivedHH");
    		aggregationSpec.aggregationParameters.put("Statistic", "Count");
    		aggregationSpec.aggregationParameters.put("Geography", "TAZ");
    		aggregationSpec.aggregationParameters.put("Outfile", "Aggregation1.csv");
    		aggregationSpec.aggregationParameters.put("OutfileType", "ascii");
    		aggregationSpec.aggregationParameters.put("OutfileDelimiter", "Comma");
    		aggregationSpec.aggregationParameters.put("IncludeOutfileHeader", "true");
    		aggregationSpec.aggregationParameters.put("CategoryLabelType", "Descriptions");
    		aggregationSpec.dimensionVarnames.put("1","hsizecat6");
    		aggregationSpec.dimensionVarnames.put("2","hinccat1");
    		aggregationSpec.dimensionVarnames.put("3","hwrkcat5");
    		aggregationSpec.dimensionVarnames.put("4","hNCcat6");
    		aggregationSpec.dimensionSizes.put("1", "6");
    		aggregationSpec.dimensionSizes.put("2", "4");
    		aggregationSpec.dimensionSizes.put("3", "5");
    		aggregationSpec.dimensionSizes.put("4", "6");
        	
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".Item").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"Item",
        				PropertyParser.getPropertyByName("Aggregator."+si+".Item").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".Statistic").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"Statistic",
        				PropertyParser.getPropertyByName("Aggregator."+si+".Statistic").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".Geography").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"Geography",
        				PropertyParser.getPropertyByName("Aggregator."+si+".Geography").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".Outfile").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"Outfile",
        				PropertyParser.getPropertyByName("Aggregator."+si+".Outfile").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".OutfileType").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"OutfileType",
        				PropertyParser.getPropertyByName("Aggregator."+si+".OutfileType").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".OutfileDelimiter").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"OutfileDelimiter",
        				PropertyParser.getPropertyByName("Aggregator."+si+".OutfileDelimiter").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".IncludeOutfileHeader").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"IncludeOutfileHeader",
        				PropertyParser.getPropertyByName("Aggregator."+si+".IncludeOutfileHeader").trim());
        	if(!PropertyParser.getPropertyByName("Aggregator."+si+".CategoryLabelType").trim().equals(null))
        		aggregationSpec.aggregationParameters.put(
        				"CategoryLabelType",
        				PropertyParser.getPropertyByName("Aggregator."+si+".CategoryLabelType").trim());

    		dimensionVariables=PropertyParser.getPropertyElementsByName("Aggregator."+si+".DimensionVariables",",");
            if (!dimensionVariables.isEmpty()) {
            	aggregationSpec.dimensionVarnames.clear();
            	aggregationSpec.dimensionSizes.clear();
            	for(int j=1; j<=dimensionVariables.size(); j++){
            	String sj = String.valueOf(j);
            	String dimensionVariable = (String)dimensionVariables.get(j-1);
            	aggregationSpec.dimensionVarnames.put(
            			sj,
            			dimensionVariable);
            	aggregationSpec.dimensionSizes.put(
            			sj,
            			String.valueOf(mdd.getNumberOfValues(dimensionVariable)));
            	}
            aggregationSpecs.put(i,aggregationSpec);	
            }
        }
	}
	public int getNumberOfAggregations() {
		return numberOfAggregations;
	}

	public AggregationSpec getAggregationSpec(int i) {
		return (AggregationSpec)aggregationSpecs.get(i);
	}
	
    private class AggregationSpec {
    	protected int aggregationNumber;
    	protected LinkedHashMap aggregationParameters; // <string,string>
    	protected LinkedHashMap dimensionSizes; // <string,string?>
    	protected LinkedHashMap dimensionVarnames; // <string,string?>
    	
    	/*
    	 * Constructor (empty)
    	 */
    	public AggregationSpec() {
            aggregationParameters=new LinkedHashMap();
            dimensionSizes=new LinkedHashMap();
            dimensionVarnames=new LinkedHashMap();
            aggregationNumber=0;
    	}
    	public int getNumberOfDimensions(){
    		return dimensionSizes.size();
    	}
    	public int getDimensionSize(int dimension){
    		return Integer.valueOf((String)dimensionSizes.get(String.valueOf(dimension)));
    	}
    	public String getDimensionVarname(int dimension){
    		return (String)dimensionVarnames.get(String.valueOf(dimension));
    	}
    	public String getAggregationParameter(String key){
    		return (String)aggregationParameters.get(key);
    	}
    	public int getAggregationNumber(){
    		return (int)aggregationNumber;
    	}
    }

    public NDimensionalMatrix aggregate(Vector [][] PopSyn, AggregationSpec aS){
    	NDimensionalMatrix aggregation;
    	/*Note: should capture and log exceptions.  Simple handling would be adequate, 
    	  such as skipping to next aggregation after reporting exception 
    	*/
    	
    	// initialize aggregation matrix and work variables, using info from aS and mdd.  
    	int length = aS.getNumberOfDimensions();
    	int[] shape = new int[length];
    	int maxshape = 0;
    	int[] position = new int[length];
        String[] varname = new String[length];
        for(int i=1; i<=length; i++){
        	shape[i-1] = aS.getDimensionSize(i);
        	if(shape[i-1] > maxshape) maxshape = shape[i-1];
        	position[i-1] = 0;
        	varname[i-1] = aS.getDimensionVarname(i);
        }
        String[][] varIndex = new String[length][maxshape];
        for(int i=0; i<length; i++){
            for(int j=0; j<shape[i]; j++){
            	varIndex[i][j] = mdd.getStringValueFromValueIndex(varname[i],j);
            }
        }
        

        
    	aggregation = new NDimensionalMatrix("name",length,shape);
     	aggregation.setMatrix(0);

     	// loop through both dimensions of PopSyn, and increment aggregation for each HH or person.
//     	Vector[][] popSyn = PopSyn;
        int NoHHCat=PopSyn.length;
        int NoTAZ=PopSyn[0].length;
		DrawnHH hh=null;
		/* need to generalize to use aggregation Item to determine level of aggregation (HH, person, 
		person scenario Day, person scenario tour, etc) and then aggregate at that level.
		The following code assumes HH level.
		*/ 
        double runningTime0 = System.currentTimeMillis();
        String TAZExt="";
        String itemValue="";
        Float oldvalue;
        try {
			for(int i=0; i<NoHHCat; i++){
				for(int j=0; j<NoTAZ; j++){
					TAZExt=mdd.getStringValueFromValueIndex("taz",j);
	                //number of HHs in current PopSyn cell
					int NoHHs=PopSyn[i][j].size();
					for(int k=0; k<NoHHs; k++){
	                    //get a HH
						hh=(DrawnHH)PopSyn[i][j].get(k);
				        for(int l=0; l<length; l++){
				            if(varname[l].equalsIgnoreCase("taz")) itemValue=TAZExt;
				            else {itemValue=""+hh.getHHAttr(varname[l]);}
				            position[l]=mdd.getValueIndex(varname[l], itemValue);
//				        	if(logDebug) logger.info("varname     = "+varname);
//				        	if(logDebug) logger.info(" shape["+ l +"]    = "+shape[l]);
//				        	if(logDebug) logger.info(" position["+ l +"] = "+position[l]);
				        }	
				        // increment the cell of aggregation corresponding to this HH
				        // need to generalize to use the Statistic parameter insead of assuming Count
				        oldvalue = aggregation.getValue(position);
				        aggregation.setValue(oldvalue+1,position);
				        
					}
				}
			}
	        double runningTime1 = System.currentTimeMillis() - runningTime0;
	        logger.info ("Aggregating took = " + (float)((runningTime1/1000.0)/60.0)+" minute.");

		
			// loop through aggregation and write a line for each cell
			/* need to generalize to write to print or file, and to verious file types and delimiters.
			The following code assumes write to ascii comma delimited, with category numbers within each dimension
			*/ 
	        PrintWriter stream = null;
	        runningTime0 = System.currentTimeMillis();
	        try {
	            
	            // open output streams for writing aggregation
	            stream = new PrintWriter( new BufferedWriter( new FileWriter(outputDirectory
	            		+aS.getAggregationParameter("Outfile")) ) );
	
	            // write file header record
	            if(((String)aS.aggregationParameters.get("IncludeOutfileHeader")).equalsIgnoreCase("true")){
	            	printHeader(aS, aggregation,stream,position,length);
	            }
	
	            // construct the aggregation records and write them to the file 
	            // cycle through all cells of matrix
	        	if (length>7) logger.warn ("Aggregation dimensions exceed limit of 7; no output written for aggregation "+aS.getAggregationNumber());
	        	else{
	        		for(int i1=0; i1<shape[0]; i1++){
	        			position[0]=i1;
	        			if (length==1) printRow(varIndex,aggregation,stream,position,length);	
	        			else if (length>1){
	        			for(int i2=0; i2<shape[1];i2++){
	        				position[1]=i2;	
	        				if (length==2) printRow(varIndex,aggregation,stream,position,length);	
	                		else if (length>2){
	                		for(int i3=0; i3<shape[2];i3++){
	                			position[2]=i3;	
	                    		if (length==3) printRow(varIndex,aggregation,stream,position,length);	
	                    		else if (length>3){
	                    		for(int i4=0; i4<shape[3];i4++){
	                    			position[3]=i4;	
	                        		if (length==4) printRow(varIndex,aggregation,stream,position,length);	
	                        		else if (length>4){
	                        		for(int i5=0; i5<shape[4];i5++){
	                        			position[4]=i5;	
	                            		if (length==5) printRow(varIndex,aggregation,stream,position,length);	
	                            		else if (length>5){
	                            		for(int i6=0; i6<shape[5];i6++){
	                            			position[5]=i6;	
	                                		if (length==6) printRow(varIndex,aggregation,stream,position,length);	
	                                		else if (length>6){
	                                		for(int i7=0; i7<shape[6];i7++){
	                                			position[6]=i7;	
	                                    		if (length==7) printRow(varIndex,aggregation,stream,position,length);
	                                		}
	                                		}
	                            		}
	                            		}
	                        		}
	                        		}
	                    		}
	                    		}
	                		}
	                		}
	        			}
	        			}
	        		}
	        	}	
	
	        }
	        catch (IOException e) {
	            logger.warn ("I/O exception caught writing aggregation output file for aggregation "+aS.getAggregationNumber(),e);
	        }
	 
	        finally {
	        	stream.close();
	        }
	        runningTime1 = System.currentTimeMillis() - runningTime0;
	        logger.info ("Writing aggregation took = " + (float)((runningTime1/1000.0)/60.0)+" minute.");

		}
        catch (Exception e) {
            logger.warn ("Exception caught attempting aggregation "+aS.getAggregationNumber(),e);
        }
       	return aggregation;

	}
    public void printRow(String[][] varIndex, NDimensionalMatrix aggregation, PrintWriter stream, int[] position, int length){
//        for(int i=0; i<aggregation.getNumberOfElements()-1; i++){
		if (aggregation.getValue(position)>0){
            //cycle through all dimensions printing index, then print aggregation value
			for(int k=0; k<length; k++){
				if (k==0){
					stream.print(varIndex[k][position[k]]);
//					stream.print(mdd.getStringValueFromValueIndex(varname[k],position[k]));					
				}
				else{
					stream.print(","+ varIndex[k][position[k]]);
//					stream.print(","+ mdd.getStringValueFromValueIndex(varname[k],position[k]));
				}
			}	
			stream.println(","+round(aggregation.getValue(position)));
		}	
//		}
	}

    public void printHeader(AggregationSpec aS, NDimensionalMatrix aggregation,PrintWriter stream,
    		int[] position, int length){
//    	for(int i=0; i<aggregation.getNumberOfElements()-1; i++){
    		
    		//cycle through all dimensions printing index, then print aggregation value
    		for(int k=0; k<length; k++){
    			if (k==0){
    				stream.print(aS.getDimensionVarname(k+1));
    			}
    			else{
    				stream.print(","+ aS.getDimensionVarname(k+1));
    			}
    		}	
    		stream.println(","+"AggrValue");
//        }
    }
    
    
	//for testing purpose only
    /*
	public static void main(String [] args){
		
		MasterDataDictionary mdd;
		Aggregator aggregator;
		String key;
		String key2;
		PropertyParser propertyParser = new PropertyParser(args[0]);  
		mdd=new MasterDataDictionary("c:/PopSyn/PUMS2000Dict.txt", "c:/PopSyn/DataDefinitionInput.txt");
		logger.info("Number of Master Data Dictionary entries: "+mdd.entries.size());
		aggregator = new Aggregator(mdd);
		
    	for(Iterator aggIterator = aggregator.aggregationSpecs.keySet().iterator();aggIterator.hasNext();){
        	key=(String)aggIterator.next();
        	AggregationSpec aggr=(AggregationSpec)aggregator.aggregationSpecs.get(key);
        	logger.info("");
        	logger.info("aggregation number=  "+ key);
        	logger.info("Item=                "+ aggr.aggregationParameters.get("Item"));
        	logger.info("Statistic=           "+ aggr.aggregationParameters.get("Statistic"));
        	logger.info("Geography=           "+ aggr.aggregationParameters.get("Geography"));
        	logger.info("Outfile=             "+ aggr.aggregationParameters.get("Outfile"));
        	logger.info("OutfileType=         "+ aggr.aggregationParameters.get("OutfileType"));
        	logger.info("OutfileDelimiter=    "+ aggr.aggregationParameters.get("OutfileDelimiter"));
        	logger.info("IncludeOutfileHeader="+ aggr.aggregationParameters.get("IncludeOutfileHeader"));
        	logger.info("CategoryLabelType=   "+ aggr.aggregationParameters.get("CategoryLabelType"));
        	
        	for(Iterator dimIterator = aggr.dimensionSizes.keySet().iterator();dimIterator.hasNext();){
            	key2=(String)dimIterator.next();
        			logger.info("  dimensionVariable= "+ key2 +" numberOfCategories= "+ aggr.dimensionSizes.get(key2));
         		}
        }
      	logger.info("");
      	logger.info("ok, I am done.");
	
	}
	*/

}
