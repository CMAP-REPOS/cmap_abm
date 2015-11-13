package com.pb.cmap.ctramp;

import java.io.Serializable;
import org.apache.log4j.Logger;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.HouseholdIf;
import com.pb.models.ctrampIf.PersonIf;
import com.pb.models.ctrampIf.TourIf;

public class Tour implements TourIf, Serializable
{

    private Person    perObj;
    private Household hhObj;

    private byte tourCategoryIndex;

    private String    tourCategory;
    private String    tourPurpose;
    private String    subtourPurpose;

    // use this array to hold personNum (i.e. index values for Household.persons
    // array) for persons in tour.
    // for individual tour types, this array is null.
    // for joint tours, there will be an entry for each participating person.
    private byte[]     personNumArray;

    // alternative number chosen by the joint tour composition model ( 1=adults,
    // 2=children, 3=mixed ).
    private int       jointTourComposition;

    private int       tourId;
    private int       tourOrigTaz;
    private int       tourDestTaz;
    private int       tourParkTaz;
    private int       tourOrigWalkSubzone;
    private int       tourDestWalkSubzone;
    private int       tourDepartPeriod;
    private int       tourArrivePeriod;
    private int       tourMode;
    private int       subtourFreqChoice;

    private int       tourPrimaryPurposeIndex;

    private float[]   tourModalProbabilities;
    private float[]   tourModalUtilities;

    private int       stopFreqChoice;
    private Stop[]    outboundStops;
    private Stop[]    inboundStops;

    private int     btapIn;
    private int     atapIn;
    private int     btapOut;
    private int     atapOut;
    
    // this constructor used for default tour creation for work/school location choice mode choice logsum calculation
    public Tour(PersonIf perObj, int primaryIndex)
    {
        hhObj = (Household) perObj.getHouseholdObject();
        this.perObj = (Person) perObj;
        this.tourId = -1;
        tourCategory = ModelStructure.MANDATORY_CATEGORY;
        tourCategoryIndex = (byte)ModelStructure.MANDATORY_CATEGORY_INDEX;
        tourPrimaryPurposeIndex = primaryIndex;
    }

    // this constructor used for mandatory tour creation
    public Tour(PersonIf perObj, int tourId, int primaryIndex)
    {
        hhObj = (Household) perObj.getHouseholdObject();
        this.perObj = (Person) perObj;
        this.tourId = tourId;
        tourCategory = ModelStructure.MANDATORY_CATEGORY;
        tourCategoryIndex = (byte)ModelStructure.MANDATORY_CATEGORY_INDEX;
        tourPrimaryPurposeIndex = primaryIndex;
    }

    // this constructor used for joint tour creation
    public Tour(HouseholdIf hhObj, String tourPurpose, String category, int primaryIndex)
    {
        this.hhObj = (Household) hhObj;
        this.tourPurpose = tourPurpose;
        tourCategory = category;
        tourCategoryIndex = (byte)ModelStructure.JOINT_NON_MANDATORY_CATEGORY_INDEX;
        tourPrimaryPurposeIndex = primaryIndex;
    }

    // this constructor used for individual non-mandatory or at-work subtour creation
    public Tour(int id, HouseholdIf hhObj, PersonIf persObj, String tourPurpose, String category, int primaryIndex)
    {
        this.hhObj = (Household) hhObj;
        this.perObj = (Person) persObj;
        tourId = id;
        this.tourPurpose = tourPurpose;
        tourCategory = category;
        tourCategoryIndex = category.equalsIgnoreCase(ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY) ? (byte)ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY_INDEX : (byte)ModelStructure.AT_WORK_CATEGORY_INDEX;
        tourPrimaryPurposeIndex = primaryIndex;
    }

    public PersonIf getPersonObject()
    {
        return perObj;
    }

    public void setPersonObject(PersonIf p)
    {
        perObj = (Person) p;
    }

    public void setPersonNumArray(byte[] personNums)
    {
        personNumArray = personNums;
    }

    public byte[] getPersonNumArray()
    {
        return personNumArray;
    }

