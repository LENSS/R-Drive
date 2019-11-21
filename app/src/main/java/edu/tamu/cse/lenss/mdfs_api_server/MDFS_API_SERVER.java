package edu.tamu.cse.lenss.mdfs_api_server;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import edu.tamu.cse.lenss.android.MainActivity;

public class MDFS_API_SERVER extends Thread {

    //constant static fields
    public static String LOCALFILEPATH = "LOCALFILEPATH";
    public static String MDFSFILEPATH = "MDFSFILEPATH";
    public static int MDFS_API_PORT = 5153;


    public boolean isRunning = false;
    private DatagramSocket ds;
    private InetAddress ip;

    @Override
    public void run(){

        //change boolean
        this.isRunning = true;

        //init server
        try { ds= new DatagramSocket(MDFS_API_PORT); } catch (SocketException e) { e.printStackTrace(); }

        //init buffer
        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = null;

        while(isRunning){

            //create a DatgramPacket to receive the data.
            DpReceive = new DatagramPacket(receive, receive.length);

            //receive the data in byte buffer.
            try {
                ds.receive(DpReceive);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //convert bytes into string, then JSONObject
            try {
                //get reqJSON
                JSONObject req = new JSONObject(new String(DpReceive.getData()));

                //execute request
                String ret = MainActivity.Foo("mdfs -put " + req.get(LOCALFILEPATH) + " " + req.get(MDFSFILEPATH));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Clear the buffer after every message.
            receive = new byte[65535];
        }
    }

    @Override
    public void interrupt(){
        isRunning = false;
        super.interrupt();
    }


}
