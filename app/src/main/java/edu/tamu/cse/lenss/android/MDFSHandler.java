package edu.tamu.cse.lenss.android;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import edu.tamu.cse.lenss.CLI.cli_processor;
import edu.tamu.lenss.mdfs.Handler.ServiceHelper;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;

import static edu.tamu.cse.lenss.android.MDFSService.CHANNEL_ID;


//this is the mdfs handler that starts the command line interface(cli) and the MDFS library.

public class MDFSHandler extends Thread {

    public cli_processor cli;
    public Context context;

    public MDFSHandler(Context c) {this.context = c;}

    @Override
    public void run(){
        //note: MDFS must start before CL

        startMDFS();
        startCLI();
    }


    @Override
    public void interrupt() {



        super.interrupt();
        ServiceHelper.releaseService();
        cli.interrupt();
    }

    public void startMDFS(){

        //init serviceHelper
        ServiceHelper.getInstance();
        try {
            //Set encryption key
            byte[] encryptKey = new byte[32];
            int[] keyValues = {121, 108, 85, 100, -17, 52, 31, 65, -106, 82, 116, -94, -71, 50, -80, -90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int index = 0;
            for(int i: keyValues){encryptKey[index] = (byte)i; index++;}
            ServiceHelper.getInstance().setEncryptKey(encryptKey);

        } catch (NullPointerException  e) {
            e.printStackTrace();
        }

    }


    private void startCLI() {
        cli = new cli_processor();
        cli.start();
    }


    
}
