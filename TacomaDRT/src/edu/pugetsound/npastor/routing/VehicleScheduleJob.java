package edu.pugetsound.npastor.routing;

import edu.pugetsound.npastor.utils.Trip;

/**
 * Represents a job in a vehicle's schedule
 * @author Nathan P
 *
 */
public class VehicleScheduleJob implements Comparable<VehicleScheduleJob> {

	public static final int JOB_TYPE_PICKUP = 0;
	public static final int JOB_TYPE_DROPOFF = 3;
	public static final int JOB_TYPE_START = 1;
	public static final int JOB_TYPE_END = 2;
	
	int mType;
	Trip mTrip;
	double mStartTime;
	double mDuration;
	
	public VehicleScheduleJob(Trip trip, double startTime, double duration, int type) {
		mTrip = trip;
		mStartTime = startTime;
		mDuration = duration;
		mType = type;
	}
	
	public int getType() {
		return mType;
	}
	
	public Trip getTrip() {
		return mTrip;
	}
	
	public double getStartTime() {
		return mStartTime;
	}
	
	public double getEndTime() {
		return mStartTime + mDuration;
	}
	
	public double getDuration() {
		return mDuration;
	}
	
	public int compareTo(VehicleScheduleJob job) {
		int returnVal = 0;
		
		double compareVal = mStartTime - job.getStartTime();
		if(compareVal < 0) returnVal = -1;
		else if(compareVal > 0) returnVal = 1;
		
		return returnVal;
	}
	
}
