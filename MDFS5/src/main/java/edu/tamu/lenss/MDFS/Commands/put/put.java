package edu.tamu.lenss.MDFS.Commands.put;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.lenss.MDFS.Cipher.MDFSCipher;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Model.Fragment;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileCreate;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.ReedSolomon.ReedSolomon;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;

//this is mdfs file creation process.
public class put {

    //variables
    private File file;                              //the actual file
    private String fileID;                          //file ID
    private byte[] encryptKey;                      //put_encryption key
    private int blockCount;
    private int blockSize;
    private MDFSMetadata metadata;                  //metadata object for this file
    private String filePathMDFS;                    //virtual directory path in MDFS in which the file will be saved. if dir doesnt exist, it willbe created first
    private String fileCreationReqUUID;             //unique id
    private int k2, n2;
    private String uniqueReqID;                     //unique req id for file creation job
    private static final int BYTES_IN_INT = 4;


    //logger
    public static Logger logger = Logger.getLogger(put.class);

    //public constructor
    public put(){};

    //takes parameters to file creation and does the job
    public String put(String filePathLocal, String filePathMDFS, String filename, String clientID) {


        //add slash filepathlocal does not have an ending slash
        if(filePathLocal.charAt(filePathLocal.length()-1)!='/'){
            filePathLocal = filePathLocal + "/";
        }

        //load the file first
        this.file = new File(filePathLocal + filename);

        //check if file exists
        if(!file.exists()){
            return "-put failed! File not found.";
        }

        //check if filesize is 0
        if(file.length()<=0){
            return "-put failed! File size is 0 byte.";
        }

        //check MAX filesize
        if(Constants.CHECK_MAX_FILE_SIZE_LIMIT) {

            if (file.length() >= Long.MAX_VALUE || file.length() >= Constants.MAX_FILE_SIZE) {
                return "-put failed! File too large! Max allowed size " + Constants.MAX_FILE_SIZE + " bytes and filesize " + file.length() + " bytes.";
            }
        }

        //if file is good, init variables
        this.blockSize = (Constants.BLOCK_SIZE_IN_MB * (1024*1024));
        this.blockCount = (int)Math.ceil((double)this.file.length()/this.blockSize);
        this.encryptKey = ServiceHelper.getInstance().getEncryptKey();
        this.filePathMDFS = filePathMDFS;
        this.fileID = UUID.randomUUID().toString().replaceAll("-", "");
        this.fileCreationReqUUID = UUID.randomUUID().toString();
        this.metadata = MDFSMetadata.createFileMetadata(this.fileCreationReqUUID, this.fileID, this.file.length(), EdgeKeeper.ownGUID, EdgeKeeper.ownGUID, this.filePathMDFS + "/" + this.file.getName(), Constants.metadataIsGlobal);
        this.uniqueReqID = "uniqueReqID";

        //create empty node of candidate nodes that will be passed into the alg
        List<NK.Node> candidateNodes = new ArrayList<>();

        //get user expected file availability time
        int FAT_int = Constants.FILE_AVAILABILITY_TIME;

        //variable to hold highest Battery Remaining Time in all Devices
        Double highest_BRT = 0.0;

        //get Wa
        Double Wa = Constants.WA_FOR_ALGORITHM;

        //fetch edge health status
        JSONObject edgeStatus = EKClient.getEdgeStatus();
        if(edgeStatus==null){return "Error: Failed to decide replica devices";}

        //get device status for all devices in the edge
        JSONObject allDeviceStatus=null;
        try {allDeviceStatus = edgeStatus.getJSONObject(RequestTranslator.deviceStatus);} catch (JSONException e) {e.printStackTrace();}
        if(allDeviceStatus==null){return "Error: Failed to decide replica devices";}

        //get all GUIDs which have device status availble
        Iterator<String> guids = allDeviceStatus.keys();

        while(guids.hasNext()) {
            String guid = guids.next();

            //get device status for this guid
            JSONObject deviceStatus = null;

            try { deviceStatus = allDeviceStatus.getJSONObject(guid); } catch (JSONException e) { e.printStackTrace(); }
            if(deviceStatus==null){continue;}

            try {
            } catch (Exception e) {
                e.printStackTrace();
            }


            //get battery percentage for this guid
            int battParc = -1;
            try {battParc = deviceStatus.getInt(EKUtils.batteryPercentage); } catch (JSONException e) {e.printStackTrace();}

            //get avail storage for this guid
            Long availStorage = new Long(-1);
            try {availStorage = deviceStatus.getLong(EKUtils.freeExternalMemory); } catch (JSONException e) {e.printStackTrace();}


            //make new Node object
            if(battParc!=-1 && availStorage!=-1) {

                //convert battery percentage into battery remaining time in mins
                Double BRT = new Double(battParc * 3);

                //save the highest Battery Remaining Time
                highest_BRT = Math.max(BRT, highest_BRT);

                //convert storage size from bytes into MB
                Double availStorageInMB = new Double(availStorage/new Long(1000000));

                //add this node to candidateNodes list
                candidateNodes.add(new NK.Node(availStorageInMB, BRT, guid, new Double(FAT_int)));

            }
        }


        //check if user expected File Availability Time is higher than highest Available Battery Remaining Time in any device
        //in that case user expected File Availability Time is shrunk down to the highest Available Battery Remaining Time
        Double FAT_double = null;
        if(FAT_int>highest_BRT){
            FAT_double = highest_BRT;
        }else{
            FAT_double = new Double(FAT_int);
        }

        //run the algorithm and get solution
        NK.Solution sol = NK.simple_n_k_combinations(candidateNodes, Wa, FAT_double, new Double(file.length()/1000));

        //create empty choden nodes
        List<String> chosenNodes = null;
        if(sol!=null && sol.CR>0.0){
            chosenNodes = sol.getTopNNodesGUIDs();
            this.n2 = sol.N;
            this.k2 = sol.K;
        }else{
            return "Not enough resource available!";
        }

        //set N2 and K2
        this.metadata.setn2((int)this.n2);
        this.metadata.setk2((int)this.k2);
        if(this.n2 < 1 || this.k2 < 1){
            return "Decided N or K value is invalid, " + "N: " + this.n2  + " K: " + this.k2 + ".";
        }

        //check if single block or multiple block
        if (blockCount > 1) {

            //create file specific directory.
            ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
            String outputDirPath = File.separator + edu.tamu.lenss.MDFS.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), fileID);  //Isagor0!

