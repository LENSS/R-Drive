package edu.tamu.lenss.mdfs.models;

//this class starts GNS and rsock at the beginning when the app starts.

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileCreation;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileRetrieval;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileDeletion;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForRsockTest;


public class runGNSandRsock {

    public runGNSandRsock(){

        //we need EdgeKeeper, so init EdgeKeeper first
        new EdgeKeeper();

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
        Thread t4 = new Thread(new RsockReceiveForRsockTest());
        t4.start();
    }


    public void stopAll(){
        EdgeKeeper.stop();
        RsockReceiveForFileCreation.stop();
        RsockReceiveForFileRetrieval.stop();
        RsockReceiveForFileDeletion.stop();
        RsockReceiveForRsockTest.stop();

    }

}
