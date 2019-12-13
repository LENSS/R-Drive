package edu.tamu.lenss.MDFS.Commands.put;
import android.os.Environment;
import com.google.common.io.Files;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import rsock.Topology;

import static java.lang.Thread.sleep;


public class MDFSFileCreatorViaRsockNG{

    //log
    static Logger logger = Logger.getLogger(MDFSFileCreatorViaRsockNG.class);

    //variables
    private File file;                      //the actual file
    private MDFSFileInfo fileInfo;          //file information
    private String fileID;                  //file ID
    private byte[] encryptKey;              //encryption key
    private int blockCount;
    private int maxBlockSize;
    private double encodingRatio;
    private boolean isN2K2Chosen = false;
    private boolean isPartComplete = false;
    private boolean isSending = false;
    List<String> chosenNodes;
    MDFSMetadata metadata;                  //metadata object for this file
    String filePathMDFS;                    //virtual directory path in MDFS in which the file will be saved. if dir doesnt exist, it willbe created first
    String fileCreationReqUUID;             //unique id


    //private default constructor
    private MDFSFileCreatorViaRsockNG(){}

    //public constructor
    public MDFSFileCreatorViaRsockNG(
                File f,
                String filePathMDFS,
                int maxBlockSize,
                double encodingRatio,
                byte[] key) {
        this.fileID = UUID.randomUUID().toString().replaceAll("-", "");
        this.fileCreationReqUUID = UUID.randomUUID().toString();
        this.file = f;
        this.filePathMDFS = filePathMDFS;
        this.encodingRatio = encodingRatio;
        this.blockCount = (int)Math.ceil((double)file.length()/maxBlockSize);
        this.maxBlockSize = maxBlockSize;
        this.fileInfo = new MDFSFileInfo(file.getName(), fileID, filePathMDFS);
        this.fileInfo.setFileSize(file.length());
        this.fileInfo.setNumberOfBlocks((byte)blockCount);
        this.encryptKey = key;
        this.chosenNodes = new ArrayList<>();
        this.metadata = MDFSMetadata.createFileMetadata(fileCreationReqUUID, fileID, fileInfo.getFileSize(), EdgeKeeper.ownGUID, EdgeKeeper.ownGUID, filePathMDFS + "/" + fileInfo.getFileName(), Constants.metadataIsGlobal);
    }



