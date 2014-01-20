package edu.pugetsound.npastor.routing;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import edu.pugetsound.npastor.routing.RouteCache.RouteCacheBuilder;
import edu.pugetsound.npastor.utils.Trip;

public class RoutefinderTask implements Runnable {
	
	public static final String TAG = "RoutefinderTask";

	private static final int UPDATE_INTERVAL = 1000; // Update progress at this interval
	
	private RouteCacheBuilder mCache;
	private ArrayList<Trip> mTrips;
	private int mStartIndex;
	private int mEndIndex;
	private CountDownLatch mLatch;
	private AtomicInteger mProgress;


	public RoutefinderTask (RouteCacheBuilder cache, ArrayList<Trip> trips, 
			int startI, int endI, CountDownLatch latch, AtomicInteger progress) {
		mCache = cache;
		mTrips = trips;
		mStartIndex = startI;
		mEndIndex = endI;
		mLatch = latch;
		mProgress = progress;
	}

	public void run() {
		Routefinder router = new Routefinder();
		int routedAtLastUpdate = 0;
		int totalRouted = 0;

		for(int i = mStartIndex; i < mEndIndex; i++) {

			// This trip's route
			Trip t1 = mTrips.get(i);
			int t1Id = t1.getIdentifier();
			mCache.putHash(t1Id, true, t1Id, false, 
					router.getTravelTimeMins(t1.getOriginPoint(), t1.getDestinationPoint()));

			totalRouted++;
			for(int j = 0; j < mTrips.size(); j++) {
				Trip t2 = mTrips.get(j);
				int t2Id = t2.getIdentifier();

				// Don't route trips to themselves
				if(t1Id == t2Id) continue;

				// T1 origin to T2 origin
				mCache.putHash(t1Id, true, t2Id, true, 
						router.getTravelTimeMins(t1.getOriginPoint(), t2.getOriginPoint()));

				// T1 origin to T2 dest
				mCache.putHash(t1Id, true, t2Id, false,
						router.getTravelTimeMins(t1.getOriginPoint(), t2.getDestinationPoint()));
			
				// T1 dest to T2 origin
				mCache.putHash(t1Id, false, t2Id, true,
						router.getTravelTimeMins(t1.getDestinationPoint(), t2.getOriginPoint()));
			
				// T1 dest to T2 dest
				mCache.putHash(t1Id, false, t2Id, false, 
						router.getTravelTimeMins(t1.getDestinationPoint(), t2.getDestinationPoint()));
				
				totalRouted = totalRouted + 4;
			}
			int increment = totalRouted - routedAtLastUpdate;
			
			// To avoid synchronization bottlenecks on the AtomicInteger,
			// don't update every iteration
			if(increment > UPDATE_INTERVAL) {
				mProgress.addAndGet(increment);
				routedAtLastUpdate = totalRouted;
			}
		}
		
		// Work is done, decrement latch
		mLatch.countDown();
	}
}
