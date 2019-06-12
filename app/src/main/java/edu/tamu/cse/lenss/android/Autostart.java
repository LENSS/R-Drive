package edu.tamu.cse.lenss.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;

import edu.tamu.cse.lenss.gnsService.server.GNSServiceUtils;

/**
 * This class will get notification during boottime start the service.
 *
 * The code is taken from  https://stackoverflow.com/questions/7690350/android-start-service-on-boot
 * Modified by: sbhunia
 */
public class Autostart extends BroadcastReceiver
{
    Logger logger = Logger.getLogger(this.getClass());


    public void onReceive(Context context, Intent arg1)
    {

        try { // Initialize the logger
            GNSServiceUtils.initLogger(MDFSService.loggerLocation, Level.ALL);
        } catch (IOException e) {
            System.out.println("Problem with creating logger");
        }

        logger.debug("Autostart got notification of boot");
        Intent intent = new Intent(context,MDFSService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        logger.info("Started the Service from autostart");
    }
}
