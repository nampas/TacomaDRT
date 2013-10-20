package edu.pugetsound.npastor.routing;

import java.util.ArrayList;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class DRTRouting {

	ArrayList<Trip> mRequestedTrips;
	ArrayList<Vehicle> mVehicles;
	
	public DRTRouting() {
		mVehicles = generateVehicles();
	}
	
	/**
	 * Begins the scheduling and routing algorithm
	 * @param requestedTrips A fully initialized list of requested trips
	 */
	public void doRoute(ArrayList<Trip> requestedTrips) {
		mRequestedTrips = requestedTrips;
	}
	
	private ArrayList<Vehicle> generateVehicles() {
		ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
		for(int i = 0; i < Constants.VEHCILE_QUANTITY; i++) {
			vehicles.add(new Vehicle());
		}
		return vehicles;
	}
}
