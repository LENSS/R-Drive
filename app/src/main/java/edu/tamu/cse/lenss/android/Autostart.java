package edu.tamu.cse.lenss.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import java.io.IOException;


/**
 * This class will get notification during boottime start the service.
 *
 * The code is taken from  https://stackoverflow.com/questions/7690350/android-start-service-on-boot
 * Modified by: sbhunia
 */
public class Autostart extends BroadcastReceiver
{


    public void onReceive(Context context, Intent arg1)
    {

/*        try { // Initialize the logger
            GNSServiceUtils.initLogger(MDFSService.loggerLocation, Level.ALL);
        } catch (IOException e) {
            System.out.println("Problem with creating logger");
        }*/

        Intent intent = new Intent(context,MDFSService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

    }
}
