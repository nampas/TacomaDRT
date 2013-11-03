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
	public static long mStartTime;
	
	public final static String TAG = "TacomaDRT";
	
	public static void main(String[] args) {
		TacomaDRTMain drt = new TacomaDRTMain();
		drt.runModel();
	}
	
	public TacomaDRTMain() {
		mTripGen = new TripGenerator();
	}
	
	public void runModel() {
		mStartTime = System.currentTimeMillis();
		setSimulationDirectory();
		
		// Run the trip generation
		mTripGen.generateTrips(); 
		
		long tripGenTime = System.currentTimeMillis();
		float elapsedSecs = (float)(tripGenTime - mStartTime) / 1000;
		int timeMins = (int)(elapsedSecs / 60);
		float remainderSecs = elapsedSecs % 60.0f;
		Log.info(TAG, "Trip generation complete: " + Constants.TOTAL_TRIPS + " trips in " + elapsedSecs + " seconds (" + timeMins + ":" + remainderSecs + ")");
		
		// Run the simulation!
		mSimulation = new DRTSimulation(mTripGen.getTrips());
		mSimulation.runSimulation();
	}
	
	private void setSimulationDirectory() {
		mSimulationDirectory = Constants.SIM_BASE_DIRECTORY + "/sim" + DRTUtils.formatMillis(mStartTime);
		boolean result = new File(mSimulationDirectory).mkdirs();
		if(!result) {
			Log.error(TAG, "Unable to create simulation base directory at: " + mSimulationDirectory);
		}
	}
	
	public static String getSimulationDirectory() {
		return mSimulationDirectory;
	}
}
