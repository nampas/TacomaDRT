package edu.pugetsound.npastor.routing;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

/**
 * A parallelized implementation of the REBUS algorithm. This is a heuristic solution to the dial-a-ride problem,
 * developed by Madsen et al. (1995). It uses a two-step insertion technique, outlined below.
 * 
 *   A brief REBUS outline:
 *   1) Queue all jobs according to a difficulty cost
 *   2) Consume the job queue. For each job in each vehicle schedule, do:
 *      2a) Insert job into schedule in every legitimate permutation (e.g, no dropffs before pickups)
 *          For each permutation, do:
 *          2aa) Determine insertion feasibility by assessing if any constraints have been violated
 *          2bb) If no constraints have been violated, calculate this schedule's objective function
 *               Lower objective function scores are more desirable.
 *      2b) If one or more feasible insertions have been found, chose the insertion with the smallest
 *          objective function scores. Otherwise, reject the trip as unschedulable
 *     
 * Efficiency? 0.0347619x^2 - 0.730952x + 1.64286
 * @author Nathan Pastor
 */
public class Rebus {
	
	public static final String TAG = "Rebus";
	
	private static final int NUM_SCHEDULER_THREADS = 4;

	// *****************************************
	//         REBUS function constants
	// *****************************************
	// Job cost constants (job difficulty)
	public static final float WINDOW_C1 = 1.0f;
	public static final float WINDOW_C2 = 1.0f;
	public static final float TR_TIME_C1 = 1.0f;
	public static final float TR_TIME_C2 = 1.0f;
	public static final float MAX_TRAVEL_COEFF = 2.0f; //Max travel time for a given trip is this coefficient
														//multiplied by direct travel time
	
	// Load constants (insertion feasibility)
	public static final float DR_TIME_C1 = 1.0f; // Cvariable in Madsen's notation
	public static final float DR_TIME_C2 = 1.0f; // Cconst in Madsen's notation
	public static final float WAIT_C1 = 1.0f;
	public static final float WAIT_C2 = 1.0f;
	public static final float DEV_C = 1.0f;
	public static final float CAPACITY_C = 1.0f;
	
	PriorityQueue<REBUSJob> mJobQueue;
	private int mTotalJobsHandled;
	private ExecutorService mScheduleExecutor;
	private Routefinder mRouter;
	
	public Rebus() {
		mJobQueue = new PriorityQueue<REBUSJob>();
		mTotalJobsHandled = 0;
		mScheduleExecutor = Executors.newFixedThreadPool(NUM_SCHEDULER_THREADS);
		mRouter = new Routefinder();
	}
	
