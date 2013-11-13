package edu.pugetsound.npastor.riderGen;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
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

import com.graphhopper.GHResponse;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import edu.pugetsound.npastor.TacomaDRTMain;
import edu.pugetsound.npastor.routing.Routefinder;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.DRTUtils;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.RiderChars;
import edu.pugetsound.npastor.utils.RiderChars.DayDivision;
import edu.pugetsound.npastor.utils.ShapefileWriter;
import edu.pugetsound.npastor.utils.Trip;

/**
 * Generates all daily trips on the DRT network
 * @author Nathan Pastor
 *
 */
public class TripGenerator {

	public final static String TAG = "TripGenerator";
	
	private ArrayList<Trip> mTrips;
	private RiderChars mRiderChars;
	private PCAgeEmployment mPCData;
	private Random mRandom;

	public TripGenerator() {
		mTrips = new ArrayList<Trip>();
		mRiderChars = new RiderChars();
		mPCData = new PCAgeEmployment();
		mRandom = new Random();
	}

	/**
	 * Begins the trip generation process
	 */
	public void generateTrips() {
		for(int i = 0; i < Constants.TOTAL_TRIPS; i++) {
			mTrips.add(new Trip(i+1));
		}
		// Generate all trip attributes. ORDER IS IMPORTANT!
		generateTripTypes();
		generateAges();
		assignDirections();
		generateEndpointTracts();
		generateEndpoints();
		generatePickupTimes();
//		for(int i = 0; i < mTrips.size(); i++)
//			Log.info(TAG, mTrips.get(i).toString());
				
		onTripsGenerated();
	}
	
	private void onTripsGenerated() {
		generateDirections();
		writeTripsToFile();
		writeTripGeoToShp();
	}
	
