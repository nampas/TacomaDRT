package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

/**
 * This Callable class checks the feasibility of scheduling a trip in a vehicle schedule, using the 
 * REBUS algorithm. Because REBUS requires that every trip be evaluated in every vehicle, we can 
 * parallelize the process by delegating work related to different vehicles to different worker 
 * threads. This class represents one of those threads.
 * 
 * @author Nathan Pastor
 */
public class RebusScheduleTask implements Callable<ScheduleResult> {
	
	private static final String TAG = "RebusScheduleTask";
	
	private int mVehiclePlanIndex;
	private VehicleScheduleJob mPickupJob;
	private VehicleScheduleJob mDropoffJob;
	private Routefinder mRouter;
	VehicleScheduleNode mScheduleRoot;
	
	public RebusScheduleTask (int vehicleIndex, VehicleScheduleNode scheduleRoot, VehicleScheduleJob pickupJob, VehicleScheduleJob dropoffJob) {
		mVehiclePlanIndex = vehicleIndex;
		mPickupJob = pickupJob;
		mDropoffJob = dropoffJob;
		mScheduleRoot = scheduleRoot;
		mRouter = new Routefinder();
	}

	/**
	 * Similar to a Runnable's run(), this method begins thread operations
	 */
	public ScheduleResult call() {
		Log.d(TAG, "Executing in vehicle thread " + mVehiclePlanIndex);
		return evaluateTripInVehicle();
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
		
		// FOLLOWING COMMENTS (except those in parenthesis) ARE MADSEN'S REBUS PSEUDO-CODE
		// Step 1: Place s1, s2 just after this first stop T0 in the mSchedule, and update the schedule
		VehicleScheduleNode pickupNode = new VehicleScheduleNode(mPickupJob, null, null);
		VehicleScheduleNode dropoffNode = new VehicleScheduleNode(mDropoffJob, null, null);
		
//		Log.d(TAG, "BEFORE INITIAL INSERTION \n" + VehicleScheduleNode.getListString(mScheduleRoot));
		VehicleScheduleNode.setNext(mScheduleRoot, pickupNode);
		VehicleScheduleNode.setNext(pickupNode, dropoffNode);
		
//		Log.d(TAG, "AFTER INITIAL INSERTION \n" + VehicleScheduleNode.getListString(mScheduleRoot));
		
		// Step 2: While all insertions have not been evaluated, do
		boolean isFirstEval = true;
		outerloop:
		while(pickupNode.getNext().getNext() != null) {
//			Log.d(TAG, "in while");
			if(isFirstEval) {
				isFirstEval = false;
			//  a) if s2 is before the last stop T1 in the mSchedule...
			} else if(dropoffNode.getNext().getJob().getType() == VehicleScheduleJob.JOB_TYPE_END) {
//				Log.info(TAG, " --- FIRST");
				// then move s1 one step to the right...
				VehicleScheduleNode.remove(dropoffNode);
				VehicleScheduleNode s1Next = pickupNode.getNext();
				VehicleScheduleNode.remove(pickupNode);
				VehicleScheduleNode.setNext(s1Next, pickupNode);
				// and place s2 just after s1 and update the mSchedule. Go to 2(b).
				VehicleScheduleNode.setNext(pickupNode, dropoffNode);
			//     else, move s2 one step to the right
			} else {
//				Log.info(TAG, " --- SECOND");
				VehicleScheduleNode s2Next = dropoffNode.getNext();
				VehicleScheduleNode.remove(dropoffNode);
				VehicleScheduleNode.setNext(s2Next, dropoffNode);
			}
			
			// b) Check for feasibility.
			boolean potentiallyFeasible = true;			
			while(potentiallyFeasible) {
				// (ensure this is a valid schedule ordering. Trip related jobs cannot be last in schedule)
				if(!dropoffNode.hasNext() || !pickupNode.hasNext()) {
//					Log.info(TAG, "-------BREAKING ON INDEX TOO HIGH");
					break;
				}
				FeasibilityResult feasResult = checkScheduleFeasibility(mScheduleRoot, pickupNode);
				int feasCode = feasResult.mResultCode;
				//  i. if the insertion is feasible...
				if(feasCode == FeasibilityResult.SUCCESS) {
					// then calculate the change in the objective and compare to the previously found insertions
					double objectiveFunc = calculateObjFunc(mScheduleRoot);
					Log.d(TAG, "Feasible solution found, objective func is " + objectiveFunc);
					if(objectiveFunc < schedResult.mOptimalScore || schedResult.mSolutionFound == false) {
						schedResult.mOptimalPickupIndex = VehicleScheduleNode.getIndex(mScheduleRoot, pickupNode);
						schedResult.mOptimalDropoffIndex = VehicleScheduleNode.getIndex(mScheduleRoot, dropoffNode);
						schedResult.mOptimalScore = objectiveFunc;
						schedResult.mSolutionFound = true;
					}
//					Log.info(TAG, "-------BREAKING ON SUCCESS");
					potentiallyFeasible = false; // (No longer potentially feasible, mSchedule is certainly feasible)
				
				// ii. If the insertion is not feasible, check for the following situations:
				} else {
					
					// A. if the capacity constraints, the maximum travel time or the time window
					//    related to s2 have been violated...
					if(feasCode == FeasibilityResult.FAIL_MAX_TRAVEL_TIME && 
							feasResult.mFailsOnId == mPickupJob.getTrip().getIdentifier()) {
//						Log.info(TAG, " --- THIRD");
						// then move s1 one step to the right...
						VehicleScheduleNode.remove(dropoffNode);
						VehicleScheduleNode s1Next = pickupNode.getNext();
						VehicleScheduleNode.remove(pickupNode);
						VehicleScheduleNode.setNext(s1Next, pickupNode);
						// and place s2 just after s1 and update the schedule. Go to 2(b).
						VehicleScheduleNode.setNext(pickupNode, dropoffNode);
					// B. if the time window related to s1 is violated then stop
					} else if(feasCode == FeasibilityResult.FAIL_WINDOW && 
							feasResult.mFailsOnId == mPickupJob.getTrip().getIdentifier()) {
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
	 * @param pickupIndex The index of the pickup job
	 * @return A FeasibilityResult object containing the result code (mResultCode). In the case of a failure, this
	 *         result also includes the job (mFailsOn) that the result code applies to. 
	 */
	private FeasibilityResult checkScheduleFeasibility(VehicleScheduleNode scheduleRoot, VehicleScheduleNode updateFrom) {
		Rebus.updateServiceTimes(updateFrom, mRouter);

		Log.d(TAG, "Checking schedule feasibility\n" + VehicleScheduleNode.getListString(scheduleRoot));
		
		int numPassengers = 0;
		FeasibilityResult result = new FeasibilityResult();
		VehicleScheduleNode curNode = scheduleRoot;
		while(curNode != null) {
			VehicleScheduleJob curJob = curNode.getJob();
			int type = curJob.getType();
			// For pickup jobs we can test the vehicle capacity and pickup window constraints
			if(type == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				numPassengers++;
				// Check if vehicle capacity has been exceeded
				if(numPassengers > Vehicle.VEHICLE_CAPACITY) {
					result.mFailsOnId = curJob.getTrip().getIdentifier();
					result.mResultCode = FeasibilityResult.FAIL_CAPACITY;
					break;
				}
				// Check if pickup window is satisfied
				// If current time exceeds the max pickup window, fail the feasibility test
				if(curJob.getServiceTime() > curJob.getStartTime() + Constants.PICKUP_SERVICE_WINDOW) {
					result.mFailsOnId = curJob.getTrip().getIdentifier();
					result.mResultCode = FeasibilityResult.FAIL_WINDOW;
					break;
				}
			// For dropoff jobs we can test the maximal travel time constraint
			} else if(type == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				numPassengers--;

				// Get the travel time between last location and here
				int totalTripTravelTime = curJob.getServiceTime() - findCorrespondingJob(curJob, scheduleRoot).getServiceTime();
				
				// If the total trip travel time exceeds the max allowable trip travel time,
				// fail the feasibility test.
				if(totalTripTravelTime > maxTravelTime(curJob.getTrip())) {
					result.mFailsOnId = curJob.getTrip().getIdentifier();
					result.mResultCode = FeasibilityResult.FAIL_MAX_TRAVEL_TIME;
					break;
				}
			}
			// Move to next node
			curNode = curNode.getNext();
		}
		Log.d(TAG, "Returning schedule feas code " + result.mResultCode);
		return result;
	}
	
	/**
	 * Calculates the objective function for the specified schedule.
	 * Lower scores are more desirable
	 * @param schedule The schedule to consider
	 * @return The objective function score of the specified schedule
	 */
	private double calculateObjFunc(VehicleScheduleNode scheduleRoot) {
		double objectiveFunction = 0;
		int passengers = 0;
		
		VehicleScheduleNode curNode = scheduleRoot;
		while(curNode != null) {
			VehicleScheduleJob curJob = curNode.getJob();
			if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				// Increment passengers and update total objective function
				passengers++;
				objectiveFunction += getLoad(curJob, scheduleRoot, passengers);
			} else if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				// Decrement passengers and update total objective function
				passengers--;	
				objectiveFunction += getLoad(curJob, scheduleRoot, passengers);
			}
			
			// Update the current node
			curNode = curNode.getNext();
		}
		return objectiveFunction;
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
	 * TODO: optimize by searching from the job's node outwards, depending 
	 * Finds the corresponding job. Pickup and dropoff jobs for the same trip are considered corresponding.
	 * @param job Job to find corresponding pair for
	 * @return The corresponding job
	 */
	private VehicleScheduleJob findCorrespondingJob(VehicleScheduleJob keyJob, VehicleScheduleNode scheduleRoot) {
		//For now, assume this is a pickup/dropoff
		int keyId = keyJob.getTrip().getIdentifier();
		VehicleScheduleJob correspondingJob = null;
		VehicleScheduleNode curNode = scheduleRoot;
		while(curNode != null) {
			VehicleScheduleJob j = curNode.getJob();
			if(j.getTrip() != null) {
				if(j.getTrip().getIdentifier() == keyId && j.getType() != keyJob.getType()) {
					correspondingJob = j;
					break;
				}
			}
			
			// Update current node
			curNode = curNode.getNext();
		}
		if(correspondingJob == null) 
			Log.error(TAG, "No corresponding job found for job type " +
					keyJob.getType() + ", trip " + keyJob.getTrip().getIdentifier());
		return correspondingJob;
	}
	
	// ********************************************
	//               LOAD FUNCTIONS
	//  Load functions estimate the feasibility of 
	//  inserting a new job into an existing plan.
	// ********************************************
	
	/**
	 * Calculates the load of the job at its current location in schedule
	 * @param j The job to evaluate
	 * @param passengers Number of passengers in the vehicle
	 * @return The load cost for this stop (the specified job)
	 */
	public double getLoad(VehicleScheduleJob job, VehicleScheduleNode scheduleRoot, int passengers) {
		return loadDrivingTime(job, scheduleRoot) +
				loadWaitingTime(job) +
				loadDesiredServiceTimeDeviation(job) +
				loadCapacityUtilization(job, passengers);
	}
	
	/**
	 * Calculates the driving time component of the load value
	 * Madsen notation: Cvariable * Tdr_time + Cconstant * (Twait + Chandle)
	 * @param job Job to evaluate
	 * @return The driving time laod cost for this stop (the specified job)
	 */
	private double loadDrivingTime(VehicleScheduleJob job, VehicleScheduleNode scheduleRoot) {
		Trip t = job.getTrip();
		int minDrivingTimeMins = (int)t.getRoute().getTime() / 60;
		int waitingTime;
		if(job.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP)
			waitingTime = job.getServiceTime() - job.getStartTime();
		else {
			VehicleScheduleJob corJob = findCorrespondingJob(job, scheduleRoot);
			waitingTime = corJob.getServiceTime() - corJob.getStartTime();
		}
		
		// TODO: WHAT IS HANDLING TIME???? (0.0)
		double cost = Rebus.DR_TIME_C1 * minDrivingTimeMins + Rebus.DR_TIME_C2 * (waitingTime + 0.0);
		
		return cost;
	}
	
	/**
	 * Calculates the waiting time component of the load value
	 * Madsen notation: C2wait * Twait^2 + C1wait * Twait
	 * @param job Job to evaluate
	 * @return The waiting time load cost for this stop (the specified job)
	 */
	private double loadWaitingTime(VehicleScheduleJob job) {
		double cost = 0;
		//TODO: should this only apply to pickup jobs?
		if(job.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP) {
			int waitingTime = job.getServiceTime() - job.getStartTime();
			cost = Rebus.WAIT_C2 * (waitingTime * waitingTime) + Rebus.WAIT_C1 * waitingTime;
		}
		return cost;
	}
	
	/**
	 * Calculates the service time deviation component of the load value
	 * Madsen notation: Cdev * Tdev^2
	 * @param job Job to evaluate
	 * @return The deviation from desired service time load cost for this stop (the specified job)
	 */
	private double loadDesiredServiceTimeDeviation(VehicleScheduleJob job) {
		//TODO: what is deviation?
		int deviation = job.getServiceTime() - job.getStartTime();
		double cost = Rebus.DEV_C * (deviation * deviation);
		return cost;
	}
	
	/**
	 * Calculates the capacity utilization component of the load value
	 * Madsen notation: Ci * Vfreei^2
	 * @param job The job to evaulate
	 * @param passengers Number of passengers in vehicle
	 * @return The vehicle capacity utilization load cost for this stop (the specified job)
	 */
	private double loadCapacityUtilization(VehicleScheduleJob job, int passengers) {
		// Number of seats free
		int free = Vehicle.VEHICLE_CAPACITY - passengers;
		
		double cost = Rebus.CAPACITY_C * (free * free);
		
		return cost;
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
		
		public int mFailsOnId;
		public int mResultCode;
		
		public FeasibilityResult() {
			mFailsOnId = -1;
			mResultCode = SUCCESS;
		}
	}
}