	public int getQueueSize() {
		return mJobQueue.size();
	}
	
	
	public void onRebusFinished() {
		mScheduleExecutor.shutdown();
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
	public ArrayList<Trip> scheduleQueuedJobs(Vehicle[] plan) {
		Log.info(TAG, "*************************************");
		Log.info(TAG, "       Scheduling " + mJobQueue.size() + " job(s)");
		ArrayList<Trip> rejectedTrips = new ArrayList<Trip>();
		while(!mJobQueue.isEmpty()) {
			REBUSJob job = mJobQueue.poll();
			mTotalJobsHandled++;
			if(!scheduleJob(job, plan)) {
				// If job was not successfully scheduled, add to list of failed jobs
				rejectedTrips.add(job.getTrip());
				Log.info(TAG, "   REJECTED. Trip " + job.getTrip().getIdentifier());
			}
		}
		Log.info(TAG, rejectedTrips.size() + " trip(s) rejected from scheduling.");
		return rejectedTrips;
	}
	
	/**
	 * Schedules the specified job
	 * @param job The job to schedule
	 * @param plan The existing vehicle plans
	 * @result true if job was successfully placed in a schedule, false if otherwise
	 */
	private boolean scheduleJob(REBUSJob job, Vehicle[] plan) {
		boolean scheduleSuccessful = false;
		if(job.getType() == REBUSJob.JOB_NEW_REQUEST) {
			Trip t = job.getTrip();
			Log.info(TAG, "On trip " + mTotalJobsHandled + ". Scheduling " + t.toString().replace("\n", "") +
					   "\n           Cost: " + job.getCost());
			
			// Split the trip into pickup and dropoff jobs
			int durationMins = (int)t.getRoute().getTime() / 60;
			VehicleScheduleJob pickupJob = new VehicleScheduleJob(t, t.getPickupTime(), durationMins, VehicleScheduleJob.JOB_TYPE_PICKUP);
			VehicleScheduleJob dropoffJob = new VehicleScheduleJob(t, t.getPickupTime() + durationMins, 0, VehicleScheduleJob.JOB_TYPE_DROPOFF);
			
			// Keep track of the most optimal insertion of the job
			ScheduleResult optimalScheduling = null;
			
			// A list of thread tasks, which will be invoked simultaneously 
			Collection<RebusScheduleTask> threadTasks = new ArrayList<RebusScheduleTask>();
			
			// Job must be evaluated in every vehicle.
			// Prepare threads to execute in parallel
			for(int i = 0; i < plan.length; i++) {
				Vehicle v = plan[i];
				
				// Build new worker threads and copy schedules and jobs. We don't want to modify existing schedule
				ArrayList<VehicleScheduleJob> existingSchedule = v.getSchedule();
				ArrayList<VehicleScheduleJob> scheduleCopy  = new ArrayList<VehicleScheduleJob>();
				for(int j = 0; j < existingSchedule.size(); j++) {
					scheduleCopy.add(existingSchedule.get(j).clone());
				}
				threadTasks.add(new RebusScheduleTask(i, scheduleCopy, pickupJob.clone(), dropoffJob.clone()));
			}
			
			// Execute all threads and wait
			// TODO: faster to run threads as they're built?
			List<Future<ScheduleResult>> threadResults = null;
			try {
				threadResults = mScheduleExecutor.invokeAll(threadTasks);
			} catch (InterruptedException e) {
				Log.error(TAG, "MAIN THREAD INTERRUPTED WHILE WAITING FOR SCHEDULE TAKS TO COMPLETE");
				e.printStackTrace();
				System.exit(1);
			}
			
			// Examine all scheduling results, and find the most optimal
			for(Future<ScheduleResult> f : threadResults) {
				ScheduleResult curResult = null;
				try {
					curResult = f.get();
				} catch (InterruptedException e) {
					Log.error(TAG, "MAIN THREAD INTERRUPTED WHILE EVALUATING SCHEDULE RESULTS");
					e.printStackTrace();
					System.exit(1);
				} catch (ExecutionException e) {
					Log.error(TAG, "MAIN THREAD INTERRUPTED WHILE EVALUATING SCHEDULE RESULTS");
					e.printStackTrace();
					System.exit(1);
				}
				if(curResult.mSolutionFound) {
					if(optimalScheduling == null || curResult.mOptimalScore < optimalScheduling.mOptimalScore)
						optimalScheduling = curResult;
				}
			}
			
			if(optimalScheduling != null) {
				// Do the scheduling if a feasible result has been found
				Vehicle optimalVehicle = plan[optimalScheduling.mVehicleIndex];
				ArrayList<VehicleScheduleJob> optimalSchedule = optimalVehicle.getSchedule();
				optimalSchedule.add(optimalScheduling.mOptimalPickupIndex, pickupJob);
				optimalSchedule.add(optimalScheduling.mOptimalDropoffIndex, dropoffJob);
				updateServiceTimes(optimalSchedule, mRouter);
				
				Log.info(TAG, "   SCHEDULED. Trip " + t.getIdentifier() + ". Vehicle: " + optimalVehicle.getIdentifier() + 
							". Pickup index: " + optimalScheduling.mOptimalPickupIndex + 
							". Dropoff index: " + optimalScheduling.mOptimalDropoffIndex);
				scheduleSuccessful = true;
			}
		} else {
			scheduleSuccessful = true;
		}
		return scheduleSuccessful;
	}
	
	/**
	 * TODO: Do this during scheduling so that we don't have to pathfind for the same routes twice
	 * Updates the service times of each job in this schedule
	 * @param schedule
	 */
	public static void updateServiceTimes(ArrayList<VehicleScheduleJob> schedule, Routefinder router) {
		int curTime = 0;
		Point2D lastLoc = null;
		for(int i = 0; i < schedule.size(); i++) {
			VehicleScheduleJob curJob = schedule.get(i);
			int type = curJob.getType();
			// For now, skip start and end jobs
			if(type == VehicleScheduleJob.JOB_TYPE_START || type == VehicleScheduleJob.JOB_TYPE_END)
				continue;
	
			// Initialize location and time
			if(lastLoc == null) {
				lastLoc = curJob.getTrip().getFirstEndpoint();
				curTime = curJob.getStartTime();
				curJob.setServiceTime(curTime);
			} else {
				// Set current location to pickup or dropoff coordinates
				Point2D curLoc;
				if(type == VehicleScheduleJob.JOB_TYPE_PICKUP)
					curLoc = curJob.getTrip().getFirstEndpoint();
				else 
					curLoc = curJob.getTrip().getSecondEndpoint();
	
				// Calculate time to travel from last point to here
				int lastLegSec = router.getTravelTimeSec(lastLoc, curLoc);
				// Add to current time, and set job's schedule service time
				curTime += lastLegSec / 60;
				// Don't service pickup jobs early
				if(curTime < curJob.getStartTime()) {
					curTime = curJob.getStartTime();
				}
				curJob.setServiceTime(curTime);

				// Update location
				lastLoc = curLoc;
			}
		}
	}
	
	// *************************************************
	//                 COST FUNCTIONS
	//  Cost functions assign a difficulty value to 
	//  each new trip. We will therefore schedule the 
	//  high priority (higher cost) trips first.
	// *************************************************
	
	/**
	 * Calculate the trip cost
	 * @param t Trip for which to calculate cost
	 * @return The trip cost
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
			
			// We could run in to round-down errors if we just return the cost difference
			// e.g 0.4d -> 0 if returned as an int
			if (result < 0) compareVal = 1;
			else if(result > 0) compareVal = -1;
			else compareVal = 0;
			
			return compareVal;
		}
	}
}
