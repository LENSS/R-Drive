package edu.tamu.cse.lenss.CLI;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;

public class handleMkdirCommand {

    public static void handleMkdirCommand(String clientID, String mdfsDir){

        //make FileMetadata object with cmd = CREATE_MDFS_DIR_REQUEST
        FileMetadata metadataReq = new FileMetadata(EdgeKeeperConstants.CREATE_MDFS_DIR_REQUEST, new Date().getTime(), EdgeKeeperConstants.getMyGroupName(), GNS.ownGUID, mdfsDir, "dummyMessage");

        //create client connection
        client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);

        //connect
        boolean connected = client.connect();

        //check if connection succeeded..if not, return with error msg
        if(!connected){
            clientSockets.sendAndClose(clientID, "mkdir Error! Could not connect to EdgeKeeper.");
            return;
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
            clientSockets.sendAndClose(clientID, "CLIII -mkdir Info. Did not receive a reply from Edgekeeper, request might/might not succeed.");
            return;
        }else{
            //close client socket
            client.close();

            //get data from recvBuf and make string
            StringBuilder bd = new StringBuilder();
            while (recvBuf.remaining() > 0){ bd.append((char)recvBuf.get());}
            String str1 = bd.toString();

            //make metadata from str1
            FileMetadata metadataRet = FileMetadata.parse(str1);

            //check command
            if(metadataRet.command==EdgeKeeperConstants.CREATE_MDFS_DIR_REPLY_SUCCESS){

                //reply with success
                clientSockets.sendAndClose(clientID, "CLIII Success! Directory Created.");

            }else if(metadataRet.command==EdgeKeeperConstants.CREATE_MDFS_DIR_REPLY_FAILED){

                //reply with failure
                clientSockets.sendAndClose(clientID, "CLIII Failed! Directory Creation failed. Reason: " + metadataRet.message);

            }
        }

    }
}
