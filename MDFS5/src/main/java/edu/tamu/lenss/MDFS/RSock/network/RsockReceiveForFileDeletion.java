package edu.tamu.lenss.MDFS.RSock.network;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import RsockCommLibrary.Interface;
import RsockCommLibrary.ReceivedFile;
import edu.tamu.lenss.MDFS.Utils.Pair;

//this class is used for receiving packets that contains file information
// to delete from a  device completely.
public class RsockReceiveForFileDeletion implements Runnable{

    //logger
    static Logger logger = Logger.getLogger(RsockReceiveForFileDeletion.class);

    //default is false
    public static AtomicBoolean is_running = new AtomicBoolean(false);

    //constructor
    public RsockReceiveForFileDeletion(){}

    @Override
    public void run() {

        //init rsock
        try {
            RSockConstants.intrfc_deletion = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_deletion_appid, 10800);

            //set true
            is_running.set(true);

            //log
            logger.log(Level.ERROR, "Rsock init successful!");

        }catch (Exception e){

            //log
            logger.log(Level.ERROR, "Rsock Exception! API init threw exception, ", e);

            //UI update
            try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED,  RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

            //set false
            is_running.set(false);

            //set rsock false
            RSockConstants.RSOCK = false;

        }

        //check if init succeeded
        if(is_running.get()) {
            try {
                if (!RSockConstants.intrfc_deletion.isConnectedToDaemon()) {

                    //log
                    logger.log(Level.DEBUG, "Rsock Exception! API inti succeeded but Interface.java isConnectedToDaemon() returned false;");

                    //UI update
                    try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED,  RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                    //set false
                    is_running.set(false);

                    //close rsock
                    try { RSockConstants.intrfc_deletion.close(); } catch (Exception e) { e.printStackTrace(); }

                    //set rsock false
                    RSockConstants.RSOCK = false;


                }
            } catch (Exception e) {

                //log
                logger.debug("Rsock Exception! RsockConstants.intrfc.isConnectedToDaemon() ", e);

                //UI update
                try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED, RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                //set false
                is_running.set(false);

                //close rsock
                try { RSockConstants.intrfc_deletion.close(); } catch (Exception ee) { ee.printStackTrace(); }

                //set rsock false
                RSockConstants.RSOCK = false;

            }
        }


        //AT THIS POINT IT IS ASSUMED, THAT RSOCK API HAS BEEN INITIALIZED SUCCESSFULLY.
        //variables
        ReceivedFile rcvdfile = null;
        String[] tokens = null;
        String fileInformation;
        String fileName;
        String fileID;


        if(RSockConstants.RSOCK) {

            //while loop
            while (is_running.get()) {

                //blocking on receiving through rsock at a particular endpoint
                rcvdfile = RSockConstants.intrfc_deletion.receive();

                //check null
                if(rcvdfile!=null) {

                    //check if packet is poison
                    if(rcvdfile.getRsock_daemon_status() == ReceivedFile.RSOCK_DAEMON_STATUS.Disconnected){

                        logger.debug("Application received ReceivedFile to notify it that Rsock daemon has died!!!");

                        if(!RSockConstants.intrfc_deletion.isConnectedToDaemon()) {

                            //close rsock
                            try { RSockConstants.intrfc_deletion.close(); } catch (Exception e) { e.printStackTrace(); }

                            //UI update
                            try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED, RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                            //set false
                            is_running.set(false);

                            //set rsock false
                            RSockConstants.RSOCK = false;

                        }else{

                            //log
                            logger.log(Level.DEBUG, "Rsock Exception! logical error -> Interface.java receive() returned ReceivedFile packet with Disconnected tag but Interface.java isConnectedToDaemon() returned true.");

                            //UI update
                            try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED, RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                            //set false
                            is_running.set(false);

                            //set rsock false
                            RSockConstants.RSOCK = false;
                        }
                    }else{

                        //packet is not null and not poison so, this is good data
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

                    }

                }else{

                    //log
                    logger.log(Level.DEBUG, "Rsock Exception! Interface.java receive() returned null.");

                    //UI update
                    try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED, RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                    //set false
                    is_running.set(false);

                    //set rsock false
                    RSockConstants.RSOCK = false;

                }

            }
        }


    }

    public static void stop(){
        try{RSockConstants.intrfc_deletion.close();}catch (Exception e){logger.debug("Rsock Exception! exception in Interface.java close() function, ", e);}
        if(is_running!=null) { is_running.set(false); }
    }

}
