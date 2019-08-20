package edu.tamu.cse.lenss.CLI;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.tamu.lenss.mdfs.EdgeKeeper.Directory;

import static java.lang.Thread.sleep;

class RequestHandler implements Runnable{

    //class variables
    private Socket cSocket;
    Context appContext;


    //commands
    String[] comm = { "-help", "-ls", "-list", "-mkdir", "-rm", "-setfacl", "-getfacl", "-put", "-get", "-copyFromLocal", "-copyToLocal", "-hdfs"};
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
            String command = inBuffer.readLine();  //note: the reading finishes until one or more \n is read

            //make clientSockets object
            clientSockets socket = new clientSockets(inBuffer, os, cSocket);

            //generate random uuid (aka clientID)
            String clientID = UUID.randomUUID().toString().substring(0,12);

            //put the clientSockets in the static map <clientID, clientSockets>
            clientSockets.sockets.put(clientID, socket);

            //process req
            processRequestCpp(clientID, command);

        }catch (IOException e) {
            System.out.println("Problem handling client socket in RequestHandler "+ e);

        }

    }

    //this function works as a parser and syntactical analyzer
    private void processRequestCpp(String clientID, String command) {

        //check if its an empty string
        if(command.equals("")){
            clientSockets.sendAndClose(clientID, "No such command found. Type \"mdfs help\" for more information.");
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
        if(cmd.length>0 && cmd[0].equals("mdfs") || cmd.length>0 && cmd[0].equals("./mdfs")){

            //check if there is any second token
            if(cmd.length>1){

                //check what type of operand the second token is
                if(commandNames.contains(cmd[1])) {

                    //operand exists, take action based on operand type
                    if (cmd[1].equals("-help")) {

                        handleHelpCommand.handleHelpCommand(clientID);

                    } else if (cmd[1].equals("-put")) {

                        //check if third token exists which is <local_filepath>
                        if (cmd.length > 2) {

                            String fileNameWithFullLocalPath = cmd[2];

                            //get filename and filepath
                            String filename = "";
                            String filepathLocal = "";
                            for (int i = fileNameWithFullLocalPath.length() - 1; i >= 0; i--) {
                                if (fileNameWithFullLocalPath.charAt(i) != '/') {
                                    filename = filename + fileNameWithFullLocalPath.charAt(i);
                                } else {
                                    filename = new StringBuilder(filename).reverse().toString();
                                    filepathLocal = fileNameWithFullLocalPath.replace(filename, "");
                                    break;
                                }
                            }


                            //check if the filePathLocal is valid
                            String isValLocDir  = utils.isValidLocalDirInAndroidPhone(filepathLocal);

                            if(isValLocDir.equals("OK")) {


                                //check if this file exists
                                File[] listoffiles = new File(filepathLocal).listFiles();
                                boolean fileExists = false;
                                if (listoffiles == null) {
                                    fileExists = false;
                                } else {
                                    for (int i = 0; i < listoffiles.length; i++) {
                                        if (listoffiles[i].getName().equals(filename)) {
                                            fileExists = true;
                                            break;
                                        }
                                    }
                                }

                                //file exists
                                if (fileExists) {

                                    //check if the file extension is jpg or mp4 or txt or pdf etc
                                    if (utils.checkFileExtension(filename)) {

                                        //check if fourth token exists and is <mdfs_filepath>
                                        if (cmd.length > 3) {
                                            String filePathMDFS = cmd[3];

                                            //check if mdfs filepath is valid
                                            String dirValidCheck = utils.isValidMDFSDir(filePathMDFS);
                                            if (dirValidCheck.equals("OK")) {

                                                //dir is valid, get the perm list
                                                String[] perm = utils.checkPermittedNodes("WORLD");  //dummy

                                                //do the job
                                                handlePutCommand hand = new handlePutCommand();
                                                hand.handleCreateCommand(filepathLocal, filePathMDFS, filename, perm, clientID);

                                            } else {
                                                clientSockets.sendAndClose(clientID, "Error! " + "MDFS " + dirValidCheck);
                                            }

                                        } else {
                                            clientSockets.sendAndClose(clientID, "Command not complete, mention a filepath in MDFS where the file will be stored.");
                                        }
                                    } else {
                                        clientSockets.sendAndClose(clientID, "File Extension not supported.");
                                    }
                                } else {
                                    clientSockets.sendAndClose(clientID, "Directory/file does not exist.");
                                }
                            }else{
                                clientSockets.sendAndClose(clientID, "Error! " + "Android " + isValLocDir);
                            }
                        } else {
                            clientSockets.sendAndClose(clientID, "Command not complete, mention a filename with absolute path...ex:/storage/emulated/0/.../test.jpg ");
                        }
                    }else if(cmd[1].equals("-get")){

                        //check if the next token exists that would be filename with MDFS dir
                        if(cmd.length>2){

                            //get the MDFS path
                            String mdfsDirWithFilename = cmd[2];

                            //check if the last char of mdfsDirWithFilename is slash
                            if(mdfsDirWithFilename.charAt(mdfsDirWithFilename.length()-1)=='/'){
                                clientSockets.sendAndClose(clientID, "Filename not mentioned.");
                            }else{

                                //divide the mdfsDirWithFilename in tokens
                                String[] dirTokens = mdfsDirWithFilename.split("/");

                                //remove all empty strings
                                dirTokens = Directory.delEmptyStr(dirTokens);

                                //check if the last token is a filename
                                if(utils.checkFileExtension(dirTokens[dirTokens.length-1])){

                                    //get the filename
                                    String filename = dirTokens[dirTokens.length-1];

                                    //get the MDFS directory of the file
                                    String mdfsDir = "/";
                                    if(dirTokens.length>1){
                                        for(int i=0; i< dirTokens.length-1; i++){ mdfsDir = mdfsDir + dirTokens[i] + "/"; }
                                    }else{
                                        //the file is at the root
                                        mdfsDir = mdfsDir;
                                    }

                                    //check if MDFS dir is valid
                                    String isMDFSdirValid = utils.isValidMDFSDir(mdfsDir);
                                    if(isMDFSdirValid.equals("OK")){

                                        //mdfs dir is valid
                                        //check if next token exists
                                        if(cmd.length>3){

                                            //next token exists tht is local dir
                                            String locDir = cmd[3];

                                            //check if local path is valid
                                            String locDirValid = utils.isValidLocalDirInAndroidPhone(locDir);

                                            if(locDirValid.equals("OK")){

                                                //do the job
                                                handleGETrequest.handleGETrequest(clientID, filename, mdfsDir, locDir);


                                            }else{
                                                clientSockets.sendAndClose(clientID, "Invalid local directory, " + locDirValid);
                                            }


                                        }else{
                                            clientSockets.sendAndClose(clientID, "Command not comeplete, local filepath not mentioned.");
                                        }


                                    }else{
                                        clientSockets.sendAndClose(clientID, "Error! MDFS " + isMDFSdirValid);
                                    }

                                }else{
                                    clientSockets.sendAndClose(clientID, "Command not complete, Filename not mentioned.");
                                }

                            }
                        }else{
                            clientSockets.sendAndClose(clientID, "Command not complete, mention a filename with MDFS filepath.");
                        }
                    }else if (cmd[1].equals("-mkdir")) {

                        //check if the next token exists that is a mdfs directory
                        if (cmd.length > 2) {

                            //get the mdfs directory token
                            String mdfsDir = cmd[2];

                            //check if the mdfs directory is valid
                            String isDirValid = utils.isValidMDFSDir(mdfsDir);
                            if(isDirValid.equals("OK")){

                                //do the job
                                handleMkdirCommand.handleMkdirCommand(clientID, mdfsDir);

                            }else{
                                clientSockets.sendAndClose(clientID, "Error! MDFS " + isDirValid);
                            }
                        } else {
                            clientSockets.sendAndClose(clientID, "Command not complete, mention a new MDFS directory.");
                        }
                    }else if(cmd[1].equals("-rm")){

                        //check if next token exists
                        if(cmd.length>2){

                            //get the next token
                            //next token can be just a MDFS dir or a dir with filename
                            String dir = cmd[2];

                            //check if the dir is just root
                            if(dir.equals("/")){

                                //cannot delete root
                                clientSockets.sendAndClose(clientID, "Cannot delete / directory.");

                            }else if(dir.equals("/*")){

                                //delete all files and folders in the root
                                handleRMcommand.handleRMcommand(clientID, dir, new String[0], "del_dir");

                            }else {

                                //check if its a subDir deletion request or a file deletion request
                                //split the dir in tokens
                                String[] dirTokens = dir.split("/");

                                //remove empty strings
                                dirTokens = Directory.delEmptyStr(dirTokens);


                                //check if the last elem is file or dir
                                if (utils.checkFileExtension(dirTokens[dirTokens.length - 1])) {

                                    //this is a file deletion request
                                    handleRMcommand.handleRMcommand(clientID, dir, dirTokens, "del_file");
                                } else {

                                    //this is a directory deletion request
                                    handleRMcommand.handleRMcommand(clientID, dir, dirTokens, "del_dir");
                                }
                            }

                        }else{
                            clientSockets.sendAndClose(clientID, "Mention a MDFS directory or a MDFS filepath with filename to delete.");
                        }
                    }else if(cmd[1].equals("-ls")){

                        //check if next token exists
                        if(cmd.length>2){

                            //get the next token
                            //next token is a MDFS dir
                            String mdfsDir  = cmd[2];

                            //check if the dir is valid
                            String isDirValid = utils.isValidMDFSDir(mdfsDir);

                            if(isDirValid.equals("OK")){

                                //do the job
                                handleLScommand.handleLScommand(clientID, mdfsDir);

                            }else{
                                clientSockets.sendAndClose(clientID, "MDFS " + isDirValid);
                            }
                        }else{
                            clientSockets.sendAndClose(clientID, "Mention a MDFS directory to list files and folders.");
                        }
                    }else if(cmd[1].equals("-copyFromLocal")){
                        //do the job
                        handleCOPYFROMLOCALcommand.handleCOPYFROMLOCALcommand(clientID, cmd);
                    }else if(cmd[1].equals("-copyToLocal")){
                        //do the job
                        handleCOPYTOLOCALcommand.handleCOPYTOLOCALcommand(clientID, cmd);
                    }else if(cmd[1].equals("-hdfs")){
                        clientSockets.sendAndClose(clientID, "Hadoop HDFS commands are not supported.");
                    }else{
                        clientSockets.sendAndClose(clientID, "Command has not been implemented yet.");
                    }
                }else{
                    clientSockets.sendAndClose(clientID, "No command "+ cmd[1] + " found.");
                }
            }else{
                clientSockets.sendAndClose(clientID, "Operand missing.");
            }
        }else{
            clientSockets.sendAndClose(clientID, "Not a MDFS command...Type \"mdfs -help\" for more information.");
        }
    }



//==============================================================================



}




