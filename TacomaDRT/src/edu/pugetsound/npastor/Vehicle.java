package edu.pugetsound.npastor;

import java.util.ArrayList;

public class Vehicle {

	private int mVehicleId;
	private int mCapacity;
	private int mMPH;
	private ArrayList<Trip> mPassengers;
	
	public Vehicle() {
		mCapacity = Constants.VEHCILE_QUANTITY;
		mPassengers = new ArrayList<Trip>();
		mMPH = Constants.VEHICLE_MPH;
		mVehicleId = hashCode();
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
}
