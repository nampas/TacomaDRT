package edu.pugetsound.npastor.riderGen;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import edu.pugetsound.npastor.utils.Log;

/**
 * Represents a CSV file, and provides basic access functions
 * 
 * This class is abysmally inefficient.
 * @author Nathan P
 *
 */
public class TractCSVFile extends File {

	public static final String TAG = "TractCSVFile";
	
	private static final String COMMA_DELIMITER = ",";
	private ArrayList<String> mColumnLabels;
	private HashMap<Integer, Integer> mColumnCodes; //Maps column codes to column locations
	
	public TractCSVFile(String uri) {
		super(uri);
		mColumnLabels = new ArrayList<String>();
		mColumnCodes = null;
		parseColumnHeaders();
	}
	
	
	public void parseColumnHeaders() {
		Scanner scan = makeAScanner();
		if(scan.hasNextLine()) {
			String headers = scan.nextLine();
			String[] tokens = headers.split(COMMA_DELIMITER);
			for(int i = 0; i < tokens.length; i++) {
				mColumnLabels.add(tokens[i]);
			}
		} else {
			throw new IllegalStateException("File has no column headers");
		}
	}
	
	public ArrayList<String> getColumnLabels() {
		return mColumnLabels;
	}
	
	/**
	 * Maps column codes to actual location columns.
	 * NOTE ON USAGE: column codes in the argument ArrayList must be at the index of their desired location column. (use getColumnLabels())
	 * @param columnCodes column codes, each at the indx of their location column
	 */
	public void setColumnCodes(ArrayList<Integer> columnCodes) {
		mColumnCodes = new HashMap<Integer, Integer>();
		for(int i = 0; i < columnCodes.size(); i++) {
			mColumnCodes.put(columnCodes.get(i), i);
		}
	}
	
	public String getValueAtTract(String censusTract, int columnCode) {
		if(mColumnCodes == null) throw new IllegalStateException("Columns have not been associated with codes. Use setColumnCodes()");
		
		Scanner scan = makeAScanner();
		while(scan.hasNextLine()) {
			String curLine = scan.nextLine();
			String[] tokens = curLine.split(COMMA_DELIMITER);
			if(tokens[0].equals(censusTract)) {
				return tokens[mColumnCodes.get(columnCode)];
			}
		}
		return null;
	}
	
	private String getValueFromRowString(String row, int columnCode) {
		String[] tokens = row.split(COMMA_DELIMITER);
		String val = tokens[mColumnCodes.get(columnCode)];
		return val;
	}
	
	public ArrayList<String> getColumnByCode(int columnCode) {
		ArrayList<String> column = new ArrayList<String>();
		Scanner scan = makeAScanner();
		while(scan.hasNextLine()) {
			column.add(getValueFromRowString(scan.nextLine(), columnCode));
		}
		scan.close();
		return column;
	}
	
	/**
	 * Get the census tract number at a row
	 * @param row Row at which to get census tract number
	 * @return Census tract at row
	 */
	public String getTractAtRow(int row) {
		Scanner scan = makeAScanner();
		String tract = "";
		for(int i = 0; i < row; i++) {
			if(scan.hasNextLine()) scan.nextLine();
		}
		if(scan.hasNextLine()) {
			String curLine = scan.nextLine();
			String firstToken = curLine.substring(0, curLine.indexOf(COMMA_DELIMITER));
			tract = firstToken;
		}
		scan.close();
		
		tract = parseTract(tract);
		return tract;
	}
	
	private String parseTract(String tractString) {
		String tractNum;
		String[] tokens = tractString.split(" ");
		if(tokens.length == 1) {
			tractNum = tractString;
		} else {
			tractNum = tokens[2];
		}
		return tractNum;
	}
	
	private Scanner makeAScanner() {
		try{
			Scanner scan = new Scanner(this);
			return scan;
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
