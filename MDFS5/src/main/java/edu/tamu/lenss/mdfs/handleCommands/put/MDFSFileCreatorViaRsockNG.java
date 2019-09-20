package edu.tamu.lenss.mdfs.handleCommands.put;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import rsock.Topology;

import static java.lang.Thread.sleep;


public class MDFSFileCreatorViaRsockNG{

    //log
    static Logger logger = Logger.getLogger(MDFSFileCreatorViaRsockNG.class);

    //variables
    private File file;                      //the actual file
    private MDFSFileInfo fileInfo;          //file information
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
    String uniqueReqID;                     //unique id


    //private default constructor
    private MDFSFileCreatorViaRsockNG(){}

    //public constructor
    public MDFSFileCreatorViaRsockNG(
                File f,
                String filePathMDFS,
                int maxBlockSize,
                double encodingRatio,
                byte[] key) {
        this.file = f;
        this.filePathMDFS = filePathMDFS;
        this.encodingRatio = encodingRatio;
        this.blockCount = (int)Math.ceil((double)file.length()/maxBlockSize);
        this.maxBlockSize = maxBlockSize;
        this.fileInfo = new MDFSFileInfo(file.getName(), file.lastModified());
        this.fileInfo.setFileSize(file.length());
        this.fileInfo.setNumberOfBlocks((byte)blockCount);
        this.uniqueReqID = UUID.randomUUID().toString();
        this.encryptKey = key;
        this.chosenNodes = new ArrayList<>();
        this.metadata = MDFSMetadata.createFileMetadata(uniqueReqID, fileInfo.getCreatedTime(), fileInfo.getFileSize(), EdgeKeeper.ownGUID, EdgeKeeper.ownGUID, filePathMDFS + "/" + fileInfo.getFileName(), Constants.isGlobal);
    }



    //start function to call after calling constructor
    public String start(){

        //log
        logger.log(Level.ALL, "Start file creation in MDFS for filename: " + fileInfo.getFileName());

        //first decide candidate nodes
        String fetchTop = fetchTopologyAndChooseNodes();
        if(fetchTop.equals("SUCCESS")){

            //then decide N, K values
            String NKVal = chooseN2K2();

            if(NKVal.equals("SUCCESS")) {

                //log
                logger.log(Level.ALL, "Number of blocks: " + blockCount);

                //check if its single block or multiple blocks
                if (blockCount > 1) {

                    //partition the file
                    if (partition()) {

                        isPartComplete = true;
                        String sendRet = sendBlocks();

                        if (sendRet.equals("SUCCESS")) {

                            //update own directory and edgekeeper\
                            //returns SUCCESS or error message
                            return updateDirectory();

                        } else {

                            //log
                            logger.log(Level.DEBUG, "Failed to push all file data in rsock...reason: " + sendRet);

                            return sendRet;
                        }
                    } else {
                        logger.log(Level.DEBUG, "Failed to push all file data in rsock...reason: " + "File to block partition failed.");
                        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "\n");

                        return "File to block partition failed.";
                    }
                } else {

                    // Single block so we create a new file and copy all the bytes from main file to new file in mdfs directory and operate on that
                    ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__blk__0 (file)
                    File fileBlock = IOUtilities.createNewFile(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + MDFSFileInfo.getFileDirPath(file.getName(), file.lastModified()) + File.separator + MDFSFileInfo.getBlockName(file.getName(), (byte) 0));  //Isagor0!
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


                        //update own directory and edgekeeper
                        return updateDirectory();
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


    //takes a file and converts it into multiple blocks.
    ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__blk__0 (file)
    private boolean partition(){
        boolean result = false;

        ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
        String outputDirPath = File.separator + edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified());  //Isagor0!

        byte[] fileBytes = IOUtilities.fileToByte(file);

        byte[][] blockBytes = new byte[blockCount][];

        int startIndex = 0;
        int endIndex = maxBlockSize;
        for(int i=0; i< blockCount; i++){

            //allocate space for ith block
            blockBytes[i] = new byte[(endIndex -startIndex + Integer.BYTES)];

            //add the number itself of bytes about to be copied
            ByteBuffer.wrap(blockBytes[i]).putInt(endIndex - startIndex);

            //copy the file data into block file
            System.arraycopy(fileBytes, startIndex, blockBytes[i], Integer.BYTES, (endIndex -startIndex));

            //update start and end index for next iteration
            startIndex = endIndex;
            endIndex = endIndex + maxBlockSize;
            if(endIndex>fileBytes.length){endIndex = fileBytes.length;}
        }

        //save the blockBytes into each files with appropriate naming
        for(int i=0; i< blockCount; i++){
            File blockFile = IOUtilities.byteToFile(blockBytes[i], AndroidIOUtils.getExternalFile(outputDirPath), (file.getName() + "__blk__" + i));
        }

        //log
        logger.log(Level.ALL, "Successfully partitioned file into blocks.");

        result = true;
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
        final File fileDir = AndroidIOUtils.getExternalFile(edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified()));   //Isagor0!

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

            //log
            logger.log(Level.ALL, "Starting to send block# " + idx);
            uploadQ.add(new MDFSBlockCreatorViaRsockNG(blockF, filePathMDFS, fileInfo, idx, uniqueReqID, chosenNodes, encryptKey, metadata));
        }

        //create a result list
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
            //if(peerGUIDsListfromGNS==null){ return "GNS Error! called getPeerGUIDs() and returned null.";}
            //if(peerGUIDsListfromGNS.size()==0){ return "no other MDFS peer registered to GNS."; }
        }catch(Exception e ){
            //dont need to handle this error
        }

