package edu.pugetsound.npastor.routing;

/**
 * Wrapper class which contains the result of a RebusScheduleTask
 * @author Nathan P
 *
 */
public class ScheduleResult {
	public int mVehicleIndex;
	public boolean mSolutionFound;
	public double mOptimalScore;
	public int mOptimalPickupIndex;
	public int mOptimalDropoffIndex;
	
	public ScheduleResult(int vehiclePlanIndex) {
		mVehicleIndex = vehiclePlanIndex;
		mSolutionFound = false;
		mOptimalScore = 0;
	}
}