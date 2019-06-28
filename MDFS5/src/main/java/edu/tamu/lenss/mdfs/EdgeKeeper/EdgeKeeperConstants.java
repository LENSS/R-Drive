package edu.tamu.lenss.mdfs.EdgeKeeper;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.tamu.lenss.mdfs.GNS.GNS;

//this class contains request and reply codes for all edgekeeper communications.
//these codes are used in EdgeKeeperMetadata class.
public class EdgeKeeperConstants {

    //EdgeKeeper variables
    public static String my_wifi_ip_temp = "";
    public static final String dummy_EdgeKeeper_ip = "192.168.0.2";
    public static final int dummy_EdgeKeeper_port = 9995;


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
    public static int METADATA_WITHDRAW_REPLY_FAILED = 90;

    //group tp GUID conversion commands
    public static int GROUP_TO_GUID_CONV_REQUEST = 100;
    public static int GROUP_TO_GUID_CONV_REPLY_SUCCESS = 110;
    public static int GROUP_TO_GUID_CONV_REPLY_FAILED = 120;

    //own group information submission commands
    public static int GROUP_INFO_SUBMISSION_REQUEST = 130;

    //directory fetch request commands
    public static int FETCH_ALL_FILES_REQUEST = 140;
    public static int FETCH_ALL_FILES_REPLY_SUCCESS = 150;
    public static int FETCH_ALL_FILES_REPLY_FAILED = 160;

    //command if connecting to EdgeKeeper failed
    public static int EDGEKEEPER_CONNECTION_FAILED = 170;

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



}
