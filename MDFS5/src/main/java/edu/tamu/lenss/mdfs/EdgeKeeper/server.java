package edu.tamu.lenss.mdfs.EdgeKeeper;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import edu.tamu.lenss.mdfs.GNS.GNSConstants;

public class server{
    //tcp variables
    public int port = -1;
    ServerSocketChannel serverSocketChannel;
    SocketChannel socketChannel;
    ByteBuffer recvBuf;
    Thread readerTrd;


    public server(int port){
        this.port = port;

        //open
        try { this.serverSocketChannel = ServerSocketChannel.open(); } catch (IOException e) { e.printStackTrace(); }

        //bind
        try { serverSocketChannel.socket().bind(new InetSocketAddress(port)); } catch (IOException e) { e.printStackTrace(); }
        System.out.println("EdgeKeeper server is running...");

        //start server thread
        this.readerTrd = new Thread(new Runnable() {
            @Override
            public void run(){

                //create/load DataStore object
                DataStore datastore = DataStore.getInstance();

                try {
                    ByteBuffer sendBuf;
                    while (true) {
                        //server accepts a client, serves it and then moves on to serve the next client.
                        // this is important to ensure data consistency.
                        try { socketChannel = serverSocketChannel.accept(); } catch (IOException e) { e.printStackTrace(); }
                        System.out.println("EdgeKeeper server accepted a client");

                        //set socket timeout during receive
                        try { socketChannel.socket().setSoTimeout((int)EdgeKeeperConstants.readIntervalInMilliSec); } catch (SocketException e) { e.printStackTrace(); }

                        //receive ByteBuffer flipped
                        boolean ret = receive(socketChannel);

                        //check
                        if (ret) {
                            //make string out of recvBuf
                            StringBuilder bd = new StringBuilder();
                            while (recvBuf.remaining() > 0)
                                bd.append((char) recvBuf.get());
                            String str1 = bd.toString();

                            //make EdgeKeeperMetadata object by passing flipped ByteBuffer
                            FileMetadata metadataRec = FileMetadata.parse(str1);

                            //check for command
                            if (metadataRec != null) {
                                if((metadataRec.command == EdgeKeeperConstants.FILE_CREATOR_METADATA_DEPOSIT_REQUEST) || (metadataRec.command == EdgeKeeperConstants.FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST)){
                                    System.out.println("EdgeKeeper server got metadata Deposit request from fragment receiver");

                                    //retrieve the old metadata from DataStore if it even exists
                                    FileMetadata metadataOld = datastore.getFileMetadata(metadataRec.fileID);

                                    //check if METADATA_WITHDRAW_REPLY_FAILED command returned that means a file doesnt exist
                                    if(metadataOld.command == EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST){
                                        //check deleted file list to verify
                                        if(!datastore.deletedFiles.contains(metadataRec.fileID)){
                                            //coming here means its a completely new file metadata.
                                            //in this case, we add all information in all data structures
                                            //in DataStore.

                                            //push the metadata as usual as if a new file
                                            datastore.putFileMetadata(metadataRec);

                                            //push fileName to fileID mapping
                                            datastore.putFileNameToFileIDMap(metadataRec.filename, metadataRec.fileID);

                                            //todo: push fileID to directory mapping information

                                            //push group information
                                            datastore.putGroupNameBYGUID(metadataRec.metadataDepositorGUID, metadataRec.ownGroupNames);

                                            //todo: update MDFS GLOBAL DIRECTORY
                                        }else{
                                            continue;  //fileID belongs to deletedFiles list, meaning this file has already been deleted. so we ignore metadata
                                        }
                                    }else{
                                        //coming here means its a metadata for an old existing file
                                        //in this case we dont change anything, only add the new fragment info into the metadata

                                        //retrieve new block and fragment numbers
                                        String blockNumHeldByThisClient = metadataRec.getBlockNumbersHeldByNode(metadataRec.metadataDepositorGUID).get(0);                                          //this operation will never fail
                                        String fragmentNumHeldByThisClient = metadataRec.getFragmentListByNodeAndBlockNumber(metadataRec.metadataDepositorGUID, blockNumHeldByThisClient).get(0);  //this operation will never fail

                                        //add new block and fragment information into the old metadata object
                                        metadataOld.addInfo(metadataRec.metadataDepositorGUID, blockNumHeldByThisClient, fragmentNumHeldByThisClient);

                                        //push the old metadata object back
                                        datastore.putFileMetadata(metadataOld);

                                        //todo: push fileID to directory mapping information

                                        //push new group information
                                        datastore.putGroupNameBYGUID(metadataRec.metadataDepositorGUID, metadataRec.ownGroupNames);

                                        ////todo: update MDFS GLOBAL DIRECTORY


                                    }


                                }else if (metadataRec.command == EdgeKeeperConstants.METADATA_WITHDRAW_REQUEST) {
                                    System.out.println("EdgeKeeper server got metadata Withdraw request");

                                    //first push group information
                                    datastore.putGroupNameBYGUID(metadataRec.metadataRequesterGUID, metadataRec.ownGroupNames);

                                    //get the requested metadata from dataStore
                                    FileMetadata metadataRet = datastore.getFileMetadata(metadataRec.fileID);

                                    //if file exists
                                    if(metadataRet.command!=EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST){
                                        List<String> permList = Arrays.asList(metadataRet.permissionList);
                                        List<String> groupsMemberOf = datastore.getGroupNamesbyGUID(metadataRec.metadataRequesterGUID);

                                        //If exists, check if withdraw requester node has permission for this metadata
                                        boolean authorized = false;
                                        if(permList.contains("WORLD")){
                                            authorized = true;
                                        }else if((permList.contains("OWNER")) && metadataRet.fileCreatorGUID.equals(metadataRec.metadataRequesterGUID)){
                                            authorized = true;
                                        }else{
                                            for(int i=0; i<groupsMemberOf.size(); i++){
                                                String groupname = groupsMemberOf.get(i);
                                                for(int j=0; j<permList.size(); j++){

                                                    //check for GROUP membership
                                                    if(permList.get(j).equals("GROUP:" + groupname)){
                                                        authorized = true;
                                                    }

                                                    //check if its a GUID and matches with metadata requester
                                                    if( (permList.get(j).length()== GNSConstants.GUID_LENGTH) && (permList.get(j).equals(metadataRec.metadataRequesterGUID))){
                                                        authorized = true;
                                                    }

                                                    if(authorized){break;}
                                                }
                                                if(authorized){break;}
                                            }

                                            if(!authorized){
                                                //we change the command to METADATA_WITHDRAW_REPLY_FAILED_PERMISSIONDENIED of metadata reply
                                                metadataRet.setCommand(EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_PERMISSIONDENIED);
                                            }
                                        }

                                    }else{
                                        //file/metadata doesnt exists so we send back metadata with command = METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST
                                        continue;
                                    }

                                    //convert metadata into json string
                                    String str = metadataRet.toBuffer(metadataRet);

                                    //allocate space for reply
                                    sendBuf = ByteBuffer.allocate(str.length());
                                    sendBuf.order(ByteOrder.LITTLE_ENDIAN);
                                    sendBuf.clear();

                                    //put data in sendBuf
                                    sendBuf.put(str.getBytes());
                                    sendBuf.flip();

                                    //send back
                                    send(sendBuf);
                                } else if (metadataRec.command == EdgeKeeperConstants.GROUP_TO_GUID_CONV_REQUEST) {

                                    System.out.println("EdgeKeeper server got Group to GUID Conversion request(C)");
                                    List<String> groupnames = metadataRec.groupOrGUID;

                                    //first push group information
                                    datastore.putGroupNameBYGUID(metadataRec.groupConversionRequesterGUID, metadataRec.ownGroupNames);

                                    //make a GUIDList to return
                                    List<String> listofResultantGUIDS = new ArrayList<>();

                                    //get all GUIDs who are member of at least one group
                                    List<String> allGUIDsBelongsToAnyGroup = new ArrayList(datastore.GUIDtoGroupNamesMap.keySet());

                                    //check and find
                                    for (int i = 0; i < groupnames.size(); i++) {
                                        String groupname = groupnames.get(i);
                                        //trace all GUIDs who belong to this groupname
                                        for (int j = 0; j < allGUIDsBelongsToAnyGroup.size(); j++) {
                                            String guid = allGUIDsBelongsToAnyGroup.get(j);
                                            if (datastore.GUIDtoGroupNamesMap.get(guid).contains(groupname)) {
                                                listofResultantGUIDS.add(guid);
                                            }
                                        }
                                    }


                                    //make reply metadata object
                                    FileMetadata metadataRet;
                                    if (listofResultantGUIDS.size() != 0) {
                                        metadataRet = new FileMetadata(EdgeKeeperConstants.GROUP_TO_GUID_CONV_REPLY_SUCCESS, new Date().getTime(), new ArrayList<>(), metadataRec.groupConversionRequesterGUID, listofResultantGUIDS);
                                    } else {
                                        metadataRet = new FileMetadata(EdgeKeeperConstants.GROUP_TO_GUID_CONV_REPLY_FAILED, new Date().getTime(), new ArrayList<>(), metadataRec.groupConversionRequesterGUID, listofResultantGUIDS);
                                    }

                                    //convert metadata into json string
                                    String str = metadataRet.toBuffer(metadataRet);

                                    //allocate space for reply
                                    sendBuf = ByteBuffer.allocate(str.length());
                                    sendBuf.order(ByteOrder.LITTLE_ENDIAN);
                                    sendBuf.clear();

                                    //put data in sendBuf
                                    sendBuf.put(str.getBytes());
                                    sendBuf.flip();

                                    //send back
                                    send(sendBuf);

                                    //dummy sleep for interrupt
                                    Thread.sleep(0);

                                }
                            }

                        }else{
                            socketChannel.close();
                        }

                    }
                }catch(InterruptedException | IOException e){
                    e.printStackTrace();
                }

            }
        });
        readerTrd.start();

    }//constructor


    //this function takes an already flipped byteBuffer and sends
    //the input buffer must be LITTLE_ENDIAN
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

    //receive function
    //returns true if receive succeeded and populates recvBuf
    //returns false if receive failed.
    //this function already flipps the buffer
    //the output buffer is LITTLE_ENDIAN
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
            try { r = socket.read(size); } catch(SocketTimeoutException time){return false;} catch (IOException e) { return false;}
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
            try { r = socket.read(recv); }  catch(SocketTimeoutException time){return false;} catch (IOException e) { return false;}
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

        //return values
        if(recv.limit()>0){ return true; } else{ return false; }

    }

    public void close(){
        try {
            serverSocketChannel.close();
            socketChannel.close();
            readerTrd.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}


