package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

/**
 * This Runnable class checks the feasibility of scheduling a trip in a vehicle schedule.
 * Because REBUS requires that every trip be evaluated in every vehicle, we can easily parallelize 
 * the process by delegating work related to different vehicles to different worker threads. This
 * class represents one of those threads.
 * @author Nathan P
 *
 */
public class RebusScheduleTask implements Callable<ScheduleResult> {
	
	private static final String TAG = "RebusScheduleTask";
	
	private int mVehiclePlanIndex;
	private VehicleScheduleJob mPickupJob;
	private VehicleScheduleJob mDropoffJob;
	private Routefinder mRouter;
	ArrayList<VehicleScheduleJob> mSchedule;
	
	public RebusScheduleTask (int vehicleIndex, ArrayList<VehicleScheduleJob> schedule, VehicleScheduleJob pickupJob, VehicleScheduleJob dropoffJob) {
		mVehiclePlanIndex = vehicleIndex;
		mPickupJob = pickupJob;
		mDropoffJob = dropoffJob;
		mSchedule = schedule;
		mRouter = new Routefinder();
	}

	public ScheduleResult call() {
		return evaluateTripInVehicle(mPickupJob, mDropoffJob, mSchedule, mVehiclePlanIndex);
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
	private ScheduleResult evaluateTripInVehicle(VehicleScheduleJob pickupJob, VehicleScheduleJob dropoffJob, 
			ArrayList<VehicleScheduleJob> schedule, int vehicleIndex) {
		
		// Initialize the result
		ScheduleResult schedResult = new ScheduleResult();
		schedResult.mSolutionFound = false;
		schedResult.mVehicleIndex = vehicleIndex;
		
		int pickupIndex = 1; //s1 in Madsen's notation
		int dropoffIndex = 2; //s2 in Madsen's notation
		
		// FOLLOWING COMMENTS (except parenthetical comments) ARE MADSEN'S REBUS PSEUDO-CODE
		// Step 1: Place s1, s2 just after this first stop T0 in the schedule, and update the schedule
		schedule.add(pickupIndex, pickupJob);
		schedule.add(dropoffIndex, dropoffJob);
		
		// Step 2: While all insertions have not been evaluated, do
		boolean isFirstEval = true;
		outerloop:
		while(pickupIndex < schedule.size() - 2) {
			if(isFirstEval) {
				isFirstEval = false;
			//  a) if s2 is before the last stop T1 in the schedule...
			} else if(schedule.get(dropoffIndex+1).getType() == VehicleScheduleJob.JOB_TYPE_END) {
//				Log.info(TAG, " --- FIRST");
				// then move s1 one step to the right...
				schedule.remove(dropoffIndex); // Remove so we don't swap pickup/dropoff order
				schedule.set(pickupIndex, schedule.get(pickupIndex+1)); // (swap elements, save time!)
				pickupIndex++;
				schedule.set(pickupIndex, pickupJob);
				// and place s2 just after s1 and update the schedule. Go to 2(b).
				dropoffIndex = pickupIndex + 1;
				schedule.add(dropoffIndex, dropoffJob);
			//     else, move s2 one step to the right
			} else {
//				Log.info(TAG, " --- SECOND");
				schedule.set(dropoffIndex, schedule.get(dropoffIndex+1)); // (swap elements)
				dropoffIndex++;
				schedule.set(dropoffIndex, dropoffJob);
			}
			
			// b) Check for feasibility.
			boolean potentiallyFeasible = true;			
			while(potentiallyFeasible) {
				// (ensure this is a valid schedule ordering. Trip related jobs cannot be last schedule)
				if(dropoffIndex == schedule.size() - 1 || pickupIndex == schedule.size() - 1) {
//					Log.info(TAG, "-------BREAKING ON INDEX TOO HIGH");
					break;
				}
				FeasibilityResult feasResult = checkScheduleFeasibility(schedule);
				int feasCode = feasResult.mResultCode;
				VehicleScheduleJob failsOn = feasResult.mFailsOn; // The job the test failed on
				//  i. if the insertion is feasible...
				if(feasCode == FeasibilityResult.SUCCESS) {
					// then calculate the change in the objective and compare to the previously found insertions
					double objectiveFunc = calculateObjFunc(schedule);
//					Log.info(TAG, "success, objective func is " + objectiveFunc);
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
							failsOn.getTrip().getIdentifier() == pickupJob.getTrip().getIdentifier()) {
//						Log.info(TAG, " --- THIRD");
						// then move s1 one step to the right...
						schedule.remove(dropoffIndex); // Remove so we don't swap pickup/dropoff order
						schedule.set(pickupIndex, schedule.get(pickupIndex+1)); // (swap elements, save time!)
						pickupIndex++;
						schedule.set(pickupIndex, pickupJob);
						// and place s2 just after s1 and update the schedule. Go to 2(b).
						dropoffIndex = pickupIndex + 1;
						schedule.add(dropoffIndex, dropoffJob);
					// B. if the time window related to s1 is violated then stop
					} else if(feasCode == FeasibilityResult.FAIL_WINDOW && 
							failsOn.getTrip().getIdentifier() == pickupJob.getTrip().getIdentifier()) {
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
	 * @return The job that the feasibility check fails on, null if schedule passes feasibility
	 */
	private FeasibilityResult checkScheduleFeasibility(ArrayList<VehicleScheduleJob> schedule) {
		Rebus.updateServiceTimes(schedule, mRouter);
//		for(int i = 0; i < schedule.size(); i++) {
//			VehicleScheduleJob job = schedule.get(i);
//			Log.info(TAG, job.toString());
//		}
		
		int numPassengers = 0;
		FeasibilityResult result = new FeasibilityResult();
		for(int i = 0; i < schedule.size(); i++) {
			VehicleScheduleJob curJob = schedule.get(i);
			int type = curJob.getType();
			// For pickup jobs we can test the vehicle capacity and pickup window constraints
			if(type == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				numPassengers++;
				// Check if vehicle capacity has been exceeded
				if(numPassengers > Vehicle.VEHICLE_CAPACITY) {
					result.mFailsOn = curJob;
					result.mResultCode = FeasibilityResult.FAIL_CAPACITY;
					break;
				}
				// Check if pickup window is satisfied
				// If current time exceeds the max pickup window, fail the feasibility test
				if(curJob.getServiceTime() > curJob.getStartTime() + Constants.PICKUP_SERVICE_WINDOW) {
					result.mFailsOn = curJob;
					result.mResultCode = FeasibilityResult.FAIL_WINDOW;
					break;
				}
			// For dropoff jobs we can test the maximal travel time constraint
			} else if(type == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				numPassengers--;

				// Get the travel time between last location and here
				int totalTripTravelTime = curJob.getServiceTime() - findCorrespondingJob(curJob, schedule).getServiceTime();
				
				// If the total trip travel time exceeds the max allowable trip travel time,
				// fail the feasibility test.
				if(totalTripTravelTime > maxTravelTime(curJob.getTrip())) {
					result.mFailsOn = curJob;
					result.mResultCode = FeasibilityResult.FAIL_MAX_TRAVEL_TIME;
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Calculates the objective function for the specified schedule.
	 * Lower scores are more desirable
	 * @param schedule The schedule to consider
	 * @return The objective function score of the specified schedule
	 */
	private double calculateObjFunc(ArrayList<VehicleScheduleJob> schedule) {
		double objectiveFunction = 0;
		int passengers = 0;
		
		for(int i = 0; i < schedule.size(); i++) {
			VehicleScheduleJob curJob = schedule.get(i);
			if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				// Increment passengers and update total objective function
				passengers++;
				objectiveFunction += getLoad(curJob, schedule, passengers);
			} else if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				// Decrement passengers and update total objective function
				passengers--;	
				objectiveFunction += getLoad(curJob, schedule, passengers);
			}
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
	public double getLoad(VehicleScheduleJob job, ArrayList<VehicleScheduleJob> schedule, int passengers) {
		return loadDrivingTime(job, schedule) +
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
	private double loadDrivingTime(VehicleScheduleJob job, ArrayList<VehicleScheduleJob> schedule) {
		Trip t = job.getTrip();
		int minDrivingTimeMins = (int)t.getRoute().getTime() / 60;
		int waitingTime;
		if(job.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP)
			waitingTime = job.getServiceTime() - job.getStartTime();
		else {
			VehicleScheduleJob corJob = findCorrespondingJob(job, schedule);
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
			cost = Rebus.WAIT_C2 * Math.pow(waitingTime, 2) + Rebus.WAIT_C1 * waitingTime;
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
		double cost = Rebus.DEV_C * Math.pow(deviation, 2);
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
		
		double cost = Rebus.CAPACITY_C * Math.pow(free, 2);
		
		return cost;
	}
	
	/**
	 * Finds the corresponding job. Pickup and dropoff jobs for the same trip are considered corresponding.
	 * @param job Job to find corresponding pair for
	 * @return The corresponding job
	 */
	private VehicleScheduleJob findCorrespondingJob(VehicleScheduleJob job, ArrayList<VehicleScheduleJob> jobs) {
		//For now, assume this is a pickup/dropoff
		int keyId = job.getTrip().getIdentifier();
		VehicleScheduleJob correspondingJob = null;
		for(VehicleScheduleJob j : jobs) {
			Trip t = j.getTrip();
			if(t != null) {
				if(t.getIdentifier() == keyId && j.getType() != job.getType()) {
					correspondingJob = j;
					break;
				}
			}
		}
		if(correspondingJob == null) 
			Log.error(TAG, "No corresponding job found for job type " +
					job.getType() + ", trip " + job.getTrip().getIdentifier());
		return correspondingJob;
	}
	
	// Wrapper class which contains the result of checkFeasibility() function
	public class FeasibilityResult {
		
		public static final int FAIL_CAPACITY = 0; // Vehicle over capacity
		public static final int FAIL_MAX_TRAVEL_TIME = 1; // Excessive trip time
		public static final int FAIL_WINDOW = 2; // Pickup too late
		public static final int FAIL_EARLY_SERVICE = 4; // Pickup too early
		public static final int SUCCESS = 3;
		
		public VehicleScheduleJob mFailsOn;
		public int mResultCode;
		
		public FeasibilityResult() {
			mFailsOn = null;
			mResultCode = SUCCESS;
		}
	}
}