    //start function to call after calling constructor
    public String start(){

        //log
        logger.log(Level.ALL, "Start file creation in MDFS for filename: " + fileInfo.getFileName());

        //first decide candidate nodes
        String fetchTop = fetchTopologyAndChooseNodesFromGNS();
        if(fetchTop.equals("SUCCESS")){

            //then decide N, K values
            String NKVal = chooseN2K2();

            if(NKVal.equals("SUCCESS")) {

                //log
                logger.log(Level.ALL, "Number of blocks: " + blockCount);

                //check if its single block or multiple blocks
                if (blockCount > 1) {

                    //partition the file into blocks,
                    //and write the blocks in disk
                    if (partitionSmart()) {

                        isPartComplete = true;
                        String sendRet = sendBlocks();

                        if (sendRet.equals("SUCCESS")) {

                            //update to edgekeeper\
                            //returns SUCCESS or error message
                            return updateEdgeKeeper();

                        } else {

                            //log
                            logger.log(Level.DEBUG, "Failed to push data of file "+ file.getName() +" rsock...reason: " + sendRet);

                            return sendRet;
                        }
                    } else {
                        logger.log(Level.DEBUG, "Failed to push file data for file "+ file.getName() +" in rsock...reason: " + "File to block partition failed.");

                        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "\n");
                        return "File to block partition failed.";
                    }
                } else {

                    // Single block so we create a new file and copy all the bytes from main file to new file in mdfs directory and operate on that
                    ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__blk__0 (file)
                    File fileBlock = IOUtilities.createNewFile(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + MDFSFileInfo.getFileDirPath(file.getName(), fileID) + File.separator + MDFSFileInfo.getBlockName(file.getName(), (byte) 0));  //Isagor0!
                    try {
                        Files.copy(file, fileBlock);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return "Copying block from local drive failed";
                    }

                    //no need for partition but make this variable true
                    synchronized (fileInfo) {
                        isPartComplete = true;
                    }

                    //sending single block
                    String sendRet = sendBlocks();
                    if (sendRet.equals("SUCCESS")) {

                        //log
                        logger.log(Level.ALL, "File data has been pushed to the rsock.");
                        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "\n");


                        //update to edgekeeper
                        return updateEdgeKeeper();
                    } else {

                        logger.log(Level.ALL, "Failed to push all file data in rsock...reason: " + sendRet);

                        return sendRet;
                    }
                }
            }else{

                //log
                logger.log(Level.DEBUG, "Failed to push all file data in rsock...reason: " + NKVal);

                return NKVal;
            }
        }else{

            //log
            logger.log(Level.DEBUG, "Failed to push all file data in rsock...reason: " + fetchTop);

            return fetchTop;
        }
    }

    private String fetchTopologyAndChooseNodesFromGNS(){
        List<String> peerGUIDsListfromGNS = null;
        //get peer guids who are running mdfs from GNS
        try{
            //peerGUIDsListfromGNS = EKClient.getPeerGUIDs(EdgeKeeperConstants.EdgeKeeper_s, EdgeKeeperConstants.EdgeKeeper_s1);
            peerGUIDsListfromGNS = EKClient.getAllLocalGUID();
        }catch(Exception e ){
            //dont need to handle this error
        }

        if(peerGUIDsListfromGNS!=null){
            logger.log(Level.ALL, "fetched mdfs peers from android: " + peerGUIDsListfromGNS);
            chosenNodes = peerGUIDsListfromGNS;
            return "SUCCESS";
        }else{
            return "FAILURE";
        }
    }


    //takes a file and converts it into multiple blocks,
    //and writes the blocks in disk with the __blk__ tag.
    ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__blk__0 (file)
    private boolean partition(){

        boolean result = false;

        try {
            ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
            String outputDirPath = File.separator + edu.tamu.lenss.MDFS.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), fileID);  //Isagor0!

            byte[] fileBytes = IOUtilities.fileToByte(file);

            byte[][] blockBytes = new byte[blockCount][];

            int startIndex = 0;
            int endIndex = maxBlockSize;
            for (int i = 0; i < blockCount; i++) {

                //allocate space for ith block
                blockBytes[i] = new byte[(endIndex - startIndex + Integer.BYTES)];

                //add the number itself of bytes about to be copied
                ByteBuffer.wrap(blockBytes[i]).putInt(endIndex - startIndex);

                //copy the file data into block file
                System.arraycopy(fileBytes, startIndex, blockBytes[i], Integer.BYTES, (endIndex - startIndex));

                //update start and end index for next iteration
                startIndex = endIndex;
                endIndex = endIndex + maxBlockSize;
                if (endIndex > fileBytes.length) {
                    endIndex = fileBytes.length;
                }
            }

            //save the blockBytes into each files with appropriate naming
            for (int i = 0; i < blockCount; i++) {
                File blockFile = IOUtilities.byteToFile(blockBytes[i], AndroidIOUtils.getExternalFile(outputDirPath), (file.getName() + "__blk__" + i));

            }

            //log
            logger.log(Level.ALL, "Successfully partitioned file into blocks.");

            //coming here means all works succeeded without exception.
            result = true;

        }catch(Exception e ){
            logger.log(Level.ERROR, "Could not partition file " + file.getName() +" into blocks.");
        }
        return result;
    }

    public boolean partitionSmart(){
        boolean result = false;
        try{

            ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
            String outputDirPath = File.separator + edu.tamu.lenss.MDFS.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), fileID);  //Isagor0!

            int filesize = (int)file.length();
            int startIndex = 0;
            int endIndex = maxBlockSize;
            byte[] blockBytes;
            for (int i = 0; i < blockCount; i++) {

                //allocate space for ONE block
                blockBytes = new byte[Integer.BYTES + (int)(endIndex - startIndex)];

                //add the number itself of bytes about to be copied
                ByteBuffer.wrap(blockBytes).putInt((int)(endIndex - startIndex));

                //read one block amount of data
                IOUtilities.fileToByte(file, startIndex, endIndex, blockBytes, Integer.BYTES);

                //write the block as a file in disk
                File blockFile = IOUtilities.byteToFile(blockBytes, AndroidIOUtils.getExternalFile(outputDirPath), (file.getName() + "__blk__" + i));

                //update start and end index for next iteration
                startIndex = endIndex;
                endIndex = endIndex + maxBlockSize;
                if (endIndex > filesize) {
                    endIndex = filesize;
                }

            }

            //update boolean
            result = true;

        }catch(Exception e){
            e.printStackTrace();
            logger.log(Level.ERROR, "Could not partition file " + file.getName() + " into blocks.");
        }

        return result;
    }


    private synchronized String sendBlocks(){

        //check before proceeding
        synchronized(fileInfo){
            if(!isN2K2Chosen || !isPartComplete || isSending){
                return "Failed to send block.";
            }
        }

        //change isSending variable
        isSending = true;

        //create a queue
        List<MDFSBlockCreatorViaRsockNG> uploadQ = new ArrayList<>();

        //load the block directory
        ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
        final File fileDir = AndroidIOUtils.getExternalFile(edu.tamu.lenss.MDFS.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), fileID));   //Isagor0!

        //array of blocks
        final File[] blocks = fileDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File f) {
                return f.isFile()
                        && f.length()>0
                        && f.getName().contains(file.getName());
            }
        });

        //populate uploadQ
        String fName;
        for(File blockF : blocks){
            fName = blockF.getName();
            byte idx = Byte.parseByte(fName.substring((fName.lastIndexOf("_")+1)));   //idx = block number

            //add block in uploadQ
            uploadQ.add(new MDFSBlockCreatorViaRsockNG(blockF, filePathMDFS, fileInfo, idx, fileCreationReqUUID, chosenNodes, encryptKey, metadata));
        }

        //create a result variable
        boolean result = false;
        String blockResult = "";
        MDFSBlockCreatorViaRsockNG curBlock = null;
        for(int i=0; i< uploadQ.size(); i++){
            curBlock = uploadQ.get(i);
            blockResult = curBlock.start();
            if(blockResult.equals("SUCCESS")){

                //log
                logger.log(Level.ALL, "Block# " + curBlock.getBlockIdx() + " has been pushed to Rsock.");

                result = true;
                curBlock.deleteBlockFile();
            }else{
                //no point of sending other blocks if one block fails.
                result = false;
                break;
            }
        }

        if(result){return "SUCCESS";}
        else{
            logger.log(Level.DEBUG, "Failed to push block# " + curBlock.getBlockIdx() + " to rsock...reason: " + blockResult);
            return blockResult;
        }
    }

    // this function basically populates chosenNodes list with GUIDs.
    //chosenNodes list is used later for choosing n2 and k2 values.
    private String fetchTopologyAndChooseNodes(){

        //lists and sets
        List<String> peerGUIDsListfromGNS = null;
        Set<String> peerGUIDsSetfromOLSR = null;
        List<String> peerGUIDsListfromOLSR = null;

        //get peer guids who are running mdfs from GNS
        try{
            peerGUIDsListfromGNS = EKClient.getPeerGUIDs(EdgeKeeperConstants.EdgeKeeper_s, EdgeKeeperConstants.EdgeKeeper_s1);
        }catch(Exception e ){
            //dont need to handle this error
        }

        //get all nearby vertices from Topology.java from OLSR(rsockJavaAPI) and put it in a list
        try {
            peerGUIDsSetfromOLSR = Topology.getInstance(RSockConstants.intrfc_creation_appid).getVertices();
            peerGUIDsListfromOLSR = new ArrayList<String>(peerGUIDsSetfromOLSR);
        }catch(Exception e ){
            //dont need to handle this error
        }

        //only cross them if both type of list are not null and size 0
        if(peerGUIDsListfromGNS!=null && peerGUIDsListfromOLSR!=null && peerGUIDsListfromGNS.size()!=0 && peerGUIDsListfromOLSR.size()!=0) {

            //print MDFS peers size
            System.out.println("mdfs peers from GNS: " + peerGUIDsListfromGNS.size());
            for(String guid: peerGUIDsListfromGNS){
                System.out.println(guid + " ");
            }
            System.out.println();

            //print OLSR peers size
            System.out.println("mdfs peers from olsr : "  + peerGUIDsListfromOLSR.size());
            for(String guid: peerGUIDsListfromOLSR){
                System.out.println(guid + " ");
            }
            System.out.println();


            try {
                //cross the peerGUIDsListfromGNS and peerGUIDsListfromOLSR and get the common ones
                List<String> commonPeerGUIDs = new ArrayList<>();
                for (int i = 0; i < peerGUIDsListfromOLSR.size(); i++) {
                    if (peerGUIDsListfromGNS.contains(peerGUIDsListfromOLSR.get(i))) {
                        commonPeerGUIDs.add(peerGUIDsListfromOLSR.get(i));
                    }
                }

                //print common ones
                System.out.println("size of common guids: " + commonPeerGUIDs.size());

                //here
                //get the common ones
                chosenNodes = commonPeerGUIDs;

            }catch(Exception e ){
                //dont need to handle this error
            }
        }


        //log
        logger.log(Level.ALL, "Successfully fetched neighbor nodes from olsr and MDFS nodes from EdgeKeeper");

        //add myself if it is not already in it
        if(!chosenNodes.contains(EdgeKeeper.ownGUID)){chosenNodes.add(EdgeKeeper.ownGUID);}

        return "SUCCESS";
    }


    //this function selects n2, k2 values,
    //based on the size of the chosenNodes list.
    //n2 will be chosen equal to number of nodes in the list.
    //k2 will be chosen by the below equation.
    private String chooseN2K2(){

        //print
        System.out.print("debuggg chosen nodes: " );
        for(String node: chosenNodes){System.out.print(node + " , ");}

        //set n2 , k2
        byte n2; if(chosenNodes.size() >= Constants.MAX_N_VAL){ n2 = (byte)Constants.MAX_N_VAL;} else{ n2 = (byte)chosenNodes.size();}
        byte k2 = (byte) Math.round(n2 * encodingRatio);
        fileInfo.setFragmentsParms(n2, k2);
        this.metadata.setn2((int)n2);
        this.metadata.setk2((int)k2);
        if(n2 < 1 || k2 < 1){

            //log
            logger.log(Level.DEBUG, "Decided N or K value is invalid, " + "N: " + n2  + " K: " + k2 + ".");

            return "Decided N or K value is invalid, " + "N: " + n2  + " K: " + k2 + ".";
        }

        synchronized(fileInfo){
            isN2K2Chosen = true;
        }

        //log
        logger.log(Level.ALL, "Successfully choose N, K values : " + " N: " + n2 + " and K: " + k2);



        return "SUCCESS";
    }

    //this function sends the update to the edgekeeper
    //if it cannot connect to the edgekeeper, then it puts the data in DTNQueue
    private String updateEdgeKeeper() {

        //add information of all blocks/fragments that I(file creator) have(file creator has all fragments of all blocks)
        for(int i=0; i< blockCount; i++){
            for(int j=0; j<metadata.getn2(); j++){
                metadata.addInfo(EdgeKeeper.ownGUID, i, j);
            }
        }

        //send the metadata to the local edgeKeeper
        JSONObject repJSON = EKClient.putMetadata(metadata);

        //check reply
        if(repJSON!=null) {
            try {
                if (repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {
                    return "SUCCESS";
                } else {
                    return repJSON.getString(RequestTranslator.messageField);
                }
            } catch (JSONException e) {
                return "Json exception when sending metadata to edgekeeper.";
            }
        }else{
            //log
            logger.log(Level.ALL, "File has been created on mdfs but could not submit file metadata (could not connect to local EdgeKeeper).");
            MDFSFileCreatorViaRsockNG.logger.log(Level.DEBUG, "\n\n\n");

            //return
            return "File has been created on mdfs but could not submit file metadata (could not connect to local EdgeKeeper).";
        }

    }


}
