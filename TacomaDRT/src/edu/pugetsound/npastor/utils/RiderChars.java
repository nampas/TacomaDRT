package edu.pugetsound.npastor.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Reads custom APTA data txt files, which associate age groups with
 * trip percentages and trip types with percentages
 * 
 * @author Nathan Pastor
 */
public class RiderChars {

	public static final String TAG = "RiderChars";
	
	private HashMap<Integer, Double> mRiderAgePcts; // Associates age groups with their percentages
	private HashMap<Integer, Double> mTripTypePcts; // Associates trip types with their percentages 
	private HashMap<Integer, DayDivision> mDayDistributions; // Associates day time perios with their percentages

	/**
	 * Creates new instance of RiderChars. This will read from the rider characteristics
	 * data file whose address is hardcoded in Constants.java
	 */
	public RiderChars() {
		mRiderAgePcts = new HashMap<Integer, Double>();
		mTripTypePcts = new HashMap<Integer, Double>();
		mDayDistributions = new HashMap<Integer, DayDivision>();
		readRiderCharsFile();
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
	
	public HashMap<Integer, DayDivision> getTripDistributiions() {
		return mDayDistributions;
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
	private void readRiderCharsFile() {
		try {
			Scanner fScan = new Scanner(new File(Constants.RIDER_CHARS_FILE)); //Uses hardcoded address
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
		if(tokens.length==0) return; //Can skip blank lines

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
		// Check trip distributions across day
		else if(tokens[0].equals(Constants.MORNING_LBL))
			mDayDistributions.put(Constants.MORNING_PERIOD, parseDayDivision(tokens));
		else if(tokens[0].equals(Constants.MORN_PEAK_PERIOD_LBL))
			mDayDistributions.put(Constants.MORN_PEAK_PERIOD, parseDayDivision(tokens));
		else if(tokens[0].equals(Constants.DAY_LBL))
			mDayDistributions.put(Constants.DAY_PERIOD, parseDayDivision(tokens));
		else if(tokens[0].equals(Constants.AFTNOON_PEAK_PERIOD_LBL))
			mDayDistributions.put(Constants.AFTNOON_PEAK_PERIOD, parseDayDivision(tokens));
		else if(tokens[0].equals(Constants.EVENING_LBL))
			mDayDistributions.put(Constants.EVENING_PERIOD, parseDayDivision(tokens));
			
	}
	
	private DayDivision parseDayDivision(String[] tokens) {
		DayDivision div;
		if(tokens.length == 4) {
			div = new DayDivision(Double.valueOf(tokens[1]),
								Integer.valueOf(tokens[2]),
								Integer.valueOf(tokens[3]));
		} else {
			div = new DayDivision(Double.valueOf(tokens[1]));
		}
		return div;
	}

	/**
	 * Represents a subdivision of the day
	 * @author Nathan P
	 */
	public class DayDivision {
		
		private int mStartTime;
		private int mLength;
		private double mPercentage;
		
		// Constructor for peak periods (when start time and length are defined
		public DayDivision(double percentage, int startTime, int length) {
			mStartTime = startTime;
			mLength = length;
			mPercentage = percentage;
		}
		
		// Construct for non-peak periods, whose start time and legnth are dependent on peak periods
		public DayDivision(double percentage) {
			mPercentage = percentage;
			mStartTime = -1;
			mLength = -1;
		}
		
		public int getStart() {
			return mStartTime;
		}
		
		public int getLength() {
			return mLength;
		}
		
		public double getPercentage() {
			return mPercentage;
		}
		
	}
	
}
