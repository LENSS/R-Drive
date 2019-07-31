package edu.tamu.cse.lenss.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;

import edu.tamu.cse.lenss.CLI.cli_processor;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;

public class MDFSHandler extends Thread {

    public final String CHANNEL_ID = "edu.tamu.cse.lenss.android.CHANNEL";

    public cli_processor cli;
    Context appContext;

    public MDFSHandler(Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public void run(){
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
        ServiceHelper.getInstance(appContext);
        try {
            //Set encryption key
            byte[] encryptKey = new byte[32];
            int[] keyValues = {121, 108, 85, 100, -17, 52, 31, 65, -106, 82, 116, -94, -71, 50, -80, -90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int index = 0;
            for(int i: keyValues){encryptKey[index] = (byte)i; index++;}
            ServiceHelper.getInstance().setEncryptKey(encryptKey);

        } catch (NullPointerException /*|| IOException*/ e) {
            e.printStackTrace();
        }
        LocalBroadcastManager.getInstance(appContext).registerReceiver(mMessageReceiver, new IntentFilter("current_ip"));  //sagor

    }


    private void startCLI() {
        cli = new cli_processor(appContext);
        cli.start();
    }




    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //todo
            //myIP = intent.getStringExtra("message");
            //displayIP.setText(myIP);
        }
    };



}
