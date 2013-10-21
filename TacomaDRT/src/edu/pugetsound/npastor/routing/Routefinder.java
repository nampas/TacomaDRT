package edu.pugetsound.npastor.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;

import delaunay_triangulation.Point_dt;
import edu.pugetsound.npastor.utils.Log;

/**
 * For finding quickets routes between two points
 * @author Nathan P
 *
 */
public class Routefinder {
	
	public static final String TAG = "Routefinder";

	// Supported pathfinding algorithms
	public static final String A_STAR_BI = "astarbi";
	public static final String A_STAR = "astar";
	public static final String DIJKSTRA = "dijkstra";
	public static final String DIJKSTRA_BI = "dijkstrabi";
	public static final String DIJKSTRA_NAT = "dijkstraNative";
	
	public static final String ROUTE_ALGORITHM = A_STAR_BI;
	
	GraphHopperAPI mRouter;
	
	public Routefinder() {
		mRouter = new GraphHopper().forServer();
		((GraphHopper) mRouter).setCHShortcuts("fastest");
		mRouter.load("files/tac-gh");
	}
	
	public void findRoute(Point_dt origin, Point_dt destination) {
		GHRequest routeRequest = new GHRequest(origin.y(), origin.x(), destination.y(), destination.x());
		routeRequest.setAlgorithm(A_STAR_BI);
		Log.info(TAG, "Routing between points: " + origin + "  " + destination);
		GHResponse routeResponse = mRouter.route(routeRequest);
		Log.info(TAG, "Distance in meters: " + routeResponse.getDistance() + ". Seconds to complete: " + routeResponse.getTime());
	}
}
