package edu.tamu.cse.lenss.CLI;

public class handleRetrieveCommand {


    public static void handleRetrieveCommand(String filename, String clientID){

        if(filename.equals("ALL")){
            //TODO:
            //fetch metadata_batch for all files for this node
            //send filename, metadata to each new MDFSFileRetrieverViaRsock in a thread
            //reply return
        }else{
            //TODO:
            //fetch file metadata from edgekeeper
            //if fails then return FAILED
            //if succeed
            //send filename, metadata to new MDFSFileRetrieverViaRsock in a thread
            //reply return
        }
    }
}
