package com.pb.models.ctrampIf;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.models.ctrampIf.jppf.*;
import com.pb.common.newmodel.ChoiceModelApplication;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author crf
 *         Started Nov 4, 2010 1:55:52 PM
 */
public class TravelTimeModel {
    private static final int TT_DATA_SHEET = 0;
    private static final int TT_PEAK_SHEET = 1;
    private static final int TT_OFFPEAK_SHEET = 2;
    private static final String PROPERTIES_UEC_TRAVEL_TIME_MODEL = "UecFile.TravelTime";

    private static final String[] timePeriods = {"peak","offpeak"};
    private static final int START_AM_PEAK = 6;
    private static final int END_AM_PEAK = 9;
    private static final int START_PM_PEAK = 15;
    private static final int END_PM_PEAK = 18;
    
    private final ChoiceModelApplication peakModel;
    private final ChoiceModelApplication offPeakModel;
    private final TravelTimeDMU modelDmu;
    private final IndexValues indices;
    private final Map<String,Integer> peakModeToAlternativeMap;
    private final Map<String,Integer> offPeakModeToAlternativeMap;

    public TravelTimeModel(HashMap<String, String> propertyMap) {
        String mcUecFile = propertyMap.get(com.pb.models.ctrampIf.jppf.CtrampApplication.PROPERTIES_PROJECT_DIRECTORY) +
                           propertyMap.get(PROPERTIES_UEC_TRAVEL_TIME_MODEL);

        modelDmu = new TravelTimeDMU();
        indices = new IndexValues();
        peakModel = new ChoiceModelApplication(mcUecFile,TT_PEAK_SHEET,TT_DATA_SHEET,propertyMap,modelDmu);
        offPeakModel = new ChoiceModelApplication(mcUecFile,TT_OFFPEAK_SHEET,TT_DATA_SHEET,propertyMap,modelDmu);
        peakModeToAlternativeMap = new HashMap<String,Integer>();
        String[] alts = peakModel.getAlternativeNames();
        for (int i = 0; i < alts.length; i++)
            peakModeToAlternativeMap.put(alts[i].toLowerCase(),i);
        offPeakModeToAlternativeMap = new HashMap<String,Integer>();
        alts = offPeakModel.getAlternativeNames();
        for (int i = 0; i < alts.length; i++)
            offPeakModeToAlternativeMap.put(alts[i].toLowerCase(),i);
    }
   
    private boolean tripStartsInPeakPeriod(int startHour) {
        return ((startHour >= START_AM_PEAK && startHour <= END_AM_PEAK) ||
                (startHour >= START_PM_PEAK && startHour <= END_PM_PEAK));
    }

    private double getTravelTime(int origin, int dest, Boolean peak, String alt) {
        indices.setOriginZone(origin);
        indices.setDestZone(dest);
        ChoiceModelApplication app = peak ? peakModel : offPeakModel;
        app.computeUtilities(modelDmu,indices);
        return app.getUtilities()[(peak ? peakModeToAlternativeMap : offPeakModeToAlternativeMap).get(alt)];

    }

    public String[] getAlternatives(String period) {
        return (period == timePeriods[0]) ? peakModel.getAlternativeNames() : offPeakModel.getAlternativeNames();
    }
    
    public String[] getTimePeriods() {
        return timePeriods;
    }
    public double[] getTravelTimes(String period, int origin, int dest) {
        indices.setOriginZone(origin);
        indices.setDestZone(dest);
        ChoiceModelApplication app = (period == timePeriods[0]) ? peakModel : offPeakModel;
        app.computeUtilities(modelDmu,indices);
        return app.getUtilities();
    }

    public double getTravelTime(String modeName, int startHour, int origin, int dest) {
        return getTravelTime(origin,dest,tripStartsInPeakPeriod(startHour),modeName.toLowerCase());
    }

    private class TravelTimeDMU implements VariableTable {

        public int getIndexValue(String variableName) {
            throw new UnsupportedOperationException();
        }

        public int getAssignmentIndexValue(String variableName) {
            throw new UnsupportedOperationException();
        }

        public double getValueForIndex(int variableIndex) {
            throw new UnsupportedOperationException();
        }

        public double getValueForIndex(int variableIndex, int arrayIndex) {
            throw new UnsupportedOperationException();
        }

        public void setValue(String variableName, double variableValue) {
            throw new UnsupportedOperationException();
        }

        public void setValue(int variableIndex, double variableValue) {
            throw new UnsupportedOperationException();
        }
    }

}