    public boolean getPersonInJointTour(PersonIf person)
    {
        boolean inTour = false;
        for (int num : personNumArray)
        {
            if (person.getPersonNum() == num)
            {
                inTour = true;
                break;
            }
        }
        return inTour;
    }

    public void setJointTourComposition(int compositionAlternative)
    {
        jointTourComposition = compositionAlternative;
    }

    public int getJointTourComposition()
    {
        return jointTourComposition;
    }

    public void setTourPurpose(String name)
    {
        tourPurpose = name;
    }

    public void setSubTourPurpose(String name)
    {
        subtourPurpose = name;
    }

    public String getSubTourPurpose()
    {
        return subtourPurpose;
    }

    public String getTourCategory()
    {
        return tourCategory;
    }

    public String getTourPurpose()
    {
        return tourPurpose;
    }

    public String getTourPrimaryPurpose()
    {
        int index = tourPurpose.indexOf('_');
        if (index < 0) return tourPurpose;
        else return tourPurpose.substring(0, index);
    }

    public int getTourPrimaryPurposeIndex()
    {
        return tourPrimaryPurposeIndex;
    }

    public int getTourModeChoice()
    {
        return tourMode;
    }

    public void setTourId(int id)
    {
        tourId = id;
    }

    public void setTourOrigTaz(int orig)
    {
        tourOrigTaz = orig;
    }

    public void setTourDestTaz(int dest)
    {
        tourDestTaz = dest;
    }

    public void setTourOrigWalkSubzone(int subzone)
    {
        tourOrigWalkSubzone = subzone;
    }

    public void setTourDestWalkSubzone(int subzone)
    {
        tourDestWalkSubzone = subzone;
    }

    public void setTourDepartPeriod(int departPeriod)
    {
        tourDepartPeriod = departPeriod;
    }

    public void setTourArrivePeriod(int arrivePeriod)
    {
        tourArrivePeriod = arrivePeriod;
    }

    public void setTourModeChoice(int modeIndex)
    {
        tourMode = modeIndex;
    }

    public void setTourParkTaz(int parkTaz)
    {
        tourParkTaz = parkTaz;
    }

    // methods DMU will use to get info from household object

    public int getTourOrigTaz()
    {
        return tourOrigTaz;
    }

    public int getTourDestTaz()
    {
        return tourDestTaz;
    }
    
    public void setTourBTapIn(int tap) {
    	btapIn = tap;
    }

    public void setTourATapIn(int tap) {
    	atapIn = tap;
    }

    public int getTourBTapIn() {
    	return(btapIn);
    }

    public int getTourATapIn() {
    	return(atapIn);
    }

    public void setTourBTapOut(int tap) {
    	btapOut = tap;
    }

    public void setTourATapOut(int tap) {
    	atapOut = tap;
    }

    public int getTourBTapOut() {
    	return(btapOut);
    }

    public int getTourATapOut() {
    	return(atapOut);
    }
    public int getTourOrigWalkSubzone()
    {
        return tourOrigWalkSubzone;
    }

    public int getTourDestWalkSubzone()
    {
        return tourDestWalkSubzone;
    }

    public int getTourDepartPeriod()
    {
        return tourDepartPeriod;
    }

    public int getTourArrivePeriod()
    {
        return tourArrivePeriod;
    }

    public int getTourParkTaz()
    {
        return tourParkTaz;
    }

    public int getHhId()
    {
        return hhObj.getHhId();
    }

    public int getHhTaz()
    {
        return hhObj.getHhTaz();
    }

    public int getTourId()
    {
        return tourId;
    }

    public int getSubtourIndexFromSubtourId(int subtourIndex)
    {
        // when subtour was created, it's purpose index was set to 10*work purpose
        // index + at-work subtour index
        int workTourIndex = subtourIndex / 10;
        return subtourIndex - 10 * workTourIndex;
    }

    public void setSubtourFreqChoice(int choice)
    {
        subtourFreqChoice = choice;
    }

    public int getSubtourFreqChoice()
    {
        return subtourFreqChoice;
    }

    public void setStopFreqChoice(int chosenAlt)
    {
        stopFreqChoice = chosenAlt;
    }

    public int getStopFreqChoice()
    {
        return stopFreqChoice;
    }

