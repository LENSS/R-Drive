package edu.tamu.cse.lenss.android;


import android.content.Intent;
import android.os.Environment;
import android.app.Service;
import android.os.IBinder;

//this is the mdfs service that runs of the phones background.
public class MDFSService extends Service {

    MDFSHandler mdfsHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        mdfsHandler = new MDFSHandler();
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
