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
import java.util.Date;
import java.util.List;

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

                //create/load Directory object
                Directory directory = Directory.getInstance();

                try {
                    ByteBuffer sendBuf;
                    while (true) {
                        //server accepts a client, serves it and then moves on to serve the next client.
                        // this is important to ensure data consistency.
                        try { socketChannel = serverSocketChannel.accept(); } catch (IOException e) { e.printStackTrace(); }
                        System.out.println("EdgeKeeper server accepted a client");

                        //set socket timeout during receive
                        //try { socketChannel.socket().setSoTimeout((int)EdgeKeeperConstants.readIntervalInMilliSec); } catch (SocketException e) { e.printStackTrace(); }

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

                            //check if null
                            if (metadataRec != null) {

                                //check for command
                                if((metadataRec.command == EdgeKeeperConstants.FILE_CREATOR_METADATA_DEPOSIT_REQUEST) || (metadataRec.command == EdgeKeeperConstants.FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST)){
                                    System.out.println("EdgeKeeper server got metadata Deposit request from fragment receiver");

                                    //first check if the metadataRec reqUniqueID belongs to deleted files,
                                    //that means the file has been deleted
                                    if(directory.deletedFiles.contains(metadataRec.uniqueReqID)){
                                        continue;   //the file has already been deleted so we ignore this metadata

                                    } else {

                                        //if file not deleted then check if file exists already or nah
                                        if (directory.fileExists(metadataRec.filename, metadataRec.filePathMDFS) && directory.getFileMetadata(metadataRec.filename, metadataRec.filePathMDFS).filePathMDFS.equals(metadataRec.filePathMDFS)) {

                                            //coming here means a file metadata for this file already exists
                                            //fetch the old file metadata
                                            FileMetadata metadataOld = directory.getFileMetadata(metadataRec.filename, metadataRec.filePathMDFS);

                                            //retrieve new block and fragment numbers
                                            String blockNumHeldByThisClient = metadataRec.getBlockNumbersHeldByNode(metadataRec.metadataDepositorGUID).get(0);                                          //this operation will never fail
                                            String fragmentNumHeldByThisClient = metadataRec.getFragmentListByNodeAndBlockNumber(metadataRec.metadataDepositorGUID, blockNumHeldByThisClient).get(0);  //this operation will never fail

                                            //add new block and fragment information into the old metadata object
                                            metadataOld.addInfo(metadataRec.metadataDepositorGUID, blockNumHeldByThisClient, fragmentNumHeldByThisClient);

                                            //remove old file metadata from directory
                                            directory.removefileMetadata(metadataRec.filename, metadataRec.filePathMDFS);

                                            //push the old metadata object back
                                            directory.putFileMetadata(metadataOld, metadataOld.filename, metadataOld.filePathMDFS);


                                        } else {

                                            //coming here means this is a totally new file metadata
                                            //put file metadata as usual
                                            directory.putFileMetadata(metadataRec, metadataRec.filename, metadataRec.filePathMDFS);

                                        }
                                    }


                                }else if (metadataRec.command == EdgeKeeperConstants.METADATA_WITHDRAW_REQUEST) {
                                    System.out.println("EdgeKeeper server got metadata Withdraw request");

                                    //return object
                                    FileMetadata metadataRet = null;

                                    //get all the required variables
                                    String dir = metadataRec.filePathMDFS;
                                    String filename = metadataRec.filename;

                                    //check if the directory exists to begin with
                                    if(directory.dirExists(dir)){

                                        //check if the file exists in the dir
                                        if(directory.fileExists(filename, dir)){

                                            //get the file metadata
                                            metadataRet = directory.getFileMetadata(filename, dir);

                                            //change the command of the metadata
                                            metadataRet.setCommand(EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_SUCCESS);


                                        }else{

                                            //dir exists but the file doesnt exist
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST, "a file of name " + metadataRec.filename + " doesnt exists in the directory " + metadataRec.filePathMDFS);

                                        }
                                    }else{

                                        //directory doesnt exists to begin with
                                        metadataRet = new FileMetadata(EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_DIRNOTEXIST, "directory " + metadataRec.filePathMDFS + " doesnt exist.");

                                    }

                                    //send back the reply
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


                                }else if(metadataRec.command==EdgeKeeperConstants.CREATE_MDFS_DIR_REQUEST){

                                    //reply object
                                    FileMetadata metadataRet;

                                    //check if the requested directory already exists
                                    if(directory.dirExists(metadataRec.filePathMDFS)){

                                        //directory already exists, create dummy obj with command = CREATE_MDFS_DIR_REPLY_FAILED
                                        metadataRet = new FileMetadata(EdgeKeeperConstants.CREATE_MDFS_DIR_REPLY_FAILED, "MDFS directory already exists.");

                                    }else{

                                        //directory doesnt exists so we create it
                                        boolean result = directory.addDirectory(metadataRec.filePathMDFS);

                                        //create obj with command = CREATE_MDFS_DIR_REPLY_SUCCESS
                                        if(result) {
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.CREATE_MDFS_DIR_REPLY_SUCCESS, new Date().getTime(), new ArrayList<>(), metadataRec.mdfsdirectoryJObREquesterGUID, metadataRec.filePathMDFS, "success");
                                        }else{
                                            //directory creation failed for some reason, create dummy obj with command = CREATE_MDFS_DIR_REPLY_FAILED
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.CREATE_MDFS_DIR_REPLY_FAILED, "arbitrary error.");
                                        }
                                    }

                                    //send back the reply
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
                                }else if(metadataRec.command == EdgeKeeperConstants.REMOVE_MDFS_DIR_REQUEST){
                                    System.out.println("EdgeKeeper server got remove directory request");
                                    //note: a dir deletion means all of both files and folders to be deleted

                                    //reply object
                                    FileMetadata metadataRet= null;

                                    //check if the dir is /*
                                    if(metadataRec.filePathMDFS.equals("/*")){

                                        //request for deleting all the files and folders in root dir
                                       boolean result = directory.removeDirectory(metadataRec.filePathMDFS);

                                        if (result) {

                                            //files and folders deletion at root success
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_DIR_REPLY_SUCCESS, new Date().getTime(), new ArrayList<>(), metadataRec.removeRequesterGUID, metadataRec.filePathMDFS, metadataRec.filename, "success");
                                        } else {

                                            //file and folder at root deletion failed for some reason
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_DIR_REPLY_FAILED, "arbitrary error.");


                                        }

                                    }else {

                                        //request for deleting any of the subDir
                                        //check if the dir already exists or nah
                                        if (directory.dirExists(metadataRec.filePathMDFS)) {

                                            //if dir exists, delete it
                                            boolean result = directory.removeDirectory(metadataRec.filePathMDFS);

                                            if (result) {

                                                //dir deletion success
                                                metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_DIR_REPLY_SUCCESS, new Date().getTime(), new ArrayList<>(), metadataRec.removeRequesterGUID, metadataRec.filePathMDFS, metadataRec.filename, "success");
                                            } else {

                                                //dir deletion failed for some reason
                                                metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_DIR_REPLY_FAILED, "arbitrary error.");

                                            }

                                        } else {

                                            //dir doesnt even exist
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_DIR_REPLY_FAILED, "MDFS directory doesnt exist.");

                                        }
                                    }

                                    //send back the reply
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

                                }else if(metadataRec.command == EdgeKeeperConstants.REMOVE_MDFS_FILE_REQUEST){
                                    System.out.println("EdgeKeeper server got remove file request");
                                    //note: a file deletion means only that file to be deleted, but nothing else in that dir.

                                    //reply object
                                    FileMetadata metadataRet;

                                    //check if dir already exists or nah
                                    if(directory.dirExists(metadataRec.filePathMDFS)){

                                        //check if file in the dir exists
                                        if(directory.fileExists(metadataRec.filename, metadataRec.filePathMDFS)){

                                            //delete the file
                                            boolean result = directory.removefileMetadata(metadataRec.filename, metadataRec.filePathMDFS);

                                            if(result){
                                                //file deletion success
                                                metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_FILE_REPLY_SUCCESS, new Date().getTime(), new ArrayList<>(), metadataRec.removeRequesterGUID ,  metadataRec.filePathMDFS, metadataRec.filename, "success" );

                                            }else{
                                                //for some reason file deletion failed
                                                metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_FILE_REPLY_FAILED, "arbitrary error.");

                                            }

                                        }else{
                                            //dir exists but file doesnt
                                            metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_FILE_REPLY_FAILED, "MDFS directory exists but file doesnt exist.");
                                        }

                                    }else{
                                        //dir doest even exist
                                        metadataRet = new FileMetadata(EdgeKeeperConstants.REMOVE_MDFS_FILE_REPLY_FAILED, "MDFS directory doesnt exist.");
                                    }

                                    //send back the reply
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


                                }else if(metadataRec.command == EdgeKeeperConstants.GET_MDFS_FILES_AND_DIR_REQUEST){
                                    System.out.println("EdgeKeeper server got list files and folders request");

                                    //reply object
                                    FileMetadata metadataRet;

                                    //check if dir even exists or nah
                                    if(directory.dirExists(metadataRec.filePathMDFS)){

                                        //ge the files and folders in a String
                                        String result= "///<newline><newline>" ;
                                        List<String> files = directory.getAllFileNames(metadataRec.filePathMDFS);
                                        List<String> folders = directory.getAllDirNames(metadataRec.filePathMDFS);

                                        for(int i=0; i<files.size(); i++){
                                            result = result + "file:   " + files.get(i) + "<newline>";
                                        }

                                        for(int i=0; i<folders.size(); i++){
                                            result = result + "folder: " + folders.get(i) + "<newline>";
                                        }
                                        result = result + "<newline>///";

                                        metadataRet = new FileMetadata(EdgeKeeperConstants.GET_MDFS_FILES_AND_DIR_REPLY_SUCCESS, new Date().getTime(), new ArrayList<>(), metadataRec.mdfsdirectoryJObREquesterGUID, metadataRec.filePathMDFS, result);

                                    }else{
                                        metadataRet = new FileMetadata(EdgeKeeperConstants.GET_MDFS_FILES_AND_DIR_REPLY_FAILED, "MDFS directory doesnt exist.");

                                    }

                                    //send back the reply
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

                                }else{
                                    //not implemented yet
                                }

                            }

                        }else{
                            socketChannel.close();
                        }

                        //dummy sleep for interrupt
                        Thread.sleep(0);
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
            try { r = socket.read(size); } catch(SocketTimeoutException time){time.printStackTrace(); return false;} catch (IOException e) { return false;}
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
            try { r = socket.read(recv); }  catch(SocketTimeoutException time){time.printStackTrace(); return false;} catch (IOException e) { return false;}
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


