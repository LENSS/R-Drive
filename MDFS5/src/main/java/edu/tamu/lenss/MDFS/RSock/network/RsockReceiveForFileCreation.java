package edu.tamu.lenss.MDFS.RSock.network;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Model.MDFSRsockBlockForFileCreate;
import RsockCommLibrary.Interface;
import RsockCommLibrary.ReceivedFile;



//this class is used for receiving packets for creating a file in MDFS.
//this is the entry point of a file fragment to enter this node.
//this class receives a packet from rsock cilent library(rsock java api),
//and saves it.
public class RsockReceiveForFileCreation implements Runnable{


    //logger
    public static Logger logger = Logger.getLogger(RsockReceiveForFileCreation.class);

    private static final long serialVersionUID = 8L;

    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForFileCreation(){

    }


    @Override
    public void run() {

        if(RSockConstants.intrfc_creation==null) {
            RSockConstants.intrfc_creation = new Interface(EdgeKeeper.ownGUID, RSockConstants.intrfc_creation_appid, 3600);
        }

        ReceivedFile rcvdfile = null;
        if(RSockConstants.RSOCK) {

            while (!isTerminated) {
                try {
                    //blocking on receiving through rsock at a particular endpoint
                    try {
                        rcvdfile = RSockConstants.intrfc_creation.receive();
                    } catch (InterruptedException e) {
                        //rsock api closing
                        isTerminated = true;
                    }

                    //check for null
                    if (rcvdfile != null) {

                        System.out.println("new incoming rsock for file creation");

                        //convert byteArray into MDFSRsockBlockCreator object
                        ByteArrayInputStream bis = new ByteArrayInputStream(rcvdfile.getFileArray());
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        MDFSRsockBlockForFileCreate mdfsrsockblock = (MDFSRsockBlockForFileCreate) ois.readObject();
                        bis.close();
                        ois.close();

                        //convert MDFSRsockBlockCreator obj into FragmentTransferInfo (header), File (fileFrag) etc.
                        String fileName = (String) mdfsrsockblock.fileName;
                        String filePathMDFS = (String) mdfsrsockblock.filePathMDFS;
                        byte[] byteArray = (byte[]) mdfsrsockblock.fileFrag;
                        String fileID = (String) mdfsrsockblock.fileID;
                        long filesize = (long) mdfsrsockblock.entireFileSize;
                        int n2 = (int) mdfsrsockblock.n2;
                        int k2 = (int) mdfsrsockblock.k2;
                        int blockIdx = (int) mdfsrsockblock.blockIdx;
                        int fragmentIdx = (int) mdfsrsockblock.fragmentIdx;
                        String fileCreatorGUID = (String) mdfsrsockblock.fileCreatorGUID;
                        String uniquereqid = (String) mdfsrsockblock.uniqueReqID;
                        boolean isGlobal = (boolean) mdfsrsockblock.isGlobal;

                        //now save the fileFrag
                        saveTheFileFragment(fileName, filePathMDFS, byteArray, fileID, filesize, n2, k2, blockIdx, fragmentIdx, fileCreatorGUID, uniquereqid, isGlobal);
                        logger.log(Level.DEBUG, "fragment# " + fragmentIdx + " of block# " + blockIdx + " of filename " + fileName + " received from rsock.");
                    }

                } catch (IOException | ClassNotFoundException e) {

                    logger.log(Level.FATAL, "Exception in constructing fragment ", e);
                }
            }

        }

    }

    //this function does two jobs one: save the filefrag locally in this device, two: submits fragment metadata to EdgeKeeper
    private void saveTheFileFragment(String fileName, String filePathMDFS, byte[] byteArray, String fileID , long filesize, int n2, int k2, int blockIdx, int fragmentIdx, String fileCreatorGUID, String uniquereqid, boolean isGlobal) {
            //create file
            File tmp0 = null;

            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileID, blockIdx));

            if(!tmp0.exists()){
                if(!tmp0.mkdirs()){
                    return;
                }
            }

            //write on file
             try {
                 tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileID, blockIdx, fragmentIdx));
                 FileOutputStream outputStream = new FileOutputStream(tmp0);
                 outputStream.write(byteArray);
                 outputStream.flush();
                 outputStream.close();
             }catch(IOException e){

                 //log
                 logger.log(Level.DEBUG,"Could not write fragment bytes into file for fragment# " + fragmentIdx + " of block# " + blockIdx + " of filename " + fileName + ".");
             }

    }

    public static void stop(){
        isTerminated = true;
    }

}
