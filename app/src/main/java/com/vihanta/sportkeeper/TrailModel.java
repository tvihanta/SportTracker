package com.vihanta.sportkeeper;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.vihanta.sportkeeper.Utils;

public class TrailModel extends ViewModel{

    private static final String TAG = TrailModel.class.getSimpleName();
    public MutableLiveData<List<Location>> getTrail() {
        setTrail();
        return trail;
    }

    private Utils utils = new Utils();

    public MutableLiveData<List<Location>> trail;

    //call this to update liveData var
    public void setTrail(){
        trail.setValue(points);
    }


    private List<Location> points = null;
    public List<Location> getPoints() {
        return points;
    }
    public void setPoints(List<Location> pPoints) {
        points = pPoints;
    }
    public void addPoint(Location loc){
        Log.d(TAG, "adding point to model");
        if(points == null){
            points = new ArrayList<>();
        }
        points.add(loc);
        setTrail();
    }


    public float getSpeed() {return speed;}
    public void setSpeed(float pSpeed) {
        try {
            //Location loc = getLatest();
            speed =  distance / (float) (time / 1000f / 60f / 60f);
            //speed = pSpeed;
        }
        catch (Exception e){
            speed = 0;
        }
    }
    private float speed = 0f;

    public float getDistance() {
        return distance;
    }
    public String getAudioDistance() {
        return String.format("%.3f", distance);
    }
    public void setDistance() {
        this.distance = calculateDistance();
    }
    private float distance = 0f;

    public float getPreviousLapDistance() { return previousLapDistance; }
    public void setPreviousLapDistance() {
        this.previousLapDistance = getDistance();
    }
    private float previousLapDistance = 0f;

    public Location getPreviousLapEndLocation() {
        return previousLapEndLocation;
    }
    public void setPreviousLapEndLocation(Location previousLapEndLocation) {
        this.previousLapEndLocation = previousLapEndLocation;
    }
    private Location previousLapEndLocation = null;


    public double getTime() {
        return time;
    }
    public void setTime(double time) {
        this.time = time;
    }
    public String getHumanReadableTime() {
        return humanReadableTime;
    }
    public void setHumanReadableTime(long time) {

        this.humanReadableTime = utils.getReadableTime(time);
    }
    private String humanReadableTime = "";
    private double time = 0;
    public String getAudioTime() {
        if(audioTime == null) {
            setAudioTime();
        }
        return audioTime;
    }
    public void setAudioTime() {
        try{
            this.audioTime = utils.parseTimeForAudio(getHumanReadableTime());
        }catch (Exception e){
            Log.e(TAG, e.getMessage() );
            this.audioTime = "";
        }
    }
    private String audioTime =null;

    private ArrayList<HashMap> mLapData;

    public JSONArray getTypes() {
        return types;
    }
    public void setTypes(JSONArray types) {
        this.types = types;
    }
    private JSONArray types;

    public List<String> getTypeNames() {
        if(typeNames == null) {
            typeNames = new ArrayList<String>();
        }
        return typeNames;
    }

    private int getTypeIdbyName(String value){

        int id = 1;
        for(int n = 0; n < types.length(); n++)
        {
            try {
                JSONObject object = types.getJSONObject(n);
                if(object.get("type") == value){
                    id = object.getInt("id");
                    break;
                }

            }catch (Exception e){
                Log.e(TAG, e.getMessage() );
            }

        }
        return id;
    }

    public void setTypeNames(List<String> typeNames) {
        this.typeNames = typeNames;
    }
    private List<String> typeNames = new ArrayList<String>();


    public void init() {
        if (this.trail == null) {
            trail = new MutableLiveData<List<Location>>();
            trail.setValue(new ArrayList<Location>());
        }
        mLapData = new ArrayList<>();
    }

    public Location getLatest(){

        if (points != null && !points.isEmpty()) {
            return points.get(points.size() - 1);
        }
        return null;
    }

    /**
     * calculates distance between the last point in array and the one before it.
     * @return
     */
    private float calculateDistance(){
        float len = 0;
        if(points != null && points.size() > 2) {
            len = distance;
            len += points.get(points.size()-1).distanceTo(points.get(points.size()-2)) / 1000;
            len = (float) Math.round(len * 1000) / 1000;

            return len;
        }
        return 0;
    }

