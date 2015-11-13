package com.pb.cmap.tvpb;

public class TapPair implements Comparable<TapPair>{
	
	public int otap;
	public int dtap;
	public int otaptaz;
	public int dtaptaz;
	public boolean isWalkTapPair = false;
	public boolean isPnrTapPair = false;
	public boolean isKnrTapPair = false;
	public int NA_VALUE = 0; //0 since all valid paths have a positive generalized cost
	
    public double[] maz2tapUtils;
    public double[] otapUtils;
    public double[] tap2tapUtils;
    public double[] dtapUtils;
    public double[] tap2mazUtils;
    
	public TapPair(int otap, int dtap, boolean isWalkTapPair, boolean isPnrTapPair, boolean isKnrTapPair, int alts) {
		this.otap = otap;
		this.dtap = dtap;
		this.isWalkTapPair = isWalkTapPair;
		this.isPnrTapPair = isPnrTapPair;
		this.isKnrTapPair = isKnrTapPair;
		
		maz2tapUtils  = new double[alts];
	    otapUtils = new double[alts];
	    tap2tapUtils = new double[alts];
	    dtapUtils = new double[alts];
	    tap2mazUtils = new double[alts];
	    
	}
	
	public double[] getTotalUtils() {
		
		double[] totalUtils = new double[maz2tapUtils.length];
		for(int i=0; i<maz2tapUtils.length; i++) {
			if(maz2tapUtils[i] != NA_VALUE & otapUtils[i] != NA_VALUE & tap2tapUtils[i] != NA_VALUE &
					dtapUtils[i] != NA_VALUE & tap2mazUtils[i] != NA_VALUE) {
				totalUtils[i] = maz2tapUtils[i] + otapUtils[i] + tap2tapUtils[i] + dtapUtils[i] + tap2mazUtils[i];
			} else {
				totalUtils[i] = NA_VALUE;
			}
		}
		return(totalUtils);
	}
	
	@Override
	public int compareTo(TapPair o) {
		
			double bestFromThis;
			double bestFromOther;
			
			if(maz2tapUtils.length==2) {
				//sort so if either alt is better then the whole tapPair is better
				bestFromThis = Math.min(this.getTotalUtils()[0], this.getTotalUtils()[1]); 
				bestFromOther = Math.min(o.getTotalUtils()[0], o.getTotalUtils()[1]);
				
				if(bestFromThis == NA_VALUE) {
					bestFromThis = Math.max(this.getTotalUtils()[0], this.getTotalUtils()[1]);
				}
				if(bestFromOther == NA_VALUE) {
					bestFromOther = Math.max(o.getTotalUtils()[0], o.getTotalUtils()[1]);
				}
				
			} else {
				bestFromThis = this.getTotalUtils()[0]; 
				bestFromOther = o.getTotalUtils()[0];				
			}
			
			//return compareTo value
		    if ( bestFromThis < bestFromOther ) {
		    	return -1;
		    } else if (bestFromThis == bestFromOther) {
		    	return 0;
		    } else {
		    	return 1;
		    }
	    
		
	}
	
}