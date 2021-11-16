package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.tamu.lenss.MDFS.Commands.copyfromlocal.copyfromlocal;
import edu.tamu.lenss.MDFS.Commands.get.get;
import edu.tamu.lenss.MDFS.Commands.help.help;
import edu.tamu.lenss.MDFS.Commands.ls.ls;
import edu.tamu.lenss.MDFS.Commands.ls.lsUtils;
import edu.tamu.lenss.MDFS.Commands.mkdir.mkdir;
import edu.tamu.lenss.MDFS.Commands.put.put;
import edu.tamu.lenss.MDFS.Commands.rm.rm;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.RSock.testRsock;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;


//this class processes one mdfs command
public class ProcessOneRequest {

    //commands
    private static String[] comm = { "-help", "-ls", "-list", "-mkdir", "-rm", "-setfacl", "-getfacl", "-put", "-get", "-copyFromLocal", "-copyToLocal", "-hdfs", "-testRsock"};
    private static Set<String> commandNames = new HashSet<>(Arrays.asList(comm));


    //this function works as a parser and syntactical analyzer
    public static String processRequest(String clientID, String command) {

        //check if its an empty string
        if(command.equals("")){
            clientSockets.sendAndClose(clientID, "No such command found. Type \"mdfs help\" for more information.");
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
        if(cmd.length>0 && cmd[0].equals("Mdfs")  || cmd.length>0 && cmd[0].equals("mdfs") || cmd.length>0 && cmd[0].equals("./mdfs")  || cmd.length>0 && cmd[0].equals("./Mdfs")){

            //check if there is any second token
            if(cmd.length>1){

                //check what type of operand the second token is
                if(commandNames.contains(cmd[1])) {

                    //operand exists, take action based on operand type
                    if (cmd[1].equals("-help")) {

                        //handle help command and return reply
                        String reply = help.help(clientID);
                        clientSockets.sendAndClose(clientID, reply);
                        return reply;

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
                            String isValLocDir  = utils.isValidLocalDirInAndroidPhone(filepathLocal);  //Isagor0!

                            if(isValLocDir.equals("OK")) {

                                //special case for survey123 appdata directory
                                //replace <SuRvEy123> with a space.
                                if(filepathLocal.contains("<SuRvEy123>")){
                                    filepathLocal = filepathLocal.replace("<SuRvEy123>", " ");
                                }

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

                                                String reply= new put().put(filepathLocal, filePathMDFS, filename, clientID);
                                                clientSockets.sendAndClose(clientID, reply);
                                                return reply;

                                            } else {
                                                String reply = "Error! " + "MDFS " + dirValidCheck;
                                                clientSockets.sendAndClose(clientID, reply);
                                                return reply;
                                            }
                                        } else {
                                            String reply = "Command not complete, mention a filepath in MDFS where the file will be stored.";
                                            clientSockets.sendAndClose(clientID, reply);
                                            return reply;
                                        }
                                    } else {
                                        String reply = "File Extension not supported.";
                                        clientSockets.sendAndClose(clientID, reply);
                                        return reply;
                                    }
                                } else {
                                    String reply = "Directory/file does not exist.";
                                    clientSockets.sendAndClose(clientID, reply);
                                    return reply;
                                }
                            }else{
                                String reply = "Error! " + "Android " + isValLocDir;   //Isagor0!
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;
                            }
                        } else {
                            String reply = "Command not complete, mention a filename with absolute path...ex:/storage/emulated/0/.../test.jpg ";   //Isagor0!
                            clientSockets.sendAndClose(clientID, reply);
                            return reply;
                        }
                    }else if(cmd[1].equals("-get")){

                        //check if the next token exists that would be filename with MDFS dir
                        if(cmd.length>2){

                            //get the MDFS path
                            String mdfsDirWithFilename = cmd[2];

                            //check if the last char of mdfsDirWithFilename is slash
                            if(mdfsDirWithFilename.charAt(mdfsDirWithFilename.length()-1)=='/'){

                                String reply = "Filename not mentioned.";
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;

                            }else{

                                //divide the mdfsDirWithFilename in tokens
                                String[] dirTokens = mdfsDirWithFilename.split("/");

                                //remove all empty strings
                                dirTokens = IOUtilities.delEmptyStr(dirTokens);

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

                                            //next token exists that is local dir
                                            String outputDir = cmd[3];

                                            //check if local path is valid
                                            String locDirValid = utils.isValidLocalDirInAndroidPhone(outputDir);  //Isagor0!

                                            if(locDirValid.equals("OK")){

                                                //handle get command and return reply
                                                String reply = get.get(mdfsDirWithFilename, outputDir);


                                                clientSockets.sendAndClose(clientID, reply);
                                                return reply;

                                            }else{
                                                String reply = "Invalid Android directory, " + locDirValid;
                                                clientSockets.sendAndClose(clientID, reply);
                                                return reply;
                                            }


                                        }else{
                                            String reply = "Command not complete, local Android filepath not mentioned.";
                                            clientSockets.sendAndClose(clientID, reply);
                                            return reply;
                                        }


                                    }else{
                                        String reply = "Error! MDFS " + isMDFSdirValid;
                                        clientSockets.sendAndClose(clientID, reply);
                                        return reply;
                                    }

                                }else{
                                    String reply = "Command not complete, Filename not mentioned.";
                                    clientSockets.sendAndClose(clientID, reply);
                                    return reply;
                                }

                            }
                        }else{
                            String reply = "Command not complete, mention a filename with MDFS filepath.";
                            clientSockets.sendAndClose(clientID, reply);
                            return reply;
                        }
                    }else if (cmd[1].equals("-mkdir")) {

                        //check if the next token exists that is a mdfs directory
                        if (cmd.length > 2) {

                            //get the mdfs directory token
                            String mdfsDir = cmd[2];

                            //check if the mdfs directory is valid
                            String isDirValid = utils.isValidMDFSDir(mdfsDir);
                            if(isDirValid.equals("OK")){

                                //handle mkdir command and return reply
                                String reply = mkdir.mkdir(mdfsDir);
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;

                            }else{
                                String reply = "Error! MDFS " + isDirValid;
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;
                            }
                        } else {
                            String reply = "Command not complete, mention a new MDFS directory.";
                            clientSockets.sendAndClose(clientID, reply);
                            return reply;
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
                                String reply  = "Cannot delete / directory.";
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;

                            }else {

                                //check if its a subDir deletion request or a file deletion request
                                //split the dir in tokens
                                String[] dirTokens = dir.split("/");

                                //remove empty strings
                                dirTokens = IOUtilities.delEmptyStr(dirTokens);


                                //check if the last elem is file or dir
                                String reqType = "";
                                if (utils.checkFileExtension(dirTokens[dirTokens.length - 1])) {
                                    reqType = rm.RM_FILE;
                                } else {
                                    reqType = rm.RM_DIRECTORY;
                                }

                                //handle rm command and return reply
                                String reply = rm.rm(dir,reqType);
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;

                            }

                        }else{
                            String reply = "Mention a MDFS directory or a MDFS filepath with filename to delete.";
                            clientSockets.sendAndClose(clientID, reply);
                            return reply;
                        }
                    }else if(cmd[1].equals("-ls")){

                        //check if next token exists
                        if(cmd.length>2){

                            //get the next token.
                            //next token is a MDFS dir.
                            String mdfsDir  = cmd[2];

                            //check if the dir is valid
                            String isDirValid = utils.isValidMDFSDir(mdfsDir);

                            if(isDirValid.equals("OK")){

                                //handle ls command and return reply
                                String reply = ls.ls(mdfsDir, "lsRequestForBothOwnAndNeighborEdge" );
                                clientSockets.sendAndClose(clientID, lsUtils.jsonToPlainString(reply));
                                return reply;

                            }else{
                                String reply = "MDFS " + isDirValid;
                                clientSockets.sendAndClose(clientID, reply);
                                return reply;
                            }
                        }else{
                            String reply = "Mention a MDFS directory to list files and folders.";
                            clientSockets.sendAndClose(clientID, reply);
                            return reply;
                        }
                    }else if(cmd[1].equals("-copyFromLocal")){

                        //handle copyfromlocal command and return reply
                        String reply = copyfromlocal.copyfromlocal(cmd);
                        clientSockets.sendAndClose(clientID, reply);

                    }else if(cmd[1].equals("-copyToLocal")){

                        //handle copytolocal command
                        copytolocal.copytolocal(clientID, cmd);

                    }else if(cmd[1].equals("-hdfs")){

                        String reply = "Hadoop HDFS commands are not supported.";
                        clientSockets.sendAndClose(clientID, reply);
                        return reply;

                    }else if(cmd[1].equals("-testRsock")){
                        String reply = testRsock.testrsock();
                        clientSockets.sendAndClose(clientID, reply);
                        return reply;
                    }else{
                        String reply = "Command has not been implemented yet.";
                        clientSockets.sendAndClose(clientID, reply);
                        return reply;
                    }
                }else{
                    String reply = "No command "+ cmd[1] + " found.";
                    clientSockets.sendAndClose(clientID, reply);
                    return reply;
                }
            }else{
                String reply = "Operand missing.";
                clientSockets.sendAndClose(clientID, reply);
                return reply;
            }
        }else{
            String reply = "Not a MDFS command...Type \"mdfs -help\" for more information.";
            clientSockets.sendAndClose(clientID, reply);
            return reply;

        }

        return "OK";
    }
}
