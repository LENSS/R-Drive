package edu.tamu.lenss.mdfs.handleCommands.get;

import org.json.JSONObject;

import java.io.File;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;

public class get {


    public static String get_devel(String mdfsDirWithFilename, String localDir){
        //first retrieve the metadata from edgeKeeper
        MDFSMetadata metadata = fetchFileMetadataFromEdgeKeeper(mdfsDirWithFilename);

        //check for null
        if(metadata!=null){

            //re-create MDFSFileInfo object
            MDFSFileInfo fileInfo  = new MDFSFileInfo(metadata.getFileName(), metadata.getCreatedTime());
            fileInfo.setFileSize(metadata.getFileSize());
            fileInfo.setNumberOfBlocks((byte)metadata.getBlockCount());
            fileInfo.setFragmentsParms((byte)metadata.getn2(),  (byte)metadata.getk2());

            //make mdfsfileretriever object
            MDFSFileRetrieverViaRsockNG retriever = new MDFSFileRetrieverViaRsockNG(fileInfo, metadata, localDir, ServiceHelper.getInstance().getEncryptKey());

            //return
            return retriever.start();



        }else{
            return "-get failed! Could not fetch file metadata from local EdgeKeeper (check connection).";
        }
    }


    public static String get(String mdfsDirWithFilename, String localDir){

        //first retrieve the metadata from edgeKeeper
        MDFSMetadata metadata = fetchFileMetadataFromEdgeKeeper(mdfsDirWithFilename);

        //check for null
        if(metadata!=null){

            //re-create MDFSFileInfo object
            MDFSFileInfo fileInfo  = new MDFSFileInfo(metadata.getFileName(), metadata.getCreatedTime());
            fileInfo.setFileSize(metadata.getFileSize());
            fileInfo.setNumberOfBlocks((byte)metadata.getBlockCount());
            fileInfo.setFragmentsParms((byte)metadata.getn2(),  (byte)metadata.getk2());

            //make mdfsfileretriever object
            MDFSFileRetrieverViaRsock retriever = new MDFSFileRetrieverViaRsock(fileInfo, metadata, localDir);
            retriever.setDecryptKey(ServiceHelper.getInstance().getEncryptKey());
            retriever.setListener(fileRetrieverListenerviarsock);
            retriever.start();

            //send reply to cli client
            return "-get Info: request has been placed.";


        }else{
            return "-get failed! Could not fetch file metadata from local EdgeKeeper. (execute -ls command to check if file exists.).";
        }
    }


    //fetches file metadata from EdgeKeeper.
    //returns FileMetadata object or null.
    private static MDFSMetadata fetchFileMetadataFromEdgeKeeper(String mdfsDirWithFilename) {

        try {
            //request to get file metadata
            JSONObject repJSON = EKClient.getMetadata(mdfsDirWithFilename);

            //check reply
            if (repJSON != null) {

                //get success or error message
                if (repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                    //get metadata string and convert into class
                    MDFSMetadata metadata = MDFSMetadata.createMetadataFromBytes(repJSON.getString(RequestTranslator.MDFSmetadataField).getBytes());

                    //check Gson conversion succeeded
                    if(metadata!=null){
                        return metadata;
                    }else{
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }catch(Exception e){
            return null;
        }

    }

    //rsock listener
    private static MDFSBlockRetrieverViaRsock.BlockRetrieverListenerViaRsock fileRetrieverListenerviarsock = new MDFSBlockRetrieverViaRsock.BlockRetrieverListenerViaRsock(){        //RSOCK

        @Override
        public void onError(String error, MDFSFileInfo fileInfo) {
            System.out.println("xxx:::" + error);
            return;
        }

        @Override
        public void statusUpdate(String status) {
            System.out.println("xxx:::" + status);
            return;
        }

        @Override
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
            System.out.println("xxx:::" + "success");
            return;
        }
    };
}
