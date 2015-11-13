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

import java.util.Vector;

import com.pb.models.synpopV3.DataDictionary;
import com.pb.models.synpopV3.DerivedHHFactory;

/**
 * A class that creates DerivedHH objects
 * 
 * @author D. Ory (same as Erhardt DerivedHHSFFactory)
 * @version 1.0 Oct 19, 2007
 *
 */
public class DerivedHHARCFactory extends DerivedHHFactory {

    
    public DerivedHHARCFactory() {
        super(); 
    }
    
    public DerivedHHARC createDerivedHH(String record_raw, Vector pRecords_raw, DataDictionary dd) {
        DerivedHHARC hh = new DerivedHHARC(record_raw, pRecords_raw, dd); 
        return hh; 
    }
    
}
