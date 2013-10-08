package edu.pugetsound.npastor;

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
}