            //partition and send
            long filesize = file.length();
            long startIndex = 0;
            long endIndex = blockSize;
            byte[] blockBytes;
            for (int i = 0; i < blockCount; i++) {

                //allocate space for ONE block
                blockBytes = new byte[(int)(endIndex - startIndex)];


                //read one block amount of data
                IOUtilities.fileToByte(file, startIndex, endIndex, blockBytes, 0);

                //handle each block one after another
                handleOneBlock(filename, filePathMDFS, fileID, n2, k2, blockBytes, filesize, i, uniqueReqID, metadata, encryptKey,chosenNodes);
                //update start and end index for next block iteration
                startIndex = endIndex;
                endIndex = endIndex + blockSize;
                if (endIndex > filesize) {
                    endIndex = filesize;
                }

            }
        }else{

            //allocate only one block space
            byte[] blockBytes = new byte[(int)file.length()];
            IOUtilities.fileToByte(file, 0, file.length(), blockBytes, 0);


            //handle only one block
            handleOneBlock(filename, filePathMDFS, fileID, n2, k2, blockBytes, file.length(), 0, uniqueReqID, metadata, encryptKey, chosenNodes);
        }

        //update EdgeKeeper
        JSONObject repJSON = EKClient.putMetadata(metadata);

        //update final log
        logger.log(Level.ALL, "SUCCESS_ File Creation Success! filename: " + filename + " blockCount: " + blockCount + " n2: " + n2 + " k2: " + k2);

        return "File Creation Success!";
    }


    //put_encryption + erasure coding + sending + metadata update for one block only.
    public void handleOneBlock(String filename, String filePathMDFS, String fileID, int n2, int k2, byte[] blockBytes, long filesize, int blockIdx, String uniqueReqID, MDFSMetadata metadata, byte[] encryptKey, List<String> chosenNodes){

        //do put_encryption.
        //extra allocation of 1024 since due to put_encryption, data may expand.
        byte[] encryptedBlockBytes = new byte[BYTES_IN_INT + blockBytes.length + 1024];
        int encryptCount = MDFSCipher.getInstance().encryptX(blockBytes, 0, blockBytes.length, encryptKey, encryptedBlockBytes, BYTES_IN_INT);
        ByteBuffer.wrap(encryptedBlockBytes).putInt(encryptCount);

        //do erasure coding and get shards [][]
        byte [][] shards = erasureCodingEncode(encryptedBlockBytes, (BYTES_IN_INT + encryptCount), k2, n2);

        //create a directory where all fragments reside.
        ///storage/emulated/0/MDFS/test.jpg_0123/test.jpg_0/ (directory)
        File fragsDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(filename, fileID, blockIdx));
        if(!fragsDir.exists()){
            fragsDir.mkdirs();
        }

        //first copy over candidateNodes in a new list
        List<String> candNodes = new ArrayList<>(chosenNodes);

        //remove my GUID if present
        candNodes.remove(EdgeKeeper.ownGUID);

        //handle each shard, aka fragment
        for(int j=0; j< shards.length; j++){

            if(j==0) {
                //create a Fragment object
                Fragment fr = new Fragment(filename, filePathMDFS, shards[j], fileID, filesize, n2, k2, blockIdx, j, blockCount);

                //convert Fragment object into byteArray
                byte[] frArray = IOUtilities.objectToByteArray(fr);

                //save the frARrra, the first shard in own device.
                IOUtilities.byteToFile_FOS_write(frArray, fragsDir, MDFSFileInfo.getFragName(filename, blockIdx, j));

                //update metadata
                metadata.addInfo(EdgeKeeper.ownGUID, blockIdx, j);
            }else {

                //send the jth shard to (j-1)th device
                //wrap shards in MDFSFragmentForFileCreate class
                MDFSFragmentForFileCreate oneFragment = new MDFSFragmentForFileCreate(
                        filename,
                        filePathMDFS,
                        shards[j],
                        fileID,
                        filesize,
                        n2,
                        k2,
                        blockIdx,
                        j,
                        EdgeKeeper.ownGUID,
                        uniqueReqID,
                        blockCount,
                        true
                );


                //send the shard to the destination
                sendFragment(candNodes.get(j-1), oneFragment);

                //update metadata
                metadata.addInfo(candNodes.get(j-1), blockIdx, j);

            }

        }
    }




    //takes a MDFSFragmentForFileCreate object, converts it into bytearray, and sends it to the destination.
    //f GUID is ownGUID of this device, dont send it.
    public boolean sendFragment(String destGUID, MDFSFragmentForFileCreate oneFragment){

        long t9 = System.currentTimeMillis();

        //return value
        boolean result = false;

        //convert object to bytearray
        byte[] data = IOUtilities.objectToByteArray(oneFragment);

        if(!destGUID.equals(EdgeKeeper.ownGUID)){
            String uuid = UUID.randomUUID().toString().substring(0, 12);
            if(RSockConstants.RSOCK) {
                result = RSockConstants.intrfc_creation.send(uuid, data, data.length, "nothing", "nothing", destGUID);
            }else{
                result = true; //dummy true
            }
        }else{
            result = true;
        }


        return result;
    }


    //reed-solomon encoding
    public static byte[][] erasureCodingEncode(byte[] allBytes, int dataLength, int K2, int N2){

        //file size
        int fileSize = dataLength;

        // Figure out how big each shard will be.
        int storedSize = fileSize ;
        int shardSize = (storedSize + K2 - 1) / K2;

        // Make the buffers to hold the shards.
        byte [] [] shards = new byte [N2] [shardSize];

        // Fill in the data shards
        for (int i = 0; i < K2; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }

        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = ReedSolomon.create(K2, N2-K2);
        reedSolomon.encodeParity(shards, 0, shardSize);

        //return
        return shards;
    }

    public static List<String> getalllocalguids(){
        return EKClient.getAllLocalGUID();
    }
}