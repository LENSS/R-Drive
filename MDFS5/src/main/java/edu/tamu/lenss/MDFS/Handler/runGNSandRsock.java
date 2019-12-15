package edu.tamu.lenss.MDFS.Handler;

//this class starts GNS and rsock at the beginning when the app starts.

import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileCreation;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileRetrieval;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileDeletion;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForRsockTest;


public class runGNSandRsock {

    public runGNSandRsock(){

        //we need EdgeKeeper, so init EdgeKeeper first
        new EdgeKeeper();

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


    public void stopAll(){
        //stop edgeKeeper
        EdgeKeeper.stop();

        //stop rsock if its running.
        if(RSockConstants.RSOCK) {
            RsockReceiveForFileCreation.stop();
            RsockReceiveForFileRetrieval.stop();
            RsockReceiveForFileDeletion.stop();
            RsockReceiveForRsockTest.stop();
        }

    }

}
