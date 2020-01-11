package edu.tamu.lenss.MDFS.ReedSolomon;

import org.apache.log4j.Level;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


import edu.tamu.lenss.MDFS.Commands.get.MDFSFileRetrieverViaRsock;
import edu.tamu.lenss.MDFS.Commands.put.MDFSFileCreatorViaRsockNG;
import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.Cipher.FragmentInfo;
import edu.tamu.lenss.MDFS.Cipher.MDFSCipher;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;


//This class takes a file and do erasure coding and breaks into n shards.
public class EnCoDer {
    private byte[] encryptKey;
    private int N2;
    private int K2;
    private File clearFile;
    private File tmpFile;
    private byte[] encryptedByte;  //contains only fileBytes
    private int PARITY_SHARDS;
    private static final int BYTES_IN_INT = 4;


    public EnCoDer(byte[] encryptKey, int n2, int k2, File file){
        this.encryptKey = encryptKey;
        this.N2 = n2;
        this.K2 = k2;
        this.clearFile = file;
        this.PARITY_SHARDS = n2 - k2;
    }

    public List<FragmentInfo> ifYouSmellWhatTheRockIsCooking(){

        //cipher/encrypt the file
        javaCipher();

        //check if the cipher succeeded
        if(encryptedByte==null){
            return null;
        }

        //now decode the blockFile into fragments
        return getFragmantsFromBlockFile();
    }

    private void javaCipher(){

        try {
            //first write a temporary file in cache directory
            tmpFile = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_CACHE + File.separator + "encrypt_" + clearFile.getName());  //Isagor0!
            IOUtilities.createNewFile(tmpFile);


            MDFSCipher myCipher = MDFSCipher.getInstance();
            if (myCipher.encrypt(clearFile.getAbsolutePath(), tmpFile.getAbsolutePath(), encryptKey)) {
                encryptedByte = IOUtilities.fileToByte(tmpFile);
                tmpFile.delete();
            }
        }catch (Exception e){
            MDFSFileCreatorViaRsockNG.logger.log(Level.ERROR, "Java cipher encryption failed.");
            encryptedByte = null;
        }
    }


    private List<FragmentInfo> getFragmantsFromBlockFile(){

        // Get the size of the input file.  (Files bigger that
        // Integer.MAX_VALUE will fail here!)
        int fileSize = (int) encryptedByte.length;

        // Figure out how big each shard will be.  The total size stored
        // will be the file size (4 bytes) plus the file.
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

            fileFragments.add(new FragmentInfo(clearFile.getName(), type, allBytes.length, shards[i], i, K2, N2, System.currentTimeMillis()));
        }

        return fileFragments;
    }

    private void nocipher(){
        encryptedByte = IOUtilities.fileToByte(new File(clearFile.getAbsolutePath()));
    }

}
