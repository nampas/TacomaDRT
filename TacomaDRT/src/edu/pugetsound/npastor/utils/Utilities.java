package edu.pugetsound.npastor.utils;


public class Utilities {

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
	
	// Converts a minute value to hh:mm format
	public static String minsToHrMin(int mins) {
		int hh = mins / 60;
		int mm = mins % 60;
		String str = hh + ":";
		if(mm < 10) str += "0" + mm;
		else str += mm;
		return str;
	}
}
