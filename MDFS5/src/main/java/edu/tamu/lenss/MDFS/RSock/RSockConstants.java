package edu.tamu.lenss.MDFS.RSock;

import RsockCommLibrary.Interface;


//this class contains constants for Rsock initialization.
public class RSockConstants {


    //enable/disable rsock from the whole project
    public static boolean RSOCK = true;

    //testrsock
    public static boolean testRsock = false;

    //rsock api instances.
    public static Interface intrfc_creation;
    public static Interface intrfc_retrieval;
    public static Interface intrfc_deletion;
    public static Interface intrfc_test;

    //appID using which above Rsock api objects are registered to the daemon.
    public static final String intrfc_creation_appid = "mdfsFileCreation";
    public static final String intrfc_retrieval_appid = "mdfsFileRetrieval";
    public static final String intrfc_deletion_appid = "mdfsFileDeletion";
    public static final String intrfc_test_appid = "mdfsRsockTest";

    //deletion tag
    public static final String deletion_tag = "_delete_";

    //error message
    public static String Rsock_Library_Closed_Message = "RSOCK library closed, please restart Rsock -> MDFS.";


}
