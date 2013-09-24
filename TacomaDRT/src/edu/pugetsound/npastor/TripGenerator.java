package edu.pugetsound.npastor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Generates all daily trips on the DRT network
 * @author Nathan Pastor
 *
 */
public class TripGenerator {
	
	//Total trips per day
	private final static int TOTAL_TRIPS = 5;
	
	private ArrayList<Trip> mTrips;
	private AptaData mAptaData;

	public TripGenerator() {
		mTrips = new ArrayList<Trip>();
		mAptaData = new AptaData();
	}
	
	/**
	 * Begins the trip generation process
	 */
	public void generateTrips() {
		//TODO: everything
	}
	
	/**
	 * Reads custom APTA data txt files, which associate age groups with
	 * trip percentages and trip types with percentages
	 * 
	 * @author Nathan Pastor
	 */
	private class AptaData {
		
		private HashMap<String, Double> mRiderAgePcts; // Associates age groups with their percentages
		private HashMap<String, Double> mTripTypePcts; // Associates trip types with their percentages 
		
		/**
		 * Creates new instance of AptaData. This will read from the APTA
		 * data file whose address is hardcoded in Constants.java
		 */
		public AptaData() {
			mRiderAgePcts = new HashMap<String, Double>();
			mTripTypePcts = new HashMap<String, Double>();
			readAptaFile();
			System.out.println(mRiderAgePcts);
			System.out.println(mTripTypePcts);
		}
		
		/**
		 * Returns the percentage of total transit riders that the specified 
		 * age group constitutes
		 * @param ageGroupCode Integer representing age group, defined in Constants.java
		 * @return Percentage of total transit riders that the specified age group constitutes
		 */
		public Double getAgeGroupPct(String ageGroup) {
			return mRiderAgePcts.get(ageGroup);
		}
		
		/**
		 * Returns the percentage of total trips riders that the specified 
		 * trip type constitutes
		 * @param ageGroupCode Integer representing trip type, defined in Constants.java
		 * @return Percentage of total transit trips that the specified trip type constitutes
		 */
		public Double getTripTypePct(String tripType) {
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
			for(String ageGroup : Constants.AGE_GROUPS) {
				if(tokens[0].equals(ageGroup)) {
					mRiderAgePcts.put(ageGroup, Double.valueOf(tokens[1]));
					return;
				}
			}
			for(String tripType : Constants.TRIP_TYPES) {
				if(tokens[0].equals(tripType)) {
					mTripTypePcts.put(tripType, Double.valueOf(tokens[1]));
					return;
				}
			}
		}
	}
}
