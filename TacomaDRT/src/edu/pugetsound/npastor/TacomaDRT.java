package edu.pugetsound.npastor;

import java.util.Random;

import edu.pugetsound.npastor.riderGen.TripGenerator;
import edu.pugetsound.npastor.routing.DRTRouting;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.D;

public class TacomaDRT {

	//Note to self: Run from .\TacomaDRT\bin
	//execute: java edu/pugetsound/npastor/TacomaDRT
	private TripGenerator mTripGen;
	private DRTRouting mTripRouting;
	
	public final static String TAG = "TacomaDRT";
	
	public static void main(String[] args) {
		TacomaDRT drt = new TacomaDRT();
		drt.runModel();
	}
	
	public TacomaDRT() {
		mTripGen = new TripGenerator();
		mTripRouting = new DRTRouting();
	}
	
	public void runModel() {
		long startTime = System.currentTimeMillis();
		
		// Run the trip generation
		mTripGen.generateTrips(); 
		
		long tripGenTime = System.currentTimeMillis();
		float elapsedSecs = (float)(tripGenTime - startTime) / 1000;
		int timeMins = (int)(elapsedSecs / 60);
		float remainderSecs = elapsedSecs % 60.0f;
		D.info(TAG, "Trip generation complete: " + Constants.TOTAL_TRIPS + " trips in " + elapsedSecs + " seconds (" + timeMins + ":" + remainderSecs + ")");
		
		// Run the routing!
		mTripRouting.doRoute(mTripGen.getTrips());
	}
}
