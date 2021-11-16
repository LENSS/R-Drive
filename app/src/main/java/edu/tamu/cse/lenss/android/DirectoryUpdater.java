package edu.tamu.cse.lenss.android;

import android.app.Activity;
import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.tamu.lenss.MDFS.Commands.get.getUtils;

//This class updates the file icons for current edge directory
public class DirectoryUpdater {

    Context context;
    Activity activity;

    //This thread populates shared preference (key: filepath_fileID, value: blockCount_N_K values)
    Thread SPupdater = new Thread(new Runnable() {
        @Override
        public void run() {

            //first let the app start at its own pace
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while(true) {
                //refresh MainActivity
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        MainActivity.setViewForOwnEdge(MainActivity.ownEdgeCurrentDir);
                    }
                });

                //regular sleep
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });


    public DirectoryUpdater(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
        SPupdater.start();
    }
}
