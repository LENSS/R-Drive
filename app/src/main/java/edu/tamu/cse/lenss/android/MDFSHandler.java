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
        if(AndroidIOUtils.isConnectedToWifi(appContext)) {
            //init serviceHelper
            ServiceHelper.getInstance(appContext);  //sagor
            try {
                //Set encryption key
                byte[] encryptKey = new byte[32];
                appContext.getAssets().open("keystore", AssetManager.ACCESS_BUFFER).read(encryptKey, 0, 32);
                ServiceHelper.getInstance().setEncryptKey(encryptKey);
            } catch (IOException e) {
                e.printStackTrace();
            }
            LocalBroadcastManager.getInstance(appContext).registerReceiver(mMessageReceiver, new IntentFilter("current_ip"));  //sagor

        }
        System.out.println(" onetime ServiceHelper is created");

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
