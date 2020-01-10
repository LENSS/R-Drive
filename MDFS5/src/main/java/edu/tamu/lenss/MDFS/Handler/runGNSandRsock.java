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

    }


    public void stopAll(){
        //stop edgeKeeper
        EdgeKeeper.stop();
    }

}
