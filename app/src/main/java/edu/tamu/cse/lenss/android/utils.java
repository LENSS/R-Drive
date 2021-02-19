package edu.tamu.cse.lenss.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class utils {

    public static void vibrator(int millisec, Context context){
        Vibrator v = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(millisec, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(millisec);
        }
    }

    //get all keys of a shared pref
    public static Set<String> SharedPreferences_keys(String prefName){
        try {
            SharedPreferences pref = MainActivity.context.getSharedPreferences(prefName, MODE_PRIVATE);
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
            SharedPreferences SP = MainActivity.context.getSharedPreferences(prefName, MODE_PRIVATE);

            //get editor
            SharedPreferences.Editor editor = SP.edit();
            editor.putString(key, value);
            editor.commit();
        }catch(Exception e ){
            e.printStackTrace();
        }
    }

    //get data from a shared pref
    public static String SharedPreferences_get(String prefName, String key){

        try {
            //init pref
            SharedPreferences SP = MainActivity.context.getSharedPreferences(prefName, MODE_PRIVATE);

            SharedPreferences.Editor editor = SP.edit();
            String data = SP.getString(key, "");
            return data;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static String getKey(Map<String, String> map, String value) {
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    //Convert a File to a byte array
    public static byte[] fileToByte(File file){
        try {
            RandomAccessFile randF = new RandomAccessFile(file, "r");
            byte[] b = new byte[(int)randF.length()];
            randF.read(b);
            randF.close();
            return b;

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return null;
    }


    //Convert byte array to a file
    public static File byteToFile(byte[] data, String filepath, String filename){


        //get the directory
        File f1 = new File(filepath);

        //check if directory exists or create
        if(!f1.exists()){
            if(!f1.mkdirs()){
                return null;
            }
        }

        //get the file
        File f2 = new File(filepath + filename);

        //write bytes in f2
        if(f2!=null) {
            try {
                FileOutputStream fos = new FileOutputStream(f2);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return f2;
    }

    public static boolean isNumeric(String str) {

        // null or empty
        if (str == null || str.length() == 0) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;

    }



}
