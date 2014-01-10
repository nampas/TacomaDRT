package edu.pugetsound.npastor.simulation;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import edu.pugetsound.npastor.routing.RouteCache;
import edu.pugetsound.npastor.routing.RoutefinderTask;
import edu.pugetsound.npastor.routing.Vehicle;
import edu.pugetsound.npastor.routing.VehicleScheduleJob;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.ShapefileWriter;
import edu.pugetsound.npastor.utils.Trip;

public class DRTSimulation {
	
	public static final String TAG = "DRTSimulation";
	
	private static final String NUM_VEHICLES_FILE_LBL = "num_vehicles";
	
	private static final int ROUTE_UPDATE_INCREMENT = 10; // Update route progress at this percentage increment
	
	private static final String COMMA_DELIM = ",";
	
	private ArrayList<Trip> mTrips;
	private PriorityQueue<SimEvent> mEventQueue;
	private Vehicle[] mVehiclePlans;
	private Rebus mRebus;
	private boolean mFromFile;
	private RouteCache mCache;
	private ArrayList<Trip> mRejectedTrips;
	private int mTotalTrips;

	public DRTSimulation(ArrayList<Trip> trips, boolean fromFile) {
		mFromFile = fromFile;
		mTrips = trips;
		mTotalTrips = trips.size();
		mEventQueue = new PriorityQueue<SimEvent>();
		mVehiclePlans = new Vehicle[0];
		mRejectedTrips = new ArrayList<Trip>();
	}
	
