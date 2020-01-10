package edu.tamu.lenss.MDFS.RSock.network;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.MDFS.Commands.get.get;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Commands.get.FileMerge;
import edu.tamu.lenss.MDFS.Commands.get.MDFSFileRetrieverViaRsock;
import edu.tamu.lenss.MDFS.Commands.get.getUtils;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSRsockBlockForFileRetrieve;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import RsockCommLibrary.*;

import static java.lang.Thread.sleep;


//this class is used for block retrieval via rsock.
//this class receives fragment requests and send the fragment to the requestor.
public class RsockReceiveForFileRetrieval implements Runnable {


    //log
    public static Logger logger = Logger.getLogger(RsockReceiveForFileRetrieval.class);

    private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";
    private static boolean isRunning = true;

    public RsockReceiveForFileRetrieval() {}

    @Override
    public void run() {


        //create rsock Interface intrfc
        if(RSockConstants.intrfc_retrieval==null) {
            RSockConstants.intrfc_retrieval = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_retrieval_appid, 3600);
        }

        //variable
        boolean classConversion = false;
        ReceivedFile receivedFile = null;
        byte[] byteArray = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        File tmp0 = null;
        boolean success = false;
        MDFSRsockBlockForFileRetrieve mdfsrsockblock = null;


        if(RSockConstants.RSOCK) {
            while (isRunning) {

                //clear all variables for next iteration
                classConversion = false;
                byteArray = null;
                receivedFile = null;
                bis = null;
                ois = null;
                tmp0 = null;
                success = false;
                mdfsrsockblock = null;

                //blocking on receiving through rsock at a particular endpoint
                try {
                    receivedFile = RSockConstants.intrfc_retrieval.receive();
                } catch (InterruptedException e) {
                    //rosck api returned null and closing
                    isRunning = false;
                }

                //if unblocked, check if received something
                if (receivedFile != null) {

                    try {
                        //create MDFSRsockBlockRetrieval object from raw byteArray
                        bis = new ByteArrayInputStream(receivedFile.getFileArray());
                        ois = new ObjectInputStream(bis);
                        mdfsrsockblock = (MDFSRsockBlockForFileRetrieve) ois.readObject();
                        bis.close();
                        ois.close();
                        classConversion = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.log(Level.ERROR, "Failed to convert bytes into MDFSRsockBlockForFileRetrieve class. ", e);
                        classConversion = false;
                    }

                    //if bytes to class conversion succeeded
                    if (classConversion) {

                        //get type variable inside mdfsrsockblock
                        MDFSRsockBlockForFileRetrieve.Type type = (MDFSRsockBlockForFileRetrieve.Type) mdfsrsockblock.type;

                        //if the other party is asking for a fragment.
                        //then, we send the fragment if we have it.
                        //requester can be both in same edge or diff edge.
                        if (type == MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment) {

                            //its a request for fetching a particular file fragment.
                            getUtils.justDoit(mdfsrsockblock);
                        }

                        //this type of packet means this node is receiving a fragment for a file.
                        //this reply can come from either same edge or diff edge.
                        else if (type == MDFSRsockBlockForFileRetrieve.Type.ReplyFromOneClientToAnotherForOneFragment) {

                            //check if the received file fragment is not null
                            if (mdfsrsockblock.fileFrag != null) {

                                //check if file directory exists
                                File fileDIr = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId));

                                //check if file directory exists
                                if (!fileDIr.exists()) {

                                    //create file directory
                                    if (fileDIr.mkdirs()) {
                                        System.out.println("created file directory coz it didnt exist");
                                    } else {

                                        //log
                                        logger.log(Level.DEBUG, "Could not create file directory for storing fragment for file " + mdfsrsockblock.fileName);
                                    }

                                }


                                //check if block directory exists
                                File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx));
                                if (!blockDir.exists()) {

                                    //create block directory
                                    System.out.println("creating block directory for block# " + mdfsrsockblock.blockIdx);
                                    if (blockDir.mkdirs()) {
                                        System.out.println("created block directory.");
                                    } else {
                                        System.out.println("couldnt create block directory.");

                                        //log
                                        logger.log(Level.DEBUG, "Could not create block directory for storing fragment for file " + mdfsrsockblock.fileName);
                                    }

                                }


                                //create fileFrag from byteArray and write on disk
                                ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0_dOwN__lOaDiNg___   (file)
                                File tmp1 = null;
                                try {
                                    tmp1 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex) + DOWNLOADING_SIGNATURE);
                                    FileOutputStream outputStream = new FileOutputStream(tmp1);
                                    outputStream.write(mdfsrsockblock.fileFrag);
                                    outputStream.flush();
                                    outputStream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


                                //log
                                logger.log(Level.ALL, "Received fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName + " from node " + mdfsrsockblock.srcGUID);

                                // Rename the fragment without the DOWNLOADING_SIGNATURE
                                ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0 (file)
                                success = IOUtilities.renameFile(tmp1, MDFSFileInfo.getFragName(mdfsrsockblock.fileName, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex));

                                ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0 (file)
                                tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex));
                                try {
                                    sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                //log
                                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Finish downloading fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName + " from node " + mdfsrsockblock.srcGUID);

