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
	public static final String A_STAR_BI = "astarbi";
	public static final String A_STAR = "astar";
	public static final String DIJKSTRA = "dijkstra";
	public static final String DIJKSTRA_BI = "dijkstrabi";
	public static final String DIJKSTRA_NATIVE = "dijkstraNative";
	
	// The pathfinding algorithm we'll use
	public static final String ROUTE_ALGORITHM = A_STAR_BI;
	
	GraphHopperAPI mRouter;
	
	public Routefinder() {
		mRouter = new GraphHopper().forServer();
		((GraphHopper) mRouter).setCHShortcuts("fastest");
		
		// Load the pre-built Tacoma street graph
		mRouter.load("files/tac-gh");
	}
	
	public GHResponse findRoute(Point2D origin, Point2D destination) {
		// Build request and set pathfinding algorithm
		GHRequest routeRequest = new GHRequest(origin.getY(), origin.getX(), destination.getY(), destination.getX());
		routeRequest.setAlgorithm(A_STAR_BI);
	
		// Do routing
		GHResponse routeResponse = mRouter.route(routeRequest);
		Log.info(TAG, "Distance in meters: " + routeResponse.getDistance() + ". Seconds to complete: " + routeResponse.getTime());
		PointList points = routeResponse.getPoints();
		for(int i = 0; i < points.getSize(); i++) {
			System.out.println(points.getLatitude(i) + "  " + points.getLongitude(i));
		}
		
		return routeResponse;
	}
}