        //get all nearby vertices from Topology.java from OLSR(rsockJavaAPI) and put it in a list
        try {
            peerGUIDsSetfromOLSR = Topology.getInstance(RSockConstants.intrfc_creation_appid).getVertices();
            //if(peerGUIDsSetfromOLSR==null){return "No neighbors found from OLSR.";}
            //if(peerGUIDsSetfromOLSR.size()==0){return "No neighbors found from OLSR.";}
            peerGUIDsListfromOLSR = new ArrayList<String>(peerGUIDsSetfromOLSR);
        }catch(Exception e ){
            //dont need to handle this error
        }

        //only cross them if both type of list are not null and size 0
        if(peerGUIDsListfromGNS!=null && peerGUIDsListfromOLSR!=null && peerGUIDsListfromGNS.size()!=0 && peerGUIDsListfromOLSR.size()!=0) {

            try {
                //cross the peerGUIDsListfromGNS and peerGUIDsListfromOLSR and get the common ones
                List<String> commonPeerGUIDs = new ArrayList<>();
                for (int i = 0; i < peerGUIDsListfromOLSR.size(); i++) {
                    if (peerGUIDsListfromGNS.contains(peerGUIDsListfromOLSR.get(i))) {
                        commonPeerGUIDs.add(peerGUIDsListfromOLSR.get(i));
                    }
                }

                //make a map<guid, pathWeight> for all nodes from ownGUID to each commonPeerGUIDs
                HashMap<String, Double> peerGUIDsWithWeights = new HashMap<>();

                //call Dijkstra from ownGUID to each of the commonPeerGUIDs and populate peerGUIDsWithWeights map
                for (int i = 0; i < commonPeerGUIDs.size(); i++) {
                    double pathWeight = Topology.getInstance(RSockConstants.intrfc_creation_appid).getShortestPathWeight(EdgeKeeper.ownGUID, commonPeerGUIDs.get(i));
                    peerGUIDsWithWeights.put(commonPeerGUIDs.get(i), pathWeight);
                }

                //sort the map by ascending order(less weight to more weight)
                Map<String, Double> peerGUIDsWithWeightsSorted = IOUtilities.sortByComparator(peerGUIDsWithWeights, true);

                //get first MAX_N_VAL(or less) number of nodes in a list(ones with less weight)
                int count = 0;
                for (Map.Entry<String, Double> pair : peerGUIDsWithWeightsSorted.entrySet()) {
                    this.chosenNodes.add(pair.getKey());
                    count++;
                    if (count >= Constants.MAX_N_VAL) {
                        break;
                    }
                }
            }catch(Exception e ){
                //dont need to handle this error
            }
        }


        //log
        logger.log(Level.ALL, "Successfully fetched neighbor nodes from olsr and MDFS nodesfrom EdgeKeeper");

        //add myself if it is not already in it
        if(!chosenNodes.contains(EdgeKeeper.ownGUID)){chosenNodes.add(EdgeKeeper.ownGUID);}

        return "SUCCESS";
    }

    private String fetchTopDummy(){

        chosenNodes.add("1BC657F8971A53E9BD90C285EB17C9080EC3EB8E");
        chosenNodes.add("8877417A2CBA0D19636B44702E7DB497B5834559");
        chosenNodes.add("05EE5FDB77AC19C7FBE5F92DD493B039BB4CD869");

        //log
        logger.log(Level.ALL, "Successfully fetched neighbor nodes from olsr and MDFS nodesfrom EdgeKeeper");

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
        logger.log(Level.ALL, "Successfully choose N, K values : " + " N: ");

        return "SUCCESS";
    }




    private String updateDirectory() {

        //set file creator
        ServiceHelper serviceHelper = ServiceHelper.getInstance();

        // Update my local directory
        serviceHelper.getDirectory().addFile(fileInfo);

        //now update file metadata to EdgeKeeper
        return updateEdgeKeeper();

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
