package com.pb.cmap.tourBased;

import java.util.HashMap;

import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.AtWorkSubtourFrequencyDMU;
import com.pb.models.ctrampIf.ModelStructure;

public class CmapAtWorkSubtourFrequencyDMU extends AtWorkSubtourFrequencyDMU implements VariableTable {

    
    public CmapAtWorkSubtourFrequencyDMU( ModelStructure modelStructure ){
        super( modelStructure );
        this.modelStructure = modelStructure;
        setupMethodIndexMap();
    }



    /**
     * @return household income category
     */
    public int getHhIncomeCategory() {
        return hh.getIncomeSegment();
    }



    private void setupMethodIndexMap() {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getHhIncomeCategory", 0 );
        methodIndexMap.put( "getNumAutos", 1 );
        methodIndexMap.put( "getPersonIsFullTimeWorker", 2 );
        methodIndexMap.put( "getPersonIsNonworker", 3 );
        methodIndexMap.put( "getWorkTazAreaType", 4 );
        methodIndexMap.put( "getWorkTourDuration", 5 );
        methodIndexMap.put( "getJointMaintShopEatPerson", 6 );
        methodIndexMap.put( "getJointDiscrPerson", 7 );
        methodIndexMap.put( "getIndivMaintShopEscortFt", 8 );
        methodIndexMap.put( "getIndivMaintShopEscortPt", 9 );
        methodIndexMap.put( "getIndivDiscrFt", 10 );
        methodIndexMap.put( "getIndivDiscrPt", 11 );
        methodIndexMap.put( "getIndivEatOut", 12 );
        methodIndexMap.put( "getWorkTourModeIsSOV", 13 );
        methodIndexMap.put( "getNumPersonWorkTours", 14 );
        methodIndexMap.put( "getWorkStudNonMandatoryTours", 15 );
    }
    
    
    
    public double getValueForIndex(int variableIndex, int arrayIndex) {

        switch ( variableIndex ){
            case 0: return getHhIncomeCategory();
            case 1: return getNumAutos();
            case 2: return getPersonIsFullTimeWorker();
            case 3: return getPersonIsNonworker();
            case 4: return getWorkTazAreaType();
            case 5: return getWorkTourDuration();
            case 6: return getJointMaintShopEatPerson();
            case 7: return getJointDiscrPerson();
            case 8: return getIndivMaintShopEscortFt();
            case 9: return getIndivMaintShopEscortPt();
            case 10: return getIndivDiscrFt();
            case 11: return getIndivDiscrPt();
            case 12: return getIndivEatOut();
            case 13: return getWorkTourModeIsSOV();
            case 14: return getNumPersonWorkTours();
            case 15: return getWorkStudNonMandatoryTours();

            default:
                logger.error("method number = "+variableIndex+" not found");
                throw new RuntimeException("method number = "+variableIndex+" not found");
        
        }
        
    }


}
