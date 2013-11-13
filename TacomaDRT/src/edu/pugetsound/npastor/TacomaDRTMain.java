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
	
	public void runModel(String tripFilePath) {
		mTripGenStartTime = System.currentTimeMillis();
		setSimulationDirectory();
		
		// Run the trip generation
		if(tripFilePath == null)
			mTripGen.generateTrips(); 
		else 
			mTripGen.generateTripsFromFile(tripFilePath);
		
		// Print trip gen time
		long tripGenEndTime = System.currentTimeMillis();
		String message = "Trip generation complete: " + Constants.TOTAL_TRIPS + " trips in ";
		printTime(message, tripGenEndTime, mTripGenStartTime);
		
		// Run the simulation!
		mSimStartTime = System.currentTimeMillis();
		mSimulation = new DRTSimulation(mTripGen.getTrips());
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
	
	private void printTime(String message, long endTimeMillis, long startTimeMillis) {
		float elapsedSecs = (float)(endTimeMillis - startTimeMillis) / 1000;
		int timeMins = (int)(elapsedSecs / 60);
		float remainderSecs = elapsedSecs % 60.0f;
		Log.info(TAG, message + elapsedSecs + " seconds (" + timeMins + ":" + remainderSecs + ")");
	}
	
	private void setSimulationDirectory() {
		mSimulationDirectory = Constants.SIM_BASE_DIRECTORY + "/sim" + DRTUtils.formatMillis(mTripGenStartTime);
		boolean result = new File(mSimulationDirectory).mkdirs();
		if(!result) {
			Log.error(TAG, "Unable to create simulation base directory at: " + mSimulationDirectory);
		}
	}
	
	public static String getSimulationDirectory() {
		return mSimulationDirectory;
	}
}
