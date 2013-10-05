package edu.pugetsound.npastor;

import java.util.ArrayList;
import java.util.Random;

/**
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
		System.out.println("Tract at row 3:" + mEmploymentByTract.getTractAtRow(3));
	}
	
//	public String getTractWeightedByAge(int ageGroupCode) {
//		
//	}
//	
//	public String getTractWeightedByEmp(int employmentCode) {
//		ArrayList<String> column = mEmploymentByTract.getColumn(employmentCode);
//		int total = Integer.valueOf(column.get(column.size()-1));
//		int randomVal = mRand.nextInt(total+1);
//	}
	
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