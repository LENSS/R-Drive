package edu.tamu.lenss.MDFS.Commands.get;

import org.apache.log4j.Level;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;

public class get {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(get.class);

    public static String get(String mdfsDirWithFilename, String localDir){

        //first retrieve the metadata from edgeKeeper
        MDFSMetadata metadata = fetchFileMetadataFromEdgeKeeper(mdfsDirWithFilename);

        //check for null
        if(metadata!=null){

            //logger
            logger.log(Level.ALL, "Command:get | log: Fetched file metadata for filaname " + metadata.getFileName() + " of fileID "  + metadata.getFileID());

            //re-create MDFSFileInfo object
            MDFSFileInfo fileInfo  = new MDFSFileInfo(metadata.getFileName(), metadata.getFileID());
            fileInfo.setFileSize(metadata.getFileSize());
            fileInfo.setNumberOfBlocks((byte)metadata.getBlockCount());
            fileInfo.setFragmentsParms((byte)metadata.getn2(),  (byte)metadata.getk2());

            //make mdfsfileretriever object
            MDFSFileRetrieverViaRsock retriever = new MDFSFileRetrieverViaRsock(fileInfo, metadata, localDir, ServiceHelper.getInstance().getEncryptKey());

            //return
            return retriever.start();



        }else{

            //log
            logger.log(Level.ERROR, "Could not fetch file metadata from local EdgeKeeper for filename " + mdfsDirWithFilename);

            return "-get failed! Could not fetch file metadata from local EdgeKeeper (check connection).";
        }
    }


    //fetches file metadata from EdgeKeeper.
    //checks whether the file metadata is valid or nah.
    //returns FileMetadata object or null.
    public static MDFSMetadata fetchFileMetadataFromEdgeKeeper(String mdfsDirWithFilename) {

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

                        //log
                        logger.log(Level.ERROR, "-get failed! Could not convert json object into metadata for file " + mdfsDirWithFilename);

                        return null;
                    }
                } else {
                    //no need to add logger here, client will handle that.
                    return null;
                }
            } else {

                //log
                logger.log(Level.ERROR, "-get failed! Could not fetch metadata for file " + mdfsDirWithFilename);

                return null;
            }
        }catch(Exception e){
            return null;
        }

    }

    //TODO:
    //this function is used,
    //for sedning a file fetch request from neighbor edge.
    public static void getNG(String filename, String filePathMDFS, String neighborMasterGUID){
        //first check if the file had already been retrieved.
                //check the SP we use for book keeping

    }

}
