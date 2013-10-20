package edu.pugetsound.npastor.riderGen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import edu.pugetsound.npastor.TacomaDRT;
import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.D;
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
//		for(int i = 0; i < mTrips.size(); i++)
//			D.info(TAG, mTrips.get(i).toString());
		
		writeTripsToFile();
	}
	
	public ArrayList<Trip> getTrips() {
		return mTrips;
	}

	/**
	 * Generates ages based on APTA age distributions
	 */
	private void generateAges() {
		// Will contain all the generated ages
		D.info(TAG, "Generating trip ages");
		ArrayList<Integer> ages = new ArrayList<Integer>();

		Object[] keys = mAptaData.getAgeGroupPcts().keySet().toArray();
		// Loop through the key set, process each age group
		for(Object curKey : keys) {
			double percentage = mAptaData.getAgeGroupPct((int)curKey);
			int groupTotal = (int)Math.ceil(percentage * Constants.TOTAL_TRIPS / 100); // Total riders in this group
			switch((int)curKey) {
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
		D.info(TAG, "Total ages genererated: " + ages.size());
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
		
		D.info(TAG, "Generating trip types");
		// Will contain all the generated ages
		ArrayList<Integer> trips = new ArrayList<Integer>();

		Object[] keys = mAptaData.getTripTypePcts().keySet().toArray();
		// Loop through the key set, process each trip type
		for(int i = 0; i < keys.length; i++) {
			int curKey = (int) keys[i];
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
		D.info(TAG, "Assigning trip directions");
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
		D.info(TAG, "Generating trip endpoint tracts");
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
	
	/**
	 * Writes the generated trips to file. 
	 * TODO: Make these trips reloadable in a subsequent simulation
	 */
	private void writeTripsToFile() {
		// Format the simulation start time
		Date date = new Date(TacomaDRT.mStartTime);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
		String dateFormatted = formatter.format(date);
		
		// Get filename and add current time and file extension
		String filename = Constants.GENERATED_TRIPS_FILE + dateFormatted + ".txt";
		D.info(TAG, "Writing trips to: " + filename);
		
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
			D.error(TAG, "Unable to write to file");
			ex.printStackTrace();
		}
	}
	
	//TODO: determine time distribution across day
	//TODO: determine when requests are made known to agency
	/**
	 * Generates a trip pickup time and the time when the trip was made known to the agency.
	 * Both in minute precision
	 */
	private void generatePickupTimes() {
		
		D.info(TAG, "Generating pickup times");
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
		D.info(TAG, "Generating trip endpoints");
		TractPointGenerator pointGen = new TractPointGenerator();
		for(Trip t : mTrips) {
			t.setFirstEndpoint(pointGen.randomPointInTract(t.getFirstTract()));
			t.setSecondEndpoint(pointGen.randomPointInTract(t.getSecondTract()));
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
			D.info(TAG, "Rider age mappings, group to percent: " + mRiderAgePcts.toString());
			D.info(TAG, "Trip type mappings, group to percent: " + mTripTypePcts.toString());
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