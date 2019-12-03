package edu.tamu.cse.lenss.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import edu.tamu.cse.lenss.CLI.CLIRequestHandler;
import edu.tamu.lenss.MDFS.Commands.ls.ls;
import edu.tamu.lenss.MDFS.Commands.ls.lsUtils;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;

import static java.lang.Thread.sleep;

//import edu.tamu.cse.lenss.utils



//this is the main actiivty and where the execution of this app starts.
public class MainActivity extends AppCompatActivity {



    //global variables
    public static Activity activity;
    public static Context context;
    TextView textView;
    ListView listView;
    Button backButton;
    Button refreshButton;
    Button mkdirButton;
    Button putButton;
    Button toggleView;
    ArrayAdapter arrayAdapter;

    //current browsing directory for own edge
    private static String ownEdgeCurrentDir = "";

    //current browsing direcotory for neighbor edge
    private static String neighborEdgeCurrentDir = "";
    private static String currentBrowsingNeighbor = "";

    //view mode whether its ownEdgeDir or neighborEdgeDir view.
    private static final String OWNEDGEDIR = "OWNEDGEDIR";
    private static final String NEIGHBOREDGEDIR = "NEIGHBOREDGEDIR";
    public static final String SELECTEDGEMASTER = "Select an edge directory";
    private static String allNeighborEdgeDirsCache = "";
    private static String currentMode = OWNEDGEDIR; //start with own edge dir


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        context = this;

