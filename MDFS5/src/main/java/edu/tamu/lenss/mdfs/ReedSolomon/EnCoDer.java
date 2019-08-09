package edu.tamu.lenss.mdfs.ReedSolomon;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.cipher.FragmentInfo;
import edu.tamu.lenss.mdfs.cipher.MDFSCipher;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class EnCoDer {
    private byte[] encryptKey;
    private byte N2;
    private byte K2;
    private File clearFile;
    private File tmpFile;
    private byte[] encryptedByte;  //contains only fileBytes
    private int PARITY_SHARDS;
    private static final int BYTES_IN_INT = 4;


    public EnCoDer(byte[] encryptKey, byte n2, byte k2, File file){
        this.encryptKey = encryptKey;
        this.N2 = n2;
        this.K2 = k2;
        this.clearFile = file;
        this.PARITY_SHARDS = n2 - k2;
    }

    public List<FragmentInfo> ifYouSmellWhatTheRockIsCooking(){
        //first write the file in cache directory
        tmpFile = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_CACHE + File.separator + "encrypt_" + clearFile.getName());
        IOUtilities.createNewFile(tmpFile);

        //cipher/encrypt the file
        cipher();

        //check if the cipher succeeded
        if(encryptedByte==null){
            return null;
        }

        //now decode the blockFile into fragments
        return getFragmantsFromBlockFile();
    }

    private List<FragmentInfo> getFragmantsFromBlockFile(){

        // Get the size of the input file.  (Files bigger that
        // Integer.MAX_VALUE will fail here!)
        int fileSize = (int) encryptedByte.length;

        // Figure out how big each shard will be.  The total size stored
        // will be the file size (8 bytes) plus the file.
        int storedSize = fileSize + BYTES_IN_INT;
        int shardSize = (storedSize + K2 - 1) / K2;

        // Create a buffer holding the file size, followed by
        // the contents of the file.
        int bufferSize = shardSize * K2;
        byte [] allBytes = new byte[bufferSize];
        ByteBuffer.wrap(allBytes).putInt(fileSize);

        int index = BYTES_IN_INT;
        for(int i=0; i<encryptedByte.length; i++){
            allBytes[index] = encryptedByte[i];
            index++;
        }

        // Make the buffers to hold the shards.
        byte [] [] shards = new byte [N2] [shardSize];

        // Fill in the data shards
        for (int i = 0; i < K2; i++) {
            System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
        }

        // Use Reed-Solomon to calculate the parity.
        ReedSolomon reedSolomon = ReedSolomon.create(K2, PARITY_SHARDS);
        reedSolomon.encodeParity(shards, 0, shardSize);

        //make FragmentInfo object
        List<FragmentInfo> fileFragments = new ArrayList<FragmentInfo>();

        byte type;
        for(int i=0; i < shards.length; i++){
            if(i < K2)
                type = FragmentInfo.DATA_TYPE;
            else
                type = FragmentInfo.CODING_TYPE;

            fileFragments.add(new FragmentInfo(clearFile.getName(), type, allBytes.length, shards[i], (byte)i, K2, N2, System.currentTimeMillis()));
        }

        return fileFragments;
    }


    private void cipher(){
        MDFSCipher myCipher = MDFSCipher.getInstance();
        if(myCipher.encrypt(clearFile.getAbsolutePath(), tmpFile.getAbsolutePath(), encryptKey)){
            encryptedByte = IOUtilities.fileToByte(tmpFile);
            tmpFile.delete();
        }
    }
}
