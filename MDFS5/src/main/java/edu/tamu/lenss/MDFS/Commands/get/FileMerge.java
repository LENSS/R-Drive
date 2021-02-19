package edu.tamu.lenss.MDFS.Commands.get;

import android.os.Environment;

import org.apache.log4j.Level;

import java.io.File;
import java.io.FileFilter;
import java.nio.ByteBuffer;

import edu.tamu.lenss.MDFS.Cipher.MDFSCipher;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileRetrieve;
import edu.tamu.lenss.MDFS.ReedSolomon.ReedSolomon;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;

//this class is called by RsockReceiveForFileRetrieval.java class.
//this class is run in a thread to merge a file after enough fragments are available for each block.
//this class assumes that enough fragments for all blocks are available so it tries decode them.
//so, before using this class, developer is suggested to check for enough fragments presence first.
//check getUtils class for helper functions.
public class FileMerge implements Runnable{

    MDFSFragmentForFileRetrieve mdfsfrag;
    public static int blockidx;
    private static final int BYTES_IN_INT = 4;


    //default private constructor
    private FileMerge(){}

    //only public constructor
    public FileMerge(MDFSFragmentForFileRetrieve mdfsfrag){
        this.mdfsfrag = mdfsfrag;
    }

    @Override
    public void run() {

        //make the resultant file
        //create a new file and append bytes in it
        File outputFile = IOUtilities.createNewFile(getDecryptedFilePath());
        outputFile.setWritable(true);

        //variables
        byte[] blockBytes;
        byte[] encryptedBlockBytes;
        File[] fragments;
        long totalBytesWritten = 0;

        //for each block decode blockFile
        for(int blockIdx=0; blockIdx< mdfsfrag.totalNumOfBlocks; blockIdx++){

            //hack!
            blockidx = blockIdx;

            // get stored fragments for each block
            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg__0/  (directory)
            File fragDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(mdfsfrag.fileName, mdfsfrag.fileId, blockIdx));
            fragments = fragDir.listFiles(new FileFilter(){
                @Override
                public boolean accept(File f) {

                    //filter
                    return ( f.isFile() && f.getName().contains(mdfsfrag.fileName + "__" + blockidx + "__frag__") );
                }
            });

            //decode from fragments/shards to block as bytearray
            encryptedBlockBytes = decodeBlockFile(fragments, blockIdx, mdfsfrag.n2, mdfsfrag.k2);

            //decrypt the bytearray
            blockBytes = new byte[encryptedBlockBytes.length];
            int encryptCount = ByteBuffer.wrap(encryptedBlockBytes).getInt();
            int decryptCount = MDFSCipher.getInstance().decryptX(encryptedBlockBytes, BYTES_IN_INT, encryptCount , ServiceHelper.getInstance().getEncryptKey(),blockBytes, 0);

            //append the bytes to the resultant file
            IOUtilities.byteToFile_RAF_append(blockBytes, 0, decryptCount, outputFile, totalBytesWritten);
            //IOUtilities.byteToFile_FC_append(blockBytes, 0, decryptCount, outputFile);
            //IOUtilities.byteToFile_BOS_append(blockBytes, 0, decryptCount, outputFile, false);
            totalBytesWritten = totalBytesWritten + decryptCount;

        }

        MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "File Retrieval Success! filename: " + mdfsfrag.fileName);

    }

    //takes a list of shards as bytearrays, uses erasure coding and
    // returns the decoded bytearray.
    private byte[] decodeBlockFile(File[] blockFragments, int blockidx, int N2, int K2){

        //create space for file fragments
        byte[][] shards = new byte[N2][];
        boolean[] shardPresent = new boolean[N2];
        int shardCount = 0;
        int shardSize = 0;

        for(File f: blockFragments){

            //get bytes from fragments
            byte[] fragBytes = IOUtilities.fileToByte(f);

            //get fragment number
            int fragmentIdx = getUtils.parseFragNum(f.getName());

            //populate existing shard indexes with data
            shardSize = fragBytes.length;
            shards[fragmentIdx] = fragBytes;
            shardPresent[fragmentIdx] = true;
            shardCount++;

        }

        //populate other index of shard arrays by 0s
        for (int i = 0; i < N2; i++) {
            if (!shardPresent[i]) {
                shards[i] = new byte[shardSize];
            }
        }

        // Use Reed-Solomon to fill in the missing shards
        ReedSolomon reedSolomon = ReedSolomon.create(K2, N2-K2);
        reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

        // Combine the data shards into one buffer for convenience
        byte[] allBytes = new byte[shardSize * K2];
        for (int i = 0; i < K2; i++) {
            System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
        }

        return allBytes;
    }

    //modifies decrypted path
    private String getDecryptedFilePath(){  //Isagor0!
        return mdfsfrag.localDir + File.separator + mdfsfrag.fileName;
    }



}







