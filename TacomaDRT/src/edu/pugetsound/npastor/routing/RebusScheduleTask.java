package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

/**
 * This Runnable class checks the feasibility of scheduling a trip in a vehicle schedule, using the 
 * REBUS algorithm. Because REBUS requires that every trip be evaluated in every vehicle, we can 
 * parallelize the process by delegating work related to different vehicles to different worker 
 * threads. This class represents one of those threads.
 * 
 * @author Nathan Pastor
 */
public class RebusScheduleTask implements Runnable {
	
	private static final String TAG = "RebusScheduleTask";
	
	private LoadCost mLoadCost;
	private Vehicle mVehicle;
	private int mVehiclePlanIndex;
	private VehicleScheduleJob mPickupJob;
	private VehicleScheduleJob mDropoffJob;
	private ArrayList<VehicleScheduleJob> mSchedule;
	private ScheduleResult[] mResults;
	private CountDownLatch mLatch;
	private RouteCache mCache;
	
	public RebusScheduleTask (int vehicleIndex, Vehicle vehicle, ArrayList<VehicleScheduleJob> schedule,
							RouteCache cache, VehicleScheduleJob pickupJob, 
							VehicleScheduleJob dropoffJob, ScheduleResult[] results, CountDownLatch latch) {
		mVehiclePlanIndex = vehicleIndex;
		mPickupJob = pickupJob;
		mDropoffJob = dropoffJob;
		mSchedule = schedule;
		mVehicle = vehicle;
		mResults = results;
		mLatch = latch;		
		mCache = cache;
		mLoadCost = new LoadCost(mVehiclePlanIndex, mCache);
	}

	/**
	 * Execute this thread
	 */
	public void run() {
		ScheduleResult result = evaluateTripInVehicle();
		mResults[mVehiclePlanIndex] = result;
		// This task's work is done: decrement the counter
		mLatch.countDown();
	}
	
