package edu.pugetsound.npastor.simulation;

import java.util.Scanner;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.GraphHopperAPI;

/**
 * Utility for verifying driving times
 * @author Nathan P
 *
 */
public class VerifyRouteUtility {

	
	public static void main(String[] args) {
		GraphHopperAPI router = new GraphHopper().forServer();
		((GraphHopper) router).setCHShortcuts("fastest");
		
		// Load the pre-built Tacoma street graph
		router.load("files/tac-gh");
		
		boolean doQuit = false;
		Scanner scanner = new Scanner(System.in);
		scanner.useDelimiter("\n");
		while(!doQuit) {
			System.out.print("origin: ");
			String[] origin = scanner.next()
					.replaceAll(scanner.delimiter().pattern(), "")
					.split(",");
			System.out.print("destination: ");
			String[] dest = scanner.next().split(",");
			System.out.println("Routing " + origin[0] + ", " + origin[1] 
					+ " to " + dest[0] + ", " + dest[1]);

			GHRequest routeRequest = new GHRequest(Double.valueOf(origin[1]),
												Double.valueOf(origin[0]),
												Double.valueOf(dest[1]),
												Double.valueOf(dest[0]));
			routeRequest.setAlgorithm("astarbi");
			GHResponse response = router.route(routeRequest);
			
			System.out.println("Route time in seconds: " + response.getTime());
			
			System.out.print("Continue? ");
			String more = scanner.next().toLowerCase()
					.replaceAll(scanner.delimiter().pattern(), "");
			System.out.println(more);
			if(!more.equals("y") && !more.equals("yes"))
				doQuit = true;
		}
		scanner.close();
	}
}
