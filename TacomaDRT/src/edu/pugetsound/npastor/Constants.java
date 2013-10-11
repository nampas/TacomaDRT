package edu.pugetsound.npastor;

/**
 * Holds constants used throughout the program
 * @author Nathan Pastor
 */
public class Constants {
	
	// Filenames and paths
	public final static String APTA_DATA_FILE = "../files/APTAData.txt"; //APTA rider age and trip type distributions
	public final static String PC_CENSUS_SHP = "../files/PCCensusTracts/pie10ct.shp"; //Pierce County census tract shapefile
	public final static String PC_AGE_FILE = "../files/PCAgeEmployment/PCAgeTotals.csv"; //Pierce County age data by census tract, from census
	public final static String PC_EMPLOYMENT_FILE = "../files/PCAgeEmployment/TacomaTractEmp2009.csv "; // Pierce County employment data, from PSRC
	
	//Total trips per day
	public final static int TOTAL_TRIPS = 10;
	
	// Agency constants
	public final static int BEGIN_OPERATION_HOUR = 6; // Service begins at this hour
	public final static int END_OPERATION_HOUR = 19; // (7:00 PM) Service ends at this hour
	
	public final static int VEHCILE_QUANTITY = 10; //TODO: determine how many, to be pinned to fixed route cost
	
	//***************************************
	//           APTA Trip Types
	//***************************************
	public final static int TRIP_COMMUTE = 0;
	public final static int TRIP_SCHOOL = 1;
	public final static int TRIP_SOCIAL = 2;
	public final static int TRIP_SHOPPING_DINING = 3;
	public final static int TRIP_MEDICAL_DENTAL = 4;
	public final static int TRIP_PERSONAL_BUSINESS = 5;
	public final static int TRIP_OTHER = 6;
	
	//***************************************
	//    APTA Age Groups (For census too)
	//***************************************
	public final static int APTA_AGE_0_14 = 10;
	public final static int APTA_AGE_15_19 = 11;
	public final static int APTA_AGE_20_24 = 12;
	public final static int APTA_AGE_25_34 = 13;
	public final static int APTA_AGE_35_44 = 14;
	public final static int APTA_AGE_45_54 = 15;
	public final static int APTA_AGE_55_64 = 16;
	public final static int APTA_AGE_65_OVER = 17;
	
	public final static String APTA_AGE_0_14_LBL = "0-14";
	public final static String APTA_AGE_15_19_LBL = "15-19";
	public final static String APTA_AGE_20_24_LBL = "20-24";
	public final static String APTA_AGE_25_34_LBL = "25-34";
	public final static String APTA_AGE_35_44_LBL = "35-44";
	public final static String APTA_AGE_45_54_LBL = "45-54";
	public final static String APTA_AGE_55_64_LBL = "55-64";
	public final static String APTA_AGE_65_OVER_LBL = "65+";
	
	//***************************************
	//         PSRC Employment Groups
	//***************************************
	public final static int PSRC_CONST_RES = 30;
	public final static int PSRC_FIRE = 31;
	public final static int PSRC_MANF = 32;
	public final static int PSRC_RETAIL = 33;
	public final static int PSRC_SERVS = 34;
	public final static int PSRC_WTU = 35;
	public final static int PSRC_GOVT = 36;
	public final static int PSRC_EDU = 37;
	public final static int PSRC_TOTAL = 38;
	
	public static final String PSRC_CONST_RES_LBL = "Const/Res";
	public static final String PSRC_FIRE_LBL = "FIRE";
	public static final String PSRC_MANF_LBL = "Manufacturing";
	public static final String PSRC_RETAIL_LBL = "Retail";
	public static final String PSRC_SERVS_LBL = "Services";
	public static final String PSRC_WTU_LBL = "WTU";
	public static final String PSRC_GOVT_LBL = "Government";
	public static final String PSRC_EDU_LBL = "Education";
	public static final String PSRC_TOTAL_LBL = "Total";
}
