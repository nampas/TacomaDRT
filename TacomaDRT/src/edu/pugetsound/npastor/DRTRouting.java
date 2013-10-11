package edu.pugetsound.npastor;

import java.util.ArrayList;

public class DRTRouting {

	ArrayList<Trip> mRequestedTrips;
	ArrayList<Vehicle> mVehicles;
	
	public DRTRouting() {
		
	}
	
	/**
	 * Begins the scheduling and routing algorithm
	 * @param requestedTrips A fully initialized list of requested trips
	 */
	public void doRoute(ArrayList<Trip> requestedTrips) {
		mRequestedTrips = requestedTrips;
	}
}
