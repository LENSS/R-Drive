package edu.tamu.lenss.mdfs.EdgeKeeper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

public class client{
    public String serverIP;
    public int port = -1;
    public SocketChannel socket;
    public boolean isConnected = false;

    public client(String serverip, int port){
        this.serverIP = serverip;
        this.port = port;
    }

    public void connect(){
        if(socket==null){
            try { this.socket = SocketChannel.open();
                this.socket.connect(new InetSocketAddress(serverIP, port));
            } catch (IOException e) { e.printStackTrace(); }
            if(socket!=null){
                isConnected = true;
                System.out.println("EdgeKeeper client Socket is connected");
            }else{System.out.println("EdgeKeeper client Socket is not connected");}
        }
    }

    public void close(){
        isConnected = false;
        try { socket.close(); System.out.println("EdgeKeeper client Socket is closed"); } catch (IOException e) { e.printStackTrace(); }
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

    public ByteBuffer receive(){
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
            try { r = socket.read(size); } catch (IOException e) { e.printStackTrace(); }
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
            try { r = socket.read(recv); } catch (IOException e) { e.printStackTrace(); }
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

        //put all the data in the recvBuf
        for(int i=0; i< recv.limit(); i++){ recvBuf.put(recv.get(i)); }
        recvBuf.flip();

        //return
        return recvBuf;
    }
}
