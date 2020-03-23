package com.vihanta.sportkeeper;


import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public String getReadableTime(Long nanos){

        long tempSec = nanos/(1000);
        long sec = tempSec % 60;
        long min = (tempSec /60) % 60;
        long hour = (tempSec /(60*60)) % 24;
        long day = (tempSec / (24*60*60)) % 24;

        String s = String.format("%02d:%02d", min,sec);

        if (hour > 0){
            s = hour +"h "+s;
        }else if (day > 0){
            s = day +"d "+s;
        }
        return s;
    }

    /**
     * @param pTime human readable time from getReadableTime
     * @return string that contains units of time added as words used by TTS
     */
    public String parseTimeForAudio(String pTime){
        String res ="";
        String[] ss = pTime.split(":");
        if(ss.length == 3){
            res += Integer.parseInt(ss[0]) + "hours "
                    +Integer.parseInt(ss[1])+" minutes "
                    +Integer.parseInt(ss[2])+" seconds ";
        } else {
            res += Integer.parseInt(ss[0])+" minutes "
                    +Integer.parseInt(ss[1])+" seconds ";
        }
        return res;
    }

    public static String getConfigValue(Context context, String name) {
        Resources resources = context.getResources();

        try {
            InputStream rawResource = resources.openRawResource(R.raw.config);
            Properties properties = new Properties();
            properties.load(rawResource);
            return properties.getProperty(name);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Unable to find the config file: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Failed to open config file.");
        }

        return null;
    }

    //




}
