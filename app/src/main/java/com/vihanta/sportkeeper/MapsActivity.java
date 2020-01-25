package com.vihanta.sportkeeper;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.SupportMapFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


public class MapsActivity extends android.support.v7.app.AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private LocalBroadcastManager broadcaster;
    private MapManager mapManager;

    private Boolean mStarted = false;


    private TextView elapsedTime;
    private TextView distanceView;
    private TextView speedView;
    private Button startButton;
    private Button saveButton;
    private Button cancelButton;
    private LinearLayout otherButtons;

    private String mHumanReadableTime;

    private long millisecondTime, startTime, timeBuff, updateTime = 0L ;
    Handler handler;
    int seconds, minutes, milliSeconds ;

    private LocationService mService = null;
    private boolean mBound = false;

    private static final String PACKAGE_NAME = R.string.app_ident+".mapsactivity";
    static final String START_ACTION = PACKAGE_NAME + ".start";
    static final String PAUSE_ACTION = PACKAGE_NAME + ".pause";
    static final String CANCEL_ACTION = PACKAGE_NAME + ".cancel";
    static final String SAVED_ACTION = PACKAGE_NAME + ".saved";
    static final String SETTING_CHANGE = PACKAGE_NAME + ".setting_change";


    RequestQueue queue;

    @Inject TrailModel mModel;
    @Inject Utils utils;

    private TrailViewModel mViewModel;

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

            if (!mStarted) {
                Log.d(TAG, "Started");
                mStarted = true;
                startButton.setText(R.string.pause);
                otherButtons.setVisibility(View.INVISIBLE);

                startTime = SystemClock.uptimeMillis();
                handler.postDelayed(runnable, 0);

                Intent intent = new Intent(START_ACTION);
                broadcaster.sendBroadcast(intent);

            } else {
                Log.d(TAG, "Paused");
                mStarted = false;
                startButton.setText(R.string.start);
                otherButtons.setVisibility(View.VISIBLE);

                timeBuff += millisecondTime;
                handler.removeCallbacks(runnable);

                Intent intent = new Intent(PAUSE_ACTION);
                broadcaster.sendBroadcast(intent);
            }
        }
    };

    private View.OnClickListener cancel = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d(TAG, "canceled");
            Intent intent = new Intent(CANCEL_ACTION);
            broadcaster.sendBroadcast(intent);
            resetView();
        }
    };



    private View.OnClickListener save = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d(TAG, "save");

            FragmentManager fragmentManager = getSupportFragmentManager();
            ResultDialogFragment newFragment = new ResultDialogFragment();

            newFragment.show(fragmentManager, "dialog");

            /*
            // The device is smaller, so show the fragment fullscreen
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            // For a little polish, specify a transition animation
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            // To make it fullscreen, use the 'content' root view as the container
            // for the fragment, which is always the root view for the activity
            transaction.add(android.R.id.content, newFragment).addToBackStack(null).commit();
            */

        }
    };

    private void resetView(){
        mStarted = false;
        startButton.setText(R.string.start);
        otherButtons.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(runnable);
        startTime = SystemClock.uptimeMillis();
        timeBuff = 0;
        updateTime = 0;

        elapsedTime.setText("00:00");
        distanceView.setText("0.00");
        mModel.reset();
    }
    private void registerListeners(){
        broadcaster = LocalBroadcastManager.getInstance(this);
        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE));
        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(MapsActivity.START_ACTION));
        broadcaster.registerReceiver(mMessageReceiver,
                new IntentFilter(MapsActivity.SAVED_ACTION));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((SKapp)getApplication()).getmMainComponent().inject(this);

        setContentView(R.layout.activity_maps);
        broadcaster = LocalBroadcastManager.getInstance(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);


        registerListeners();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        otherButtons =  (LinearLayout) findViewById(R.id.otherButtons);
        saveButton   =  (Button) findViewById(R.id.saveButton);
        cancelButton =  (Button) findViewById(R.id.cancelButton);
        elapsedTime  =  (TextView) findViewById(R.id.elapsedTime);
        distanceView =  (TextView) findViewById(R.id.distance);
        startButton  =  (Button) findViewById(R.id.startButton);
        speedView    =  (TextView) findViewById(R.id.speed);

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

        mViewModel = ViewModelProviders.of(this).get(TrailViewModel.class);

        mapManager = new MapManager(this, mapFragment, broadcaster, mModel);

        mModel.getTrail().observe(this, new Observer<List<Location>>() {
            @Override
            public void onChanged(@Nullable List<Location> locations) {
                Log.d(TAG, "Model Change");
                setModelValuesToUI();
            }
        });

        queue = Volley.newRequestQueue(this.getBaseContext());

        getTypesFromApi();

    }

    private void setModelValuesToUI (){

        distanceView.setText(mModel.getAudioDistance());
        speedView.setText(String.format("%.2f", mModel.getSpeed()));

    }

    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()){
                case LocationService.LOCATION_UPDATE:
                    Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                    break;
                case MapsActivity.START_ACTION:
                    mStarted = true;
                    TrailModel mm = mModel;
                    break;
                case MapsActivity.PAUSE_ACTION:
                    mStarted = false;
                    break;
                case MapsActivity.SAVED_ACTION:
                    resetView();
                    break;

            }
        }
    };

    public Runnable runnable = new Runnable() {

        public void run() {

            millisecondTime = SystemClock.uptimeMillis() - startTime;
            updateTime = timeBuff + millisecondTime;

            elapsedTime.setText(mHumanReadableTime);
            mModel.setTime(updateTime);
            mModel.setHumanReadableTime(updateTime);

            mHumanReadableTime = mModel.getHumanReadableTime();

            handler.postDelayed(this, 0);
        }

    };



    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(
                new Intent(this, LocationService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LocationService.LOCATION_UPDATE));
        registerListeners();
        mapManager.drawPolylineFromList();
        setModelValuesToUI();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        mModel.reset();
        super.onDestroy();

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(mStarted) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Do you want to close?")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do finish
                                MapsActivity.this.finish();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do nothing
                                return;
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }

        return super.onKeyDown(keyCode, event);
    }


    /**
     * menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("display_text")) {
            Log.d(TAG, "onSharedPreferenceChanged: settings change");
        } /* else if (key.equals("color")) {
            loadColorFromPreference(sharedPreferences);
        } else if (key.equals("size"))  {
            loadSizeFromPreference(sharedPreferences);
        }*/
    }

    private void getTypesFromApi(){

        mModel.setTypes(new JSONArray());
        mModel.setTypeNames(new ArrayList<String>());

        String url = "http://ippe.kapsi.fi/sportkeeper/types";
        StringRequest typeReq = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d(TAG,"Request OK");
                        try {
                            mModel.setTypes(new JSONArray(response));
                            JSONArray arr = mModel.getTypes();

                            for(int n = 0; n < arr.length(); n++)
                            {
                                JSONObject object = arr.getJSONObject(n);
                                mModel.getTypeNames().add(object.get("type").toString());

                            }

                           /* for(int i = 0; i<types.names().length(); i++){
                                Log.v(TAG, "key = " + types.names().getString(i) + " value = " + types.get(types.names().getString(i)));
                                typeNames.add(types.get(types.names().getString(i)).toString());
                            }*/


                        }catch (Exception e){
                            Log.e(TAG, e.getMessage() );
                        }

                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.e(TAG, "errori");
                    }
                }
        ) {
            /*@Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("json", s);
                //params.put("geo", mModel.);

                return params;
            }*/
        };
        queue.add(typeReq);

    }


}
