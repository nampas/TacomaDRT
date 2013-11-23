package edu.pugetsound.npastor;

import java.io.File;
import java.util.ArrayList;

import edu.pugetsound.npastor.riderGen.TripGenerator;
import edu.pugetsound.npastor.routing.LRURouteCache;
import edu.pugetsound.npastor.routing.RouteWrapper;
import edu.pugetsound.npastor.routing.Routefinder;
import edu.pugetsound.npastor.simulation.DRTSimulation;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

public class TacomaDRTMain {

	//Note to self: Run from .\TacomaDRT\bin
	//execute: java edu/pugetsound/npastor/TacomaDRT
	private TripGenerator mTripGen;
	private DRTSimulation mSimulation;
	private static String mSimulationDirectory;
	public static long mTripGenStartTime;
	public static long mSimStartTime;
	
	public final static String TAG = "TacomaDRTMain";
	
	public static void main(String[] args) {
		TacomaDRTMain drt = new TacomaDRTMain();
		String tripFile = null;
		if(args.length > 0)
			tripFile = args[0];
		drt.runModel(tripFile);
	}
	
	public TacomaDRTMain() {
		mTripGen = new TripGenerator();
	}
	
	public void runModel(String tripVehicleFilePath) {
		mTripGenStartTime = System.currentTimeMillis();
		setSimulationDirectory();
		
		// Run the trip generation
		if(tripVehicleFilePath == null)
			mTripGen.generateTrips(); 
		else 
			mTripGen.generateTripsFromFile(tripVehicleFilePath);
		
		// Print trip gen time
		long tripGenEndTime = System.currentTimeMillis();
		String message = "Trip generation complete: " + mTripGen.getTrips().size() + " trips in ";
		printTime(message, tripGenEndTime, mTripGenStartTime);
		
		// Run the simulation!
		mSimStartTime = System.currentTimeMillis();
	
		testRoutingTimes(mTripGen.getTrips());
		
				
		// Print simulation time
		long simEndTime = System.currentTimeMillis();
		message = "Simulation finished in ";
		printTime(message, simEndTime, mSimStartTime);
		
		// Print total time
		printTime("Entire model finished in ", simEndTime, mTripGenStartTime);
		
		// Write any remaining messages to log file
		Log.writeBufferToLogFile();
	}
	
	private void testRoutingTimes(ArrayList<Trip> trips) {
		LRURouteCache cache = LRURouteCache.getInstance();
		Routefinder router = new Routefinder();
		
		int routed = 0;
		
		for(Trip t: trips) {
			RouteWrapper curRoute = new RouteWrapper(t.getFirstEndpoint(), t.getSecondEndpoint());
			curRoute.timeMins = (byte) (router.getTravelTimeSec(curRoute) / 60); 
			cache.put(curRoute.hashCode(), curRoute);
			for(Trip v : trips) {
				if(t.getIdentifier() == v.getIdentifier()) continue;
				
				RouteWrapper oToO = new RouteWrapper(t.getFirstEndpoint(), v.getFirstEndpoint());
				oToO.timeMins = (byte) (router.getTravelTimeSec(oToO) / 60);
				
				RouteWrapper oToD = new RouteWrapper(t.getFirstEndpoint(), v.getSecondEndpoint());
				oToD.timeMins = (byte) (router.getTravelTimeSec(oToD)/ 60);
				
				RouteWrapper dToO = new RouteWrapper(t.getSecondEndpoint(), v.getFirstEndpoint());
				dToO.timeMins = (byte) (router.getTravelTimeSec(dToO)/ 60);
				
				RouteWrapper dToD = new RouteWrapper(t.getSecondEndpoint(), v.getSecondEndpoint());
				dToD.timeMins = (byte) (router.getTravelTimeSec(dToD) / 60);
				
				cache.put(oToO.hashCode(), oToO);
				cache.put(oToD.hashCode(), oToD);
				cache.put(dToO.hashCode(), dToO);
				cache.put(dToD.hashCode(), dToD);
				
				if(cache.size() % 10000 == 0)
					Log.info(TAG, "Cache size at " + cache.size());
				
				routed += 4;
			}
			routed++;
		}
		Log.info(TAG, "End cache size " + cache.size());
		Log.info(TAG, "Routes routed " + routed);
	}
	
	private void printTime(String message, long endTimeMillis, long startTimeMillis) {
		float elapsedSecs = (float)(endTimeMillis - startTimeMillis) / 1000;
		int timeMins = (int)(elapsedSecs / 60);
		float remainderSecs = elapsedSecs % 60.0f;
		Log.info(TAG, message + elapsedSecs + " seconds (" + timeMins + ":" + remainderSecs + ")");
	}
	
	private void setSimulationDirectory() {
		mSimulationDirectory = Constants.SIM_BASE_DIRECTORY + "/sim" + DRTUtils.formatMillis(mTripGenStartTime);
		// Make base directory
		boolean result = new File(mSimulationDirectory).mkdirs();
		if(!result)
			Log.error(TAG, "Unable to create simulation directory at: " + mSimulationDirectory);

		// Make route shp directory
		String routeShpDir = mSimulationDirectory + Constants.ROUTE_SHP_DIR;
		result = new File(routeShpDir).mkdirs();
		if(!result)
			Log.error(TAG, "Unable to create route simulation directory at: " + routeShpDir);
		
		// Make trip shp directory
		String tripShpDir = mSimulationDirectory + Constants.TRIP_SHP_DIR;
		result = new File(tripShpDir).mkdirs();
		if(!result)
			Log.error(TAG, "Unable to create trip simulation directory at: " + tripShpDir);
	}
	
	public static String getSimulationDirectory() {
		return mSimulationDirectory;
	}
	
	public static String getRouteShpSimDirectory() {
		return getSimulationDirectory() + Constants.ROUTE_SHP_DIR;
	}
	
	public static String getTripShpSimDirectory() {
		return getSimulationDirectory() + Constants.TRIP_SHP_DIR;
	}
}
