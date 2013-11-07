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
		ArrayList<Trip> rejectedTrips = new ArrayList<Trip>();
		while(!mJobQueue.isEmpty()) {
			REBUSJob job = mJobQueue.poll();
			if(!scheduleJob(job, plan)) {
				// If job was not successfully scheduled, add to list of failed jobs
				rejectedTrips.add(job.getTrip());
			}
		}
		return rejectedTrips;
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
			
			// Evaluate the job in every vehicle
			for(Vehicle vehicle : plan) {
				ScheduleResult result = evaluateTripInSchedule(pickupJob, dropoffJob, vehicle.getSchedule());
				// Replace the most optimal scheduling if this result has a smaller score (disturbs objective function less)
				if(result.mSolutionFound && (optimalScheduling == null || result.mOptimalScore < optimalScheduling.mOptimalScore))
					optimalScheduling = result;
			}
			if(optimalScheduling != null) {
				scheduleSuccessful = true;
				//TODO: actually do the scheduling
			}
		} else {
			scheduleSuccessful = true;
		}
		
		return scheduleSuccessful;
	}
	
	/**
	 * Evaluates the given trip (specified as pickup and dropoff events) in the given schedule. This will NOT schedule the jobs.
	 * @param pickup The trip's pickup job
	 * @param dropoff The trip's dropoff job
	 * @param schedule The schedule to evaluate
	 * @return A ScheduleResult object containing the results of the evaluation. If multiple feasible solution was found, this will
	 *         contain the solution that disturbs the objective function the least (miminizes mJobCost). If no feasible solution was
	 *         found, mSolutionFound will be set to false
	 */
	private ScheduleResult evaluateTripInSchedule(VehicleScheduleJob pickupJob, VehicleScheduleJob dropoffJob, ArrayList<VehicleScheduleJob> schedule) {
		
		// Initialize the result
		ScheduleResult result = new ScheduleResult();
		result.mSchedule = schedule;
		result.mSolutionFound = false;
		
		// Copy the list so we don't mess it up
		ArrayList<VehicleScheduleJob> scheduleCopy  = new ArrayList<VehicleScheduleJob>();
		for(int i = 0; i < schedule.size(); i++) {
			scheduleCopy.add(schedule.get(i).clone());
		}
		
		int pickupIndex = 1; //s1 in Madsen's notation
		int dropoffIndex = 2; //s2 in Madsen's notation
		
		// FOLLOWING COMMENTS ARE MADSEN'S REBUS PSEUDO CODE
		// Step 1: Place s1, s2 just after this first stop T0 in the scheduleCopy, and update the scheduleCopy
		scheduleCopy.add(pickupIndex, pickupJob);
		scheduleCopy.add(dropoffIndex, dropoffJob);
		updateSchedule(scheduleCopy, pickupIndex, dropoffIndex);
		
		// Step 2: While all insertions have not been evaluated, do
		while(pickupIndex < scheduleCopy.size() - 2) {
			// a) if s2 is before the last stop T1 in the scheduleCopy...
			if(scheduleCopy.get(dropoffIndex+1).getType() == VehicleScheduleJob.JOB_TYPE_END) {
				// then move s1 one stop to the right... 
				scheduleCopy.set(pickupIndex, scheduleCopy.get(pickupIndex+1)); // (swap elements, save time!)
				pickupIndex++;
				scheduleCopy.set(pickupIndex, pickupJob);
				// and place s2 just after s1...
				scheduleCopy.remove(dropoffIndex);
				dropoffIndex = pickupIndex + 1;
				scheduleCopy.add(dropoffIndex, dropoffJob);
				// and update the scheduleCopy.
				updateSchedule(scheduleCopy, pickupIndex, dropoffIndex);
			//    else, move s2 one step to the right
			} else {
				scheduleCopy.set(dropoffIndex, scheduleCopy.get(dropoffIndex+1)); // (swap elements)
				dropoffIndex++;
				scheduleCopy.set(dropoffIndex, dropoffJob);
			}
			// b) Check for feasibility 
			if(checkFeasibility(scheduleCopy)) {
				// i. if the insertion if feasible, then calculate the change in the objective,
				//    and compare to the previously found insertions
				double objFuncChange = calculateObjFuncChange(scheduleCopy);
				if(objFuncChange < result.mOptimalScore) {
					
				}
			} else {
				// ii. If the insertion is not feasible, check for the following situations:
				// TODO: all off this
			}
		}

		
		return result;
	}
	
	private void updateSchedule(ArrayList<VehicleScheduleJob> schedule, int pickupIndex, int dropoffIndex) {
		
	}
	
	/**
	 * Checks the feasibility of the given schedule. A schedule will FAIL the feasibility test if a time window at
	 * any stop is not satisfied, if the maximum travel time for any trip is exceeded, or if the vehicle capacity
	 * is exceeded at any point along its route. Otherwise, it will succeed.
	 * @param schedule The schedule for which to check feasibility
	 * @return True if schedule is feasible, false otherwise
	 */
	private boolean checkFeasibility(ArrayList<VehicleScheduleJob> schedule) {
		int numPassengers = 0;
		int currentTime = 0;
		boolean isFeasible = false;
		for(int i = 0; i < schedule.size(); i++) {
			VehicleScheduleJob curJob = schedule.get(i);
			int type = curJob.getType();
			if(type == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				numPassengers++;
				// Check if vehicle capacity has been exceeded
				if(numPassengers > Vehicle.VEHICLE_CAPACITY)
					break;
				
				//TODO: check window and max travel time
			} else if(type == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				numPassengers--;
			}
		}
		
		
		return isFeasible;
	}
	
	private double calculateObjFuncChange(ArrayList<VehicleScheduleJob> schedule) {
		double objFuncChange = 0;
		
		return objFuncChange;
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
		
		private double mJobCost;
		private int mType;
		private Trip mTrip;
		
		public REBUSJob(int type, Trip trip, double jobCost) {
			mTrip = trip;
			mType = type;
			mJobCost = jobCost;
		}
		
		public double getCost() {
			return mJobCost;
		}
		
		public int getType() {
			return mType;
		}
		
		public Trip getTrip() {
			return mTrip;
		}

		public int compareTo(REBUSJob REBUSJob) {
			int compareVal;
			double result = mJobCost - REBUSJob.getCost();
			
			if (result < 0) compareVal = 1;
			else if(result > 0) compareVal = -1;
			else compareVal = 0;
			
			return compareVal;
		}
	}
	
	// Wrapper class which contains the result of evaluateTripInSchedule() function
	private class ScheduleResult {
		public boolean mSolutionFound;
		public ArrayList<VehicleScheduleJob> mSchedule;
		public double mOptimalScore;
		public int mOptimalPickupIndex;
		public int mOptimalDropoffIndex;
		
		public ScheduleResult() {
			mOptimalScore = 1000;
		}
	}
}
