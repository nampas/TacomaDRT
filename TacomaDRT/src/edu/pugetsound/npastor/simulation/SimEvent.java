package edu.pugetsound.npastor.simulation;

import edu.pugetsound.npastor.utils.Trip;

public class SimEvent implements Comparable<SimEvent> {

	public final static int EVENT_NEW_REQUEST = 0;
	public final static int EVENT_PICKUP = 1;
	public final static int EVENT_DROPOFF = 2;
	
	private int mType;
	private Trip mTrip;
	private int mTime;
	
	public SimEvent(int type, int timeMins) {
		mType = type;
		mTrip = null;
		mTime = timeMins;
	}
	
	public SimEvent(int type, Trip trip, int timeMins) {
		mType = type;
		mTrip = trip;
		mTime = timeMins;
	}
	
	public int getType() {
		return mType;
	}
	
	public Trip getTrip() {
		return mTrip;
	}
	
	public int getTimeMins() {
		return mTime;
	}

	public int compareTo(SimEvent event) {
		return mTime - event.getTimeMins();
	}

	
}
