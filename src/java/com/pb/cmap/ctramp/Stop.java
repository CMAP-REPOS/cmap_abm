package com.pb.cmap.ctramp;

import java.io.Serializable;
import org.apache.log4j.Logger;
import com.pb.models.ctrampIf.ModelStructure;
import com.pb.models.ctrampIf.StopIf;
import com.pb.models.ctrampIf.TourIf;

public class Stop implements StopIf, Serializable
{

    private int     id;
    private int     orig;
    private int     origWalkSegment;
    private int     dest;
    private int     destWalkSegment;
    private int     park;
    private int     mode;
    private int     stopPeriod;
    private boolean inbound;
    private int     btap;
    private int     atap;

    private String  origPurpose;
    private String  destPurpose;
    private int stopPurposeIndex;
    
    private Tour    parentTour;

    public Stop(TourIf parentTour, String origPurpose, String destPurpose, int id, boolean inbound, int stopPurposeIndex)
    {
        this.parentTour = (Tour) parentTour;
        this.origPurpose = origPurpose;
        this.destPurpose = destPurpose;
        this.stopPurposeIndex = stopPurposeIndex;
        this.id = id;
        this.inbound = inbound;
    }

    public void setOrig(int orig)
    {
        this.orig = orig;
    }

    public void setDest(int dest)
    {
        this.dest = dest;
    }
    
    
    public void setBTap(int tap) {
    	btap = tap;
    }

    public void setATap(int tap) {
    	atap = tap;
    }

    public int getBTap() {
    	return(btap);
    }

    public int getATap() {
    	return(atap);
    }

    public void setPark(int park)
    {
        this.park = park;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    public void setStopPeriod(int period)
    {
        this.stopPeriod = period;
    }

    public int getOrig()
    {
        return orig;
    }

    public int getDest()
    {
        return dest;
    }

    public int getPark()
    {
        return park;
    }

    public String getDestPurpose()
    {
        return destPurpose;
    }

    public int getStopPurposeIndex()
    {
        return stopPurposeIndex;
    }
    
    public int getMode()
    {
        return mode;
    }

    public int getStopPeriod() {
        return stopPeriod;
    }

    public TourIf getTour()
    {
        return parentTour;
    }

    public boolean isInboundStop()
    {
        return inbound;
    }

    public int getStopId()
    {
        return id;
    }

    public void logStopObject( Logger logger, int totalChars ) {
        
        String separater = "";
        for (int i=0; i < totalChars; i++)
            separater += "-";

        Household.logHelper( logger, "stopId: ", id, totalChars );
        Household.logHelper( logger, "origPurpose: ", origPurpose, totalChars );
        Household.logHelper( logger, "destPurpose: ", destPurpose, totalChars );
        Household.logHelper( logger, "orig: ", orig, totalChars );
        Household.logHelper( logger, "dest: ", dest, totalChars );
        Household.logHelper( logger, "mode: ", mode, totalChars );
        Household.logHelper( logger, "direction: ", inbound ? "inbound" : "outbound", totalChars );
        Household.logHelper( logger, inbound ? "inbound arrive period" : "outbound depart period: ", stopPeriod, totalChars );
        logger.info(separater);
        logger.info( "" );
        logger.info( "" );

    }

    public String getOrigPurpose() {
        return origPurpose;
    }

    public int getTourDepartPeriod() {
        return parentTour.getTourDepartPeriod();
    }

    public int getDestWalkSegment() {
        return destWalkSegment;
    }

    public void setDestWalkSegment(int destSegment) {
        destWalkSegment = destSegment;
    }

    public int getOrigWalkSegment() {
        return origWalkSegment;
    }

    public void setOrigWalkSegment(int origSegment) {
        origWalkSegment = origSegment;
    }

    public int getDestPurposeIndex() {
        return stopPurposeIndex;
    }
}
