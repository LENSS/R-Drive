package edu.tamu.lenss.mdfs.handleCommands.rm;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class rm {

    public static String rm(String mdfsPath, String reqType){
        try {
            if (reqType.equals("del_file")) {

                //send rm_file request
                JSONObject repJSON = EKClient.rm_file(mdfsPath);

                //check reply
                if (repJSON != null) {

                    //check for success or error
                    if (repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                        //convert Json String into class object
                        MDFSMetadata metadata = MDFSMetadata.createMetadataFromBytes(repJSON.getString(RequestTranslator.MDFSmetadataField).getBytes());

                        //check for null
                        if(metadata!=null){

                            //get file name
                            String fileName = IOUtilities.getFileNameFromFullPath(metadata.getFilePathMDFS());

                            //get file ID
                            long fileID = metadata.getCreatedTime();

                            //delete the fragment from my disk
                            try {
                                ServiceHelper.getInstance().getDirectory().deleteFile(fileID, fileName);
                            }catch(Exception e){
                                e.printStackTrace();
                            }

                            //get all unique fragmentHoldersGUIDs
                            List<String> uniqueFragHoldersGUIDs = metadata.getAllUniqueFragmentHolders();

                            //send deletion to each guid
                            for(String guid: uniqueFragHoldersGUIDs){
                                sendDeletionReq(fileName, fileID, guid);
                            }

                            //return success message
                            return "File deletion success!";

                        }else{
                            return "File has been deleted from edgeKeeper but could not trigger deletion to other MDFS devices.";
                        }


                    } else {
                        //return error message
                        return "-rm failed! " + repJSON.getString(RequestTranslator.messageField);
                    }
                } else {
                    //could not connect to edgekeeper
                    return "Could not connect to local EdgeKeeper.";
                }

            } else if (reqType.equals("del_dir")) {

                //send rm_directory request
                JSONObject repJSON = EKClient.rm_directory(mdfsPath);

                //check reply
                if (repJSON != null) {

                    //check for success or error
                    if (repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                        //return success message
                        return "Directory deletion success!";  //todo: delete all the file list it returns

                    } else {

                        //return error message
                        return "-rm failed! " + repJSON.getString(RequestTranslator.messageField);
                    }
                } else {

                    //could nnot connect to edgekeeper
                    return "Could not connect to local EdgeKeeper.";
                }
            }
        }catch(JSONException e){
            return "Json exception.";
        }

        return null;
    }

    //takes a filename, fileid, guid, and sends deletion through rsock to destination
    private static void sendDeletionReq(String fileName, long fileID, String GUID){

        //make deletion payload
        String delCommand = fileName + RSockConstants.deletion_tag + Long.toString(fileID);

        //send through rsock and dont expect reply
        RSockConstants.intrfc_deletion.send(UUID.randomUUID().toString().substring(0, 12), delCommand.getBytes(), delCommand.length(), "nothing", "nothing", GUID, 0, "default","default", "default");

    }
}