	/**
	 * Generates trips from a trip log. This allows for the same set of data to be used
	 * across instances of the model, so we can test how different variables affect the
	 * results
	 */
	public void generateTripsFromFile(String tripLogPath) {
		File file = new File(tripLogPath);
		Log.info(TAG, "Loading trips from: " + file.getPath());
		
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String[] tokens = scanner.nextLine().split(" ");
				// Build the trip from the file line
				Trip newTrip = new Trip(Integer.valueOf(tokens[0]));
				newTrip.setTripType(Integer.valueOf(tokens[1]));
				newTrip.setRiderAge(Integer.valueOf(tokens[2]));
				newTrip.setDirection(tokens[3].equals("1") ? true : false);
				newTrip.setFirstTract(tokens[4]);
				newTrip.setFirstEndpoint(new Point2D.Double(Double.valueOf(tokens[5]), Double.valueOf(tokens[6])));
				newTrip.setSecondTract(tokens[7]);
				newTrip.setSecondEndpoint(new Point2D.Double(Double.valueOf(tokens[8]), Double.valueOf(tokens[9])));
				newTrip.setPickupTime(Integer.valueOf(tokens[10]));
				newTrip.setCalInTime(Integer.valueOf(tokens[11]));
				
				// And add trip to list
				mTrips.add(newTrip);
			}
			scanner.close();
		} catch(FileNotFoundException ex) {
			Log.error(TAG, "Unable to find trip file at: " + tripLogPath);
			ex.printStackTrace();
			System.exit(1);
		}
		
		onTripsGenerated();
	}
	
	public ArrayList<Trip> getTrips() {
		return mTrips;
	}

	/**
	 * Generates ages based on APTA age distributions
	 */
	private void generateAges() {
		// Will contain all the generated ages
		Log.info(TAG, "Generating trip ages");
		ArrayList<Integer> ages = new ArrayList<Integer>();

		Object[] keys = mRiderChars.getAgeGroupPcts().keySet().toArray();
		// Loop through the key set, process each age group
		for(Object curKey : keys) {
			double percentage = mRiderChars.getAgeGroupPct((Integer)curKey);
			int groupTotal = (int)Math.ceil(percentage * Constants.TOTAL_TRIPS / 100); // Total riders in this group
			switch((Integer)curKey) {
				case Constants.APTA_AGE_0_14:
					ages.addAll(generateAgesInRange(groupTotal, 0, 14));
					break;
				case Constants.APTA_AGE_15_19:
					ages.addAll(generateAgesInRange(groupTotal, 15, 19));
					break;
				case Constants.APTA_AGE_20_24:
					ages.addAll(generateAgesInRange(groupTotal, 20, 24));
					break;
				case Constants.APTA_AGE_25_34:
					ages.addAll(generateAgesInRange(groupTotal, 25, 34));
					break;
				case Constants.APTA_AGE_35_44:
					ages.addAll(generateAgesInRange(groupTotal, 35, 44));
					break;
				case Constants.APTA_AGE_45_54:
					ages.addAll(generateAgesInRange(groupTotal, 45, 54));
					break;
				case Constants.APTA_AGE_55_64:
					ages.addAll(generateAgesInRange(groupTotal, 55, 64));
					break;
				case Constants.APTA_AGE_65_OVER:
					ages.addAll(generateAgesInRange(groupTotal, 65, 80)); //TODO: DETERMINE MAX AGE HERE
			}
		}
		Log.info(TAG, "Total ages genererated: " + ages.size());
		// Finally add ages to trips, but do it randomly
		for(Trip t : mTrips) {
			int randomIndex = mRandom.nextInt(ages.size());
			t.setRiderAge(ages.get(randomIndex)); // Pick a random age out of ages list
			ages.remove(randomIndex);
		}
	}

	private ArrayList<Integer> generateAgesInRange(int quantity, int min, int max) {
		ArrayList<Integer> ages = new ArrayList<Integer>();
		for(int i = 0; i < quantity; i++) {
			int age = mRandom.nextInt(max - min + 1) + min; // +1 for exclusivity
			ages.add(age);
		}
		return ages;
	}

	private void generateTripTypes() {
		
		Log.info(TAG, "Generating trip types");
		// Will contain all the generated ages
		ArrayList<Integer> trips = new ArrayList<Integer>();

		Object[] keys = mRiderChars.getTripTypePcts().keySet().toArray();
		// Loop through the key set, process each trip type
		for(int i = 0; i < keys.length; i++) {
			int curKey = (Integer) keys[i];
			double percentage = mRiderChars.getTripTypePct(curKey);
			int groupTotal = (int) Math.ceil(percentage * Constants.TOTAL_TRIPS / 100); // Total riders in this group
			for(int j = 0; j < groupTotal; j++) {
				trips.add(curKey);
			}
		}

		// Finally add ages to trips, but do it randomly
		for(int i = 0; i < mTrips.size(); i++) {
			int randomIndex = mRandom.nextInt(trips.size());
			mTrips.get(i).setTripType(trips.get(randomIndex)); // Pick a random trip out of trips list
			trips.remove(randomIndex);
		}
	}

	private void assignDirections() {
		Log.info(TAG, "Assigning trip directions");
		// Half of list is inbound, other half is outbound
		ArrayList<Boolean> directions = new ArrayList<Boolean>();
		for(int i = 0; i < mTrips.size(); i++) {
			if (i < mTrips.size() / 2)
				directions.add(true);
			else 
				directions.add(false);
		}

		// Add direction to trips randomly
		for(int i = 0; i < mTrips.size(); i++) {
			int randomIndex = mRandom.nextInt(directions.size());
			mTrips.get(i).setDirection(directions.get(randomIndex)); // Pick a random trip out of tripls list
		}
	}
	
	private void generateEndpointTracts() {
		Log.info(TAG, "Generating trip endpoint tracts");
		for(Trip t: mTrips) {
			int[] riderAgeGroup = {DRTUtils.getGroupForAge(t.getRiderAge())};
			String firstTract = Trip.TRACT_NOT_SET;
			String secondTract = Trip.TRACT_NOT_SET;
			switch(t.getTripType()) {
				case Constants.TRIP_COMMUTE:
					int[] commuteCode = {Constants.PSRC_TOTAL};
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					secondTract = mPCData.getWeightedTract(commuteCode, true);
					break;
				case Constants.TRIP_MEDICAL_DENTAL:
					int[] medCodes = {Constants.PSRC_SERVS, Constants.PSRC_GOVT};
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					secondTract = mPCData.getWeightedTract(medCodes, true);
					break;
				case Constants.TRIP_OTHER:
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					//TODO: should this be random?
					secondTract = mPCData.getRandomTract();
					break;
				case Constants.TRIP_PERSONAL_BUSINESS:
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					//TODO: should this be random?
					secondTract = mPCData.getRandomTract();
					break;
				case Constants.TRIP_SCHOOL:
					int [] schoolCode = {Constants.PSRC_EDU};
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					secondTract = mPCData.getWeightedTract(schoolCode, true);
					break;
				case Constants.TRIP_SHOPPING_DINING:
					int[] shopCodes = {Constants.PSRC_SERVS, Constants.PSRC_RETAIL};
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					secondTract = mPCData.getWeightedTract(shopCodes, true);
					break;
				case Constants.TRIP_SOCIAL:
					firstTract = mPCData.getWeightedTract(riderAgeGroup, false);
					//TODO: should this be weighted for total population level?
					secondTract = mPCData.getWeightedTract(riderAgeGroup, false);
			}
			if(t.getDirection()) {
				t.setFirstTract(firstTract);
				t.setSecondTract(secondTract);
			} else {
				t.setFirstTract(secondTract);
				t.setSecondTract(firstTract);
			}
		}
	}
	
	//TODO: determine time distribution across day
	//TODO: determine when requests are made known to agency
	/**
	 * Generates a trip pickup time and the time when the trip was made known to the agency.
	 * Both in minute precision
	 */
	private void generatePickupTimes() {
		
		Log.info(TAG, "Generating pickup times");
		int minRequestableTime = Constants.BEGIN_OPERATION_HOUR * 60;
		int maxRequestWindow = Constants.END_REQUEST_WINDOW * 60;
		
		Double[] percentByHour = buildDayDistribution();
		double percentDynamicRequests = mRiderChars.getDynamicRequestPct();
		
		int unsatisfiableConditions = 0;
		for(Trip t : mTrips) {
			int requestTime = 0;
			int callInTime = 0;
			// First calculate request time
			
			double random = mRandom.nextDouble() * 100;
			double runningTotal = 0;
			for(int i = Constants.BEGIN_OPERATION_HOUR; i < Constants.END_OPERATION_HOUR; i++) {
				runningTotal += percentByHour[i];
				if(random < runningTotal) {
					requestTime = i;
					break;
				}
			} 
			requestTime = requestTime * 60 + mRandom.nextInt(60);
	
			// Then set time at which trip was requested
			double requestVal = mRandom.nextDouble() * 100;
			if(requestVal < percentDynamicRequests) {
				// Max request time will be the agency determined cutoff, or the requested time minus the buffer
				int maxRequestTime = Math.min(requestTime - Constants.CALL_REQUEST_BUFFER_MINS, maxRequestWindow);
				if(maxRequestTime < minRequestableTime) {
					//TODO: fix this!!
					unsatisfiableConditions++;
				} else {
					int windowMins = maxRequestTime - minRequestableTime;
					callInTime = mRandom.nextInt(windowMins + 1) + minRequestableTime;
				}
			}
			t.setPickupTime(requestTime);
			t.setCalInTime(callInTime);
		}
		Log.info(TAG, "Dynamic request: There were " + unsatisfiableConditions + 
				" times where max request time is less than minimum requestable time. Unsatisfiable condition");
	}
	
	// Using trip distribution data from RiderChars file, makes a list of trip distribution by hour
	private Double[] buildDayDistribution() {
		Double[] percentByHour = new Double[24];
		HashMap<Integer, DayDivision> tripsByPeriod = mRiderChars.getTripDistributiions();
		
		// First do peak periods
		DayDivision morningPeak = tripsByPeriod.get(Constants.MORN_PEAK_PERIOD);
		int hrLength = morningPeak.getLength();
		for(int i = 0; i < hrLength; i++) {
			percentByHour[i + morningPeak.getStart()] = morningPeak.getPercentage() / hrLength;
		}
		DayDivision afternoonPeak = tripsByPeriod.get(Constants.AFTNOON_PEAK_PERIOD);
		hrLength = afternoonPeak.getLength();
		for(int i = 0; i < hrLength; i++) {
			percentByHour[i + afternoonPeak.getStart()] = afternoonPeak.getPercentage() / hrLength;
		}
		
		// Then fill in the rest of the day
		DayDivision nonPeak = tripsByPeriod.get(Constants.MORNING_PERIOD);
		hrLength = morningPeak.getStart() - Constants.BEGIN_OPERATION_HOUR;
		for(int i = 0; i < hrLength; i++) {
			percentByHour[Constants.BEGIN_OPERATION_HOUR + i] = nonPeak.getPercentage() / hrLength;
		}
		nonPeak = tripsByPeriod.get(Constants.DAY_PERIOD);
		hrLength = afternoonPeak.getStart() - morningPeak.getStart() - morningPeak.getLength();
		for(int i = 0; i < hrLength; i++) {
			percentByHour[morningPeak.getStart() + morningPeak.getLength() + i] = nonPeak.getPercentage() / hrLength;
		}
		nonPeak = tripsByPeriod.get(Constants.EVENING_PERIOD);
		hrLength = Constants.END_OPERATION_HOUR - afternoonPeak.getStart() - afternoonPeak.getLength();
		for(int i = 0; i < hrLength; i++) {
			percentByHour[afternoonPeak.getStart() + afternoonPeak.getLength() + i] = nonPeak.getPercentage() / hrLength;
		}

		return percentByHour;
	}
	
	private void generateEndpoints() {
		Log.info(TAG, "Generating trip endpoints");
		TractPointGenerator pointGen = new TractPointGenerator();
		for(int i = 0; i < mTrips.size(); i++) {
			Trip t = mTrips.get(i);
			t.setFirstEndpoint(pointGen.randomPointInTract(t.getFirstTract()));
			t.setSecondEndpoint(pointGen.randomPointInTract(t.getSecondTract()));
			if(i % 500 == 0)
				Log.info(TAG, "  Endpoint generation at trip " + i);
		}
	}
	
	/**
	 * Generates directions between trip endpoints
	 */
	private void generateDirections() {
		Log.info(TAG, "Generating trip directions");
		Routefinder router = new Routefinder();
		for(Trip t : mTrips) {
			GHResponse routeResponse;
			if(t.getDirection())
				routeResponse = router.findRoute(t.getFirstEndpoint(), t.getSecondEndpoint());
			else 
				routeResponse = router.findRoute(t.getSecondEndpoint(), t.getFirstEndpoint());
			
			t.setRoute(routeResponse);
		}
	}
	
	/**
	 * Writes the generated trips to 2 files. There is a "readable" file which contains an easier to read
	 * representation of the trips, and a separate file which contains an easier to parse representation.
	 * The latter is used for generateTripsFromFile()
	 */
	private void writeTripsToFile() {
		// Format the simulation start time
		String dateFormatted = DRTUtils.formatMillis(TacomaDRTMain.mTripGenStartTime);
		
		// Get filename and add current time and file extension
		String filename = TacomaDRTMain.getSimulationDirectory() + Constants.TRIPS_PREFIX_TXT + dateFormatted + ".txt";
		String filenameReadable = TacomaDRTMain.getSimulationDirectory() + Constants.TRIPS_READABLE_PREFIX_TXT + dateFormatted + ".txt";
		Log.info(TAG, "Writing trips to: " + filename);
		Log.info(TAG, "Writing trips readable to: " + filenameReadable);
		
		// Write to 2 files
		try {
			FileWriter writer = new FileWriter(filename, false);
			PrintWriter lineWriter = new PrintWriter(writer);
			
			FileWriter readableWriter = new FileWriter(filenameReadable, false);
			PrintWriter readableLineWriter = new PrintWriter(readableWriter);
			
			for(Trip t : mTrips) {
				// Write to parsable file
				lineWriter.println(buildParsableTripFileLine(t));
				
				// Write readable file
				String readableLine = t.toString().replace("\n", ""); // Get rid of all line breaks
				readableLineWriter.println(readableLine);
			}
			lineWriter.close();
			writer.close();
			readableLineWriter.close();
			readableWriter.close();
			Log.info(TAG, "  File succesfully writen at:" + filename);
			Log.info(TAG, "  File succesfully writen at:" + filenameReadable);
		} catch (IOException ex) {
			Log.error(TAG, "Unable to write to file");
			ex.printStackTrace();
		}
	}
	
	/**
	 * Helper method for writing trip txt files. Builds a line of the file, which contains all trip data
	 * @param t The trip
	 * @return All trip data in a string
	 */
	private String buildParsableTripFileLine(Trip t) {
		String sp = " ";
		String line = t.getIdentifier() + sp + 
				t.getTripType() + sp +
				t.getRiderAge() + sp + 
				(t.getDirection() ? "1" : "0") + sp +
				t.getFirstTract() + sp +
				t.getFirstEndpoint().getX() + sp +
				t.getFirstEndpoint().getY() + sp +
				t.getSecondTract() + sp +
				t.getSecondEndpoint().getX() + sp +
				t.getSecondEndpoint().getY() + sp +
				t.getPickupTime() + sp +
				t.getCallInTime();
		return line;
	}
	
	private SimpleFeatureType buildFeatureType() {
		// Build feature type
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("TripEndpoints");
        builder.setCRS(DefaultGeographicCRS.WGS84); // long/lat projection system
        builder.add("Location", Point.class); // Geo data
        builder.add("Trip", String.class); // Trip identifier
        builder.add("Tract", String.class); // Tract number the point falls in
        
        final SimpleFeatureType featureType = builder.buildFeatureType();
        return featureType;
	}
	
	private SimpleFeatureCollection createGeoFeatureCollection(SimpleFeatureType featureType) {
		
        // New collection with feature type
		SimpleFeatureCollection collection = FeatureCollections.newCollection();
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        
        // Loop through trips, adding endpoints
        for(Trip t : mTrips) {
        	Point firstEndpoint = geometryFactory.createPoint(new Coordinate(t.getFirstEndpoint().getX(), t.getFirstEndpoint().getY(), 0.0));
        	Point secondEndpoint = geometryFactory.createPoint(new Coordinate(t.getSecondEndpoint().getX(), t.getSecondEndpoint().getY(), 0.0));
        	
        	// Add both feature to collection
        	if(t.getFirstTract() != Trip.TRACT_NOT_SET) {
	            featureBuilder.add(firstEndpoint); // Geo data
	            featureBuilder.add(String.valueOf(t.getIdentifier())); // Trip identifier
	            featureBuilder.add(String.valueOf(t.getFirstTract())); // Tract
	            SimpleFeature firstFeature = featureBuilder.buildFeature(null);
	            ((DefaultFeatureCollection)collection).add(firstFeature);
        	}
            
        	if(t.getSecondTract() != Trip.TRACT_NOT_SET) {
	            featureBuilder.add(secondEndpoint); // Location 
	            featureBuilder.add(String.valueOf(t.getIdentifier())); // Trip identifier
	            featureBuilder.add(String.valueOf(t.getSecondTract())); // Tract
	            SimpleFeature secondFeature = featureBuilder.buildFeature(null);
	            ((DefaultFeatureCollection)collection).add(secondFeature);
        	}
        	
        	// Writing trip waypoints to trip geo file (for debugging mostly, this will clutter up the file)
//        	PointList points = t.getRoute().getPoints();
//    		for(int i = 0; i < points.getSize(); i++) {
//    			Point waypoint = geometryFactory.createPoint(new Coordinate(points.getLongitude(i), points.getLatitude(i), 0.0));
//    			featureBuilder.add(waypoint); // Location 
//   	            featureBuilder.add(String.valueOf(t.getIdentifier())); // Trip identifier
//   	            featureBuilder.add("Waypoint"); // Tract
//   	            SimpleFeature secondFeature = featureBuilder.buildFeature(null);
//   	            ((DefaultFeatureCollection)collection).add(secondFeature);
//    		}
    		
        }
        return collection;
	}
	
	/**
	 * Writes the trip geographic data to a shapefile
	 */
	private void writeTripGeoToShp() {

		// Build feature type and feature collection
		SimpleFeatureType featureType = buildFeatureType();
		SimpleFeatureCollection collection = createGeoFeatureCollection(featureType);
		
		// Format time and create filename
		String dateFormatted = DRTUtils.formatMillis(TacomaDRTMain.mTripGenStartTime);
		String filename = TacomaDRTMain.getSimulationDirectory() + Constants.TRIP_PREFIX_SHP + dateFormatted + ".shp";
        File shpFile = new File(filename);
        
        ShapefileWriter shpWriter = new ShapefileWriter();
        shpWriter.writeShapefile(featureType, collection, shpFile);
	}
}