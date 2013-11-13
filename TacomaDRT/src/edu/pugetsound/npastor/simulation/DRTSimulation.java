package edu.pugetsound.npastor.simulation;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.PriorityQueue;

import edu.pugetsound.npastor.TacomaDRTMain;
import edu.pugetsound.npastor.routing.REBUS;
import edu.pugetsound.npastor.routing.Vehicle;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

public class DRTSimulation {
	
	public static final String TAG = "DRTSimulation";
	
	ArrayList<Trip> mTrips;
	PriorityQueue<SimEvent> mEventQueue;
	ArrayList<Vehicle> mVehiclePlans;
	REBUS mREBUS;
	
	ArrayList<Trip> mRejectedTrips;
	private int mTotalTrips;

	public DRTSimulation(ArrayList<Trip> trips) {
		mTrips = trips;
		mTotalTrips = trips.size();
		mEventQueue = new PriorityQueue<SimEvent>();
		mREBUS = new REBUS();
		mVehiclePlans = new ArrayList<Vehicle>();
		mRejectedTrips = new ArrayList<Trip>();
	}
	
	public void runSimulation() {
		
		Log.info(TAG, "Running simulation");		
		generateVehicles();
		enqueueTripRequestEvents();
		doAPrioriScheduling();
		
		int lastTime = 0;
		// Run simulation until event queue is empty
		while(!mEventQueue.isEmpty()) {
			SimEvent nextEvent = mEventQueue.poll();
			int nextTime = nextEvent.getTimeMins();
			switch(nextEvent.getType()) {
				case SimEvent.EVENT_NEW_REQUEST:
					consumeNewRequestEvent(nextEvent, (lastTime != nextTime ? true : false));
					break;
			}
			lastTime = nextTime;
		}
		
		onSimulationFinished();
	}
	
	private void generateVehicles() {
		Log.info(TAG, "Generating " + Constants.VEHCILE_QUANTITY + " vehicles");	
		for(int i = 0; i < Constants.VEHCILE_QUANTITY; i++) {
			mVehiclePlans.add(new Vehicle(i+1));
		}
	}
	
	/**
	 * Enqueues all trip requests in the event queue
	 */
	private void enqueueTripRequestEvents() {
		Log.info(TAG, "Enqueueing all trip request events in simulation queue");
		for(Trip t: mTrips) {
			int requestTime = t.getCallInTime();
			SimEvent requestEvent = new SimEvent(SimEvent.EVENT_NEW_REQUEST, t, requestTime);
			mEventQueue.add(requestEvent);
		}
	}
	
	/**
	 * Contains procedures to execute when a simulation has finished running
	 */
	private void onSimulationFinished() {
		Log.info(TAG, "*************************************");
		Log.info(TAG, "       SIMULATION COMPLETE");
		Log.info(TAG, "*************************************");
		
		for(Vehicle v : mVehiclePlans) {
			Log.info(TAG, v.scheduleToString());
		}
		
		// Print total rejected trips and rate
		float rejectionRate = (float) mRejectedTrips.size() / mTotalTrips * 100;
		Log.info(TAG, "Total trips simulated: " + mTotalTrips + ". Total trips rejected by REBUS: " + mRejectedTrips.size() +
				". Rejection rate: " + rejectionRate + "%");
		
		// Write vehicle routing files
		writeScheduleTxtFile();
		writeScheduleShpFile();
	}
	
	/**
	 * Schedule all trips that are known a priori. That is, all trips which
	 * are known to the agency before service hours begin. These are the static requests.
	 */
	private void doAPrioriScheduling() {
		Log.info(TAG, "Doing a priori scheduling (all static trip requests)");
		boolean moreStaticRequests = true;
		while(moreStaticRequests) {
			SimEvent event = mEventQueue.peek();
			if(event == null) moreStaticRequests = false;
			else if(event.getType() == SimEvent.EVENT_NEW_REQUEST) { // Ensure this is correct event type
				if(event.getTimeMins() < Constants.BEGIN_OPERATION_HOUR * 60) { // Ensure request time is before operation begins
					consumeNewRequestEvent(event, false); // Enqueue trip in the scheduler, don't immediately schedule
					mEventQueue.poll();
				} else {
					moreStaticRequests = false; // Break loop when we've reached operation hours
				}
			}
		}
		Log.info(TAG, mREBUS.getQueueSize() + " static jobs queued");
		// Now that all static trips are enqueued we can schedule them.
		mRejectedTrips.addAll(mREBUS.scheduleQueuedJobs(mVehiclePlans));
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
		if(schedule) {
			mRejectedTrips.addAll(mREBUS.scheduleQueuedJobs(mVehiclePlans));
		}
	}
	
	/**
	 * Write vehicle schedules to a text file
	 */
	private void writeScheduleTxtFile() {

		// Build text list
		ArrayList<String> text = new ArrayList<String>();
		for(Vehicle v : mVehiclePlans) {
			// Write to file
			text.add(v.scheduleToString());
		}	
		
		// Write file
		DRTUtils.writeTxtFile(text, Constants.SCHED_PREFIX_TXT);
	}
	
	private void writeStatisticsTxtFile() {
		
	}
	
	/**
	 * Write vehicle schedules to a shapfile
	 */
	private void writeScheduleShpFile() {
		
	}
}
