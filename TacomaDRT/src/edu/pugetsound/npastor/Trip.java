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
	private String mEndpointTract1;
	private String mEndpointTract2;
	private int mPickupTime;
	
	public Trip() {
		mTripType = -1;
		mRiderAge = -1;
		mIsOutbound = true;
		mIdentifier = hashCode();
		mEndpointTract1 = "Not set";
		mEndpointTract2 = "Not set";
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
	
	public void setFirstTract(String tract) {
		mEndpointTract1 = tract;
	}
	
	public void setSecondTract(String tract) {
		mEndpointTract2 = tract;
	}
	
	public void setPickupTime(int minutes) {
		mPickupTime = minutes;
	}
	
	public int getPickupTime() {
		return mPickupTime;
	}
	
	public String getFirstTract(String tract) {
		return mEndpointTract1;
	}
	
	public String getSecondTract(String tract) {
		return mEndpointTract2;
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
				"\n  Outbound? " + mIsOutbound +
				"\n  First Tract: " + mEndpointTract1 +
				"\n  Second Tract: " + mEndpointTract2 +
				"\n  Pickup Time: " + Utilities.minsToHrMin(mPickupTime);
	}
}
