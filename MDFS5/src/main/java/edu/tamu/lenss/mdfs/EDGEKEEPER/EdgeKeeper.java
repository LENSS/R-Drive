package edu.tamu.lenss.mdfs.EDGEKEEPER;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;

public class EdgeKeeper {

    public static String ownGUID;

    public EdgeKeeper(){
        register();
        obtainOwnGUID();
    }


    //note: must make sure this function starts after EdgeKeeper server is running
    private void obtainOwnGUID() {
        ownGUID = EKClient.getOwnGuid();
        if(ownGUID==null){
            System.out.println("EdgeKeeper Error! could not init EdgeKeeper");
            throw new NullPointerException("EdgeKeeper initialization error...Maybe local EdgeKeeper server is not running or not connected.");
        }else{
            System.out.println("own GUID: " + ownGUID);
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
