package edu.pugetsound.npastor.routing;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicIntegerArray;

import edu.pugetsound.npastor.utils.DRTUtils;
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
	
	private int mType;
	private Trip mTrip;
	private int mStartTime;
	private int mDuration;
	private int mPlannedServiceTime;
	private int mTimeToNextJob;
	private VehicleScheduleJob mNextJob;
	private Point2D mLocation;
	
	// These arrays allow for worker threads to set their own
	// "working" values as they evaluate this job in their 
	// respective schedules. With these arrays, we can avoid cloning
	// VehicleScheduleJob objects into new lists every time we start
	// a new thread
	private int[] mWorkingTimesToNextJob;
	private VehicleScheduleJob[] mWorkingNextJobs;
	private int[] mWorkingServiceTimes;

	
	public VehicleScheduleJob(Trip trip, Point2D location, int startTime, int duration, int type, int vehicleNum) {
		mTrip = trip;
		mStartTime = startTime;
		mDuration = duration;
		mType = type;
		mPlannedServiceTime = startTime;
		mLocation = location;
		mWorkingTimesToNextJob = new int[vehicleNum];
		mWorkingNextJobs = new VehicleScheduleJob[vehicleNum];
		mWorkingServiceTimes = new int[vehicleNum];
	}
	
	/**
	 * Sets the working service time for the specified vehicle index
	 * to the specified value
	 * @param vehicleIndex Vehicle index
	 * @param value Working service time for specified vehicle index
	 */
	public void setWorkingServiceTime(int vehicleIndex, int value) {
		mWorkingServiceTimes[vehicleIndex] = value;
	}
	
	public void setNextJob(int vehicleIndex, VehicleScheduleJob nextJob) {
		if(vehicleIndex < 0)
			mNextJob = nextJob;
		else
			mWorkingNextJobs[vehicleIndex] = nextJob;
	}
	
	public void setServiceTime(int serviceTime) {
		mPlannedServiceTime = serviceTime;
	}
	
	public void setTimeToNextJob(int vehicleIndex, int timeMins) {
		if(vehicleIndex < 0)
			mTimeToNextJob = timeMins;
		else
			mWorkingTimesToNextJob[vehicleIndex] = timeMins;
	}
	
	/**
	 * Checks if the specified job is the next job that this instance is aware of
	 * @param toCheck Job to check 
	 * @return True if toCheck is equal to the next job that this is aware of, false otherwise
	 */
	public boolean nextJobIs(int vehicleIndex, VehicleScheduleJob toCheck) {
		if(vehicleIndex < 0) {
			if(mNextJob == null)
				return false;
			else
				return toCheck.equals(mNextJob);
		}
		
		if(mWorkingNextJobs[vehicleIndex] == null) 
			return false;
		else
			return toCheck.equals(mWorkingNextJobs[vehicleIndex]);
	}
	
	public int getWorkingServiceTime(int vehicleNum) {
		return mWorkingServiceTimes[vehicleNum];
	}
	
	public Point2D getLocation() {
		return mLocation;
	}
	
	public int getTimeToNextJob(int vehicleIndex) {
		if(vehicleIndex < 0)
			return mTimeToNextJob;
		else
			return mWorkingTimesToNextJob[vehicleIndex];
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
	
	@Override
	public boolean equals(Object obj) {
		VehicleScheduleJob compareJob = (VehicleScheduleJob) obj;
		int compareType = compareJob.getType();
		
		if(compareType == VehicleScheduleJob.JOB_TYPE_START || compareType == VehicleScheduleJob.JOB_TYPE_END ||
				mType == VehicleScheduleJob.JOB_TYPE_START || mType == VehicleScheduleJob.JOB_TYPE_END)
			return false;
		else {
			Trip compareTrip = compareJob.getTrip();
			if(compareTrip.getIdentifier() == mTrip.getIdentifier() &&
					compareType == mType)
				return true;
			else
				return false;
		}
	}
	
	public String toString() {
		String str = "Job type: " + mType + ". Start time: " + DRTUtils.minsToHrMin(mStartTime)
					+ ". Service time: " + DRTUtils.minsToHrMin(mPlannedServiceTime);
		if(mTrip != null) 
			str += ". Trip ID: " + mTrip.getIdentifier()
			+ ". Location: " + mLocation.getY() + ", " + mLocation.getX();
		return str;
	}
	
	// Note this is not a "perfect" clone. We won't bother deep copying the behemoth that is the Trip class.
	// However, all primitive types ARE copied, which is all we really need to for our purposes anyway.
//	@Override
//	public VehicleScheduleJob clone() {
//		int type = mType;
//		int startTime = mStartTime;
//		int duration = mDuration;
//		int serviceTime = mPlannedServiceTime;
//		int timeToNextJob = mTimeToNextJob;
//		VehicleScheduleJob clone = new VehicleScheduleJob(mTrip, mLocation, startTime, duration, type, mWorkingServiceTimes.length);
//		clone.setServiceTime(serviceTime);
//		clone.setNextJob(mNextJob);
//		clone.setTimeToNextJob(timeToNextJob);
//		return clone;
//		
//	}
}
