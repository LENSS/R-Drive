package edu.tamu.cse.lenss.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import org.apache.log4j.Logger;


public class MDFSService extends Service {
    Logger logger = Logger.getLogger(this.getClass());

    public static final String loggerLocation = Environment.getExternalStorageDirectory().toString() + "/distressnet/mdfs.log";

    public final String CHANNEL_ID = "edu.tamu.cse.lenss.android.CHANNEL";

    private Context appContext;
    private MDFSHandler mdfsHandler;

    NotificationManagerCompat notificationManager;
    static int notificationID;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void onCreate(){
        appContext = getApplication().getApplicationContext();

        mdfsHandler = new MDFSHandler();

        logger.info("Service is created");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        notificationManager = NotificationManagerCompat.from(appContext);

        logger.info("Starting services");


        mdfsHandler.start();
        createNotification("Service started");


        logger.info("MDFS service started");

        return START_NOT_STICKY;

    }

    private void createNotification(String notificationText){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                //.setSmallIcon(R.drawable.notification_icon)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    // notificationId is a unique int for each notification that you must define
        notificationManager.notify(++notificationID, builder.build());
    }


    @Override
    public void onDestroy() {
        logger.info("Service is shutting down");

        mdfsHandler.terminate();

        createNotification("Service terminated");


        // Now restart the service
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("edu.tamu.cse.lenss.android.restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);


    }


}
