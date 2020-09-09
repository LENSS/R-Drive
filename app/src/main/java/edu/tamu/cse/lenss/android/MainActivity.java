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
import android.util.TypedValue;
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

import java.io.File;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS.Executor;
import edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS.UIRequestHandler;
import edu.tamu.lenss.MDFS.Commands.get.get;
import edu.tamu.lenss.MDFS.Commands.ls.ls;
import edu.tamu.lenss.MDFS.Commands.ls.lsUtils;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.MissingLInk.MissingLink;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;

//import edu.tamu.cse.lenss.utils



//this is the main actiivty and where the execution of this app starts.
public class MainActivity extends AppCompatActivity {



    //global variables
    public static Activity activity;
    public static Context context;
    static TextView textView;
    static ListView listView;
    static Button backButton;
    static Button refreshButton;
    static Button mkdirButton;
    static Button putButton;
    static Button toggleView;
    public static ArrayAdapter arrayAdapter;

    //current browsing directory for own edge
    private static String ownEdgeCurrentDir = "";

    //current browsing direcotory for neighbor edge
    private static String neighborEdgeCurrentDir = "";
    private static String currentBrowsingNeighborGUID = "";

    //view mode whether its ownEdgeDir or neighborEdgeDir view.
    private static final String OWNEDGEDIR = "OWN EDGE DIR";
    private static final String NEIGHBOREDGEDIR = "NEIGHBOR EDGE DIR";
    public static final String SELECTEDGEMASTER = "Select an edge directory";
    private static String allNeighborEdgeDirsCache = "";
    private static String currentMode = OWNEDGEDIR; //start with own edge dir


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;
        context = this;
        MissingLink.context = this;

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

                    //resultant list to show on listView
                    List<String> tokens = new ArrayList<>();

