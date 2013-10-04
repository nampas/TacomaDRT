package edu.pugetsound.npastor;

/**
 * Represents a DRT trip
 * @author Nathan P
 *
 */
public class Trip {

	private int mTripType;
	private int mRiderAge;
	private boolean mIsOutbound;
	private int mIdentifier;
	
	public Trip() {
		mTripType = -1;
		mRiderAge = -1;
		mIsOutbound = true;
		mIdentifier = hashCode();
	}
	
	public void setTripType(int tripType) {
		mTripType = tripType;
	}
	
	public void setRiderAge(int riderAge) {
		mRiderAge = riderAge;
	}
	
	public void setDirection(boolean isOutbound) {
		mIsOutbound = isOutbound;
	}
	
	public int getRiderAge() {
		return mRiderAge;
	}
	
	public int getTripType() {
		return mTripType;
	}
	
	public boolean getDirection() {
		return mIsOutbound;
	}
	
	public int getIdentifier() {
		return mIdentifier;
	}
	
	public String toString() {
		return "Trip: " + mIdentifier + 
				"\n  Type: " + mTripType + 
				"\n  Age: " + mRiderAge +
				"\n  Outbound? " + mIsOutbound;
	}
}
