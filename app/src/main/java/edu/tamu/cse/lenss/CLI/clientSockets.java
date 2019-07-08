package edu.tamu.cse.lenss.CLI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

//this class temporarily contains the sockets of a newly connected client, until the request has been completed.
//when the request has been completed, the client socket is taken out of map, reply is sent and the socket is closed.

public class clientSockets {
    public static Map<String, clientSockets> sockets = new HashMap();

    BufferedReader inBuffer;
    OutputStream os;
    Socket cSocket;


    public clientSockets(BufferedReader inBuffer, OutputStream os, Socket cSocket){
        this.inBuffer = inBuffer;
        this.os = os;
        this.cSocket = cSocket;
    }

    private static void close(String clientID){
        ///get the clientSockets object from map
        clientSockets socket = sockets.get(clientID);
        try {
            socket.inBuffer.close();
            socket.os.close();
            socket.cSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void send(String clientID, String reply){
        ///get the clientSockets object from map
        clientSockets socket = sockets.get(clientID);

        //write on the socket aka send reply to client
        try {
            socket.os.write((reply+"\n\n").getBytes());
            socket.os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void sendAndClose(String clientID, String reply){
        send(clientID, reply);
        close(clientID);
        //remeove the entry from the map
        sockets.remove(clientID);
    }
}
