package edu.pugetsound.npastor.routing;

import java.util.ArrayList;

// Wrapper class which contains the result of evaluateTripInSchedule() function
public class ScheduleResult {
	public int mVehicleIndex;
	public boolean mSolutionFound;
	public double mOptimalScore;
	public int mOptimalPickupIndex;
	public int mOptimalDropoffIndex;
	
	public ScheduleResult() {
		mOptimalScore = 100000;
	}
}