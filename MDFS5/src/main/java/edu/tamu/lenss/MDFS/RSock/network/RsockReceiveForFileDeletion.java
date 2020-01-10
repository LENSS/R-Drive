package edu.tamu.lenss.MDFS.RSock.network;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import RsockCommLibrary.Interface;
import RsockCommLibrary.ReceivedFile;

//this class is used for receiving packets that contains file information
// to delete from a  device completely.
public class RsockReceiveForFileDeletion implements Runnable{

    //logger
    static Logger logger = Logger.getLogger(RsockReceiveForFileDeletion.class);


    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForFileDeletion(){}

    @Override
    public void run() {

        //if rsock client library object is null, init it once.
        if(RSockConstants.intrfc_deletion==null) {
            RSockConstants.intrfc_deletion = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_deletion_appid, 3600);
        }

        //variables
        ReceivedFile rcvdfile = null;
        String[] tokens = null;
        String fileInformation;
        String fileName;
        String fileID;


        if(RSockConstants.RSOCK) {

            //while loop
            while (!isTerminated) {

                //blocking on receiving through rsock at a particular endpoint
                try {
                    rcvdfile = RSockConstants.intrfc_deletion.receive();
                } catch (InterruptedException e) {
                    //rsock api closing
                    isTerminated = true;
                }

                if (rcvdfile != null) {

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
                        ServiceHelper.getInstance().getDirectory().deleteFile(fileID, fileName);

                        //log
                        logger.log(Level.ALL, "File " + fileName + " has been deleted from disk.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else{
                    //log
                    logger.log(Level.DEBUG, "Rsock api returned null.");
                }

            }
        }


    }

    public static void stop(){
        isTerminated = true;
    }

}
