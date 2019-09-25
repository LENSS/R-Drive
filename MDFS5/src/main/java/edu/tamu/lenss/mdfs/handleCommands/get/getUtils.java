package edu.tamu.lenss.mdfs.handleCommands.get;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;


//this class contains only static functions
public class getUtils {




    //this function fetches missing fragment number for each block from disk.
    //all missing fragment indexes are marked 1, all available fragments are marked 0.
    //the boolean value is true when the function has executed successfully.
    //the boolean value if false, when the function execution has failed for some reason(maybe file read).
    public static boolean getMissingFragmentsOfABlockFromDisk(int[][] missingBlocksAndFrags, String filename, Long fileid, int N2, int blockNumber){

        try {
            //pull the directory of the block
            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg__0/ (directory)
            File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(filename, fileid, (byte) blockNumber));

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


}
