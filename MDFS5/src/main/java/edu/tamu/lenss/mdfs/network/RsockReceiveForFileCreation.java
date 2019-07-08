package edu.tamu.lenss.mdfs.network;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.MDFSBlockCreator;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.MDFSRsockBlockCreator;
import example.Interface;
import example.ReceivedFile;

import static java.lang.Thread.sleep;


public class RsockReceiveForFileCreation implements Runnable{

    private static final String TAG = MDFSBlockCreator.class.getSimpleName();

    private boolean isTerminated;

    //constructor
    public RsockReceiveForFileCreation(){
        isTerminated = false;
    }


    @Override
    public void run() {
        if(RSockConstants.intrfc_creation==null) {
            RSockConstants.intrfc_creation = Interface.getInstance(GNS.getGNSInstance().getOwnGuid(), RSockConstants.intrfc_creation_appid, 999);
        }
        System.out.println("Rsock receiver thread is running...");
        ReceivedFile rcvdfile = null;
        while(!isTerminated){
            try {
                //blocking on receving through rsock
                try { rcvdfile = RSockConstants.intrfc_creation.receive(0, "default"); } catch (InterruptedException e) {e.printStackTrace(); }
                if(rcvdfile!=null) {
                    System.out.println("new incoming rsock");

                    System.out.println("print: " + Arrays.toString(rcvdfile.getFileArray()));

                    //convert byteArray into MDFSRsockBlockCreator object
                    ByteArrayInputStream bis = new ByteArrayInputStream(rcvdfile.getFileArray());
                    ObjectInputStream ois = new ObjectInputStream(bis);
                    MDFSRsockBlockCreator mdfsrsockblock = (MDFSRsockBlockCreator) ois.readObject();
                    bis.close();
                    ois.close();

                    //convert MDFSRsockBlockCreator obj into FragmentTransferInfo (header), File (fileFrag) etc.
                    FragmentTransferInfo header = (FragmentTransferInfo) mdfsrsockblock.fragTransInfoHeader;
                    byte[] byteArray = (byte[]) mdfsrsockblock.fileFrag;
                    long fileFragLength = (long) mdfsrsockblock.fileFragLength;
                    int numOfBlocks = (int) mdfsrsockblock.blockCount;
                    byte n2 =  (byte) mdfsrsockblock.n2;
                    byte k2 =  (byte) mdfsrsockblock.k2;
                    String fileName = (String) mdfsrsockblock.fileName;
                    String filePathMDFS= (String) mdfsrsockblock.filePathMDFS;
                    long fileCreatedTime = (long) mdfsrsockblock.fileCreatedTime;
                    String[] permList = (String[]) mdfsrsockblock.permList;
                    String uniquereqid = (String) mdfsrsockblock.uniqueReqID;
                    String fileCreatorGUID = (String) mdfsrsockblock.fileCreatorGUID;
                    String destGUID = (String) mdfsrsockblock.destGUID;

                    //now save the fileFrag
                    saveTheFileFragAndUpdateMetadataToEdgeKeeper(fileName, filePathMDFS, fileCreatedTime, permList, uniquereqid, numOfBlocks, n2, k2, header, byteArray, fileCreatorGUID, destGUID);


                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (NullPointerException | IOException | ClassNotFoundException e) {  ////InterruptedException
                e.printStackTrace();
            }
        }

    }

    //part of this function basically copied from FragExchangeHelper.java receiveBlockFragment() function
    //this function does two jobs one: save the filefrag locally in this device, two: submits fragment metadata to EdgeKeeper
    private void saveTheFileFragAndUpdateMetadataToEdgeKeeper(String filename, String filePathMDFS, long fileCreatedTime, String[] permList, String uniquereqid, int numOfBlocks, byte n2, byte k2, FragmentTransferInfo header, byte[] byteArray, String fileCreatorGUID, String destGUID) {  //note: destGUID was never used
        //create file
        File tmp0 = null;
        try{
            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(header.getFileName(),header.getCreatedTime(), header.getBlockIndex()));

            if(!tmp0.exists()){
                if(!tmp0.mkdirs()){
                    Logger.e(TAG, "Fail to create block directory for " + header.getFileName());
                    return;
                }
            }

            //write on file
            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(header.getFileName(), header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex()));
            FileOutputStream outputStream = new FileOutputStream(tmp0);
            outputStream.write(byteArray);
            outputStream.flush();
            outputStream.close();

            //update own local directory data
            ServiceHelper.getInstance().getDirectory().addBlockFragment(header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex());

            //update to edgekeeper about the fragments that I just received
            FileMetadata metadata = new FileMetadata(EdgeKeeperConstants.FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST, EdgeKeeperConstants.getMyGroupName(), GNS.ownGUID, fileCreatorGUID, fileCreatedTime, permList, new Date().getTime(), uniquereqid, filename, filePathMDFS, numOfBlocks , n2, k2);

            //add info of the fragment I received/have
            metadata.addInfo(GNS.ownGUID, Integer.toString((int)header.getBlockIndex()), Integer.toString((int)header.getFragIndex()));

            //create client connection and connect
            client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);
            boolean connected = client.connect();

            if(!connected){
                //if couldnt connect to EdgeKeeper, we put in DTN queue
                client.putInDTNQueue(metadata, 3);
                return;
            }

            //set socket read timeout(unnecessary here)
            client.setSocketReadTimeout();

            //get json string
            String str= metadata.toBuffer(metadata);

            //make sendBuf
            ByteBuffer sendBuf = ByteBuffer.allocate(str.length());
            sendBuf.order(ByteOrder.LITTLE_ENDIAN);
            sendBuf.clear();

            //put data in sendBuf
            sendBuf.put(str.getBytes());
            sendBuf.flip();

            //send
            client.send(sendBuf);

            //sleep 1 second
            try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

            //close connection
            client.close();

        }catch(IOException | NullPointerException | SecurityException e){
            e.printStackTrace();
        }
    }

}
