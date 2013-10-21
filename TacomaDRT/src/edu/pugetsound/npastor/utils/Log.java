package edu.pugetsound.npastor.utils;

/**
 * A print utility, for error and debugging messages.
 * @author Nathan Pastor
 *
 */
public class Log {

	/**
	 * Prints debug strings if debug mode is enabled (Constants.DEBUG)
	 * @param tag Message tag (usually class name)
	 * @param message Debug message
	 */
	public static void info(String tag, String message) {
		if(Constants.DEBUG) 
			System.out.println(tag + " : " + message);
	}
	
	/**
	 * Prints error message
	 * @param tag Message tag (usually class name)
	 * @param message Error message
	 */
	public static void error(String tag, String message) {
		System.err.println("ERROR: " + tag + " : " + message);
	}
}
