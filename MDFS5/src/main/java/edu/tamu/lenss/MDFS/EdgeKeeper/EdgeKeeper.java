package edu.tamu.lenss.MDFS.EdgeKeeper;

import org.apache.log4j.Level;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileCreation;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileDeletion;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileRetrieval;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForRsockTest;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import edu.tamu.lenss.MDFS.Utils.Pair;

public class EdgeKeeper {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EdgeKeeper.class);

    public static String ownGUID;
    public static String ownName;
    public static boolean isActive = false;
    public static String EK_Library_Closed_Message = "EdgeKeeper Closed!";
    private static int count = 0;

    public EdgeKeeper() {
        register();
        obtainOwnGUID();

    }


    //note: must make sure this function starts after EdgeKeeper server is running
    private static void obtainOwnGUID() {
        try {
            ownGUID = EKClient.getOwnGuid();
            if (ownGUID == null) {
                logger.log(Level.ERROR, "EdgeKeeper initialization error...Maybe local EdgeKeeper server is not running or not connected.");
                IOUtilities.miscellaneousWorks.put(new Pair(Constants.EDGEKEEPER_CLOSED, EK_Library_Closed_Message ));
                isActive = false;
            } else {
                //success happening here
                ownName = EKClient.getAccountNamebyGUID(ownGUID);
                isActive = true;
                runRsock();
            }
        }catch (Exception e){
            logger.log(Level.DEBUG, "EdgeKeeper Exception! could not fetch ownGUID and ownNAME.");
            isActive = false;
        }

    }

    //register
    private static void register(){
        EKClient.addService(EdgeKeeperConstants.EdgeKeeper_s, EdgeKeeperConstants.EdgeKeeper_s1);
    }

    public static void runRsock(){

        //check if rsock is enabled or nah
        if(RSockConstants.RSOCK) {

            //start file creator rsock in a thread
            //Thread t1 = new Thread(new RsockReceiveForFileCreation());
            Thread t1 = new Thread(new RsockReceiveForFileCreation());
            t1.start();

            // start file retriever rsock in a thread
            Thread t2 = new Thread(new RsockReceiveForFileRetrieval());
            t2.start();

            // start file deletion rsock in a thread
            Thread t3 = new Thread(new RsockReceiveForFileDeletion());
            t3.start();

            // start file test rsock in a thread
            if (RSockConstants.testRsock) {
                Thread t4 = new Thread(new RsockReceiveForRsockTest());
                t4.start();
            }
        }
    }


    public static void stop(){

        //stop rsock
        try {
            //stop rsock if its running.
            if (RSockConstants.RSOCK) {
                RsockReceiveForFileCreation.stop();
                RsockReceiveForFileRetrieval.stop();
                RsockReceiveForFileDeletion.stop();
                if(RSockConstants.testRsock){RsockReceiveForRsockTest.stop();}
            }
        }catch (Exception e){
            logger.debug("Rsock Exception! could not call close() in Interface.java, ", e);
        }

        //stop edgekeeper
        try {
            EKClient.removeService(EdgeKeeperConstants.EdgeKeeper_s);
        }catch (Exception e){
            logger.debug("EdgeKeeper Exception! exception when calling removeService(), ",e);;
        }
    }
}
