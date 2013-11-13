package edu.pugetsound.npastor.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import edu.pugetsound.npastor.TacomaDRTMain;

/**
 * A print utility, for error and debugging messages.
 * @author Nathan Pastor
 *
 */
public class Log {

	public static final String TAG = "Log";
	
	private static final int MSG_BUFFER_LENGTH = 20; // Need to balance costs of buffer size and write-to-disk operations
	private static String[] mMessageBuffer = new String[MSG_BUFFER_LENGTH];
	private static int mBufferPos = 0;
	
	/**
	 * Prints info strings
	 * @param tag Message tag (usually class name)
	 * @param message Info message
	 */
	public static void info(String tag, String message) {
		String msg = tag + " : " + message;
		System.out.println(msg);
		addMsgToBuffer(msg);	
	}
	
	/**
	 * Prints debug messages if debug mode is enabled (Constants.DEBUG)
	 * @param tag Message tag (usually class name)
	 * @param message Debug message
	 */
	public static void d(String tag, String message) {
		if(Constants.DEBUG) {
			String msg = tag + " : " + message;
			System.out.println(msg);
			addMsgToBuffer(msg);
		}
	}
	
	/**
	 * Prints error message
	 * @param tag Message tag (usually class name)
	 * @param message Error message
	 */
	public static void error(String tag, String message) {
		String msg = "ERROR: " + tag + " : " + message;
		System.err.println(msg);
		addMsgToBuffer(msg);
	}
		
	private static void addMsgToBuffer(String message) {
		mMessageBuffer[mBufferPos] = message;
		mBufferPos++;
		if(mBufferPos == MSG_BUFFER_LENGTH) {
			mBufferPos = 0;
			writeBufferToLogFile();
		}
	}
	
	public static void writeBufferToLogFile() {
		
		// Format the simulation start time
		String dateFormatted = DRTUtils.formatMillis(TacomaDRTMain.mTripGenStartTime);
		
		// Get filename and add current time and file extension
		String filename = TacomaDRTMain.getSimulationDirectory() + Constants.LOG_PREFIX_TXT + dateFormatted + ".txt";
		
		// Write to file
		try {
			FileWriter writer = new FileWriter(filename, true);
			PrintWriter lineWriter = new PrintWriter(writer);
			for(String msg : mMessageBuffer) {
				if(msg != null)
					lineWriter.println(msg);
			}
			lineWriter.close();
			writer.close();
			mMessageBuffer = new String[MSG_BUFFER_LENGTH];
		} catch (IOException ex) {
			System.err.println(TAG +  " : Unable to write to log file");
			ex.printStackTrace();
		}
	}
}
