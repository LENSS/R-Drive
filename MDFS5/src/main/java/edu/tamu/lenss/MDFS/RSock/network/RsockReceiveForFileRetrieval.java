package edu.tamu.lenss.MDFS.RSock.network;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import RsockCommLibrary.Interface;
import RsockCommLibrary.ReceivedFile;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.MDFS.Commands.get.FileMerge;
import edu.tamu.lenss.MDFS.Commands.get.get;
import edu.tamu.lenss.MDFS.Commands.get.getUtils;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Model.Fragment;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileRetrieve;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import edu.tamu.lenss.MDFS.Utils.Pair;

import static java.lang.Thread.sleep;

//this class is used for block retrieval via rsock.
//this class receives fragment requests and send the fragment to the requestor.
public class RsockReceiveForFileRetrieval implements Runnable {


    //log
    public static Logger logger = Logger.getLogger(RsockReceiveForFileRetrieval.class);

    private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";

    //default is false
    public static AtomicBoolean is_running = new AtomicBoolean(false);

    //constructor
    public RsockReceiveForFileRetrieval() {}

    @Override
    public void run() {


        //init rsock
        try {
            RSockConstants.intrfc_retrieval = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_retrieval_appid, 10800);

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
                if (!RSockConstants.intrfc_retrieval.isConnectedToDaemon()) {

                    //log
                    logger.log(Level.DEBUG, "Rsock Exception! API inti succeeded but Interface.java isConnectedToDaemon() returned false;");

                    //UI update
                    try { IOUtilities.miscellaneousWorks.put(new Pair(Constants.RSOCK_CLOSED,  RSockConstants.Rsock_Library_Closed_Message)); } catch (InterruptedException e1) { e1.printStackTrace(); }

                    //set false
                    is_running.set(false);

                    //close rsock
                    try { RSockConstants.intrfc_retrieval.close(); } catch (Exception e) { e.printStackTrace(); }

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
                try { RSockConstants.intrfc_retrieval.close(); } catch (Exception ee) { ee.printStackTrace(); }

                //set rsock false
                RSockConstants.RSOCK = false;

            }
        }


        //AT THIS POINT IT IS ASSUMED, THAT RSOCK API HAS BEEN INITIALIZED SUCCESSFULLY.
        //variables
        boolean classConversion = false;
        ReceivedFile rcvdfile = null;
        byte[] byteArray = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        File tmp0 = null;
        boolean success = false;
        MDFSFragmentForFileRetrieve mdfsfrag = null;


