package edu.tamu.lenss.mdfs.RSock.network;


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

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.mdfs.Commands.get.FileMerge;
import edu.tamu.lenss.mdfs.Commands.get.MDFSFileRetrieverViaRsock;
import edu.tamu.lenss.mdfs.Commands.get.getUtils;
import edu.tamu.lenss.mdfs.Model.MDFSFileInfo;
import edu.tamu.lenss.mdfs.Model.MDFSRsockBlockForFileRetrieve;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.Utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;
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
                    if (type == MDFSRsockBlockForFileRetrieve.Type.Request) {

                        logger.log(Level.ALL, "fraggg received fragment request from node " + mdfsrsockblock.srcGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName);


                        //get the file fragment from my disk
                        tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex));

                        //if fragment was fetched
                        if (tmp0.length() > 0) {

                            //convert file tmp0 into byteArray
                            byteArray = new byte[(int) tmp0.length()];
                            try {
                                FileInputStream fileInputStream = new FileInputStream(tmp0);
                                fileInputStream.read(byteArray);
                            } catch (FileNotFoundException e) {
                                System.out.println("File Not Found.");
                                e.printStackTrace();
                            } catch (IOException e1) {
                                System.out.println("Error Reading The File.");
                                e1.printStackTrace();
                            }

                            //now, change mdfsrsockblock into a Reply object
                            mdfsrsockblock.flipIntoReply(byteArray);

                            //convert mdfsrsockblock object into bytearray and do send
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ObjectOutputStream ooos = null;
                            try {
                                ooos = new ObjectOutputStream(bos);
                                ooos.writeObject(mdfsrsockblock);
                                ooos.flush();
                                byte[] data = bos.toByteArray();

                                //send the object over rsock and expect no reply
                                String uuid = UUID.randomUUID().toString().substring(0, 12);
                                RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", mdfsrsockblock.destGUID, 500, "hdrRecv", receivedFile.getReplyEndpoint(), "noReply");

                                //log
                                logger.log(Level.ALL, "fraggg resolved fragment request from node " + mdfsrsockblock.destGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{

                            //log
                            logger.log(Level.DEBUG, "fraggg Could not serve fragment request from node " + mdfsrsockblock.srcGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName);
                        }
                    }

                    //if other party sent a fragment that I asked for sometimes before.
                    //then process it.
                    else if(type == MDFSRsockBlockForFileRetrieve.Type.Reply){

                        //check if the received file is not null
                        if(mdfsrsockblock.fileFrag!=null){

                            //check if file directory exists
                            File fileDIr = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId));

                            //check if file directory exists
                            if(!fileDIr.exists()){

                                //create file directory
                                if(fileDIr.mkdirs()){
                                    System.out.println("created file directory as it didnt exist");
                                }else{
                                    System.out.println("couldnt create file directory");

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
