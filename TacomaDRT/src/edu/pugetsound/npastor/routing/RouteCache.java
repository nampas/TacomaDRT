package edu.pugetsound.npastor.routing;

/**
 * An immutable route cache. Use RouteCacheBuilder to instantiate.
 * @author Nathan P
 *
 */
public class RouteCache {
	
	public final static String TAG = "RouteCache"; 

	private byte[][] mCache;
	
	private RouteCache(byte[][] cache) {
		mCache = cache;
	}

	/**
	 * Gets an element from the cache. The caller need not deal with hashing, hashing happens
	 * internally
	 * @param t1Id Id of first trip
	 * @param t1Origin True if route begins at the first trip's origin, false if it begins at the destination
	 * @param t2Id Id of second trip
	 * @param t2Origin True if route ends at second trip's origin, false if it ends at the destination
	 * @return The travel time between the locations specified
	 */
	public byte getHash(int t1Id, boolean t1Origin, int t2Id, boolean t2Origin) {
		int[] indices = hash(t1Id, t1Origin, t2Id, t2Origin);
		return mCache[indices[0]][indices[1]];
	}
	
	public byte getDirect(int i1, int i2) {
		return mCache[i1][i2];
	}
	
	/**
	 * Hashes the specified route, returning an array of cache indices where the
	 * route can be found
	 * @param t1Id Id of first trip
	 * @param t1Origin True if route begins at the first trip's origin, false if it begins at the destination
	 * @param t2Id Id of second trip
	 * @param t2Origin True if route ends at second trip's origin, false if it ends at the destination
	 * @return
	 */
	private static int[] hash(int t1Id, boolean t1Origin, int t2Id, boolean t2Origin) {
		int[] indices = new int[2];
		indices[0] = t1Id * 2 + (t1Origin ? 0 : 1);
		indices[1] = t2Id * 2 + (t2Origin ? 0 : 1);
		return indices;
	}
	
	/**
	 * A route cache builder
	 * @author npastor
	 *
	 */
	public static class RouteCacheBuilder {
		
		private byte[][] mCache;
		
		public RouteCacheBuilder(int numTrips) {
			mCache = new byte[numTrips*2][numTrips*2];
		}
		
		public void putHash(int t1Id, boolean t1Origin, int t2Id, boolean t2Origin, byte value) {
			int[] indices = hash(t1Id, t1Origin, t2Id, t2Origin);
			mCache[indices[0]][indices[1]] = value;
		}
		
		/**
		 * Puts an element in the cache at the specified index. This should be used with care,
		 * for most access cases, putHash() is desirable
		 * @param i1 First index
		 * @param i2 Second index
		 * @param value Value to insert at specified location
		 */
		public void putDirect(int i1, int i2, byte value) {
			mCache[i1][i2] = value;
		}
		
		public RouteCache build() {
			return new RouteCache(mCache);
		}
	}
}
