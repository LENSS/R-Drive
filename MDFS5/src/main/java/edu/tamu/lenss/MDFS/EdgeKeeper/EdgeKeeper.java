package edu.tamu.lenss.MDFS.EdgeKeeper;

import android.os.Looper;
import android.widget.Toast;

import org.apache.log4j.Level;
import org.apache.log4j.chainsaw.Main;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.lenss.MDFS.MissingLInk.MissingLink;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileCreation;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileDeletion;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileRetrieval;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForRsockTest;

public class EdgeKeeper {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EdgeKeeper.class);

    public static String ownGUID;
    public static String ownName;
    public static boolean started = false;
    private static int count = 0;

    public EdgeKeeper(){
        Thread tr1 = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                while(!started) {
                    doo();

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        tr1.start();
    }

    private static void doo() {
        register();
        obtainOwnGUID();
    }


    //note: must make sure this function starts after EdgeKeeper server is running
    private static void obtainOwnGUID() {
        ownGUID = EKClient.getOwnGuid();
        ownName = EKClient.getOwnAccountName();
        if(ownGUID==null | ownName==null){
            logger.log(Level.ERROR, "EdgeKeeper Error! could not init local EdgeKeeper " + count++);
        }else{
            logger.log(Level.ALL,"own GUID: " + ownGUID);
            started = true;
            if(count==0) {
                RSockConstants.RSOCK = true;
            }else{
                RSockConstants.RSOCK = false;
            }
            runRsock();

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


    public static boolean stop(){

        //stop rsock if its running.
        if(RSockConstants.RSOCK) {
            RsockReceiveForFileCreation.stop();
            RsockReceiveForFileRetrieval.stop();
            RsockReceiveForFileDeletion.stop();
            RsockReceiveForRsockTest.stop();
        }

        return EKClient.removeService(EdgeKeeperConstants.EdgeKeeper_s);
    }
}
