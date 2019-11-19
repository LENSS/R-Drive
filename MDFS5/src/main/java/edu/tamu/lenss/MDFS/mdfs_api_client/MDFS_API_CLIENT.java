package edu.tamu.lenss.MDFS.mdfs_api_client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

//this is the API used by a client,
//to use MDFS as a fileSystem.
public class MDFS_API_CLIENT {

    //static fields
    public static String LOCALFILEPATH = "LOCALFILEPATH";
    public static String MDFSFILEPATH = "MDFSFILEPATH";
    public static int MDFS_API_PORT = 5153;

    //general fields
    private BlockingQueue<JSONObject> Queue;
    private MDFS_Thread mdfs_thread;
    private boolean isClosed;


    //default public constructor
    public MDFS_API_CLIENT(){
        this.Queue = new LinkedBlockingDeque<JSONObject>();
        this.mdfs_thread = new MDFS_Thread();
        this.mdfs_thread.start();
        this.isClosed = false;
    }

    //QueueToSend function
    public boolean QueueToSend(String filepathWithName, String mdfsDirectory){

        if(!this.isClosed) {
            //make a MDFS request packet
            JSONObject reqPacket = new JSONObject();

            //add parameters
            try {
                reqPacket.put(LOCALFILEPATH, filepathWithName);
                reqPacket.put(MDFSFILEPATH, mdfsDirectory);
            }catch (JSONException e){
                e.printStackTrace();
            }

            //put it in the queue
            try {
                this.Queue.put(reqPacket);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return false;
    }


    //close now
    public void closeNow(){
        if(!this.isClosed) {
            this.isClosed = true;
            this.mdfs_thread.close();
            this.Queue.clear();
        }
    }

    //close when cached data is processed.
    public void closeWhenDone(){
        if(!this.isClosed){
            this.isClosed = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        if(Queue.size()==0){
                            closeNow();
                            break;
                        }else{
                            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                    }
                }
            }).start();
        }
    }



    //MDFS Thread class
    private class MDFS_Thread extends Thread {

        private boolean isRunning;
        private DatagramSocket ds;
        private InetAddress ip;

        public MDFS_Thread(){
            this.isRunning = true;
            try { this.ds = new DatagramSocket();} catch (SocketException e) { e.printStackTrace();}
            try { this.ip = InetAddress.getLocalHost();} catch (UnknownHostException e) { e.printStackTrace();}
        }

        @Override
        public void run(){

            while(isRunning && !Thread.currentThread().isInterrupted()){

                //take a packet
                try {
                    JSONObject packet = Queue.take();

                    //make string and then make byte[]
                    byte[] data = packet.toString().getBytes();

                    //make UDP packet
                    DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

                    //send via UDP
                    ds.send(DpSend);

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void close(){
            this.isRunning = false;
            super.interrupt();
        }
    }
}
