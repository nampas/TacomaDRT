package edu.pugetsound.npastor.utils;

/**
 * A simple print utility, for error and debugging messages.
 * @author Nathan P
 *
 */
public class D {

	public static void info(String tag, String message) {
		if(Constants.DEBUG) 
			System.out.println(tag + " : " + message);
	}
	
	public static void error(String tag, String message) {
		System.out.println("--ERROR: " + tag + " : " + message);
	}
}
