package edu.tamu.cse.lenss.CLI;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.tamu.lenss.mdfs.Constants;

import static java.lang.Thread.sleep;

class RequestHandler implements Runnable{

    //class variables
    private Socket cSocket;
    Context appContext;

    //filepath
    public String fileCreatePath = "/distressnet/MStorm/StreamFDPic/temp/";

    //command parsing variables
    String[] comm = {"directory", "set_perm", "get_perm", "create", "retrieve", "delete", "help"};
    Set<String> commandNames = new HashSet<>(Arrays.asList(comm));
    String[] subcomm = {"permission"};
    Set<String> subCommandNames = new HashSet<>(Arrays.asList(subcomm));
    String[] action = {"all", "decrypted", "undecrypted"};
    Set<String> actionNames = new HashSet<>(Arrays.asList(action));

    public RequestHandler( Socket cSocket, Context con) {
        this.cSocket = cSocket;
        this.appContext = con;
    }

    public void run() {

        try{
            //Read the request from the CPP daemon
            BufferedReader inBuffer = new BufferedReader(new InputStreamReader( cSocket.getInputStream() ));
            OutputStream os = cSocket.getOutputStream();
            String command = inBuffer.readLine();

            //print request
            //System.out.println("CLIII command receiving: " + command);


            //process req
            String reply = processRequestCpp(command);
            //String reply = "test reply";

            //print reply
            System.out.println("CLIII reply sending: " + reply);

            //send the message and reply to the Daemon
            os.write((reply+"\n\n").getBytes());
            os.flush();

            //Now close the socket.
            inBuffer.close();
            os.close();
            cSocket.close();

        }catch (IOException e) {
            System.out.println("CLIII Problem handling client socket in RequestHandler"+ e);

        }

    }

