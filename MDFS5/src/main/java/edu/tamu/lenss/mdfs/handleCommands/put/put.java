package edu.tamu.lenss.mdfs.handleCommands.put;

import android.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.handleCommands.put.MDFSFileCreatorViaRsockNG;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class put {

    public static String put(String filePathLocal, String filePathMDFS, String filename, String clientID) {

        Pair pair =  loadFile(filePathLocal, filename, clientID);

        //if file has been loaded, then send
        if(pair!=null) {
            if (pair.getString().equals("SUCCESS") && pair.getFile() != null) {
                return sendFile(filename, pair.getFile(), filePathMDFS);
            } else {
                return pair.getString();
            }
        }else{
            return "-put failed! Could not load file.";
        }
    }


    private static Pair loadFile(String filePathLocal,String filename, String clientID){

        //return pair
        Pair pair = new Pair();

        //get all the files in the folder
        File[] listofFiles = new File(filePathLocal).listFiles();
        int index = -1;
        boolean fileExists = false;
        for(int i=0; i< listofFiles.length; i++){
            if(listofFiles[i].getName().equals(filename)){
                index = i;
                fileExists = true;
                break;
            }
        }

        //check if file exists
        if(fileExists){

            //get the file
            File file = listofFiles[index];

            //check file size
            if(file.length()>Integer.MAX_VALUE){
                pair.setString("-put failed! File size too large " + "(Max: " + Integer.MAX_VALUE + " bytes)." + "\n" + "Note: Actual available memory on device may be much lower than above number.");
                pair.setFile(null);
                return pair;
            }else if((file.length()/Constants.MAX_BLOCK_SIZE)>127){ //max java byte value
                pair.setString("-put failed! Block count exceeded. Choosing a larger block size might solve this problem.");
                pair.setFile(null);
            }else{
                pair.setString("SUCCESS");
                pair.setFile(file);
            }
        }else{
            pair.setString("-put failed! File not found.");
            pair.setFile(null);
        }

        return pair;
    }

    //takes a file, and a MDFS path and sends it
    private static String sendFile(String filename, File file, String filePathMDFS){

        ///check block count is not exceeded
        int maxBlockSize = Constants.MAX_BLOCK_SIZE;
        if(maxBlockSize > (Integer.MAX_VALUE-Integer.BYTES - 1)){
            maxBlockSize = Integer.MAX_VALUE - Integer.BYTES - 1;
        }

        //send the file in the same thread
        MDFSFileCreatorViaRsockNG curFile = new MDFSFileCreatorViaRsockNG(file, filePathMDFS, maxBlockSize, Constants.K_N_RATIO, ServiceHelper.getInstance().getEncryptKey());
        String ret = curFile.start();

        //check return
        if(ret.equals("SUCCESS")){
            //return success message
            return "File Creation Success!";
        }else{
            //return error message
            return "-put failed! " + ret;
        }
    }


    //simple tuple class in java
    static class Pair{
        String string;
        File file;

        public Pair(){ }

        public String getString(){
            return string;
        }

        public File getFile(){
            return file;
        }

        public void setString(String str){ this.string = str;}

        public void setFile(File f){this.file = f;}

    }


}
