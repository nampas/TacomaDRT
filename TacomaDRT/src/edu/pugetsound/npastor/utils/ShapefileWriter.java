package edu.pugetsound.npastor.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.directory.DirectoryDataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.opengis.feature.simple.SimpleFeatureType;

public class ShapefileWriter {
	
	public static final String TAG = "ShapefileWriter";

	/**
	 * Writes the geographic data to a shapefile
	 * Adapted from: http://docs.geotools.org/latest/tutorials/feature/csv2shp.html#write-the-feature-data-to-the-shapefile
	 * @param featureType The feature type of the collection
	 * @param collection A collection of feature types
	 * @param shpFile A file descripter. This specifies name and destination of the file
	 */
	public static void writeShapefile(SimpleFeatureType featureType, SimpleFeatureCollection collection, File shpFile) {
		
        Log.iln(TAG, "Writing trips to shapefile at: " + shpFile.getPath());
        
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        try {
	        Map<String, Serializable> params = new HashMap<String, Serializable>();
	        params.put("url", shpFile.toURI().toURL());
	        params.put("create spatial index", Boolean.TRUE);
	        
	        // Build the data store, which will hold our collection
	        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
//	        DataStore dataStore = dataStoreFactory.createNewDataStore(params);
	        dataStore.createSchema(featureType);
	        
	        // Finally, write the features to the shapefile
	        Transaction transaction = new DefaultTransaction("create");
	        String typeName = dataStore.getTypeNames()[0];
	        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
	        
	        if (featureSource instanceof FeatureStore) {
	            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	            featureStore.setTransaction(transaction);
                featureStore.addFeatures(collection);
                transaction.commit();
                Log.iln(TAG, "  File succesfully writen at:" + shpFile.getPath());
	        }
	        transaction.close();
        } catch (MalformedURLException ex) {
        	Log.e(TAG, "Unable to save trips to shapefile");
        	ex.printStackTrace();
        } catch (IOException ex) {
        	Log.e(TAG, "Unable to open or write to shapefile");
        	ex.printStackTrace();
        }
	}
	

}
