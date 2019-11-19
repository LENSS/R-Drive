package edu.tamu.cse.lenss.Notifications;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import edu.tamu.cse.lenss.android.R;


//
//This class is used to actually QueueToSend out a notification.
//when alarm gets triggered, it calls out this class to QueueToSend out a notification.
//
public class NotificationDo extends BroadcastReceiver {


   @Override
   public void onReceive(Context context, Intent intent) {





       String filename = intent.getStringExtra("filename");
        int code = Integer.parseInt(intent.getStringExtra("code"));



       //notification
       Intent notificationIntent = new Intent(context, NotificationActivity.class);
       TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
       stackBuilder.addParentStack(NotificationActivity.class);
       stackBuilder.addNextIntent(notificationIntent);

       PendingIntent pendingIntent = stackBuilder.getPendingIntent(code, PendingIntent.FLAG_UPDATE_CURRENT);

       NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

       long[] pattern = {0, 500, 0 };
       Notification notification = builder
               .setContentText("File " + filename + " has been retrieved.")
               .setContentTitle("File Retrieved!")
               .setTicker("New Message Alert!")
               .setAutoCancel(true)
               .setSmallIcon(R.drawable.bg_default)
               .setVibrate(pattern)
               .setWhen(System.currentTimeMillis()).build();

       NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
       notificationManager.notify(code, notification);

   }
}





























