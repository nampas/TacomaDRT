package edu.pugetsound.npastor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.geometry.DirectPosition1D;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.feature.Feature;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;


/**
 * This class is responsible for generating a random point within a specified census tract
 * This point is returned as lat/long coordinates
 * Code adapted from: 
 *   http://osdir.com/ml/gis.geotools2.user/2006-07/msg00060.html
 *   http://docs.codehaus.org/display/GEOTOOLS/Data+Reading
 * @author Nathan P
 *
 */
public class TractPointGenerator {

	public final String TRACT_PROPERTY_NAME = "NAME10";
	
	ShapefileDataStore mCensusFile;
    FeatureSource mFeatureSource;
    FeatureCollection mFeatureCollection;
    Random mRand;
    
	public TractPointGenerator() {
		File file = new File(Constants.PC_CENSUS_SHP);
		mRand = new Random();
		
		try {
			mCensusFile = new ShapefileDataStore(file.toURI().toURL());
			mFeatureSource = mCensusFile.getFeatureSource();
			mFeatureCollection = mFeatureSource.getFeatures();
		} catch(MalformedURLException ex) {
			System.out.println("Error opening census tract file.");
			ex.printStackTrace();
			System.exit(1);
		} catch(IOException ex) {
			System.out.println("Error opening census tract file.");
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Generates a random point within the specified census tract
	 * @param tract The tract in which to generate a point
	 * @return The point, returned as lat/long coordinates
	 */
	public double[] generateRandomPoint(String tract) {

		double[] latLong = new double[2];
		
		// Iterate through file to find correct census tract
		FeatureIterator iterator = mFeatureCollection.features();
		while (iterator.hasNext()) {
			
			// Pull out the feature we're concerned with: census tract name
			Feature feature = (Feature) iterator.next();
			String fileTract = (String) feature.getProperty(TRACT_PROPERTY_NAME).getValue();

			// If tract matches specified tract, pull out geometric data
			if(fileTract.equals(tract)) {
				System.out.println("Found a match: " + fileTract);
				GeometryAttributeImpl featureGeometry = (GeometryAttributeImpl) feature.getDefaultGeometryProperty();
//				System.out.println(featureGeometry.toString());
				
				MultiPolygon geo = (MultiPolygon) featureGeometry.getValue();
				System.out.println(geo);
			}
			
		}
		return latLong;
	}
	
	/**
	 * Generates a random point within a polygon
	 * @param polygon The polygon
	 * @return The point
	 */
	private DirectPosition2D generatePoint(MultiPolygon polygon) {
		
		// TODO: USE MATH TO MAKE THIS BETTER!!
		// Get minimum bounding box for polygon
		DirectPosition2D point = new DirectPosition2D(0,0);
		Envelope bounds = (Envelope) polygon.getEnvelope();
		boolean validPoint = false;
		
//		while(!validPoint) {
//			double xMax = bounds.getMaximum(0) - bounds.getMinimum(0);
//			double yMax = bounds.getMaximum(1) - bounds.getMinimum(1);
//			
//			double x = mRand.nextDouble() * xMax;
//			double y = mRand.nextDouble() * yMax;
//			
//			DirectPosition2D dp = new DirectPosition2D(x, y);
//			if(polygon.contains() {
//				validPoint = true;
//				point = dp;
//			}
//		}
		return point;
	}

}
