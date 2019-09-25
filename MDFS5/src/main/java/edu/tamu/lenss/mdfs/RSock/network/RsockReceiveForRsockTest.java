package edu.tamu.lenss.mdfs.RSock.network;

import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import example.Interface;
import example.ReceivedFile;


//this class is used solely totest rsock capability.
public class RsockReceiveForRsockTest implements Runnable{

    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForRsockTest(){}

    @Override
    public void run() {

        //if rsock client library object is null, init it once.
        if(RSockConstants.intrfc_test==null) {
            RSockConstants.intrfc_test = Interface.getInstance(EdgeKeeper.ownGUID, RSockConstants.intrfc_test_appid, 999);
        }

        //variables
        ReceivedFile rcvdfile = null;

        //while loop
        while(!isTerminated){

            //blocking on receiving through rsock at a particular endpoint
            try {
                rcvdfile = RSockConstants.intrfc_test.receive(100, RSockConstants.rsockTestEndpoint); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(rcvdfile!=null) {
                System.out.println("testRsock received string of size: " + new String(rcvdfile.getFileArray()).length());
            }

        }

        //execution only comes here when the above while loop is broken.
        //above while loop is only broken when mdfs is closing.
        //came out of while loop, now close the rsock client library object.
        RSockConstants.intrfc_deletion.close();
        RSockConstants.intrfc_deletion = null;
    }

    public static void stop(){
        isTerminated = true;
    }
}
