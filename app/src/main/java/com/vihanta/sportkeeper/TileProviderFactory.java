package com.vihanta.sportkeeper;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class TileProviderFactory {
	
	public static WMSTileProvider getOsgeoWmsTileProvider() {

        //This is configured for:
        // http://beta.sedac.ciesin.columbia.edu/maps/services
        // (TODO check that this WMS service still exists at the time you try to run this demo,
        // if it doesn't, find another one that supports EPSG:900913
        /*final String WMS_FORMAT_STRING =
				"http://sedac.ciesin.columbia.edu/geoserver/wms" +
	    		"?service=WMS" +
	    		"&version=1.1.1" +  			
	    		"&request=GetMap" +
	    		"&layers=gpw-v3-population-density_2000" +
	    		"&bbox=%f,%f,%f,%f" +
	    		"&width=1024" +
	    		"&height=1024" +
	    		"&srs=EPSG:900913" +
	    		"&format=image/png" +				
	    		"&transparent=true";
		*/

		final String WMS_FORMAT_STRING =
				"http://tiles.kartat.kapsi.fi/peruskartta" +
						"?service=WMS" +
						"&version=1.3.0" +
						"&request=GetMap" +
					//	"&layers=taustakartta_01" +
						"&bbox=%f,%f,%f,%f" +
						"&width=1024" +
						"&height=1024" +
						"&crs=EPSG:3857" +
						"&format=image/png"+
						"&transparent=false";
		
		WMSTileProvider tileProvider = new WMSTileProvider(256,256) {
        	
	        @Override
	        public synchronized URL getTileUrl(int x, int y, int zoom) {
	        	double[] bbox = getBoundingBox(x, y, zoom);
	            String s = String.format(Locale.US, WMS_FORMAT_STRING, bbox[MINX],
	            		bbox[MINY], bbox[MAXX], bbox[MAXY]);
	            //Log.d("WMSDEMO", s);
	            URL url = null;
	            try {
	                url = new URL(s);
	            } catch (MalformedURLException e) {
	                throw new AssertionError(e);
	            }
	            return url;
	        }
		};
		return tileProvider;
	}
}
