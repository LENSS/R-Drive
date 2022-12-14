package edu.tamu.cse.lenss.android;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;

import java.util.Calendar;

import edu.tamu.cse.lenss.Notifications.NotificationUtils;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import edu.tamu.lenss.MDFS.Utils.Pair;

import static java.lang.Thread.sleep;

//import org.apache.log4j.Logger;

//this is the mdfs service that runs of the phones background.
public class RDRIVEService extends Service {

    RDRIVEHandler rdrivehandler;
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

        rdrivehandler = new RDRIVEHandler(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //start miscellaneous thread
        //used primarily for notifying UI stuff.
        //no harm if this thread is disabled,
        //only the UI will not be updated for error events.
        miscellaneous = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (miscThdEnabled) {
                        //Pair p = IOUtilities.miscellaneousWorks.poll(10, TimeUnit.MILLISECONDS);
                        Pair p = IOUtilities.miscellaneousWorks.take();
                        if(p!=null) {

                            if (p.getString_1().equals(Constants.FILE_RETRIEVE_NOTIFICATION)) {

                                //its a notification, show it
                                set_alarm(0, p.getString_2());

                            }else if(p.getString_1().equals(Constants.EDGEKEEPER_CLOSED)){

                                //EK closed, freeze UI
                                MainActivity.freezeUI(p.getString_2());

                            }else if(p.getString_1().equals(Constants.RSOCK_CLOSED)){

                                //rsock api closed, freeze UI
                                MainActivity.freezeUI(p.getString_2());
                            }

                            //dummy sleep
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
        rdrivehandler.run();

        //notification
        showNotificationAndStartService("Started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {

        System.out.println("Stopping service");
        if(miscellaneous !=null){miscThdEnabled = false;}
        rdrivehandler.interrupt();
        super.onDestroy();

        //restart the service
        /*Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("edu.tamu.cse.lenss.android.restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
        */
    }



    public static final String CHANNEL_ID = RDRIVEService.class.getName();
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
            Toast.makeText(this, "Foreground permission not granted", Toast.LENGTH_LONG).show();
            this.onDestroy();
        }


    }


    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
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
