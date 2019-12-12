package edu.tamu.cse.lenss.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class Utils {

    //logger
    public static Logger logger = Logger.getLogger(Utils.class);

    //get all keys of a shared pref
    //returns null.
    public static Set<String> SharedPreferences_keys(String prefName, Context context){
        try {
            SharedPreferences pref = context.getSharedPreferences(prefName, MODE_PRIVATE);
            Map<String, ?> allEntries = pref.getAll();
            Set<String> keys = allEntries.keySet();
            return keys;
        }catch (Exception e ){
            e.printStackTrace();
        }

        return null;
    }

    //put data in a shared pref
    public static void SharedPreferences_put(String prefName, String key, String value, Context context){

        try {
            //init pref
            SharedPreferences SP = context.getSharedPreferences(prefName, MODE_PRIVATE);

            //get editor
            SharedPreferences.Editor editor = SP.edit();
            editor.putString(key, value);
            editor.commit();
        }catch(Exception e ){
            e.printStackTrace();
        }
    }

    //get value from a shared pref using a key
    public static String SharedPreferences_get(String prefName, String key, Context context){

        String defValue = "DEFAULT VALUE";
        try {
            //init pref
            SharedPreferences SP = context.getSharedPreferences(prefName, MODE_PRIVATE);

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




