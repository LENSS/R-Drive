package edu.tamu.cse.lenss.android;


import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.app.Service;
import android.os.IBinder;

public class MDFSService extends Service {

    public static final String loggerLocation = Environment.getExternalStorageDirectory().toString() + "/distressnet/mdfs.log";
    private Context appContext;
    MDFSHandler mdfsHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        appContext = getApplication().getApplicationContext();
        mdfsHandler = new MDFSHandler(appContext);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Starting service");
        mdfsHandler.run();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        System.out.println("Stopping service");

        mdfsHandler.interrupt();
        super.onDestroy();

        //restart the service
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("edu.tamu.cse.lenss.android.restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
        */
    }
}
