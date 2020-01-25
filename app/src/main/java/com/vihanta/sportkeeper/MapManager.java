package com.vihanta.sportkeeper;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
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

import java.util.List;

public class MapManager implements OnMapReadyCallback {

    private static final String PACKAGE_NAME =
            "com.vihanta.sportkeeper.locationservice";

    static final String DISTANCE_UPDATE= PACKAGE_NAME + ".distance";

    private static final String TAG = MapManager.class.getSimpleName();

    private GoogleMap mMap;
    LocalBroadcastManager broadcaster;

    private Location currentLoc;
    private LocationManager locMan;

    private Polyline mTrail;
    private Marker mPosition;

    private float mTrailLength = 0;
    private Boolean mStarted = false;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private TrailModel viewModel;


    public MapManager(Context pContext, SupportMapFragment mapFragment,
                      LocalBroadcastManager brdc, TrailModel pModel){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mapFragment.getMapAsync(this);
        //broadcaster = brdc;
        broadcaster = LocalBroadcastManager.getInstance(pContext);

        viewModel = pModel;
        viewModel.trail.observe((LifecycleOwner) pContext, new  Observer<List<Location>>() {

            @Override
            public void onChanged(@Nullable List<Location> locations) {
                Log.d(TAG, "model Change: ");
                Location latest = viewModel.getLatest();
                if(latest != null) {
                    setOwnPosition(latest);
                    addToPolyline(latest);
                }
            }
        });

        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(MapsActivity.CANCEL_ACTION));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()){

                case MapsActivity.CANCEL_ACTION:
                    mStarted = false;
                    removeMarkers();
                    break;
            }
        }
    };

    private void setOwnPosition(Location pLoc){
        try {
            LatLng locat = new LatLng(pLoc.getLatitude(), pLoc.getLongitude());
            mPosition.setPosition(locat);
            mPosition.setVisible(true);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(locat));

        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private void addToPolyline(Location pLoc){

        try {
            LatLng p = new LatLng(pLoc.getLatitude(), pLoc.getLongitude());
            List<LatLng> points = mTrail.getPoints();
            points.add(p);
            mTrail.setPoints(points);
            //getLengthOfPolyline();

        }catch (Exception e){
            Log.e(TAG, e.getMessage() );
        }
    }

    public void drawPolylineFromList(){
        if(mTrail != null) {
            removeMarkers();
            try {
                mTrail.setPoints(viewModel.getLatLongList());
                //getLengthOfPolyline();

            }catch (Exception e){
                Log.e(TAG, e.getMessage() );
            }
        }
    }


    public void removeMarkers(){
        mTrail.remove();
        mPosition.remove();

        mTrail = mMap.addPolyline(new PolylineOptions().clickable(false));
        mPosition = mMap.addMarker(new MarkerOptions()
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

                mTrail = mMap.addPolyline(new PolylineOptions().clickable(false));
                mPosition = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(0, 0))
                        .visible(false)
                        .title("my position"));

                mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

            }
        });

    }
}
