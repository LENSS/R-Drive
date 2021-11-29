package edu.tamu.lenss.MDFS.Commands.get;

import androidx.annotation.FloatRange;

import org.apache.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.tamu.lenss.MDFS.Model.Fragment;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileCreate;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileRetrieve;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;

import static edu.tamu.lenss.MDFS.Constants.ANDROID_DIR_ROOT;

//this class contains only static functions
public class getUtils {

    //contains uuids for all the resolved get requests
    static List<String> resolvedRequests = new ArrayList<>();

    //this function fetches missing fragment number for each block from disk.
    //all missing fragment indexes are marked 1, all available fragments are marked 0.
    //the boolean value is true when the function has executed successfully.
    //the boolean value if false, when the function execution has failed for some reason(maybe file read).
    public static boolean getMissingFragmentsOfABlockFromDisk(int[][] missingBlocksAndFrags, String filename, String fileID, int N2, int blockNumber){

        try {
            //pull the directory of the block
            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg__0/ (directory)
            File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(filename, fileID, blockNumber));

            //check if block directory exists
            if (blockDir.exists() && blockDir.isDirectory() && blockDir.getName().contains(Integer.toString(blockNumber))) {

                //get all the fragments of this block directory and parse
                ///storage/emulated/0/MDFS/test1.jpg__0123/test1.jpg__0/test1.jpg__0__frag__0 (file)
                File[] fragFiles = blockDir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File file) {
                        if (file.isFile() && file.getName().contains(filename) && file.getName().contains("__frag__")) {
                            return true;
                        }
                        return false;
                    }

                });

                //array for file frags that I have.
                //mark the fragments i have into 0.
                int[] array = new int[N2];
                Arrays.fill(array, 1);
                for (int i = 0; i < fragFiles.length; i++) {
                    array[(int) parseFragNum(fragFiles[i].getName())] = 0;
                }

                //add the fragment index into the main list
                missingBlocksAndFrags[blockNumber] = array;

            } else {

                //block directory doesnt exist,
                //that means all the N2 fragments,
                //are missing for this block.
                //create a new array, mark all entry to 1,
                //and init to block.
                int[] array = new int[N2];
                Arrays.fill(array, 1);

                //add the fragment index into the main list
                missingBlocksAndFrags[blockNumber] = array;

            }

            //coming here means all the above steps succeeded,
            // so we change the variable
            return true;

        }catch(Exception e){
            e.printStackTrace();
            return false;
            //logger:
        }

    }

    //checks if for each block, K frags are available and no fragment needs to be retrieved.
    //returns true if no blocks needs retrieval, false otherwise.
    public static boolean checkEnoughFragsAvailableForWholeFile(int[][] missingBlocksAndFrags, int K2){

        for(int i=0; i< missingBlocksAndFrags.length; i++){

            //count number of frags for this block
            int k= K2;
            for(int j=0; j< missingBlocksAndFrags[i].length; j++){
                if(missingBlocksAndFrags[i][j]==0){k--;}
            }

            //check if for this block k frags are not present
            if(k>0){

                //for this block, K frags are not available.
                //that means this file is not ready for merge.
                return false;
            }
        }

        //for any block, no fragment needs retrieval.
        return true;
    }


    //get a Map of all MDFS files have enough fragments available locally so that they can be decoded right away
    public static Map<String, List<String>> getAllLocallyAvailableFiles(){
        Map<String, List<String>> locallyAvailableFiles = new HashMap<>();

        try {
            String localStorageDir = File.separator + "sdcard" + File.separator + ANDROID_DIR_ROOT + File.separator;
            File localStorage = new File(localStorageDir);
            if(!localStorage.exists()){localStorage.mkdir();}
            File[] filesInLocalStorage = localStorage.listFiles();

            //iterate over each folders in /sdcard/MDFS/ directory in Android
            for (File f : filesInLocalStorage) {

                String fname = f.getName().substring(0, f.getName().indexOf('_'));
                List<String> l = locallyAvailableFiles.getOrDefault(fname, new ArrayList<>());

                //we are inside the block directory of this file
                //check if there is any block available for this file (load the first block)
                File blockDir = new File(localStorageDir + f.getName());
                File[] blocks = blockDir.listFiles();
                if (blocks.length == 0) {
                    l.add(null);
                } else {
                    //load the fragmentDir
                    File fragDir = new File(localStorageDir + f.getName() + File.separator + blocks[0].getName() + File.separator);
                    File[] fragments = fragDir.listFiles();
                    if (fragments.length == 0) {
                        l.add(null);
                    } else {
                        //load the fragment (as Fragment object) and convert from bytes to object
                        File frFile = new File(localStorageDir + f.getName() + File.separator + blocks[0].getName() + File.separator + fragments[0].getName());

                        //get byteArray from this file
                        byte[] byteArray = IOUtilities.fileToByte(frFile);

                        //convert bytesArray into Fragment object
                        Fragment fr = IOUtilities.bytesToObject(byteArray, Fragment.class);

                        int[][] missingBlocksAndFrags = new int[fr.totalNumOfBlocks][fr.n2];

                        boolean succeeded = false;
                        for (int i = 0; i < fr.totalNumOfBlocks; i++) {
                            succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, fr.fileName, fr.fileID, fr.n2, i);
                        }

                        if (succeeded) {
                            if (getUtils.checkEnoughFragsAvailableForWholeFile(missingBlocksAndFrags, fr.k2)) {
                                l.add(fr.fileID + "__" + fr.n2 + "__" + fr.k2 + "__" + fr.totalNumOfBlocks);
                            } else {
                                l.add(null);
                            }
                        } else {
                            l.add(null);
                        }

                    }

                }

                locallyAvailableFiles.put(fname, l);

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return locallyAvailableFiles;
    }

    //get block number from a fragment name
    public static int parseBlockNum(String fName){
        String str = fName.substring(0, fName.lastIndexOf("__frag__"));
        str = str.substring(str.lastIndexOf("_")+1);
        return Integer.parseInt(str.trim());
    }

    //get fragment number from a fragment name
    public static int parseFragNum(String fName) {
        return Integer.parseInt(fName.substring(fName.lastIndexOf("_") + 1).trim());
    }


    //prints the missing fragments of each block in a 2d matrix
    //1 means: fragment missing, 0 means fragment exists in this devices disk.
    public static void print2DArray(int[][] missingBlocksAndFrags, String message){

        System.out.println(message);
        for(int i=0; i< missingBlocksAndFrags.length; i++){
            for(int j=0; j< missingBlocksAndFrags[i].length; j++){
                System.out.print(missingBlocksAndFrags[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println();
        System.out.println();
    }

    //takes a mdfsfragForFileRetrieve and serves a RequestFromOneClientToAnotherForOneFragment request.
    //this function fetches the particular fragment of particular block of a file, and sends it back to the sourceGUID.
    //the functions flips the source and destination.
    //if the file directory, blockDirectory, or fragment doesnt exist, then nothing is sent.
    public static void justDoit(MDFSFragmentForFileRetrieve mdfsfrag){

        get.logger.log(Level.ALL, "received fragment request from node " + mdfsfrag.srcGUID + " for fragment# " + mdfsfrag.fragmentIndex + " of block# " + mdfsfrag.blockIdx + " of filename " + mdfsfrag.fileName);

        //get the file fragment from my disk
        //if the file directory doesnt exist, tmp0 size will be 0.
        File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(mdfsfrag.fileName, mdfsfrag.fileId, mdfsfrag.blockIdx, mdfsfrag.fragmentIndex));

        //if fragment was loaded from disk
        if (tmp0!=null && tmp0.exists() && tmp0.isFile() && tmp0.length() > 0) {

            //convert file tmp0 into byteArray
            byte[] byteArray = IOUtilities.fileToByte(tmp0);

            //convert bytesArray into Fragment object
            Fragment fr = IOUtilities.bytesToObject(byteArray, Fragment.class);

            //now, change mdfsfrag into a ReplyFromOneClientToAnotherForOneFragment object
            mdfsfrag.flipIntoReply(fr.fileFrag);

            //convert mdfsfrag object into bytearray and do send
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream ooos = null;
            try {
                ooos = new ObjectOutputStream(bos);
                ooos.writeObject(mdfsfrag);
                ooos.flush();
                byte[] data = bos.toByteArray();

                //send the object over rsock and expect no reply
                if(RSockConstants.RSOCK) {
                    String uuid = UUID.randomUUID().toString().substring(0, 12);
                    RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", mdfsfrag.destGUID);
                }

                //log
                get.logger.log(Level.ALL, "CASE #1: resolved fragment request from node " + mdfsfrag.destGUID + " for fragment# " + mdfsfrag.fragmentIndex + " of block# " + mdfsfrag.blockIdx + " of filename " + mdfsfrag.fileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{

            //log
            get.logger.log(Level.DEBUG, "Could not serve fragment request from node " + mdfsfrag.srcGUID + " for fragment# " + mdfsfrag.fragmentIndex + " of block# " + mdfsfrag.blockIdx + " of filename " + mdfsfrag.fileName + " due to file directory no longer exists in storage.");
        }
    }

}
