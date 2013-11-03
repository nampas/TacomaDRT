package edu.pugetsound.npastor.simulation;

import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.routing.REBUS;
import edu.pugetsound.npastor.routing.Vehicle;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Trip;

public class DRTSimulation {
	
	ArrayList<Trip> mTrips;
	PriorityQueue<SimEvent> mEventQueue;
	ArrayList<Vehicle> mVehiclePlans;
	REBUS mREBUS;

	public DRTSimulation(ArrayList<Trip> trips) {
		mTrips = trips;
		mEventQueue = new PriorityQueue<SimEvent>();
		mREBUS = new REBUS();
		mVehiclePlans = new ArrayList<Vehicle>();
	}
	
	public void runSimulation() {
		generateVehicles();
		enqueueTripRequestEvents();
		doAPrioriScheduling();
		
		// Run simulation until event queue is empty
		while(!mEventQueue.isEmpty()) {
			SimEvent nextEvent = mEventQueue.poll();
			switch(nextEvent.getType()) {
			case SimEvent.EVENT_DROPOFF:
				consumeDropoffEvent(nextEvent);
				break;
			case SimEvent.EVENT_PICKUP:
				consumePickupEvent(nextEvent);
				break;
			case SimEvent.EVENT_NEW_REQUEST:
				consumeNewRequestEvent(nextEvent, true);
				break;
			}
		}
	}
	
	private void generateVehicles() {
		for(int i = 0; i < Constants.VEHCILE_QUANTITY; i++) {
			mVehiclePlans.add(new Vehicle());
		}
	}
	
	/**
	 * Enqueues all trip requests in the event queue
	 */
	private void enqueueTripRequestEvents() {
		for(Trip t: mTrips) {
			int requestTime = t.getCallInTime();
			SimEvent requestEvent = new SimEvent(SimEvent.EVENT_NEW_REQUEST, t, requestTime);
			mEventQueue.add(requestEvent);
		}
	}
	
	/**
	 * Schedule all trips that are known a priori. That is, all trips which
	 * are known to the agency before service hours begin. These are the static requests.
	 */
	private void doAPrioriScheduling() {
		boolean moreStaticRequests = true;
		while(moreStaticRequests) {
			SimEvent event = mEventQueue.peek();
			if(event == null) break;
			if(event.getType() == SimEvent.EVENT_NEW_REQUEST) { // Ensure this is correct event type
				if(event.getTimeMins() < Constants.BEGIN_OPERATION_HOUR) { // Ensure request time is before operation begins
					consumeNewRequestEvent(event, false); // Enqueue trip in the scheduler, don't immediately schedule
					mEventQueue.poll();
				} else {
					moreStaticRequests = false; // Break loop when we've reached operation hours
				}
			}
		}
		// Now that all static trips are enqueued we can schedule them.
		mREBUS.scheduleQueuedJobs(mVehiclePlans);
	}
	
	/**
	 * Consumes a dropoff event
	 * @param event Dropoff event
	 */
	private void consumeDropoffEvent(SimEvent event) {
		
	}
	
	/**
	 * Consumes a pickup event
	 * @param event Pickup event
	 */
	private void consumePickupEvent(SimEvent event) {
		
	}
	
	/**
	 * Consumes a new trip request event
	 * @param event Trip request event
	 * @parm schedule Specifies if scheduling should be executed immediately.
	 */
	private void consumeNewRequestEvent(SimEvent event, boolean schedule) {
		Trip t = event.getTrip();
		// Enqueue the trip in the REBUS queue, and schedule if requested
		mREBUS.enqueueTripRequest(t);
		if(schedule) mREBUS.scheduleQueuedJobs(mVehiclePlans);
		// Add the pickup and dropoff events
		SimEvent pickupEvent = new SimEvent(SimEvent.EVENT_PICKUP, t, t.getPickupTime());
		//TODO: DETERMINE DROPOFF TIME
		SimEvent dropoffEvent = new SimEvent(SimEvent.EVENT_PICKUP, t, t.getPickupTime());
		
		mEventQueue.add(pickupEvent);
		mEventQueue.add(dropoffEvent);
	}
}
