package edu.tamu.lenss.MDFS.RSock.network;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
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
import edu.tamu.lenss.MDFS.Utils.MDFSDirectory;
import example.*;

import static java.lang.Thread.sleep;


//this class is used for block retrieval via rsock.
//this class receives fragment requests and send the fragment to the requestor.
public class RsockReceiveForFileRetrieval implements Runnable {


    //log
    public static Logger logger = org.apache.log4j.Logger.getLogger(RsockReceiveForFileRetrieval.class);

    private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";
    static boolean isRunning = true;

    public RsockReceiveForFileRetrieval() {}

    @Override
    public void run() {


        //create rsock Interface intrfc
        RSockConstants.intrfc_retrieval = Interface.getInstance(EdgeKeeper.ownGUID, RSockConstants.intrfc_retrieval_appid, 3600, true);

        //variable
        boolean classConversion = false;
        ReceivedFile receivedFile = null;
        byte[] byteArray = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        File tmp0 = null;
        boolean success = false;
        MDFSRsockBlockForFileRetrieve mdfsrsockblock = null;


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
            try { receivedFile = RSockConstants.intrfc_retrieval.receive(100,RSockConstants.fileRetrieveEndpoint);} catch (InterruptedException e) {e.printStackTrace(); }

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
                }catch(Exception e){
                    e.printStackTrace();
                    //logger: could not convert data into class
                }

