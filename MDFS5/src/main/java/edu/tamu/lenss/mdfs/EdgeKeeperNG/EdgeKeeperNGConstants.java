package edu.tamu.lenss.mdfs.EdgeKeeperNG;


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

//this class contains request and reply codes for all edgekeeper communications.
//these codes are used in EdgeKeeperMetadata class.
public class EdgeKeeperNGConstants {

    //EdgeKeeper variables
    public static final String EdgeKeeper_ip = "xyz";
    public static final int EdgeKeeper_port = 0123;

    //guid variables
    public static String dummyGUID = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    public static int GUID_LENGTH = 40;

    //edgekeeper socket variables
    public static final long readIntervalInMilliSec = 3000;



}
