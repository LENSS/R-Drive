package edu.tamu.cse.lenss.CLI;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsock;
import edu.tamu.lenss.mdfs.MDFSFileCreatorViaRsock;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

import static java.lang.Thread.sleep;

class RequestHandler implements Runnable{

    //class variables
    private Socket cSocket;
    Context appContext;

    //filepath
    public String fileCreatePath = "/distressnet/MStorm/StreamFDPic/temp/";

    //command parsing variables
    String[] comm = {"directory", "set_perm", "get_perm", "add_group", "remove_group", "group_list", "create", "retrieve", "delete", "help"};
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

            //make clientSockets object
            clientSockets socket = new clientSockets(inBuffer, os, cSocket);

            //generate random uuid (aka clientID)
            String uuid = UUID.randomUUID().toString().substring(0,12);

            //put the clientSockets in the static map <clientID, clientSockets>
            clientSockets.sockets.put(uuid, socket);

            //process req
            processRequestCpp(uuid, command);

        }catch (IOException e) {
            System.out.println("CLIII Problem handling client socket in RequestHandler"+ e);

        }

    }

    //this function works as a parser and syntactical analyzer
    private void processRequestCpp(String uuid, String command) {

        //check if its an empty string
        if(command.equals("")){
            clientSockets.sendAndClose(uuid, "CLIII Error! No command found...Type \"mdfs help\" for more information.");
            return;
        }

        //replace tab with space
        command = command.replaceAll("\t", " ");

        //trim the leading and trailing whitespaces
        command = command.trim();

        //replace multiple spaces into one
        command = command.replaceAll("( +)"," ");

        //split the command into space separated tokens
        String[] cmd = command.split(" ");

        //print tokens
        //for(int i=0; i<cmd.length; i++){ System.out.println("tokens: " + cmd[i]); }

        //check if the first token is "mdfs"
        if(cmd.length>0 && cmd[0].toLowerCase().equals("mdfs")){
            //check what type of command it is
            if(cmd.length>1 && commandNames.contains(cmd[1].toLowerCase())){
                //command exists, take action based on command type
                 if(cmd[1].toLowerCase().equals("help")){
                     handleHelpCommand.handleHelpCommand(uuid);
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
                                    perm = utils.checkPermittedNodes(perm);
                                    if(perm.length!=0){
                                        handleCreateCommand.handleCreateCommand(path, filename, perm, uuid);
                                    }else{
                                        clientSockets.sendAndClose(uuid, "CLIII Error! Permission list invalid or not found.");
                                    }

                                }else{
                                    clientSockets.sendAndClose(uuid, "CLIII Error! Command not complete, add Permission subcommand.");
                                }

                            }else{
                                clientSockets.sendAndClose(uuid, "CLIII Error! File must be either .jpg or .mp4 file.");
                            }
                        }else{
                            clientSockets.sendAndClose(uuid, "CLIII Error! File does not exist.");
                        }
                    }else{
                        clientSockets.sendAndClose(uuid, "CLIII Error! Command not complete, mention a filename.");
                    }
                 }else if(cmd[1].toLowerCase().equals("retrieve")){
                    if(cmd.length>2){
                        String filename = cmd[2];
                        //check if the file extension is jpg or mp4
                        if(filename.contains(".jpg")  || filename.contains(".mp4")){
                            int numOfFiles = handleRetrieveCommand(filename);   //todo: dont return anything here
                            if(numOfFiles>0){
                                clientSockets.sendAndClose(uuid, "CLIII Info! \" + numOfFiles + \" files has been retrieved.");
                            }else{
                                clientSockets.sendAndClose(uuid, "CLIII Error! no file has been retrieved.");
                            }

                        }else if(filename.toLowerCase().equals("all")){
                            int numOfFiles = handleRetrieveCommand(filename);   //todo: dont return anything here
                            if(numOfFiles>0){
                                clientSockets.sendAndClose(uuid, "CLIII Info! \" + numOfFiles + \" files has been retrieved.");
                            }else{
                                clientSockets.sendAndClose(uuid, "CLIII Error! no file has been retrieved.");
                            }
                        }else{
                            clientSockets.sendAndClose(uuid, "CLIII Error! File must be either .jpg or .mp4 file.");
                        }
                    }else{
                        clientSockets.sendAndClose(uuid, "CLIII Error! File name not mentioned.");
                    }
                 }else if(cmd[1].toLowerCase().equals("directory")){
                     if(cmd.length>2){
                        if(actionNames.contains(cmd[2].toLowerCase())){
                            handleDirectoryCommand(cmd[2]);  //todo dont return anything here
                        }else{
                            clientSockets.sendAndClose(uuid, "CLIII Error! Command modifier not valid, mention a valid command modifier- decrypted, undecrypted, or all.");
                        }
                     }else{
                         clientSockets.sendAndClose(uuid, "CLIII Error! Command not complete, mention a command modifier- decrypted, undecrypted, or all.");
                     }
                 }else if(cmd[1].toLowerCase().equals("delete")){
                     if(cmd.length>2){
                         int[] deleted = null;
                         if((cmd[2].toLowerCase().equals("all"))){
                            deleted = handleDeleteCommand(cmd[2]);  //todo: dont return anything here
                         }else if(cmd[2].contains(".jpg") || cmd[2].contains(".mp4")){
                            deleted = handleDeleteCommand(cmd[2]);  //todo: dont return anything here
                         }else{
                             clientSockets.sendAndClose(uuid, "CLIII Error! File must be either .jpg or .mp4 file.");
                         }

                         /*if(deleted!=null){   //todo: use this block somewhere else
                            if(deleted[1]==0){
                                return "CLIII Error! Deletion failed. No file exists of this name.";
                            }else if(deleted[1]>0 && deleted[0]==0){
                                return "CLIII Error! " + deleted[0] + " out of " + deleted[1] + " file has been deleted, due to permission restriction.";
                            }else if(deleted[1]>0 && deleted[0]>0){
                                return "CLIII Info! "+ deleted[0] + " out of " + deleted[1] + " file(s) has been deleted.";
                            }
                         }else{
                             return "CLIII Error! Deletion Command failed.";
                         }*/
                     }else{
                         clientSockets.sendAndClose(uuid, "CLIII Error! Command not complete, mention a command modifier- filename, or all.");
                     }
                 }else if(cmd[1].toLowerCase().equals("set_perm")){
                     if(cmd.length>2){
                        String filename = cmd[2];
                        if(filename.contains(".jpg") || filename.contains(".mp4")){
                            if(cmd.length>3){
                                String[] perm = Arrays.asList(cmd).subList(3, cmd.length-1 + 1).toArray(new String[0]);
                                //check if the permitted entities are either "owner", "group", "world" or a 40 bytes GUID
                                perm = utils.checkPermittedNodes(perm);
                                if(perm.length!=0){
                                    //call handleSetPermCommand function
                                    boolean reply = handleSetPermCommand(filename, perm);  //todo: dont return anything here
                                    if(reply==true){
                                        clientSockets.sendAndClose(uuid, "CLIII Success! File permission change success.");
                                    }else{
                                        clientSockets.sendAndClose(uuid, "CLIII Error! File permission change failure.");
                                    }

                                }else{
                                    clientSockets.sendAndClose(uuid, "CLIII Error! Permission list invalid.");
                                }
                            }else{
                                clientSockets.sendAndClose(uuid, "CLIII Error! Permission list not found.");
                            }
                        }else{
                            clientSockets.sendAndClose(uuid, "CLIII Error! File must be either .jpg or .mp4 file.");
                        }
                     }else{
                         clientSockets.sendAndClose(uuid, "CLIII Error! Command not complete, mention a filename.");
                     }
                 }else if(cmd[1].toLowerCase().equals("get_perm")){
                     if(cmd.length>2){
                         if(cmd[2].toLowerCase().equals("all")){
                            String reply = handleGetPermCommand(cmd[2]);  //todo: dont return anywhere here
                         }else{
                             if(cmd[2].contains(".jpg") || cmd[2].contains(".mp4")){
                                 String reply = handleGetPermCommand(cmd[2]);  //todo: dont return anywhere here
                             }else{
                                 clientSockets.sendAndClose(uuid, "CLIII Error! File must be either .jpg or .mp4 file.");

                             }
                         }
                     }else{
                         clientSockets.sendAndClose(uuid, "CLIII Error! Command not complete, mention a command modifier- filename, or all.");
                     }
                 }else if(cmd[1].toLowerCase().equals("add_group")){
                    //todo
                 }else if(cmd[1].toLowerCase().equals("remove_group")){
                     //todo
                 }else if(cmd[1].toLowerCase().equals("group_list")){
                     //todo
                 }
            }else{
                clientSockets.sendAndClose(uuid, "CLIII Error! Command doesnt exist.");
            }
        }else{
            clientSockets.sendAndClose(uuid, "CLIII Error! Not a MDFS command...Type \"mdfs help\" for more information.");

        }

    }

    private String handleGetPermCommand(String filename) {
        //TODO: get permission
        //create connection to edgekeeper
        //send permission list, ownGUID
        //edgeKeeper:get all files permission lists
        return "result";
    }

    private boolean handleSetPermCommand(String filename, String[] perm) {
        //TODO: change file permission
        //create connection to edgekeeper
        //send parmission List, filename, ownGUID
        //edgekeeper: //checks if a file of this name exists
                      //if exists, check if the requester is the owner of the file
                      //if it is change the permission..return true.
        return true;
    }




    public int handleRetrieveCommand(String filename){
        //TODO: retrieve file
        //send filename, guid to edgekeeper
        //retrieve metadata for this file from edgekeeper
        //if metadata has been refused due to file permission, then return 0.
        //else,list of metadataaa for all the files those this user has permission
        //exclude the files this node created, aka already has fragments
        //for each file,
            //retrieve the file frags;
            //upon retrieving each file frag, update own directory as done normally(already done)
        return 100;
    }

    public String handleDirectoryCommand(String cmdModifier){
        //TODO: get directory
        //send directory req
        //receive directory reply with all files names
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

    //handle group command
        //change group pull/push in every EdgeKeeperMetadata object




//==============================================================================



}




