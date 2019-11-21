package edu.tamu.cse.lenss.mdfs_api_client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

//this is the API used by a client,
//to use MDFS as a fileSystem.
public class MDFS_API_CLIENT {

    //constant static fields
    public static String LOCALFILEPATH = "LOCALFILEPATH";
    public static String MDFSFILEPATH = "MDFSFILEPATH";
    public static int MDFS_API_PORT = 5153;

    //put function
    public static void put(String filepathWithName, String mdfsDirectory){

        //make a MDFS request packet
        JSONObject reqPacket = new JSONObject();

        //add parameters
        try {
            reqPacket.put(LOCALFILEPATH, filepathWithName);
            reqPacket.put(MDFSFILEPATH, mdfsDirectory);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //make string and then make byte[]
        byte[] data = reqPacket.toString().getBytes();

        try{

            //init socket and ip
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getLocalHost();

            //make UDP packet
            DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

            //send
            ds.send(DpSend);

        }catch(IOException e){
            e.printStackTrace();
        }

    }

}
