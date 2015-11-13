package com.pb.models.ctrampIf;

import org.apache.log4j.Logger;

public interface TourIf
{

    public abstract PersonIf getPersonObject();

    public abstract void setPersonObject(PersonIf p);

    public abstract void setPersonNumArray(byte[] personNums);

    public abstract byte[] getPersonNumArray();

    public abstract boolean getPersonInJointTour(PersonIf person);

    public abstract void setJointTourComposition(int compositionAlternative);

    public abstract int getJointTourComposition();

    public abstract void setTourPurpose(String name);

    public abstract String getTourPurpose();
    
    public abstract String getTourCategory();

    public abstract byte getTourCategoryIndex();

    public abstract String getTourPrimaryPurpose();

    public abstract int getTourPrimaryPurposeIndex();

    public abstract boolean getTourCategoryIsMandatory();

    public abstract boolean getTourCategoryIsAtWork();

    public abstract boolean getTourCategoryIsJointNonMandatory();

    public abstract boolean getTourCategoryIsIndivNonMandatory();

    public abstract int getTourModeChoice();

    public String getSubTourPurpose();

    public void setSubTourPurpose(String name);
    
    
    //TODO: probably need to add a setter method so the project specific code can
    // set the mode alternatives that are SOV, rather than have them hard-coded as they are.
    public abstract void setTourId(int id);

    public abstract void setTourOrigTaz(int origTaz);

    public abstract void setTourDestTaz(int destTaz);
    
    public abstract void setTourBTapIn(int tap);

    public abstract void setTourATapIn(int tap);
    
    public abstract void setTourBTapOut(int tap);

    public abstract void setTourATapOut(int tap);

    public abstract void setTourOrigWalkSubzone(int subzone);

    public abstract void setTourDestWalkSubzone(int subzone);

    public abstract void setTourDepartPeriod( int departInterval);
    
    public abstract void setTourArrivePeriod( int arriveInterval);
    
    public abstract void setTourModeChoice(int modeIndex);

    public abstract void setTourParkTaz(int parkTaz);

    public abstract int getTourOrigTaz();

    public abstract int getTourDestTaz();
    
    public abstract int getTourBTapIn();

    public abstract int getTourATapIn();

    public abstract int getTourBTapOut();

    public abstract int getTourATapOut();

    public abstract int getTourOrigWalkSubzone();

    public abstract int getTourDestWalkSubzone();

    public abstract int getTourDepartPeriod();

    public abstract int getTourArrivePeriod();

    public abstract int getTourParkTaz();

    public abstract int getHhId();

    public abstract int getHhTaz();

    public abstract int getTourId();

    public int getWorkTourIndexFromSubtourId( int subtourId );

    public int getSubtourIdFromIndices(int workTourIndex, int subtourIndex );

    public abstract void setSubtourFreqChoice(int choice);

    public abstract int getSubtourFreqChoice();

    public abstract void setStopFreqChoice(int chosenAlt);

    public abstract int getStopFreqChoice();

    public abstract void createOutboundStops( String[] stopOrigPurposes, String[] stopDestPurposes, int[] stopPurposeIndex );

    public abstract void createInboundStops( String[] stopOrigPurposes, String[] stopDestPurposes, int[] stopPurposeIndex );

    /**
     * Create a Stop object to represent a half-tour where no stops were generated.  The id for the stop is set
     * to -1 so that trips for half-tours without stops can be distinguished in the output trip files from
     * turs that have stops.  Trips for these tours come from stop objects with ids in the range 0,...,3.
     * 
     * @param origPurp is "home" or "work" (for at-work subtours) if outbound, or the primary tour purpose if inbound
     * @param destPurp is "home" or "work" (for at-work subtours) if inbound, or the primary tour purpose if outbound
     * @param inbound is true if the half-tour is inbound, or false if outbound.
     * @return the created Stop object.
     */
    public abstract StopIf createStop(ModelStructure modelStructure, String origPurp,
            String destPurp, boolean inbound, boolean subtour);

    public abstract int getNumOutboundStops();

    public abstract int getNumInboundStops();

    public abstract StopIf[] getOutboundStops();

    public abstract StopIf[] getInboundStops();

    public abstract void clearStopModelResults();

    public abstract String getTourWindow(String purposeAbbreviation);

    public abstract void logTourObject(Logger logger, int totalChars);

    public abstract void logEntireTourObject(Logger logger);

    public abstract void setTourModalUtilities(float[] utils);

    public abstract float[] getTourModalUtilities();

    public abstract void setTourModalProbabilities(float[] probs);

    public abstract float[] getTourModalProbabilities();

}