package edu.pugetsound.npastor.routing;

import java.util.ArrayList;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class VehicleSchedule {

	ArrayList<VehicleScheduleJob> mScheduledJobs;
	ArrayList<VehicleScheduleJob> mConsumedJobs;
	Vehicle mVehicle;
	
	public VehicleSchedule(Vehicle vehicle) {
		mScheduledJobs = new ArrayList<VehicleScheduleJob>();
		mConsumedJobs = new ArrayList<VehicleScheduleJob>();
		mVehicle = vehicle;
		initialize();
	}
	
	private void initialize() {
		// Add start and end jobs to queue (leave and return to base)
		VehicleScheduleJob startJob = new VehicleScheduleJob(null, Constants.BEGIN_OPERATION_HOUR - 1, 0, VehicleScheduleJob.JOB_TYPE_START);
		VehicleScheduleJob endJob = new VehicleScheduleJob(null, Constants.END_OPERATION_HOUR + 1, 0, VehicleScheduleJob.JOB_TYPE_END);
		mScheduledJobs.add(startJob);
		mScheduledJobs.add(endJob);
	}
	
	/**
	 * Returns the job at the specified index
	 * @param index
	 * @return Job at specified index, or null if out of bounds
	 */
	public VehicleScheduleJob getJob(int index) {

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
	public void scheduleJob(VehicleScheduleJob newJob) {
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
	public VehicleScheduleJob consumeNextJob() {
		VehicleScheduleJob job = mScheduledJobs.remove(0);
		mConsumedJobs.add(job);
		return job;
	}
	
	public int getNumJobsRemaining() {
		return mScheduledJobs.size();
	}
}
