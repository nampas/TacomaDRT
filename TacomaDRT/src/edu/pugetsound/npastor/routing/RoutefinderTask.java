package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import edu.pugetsound.npastor.utils.Log;
import edu.pugetsound.npastor.utils.Trip;

public class RoutefinderTask implements Runnable {
	
	public static final String TAG = "RoutefinderTask";

	private static final int PRINT_PERCENTAGE = 20; // print percentage after this increment
	
	private RouteCache mCache;
	private ArrayList<Trip> mTrips;
	private int mStartIndex;
	private int mEndIndex;
	private CountDownLatch mLatch;
	private int mThreadId;

	public RoutefinderTask (RouteCache cache, ArrayList<Trip> trips, 
			int startI, int endI, CountDownLatch latch, int threadId) {
		mCache = cache;
		mTrips = trips;
		mStartIndex = startI;
		mEndIndex = endI;
		mLatch = latch;
		mThreadId = threadId;
	}

	public void run() {
		Routefinder router = new Routefinder();
		int totalToRoute = (mEndIndex - mStartIndex) * mTrips.size() * 4;
		int totalRouted = 0;
		int lastPercent = -1;
		for(int i = mStartIndex; i < mEndIndex; i++) {

			// This trip's route
			Trip t1 = mTrips.get(i);
			int t1Id = t1.getIdentifier();
			mCache.putHash(t1Id, true, t1Id, false, 
					router.getTravelTimeMins(t1.getFirstEndpoint(), t1.getSecondEndpoint()));

			totalRouted++;
			for(int j = 0; j < mTrips.size(); j++) {
				Trip t2 = mTrips.get(j);
				int t2Id = t2.getIdentifier();

				// Don't route trips to themselves
				if(t1Id == t2Id) continue;

				// T1 origin to T2 origin
				mCache.putHash(t1Id, true, t2Id, true, 
						router.getTravelTimeMins(t1.getFirstEndpoint(), t2.getFirstEndpoint()));

				// T1 origin to T2 dest
				mCache.putHash(t1Id, true, t2Id, false,
						router.getTravelTimeMins(t1.getFirstEndpoint(), t2.getSecondEndpoint()));
			
				// T1 dest to T2 origin
				mCache.putHash(t1Id, false, t2Id, true,
						router.getTravelTimeMins(t1.getSecondEndpoint(), t2.getFirstEndpoint()));
			
				// T1 dest to T2 dest
				mCache.putHash(t1Id, false, t2Id, false, 
						router.getTravelTimeMins(t1.getSecondEndpoint(), t2.getSecondEndpoint()));
				
				totalRouted = totalRouted + 4;
			}
			int percent = (int)(((double)totalRouted / totalToRoute) * 100);
			if((percent / PRINT_PERCENTAGE) != (lastPercent / PRINT_PERCENTAGE)) {
				Log.info(TAG, "Worker thread " + mThreadId + (percent < 10 ? " at 0" : " at ") + percent + "%");
				lastPercent = percent;
			}
		}
		
		// Work is done, decrement latch
		mLatch.countDown();
	}
}
