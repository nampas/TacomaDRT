package edu.pugetsound.npastor.riderGen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
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
import com.vividsolutions.jts.geom.Point;

import edu.pugetsound.npastor.TacomaDRT;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;
import edu.pugetsound.npastor.utils.Utilities;

/**
 * Generates all daily trips on the DRT network
 * @author Nathan Pastor
 *
 */
public class TripGenerator {

	public final static String TAG = "TripGenerator";
	
	private ArrayList<Trip> mTrips;
	private AptaData mAptaData;
	private PCAgeEmployment mPCData;
	private Random mRandom;

	public TripGenerator() {
		mTrips = new ArrayList<Trip>();
		mAptaData = new AptaData();
		mPCData = new PCAgeEmployment();
		mRandom = new Random();
	}

	/**
	 * Begins the trip generation process
	 */
	public void generateTrips() {
		for(int i = 0; i < Constants.TOTAL_TRIPS; i++) {
			mTrips.add(new Trip());
		}
		// Generate all trip attributes. ORDER IS IMPORTANT!
		generateTripTypes();
		generateAges();
		assignDirections();
		generateEndpointTracts();
		generateEndpoints();
		generatePickupTimes();
		for(int i = 0; i < mTrips.size(); i++)
			Log.info(TAG, mTrips.get(i).toString());
		
		writeTripsToFile();
		writeTripGeoToShp();
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

		Object[] keys = mAptaData.getAgeGroupPcts().keySet().toArray();
		// Loop through the key set, process each age group
		for(Object curKey : keys) {
			double percentage = mAptaData.getAgeGroupPct((Integer)curKey);
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

		Object[] keys = mAptaData.getTripTypePcts().keySet().toArray();
		// Loop through the key set, process each trip type
		for(int i = 0; i < keys.length; i++) {
			int curKey = (Integer) keys[i];
			double percentage = mAptaData.getTripTypePct(curKey);
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
			int[] riderAgeGroup = {Utilities.getGroupForAge(t.getRiderAge())};
			switch(t.getTripType()) {
				case Constants.TRIP_COMMUTE:
					int[] commuteCode = {Constants.PSRC_TOTAL};
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
					t.setSecondTract(mPCData.getWeightedTract(commuteCode, true));
					break;
				case Constants.TRIP_MEDICAL_DENTAL:
					int[] medCodes = {Constants.PSRC_SERVS, Constants.PSRC_GOVT};
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
					t.setSecondTract(mPCData.getWeightedTract(medCodes, true));
					break;
				case Constants.TRIP_OTHER:
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
					break;
				case Constants.TRIP_PERSONAL_BUSINESS:
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
					break;
				case Constants.TRIP_SCHOOL:
					int [] schoolCode = {Constants.PSRC_EDU};
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
					t.setSecondTract(mPCData.getWeightedTract(schoolCode, true));
					break;
				case Constants.TRIP_SHOPPING_DINING:
					int[] shopCodes = {Constants.PSRC_SERVS, Constants.PSRC_RETAIL};
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
					t.setSecondTract(mPCData.getWeightedTract(shopCodes, true));
					break;
				case Constants.TRIP_SOCIAL:
					t.setFirstTract(mPCData.getWeightedTract(riderAgeGroup, false));
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
		int minRequestTime = Constants.BEGIN_OPERATION_HOUR * 60;
		int maxRequestTime = Constants.END_OPERATION_HOUR * 60;
		int minRequestWindow = Constants.BEGIN_REQUEST_WINDOW * 60;
		int maxRequestWindow = Constants.END_REQUEST_WINDOW * 60;
		
		for(Trip t : mTrips) {
//			int calledAt = mRandom.nextInt(maxRequestWindow - minRequestWindow + 1) + minRequestWindow;
			int calledAt = 0;
			
			// Request time must be before operation ends and after buffer IF request was made during service hours
			int minTime = calledAt > minRequestTime ? (int)Constants.CALL_REQUEST_BUFFER + calledAt : minRequestTime;
			int request = mRandom.nextInt(maxRequestTime - minTime  + 1) + minTime;

			// int request = request & 5; // Round down to nearest multiple of 5
			t.setPickupTime(request);
			t.setCalInTime(calledAt);
		}
	}
	
	private void generateEndpoints() {
		Log.info(TAG, "Generating trip endpoints");
		TractPointGenerator pointGen = new TractPointGenerator();
		for(Trip t : mTrips) {
			t.setFirstEndpoint(pointGen.randomPointInTract(t.getFirstTract()));
			t.setSecondEndpoint(pointGen.randomPointInTract(t.getSecondTract()));
		}
	}
	
	/**
	 * Writes the generated trips to file. 
	 * TODO: Make these trips reloadable in a subsequent simulation
	 */
	private void writeTripsToFile() {
		// Format the simulation start time
		String dateFormatted = Utilities.formatMillis(TacomaDRT.mStartTime);
		
		// Get filename and add current time and file extension
		String filename = Constants.GENERATED_TRIPS_FILE + dateFormatted + ".txt";
		Log.info(TAG, "Writing trips to: " + filename);
		
		// Write to file
		try {
			FileWriter writer = new FileWriter(filename, false);
			PrintWriter lineWriter = new PrintWriter(writer);
			for(Trip t : mTrips) {
				String curTrip = t.toString();
				curTrip.replace("\n", ""); // Get rid of all line break;
				lineWriter.println(curTrip);
			}
			lineWriter.close();
			writer.close();
		} catch (IOException ex) {
			Log.error(TAG, "Unable to write to file");
			ex.printStackTrace();
		}
	}
	
	private SimpleFeatureType buildFeatureType() {
		// Build feature type
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("TripEndpoints");
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add("Location", Point.class); // Geo data
        builder.length(15).add("Trip", String.class); // Trip identifier
        builder.add("Tract", Double.class); // Tract number the point falls in
        
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
        	Point firstEndpoint = geometryFactory.createPoint(new Coordinate(t.getFirstEndpoint().x(), t.getFirstEndpoint().y(), 0.0));
        	Point secondEndpoint = geometryFactory.createPoint(new Coordinate(t.getSecondEndpoint().x(), t.getSecondEndpoint().y(), 0.0));
        	
        	Log.info(TAG, firstEndpoint.getCoordinate() + "    " + secondEndpoint.getCoordinate());
        	
        	// Add both feature to collection
        	if(t.getFirstTract() != "Not set") {
	            featureBuilder.add(firstEndpoint); // Geo data
	            featureBuilder.add(String.valueOf(t.getIdentifier())); // Trip identifier
	            featureBuilder.add(t.getFirstTract()); // Tract
	            SimpleFeature firstFeature = featureBuilder.buildFeature(null);
	            ((DefaultFeatureCollection)collection).add(firstFeature);
        	}
            
        	if(t.getSecondTract() != "Not set") {
	            featureBuilder.add(secondEndpoint); // Location
	            featureBuilder.add(String.valueOf(t.getIdentifier())); // Name
	            featureBuilder.add(t.getSecondTract()); // Tract
	            SimpleFeature secondFeature = featureBuilder.buildFeature(null);
	            ((DefaultFeatureCollection)collection).add(secondFeature);
        	}
        }
        return collection;
	}
	
	/**
	 * Writes the trip geographic data to a shapefile
	 * Adapted from: http://docs.geotools.org/latest/tutorials/feature/csv2shp.html#write-the-feature-data-to-the-shapefile
	 */
	private void writeTripGeoToShp() {

		SimpleFeatureType featureType = buildFeatureType();
		SimpleFeatureCollection collection = createGeoFeatureCollection(featureType);
		
		// Format time and create filename
		String dateFormatted = Utilities.formatMillis(TacomaDRT.mStartTime);
		String filename = Constants.TRIP_SHP + dateFormatted + ".shp";
        File shpFile = new File(filename);

        Log.info(TAG, "Writing trips to shapefile at: " + filename);
        
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        try {
	        Map<String, Serializable> params = new HashMap<String, Serializable>();
	        params.put("url", shpFile.toURI().toURL());
	        params.put("create spatial index", Boolean.TRUE);
	        
	        // Build the data store, which will hold our collection
	        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
	        dataStore.createSchema(featureType);
	        
	        // Finally, write the features to the shapefile
	        Transaction transaction = new DefaultTransaction("create");
	        String typeName = dataStore.getTypeNames()[0];
	        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
	        
	        if (featureSource instanceof FeatureStore) {
	            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	            featureStore.setTransaction(transaction);
                featureStore.addFeatures(collection);
                transaction.commit();
                Log.info(TAG,  "Data committed to file");
	        }
	        transaction.close();
        } catch (MalformedURLException ex) {
        	Log.error(TAG, "Unable to save trips to shapefile");
        	ex.printStackTrace();
        } catch (IOException ex) {
        	Log.error(TAG, "Unable to open or write to shapefile");
        	ex.printStackTrace();
        }
	}

	/**
	 * Reads custom APTA data txt files, which associate age groups with
	 * trip percentages and trip types with percentages
	 * 
	 * @author Nathan Pastor
	 */
	private class AptaData {

		public static final String TAG = "TripGenerator.AptaData";
		
		private HashMap<Integer, Double> mRiderAgePcts; // Associates age groups with their percentages
		private HashMap<Integer, Double> mTripTypePcts; // Associates trip types with their percentages 

		/**
		 * Creates new instance of AptaData. This will read from the APTA
		 * data file whose address is hardcoded in Constants.java
		 */
		public AptaData() {
			mRiderAgePcts = new HashMap<Integer, Double>();
			mTripTypePcts = new HashMap<Integer, Double>();
			readAptaFile();
			Log.info(TAG, "Rider age mappings, group to percent: " + mRiderAgePcts.toString());
			Log.info(TAG, "Trip type mappings, group to percent: " + mTripTypePcts.toString());
		}

		/**
		 * Returns map associating rider age groups with their percentages
		 * @return Map associating rider age groups with percentages
		 */
		public HashMap<Integer, Double> getAgeGroupPcts() {
			return mRiderAgePcts;
		}

		/**
		 * Returns map associating trip types with their percentages
		 * @return Map associating trip types groups with percentages
		 */
		public HashMap<Integer, Double> getTripTypePcts() {
			return mTripTypePcts;
		}

		/**
		 * Returns the percentage of total transit riders that the specified 
		 * age group constitutes
		 * @param ageGroupCode Integer representing age group, defined in Constants.java
		 * @return Percentage of total transit riders that the specified age group constitutes
		 */
		public Double getAgeGroupPct(int ageGroup) {
			return mRiderAgePcts.get(ageGroup);
		}

		/**
		 * Returns the percentage of total trips riders that the specified 
		 * trip type constitutes
		 * @param ageGroupCode Integer representing trip type, defined in Constants.java
		 * @return Percentage of total transit trips that the specified trip type constitutes
		 */
		public Double getTripTypePct(int tripType) {
			return mTripTypePcts.get(tripType);
		}

		//Parses APTA data file
		private void readAptaFile() {
			try {
				Scanner fScan = new Scanner(new File(Constants.APTA_DATA_FILE)); //Uses hardcoded address
				while(fScan.hasNextLine()) {
					String curLine = fScan.nextLine();
					parseFileLine(curLine);
				}
				fScan.close();
			} catch (FileNotFoundException ex) {
				//TODO: error handling
				ex.printStackTrace();
			}

		}

		//Parses a line of the file
		private void parseFileLine(String line) {
			String[] tokens = line.split(" ");

			// Check trip types
			if(tokens[0].equals("commute"))
				mTripTypePcts.put(Constants.TRIP_COMMUTE, Double.valueOf(tokens[1]));
			else if(tokens[0].equals("school"))
				mTripTypePcts.put(Constants.TRIP_SCHOOL, Double.valueOf(tokens[1]));
			else if(tokens[0].equals("social"))
				mTripTypePcts.put(Constants.TRIP_SOCIAL, Double.valueOf(tokens[1]));
			else if(tokens[0].equals("shopping/dining"))
				mTripTypePcts.put(Constants.TRIP_SHOPPING_DINING, Double.valueOf(tokens[1]));
			else if(tokens[0].equals("medical/dental"))
				mTripTypePcts.put(Constants.TRIP_MEDICAL_DENTAL, Double.valueOf(tokens[1]));
			else if(tokens[0].equals("personal_business"))
				mTripTypePcts.put(Constants.TRIP_PERSONAL_BUSINESS, Double.valueOf(tokens[1]));
			else if(tokens[0].equals("other"))
				mTripTypePcts.put(Constants.TRIP_OTHER, Double.valueOf(tokens[1]));
			// Check age groups
			else if(tokens[0].equals(Constants.APTA_AGE_0_14_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_0_14, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_15_19_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_15_19, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_20_24_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_20_24, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_25_34_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_25_34, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_35_44_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_35_44, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_45_54_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_45_54, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_55_64_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_55_64, Double.valueOf(tokens[1]));
			else if(tokens[0].equals(Constants.APTA_AGE_65_OVER_LBL))
				mRiderAgePcts.put(Constants.APTA_AGE_65_OVER, Double.valueOf(tokens[1]));
		}
	}
}
