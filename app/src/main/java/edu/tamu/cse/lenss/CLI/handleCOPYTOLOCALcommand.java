package edu.tamu.cse.lenss.CLI;

import android.os.Environment;

import org.sat4j.pb.tools.INegator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static java.lang.Thread.sleep;

public class handleCOPYTOLOCALcommand {

    public static boolean readyToSend = false;

    public static  void handleCOPYTOLOCALcommand(String clientID, String[] cmd){

        //no need to check the command this command is already verified
        String androidDir = cmd[2];
        String filename = cmd[3];

        //check if the androidDir has the slash in it al the end
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

            //get all the files in a byteArray
            try {
                byte[] bArray = new byte[1000000];  // allocate a size of 1 MB
                FileInputStream in = new FileInputStream(androidDir + filename);
                readLen = in.read(bArray);
                in.close();

                //check if we could load the file
                if (readLen <= 0) {
                    reply = "COULDNOTLOADFILE 0 DUMMYFILEDATA";
                    readyToSend = true;

                }else {
                    data = "";
                    for (int i = 0; i < readLen; i++) { data = data + Integer.toString((int) bArray[i]) + "/"; }
                    reply = "SUCCESS" + " " + Integer.toString(readLen) + " "+ data;
                    readyToSend = true;
                }
            }catch(IOException e){e.printStackTrace();}
        }else{
            reply = "DIRORFILENOTEXIST 0 DUMMYFILEDATA";
            readyToSend = true;
        }

        if(readyToSend) {

            //separate the reply string into different 1000 pieces
            //the first piece contains numOfPieces + "_" + 1000 bytes
            if(reply.length()>1000) {

                //create replyToken array length
                int numOfReplyTokens = (int)Math.ceil(reply.length()/1000);
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
                    start = end; end = end + 1000;
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

                    clientSockets.send(clientID, replyTokens[i]);

                    System.out.println("cliii rep token sent: " + count +  " " + replyTokens[i]); count++;
                    Sleep(100);
                }

                clientSockets.close(clientID);

            }else{
                //make the reply
                reply = "1" + "^"  +  reply;

                //send the reply
                clientSockets.sendAndClose(clientID, reply);
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