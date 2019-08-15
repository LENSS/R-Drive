package edu.tamu.cse.lenss.CLI;

import java.io.File;
import java.io.FileOutputStream;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsock;
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsockNG;
import edu.tamu.lenss.mdfs.MDFSFileCreatorViaRsock;
import edu.tamu.lenss.mdfs.MDFSFileCreatorViaRsockNG;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class handlePutCommand {

    public handlePutCommand(){}

    public void handleCreateCommand(String filePathLocal, String filePathMDFS, String filename, String[] perm, String clientID) {
        loadFile(filePathLocal, filePathMDFS, filename, perm, clientID);
    }

    private void loadFile(String filePathLocal, String filePathMDFS, String filename, String[] perm, String clientID){

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
            compressAndsendFile(file, filePathMDFS,null, false, perm, clientID);

        }else{
            clientSockets.sendAndClose(clientID, "-put Failed! File not found.");
            return;
        }
    }

    private void compressAndsendFile(File orgiginalFile, String filePathMDFS, String newFileName, boolean compressed, String[] perm, String clientID){
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
                sendFile(compressedFile, filePathMDFS, perm, clientID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            sendFile(renamedFile, filePathMDFS, perm, clientID);
        }

    }

    private void sendFile(final File file, String filePathMDFS, String[] perm, String clientID){

        /*//old school
        MDFSFileCreatorViaRsock creator = new MDFSFileCreatorViaRsock(file, filePathMDFS, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO, perm, clientID);
        creator.setEncryptKey(ServiceHelper.getInstance().getEncryptKey());
        creator.setListener(fileCreatorListenerviarsock);
        creator.start();*/

        //new school
        new FileCreatorThread(new MDFSFileCreatorViaRsockNG(file, filePathMDFS, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO, perm, ServiceHelper.getInstance().getEncryptKey() ,clientID), clientID).start();

    }

    //rsock listener
    private static MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock fileCreatorListenerviarsock = new MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock(){
        @Override
        public void statusUpdate(String status) {

        }

        @Override
        public void onError(String error, String clientID) {
            clientSockets.sendAndClose(clientID, "Error! File Creation Failed. " + error);
            return;
        }

        @Override
        public void onComplete(String msg, String clientID) {
            clientSockets.sendAndClose(clientID, "Success! " + msg);
            return;
        }
    };


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
