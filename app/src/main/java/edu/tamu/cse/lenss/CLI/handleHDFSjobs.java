package edu.tamu.cse.lenss.CLI;

import edu.tamu.lenss.mdfs.hadoop.hdfsClient;

public class handleHDFSjobs {

    public static void handleHDFSjobs(String clientID){

        String result = null;
        //hdfsClient.ONE();
        hdfsClient.TWO();

        clientSockets.sendAndClose(clientID, "done!");
    }
}