        if(RSockConstants.RSOCK) {

            while (is_running.get()) {

                //clear all variables for next iteration
                classConversion = false;
                byteArray = null;
                rcvdfile = null;
                bis = null;
                ois = null;
                tmp0 = null;
                success = false;
                mdfsfrag = null;

                //blocking on receiving through rsock at a particular endpoint
                rcvdfile = RSockConstants.intrfc_retrieval.receive();

                //check null
                if(rcvdfile!=null){

                    //check if packet is poison
                    if(rcvdfile.getRsock_daemon_status() == ReceivedFile.RSOCK_DAEMON_STATUS.Disconnected){

                        logger.debug("Application received ReceivedFile to notify it that Rsock daemon has died!!!");

                        if(!RSockConstants.intrfc_retrieval.isConnectedToDaemon()) {

                            //close rsock
                            try { RSockConstants.intrfc_retrieval.close(); } catch (Exception e) { e.printStackTrace(); }

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
                        //create mdfsfragRetrieval object from raw byteArray
                        mdfsfrag = IOUtilities.bytesToObject(rcvdfile.getFileArray(), MDFSFragmentForFileRetrieve.class);
                        if(mdfsfrag!=null){classConversion=true;}

                        //if bytes to class conversion succeeded
                        if (classConversion) {

                            //if the other party is asking for a fragment.
                            //then, we send the fragment if we have it.
                            //requester can be both in same edge or diff edge.
                            if (mdfsfrag.type == MDFSFragmentForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment) {

                                //its a request for fetching a particular file fragment.
                                getUtils.justDoit(mdfsfrag);
                            }

                            //this type of packet means this node is receiving a fragment of a file.
                            //this reply can come from either same edge or diff edge.
                            else if (mdfsfrag.type == MDFSFragmentForFileRetrieve.Type.ReplyFromOneClientToAnotherForOneFragment) {

                                //check if the received file fragment is not null
                                if (mdfsfrag.fileFrag != null) {

                                    //check if block directory exists
                                    File fragsDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(mdfsfrag.fileName, mdfsfrag.fileId, mdfsfrag.blockIdx));
                                    if(!fragsDir.exists()){
                                        fragsDir.mkdirs();
                                    }

                                    //create a Fragment object
                                    Fragment fr = new Fragment(mdfsfrag.fileName, mdfsfrag.filePathMDFS, mdfsfrag.fileFrag, mdfsfrag.fileId, mdfsfrag.entireFileSize, mdfsfrag.n2, mdfsfrag.k2, mdfsfrag.blockIdx, mdfsfrag.fragmentIndex, mdfsfrag.totalNumOfBlocks);

                                    //convert Fragment object into byteArray
                                    byte[] frArray = IOUtilities.objectToByteArray(fr);

                                    //save the shard in own device.
                                    IOUtilities.byteToFile_FOS_write(frArray, fragsDir , MDFSFileInfo.getFragName(mdfsfrag.fileName, mdfsfrag.blockIdx, mdfsfrag.fragmentIndex));

                                    //log
                                    logger.log(Level.ALL, "Received fragment# " + mdfsfrag.fragmentIndex + " of block# " + mdfsfrag.blockIdx + " of filename " + mdfsfrag.fileName + " from node " + mdfsfrag.srcGUID);

                                    //after receiving each fragment, we check enough fragments are available for this block.
                                    //check if enough fragment has received for this block
                                    //make a list for block_fragments I dont have
                                    int[][] missingBlocksAndFrags = new int[mdfsfrag.totalNumOfBlocks][];

                                    //get/populate missingBlocksAndFrags array for the fragments of this block
                                    boolean succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, mdfsfrag.fileName, mdfsfrag.fileId, mdfsfrag.n2, mdfsfrag.blockIdx);

                                    //check if this node has already K frags for each block
                                    if (succeeded && missingBlocksAndFrags[mdfsfrag.blockIdx] != null) {

                                        //check if at least K fragments are available for this block
                                        int k = mdfsfrag.k2;
                                        for (int i = 0; i < missingBlocksAndFrags[mdfsfrag.blockIdx].length; i++) {
                                            if (missingBlocksAndFrags[mdfsfrag.blockIdx][i] == 0) {
                                                k--;
                                            }
                                        }

                                        //check if k is <= than 0
                                        if (k <= 0) {

                                            //enough fragments for this block is available,
                                            //then we try to check if enough fragments for all blocks are available
                                            missingBlocksAndFrags = new int[mdfsfrag.totalNumOfBlocks][mdfsfrag.n2];

                                            //get/populate missingBlocksAndFrags array for each block
                                            succeeded = false;
                                            for (int i = 0; i < mdfsfrag.totalNumOfBlocks; i++) {
                                                succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, mdfsfrag.fileName, mdfsfrag.fileId, mdfsfrag.n2, i);
                                            }

                                            if (succeeded) {
                                                //check if this node has already K frags for each block
                                                if (getUtils.checkEnoughFragsAvailableForWholeFile(missingBlocksAndFrags, mdfsfrag.k2)) {

                                                    logger.log(Level.ALL, "RRFFR File " + mdfsfrag.fileName + " has enough fragments.");

                                                    //merge the file in a new thread
                                                    new Thread(new FileMerge(mdfsfrag)).run();
                                                } else {
                                                    logger.log(Level.ALL, "RRFFR File " + mdfsfrag.fileName + " doesnt have enough fragments yet.");
                                                }
                                            } else {
                                                get.logger.log(Level.DEBUG, "Merge failed for filename: " + mdfsfrag.fileName);
                                            }

                                        } else {
                                            //for this block not enough fragments are present.
                                            //so, we wont be able to recreate this block.
                                            //so there is no use to check if other blocks have enough fragments or nah.
                                        }

                                    }

                                }
                            } else if (mdfsfrag.type == MDFSFragmentForFileRetrieve.Type.RequestFromOneClientInOneAEdgeToMasterOfAnotherEdgeForWholeFile) {
                                //receiving this request means This guy is currently the master of this edge.
                                //first fetch and check if the file metadata exists.
                                //then check if file creator is me. if me, send the fragments immediately,
                                // if not, make RequestFromMasterToClientInSameEdge packet and send to original file creator.

                                //log
                                logger.log(Level.ALL, "This node received a file retrierval request from GUID " + mdfsfrag.srcGUID + " of another edge for " + mdfsfrag.filePathMDFS + mdfsfrag.fileName + " file");


                                //first retrieve the metadata from edgeKeeper
                                MDFSMetadata metadata = get.fetchFileMetadataFromEdgeKeeper(mdfsfrag.filePathMDFS + mdfsfrag.fileName);

                                //check for null
                                if (metadata != null) {

                                    //logger
                                    logger.log(Level.ALL, "Command:get | log: Fetched file metadata for filaname " + metadata.getFileName() + " of fileID " + metadata.getFileID());

                                    //check I am the original creator of this file
                                    if (metadata.getFileCreatorGUID().equals(EdgeKeeper.ownGUID)) {

                                        //I am the original creator of this file,
                                        //that means, I should have all the fragments,
                                        //no need to send request for fragments to other nodes.
                                        //check by filename__fileID if I have the file fragments in MDFS directory in disk.
                                        File f = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(metadata.getFileName(), metadata.getFileID())); //Isagor0!

                                        //check
                                        if (f != null && f.exists() && f.isDirectory()) {

                                            //file folder exists
                                            for (int i = 0; i < metadata.getBlockCount(); i++) {
                                                for (int j = 0; j < metadata.getn2(); j++) {

                                                    //create dummy mdfsfragForFileRetrieve of type == RequestFromOneClientToAnotherForOneFragment,
                                                    // and send request to own self.
                                                    //source = file requester from diff edge
                                                    //destination = ownGUID
                                                    MDFSFragmentForFileRetrieve mdfsfrag1 = new MDFSFragmentForFileRetrieve(mdfsfrag.blockRetrieveReqUUID, MDFSFragmentForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment, metadata.getn2(), metadata.getk2(), mdfsfrag.srcGUID, EdgeKeeper.ownName, mdfsfrag.fileName, metadata.getFilePathMDFS(), metadata.getFileID(), metadata.getBlockCount(), i, j, mdfsfrag.outputDir, null, 0, false);

                                                    //get each fragment of each block and send
                                                    getUtils.justDoit(mdfsfrag1);

                                                    logger.log(Level.ALL, "CASE #1: master served the file request " + mdfsfrag.filePathMDFS + mdfsfrag.fileName + " from a neighbor edge.");

                                                }
                                            }
                                            logger.log(Level.ALL, "CASE #1:master sent all file fragments for file " + mdfsfrag.filePathMDFS + mdfsfrag.fileName + " to a neighbor edge.");

                                        } else {
                                            logger.log(Level.DEBUG, "Could not serve fragment request from node " + mdfsfrag.srcGUID + " for filename " + mdfsfrag.fileName + "due to file directory no longer exists in storage.");
                                        }

                                    } else {

                                        //I am not the original creator of this file.
                                        //need to send request to the fileCreator OR, each nodes who has the file fragments
                                        //send the fragment requests to the fileCreatorGUID.
                                        //source: file requester from diff edge
                                        //destination: file creator.
                                        MDFSFragmentForFileRetrieve mdfsfrag1 = new MDFSFragmentForFileRetrieve(mdfsfrag.blockRetrieveReqUUID, MDFSFragmentForFileRetrieve.Type.RequestFromMasterToClientInSameEdgeForWholeFile, metadata.getn2(), metadata.getk2(), mdfsfrag.srcGUID, metadata.getFileCreatorGUID(), mdfsfrag.fileName, mdfsfrag.filePathMDFS, metadata.getFileID(), metadata.getBlockCount(), -1, -1, mdfsfrag.outputDir, null, mdfsfrag.entireFileSize, false);

                                        //get byteArray from object
                                        byte[] data = IOUtilities.objectToByteArray(mdfsfrag1);

                                        //send request
                                        if (data != null) {
                                            String uuid = UUID.randomUUID().toString().substring(0, 12);
                                            boolean sent = false;
                                            if (RSockConstants.RSOCK) {
                                                sent = RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", metadata.getFileCreatorGUID());
                                            }

                                            if (sent) {
                                                //log
                                                logger.log(Level.ALL, "CASE #2: master forwarded a file req from another edge for file " + mdfsfrag.filePathMDFS + mdfsfrag.fileName + " to " + metadata.getFileCreatorGUID());


                                            } else {

                                                //log
                                                logger.log(Level.DEBUG, "CASE #2: master failed to forwarde a file req from another edge for file " + mdfsfrag.filePathMDFS + mdfsfrag.fileName + " to " + metadata.getFileCreatorGUID());
                                            }
                                        }
                                    }

                                } else {

                                    //log
                                    logger.log(Level.ERROR, "Could not fetch file metadata from local EdgeKeeper for filename " + mdfsfrag.fileName);

                                }
                            } else if (mdfsfrag.type == MDFSFragmentForFileRetrieve.Type.RequestFromMasterToClientInSameEdgeForWholeFile) {

                                logger.log(Level.ALL, "CASE #3: filecreator received a request from master to sent all file fragments for " + mdfsfrag.fileName + " to a node in diff edge.");

                                //master of this edge sent a request to this node (the original filecreater) to resolve a file request from diff edge.
                                //make dummy several RequestFromOneClientToAnotherForOneFragment packets and feed to itself.
                                //source: file requester from diff edge
                                //destination: ownGUID
                                for (int i = 0; i < mdfsfrag.totalNumOfBlocks; i++) {
                                    for (int j = 0; j < mdfsfrag.n2; j++) {

                                        //create dummy mdfsfragForFileRetrieve of type == RequestFromOneClientToAnotherForOneFragment,
                                        //source = file requester from diff edge
                                        //destination = original fileCreator aka ownGUID.
                                        MDFSFragmentForFileRetrieve mdfsfrag1 = new MDFSFragmentForFileRetrieve(mdfsfrag.blockRetrieveReqUUID, MDFSFragmentForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment, mdfsfrag.n2, mdfsfrag.k2, mdfsfrag.srcGUID, EdgeKeeper.ownGUID, mdfsfrag.fileName, mdfsfrag.filePathMDFS, mdfsfrag.fileId, mdfsfrag.totalNumOfBlocks, i, j, mdfsfrag.outputDir, null, mdfsfrag.entireFileSize, false);

                                        //get each fragment of each block and send
                                        getUtils.justDoit(mdfsfrag1);

                                    }
                                }

                                logger.log(Level.ALL, "CASE #3: filecreator sent all file fragments for " + mdfsfrag.fileName + " to a node in diff edge.");
                            } else {
                                //this request is not implemented
                                logger.log(Level.DEBUG, "MDFS received an unknown file retrieval request.");
                            }
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

            }//while

        }

    }

    public static void stop(){
        try{RSockConstants.intrfc_retrieval.close();}catch (Exception e){logger.debug("Rsock Exception! exception in Interface.java close() function, ", e);}
        if(is_running!=null) { is_running.set(false); }
    }
}

