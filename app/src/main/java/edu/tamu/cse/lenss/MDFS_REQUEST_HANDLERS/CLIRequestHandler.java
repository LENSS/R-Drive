package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import static edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS.ProcessOneRequest.processRequestCpp;


//this class only takes requests from Command Line Interface.
public class CLIRequestHandler implements Runnable {

    //class variables
    private Socket cSocket;

    public CLIRequestHandler(Socket cSocket) {
        this.cSocket = cSocket;
    }

    @Override
    public void run() {

        try{
            //Read the request from CLI socket
            System.out.println("RequestHandler reading command from socket..");
            BufferedReader inBuffer = new BufferedReader(new InputStreamReader( cSocket.getInputStream()));
            OutputStream os = cSocket.getOutputStream();
            String command = inBuffer.readLine();  //note: the reading finishes until one or more \n is read
            System.out.println("RequestHandler command: "  + command);

            //make clientSockets object
            clientSockets socket = new clientSockets(inBuffer, os, cSocket);

            //generate random clientID
            String clientID = UUID.randomUUID().toString().substring(0,12);

            //put the clientSockets in the static map <clientID, clientSockets>
            clientSockets.sockets.put(clientID, socket);

            //process req
            processRequestCpp(clientID, command);

        }catch (IOException e) {
            System.out.println("Problem handling client socket in RequestHandler "+ e);

        }

    }
}




