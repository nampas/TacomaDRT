package edu.pugetsound.npastor.riderGen;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;


/**
 * This class is responsible for generating a random point within a specified census tract
 * This point is returned as longitude/latitude coordinates (in that order!)
 * Code adapted from: 
 *   http://osdir.com/ml/gis.geotools2.user/2006-07/msg00060.html
 *   http://docs.codehaus.org/display/GEOTOOLS/Data+Reading
 * @author Nathan P
 *
 */
public class TractPointGenerator {

	public final static String TAG = "TractPointGenerator";
	public final String TRACT_PROPERTY_NAME = "NAME10";
	
	private ShapefileDataStore mCensusFile;
    private FeatureCollection mCensusFeatureCollection;
    private MathTransform mCensusProjectionTransform;
    
    private CityBoundaryShp mTacomaBoundary;

	GeometryFactory mGeoFactory;
    Random mRand;
    
	public TractPointGenerator() {
		File censusFile = new File(Constants.FILE_BASE_DIR + Constants.PC_CENSUS_SHP);
		File boundaryFile = new File(Constants.FILE_BASE_DIR + Constants.TACOMA_BOUNDARY_SHP);
		mGeoFactory = new GeometryFactory();
		mRand = new Random();
		
		try {
			mCensusFile = new ShapefileDataStore(censusFile.toURI().toURL());
			FeatureSource censusFeatureSource = mCensusFile.getFeatureSource();
			mCensusFeatureCollection = censusFeatureSource.getFeatures();

			// Make the transformation from file CRS to long/lat CRS
			CoordinateReferenceSystem censusFileCRS = censusFeatureSource.getSchema().getCoordinateReferenceSystem();
			mCensusProjectionTransform = CRS.findMathTransform(censusFileCRS, DefaultGeographicCRS.WGS84, true);

			// We need a Tacoma boundary file to ensure points are on land
//			mTacomaBoundary = new CityBoundaryShp();
			mTacomaBoundary = CityBoundaryShp.getInstance();
			
		} catch(MalformedURLException ex) {
			Log.e(TAG, ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		} catch(IOException ex) {
			Log.e(TAG, ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		} catch(FactoryException ex) {
			Log.e(TAG, "Error creating transformation from file CRS to WGS84 projection. " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Generates a random point within the specified census tract
	 * @param tract The tract in which to generate a point
	 * @return The point, returned as long/lat coordinates
	 */
	public Point2D randomPointInTract(String tract) {

		Point2D latLong = new Point2D.Double();
		
		// Iterate through file to find correct census tract
		FeatureIterator<Feature> iterator = mCensusFeatureCollection.features();
		while (iterator.hasNext()) {
			
			// Pull out the feature we're concerned with: census tract name
			Feature feature = (Feature) iterator.next();
			String fileTract = (String) feature.getProperty(TRACT_PROPERTY_NAME).getValue();

			// If tract matches specified tract, pull out geometric data
			if(fileTract.equals(tract)) {
				GeometryAttributeImpl featureGeometry = (GeometryAttributeImpl) feature.getDefaultGeometryProperty();
				
				// Get the shape data
				MultiPolygon geo = (MultiPolygon) featureGeometry.getValue();
				// Loop until we find a point within the Tacoma city boundaries. Some census tracts overlap other jurisdictions,
				// and some census tracts include ocean/harbor space!
				boolean pointInBoundary = false;
				while(!pointInBoundary) {
					latLong = randomPointInPoly(geo);
					pointInBoundary = mTacomaBoundary.isInBoundary(latLong);
				}
				break;
			}	
		}
		iterator.close(); // VERY IMPORTANT!! Out of memory errors otherwise on simulation sizes > ~3300
		return latLong;
	}
	
	/**
	 * Generates a random point within a shapefile polygon
	 * @param polygon The polygon
	 * @return The point
	 */
	private Point2D randomPointInPoly(Geometry polygon) {
		
		try {
			// First we project the point to long/lat
			Geometry projectedPoly = JTS.transform(polygon, mCensusProjectionTransform);

			// Then get a point in the polygon
			return boundingBoxMethod(projectedPoly);
		} catch (TransformException ex) {
			Log.e(TAG, "Unable to transform polygon into long/lat");
			ex.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	/**
	 * Generates a random point inside the polygon using the bounding box method.
	 * Points are generated randomly within the bounding box, and the the first point
	 * which also lies within polygon is returned
	 * @param polygon Polygon in which to make random point
	 * @return A random point within the polygon
	 */
	private Point2D boundingBoxMethod(Geometry polygon) {
		Geometry boundingBox = polygon.getEnvelope();
		Coordinate[] boundingCoords = boundingBox.getCoordinates();
		
		// Determine range of values
		double maxX = 0;
		double minX = 0;
		double maxY = 0;
		double minY = 0;
		for(int i = 0; i < boundingCoords.length; i++) {
			Coordinate c = boundingCoords[i];
			if(i == 0) { // Initialize values
				maxX = c.x;
				minX = c.x;
				maxY = c.y;
				minY = c.y;
			} else {
				maxX = Math.max(maxX, c.x);
				minX = Math.min(minX, c.x);
				maxY = Math.max(maxY, c.y);
				minY = Math.min(minY, c.y);
			}			
		}
		
		Point point = null;
		// Determine bounding box range
		double xRange = Math.abs(maxX - minX);
		double yRange = Math.abs(maxY - minY);
		// Loop until we have a random point inside the polygon
		boolean pointFound = false;
		while(!pointFound) {
			double randX = mRand.nextDouble() * xRange + minX;
			double randY = mRand.nextDouble() * yRange + minY;
			point = mGeoFactory.createPoint(new Coordinate(randX, randY));
			if(polygon.contains(point)) 
				pointFound = true;
		}
		
		return new Point2D.Double(point.getX(), point.getY());
	}
	
	public void onGenerationComplete() {
		mTacomaBoundary.close();
		mCensusFile.dispose();
	}
}


