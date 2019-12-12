package edu.tamu.cse.lenss.MDFS5.RSock.network;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import RsockJavaLibrary.example.Interface;
import RsockJavaLibrary.example.ReceivedFile;
import edu.tamu.cse.lenss.MDFS5.EdgeKeeper.EdgeKeeper;
import edu.tamu.cse.lenss.MDFS5.Handler.ServiceHelper;
import edu.tamu.cse.lenss.MDFS5.RSock.RSockConstants;
import edu.tamu.cse.lenss.MDFS5.Utils.IOUtilities;

//this class is used for receiving packets that contains file information
// to delete from a  device completely.
public class RsockReceiveForFileDeletion implements Runnable{

    //logger
    public static Logger logger = Logger.getLogger(RsockReceiveForFileDeletion.class);


    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForFileDeletion(){}

    @Override
    public void run() {

        //if rsock client library object is null, init it once.
        if(RSockConstants.intrfc_deletion==null) {
            RSockConstants.intrfc_deletion = Interface.getInstance(EdgeKeeper.ownGUID, RSockConstants.intrfc_deletion_appid, 3600, false);
        }

        //variables
        ReceivedFile rcvdfile = null;
        String[] tokens = null;
        String fileInformation;
        String fileName;
        String fileID;

        //while loop
        while(!isTerminated){

            //blocking on receiving through rsock at a particular endpoint
            try {
                rcvdfile = RSockConstants.intrfc_deletion.receive(100, RSockConstants.fileDeleteEndpoint); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(rcvdfile!=null) {

                System.out.println("new incoming rsock for file deletion");

                //get file information and parse
                fileInformation = new String(rcvdfile.getFileArray());
                tokens = fileInformation.split(RSockConstants.deletion_tag);
                tokens = IOUtilities.delEmptyStr(tokens);
                fileName = tokens[0];
                fileID = tokens[1];

                //log
                logger.log(Level.ALL, "Received file deletion request for file " + fileName);

                //trigger delete on this device
                try {
                    ServiceHelper.fetchInstance().getDirectory().deleteFile(fileID, fileName);

                    //log
                    logger.log(Level.ALL, "File " + fileName + " has been deleted from disk.");
                }catch(Exception e){
                    e.printStackTrace();
                }
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
