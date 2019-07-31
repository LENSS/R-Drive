package edu.tamu.lenss.mdfs.EdgeKeeper;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.utils.Logger;

//this class contains request and reply codes for all edgekeeper communications.
//these codes are used in EdgeKeeperMetadata class.
public class EdgeKeeperConstants {

    //app context
    public static Context context;

    //EdgeKeeper variables
    public static final String dummy_EdgeKeeper_ip = "192.168.1.45";
    public static final int dummy_EdgeKeeper_port = 9995;

    //guid variables
    public static String dummyGUID = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    public static int GUID_LENGTH = 40;

    //edgekeeper socket variables
    public static final long readIntervalInMilliSec = 3000;


    //depositing metadata BY FILE CREATOR to the EdgeKeeper commmands
    public static int FILE_CREATOR_METADATA_DEPOSIT_REQUEST = 10;
    public static int FILE_CREATOR_METADATA_DEPOSIT_REPLY_SUCCESS = 20;
    public static int FILE_CREATOR_METADATA_DEPOSIT_REPLY_FAILED = 30;

    //depositing metadata BY Fragment receiver to the EdgeKeeper commmands
    public static int FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST = 40;
    public static int FRAGMENT_RECEIVER_METADATA_DEPOSIT_REPLY_SUCCESS = 50;
    public static int FRAGMENT_RECEIVER_METADATA_DEPOSIT_REPLY_FAILED = 60;

    //withdrawing metadata from the EdgeKeeper commands
    public static int METADATA_WITHDRAW_REQUEST = 70;
    public static int METADATA_WITHDRAW_REPLY_SUCCESS = 80;
    public static int METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST = 90;
    public static int METADATA_WITHDRAW_REPLY_FAILED_PERMISSIONDENIED = 100;
    public static int METADATA_WITHDRAW_REPLY_FAILED_DIRNOTEXIST = 105;

    //group tp GUID conversion commands
    public static int GROUP_TO_GUID_CONV_REQUEST = 110;
    public static int GROUP_TO_GUID_CONV_REPLY_SUCCESS = 120;
    public static int GROUP_TO_GUID_CONV_REPLY_FAILED = 130;

    //own group information submission commands
    public static int GROUP_INFO_SUBMISSION_REQUEST = 140;

    //directory request commands
    public static int CREATE_MDFS_DIR_REQUEST = 150;
    public static int CREATE_MDFS_DIR_REPLY_SUCCESS = 160;
    public static int CREATE_MDFS_DIR_REPLY_FAILED= 170;

    //remove direcotyr or file commands
    public static int REMOVE_MDFS_DIR_REQUEST = 180;
    public static int REMOVE_MDFS_DIR_REPLY_SUCCESS = 190;
    public static int REMOVE_MDFS_DIR_REPLY_FAILED = 200;

    public static int REMOVE_MDFS_FILE_REQUEST = 210;
    public static int REMOVE_MDFS_FILE_REPLY_SUCCESS = 220;
    public static int REMOVE_MDFS_FILE_REPLY_FAILED = 230;

    public static int GET_MDFS_FILES_AND_DIR_REQUEST = 240;
    public static int GET_MDFS_FILES_AND_DIR_REPLY_SUCCESS = 250;
    public static int GET_MDFS_FILES_AND_DIR_REPLY_FAILED = 260;


    //command if connecting to EdgeKeeper failed
    public static int EDGEKEEPER_CONNECTION_FAILED = 270;

    //MDFS group info temporarily stored here todo: this needs to be written on disk
    public static List<String> getMyGroupName(){
        if(GNS.ownGUID.equals("1BC657F8971A53E9BD90C285EB17C9080EC3EB8E")){
            String[] firstArr = {"ABCD", "EFGH"};
            List<String> first = Arrays.asList(firstArr);
            return  first;
        }else if(GNS.ownGUID.equals("8877417A2CBA0D19636B44702E7DB497B5834559")){
            String[] secondArr = {"EFGH", "IJKL"};
            List<String> second = Arrays.asList(secondArr);
            return second;
        }else if(GNS.ownGUID.equals("D9C6D170C3C5E6032D0C06D8C495C4E0BB769278")){
            String[] thirdArr = {"IJKL", "ABCD"};
            List<String> third = Arrays.asList(thirdArr);
            return third;
        }

        return new ArrayList<>();
    }

    public static String getWifiIP(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null)
            return null;

        WifiInfo wInfo = wifiManager.getConnectionInfo();
        if(wInfo == null)
            return null;

        int ipAddress = wInfo.getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = null;
        }
        return ipAddressString;
    }

}
