package edu.tamu.lenss.MDFS.Commands.put;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.MDFS.Cipher.MDFSCipher;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
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
    private int maxBlockSize;
    private double encodingRatio;
    private List<String> chosenNodes;
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
        this.encodingRatio = Constants.K_N_RATIO;
        this.maxBlockSize = Constants.MAX_BLOCK_SIZE;
        if(this.maxBlockSize >= Integer.MAX_VALUE){ this.maxBlockSize = (int) Constants.DEFAULT_BLOCK_SIZE; }
        this.blockCount = (int)Math.ceil((double)this.file.length()/this.maxBlockSize);
        this.encryptKey = ServiceHelper.getInstance().getEncryptKey();
        this.filePathMDFS = filePathMDFS;
        this.fileID = UUID.randomUUID().toString().replaceAll("-", "");
        this.fileCreationReqUUID = UUID.randomUUID().toString();
        this.metadata = MDFSMetadata.createFileMetadata(this.fileCreationReqUUID, this.fileID, this.file.length(), EdgeKeeper.ownGUID, EdgeKeeper.ownGUID, this.filePathMDFS + "/" + this.file.getName(), Constants.metadataIsGlobal);
        this.uniqueReqID = "uniqueReqID";

        //first decide the candidate nodes
        //add ownGUID if not in the list already.
        chosenNodes = EKClient.getAllLocalGUID();
        if(chosenNodes==null){
            chosenNodes = new ArrayList<>();
            chosenNodes.add(EdgeKeeper.ownGUID);
        }

        //this selects n2, k2 values,
        //based on the size of the chosenNodes list.
        //n2 will be chosen equal to number of nodes in the list.
        //k2 will be chosen by the below equation.
        //set n2 , k2
        if(chosenNodes.size() >= Constants.MAX_N_VAL){ this.n2 = Constants.MAX_N_VAL;} else{ this.n2 = chosenNodes.size();}
        this.k2 = (int) Math.round(n2 * encodingRatio);

        //for testing only | delete me
        //n2 = 3; k2 = 2;

        this.metadata.setn2((int)n2);
        this.metadata.setk2((int)k2);
        if(n2 < 1 || k2 < 1){
            return "Decided N or K value is invalid, " + "N: " + n2  + " K: " + k2 + ".";
        }

        //check if single block or multiple block
        if (blockCount > 1) {

            //create file specific directory.
            ///storage/emulated/0/MDFS/test1.jpg_0123/ (directory)
            String outputDirPath = File.separator + edu.tamu.lenss.MDFS.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), fileID);  //Isagor0!

            //partition and send
            long filesize = file.length();
            long startIndex = 0;
            long endIndex = maxBlockSize;
            byte[] blockBytes;
            for (int i = 0; i < blockCount; i++) {

                //allocate space for ONE block
                blockBytes = new byte[(int)(endIndex - startIndex)];


                //read one block amount of data
                IOUtilities.fileToByte(file, startIndex, endIndex, blockBytes, 0);

                //handle each block one after another
                handleOneBlock(filename, filePathMDFS, fileID, n2, k2, blockBytes, filesize, i, uniqueReqID, metadata, encryptKey, chosenNodes);

                //update start and end index for next block iteration
                startIndex = endIndex;
                endIndex = endIndex + maxBlockSize;
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
        logger.log(Level.ALL, "File Creation Success! filename: " + filename + " blockCount: " + blockCount + " n2: " + n2 + " k2: " + k2);

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

        //handle each shard, aka fragment
        for(int j=0; j< shards.length; j++){

            //save the shard in own device.
            IOUtilities.byteToFile_FOS_write(shards[j], fragsDir , MDFSFileInfo.getFragName(filename, blockIdx, j));

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
                    true);


            //send the shard to the destinations
            for(String GUID: chosenNodes){
                sendFragment(GUID, oneFragment);

                //update metadata
                metadata.addInfo(GUID, blockIdx, j);
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

}
