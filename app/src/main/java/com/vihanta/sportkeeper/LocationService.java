package com.vihanta.sportkeeper;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Locale;

import javax.inject.Inject;


    public class LocationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String PACKAGE_NAME =
                "com.vihanta.sportkeeper.locationservice";

        private static final String TAG = LocationService.class.getSimpleName();

        private static final String CHANNEL_ID = "channel_01";

        static final String LOCATION_UPDATE = PACKAGE_NAME + ".broadcast";

        static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
        private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
                ".started_from_notification";

        private final IBinder mBinder = new LocalBinder();

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 6000;

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value.
         */
        private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
                UPDATE_INTERVAL_IN_MILLISECONDS / 2;

        /**
         * The identifier for the notification displayed for the foreground service.
         */
        private static final int NOTIFICATION_ID = 12345678;

        /**
         * Used to check whether the bound activity has really gone away and not unbound as part of an
         * orientation change. We create a foreground service notification only if the former takes
         * place.
         */
        private boolean mChangingConfiguration = false;
        private NotificationManager mNotificationManager;

        /**
         * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
         */
        private LocationRequest mLocationRequest;

        /**
         * Provides access to the Fused Location Provider API.
         */
        private FusedLocationProviderClient mFusedLocationClient;

        /**
         * Callback for changes in location.
         */
        private LocationCallback mLocationCallback;
        private Handler mServiceHandler;

        /**
         * The current location.
         */
        private Location mPreviousLocation;

        private LocalBroadcastManager broadcastManager;
        private Boolean mStartedRecording = false;

        @Inject TrailModel mModel;
        @Inject Utils utils;

        private TextToSpeech textToSpeech;
        private float mAudioThresholdDistance = 0.5f;
        private int mNumberOfLaps = 1;
        private boolean mAudioFeedback = true;

        private static float ACCURACY_THRESHOLD = 20f;

        public LocationService() {
        }

        @Override
        public void onCreate() {

            ((SKapp)getApplication()).getmMainComponent().inject(this);

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    onNewLocation(locationResult.getLastLocation());
                }
            };

            createLocationRequest();
            getLastLocation();

            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            mServiceHandler = new Handler(handlerThread.getLooper());
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // Android O requires a Notification Channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.app_name);
                // Create the channel for the notification
                NotificationChannel mChannel =
                        new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW);
                mChannel.setSound(null, null);

                // Set the Notification Channel for the Notification Manager.
                mNotificationManager.createNotificationChannel(mChannel);
            }


            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        int ttsLang = textToSpeech.setLanguage(Locale.US);

                        if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                                || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "The Language is not supported!");
                        } else {
                            Log.i("TTS", "Language Supported.");
                        }
                        Log.i("TTS", "Initialization success.");
                    } else {
                        //Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "error initializing TTS");
                    }
                }
            });


            broadcastManager = LocalBroadcastManager.getInstance(this);
            broadcastManager.registerReceiver(receiver,
                    new IntentFilter(MapsActivity.START_ACTION));
            broadcastManager.registerReceiver(receiver,
                    new IntentFilter(MapsActivity.PAUSE_ACTION));
            broadcastManager.registerReceiver(receiver,
                    new IntentFilter(MapsActivity.CANCEL_ACTION));



            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            mAudioThresholdDistance =
                    formatIntervalPrefrence(sharedPreferences.getString("feedback_interval",
                                                                        "500"));
            mAudioFeedback =
                    sharedPreferences.getBoolean("audio_feedback", true);
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

    /**
     * receive updates from modules
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()){
                case MapsActivity.START_ACTION:
                    mStartedRecording = true;
                    break;
                case MapsActivity.PAUSE_ACTION:
                    mStartedRecording = false;
                    break;
                case MapsActivity.CANCEL_ACTION:
                    mStartedRecording = false;
                    break;
            }
        }
    };

    private float formatIntervalPrefrence(String s){
        return Float.parseFloat(s) / 1000;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        // We got here because the user decided to remove location updates from the notification.
        /*if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }*/
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");
        Log.i(TAG, "Starting foreground service");
        startForeground(NOTIFICATION_ID, getNotification());
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mNotificationManager.cancel(NOTIFICATION_ID);
        mServiceHandler.removeCallbacksAndMessages(null);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        //Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            //Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            //Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            //Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        CharSequence text = "notify";//Utils.getLocationText(mPreviousLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                intent, 0);
                //new Intent(this, MapsActivity.class), 0);

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle(getText(R.string.app_name))
                        .setContentText(getText(R.string.app_name))
                        .setSmallIcon(R.drawable.common_google_signin_btn_text_light)
                        .setContentIntent(activityPendingIntent)
                        .setTicker(getText(R.string.cancel))
                        .setChannelId(CHANNEL_ID)
                        .build();

        // Set the Channel ID for Android O.
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }*/

        return notification;
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mPreviousLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);


        if(mStartedRecording && location.getAccuracy() < ACCURACY_THRESHOLD) {

            mModel.addPoint(location);
            mModel.setDistance();
            mModel.setSpeed(location.getSpeed());
            Log.i(TAG, String.format("LOCATION_UPDATE %s, %s, %s",
                    mModel.getHumanReadableTime(), location.getAccuracy(), mModel.getDistance()));
            Log.i(TAG, String.format(" %s", mModel.getAudioTime()));

            // update liveData variable to generate updates to ui
            mModel.setTrail();

            float dist = mModel.getDistance();
            if ( dist > mAudioThresholdDistance * mNumberOfLaps) {
                mNumberOfLaps++;

                float speedInLap = mModel.calculateLapSpeed();

                if(mAudioFeedback) {
                    mModel.setAudioTime();
                    doAudio(mModel.getAudioTime(), mModel.getAudioDistance(), mModel.getSpeed(), speedInLap);
                }
                mModel.setPreviousLapEndLocation(location);
                mModel.setPreviousLapDistance();
            }

        }
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, String.format("onSharedPreferenceChanged: %s", key));
        setPrefrencesToValues(sharedPreferences);

    }

    private void setPrefrencesToValues(SharedPreferences sharedPreferences){
        mAudioThresholdDistance = formatIntervalPrefrence(
                sharedPreferences.getString("feedback_interval", "500"));
        mAudioFeedback = sharedPreferences.getBoolean("audio_feedback", true);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void doAudio(String formattedTime, String distance, float speed, float avgSpeed){
        try {

            String data = formatAudioFeedback(formattedTime, distance, speed, avgSpeed);
            int speechStatus = textToSpeech.speak(data, TextToSpeech.QUEUE_FLUSH, null, null);

            if (speechStatus == TextToSpeech.ERROR) {
                Log.e("TTS", "Error in converting Text to Speech!");
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage() );
        }

    }

    private String formatAudioFeedback (String formattedTime, String distance, float speed, float avgSpeed){

        String res  = "";
        res += distance +" kilometers ";
        res += formattedTime;
        if(avgSpeed > 0){
            res += "speed: " + String.format("%.2f", avgSpeed);
        }

        //res += " Speed "+ speed;

        return res;
    }


}


