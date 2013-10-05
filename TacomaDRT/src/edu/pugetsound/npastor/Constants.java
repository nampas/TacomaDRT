package edu.pugetsound.npastor;

/**
 * Holds constants used throughout the program
 * @author Nathan Pastor
 */
public class Constants {
	
	// Filenames and paths
	public final static String APTA_DATA_FILE = "../files/APTAData.txt"; //APTA rider age and trip type distributions
	public final static String PC_CENSUS_SHP = "../files/PCCensusTracts/pie10ct.shp"; //Pierce County census tract shapefile
	public final static String PC_AGE_FILE = "../files/PCAgeEmployment/DEC_10_SF1_P12_with_ann.txt"; //Piece county age data by census tract, from census
	public final static String PC_EMPLOYMENT_FILE = "../files/PCAgeEmployment/TacomaTractEmp2009.txt "; // Pierce County employment data, from PSRC
	
	//Total trips per day
	public final static int TOTAL_TRIPS = 10;
	
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
	//            APTA Age Groups
	//***************************************
	public final static int APTA_AGE_0_14 = 10;
	public final static int APTA_AGE_15_19 = 11;
	public final static int APTA_AGE_20_24 = 12;
	public final static int APTA_AGE_25_34 = 13;
	public final static int APTA_AGE_35_44 = 14;
	public final static int APTA_AGE_45_54 = 15;
	public final static int APTA_AGE_55_64 = 16;
	public final static int APTA_AGE_65_OVER = 17;
	
	//***************************************
	//           Census Age Groups
	//***************************************
	
	// Constants representing Census age groups
	
	//***************************************
	//         PSRC Employment Groups
	//***************************************
	public final static int PSRC_EMP_CONST_RES = 30;
	public final static int PSRC_EMP_FIRE = 31;
	public final static int PSRC_EMP_MANF = 32;
	public final static int PSRC_EMP_RETAIL = 33;
	public final static int PSRC_EMP_SERVS = 34;
	public final static int PSRC_EMP_WTU = 35;
	public final static int PSRC_EMP_GOVT = 36;
	public final static int PSRC_EMP_EDU = 37;
	public final static int PSRC_EMP_TOTAL = 38;
}
