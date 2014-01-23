package edu.pugetsound.npastor.routing;

import java.util.ArrayList;

import edu.pugetsound.npastor.utils.Trip;

public class LoadCost {

	private int mVehiclePlanIndex;
	private RouteCache mCache;
	
	public LoadCost(int vehiclePlanIndex, RouteCache cache) {
		mVehiclePlanIndex = vehiclePlanIndex;
		mCache = cache;
	}
	
	/**
	 * Calculates the objective function for the specified schedule.
	 * Lower scores are more desirable
	 * @param schedule The schedule to consider
	 * @return The objective function score of the specified schedule
	 */
	public double calculateObjFunc(ArrayList<VehicleScheduleJob> schedule) {
		double objectiveFunction = 0;
		int passengers = 0;
		
		String msg = "";
		
		for(int i = 1; i < schedule.size() - 1; i++) {
			VehicleScheduleJob curJob = schedule.get(i);
			VehicleScheduleJob lastJob = schedule.get(i-1);
			int jobType = curJob.getType();
			
			// Update passenger count 
			if(jobType == VehicleScheduleJob.JOB_TYPE_PICKUP) {
				passengers++;			
			} else if(jobType == VehicleScheduleJob.JOB_TYPE_DROPOFF) {
				passengers--;	
			}
			
			// Update running total of the objective function
			double objectiveFuncInc = getJobLoad(curJob, 
					passengers, lastJob, schedule.size());
			msg += objectiveFuncInc + " ";
			objectiveFunction += objectiveFuncInc;
		}
		
//		Log.iln(TAG, msg);
		return objectiveFunction;
	}
	
	// ********************************************
	//             REBUS LOAD FUNCTIONS
	//  Load functions estimate the desirability of 
	//  inserting a new job into an existing plan.
	// ********************************************

	/**
	 * Calculates the load of the job at its current location in schedule
	 * @param j The job to evaluate
	 * @param schedule The schedule
	 * @param passengers Number of passengers in the vehicle
	 * @return The load cost for this stop (the specified job)
	 */
	private double getJobLoad(VehicleScheduleJob job, int passengers,
			VehicleScheduleJob lastJob, int scheduleSize) {
		// Calculate the REBUS load cost
		//			Log.iln(TAG, "Load cost for job type " + job.getType() + ", id " + job.getTrip().getIdentifier());
		double cost = loadDrivingTime(job) +
				loadWaitingTime(job) +
				loadDesiredServiceTimeDeviation(job) +
				loadCapacityUtilization(passengers);
		// Add in the vehicle utilization cost if enabled
		if(Rebus.isSettingEnabled(Rebus.FAVOR_BUSY_VEHICLES))
			cost -= loadVehicleUtilization(scheduleSize);
		if(Rebus.isSettingEnabled(Rebus.MINIMIZE_ROUTE_TIME))
			cost += loadTotalTime(lastJob, job, scheduleSize);

		return cost;
	}

	/**
	 * Calculates the driving time component of the load value
	 * Madsen notation: Cvariable * Tdr_time + Cconstant * (Twait + Chandle)
	 * @param job Job to evaluate
	 * @return The driving time laod cost for this stop (the specified job)
	 */
	private double loadDrivingTime(VehicleScheduleJob job) {
		Trip t = job.getTrip();
		int minDrivingTime = (int)t.getRoute().getTime() / 60;

		// We need to construct the entire trip that this job is a member of.
		VehicleScheduleJob startJob = job.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP ?
				job : job.getCorrespondingJob();
		VehicleScheduleJob endJob = job.getType() == VehicleScheduleJob.JOB_TYPE_DROPOFF ? 
				job : job.getCorrespondingJob();
		int waitingTime = job.getWaitTime(mVehiclePlanIndex);

		double cost = Rebus.DR_TIME_C1 * (endJob.getWorkingServiceTime(mVehiclePlanIndex) - startJob.getWorkingServiceTime(mVehiclePlanIndex)) 
				+ Rebus.DR_TIME_C2 * (waitingTime + Rebus.HANDLE_TIME);

		//			Log.i(TAG, "     Driving time: " + cost, true, true);
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

		// Waiting time can only occur at a pickup job (impossible to arrive early to a dropoff)
		if(job.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP) {
			int waitingTime = job.getWaitTime(mVehiclePlanIndex);
			cost = Rebus.WAIT_C2 * (waitingTime * waitingTime) + Rebus.WAIT_C1 * waitingTime;
		}
		//			Log.i(TAG, ". Waiting time: " + cost, true, true);
		return cost;
	}

	/**
	 * Calculates the service time deviation component of the load value
	 * Madsen notation: Cdev * Tdev^2
	 * @param job Job to evaluate
	 * @return The deviation from desired service time load cost for this stop (the specified job)
	 */
	private double loadDesiredServiceTimeDeviation(VehicleScheduleJob job) {
		int deviation = job.getWorkingServiceTime(mVehiclePlanIndex) - job.getStartTime();
		double cost = Rebus.DEV_C * (deviation * deviation);

		//			Log.i(TAG, ". Service time dev: " + cost, true, true);
		return cost;
	}

	/**
	 * Calculates the capacity utilization component of the load value
	 * Madsen notation: Ci * Vfreei^2
	 * @param job The job to evaulate
	 * @param passengers Number of passengers in vehicle
	 * @return The vehicle capacity utilization load cost for this stop (the specified job)
	 */
	private double loadCapacityUtilization(int passengers) {
		// Number of seats free
		int free = Vehicle.VEHICLE_CAPACITY - passengers;

		double cost = Rebus.CAPACITY_C * (free * free);

		//			Log.i(TAG, ". Cap util: " + cost, true, true);
		return cost;
	}
	
	// *****************************
	//        REBUS ADDITIONS
	// *****************************

	/**
	 * An addition to the REBUS algorithm. Penalizes vehicles with less utilization across the day, encouraging
	 * scheduling in more highly booked vehicles.
	 * @param numJobs Number of jobs in the vehicle's schedule
	 * @return The vehicle utilization cost
	 */
	private double loadVehicleUtilization(int numJobs) {
		double cost = Math.pow(numJobs / 2, -1) * Rebus.VEHICLE_UTIL_C;// / mSchedule.size();
		//			Log.i(TAG, ". Vehicle util: " + cost + "\n", true, true);
		return cost;
	}

	/**
	 * Penalizes schedules with higher total times
	 * @param lastJob The previous job in the schedule
	 * @param curJob Current job to evaluate
	 * @param numJobs Total number of jobs in the schedule
	 * @return The mileage heuristic cost
	 */
	private double loadTotalTime(VehicleScheduleJob lastJob, VehicleScheduleJob curJob, int numJobs) {
		double cost;

		if(lastJob.getTrip() == null)
			cost = 0;
		else if(lastJob.nextJobIs(mVehiclePlanIndex, curJob))
			cost = lastJob.getTimeToNextJob(mVehiclePlanIndex);
		else 
			cost = mCache.getHash(lastJob.getTrip().getIdentifier(), 
					lastJob.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP, 
					curJob.getTrip().getIdentifier(), 
					curJob.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP);
		cost = cost * Rebus.TIME_C / numJobs;
		//			Log.i(TAG, ". Mileage: " + cost + "\n", true, true);
		return cost;
	}
	
	/**
	 * 
	 */
}
