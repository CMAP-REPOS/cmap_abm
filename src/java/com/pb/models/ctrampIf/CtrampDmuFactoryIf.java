package com.pb.models.ctrampIf;


/**
 * Created by IntelliJ IDEA.
 * User: Jim
 * Date: Jul 9, 2008
 * Time: 3:13:17 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CtrampDmuFactoryIf {

    public AutoOwnershipChoiceDMU getAutoOwnershipDMU();
    
    public FreeParkingChoiceDMU getFreeParkingChoiceDMU();

    public CoordinatedDailyActivityPatternDMU getCoordinatedDailyActivityPatternDMU();

    public DcSoaDMU getDcSoaDMU();

    public DestChoiceDMU getDestChoiceDMU();

    public TourModeChoiceDMU getModeChoiceDMU();

    public IndividualMandatoryTourFrequencyDMU getIndividualMandatoryTourFrequencyDMU();

    public TourDepartureTimeAndDurationDMU getTourDepartureTimeAndDurationDMU();

    public AtWorkSubtourFrequencyDMU getAtWorkSubtourFrequencyDMU();

    public JointTourFrequencyDMU getJointTourFrequencyDMU();

    public IndividualNonMandatoryTourFrequencyDMU getIndividualNonMandatoryTourFrequencyDMU();

    public StopFrequencyDMU getStopFrequencyDMU();

    public StopDCSoaDMU getStopDCSoaDMU();
    
    public StopLocationDMU getStopLocationDMU();

    public TripModeChoiceDMU getTripModeChoiceDMU();
    
    public ParkingChoiceDMU getParkingChoiceDMU();

}
