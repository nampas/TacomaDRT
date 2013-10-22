package edu.pugetsound.npastor.routing;

import java.util.ArrayList;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class DRTRouting {

	ArrayList<Trip> mRequestedTrips;
	ArrayList<Vehicle> mVehicles;
	Routefinder mRoutefinder;
	
	public DRTRouting() {
		mVehicles = generateVehicles();
		mRoutefinder = new Routefinder();
	}
	
	/**
	 * Begins the scheduling and routing algorithm
	 * @param requestedTrips A fully initialized list of requested trips
	 */
	public void doRoute(ArrayList<Trip> requestedTrips) {
		mRequestedTrips = requestedTrips;
//		for(Trip t : requestedTrips) {
//			mRoutefinder.findRoute(t.getFirstEndpoint(), t.getSecondEndpoint());
//		}
	}
	
	private ArrayList<Vehicle> generateVehicles() {
		ArrayList<Vehicle> vehicles = new ArrayList<Vehicle>();
		for(int i = 0; i < Constants.VEHCILE_QUANTITY; i++) {
			vehicles.add(new Vehicle());
		}
		
		return vehicles;
	}
}
