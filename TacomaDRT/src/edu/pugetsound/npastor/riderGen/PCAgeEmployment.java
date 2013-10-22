package edu.pugetsound.npastor.riderGen;

import java.util.ArrayList;
import java.util.Random;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;

/**
 * TODO: Y NO 9400 CENSUS TRACTS IN PSRC EMPLOYMENT DATA?!?!
 * Age and employment data by census tract in Pierce County
 * @author Nathan P
 *
 */
public class PCAgeEmployment {
	
	public static final String TAG = "PCAgeEmployment";
	
	private TractCSVFile mAgeByTract;
	private TractCSVFile mEmploymentByTract;
	private Random mRand;
	
	
	public PCAgeEmployment() {
		mRand = new Random();
		mAgeByTract = new TractCSVFile(Constants.PC_AGE_FILE);
		mEmploymentByTract = new TractCSVFile(Constants.PC_EMPLOYMENT_FILE);
		
		// Associate column codes (defined in constants) with column indices
		ArrayList<Integer> empColumnCodes = generateEmpColumnCodes(mEmploymentByTract.getColumnLabels());
		ArrayList<Integer> ageColumnCodes = generateAgeColumnCodes(mAgeByTract.getColumnLabels());
		mEmploymentByTract.setColumnCodes(empColumnCodes);
		mAgeByTract.setColumnCodes(ageColumnCodes);
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
		int total = 0;
		
		// Calculate combined column total
		for(int i = 0; i < columnCodes.length; i++) {
			ArrayList<String> curCol = isEmployment ? mEmploymentByTract.getColumnByCode(columnCodes[i]) : mAgeByTract.getColumnByCode(columnCodes[i]);
			total += Integer.valueOf(curCol.get(curCol.size()-1)); // Total is held at last last row
			columns.add(curCol); // Add current column to local list to speed up loops below
			if(i==0) columnLength = curCol.size();
		}
		
		// Generate random number within total range
		int randomVal = mRand.nextInt(total + 1);
		int runningTotal = 0;
		// When random value falls within running total, pick last tract
		for(int i = 1; i < columnLength; i++) {
			for(int j = 0; j < columns.size(); j++) {
				if(!columns.get(j).get(i).contains("*") && !columns.get(j).get(i).contains("-"))
					runningTotal += Integer.valueOf(columns.get(j).get(i));
			}
			if(randomVal < runningTotal) {
				tract = isEmployment ? mEmploymentByTract.getTractAtRow(i) : mAgeByTract.getTractAtRow(i);
				break;
			}
		}
		return tract;
	}
	
	/**
	 * Returns a random census tract
	 * @return Random census tract
	 */
	public String getRandomTract() {
		String tract = "";
		ArrayList<String> labelColumn = mEmploymentByTract.getColumnByCode(TractCSVFile.TRACT_LABEL_COLUMN);
		
		int tractRow = mRand.nextInt(labelColumn.size()) + 1;
		tract = mEmploymentByTract.getTractAtRow(tractRow);
		return tract;
	}
	
	private ArrayList<Integer> generateEmpColumnCodes(ArrayList<String> columnLabels) {
		ArrayList<Integer> columnCodes = new ArrayList<Integer>(columnLabels.size());
		for(int i = 0; i < columnLabels.size(); i++) {
			String curLabel = columnLabels.get(i);
			
			if(curLabel.equals(Constants.PSRC_CONST_RES_LBL))
				columnCodes.add(i, Constants.PSRC_CONST_RES);
			else if(curLabel.equals(Constants.PSRC_FIRE_LBL))
				columnCodes.add(i, Constants.PSRC_FIRE);
			else if(curLabel.equals(Constants.PSRC_MANF_LBL))
				columnCodes.add(i, Constants.PSRC_MANF);
			else if(curLabel.equals(Constants.PSRC_RETAIL_LBL))
				columnCodes.add(i, Constants.PSRC_RETAIL);
			else if(curLabel.equals(Constants.PSRC_SERVS_LBL))
				columnCodes.add(i, Constants.PSRC_SERVS);
			else if(curLabel.equals(Constants.PSRC_WTU_LBL))
				columnCodes.add(i, Constants.PSRC_WTU);
			else if(curLabel.equals(Constants.PSRC_GOVT_LBL))
				columnCodes.add(i, Constants.PSRC_GOVT);
			else if(curLabel.equals(Constants.PSRC_EDU_LBL))
				columnCodes.add(i, Constants.PSRC_EDU);
			else if(curLabel.equals(Constants.PSRC_TOTAL_LBL))
				columnCodes.add(i, Constants.PSRC_TOTAL);
			else 
				columnCodes.add(i, -1);
		}
		
		return columnCodes;
	}
	
	private ArrayList<Integer> generateAgeColumnCodes(ArrayList<String> columnLabels) {
		ArrayList<Integer> columnCodes = new ArrayList<Integer>();
		for(int i = 0; i < columnLabels.size(); i++) {
			String curLabel = columnLabels.get(i);
			
			if(curLabel.equals(Constants.APTA_AGE_0_14_LBL))
				columnCodes.add(i, Constants.APTA_AGE_0_14);
			else if(curLabel.equals(Constants.APTA_AGE_15_19_LBL))
				columnCodes.add(i, Constants.APTA_AGE_15_19);
			else if(curLabel.equals(Constants.APTA_AGE_20_24_LBL))
				columnCodes.add(i, Constants.APTA_AGE_20_24);
			else if(curLabel.equals(Constants.APTA_AGE_25_34_LBL))
				columnCodes.add(i, Constants.APTA_AGE_25_34);
			else if(curLabel.equals(Constants.APTA_AGE_35_44_LBL))
				columnCodes.add(i, Constants.APTA_AGE_35_44);
			else if(curLabel.equals(Constants.APTA_AGE_45_54_LBL))
				columnCodes.add(i, Constants.APTA_AGE_45_54);
			else if(curLabel.equals(Constants.APTA_AGE_55_64_LBL))
				columnCodes.add(i, Constants.APTA_AGE_55_64);
			else if(curLabel.equals(Constants.APTA_AGE_65_OVER_LBL))
				columnCodes.add(i, Constants.APTA_AGE_65_OVER);
			else 
				columnCodes.add(i, -1);
		}
		
		return columnCodes;
	}
}