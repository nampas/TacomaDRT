package edu.pugetsound.npastor;

import java.util.Random;

public class TacomaDRT {

	//Note to self: Run from .\TacomaDRT\bin
	//execute: java edu/pugetsound/npastor/TacomaDRT
	private TripGenerator mTripGen;
	
	public static void main(String[] args) {
		TacomaDRT drt = new TacomaDRT();
		drt.runModel();
	}
	
	public TacomaDRT() {
		mTripGen = new TripGenerator();
	}
	
	public void runModel() {
		mTripGen.generateTrips();
	}
}
