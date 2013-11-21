package edu.pugetsound.npastor.routing;

import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class acts as a LRU cache for routes. Because of the excessive computational cost of
 * pathfinding, we cache as many routes as the heap will allow.
 * @author Nathan P
 *
 */
public class LRURouteCache extends LinkedHashMap<Integer, Byte>{

	private static final int MAX_ENTRIES = 1000000;
	
	private static LRURouteCache mInstance = null;

	/**
	 * This class implements a concurrent singleton pattern. Method is synchronized
	 * to avoid the creation of multiple instances.
	 * @return The singleton LRURouteCache instance
	 */
	public synchronized static LRURouteCache getInstance() {
		if(mInstance == null)
			mInstance = new LRURouteCache();
		return mInstance;			
	}
	
	public synchronized Byte get(Integer key) {
		return super.get(key);
	}
	
	public synchronized Byte put(Integer key, Byte value) {
		return super.put(key, value);
	}
	
	// This class acts as a LRU cache, so remove eldest entries when we've exceeded capacity
	@Override
	protected synchronized boolean removeEldestEntry(Map.Entry<Integer, Byte> eldest) {
        return size() > MAX_ENTRIES;
    }
	
	/**
	 * Hashes the specified route. This hash should be used to get and set elmements
	 * in thte cache. Based on the algorithm described by Joshua Bloch in Effective
	 * Java 2nd Edition 
	 * @param origin Origin point
	 * @param destination Destination point
	 * @return The hash of thet two route
	 */
	public static int makeRouteHash(Point2D origin, Point2D destination) {
		int result = 17;
		long temp = Double.doubleToLongBits(origin.getX());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(origin.getY());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(destination.getX());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(destination.getY());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		
		return result;
	}
}