    public void createOutboundStops(String[] stopOrigPurposes, String[] stopDestPurposes, int[] stopPurposeIndex)
    {
        outboundStops = new Stop[stopOrigPurposes.length];
        for (int i = 0; i < stopOrigPurposes.length; i++)
            outboundStops[i] = new Stop(this, stopOrigPurposes[i], stopDestPurposes[i], i, false, stopPurposeIndex[i]);
    }

    public void createInboundStops(String[] stopOrigPurposes, String[] stopDestPurposes, int[] stopPurposeIndex)
    {
        // needs outbound stops to be created first to get id numbering correct

        inboundStops = new Stop[stopOrigPurposes.length];
        for (int i = 0; i < stopOrigPurposes.length; i++)
            inboundStops[i] = new Stop(this, stopOrigPurposes[i], stopDestPurposes[i], i, true, stopPurposeIndex[i]);
    }

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
    public Stop createStop( ModelStructure modelStructure, String origPurp, String destPurp, boolean inbound, boolean subtour ) {
        Stop stop = null;
        int id = -1;
        if ( inbound ) {
            inboundStops = new Stop[1];
            inboundStops[0] = new Stop( this, origPurp, destPurp, id, inbound, 0 );
            stop = inboundStops[0]; 
        }
        else {
            outboundStops = new Stop[1];
            outboundStops[0] = new Stop( this, origPurp, destPurp, id, inbound, 0 );
            stop = outboundStops[0]; 
        }
        return stop;
    }
    
    public int getNumOutboundStops()
    {
        if (outboundStops == null) return 0;
        else return outboundStops.length;
    }

    public int getNumInboundStops()
    {
        if (inboundStops == null) return 0;
        else return inboundStops.length;
    }

    public Stop[] getOutboundStops()
    {
        return outboundStops;
    }

    public Stop[] getInboundStops()
    {
        return inboundStops;
    }

    public void clearStopModelResults()
    {
        stopFreqChoice = 0;
        outboundStops = null;
        inboundStops = null;
    }

    public String getTourWindow( String purposeAbbreviation ) {
        String returnString = String.format("      %5s:     |", purposeAbbreviation );
        byte[] windows = perObj.getTimeWindows();
        for ( int i=1; i < windows.length; i++ ) {
            String tempString = String.format("%s", i >= tourDepartPeriod && i <= tourArrivePeriod ? purposeAbbreviation : "    " );
            if ( tempString.length() == 2 || tempString.length() == 3 )
                tempString = " " + tempString;
            returnString += String.format("%4s|", tempString );
        }
        return returnString;
    }

