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

import java.io.Serializable;

import com.pb.models.ctrampIf.AtWorkSubtourFrequencyDMU;
import com.pb.models.ctrampIf.AutoOwnershipChoiceDMU;
import com.pb.models.ctrampIf.CoordinatedDailyActivityPatternDMU;
import com.pb.models.ctrampIf.CtrampDmuFactoryIf;
import com.pb.models.ctrampIf.DcSoaDMU;
import com.pb.models.ctrampIf.DestChoiceDMU;
import com.pb.models.ctrampIf.FreeParkingChoiceDMU;
import com.pb.models.ctrampIf.IndividualMandatoryTourFrequencyDMU;
import com.pb.models.ctrampIf.IndividualNonMandatoryTourFrequencyDMU;
import com.pb.models.ctrampIf.JointTourFrequencyDMU;
import com.pb.models.ctrampIf.TourModeChoiceDMU;
import com.pb.models.ctrampIf.ParkingChoiceDMU;
import com.pb.models.ctrampIf.StopDCSoaDMU;
import com.pb.models.ctrampIf.StopFrequencyDMU;
import com.pb.models.ctrampIf.StopLocationDMU;
import com.pb.models.ctrampIf.TazDataIf;
import com.pb.models.ctrampIf.TourDepartureTimeAndDurationDMU;
import com.pb.models.ctrampIf.TripModeChoiceDMU;
import com.pb.cmap.tourBased.CmapModelStructure;

/**
 * ArcCtrampDmuFactory is a class that ...
 *
 * @author Kimberly Grommes
 * @version 1.0, Jul 17, 2008
 * Created by IntelliJ IDEA.
 */
public class CmapCtrampDmuFactory implements CtrampDmuFactoryIf, Serializable {

    TazDataIf tazDataHandler;
    CmapModelStructure modelStructure;

    public CmapCtrampDmuFactory( TazDataIf tazDataHandler, CmapModelStructure modelStructure ) {
        this.tazDataHandler = tazDataHandler;
        this.modelStructure = modelStructure;
    }



    public AutoOwnershipChoiceDMU getAutoOwnershipDMU() {
        return new CmapAutoOwnershipChoiceDMU();
    }


    public FreeParkingChoiceDMU getFreeParkingChoiceDMU() {
        return new CmapFreeParkingChoiceDMU();
    }


    public CoordinatedDailyActivityPatternDMU getCoordinatedDailyActivityPatternDMU() {
        return new CmapCoordinatedDailyActivityPatternDMU();
    }


    public DcSoaDMU getDcSoaDMU() {
        return new CmapDcSoaDMU( tazDataHandler );
    }


    public DestChoiceDMU getDestChoiceDMU() {
        return new CmapDestChoiceDMU( tazDataHandler, modelStructure );
    }


    public TourModeChoiceDMU getModeChoiceDMU() {
        return new CmapTourModeChoiceDMU( tazDataHandler, modelStructure );
    }


    public IndividualMandatoryTourFrequencyDMU getIndividualMandatoryTourFrequencyDMU() {
        return new CmapIndividualMandatoryTourFrequencyDMU(tazDataHandler);
    }

    
    public TourDepartureTimeAndDurationDMU getTourDepartureTimeAndDurationDMU() {
        return new CmapTourDepartureTimeAndDurationDMU( tazDataHandler, modelStructure );
    }

    
    public AtWorkSubtourFrequencyDMU getAtWorkSubtourFrequencyDMU() {
        return new CmapAtWorkSubtourFrequencyDMU( modelStructure );
    }

    
    public JointTourFrequencyDMU getJointTourFrequencyDMU() {
        return new CmapJointTourFrequencyDMU( modelStructure );
    }

    
    public IndividualNonMandatoryTourFrequencyDMU getIndividualNonMandatoryTourFrequencyDMU() {
        return new CmapIndividualNonMandatoryTourFrequencyDMU();
    }

    
    public StopFrequencyDMU getStopFrequencyDMU() {
        return new CmapStopFrequencyDMU(tazDataHandler, modelStructure );
    }

    
    public StopDCSoaDMU getStopDCSoaDMU() {
        return new CmapStopDCSoaDMU(tazDataHandler, modelStructure);
    }
    
    
    public StopLocationDMU getStopLocationDMU() {
        return new CmapStopLocationDMU(tazDataHandler, modelStructure);
    }

    
    public TripModeChoiceDMU getTripModeChoiceDMU() {
        return new CmapTripModeChoiceDMU( tazDataHandler, modelStructure );
    }

    public ParkingChoiceDMU getParkingChoiceDMU() {
        return new CmapParkingChoiceDMU( tazDataHandler, modelStructure );
    }

}
