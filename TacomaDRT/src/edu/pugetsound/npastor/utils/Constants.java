package edu.pugetsound.npastor.utils;

/**
 * Holds constants used throughout the program
 * @author Nathan Pastor
 */
public class Constants {
	
	public final static boolean DEBUG = false;
	
	// Filenames and paths
	public final static String FILE_BASE_DIR = "files";
	public final static String RIDER_CHARS_FILE = "/RiderCharacteristics.txt"; //Contains APTA rider age and trip type distributions, trip distributions across day and more
	public final static String PC_CENSUS_SHP = "/PCCensusTracts/pie10ct.shp"; //Pierce County census tract shapefile
	public final static String PC_AGE_FILE = "/PCAgeEmployment/PCAgeTotals.csv"; //Pierce County age data by census tract, from census
	public final static String PC_EMPLOYMENT_FILE = "/PCAgeEmployment/TacomaTractEmp2009.csv "; // Pierce County employment data, from PSRC
	public final static String TACOMA_BOUNDARY_SHP = "/TacomaBoundary/TacomaBoundary.shp"; // Tacoma city limits .shp, MOSTLY clipped to shoreline
	
	// Generated simulation files
	public final static String SIM_BASE_DIRECTORY = "files/Simulations";
	public final static String TRIPS_VEHICLES_TXT = "/trips_vehicles.txt";
	public final static String TRIPS_READABLE_TXT = "/trips_readable.txt";
	public final static String ROUTE_CACHE_CSV = "/route_cache.csv";
	public final static String LOG_TXT = "/log.txt";
	public final static String SCHED_TXT = "/schedules.txt";
	public final static String STATS_CSV = "/statistics.csv";
	public final static String REBUS_SETTINGS_CSV = "/rebus_settings.csv";
	public final static String TRIP_SHP_DIR = "/trips_shp";
	public final static String TRIP_PREFIX_SHP = "/tripgeo_";
	public final static String ROUTE_SHP_DIR = "/routes_shp";
	public final static String VEH_ROUTE_SHP_DIR = "/vehicle_"; // Vehicle id must be appended
	public final static String ROUTE_PREFIX_SHP = "/vehicle_";
	
	// Total trips per day
	public final static int TOTAL_TRIPS = 100;
	
	// Agency constants
	public final static int BEGIN_OPERATION_HOUR = 6; // Service begins at this hour
	public final static int END_OPERATION_HOUR = 19; // (7:00 PM) Service ends at this hour
	public final static int BEGIN_REQUEST_WINDOW = 0; // Time when riders can begin to make requests
	public final static int END_REQUEST_WINDOW = 18; // Cutoff time for making request known to agency for the day
	public final static int CALL_REQUEST_BUFFER_MINS = 40; // Minimum buffer between call in time and request time
	public final static int VEHICLE_QUANTITY = 4; // DRT fleet size
	public final static int PICKUP_SERVICE_WINDOW = 30; //Max difference between actual pickup time and requested pickup time
	
	// Dynamic requests
	public final static String DYNAMIC_REQUESTS_PCT = "dynamic_requests";
	
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
	
	//*****************************************
	//      Trip Distributions Across Day
	//*****************************************
	public final static int MORNING_PERIOD = 50;
	public final static int MORN_PEAK_PERIOD = 51;
	public final static int DAY_PERIOD = 52;
	public final static int AFTNOON_PEAK_PERIOD = 53;
	public final static int EVENING_PERIOD = 54;
	
	public static final String MORNING_LBL = "morning";
	public static final String MORN_PEAK_PERIOD_LBL = "morning_peak";
	public static final String DAY_LBL = "day";
	public final static String AFTNOON_PEAK_PERIOD_LBL = "afternoon_peak";
	public static final String EVENING_LBL = "evening";
	
	
}
