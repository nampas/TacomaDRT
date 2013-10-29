package edu.pugetsound.npastor.simulation;

import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.utils.Trip;

public class DRTSimulation {
	
	ArrayList<Trip> mTrips;
	PriorityQueue<SimEvent> mEventQueue;

	public DRTSimulation(ArrayList<Trip> trips) {
		mTrips = trips;
		mEventQueue = new PriorityQueue<SimEvent>();
	}
	
	public void runSimulation() {
		queueTripRequests();
	}
	
	/**
	 * Queues all trip requests
	 */
	private void queueTripRequests() {
		for(Trip t: mTrips) {
			int requestTime = t.getCallInTime();
			SimEvent requestEvent = new SimEvent(SimEvent.EVENT_NEW_REQUEST, t, requestTime);
			mEventQueue.add(requestEvent);
		}
	}
}
