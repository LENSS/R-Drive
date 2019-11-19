package edu.tamu.lenss.MDFS.Commands.copyfromlocal;

import android.os.Environment;

import org.apache.log4j.Level;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


//copy a file from cli client side to the android phone/linux machine where mdfs is running
public class copyfromlocal {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(copyfromlocal.class);

    public static String copyfromlocal( String[] cmd){

        //log
        logger.log(Level.ALL, "Starting to handle -copyFromLocal command.");

        //this command has already been verified and parsed from the client side
        //get all the variables
        String filename = cmd[2];
        int filesize = Integer.parseInt(cmd[3]);
        String androidDir = cmd[4];
        String file = cmd[5];

        //convert file(string) into file byteArray
        String[] byteValueStr = file.split("/");
        byte[] byteValueInt = new byte[filesize];
        for(int i=0; i< byteValueStr.length; i++){byteValueInt[i] = (byte)Integer.parseInt(byteValueStr[i]); }

        //check if the last char on androidDir is a slash otherwise add it
        if(androidDir.charAt(androidDir.length()-1)!='/'){androidDir = androidDir + "/";}

        //prepare android directory
        if(androidDir.equals("/storage/emulated/0/")){androidDir =  Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator; }
        else{androidDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + androidDir.substring("/storage/emulated/0/".length(), androidDir.length()) + File.separator;}

        //create the android directory
        //dont create if already exists
        File dir = new File(androidDir);
        if(!dir.exists()){
            boolean mkdir = dir.mkdirs();
            if(!mkdir){

                //log
                logger.log(Level.ALL, "-copyFromLocal failed! Could not make directory in android phone.");

                //return
                return "Error! Cannot make directory in android phone.";
            }
        }

        //check if the file already exists, if does, delete it first
        File fi = new File(androidDir + filename);
        if(fi.exists()){ fi.delete(); }

        //writing file on device
        try {
            File fiLe = new File(androidDir + filename);
            fiLe.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(fiLe);
            outputStream.write(byteValueInt);
            outputStream.flush();
            outputStream.close();
        }catch(IOException e ){
            e.printStackTrace();
        }


        //log
        logger.log(Level.ALL, "-copyFromLocal success");

        //QueueToSend back reply
        return "File copy success!";
    }
}
