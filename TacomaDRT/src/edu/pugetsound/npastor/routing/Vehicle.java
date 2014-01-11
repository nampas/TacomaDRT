package edu.pugetsound.npastor.routing;

import java.util.ArrayList;

import edu.pugetsound.npastor.utils.Constants;
public class Vehicle {

	//TODO: what is capacity?
	public static final int VEHICLE_CAPACITY = 15;
	
	private int mVehicleId;
	private ArrayList<VehicleScheduleJob> mSchedule;
	
	
	public Vehicle(int id) {
		mVehicleId = id;
		initSchedule();		
	}
	
	private void initSchedule() {
		mSchedule = new ArrayList<VehicleScheduleJob>();
		// Add start and end jobs
		mSchedule.add(new VehicleScheduleJob(null, null, Constants.BEGIN_OPERATION_HOUR*60, 0, VehicleScheduleJob.JOB_TYPE_START, 0));
		mSchedule.add(new VehicleScheduleJob(null, null, Constants.END_OPERATION_HOUR*60, 0, VehicleScheduleJob.JOB_TYPE_END, 0));
	}
	
	public ArrayList<VehicleScheduleJob> getSchedule() {
		return mSchedule;
	}
	
	public int getIdentifier() {
		return mVehicleId;
	}
	
	public String scheduleToString() {
		String str = "Vehicle " + mVehicleId + " schedule:";
		for(int i = 0; i < mSchedule.size(); i++) {
			str += "\n " + mSchedule.get(i) ;
		}
		return str;
	}
}
