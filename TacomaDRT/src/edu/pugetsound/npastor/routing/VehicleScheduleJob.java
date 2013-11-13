package edu.pugetsound.npastor.routing;

import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

/**
 * Represents a job in a vehicle's schedule
 * @author Nathan P
 *
 */
public class VehicleScheduleJob implements Comparable<VehicleScheduleJob>, Cloneable {

	public static final String TAG = "VehicleScheduleJob";
	
	public static final int JOB_TYPE_PICKUP = 0;
	public static final int JOB_TYPE_DROPOFF = 1;
	public static final int JOB_TYPE_START = 2;
	public static final int JOB_TYPE_END = 3;
	
	int mType;
	Trip mTrip;
	int mStartTime;
	int mDuration;
	int mPlannedServiceTime;
	
	public VehicleScheduleJob(Trip trip, int startTime, int duration, int type) {
		mTrip = trip;
		mStartTime = startTime;
		mDuration = duration;
		mType = type;
		mPlannedServiceTime = startTime;
	}
	
	
	/**
	 * Every VehicleScheduleJob can contain one reference to another VehicleScheduleJob. 
	 * This is useful for linking pickup and dropoff jobs of the same trip
	 * @param job The job to link
	 */
//	public void setCorrespondingJob(VehicleScheduleJob job) {
//		mCorrespondingJob = job;
//	}
	
	public void setServiceTime(int serviceTime) {
		mPlannedServiceTime = serviceTime;
	}
	
	public int getType() {
		return mType;
	}
	
	public Trip getTrip() {
		return mTrip;
	}
	
	public int getStartTime() {
		return mStartTime;
	}
	
	public int getEndTime() {
		return mStartTime + mDuration;
	}
	
	public int getDuration() {
		return mDuration;
	}
	
	public int getServiceTime() {
		return mPlannedServiceTime;
	}
	
	public int compareTo(VehicleScheduleJob job) {
		int returnVal = 0;
		
		double compareVal = mStartTime - job.getStartTime();
		if(compareVal < 0) returnVal = -1;
		else if(compareVal > 0) returnVal = 1;
		
		return returnVal;
	}
	
	public String toString() {
		String str = "Job type: " + mType + ". Start time: " + DRTUtils.minsToHrMin(mStartTime) +
						". Service time: " + DRTUtils.minsToHrMin(mPlannedServiceTime);
		if(mTrip != null) str += ". Trip ID: " + mTrip.getIdentifier();
		return str;
	}
	
	// Note this is not a "perfect" clone. We won't bother deep copying the behemoth that is the Trip class.
	// However, all primitive types ARE copied, which is all we really need to for our purposes anyway.
	@Override
	public VehicleScheduleJob clone() {
		int type = mType;
		int startTime = mStartTime;
		int duration = mDuration;
		int serviceTime = mPlannedServiceTime;
		VehicleScheduleJob clone = new VehicleScheduleJob(mTrip, startTime, duration, type);
		clone.setServiceTime(serviceTime);
		return clone;
		
	}
}
