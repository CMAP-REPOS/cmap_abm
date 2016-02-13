package com.pb.cmap.tvpb;

import java.io.Serializable;

/**
 * This class is used for specifying modes.
 * 
 * @author Christi Willison
 * @version Sep 8, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class Modes
        implements Serializable
{

    public enum TransitMode
    {
    	// label and true = premium
        COMMUTER_RAIL("cr", true), 
        LIGHT_RAIL("lr", true), 
        BRT("brt", true), 
        EXPRESS_BUS("eb", true), 
        LOCAL_BUS("lb", false);

        private final String  name;
        private final boolean premium;

        TransitMode(String name, boolean premium)
        {
            this.name = name;
            this.premium = premium;
        }

        public TransitMode[] getTransitModes()
        {
            return TransitMode.values();
        }

        public boolean isPremiumMode(TransitMode transitMode)
        {
            return transitMode.premium;
        }

        public String toString()
        {
            return name;
        }

    }

    public enum AccessMode
    {
        WALK("WLK"), PARK_N_RIDE("PNR"), KISS_N_RIDE("KNR");
        private final String name;

        AccessMode(String name)
        {
            this.name = name;
        }

        public AccessMode[] getAccessModes()
        {
            return AccessMode.values();
        }
    }

}
