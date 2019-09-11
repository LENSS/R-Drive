package edu.tamu.cse.lenss.CLI;

import java.io.File;
import java.io.FileOutputStream;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.handleCommands.put.MDFSFileCreatorViaRsockNG;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class handlePutCommand {

    public handlePutCommand(){}

    public void handleCreateCommand(String filePathLocal, String filePathMDFS, String filename, String clientID) {
        loadFile(filePathLocal, filePathMDFS, filename, clientID);
    }


    private void loadFile(String filePathLocal, String filePathMDFS, String filename, String clientID){

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

        if(fileExists){
            File file = listofFiles[index];
            if(file.length()>Integer.MAX_VALUE){
                clientSockets.sendAndClose(clientID, "-put Failed! File size too large " + "(Max: " + Integer.MAX_VALUE + " bytes)." + "\n" + "Note: Actual available memory on device may be much lower than above number.");
            }else if((file.length()/Constants.MAX_BLOCK_SIZE)>127){ //max java byte value
                clientSockets.sendAndClose(clientID, "-put Failed! Block count exceeded. Choosing a larger block size might solve this problem.");
            }else{
                compressAndsendFile(file, filePathMDFS, null, false, clientID);
            }
        }else{
            clientSockets.sendAndClose(clientID, "-put Failed! File not found.");
            return;
        }
    }

    private void compressAndsendFile(File orgiginalFile, String filePathMDFS, String newFileName, boolean compressed, String clientID){
        File renamedFile;
        if(newFileName != null){
            renamedFile = new File(orgiginalFile.getParent()+ File.separator + newFileName);
            orgiginalFile.renameTo(renamedFile);
        }
        else{
            renamedFile = orgiginalFile;
        }

        if(compressed){
            try {
                File tmpF = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_CACHE);
                final File compressedFile = IOUtilities.createNewFile(tmpF, newFileName);
                FileOutputStream out = new FileOutputStream(compressedFile);
                renamedFile.delete();
                sendFile(compressedFile, filePathMDFS, clientID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            sendFile(renamedFile, filePathMDFS, clientID);
        }

    }

    private void sendFile(final File file, String filePathMDFS, String clientID){

        //new school
        int maxBlockSize = Constants.MAX_BLOCK_SIZE;
        if(maxBlockSize > (Integer.MAX_VALUE-Integer.BYTES - 1)){ maxBlockSize = Integer.MAX_VALUE - Integer.BYTES - 1;}
        new FileCreatorThread(new MDFSFileCreatorViaRsockNG(file, filePathMDFS, maxBlockSize, Constants.K_N_RATIO, ServiceHelper.getInstance().getEncryptKey() ,clientID), clientID).start();

    }


    public class FileCreatorThread extends Thread{

        MDFSFileCreatorViaRsockNG curFile;
        String clientID;

        public FileCreatorThread(MDFSFileCreatorViaRsockNG curFile, String clientID){
            this.curFile = curFile;
            this.clientID = clientID;
        }

        public void run(){
            String ret = curFile.start();
            if(ret.equals("SUCCESS")){
                clientSockets.sendAndClose(clientID, "File Creation Success.");
            }else{
                clientSockets.sendAndClose(clientID, ret);

            }
        }



    }
}