	/**
	 * Runs the DRT simulation, but parses the specified file to determine
	 * how many vehicles to model
	 */
	public void runSimulation() {
			
		Log.iln(TAG, "Running simulation");
		
		if(mCache == null) {
			throw new IllegalStateException("Cache has not been instantiated. Call buildCache() before runSimulation()");
		}
		mRebus = new Rebus(mCache, 0);
		mRebus.printEnabledHints();
		
		// If a file path is specified, parse out the number of vehicles to generate
		// Otherwise, use the value defined in Constants
		int vehicleQuantity = -1;
		if(!mFromFile) {
			vehicleQuantity = Constants.VEHCILE_QUANTITY;
		} else {
			File file = new File(TacomaDRTMain.getSourceTripVehDir());
			Log.iln(TAG, "Loading number of vehicles from: " + file.getPath());
	
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
				if(vehicleQuantity == -1)
					throw new IllegalArgumentException("Vehicle quantity not specified in file at " + file.getPath());				
			} catch(FileNotFoundException ex) {
				Log.e(TAG, "Unable to find trip file at: " + file.getPath());
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
	
	private void generateVehicles(int numVehicles) {
		Log.iln(TAG, "Generating " + numVehicles + " vehicles");
		mVehiclePlans = new Vehicle[numVehicles];
		for(int i = 0; i < numVehicles; i++) {
			mVehiclePlans[i] = new Vehicle(i+1);
		}
	}
	
	/**
	 * Enqueues all trip requests in the event queue
	 */
	private void enqueueTripRequestEvents() {
		Log.iln(TAG, "Enqueueing all trip request events in simulation queue");
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
		Log.iln(TAG, "*************************************");
		Log.iln(TAG, "       SIMULATION COMPLETE");
		Log.iln(TAG, "*************************************");
		
		mRebus.onRebusFinished();
		
		for(Vehicle v : mVehiclePlans) {
			Log.iln(TAG, v.scheduleToString());
		}
		
		// Print total rejected trips and rate
		float rejectionRate = (float) mRejectedTrips.size() / mTotalTrips * 100;
		Log.iln(TAG, "Total trips simulated: " + mTotalTrips + ". Total trips rejected by REBUS: " + mRejectedTrips.size() +
				". Rejection rate: " + rejectionRate + "%");
		
		// Write simulation files
		appendTripVehicleTxtFile();
		writeScheduleTxtFile();
		writeScheduleShpFile();
		writeStatisticsTxtFile();
	}
	
	/**
	 * Schedule all trips that are known a priori. That is, all trips which
	 * are known to the agency before service hours begin. These are the static requests.
	 */
	private void doAPrioriScheduling() {
		Log.iln(TAG, "Doing a priori scheduling (all static trip requests)");
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
		Log.iln(TAG, mRebus.getQueueSize() + " static jobs queued");
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
	
	// ******************************
	//         SIM FILE STUFF
	// ******************************
	
	/**
	 * Write vehicle schedules to a text file
	 */
	private void writeScheduleTxtFile() {

		// Build text list
		ArrayList<String> text = new ArrayList<String>();
		
		// Add all vehicle schedules
		for(Vehicle v : mVehiclePlans) {
			text.add(v.scheduleToString());
		}	
		
		// Add all rejected trips
		text.add("\r\n REJECTED TRIPS \r\n");
		for(Trip t : mRejectedTrips) {
			text.add(t.toString());
		}

		// Write file
		DRTUtils.writeTxtFile(text, Constants.SCHED_TXT);
	}
	
	/**
	 * Append the vehicle quantity to the trip/vehicle file
	 */
	private void appendTripVehicleTxtFile() {
		ArrayList<String> text = new ArrayList<String>(1);
		text.add(NUM_VEHICLES_FILE_LBL + " " + mVehiclePlans.length);
		DRTUtils.writeTxtFile(text, Constants.TRIPS_VEHICLES_TXT);
		DRTUtils.writeTxtFile(text, Constants.TRIPS_READABLE_TXT);
	}
	
	private void writeStatisticsTxtFile() {
		ArrayList<String> text = new ArrayList<String>();

		String headers = "Vehicle" + COMMA_DELIM
						+ "Trips serviced" + COMMA_DELIM
						+ "Max travel time dev" + COMMA_DELIM
						+ "Avg travel time dev" + COMMA_DELIM
						+ "Max pickup dev" + COMMA_DELIM
						+ "Avg pickup dev" + COMMA_DELIM;
		
		text.add(headers);
				
		// Global travel time deviation max and pickup deviation max
		int globalMaxTrTimeDev = 0;
		int globalMaxPickupDev = 0;
		// Global travel time deviation and pickup deviation totals
		double globalTotalTrTimeDev = 0;
		double globalTotalPickupDev = 0;
		for(Vehicle curVeh : mVehiclePlans) {
			StatsWrapper result = calcVehicleStats(curVeh);
			// Build vehicle statistics string
			String vehString = "" + curVeh.getIdentifier() + COMMA_DELIM
							+ result.numTrips + COMMA_DELIM
							+ result.maxTravelTimeDev + COMMA_DELIM
							+ result.avgTravelTimeDev + "%" + COMMA_DELIM
							+ result.maxPickupDev + COMMA_DELIM		
							+ result.avgPickupDev + "%" + COMMA_DELIM;
			text.add(vehString);
			
			// Update global statistics
			globalTotalTrTimeDev += result.avgTravelTimeDev;
			globalTotalPickupDev += result.avgPickupDev;
			globalMaxTrTimeDev = (int) Math.max(globalMaxTrTimeDev, result.maxTravelTimeDev);
			globalMaxPickupDev = (int) Math.max(globalMaxPickupDev, result.maxPickupDev);
		}
		
		// Build global statistics string
		globalTotalTrTimeDev = globalTotalTrTimeDev / mVehiclePlans.length;
		globalTotalPickupDev = globalTotalPickupDev / mVehiclePlans.length;
		String globalString = "Total" + COMMA_DELIM
							+ mTrips.size() + COMMA_DELIM
							+ globalMaxTrTimeDev + COMMA_DELIM
							+ globalTotalTrTimeDev + "%" + COMMA_DELIM
							+ globalMaxPickupDev + COMMA_DELIM
							+ globalTotalPickupDev + "%" + COMMA_DELIM;
		text.add(globalString);
		
		DRTUtils.writeTxtFile(text, Constants.STATS_CSV);
	}
	
	private StatsWrapper calcVehicleStats(Vehicle v) {
		StatsWrapper result = new StatsWrapper();
		ArrayList<VehicleScheduleJob> schedule = v.getSchedule();
		result.numTrips = schedule.size() / 2 - 1; // Ignore start/end jobs
		int pickupDevTotal = 0;  // Running pickup deviation total
		int travelTimeDevTotal = 0; // Running travel time total

		for(VehicleScheduleJob job : schedule) {
			switch(job.getType()) {
			case VehicleScheduleJob.JOB_TYPE_START:
			case VehicleScheduleJob.JOB_TYPE_END:
				break;
			case VehicleScheduleJob.JOB_TYPE_PICKUP:
				int curPickupDev = job.getServiceTime() - job.getStartTime();
				pickupDevTotal += curPickupDev;
				result.maxPickupDev = Math.max(result.maxPickupDev, curPickupDev);
				break;
			case VehicleScheduleJob.JOB_TYPE_DROPOFF:
				int curTrTimeDev = job.getServiceTime() - job.getStartTime();
				travelTimeDevTotal += curTrTimeDev;
				result.maxTravelTimeDev = Math.max(result.maxTravelTimeDev, curTrTimeDev);
				break;
			}
		}
		
		result.avgPickupDev = (double) pickupDevTotal / result.numTrips / 2;
		result.avgTravelTimeDev = (double) travelTimeDevTotal / result.numTrips / 2;
		
		return result;		
	}
	
	/**
	 * Write vehicle schedules to a shapefile
	 */
	private void writeScheduleShpFile() {
		// Build feature type and feature collection
		SimpleFeatureType featureType = buildFeatureType();
		SimpleFeatureCollection collection = createShpFeatureCollection(featureType);
		
		// Format time and create filename
		String dateFormatted = DRTUtils.formatMillis(TacomaDRTMain.tripGenStartTime);
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
        	ArrayList<VehicleScheduleJob> schedule = v.getSchedule();
        	Coordinate[] coordinates = new Coordinate[schedule.size()-2];
        	// Add all pickup/dropoff points to the line
        	for(int i = 1; i < schedule.size()-1; i++) {
        		VehicleScheduleJob curJob = schedule.get(i);
        		Point2D loc = null;
        		if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_PICKUP)
        			loc = curJob.getTrip().getFirstEndpoint();
        		else if(curJob.getType() == VehicleScheduleJob.JOB_TYPE_DROPOFF)
        			loc = curJob.getTrip().getSecondEndpoint();

        		coordinates[i-1] = new Coordinate(loc.getX(), loc.getY());	
        	}	        	
        	LineString line = geometryFactory.createLineString(coordinates);
        	
        	// Build the feature
            featureBuilder.add(line); // Geo data
            featureBuilder.add(String.valueOf(v.getIdentifier())); // Trip identifier
            SimpleFeature feature = featureBuilder.buildFeature(null);
            ((DefaultFeatureCollection)collection).add(feature);			
        }
        return collection;
	}
	
	// **************************************
	//             CACHE STUFF
	// **************************************
	
	/**
	 * Builds the route cache. If this simulation instance is a re-run, we can use
	 * the previously generated cache. Otherwise, we compute every route...
	 */
	public void buildCache() {
		mCache = new RouteCache(mTrips.size());
		// If we're re-running a simulation, we can re-use the previous routes
		if(mFromFile) {
			buildCacheFromFile();
		} else {
			doAllRoutefinding();
		}
		writeCacheToFile();
	}
	
	/**
	 * Delegates routefinding to worker threads
	 */
	private void doAllRoutefinding() {
		int numThreads = TacomaDRTMain.numThreads;
		
		long routeStartTime = System.currentTimeMillis();
		Log.iln(TAG, "Building route cache with " + numThreads + " threads. This may take a while...");
		CountDownLatch latch = new CountDownLatch(numThreads); // To inform of thread completion
		AtomicInteger progress = new AtomicInteger(); // For tracking caching progress
		int totalRoutes = (int) Math.pow(mTrips.size()*2, 2);
		
		// Number of trips each thread will be calculating routes from
		int threadTaskSize = mTrips.size() / numThreads;
		
		for(int i = 0; i < numThreads; i++) {
			int startIndex = threadTaskSize * i;
			int endIndex = (i+1 == numThreads) ? mTrips.size() : startIndex + threadTaskSize;
			RoutefinderTask routeTask = new RoutefinderTask(mCache, mTrips, startIndex, endIndex, latch, progress);
			new Thread(routeTask).start();
		}
		
		// Alternate waiting and updating progress. You should bring a book.
		Log.i(TAG, "Routing at 0%", false, true);
		int lastPercent = -1;
		try {
			boolean tasksComplete = false;
			while(!tasksComplete) {
				int percent = (int)(((double)progress.get() / totalRoutes) * 100);
				if(lastPercent + ROUTE_UPDATE_INCREMENT <= percent) {
					Log.i(TAG, ", " + percent + "%", true,
							(percent % 25 == 0 && percent / 25 != lastPercent / 25) ? true : false);
					lastPercent = percent;
				}
				tasksComplete = latch.await(5, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
		long routeEndTime = System.currentTimeMillis();
		TacomaDRTMain.printTime("All routes calculated and cached in ", routeEndTime, routeStartTime);
	}
	
	/**
	 * Moves a source cache into memory
	 */
	private void buildCacheFromFile() {
		File file = new File(TacomaDRTMain.getSourceCacheDir());
		Log.iln(TAG, "Loading cache from file at " + file.getPath());
		
		Scanner scanner;
		try {
			scanner = new Scanner(file);
			int size = mTrips.size()*2;
			for(int i = 0; i < size; i++) {
				String[] tokens = scanner.nextLine().split(COMMA_DELIM);
				for(int j = 0; j < size; j++) {
					mCache.putDirect(i, j, Byte.valueOf(tokens[j]));
				}				
			}
			scanner.close();	
		} catch(FileNotFoundException ex) {
			Log.e(TAG, "Unable to find trip file at: " + file.getPath());
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Writes the cache to file. Re-runs of this simulation can read 
	 * the cache file to avoid recomputing travel times
	 */
	private void writeCacheToFile() {
		
		// Get filename
		String path = TacomaDRTMain.getSimulationDirectory() + Constants.ROUTE_CACHE_CSV;
		Log.iln(TAG, "Writing cache file to: " + path);
		
		// Build a table
		try {
			FileWriter writer = new FileWriter(path, true);
			PrintWriter lineWriter = new PrintWriter(writer);
			
			int size = mTrips.size() * 2;
			for(int i = 0; i < size; i++) {
				StringBuilder str = new StringBuilder();
				for(int j = 0; j < size; j++) {
					str.append(mCache.getDirect(i, j) + COMMA_DELIM);
				}
				// Write distances to file
				lineWriter.println(str.toString());	
			}

			lineWriter.close();
			writer.close();
			
			// This cache is valuable! Set read only
			new File(path).setReadOnly();
			Log.iln(TAG, "  File succesfully writen at:" + path);
		} catch (IOException ex) {
			Log.e(TAG, "Unable to write to file");
			ex.printStackTrace();
		}		
	}
	
	private class StatsWrapper {
		private double maxPickupDev;
		private double avgPickupDev;
		private double maxTravelTimeDev;
		private double avgTravelTimeDev;
		private int numTrips;

		public StatsWrapper() {
			numTrips = 0;
			maxPickupDev = -1;
			avgPickupDev = -1;
			maxTravelTimeDev = -1;
			avgTravelTimeDev = -1;
		}
	}
}
