package edu.tamu.lenss.mdfs.EdgeKeeper;

import android.text.method.NumberKeyListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static java.lang.Thread.sleep;

//first connect() must be called
//second setsocketReadTimeout should be called if want to set a read timeout
//third send() or receive() functions should be called
//fourth, close() function must be called
public class client{
    public String serverIP;
    public int port = -1;
    public SocketChannel socket;
    public boolean isConnected = false;

    public client(String serverip, int port){
        this.serverIP = serverip;
        this.port = port;
    }

    //connects the socket and returns true if success
    public boolean connect(){
        if(socket==null){
            try { this.socket = SocketChannel.open();
                this.socket.connect(new InetSocketAddress(serverIP, port));
            } catch (IOException e) { e.printStackTrace(); }
            if(socket!=null){
                isConnected = true;
                System.out.println("EdgeKeeper client Socket is connected");
                return true;
            }else{
                System.out.println("EdgeKeeper client Socket is not connected");
                return false;
            }
        }
        return false;
    }

    public void setSocketReadTimeout(){
        try { socket.socket().setSoTimeout((int) EdgeKeeperConstants.readIntervalInMilliSec); } catch (SocketException e) { e.printStackTrace(); }
    }

    public void close(){
        isConnected = false;
        try { socket.close(); System.out.println("EdgeKeeper client Socket is closed"); } catch (IOException e) { e.printStackTrace(); }
    }

    //this function already takes a flipped buffer and sends them
    //this input buffer must be LITTLE_ENDIAN
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
        if(socket.isConnected()) {
            try {
                w = socket.write(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (w > 0) {
                System.out.println("EdgeKeeper client Data sent: " + w);
            }
        }else{
            System.out.println("EdgeKeeper client Socket is not connected");
        }
    }

    //receive data from server.
    //if succeed, returns a bytebuffer.
    //if fails, returns null.
    //this function already flips the buffer, if a buffer is being returned.
    //the output buffer is LITTLE_ENDIAN.
    public <T> ByteBuffer receive(){

        //first, read only Long.BYTES amount to figure out the total receive size
        System.out.println("EdgeKeeper client waiting for reply");
        long size_of_recv = 0;
        ByteBuffer size = ByteBuffer.allocate( Long.BYTES);
        size.order(ByteOrder.LITTLE_ENDIAN);
        size.clear();

        //read only Long.BYTES amount
        int iii = 0;
        do{
            int r = 0;
            try { r = socket.read(size); } catch(SocketTimeoutException time){return null;} catch (IOException e) { return null;}
            if(r>0) {
                iii = iii + r;
            }
        }while(size.position()<Long.BYTES);
        size.flip();


        //convert size buffer into a long
        size_of_recv = size.getLong();
        size.rewind();


        // when size_of_recv is known, create a data buffer of size of size_of_recv
        ByteBuffer recv = ByteBuffer.allocate((int) size_of_recv);
        recv.order(ByteOrder.LITTLE_ENDIAN);
        recv.clear();

        // then read only size_of_recv much bytes from socket
        int ii = 0;
        do{
            int r = 0;
            try { r = socket.read(recv); } catch(SocketTimeoutException time){return null;} catch (IOException e) { return null;}
            if(r>0){
                ii = ii+ r;
            }
        }while(recv.position()<size_of_recv);
        recv.flip();


        //print
        System.out.println("EdgeKeeper client socket read: " + (Long.BYTES + (int) recv.limit()));

        //allocte return packet
        ByteBuffer recvBuf = ByteBuffer.allocate(Long.BYTES + recv.limit());
        recvBuf.order(ByteOrder.LITTLE_ENDIAN);
        recvBuf.clear();

        //put all the data in the staticrecvBuf
        for(int i=0; i< recv.limit(); i++){ recvBuf.put(recv.get(i)); }
        recvBuf.flip();

        //return
        return recvBuf;
    }

    //this function should only be used when manually connecting to the endgekeeper fails
    //this function only handles EdgeKeeperMetadata of command types FILE_CREATOR_METADATA_DEPOSIT_REQUEST, FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST, GROUP_INFO_SUBMISSION_REQUEST.
    //this function should only be used for one way data sending(that is from client to EdgeKeeper) and only when a return is not expected.
    public static void putInDTNQueue(EdgeKeeperMetadata metadata, int minutesInterval){
        if(metadata.command==EdgeKeeperConstants.FILE_CREATOR_METADATA_DEPOSIT_REQUEST ||
                metadata.command==EdgeKeeperConstants.FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST  ||
                metadata.command==EdgeKeeperConstants.GROUP_INFO_SUBMISSION_REQUEST ){
            DTN dtn = new DTN(metadata, minutesInterval);
            dtn.start();
        }
    }

    private static class DTN extends Thread{
        EdgeKeeperMetadata metadata;
        long intervalInMilliSeconds;
        long interval;

        public DTN(EdgeKeeperMetadata metadata, int intervalInMinutes){
            this.metadata = metadata;
            this.intervalInMilliSeconds = intervalInMinutes * 60 * 1000;
            this.interval = 500;
        }

        @Override
        public void run(){
            client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);

            boolean connected = false;
            long durationWaited = 0;

            while(!connected){
                //connect
                connected = client.connect();

                //make and check if connection succeeded otherwise sleep
                if(connected){
                    break;
                }else{
                    Sleep((int)interval);
                    durationWaited = durationWaited + interval;
                }

                if((durationWaited >= intervalInMilliSeconds)) {break;}
            }

            //set socket read timeout
            client.setSocketReadTimeout();

            if(connected && client.socket.isConnected()){

                //set socket read timeout(not doing anything here)
                client.setSocketReadTimeout();

                //convert into json string
                String str = metadata.toBuffer(metadata);

                //create bytebuffer
                ByteBuffer sendBuf = ByteBuffer.allocate(str.length());
                sendBuf.order(ByteOrder.LITTLE_ENDIAN);
                sendBuf.clear();

                //put str in sendBuf and flip
                sendBuf.put(str.getBytes());
                sendBuf.flip();

                //send metadata req
                client.send(sendBuf);

                //close client socket
                client.close();
            }
        }

    }

    public static void Sleep(int intervalInMilli){
        try { sleep(intervalInMilli); } catch (InterruptedException e) { e.printStackTrace(); }
    }
}
