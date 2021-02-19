package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static java.lang.Thread.sleep;

//copy a file from android phone/linux machine, where mdfs is running, to the cli client side linux machine
public class copytolocal {

    public static boolean readyToSend = false;

    public static void copytolocal(String clientID, String[] cmd){


        //sout
        System.out.println("Staring to handle -copyToLocal command.");

        //no need to check the command this command is already verified
        String androidDir = cmd[2];
        String filename = cmd[3];

        //check if the androidDir has the slash in it al the end, if not add it.
        if(androidDir.charAt(androidDir.length()-1)!='/'){androidDir = androidDir + "/";}

        //prepare android directory
        if(androidDir.equals("/storage/emulated/0/")){androidDir =  Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator; }
        else{androidDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + androidDir.substring("/storage/emulated/0/".length(), androidDir.length()) + File.separator;}

        //reply string variables
        String reply ="";
        int readLen = 0;
        String data = "";

        //check if the file exists
        File file = new File(androidDir + filename);

        if(file.exists()){

            if(file.length()>550000){
                reply = "FILETOOLARGE 0 DUMMYFILEDATA";
                readyToSend = true;
            }else {


                try {

                    //get all the files in a byteArray
                    byte[] bArray = new byte[(int) file.length()];  // allocate a size of 2 MB
                    FileInputStream in = new FileInputStream(androidDir + filename);
                    readLen = in.read(bArray);
                    in.close();

                    //check if we could load the file
                    if (readLen <= 0) {
                        reply = "COULDNOTLOADFILE 0 DUMMYFILEDATA";
                        readyToSend = true;

                    } else {
                        //file is loaded, now pad the file and make a string of bytes.
                        //note: heavily inefficient way but no other convenient way found.
                        //(ex: sending a 2 mb file requires almost 8 mb data transfer)
                        data = "";
                        for (int i = 0; i < readLen; i++) {
                            data = data + Integer.toString((int) bArray[i]) + "/";
                        }
                        reply = "SUCCESS" + " " + Integer.toString(readLen) + " " + data;

                        //check if the final reply variable size is less than 1MB
                        //note: on the reciever side, a 5 mb size limit si sent in send_receive_ng() function.
                        if (reply.length() > 1000000) {
                            reply = "FILETOOLARGE 0 DUMMYFILEDATA";
                            readyToSend = true;
                        } else {
                            //dont change the reply variable, just enable readyToSend
                            readyToSend = true;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else{
            reply = "DIRORFILENOTEXIST 0 DUMMYFILEDATA";
            readyToSend = true;
        }

        //now send it part by part
        //this is done due to mac layer mtu being 1500 for wifi
        if(readyToSend) {
            //separate the reply string into different 1000 pieces
            //the first piece contains numOfPieces + "_" + 1000 bytes
            if(reply.length()>1000) {

                //create replyToken array length
                int numOfReplyTokens = (int) Math.ceil(reply.length()/1000);
                if((reply.length() - (1000*numOfReplyTokens))>0){ numOfReplyTokens = numOfReplyTokens + 1;}

                //create reply tokens array
                String[] replyTokens = new String[numOfReplyTokens];

                System.out.println("cliii " + reply.length() + " " +numOfReplyTokens);

                //divide the reply string into replyTokens
                String replyToken = "";
                int replyTokenIndex = 0;
                int start = 0;
                int end = 1000;
                int total = 0;


                while(true) {
                    //check when to break
                    if(total>=reply.length()){break;}

                    //make one replyToken
                    for (int i = start; i < end; i++) { replyToken = replyToken + reply.charAt(i); total++;}

                    //increment indexes for next iteration
                    start = end;
                    end = end + 1000;
                    if(end>=reply.length()){end = reply.length();}

                    //put the replyToken into array
                    replyTokens[replyTokenIndex] = replyToken;
                    replyTokenIndex++;

                    //clean the replyToken string for next iteration
                    replyToken = "";

                }

                //add the numbers of replyTokens at the beginning of the first token
                replyTokens[0] = Integer.toString(numOfReplyTokens) + "^" + replyTokens[0];

                //send the reply tokens one by one
                int count = 0;
                for(int i=0; i< replyTokens.length; i++){

                    //print first reply
                    System.out.println(replyTokens[i]);


                    //send and sleep
                    clientSockets.send(clientID, replyTokens[i]);
                    Sleep(500);
                }

                //when done, close the socket
                //clientSockets.close(clientID);

            }else{

                //make the only reply
                reply = "1" + "^"  +  reply;

                //send the reply
                clientSockets.send(clientID, reply);
            }

        }

    }

    public static void Sleep(int millisec){
        try {
            sleep(millisec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
