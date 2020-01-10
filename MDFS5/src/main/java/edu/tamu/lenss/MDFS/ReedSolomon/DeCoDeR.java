package edu.tamu.lenss.MDFS.ReedSolomon;

import com.google.common.io.Files;

import org.apache.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import edu.tamu.lenss.MDFS.Commands.get.FileMerge;
import edu.tamu.lenss.MDFS.Commands.get.MDFSFileRetrieverViaRsock;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.Cipher.FragmentInfo;
import edu.tamu.lenss.MDFS.Cipher.MDFSCipher;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;


//this file takes multiple fragments and do the reverse erasure-coding thing to create the actual block.
//note: a shard and a fragment is the same thing.
//if K fragments(out of n) of a block is present, then you can create the n-k missing fragments.
public class DeCoDeR {

    private byte[] decryptKey;
    private byte N2;
    private byte K2;
    private List<FragmentInfo> fileFragments;
    private String decodedFilePath;
    private int PARITY_SHARDS;
    private byte[] encryptedByte;
    private static final int BYTES_IN_INT = 4;

    public DeCoDeR(byte[] encryptKey, byte n2, byte k2, List<FragmentInfo> fileFragments, String decodedFilePath){
        this.decryptKey = encryptKey;
        this.N2 = n2;
        this.K2 = k2;
        this.fileFragments = fileFragments;
        this.decodedFilePath = decodedFilePath;
        this.PARITY_SHARDS = N2 - K2;
    }

    public boolean ifYouSmellWhatTheRockIsCooking(){

        try {
            //create space for file fragments
            byte[][] shards = new byte[N2][];
            boolean[] shardPresent = new boolean[N2];
            int shardCount = 0;
            int shardSize = 0;

            //copy the file fragments into their designated array position
            for (int i = 0; i < fileFragments.size(); i++) {
                shardSize = fileFragments.get(i).getFragment().length;
                shards[fileFragments.get(i).getFragmentNumber()] = fileFragments.get(i).getFragment();
                shardPresent[fileFragments.get(i).getFragmentNumber()] = true;
                shardCount++;
            }

            //check if present shard count is equal or larger than K2
            if (shardCount < K2) {
                return false;
            }

            //populate other index of shard array by 0s
            for (int i = 0; i < N2; i++) {
                if (!shardPresent[i]) {
                    shards[i] = new byte[shardSize];
                }
            }

            // Use Reed-Solomon to fill in the missing shards
            ReedSolomon reedSolomon = ReedSolomon.create(K2, PARITY_SHARDS);
            reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);


            // Combine the data shards into one buffer for convenience.
            // (This is not efficient, but it is convenient.)
            byte[] allBytes = new byte[shardSize * K2];
            for (int i = 0; i < K2; i++) {
                System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
            }

            // Extract the file length
            //the first Integer.BYTES of first fragment contains the size
            int fileSize = ByteBuffer.wrap(allBytes).getInt();

            //copy all the bytes, except the first four bytes to encryptedBytes array
            encryptedByte = new byte[fileSize];
            System.arraycopy(allBytes, BYTES_IN_INT, encryptedByte, 0, fileSize);

            //check if the encryptedByte are null or something
            if (encryptedByte == null) {
                return false;
            }

            //then decipher/decrypt the encryptedBYte
            return javaCipher();

        }catch (Exception e){

            MDFSFileRetrieverViaRsock.logger.log(Level.ERROR, "Fragments to block decoding failed, ", e);
            return false;
        }
    }

    private boolean javaCipher(){
        try {
            File tmpEncryptFile = IOUtilities.byteToFile(encryptedByte, AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_CACHE), "decodercache_" + System.nanoTime());  //Isagor0!
            // Decrypt
            MDFSCipher myCipher = MDFSCipher.getInstance();
            boolean isSuccess = myCipher.decrypt(tmpEncryptFile.getAbsolutePath(), decodedFilePath, decryptKey);
            tmpEncryptFile.delete();
            return isSuccess;
        }catch (Exception e ){
            MDFSFileRetrieverViaRsock.logger.log(Level.ERROR, "Java cipher decryption failed.");
            return false;
        }
    }


    private boolean nocipher(){

        ///storage/emulated/0/MDFS/cache/decodercache_0123 (file)
        File tmpEncryptFile1 = IOUtilities.byteToFile(encryptedByte, AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_CACHE), "decodercache_" + System.nanoTime());  //Isagor0!
        File newFile = IOUtilities.createNewFile(decodedFilePath);
        try { Files.copy(tmpEncryptFile1, newFile); } catch (IOException e) { e.printStackTrace(); }
        tmpEncryptFile1.delete();
        return true;

    }

}
