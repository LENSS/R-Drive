package edu.tamu.lenss.mdfs.handleCommands.copyfromlocal;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


//copy a file from cli client side to the android phone/linux machine where mdfs is running
public class copyfromlocal {

    public static String copyfromlocal( String[] cmd){

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
            if(!mkdir){ return "Error! Cannot make directory in android phone."; }
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

        //send back reply
        return "File copy success!";
    }
}
