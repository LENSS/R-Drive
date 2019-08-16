package edu.tamu.lenss.mdfs;

import android.os.Environment;

import com.google.common.io.Files;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.CallableTask;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mp4process.SplitVideo;
import rsock.Topology;

import static java.lang.Thread.sleep;


public class MDFSFileCreatorViaRsockNG{

    private File file;              //the actual file
    private MDFSFileInfo fileInfo;  //file information
    private byte[] encryptKey;
    private int blockCount;
    private int maxBlockSize;
    private double encodingRatio;
    private boolean isN2K2Chosen = false;
    private boolean isPartComplete = false;
    private boolean isSending = false;
    List<String> chosenNodes;
    String[] permList;              //permList contains entries like WORLD, OWNER, GROUP:<group_name> or GUIDs
    FileMetadata metadata;          //metadata object for this file
    String clientID;                //the client who is making this request
    String filePathMDFS;            //virtual directory path in MDFS in which the file will be saved. if dir doesnt exist, it willbe created first
    String uniqueReqID;             //unique id for file creation req

    public MDFSFileCreatorViaRsockNG(
                File f,
                String filePathMDFS,
                int maxBlockSize,
                double encodingRatio,
                String[] permList,
                byte[] key,
                String clientID) {
        this.file = f;
        this.filePathMDFS = filePathMDFS;
        this.encodingRatio = encodingRatio;
        this.blockCount = (int)Math.ceil((double)file.length()/maxBlockSize);
        this.maxBlockSize = maxBlockSize;
        this.fileInfo = new MDFSFileInfo(file.getName(), file.lastModified());
        this.fileInfo.setFileSize(file.length());
        this.fileInfo.setNumberOfBlocks((byte)blockCount);
        this.uniqueReqID = UUID.randomUUID().toString().substring(0,12);
        this.permList = permList;
        this.encryptKey = key;
        this.clientID = clientID;
        this.chosenNodes = new ArrayList<>();
        this.metadata = new FileMetadata(EdgeKeeperConstants.FILE_CREATOR_METADATA_DEPOSIT_REQUEST,
                    EdgeKeeperConstants.getMyGroupName(),
                    GNS.ownGUID, GNS.ownGUID,
                    fileInfo.getCreatedTime(),
                    new String[1],
                    new Date().getTime(),
                    uniqueReqID,
                    fileInfo.getFileName(),
                    fileInfo.getFileSize(),
                    0000,
                    filePathMDFS ,
                    blockCount,
                    (byte)0,
                    (byte) 0);
    }


    public String start(){

        //first decide candidate nodes and choose N, K values
        if(fetchTopologyAndChooseNodes().equals("SUCCESS") && chooseN2K2().equals("SUCCESS")){
            //check if its single block or multiple blocks
            if(blockCount > 1){
                System.out.println("blockcount: " + blockCount);
                //partition the file
                if(partition1()){
                    isPartComplete = true;
                     String sendRet = sendBlocks();
                     if(sendRet.equals("SUCCESS")){
                        //update own directory and edgekeeper
                         return updateDirectory();
                     }else{
                         return sendRet;
                     }
                }else{
                    return "File to block partition failed.";
                }
            }else{
                // Single block so we create a new file and copy all the bytes from main file to new file in mdfs directory and operate on that
                ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__blk__0 (file)
                File fileBlock = IOUtilities.createNewFile(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + MDFSFileInfo.getFileDirPath(file.getName(), file.lastModified()) + File.separator + MDFSFileInfo.getBlockName(file.getName(), (byte)0));
                try {
                    Files.copy(file, fileBlock);
                } catch (IOException e) {
                    e.printStackTrace();
                    return "Copying block from local drive failed";
                }

                //no need for partition but make this variable true
                synchronized(fileInfo){
                    isPartComplete = true;
                }

                //do sending multiple blocks
                String sendRet = sendBlocks();
                if(sendRet.equals("SUCCESS")){
                    //update own directory and edgekeeepr
                    return updateDirectory();
                }else{
                    return sendRet;
                }
            }
        }else{
            //dont do anything here, errors have been handled here
        }

        return "File Creation Failed.";
    }


    //takes a file and converts it into multiple blocks.
    //blocks will be saved in /storage/emulated/0/MDFS/test1.jpg_0123/ directory with "__blk__" signature.
    private boolean partition1(){
        boolean result = false;

        ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
        String outputDirPath = File.separator + edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified());

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

