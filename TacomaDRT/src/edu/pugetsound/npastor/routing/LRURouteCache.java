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
public class LRURouteCache extends LinkedHashMap<Integer, RouteWrapper>{

	private static final int MAX_ENTRIES = 11000000;
	
	private static final int INIT_SIZE = 1000000;
	
	private static LRURouteCache mInstance = null;
	
	private LRURouteCache(int initSize) {
		super(initSize);
	}

	/**
	 * This class implements a concurrent singleton pattern. Method is synchronized
	 * to avoid the creation of multiple instances.
	 * @return The singleton LRURouteCache instance
	 */
	public synchronized static LRURouteCache getInstance() {
		if(mInstance == null)
			mInstance = new LRURouteCache(INIT_SIZE);
		return mInstance;			
	}
	
	public RouteWrapper get(Integer key) {
		return super.get(key);
	}
	
	public synchronized RouteWrapper put(Integer key, RouteWrapper value) {
		return super.put(key, value);
	}
	
	// This class acts as a LRU cache, so remove eldest entries when we've exceeded capacity
	@Override
	protected synchronized boolean removeEldestEntry(Map.Entry<Integer, RouteWrapper> eldest) {
        return size() > MAX_ENTRIES;
    }
}
