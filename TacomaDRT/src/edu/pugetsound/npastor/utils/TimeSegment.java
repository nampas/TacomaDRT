package edu.pugetsound.npastor.utils;

public class TimeSegment {

	private int mStartMins;
	private int mEndMins;
	
	public TimeSegment(int startMins, int endMins) {
		mStartMins = startMins;
		mEndMins = endMins;
	}
	
	public int getStartMins() {
		return mStartMins;
	}
	
	public int getEndMins() {
		return mEndMins;
	}
}