    public void logTourObject(Logger logger, int totalChars)
    {

        String personNumArrayString = "-";
        if (personNumArray != null)
        {
            personNumArrayString = "[ ";
            personNumArrayString += String.format("%d", personNumArray[0]);
            for (int i = 1; i < personNumArray.length; i++)
                personNumArrayString += String.format(", %d", personNumArray[i]);
            personNumArrayString += " ]";
        }

        Household.logHelper(logger, "tourId: ", tourId, totalChars);
        Household.logHelper(logger, "tourCategory: ", tourCategory, totalChars);
        Household.logHelper(logger, "tourPurpose: ", tourPurpose, totalChars);
        Household.logHelper(logger, "tourPurposeIndex: ", tourPrimaryPurposeIndex, totalChars);
        Household.logHelper(logger, "personNumArray: ", personNumArrayString, totalChars);
        Household.logHelper(logger, "jointTourComposition: ", jointTourComposition, totalChars);
        Household.logHelper(logger, "tourOrigTaz: ", tourOrigTaz, totalChars);
        Household.logHelper(logger, "tourDestTaz: ", tourDestTaz, totalChars);
        Household.logHelper(logger, "tourOrigWalkSubzone: ", tourOrigWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDestWalkSubzone: ", tourDestWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", tourDepartPeriod, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", tourArrivePeriod, totalChars);
        Household.logHelper(logger, "tourMode: ", tourMode, totalChars);
        Household.logHelper(logger, "stopFreqChoice: ", stopFreqChoice, totalChars);

        String tempString = String.format("outboundStops[%s]:", outboundStops == null ? "" : String
                .valueOf(outboundStops.length));
        logger.info(tempString);

        tempString = String.format("inboundStops[%s]:", inboundStops == null ? "" : String
                .valueOf(inboundStops.length));
        logger.info(tempString);

    }

    public void logEntireTourObject(Logger logger)
    {

        int totalChars = 60;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        String personNumArrayString = "-";
        if (personNumArray != null)
        {
            personNumArrayString = "[ ";
            personNumArrayString += String.format("%d", personNumArray[0]);
            for (int i = 1; i < personNumArray.length; i++)
                personNumArrayString += String.format(", %d", personNumArray[i]);
            personNumArrayString += " ]";
        }

        Household.logHelper(logger, "tourId: ", tourId, totalChars);
        Household.logHelper(logger, "tourCategory: ", tourCategory, totalChars);
        Household.logHelper(logger, "tourPurpose: ", tourPurpose, totalChars);
        Household.logHelper(logger, "tourPurposeIndex: ", tourPrimaryPurposeIndex, totalChars);
        Household.logHelper(logger, "personNumArray: ", personNumArrayString, totalChars);
        Household.logHelper(logger, "jointTourComposition: ", jointTourComposition, totalChars);
        Household.logHelper(logger, "tourOrigTaz: ", tourOrigTaz, totalChars);
        Household.logHelper(logger, "tourDestTaz: ", tourDestTaz, totalChars);
        Household.logHelper(logger, "tourOrigWalkSubzone: ", tourOrigWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDestWalkSubzone: ", tourDestWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", tourDepartPeriod, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", tourArrivePeriod, totalChars);
        Household.logHelper(logger, "tourMode: ", tourMode, totalChars);
        Household.logHelper(logger, "stopFreqChoice: ", stopFreqChoice, totalChars);

        if (outboundStops != null)
        {
            logger.info("Outbound Stops:");
            if (outboundStops.length > 0)
            {
                for (int i = 0; i < outboundStops.length; i++)
                    outboundStops[i].logStopObject(logger, totalChars);
            } else
            {
                logger.info("     No outbound stops");
            }
        } else
        {
            logger.info("     No outbound stops");
        }

        if (inboundStops != null)
        {
            logger.info("Inbound Stops:");
            if (inboundStops.length > 0)
            {
                for (int i = 0; i < inboundStops.length; i++)
                    inboundStops[i].logStopObject(logger, totalChars);
            } else
            {
                logger.info("     No inbound stops");
            }
        } else
        {
            logger.info("     No inbound stops");
        }

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public void setTourModalUtilities(float[] utils)
    {
        tourModalUtilities = utils;
    }

    public float[] getTourModalUtilities()
    {
        return tourModalUtilities;
    }

    public void setTourModalProbabilities(float[] probs)
    {
        tourModalProbabilities = probs;
    }

    public float[] getTourModalProbabilities()
    {
        return tourModalProbabilities;
    }

    public byte getTourCategoryIndex() {
        return tourCategoryIndex;
    }

    public boolean getTourCategoryIsMandatory() {
        return tourCategoryIndex == ModelStructure.MANDATORY_CATEGORY_INDEX;
    }

    public boolean getTourCategoryIsAtWork() {
        return tourCategoryIndex == ModelStructure.AT_WORK_CATEGORY_INDEX;
    }

    public boolean getTourCategoryIsJointNonMandatory() {
        return tourCategoryIndex == ModelStructure.JOINT_NON_MANDATORY_CATEGORY_INDEX;
    }

    public boolean getTourCategoryIsIndivNonMandatory() {
        return tourCategoryIndex == ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY_INDEX;
    }

    public int getWorkTourIndexFromSubtourId( int subtourId ) {
        // when subtour was created, it's purpose index was set to 10*(work purpose index + 1) + at-work subtour index
        return (subtourId / 10) - 1;
    }

    public int getSubtourIdFromIndices(int workTourIndex, int subtourIndex ) {
        // this is used to create the sutour, it's purpose index was set to 10*(work purpose index + 1) + at-work subtour index
        return 10*(workTourIndex+1) + subtourIndex;
    }
    
}
