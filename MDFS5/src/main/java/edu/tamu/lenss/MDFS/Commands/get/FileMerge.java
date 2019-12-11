package edu.tamu.lenss.MDFS.Commands.get;

import android.os.Environment;

import com.google.common.io.Files;

import org.apache.log4j.Level;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.tamu.lenss.MDFS.Constants;
import edu.tamu.lenss.MDFS.ReedSolomon.DeCoDeR;
import edu.tamu.lenss.MDFS.Cipher.FragmentInfo;
import edu.tamu.lenss.MDFS.Handler.ServiceHelper;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSRsockBlockForFileRetrieve;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;
import edu.tamu.lenss.MDFS.Utils.Pair;

//this class is called by RsockReceiveForFileRetrieval.java class.
//this class is run in a thread to merge a file after enough fragments are available for each block.
//the passed mdfsrsockblock must have to be a ReplyFromOneClientToAnotherForOneFragment type.
public class FileMerge implements Runnable{


    MDFSRsockBlockForFileRetrieve mdfsrsockblock;

    //default private constructor
    private FileMerge(){}

    //only public constructor
    public FileMerge(MDFSRsockBlockForFileRetrieve mdfsrsockblock){
        this.mdfsrsockblock = mdfsrsockblock;
    }

    @Override
    public void run() {

        //log
        MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Starting to merge file for filaname " + mdfsrsockblock.fileName);

        //for each block decode blockFile
        for(int i=0; i< mdfsrsockblock.totalNumOfBlocks; i++){

            //get the fragments
            List<FragmentInfo> fragments = getStoredFragsOfABlock((byte)i);

            //decode and write the blockFile in disk
            if(fragments!=null) {

                decodeBlockFile(fragments, i);

            }else{

                //for some block, fragments couldnt be fetched form disk.
                //maybe it is due to a delete request for this file.

                //log and return
                MDFSFileRetrieverViaRsock.logger.log(Level.ERROR,"File " + mdfsrsockblock.fileName + " merge failed because for block#  " + i + " could not fetch any fragments from disk. ");
            }

        }

        //now merge all the blocks of this file
        if(mdfsrsockblock.totalNumOfBlocks==1){
            mergeSingleBlock();
        }else{
            mergeMultipleBlocksSmart();
        }

        if(!mdfsrsockblock.sameEdge){
            //TODO:
            //the file we merged is from a diff edge.
            //we need to keep record of this in SP that we have a file from diff edge.
            //so next time we dont fetch this file again.

        }

    }



