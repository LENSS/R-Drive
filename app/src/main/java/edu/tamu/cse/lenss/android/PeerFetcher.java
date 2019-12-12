package edu.tamu.cse.lenss.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.tamu.cse.lenss.MDFS5.EdgeKeeper.EdgeKeeper;
import edu.tamu.cse.lenss.MDFS5.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;

import static android.content.Context.MODE_PRIVATE;

public class PeerFetcher extends Thread {

    //logger
    public static Logger logger = Logger.getLogger(PeerFetcher.class);

    //GUID and Names shared pref
    private static final String MDFS_PEERS_SHARED_PREF = "MDFS_PEERS_SHARED_PREF";
    private final Context context;

    //thread variables
    private AtomicBoolean is_running = new AtomicBoolean(true);

    public PeerFetcher(Context context){
        this.context = context;
    }


    @Override
    public void run(){


        while(is_running.get()) {


            System.out.println("Fetching peers...");


            //fetch guids
            List<String> peerGUIDs = EKClient.getPeerGUIDs(EdgeKeeperConstants.EdgeKeeper_s, EdgeKeeperConstants.EdgeKeeper_s1);

            //check for name
            if(peerGUIDs!=null) {

                //remove my own GUID, if present
                peerGUIDs.remove(EdgeKeeper.ownGUID);

                //fetch all current keys,aka GUIDs in SP.
                Set<String> guids = Utils.SharedPreferences_keys(MDFS_PEERS_SHARED_PREF, context);

                if(guids!=null) {

                    //run loop for each fetched GUID
                    for (int i = 0; i < peerGUIDs.size(); i++) {

                        //get each guid
                        String guid = peerGUIDs.get(i);

                        //check if this guid already exists
                        if (!guids.contains(guid)) {

                            //make call to convert guid into name
                            String name = EKClient.getAccountNamebyGUID(guid);

                            //check
                            if (name != null) {

                                //put this <guid, name> pair in SP
                                Utils.SharedPreferences_put(MDFS_PEERS_SHARED_PREF, guid, name, context);
                            }
                        }
                    }
                }else{
                    logger.log(Level.ERROR, "Could not fetch all keys from SP.");
                }

            }

            //sleep
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }

    //get all current peers guids as list
    //list can be empty
    public List<String> getCurrentPeersGUIDs(){

        //getAll from SP
        SharedPreferences pref = context.getSharedPreferences(MDFS_PEERS_SHARED_PREF, MODE_PRIVATE);
        Map<String, ?> allEntries = pref.getAll();
        Set<String> guidSet = allEntries.keySet();
        List<String> guidList = new ArrayList<>(guidSet);

        return guidList;
    }

    //get all current unique peer names as list
    //list can be empty.
    public  List<String> getCurrentPeersNames(){

        //create resultant list
        List<String> result = new ArrayList<>();

        //getAll from SP
        SharedPreferences pref =  context.getSharedPreferences(MDFS_PEERS_SHARED_PREF, MODE_PRIVATE);
        Map<String, ?> allEntries = pref.getAll();

        //loop
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String name = entry.getValue().toString();

            //check if already exists
            if(!result.contains(name)){
                result.add(name);
            }
        }

        return result;
    }




    //takes one guid and returns name for it, if data exists.
    public String GUIDtoNameConversion(String GUID){

        //fetch value(name) for this key(guid)
        String name = Utils.SharedPreferences_get(MDFS_PEERS_SHARED_PREF, GUID, context);

        //check
        if(name!=null){
            return name;
        }else{
            return GUID.substring(0, 5) + "-" + GUID.substring(GUID.length()-5, GUID.length());
        }

    }


    //takes a list of Names and returns a list of GUIDs
    //list can be empty.
    public List<String> namesToGUIDsConversion(List<String> names){

        //create resultant list
        List<String> guidList = new ArrayList<>();

        //getAll from SP
        SharedPreferences pref =  context.getSharedPreferences(MDFS_PEERS_SHARED_PREF, MODE_PRIVATE);
        Map<String, ?> allEntries = pref.getAll();

        //loop
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String guid = entry.getKey();
            String name = entry.getValue().toString();

            //check if name matches
            if(names.contains(name)){
                guidList.add(guid);
            }
        }

        return guidList;
    }


    //take one name and converts in into guid
    //can return null.
    public String NameToGUIDConversion(String Name){

        //getAll from SP
        SharedPreferences pref =  context.getSharedPreferences(MDFS_PEERS_SHARED_PREF, MODE_PRIVATE);
        Map<String, ?> allEntries = pref.getAll();

        //loop
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String guid = entry.getKey();
            String name = entry.getValue().toString();

            //check if name matches
            if(name.equals(Name)){
                return guid;
            }
        }

        return null;
    }


    @Override
    public void interrupt(){
        is_running.set(false);
        super.interrupt();
    }
}