                if(classConversion) {

                    //get type variable inside mdfsrsockblock
                    MDFSRsockBlockForFileRetrieve.Type type = (MDFSRsockBlockForFileRetrieve.Type) mdfsrsockblock.type;

                    //if the other party is asking for a fragment.
                    //then, we send the fragment if we have it.
                    if (type == MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientToAnother) {

                        //its a request for fetching a particular file fragment.
                        getUtils.justDoit(mdfsrsockblock, receivedFile.getReplyEndpoint());
                    }

                    //if other party sent a fragment that I asked for sometimes before.
                    //then process it.
                    else if(type == MDFSRsockBlockForFileRetrieve.Type.ReplyFromOneClientToAnother){

                        //check if the received file is not null
                        if(mdfsrsockblock.fileFrag!=null){

                            //check if file directory exists
                            File fileDIr = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId));

                            //check if file directory exists
                            if(!fileDIr.exists()){

                                //create file directory
                                if(fileDIr.mkdirs()){
                                    System.out.println("created file directory coz it didnt exist");
                                }else{

                                    //log
                                    logger.log(Level.DEBUG, "Could not create file directory for storing fragment for file " + mdfsrsockblock.fileName);
                                }

                            }


                            //check if block directory exists
                            File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx));
                            if(!blockDir.exists()){

                                //create block directory
                                System.out.println("creating block directory for block# " + mdfsrsockblock.blockIdx);
                                if(blockDir.mkdirs()){
                                    System.out.println("created block directory.");
                                }else{
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
                                outputStream.write( mdfsrsockblock.fileFrag);
                                outputStream.flush();
                                outputStream.close();
                            }catch(Exception e){ e.printStackTrace();}


                            //log
                            logger.log(Level.ALL, "Received fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName + " from node " + mdfsrsockblock.srcGUID);

                            // Rename the fragment without the DOWNLOADING_SIGNATURE
                            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0 (file)
                            success = IOUtilities.renameFile(tmp1, MDFSFileInfo.getFragName(mdfsrsockblock.fileName, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex));

                            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0 (file)
                            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex));
                            try { sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

                            //log
                            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Finish downloading fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName + " from node " + mdfsrsockblock.srcGUID );

                            //after receiving each fragment, we check enough fragments are available for this block.
                            //check if enough fragment has received for this block
                            //make a list for block_fragments I dont have
                            int[][]missingBlocksAndFrags = new int[mdfsrsockblock.totalNumOfBlocks][];

                            //get/populate missingBlocksAndFrags array for the fragments of this block
                            boolean succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.n2, mdfsrsockblock.blockIdx);

                            //check if this node has already K frags for each block
                            if (succeeded && missingBlocksAndFrags[mdfsrsockblock.blockIdx]!=null) {

                                //check if at least K fragments are available for this block
                                int k = mdfsrsockblock.k2;
                                for(int i=0; i< missingBlocksAndFrags[mdfsrsockblock.blockIdx].length; i++){
                                    if(missingBlocksAndFrags[mdfsrsockblock.blockIdx][i]==0){k--;}
                                }

                                //check if k is <= than 0
                                if(k<=0){

                                    //enough fragments for this block is available,
                                    //then we try to check if enough fragments for all blocks are available
                                    missingBlocksAndFrags = new int[mdfsrsockblock.totalNumOfBlocks][mdfsrsockblock.n2];

                                    //get/populate missingBlocksAndFrags array for each block
                                    succeeded  = false;
                                    for(int i=0; i< mdfsrsockblock.totalNumOfBlocks; i++){
                                        succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.n2, i);
                                    }

                                    if(succeeded){
                                        //check if this node has already K frags for each block
                                        if (getUtils.checkEnoughFragsAvailable(missingBlocksAndFrags, mdfsrsockblock.k2)) {

                                            //merge the file in a new thread
                                            new Thread(new FileMerge(mdfsrsockblock)).run();
                                        }
                                    }else{
                                        MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Merge failed for filename: " + mdfsrsockblock.fileName);
                                    }

                                }else{
                                    //for this block not enough fragments are present.
                                    //so, we wont be able to recreate this block.
                                    //so there is no use to check if other blocks have enough fragments or nah.
                                }

                            }

                        }
                    }else if(type == MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientInOneAEdgeToMasterOfAnotherEdge){
                        //This request means This guy is currently the master of this edge.
                        //This guy needs to ask one of its client node to send fragments to the requesting client from another edge.
                        //first fetch the file metadata exists in this.
                        //then check if file creator is me. if me, send the fragments immediately, if not, make RequestFromMasterToClientInSameEdge packet.

                        //log
                        logger.log(Level.ALL, "This node received a file retrierval request from GUID " + mdfsrsockblock.srcGUID +" of another edge for " + mdfsrsockblock.filePathMDFS+ mdfsrsockblock.fileName + "file");


                        //first retrieve the metadata from edgeKeeper
                        MDFSMetadata metadata = get.fetchFileMetadataFromEdgeKeeper(mdfsrsockblock.filePathMDFS + mdfsrsockblock.fileName);

                        //check for null
                        if(metadata!=null){

                            //logger
                            logger.log(Level.ALL, "Command:get | log: Fetched file metadata for filaname " + metadata.getFileName() + " of fileID "  + metadata.getFileID());

                            //check I am the original creator of this file
                            if(metadata.getFileCreatorGUID().equals(EdgeKeeper.ownGUID)){

                                //I am the original creator of this file,
                                //that means, I should have all the fragments,
                                //no need to send request for fragments to other nodes.
                                //check by filename__fileID if I have the file fragments in MDFS directory in disk.
                                File f = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(mdfsrsockblock.fileName, mdfsrsockblock.fileId)); //Isagor0!

                                //check
                                if(f!=null && f.exists() && f.isDirectory()){

                                    //file folder exists
                                    for(int i=0; i< metadata.getBlockCount(); i++) {
                                        for(int j =0; j<metadata.getn2(); j++) {
                                            //create dummy MDFSRsockBlockForFileRetrieve of type == RequestFromOneClientToAnother,
                                            // and send request to own self.
                                            //source = the GUID of file requester
                                            //destination = ownGUID
                                            MDFSRsockBlockForFileRetrieve mdfsrsockblock1 = new MDFSRsockBlockForFileRetrieve(UUID.randomUUID().toString(), MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientToAnother, (byte)metadata.getn2(), (byte)metadata.getk2(), mdfsrsockblock.srcGUID, EdgeKeeper.ownName, mdfsrsockblock.fileName, mdfsrsockblock.fileId, (byte)metadata.getBlockCount(), (byte)i, (byte)j, "/storage/emulated/0/" + Constants.DEFAULT_DECRYPTION_FOLDER_NAME + "/", null, false);

                                            //get each fragment of each block and send
                                            getUtils.justDoit(mdfsrsockblock1, receivedFile.getReplyEndpoint());

                                        }
                                    }
                                }else{
                                    logger.log(Level.DEBUG, "Could not serve fragment request from node " + mdfsrsockblock.srcGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName + "due to file directory no longer exists in storage.");
                                }

                            }else{

                                //I am not the original creator of this file.
                                //need to send request to each nodes who has the file fragments
                                //send the fragment requests to the fileCreatorGUID.
                                for(int i=0; i< metadata.getBlockCount(); i++) {
                                    for(int j =0; j<metadata.getn2(); j++) {
                                        //create dummy MDFSRsockBlockForFileRetrieve of type == RequestFromOneClientToAnother,
                                        // and send request to the fileCreatorGUID.
                                        //source = the GUID of file requester
                                        //destination = original fileCreator
                                        MDFSRsockBlockForFileRetrieve mdfsrsockblock1 = new MDFSRsockBlockForFileRetrieve(UUID.randomUUID().toString(), MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientToAnother, (byte)metadata.getn2(), (byte)metadata.getk2(), mdfsrsockblock.srcGUID, metadata.getFileCreatorGUID(), mdfsrsockblock.fileName, mdfsrsockblock.fileId, (byte)metadata.getBlockCount(), (byte)i, (byte)j, "/storage/emulated/0/" + Constants.DEFAULT_DECRYPTION_FOLDER_NAME + "/", null, false);

                                        //get each fragment of each block and send
                                        getUtils.justDoit(mdfsrsockblock1, receivedFile.getReplyEndpoint());

                                    }
                                }

                            }

                        }else{

                            //log
                            logger.log(Level.ERROR, "Could not fetch file metadata from local EdgeKeeper for filename " + mdfsrsockblock.fileName);


                        }
                    }else{
                        //this request is not implemented
                        logger.log(Level.DEBUG, "MDFS received an unknown file retrieval request.");
                    }
                }
            }
        }//while

        //came out of while loop, now close the rsock client library object
        RSockConstants.intrfc_retrieval.close();
        RSockConstants.intrfc_retrieval = null;
    }

    public static void stop(){
        isRunning = false;
    }
}
