package edu.pugetsound.npastor;

/**
 * Holds constants used throughout the program
 * @author Nathan Pastor
 */
public class Constants {
	
	// Filenames
	public final static String APTA_DATA_FILE = "../files/APTAData.txt";
	
	//Total trips per day
	public final static int TOTAL_TRIPS = 10;
	
	// Constants representing trip types
	public final static int TRIP_COMMUTE = 0;
	public final static int TRIP_SCHOOL = 1;
	public final static int TRIP_SOCIAL = 2;
	public final static int TRIP_SHOPPING_DINING = 3;
	public final static int TRIP_MEDICAL_DENTAL = 4;
	public final static int TRIP_PERSONAL_BUSINESS = 5;
	public final static int TRIP_OTHER = 6;
	
	// These strings are trip type labels in the APTA text file
	public static final String[] TRIP_TYPES = {
		"commute", "school", "social", "shopping/dining",
		"medical/dental", "personal_business", "other"};
	
	// Constants representing age groups
	public final static int AGE_0_14 = 10;
	public final static int AGE_15_19 = 11;
	public final static int AGE_20_24 = 12;
	public final static int AGE_25_34 = 13;
	public final static int AGE_35_44 = 14;
	public final static int AGE_45_54 = 15;
	public final static int AGE_55_64 = 16;
	public final static int AGE_65_OVER = 17;
	
	// These strings are age group labels in the APTA text file
	public static final String[] AGE_GROUPS = {
		"0-14", "15-19", "20-24", "25-34",
		"35-44", "45-54", "55-64", "65+"};
}
