package edu.tamu.cse.lenss.CLI;

import edu.tamu.lenss.mdfs.handleCommands.ls.ls;

public class handleLScommand {

    public static void handleLScommand(String clientID, String mdfsDir){

        //do ls and send back reply to client
        clientSockets.sendAndClose(clientID, ls.ls(mdfsDir));
    }



}
