package edu.tamu.cse.lenss.CLI;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.MDFSBlockRetrieverViaRsock;
import edu.tamu.lenss.mdfs.MDFSFileRetrieverViaRsock;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;

public class handleGETrequest {

    public static void handleGETrequest(String clientID, String filename, String mdfsDir, String localDir){

        //first retrieve the metadata from edgeKeeper
        FileMetadata metadata = fetchFileMetadataFromEdgeKeeper(clientID,filename, mdfsDir);

        //check for null
        if(metadata!=null){

            //re-create MDFSFileInfo object
            MDFSFileInfo fileInfo  = new MDFSFileInfo(metadata.filename, metadata.fileID);
            fileInfo.setFileSize(metadata.filesize);
            fileInfo.setNumberOfBlocks((byte)metadata.numOfBlocks);
            fileInfo.setFragmentsParms((byte)metadata.n2,  (byte)metadata.k2);

            //make mdfsfileretriever object
            MDFSFileRetrieverViaRsock retriever = new MDFSFileRetrieverViaRsock(fileInfo, metadata, clientID, localDir);  //RSOCK
            retriever.setDecryptKey(ServiceHelper.getInstance().getEncryptKey());
            retriever.setListener(fileRetrieverListenerviarsock);
            retriever.start();

            //send reply to cli client
            clientSockets.sendAndClose(clientID, "-get Info: request has been place.");

        }else{
            //dont do anything here..errors have been handled already
        }
    }


    //fetches file metadata from EdgeKeeper.
    //returns FileMetadata object or null.
    private static FileMetadata fetchFileMetadataFromEdgeKeeper(String clientID, String filename, String mdfsDir) {

        //make request metadata object
        FileMetadata metadataReq = new FileMetadata(EdgeKeeperConstants.METADATA_WITHDRAW_REQUEST, EdgeKeeperConstants.getMyGroupName(), GNS.ownGUID, new Date().getTime(), filename, mdfsDir);

        //create client connection
        client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);

        //connect
        boolean connected = client.connect();

        //check if connection succeeded..if not, return with error msg
        if(!connected){
            clientSockets.sendAndClose(clientID, "-get Error! Could not connect to EdgeKeeper.");
            return null;
        }

        //if connected, set socket read timeout(necessary here as we are expected reply in time)
        client.setSocketReadTimeout();

        //convert `object into json string
        String str = metadataReq.toBuffer(metadataReq);

        //create bytebuffer
        ByteBuffer sendBuf = ByteBuffer.allocate(str.length());
        sendBuf.order(ByteOrder.LITTLE_ENDIAN);
        sendBuf.clear();

        //put str in sendBuf and flip
        sendBuf.put(str.getBytes());
        sendBuf.flip();

        //send metadata req
        client.send(sendBuf);

        //get return
        ByteBuffer recvBuf = client.receive();

        //check if receive value is null or nah(can be null due to timeout)
        if(recvBuf==null){

            //close client socket
            client.close();
            clientSockets.sendAndClose(clientID, "-get Info: Did not receive a reply from Edgekeeper, request might/might not succeed.");
            return null;

        }else{

            //close edgekeeper client socket
            client.close();

            //get data from recvBuf and make string
            StringBuilder bd = new StringBuilder();
            while (recvBuf.remaining() > 0){ bd.append((char)recvBuf.get());}
            String str1 = bd.toString();

            //make metadata from str1
            FileMetadata metadataRet = FileMetadata.parse(str1);

            //check command
            if(metadataRet.command==EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_SUCCESS){

                //return the metadata
                return metadataRet;

            }else if(metadataRet.command==EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST){

                //reply with failure as file dont exist
                clientSockets.sendAndClose(clientID, "-get Failed! File doesnt exist.");
                return null;

            }else if(metadataRet.command==EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_PERMISSIONDENIED){

                //reply with failure as permission denied
                clientSockets.sendAndClose(clientID, "-get Failed! File permission denied.");
                return null;
            }else if(metadataRet.command==EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_DIRNOTEXIST){

                //reply with failure as dir doesnt exist
                clientSockets.sendAndClose(clientID, "-get Failed! Directory doesnt exist.");
                return null;
            }
        }

        clientSockets.sendAndClose(clientID, "-get Error! EdgeKeeper has no metadata for this file.");
        return null;
    }

    //rsock listener
    private static MDFSBlockRetrieverViaRsock.BlockRetrieverListenerViaRsock fileRetrieverListenerviarsock = new MDFSBlockRetrieverViaRsock.BlockRetrieverListenerViaRsock(){        //RSOCK

        @Override
        public void onError(String error, MDFSFileInfo fileInfo, String clientID) {
            System.out.println("xxx:::" + error);
            return;
        }

        @Override
        public void statusUpdate(String status, String clientID) {
            System.out.println("xxx:::" + status);
            return;
        }

        @Override
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo, String clientID) {
            System.out.println("xxx:::" + "success");
            return;
        }
    };
}
