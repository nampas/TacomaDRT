package edu.pugetsound.npastor;

import java.util.Random;

import edu.pugetsound.npastor.riderGen.TripGenerator;
import edu.pugetsound.npastor.routing.DRTRouting;

public class TacomaDRT {

	//Note to self: Run from .\TacomaDRT\bin
	//execute: java edu/pugetsound/npastor/TacomaDRT
	private TripGenerator mTripGen;
	private DRTRouting mTripRouting;
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("user.dir"));
		TacomaDRT drt = new TacomaDRT();
		drt.runModel();
	}
	
	public TacomaDRT() {
		mTripGen = new TripGenerator();
		mTripRouting = new DRTRouting();
	}
	
	public void runModel() {
		mTripGen.generateTrips();
		mTripRouting.doRoute(mTripGen.getTrips());
	}
}
