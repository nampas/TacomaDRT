package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.PriorityQueue;



import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

/**
 * An implementation of the REBUS algorithm. This is a heuristic solution to the dial-a-ride problem,
 * developed by Madsen et al. (1995)
 * @author Nathan P
 *
 */
public class REBUS {
	
	public static final String TAG = "REBUS";

	// *****************************************
	//         REBUS function constants
	// *****************************************
	// Job cost constants (job difficulty)
	private static final float WINDOW_C1 = 1.0f;
	private static final float WINDOW_C2 = 1.0f;
	private static final float WINDOW_MAX = 30.0f; // minutes
	private static final float TR_TIME_C1 = 1.0f;
	private static final float TR_TIME_C2 = 1.0f;
	private static final float TR_TIME_MAX = 1.0f;
	
	// Load constants (insertion feasibility)
	private static final float WAIT_C1 = 1.0f;
	private static final float WAIT_C2 = 1.0f;
	private static final float DEV_C = 1.0f;
	private static final float CAPACITY_C = 1.0f;
	
	PriorityQueue<Job> mJobQueue;
	
	public REBUS() {
		mJobQueue = new PriorityQueue<Job>();
	}
	
	/**
	 * Add a new trip request to the job queue. This will NOT schedule
	 * the job, scheduleQueuedJobs() must be called to do that.
	 * @param newTrip The trip to be added to job queue
	 */
	public void enqueueTripRequest(Trip newTrip) {
		Job newRequest = new Job(Job.JOB_NEW_REQUEST, newTrip, this);
		mJobQueue.add(newRequest);
	}
	
	/**
	 * Schedules all enqueued jobs, given the existing plan.
	 * This will modify the existing plan to include new jobs
	 * @param plan The existing route scheduling
	 * TODO: a robust return mechanism, which should include which (if any) jobs were not scheduled
	 */
	public void scheduleQueuedJobs(ArrayList<Vehicle> plan) {
		Log.info(TAG, "*************************************");
		Log.info(TAG, "      Scheduling " + mJobQueue.size() + " jobs");
		Log.info(TAG, "*************************************");
		while(!mJobQueue.isEmpty()) {
			Job job = mJobQueue.poll();
			scheduleJob(job);
		}
	}
	
	private void scheduleJob(Job job) {
		//TODO: REBUS YAYYY
	}
	
	
	// *************************************************
	//                 COST FUNCTIONS
	//  Cost functions assign a difficulty value to 
	//  each new trip insertion. We will therefore
	//  schedule the high priority (higher cost) trips
	//  first.
	// *************************************************
	
	/**
	 * Calculate the trip cost
	 * @param t Trip for which to calculate cost
	 * @return
	 */
	public double getCost(Trip t) {
		return costTimeWindow(t) + costMaxTravelTime(t);
	}
	
	/**
	 * Calculates the time window component of the trip's cost
	 * @param t Trip
	 * @return A double representing time window cost
	 */
	private double costTimeWindow(Trip t) {
		return 0;
		
	}
	
	/**
	 * Calculates the maximal travel time component of the trip's cost
	 * @param t
	 * @return
	 */
	private double costMaxTravelTime(Trip t) {
		return 0;
	}
	
	// ********************************************
	//               LOAD FUNCTIONS
	//  Load functions estimate the feasibility of 
	//  inserting a new job into an existing plan.
	// ********************************************
	
	/**
	 * Calculates the load of the trip if inserted into the specified vehicle
	 * @param t The trip to insert
	 * @param v The vehicle
	 * @return
	 */
	public double getLoad(Trip trip, Vehicle vehiclePlan) {
		return loadDrivingTime(trip, vehiclePlan) +
				loadWaitingTime(trip, vehiclePlan) +
				loadDesiredServiceTimeDeviation(trip, vehiclePlan) +
				loadCapacityUtilization(trip, vehiclePlan);
	}
	
	/**
	 * Calculates the driving time component of the load value
	 * @param t
	 * @param v
	 * @return
	 */
	private double loadDrivingTime(Trip trip, Vehicle vehiclePlan) {
		return 0;
	}
	
	/**
	 * Calculates the the waiting time component of the load value
	 * @param t
	 * @param v
	 * @return
	 */
	private double loadWaitingTime(Trip trip, Vehicle vehiclePlan) {
		return 0;
	}
	
	/**
	 * Calculates the service time deviation component of the load value
	 * @param t
	 * @param v
	 * @return
	 */
	private double loadDesiredServiceTimeDeviation(Trip trip, Vehicle vehiclePlan) {
		return 0;
	}
	
	/**
	 * Calculates the capacity utilization component of the load value
	 * @param t
	 * @param v
	 * @return
	 */
	private double loadCapacityUtilization(Trip trip, Vehicle vehiclePlan) {
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
