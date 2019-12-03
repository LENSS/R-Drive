package edu.tamu.cse.lenss.CLI;


import org.apache.commons.math3.analysis.function.Constant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import edu.tamu.lenss.MDFS.Constants;

//this class temporarily contains the sockets of a newly connected CLI client, until the request has been completed.
//when the request has been completed, the client socket is taken out of map, reply is sent and the socket is closed.
//if clientID is NON_CLI_CLIENT, then nothing happens, else the corresponding client socket gets a reply and gets cloed.
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

    public static void close(String clientID){

        if(!clientID.equals(Constants.NON_CLI_CLIENT)) {

            ///get the clientSockets object from map
            clientSockets socket = sockets.get(clientID);

            if (socket != null) {
                try {
                    socket.inBuffer.close();
                    socket.os.close();
                    socket.cSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void send(String clientID, String reply){


        if(!clientID.equals(Constants.NON_CLI_CLIENT)) {

            ///get the clientSockets object from map
            clientSockets socket = sockets.get(clientID);

            //write on the socket aka send reply to client
            if (socket != null) {
                try {
                    socket.os.write((reply + "\n").getBytes());
                    socket.os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    close(clientID);
                }
            }
        }
    }



    public static void sendAndClose(String clientID, String reply){

        if(!clientID.equals(Constants.NON_CLI_CLIENT)) {

            send(clientID, reply);
            close(clientID);

            //remove the entry from the map
            sockets.remove(clientID);
        }
    }

}
