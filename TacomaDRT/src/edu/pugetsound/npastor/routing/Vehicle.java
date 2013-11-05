package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class Vehicle {

	private int mVehicleId;
	private int mCapacity;
	private int mMPH;
	private ArrayList<Trip> mPassengers;
	private VehicleSchedule mSchedule;
	private int mIdentifier;
	
	
	public Vehicle(int id) {
		mCapacity = Constants.VEHCILE_QUANTITY;
		mPassengers = new ArrayList<Trip>();
		mMPH = Constants.VEHICLE_MPH;
		mVehicleId = hashCode();
		mSchedule = new VehicleSchedule(this);
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
	
	public int getCapacity() {
		return mCapacity;
	}
	
	public VehicleSchedule getSchedule() {
		return mSchedule;
	}
	
	public int getIdentifier() {
		return mIdentifier;
	}
}
