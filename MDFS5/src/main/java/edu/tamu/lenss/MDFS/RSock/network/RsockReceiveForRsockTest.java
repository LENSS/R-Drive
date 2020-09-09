package edu.tamu.lenss.MDFS.RSock.network;

import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import RsockCommLibrary.Interface;
import RsockCommLibrary.ReceivedFile;


//this class is used solely totest rsock capability.
public class RsockReceiveForRsockTest implements Runnable{

    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForRsockTest(){}

    @Override
    public void run() {

        //if rsock client library object is null, init it once.
        if(RSockConstants.intrfc_test==null) {
            RSockConstants.intrfc_test = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_test_appid, 999);
        }

        //variables
        ReceivedFile rcvdfile = null;

        //while loop
        while(!isTerminated){

            //blocking on receiving through rsock at a particular endpoint
            try {
                rcvdfile = RSockConstants.intrfc_test.receive(); }
            catch (Exception e) {
                //rsock api threw exception.
                //meaning, Rsock api closed down.
                //need to come out of this thread
                isTerminated = true;
            }

            if(rcvdfile!=null) {
                System.out.println("testRsock received string of size: " + new String(rcvdfile.getFileArray()).length());
            }else{
                System.out.println("testRsock received null.");
            }

        }
    }

    public static void stop(){
        isTerminated = true;
    }
}
