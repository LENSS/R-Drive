package edu.tamu.lenss.mdfs.ReedSolomon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
        System.out.println("tmpfile dirdirdir: " + tmpFile.getAbsolutePath() + "      "  + tmpFile.getName());
        IOUtilities.createNewFile(tmpFile);

        //cipher/encrypt the file
        nocipher();

        //check if the cipher succeeded
        if(encryptedByte==null){
            return null;
        }

        //now decode the blockFile into fragments
        return getFragmantsFromBlockFile();
    }

    private void cipher(){
        MDFSCipher myCipher = MDFSCipher.getInstance();
        if(myCipher.encrypt(clearFile.getAbsolutePath(), tmpFile.getAbsolutePath(), encryptKey)){
            encryptedByte = IOUtilities.fileToByte(tmpFile);
            tmpFile.delete();
        }
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

    private void nocipher(){

        encryptedByte = IOUtilities.fileToByte(new File(clearFile.getAbsolutePath()));

        //print without encrypt
        System.out.print("xxyyzz without encrypt length: " + encryptedByte.length);
        System.out.println();
        System.out.print("xxyyzz without encrypt first 30: ");
        for(int i=0; i< 30; i++){ System.out.print(encryptedByte[i] + ", "); }
        System.out.println();
        System.out.print("xxyyzz without encrypt last 30: ");
        for(int i = encryptedByte.length-31; i < encryptedByte.length; i++){ System.out.print(encryptedByte[i] + ", "); }
        System.out.println();

        //write on a file
        try {
            FileWriter writer = new FileWriter("/storage/emulated/0/MDFS/cipher.txt", true);
            for(int i=0; i< encryptedByte.length; i++){
                writer.write(Integer.toString(encryptedByte[i]) + "\n");
            }
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }




    private void cipherr(){
        MDFSCipher myCipher = MDFSCipher.getInstance();
        if(myCipher.encrypt(clearFile.getAbsolutePath(), tmpFile.getAbsolutePath(), encryptKey)){
            encryptedByte = IOUtilities.fileToByte(tmpFile);
            tmpFile.delete();
        }

        //print after encrypt
        System.out.print("xxyyzz afterrr encrypt length: " + encryptedByte.length);
        System.out.println();
        System.out.print("xxyyzz afterrr encrypt first 30: ");
        for(int i=0; i< 30; i++){ System.out.print(encryptedByte[i] + ", "); }
        System.out.println();
        System.out.print("xxyyzz afterrr encrypt last 30: ");
        for(int i = encryptedByte.length-31; i < encryptedByte.length; i++){ System.out.print(encryptedByte[i] + ", "); }
        System.out.println();
    }

    private void cipher1(){

        //read the file from the disk
        File blockFile = new File(clearFile.getAbsolutePath());
        byte[] unencryptedByte = IOUtilities.fileToByte(blockFile);

        //print before encrypt
        System.out.print("xxyyzz beforeee encrypt length: " + unencryptedByte.length);
        System.out.println();
        System.out.print("xxyyzz beforeee encrypt first 30: ");
        for(int i=0; i< 30; i++){ System.out.print(unencryptedByte[i] + ", "); }
        System.out.println();
        System.out.print("xxyyzz beforeee encrypt last 30: ");
        for(int i = unencryptedByte.length-31; i < unencryptedByte.length; i++){ System.out.print(unencryptedByte[i] + ", ") ; }
        System.out.println();

        //create cipher instance
        MDFSCipher myCipher = MDFSCipher.getInstance().getInstance();

        //get encrypted bytes
        encryptedByte = myCipher.encrypt(unencryptedByte, encryptKey);

        //print after encrypt
        System.out.print("xxyyzz afterrr encrypt length: " + encryptedByte.length);
        System.out.println();
        System.out.print("xxyyzz afterrr encrypt first 30: ");
        for(int i=0; i< 30; i++){ System.out.print(encryptedByte[i] + ", "); }
        System.out.println();
        System.out.print("xxyyzz afterrr encrypt last 30: ");
        for(int i = encryptedByte.length-31; i < encryptedByte.length; i++){ System.out.print(encryptedByte[i] + ", "); }
        System.out.println();
    }
}
