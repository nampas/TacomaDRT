package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class Vehicle {

	//TODO: what is capacity?
	public static final int VEHICLE_CAPACITY = 20;
	
	private int mVehicleId;
	private int mMPH;
	private ArrayList<Trip> mPassengers;
	private ArrayList<VehicleScheduleJob> mSchedule;
	private int mIdentifier;
	
	
	public Vehicle(int id) {
		mPassengers = new ArrayList<Trip>();
		mMPH = Constants.VEHICLE_MPH;
		mVehicleId = hashCode();
		mIdentifier = id;
		initSchedule();
		
	}
	
	private void initSchedule() {
		mSchedule = new ArrayList<VehicleScheduleJob>();
		// Add start and end jobs
		mSchedule.add(new VehicleScheduleJob(null, 0, 0, VehicleScheduleJob.JOB_TYPE_START));
		mSchedule.add(new VehicleScheduleJob(null, 0, 0, VehicleScheduleJob.JOB_TYPE_END));
	}
	
	public void addPassenger(Trip t) {
		mPassengers.add(t);
	}
	
	public void addPassengers(ArrayList<Trip> passengers) {
		mPassengers.addAll(passengers);
	}
	
	public void removePassenger(Trip t) {
		mPassengers.remove(t);
	}
	
	public void removeAllPassengers() {
		mPassengers.clear();
	}
	
	public int getCurrentLoad() {
		return mPassengers.size();
	}
	
	public ArrayList<VehicleScheduleJob> getSchedule() {
		return mSchedule;
	}
	
	public int getIdentifier() {
		return mIdentifier;
	}
}
