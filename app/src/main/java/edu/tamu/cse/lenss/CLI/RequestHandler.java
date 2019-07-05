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


    //commands
    String[] comm = { "-help", "-ls", "-list", "-mkdir", "-rm", "-setfacl", "-getfacl", "-put", "-get"};
    Set<String> commandNames = new HashSet<>(Arrays.asList(comm));

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
            String clientID = UUID.randomUUID().toString().substring(0,12);

            //put the clientSockets in the static map <clientID, clientSockets>
            clientSockets.sockets.put(clientID, socket);

            //process req
            processRequestCpp(clientID, command);

        }catch (IOException e) {
            System.out.println("CLIII Problem handling client socket in RequestHandler"+ e);

        }

    }

    //this function works as a parser and syntactical analyzer
    private void processRequestCpp(String clientID, String command) {

        //check if its an empty string
        if(command.equals("")){
            clientSockets.sendAndClose(clientID, "CLIII Error! No command found...Type \"mdfs help\" for more information.");
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
        if(cmd.length>0 && cmd[0].equals("mdfs")){
            //check what type of command the second token is
            if(cmd.length>1 && commandNames.contains(cmd[1].toLowerCase())){
                //command exists, take action based on command type
                if(cmd[1].toLowerCase().equals("-help")){
                    handleHelpCommand.handleHelpCommand(clientID);
                }else if(cmd[1].toLowerCase().equals("-put")){
                    //check if third token exists which is <local_filepath>
                    if(cmd.length>2){
                        String fileNameWithFullLocalPath = cmd[2];

                        //get filename and filepath
                        String filename = "";
                        String filepathLocal = "";
                        for(int i=fileNameWithFullLocalPath.length()-1; i>=0; i--){
                            if(fileNameWithFullLocalPath.charAt(i)!='/'){
                                filename = filename + fileNameWithFullLocalPath.charAt(i);
                            }else{
                                filename = new StringBuilder(filename).reverse().toString();
                                filepathLocal = fileNameWithFullLocalPath.replace(filename,"");
                                break;
                            }
                        }

                        //check if this file exists
                        File[] listoffiles = new File(filepathLocal).listFiles();
                        boolean fileExists = false;
                        for(int i=0; i< listoffiles.length; i++){ if(listoffiles[i].getName().equals(filename)){fileExists = true; break;} }

                        //file exists
                        if(fileExists){
                            //check if the file extension is jpg or mp4 or txt or pdf etc
                            if(utils.checkFileExtension(filename)){

                                //check if fourth token exists and is <mdfs_filepath>
                                if(cmd.length>3){
                                    String filePathMDFS = cmd[3];
                                    String[] perm = utils.checkPermittedNodes("WORLD");
                                    handleCreateCommand.handleCreateCommand(filepathLocal, filePathMDFS, filename, perm, clientID);
                                }else{
                                    clientSockets.sendAndClose(clientID, "CLIII Error! Command not complete, mention a filepath in MDFS where the file will be stored.");
                                }
                            }else{
                                clientSockets.sendAndClose(clientID, "CLIII Error! File Extension not supported.");
                            }
                        }else{
                            clientSockets.sendAndClose(clientID, "CLIII Error! Directory/file does not exist.");
                        }
                    }else{
                        clientSockets.sendAndClose(clientID, "CLIII Error! Command not complete, mention a filename with absolute path..ex:/storage/emulated/0/.../test.jpg ");
                    }
                }else if(cmd[1].toLowerCase().equals("retrieve")){
                    if(cmd.length>2){
                        String filename = cmd[2];
                        //check if the file extension is jpg or mp4 or all/ALL
                        if(!filename.equals("all") || !filename.equals("ALL")){
                            if(filename.contains(".jpg")  || filename.contains(".mp4")){
                                handleRetrieveCommand.handleRetrieveCommand(filename, clientID);
                            }else{
                                clientSockets.sendAndClose(clientID, "CLIII Error! File must be either .jpg or .mp4 file.");
                            }

                        }else{
                            //retrieve all files this node has permission of
                            handleRetrieveCommand.handleRetrieveCommand("ALL", clientID);
                        }
                    }else{
                        clientSockets.sendAndClose(clientID, "CLIII Error! File name not mentioned.");
                    }
                }else if(cmd[1].toLowerCase().equals("directory")){
                     if(cmd.length>2){

                     }else{
                         clientSockets.sendAndClose(clientID, "CLIII Error! Command not complete, mention a command modifier- decrypted, undecrypted, or all.");
                     }
                }else if(cmd[1].toLowerCase().equals("delete")){
                     if(cmd.length>2){
                         int[] deleted = null;
                         if((cmd[2].toLowerCase().equals("all"))){
                            deleted = handleDeleteCommand(cmd[2]);  //todo: dont return anything here
                         }else if(cmd[2].contains(".jpg") || cmd[2].contains(".mp4")){
                            deleted = handleDeleteCommand(cmd[2]);  //todo: dont return anything here
                         }else{
                             clientSockets.sendAndClose(clientID, "CLIII Error! File must be either .jpg or .mp4 file.");
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
                         clientSockets.sendAndClose(clientID, "CLIII Error! Command not complete, mention a command modifier- filename, or all.");
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
                                        clientSockets.sendAndClose(clientID, "CLIII Success! File permission change success.");
                                    }else{
                                        clientSockets.sendAndClose(clientID, "CLIII Error! File permission change failure.");
                                    }

                                }else{
                                    clientSockets.sendAndClose(clientID, "CLIII Error! Permission list invalid.");
                                }
                            }else{
                                clientSockets.sendAndClose(clientID, "CLIII Error! Permission list not found.");
                            }
                        }else{
                            clientSockets.sendAndClose(clientID, "CLIII Error! File must be either .jpg or .mp4 file.");
                        }
                     }else{
                         clientSockets.sendAndClose(clientID, "CLIII Error! Command not complete, mention a filename.");
                     }
                }else if(cmd[1].toLowerCase().equals("get_perm")){
                     if(cmd.length>2){
                         if(cmd[2].toLowerCase().equals("all")){
                            String reply = handleGetPermCommand(cmd[2]);  //todo: dont return anywhere here
                         }else{
                             if(cmd[2].contains(".jpg") || cmd[2].contains(".mp4")){
                                 String reply = handleGetPermCommand(cmd[2]);  //todo: dont return anywhere here
                             }else{
                                 clientSockets.sendAndClose(clientID, "CLIII Error! File must be either .jpg or .mp4 file.");

                             }
                         }
                     }else{
                         clientSockets.sendAndClose(clientID, "CLIII Error! Command not complete, mention a command modifier- filename, or all.");
                     }
                }else if(cmd[1].toLowerCase().equals("add_group")){
                    //todo
                }else if(cmd[1].toLowerCase().equals("remove_group")){
                     //todo
                }else if(cmd[1].toLowerCase().equals("group_list")){
                     //todo
                 }
            }else{
                clientSockets.sendAndClose(clientID, "CLIII Error! Command incomplete.");
            }
        }else{
            clientSockets.sendAndClose(clientID, "CLIII Error! Not a MDFS command...Type \"mdfs -help\" for more information.");

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




