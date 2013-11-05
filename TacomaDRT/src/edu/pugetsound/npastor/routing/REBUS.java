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
	private static final float MAX_TRAVEL_COEFF = 2.0f; //Max travel time for a given trip is this coefficient
														//multiplied by direct travel time
	
	// Load constants (insertion feasibility)
	private static final float WAIT_C1 = 1.0f;
	private static final float WAIT_C2 = 1.0f;
	private static final float DEV_C = 1.0f;
	private static final float CAPACITY_C = 1.0f;
	
	PriorityQueue<REBUSJob> mJobQueue;
	
	public REBUS() {
		mJobQueue = new PriorityQueue<REBUSJob>();
	}
	
	/**
	 * Add a new trip request to the job queue. This will NOT schedule
	 * the job, scheduleQueuedJobs() must be called to do that.
	 * @param newTrip The trip to be added to job queue
	 */
	public void enqueueTripRequest(Trip newTrip) {
		REBUSJob newRequest = new REBUSJob(REBUSJob.JOB_NEW_REQUEST, newTrip, getCost(newTrip));
		mJobQueue.add(newRequest);
	}
	
	/**
	 * Schedules all enqueued jobs, given the existing plan.
	 * This will modify the existing plan to include new jobs
	 * @param plan The existing route scheduling
	 * @result A list of trips which REBUS was not able to schedule
	 */
	public ArrayList<Trip> scheduleQueuedJobs(ArrayList<Vehicle> plan) {
		Log.info(TAG, "*************************************");
		Log.info(TAG, "      Scheduling " + mJobQueue.size() + " job(s)");
		Log.info(TAG, "*************************************");
		ArrayList<Trip> unscheduledTrips = new ArrayList<Trip>();
		while(!mJobQueue.isEmpty()) {
			REBUSJob job = mJobQueue.poll();
			if(!scheduleJob(job, plan)) {
				// If job was not successfully scheduled, add to list of failed jobs
				unscheduledTrips.add(job.getTrip());
			}
		}
		return unscheduledTrips;
	}
	
	/**
	 * Schedules the specified job
	 * @param job The job to schedule
	 * @param plan The existing vehicle plans
	 * @result true if job was successfully placed in a schedule, false if otherwise
	 */
	private boolean scheduleJob(REBUSJob job, ArrayList<Vehicle> plan) {
		boolean scheduleSuccessful  = false;
		if(job.getType() == REBUSJob.JOB_NEW_REQUEST) {
			Trip t = job.getTrip();
			Log.info(TAG, "Scheduling trip " + t.getIdentifier() + ". Cost: " + job.getCost());
			
			// Split the trip into pickup and dropoff jobs
			double durationMins = (double)t.getRoute().getTime() / 60;
			VehicleScheduleJob pickupJob = new VehicleScheduleJob(t, t.getPickupTime(), durationMins, VehicleScheduleJob.JOB_TYPE_PICKUP);
			VehicleScheduleJob dropoffJob = new VehicleScheduleJob(t, t.getPickupTime() + durationMins, 0, VehicleScheduleJob.JOB_TYPE_DROPOFF);
			
			// Keep track of the most optimal insertion of the job
			ScheduleResult optimalScheduling = null;
			
			// We evaluate the job in every vehicle at every location
			for(Vehicle vehicle : plan) {
				ScheduleResult result = evaluateTripInSchedule(pickupJob, dropoffJob, vehicle.getSchedule());
				// Replace the most optimal scheduling if this result has a smaller score (disturbs objective function less)
				if(result.mSolutionFound && (optimalScheduling == null || result.mOptimalScore < optimalScheduling.mOptimalScore))
					optimalScheduling = result;
			}
			if(optimalScheduling != null) scheduleSuccessful = true;
		} else {
			scheduleSuccessful = true;
		}
		
		return scheduleSuccessful;
	}
	
	private ScheduleResult evaluateTripInSchedule(VehicleScheduleJob pickup, VehicleScheduleJob dropoff, VehicleSchedule schedule) {
		ScheduleResult result = new ScheduleResult();
		result.mSchedule = schedule;
		result.mSolutionFound = false;
		
		//TODO: REBUS YAYYY
		
		return result;
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
		//For now, all trip windows have equal size
		return 0;
		
	}
	
	/**
	 * Calculates the maximal travel time component of the trip's cost. Essentially, longer trips
	 * will be weighed as more difficult
	 * @param t The trip to calculate max travel cost for
	 * @return A value representing maximal travel cost of trip
	 */
	private double costMaxTravelTime(Trip t) {
		long minTime = t.getRoute().getTime();
		// Calculate difference between max allowable travel time and min possible travel time
		double deltaTransit = (minTime * MAX_TRAVEL_COEFF) - minTime;
		// Maximal travel time cost function
		double costFunction = TR_TIME_C2 * Math.pow(deltaTransit, -1) + TR_TIME_C1;
		return costFunction;
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
	private class REBUSJob implements Comparable<REBUSJob> {
		public static final int JOB_NEW_REQUEST = 0;
		
		private double mREBUSJobCost;
		private int mType;
		private Trip mTrip;
		
		public REBUSJob(int type, Trip trip, double REBUSJobCost) {
			mTrip = trip;
			mType = type;
			mREBUSJobCost = REBUSJobCost;
		}
		
		public double getCost() {
			return mREBUSJobCost;
		}
		
		public int getType() {
			return mType;
		}
		
		public Trip getTrip() {
			return mTrip;
		}

		public int compareTo(REBUSJob REBUSJob) {
			int compareVal = 0;
			double result = mREBUSJobCost - REBUSJob.getCost();
			
			if (result < 0) compareVal = 1;
			else if(result > 0) compareVal = -1;
			
			return compareVal;
		}
	}
	
	private class ScheduleResult {
		public boolean mSolutionFound;
		public VehicleSchedule mSchedule;
		public double mOptimalScore;
		public int mOptimalPickupIndex;
		public int mOpitmalDropoffIndex;
	}
}
