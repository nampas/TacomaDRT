package edu.pugetsound.npastor.simulation;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import edu.pugetsound.npastor.TacomaDRTMain;
import edu.pugetsound.npastor.routing.Rebus;
import edu.pugetsound.npastor.routing.Vehicle;
import edu.pugetsound.npastor.routing.VehicleScheduleJob;
import edu.pugetsound.npastor.routing.VehicleScheduleNode;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.ShapefileWriter;
import edu.pugetsound.npastor.utils.Trip;

public class DRTSimulation {
	
	public static final String TAG = "DRTSimulation";
	
	private static final String NUM_VEHICLES_FILE_LBL = "num_vehicles";
	
	ArrayList<Trip> mTrips;
	PriorityQueue<SimEvent> mEventQueue;
	Vehicle[] mVehiclePlans;
	Rebus mRebus;
	
	ArrayList<Trip> mRejectedTrips;
	private int mTotalTrips;

	public DRTSimulation(ArrayList<Trip> trips) {
		mTrips = trips;
		mTotalTrips = trips.size();
		mEventQueue = new PriorityQueue<SimEvent>();
		mRebus = new Rebus();
		mVehiclePlans = new Vehicle[0];
		mRejectedTrips = new ArrayList<Trip>();
	}
	
	/**
	 * Runs the DRT simulation, but parses the specified file to determine
	 * how many vehicles to model
	 */
	public void runSimulation(String filePath) {
		
		Log.info(TAG, "Running simulation");			
		// If a file path is specified, parse out the number of vehicles to generate
		// Otherwise, use the value defined in Constants
		int vehicleQuantity = 0;
		if(filePath == null) {
			vehicleQuantity = Constants.VEHCILE_QUANTITY;
		} else {
			File file = new File(filePath);
			Log.info(TAG, "Loading number of vehicles from: " + file.getPath());
	
			try {
				Scanner scanner = new Scanner(file);
				while (scanner.hasNextLine()) {
					String[] tokens = scanner.nextLine().split(" ");
					if(tokens[0].equals(NUM_VEHICLES_FILE_LBL)) {
						vehicleQuantity = Integer.valueOf(tokens[1]);
						break;
					}						
				}
				scanner.close();
				if(vehicleQuantity == 0)
					throw new IllegalArgumentException("Vehicle quantity not specified in file at " + filePath);				
			} catch(FileNotFoundException ex) {
				Log.error(TAG, "Unable to find trip file at: " + filePath);
				ex.printStackTrace();
				System.exit(1);
			}
		}
	
		generateVehicles(vehicleQuantity);
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
	
	/**
	 * Runs the DRT simulation.
	 */
	public void runSimulation() {
		runSimulation(null);
	}
	
	private void generateVehicles(int numVehicles) {
		Log.info(TAG, "Generating " + numVehicles + " vehicles");
		mVehiclePlans = new Vehicle[numVehicles];
		for(int i = 0; i < numVehicles; i++) {
			mVehiclePlans[i] = new Vehicle(i+1);
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
		
		mRebus.onRebusFinished();
		
		for(Vehicle v : mVehiclePlans) {
			Log.info(TAG, v.scheduleToString());
		}
		
		// Print total rejected trips and rate
		float rejectionRate = (float) mRejectedTrips.size() / mTotalTrips * 100;
		Log.info(TAG, "Total trips simulated: " + mTotalTrips + ". Total trips rejected by REBUS: " + mRejectedTrips.size() +
				". Rejection rate: " + rejectionRate + "%");
		
		// Write vehicle routing files
		appendTripVehicleTxtFile();
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
		Log.info(TAG, mRebus.getQueueSize() + " static jobs queued");
		// Now that all static trips are enqueued we can schedule them.
		mRejectedTrips.addAll(mRebus.scheduleQueuedJobs(mVehiclePlans));
	}
	
	/**
	 * Consumes a new trip request event
	 * @param event Trip request event
	 * @parm schedule Specifies if scheduling should be executed immediately.
	 */
	private void consumeNewRequestEvent(SimEvent event, boolean schedule) {
		Trip t = event.getTrip();
		// Enqueue the trip in the REBUS queue, and schedule if requested
		mRebus.enqueueTripRequest(t);
		if(schedule) {
			mRejectedTrips.addAll(mRebus.scheduleQueuedJobs(mVehiclePlans));
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
	
	/**
	 * Append the vehicle quantity to the trip/vehicle file
	 */
	private void appendTripVehicleTxtFile() {
		ArrayList<String> text = new ArrayList<String>(1);
		text.add(NUM_VEHICLES_FILE_LBL + " " + mVehiclePlans.length);
		DRTUtils.writeTxtFile(text, Constants.TRIPS_VEHICLES_PREFIX_TXT);
	}
	
	private void writeStatisticsTxtFile() {
		
	}
	
	/**
	 * Write vehicle schedules to a shapefile
	 */
	private void writeScheduleShpFile() {
		// Build feature type and feature collection
		SimpleFeatureType featureType = buildFeatureType();
		SimpleFeatureCollection collection = createShpFeatureCollection(featureType);
		
		// Format time and create filename
		String dateFormatted = DRTUtils.formatMillis(TacomaDRTMain.mTripGenStartTime);
		String filename = TacomaDRTMain.getRouteShpSimDirectory() + Constants.ROUTE_PREFIX_SHP + dateFormatted + ".shp";
        File shpFile = new File(filename);
        
        ShapefileWriter.writeShapefile(featureType, collection, shpFile);
	}
	
	/**
	 * Creates a Line feature type for the the route shapefile
	 * @return A line feature type
	 */
	private SimpleFeatureType buildFeatureType() {
		// Build feature type
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("TripRoutes");
        builder.setCRS(DefaultGeographicCRS.WGS84); // long/lat projection system
        builder.add("Route", LineString.class); // Geo data
        builder.add("Vehicle", String.class); // Vehicle identifier
        
        final SimpleFeatureType featureType = builder.buildFeatureType();
        return featureType;
	}
	
	private SimpleFeatureCollection createShpFeatureCollection(SimpleFeatureType featureType) {
	   // New collection with feature type
		SimpleFeatureCollection collection = FeatureCollections.newCollection();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        
        // Loop through vehicles
        for(Vehicle v : mVehiclePlans) {
        	VehicleScheduleNode curNode = v.getScheduleRoot();
        	ArrayList<Coordinate> coords = new ArrayList<Coordinate>();
        	// Add all pickup/dropoff points to the line
        	while(curNode != null) {
        		VehicleScheduleJob curJob = curNode.getJob();
        		Point2D loc = null;
        		if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP)
        			loc = curJob.getTrip().getFirstEndpoint();
        		else if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_DROPOFF)
        			loc = curJob.getTrip().getSecondEndpoint();

        		if(loc != null)
        			coords.add(new Coordinate(loc.getX(), loc.getY()));	
        		
        		curNode = curNode.getNext();
        	}	     
        	
        	// Convert ArrayList to array, and build a line
        	Coordinate[] coordArray = new Coordinate[coords.size()];
        	coords.toArray(coordArray);
        	LineString line = geometryFactory.createLineString(coordArray);
        	
        	// Build the feature
            featureBuilder.add(line); // Geo data
            featureBuilder.add(String.valueOf(v.getIdentifier())); // Trip identifier
            SimpleFeature feature = featureBuilder.buildFeature(null);
            ((DefaultFeatureCollection)collection).add(feature);			
        }
        return collection;
	}
}
