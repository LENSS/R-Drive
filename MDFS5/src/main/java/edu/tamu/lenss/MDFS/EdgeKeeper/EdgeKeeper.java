package edu.tamu.lenss.MDFS.EdgeKeeper;

import android.widget.Toast;

import org.apache.log4j.Level;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;

public class EdgeKeeper {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EdgeKeeper.class);

    public static String ownGUID;
    public static String ownName;

    public EdgeKeeper(){

        register();
        obtainOwnGUID();


    }


    //note: must make sure this function starts after EdgeKeeper server is running
    private void obtainOwnGUID() {
        ownGUID = EKClient.getOwnGuid();
        ownName = EKClient.getOwnAccountName();
        if(ownGUID==null | ownName==null){
            logger.log(Level.ERROR, "EdgeKeeper Error! could not init local EdgeKeeper");
            throw new NullPointerException("EdgeKeeper initialization error...Maybe local EdgeKeeper server is not running or not connected.");

        }else{
            logger.log(Level.ALL,"own GUID: " + ownGUID);
        }

    }

    //register
    private void register(){
        EKClient.addService(EdgeKeeperConstants.EdgeKeeper_s, EdgeKeeperConstants.EdgeKeeper_s1);
    }


    public static boolean stop(){
        return EKClient.removeService(EdgeKeeperConstants.EdgeKeeper_s);
    }
}
