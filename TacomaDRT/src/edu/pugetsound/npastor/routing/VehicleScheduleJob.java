package edu.pugetsound.npastor.routing;

import java.awt.geom.Point2D;

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
	
	public static final int TIME_TO_NEXT_UNKNOWN = -1;
	
	public static final int JOB_TYPE_PICKUP = 0;
	public static final int JOB_TYPE_DROPOFF = 1;
	public static final int JOB_TYPE_START = 2;
	public static final int JOB_TYPE_END = 3;
	
	private int mType;
	private Trip mTrip;
	private int mStartTime;
	private int mDuration;
	private int mPlannedServiceTime;
	private Point2D mLocation;
	
	// For keeping track of next job
	private int mTimeToNextJob;
	private VehicleScheduleJob mNextJob;
	
	public VehicleScheduleJob(Trip trip, Point2D location, int startTime, int duration, int type) {
		mTrip = trip;
		mStartTime = startTime;
		mDuration = duration;
		mType = type;
		mPlannedServiceTime = startTime;
		mLocation = location;
		mTimeToNextJob = TIME_TO_NEXT_UNKNOWN;
		mNextJob = null;
	}
	
	public void setNextJob(VehicleScheduleJob nextJob) {
		mNextJob = nextJob;
	}
	
	public void setServiceTime(int serviceTime) {
		mPlannedServiceTime = serviceTime;
	}
	
	public void setTimeToNextJob(int timeMins) {
		mTimeToNextJob = timeMins;
	}
	
	/**
	 * Checks if the specified job is the next job that this instance is aware of
	 * @param toCheck Job to check 
	 * @return True if toCheck is equal to the next job that this is aware of, false otherwise
	 */
	public boolean nextJobIs(VehicleScheduleJob toCheck) {
		if(mNextJob == null) 
			return false;
		else
			return toCheck.equals(mNextJob);
	}
	
	public Point2D getLocation() {
		return mLocation;
	}
	
	public int getTimeToNextJob() {
		return mTimeToNextJob;
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
//	
//	@Override
//	public boolean equals(Object obj) {
//		VehicleScheduleJob job = (VehicleScheduleJob) obj;
//		int jobType = job.getType();
//		
//		if(jobType == VehicleScheduleJob.JOB_TYPE_START || jobType == VehicleScheduleJob.JOB_TYPE_END)
//			return false;
//		else {
//			Trip jobTrip = job.getTrip();
//			if(jobTrip.getIdentifier() == )
//		}
//	}
//	
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
		VehicleScheduleJob clone = new VehicleScheduleJob(mTrip, mLocation, startTime, duration, type);
		clone.setServiceTime(serviceTime);
		return clone;
		
	}
}
