package edu.tamu.cse.lenss.android;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Arrays;

import edu.tamu.cse.lenss.CLI.RequestHandler;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;

import static java.lang.Thread.sleep;

//import edu.tamu.cse.lenss.utils



//this is the main actiivty and where the execution of this app starts.
public class MainActivity extends AppCompatActivity {


    //global variables
    TextView textView;
    ListView listView;
    Button backButton;
    Button refreshButton;
    ArrayAdapter arrayAdapter;
    private static String currentView = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        this.backButton = (Button) findViewById(R.id.backButton);
        this.refreshButton = (Button) findViewById(R.id.refreshButton);
        this.textView = (TextView) findViewById(R.id.textview);
        this.listView = (ListView)  findViewById(R.id.listview);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {

                //get item
                String item =  (String)((TextView) view).getText();

                //check its a directory or file
                if(item.charAt(0)=='/'){
                    //item is a directory
                    //tokenize current directory
                    String[] tokens = IOUtilities.delEmptyStr(currentView.split("/"));

                    //make new directory
                    String newDir = currentView +item.subSequence(1, item.length()) + "/";

                    //setview
                    setView(newDir);

                }
            }

        });

        registerForContextMenu(listView);

        //check permission for this app
        checkPermissions();
        setView("/");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        menu.setHeaderTitle("Select The Action");
    }


    @Override
    public boolean onContextItemSelected(MenuItem item){

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String value = ((TextView) info.targetView).getText().toString();

        //check if value is a directory or file
        if(value.charAt(0)=='/') {

            //value is a directory
            if (item.getItemId() == R.id.open) {

                //just fetch directory ans set view
                setView(currentView + value.substring(1, value.length()) + "/");

            } else if (item.getItemId() == R.id.delete) {

                //delete the directory and refresh
                String ret = Foo("mdfs -rm " + currentView + value.substring(1, value.length()) + "/");

                //check reply
                if(ret!=null){
                    Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();
                    try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
                    setView(currentView);
                }else{
                    Toast.makeText(this, "Could not delete directory, " + ret , Toast.LENGTH_SHORT).show();
                }
            }
        }else{
            //value is a file
            if(item.getItemId() == R.id.open){

                //make mdfs get request
                String ret = Foo("mdfs -get " + currentView + value + " /storage/emulated/0/decrypted/");
                Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();

            }else if(item.getItemId() == R.id.delete){

                //delete the file and refresh
                String ret = Foo("mdfs -rm " + currentView + value);

                //check reply
                if(ret!=null){
                    Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();
                    try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
                    setView(currentView);
                }else{
                    Toast.makeText(this, "Could not delete file " + value + ", " + ret , Toast.LENGTH_SHORT).show();
                }
            }

        }
        return true;
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

    //=========================================================================

    //takes mdfs command and execute it
    public String Foo(String command) {
        String reply = RequestHandler.processRequestCpp(Constants.NON_CLI_CLIENT, command);

        if(reply!=null){
            return reply;
        }else{
            return "Returned null!";
        }
    }


    //takes a directory string, fetch directory and sets view.
    public void setView(String directory){

        //first set current directory
        currentView = directory;

        //first get reply for ls command for / directory
        String reply = Foo("mdfs -ls " + directory);

        //check if reply is correct
        if(reply!=null){

            //toast
            //sToast.makeText(this, "Fetched root directory.", Toast.LENGTH_SHORT).show();

            //set current directory
            textView.setText("Directory: " + currentView);

            //tokenize the elements and delete empty strings
            String[] tokens = IOUtilities.delEmptyStr(reply.split("   "));

            //create arrayList and populate
            ArrayList arrayList = new ArrayList(Arrays.asList(tokens));

            //initialize and set array adapter
            this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
            listView.setAdapter(arrayAdapter);



        }else{
            Toast.makeText(this, "Could not fetch root directory.", Toast.LENGTH_SHORT).show();
            textView.setText(currentView);
        }
    }



    //back button pressed
    public void backButtonClicked(View view) {
        if(currentView.equals("/")){
            Toast.makeText(this, "Cannot go back beyond / directory", Toast.LENGTH_SHORT).show();
        }else{
            //first tokenize current directory
            String[] tokens = IOUtilities.delEmptyStr(currentView.split("/"));

            //then take only tokens.length -1 numbers of tokens
            String newDir = "/";
            for(int i=0; i< tokens.length-1; i++){newDir = newDir + tokens[i] + "/";}

            //fetch directory from edgeKeeper
            String reply = Foo("mdfs -ls " + newDir);

            //check
            if(reply!=null){

                //tokenize the elements and delete empty strings
                tokens = IOUtilities.delEmptyStr(reply.split("   "));

                //create arrayList and populate
                ArrayList arrayList = new ArrayList(Arrays.asList(tokens));

                //initialize and set array adapter
                this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
                listView.setAdapter(arrayAdapter);

                //set current direcotory to newDir
                currentView = newDir;
                textView.setText("Directory: " + currentView);

            }else{
                Toast.makeText(this, "Could not fetch directory", Toast.LENGTH_SHORT).show();
            }

        }
    }

    //refresh button pressed
    public void refreshButtonClicked(View view){
        setView(currentView);
    }





}
