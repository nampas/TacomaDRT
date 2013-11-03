package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class Vehicle {

	private int mVehicleId;
	private int mCapacity;
	private int mMPH;
	private ArrayList<Trip> mPassengers;
	private Schedule mSchedule;
	
	
	public Vehicle() {
		mCapacity = Constants.VEHCILE_QUANTITY;
		mPassengers = new ArrayList<Trip>();
		mMPH = Constants.VEHICLE_MPH;
		mVehicleId = hashCode();
		mSchedule = new Schedule();
	}
	
	public void addPassenger(Trip t) {
		mPassengers.add(t);
	}
	
	public void addPassengers(ArrayList<Trip> passengers) {
		mPassengers.addAll(passengers);
	}
	
	public void removePassenger(Trip t) {
		mPassengers.remove(t);
	}
	
	public void removeAllPassengers() {
		mPassengers.clear();
	}
	
	public int getCurrentLoad() {
		return mPassengers.size();
	}
	
	public int getCapacity() {
		return mCapacity;
	}
	
	public Schedule getSchedule() {
		return mSchedule;
	}
	
	public class Schedule {

		ArrayList<ScheduleJob> mScheduledJobs;
		ArrayList<ScheduleJob> mConsumedJobs;
		
		public Schedule() {
			mScheduledJobs = new ArrayList<ScheduleJob>();
			mConsumedJobs = new ArrayList<ScheduleJob>();
			initialize();
		}
		
		private void initialize() {
			// Add start and end jobs to queue (leave and return to base)
			ScheduleJob startJob = new ScheduleJob(null, Constants.BEGIN_OPERATION_HOUR - 1, 0, ScheduleJob.JOB_TYPE_START);
			ScheduleJob endJob = new ScheduleJob(null, Constants.END_OPERATION_HOUR + 1, 0, ScheduleJob.JOB_TYPE_END);
			mScheduledJobs.add(startJob);
			mScheduledJobs.add(endJob);
		}
		
		/**
		 * Returns the job at the specified index
		 * @param index
		 * @return Job at specified index, or null if out of bounds
		 */
		public ScheduleJob getJob(int index) {

			if(index >= mScheduledJobs.size())
				return null;
			else 
				return mScheduledJobs.get(index);
		}
		
		/**
		 * Adds a job to the schedule, which is inserted based on 
		 * start time priority
		 * @param newJob New job to add
		 */
		public void scheduleJob(ScheduleJob newJob) {
			double startTime = newJob.getStartTime();
			int index = 0;
			while(mScheduledJobs.get(index) != null && 
					mScheduledJobs.get(index).getStartTime() > startTime)
				index++;
			
			mScheduledJobs.add(index, newJob);
		}
		
		/**
		 * Returns next job in schedule queue
		 * @return
		 */
		public ScheduleJob consumeNextJob() {
			ScheduleJob job = mScheduledJobs.remove(0);
			mConsumedJobs.add(job);
			return job;
		}
		
		public int getNumJobsRemaining() {
			return mScheduledJobs.size();
		}
	}
	
	/**
	 * Represents a job in a vehicle's schedule
	 * @author Nathan P
	 *
	 */
	public class ScheduleJob implements Comparable<ScheduleJob> {

		public static final int JOB_TYPE_PICKUP = 0;
		public static final int JOB_TYPE_START = 1;
		public static final int JOB_TYPE_END = 2;
		
		int mType;
		Trip mTrip;
		double mStartTime;
		double mDuration;
		
		public ScheduleJob(Trip trip, double startTime, double duration, int type) {
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
		
		public int compareTo(ScheduleJob job) {
			int returnVal = 0;
			
			double compareVal = mStartTime - job.getStartTime();
			if(compareVal < 0) returnVal = -1;
			else if(compareVal > 0) returnVal = 1;
			
			return returnVal;
		}
		
	}
}
