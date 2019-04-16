package com.vihanta.sportkeeper;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.SupportMapFragment;

public class MapsActivity extends AppCompatActivity {


    private LocalBroadcastManager broadcaster;
    private MapManager mapManager;

    private Boolean mStartedRecording = false;

    private TextView elapsedTime;
    private TextView distanceView;
    private Button startButton;
    private Button saveButton;
    private Button cancelButton;
    private LinearLayout otherButtons;

    private long millisecondTime, startTime, timeBuff, updateTime = 0L ;
    Handler handler;
    int seconds, minutes, milliSeconds ;

    float distance = 0;

    private LocationService mService = null;
    private boolean mBound = false;

    private static final String PACKAGE_NAME = R.string.app_ident+".mapsactivity";
    static final String START_ACTION = PACKAGE_NAME + ".start";
    static final String PAUSE_ACTION = PACKAGE_NAME + ".pause";
    static final String CANCEL_ACTION = PACKAGE_NAME + ".cancel";

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.requestLocationUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private static final String[] LOCATION_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };


    private View.OnClickListener startEvent = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("MAIN", "Started");
            if (!mStartedRecording) {
                mStartedRecording = true;
                startButton.setText(R.string.pause);
                otherButtons.setVisibility(View.INVISIBLE);

                startTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);

                Intent intent = new Intent(START_ACTION);
                broadcaster.sendBroadcast(intent);
            } else {
                mStartedRecording = false;
                startButton.setText(R.string.start);
                otherButtons.setVisibility(View.VISIBLE);

                timeBuff += millisecondTime;
                handler.removeCallbacks(runnable);

                Intent intent = new Intent(PAUSE_ACTION);
                //intent.putExtra("message", "This is my message!");
                broadcaster.sendBroadcast(intent);
            }
        }
    };

    private View.OnClickListener cancel = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("MAIN", "canceled");
            resetValues();
            mapManager.removeMarkers();

        }
    };

    private View.OnClickListener save = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("MAIN", "save");


        }
    };

    private void resetValues(){
        mStartedRecording = false;
        startButton.setText(R.string.start);
        otherButtons.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(runnable);
        startTime = SystemClock.uptimeMillis();
        timeBuff = 0;
        updateTime = 0;

        elapsedTime.setText("00:00");
        distanceView.setText("0.00 Km");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        broadcaster = LocalBroadcastManager.getInstance(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        // Acquire a reference to the system Location Manager
        //FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        //LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mapManager = new MapManager(mapFragment, broadcaster);

        broadcaster.registerReceiver(locationUpdate,
                new IntentFilter("position_update"));

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        otherButtons =  (LinearLayout) findViewById(R.id.otherButtons);
        saveButton   =  (Button) findViewById(R.id.saveButton);
        cancelButton =  (Button) findViewById(R.id.cancelButton);
        elapsedTime  =  (TextView) findViewById(R.id.elapsedTime);
        distanceView =  (TextView) findViewById(R.id.distance);
        startButton  =  (Button) findViewById(R.id.startButton);

        handler = new Handler();

        startButton.setOnClickListener(startEvent);
        cancelButton.setOnClickListener(cancel);
        saveButton.setOnClickListener(save);

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(LOCATION_PERMS, 1);
        }
        if(!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)){
            requestPermissions(LOCATION_PERMS, 1);
        }

    }

    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
    }

    private BroadcastReceiver locationUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            distance = intent.getFloatExtra("distance", 0);
            distanceView.setText(String.format("%.2f", distance));
            Log.d("MAIN.locUpd", "distance: " + distance);
        }
    };



    public Runnable runnable = new Runnable() {

        public void run() {

            millisecondTime = SystemClock.uptimeMillis() - startTime;
            updateTime = timeBuff + millisecondTime;
            elapsedTime.setText(getReadableTime(updateTime));
            handler.postDelayed(this, 0);
        }

    };

    private String getReadableTime(Long nanos){

        long tempSec = nanos/(1000);
        long sec = tempSec % 60;
        long min = (tempSec /60) % 60;
        long hour = (tempSec /(60*60)) % 24;
        long day = (tempSec / (24*60*60)) % 24;

        String s = String.format("%d:%d", min,sec);

        if (hour > 0){
            s = hour +"h "+s;
        }else if (day > 0){
            s = day +"d "+s;
        }
        return s;

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(locationUpdate,
                new IntentFilter(LocationService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdate);
        super.onPause();
    }

    @Override
    protected void onStop() {

        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;

        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        //mServiceConnection.
        Intent inte = new Intent(this, LocationService.class);
        stopService(inte);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdate);
        super.onDestroy();

    }
}
