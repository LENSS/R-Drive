package edu.tamu.cse.lenss.mdfs_api_client;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

//this is the API used by a client,
//to use MDFS as a fileSystem.
public class MDFS_API_CLIENT {

    //constant static fields
    private static String LOCALFILEPATH = "LOCALFILEPATH";
    private static String MDFSFILEPATH = "MDFSFILEPATH";
    private static String OWNGUID = "OWNGUID";
    private static String ITEMS = "ITEMS";
    private static int MDFS_API_PORT = 5153;


    /**
     * put a file from local storage to MDFS
     * @param local file path location in storage
     * @param mdfs directory path
     * @param this device GUID
     * @param list of permitted GUIDs
     *
     * */
    public static boolean put(String localFilePath, String mdfsDirectory, String ownGUID, List<String> permissionList){

        //make a MDFS request packet
        JSONObject reqPacket = new JSONObject();

        if(permissionList==null || permissionList.size()==0){
            permissionList.add(ownGUID);
        }

        //add parameters
        try {
            reqPacket.put(LOCALFILEPATH, localFilePath);
            reqPacket.put(MDFSFILEPATH, mdfsDirectory);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //make string and then make byte[]
        byte[] data = reqPacket.toString().getBytes();

        try{

            //init socket and ip
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName("127.0.0.1");

            //make UDP packet
            DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

            //send
            ds.send(DpSend);

        }catch(IOException e){
            e.printStackTrace();
        }

        return true;

    }

    /**
     * @param mdfs file path
     * @param local file path after file retrieval
     * @param ownGUID
     *
     * */
    public static boolean get(String mdfsFilePath, String localFilePath, String ownGUID){

        //make a MDFS request packet
        JSONObject reqPacket = new JSONObject();

        //add parameters
        try {
            reqPacket.put(LOCALFILEPATH, localFilePath);
            reqPacket.put(MDFSFILEPATH, mdfsFilePath);
            reqPacket.put(OWNGUID, ownGUID);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //make string and then make byte[]
        byte[] data = reqPacket.toString().getBytes();

        try{

            //init socket and ip
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName("127.0.0.1");

            //make UDP packet
            DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

            //send
            ds.send(DpSend);

        }catch(IOException e){
            e.printStackTrace();
        }

        return true;


    }


    /**
     * Delete a file or directory from MDFS.
     * @param directory or file in MDFS
     * @param this device GUID
     * @returns true in success.
     * */
    public static boolean delete(String mdfsDirectory, String ownGUID){

        //make a MDFS request packet
        JSONObject reqPacket = new JSONObject();

        //add parameters
        try {
            reqPacket.put(MDFSFILEPATH, mdfsDirectory);
            reqPacket.put(OWNGUID, ownGUID);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //make string and then make byte[]
        byte[] data = reqPacket.toString().getBytes();

        try{

            //init socket and ip
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName("127.0.0.1");

            //make UDP packet
            DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

            //send
            ds.send(DpSend);

        }catch(IOException e){
            e.printStackTrace();
        }

        return true;

    }

    /**
     * Create a direcotory in MDFS.
     * @param directory or file in MDFS
     * @param this device GUID
     * */
    public static boolean mkdir(String mdfsDirectory, String ownGUID){

        //make a MDFS request packet
        JSONObject reqPacket = new JSONObject();

        //add parameters
        try {
            reqPacket.put(MDFSFILEPATH, mdfsDirectory);
            reqPacket.put(OWNGUID, ownGUID);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //make string and then make byte[]
        byte[] data = reqPacket.toString().getBytes();

        try{

            //init socket and ip
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName("127.0.0.1");

            //make UDP packet
            DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

            //send
            ds.send(DpSend);

        }catch(IOException e){
            e.printStackTrace();
        }

        return true;

    }

    /**
     * list a directory in MDFS.
     * @param directory or file in MDFS
     * @param this device GUID
     * @returns List of items
     * */
    public List<String> ls(String mdfsDirectory, String ownGUID){

        //make a MDFS request packet
        JSONObject reqPacket = new JSONObject();

        //add parameters
        try {
            reqPacket.put(MDFSFILEPATH, mdfsDirectory);
            reqPacket.put(OWNGUID, ownGUID);
        }catch (JSONException e){
            e.printStackTrace();
        }

        //make string and then make byte[]
        byte[] data = reqPacket.toString().getBytes();

        try{

            //init socket and ip
            DatagramSocket ds = new DatagramSocket();
            InetAddress ip = InetAddress.getByName("127.0.0.1");

            //make UDP packet
            DatagramPacket DpSend = new DatagramPacket(data, data.length, ip, MDFS_API_PORT);

            //send
            ds.send(DpSend);

            return new ArrayList<>();

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

}
