package edu.tamu.cse.lenss.android;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.app.Service;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import edu.tamu.cse.lenss.Notifications.NotificationUtils;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;
import edu.tamu.lenss.mdfs.Utils.Pair;

import static java.lang.Thread.sleep;

//import org.apache.log4j.Logger;

//this is the mdfs service that runs of the phones background.
public class MDFSService extends Service {

    MDFSHandler mdfsHandler;
    private Intent intent;

    //A thread that does all the miscellaneous works.
    //such as : notification.
    public Thread miscellaneous;
    public boolean miscThdEnabled = true;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        mdfsHandler = new MDFSHandler(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //start miscellaneous thread
        miscellaneous = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (miscThdEnabled) {
                        Pair p = IOUtilities.miscellaneousWorks.poll(10, TimeUnit.MILLISECONDS);
                        if(p!=null) {
                            if (p.getString_1().equals(Constants.NOTIFICATION)) {
                                set_alarm(0, p.getString_2());
                            }
                            sleep(0);
                        }
                    }
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        miscellaneous.start();

        this.intent = intent;

        System.out.println("Starting service");
        mdfsHandler.run();

        //notification
        showNotificationAndStartService("Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        System.out.println("Stopping service");
        if(miscellaneous !=null){miscThdEnabled = false;}
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
    public void showNotificationAndStartService(String message) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        long[] pattern = {0, 500, 0 };
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message)
                .setContentText(message)
                .setContentText(input)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setVibrate(pattern)
                .build();

        try {
            startForeground(1, notification);
        } catch (RuntimeException e){
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


    public void set_alarm(long timeUntilNotification, String filename){

        Calendar clndr = Calendar.getInstance();
        clndr.add(Calendar.SECOND, 1);  //redundant line

        //create intent
        Intent intentA = new Intent("sagor.mohammad.action.DISPLAY_NOTIFICATION");

        //put extra
        intentA.putExtra("code",Integer.toString(NotificationUtils.code));
        intentA.putExtra("filename", filename);

        PendingIntent pendingIntentX = PendingIntent.getBroadcast(getApplicationContext(), NotificationUtils.req_code, intentA, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManagerX = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManagerX.cancel(pendingIntentX);
        alarmManagerX.set(AlarmManager.RTC,clndr.getTimeInMillis()+timeUntilNotification, pendingIntentX);
        NotificationUtils.code++;
        NotificationUtils.req_code++;

        //set the dummy intent
        intentA.setAction(Long.toString(System.currentTimeMillis()));

    }




}
