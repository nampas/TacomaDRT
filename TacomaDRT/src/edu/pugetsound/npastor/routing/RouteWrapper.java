package edu.pugetsound.npastor.routing;

import java.awt.geom.Point2D;

public class RouteWrapper {

	public Point2D origin;
	public Point2D dest;
	public byte timeMins;
	
	public RouteWrapper(Point2D origin, Point2D dest, byte timeMins) {
		this.origin = origin;
		this.dest = dest;
		this.timeMins = timeMins;
	}
	
	public RouteWrapper(Point2D origin, Point2D dest) {
		this(origin, dest, (byte)-1);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		else {
			RouteWrapper compare = (RouteWrapper) obj;
			return compare.origin.equals(origin) && compare.dest.equals(dest);
		}
	}
	
	/**
	 * Hashes the specified route. Based on the algorithm described by Joshua Bloch in Effective
	 * Java 2nd Edition 
	 * @return A hash of the route
	 */
	@Override
	public int hashCode() {
		int result = 17;
		long temp = Double.doubleToLongBits(origin.getX());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(origin.getY());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(dest.getX());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(dest.getY());
		result = result * 31 * (int)(temp ^ (temp >>> 32));
		
		return result;
	}
}
