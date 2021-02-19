package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


//this class runs on a thread as a server, and receives new client connection.
//clients connect this server to send commands and do MDFS jobs
public class CLIserver extends Thread {


    public static final int JAVA_PORT = CLIConstants.CLI_JAVA_PORT;


    public ServerSocket serverSocket;
    public boolean isRunning = false;

    public CLIserver(){
    }

    //The purpose of the server is to accept new connection and let
    //the Request Handler deal with the service request in another thread.
    public void run() {
        try {
            serverSocket = new ServerSocket(JAVA_PORT);
            System.out.println("CLIII server is running" );
        } catch (IOException e) {
            System.out.println("CLIII server init failed" );
            e.printStackTrace();
        }

        isRunning = true;
        while (isRunning) {

            //Accept the socket
            Socket cSocket;
            try {
                //put the request in a new thread
                cSocket = serverSocket.accept();
                System.out.println("CLI received new connection..." );

                //execute the request.
                //note: since it is from CLI,
                // we should not make user waiting,
                // so we should bypass putting this request in any qeueu,
                // and directly execute it by some thread.
                try {
                    new Thread(new CLIRequestHandler(cSocket)).run();

                } catch (Exception e) {
                    System.out.println("Exception in handling the request in cli_processor. " + e);
                }
            } catch (IOException e) {
                System.out.println("CLIII Error in accepting client connection"+ e);
            }
        }
    }


    public void interrupt() {
        isRunning = false;
        super.interrupt();
        try {
            serverSocket.close();
            System.out.println("CLIII Closed the Serversocket" );
        } catch (IOException e) {
            System.out.println("CLIII Problem in closing the server socket for "+ e);
        }

    }


}


