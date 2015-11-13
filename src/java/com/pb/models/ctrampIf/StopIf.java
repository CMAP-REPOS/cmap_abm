package com.pb.models.ctrampIf;

import org.apache.log4j.Logger;

public interface StopIf
{

    public abstract void setOrig(int orig);

    public abstract void setDest(int dest);

    public abstract void setPark(int park);

    public abstract void setMode(int mode);

    public abstract void setStopPeriod(int period);

    public abstract int getOrig();

    public abstract int getDest();

    public abstract int getPark();

    public abstract int getOrigWalkSegment();

    public abstract void setOrigWalkSegment(int origWalkSegment);

    public abstract int getDestWalkSegment();

    public abstract void setDestWalkSegment(int destWalkSegment);

    public abstract String getOrigPurpose();

    public abstract String getDestPurpose();

    public abstract int getDestPurposeIndex();

    public abstract int getMode();

    public abstract int getTourDepartPeriod();

    public abstract int getStopPeriod();

    public abstract TourIf getTour();

    public abstract boolean isInboundStop();

    public abstract int getStopId();

    public abstract void logStopObject( Logger logger, int numChars );
    
    public abstract void setBTap(int tap);

    public abstract void setATap(int tap);

    public abstract int getBTap();

    public abstract int getATap();

}