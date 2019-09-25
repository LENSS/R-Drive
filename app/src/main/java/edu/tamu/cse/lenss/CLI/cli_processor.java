package edu.tamu.cse.lenss.CLI;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



//this class runs on a thread as a server, and receives new client connection.
//clients connect this server to send commands and do MDFS jobs
public class cli_processor extends Thread {

    public static final int JAVA_PORT = CLIConstants.CLI_PORT;

    public ExecutorService executor = Executors.newSingleThreadExecutor();
    public ServerSocket serverSocket;
    public boolean isRunning = false;

    public cli_processor(){
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

                try {
                    executor.submit(new RequestHandler(cSocket)).get();
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Exception in handeling the request in cli_processor. " + e);
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


