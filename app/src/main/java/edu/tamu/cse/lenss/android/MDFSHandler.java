package edu.tamu.cse.lenss.android;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import edu.tamu.cse.lenss.CLI.cli_processor;
import edu.tamu.cse.lenss.Notifications.NotificationUtils;
import edu.tamu.cse.lenss.mdfs_api_server.MDFS_API_SERVER;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;

import static android.content.Context.ALARM_SERVICE;


//this is the mdfs handler that starts the command line interface(cli) and the MDFS library.

public class MDFSHandler extends Thread {

    public cli_processor cli;
    public MDFS_API_SERVER mdfs_api_server;
    public Context context;
    public MDFSHandler(Context c) {this.context = c;}

    @Override
    public void run(){

        //note: MDFS must start before CL
        startMDFS();
        startCLI();
        this.mdfs_api_server = new MDFS_API_SERVER();
        this.mdfs_api_server.start();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        ServiceHelper.releaseService();
        cli.interrupt();
        this.mdfs_api_server.interrupt();
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

    public void  set_alarm(long timeUntilNotification, String filename){

        Calendar clndr = Calendar.getInstance();
        clndr.add(Calendar.SECOND, 1);  //redundant line

        //create intent
        Intent intentA = new Intent("sagor.mohammad.action.DISPLAY_NOTIFICATION");

        //put extra
        intentA.putExtra("code",Integer.toString(NotificationUtils.code));
        intentA.putExtra("filename", filename);

        PendingIntent pendingIntentX = PendingIntent.getBroadcast(context, NotificationUtils.req_code, intentA, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManagerX = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManagerX.cancel(pendingIntentX);
        alarmManagerX.set(AlarmManager.RTC,clndr.getTimeInMillis()+timeUntilNotification, pendingIntentX);
        NotificationUtils.code++;
        NotificationUtils.req_code++;

        //set the dummy intent
        intentA.setAction(Long.toString(System.currentTimeMillis()));

    }

    //simple tuple class in java
    static class Pair{
        private boolean result;
        private String message;

        public Pair(){ }


        public boolean isResult() {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }



}