        result = true;
        return result;
    }

    //divides a file into multiple blocks
    private boolean partition(){
        ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
        String outputDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified());
        SplitVideo splitVideo = new SplitVideo(file.getAbsolutePath(), outputDirPath, blockCount);

        return splitVideo.splitVideo();

    }

    private synchronized String sendBlocks(){

        //check before proceeding
        synchronized(fileInfo){
            if(!isN2K2Chosen || !isPartComplete || isSending){
                return "Unknown Error.";
            }
        }

        //change isSending variable
        isSending = true;

        //create a queue
        List<MDFSBlockCreatorViaRsockNG> uploadQ = new ArrayList<>();

        //load the block directory
        ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
        final File fileDir = AndroidIOUtils.getExternalFile(edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified()));

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
            uploadQ.add(new MDFSBlockCreatorViaRsockNG(blockF, filePathMDFS, fileInfo, idx, permList, uniqueReqID, chosenNodes, clientID, encryptKey));
        }

        //create a result list
        boolean result = false;
        String blockResult = "";
        MDFSBlockCreatorViaRsockNG curBlock;
        for(int i=0; i< uploadQ.size(); i++){
            curBlock = uploadQ.get(i);
            blockResult = curBlock.start();
            if(blockResult.equals("SUCCESS")){
                result = true;
                curBlock.deleteBlockFile();
            }else{
                //no point of sending other blocks if one block fails.
                result = false;
                break;
            }
        }

        if(result){return "SUCCESS";}
        else{return blockResult;}
    }

    // this function basically populates chosenNodes list with GUIDs.
    private String fetchTopologyAndChooseNodes(){

        if(permList.length==1 && permList[0].equals("WORLD")){

            //get peer guids who are running mdfs from GNS
            List<String> peerGUIDsListfromGNS = GNS.gnsServiceClient.getPeerGUIDs("MDFS", "default");
            if(peerGUIDsListfromGNS==null){ return "GNS Error! called getPeerGUIDs() and returned null.";}
            if(peerGUIDsListfromGNS.size()==0){ return "no other MDFS peer registered to GNS."; }

            //get all nearby vertices from Topology.java from rsockJavaAPI(OLSR) and put it in a list
            Set<String> peerGUIDsSetfromOLSR = Topology.getInstance(RSockConstants.intrfc_creation_appid).getVertices();
            if(peerGUIDsSetfromOLSR==null){return "Topology fetch from OLSR Error! called getNeighbors() and returned null.";}
            if(peerGUIDsSetfromOLSR.size()==0){return "No neighbors found from OLSR";}
            List<String> peerGUIDsListfromOLSR = new ArrayList<String>(peerGUIDsSetfromOLSR);

            //cross the peerGUIDsListfromGNS and peerGUIDsListfromOLSR and get the common ones
            List<String> commonPeerGUIDs = new ArrayList<>();
            for(int i = 0; i < peerGUIDsListfromOLSR.size(); i++){
                if(peerGUIDsListfromGNS.contains(peerGUIDsListfromOLSR.get(i))){
                    commonPeerGUIDs.add(peerGUIDsListfromOLSR.get(i));
                }
            }

            //make a map<guid, pathWeight> for all nodes from ownGUID to each commonPeerGUIDs
            HashMap<String, Double> peerGUIDsWithWeights = new HashMap<>();

            //call Dijkstra from ownGUID to each of the commonPeerGUIDs and populate peerGUIDsWithWeights map
            for(int i=0; i< commonPeerGUIDs.size(); i++){
                double pathWeight = Topology.getInstance(RSockConstants.intrfc_creation_appid).getShortestPathWeight(GNS.ownGUID, commonPeerGUIDs.get(i));
                peerGUIDsWithWeights.put(commonPeerGUIDs.get(i), pathWeight);
            }

            //sort the map by ascending order(less weight to more weight)
            Map<String, Double> peerGUIDsWithWeightsSorted = sortByComparator(peerGUIDsWithWeights,true );

            //get first MAX_N_VAL(or less) number of nodes in a list(ones with less weight)
            int count = 0;
            for (Map.Entry<String, Double> pair: peerGUIDsWithWeightsSorted.entrySet()) {
                this.chosenNodes.add(pair.getKey());
                count++;
                if(count>= Constants.MAX_N_VAL){break;}
            }
        }

        //add myself if it is not already in it
        if(!chosenNodes.contains(GNS.ownGUID)){chosenNodes.add(GNS.ownGUID);}

        return "SUCCESS";
    }


    //this function selects n2, k2 values
    private String chooseN2K2(){
        //print
        System.out.print("debuggg chosen nodes: " );
        for(String node: chosenNodes){System.out.print(node + " , ");}
        System.out.println();

        //set n2 , k2
        byte n2; if(chosenNodes.size() >= Constants.MAX_N_VAL){ n2 = (byte)Constants.MAX_N_VAL;} else{ n2 = (byte)chosenNodes.size();}
        byte k2 = (byte) Math.round(n2 * encodingRatio);
        fileInfo.setFragmentsParms(n2, k2);
        System.out.println("n2: " + n2 + "   k2: " + k2);
        this.metadata.setn2(n2);
        this.metadata.setk2(k2);
        if(n2 < 1 || k2 < 1){
            return "Decided N or K value is invalid.";
        }

        synchronized(fileInfo){
            isN2K2Chosen = true;
        }
        return "SUCCESS";
    }


    //utility function
    //takes a hashmap and returns sorted map by value
    //order = true, results in ascending order, false= descending order
    private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order) {
        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if (order) { return o1.getValue().compareTo(o2.getValue()); }
                else { return o2.getValue().compareTo(o1.getValue()); }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : list) { sortedMap.put(entry.getKey(), entry.getValue()); }
        return sortedMap;
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
            for(int j=0; j<metadata.n2; j++){
                metadata.addInfo(GNS.ownGUID, Integer.toString(i), Integer.toString(j));
            }
        }

        //add permList to the metadata
        metadata.setPermission(permList);  ///permList contains WORLD, OWNER, GROUP:<group_name> or GUIDs

        //create client connection and connect
        client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);
        boolean connected = client.connect();

        if(!connected){
            //if couldnt connect to EdgeKeeper, we put in DTN queue
            client.putInDTNQueue(metadata, 3);
            return "File was created on MDFS but could not submit metadata to edgekeeper due to connection failure.";
        }

        //set socket read timeout(unnecessary here)
        client.setSocketReadTimeout();

        //get json string
        String str= metadata.toBuffer(metadata);

        //make sendBuf
        ByteBuffer sendBuf = ByteBuffer.allocate(str.length());
        sendBuf.order(ByteOrder.LITTLE_ENDIAN);
        sendBuf.clear();

        //put data in sendBuf
        sendBuf.put(str.getBytes());
        sendBuf.flip();

        //send
        client.send(sendBuf);

        //sleep 1 second
        try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        //close connection
        client.close();

        return "SUCCESS";
    }


}
