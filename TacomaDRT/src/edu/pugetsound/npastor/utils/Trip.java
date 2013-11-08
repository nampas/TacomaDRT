package edu.pugetsound.npastor.utils;

import java.awt.geom.Point2D;

import com.graphhopper.GHResponse;


/**
 * Represents a DRT trip
 * @author Nathan P
 *
 */
public class Trip {

	public final String TAG = "Trip";
	public final static String TRACT_NOT_SET = "Not set";
	
	private int mTripType;
	private int mRiderAge;
	private boolean mIsOutbound;
	private int mIdentifier; // Unique trip identifier
	private String mEndpointTract1;
	private String mEndpointTract2;
	private Point2D mEndpoint1;
	private Point2D mEndpoint2;
	private GHResponse mRoute;
	private int mPickupTime;
	private int mCallTime; // Time request was called in
	
	public Trip(int id) {
		mTripType = -1;
		mRiderAge = -1;
		mIsOutbound = true;
		mIdentifier = hashCode();
		mEndpointTract1 = TRACT_NOT_SET;
		mEndpointTract2 = TRACT_NOT_SET;
		mEndpoint1 = new Point2D.Double();
		mEndpoint2 = new Point2D.Double();
		mRoute = null;
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
	
	public void setFirstEndpoint(Point2D endpoint) {
		mEndpoint1 = endpoint;
	}
	
	public void setSecondEndpoint(Point2D endpoint) {
		mEndpoint2 = endpoint;
	}
	
	public void setRoute(GHResponse route) {
		mRoute = route;
	}
	
	public GHResponse getRoute() {
		return mRoute;
	}
	
	public Point2D getFirstEndpoint() {
		return mEndpoint1;
	}
	
	public Point2D getSecondEndpoint() {
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
	
	/**
	 * Returns direction
	 * @return True if outbound, false otherwise
	 */
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
				"\n  Type: " + DRTUtils.getTripTypeString(mTripType) + 
				"\n  Age: " + mRiderAge +
				"\n  Outbound? " + mIsOutbound +
				"\n  First Tract: " + mEndpointTract1 + ". At " + mEndpoint1 +
				"\n  Second Tract: " + mEndpointTract2 + ". At " + mEndpoint2 +
				"\n  Pickup Time: " + DRTUtils.minsToHrMin(mPickupTime) +
				"\n  Request Made at: " + DRTUtils.minsToHrMin(mCallTime);
	}
}
