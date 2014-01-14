package edu.pugetsound.npastor.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.pugetsound.npastor.TacomaDRTMain;

/**
 * A print utility, for error and debugging messages.
 * @author Nathan Pastor
 *
 */
public class Log {

	public static final String TAG = "Log";
	
	private static final int MSG_BUFFER_LENGTH = 200; // Need to balance costs of buffer size and write-to-disk operations
	private static String[] mMessageBuffer = new String[MSG_BUFFER_LENGTH];
	private static Integer mBufferPos = 0;
	
	/**
	 * Prints info strings on a new line
	 * @param tag Message tag (usually class name)
	 * @param message Info message
	 * @param True if message should be printed to screen, false if it
	 *        should only be added to the log
	 */
	public static void iln(String tag, String message, boolean printToScreen) {
		String msg = curTimeString() + ": " + tag + ": " + message;
		if(printToScreen)
			System.out.println(msg);
		addMsgToBuffer(msg);	
	}
	
	/***
	 * Convenience method for printing info lines to screen and log
	 * @param tag Message tag (usually class name)
	 * @param message Info message
	 */
	public static void iln(String tag, String message) {
		iln(tag, message, true);
	}
	
	public static void i(String tag, String message, boolean append, boolean addToLog) {
		String msg;
		if(append)
			msg = message;
		else 
			msg = curTimeString() + ": " + tag + ": " + message;
		
		System.out.print(msg);
		if(addToLog)
			addMsgToBuffer(msg);
	}
	
	/**
	 * Prints debug messages if debug mode is enabled (Constants.DEBUG)
	 * @param tag Message tag (usually class name)
	 * @param message Debug message
	 */
	public static void d(String tag, String message) {
		if(Constants.DEBUG) {
			iln(tag,message, true);
		}
	}
	
	/**
	 * Prints error message
	 * @param tag Message tag (usually class name)
	 * @param message Error message
	 */
	public static void e(String tag, String message) {
		String msg = curTimeString() + ": ERROR: " + tag + ": " + message;
		System.err.println(msg);
		addMsgToBuffer(msg);
	}
	
	private static void addMsgToBuffer(String message) {
		// Synchronize on the buffer position to avoid concurrent access
		// resulting in a buffer overflow
		synchronized(mBufferPos) {
			mMessageBuffer[mBufferPos] = message;
			mBufferPos++;
			if(mBufferPos == MSG_BUFFER_LENGTH) {
				mBufferPos = 0;
				writeBufferToLogFile();
			}
		}
	}
	
	private static String curTimeString() {
		Date date = new Date(System.currentTimeMillis());
		return new SimpleDateFormat("HH:mm:ss.SSS").format(date);
	}
	
	public static void writeBufferToLogFile() {
		
		// Get filename and add current time and file extension
		String filename = TacomaDRTMain.getSimulationDirectory() + Constants.LOG_TXT;
		
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
