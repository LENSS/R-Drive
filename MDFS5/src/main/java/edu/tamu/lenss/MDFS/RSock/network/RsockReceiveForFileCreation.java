package edu.tamu.lenss.MDFS.RSock.network;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import RsockCommLibrary.Interface;
import RsockCommLibrary.ReceivedFile;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileCreate;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import edu.tamu.lenss.MDFS.Utils.Pair;

//this class is used for receiving packets for creating a file in MDFS.
//this is the entry point of a file fragment to enter this node.
//this class receives a packet from rsock cilent library(rsock java api),
//and saves it.
public class RsockReceiveForFileCreation implements Runnable{


    //logger
    public static Logger logger = Logger.getLogger(RsockReceiveForFileCreation.class);

    private static final long serialVersionUID = 88L;

    //default is false
    public static AtomicBoolean is_running = new AtomicBoolean(false);

    //constructor
    public RsockReceiveForFileCreation(){

    }


    @Override
    public void run() {

        //init rsock
        try {
            RSockConstants.intrfc_creation = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_creation_appid, 10800);

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
                if (!RSockConstants.intrfc_creation.isConnectedToDaemon()) {

                    //log
                    logger.log(Level.DEBUG, "Rsock Exception! API inti succeeded but Interface.java isConnectedToDaemon() returned false;");

                    //UI update
                    try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED,  RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                    //set false
                    is_running.set(false);

                    //close rsock
                    try { RSockConstants.intrfc_creation.close(); } catch (Exception e) { e.printStackTrace(); }

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
                try { RSockConstants.intrfc_creation.close(); } catch (Exception ee) { ee.printStackTrace(); }

                //set rsock false
                RSockConstants.RSOCK = false;

            }
        }

        //AT THIS POINT IT IS ASSUMED, THAT RSOCK API HAS BEEN INITIALIZED SUCCESSFULLY.
        //variables
        ReceivedFile rcvdfile = null;

        if(RSockConstants.RSOCK) {


            while (is_running.get()) {

                //blocking on receiving through rsock at a particular endpoint
                rcvdfile = RSockConstants.intrfc_creation.receive();

                //check null
                if(rcvdfile!=null){


                    //check if packet is poison
                    if(rcvdfile.getRsock_daemon_status() == ReceivedFile.RSOCK_DAEMON_STATUS.Disconnected){

                        logger.debug("Application received ReceivedFile to notify it that Rsock daemon has died!!!");

                        if(!RSockConstants.intrfc_creation.isConnectedToDaemon()) {

                            //close rsock
                            try { RSockConstants.intrfc_creation.close(); } catch (Exception e) { e.printStackTrace(); }

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
                        //convert rcvdfile.getFileArray() into MDFSFragmentForFileCreate object
                        MDFSFragmentForFileCreate mdfsfrag = IOUtilities.bytesToObject(rcvdfile.getFileArray(), MDFSFragmentForFileCreate.class);

                        //parse data out of mdfsfrag object
                        String fileName = (String) mdfsfrag.fileName;
                        byte[] byteArray = (byte[]) mdfsfrag.fileFrag;
                        String fileID = (String) mdfsfrag.fileID;
                        int blockIdx = (int) mdfsfrag.blockIdx;
                        int fragmentIdx = (int) mdfsfrag.fragmentIdx;

                        //now save the fileFrag
                        saveTheFileFrag(fileName, byteArray, fileID, blockIdx, fragmentIdx);
                        logger.log(Level.DEBUG, "fragment# " + fragmentIdx + " of block# " + blockIdx + " of filename " + fileName + " received from rsock.");

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

    //this function saves the filefrag locally in this device.
    //NOte, this byteArray is the file fragment.
    private void saveTheFileFrag(String fileName, byte[] byteArray, String fileID , int blockIdx, int fragmentIdx) {

        //create a directory where all fragments reside.
        ///storage/emulated/0/MDFS/test.jpg_0123/test.jpg_0/ (directory)
        File fragsDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileID, blockIdx));
        if(!fragsDir.exists()){
            fragsDir.mkdirs();
        }

        //save the shard in own device.
        IOUtilities.byteToFile_FOS_write(byteArray, fragsDir , MDFSFileInfo.getFragName(fileName, blockIdx, fragmentIdx));

    }

    public static void stop(){
        try{RSockConstants.intrfc_creation.close();}catch (Exception e){logger.debug("Rsock Exception! exception in Interface.java close() function, ", e);}
        if(is_running!=null) { is_running.set(false); }
    }

}
