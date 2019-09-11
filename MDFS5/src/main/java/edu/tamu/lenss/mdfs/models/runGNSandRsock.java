package edu.tamu.lenss.mdfs.models;

//this class starts GNS and rsock at the beginning when the app starts.

import android.os.Environment;

import org.apache.log4j.Level;

import java.io.IOException;

import edu.tamu.cse.lenss.gnsService.server.GNSServiceUtils;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.GNS.GNSConstants;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileCreation;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileRetrieval;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileDeletion;


public class runGNSandRsock {

    public runGNSandRsock(){

        //log4j used by rsockJavaAPI and GNS
        try { GNSServiceUtils.initLogger(Environment.getExternalStorageDirectory() + "/someLog", Level.ALL); }   //Isagor0!
        catch (IOException e) { System.out.println("Can not create log file probably due to insufficient permission"); }

        //we need gns, so init gns first in GNS.java file
        GNS.getGNSInstance();
        GNS.gnsServiceClient.addService(GNSConstants.GNS_s, GNSConstants.GNS_s1);


        //start file creator rsock in a thread
        Thread t1 = new Thread(new RsockReceiveForFileCreation());
        t1.start();


        // start file retriever rsock in a thread
        Thread t2 = new Thread(new RsockReceiveForFileRetrieval());
        t2.start();

        // start file deletion rsock in a thread
        Thread t3 = new Thread(new RsockReceiveForFileDeletion());
        t3.start();
    }


    public void stopAll(){
        GNS.stop();
        RsockReceiveForFileCreation.stop();
        RsockReceiveForFileRetrieval.stop();
        RsockReceiveForFileDeletion.stop();

    }

}
