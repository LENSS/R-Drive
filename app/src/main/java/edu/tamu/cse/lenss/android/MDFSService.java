package edu.tamu.cse.lenss.android;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.app.Service;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

//import org.apache.log4j.Logger;

//this is the mdfs service that runs of the phones background.
public class MDFSService extends Service {
    //Logger logger = Logger.getLogger(this.getClass());

    MDFSHandler mdfsHandler;
    private Intent intent;

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
        this.intent = intent;



        System.out.println("Starting service");
        mdfsHandler.run();

        //notification
        showNotification("Started");
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



    public static final String CHANNEL_ID = MDFSService.class.getName();

    private void showNotification(String message) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message)
                .setContentText(message)
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();

        try {
            startForeground(1, notification);
        } catch (RuntimeException e){
            //logger.fatal("Foreground permission not granted",e);
            Toast.makeText(this, "Foreground permission not granted", Toast.LENGTH_SHORT).show();
            this.onDestroy();
        }

    }
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


}
