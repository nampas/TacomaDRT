package edu.pugetsound.npastor.routing;

import java.awt.geom.Point2D;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;
import com.graphhopper.util.PointList;

import edu.pugetsound.npastor.utils.Log;

/**
 * For finding quickest driving distances between two points
 * @author Nathan P
 *
 */
public class Routefinder {
	
	public static final String TAG = "Routefinder";

	// GraphHopper supported pathfinding algorithms
	private static final String A_STAR_BI = "astarbi";
	private static final String A_STAR = "astar";
	private static final String DIJKSTRA = "dijkstra";
	private static final String DIJKSTRA_BI = "dijkstrabi";
	private static final String DIJKSTRA_NATIVE = "dijkstraNative";
	
	// The pathfinding algorithm we'll use. Dijkstra Native appears to be the fastest
	private static final String ROUTE_ALGORITHM = A_STAR_BI;
	
	GraphHopperAPI mRouter;
	
	public Routefinder() {
		mRouter = new GraphHopper().forServer();
		((GraphHopper) mRouter).setCHShortcuts("fastest");
		
		// Load the pre-built Tacoma street graph
		mRouter.load("files/tac-gh");
	}
	
	/**
	 * Finds driving directions between the two specified points
	 * @param origin Trip origin location
	 * @param destination Trip destination location
	 * @return A GHResponse object representing result of routing
	 */
	public GHResponse findRoute(Point2D origin, Point2D destination) {
		// Build request and set pathfinding algorithm
		GHRequest routeRequest = new GHRequest(origin.getY(), origin.getX(), destination.getY(), destination.getX());
		routeRequest.setAlgorithm(ROUTE_ALGORITHM);
	
		// Do routing
		GHResponse routeResponse = mRouter.route(routeRequest);
		return routeResponse;
	}
	
	/**
	 * A convenience method for finding the time required to drive between the 
	 * specified points
	 * @param origin Trip origin location
	 * @param destination Trip destination location
	 * @return The time in seconds to travel between the specified points
	 */
	public byte getTravelTimeMins(Point2D origin, Point2D destination) {
		GHResponse response = findRoute(origin, destination);
		return (byte) (response.getTime() / 60);
	}
}
