package edu.tamu.cse.lenss.android;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import edu.tamu.cse.lenss.CLI.CLIConstants;
import edu.tamu.cse.lenss.CLI.RequestHandler;
import edu.tamu.cse.lenss.Notifications.NotificationUtils;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;
import edu.tamu.lenss.mdfs.Utils.Pair;

import static java.lang.Thread.sleep;

//import edu.tamu.cse.lenss.utils


//this is the main actiivty and where the execution of this app starts.
public class MainActivity extends AppCompatActivity {


    //global variables
    private EditText ET;
    private TextView TV;
    private Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //initialize editText and Button
        this.ET = (EditText) findViewById(R.id.editText);
        this.TV= (TextView) findViewById(R.id.view_reply);
        this.send = (Button) findViewById(R.id.buttonSend);

        //check permission for this app
        checkPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    protected void onDestroy() {

        // Stop the service
        this.stopService(new Intent(this, MDFSService.class));


        /*
        //this block of code restarts the service once again after the app is closed
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("edu.tamu.cse.lenss.android.restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);*/

        super.onDestroy();


    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //if device permission is OK, this function is called from checkPermissions() function
    public void initializeApp(){
        runService();
    }


    //this function starts a service
    void runService(){
        //this.stopService(new Intent(this, MDFSService.class));

        // First stop the already running service
        this.stopService(new Intent(this, MDFSService.class));


        Intent intent = new Intent(this, MDFSService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, 0);
            startForegroundService(intent);
        } else {
            startService(intent);
        }


        //this.startService(intent);
    }




    //Before executing any code, the app should check whether the permissions are granted or not.
    //If the permission is not granted then ask the user to grant the required permission.
    //Check the detail code here: https://developer.android.com/training/permissions/requesting#java
    private static final int REQEUST_PERMISSION_GNSSERVICE = 22;
    private void checkPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQEUST_PERMISSION_GNSSERVICE);
            }
        }
        else
            initializeApp();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQEUST_PERMISSION_GNSSERVICE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeApp();
                } else {
                    Toast.makeText(this, "You must allow permission to continue.", Toast.LENGTH_SHORT).show();
                    checkPermissions();
                }
        }
    }

    //this function is triggered when input is taken via typing it on the screen, instead of CLI.
    public void inputTakenClick(View view){

        //get MDFS command String
        String command = ET.getText().toString();
        ET.setText("");

        //tokenize the command
        String[] tokens = IOUtilities.removeAllSpacesAndTokenize(command, " ");

        //print
        System.out.println(Arrays.toString(tokens));

        //check if the command contains the "-d" flag
        //if it contains -d flag, then we parse the IP.
        int indexOfDFlag = -1;
        int indexOfIPFlag = -1;
        for(int i=0; i< tokens.length; i++){

            //check if any token is -d flag
            if(tokens[i].equals("-d")){

                //check if next token is a valid IP
                if(i<tokens.length-1 && IOUtilities.isValidInet4Address(tokens[i+1])){

                    System.out.println("delete ip is valid");

                    indexOfDFlag = i;
                    indexOfIPFlag = i+1;
                    break;

                }else{

                    //set textview and must return
                    TV.setText("Command failed! Provide a valid local IP address \n (Or, dont use -d flag at all).");
                    return;
                }
            }
        }

        if(indexOfDFlag!=-1 && indexOfIPFlag!=-1) {

            System.out.println("delete index: " + indexOfDFlag + " " + indexOfIPFlag);

            //check if the IP is not mine
            System.out.println("delete own ips: "+IOUtilities.getOwnIPv4s());

            if (tokens[indexOfIPFlag].equals("localhost") || IOUtilities.getOwnIPv4s().contains(tokens[indexOfIPFlag])) {

                //recreate command without -d flag and IP field
                String comm = "";
                for (int i = 0; i < tokens.length; i++) {
                    if (i != indexOfDFlag && i != indexOfIPFlag) {
                        comm = comm + tokens[i] + " ";
                    }
                }

                System.out.println("comm: " + comm);

                //set new command
                command = comm;
                System.out.println("delete new command is: " + command);

            } else {

                //set textview and must return
                TV.setText("Command failed! Provided IP is not one of available local IPs \n (You can choose to not use -d flag at all).");
                return;
            }
        }

        //check for illegal commands
        if(command.contains("copyToLocal") || command.contains("copyFromLocal")){
            TV.setText("Command failed! copyToLocal or copyFromLocal commands are only allowed on MDFS Command Line Interface.");
            return;
        }

        //send
        Foo(command);
    }


    public void Foo(String command) {
        String reply = RequestHandler.processRequestCpp(Constants.NON_CLI_CLIENT, command);
        if(!reply.equals("OK")) {
            TV.setText(reply);

        }
    }




}
