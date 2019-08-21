package edu.tamu.lenss.mdfs.models;

//this class starts GNS and rsock at the beginning when the app starts.

import android.os.Environment;

import org.apache.log4j.Level;

import java.io.IOException;

import edu.tamu.cse.lenss.gnsService.server.GNSServiceUtils;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.GNS.GNSConstants;
import edu.tamu.lenss.mdfs.network.RsockReceiveForFileCreation;
import edu.tamu.lenss.mdfs.network.RsockReceiveForFileRetrieval;

public class runGNSandRsock {

    public runGNSandRsock(){

        //log4j used by rsockJavaAPI and GNS
        try { GNSServiceUtils.initLogger(Environment.getExternalStorageDirectory() + "/someLog", Level.ALL); }  //Isagor0!
        catch (IOException e) { System.out.println("Can not create log file probably due to insufficient permission"); }

        //if file creation or retrieval is via rsock, then we need gns, so init gns first in GNS.java file
        if(Constants.file_creation_via_rsock_or_tcp.equals("rsock") || Constants.file_retrieval_via_rsock_or_tcp.equals("rsock")) {
            GNS.getGNSInstance();
            GNS.gnsServiceClient.addService(GNSConstants.GNS_s, GNSConstants.GNS_s1);
        }

        //if file creation via rsock is enabled in Constants file, start rsock in a thread
        if(Constants.file_creation_via_rsock_or_tcp.equals("rsock")){
            Thread t1 = new Thread(new RsockReceiveForFileCreation());
            t1.start();
        }

        //if file retrieval via rsock is enabled in Constants file, start rsock in a thread
        if(Constants.file_retrieval_via_rsock_or_tcp.equals("rsock")){
            Thread t1 = new Thread(new RsockReceiveForFileRetrieval());
            t1.start();
        }

    }


    public void stopAll(){
        GNS.stop();
        RsockReceiveForFileCreation.stop();
        RsockReceiveForFileRetrieval.stop();

    }

}
