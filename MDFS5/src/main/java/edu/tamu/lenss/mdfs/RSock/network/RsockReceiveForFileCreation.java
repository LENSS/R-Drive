package edu.tamu.lenss.mdfs.RSock.network;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileCreate;
import example.Interface;
import example.ReceivedFile;



//this class is used for receiving packets for creating a file in MDFS.
//this is the entry point of a file fragment to enter this node.
//this class receives a packet from rsock cilent library(rsock java api),
//and saves it.
public class RsockReceiveForFileCreation implements Runnable{

    private static final long serialVersionUID = 8L;

    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForFileCreation(){

    }


    @Override
    public void run() {
        if(RSockConstants.intrfc_creation==null) {
            RSockConstants.intrfc_creation = Interface.getInstance(EdgeKeeper.ownGUID, RSockConstants.intrfc_creation_appid, 999);
        }
        System.out.println("Rsock receiver thread is running...");
        ReceivedFile rcvdfile = null;
        while(!isTerminated){
            try {
                //blocking on receiving through rsock
                try {
                    rcvdfile = RSockConstants.intrfc_creation.receive(100, "default"); }
                catch (InterruptedException e) {e.printStackTrace(); }

                //check for null
                if(rcvdfile!=null) {
                    System.out.println("new incoming rsock for file creation");

                    //convert byteArray into MDFSRsockBlockCreator object
                    ByteArrayInputStream bis = new ByteArrayInputStream(rcvdfile.getFileArray());
                    ObjectInputStream ois = new ObjectInputStream(bis);
                    MDFSRsockBlockForFileCreate mdfsrsockblock = (MDFSRsockBlockForFileCreate) ois.readObject();
                    bis.close();
                    ois.close();

                    //convert MDFSRsockBlockCreator obj into FragmentTransferInfo (header), File (fileFrag) etc.
                    String fileName = (String) mdfsrsockblock.fileName;
                    String filePathMDFS= (String) mdfsrsockblock.filePathMDFS;
                    byte[] byteArray = (byte[]) mdfsrsockblock.fileFrag;
                    long fileCreatedTime = (long) mdfsrsockblock.fileCreatedTime;
                    long filesize = (long) mdfsrsockblock.entireFileSize;
                    byte n2 =  (byte) mdfsrsockblock.n2;
                    byte k2 =  (byte) mdfsrsockblock.k2;
                    byte blockIdx = (byte) mdfsrsockblock.blockIdx;
                    byte fragmentIdx = (byte) mdfsrsockblock.fragmentIdx;
                    String fileCreatorGUID = (String) mdfsrsockblock.fileCreatorGUID;
                    String uniquereqid = (String) mdfsrsockblock.uniqueReqID;
                    boolean isGlobal = (boolean) mdfsrsockblock.isGlobal;

                    //now save the fileFrag
                    saveTheFileFragAndUpdateMetadataToEdgeKeeper(fileName, filePathMDFS, byteArray, fileCreatedTime, filesize, n2, k2, blockIdx, fragmentIdx, fileCreatorGUID, uniquereqid, isGlobal);

                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //execution only comes here when the above while loop is broken.
        //above while loop is only broken when mdfs is closing.
        //came out of while loop, now close the rsock client library object.
        RSockConstants.intrfc_creation.close();
        RSockConstants.intrfc_creation = null;

    }

    //this function does two jobs one: save the filefrag locally in this device, two: submits fragment metadata to EdgeKeeper
    private void saveTheFileFragAndUpdateMetadataToEdgeKeeper(String fileName, String filePathMDFS, byte[] byteArray, long fileCreatedTime, long filesize, byte n2, byte k2, byte blockIdx, byte fragmentIdx, String fileCreatorGUID, String uniquereqid, boolean isGlobal) {
        //create file
        File tmp0 = null;
        try{
            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileCreatedTime, blockIdx));

            if(!tmp0.exists()){
                if(!tmp0.mkdirs()){
                    return;
                }
            }

            //write on file
            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileCreatedTime, blockIdx, fragmentIdx));
            FileOutputStream outputStream = new FileOutputStream(tmp0);
            outputStream.write(byteArray);
            outputStream.flush();
            outputStream.close();

            //update own local directory data
            ServiceHelper.getInstance().getDirectory().addBlockFragment(fileCreatedTime, blockIdx, fragmentIdx);

            //add info of the fragment I received and
            //update to edgekeeper.
            MDFSMetadata metadata = MDFSMetadata.createFileMetadata(uniquereqid, fileCreatedTime, filesize, fileCreatorGUID, EdgeKeeper.ownGUID, filePathMDFS + "/" + fileName, isGlobal);
            metadata.setn2(n2);
            metadata.setk2(k2);
            metadata.addInfo(EdgeKeeper.ownGUID, (int)blockIdx, (int)fragmentIdx);

            //send metadata to local edgeKeeper
            JSONObject repJSON = EKClient.putMetadata(metadata);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void stop(){
        isTerminated = true;
    }

}
