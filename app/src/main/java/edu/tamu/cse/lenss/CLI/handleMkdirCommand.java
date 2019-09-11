package edu.tamu.cse.lenss.CLI;

import edu.tamu.lenss.mdfs.handleCommands.mkdir.mkdir;

public class handleMkdirCommand {

    public static void handleMkdirCommand(String clientID, String mdfsDir){

        //do mkdir and return reply to client
        clientSockets.sendAndClose(clientID, mkdir.mkdir(mdfsDir));
    }
}
