package edu.tamu.lenss.mdfs.Commands.put;

import java.io.File;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.Handler.ServiceHelper;

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

            //check if filesize is 0 bytes
            if(file.length()<=0){
                pair.setString("-put failed! File size 0 bytes.");
                pair.setFile(null);
                return pair;
            }


            //check filesize is within allowed max size
            if(file.length()>= Long.MAX_VALUE || file.length()>= Constants.MAX_FILE_SIZE){
                pair.setString("-put failed! File too large! Max allowed size " + Constants.MAX_FILE_SIZE + " bytes and filesize " + file.length() + " bytes.");
                pair.setFile(null);
                return pair;
            }

            /*//check if vm currently has filesize amount of size left
            if(Runtime.getRuntime().freeMemory()<file.length()){
                pair.setString("-put failed! Currently system doesnt have enough memory to load the file. current free memory " + Runtime.getRuntime().freeMemory() + " bytes.");
                pair.setFile(null);
                return pair;
            }*/


            //check block count
            if((file.length()/Constants.MAX_BLOCK_SIZE)> Constants.MAX_BLOCK_COUNT){
                pair.setString("-put failed! Block count exceeded. Choosing a larger block size might solve this problem.");
                pair.setFile(null);
                return pair;
            }else{
                pair.setString("SUCCESS");
                pair.setFile(file);
            }
        }else{
            pair.setString("-put failed! File not found.");
            pair.setFile(null);
            return pair;
        }

        return pair;
    }

    //takes a file, and a MDFS path and sends it
    private static String sendFile(String filename, File file, String filePathMDFS){


        //check block size is not exceeded beyond integer value.
        //in that case, switch to default block count.
        //we limit the number of blocks to be a byte value(max: 127).
        int maxBlockSize = Constants.MAX_BLOCK_SIZE;
        if(maxBlockSize >= Integer.MAX_VALUE){
            maxBlockSize = (int) Constants.DEFAULT_BLOCK_SIZE;
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