                                //after receiving each fragment, we check enough fragments are available for this block.
                                //check if enough fragment has received for this block
                                //make a list for block_fragments I dont have
                                int[][] missingBlocksAndFrags = new int[mdfsrsockblock.totalNumOfBlocks][];

                                //get/populate missingBlocksAndFrags array for the fragments of this block
                                boolean succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.n2, mdfsrsockblock.blockIdx);

                                //check if this node has already K frags for each block
                                if (succeeded && missingBlocksAndFrags[mdfsrsockblock.blockIdx] != null) {

                                    //check if at least K fragments are available for this block
                                    int k = mdfsrsockblock.k2;
                                    for (int i = 0; i < missingBlocksAndFrags[mdfsrsockblock.blockIdx].length; i++) {
                                        if (missingBlocksAndFrags[mdfsrsockblock.blockIdx][i] == 0) {
                                            k--;
                                        }
                                    }

                                    //check if k is <= than 0
                                    if (k <= 0) {

                                        //enough fragments for this block is available,
                                        //then we try to check if enough fragments for all blocks are available
                                        missingBlocksAndFrags = new int[mdfsrsockblock.totalNumOfBlocks][mdfsrsockblock.n2];

                                        //get/populate missingBlocksAndFrags array for each block
                                        succeeded = false;
                                        for (int i = 0; i < mdfsrsockblock.totalNumOfBlocks; i++) {
                                            succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.n2, i);
                                        }

                                        if (succeeded) {
                                            //check if this node has already K frags for each block
                                            if (getUtils.checkEnoughFragsAvailable(missingBlocksAndFrags, mdfsrsockblock.k2)) {

                                                logger.log(Level.ALL, "RRFFR File " + mdfsrsockblock.fileName + " has enough fragments.");

                                                //merge the file in a new thread
                                                new Thread(new FileMerge(mdfsrsockblock)).run();
                                            } else {
                                                logger.log(Level.ALL, "RRFFR File " + mdfsrsockblock.fileName + " doesnt have enough fragments yet.");
                                            }
                                        } else {
                                            MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Merge failed for filename: " + mdfsrsockblock.fileName);
                                        }

                                    } else {
                                        //for this block not enough fragments are present.
                                        //so, we wont be able to recreate this block.
                                        //so there is no use to check if other blocks have enough fragments or nah.
                                    }

                                }

                            }
                        } else if (type == MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientInOneAEdgeToMasterOfAnotherEdgeForWholeFile) {
                            //receiving this request means This guy is currently the master of this edge.
                            //first fetch and check if the file metadata exists.
                            //then check if file creator is me. if me, send the fragments immediately,
                            // if not, make RequestFromMasterToClientInSameEdge packet and send to original file creator.

                            //log
                            logger.log(Level.ALL, "This node received a file retrierval request from GUID " + mdfsrsockblock.srcGUID + " of another edge for " + mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName + " file");


                            //first retrieve the metadata from edgeKeeper
                            MDFSMetadata metadata = get.fetchFileMetadataFromEdgeKeeper(mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName);

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
                                                //create dummy MDFSRsockBlockForFileRetrieve of type == RequestFromOneClientToAnotherForOneFragment,
                                                // and send request to own self.
                                                //source = file requester from diff edge
                                                //destination = ownGUID
                                                MDFSRsockBlockForFileRetrieve mdfsrsockblock1 = new MDFSRsockBlockForFileRetrieve(mdfsrsockblock.blockRetrieveReqUUID, MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment, (byte) metadata.getn2(), (byte) metadata.getk2(), mdfsrsockblock.srcGUID, EdgeKeeper.ownName, mdfsrsockblock.fileName, metadata.getFilePathMDFS(), metadata.getFileID(), (byte) metadata.getBlockCount(), (byte) i, (byte) j, mdfsrsockblock.localDir, null, false);

                                                //get each fragment of each block and send
                                                getUtils.justDoit(mdfsrsockblock1);

                                                logger.log(Level.ALL, "CASE #1: master served the file request " + mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName + " from a neighbor edge.");

                                            }
                                        }
                                        logger.log(Level.ALL, "CASE #1:master sent all file fragments for file " + mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName + " to a neighbor edge.");

                                    } else {
                                        logger.log(Level.DEBUG, "Could not serve fragment request from node " + mdfsrsockblock.srcGUID + " for filename " + mdfsrsockblock.fileName + "due to file directory no longer exists in storage.");
                                    }

                                } else {

                                    //I am not the original creator of this file.
                                    //need to send request to the fileCreator OR, each nodes who has the file fragments
                                    //send the fragment requests to the fileCreatorGUID.
                                    //source: file requester from diff edge
                                    //destination: file creator.
                                    MDFSRsockBlockForFileRetrieve mdfsrsockblock1 = new MDFSRsockBlockForFileRetrieve(mdfsrsockblock.blockRetrieveReqUUID, MDFSRsockBlockForFileRetrieve.Type.RequestFromMasterToClientInSameEdgeForWholeFile, (byte) metadata.getn2(), (byte) metadata.getk2(), mdfsrsockblock.srcGUID, metadata.getFileCreatorGUID(), mdfsrsockblock.fileName, mdfsrsockblock.filePathMDFS, metadata.getFileID(), (byte) metadata.getBlockCount(), (byte) -1, (byte) -1, mdfsrsockblock.localDir, null, false);

                                    //get byteArray from object
                                    byte[] data = null;
                                    try {
                                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                        ObjectOutputStream oos = null;
                                        oos = new ObjectOutputStream(bos);
                                        oos.writeObject(mdfsrsockblock1);
                                        oos.flush();
                                        data = bos.toByteArray();
                                    } catch (Exception e) {
                                        logger.log(Level.DEBUG, "could not convert object into bytes.");
                                    }

                                    //send request
                                    if (data != null) {
                                        String uuid = UUID.randomUUID().toString().substring(0, 12);
                                        boolean sent = false;
                                        if (RSockConstants.RSOCK) {
                                            sent = RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", metadata.getFileCreatorGUID(), 0);
                                        }

                                        if (sent) {
                                            //log
                                            logger.log(Level.ALL, "CASE #2: master forwarded a file req from another edge for file " + mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName + " to " + metadata.getFileCreatorGUID());


                                        } else {

                                            //log
                                            logger.log(Level.DEBUG, "CASE #2: master failed to forwarde a file req from another edge for file " + mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName + " to " + metadata.getFileCreatorGUID());
                                        }
                                    }
                                }

                            } else {

                                //log
                                logger.log(Level.ERROR, "Could not fetch file metadata from local EdgeKeeper for filename " + mdfsrsockblock.fileName);

                            }
                        } else if (type == MDFSRsockBlockForFileRetrieve.Type.RequestFromMasterToClientInSameEdgeForWholeFile) {

                            logger.log(Level.ALL, "CASE #3: filecreator received a request from master to sent all file fragments for " + mdfsrsockblock.fileName + " to a node in diff edge.");

                            //master of this edge sent a request to this node to resolve a file request from diff edge.
                            //this node is the original filecreater.
                            //make dummy several RequestFromOneClientToAnotherForOneFragment packets and feed to itself,
                            //as if the original file requester asked multiple fragments from this node.
                            //source: file requester from diff edge
                            //destination: ownGUID
                            //note: even though file requester and original fileCreator are in two diff edges,
                            // DTN(aka rsock) will take care of data delivery.
                            for (int i = 0; i < mdfsrsockblock.totalNumOfBlocks; i++) {
                                for (int j = 0; j < mdfsrsockblock.n2; j++) {

                                    //create dummy MDFSRsockBlockForFileRetrieve of type == RequestFromOneClientToAnotherForOneFragment,
                                    //source = file requester from diff edge
                                    //destination = original fileCreator aka ownGUID.
                                    MDFSRsockBlockForFileRetrieve mdfsrsockblock1 = new MDFSRsockBlockForFileRetrieve(mdfsrsockblock.blockRetrieveReqUUID, MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment, (byte) mdfsrsockblock.n2, (byte) mdfsrsockblock.k2, mdfsrsockblock.srcGUID, EdgeKeeper.ownGUID, mdfsrsockblock.fileName, mdfsrsockblock.filePathMDFS, mdfsrsockblock.fileId, (byte) mdfsrsockblock.totalNumOfBlocks, (byte) i, (byte) j, mdfsrsockblock.localDir, null, false);

                                    //get each fragment of each block and send
                                    getUtils.justDoit(mdfsrsockblock1);

                                }
                            }

                            logger.log(Level.ALL, "CASE #3: filecreator sent all file fragments for " + mdfsrsockblock.fileName + " to a node in diff edge.");
                        } else {
                            //this request is not implemented
                            logger.log(Level.DEBUG, "MDFS received an unknown file retrieval request.");
                        }
                    }
                }
            }//while

        }

    }

    public static void stop(){
        isRunning = false;
    }
}
