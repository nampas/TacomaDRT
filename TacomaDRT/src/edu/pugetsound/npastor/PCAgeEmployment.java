package edu.pugetsound.npastor;

import java.util.ArrayList;
import java.util.Random;

/**
 * TODO: Y NO 9400 CENSUS TRACTS IN PSRC EMPLOYMENT DATA?!?!
 * Age and employment data by census tract in Pierce County
 * @author Nathan P
 *
 */
public class PCAgeEmployment {
	
	public static final String PSRC_CONST_RES = "Const/Res";
	public static final String PSRC_FIRE = "FIRE";
	public static final String PSRC_MANF = "Manufacturing";
	public static final String PSRC_RETAIL = "Retail";
	public static final String PSRC_SERVS = "Services";
	public static final String PSRC_WTU = "WTU";
	public static final String PSRC_GOVT = "Government";
	public static final String PSRC_EDU = "Education";
	public static final String PSRC_TOTAL = "Total";

	private TractCSVFile mAgeByTract;
	private TractCSVFile mEmploymentByTract;
	private Random mRand;
	
	
	public PCAgeEmployment() {
		mRand = new Random();
		mAgeByTract = new TractCSVFile(Constants.PC_AGE_FILE);
		mEmploymentByTract = new TractCSVFile(Constants.PC_EMPLOYMENT_FILE);
		ArrayList<Integer> columnCodes = generateEmpColumnCodes(mEmploymentByTract.getColumnLabels());
		mEmploymentByTract.setColumnCodes(columnCodes);
		System.out.println("Tract at row 3: " + mEmploymentByTract.getTractAtRow(3));
	}
	
	/**
	 * Get a tract weighted according the specified column codes
	 * @param columnCodes List of column codes to weigh when choosing tract
	 * @param isEmployment Specifies if seeking employment or demographic data
	 * @return Name of tract
	 */
	public String getWeightedTract(int[] columnCodes, boolean isEmployment) {
		
		String tract = "";
		
		ArrayList<ArrayList<String>> columns = new ArrayList<ArrayList<String>>();
		int columnLength = 0;
		int employmentTotal = 0;
		
		// Loop through all columns to calculate total employment
		for(int i = 0; i < columnCodes.length; i++) {
			ArrayList<String> curCol = isEmployment ? mEmploymentByTract.getColumn(columnCodes[i]) : mAgeByTract.getColumn(columnCodes[i]);
			columnLength = curCol.size();
			for(int j = 1; j < columnLength; j++) {
				if(!curCol.get(j).contains("*") && !curCol.get(j).contains("-"))
					employmentTotal += Integer.valueOf(curCol.get(j));
			}
			columns.add(curCol); // Add current column to local list to speed up loops below
		}
		
		
		
		// Generate random number within total employment range
		int randomVal = mRand.nextInt(employmentTotal + 1);
		int runningTotal = 0;
		// When random value falls within running total, pick last tract
		System.out.println("Column length: " + columnLength + ". Num columns: " + columns.size());
		for(int i = 1; i < columnLength; i++) {
			for(int j = 0; j < columns.size(); j++) {
//				System.out.println(columns.get(j).get(i));
				if(!columns.get(j).get(i).contains("*") && !columns.get(j).get(i).contains("-"))
					runningTotal += Integer.valueOf(columns.get(j).get(i));
			}
			if(randomVal < runningTotal) {
				tract = mEmploymentByTract.getTractAtRow(i);
				System.out.println("Picking tract: " + tract);
				break;
			}
		}
		System.out.println("Column total: " + employmentTotal);
		System.out.println("End running total: " + runningTotal);
		return tract;
	}
	
	private ArrayList<Integer> generateEmpColumnCodes(ArrayList<String> columnLabels) {
		ArrayList<Integer> columnCodes = new ArrayList<Integer>(columnLabels.size());
		for(int i = 0; i < columnLabels.size(); i++) {
			String curLabel = columnLabels.get(i);
			
			if(curLabel.equals(PSRC_CONST_RES))
				columnCodes.add(i, Constants.PSRC_EMP_CONST_RES);
			else if(curLabel.equals(PSRC_FIRE))
				columnCodes.add(i, Constants.PSRC_EMP_FIRE);
			else if(curLabel.equals(PSRC_MANF))
				columnCodes.add(i, Constants.PSRC_EMP_MANF);
			else if(curLabel.equals(PSRC_RETAIL))
				columnCodes.add(i, Constants.PSRC_EMP_RETAIL);
			else if(curLabel.equals(PSRC_SERVS))
				columnCodes.add(i, Constants.PSRC_EMP_SERVS);
			else if(curLabel.equals(PSRC_WTU))
				columnCodes.add(i, Constants.PSRC_EMP_WTU);
			else if(curLabel.equals(PSRC_GOVT))
				columnCodes.add(i, Constants.PSRC_EMP_GOVT);
			else if(curLabel.equals(PSRC_EDU))
				columnCodes.add(i, Constants.PSRC_EMP_EDU);
			else if(curLabel.equals(PSRC_TOTAL))
				columnCodes.add(i, Constants.PSRC_EMP_TOTAL);
			else 
				columnCodes.add(i, -1);
		}
		
		return columnCodes;
	}
	
	//TODO: change this to age constants
	private ArrayList<Integer> generateAgeColumnCodes(ArrayList<String> columnLabels) {
		ArrayList<Integer> columnCodes = new ArrayList<Integer>();
		for(int i = 0; i < columnLabels.size(); i++) {
			String curLabel = columnLabels.get(i);
			
			if(curLabel.equals(PSRC_CONST_RES))
				columnCodes.add(i, Constants.PSRC_EMP_CONST_RES);
			else if(curLabel.equals(PSRC_FIRE))
				columnCodes.add(i, Constants.PSRC_EMP_FIRE);
			else if(curLabel.equals(PSRC_MANF))
				columnCodes.add(i, Constants.PSRC_EMP_MANF);
			else if(curLabel.equals(PSRC_RETAIL))
				columnCodes.add(i, Constants.PSRC_EMP_RETAIL);
			else if(curLabel.equals(PSRC_SERVS))
				columnCodes.add(i, Constants.PSRC_EMP_SERVS);
			else if(curLabel.equals(PSRC_WTU))
				columnCodes.add(i, Constants.PSRC_EMP_WTU);
			else if(curLabel.equals(PSRC_GOVT))
				columnCodes.add(i, Constants.PSRC_EMP_GOVT);
			else if(curLabel.equals(PSRC_EDU))
				columnCodes.add(i, Constants.PSRC_EMP_EDU);
			else if(curLabel.equals(PSRC_TOTAL))
				columnCodes.add(i, Constants.PSRC_EMP_TOTAL);
		}
		
		return columnCodes;
	}
}