package edu.tamu.lenss.MDFS.Commands.get;

import org.apache.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSRsockBlockForFileRetrieve;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.RSock.network.RsockReceiveForFileRetrieval;
import edu.tamu.lenss.MDFS.Utils.AndroidIOUtils;


//this class contains only static functions
public class getUtils {

    //contains uuids for all the resolved uuids
    static List<String> resolvedRequests = new ArrayList<>();

    //this function fetches missing fragment number for each block from disk.
    //all missing fragment indexes are marked 1, all available fragments are marked 0.
    //the boolean value is true when the function has executed successfully.
    //the boolean value if false, when the function execution has failed for some reason(maybe file read).
    public static boolean getMissingFragmentsOfABlockFromDisk(int[][] missingBlocksAndFrags, String filename, String fileID, int N2, int blockNumber){

        try {
            //pull the directory of the block
            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg__0/ (directory)
            File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(filename, fileID, (byte) blockNumber));

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
    public static boolean checkEnoughFragsAvailable(int[][] missingBlocksAndFrags, int K2){

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



    //get block number from a fragment name
    private byte parseBlockNum(String fName){
        String str = fName.substring(0, fName.lastIndexOf("__frag__"));
        str = str.substring(str.lastIndexOf("_")+1);
        return Byte.parseByte(str.trim());
    }

    //get fragment number from a fragment name
    private static byte parseFragNum(String fName) {
        return Byte.parseByte(fName.substring(fName.lastIndexOf("_") + 1).trim());
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

    //takes a MDFSRsockBlockForFileRetrieve and serves a RequestFromOneClientToAnotherForOneFragment request.
    //this function fetched the particular fragment of particular block of a file, and sends it back to the sourceGUID.
    //the functions flips the source and destination.
    //if the file directory, blockDirectory, or fragment doesnt exist, then nothing is sent.
    public static void justDoit(MDFSRsockBlockForFileRetrieve mdfsrsockblock, String replyEndpoint){

        RsockReceiveForFileRetrieval.logger.log(Level.ALL, "received fragment request from node " + mdfsrsockblock.srcGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName);

        //get the file fragment from my disk
        //if the file directory doesnt exist, tmp0 size will be 0.
        File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(mdfsrsockblock.fileName, mdfsrsockblock.fileId, mdfsrsockblock.blockIdx, mdfsrsockblock.fragmentIndex));

        //if fragment was fetched
        if (tmp0!=null && tmp0.exists() && tmp0.isFile() && tmp0.length() > 0) {

            //convert file tmp0 into byteArray
            byte[] byteArray = new byte[(int) tmp0.length()];
            try {
                FileInputStream fileInputStream = new FileInputStream(tmp0);
                fileInputStream.read(byteArray);
            } catch (FileNotFoundException e) {
                System.out.println("File Not Found.");
                e.printStackTrace();
            } catch (IOException e1) {
                System.out.println("Error Reading The File.");
                e1.printStackTrace();
            }

            //now, change mdfsrsockblock into a ReplyFromOneClientToAnotherForOneFragment object
            mdfsrsockblock.flipIntoReply(byteArray);

            //convert mdfsrsockblock object into bytearray and do send
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream ooos = null;
            try {
                ooos = new ObjectOutputStream(bos);
                ooos.writeObject(mdfsrsockblock);
                ooos.flush();
                byte[] data = bos.toByteArray();

                //send the object over rsock and expect no reply
                if(RSockConstants.RSOCK) {

                    String uuid = UUID.randomUUID().toString().substring(0, 12);
                    RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", mdfsrsockblock.destGUID, 0, "hdrRecv", replyEndpoint, "noReply");
                }

                //log
                RsockReceiveForFileRetrieval.logger.log(Level.ALL, "CASE #1: resolved fragment request from node " + mdfsrsockblock.destGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{

            //log
            RsockReceiveForFileRetrieval.logger.log(Level.DEBUG, "Could not serve fragment request from node " + mdfsrsockblock.srcGUID + " for fragment# " + mdfsrsockblock.fragmentIndex + " of block# " + mdfsrsockblock.blockIdx + " of filename " + mdfsrsockblock.fileName + " due to file directory no longer exists in storage.");
        }
    }



}
