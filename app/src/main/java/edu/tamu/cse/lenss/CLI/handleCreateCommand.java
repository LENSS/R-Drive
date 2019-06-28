package edu.tamu.cse.lenss.CLI;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsock;
import edu.tamu.lenss.mdfs.MDFSFileCreatorViaRsock;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class handleCreateCommand {

    public static void handleCreateCommand(String path, String filename, String[] perm, String clientID) {
        //TODO: create file

        loadFile(path, filename, perm, clientID);
        //choose the nodes only which passes perm + active to receive
        //add group feature in edgekeeper
        //send fragments + send own metadata to edgekeeper
        //only fragment receivers will update edgekeeper about their newly received fragments.
        //in updatedirectory(), update own directory but dont send update
        //in rsockreceiveforfilecreation, update own directory only when you receive a fragment.(already done)

    }

    private static void loadFile(String path, String filename, String[] perm, String clientID){

        File[] listofFiles = new File(path).listFiles();
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
            compressAndsendFile(file, null, false, perm, clientID);

        }else{
            clientSockets.sendAndClose(clientID, "file not found.");
            return;
        }
    }

    private static void compressAndsendFile(final File orgiginalFile, final String newFileName, final boolean compressed, String[] perm, String clientID){
        final File renamedFile;
        if(newFileName != null){
            renamedFile = new File(orgiginalFile.getParent()+ File.separator + newFileName);
            orgiginalFile.renameTo(renamedFile);
        }
        else{
            renamedFile = orgiginalFile;
        }

        if(compressed){
            try {
                File tmpF = AndroidIOUtils.getExternalFile(Constants.DIR_CACHE);
                final File compressedFile = IOUtilities.createNewFile(tmpF, newFileName);
                FileOutputStream out = new FileOutputStream(compressedFile);
                AndroidIOUtils.decodeSampledBitmapFromFile(renamedFile.getAbsolutePath(), Constants.COMMON_DEVICE_WIDTH, Constants.COMMON_DEVICE_HEIGHT).compress(Bitmap.CompressFormat.JPEG, 85, out);
                renamedFile.delete();
                sendFile(compressedFile, perm, clientID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            sendFile(renamedFile, perm, clientID);
        }

    }

    private static void sendFile(final File file, String[] perm, String clientID){
        //check whether file creation via rsock or tcp
        if(Constants.file_creation_via_rsock_or_tcp.equals("rsock")){  //RSOCK
            MDFSFileCreatorViaRsock creator = new MDFSFileCreatorViaRsock(file, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO, perm, clientID);
            creator.setEncryptKey(ServiceHelper.getInstance().getEncryptKey());
            creator.setListener(fileCreatorListenerviarsock);
            creator.start();
        }
    }

    //rsock listener
    private static MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock fileCreatorListenerviarsock = new MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock(){
        @Override
        public void statusUpdate(String status) {

        }

        @Override
        public void onError(String error, String clientID) {
            clientSockets.sendAndClose(clientID, "CLIII Error! File Creation Failed. Reason: " + error);
            return;
        }

        @Override
        public void onComplete(String msg, String clientID) {
            clientSockets.sendAndClose(clientID, "CLIII Success! File Create Success.");
            return;
        }
    };
}