    //this function works as a parser and syntactical analyzer
    private String processRequestCpp(String command) {

        //check if its an empty string
        if(command.equals("")){
            return "CLIII Error! No command found...Type \"mdfs help\" for more information.";
        }

        //replace tab with space
        command = command.replaceAll("\t", " ");

        //split the command into space separated tokens
        String[] cmd = command.split(" ");

        //check if the first token is "mdfs"
        if(cmd.length>0 && cmd[0].toLowerCase().equals("mdfs")){
            //check what type of command it is
            if(cmd.length>1 && commandNames.contains(cmd[1].toLowerCase())){
                //command exists, take action based on command type
                 if(cmd[1].toLowerCase().equals("help")){
                     return handleHelpCommand();
                 }else if(cmd[1].toLowerCase().equals("create")){
                    //check if next entry is a filename
                    if(cmd.length>2){
                        String filename = cmd[2];
                        //check if this file exists
                        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/distressnet/MStorm/StreamFDPic/temp/";
                        File[] listoffiles = new File(path).listFiles();
                        boolean fileExists = false;
                        for(int i=0; i< listoffiles.length; i++){ if(listoffiles[i].getName().equals(filename)){fileExists = true; break;} }
                        if(fileExists){
                            //check if the file extension is jpg or mp4
                            if(filename.contains(".jpg")  || filename.contains(".mp4")){
                                //check if command came with any permission
                                if(cmd.length>3 && subCommandNames.contains(cmd[3].toLowerCase()) && cmd[3].toLowerCase().equals("permission")){
                                    //"permission" subcommand exists get the permitted entities
                                    String[] perm = Arrays.asList(cmd).subList(4, cmd.length-1 + 1).toArray(new String[0]);
                                    //check if the permitted entities are either "owner", "group", "world" or a 40 bytes GUID
                                    perm = checkPermittedNodes(perm);
                                    if(perm.length!=0){
                                        //call handleCreateCommand function
                                        boolean reply = handleCreateCommand(path, filename, perm);
                                        if(reply==true){
                                            return "CLIII Success! File Create Success.";
                                        }else{
                                            return "CLIII Error! File create Failed. Reason: " + reply;
                                        }

                                    }else{
                                        return "CLIII Error! Permission list invalid or not found.";
                                    }

                                }else{
                                    return "CLIII Error! Command not complete, add Permission subcommand.";
                                }

                            }else{
                                return "CLIII Error! File must be either .jpg or .mp4 file.";
                            }
                        }else{
                            return "CLIII Error! File does not exist.";
                        }
                    }else{
                        return "CLIII Error! Command not complete, mention a filename.";
                    }
                 }else if(cmd[1].toLowerCase().equals("retrieve")){
                    if(cmd.length>2){
                        String filename = cmd[2];
                        //check if the file extension is jpg or mp4
                        if(filename.contains(".jpg")  || filename.contains(".mp4")){
                            int numOfFiles = handleRetrieveCommand(filename);
                            if(numOfFiles>0){
                                return "CLIII Info! " + numOfFiles + " files has been retrieved.";
                            }else{
                                return "CLIII Error! no file has been retrieved.";
                            }

                        }else if(filename.toLowerCase().equals("all")){
                            int numOfFiles = handleRetrieveCommand(filename);
                            if(numOfFiles>0){
                                return "CLIII Info! " + numOfFiles + " files has been retrieved.";
                            }else{
                                return "CLIII Error! no file has been retrieved.";
                            }
                        }else{
                            return "CLIII Error! File must be either .jpg or .mp4 file.";
                        }
                    }else{
                        return "CLIII Error! File name not mentioned.";
                    }
                 }else if(cmd[1].toLowerCase().equals("directory")){
                     if(cmd.length>2){
                        if(actionNames.contains(cmd[2].toLowerCase())){
                            return handleDirectoryCommand(cmd[2]);
                        }else{
                            return "CLIII Error! Command modifier not valid, mention a valid command modifier- decrypted, undecrypted, or all.";
                        }
                     }else{
                         return "CLIII Error! Command not complete, mention a command modifier- decrypted, undecrypted, or all.";
                     }
                 }else if(cmd[1].toLowerCase().equals("delete")){
                     if(cmd.length>2){
                         int[] deleted = null;
                         if((cmd[2].toLowerCase().equals("all"))){
                            deleted = handleDeleteCommand(cmd[2]);
                         }else if(cmd[2].contains(".jpg") || cmd[2].contains(".mp4")){
                            deleted = handleDeleteCommand(cmd[2]);
                         }else{
                             return "CLIII Error! File must be either .jpg or .mp4 file.";
                         }

                         if(deleted!=null){
                            if(deleted[1]==0){
                                return "CLIII Error! Deletion failed. No file exists of this name.";
                            }else if(deleted[1]>0 && deleted[0]==0){
                                return "CLIII Error! " + deleted[0] + " out of " + deleted[1] + " file has been deleted, due to permission restriction.";
                            }else if(deleted[1]>0 && deleted[0]>0){
                                return "CLIII Info! "+ deleted[0] + " out of " + deleted[1] + " file(s) has been deleted.";
                            }
                         }else{
                             return "CLIII Error! Deletion Command failed.";
                         }
                     }else{
                         return "CLIII Error! Command not complete, mention a command modifier- filename, or all.";
                     }
                 }else if(cmd[1].toLowerCase().equals("set_perm")){
                     if(cmd.length>2){

                     }else{
                         return "CLIII Error! Command not complete, mention a filename.";
                     }
                 }
            }else{
                return "CLIII Error! Command doesnt exist.";
            }
        }else{
            return "CLIII Error! Not a MDFS command...Type \"mdfs help\" for more information.";

        }

        return "CLIII Error! Command was not successful.";
    }

    private String handleHelpCommand() {
        return      "mdfs help                                           : Show all MDFS commands.<newline>" +
                    "mdfs directory all                                  : Show list of files available in MDFS system.<newline>" +
                    "mdfs direcotry decrypted                            : Show list of decrypted files.<newline>" +
                    "mdfs directory undecrypted                          : Show list of undecrypted files.<newline>" +
                    "mdfs set_perm <filename> <permission_list>          : Change permission of a file.<newline>" +
                    "mdfs get_perm <filename>                            : Show permissions of file available in MDFS system.<newline>" +
                    "mdfs get_perm all                                   : Show permission of all files in MDFS system.<newline>" +
                    "mdfs create <filename> permission <permission_list> : Create a file in MDFS system.<newline>" +
                    "mdfs retrieve <filename>                            : Retrieve a file from MDFS filesystem.<newline>" +
                    "mdfs retrieve all                                   : Retrieve all files from MDFS filesystem.<newline>" +
                    "mdfs detele <filename>                              : Delete a file from MDFS System owned by user.<newline>" +
                    "mdfs delete all                                     : Delete all files from MDFS System owned by User.<newline>" +
                    "<permission_list>                                   : OWNER | WORLD| GROUP | GUID(s)";
    }

