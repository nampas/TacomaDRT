package edu.pugetsound.npastor;

import java.io.File;

import edu.pugetsound.npastor.riderGen.TripGenerator;
import edu.pugetsound.npastor.simulation.DRTSimulation;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;

public class TacomaDRTMain {

	//Note to self: Run from .\TacomaDRT\bin
	//execute: java edu/pugetsound/npastor/TacomaDRT
	private DRTSimulation mSimulation;
	private static String mSimulationDirectory;
	private static String mSourceSimDirectory; //Points to the directory of the source sim, when we're re-running a simulation
	public static long mTripGenStartTime;
	public static long mSimStartTime;
	
	public final static String TAG = "TacomaDRTMain";
	
	public static void main(String[] args) {
		TacomaDRTMain drt = new TacomaDRTMain();
		if(args.length > 0)
			mSourceSimDirectory = args[0];
		else 
			mSourceSimDirectory = null;
		drt.runModel(mSourceSimDirectory);
	}
	
	public void runModel(String sourceSimDir) {

		mTripGenStartTime = System.currentTimeMillis();
		setSimulationDirectory();
		
		TripGenerator tripGen = new TripGenerator();
		
		// Run the trip generation
		if(sourceSimDir == null)
			tripGen.generateTrips(); 
		else 
			tripGen.generateTripsFromFile(getSourceTripVehDir());
		
		// Print trip gen time
		long tripGenEndTime = System.currentTimeMillis();
		String message = "Trip generation complete: " + tripGen.getTrips().size() + " trips in ";
		printTime(message, tripGenEndTime, mTripGenStartTime);
		
		if(sourceSimDir == null)
			mSimulation = new DRTSimulation(tripGen.getTrips(), false);
		else
			mSimulation = new DRTSimulation(tripGen.getTrips(), true);
		
		tripGen = null; // Deallocate the trip generator
		
		// Build the cache! This might take a long time
		mSimulation.buildCache();
		
		// Run the simulation! Inform the simulator if this is a re-run of a previous simulation,
		// in which it will use that instance's route cache and trip/vehicle file
		mSimStartTime = System.currentTimeMillis();
		mSimulation.runSimulation();
		
		// Print simulation time
		long simEndTime = System.currentTimeMillis();
		message = "Simulation finished in ";
		printTime(message, simEndTime, mSimStartTime);
		
		// Print total time
		printTime("Entire model finished in ", simEndTime, mTripGenStartTime);
		
		// Write any remaining messages to log file
		Log.writeBufferToLogFile();
	}
	
	public static void printTime(String message, long endTimeMillis, long startTimeMillis) {
		float elapsedSecs = (float)(endTimeMillis - startTimeMillis) / 1000;
		int timeMins = (int)(elapsedSecs / 60);
		float remainderSecs = elapsedSecs % 60.0f;
		Log.infoln(TAG, message + elapsedSecs + " seconds (" + timeMins + ":" + remainderSecs + ")");
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
	
	// ***********************************************
	//    FOR ACCESSING THIS SIMULATION'S DIRECTORY
	// ***********************************************
	public static String getSimulationDirectory() {
		return mSimulationDirectory;
	}
	
	public static String getRouteShpSimDirectory() {
		return getSimulationDirectory() + Constants.ROUTE_SHP_DIR;
	}
	
	public static String getTripShpSimDirectory() {
		return getSimulationDirectory() + Constants.TRIP_SHP_DIR;
	}
	
	// ********************************************************
	//     FOR ACCESSING THE SOURCE SIMULATION'S DIRECTORY,
	//   WHEN THIS INSTANCE IS RE-RUNNING A PREVIOUS INSTANCE
	// ********************************************************
	private static String getSourceSimDirectory() {
		return mSourceSimDirectory;
	}
	
	public static String getSourceTripVehDir() {
		return getSourceSimDirectory() + Constants.TRIPS_VEHICLES_TXT;
	}
	
	public static String getSourceCacheDir() {
		return getSourceSimDirectory() + Constants.ROUTE_CACHE_RC;
	}
}