	/**
	 * Evaluates this instance's trip (mPickupJob and mDropoffJob) in the given schedule. This will NOT schedule the jobs.
	 * @return A ScheduleResult object containing the results of the evaluation. If multiple feasible solution were found, this will
	 *         contain the solution that disturbs the objective function the least (minimizes mJobCost). If no feasible solution was
	 *         found, mSolutionFound will be set to false
	 */
	private ScheduleResult evaluateTripInVehicle() {
		
		// Initialize the result
		ScheduleResult schedResult = new ScheduleResult(mVehiclePlanIndex);
		
		// Fail immediately if vehicle is not in service at time of pickup job
		if(!mVehicle.isServiceableTime(mPickupJob.getStartTime()))
			return schedResult;
		
		int pickupIndex = 1; //s1 in Madsen's notation
		int dropoffIndex = 2; //s2 in Madsen's notation

		// FOLLOWING COMMENTS (except those in parenthesis) ARE MADSEN'S REBUS PSEUDO-CODE
		// Step 1: Place s1, s2 just after this first stop T0 in the mSchedule, and update the mSchedule
		mSchedule.add(pickupIndex, mPickupJob);
		mSchedule.add(dropoffIndex, mDropoffJob);
		
		// Step 2: While all insertions have not been evaluated, do
		boolean isFirstEval = true;
		outerloop:
		while(pickupIndex < mSchedule.size() - 2) {
			if(isFirstEval) {
				isFirstEval = false;
			//  a) if s2 is before the last stop T1 in the schedule...
			} else if(mSchedule.get(dropoffIndex+1).getType() == VehicleScheduleJob.JOB_TYPE_END) {
//				Log.info(TAG, " --- FIRST");
				// then move s1 one step to the right...
				mSchedule.remove(dropoffIndex); // (remove so we don't swap pickup/dropoff order)
				mSchedule.set(pickupIndex, mSchedule.get(pickupIndex+1));
				pickupIndex++;
				mSchedule.set(pickupIndex, mPickupJob);
				// and place s2 just after s1 and update the schedule. Go to 2(b).
				dropoffIndex = pickupIndex + 1;
				mSchedule.add(dropoffIndex, mDropoffJob);
			//     else, move s2 one step to the right
			} else {
//				Log.info(TAG, " --- SECOND");
				mSchedule.set(dropoffIndex, mSchedule.get(dropoffIndex+1)); // (swap elements)
				dropoffIndex++;
				mSchedule.set(dropoffIndex, mDropoffJob);
			}
			
			// b) Check for feasibility.
			boolean potentiallyFeasible = true;			
			while(potentiallyFeasible) {
				// (ensure this is a valid schedule ordering. Trip related jobs cannot be last in schedule)
				if(dropoffIndex == mSchedule.size() - 1 || pickupIndex == mSchedule.size() - 1) {
//					Log.info(TAG, "-------BREAKING ON INDEX TOO HIGH");
					break;
				}
				FeasibilityResult feasResult = checkScheduleFeasibility(mSchedule, mVehiclePlanIndex);
				int feasCode = feasResult.resultCode;
				VehicleScheduleJob failsOn = feasResult.failsOn; // The job the test failed on
				
				//  i. if the insertion is feasible...
				if(feasCode == FeasibilityResult.SUCCESS) {
					// then calculate the change in the objective and compare to the previously found insertions
					double objectiveFunc = mLoadCost.calculateObjFunc(mSchedule);
					
					// PRINT STUFF
//					String str = "Trip " + mPickupJob.getTrip().getIdentifier() + " success, veh " + mVehiclePlanIndex + ", objective func is " + objectiveFunc + ". "  + pickupIndex + ", " +  dropoffIndex;// + "\n";
//					for(int i = 0; i < mSchedule.size(); i++) {
//						VehicleScheduleJob job = mSchedule.get(i);
//						str += job.toString(mVehiclePlanIndex);
//						if(i != mSchedule.size()-1) str += "\n";
//					}
//					Log.iln(TAG, str);
					// END PRINT STUFF
					
					if(objectiveFunc < schedResult.mOptimalScore || schedResult.mSolutionFound == false) {
						schedResult.mOptimalPickupIndex = pickupIndex;
						schedResult.mOptimalDropoffIndex = dropoffIndex;
						schedResult.mOptimalScore = objectiveFunc;
						schedResult.mSolutionFound = true;
					}
//					Log.info(TAG, "-------BREAKING ON SUCCESS");
					potentiallyFeasible = false; // (No longer potentially feasible, schedule is certainly feasible)
				// ii. If the insertion is not feasible, check for the following situations:
				} else {
					// A. if the capacity constraints, the maximum travel time or the time window
					//    related to s2 have been violated...
					if(feasCode == FeasibilityResult.FAIL_MAX_TRAVEL_TIME && 
							failsOn.getTrip().getIdentifier() == mPickupJob.getTrip().getIdentifier()) {
//						Log.info(TAG, " --- THIRD");
						// then move s1 one step to the right...
						mSchedule.remove(dropoffIndex); // (remove so we don't swap pickup/dropoff order)
						mSchedule.set(pickupIndex, mSchedule.get(pickupIndex+1));
						pickupIndex++;
						mSchedule.set(pickupIndex, mPickupJob);
						// and place s2 just after s1 and update the schedule. Go to 2(b).
						dropoffIndex = pickupIndex + 1;
						mSchedule.add(dropoffIndex, mDropoffJob);
					// B. if the time window related to s1 is violated then stop
					} else if(feasCode == FeasibilityResult.FAIL_WINDOW && 
							failsOn.getTrip().getIdentifier() == mPickupJob.getTrip().getIdentifier()) {
//						Log.info(TAG, "-------BREAKING ON WINDOW");
						break outerloop;
					// C. else, go to 2
					} else {
//						Log.info(TAG, "-------BREAKING ON ELSE. result code " + feasResult.mResultCode + " fails on " + 
//								(feasResult.mFailsOn.getTrip() != null ? feasResult.mFailsOn.getTrip().getIdentifier() : feasResult.mFailsOn.getType()));
						potentiallyFeasible = false;
					}
				}
			}
		}
		return schedResult;
	}
	
