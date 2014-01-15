package edu.pugetsound.npastor.riderGen;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
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
import com.vividsolutions.jts.geom.Point;

import edu.pugetsound.npastor.utils.Constants;
import edu.pugetsound.npastor.utils.Log;

public class CityBoundaryShp {

    private static final String TAG = "CityBoundaryShp";
	
	private ShapefileDataStore mTacomaBoundaryFile;
	private FeatureCollection mBoundaryFeatureCollection;
	private MathTransform mBoundaryProjectionTransform;
   
	GeometryFactory mGeoFactory;
    Random mRand;
	
	public CityBoundaryShp() {
		File boundaryFile = new File(Constants.FILE_BASE_DIR + Constants.TACOMA_BOUNDARY_SHP);
		mGeoFactory = new GeometryFactory();
		mRand = new Random();
		
		FeatureSource boundaryFeatureSource;
		try {
			mTacomaBoundaryFile = new ShapefileDataStore(boundaryFile.toURI().toURL());
			boundaryFeatureSource = mTacomaBoundaryFile.getFeatureSource();
			mBoundaryFeatureCollection = boundaryFeatureSource.getFeatures();
			
			// Make the transformation from file CRS to long/lat CRS
			CoordinateReferenceSystem boundaryFileCRS = boundaryFeatureSource.getSchema().getCoordinateReferenceSystem();
			mBoundaryProjectionTransform = CRS.findMathTransform(boundaryFileCRS, DefaultGeographicCRS.WGS84, true);
		} catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		} catch (FactoryException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
		
	
	}
	
	/**
	 * Checks if the given point is within the Tacoma city limits ON LAND
	 * @param point The point to examine
	 * @return True if point is within Tacoma city limits and on land, false if otherwise
	 */
	public boolean isInBoundary(Point2D point) {
		boolean inTacoma = true;
		// Get the Tacoma feature, which should be the only feature in the Tacoma boundary file
		FeatureIterator iterator = mBoundaryFeatureCollection.features();
		Feature feature = (Feature) iterator.next();

		GeometryAttributeImpl tacomaFeatureGeo = (GeometryAttributeImpl) feature.getDefaultGeometryProperty();
		Geometry tacomaShape = tacomaFeatureGeo.getValue();
		try {
			Geometry projectedShape = JTS.transform(tacomaShape, mBoundaryProjectionTransform);
			// Do the containing check
			if(!projectedShape.contains(mGeoFactory.createPoint(new Coordinate(point.getX(), point.getY()))))
				inTacoma = false;
		} catch (TransformException ex) {
			Log.e(TAG, "Unable to transform the Tacoma boundary file into long/lat. " + ex.getMessage());
			ex.printStackTrace();
		}
		
		iterator.close();
		return inTacoma;
	}
	
	/**
	 * Returns the city centroid point
	 * @return A point containing the long/lat coordinates of the city's centroid
	 */
	public Point2D getCityCentroid() {
		
		// Get the Tacoma feature, which should be the only feature in the Tacoma boundary file
		FeatureIterator iterator = mBoundaryFeatureCollection.features();
		Feature feature = (Feature) iterator.next();
		GeometryAttributeImpl tacomaFeatureGeo = (GeometryAttributeImpl) feature.getDefaultGeometryProperty();
		Geometry tacomaShape = tacomaFeatureGeo.getValue();
		
		Point point = tacomaShape.getCentroid();
		
		// Re-wrap point in the Java class
		return new Point2D.Double(point.getX(), point.getY());
	}
}
