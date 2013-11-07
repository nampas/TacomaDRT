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
		mSchedule = new ArrayList<VehicleScheduleJob>();
		mIdentifier = id;
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