    //get stored fragments for this block in this device.
    //never returns null, always returns a list that is either empty
    // or has elements in it.
    private List<FragmentInfo> getStoredFragsOfABlock(byte blockIdx){


        //create a list of fragmentInfo object to return
        List<FragmentInfo> blockFragments = new ArrayList<FragmentInfo>();


        // Check stored fragments
        ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/  (directory)
        File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, blockIdx));

        //check if its a directory
        if(blockDir.isDirectory()){

            //get all the files in the directory
            File[] files = blockDir.listFiles(new FileFilter(){
                @Override
                public boolean accept(File f) {

                    //filter ony the fragments
                    return ( f.isFile() && f.getName().contains(mdfsrsockblock.fileName + "__" + blockIdx + "__frag__") );
                }
            });

            //read each fragment, make fragmentInfo object and push it into the list
            for (File f : files) {
                FragmentInfo frag;
                frag = IOUtilities.readObjectFromFile(f, FragmentInfo.class);
                if(frag != null)
                    blockFragments.add(frag);
            }
        }

        return blockFragments;
    }

    //this function gets all fragments of a file from disk,
    //and writes the block as a file with "__blk__" tag.
    private void decodeBlockFile(List<FragmentInfo> blockFragments, int blockIdx){


        // Final Check. Make sure enough fragments are available
        if (blockFragments.size() < mdfsrsockblock.k2) {
            String s = blockFragments.size() + " block fragments are available locally.";

            //log and listener
            MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, " decryption failed for block# " + blockIdx + " of file " + mdfsrsockblock.fileName + ", insufficient fragments.");
                return;
        }

        //if decryptkey is null
        if (ServiceHelper.getInstance().getEncryptKey() == null) {
            //log and listener
            MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, " decryption failed for Block# " +  blockIdx + " decryption failed, no decryption key found.");
            return;
        }

        //if enough fragments available, decryptKey is valid, then decode and store the file
        //tmp0 = /storage/emulated/0/MDFS/test1.jpg__0123/ (directory)
        //tmp = /storage/emulated/0/MDFS/test1.jpg__0123/test2.jpg_0123__blk__0 (file)
        File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId));
        File tmp = IOUtilities.createNewFile(tmp0, MDFSFileInfo.getBlockName(mdfsrsockblock.fileName, (byte)blockIdx));

        //make decoder object
        DeCoDeR decoder = new DeCoDeR(ServiceHelper.getInstance().getEncryptKey(), mdfsrsockblock.n2, mdfsrsockblock.k2, blockFragments, tmp.getAbsolutePath());

        //check if decoding completed
        //takes bunch of file fragments and writes file block with _blk__ tag
        if (decoder.ifYouSmellWhatTheRockIsCooking()) {

            //log
            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Block# " + blockIdx +" Decryption Complete for filename " + mdfsrsockblock.fileName);

        } else {

            //log and listener
            MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Failed to merge fragments of block# " + blockIdx + " for filename " + mdfsrsockblock.fileName);
            return;

        }

    }

    //This function merges multiple blocks into a file and writes the file into disk.
    //this function loads all the blockFiles(with "__blk__" tag one by one and appends them into a file.
    //this fucntion first loads all the bytes of all blocks into a map and then merges them (DUMB).
    private void mergeMultipleBlocksDumb() {

        //check if the request has already been resolved
        if(!getUtils.resolvedRequests.contains(mdfsrsockblock.blockRetrieveReqUUID)) {

            //log
            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Merging multiple blocks for filename " + mdfsrsockblock.fileName);

            boolean mergeResult = false;

            //get all the block files from disk.
            ///storage/emulated/0/MDFS/test1.jpg_0123/
            File fileDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(mdfsrsockblock.fileName, mdfsrsockblock.fileId));  //Isagor0!
            File[] blockFiles = fileDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (file.isFile() && file.getName().contains(mdfsrsockblock.fileName + "__blk__")) {
                        return true;
                    }
                    return false;
                }
            });


            //create a map with blockName to byte[] mapping
            Map<String, byte[]> blockMap = new HashMap<>();

            //populate block
            for (int i = 0; i < blockFiles.length; i++) {

                //get the bytes of the block files
                byte[] blockBytes = IOUtilities.fileToByte(blockFiles[i]);

                //get the length of bytes which are actual data (a block file = size_of_data + data)
                int blockLength = ByteBuffer.wrap(blockBytes).getInt();

                //allocate the blockLength amount of size in each array
                byte[] blockData = new byte[blockLength];

                //copy data from blockBytes[] to blockData[]
                System.arraycopy(blockBytes, Integer.BYTES, blockData, 0, blockLength);

                //put blockData[] into map
                blockMap.put(blockFiles[i].getName(), blockData);
            }


            //create a new file and append bytes in it
            File file = IOUtilities.createNewFile(getDecryptedFilePath());
            file.setWritable(true);

            for (int i = 0; i < blockMap.size(); i++) {
                try {
                    FileOutputStream fos = new FileOutputStream(file, true);
                    fos.write(blockMap.get(mdfsrsockblock.fileName + "__blk__" + i));
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mergeResult = true;

            if (mergeResult) {

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Merging multiple blocks success!");
                deleteDecryptedBlocks();


                //update miscellaneousWorks list for notification
                Pair p = new Pair(Constants.NOTIFICATION, mdfsrsockblock.fileName);
                IOUtilities.miscellaneousWorks.add(p);

                //put blockRetrieveReqUUID of this get request into resolvedRequests list
                getUtils.resolvedRequests.add(mdfsrsockblock.blockRetrieveReqUUID);

            } else {

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Failed to merge multiple blocks.");
                return;
            }
        }

    }

    //This function merges multiple blocks into a file and writes the file into disk.
    //this function loads all the blockFiles(with "__blk__" tag one by one and appends them into a file.
    //this function takes each block one after another and appends into the file (SMART).
    private void mergeMultipleBlocksSmart() {

        //check if the request has already been resolved
        if(!getUtils.resolvedRequests.contains(mdfsrsockblock.blockRetrieveReqUUID)) {

            //log
            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Merging multiple blocks for filename " + mdfsrsockblock.fileName);

            boolean mergeResult = false;

            //get all the block files from disk.
            ///storage/emulated/0/MDFS/test1.jpg_0123/
            File fileDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(mdfsrsockblock.fileName, mdfsrsockblock.fileId));  //Isagor0!
            File[] blockFiles = fileDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    if (file.isFile() && file.getName().contains(mdfsrsockblock.fileName + "__blk__")) {
                        return true;
                    }
                    return false;
                }
            });


            //create a new file and append bytes in it
            File file = IOUtilities.createNewFile(getDecryptedFilePath());
            file.setWritable(true);

            //check if file was created
            if(file==null){
                MDFSFileRetrieverViaRsock.logger.log(Level.ERROR, "All blocks have been retrieved and decrypted, but, Could not create file " + mdfsrsockblock.fileName + " and hence could not write block bytes into the file.");
                deleteDecryptedBlocks();
                return;
            }

            //populate file with bytes of block
            int startIndex = 0;
            for (int i = 0; i < blockFiles.length; i++) {

                //get the bytes of the block files
                byte[] blockBytes = IOUtilities.fileToByte(blockFiles[i]);

                //get the length of bytes which are actual data (a block file = size_of_data + data)
                int blockLength = ByteBuffer.wrap(blockBytes).getInt();

                //appends block bytes into file
                IOUtilities.byteToFile(blockBytes, Integer.BYTES, blockLength, file, startIndex);

                //increment startIndex
                startIndex = startIndex + (blockLength);

            }

            mergeResult = true;

            if (mergeResult) {

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Merging multiple blocks success!");
                deleteDecryptedBlocks();

                //update miscellaneousWorks list for notification
                Pair p = new Pair(Constants.NOTIFICATION, mdfsrsockblock.fileName);
                IOUtilities.miscellaneousWorks.add(p);

                //put blockRetrieveReqUUID of this get request into resolvedRequests list
                getUtils.resolvedRequests.add(mdfsrsockblock.blockRetrieveReqUUID);

            } else {

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Failed to merge multiple blocks.");
                return;
            }
        }

    }



    private void mergeSingleBlock(){

        //check if this get request has already been resolved
        if(!getUtils.resolvedRequests.contains(mdfsrsockblock.blockRetrieveReqUUID)) {

            //log
            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Merging single block.");

            // move block to the decrypted directory and rename
            File from = AndroidIOUtils.getExternalFile(MDFSFileInfo.
                    getFileDirPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId) + File.separator + MDFSFileInfo.getBlockName(mdfsrsockblock.fileName, (byte) 0));  //Isagor0!
            File to = IOUtilities.createNewFile(getDecryptedFilePath());


            //update miscellaneousWorks list for notification
            Pair p = new Pair(Constants.NOTIFICATION, mdfsrsockblock.fileName);
            IOUtilities.miscellaneousWorks.add(p);


            try {
                Files.move(from, to);
                getUtils.resolvedRequests.add(mdfsrsockblock.blockRetrieveReqUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    //modifies decrypted path
    private String getDecryptedFilePath(){  //Isagor0!

        //first set the path where the file ought to be saved
        //replace the "/storage/emulated/0" part from user inputted localDir since it will be added again.
        Constants.ANDROID_DIR_DECRYPTED = mdfsrsockblock.localDir.replace("/storage/emulated/0/", "/");

        ///storage/emulated/0/decrypted/test1.jpg
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Constants.ANDROID_DIR_DECRYPTED
                + File.separator + mdfsrsockblock.fileName;
    }



    //goes into /storage/emulated/0/MDFS/test1.jpg_0123/ directory and loads all the files.
    //all the files consists of both block directories(like "test1.jpg_0123_0/" that contains fragments)
    // and the block file (with "__blk__" in name) itself.
    //this function only deletes the block files and doesnt affect the block directories.
    private void deleteDecryptedBlocks(){
        File fileDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(mdfsrsockblock.fileName, mdfsrsockblock.fileId));  //Isagor0!
        File[] blockFiles = fileDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                if(file.isFile() && file.getName().contains(mdfsrsockblock.fileName+ "__blk__")) {
                    return true;
                }
                return false;
            }
        });
        for(File f : blockFiles)
            f.delete();
    }
}







