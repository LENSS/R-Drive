package edu.tamu.cse.lenss.android;

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

    public static final int JAVA_PORT = 22222;

    // Limit how many request can be handeled concurrently
    ExecutorService executor = Executors.newFixedThreadPool(50);
    ServerSocket serverSocket;

    /**
     * The purpose of the server is to accept new connection and let
     * the Request Handler del with the service request in another thread.
     */
    public void run() {
        try {
            serverSocket = new ServerSocket(JAVA_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //isRunning = true;
        while (!serverSocket.isClosed()) {
            //Accept the socket
            Socket cSocket;
            try {
                cSocket = serverSocket.accept();
                System.out.println("New connection at " );
                executor.execute(new RequestHandler(cSocket));

            } catch (IOException e) {
                System.out.println("Error in accepting client connection"+ e);
            }
        }
    }

    /*    public void closeServerSocket() throws IOException {
            serverSocket.close();
        }
    */
    /**
     * This the hook function executed when the request server is shutting down.
     * Basically esecuted during the service termination.
     */
    public void interrupt() {
        //isRunning = false;
        super.interrupt();
        try {
            serverSocket.close();
            System.out.println("Closed the Serversocket for " );
        } catch (IOException e) {
            System.out.println("Problem in closing the server socket for "+ e);
        }

    }

}


class RequestHandler implements Runnable{

    private Socket cSocket;
    public static AtomicInteger reqNumber=new AtomicInteger(0);

    int incomingReqNumber;

    public RequestHandler( Socket cSocket) {
        super();
        this.cSocket = cSocket;
    }

    public void run() {

            try{
                //Read the request from the CPP daemon
                BufferedReader inBuffer = new BufferedReader(new InputStreamReader( cSocket.getInputStream() ));
                OutputStream os = cSocket.getOutputStream();
                String inMessage = inBuffer.readLine();
                System.out.println("Incoming message: "+inMessage);

                //Process the message and reply to the Daemon
                String rep = processRequestCpp(inMessage);
                System.out.println("Reply message = "+rep);
                os.write((rep+"\n\n").getBytes());
                os.flush();

                //Now close the socket.
                inBuffer.close();
                os.close();
                cSocket.close();
            }catch (IOException e) {
                System.out.println("Problem handelling client socket in RequestHandler"+ e);

        }

        System.out.println("RequestHandler now completed request number: "+incomingReqNumber);
    }

    private String processRequestCpp(String inMessage) {
        return "";
    }


}