                    //check if the neighborEdgeCurrentDir = SELECTEDGEMASTER.
                    //that means user toggled into the neighborEdgeDir and
                    //about to choose an edge master.
                    //note: evertytime if user toggel view from
                    // ownEdgeDir to neighborEdgeDir,
                    // we fetch entire neighborEDgeDir from edgekeeper and cache it.
                    if(neighborEdgeCurrentDir.equals(SELECTEDGEMASTER)){

                        //show / directory for this particular master
                        //fetch dir object for this particular master
                        //item = masterName
                        //first convert name into guid
                        String guid = lsUtils.nameToGUID(item, allNeighborEdgeDirsCache);
                        if(guid!=null) {
                            JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(guid, allNeighborEdgeDirsCache);

                            //check null
                            if (particularMasterDirsObject != null) {

                                try {
                                    //get the directory string for root
                                    //note, at this point we need root dir to view.
                                    String dirStr = particularMasterDirsObject.getString("/");

                                    //check null
                                    if (dirStr != null) {

                                        //convert dirStr string into dirObj
                                        JSONObject dirObj = new JSONObject(dirStr);

                                        //get the FOLDERS string
                                        //note: there must be a FOLDERS object,
                                        //even there might be no folders.
                                        JSONObject FOLDERS = new JSONObject(dirObj.getString("FOLDERS"));

                                        //get folders count in FOLDERS object
                                        int count = Integer.parseInt(FOLDERS.getString("COUNT"));

                                        //get each folder names and populate lists
                                        for (int i = 0; i < count; i++) {

                                            String folderName = "/" + FOLDERS.getString(Integer.toString(i));
                                            tokens.add(folderName);
                                        }

                                        //get the FILES string
                                        //note: there must be a FILES object,
                                        //even there might be no folders.
                                        JSONObject FILES = new JSONObject(dirObj.getString("FILES"));

                                        //get files count in FILES object
                                        count = Integer.parseInt(FILES.getString("COUNT"));

                                        //get each file names and populate lists
                                        for (int i = 0; i < count; i++) {
                                            String fileName = FILES.getString(Integer.toString(i));
                                            tokens.add(fileName);
                                        }

                                        //the item is a guid of an edge master
                                        //assign currentBrowsingNeighborGUID into this master guid
                                        currentBrowsingNeighborGUID = guid;

                                        //change neighborEdgeCurrentDir = root
                                        neighborEdgeCurrentDir = "/";

                                        //change textView
                                        textView.setText(item + ": " + neighborEdgeCurrentDir);

                                        //change listView
                                        setItemsOnListView(tokens);


                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                Toast.makeText(MainActivity.context, "Could not convert name to GUID, try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }


                    }else{

                        //check its a directory or file,
                        //inside currentBrowsingNeighborGUID's directory
                        if (item.charAt(0) == '/') {

                            //the item is a dir string for any particular master.
                            //note: we dont fetch neighbor directory from edgekeeper now,
                            //we use from cache.
                            //fetch dir object for this particular master
                            JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache);

                            //check null
                            if (particularMasterDirsObject != null) {

                                try {

                                    //get the directory string for root
                                    //note, at this point we need root dir to view.
                                    item = neighborEdgeCurrentDir + item.substring(1) + File.separator;
                                    String dirStr = particularMasterDirsObject.getString(item);

                                    //check null
                                    if(dirStr!=null){

                                        //convert dirStr string into dirObj
                                        JSONObject dirObj = new JSONObject(dirStr);

                                        //get the FOLDERS string
                                        //note: there must be a FOLDERS object,
                                        //even there might be no folders.
                                        JSONObject FOLDERS = new JSONObject(dirObj.getString("FOLDERS"));

                                        //get folders count in FOLDERS object
                                        int count = Integer.parseInt(FOLDERS.getString("COUNT"));

                                        //get each folder names and populate lists
                                        for(int i=0; i< count; i++){

                                            String folderName = "/" + FOLDERS.getString(Integer.toString(i));
                                            tokens.add(folderName);
                                        }

                                        //get the FILES string
                                        //note: there must be a FILES object,
                                        //even there might be no folders.
                                        JSONObject FILES = new JSONObject(dirObj.getString("FILES"));

                                        //get files count in FILES object
                                        count = Integer.parseInt(FILES.getString("COUNT"));

                                        //get each file names and populate lists
                                        for(int i=0; i< count; i++){

                                            String fileName = FILES.getString(Integer.toString(i));
                                            tokens.add(fileName);
                                        }

                                        //change neighborEdgeCurrentDir = item(current dir String)
                                        neighborEdgeCurrentDir = item;

                                        //change textView
                                        textView.setText(lsUtils.guidTOname(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache) + ": " + neighborEdgeCurrentDir);

                                        //change listView
                                        setItemsOnListView(tokens);

                                    }else{
                                        //Toast.makeText(this, "Requested directory no longer exists in Neighbors edge, please Toggle view and come back.", Toast.LENGTH_SHORT).show();
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
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
        //menu.setHeaderTitle("Select Action");
    }


    @Override
    public boolean onContextItemSelected(MenuItem item){

        //get the item that was hold-clicked
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String value = ((TextView) info.targetView).getText().toString();

        //check if its OWNEDGEDIR OR NEIGHBOREDGEDIR
        if(currentMode.equals(OWNEDGEDIR)) {

            //check if value is a directory or file
            if (value.charAt(0) == '/') {

                //value is a directory
                if (item.getItemId() == R.id.open) {

                    //just fetch directory ans set view
                    setViewForOwnEdge(ownEdgeCurrentDir + value.substring(1, value.length()) + "/");

                } else if (item.getItemId() == R.id.delete) {

                    //delete the directory and refresh
                    Foo("mdfs -rm " + ownEdgeCurrentDir + value.substring(1, value.length()) + "/", this);

                }
            } else {
                //value is a file
                if (item.getItemId() == R.id.open) {

                    //make mdfs get request
                    Foo("mdfs -get " + ownEdgeCurrentDir + value + " /storage/emulated/0/" + Constants.DEFAULT_DECRYPTION_FOLDER_NAME + "/", this);


                } else if (item.getItemId() == R.id.delete) {

                    //delete the file and refresh
                    Foo("mdfs -rm " + ownEdgeCurrentDir + value, this);

                }

            }
            return true;
        }else if(currentMode.equals(NEIGHBOREDGEDIR)){

            //check if neighborEdgeCurrentDir = SELECTEDGEMASTER
            if(neighborEdgeCurrentDir.equals(SELECTEDGEMASTER)){
                //user selected any of the masters name
                if (item.getItemId() == R.id.open) {
                    Toast.makeText(this, "Tap on neighbor name to open directory.", Toast.LENGTH_SHORT).show();

                }else if(item.getItemId() == R.id.delete){
                    //user wants to delete a master
                    Toast.makeText(this, "Cannot delete a neighbor.", Toast.LENGTH_SHORT).show();
                }

            }else{
                //user choose a directory string
                if (item.getItemId() == R.id.open) {
                    //check if its a directory or file
                    if(value.charAt(0)=='/') {
                        //its a directory
                        Toast.makeText(this, "Tap on neighbor directory to open.", Toast.LENGTH_SHORT).show();
                    }else{
                        //its a file, so we need to retrieve it
                        get.getFileFromNeighbor(value, neighborEdgeCurrentDir, currentBrowsingNeighborGUID);
                        Toast.makeText(this, "-get Info: request has been placed.", Toast.LENGTH_SHORT).show();

                    }
                }else if(item.getItemId() == R.id.delete){
                    //user wants to delete a master
                    Toast.makeText(this, "Cannot change a neighbor directory.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        //dummy return
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


    //takes a command, executes it, and toasts the result.
    //this function submits the task and returns immediately,
    //so the caller or UI is never blocked.
    public static void Foo(String command, Context context){

        class executeFutureTaskAndGetResult implements  Runnable{

            //variables
            String command;
            Context context;

            //constructor
            public executeFutureTaskAndGetResult(String command, Context context){
                this.command = command;
                this.context = context;
            }

            //callable
            Callable<String> task = () -> {
                try {
                    return new UIRequestHandler(command, context).run();
                }
                catch (Exception e) {
                    throw new IllegalStateException("task interrupted", e);
                }
            };

            //run function
            @Override
            public void run(){
                ExecutorService executor = Executors.newSingleThreadExecutor();

                Future<String> future = executor.submit(task);

                try {
                    while (!future.isDone()) {
                        Thread.sleep(500);
                    }

                    String result = future.get();
                    
                    if(result!=null){
                        MainActivity.activity.runOnUiThread(new Runnable() {
                            public void run() {

                                //toast
                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show();

                                //refresh view
                                if(command.contains("-put") || command.contains("-rm") || command.contains("-mkdir")){
                                    setViewForOwnEdge(ownEdgeCurrentDir);
                                }

                            }
                        });
                    }else{
                        MainActivity.activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, "Could not execute command, Executor returned null.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            
        }

        Executor.executor.submit(new executeFutureTaskAndGetResult(command, context));
    }


    //takes a list of items and sets them for view
    public void setItemsOnListView(List<String> list){

        //initialize and set array adapter
        this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listView.setAdapter(arrayAdapter);

    }

    //takes a list of items and show in the

    //takes a directory string, fetch directory and sets view for ownEdgeDir.
    public static void setViewForOwnEdge(String directory){

        //first set current directory
        ownEdgeCurrentDir = directory;

        //first get reply for ls command for / directory
        String reply = ls.ls(directory, "lsRequestForOwnEdge");

        //check if reply is correct
        if(reply!=null){

            //set textview
            textView.setText(OWNEDGEDIR +": " + ownEdgeCurrentDir);

            //create List and populate with ls infols
            List<String> arrayList = lsUtils.jsonToList(reply);

            //initialize and set array adapter
            arrayAdapter = new ArrayAdapter(MainActivity.context, android.R.layout.simple_list_item_1, arrayList);
            listView.setAdapter(arrayAdapter);


        }else{
            Toast.makeText(MainActivity.context, "Could not fetch root directory.", Toast.LENGTH_SHORT).show();
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

                    //get list of all masters guids
                    List<String> neighborMastersGUIDs = lsUtils.getListOfMastersGUIDsFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);

                    //check if list is null or empty
                    if(neighborMastersGUIDs!=null){

                        //convert GUIDs into Names
                        List<String> neighborMastersNames = lsUtils.masterGUIDsToNAMEs(neighborMastersGUIDs, allNeighborEdgeDirsCache);

                        //check empty
                        if(neighborMastersNames.size()!=0) {

                            //set currentView
                            currentMode = NEIGHBOREDGEDIR;

                            //set neighbor current directory
                            neighborEdgeCurrentDir = SELECTEDGEMASTER;

                            //set all neighbor masters for view and select
                            setItemsOnListView(neighborMastersNames);

                            //set textview
                            textView.setText(SELECTEDGEMASTER);

                            //disable mkdir, put, back buttons
                            mkdirButton.setEnabled(false);
                            putButton.setEnabled(false);

                        }else{

                            Toast.makeText(this, "Neighbor masters names list returned empty.", Toast.LENGTH_SHORT).show();
                        }

                    }else if(neighborMastersGUIDs.size()==0){

                        Toast.makeText(this, "Neighbor masters guid list returned null.", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackPressed(){
        //do nothing that way it never gets back from mainactivity and doesnt crash
    }

    //back button pressed
    public void backButtonClicked(View view) {

        //check mode
        if(currentMode.equals(OWNEDGEDIR)) {

            if (ownEdgeCurrentDir.equals("/")) {
                Toast.makeText(this, "Cannot go back beyond / directory", Toast.LENGTH_SHORT).show();
            } else {

                //first tokenize current directory
                String[] tokens = IOUtilities.delEmptyStr(ownEdgeCurrentDir.split("/"));

                //then take only tokens.length -1 numbers of tokens
                String newDir = "/";
                for (int i = 0; i < tokens.length - 1; i++) {
                    newDir = newDir + tokens[i] + "/";
                }

                //fetch directory from edgeKeeper
                String reply = ls.ls(newDir, "lsRequestForOwnEdge");

                //check
                if (reply != null) {

                    //create arrayList and populate
                    List<String> arrayList = lsUtils.jsonToList(reply);

                    //initialize and set array adapter
                    this.arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
                    listView.setAdapter(arrayAdapter);

                    //set current directory to newDir
                    ownEdgeCurrentDir = newDir;
                    textView.setText(OWNEDGEDIR + ": " + ownEdgeCurrentDir);

                } else {
                    Toast.makeText(this, "Could not fetch directory, EdgeKeeper returned null.", Toast.LENGTH_SHORT).show();
                }

            }
        }else if(currentMode.equals(NEIGHBOREDGEDIR)){

            if(neighborEdgeCurrentDir.equals(SELECTEDGEMASTER)){
                Toast.makeText(this, "Use ToggleView button to switch directory", Toast.LENGTH_SHORT).show();
            }else{

                //current listView is showing directory
                //check if its root
                if(neighborEdgeCurrentDir.equals("/")){

                    //get list of all masters guids
                    List<String> neighborMastersGUIDs = lsUtils.getListOfMastersGUIDsFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);

                    //check if list is null or empty
                    if(neighborMastersGUIDs!=null && neighborMastersGUIDs.size()!=0){

                        //convert guids into names
                        List<String> neighborMastersNames = lsUtils.masterGUIDsToNAMEs(neighborMastersGUIDs, allNeighborEdgeDirsCache);

                        //set currentView
                        currentMode = NEIGHBOREDGEDIR;

                        //set neighbor current directory
                        this.neighborEdgeCurrentDir = SELECTEDGEMASTER;

                        //set all neighbor masters for view and select
                        setItemsOnListView(neighborMastersNames);

                        //set textview
                        textView.setText(SELECTEDGEMASTER);

                        //disable mkdir, put, back buttons
                        mkdirButton.setEnabled(false);
                        putButton.setEnabled(false);

                        //change currentMode
                        currentMode = NEIGHBOREDGEDIR;


                    }
                }else{

                    //neighborEdgeCurrentDir is more than just root
                    //parse them.
                    String[] tokens = IOUtilities.delEmptyStr(neighborEdgeCurrentDir.split("/"));

                    String newDir = "/";
                    for(int i=0; i< tokens.length-1; i++){
                        newDir  = newDir + tokens[i] + "/";
                    }

                    //the item is a dir string for any particular master.
                    //note: we dont fetch neighbor directory from edgekeeper now,
                    //we use from cache.
                    //fetch dir object for this particular master
                    JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache);

                    //check null
                    if (particularMasterDirsObject != null) {


                        //make result list
                        List<String> Tokens = new ArrayList<>();
                        String dirStr = null;
                        try {

                            dirStr = particularMasterDirsObject.getString(newDir);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //check null
                        if(dirStr!=null){

                            try {
                                //convert dirStr string into dirObj
                                JSONObject dirObj = new JSONObject(dirStr);

                                //get the FOLDERS string
                                //note: there must be a FOLDERS object,
                                //even there might be no folders.
                                JSONObject FOLDERS = new JSONObject(dirObj.getString("FOLDERS"));

                                //get folders count in FOLDERS object
                                int count = Integer.parseInt(FOLDERS.getString("COUNT"));

                                //get each folder names and populate lists
                                for (int i = 0; i < count; i++) {

                                    String folderName = "/" + FOLDERS.getString(Integer.toString(i));
                                    Tokens.add(folderName);
                                }

                                //get the FILES string
                                //note: there must be a FILES object,
                                //even there might be no folders.
                                JSONObject FILES = new JSONObject(dirObj.getString("FILES"));

                                //get files count in FILES object
                                count = Integer.parseInt(FILES.getString("COUNT"));

                                //get each file names and populate lists
                                for (int i = 0; i < count; i++) {

                                    String fileName = FILES.getString(Integer.toString(i));
                                    Tokens.add(fileName);
                                }

                                //change neighborEdgeCurrentDir = item(current dir String)
                                neighborEdgeCurrentDir = newDir;

                                //change textView
                                textView.setText(lsUtils.guidTOname(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache) + ": " + neighborEdgeCurrentDir);

                                //change listView
                                setItemsOnListView(Tokens);
                            }catch (JSONException e){e.printStackTrace();}

                        }else{

                            //after fetching neighborEdgeDir, it turns out that -
                            //neighborEdgeCurrentDir for currentBrowsingNeighborGUID no longer exists.
                            //maybe that master has deleted his directory.
                            //so we show toast.
                            Toast.makeText(this, "Requested directory no longer exists in Neighbors edge, please Toggle view and come back.", Toast.LENGTH_SHORT).show();
                        }
                    }


                }

            }
        }
    }

    //refresh button pressed
    public void refreshButtonClicked(View view){
        if(currentMode.equals(OWNEDGEDIR)) {
            setViewForOwnEdge(ownEdgeCurrentDir);
        }else if(currentMode.equals(NEIGHBOREDGEDIR)){

            //do ls to fetch entire neighbor information
            String allNeighborLSstr = ls.ls("/", "lsRequestForAllDirectoryiesOfAllNeighborEdges");

            //check
            if(allNeighborLSstr!=null){

                //save it into local cache
                allNeighborEdgeDirsCache = allNeighborLSstr;
            }else{
                Toast.makeText(this, "EdgeKeeper returned null when refreshed directory.", Toast.LENGTH_SHORT).show();
            }


            //take neighbor information from allNeighborEdgeDirsCache,
            //regardless fetch from edgekeeper succeeded.
            if(neighborEdgeCurrentDir.equals(SELECTEDGEMASTER)){

                //get list of all masters guids
                List<String> neighborMastersGUIDs = lsUtils.getListOfMastersGUIDsFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);

                //check if list is null or empty
                if(neighborMastersGUIDs!=null && neighborMastersGUIDs.size()!=0){

                    //convert guids into names
                    List<String> neighborMastersNames = lsUtils.masterGUIDsToNAMEs(neighborMastersGUIDs, allNeighborEdgeDirsCache);

                    //set currentView
                    currentMode = NEIGHBOREDGEDIR;

                    //set neighbor current directory
                    neighborEdgeCurrentDir = SELECTEDGEMASTER;

                    //set all neighbor masters for view and select
                    setItemsOnListView(neighborMastersNames);

                    //set textview
                    textView.setText(SELECTEDGEMASTER);

                    //disable mkdir, put, back buttons
                    mkdirButton.setEnabled(false);
                    putButton.setEnabled(false);

                    System.out.println("all neighbor masters refreshed.");

                }

            }else{

                //fetch dir object for this particular master
                JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache);

                //check null
                if (particularMasterDirsObject != null) {

                    //make result list
                    List<String> Tokens = new ArrayList<>();

                    String dirStr = null;
                    try {

                        dirStr = particularMasterDirsObject.getString(neighborEdgeCurrentDir);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    //check null
                    if(dirStr!=null){

                        try {
                            //convert dirStr string into dirObj
                            JSONObject dirObj = new JSONObject(dirStr);

                            //get the FOLDERS string
                            //note: there must be a FOLDERS object,
                            //even there might be no folders.
                            JSONObject FOLDERS = new JSONObject(dirObj.getString("FOLDERS"));

                            //get folders count in FOLDERS object
                            int count = Integer.parseInt(FOLDERS.getString("COUNT"));

                            //get each folder names and populate lists
                            for (int i = 0; i < count; i++) {

                                String folderName = "/" + FOLDERS.getString(Integer.toString(i));
                                Tokens.add(folderName);
                            }

                            //get the FILES string
                            //note: there must be a FILES object,
                            //even there might be no folders.
                            JSONObject FILES = new JSONObject(dirObj.getString("FILES"));

                            //get files count in FILES object
                            count = Integer.parseInt(FILES.getString("COUNT"));

                            //get each file names and populate lists
                            for (int i = 0; i < count; i++) {

                                String fileName = FILES.getString(Integer.toString(i));
                                Tokens.add(fileName);
                            }

                            //change textView
                            textView.setText(lsUtils.guidTOname(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache) + ": " + neighborEdgeCurrentDir);

                            //change listView
                            setItemsOnListView(Tokens);

                            System.out.println("Directory " + neighborEdgeCurrentDir + " for master " + currentBrowsingNeighborGUID + " refreshed.");

                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }else{

                        //after fetching neighborEdgeDir, it turns out that -
                        //neighborEdgeCurrentDir for currentBrowsingNeighborGUID no longer exists.
                        //maybe that master has deleted his directory.
                        //so we show toast.
                        Toast.makeText(this, "Requested directory no longer exists in Neighbors edge, please Toggle view and come back.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
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
                    Foo("mdfs -mkdir " + ownEdgeCurrentDir +value, context);

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
                    //Toast.makeText(this, path, Toast.LENGTH_LONG).show();
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
                    Foo("mdfs -put " + localfilepath + " " + ownEdgeCurrentDir + value, MainActivity.context);


                }else{
                    Toast.makeText(getApplicationContext(), "Provided mdfs directory is empty.", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        // Display the custom alert dialog on interface
        dialog.show();
    }



    //flushes the MainActivity with message
    public static void flushUI(String msg){

        //UI update
        MainActivity.activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                //UI
                MainActivity.textView.setText("");
                MainActivity.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                MainActivity.textView.setText(msg);

                //create emptylist
                List<String> arrayList = new ArrayList<>();

                //initialize and set array adapter
                arrayAdapter = new ArrayAdapter(MainActivity.context, android.R.layout.simple_list_item_1, arrayList);
                listView.setAdapter(arrayAdapter);

                //disable all buttons
                MainActivity.backButton.setEnabled(false);
                MainActivity.refreshButton.setEnabled(false);
                MainActivity.mkdirButton.setEnabled(false);
                MainActivity.putButton.setEnabled(false);
                MainActivity.toggleView.setEnabled(false);

            }
        });
    }


}