	/**
	 * Checks the feasibility of the given schedule. A schedule will FAIL the feasibility test if a time window at
	 * any stop is not satisfied, if the maximum travel time for any trip is exceeded, or if the vehicle capacity
	 * is exceeded at any point along its route. Otherwise, it will succeed.
	 * @param schedule The schedule for which to check feasibility
	 * @return A FeasibilityResult object containing the result code (mResultCode). In the case of a failure, this
	 *         result also includes the job (mFailsOn) that the result code applies to. 
	 */
	private FeasibilityResult checkScheduleFeasibility(ArrayList<VehicleScheduleJob> schedule, int vehicleNum) {
		
		int numPassengers = 0;
		FeasibilityResult result = new FeasibilityResult();
		
		Rebus.updateServiceTimes(schedule, mCache, vehicleNum);
		
		// If soft constraints are enabled, all schedules pass the feasibility check
		if(Rebus.isSettingEnabled(Rebus.SOFT_CONSTRAINTS)) {
			result.resultCode = FeasibilityResult.SUCCESS;
			return result;
		}
		
//		String str = "Checking schedule feasibility: \n";
//		for(int i = 0; i < schedule.size(); i++) {
//			VehicleScheduleJob job = schedule.get(i);
//			str += job.toString();
//			if(i != schedule.size()-1) str += "\n";
//		}
//		Log.iln(TAG, str);
		
		for(int i = 0; i < schedule.size(); i++) {
			VehicleScheduleJob curJob = schedule.get(i);
			int type = curJob.getType();
			// For pickup jobs we can test the vehicle capacity and pickup window constraints
			if(type == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				numPassengers++;
				// Check if vehicle capacity has been exceeded
				if(numPassengers > Vehicle.VEHICLE_CAPACITY) {
					result.failsOn = curJob;
					result.resultCode = FeasibilityResult.FAIL_CAPACITY;
					break;
				}
				// Check if pickup window is satisfied
				// If current time exceeds the max pickup window, fail the feasibility test
				if(curJob.getWorkingServiceTime(mVehiclePlanIndex) > curJob.getStartTime() + Constants.PICKUP_SERVICE_WINDOW) {
					result.failsOn = curJob;
					result.resultCode = FeasibilityResult.FAIL_WINDOW;
					break;
				}
			// For dropoff jobs we can test the maximal travel time constraint
			} else if(type == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				numPassengers--;

				// Get the travel time between last location and here
				int totalTripTravelTime = curJob.getWorkingServiceTime(mVehiclePlanIndex) 
						- curJob.getCorrespondingJob().getWorkingServiceTime(mVehiclePlanIndex);
				
				// If the total trip travel time exceeds the max allowable trip travel time,
				// fail the feasibility test.
				if(totalTripTravelTime > maxTravelTime(curJob.getTrip())) {
					result.failsOn = curJob;
					result.resultCode = FeasibilityResult.FAIL_MAX_TRAVEL_TIME;
					break;
				}
			}
		}
		Log.d(TAG, "Returning feasibility code: " + result.resultCode + "\n");
		return result;
	}
	
	
	
	/**
	 * Calculates the maximum allowable travel time for this trip
	 * @param t The trip
	 * @return The max allowable travel time for this trip in minutes
	 */
	private float maxTravelTime(Trip t) {
		int timeMins = (int) t.getRoute().getTime() / 60;
		return timeMins * Rebus.MAX_TRAVEL_COEFF;
	}
	
	/**
	 * Wrapper class which contains the result of checkFeasibility() function
	 * @author Nathan P
	 */
	public class FeasibilityResult {
		
		public static final int FAIL_CAPACITY = 0; // Vehicle over capacity
		public static final int FAIL_MAX_TRAVEL_TIME = 1; // Excessive trip time
		public static final int FAIL_WINDOW = 2; // Pickup too late
		public static final int FAIL_EARLY_SERVICE = 4; // Pickup too early
		public static final int SUCCESS = 3;
		
		public VehicleScheduleJob failsOn;
		public int resultCode;
		
		public FeasibilityResult() {
			failsOn = null;
			resultCode = SUCCESS;
		}
	}
	
	/**
	 * Wrapper class which contains the result of a RebusScheduleTask
	 * @author Nathan P
	 *
	 */
	public class ScheduleResult {
		public int mVehicleIndex;
		public boolean mSolutionFound;
		public double mOptimalScore;
		public int mOptimalPickupIndex;
		public int mOptimalDropoffIndex;
		
		public ScheduleResult(int vehiclePlanIndex) {
			mVehicleIndex = vehiclePlanIndex;
			mSolutionFound = false;
			mOptimalScore = 0;
		}
	}
}
