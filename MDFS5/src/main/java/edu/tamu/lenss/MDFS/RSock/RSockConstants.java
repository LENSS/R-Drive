package edu.tamu.lenss.MDFS.RSock;

import example.Interface;


//this class contains constants for Rsock initialization.
public class RSockConstants {

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

    //rsock endpoints
    //Note: values must have to be seven bytes
    public static final String fileCreateEndpoint = "RsockFC";
    public static final String fileRetrieveEndpoint = "RsockFR";
    public static final String fileDeleteEndpoint = "RsockDL";
    public static final String rsockTestEndpoint = "default";
}
