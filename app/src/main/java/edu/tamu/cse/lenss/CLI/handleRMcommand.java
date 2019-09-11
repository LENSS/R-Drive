package edu.tamu.cse.lenss.CLI;


import edu.tamu.lenss.mdfs.handleCommands.rm.rm;

public class handleRMcommand {

    public static void  handleRMcommand(String clientID, String dir, String reqType){
        clientSockets.sendAndClose(clientID, rm.rm(dir,reqType));
    }
}
