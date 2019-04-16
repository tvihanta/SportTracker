package com.vihanta.sportkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapManager implements OnMapReadyCallback {

    private static final String PACKAGE_NAME =
            "com.vihanta.sportkeeper.locationservice";

    private GoogleMap mMap;
    LocalBroadcastManager broadcaster;

    private Location currentLoc;
    private LocationManager locMan;

    private Polyline trail;
    private Marker ownPos;

    private float length = 0;
    private Boolean mStarted = false;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;


    public MapManager(SupportMapFragment mapFragment, LocalBroadcastManager brdc){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mapFragment.getMapAsync(this);
        broadcaster = brdc;

        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(LocationService.ACTION_BROADCAST));
        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(MapsActivity.START_ACTION));
        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(MapsActivity.CANCEL_ACTION));



    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MM", intent.getAction());
            switch (intent.getAction()){
                case LocationService.ACTION_BROADCAST:
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                    if(mStarted && location != null){
                        setOwnPosition(location);
                        addToPolyline(location);
                    }
                    break;
                case MapsActivity.START_ACTION:
                    if(mStarted){
                        mStarted = false;
                    } else {
                        mStarted = true;
                    }
                    break;
                case MapsActivity.PAUSE_ACTION:
                    if(mStarted){
                        mStarted = false;
                    } else {
                        mStarted = true;
                    }
                    break;
                case MapsActivity.CANCEL_ACTION:
                    break;



            }



        }
    };

    private void setOwnPosition(Location pLoc){
        try {
            LatLng locat = new LatLng(pLoc.getLatitude(), pLoc.getLongitude());
            ownPos.setPosition(locat);
            ownPos.setVisible(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(locat));

        }catch (Exception e){
            Log.e("MM.setOwnPosition", e.getMessage());
        }
    }

    private void getLengthOfPolyline(){

        Location currLocation = new Location("this");
        currLocation.setLatitude(trail.getPoints().get(trail.getPoints().size()-1).latitude);
        currLocation.setLongitude(trail.getPoints().get(trail.getPoints().size()-1).longitude);

        Location prevLocation = new Location("this");
        prevLocation.setLatitude(trail.getPoints().get(trail.getPoints().size()-2).latitude);
        prevLocation.setLongitude(trail.getPoints().get(trail.getPoints().size()-2).longitude);

        length += (prevLocation.distanceTo(currLocation));
        Intent intent2 = new Intent("position_update");
        intent2.putExtra("distance", length);
        broadcaster.sendBroadcast(intent2);

    }


    private void addToPolyline(Location pLoc){

        try {
            LatLng p = new LatLng(pLoc.getLatitude(), pLoc.getLongitude());
            List<LatLng> points = trail.getPoints();
            points.add(p);
            trail.setPoints(points);
            getLengthOfPolyline();

        }catch (Exception e){
            Log.e("ERROR in adding point", e.getMessage() );
        }

    }

    public void removeMarkers(){
        trail.remove();
        ownPos.remove();

        trail = mMap.addPolyline(new PolylineOptions().clickable(false));
        ownPos = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .visible(false)
                .title("my position"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            public void onMapLoaded() {
                TileProvider wmsTileProvider = TileProviderFactory.getOsgeoWmsTileProvider();
                TileOverlay overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(wmsTileProvider));

                trail = mMap.addPolyline(new PolylineOptions().clickable(false));
                ownPos = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(0, 0))
                        .visible(false)
                        .title("my position"));

                mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

            }
        });

    }



}
