package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.utils.Trip;

/**
 * An implementation of the REBUS algorithm. This is a heuristic solution to the dial-a-ride problem,
 * developed by Madsen et al. (1995)
 * @author Nathan P
 *
 */
public class REBUS {

	PriorityQueue<Job> mJobQueue;
	
	public REBUS() {
		mJobQueue = new PriorityQueue<Job>();
	}
	
	/**
	 * Add a new trip request to the job queue. This will NOT schedule
	 * the job, scheduleQueueJobs() must be called. 
	 * @param t
	 */
	public void enqueueTripRequest(Trip t) {
		Job newRequest = new Job(Job.JOB_NEW_REQUEST, t, this);
		mJobQueue.add(newRequest);
	}
	
	/**
	 * Schedules all enqueued jobs, given the existing plan.
	 * This will modify the existing plan to include new jobs
	 * @param plan The existing route scheduling
	 * TODO: a robust return mechanism, which should include which (if any) jobs were not scheduled
	 */
	public void scheduleQueuedJobs(ArrayList<Vehicle> plan) {
		//TODO: REBUS YAYYY
	}
	
	
	// ***********************************************
	//                COST FUNCTIONS
	// Cost functions assign a difficulty value to 
	// each new trip insertion. We will therefore
	// schedule the high priority (higher cost) trips
	// first.
	// ***********************************************
	
	public double getCost(Trip t) {
		return costTimeWindow(t) + costMaxTravelTime(t);
	}
	
	private double costTimeWindow(Trip t) {
		return 0;
		
	}
	
	private double costMaxTravelTime(Trip t) {
		return 0;
	}
	
	// ******************************************
	//              LOAD FUNCTIONS
	// Load functions estimate the feasability of 
	// inserting a new job into an existing plan.
	// ******************************************
	
	public double getLoad(Trip t, Vehicle v) {
		return loadDrivingTime(t, v) +
				loadWaitingTime(t, v) +
				loadDesiredServiceTimeDeviation(t, v) +
				loadCapacityUtilization(t, v);
	}
	
	private double loadDrivingTime(Trip t, Vehicle v) {
		return 0;
	}
	
	private double loadWaitingTime(Trip t, Vehicle v) {
		return 0;
	}
	
	private double loadDesiredServiceTimeDeviation(Trip t, Vehicle v) {
		return 0;
	}
	
	private double loadCapacityUtilization(Trip t, Vehicle v) {
		return 0;
	}

	/**
	 * Represents a REBUS job. This is either a new request or 
	 * a beginning/end point
	 * @author Nathan P
	 */
	private class Job implements Comparable<Job> {
		public static final int JOB_NEW_REQUEST = 0;
		
		private double mJobCost;
		private int mType;
		private Trip mTrip;
		private REBUS mREBUS;
		
		public Job(int type, Trip trip, REBUS rebus) {
			mTrip = trip;
			mType = type;
			mREBUS = rebus;
			mJobCost = calculateJobCost();
		}
		
		private double calculateJobCost() {
			return mREBUS.getCost(mTrip);
		}
		
		public double getCost() {
			return mJobCost;
		}

		public int compareTo(Job job) {
			int compareVal = 0;
			double result = mJobCost - job.getCost();
			
			if (result < 0) compareVal = -1;
			else if(result > 0) compareVal = 1;
			
			return compareVal;
		}
	}
}