    private boolean handleCreateCommand(String path, String filename, String[] perm) {
        //TODO: create file
        return true;
    }

    public int handleRetrieveCommand(String filename){
        //TODO: retrieve file
        return 100;
    }

    public String handleDirectoryCommand(String cmdModifier){
        //TODO: get directory
        return "result";
    }

    public int[] handleDeleteCommand(String cmdModifier){
        //mdfs returns a int[a,b] how many has deleted vs how many was not deleted
        //1/1 = one file has been deleted
        //50/100 = partially has been deleted
        //0/0 = no file to delete
        //0/100 = file delete failed
        //TODO: delete file
        int[] result = new int[2];
        result[0] = 50;
        result[1] = 100;
        return result;
    }


    public String[] checkPermittedNodes(String[] perm){
        List<String> permittedList = new ArrayList<>();
        for(int i=0; i< perm.length; i++){
            if (perm[i].toLowerCase().equals("owner")) {
                permittedList.add(perm[i]);  //note: dont lowercase
            } else if (perm[i].toLowerCase().equals("world")) {
                permittedList.add(perm[i]);  //note: dont lowercase
            } else if (perm[i].toLowerCase().contains("group")) {
                permittedList.add(perm[i]);  //note: dont lowercase
            } else if (perm[i].toLowerCase().length() == Constants.GUID_LENGTH) {
                permittedList.add(perm[i]);  //note: dont lowercase
            }
        }

        return permittedList.toArray(new String[permittedList.size()]);
    }


}



/*
    private void processRequestCpp(String inMessage){
        inMessage = "IMG_20181023_092605.jpg";

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/distressnet/MStorm/StreamFDPic/temp/";

        File[] listofFiles = new File(path).listFiles();
        int index = -1;
        boolean fileExists = false;
        for(int i=0; i< listofFiles.length; i++){
            if(listofFiles[i].getName().equals(inMessage)){
                index = i;
                fileExists = true;
                break;
            }
        }

        if(fileExists){
            System.out.println("CLIII file exists");
            File file = listofFiles[index];
            compressAndsendFile(file, null, false);
        }else{
            System.out.println("CLIII file not exists");
        }

        try { sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }


    }

    private void compressAndsendFile(final File orgiginalFile, final String newFileName, final boolean compressed){
        System.out.println("CLIII orginal filename: " + orgiginalFile.getName());
        System.out.println("CLIII Compress and name: " + compressed + " " + newFileName);
        final File renamedFile;
        if(newFileName != null){
            renamedFile = new File(orgiginalFile.getParent()+ File.separator + newFileName);
            orgiginalFile.renameTo(renamedFile);
            System.out.println("CLIII file was renamed");
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
                sendFile(compressedFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            sendFile(renamedFile);
        }
    }

    private void sendFile(final File file){
        //check whether file creation via rsock or tcp
        if(Constants.file_creation_via_rsock_or_tcp.equals("rsock")){  //RSOCK
            MDFSFileCreatorViaRsock creator = new MDFSFileCreatorViaRsock(file, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO);
            creator.setEncryptKey(ServiceHelper.getInstance().getEncryptKey());
            creator.setListener(fileCreatorListenerviarsock);
            creator.start();
            System.out.println("CLIII file sent");
        }else if(Constants.file_creation_via_rsock_or_tcp.equals("tcp")){
            MDFSFileCreator creator = new MDFSFileCreator(file, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO);
            creator.setEncryptKey(ServiceHelper.getInstance().getEncryptKey());
            creator.setListener(fileCreatorListener);
            creator.start();
        }

    }

    //rsock listener
    private MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock fileCreatorListenerviarsock = new MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock(){
        @Override
        public void statusUpdate(String status) {

        }

        @Override
        public void onError(String error) {

        }

        @Override
        public void onComplete() {

        }
    };

    private MDFSBlockCreator.MDFSBlockCreatorListener fileCreatorListener = new MDFSBlockCreator.MDFSBlockCreatorListener(){
        @Override
        public void statusUpdate(String status) {
        }

        @Override
        public void onError(String error) {
        }

        @Override
        public void onComplete() {
        }
    };*/
