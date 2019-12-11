package edu.tamu.lenss.MDFS.PeerFetcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.tamu.lenss.MDFS.MissingLInk.MissingLink;

import static android.content.Context.MODE_PRIVATE;

public class Utils {

    //logger
    public static Logger logger = org.apache.log4j.Logger.getLogger(Utils.class);

    //get all keys of a shared pref
    //returns null.
    public static Set<String> SharedPreferences_keys(String prefName){
        try {
            SharedPreferences pref = MissingLink.context.getSharedPreferences(prefName, MODE_PRIVATE);
            Map<String, ?> allEntries = pref.getAll();
            Set<String> keys = allEntries.keySet();
            return keys;
        }catch (Exception e ){
            e.printStackTrace();
        }

        return null;
    }

    //put data in a shared pref
    public static void SharedPreferences_put(String prefName, String key, String value){

        try {
            //init pref
            SharedPreferences SP = MissingLink.context.getSharedPreferences(prefName, MODE_PRIVATE);

            //get editor
            SharedPreferences.Editor editor = SP.edit();
            editor.putString(key, value);
            editor.commit();
        }catch(Exception e ){
            e.printStackTrace();
        }
    }

    //get value from a shared pref using a key
    public static String SharedPreferences_get(String prefName, String key){

        String defValue = "DEFAULT VALUE";
        try {
            //init pref
            SharedPreferences SP = MissingLink.context.getSharedPreferences(prefName, MODE_PRIVATE);

            SharedPreferences.Editor editor = SP.edit();
            String data = SP.getString(key, defValue);

            if(data.equals(defValue)){
                logger.log(Level.DEBUG, "Failed to fetch value from android shared preference, returning default value.");
            }
            return data;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

}




