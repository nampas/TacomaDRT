package edu.pugetsound.npastor;

/**
 * Holds constants used throughout the program
 * @author Nathan Pastor
 */
public class Constants {
	
	// Filenames
	public final static String APTA_DATA_FILE = "./files/APTAData.txt";
	
	// Constants representing trip types
	// These strings are also the labels in the APTA text file
	public static final String[] TRIP_TYPES = {
		"commute", "school", "social", "shopping/dining",
		"medical/dental", "personal_business", "other"};
	
	// Constants representing age groups
	// These strings are also the labels in the APTA text file
	public static final String[] AGE_GROUPS = {
		"0-14", "15-19", "20-24", "25-34",
		"35-44", "45-54", "55-64", "65+"};
}
