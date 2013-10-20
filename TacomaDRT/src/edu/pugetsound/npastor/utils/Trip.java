package edu.pugetsound.npastor.utils;

import delaunay_triangulation.Point_dt;


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
	private Point_dt mEndpoint1;
	private Point_dt mEndpoint2;
	private int mPickupTime;
	private int mCallTime; // Time request was called in
	
	public Trip() {
		mTripType = -1;
		mRiderAge = -1;
		mIsOutbound = true;
		mIdentifier = hashCode();
		mEndpointTract1 = "Not set";
		mEndpointTract2 = "Not set";
		mPickupTime = -1;
		mCallTime = -1;
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
	
	public void setCalInTime(int minutes) {
		mCallTime = minutes;
	}
	
	public void setFirstEndpoint(Point_dt endpoint) {
		mEndpoint1 = endpoint;
	}
	
	public void setSecondEndpoint(Point_dt endpoint) {
		mEndpoint2 = endpoint;
	}
	
	public Point_dt getFirstEndpoint() {
		return mEndpoint1;
	}
	
	public Point_dt getSecondEndpoint() {
		return mEndpoint2;
	}
	
	public int getPickupTime() {
		return mPickupTime;
	}
	
	public String getFirstTract() {
		return mEndpointTract1;
	}
	
	public String getSecondTract() {
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
	
	public int getCallInTime() {
		return mCallTime;
	}
	
	public String toString() {
		return "Trip: " + mIdentifier + 
				"\n  Type: " + Utilities.getTripTypeString(mTripType) + 
				"\n  Age: " + mRiderAge +
				"\n  Outbound? " + mIsOutbound +
				"\n  First Tract: " + mEndpointTract1 + ". At" + mEndpoint1 +
				"\n  Second Tract: " + mEndpointTract2 + ". At" + mEndpoint2 +
				"\n  Pickup Time: " + Utilities.minsToHrMin(mPickupTime) +
				"\n  Request Made at: " + Utilities.minsToHrMin(mCallTime);
	}
}
