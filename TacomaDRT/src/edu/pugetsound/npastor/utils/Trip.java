package edu.pugetsound.npastor.utils;

import java.awt.geom.Point2D;

import com.graphhopper.GHResponse;


/**
 * Represents a DRT trip
 * @author Nathan P
 *
 */
public class Trip {

	public final static String TAG = "Trip";
	public final static String TRACT_NOT_SET = "Not set";
	
	private int mTripType;
	private int mRiderAge;
	private boolean mIsOutbound;
	private int mIdentifier; // Unique trip identifier
	private String mOriginTract;
	private String mDestTract;
	private Point2D mOriginPoint;
	private Point2D mDestPoint;
	private GHResponse mRoute;
	private int mPickupTime;
	private int mCallTime; // Time request was called in
	
	public Trip(int id) {
		mTripType = -1;
		mRiderAge = -1;
		mIsOutbound = true;
		mIdentifier = id;
		mOriginTract = TRACT_NOT_SET;
		mDestTract = TRACT_NOT_SET;
		mOriginPoint = new Point2D.Double();
		mDestPoint = new Point2D.Double();
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
	
	public void setOriginTract(String tract) {
		mOriginTract = tract;
	}
	
	public void setDestinationTract(String tract) {
		mDestTract = tract;
	}
	
	public void setPickupTime(int minutes) {
		mPickupTime = minutes;
	}
	
	public void setCalInTime(int minutes) {
		mCallTime = minutes;
	}
	
	public void setOriginPoint(Point2D endpoint) {
		mOriginPoint = endpoint;
	}
	
	public void setDestinationPoint(Point2D endpoint) {
		mDestPoint = endpoint;
	}
	
	public void setRoute(GHResponse route) {
		mRoute = route;
	}
	
	public GHResponse getRoute() {
		return mRoute;
	}
	
	public Point2D getOriginPoint() {
		return mOriginPoint;
	}
	
	public Point2D getDestinationPoint() {
		return mDestPoint;
	}
	
	public int getPickupTime() {
		return mPickupTime;
	}
	
	public String getOriginTract() {
		return mOriginTract;
	}
	
	public String getDestinationTract() {
		return mDestTract;
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
	public boolean isOutbound() {
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
				"\n  Origin Tract: " + mOriginTract + ". At " + mOriginPoint +
				"\n  Destination Tract: " + mDestTract + ". At " + mDestPoint +
				"\n  Pickup Time: " + DRTUtils.minsToHrMin(mPickupTime) +
				"\n  Request Made at: " + DRTUtils.minsToHrMin(mCallTime) +
				"\n  Travel time: " + mRoute.getTime();
	}
}
