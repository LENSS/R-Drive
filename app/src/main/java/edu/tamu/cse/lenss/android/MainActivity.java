package edu.tamu.cse.lenss.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS.UIRequestHandler;
import edu.tamu.cse.lenss.monitor.Atak;
import edu.tamu.cse.lenss.monitor.Collector;
import edu.tamu.cse.lenss.monitor.Dropbox;
import edu.tamu.cse.lenss.monitor.Survey123;
import edu.tamu.lenss.MDFS.Commands.get.FileMerge;
import edu.tamu.lenss.MDFS.Commands.get.MDFSFileRetrieverViaRsock;
import edu.tamu.lenss.MDFS.Commands.get.get;
import edu.tamu.lenss.MDFS.Commands.get.getUtils;
import edu.tamu.lenss.MDFS.Commands.ls.ls;
import edu.tamu.lenss.MDFS.Commands.ls.lsUtils;
import edu.tamu.lenss.MDFS.Commands.put.put;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileRetrieve;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    //single executor only used for this class
    private static ExecutorService foothread = Executors.newSingleThreadExecutor();

    //global variables
    public static Activity activity;
    public static Context context;
    public static View view;
    static FloatingActionButton fab=null;
    static SwipeRefreshLayout swipeContainer;
    static DrawerLayout drawer;
    static NavigationView navigationView;
    static ActionBarDrawerToggle toggle;
    static ListView listView;
    static TextView textView;
    static Button mkdirButton;
    static Button putButton;
    static Button cameraButton;
    static String RDRIVE_SHARED_PREF = "RDRIVE_SHARED_PREF";
    static String K_VALUE = "K_VALUE";
    static String BLOCK_SIZE = "BLOCK_SIZE";
    static String FILE_DECRYPTION_PATH = "FILE_DECRYPTION_PATH";
    static String MONITOR_INTERVAL = "MONITOR_INTERVAL_IN_SECONDS";
    static String WA_FOR_ALGORITHM  =  "WA_FOR_ALGORITHM";
    static String FILE_AVAILABILITY_TIME  =  "FILE_AVAILABILITY_TIME";

    //current browsing directory for own edge
    public static String ownEdgeCurrentDir = "";

    //current browsing directory for neighbor edge
    private static String neighborEdgeCurrentDir = "";
    private static String currentBrowsingNeighborGUID = "";

    //view mode whether its ownEdgeDir or neighborEdgeDir view.
    private static final String OWNEDGEDIR = "OWN EDGE DIR";
    private static final String NEIGHBOREDGEDIR = "NEIGHBOR EDGE DIR";

    private static String allNeighborEdgeDirsCache = "";
    public static String ownEdgeDirCache = "";

    private static String currentMode = OWNEDGEDIR; //start with own edge dir
    private boolean scrollEnabled;


    //important booleans checked during startup
    private static boolean checkCameraPermission = false;
    public static boolean freezeUI = false;
    private static String freezeUI_Reason = "";

    //current list of files and folders in current view
    private static String FILE = "FILE";
    private static String FOLDER = "FOLDER";
    private static List<String> directoryItems = new ArrayList<>(); //possible entries: FOLDER, FILE_null, FILE__fileID__N__K__totalNumOfBlocks
    private static String tempFile;

    public static DirectoryUpdater drUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //used so that cameraButton feature could work without throwing FileUriExposedException exception.
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        //must do these for this class code to work.
        activity = this;
        context = getApplicationContext();
        view = findViewById(R.id.drawer_layout);

        //textview for directory
        this.textView = (TextView) findViewById(R.id.textview);
        this.textView.setMovementMethod(new ScrollingMovementMethod());

        //start directory updater function
        this.drUpdater = new DirectoryUpdater(context, activity);

        //swipe refresh
        // Lookup the swipe container view
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if(!freezeUI) {
                    if (currentMode.equals(OWNEDGEDIR)) {
                        //update own edge directory
                        setViewForOwnEdge(ownEdgeCurrentDir);

                    } else if (currentMode.equals(NEIGHBOREDGEDIR)) {

                        //do ls to fetch entire neighbor information
                        String allNeighborLSstr = ls.ls("/", "lsRequestForAllDirectoryiesOfAllNeighborEdges");

                        //check
                        if (allNeighborLSstr != null) {

                            //save it into local cache
                            allNeighborEdgeDirsCache = allNeighborLSstr;
                        } else {
                            //show error
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "EdgeKeeper returned null when refreshed directory.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        //parse dir object for this particular master
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
                            if (dirStr != null) {

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
                                    //even there might be no files.
                                    JSONObject FILES = new JSONObject(dirObj.getString("FILES"));

                                    //get files count in FILES object
                                    count = Integer.parseInt(FILES.getString("COUNT"));

                                    //get each file names and populate lists
                                    for (int i = 0; i < count; i++) {

                                        String fileName = FILES.getString(Integer.toString(i));
                                        Tokens.add(fileName);
                                    }

                                    //change textView
                                    textView.setText(neighborEdgeCurrentDir);

                                    //change listView
                                    setItemsOnListView(Tokens);


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {

                                //after fetching neighborEdgeDir, it turns out that -
                                //neighborEdgeCurrentDir for currentBrowsingNeighborGUID no longer exists.
                                //maybe that master has deleted his directory.
                                //so we show toast.
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Requested directory no longer exists in Neighbor edge.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                    }
                    utils.vibrator(50, getApplicationContext());
                    swipeContainer.setRefreshing(false);
                }else{
                    snackbar(freezeUI_Reason);
                    utils.vibrator(50, getApplicationContext());
                    swipeContainer.setRefreshing(false);
                }

            }

        });


        //toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.GRAY);

        //setting on top right of toolbar
        setSupportActionBar(toolbar);

        //floating action button at bottom right
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                plusButtonClicked();

            }
        });

        //drawer on top left of toolbar
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if(currentMode.equals(OWNEDGEDIR)){
                    updateNavHeader(EdgeKeeper.ownName, EdgeKeeper.ownGUID);
                }else if(currentMode.equals(NEIGHBOREDGEDIR)){
                    updateNavHeader(lsUtils.guidTOname(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache),currentBrowsingNeighborGUID);
                }            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);



        //listview
        this.listView = (ListView)  findViewById(R.id.listview);
        this.listView.setOnScrollListener(new AbsListView.OnScrollListener(){
            private int lastFirstVisibleItem;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //swipeContainer.setEnabled(false);
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition = (listView == null || listView.getChildCount() == 0) ? 0 : listView.getChildAt(0).getTop();
                boolean newScrollEnabled = (firstVisibleItem == 0 && topRowVerticalPosition >= 0) ? true : false;
                if (null != swipeContainer && scrollEnabled != newScrollEnabled) {
                    // Start refreshing....
                    swipeContainer.setEnabled(newScrollEnabled);
                    scrollEnabled = newScrollEnabled;
                }

            }

        });
        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {

                //get item
                String item =  (String)((TextView) view.findViewById(R.id.txtTitle)).getText();

                //add "/" if its a directory
                if(directoryItems.get(position).equals(FOLDER)){
                    item = "/" + item;
                }

                //check if currentView is OWNEDGEDIR or NEIGHBOREDGEDIR
                if(currentMode.equals(OWNEDGEDIR)) {

                    //check if item is lt least one char long
                    if(item.length()>0) {

                        //check its a directory or file
                        if (item.charAt(0) == '/') {

                            //item is a directory
                            //tokenize current directory
                            String[] tokens = IOUtilities.delEmptyStr(ownEdgeCurrentDir.split("/"));

                            //make new directory
                            String newDir = ownEdgeCurrentDir + item.subSequence(1, item.length()) + "/";

                            //setview
                            setViewForOwnEdge(newDir);

                        } else {
                            String finalItem = item;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String[] fInfo = directoryItems.get(position).split("__");
                                        if (fInfo[0].equals(FILE) && !fInfo[1].equals("null")) {
                                            String outputDir = Environment.getExternalStorageDirectory().toString() + File.separator + Constants.DECRYPTION_FOLDER_NAME + File.separator;
                                            MDFSFragmentForFileRetrieve mdfsfrag = new MDFSFragmentForFileRetrieve(null, null, Integer.parseInt(fInfo[2]), Integer.parseInt(fInfo[3]), null, null, finalItem, null, fInfo[1], Integer.parseInt(fInfo[4]), -1, -1, outputDir, null, -1, true);
                                            boolean res = FileMerge.fileMerge(mdfsfrag);
                                            if (res) {

                                                if (finalItem.contains(".jpg") || finalItem.contains(".png")) {
                                                    File file = new File(outputDir + finalItem);
                                                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                                                    Uri data = Uri.parse("file://" + file.getAbsolutePath());
                                                    intent.setDataAndType(data, "image/*");
                                                    startActivity(intent);
                                                }else if(finalItem.contains(".mp4")){
                                                    File file = new File(outputDir + finalItem);
                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setDataAndType(Uri.fromFile(file), "video/*");
                                                    startActivity(intent);
                                                }
                                            }
                                        } else {
                                            activity.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(MainActivity.context, "File must be retrieved first", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }
                }else if(currentMode.equals(NEIGHBOREDGEDIR)){

                    //resultant list to show on listView
                    List<String> tokens = new ArrayList<>();

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
                                    textView.setText(neighborEdgeCurrentDir);

                                    //change listView
                                    setItemsOnListView(tokens);

                                }else{
                                    //Toast.makeText(this, "Requested directory no longer exists in Neighbors edge, please Toggle view and come back.", Toast.LENGTH_SHORT).show();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }else{
                        //it must be a file. file on neighbor edge only responds to hold clicks which we handle onContextItemSelected() function
                    }

                }
            }

        });

        registerForContextMenu(listView);

        //check permission for this app
        utils.vibrator(300, this);
        checkPermissions();

    }


    //fetches previously set user setting from sharedpreferences
    private void setPreviousAppSetting() {

        //fetch and set K value
        String k = utils.SharedPreferences_get(RDRIVE_SHARED_PREF, K_VALUE);
        if(k!=null && !k.equals("")){
            Constants.K_VALUE = Integer.parseInt(k);
        }

        //fetch and set block size
        String bs = utils.SharedPreferences_get(RDRIVE_SHARED_PREF, BLOCK_SIZE);
        if(bs!=null && !bs.equals("")){
            Constants.BLOCK_SIZE_IN_MB = Integer.parseInt(bs);
        }

        //fetch and set file decryption folder name
        String fname = utils.SharedPreferences_get(RDRIVE_SHARED_PREF, FILE_DECRYPTION_PATH);
        if(fname!=null && !fname.equals("")){
            Constants.DECRYPTION_FOLDER_NAME = fname;
        }

        //fetch and set monitor interval
        String interval = utils.SharedPreferences_get(RDRIVE_SHARED_PREF, MONITOR_INTERVAL);
        if(interval!=null && !interval.equals("")){
            edu.tamu.cse.lenss.monitor.Constants.MONITOR_INTERVAL_IN_SECONDS = Integer.parseInt(interval);
        }

        //fetch and set Wa for algorithm
        String Wa = utils.SharedPreferences_get(RDRIVE_SHARED_PREF, WA_FOR_ALGORITHM);
        if(Wa!=null && !Wa.equals("")){
            Constants.WA_FOR_ALGORITHM = Double.parseDouble(Wa);
        }

        //fetch and set File Availability Time (in minutes) for algorithm
        String FAT = utils.SharedPreferences_get(RDRIVE_SHARED_PREF, FILE_AVAILABILITY_TIME);
        if(FAT!=null && !FAT.equals("")){
            Constants.FILE_AVAILABILITY_TIME = Integer.parseInt(FAT);
        }

    }


    @Override
    protected void onDestroy() {

        // Stop the service
        this.stopService(new Intent(this, RDRIVEService.class));


        /*
        //this block of code restarts the service once again after the app is closed
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("edu.tamu.cse.lenss.android.restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);*/

        super.onDestroy();


    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        //menu.setHeaderTitle("Select Action");
    }


    @Override
    public boolean onContextItemSelected(MenuItem menuitem){

        //get the item that was hold-clicked
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuitem.getMenuInfo();

        //get item postion from list
        int index = info.position;

        //get text from list
        View vvv = info.targetView;
        String value =  (String)((TextView) vvv.findViewById(R.id.txtTitle)).getText();

        //check if value is a directory then we add the "/" at beginning
        if(directoryItems.get(index).equals(FOLDER)){
            value = "/" + value;
        }

        //check if its OWNEDGEDIR OR NEIGHBOREDGEDIR
        if(currentMode.equals(OWNEDGEDIR)) {

            //check if item is at least oen char long
            if(value.length()>0) {
                //check if value is a directory or file
                if (value.charAt(0) == '/') {

                    //value is a directory
                    if (menuitem.getItemId() == R.id.open) {

                        //just fetch directory ans set view
                        setViewForOwnEdge(ownEdgeCurrentDir + value.substring(1, value.length()) + "/");

                    } else if (menuitem.getItemId() == R.id.delete) {

                        //delete the directory and refresh
                        Foo("mdfs -rm " + ownEdgeCurrentDir + value.substring(1, value.length()) + "/", this, true);

                    }
                } else {
                    //value is a file
                    if (menuitem.getItemId() == R.id.open) {

                        //make mdfs get request
                        Foo("mdfs -get " + ownEdgeCurrentDir + value + " " + Environment.getExternalStorageDirectory().toString() + File.separator + Constants.DECRYPTION_FOLDER_NAME + File.separator, this, true);


                    } else if (menuitem.getItemId() == R.id.delete) {

                        //delete the file and refresh
                        Foo("mdfs -rm " + ownEdgeCurrentDir + value, this, true);

                    }

                }
            }
        }else if(currentMode.equals(NEIGHBOREDGEDIR)){

            //user choose a directory string
            if (menuitem.getItemId() == R.id.open) {
                //check if its a directory or file
                if(value.charAt(0)=='/') {
                    //its a directory
                    //resultant list to show on listView
                    List<String> tokens = new ArrayList<>();

                    //check its a directory or file,
                    //inside currentBrowsingNeighborGUID's directory
                    if (value.charAt(0) == '/') {

                        //the value is a dir string for any particular master.
                        //note: we dont fetch neighbor directory from edgekeeper now,
                        //we use from cache.
                        //fetch dir object for this particular master
                        JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache);

                        //check null
                        if (particularMasterDirsObject != null) {

                            try {

                                //get the directory string for root
                                //note, at this point we need root dir to view.
                                value = neighborEdgeCurrentDir + value.substring(1) + File.separator;
                                String dirStr = particularMasterDirsObject.getString(value);

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
                                    neighborEdgeCurrentDir = value;

                                    //change textView
                                    textView.setText(neighborEdgeCurrentDir);

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
                }else{
                    //its a file, so we need to retrieve it
                    get.getFileFromNeighbor(value, neighborEdgeCurrentDir, currentBrowsingNeighborGUID);
                    Toast.makeText(this, "Request has been placed.", Toast.LENGTH_SHORT).show();

                }
            }else if(menuitem.getItemId() == R.id.delete){
                //user wants to delete a master
                Toast.makeText(this, "Cannot change a neighbor directory.", Toast.LENGTH_SHORT).show();
            }
        }

        //dummy return
        return true;
    }



    //takes a list of items (FILEs and FOLDERs)and sets them for view on directory ListView (listview).
    //does the same thing as setViewForOwnEdge() function
    //this function is mostly used when setting neighbor edge items (edge name, FILEs, FOLDERs)
    public void setItemsOnListView(List<String> list){

        int[] drawableIds = new int[list.size()];
        String[] itemtextIds = new String[list.size()];
        directoryItems.clear();

        //take care of FOLDERs first
        int index=0;
        for(int i=0;i< list.size(); i++){
            if(list.get(i).charAt(0)=='/'){
                //check if this file has enough
                drawableIds[index] = R.drawable.ic_folder;
                directoryItems.add(FOLDER);
                itemtextIds[index] = list.get(i).substring(1);
                index++;
            }
        }

        //take care if FILEs second
        for(int i=0;i< list.size(); i++){
            if(list.get(i).charAt(0)!='/'){
                drawableIds[index] = R.drawable.ic_file;
                directoryItems.add(FILE + "__" + null);
                itemtextIds[index] = list.get(i);
                index++;
            }
        }


        CustomAdapter adapter = new CustomAdapter(this, itemtextIds, drawableIds);
        listView.setAdapter(adapter);

    }

    //a static function to be called from anywhere of the project.
    //takes a directory string, fetch directory and sets view for ownEdgeDir.
    //does the same thing as setItemsOnListView() function
    //this function is used for setting oqn edge FILEs and FOLDERs in listview.
    public static void setViewForOwnEdge(String directory){

        Thread tr = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    //first set current directory
                    ownEdgeCurrentDir = directory;

                    //first get reply for ls command for / directory
                    ownEdgeDirCache = ls.ls(directory, "lsRequestForOwnEdge");

                    //check if reply is correct
                    if (ownEdgeDirCache != null) {

                        //set textview
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                textView.setText(ownEdgeCurrentDir);
                            }
                        });

                        //create List and populate with file names from ls reply
                        List<String> list = lsUtils.jsonToList(ownEdgeDirCache);

                        //get a list of all files from local storage which are ready to be decoded right away
                        Map<String, List<String>> locallyAvailableFiles = getUtils.getAllLocallyAvailableFiles();

                        //prepare array for drawable icons
                        int[] drawableIds = new int[list.size()];

                        //prepare array for file or folder name
                        String[] itemtextIds = new String[list.size()];

                        //clear old directory items
                        directoryItems.clear();

                        //iterate over each items from ls reply
                        //take care of FOLDERs first
                        int index = 0;
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).charAt(0) == '/') {
                                drawableIds[index] = R.drawable.ic_folder;
                                directoryItems.add(FOLDER);
                                itemtextIds[index] = list.get(i).substring(1);
                                index++;
                            }
                        }

                        //iterate over each items from ls reply
                        //take care of FILEs second
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).charAt(0) != '/') {

                                String fname = list.get(i);

                                //check if this file has enough fragments available
                                List<String> isAvail = locallyAvailableFiles.getOrDefault(fname, new ArrayList<>());

                                if (isAvail.size() == 0) {
                                    //enough fragments not available
                                    drawableIds[index] = R.drawable.ic_file;
                                    directoryItems.add(FILE + "__" + null);
                                    itemtextIds[index] = fname;
                                } else {
                                    if (isAvail.get(0)!=null){
                                        //enough fragments available
                                        drawableIds[index] = R.drawable.ic_file_available;
                                        directoryItems.add(FILE+ "__"+isAvail.get(0));
                                        itemtextIds[index] = fname;
                                    }else{
                                        //enough fragments not available
                                        drawableIds[index] = R.drawable.ic_file;
                                        directoryItems.add(FILE+"__"+null);
                                        itemtextIds[index] = fname;
                                    }

                                    //update the map
                                    isAvail.remove(0);
                                    locallyAvailableFiles.put(fname, isAvail);
                                }
                                index++;
                            }
                        }


                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                CustomAdapter adapter = new CustomAdapter(context, itemtextIds, drawableIds);
                                listView.setAdapter(adapter);
                            }
                        });


                    } else {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.context, "Could not fetch root directory.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        tr.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }




    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_setting) {
            drawerSettingHandler();
        } else if (id == R.id.nav_monitor) {
            monitorHandler_Tile();
        } else if (id == R.id.nav_switch) {

            SwitchHandler();

        } else if (id == R.id.nav_rshare) {

            Intent intent = getPackageManager().getLaunchIntentForPackage("com.example.rshare");
            if (intent != null) {
                // If the application is avilable
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
            }else {
                Toast.makeText(this, "Could not open RShare app.", Toast.LENGTH_SHORT).show();
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    //setting for app parameters
    //all setting is stored in Android SharedPreferences,
    //so that when app is closed, the setting remains.
    private void drawerSettingHandler() {

        //prepare the list of setting options
        String settingOptions[] ={"Change K Value","Change Block Size","Change File Decryption Path", "Change Monitor Interval", "Change File Availability Weight", "Change File Availability Time", "Clear Cache"};
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.setting_listview, null);
        alertDialog.setView(convertView);
        alertDialog.setCancelable(true);
        alertDialog.setTitle("Select Setting Option:");
        ListView lv = (ListView) convertView.findViewById(R.id.lv);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,settingOptions);
        lv.setAdapter(adapter);
        final AlertDialog d1 = alertDialog.show();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView <? > arg0, View view, int position, long id) {

                //dismiss setting_listview dialog
                d1.dismiss();
                d1.cancel();

                //get item that was selected from setting list
                String settingListItem =  (String)((TextView) view).getText();

                //prepare second alertdialog based on the chosen setting option from setting list
                AlertDialog.Builder bldr = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater infltr = getLayoutInflater();
                View dlv = infltr.inflate(R.layout.setting_edittext, null);
                bldr.setCancelable(true);
                bldr.setView(dlv);
                TextView tv = dlv.findViewById(R.id.setting_value);
                Button ok = dlv. findViewById(R.id.setting_OKButton);
                String hint="";
                int maxValue=99999;

                if(settingListItem.equals(settingOptions[0])) {

                    bldr.setTitle("Enter New K Value:");
                    maxValue = put.getalllocalguids().size();
                    if(maxValue==0){maxValue=1;}
                    hint = "1 ≤ K ≤ " + maxValue + " or 'auto'";
                    int currVal = Constants.K_VALUE;
                    if(currVal==Constants.DEFAULT_K_VALUE){
                        hint = hint + " | " + "current: auto";
                    }else {
                        hint = hint + " | " + "current: " + currVal;
                    }
                    tv.setHint(hint);



                }else if(settingListItem.equals(settingOptions[1])){

                    bldr.setTitle("Enter New Block Size:");
                    maxValue = Constants.MAX_BLOCK_SIZE_IN_MB;
                    hint = "1 ≤ BS ≤ " + maxValue + " or 'auto'";
                    long currVal = Constants.BLOCK_SIZE_IN_MB;
                    hint = hint + " | " + "current: " + currVal + " MB";
                    tv.setHint(hint);


                }else if(settingListItem.equals(settingOptions[2])){

                    bldr.setTitle("Enter New Folder Name:");
                    hint = "Enter folder name or 'auto'";
                    String currVal = Constants.DECRYPTION_FOLDER_NAME;
                    hint = hint + "\n" + "current: " + currVal;
                    tv.setHint(hint);

                }else if(settingListItem.equals(settingOptions[3])){
                    bldr.setTitle("Enter New Monitor Interval:");
                    hint = edu.tamu.cse.lenss.monitor.Constants.MINIMUM_REQUIRED_MONITOR_INTERVAL_IN_SECONDS + " ≤ int ≤ ∞ or 'auto'";
                    int currVal = edu.tamu.cse.lenss.monitor.Constants.MONITOR_INTERVAL_IN_SECONDS;
                    hint = hint + " | " + "current: " + currVal + " sec";
                    tv.setHint(hint);
                }else if(settingListItem.equals(settingOptions[4])){
                    bldr.setTitle("Enter New Wa:");
                    hint = "0.0 ≤ Wa ≤ 1.0 or 'auto'";
                    Double currVal = Constants.WA_FOR_ALGORITHM;
                    hint = hint + " | " + "current: " + currVal;
                    tv.setHint(hint);
                }else if(settingListItem.equals(settingOptions[5])){
                    bldr.setTitle("Enter New File Availability Time:");
                    hint = "1 ≤ FAT ≤ " + Constants.FILE_AVAILABILITY_TIME + " or 'auto'";
                    int currVal = Constants.FILE_AVAILABILITY_TIME;
                    hint = hint + " | " + "current: " + currVal + " min";
                    tv.setHint(hint);
                }else if(settingListItem.equals(settingOptions[6])){
                    try {
                        //get list of files from Cache dir
                        File cacheDir = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.ANDROID_DIR_CACHE);
                        File[] files = cacheDir.listFiles();
                        for (File f : files) {
                            f.delete();
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    Toast.makeText(MainActivity.context, "Cache Directory Cleaned", Toast.LENGTH_SHORT).show();
                }

                //show the second alertdialog except index 6 (clear cache)
                final AlertDialog d2 = bldr.create();
                if(!settingListItem.equals(settingOptions[6])) {
                    d2.show();
                }

                // Set OK button click listener
                int finalMaxValue = maxValue;
                String finalSethint = hint;
                ok.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        //get value from textview
                        String value = tv.getText().toString();

                        if(settingListItem.equals(settingOptions[0])) {

                            if (utils.isNumeric(value)) {

                                int valInt = Integer.parseInt(value);

                                if (valInt >= 1 && valInt <= finalMaxValue) {
                                    utils.SharedPreferences_put(RDRIVE_SHARED_PREF, K_VALUE, value);
                                    Constants.K_VALUE = valInt;
                                    Toast.makeText(MainActivity.context, "K value changed to " + valInt, Toast.LENGTH_SHORT).show();
                                    d2.dismiss();
                                } else {
                                    Toast.makeText(MainActivity.context, "Value is not valid", Toast.LENGTH_SHORT).show();
                                    tv.setText("");
                                    tv.setHint(finalSethint);
                                }


                            } else if (value.toLowerCase().equals("auto")) {
                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, K_VALUE, Integer.toString(Constants.DEFAULT_K_VALUE));
                                Constants.K_VALUE = Constants.DEFAULT_K_VALUE;
                                Toast.makeText(MainActivity.context, "K value changed to default", Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            } else {
                                Toast.makeText(MainActivity.context, "Value is not integer\nor 'auto'", Toast.LENGTH_SHORT).show();
                            }


                        }else if(settingListItem.equals(settingOptions[1])){

                            if(utils.isNumeric(value)){

                                int valInt = Integer.parseInt(value);

                                if(valInt>=1 && valInt <= finalMaxValue){
                                    utils.SharedPreferences_put(RDRIVE_SHARED_PREF, BLOCK_SIZE, value);
                                    Constants.BLOCK_SIZE_IN_MB = valInt;
                                    Toast.makeText(MainActivity.context, "Block size changed to " + valInt + " MB", Toast.LENGTH_SHORT).show();
                                    d2.dismiss();
                                }else{
                                    Toast.makeText(MainActivity.context, "Value is not valid", Toast.LENGTH_SHORT).show();
                                    tv.setText("");
                                    tv.setHint(finalSethint);
                                }


                            }else if(value.toLowerCase().equals("auto")){
                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, BLOCK_SIZE, Long.toString(Constants.DEFAULT_BLOCK_SIZE_IN_MB));
                                Constants.BLOCK_SIZE_IN_MB = Constants.DEFAULT_BLOCK_SIZE_IN_MB;
                                Toast.makeText(MainActivity.context, "Block size changed to default", Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            }else{
                                Toast.makeText(MainActivity.context, "Value is not integer\nor 'auto'", Toast.LENGTH_SHORT).show();
                            }


                        }else if(settingListItem.equals(settingOptions[2])){

                            //check no space in the path
                            if(value.toLowerCase().equals("auto")){

                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, FILE_DECRYPTION_PATH, Constants.DEFAULT_DECRYPTION_FOLDER_NAME);
                                Constants.DECRYPTION_FOLDER_NAME = Constants.DEFAULT_DECRYPTION_FOLDER_NAME;
                                Toast.makeText(MainActivity.context, "File decryption path changed to default", Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            }else if(value.equals("")){
                                Toast.makeText(MainActivity.context, "Provided path is empty", Toast.LENGTH_SHORT).show();
                                tv.setText("");
                                tv.setHint(finalSethint);
                            }else if(!value.contains(" ")){
                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, FILE_DECRYPTION_PATH, value);
                                Constants.DECRYPTION_FOLDER_NAME = value;
                                Toast.makeText(MainActivity.context, "File decryption path changed to\n" + value, Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            }else{
                                Toast.makeText(MainActivity.context, "Provided path cannot contain space", Toast.LENGTH_SHORT).show();
                                tv.setText("");
                                tv.setHint(finalSethint);
                            }

                        }else if(settingListItem.equals(settingOptions[3])){

                            if(utils.isNumeric(value)){

                                int valInt = Integer.parseInt(value);

                                if(valInt>= edu.tamu.cse.lenss.monitor.Constants.MINIMUM_REQUIRED_MONITOR_INTERVAL_IN_SECONDS){
                                    utils.SharedPreferences_put(RDRIVE_SHARED_PREF, MONITOR_INTERVAL, value);
                                    edu.tamu.cse.lenss.monitor.Constants.MONITOR_INTERVAL_IN_SECONDS = valInt;
                                    Toast.makeText(MainActivity.context, "Monitor interval changed to " + valInt + " sec", Toast.LENGTH_SHORT).show();
                                    d2.dismiss();
                                }else{
                                    Toast.makeText(MainActivity.context, "Value is not valid", Toast.LENGTH_SHORT).show();
                                    tv.setText("");
                                    tv.setHint(finalSethint);
                                }
                            }else if(value.toLowerCase().equals("auto")){
                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, MONITOR_INTERVAL, Long.toString(edu.tamu.cse.lenss.monitor.Constants.DEFAULT_MONITOR_INTERVAL_IN_SECONDS));
                                edu.tamu.cse.lenss.monitor.Constants.MONITOR_INTERVAL_IN_SECONDS = edu.tamu.cse.lenss.monitor.Constants.DEFAULT_MONITOR_INTERVAL_IN_SECONDS;
                                Toast.makeText(MainActivity.context, "Monitor interval changed to default", Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            }else{
                                Toast.makeText(MainActivity.context, "Value is not integer\nor 'auto'", Toast.LENGTH_SHORT).show();
                            }
                        }else if(settingListItem.equals(settingOptions[4])){

                            boolean isNumeric = true;
                            try { Double.parseDouble(value);} catch(NumberFormatException e) {isNumeric = false;}

                            if(isNumeric){

                                Double valDouble = Double.parseDouble(value);

                                if(valDouble>=0.0 && valDouble<=1.0){
                                    utils.SharedPreferences_put(RDRIVE_SHARED_PREF, WA_FOR_ALGORITHM, value);
                                    Constants.WA_FOR_ALGORITHM = valDouble;
                                    Toast.makeText(MainActivity.context, "Wa changed to " + valDouble, Toast.LENGTH_SHORT).show();
                                    d2.dismiss();
                                }else{
                                    Toast.makeText(MainActivity.context, "Value is not valid", Toast.LENGTH_SHORT).show();
                                    tv.setText("");
                                    tv.setHint(finalSethint);
                                }
                            }else if(value.toLowerCase().equals("auto")){
                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, WA_FOR_ALGORITHM, Double.toString(Constants.DEFAULT_WA_FOR_ALGORITHM));
                                Constants.WA_FOR_ALGORITHM = Constants.DEFAULT_WA_FOR_ALGORITHM;
                                Toast.makeText(MainActivity.context, "Wa changed to default", Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            }else{
                                Toast.makeText(MainActivity.context, "Value is not between 0 and 1\nor 'auto'", Toast.LENGTH_SHORT).show();
                            }
                        }else if(settingListItem.equals(settingOptions[5])){

                            if(utils.isNumeric(value)){

                                int valInt = Integer.parseInt(value);

                                if(valInt>= 1 && valInt <= Constants.MAX_FILE_AVAILABILITY_TIME){
                                    utils.SharedPreferences_put(RDRIVE_SHARED_PREF, FILE_AVAILABILITY_TIME, value);
                                    Constants.FILE_AVAILABILITY_TIME = valInt;
                                    Toast.makeText(MainActivity.context, "File Avail Time changed to " + valInt + " min", Toast.LENGTH_SHORT).show();
                                    d2.dismiss();
                                }else{
                                    Toast.makeText(MainActivity.context, "Value is not valid", Toast.LENGTH_SHORT).show();
                                    tv.setText("");
                                    tv.setHint(finalSethint);
                                }
                            }else if(value.toLowerCase().equals("auto")){
                                utils.SharedPreferences_put(RDRIVE_SHARED_PREF, FILE_AVAILABILITY_TIME, Long.toString(Constants.MAX_FILE_AVAILABILITY_TIME));
                                Constants.FILE_AVAILABILITY_TIME = Constants.MAX_FILE_AVAILABILITY_TIME;
                                Toast.makeText(MainActivity.context, "File Avail Time changed to default", Toast.LENGTH_SHORT).show();
                                d2.dismiss();
                            }else{
                                Toast.makeText(MainActivity.context, "Value is not integer\nor 'auto'", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });


            }

        });

    }


    //monitors a third party application's
    // appData directory and pulls the data,
    //to store in R-Drive.
    //works with monitor_view_tile.xml
    private void monitorHandler_Tile() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.monitor_view_tile, null);
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(true);
        alertDialog.setTitle("Select Applications:");
        final AlertDialog dialog = alertDialog.show();

        // Get the custom alert dialog view widgets reference
        Button survey123 = (Button) dialogView.findViewById(R.id.survey123);
        Button atak = (Button) dialogView.findViewById(R.id.ATAK);
        Button dropbox = (Button) dialogView.findViewById(R.id.DropBox);
        Button collector = (Button) dialogView.findViewById(R.id.Collector);

        //set button default colors
        survey123.setBackgroundColor(Color.LTGRAY);
        atak.setBackgroundColor(Color.LTGRAY);
        dropbox.setBackgroundColor(Color.LTGRAY);
        collector.setBackgroundColor(Color.LTGRAY);

        //set color if button pressed already
        if(Survey123.buttonPressed) {
            survey123.setBackgroundColor(Color.GRAY);
        }

        if(Atak.buttonPressed){
            atak.setBackgroundColor(Color.GRAY);
        }

        if(Dropbox.buttonPressed){
            dropbox.setBackgroundColor(Color.GRAY);
        }

        if(Collector.buttonPressed){
            collector.setBackgroundColor(Color.GRAY);
        }

        // Set survey123 button click listener
        survey123.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!freezeUI) {
                    if (!Survey123.buttonPressed) {

                        //start pulling appdata
                        Survey123.survey123_thd = new Thread(new Survey123());
                        Survey123.survey123_thd.start();

                        survey123.setBackgroundColor(Color.GRAY);
                        Survey123.buttonPressed = true;
                    } else {
                        //stop pulling appdata
                        Survey123.survey123_thd.interrupt();
                        survey123.setBackgroundColor(Color.LTGRAY);
                        Survey123.buttonPressed = false;
                    }
                }else{
                    snackbar("Cannot Monitor Survey123 appData,\n"+ freezeUI_Reason);
                    dialog.dismiss();
                }

            }
        });

        // Set atak button click listener
        atak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!freezeUI) {
                    if (!Atak.buttonPressed) {

                        atak.setBackgroundColor(Color.GRAY);
                        Atak.buttonPressed = true;
                    } else {

                        atak.setBackgroundColor(Color.LTGRAY);
                        Atak.buttonPressed = false;
                    }
                }else{
                    snackbar("Cannot Monitor ATAK appData,\n"+ freezeUI_Reason);
                    dialog.dismiss();
                }

            }
        });

        // Set dropbox button click listener
        dropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!freezeUI) {
                    if (!Dropbox.buttonPressed) {

                        dropbox.setBackgroundColor(Color.GRAY);
                        Dropbox.buttonPressed = true;
                    } else {

                        dropbox.setBackgroundColor(Color.LTGRAY);
                        Dropbox.buttonPressed = false;
                    }
                }else{
                    snackbar("Cannot Monitor DropBox appData,\n"+ freezeUI_Reason);
                    dialog.dismiss();
                }

            }
        });

        // Set collector button click listener
        collector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!freezeUI) {
                    if (!Collector.buttonPressed) {

                        collector.setBackgroundColor(Color.GRAY);
                        Collector.buttonPressed = true;
                    } else {

                        collector.setBackgroundColor(Color.LTGRAY);
                        Collector.buttonPressed = false;
                    }
                }else{
                    snackbar("Cannot Monitor Collector appData,\n"+ freezeUI_Reason);
                    dialog.dismiss();
                }

            }
        });


    }



    //shoews a list of edges and allows to select one to view its directories.
    private void SwitchHandler() {

        if(!freezeUI) {

            //do ls to fetch entire neighbor information
            String allNeighborLSstr = ls.ls("/", "lsRequestForAllDirectoryiesOfAllNeighborEdges");

            //check reply
            if (allNeighborLSstr != null) {

                //save it into local cache
                allNeighborEdgeDirsCache = allNeighborLSstr;

                //get list of all masters guids
                List<String> neighborMastersGUIDs = lsUtils.getListOfMastersGUIDsFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);


                //check if list is null or empty
                if (neighborMastersGUIDs != null) {

                    //convert GUIDs into Names
                    List<String> neighborMastersNames = lsUtils.masterGUIDsToNAMEs(neighborMastersGUIDs, allNeighborEdgeDirsCache, true);

                    //add ownName in list
                    neighborMastersNames.add("~" + EdgeKeeper.ownName.replace(".distressnet.org", "") + "~");

                    //check empty
                    if (neighborMastersNames.size() != 0) {

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                        LayoutInflater inflater = getLayoutInflater();
                        View convertView = (View) inflater.inflate(R.layout.edgelist_view, null);
                        alertDialog.setView(convertView);
                        alertDialog.setTitle("Select Edge");
                        alertDialog.setCancelable(true);
                        ListView lv = (ListView) convertView.findViewById(R.id.edgelist);
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, neighborMastersNames);
                        lv.setAdapter(adapter);
                        final AlertDialog ad = alertDialog.show();

                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {

                                //get item
                                String item = (String) ((TextView) view).getText();

                                if (item.equals("~" + EdgeKeeper.ownName.replace(".distressnet.org", "") + "~")) {

                                    //change currentMode
                                    currentMode = OWNEDGEDIR;

                                    //update nav header
                                    updateNavHeader(EdgeKeeper.ownName, EdgeKeeper.ownGUID);

                                    //set view for own edge
                                    setViewForOwnEdge(ownEdgeCurrentDir);

                                    //update directory string
                                    textView.setText(ownEdgeCurrentDir);

                                    //show fab
                                    fab.show();

                                    //cancel alertDialog
                                    ad.dismiss();


                                } else {

                                    //resultant list to show on listView
                                    List<String> tokens = new ArrayList<>();

                                    //show / directory for this particular master
                                    //fetch dir object for this particular master
                                    //item = masterName
                                    //first convert name into guid
                                    String guid = lsUtils.nameToGUID(item + ".distressnet.org", allNeighborEdgeDirsCache);

                                    if (guid != null) {

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
                                                    textView.setText(neighborEdgeCurrentDir);

                                                    //change listView
                                                    setItemsOnListView(tokens);

                                                }

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.context, "Could not convert name to GUID, try again later.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    //change currentMode
                                    currentMode = NEIGHBOREDGEDIR;

                                    //change neighborEdgeCurrentDir
                                    neighborEdgeCurrentDir = "/";

                                    //update nav header
                                    updateNavHeader(lsUtils.guidTOname(currentBrowsingNeighborGUID, allNeighborEdgeDirsCache), currentBrowsingNeighborGUID);

                                    //update directory list
                                    setItemsOnListView(tokens);

                                    //update directory string
                                    textView.setText(neighborEdgeCurrentDir);

                                    //hide fab
                                    fab.hide();

                                    //cancel alertDialog
                                    ad.dismiss();
                                }
                            }
                        });


                    } else {

                        Toast.makeText(this, "Neighbor masters names list returned empty.", Toast.LENGTH_SHORT).show();
                    }

                } else if (neighborMastersGUIDs.size() == 0) {

                    Toast.makeText(this, "Neighbor masters guid list returned null.", Toast.LENGTH_SHORT).show();
                }
            }
        }else{
            snackbar( "Cannot Show List of Neighbor Edges,\n"+ freezeUI_Reason);
        }

    }


    //shoes a snackbar on android activity main
    public static void snackbar(String message){
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    //freezes the MainActivity with message
    public static void freezeUI(String msg){

        //first set the global variables
        freezeUI = true;
        freezeUI_Reason = msg;

        //UI update
        MainActivity.activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                //UI
                MainActivity.textView.setText("");
                MainActivity.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

                //create emptylist and set array adapter
                List<String> arrayList = new ArrayList<>();
                ArrayAdapter arrayAdapter = new ArrayAdapter(MainActivity.context, android.R.layout.simple_list_item_1, arrayList);
                listView.setAdapter(arrayAdapter);

                //disable fab
                fab.setEnabled(false);


                //note: do not disable swipeContainer, instead show error snackbar
                //note: do not disable nvaigationDraweer, allow user to still use Setting and Monitor options

                snackbar(msg);
            }
        });
    }


    //takes a command, executes it, and toasts the result.
    //this function submits the task and returns immediately,
    //so the caller or UI is never blocked.
    public static void Foo(String command, Context context, boolean toast){

        //task class: creates a thread to get the task done.
        class executeFutureTaskAndGetResult implements  Runnable{

            //variables
            String command;
            Context context;
            boolean toast;

            //constructor
            public executeFutureTaskAndGetResult(String command, Context context, boolean toast){
                this.command = command;
                this.context = context;
                this.toast = toast;
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
                ExecutorService exec = Executors.newSingleThreadExecutor();

                //submit task
                Future<String> future = exec.submit(task);

                //wait until task is done
                while (!future.isDone()) {
                    try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
                }

                //only care for result if toast is expected to show,
                //otherwise, we do not even fetch the result.
                if(toast) {
                    try {

                        //get result
                        String result = future.get();

                        //check result
                        if (result != null) {

                            ///do work on UI thread
                            MainActivity.activity.runOnUiThread(new Runnable() {
                                public void run() {

                                    Toast.makeText(context, result, Toast.LENGTH_SHORT).show();

                                    //refresh view
                                    if (command.contains("-put") || command.contains("-rm") || command.contains("-mkdir")) {
                                        setViewForOwnEdge(ownEdgeCurrentDir);
                                    }

                                }
                            });
                        } else {

                            ///do work on UI thread
                            MainActivity.activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(context, "Could not execute command, Executor returned null.", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        foothread.submit(new executeFutureTaskAndGetResult(command, context, toast));
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
            case  MY_CAMERA_PERMISSION_CODE:
                if(checkCameraPermission) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    } else {
                        Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                    }
                }
        }
    }

    //if device permission is OK, this function is called from checkPermissions() function
    public void initializeApp(){
        runService();
    }


    //this function starts a service
    void runService(){

        // First stop the already running service
        this.stopService(new Intent(this, RDRIVEService.class));


        Intent intent = new Intent(this, RDRIVEService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        //coming here means R-Drive (both service and app) started properly
        setViewForOwnEdge("/");
        setPreviousAppSetting();

    }



    //--------------------------------------------------------------------------

    //PLUS button pressed
    private static final int READ_REQUEST_CODE = 42;
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    public void plusButtonClicked(){

        fab.hide();

        // Build an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.plus_view,null);

        // Specify alert dialog is not cancelable/not ignorable
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                fab.show();
            }
        });

        // Set the custom layout as alert dialog view
        builder.setView(dialogView);

        // Get the custom alert dialog view widgets reference
        putButton = (Button) dialogView.findViewById(R.id.fileButton);
        mkdirButton = (Button) dialogView.findViewById(R.id.folderButton);
        cameraButton = (Button) dialogView.findViewById(R.id.cameraButton);
        putButton.setBackgroundResource(R.mipmap.add_file);
        mkdirButton.setBackgroundResource(R.mipmap.add_folder);
        cameraButton.setBackgroundResource(R.mipmap.camera);

        // Create the alert dialog
        final AlertDialog alertDialog = builder.create();

        // Set putButton button click listener
        putButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);

                alertDialog.dismiss();
                fab.show();

            }
        });

        // Set mkdirButton button click listener
        mkdirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
                            Foo("mdfs -mkdir " + ownEdgeCurrentDir +value, context, true);

                        }else{
                            Toast.makeText(getApplicationContext(), "Provided folder is empty.", Toast.LENGTH_SHORT).show();
                        }

                        //dialog.cancel();
                        dialog.dismiss();
                    }
                });

                // Display the custom alert dialog on interface
                dialog.show();


                alertDialog.dismiss();
                fab.show();
            }
        });

        // Set cameraButton button click listener
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File cacheDir = new File(Environment.getExternalStorageDirectory() + File.separator + Constants.ANDROID_DIR_CACHE);
                if(!cacheDir.exists()){
                    cacheDir.mkdirs();
                }
                checkCameraPermission = true;
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    tempFile = EdgeKeeper.ownName.replace(".distressnet.org","") + "_" + new SimpleDateFormat("MM_dd_yy_HH_mm_ss").format(new Date()) + ".jpg";
                    File photoFile = new File("/sdcard/" + Constants.ANDROID_DIR_CACHE + File.separator + tempFile);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }

                alertDialog.dismiss();
                fab.show();
            }
        });

        TextView title = new TextView(this);
        title.setText("Create New");
        title.setBackgroundColor(Color.GRAY);
        title.setPadding(10, 10, 10, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(20);
        alertDialog.setCustomTitle(title);

        // Display the custom alert dialog on interface
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GRAY));
        alertDialog.show();

        //put the dialog at the bottom +
        //no background dimming
        Window window = alertDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);

        //set dialog and all internal button sizes
        Display display =((WindowManager)getSystemService(this.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height=display.getHeight();

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            alertDialog.getWindow().setLayout(height,(width)/4);
            putButton.setHeight((width/4));
            mkdirButton.setHeight((width/4));
            cameraButton.setHeight(width/4);
        } else {
            alertDialog.getWindow().setLayout(width,(height)/4);
            putButton.setHeight((height/4));
            mkdirButton.setHeight((height/4));
            cameraButton.setHeight((height/4));

        }

    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {

            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                String path = uri.getPath();

                // Now check if this filepath is from internal memory or not
                if (path.toLowerCase().startsWith("/document/primary:")) {
                    path = path.replaceFirst("/document/primary:", Environment.getExternalStorageDirectory().toString() + "/");
                    showDialogueForMkdir(path);

                } else {
                    Toast.makeText(this, "Choose a file from sdcard.", Toast.LENGTH_LONG).show();
                }
            }
        }else if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            String localfilePath = Environment.getExternalStorageDirectory() + File.separator + Constants.ANDROID_DIR_CACHE + File.separator + tempFile;
            String command = "mdfs -put " + localfilePath + " /camera/";
            Foo(command, this, true);
        }
    }


    //input event for passing MDFS directory value
    private void showDialogueForMkdir(String localfilepath) {

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
                    Foo("mdfs -put " + localfilepath + " " + ownEdgeCurrentDir + value, MainActivity.context, true);


                }else{
                    Toast.makeText(getApplicationContext(), "Provided mdfs directory is empty.", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        // Display the custom alert dialog on interface
        dialog.show();
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
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

                    /*//fetch directory from edgeKeeper
                    String reply = ls.ls(newDir, "lsRequestForOwnEdge");

                    //check
                    if (reply != null) {

                        //create arrayList and populate
                        List<String> arrayList = lsUtils.jsonToList(reply);

                        //initialize and set array adapter
                        setItemsOnListView(arrayList);

                        //set current directory to newDir
                        ownEdgeCurrentDir = newDir;
                        textView.setText(ownEdgeCurrentDir);

                    } else {
                        Toast.makeText(this, "Could not fetch directory, EdgeKeeper returned null.", Toast.LENGTH_SHORT).show();
                    }*/

                    setViewForOwnEdge(newDir);

                }
            }else if(currentMode.equals(NEIGHBOREDGEDIR)){

                //current listView is showing directory
                //check if its root
                if(neighborEdgeCurrentDir.equals("/")){
                    Toast.makeText(this, "Cannot go back beyond / directory", Toast.LENGTH_SHORT).show();

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
                                textView.setText(neighborEdgeCurrentDir);

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

    public void updateNavHeader(String NAME, String GUID){
        try {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            View headerView = navigationView.getHeaderView(0);
            TextView tv_NAME = (TextView) headerView.findViewById(R.id.header_text_1);
            tv_NAME.setText(NAME);

            NavigationView navigationView1 = (NavigationView) findViewById(R.id.nav_view);
            View headerView1 = navigationView1.getHeaderView(0);
            TextView tv_GUID = (TextView) headerView1.findViewById(R.id.header_text_2);
            tv_GUID.setText(GUID);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