        this.backButton = (Button) findViewById(R.id.backButton);
        this.refreshButton = (Button) findViewById(R.id.refreshButton);
        this.mkdirButton = (Button) findViewById(R.id.mkdirButton);
        this.putButton = (Button) findViewById(R.id.putButton);
        this.toggleView = (Button) findViewById(R.id.toggleView);
        this.textView = (TextView) findViewById(R.id.textview);
        this.listView = (ListView)  findViewById(R.id.listview);
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {

                //get item
                String item =  (String)((TextView) view).getText();

                //check if currentView is OWNEDGEDIR or NEIGHBOREDGEDIR
                if(currentMode.equals(OWNEDGEDIR)) {

                    //check its a directory or file
                    if (item.charAt(0) == '/') {

                        //item is a directory
                        //tokenize current directory
                        String[] tokens = IOUtilities.delEmptyStr(ownEdgeCurrentDir.split("/"));

                        //make new directory
                        String newDir = ownEdgeCurrentDir + item.subSequence(1, item.length()) + "/";

                        //setview
                        setViewForOwnEdge(newDir);

                    }
                }else if(currentMode.equals(NEIGHBOREDGEDIR)){

                    //check if the neighborEdgeCurrentDir = SELECTEDGEMASTER.
                    //that means user toggled into the neighborEdgeDir and
                    //about to choose an edge master.
                    if(neighborEdgeCurrentDir.equals(SELECTEDGEMASTER)){

                        //the item is a guid of an edge master
                        //assign currentBrowsingNeighbor into this master guid
                        currentBrowsingNeighbor = item;

                        //show / directory for this particular master
                        //fetch dir object for this particular master
                        JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromFromAllNeighborEdgeDirStr(currentBrowsingNeighbor, allNeighborEdgeDirsCache);

                        try {
                            System.out.println(particularMasterDirsObject.toString(4));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //fetch directory information for this neighbor

                    }else{

                        //the item contains
                    }

                }
            }

        });

        registerForContextMenu(listView);

        //check permission for this app
        checkPermissions();
        setViewForOwnEdge("/");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        menu.setHeaderTitle("Select Action");
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
                setViewForOwnEdge(ownEdgeCurrentDir + value.substring(1, value.length()) + "/");

            } else if (item.getItemId() == R.id.delete) {

                //delete the directory and refresh
                String ret = Foo("mdfs -rm " + ownEdgeCurrentDir + value.substring(1, value.length()) + "/");

                //check reply
                if(ret!=null){
                    Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();
                    try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
                    setViewForOwnEdge(ownEdgeCurrentDir);
                }else{
                    Toast.makeText(this, "Could not delete directory, " + ret , Toast.LENGTH_SHORT).show();
                }
            }
        }else{
            //value is a file
            if(item.getItemId() == R.id.open){

                //make mdfs get request
                String ret = Foo("mdfs -get " + ownEdgeCurrentDir + value + " /storage/emulated/0/decrypted/");
                Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();

            }else if(item.getItemId() == R.id.delete){

                //delete the file and refresh
                String ret = Foo("mdfs -rm " + ownEdgeCurrentDir + value);

                //check reply
                if(ret!=null){
                    Toast.makeText(this, ret, Toast.LENGTH_SHORT).show();
                    try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
                    setViewForOwnEdge(ownEdgeCurrentDir);
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

        // First stop the already running service
        this.stopService(new Intent(this, MDFSService.class));


        Intent intent = new Intent(this, MDFSService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            startForegroundService(intent);
        } else {
            startService(intent);
        }
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
    public static String Foo(String command) {

        String reply = CLIRequestHandler.processRequestCpp(Constants.NON_CLI_CLIENT, command);

        if(reply!=null){
            return reply;
        }else{
            return "Returned null!";
        }
    }


    //takes a list of items and sets them for view
    public void setItemsOnListView(List<String> list){

        //initialize and set array adapter
        this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(arrayAdapter);

    }

    //takes a list of items and show in the

    //takes a directory string, fetch directory and sets view.
    public void setViewForOwnEdge(String directory){

        System.out.println("inside where we are");

        //first set current directory
        ownEdgeCurrentDir = directory;

        //first get reply for ls command for / directory
        String reply = ls.ls(directory, "lsRequestForOwnEdge");

        //check if reply is correct
        if(reply!=null){

            //set textview
            textView.setText(OWNEDGEDIR +": " + ownEdgeCurrentDir);

            //create List and populate with ls info
            List<String> arrayList = lsUtils.jsonToList(reply);

            //initialize and set array adapter
            this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
            listView.setAdapter(arrayAdapter);


        }else{
            Toast.makeText(this, "Could not fetch root directory.", Toast.LENGTH_SHORT).show();
        }
    }

    //currentMode view clicked
    public void toggleViewClicked(View view){

        try {

            //check currentMode
            if (currentMode.equals(OWNEDGEDIR)) {

                //do ls to fetch entire neighbor information
                String allNeighborLSstr = ls.ls("/", "lsRequestForAllDirectoryiesOfAllNeighborEdges");

                //check reply
                if (allNeighborLSstr != null) {


                    //save it into local cache
                    allNeighborEdgeDirsCache = allNeighborLSstr;

                    //get list of all masters
                    List<String> neighborMasters = lsUtils.getListOfMastersFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);

                    //check if list is null or empty
                    if(neighborMasters!=null && neighborMasters.size()!=0){

                        //set currentView
                        currentMode = NEIGHBOREDGEDIR;

                        //set neighbor current directory
                        this.neighborEdgeCurrentDir = SELECTEDGEMASTER;

                        //set all neighbor masters for view and select
                        setItemsOnListView(neighborMasters);

                        //set textview
                        textView.setText(SELECTEDGEMASTER);

                        //disable mkdir, put, back buttons
                        mkdirButton.setEnabled(false);
                        putButton.setEnabled(false);

                        //change currentMode
                        currentMode = NEIGHBOREDGEDIR;


                    }else if(neighborMasters.size()==0){
                        Toast.makeText(this, "No neighbor directory available.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(this, "Could not fetch neighbor edge direcotry from EdgeKeeper.", Toast.LENGTH_SHORT).show();
                }
            }else if(currentMode.equals(NEIGHBOREDGEDIR)){

                //enable mkdir, put, back buttons
                mkdirButton.setEnabled(true);
                putButton.setEnabled(true);

                //setView for own directory
                setViewForOwnEdge(ownEdgeCurrentDir);

                //change currentMode
                currentMode = OWNEDGEDIR;

            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Exception happened while fetching or setting neighbor edge directory. ", Toast.LENGTH_SHORT).show();
        }
    }


    //back button pressed
    public void backButtonClicked(View view) {
        if(ownEdgeCurrentDir.equals("/")){
            Toast.makeText(this, "Cannot go back beyond / directory", Toast.LENGTH_SHORT).show();
        }else{
            //first tokenize current directory
            String[] tokens = IOUtilities.delEmptyStr(ownEdgeCurrentDir.split("/"));

            //then take only tokens.length -1 numbers of tokens
            String newDir = "/";
            for(int i=0; i< tokens.length-1; i++){newDir = newDir + tokens[i] + "/";}

            //fetch directory from edgeKeeper
            String reply = Foo("mdfs -ls " + newDir);

            //check
            if(reply!=null){

                //create arrayList and populate
                List<String> arrayList = lsUtils.jsonToList(reply);

                //initialize and set array adapter
                this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
                listView.setAdapter(arrayAdapter);

                //set current directory to newDir
                ownEdgeCurrentDir = newDir;
                textView.setText(OWNEDGEDIR + ": " + ownEdgeCurrentDir);

            }else{
                Toast.makeText(this, "Could not fetch directory", Toast.LENGTH_SHORT).show();
            }

        }
    }

    //refresh button pressed
    public void refreshButtonClicked(View view){
        setViewForOwnEdge(ownEdgeCurrentDir);
    }

    //mkdir button pressed
    public void mkdirButtonClicked(View view){

        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.mkdir_view,null);

        // Specify alert dialog is not cancelable/not ignorable
        builder.setCancelable(true);

        // Set the custom layout as alert dialog view
        builder.setView(dialogView);

        // Get the custom alert dialog view widgets reference
        Button cancelDialogButton = (Button) dialogView.findViewById(R.id.cancelButton);
        Button OKBUtton = (Button) dialogView.findViewById(R.id.OKButton);
        EditText inputFolder = (EditText) dialogView.findViewById(R.id.folderNames);

        // Create the alert dialog
        final AlertDialog dialog = builder.create();

        // Set Cancel button click listener
        cancelDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Dismiss the alert dialog
                dialog.cancel();

            }
        });

        // Set OK button click listener
        OKBUtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get value
                String value = inputFolder.getText().toString();

                //check for null and empty
                if(value!=null && value.length()!=0){

                    //remove beginning slash if present
                    if(value.charAt(0)=='/'){
                        value = value.substring(1, value.length());
                    }

                    //execute command
                    String ret = Foo("mdfs -mkdir " + ownEdgeCurrentDir +value);

                    //check ret
                    if(ret!=null){
                        Toast.makeText(getApplicationContext(), ret, Toast.LENGTH_SHORT).show();
                        setViewForOwnEdge(ownEdgeCurrentDir);
                    }else{
                        Toast.makeText(getApplicationContext(), "Could not get reply from EdgeKeeper.", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "Provided folder is empty.", Toast.LENGTH_SHORT).show();
                }

                //dialog.cancel();
                dialog.dismiss();
            }
        });

        // Display the custom alert dialog on interface
        dialog.show();
    }

    //put button clicked
    private static final int READ_REQUEST_CODE = 42;
    public void putButtonClicked(View view){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                String path = uri.getPath();

                // Now check if this filepath is from internal memory or not
                if (path.toLowerCase().startsWith("/document/primary:") )
                {
                    path = path.replaceFirst("/document/primary:", Environment.getExternalStorageDirectory().toString()+ "/");
                    showDialogue(path);

                }else{
                    Toast.makeText(this, "Choose a file from sdcard.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //input event for passing MDFS directory value
    private void showDialogue(String localfilepath) {

        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.put_view,null);

        // Specify alert dialog is not cancelable/not ignorable
        builder.setCancelable(true);

        // Set the custom layout as alert dialog view
        builder.setView(dialogView);

        // Get the custom alert dialog view widgets reference
        Button cancelDialogButton = (Button) dialogView.findViewById(R.id.cancelButton);
        Button OKBUtton = (Button) dialogView.findViewById(R.id.OKButton);
        EditText inputFolder = (EditText) dialogView.findViewById(R.id.folderNames);

        // Create the alert dialog
        final AlertDialog dialog = builder.create();

        // Set Cancel button click listener
        cancelDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Dismiss the alert dialog
                dialog.cancel();

            }
        });

        // Set OK button click listener
        OKBUtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get value
                String value = inputFolder.getText().toString();

                //check for null or empty
                if(value!=null){

                    //remove beginning slash if present
                    if (!value.equals("") && value.charAt(0) == '/') {
                        value = value.substring(1, value.length());
                    }

                    //execute command
                    String ret = Foo("mdfs -put " + localfilepath + " " + ownEdgeCurrentDir + value);

                    //check
                    if (ret != null) {
                        Toast.makeText(getApplicationContext(), ret, Toast.LENGTH_SHORT).show();
                        setViewForOwnEdge(ownEdgeCurrentDir);
                    } else {
                        Toast.makeText(getApplicationContext(), "Could not get reply from EdgeKeeper.", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(getApplicationContext(), "Provided mdfs directory is empty.", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        // Display the custom alert dialog on interface
        dialog.show();
    }


}
