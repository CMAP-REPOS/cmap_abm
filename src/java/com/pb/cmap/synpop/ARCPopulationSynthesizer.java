/*
 * Copyright  2007 PB Americas, Inc.
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

import java.util.ResourceBundle;
import java.util.Vector;

import com.pb.common.util.ResourceUtil;
import com.pb.models.synpopV3.PopulationSynthesizer;

/**
 * Calls the synpopV3 model for use with the ARC project. Also calls John Bowman's Aggregator
 * class to produce trip-based model input.
 * 
 * @author D. Ory
 *
 */

public class ARCPopulationSynthesizer extends PopulationSynthesizer {

    public ARCPopulationSynthesizer() {
        super("cmap", new DerivedHHARCFactory());
    } 
    
    public ARCPopulationSynthesizer(String propertiesRoot){
    	super(propertiesRoot, new DerivedHHARCFactory());
    }

//    public void createDerivedHHFactory(){
//    	hhFactory = new DerivedHHARCFactory();
//    }
    
    public static void main(String[] args) {
        
        // use the MTC-version of the resource bundle, and inherited derived household class
    	Vector[][] synPop;
        PopulationSynthesizer arcPopulationSynthesizer = new ARCPopulationSynthesizer(); 
        synPop = arcPopulationSynthesizer.runPopulationSynthesizer(); 
        
        // run the aggregator if requested
        ResourceBundle resourceBundle = ResourceUtil.getResourceBundle("cmap");
        boolean runAggregator = ResourceUtil.getBooleanProperty(resourceBundle, "RunAggregator");
        if(runAggregator){
        	
        	// user feedback
        	logger.info("Running Aggregator post process...");
        	
        	// create Bowman's master data dictionary object
        	String pumsDirectory   = ResourceUtil.getProperty(resourceBundle, "pums.directory");
        	String pumsDictionary  = ResourceUtil.getProperty(resourceBundle, "pums.dictionary");
        	pumsDictionary = pumsDirectory+pumsDictionary;
        	
        	String masterDictonaryFile = ResourceUtil.getProperty(resourceBundle, "MasterDataDictionaryInputFile");
        	
        	MasterDataDictionary masterDataDictionary = new MasterDataDictionary(pumsDictionary,masterDictonaryFile);
        	logger.info("Number of Master Data Dictionary entries: "+masterDataDictionary.entries.size());

        	// create Bowman's aggregator to read from control file and set up aggregationSpecs
        	Aggregator aggregator=new Aggregator(masterDataDictionary);
        	int n = aggregator.getNumberOfAggregations();
        	
        	// loop to produce aggregations
        	for(int i=1; i<=n; i++){
            	 aggregator.aggregate(synPop, aggregator.getAggregationSpec(i));
            	 //note:  aggregate produces output and returns aggregation object, but the object is not here used.
            }

        }
        
        logger.info(" ");
        logger.info("End: ARC Population Synthesizer");
        

    }

}
