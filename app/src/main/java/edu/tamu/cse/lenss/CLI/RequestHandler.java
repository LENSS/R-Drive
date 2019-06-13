package edu.tamu.cse.lenss.CLI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

class RequestHandler implements Runnable{

    private Socket cSocket;

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
            System.out.println("CLIII Incoming message: "+inMessage);

            //Process the message and reply to the Daemon
            String rep = processRequestCpp(inMessage);
            System.out.println("CLIII Reply message = "+rep);
            os.write((rep+"\n\n").getBytes());
            os.flush();

            //Now close the socket.
            inBuffer.close();
            os.close();
            cSocket.close();

        }catch (IOException e) {
            System.out.println("CLIII Problem handelling client socket in RequestHandler"+ e);

        }

    }

    private String processRequestCpp(String inMessage){
        String reply = "reply from cli";
        return reply;
    }


}