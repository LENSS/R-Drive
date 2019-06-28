package edu.tamu.lenss.mdfs.EdgeKeeper;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class server extends Thread{
    //tcp variables
    public int port = -1;
    ServerSocketChannel serverSocketChannel;
    SocketChannel socketChannel;
    ByteBuffer recvBuf;
    DataStore datastore;


    public server(int port){
        this.port = port;

        //open
        try { this.serverSocketChannel = ServerSocketChannel.open(); } catch (IOException e) { e.printStackTrace(); }

        //bind
        try { serverSocketChannel.socket().bind(new InetSocketAddress(port)); } catch (IOException e) { e.printStackTrace(); }
        System.out.println("EdgeKeeper server is running...");

        //create DataStore object
        this.datastore = DataStore.getInstance();

    }

    public void run(){
        ByteBuffer sendBuf;
        while(true){
            //accept a client
            //server accepts a client, serves it and then moves on to serve the next client.
            // this is important to ensure data consistency.
            try {socketChannel = serverSocketChannel.accept(); } catch (IOException e) { e.printStackTrace(); }
            System.out.println("EdgeKeeper server accepted a client");

            //receive ByteBuffer flipped
            boolean ret = receive(socketChannel);

            //check
            if(ret){

                //make string out of buffer
                StringBuilder bd = new StringBuilder();
                while (recvBuf.remaining() > 0)
                    bd.append((char)recvBuf.get());
                String str1 = bd.toString();

                //make EdgeKeeperMetadata object by passing flipped ByteBuffer
                EdgeKeeperMetadata metadataRec = EdgeKeeperMetadata.parse(str1);

                //check for command
                if(metadataRec!=null) {
                    if (metadataRec.command == EdgeKeeperConstants.FILE_CREATOR_METADATA_DEPOSIT_REQUEST) {
                        System.out.println("EdgeKeeper server got metadata Deposit request from file creator");
                        this.datastore.putFileMetadata(metadataRec);
                        this.datastore.putGroupNameBYGUID(metadataRec.fileCreatorGUID, metadataRec.ownGroupNames);

                    }else if(metadataRec.command == EdgeKeeperConstants.METADATA_WITHDRAW_REQUEST) {
                        System.out.println("EdgeKeeper server got metadata Withdraw request");

                        //first get the GROUP name of this GUID and store this
                        this.datastore.putGroupNameBYGUID(metadataRec.metadataRequesterGUID, metadataRec.ownGroupNames);

                        //get the requested metadata from dataStore
                        EdgeKeeperMetadata metadataRet = datastore.getFileMetadata(metadataRec.fileID);

                        //convert metadata into json string
                        String str= metadataRet.toBuffer(metadataRet);

                        //allocate space for reply
                        sendBuf = ByteBuffer.allocate(str.length());
                        sendBuf.order(ByteOrder.LITTLE_ENDIAN);
                        sendBuf.clear();

                        //put data in sendBuf
                        sendBuf.put(str.getBytes());
                        sendBuf.flip();

                        //send back
                        send(sendBuf);
                    }else if(metadataRec.command == EdgeKeeperConstants.GROUP_TO_GUID_CONV_REQUEST){
                        //first get the GROUP name of this GUID and store this
                        this.datastore.putGroupNameBYGUID(metadataRec.groupConversionRequesterGUID, metadataRec.ownGroupNames);

                        System.out.println("EdgeKeeper server got Group to GUID Conversion request(C)");
                        List<String> groupnames = metadataRec.groupOrGUID;

                        //make a GUIDList to return
                        List<String> listofResultantGUIDS =  new ArrayList<>();

                        //get all GUIDs who are member of at least one group
                        List<String> allGUIDsBelongsToAnyGroup = new ArrayList(datastore.GUIDtoGroupNamesMap.keySet());

                        //check and find
                        for(int i=0; i< groupnames.size(); i++){
                            String groupname = groupnames.get(i);
                            //trace all GUIDs who belong to this groupname
                            for(int j=0; j< allGUIDsBelongsToAnyGroup.size(); j++){
                                String guid = allGUIDsBelongsToAnyGroup.get(j);
                                if(datastore.GUIDtoGroupNamesMap.get(guid).contains(groupname)){
                                    listofResultantGUIDS.add(guid);
                                }
                            }
                        }


                        //make reply metadata object
                        EdgeKeeperMetadata metadataRet;
                        if(listofResultantGUIDS.size()!=0) {
                            metadataRet = new EdgeKeeperMetadata(EdgeKeeperConstants.GROUP_TO_GUID_CONV_REPLY_SUCCESS, new ArrayList<>(), metadataRec.groupConversionRequesterGUID, listofResultantGUIDS);
                        }else{
                            metadataRet = new EdgeKeeperMetadata(EdgeKeeperConstants.GROUP_TO_GUID_CONV_REPLY_FAILED, new ArrayList<>(), metadataRec.groupConversionRequesterGUID, listofResultantGUIDS);
                        }

                        //convert metadata into json string
                        String str= metadataRet.toBuffer(metadataRet);

                        //allocate space for reply
                        sendBuf = ByteBuffer.allocate(str.length());
                        sendBuf.order(ByteOrder.LITTLE_ENDIAN);
                        sendBuf.clear();

                        //put data in sendBuf
                        sendBuf.put(str.getBytes());
                        sendBuf.flip();

                        //send back
                        send(sendBuf);

                    }
                }


            }

        }


    }

    public void send(ByteBuffer buffer){

        //first, allocate the packet
        ByteBuffer packet = ByteBuffer.allocate(Long.BYTES + buffer.limit());
        packet.order(ByteOrder.LITTLE_ENDIAN);
        packet.clear();


        //second, put the buffer.limit() at front
        packet.putLong((long)buffer.limit());

        //third, put the buffer next
        buffer.rewind();
        for(int i=0;i<buffer.limit(); i++){ packet.put(buffer.get(i)); }
        packet.flip();

        //fourth, do send
        int w=0;
        if(socketChannel.isConnected()) {
            try {
                w = socketChannel.write(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (w > 0) {
                System.out.println("EdgeKeeper server: Data sent: " + w);
            }
        }else{
            System.out.println("EdgeKeeper server: Socket is not connected");
        }
    }

    public boolean receive(SocketChannel socket){

        //first, read only Long.BYTES amount to figure out the total receive size
        long size_of_recv = 0;
        ByteBuffer size = ByteBuffer.allocate( Long.BYTES);
        size.order(ByteOrder.LITTLE_ENDIAN);
        size.clear();

        //read only Long.BYTES amount
        int iii = 0;
        do{
            int r = 0;
            try { r = socket.read(size); } catch (IOException e) { e.printStackTrace(); }
            if(r>0) {
                iii = iii + r;
            }
        }while(size.position()<Long.BYTES);
        size.flip();


        //convert size buffer into a long
        size_of_recv = size.getLong();

        // when size_of_recv is known, create a data buffer of size of size_of_recv
        ByteBuffer recv = ByteBuffer.allocate((int) size_of_recv);
        recv.order(ByteOrder.LITTLE_ENDIAN);
        recv.clear();

        // then read only size_of_recv much bytes from socket
        int ii = 0;
        do{
            int r = 0;
            try { r = socket.read(recv); } catch (IOException e) { e.printStackTrace(); }
            if(r>0){
                ii = ii+ r;
            }
        }while(recv.position()<size_of_recv);
        recv.flip();

        //print
        System.out.println("EdgeKeeper server: socket read: " + (Long.BYTES + (int) recv.limit()));

        //allocte return packet
        recvBuf = ByteBuffer.allocate(recv.limit());
        recvBuf.order(ByteOrder.LITTLE_ENDIAN);
        recvBuf.clear();

        //put all the data in the recvBuf
        for(int i=0; i< recv.limit(); i++){ recvBuf.put(recv.get(i)); }
        recvBuf.flip();

        //return
        if(recv.limit()>0){return true;}else{return false;}
    }

}


