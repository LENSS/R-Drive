package edu.tamu.cse.lenss.mdfs_api_server;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import edu.tamu.cse.lenss.android.MainActivity;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;

public class APIserver extends Thread {

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
        try { ds= new DatagramSocket(MDFS_API_PORT,InetAddress.getByName("127.0.0.1")); } catch (Exception e) { e.printStackTrace(); }

        //init buffer
        byte[] receive = new byte[64000];
        DatagramPacket DpReceive = null;

        System.out.println("MDFS_API_SERVER is running...");

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

                //check
                if(!req.get(LOCALFILEPATH).equals("") && !req.get(MDFSFILEPATH).equals("")) {

                    //execute request command
                    String command = "mdfs -put " + req.getString(LOCALFILEPATH) + " " + req.getString(MDFSFILEPATH);

                    //execute command using a callable
                    if(RSockConstants.RSOCK) {
                        MainActivity.Foo(command, MainActivity.context);
                    }else{
                        System.out.println("MDFS API server error! Could not -put in MDFS, Rsock is down!");
                    }
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Clear the buffer after every message.
            receive = new byte[64000];
        }
    }

    @Override
    public void interrupt(){
        isRunning = false;
        ds.close();
        super.interrupt();
    }


}