    public void reset(){
        points = new ArrayList<Location>();
        //trail = new MutableLiveData<List<Location>>();
        trail.setValue(new ArrayList<Location>());
        speed = 0;
        distance = 0;
        mLapData = new ArrayList<>();
        previousLapEndLocation = null;
        previousLapDistance = 0;
        audioTime = "";
        humanReadableTime = "";
        time=0;
        setTrail();
    }

    public List<LatLng> getLatLongList(){

        List<LatLng> lst = new ArrayList<>();
        try {
            for(Location loc : points){
                LatLng p = new LatLng(loc.getLatitude(), loc.getLongitude());
                lst.add(p);
            }
        } catch (IndexOutOfBoundsException ie){
            Log.e(TAG, ie.getMessage() );
        }
        return lst;
    }

    public float calculateDistanceFromList(){
        float dist = 0;
        try {
            for(int i = 0; i < points.size();i++){
                dist += points.get(i).distanceTo(points.get(i-1));
            }
        } catch (Exception ie){
            Log.e(TAG, ie.getMessage() );
        }
        return dist;
    }

    /**
     * calculate the speed between current point and previousLapEndLocation using location times and
     * distance from current and previousLapDistance
     *
     * saves lap data to mLapData; {distance,  elapsed time, speed}
     * @return
     */
    public float calculateLapSpeed(){
        float lapSpeed = 0;
        float timedelta;
        //try {

            if (previousLapEndLocation != null) {
                timedelta = (getLatest().getElapsedRealtimeNanos() -
                        previousLapEndLocation.getElapsedRealtimeNanos());
            } else {
                timedelta = getLatest().getElapsedRealtimeNanos() -
                        points.get(0).getElapsedRealtimeNanos();
            }
            timedelta = timedelta / 1_000_000_000f;// to seconds
            float timedelta2 = (float)(timedelta / 60f / 60f); // to hours

            float lapDistance =  getDistance() - getPreviousLapDistance();
            lapSpeed =  ((float) lapDistance / (float) timedelta2);

            long temp = (long)(timedelta * 1000);

            HashMap<String, String> lap = new HashMap<>();
            lap.put("distance", String.format("%.3f", lapDistance));
            lap.put("elapsedTime", utils.getReadableTime(temp));
            lap.put("speed", String.format("%.2f", lapSpeed));
            mLapData.add(lap);

 //       } catch (Exception e){
           // Log.e(TAG, e.getMessage() );
            /*HashMap<String, String> lap = new HashMap<>();
            lap.put("distance", "0");
            lap.put("elapsedTime", "0");
            lap.put("speed", "0");
            mLapData.add(lap);*/
        //}

        return lapSpeed;
    }

    private JSONArray getEventGeo(){
        JSONArray jso = new JSONArray();
        try {

            for (Location loc : points){
                JSONObject feature = new JSONObject();

                feature.put("type", "Feature");

                JSONArray coordinates = new JSONArray();
                coordinates.put(loc.getLongitude());
                coordinates.put(loc.getLatitude());

                JSONObject geometry = new JSONObject();
                geometry.put("type", "Point");
                geometry.put("coordinates",coordinates);

                //String.format("[%f, %f]", loc.getLongitude(), loc.getLatitude()));
               // geometry.put("longitude", String.format("%f", loc.getLongitude()));

                JSONObject properties = new JSONObject();
                properties.put("time", formatLocationTime(loc.getTime()));
                properties.put("speed", String.format("%.2f",loc.getSpeed()));

                feature.put("geometry", geometry);
                feature.put("properties", properties);
                jso.put(feature);
        }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return jso;
    }

    private String formatLocationTime(long time){

        Date d = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY/MM/dd hh:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String s = sdf.format(d);
        return s;
    }

    public String toJson(String pTitle, String typeName){

        JSONObject event = new JSONObject();

        try {
            event.put("title", pTitle);
            event.put("type_id", getTypeIdbyName(typeName));
            event.put("distance", getDistance());
            event.put("time", getHumanReadableTime());
            event.put("speed", getSpeed());
            event.put("GeoJSON", getEventGeo());
            JSONArray laps = new JSONArray();
            for(int i = 0; i < mLapData.size();i++){
                JSONObject singleLap = new JSONObject();
                HashMap s = mLapData.get(i);
                singleLap.put("distance", s.get("distance"));
                singleLap.put("elapsedTime", s.get("elapsedTime"));
                singleLap.put("speed", s.get("speed"));
                laps.put(singleLap);
            }
            event.put("laps", laps);
        } catch (JSONException jse){
            Log.e(TAG, jse.getMessage());
        }
        return event.toString();
    }

}
