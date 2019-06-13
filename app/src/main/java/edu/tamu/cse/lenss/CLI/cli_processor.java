package edu.tamu.cse.lenss.CLI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class cli_processor extends Thread {

    public static final int JAVA_PORT = 2223;

    public ExecutorService executor = Executors.newFixedThreadPool(5);
    public ServerSocket serverSocket;
    public boolean isRunning = false;

    //The purpose of the server is to accept new connection and let
    //the Request Handler deal with the service request in another thread.
    public void run() {
        try {
            serverSocket = new ServerSocket(JAVA_PORT);
            System.out.println("CLIII server running" );
        } catch (IOException e) {
            System.out.println("CLIII server init failed" );
            e.printStackTrace();
        }

        isRunning = true;
        while (isRunning) {
            //Accept the socket
            Socket cSocket;
            try {
                cSocket = serverSocket.accept();
                System.out.println("CLIII received new connection..." );
                executor.execute(new RequestHandler(cSocket));

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


