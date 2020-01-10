package edu.tamu.lenss.MDFS.Commands.get;

import org.apache.log4j.Level;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSRsockBlockForFileRetrieve;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;

public class get {

    public static ExecutorService executor = Executors.newSingleThreadExecutor();

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
            MDFSFileInfo fileInfo  = new MDFSFileInfo(metadata.getFileName(), metadata.getFileID(), metadata.getFilePathMDFS());
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

    //this function is used,
    //for sending a file fetch request from neighbor edge using rsock.
    public static void getFileFromNeighbor(String filename, String filePathMDFS, String neighborMasterGUID){

        //TODO: first check if the file had already been retrieved.
        //check the SP we use for book keeping

        //class that sends fragment requests to other nodes
        class sendREq implements Runnable{

            public sendREq(){}

            @Override
            public void run(){
                //make a MDFSRsockBlockForFileRetrieve of type = RequestFromOneClientInOneAEdgeToMasterOfAnotherEdgeForWholeFile
                //note: a lot of fields are unknown to us, so we put dummy data or null.
                MDFSRsockBlockForFileRetrieve mdfsrsockblock = new MDFSRsockBlockForFileRetrieve(UUID.randomUUID().toString(), MDFSRsockBlockForFileRetrieve.Type.RequestFromOneClientInOneAEdgeToMasterOfAnotherEdgeForWholeFile, (byte)-1, (byte)-1, EdgeKeeper.ownGUID, neighborMasterGUID, filename, filePathMDFS, "FileIDUnknown", (byte)-1, (byte)-1, (byte)-1, "/storage/emulated/0/" + Constants.DEFAULT_DECRYPTION_FOLDER_NAME + "/", null, false);

                //get byteArray from object and size of the MDFSRsockBlockRetreival obj
                byte[] data = null;
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = null;
                    oos = new ObjectOutputStream(bos);
                    oos.writeObject(mdfsrsockblock);
                    oos.flush();
                    data = bos.toByteArray();
                } catch (Exception e) {
                    logger.log(Level.DEBUG, "could not convert object into bytes.");
                }

                //send request
                if (data != null) {
                    String uuid = UUID.randomUUID().toString().substring(0, 12);
                    boolean sent = false;
                    if(RSockConstants.RSOCK) {
                        sent = RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", neighborMasterGUID, 0);
                    }

                    if(sent){
                        //log
                        logger.log(Level.ALL, "fragment request for file "  + filePathMDFS+filename + " sent to neighbor master " + neighborMasterGUID);

                    }else{
                        //log
                        logger.log(Level.ALL, "failed to send fragment request for file "  + filePathMDFS+filename + " to neighbor master " + neighborMasterGUID);
                    }
                }
            }

        }

        //the file doesnt exist locally,
        //so need to send request to other nodes
        executor.submit(new sendREq());

    }

}
