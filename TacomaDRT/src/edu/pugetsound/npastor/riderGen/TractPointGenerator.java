package edu.pugetsound.npastor.riderGen;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.geometry.DirectPosition1D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

import delaunay_triangulation.BoundingBox;
import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

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
	
	ShapefileDataStore mCensusFile;
    FeatureCollection mFeatureCollection;
    MathTransform mProjectionTransform;

    Random mRand;
    
	public TractPointGenerator() {
		File file = new File(Constants.PC_CENSUS_SHP);
		mRand = new Random();
		
		try {
			mCensusFile = new ShapefileDataStore(file.toURI().toURL());
			FeatureSource featureSource = mCensusFile.getFeatureSource();
			mFeatureCollection = featureSource.getFeatures();
			
			// Make the transformation from file CRS to long/lat CRS
			CoordinateReferenceSystem fileCRS = featureSource.getSchema().getCoordinateReferenceSystem();
			mProjectionTransform = CRS.findMathTransform(fileCRS, DefaultGeographicCRS.WGS84, true);
		} catch(MalformedURLException ex) {
			Log.error(TAG, "Error opening census tract file.");
			ex.printStackTrace();
			System.exit(1);
		} catch(IOException ex) {
			Log.error(TAG, "Error opening census tract file.");
			ex.printStackTrace();
			System.exit(1);
		} catch(FactoryException ex) {
			Log.error(TAG, "Error creating transformation from file CRS to WGS84 projection");
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Generates a random point within the specified census tract
	 * @param tract The tract in which to generate a point
	 * @return The point, returned as long/lat coordinates
	 */
	public Point_dt randomPointInTract(String tract) {

		Point_dt latLong = new Point_dt();
		
		// Iterate through file to find correct census tract
		FeatureIterator iterator = mFeatureCollection.features();
		while (iterator.hasNext()) {
			
			// Pull out the feature we're concerned with: census tract name
			Feature feature = (Feature) iterator.next();
			String fileTract = (String) feature.getProperty(TRACT_PROPERTY_NAME).getValue();

			// If tract matches specified tract, pull out geometric data
			if(fileTract.equals(tract)) {
				GeometryAttributeImpl featureGeometry = (GeometryAttributeImpl) feature.getDefaultGeometryProperty();
				
				// Get the shape data
				MultiPolygon geo = (MultiPolygon) featureGeometry.getValue();
				latLong = randomPointInPoly(geo);
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
	private Point_dt randomPointInPoly(Geometry polygon) {
		
		try {
			// First we project the point to long/lat
			Geometry projectedPoly = JTS.transform(polygon, mProjectionTransform);

			// Then pick a random point
			ArrayList<Triangle_dt> triangles = triangulatePolygon(projectedPoly);
//			ArrayList<Triangle_dt> triangles = JTSTriangulate(projectedPoly);
			Triangle_dt chosenTri = generateWeightedTriangle(triangles);
			return generatePointInTriangle(chosenTri);
		} catch (TransformException ex) {
			Log.error(TAG, "Unable to transform polygon into long/lat");
			ex.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	/**
	 * Splits the specified polygon into triangles
	 * @param polygon The polygon to split into triangles
	 * @return A list of triangles
	 */
	private ArrayList<Triangle_dt> triangulatePolygon(Geometry polygon) {
		
		ArrayList<Triangle_dt> triangles = new ArrayList<Triangle_dt>();
		// Use Delaunay Triangulation library
		Delaunay_Triangulation triangulate = new Delaunay_Triangulation();
		Coordinate[] coords = polygon.getCoordinates();
		
		// Add all points from polygon
		boolean isFirstCoord = true;
		for(Coordinate c : coords) {
			if(isFirstCoord) isFirstCoord = false;
			else triangulate.insertPoint(new Point_dt(c.x, c.y));
		}
		Iterator<Triangle_dt> iter = triangulate.trianglesIterator();
		
		while(iter.hasNext()) 
			triangles.add(iter.next());
		
		return triangles;
	}
	
	/**
	 * Picks a triangle, weighted according to the triangle areas
	 * @param triangles List of triangles
	 * @return A random triangle, weighted according to its area
	 */
	private Triangle_dt generateWeightedTriangle(ArrayList<Triangle_dt> triangles) {
		
		double totalArea = 0;
		Point_dt p1, p2, p3, baseMp; // Vertices and base midpoint
		double base, height; // Base and height lengths
		ArrayList<TriangleWrapper> triangleWrap = new ArrayList<TriangleWrapper>(); // So that we don't need to calculate area twice
		
		// First calculate total polygon area
		for(Triangle_dt tr : triangles) {
			if(tr.isHalfplane()) break; // Only consider if this is a triangle
			p1 = tr.p1();
			p2 = tr.p2();
			p3 = tr.p3();			
			base = p1.distance(p2);
			// Calculate base midpoint
			double mpX = Math.abs(p1.x() - p2.x() / 2);
			double mpY = Math.abs(p1.y() - p2.y() / 2);
			baseMp = new Point_dt(mpX, mpY);
			height = baseMp.distance(p3);
			// Add triangle area to cumulative area
			double area = (height * base) / 2;
			totalArea += area;
			triangleWrap.add(new TriangleWrapper(tr, area));
		}
		
		// Pick random number between 0 and total area
		double random = mRand.nextDouble() * totalArea;
		double runningTotal = 0;
		// Loop through triangles again, break when the random number falls within running total
		for(TriangleWrapper tr : triangleWrap) {
			runningTotal += tr.getArea();
			if(random < runningTotal)
				return tr.getDtTriangle();
		}
		return null;
	}
	
	/**
	 * Generates a random point within a triangle
	 * Adapted from: http://parametricplayground.blogspot.com/2011/02/random-points-distributed-inside.html
	 * @param triangle The triangle in which to generate a point
	 * @return The point
	 */
	private Point_dt generatePointInTriangle(Triangle_dt triangle) {
	
		// Point "weight" coefficients
		double a = mRand.nextDouble();
		double b = mRand.nextDouble() * (1 - a);
		double c = 1 - a - b;
		
		// Points
		Point_dt A = triangle.p1();
		Point_dt B = triangle.p2();
		Point_dt C = triangle.p3();
		
		double resultX = (a * A.x()) + (b * B.x()) + (c * C.x());
		double resultY = (a * A.y()) + (b * B.y()) + (c * C.y());
			
		return new Point_dt(resultX, resultY, A.z());
	}
	
	private ArrayList<Triangle_dt> JTSTriangulate(Geometry polygon) {
		
		ArrayList<Triangle_dt> triangles = new ArrayList<Triangle_dt>();
		
		DelaunayTriangulationBuilder triangulate = new DelaunayTriangulationBuilder();
		triangulate.setTolerance(0.0);
		triangulate.setSites(polygon);
		GeometryCollection geos = (GeometryCollection) triangulate.getTriangles(new GeometryFactory());
		
		for(int i = 0; i < geos.getNumGeometries(); i++) {
			Geometry triangle = geos.getGeometryN(0);
			Coordinate[] coords = triangle.getCoordinates();
			Point_dt p1 = new Point_dt(coords[0].x, coords[0].y);
			Point_dt p2 = new Point_dt(coords[1].x, coords[1].y);
			Point_dt p3 = new Point_dt(coords[2].x, coords[2].y);
			triangles.add(new Triangle_dt(p1, p2, p3));
		}
		
		return triangles;
	}
	
	/*
	 * Wrapper class for associating area with Triangle_dt objects 
	 */
	private class TriangleWrapper {
		Triangle_dt mTriangle;
		double mArea;
		
		public TriangleWrapper(Triangle_dt triangle, double area) {
			mTriangle = triangle;
			mArea = area;
		}
		
		public double getArea() {
			return mArea;
		}

		public Triangle_dt getDtTriangle() {
			return mTriangle;
		}
	}	
}


