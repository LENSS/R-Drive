package edu.tamu.cse.lenss.android;


import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSBlockCreator.MDFSBlockCreatorListener;
import edu.tamu.lenss.mdfs.MDFSFileCreator;
import edu.tamu.lenss.mdfs.R;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
//import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.cse.lenss.android.BuildConfig;
import edu.tamu.cse.lenss.utils.ThumbnailGenerator;

//rsock imports
import edu.tamu.lenss.mdfs.MDFSFileCreatorViaRsock;
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock;

import static java.lang.Thread.sleep;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;


public class MDFSService extends Service {

    public static final String loggerLocation = Environment.getExternalStorageDirectory().toString() + "/distressnet/mdfs.log";

    public final String CHANNEL_ID = "edu.tamu.cse.lenss.android.CHANNEL";

    private Context appContext;


    NotificationManagerCompat notificationManager;
    static int notificationID;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void onCreate(){

        appContext = getApplication().getApplicationContext();
        if(AndroidIOUtils.isConnectedToWifi(appContext)) {
            //init serviceHelper
            ServiceHelper.getInstance(appContext);  //sagor
            try {
                //Set encryption key
                byte[] encryptKey = new byte[32];
                getAssets().open("keystore", AssetManager.ACCESS_BUFFER).read(encryptKey, 0, 32);
                ServiceHelper.getInstance().setEncryptKey(encryptKey);
            } catch (IOException e) {
                e.printStackTrace();
            }
            LocalBroadcastManager.getInstance(appContext).registerReceiver(mMessageReceiver, new IntentFilter("current_ip"));  //sagor

        }
        System.out.println("ServiceHelper is created");

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //todo
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        notificationManager = NotificationManagerCompat.from(appContext);

        //createNotification("Service started");


        return START_NOT_STICKY;

    }

    private void createNotification(String notificationText){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                //.setSmallIcon(R.drawable.notification_icon)
                //.setSmallIcon(R.drawable.ic_launcher_background)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    // notificationId is a unique int for each notification that you must define
        notificationManager.notify(++notificationID, builder.build());
    }


    @Override
    public void onDestroy() {


        //createNotification("Service terminated");


        // Now restart the service
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("edu.tamu.cse.lenss.android.restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);


    }


}
