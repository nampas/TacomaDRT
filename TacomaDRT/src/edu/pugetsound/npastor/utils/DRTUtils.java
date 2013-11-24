package edu.pugetsound.npastor.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.pugetsound.npastor.TacomaDRTMain;
import edu.pugetsound.npastor.routing.Vehicle;

public class DRTUtils {

	public final static String TAG = "DRTUtils";
	
	/**
	 * Returns age group code which the specified age falls in
	 * @param age Age
	 * @return Age group code for specified age
	 */
	public static int getGroupForAge(int age) {
		if(age < 15)
			return Constants.APTA_AGE_0_14;
		else if(age < 20)
			return Constants.APTA_AGE_15_19;
		else if(age < 25)
			return Constants.APTA_AGE_20_24;
		else if(age < 35)
			return Constants.APTA_AGE_25_34;
		else if(age < 45)
			return Constants.APTA_AGE_35_44;
		else if(age < 55)
			return Constants.APTA_AGE_45_54;
		else if(age < 65)
			return Constants.APTA_AGE_55_64;
		else
			return Constants.APTA_AGE_65_OVER;
	}
	
	public static String getTripTypeString(int tripType) {
		String tripString = "";
		
		if(tripType ==Constants.TRIP_COMMUTE)
			tripString = "commute";
		else if(tripType ==Constants.TRIP_SCHOOL)
			tripString = "school";
		else if(tripType ==Constants.TRIP_SOCIAL)
			tripString = "social";
		else if(tripType ==Constants.TRIP_SHOPPING_DINING)
			tripString = "shopping/dining";
		else if(tripType ==Constants.TRIP_MEDICAL_DENTAL)
			tripString = "medical/dental";
		else if(tripType == Constants.TRIP_PERSONAL_BUSINESS)
			tripString = "personal_business";
		else if(tripType ==Constants.TRIP_OTHER)
			tripString = "other";
		
		return tripString;
	}
	
	/**
	 * Convert minutes to HH:mm format
	 * @param mins Desired time in minutes
	 * @return Formatted String representation of specified time
	 */
	public static String minsToHrMin(int mins) {
		int hh = mins / 60;
		int mm = mins % 60;
		String str = hh + ":";
		if(mm < 10) str += "0" + mm;
		else str += mm;
		return str;
	}
	
	/**
	 * Formats a millisecond time to "yyyy-MM-dd'_'HH-mm-ss" format
	 * @param millis desired time in milliseconds
	 * @return Formatted String representation of specified time
	 */
	public static String formatMillis(long millis) {
		
		Date date = new Date(millis);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'_'HH-mm-ss");
		String dateFormatted = formatter.format(date);
		return dateFormatted;
	}
	
	public static void writeTxtFile(ArrayList<String> text, String filename) {
		
		// Get filename
		String path = TacomaDRTMain.getSimulationDirectory() + filename;
		Log.infoln(TAG, "Writing txt file to: " + filename);
		
		// Write to file
		try {
			FileWriter writer = new FileWriter(path, true);
			PrintWriter lineWriter = new PrintWriter(writer);
			
			for(String str : text) {
				// Write to file
				lineWriter.println(str);
			}
			lineWriter.close();
			writer.close();
			Log.infoln(TAG, "  File succesfully writen at:" + path);
		} catch (IOException ex) {
			Log.error(TAG, "Unable to write to file");
			ex.printStackTrace();
		}
	}
}
