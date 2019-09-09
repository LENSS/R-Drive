package edu.tamu.lenss.mdfs.EdgeKeeperNG;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
//import edgekeeper client jar


public class TalkToEdgeKeeper {

    //constructor
    public TalkToEdgeKeeper(){

        //exmaple of xmlrpc
        org.apache.xmlrpc.client.XmlRpcClient client = new org.apache.xmlrpc.client.XmlRpcClient();
        client.execute("", new Vector());

        org.apache.xmlrpc.webserver.WebServer server = new org.apache.xmlrpc.webserver.WebServer(80);

        try { server.start(); } catch (IOException e) { e.printStackTrace(); }

    }

    public boolean sendMetadata(FileMetadata metadata, String parentPath, String childPath){

        boolean result = false;

        //make the request packet
        //send it
        //receive true/false

        return result;
    }

    public FileMetadata retrieveMetadata(String path, String filename){

        //make the request packet
        //sent it
        //receive Metadata object
        //do job

        return new FileMetadata();
    }

    List<FileMetadata> deleteDirectory(String parentPath){

        //make the request packet
        //send it
        //receive a list of file metadata
        //send delete messages to each metadata holder via rsock
        return new ArrayList<>();
    }

    FileMetadata deleteMetadata(String path, String filename){


        //make the request packet
        //send it
        //receive a file metadata
        //send delete messages to each metadata holder via rsock
        return new FileMetadata();
    }

    List<FileMetadata> listMetadata(String path, String filename){

        //make request packet
        //send it
        //receive a list of file metadata string
        return new ArrayList<>();
    }




}
