package edu.tamu.lenss.mdfs.Commands.put;

import org.apache.log4j.Level;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.ReedSolomon.EnCoDer;
import edu.tamu.lenss.mdfs.Cipher.FragmentInfo;
import edu.tamu.lenss.mdfs.Handler.ServiceHelper;
import edu.tamu.lenss.mdfs.Model.MDFSFileInfo;
import edu.tamu.lenss.mdfs.Model.MDFSRsockBlockForFileCreate;
import edu.tamu.lenss.mdfs.Utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.Utils.IOUtilities;

public class MDFSBlockCreatorViaRsockNG{

    private byte blockIdx;
    private List<String> fileStorageGUIDs;
    private byte[] encryptKey;
    private byte k2, n2;
    private File blockFile;
    private ServiceHelper serviceHelper;
    private MDFSFileInfo fileInfo;
    String uniqueReqID;                     //unique req id fr file creation job
    String filePathMDFS;
    MDFSMetadata metadata;

    //private default constructor
    private MDFSBlockCreatorViaRsockNG(){}

    //constructor
    public MDFSBlockCreatorViaRsockNG(File file, String filePathMDFS, MDFSFileInfo info, byte blockIndex, String uniquereqid, List<String> chosenodes, byte[] key, MDFSMetadata metadata) {  //RSOCK
        this.blockIdx = blockIndex;
        this.blockFile = file;
        this.serviceHelper = ServiceHelper.getInstance();
        this.fileInfo = info;
        this.k2 = fileInfo.getK2();
        this.n2 = fileInfo.getN2();
        this.uniqueReqID = uniquereqid;
        this.fileStorageGUIDs = chosenodes;
        this.filePathMDFS = filePathMDFS;
        this.encryptKey = key;
        this.metadata = metadata;
    }

    //returns SUCCESS or Error message
    public String start() {

        //log
        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "Starting to handle one block, block# " + blockIdx);

        if(encryptFile()){

            //check if the only mdfs node is me or there are other nodes
            if(fileStorageGUIDs.size()==1 && fileStorageGUIDs.get(0).equals(EdgeKeeper.ownGUID)){

                //log
                MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "Block creation success.");

                //there is no other nodes to send the fragments to so return.
                return "SUCCESS";

            }else{

                //distribute fragments among other nodes
                //returns SUCCESS or error message
                return distributeFragments();

            }
        }else{

            //log
            MDFSFileCreatorViaRsockNG.logger.log(Level.DEBUG, "Block# " + blockIdx +" encryption Failed");
            return "Block encryption Failed.";

        }
    }

    //this method is only called when needs to distribute fragments among other nodes.
    //returns SUCCESS if all succeeds, Error message if not.
    private String distributeFragments() {

        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "Starting to send block fragments of block# " + blockIdx);

        //result to return
        boolean result = true;

        // Scan through all files in the folder and load them
        File fileFragDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime(), blockIdx));
        if(!fileFragDir.exists()){ return "No block directory found."; }
        File[] allFrags = fileFragDir.listFiles();

        //now send them one by one
        //iterate through all fragments and send each to each destGUIDs except myself
        Iterator<String> nodesIter = fileStorageGUIDs.iterator();
        for (File oneFrag : allFrags) {
            if (oneFrag.getName().contains("__frag__")) {

                //get the fragment number from name
                int fragIndex = ServiceHelper.getInstance().getDirectory().getFragmentIndexFromName(oneFrag.getName());

                // get the destination guid
                if (nodesIter != null && nodesIter.hasNext()) {
                    String destNode = nodesIter.next();

                    //dont sent fragments to myself again
                    if (!destNode.equals(EdgeKeeper.ownGUID)){
                        result = result && fragmentUploaderViaRsock(oneFrag, filePathMDFS, fileInfo.getCreatedTime(), fragIndex,  destNode);
                    }
                }
            }
        }

        if(result){

            //log
            MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "Successfully pushed block# " + blockIdx + " to rsock");

            //return
            return "SUCCESS";
        }
        else{

            //log
            MDFSFileCreatorViaRsockNG.logger.log(Level.DEBUG, "Failed to push one or more fragments of block# " + blockIdx + " to rsock daemon. <check rsock client library>");
            return "Failed to push one or more fragments to the rsock daemon.";}
    }

    //this function sends the fragments to each destinations
    private boolean fragmentUploaderViaRsock(File fileFrag, String filePathMDFS, long fileCreationTime, int fragIndex, String destGUID){

        //return variable
        boolean result = false;

        //get the block and fragment number
        byte blockIndex = parseBlockNum(fileFrag.getName());
        byte fragmentIndex = parseFragNum(fileFrag.getName());

        //read the content of the filefrag into bytearray
        byte[] byteArray = new byte[(int) fileFrag.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(fileFrag);
            fileInputStream.read(byteArray);
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found.");
            return false;
        }
        catch (IOException e1) {
            System.out.println("Error Reading The File.");
            return false;
        }

        //make MDFSRsockBlockCreator obj
        MDFSRsockBlockForFileCreate mdfsrsockblock = new MDFSRsockBlockForFileCreate(fileInfo.getFileName(), filePathMDFS, byteArray, fileCreationTime, fileInfo.getFileSize(), fileInfo.getN2(), fileInfo.getK2(), blockIndex, fragmentIndex, EdgeKeeper.ownGUID, uniqueReqID, Constants.metadataIsGlobal);

        //get byteArray and size of the MDFSRsockBlockCreator obj and do send over rsock
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(mdfsrsockblock);
            oos.flush();
            byte [] data = bos.toByteArray();

            //send the object over rsock and dont expect reply
            String uuid = UUID.randomUUID().toString().substring(0, 12);
            result = RSockConstants.intrfc_creation.send(uuid, data, data.length, "nothing", "nothing", destGUID, 500, RSockConstants.fileCreateEndpoint, RSockConstants.fileCreateEndpoint, "noReply");
            //result = true; //test

            //log
            MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "fragment# " + fragIndex +  " of block# " + blockIndex +" has been pushed to rsock daemon to guid: " + destGUID + "with uuid: " + uuid);

            //add block and fragment info into metadata
            metadata.addInfo(destGUID, blockIndex, fragIndex);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        //log
        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "Failed to push fragment# " + fragIndex +" to rsock daemon to guid: " + destGUID);

        return false;
    }


    //encrypt a blockfile and store in the disk.
    //this function does fragmentation/erasure coding(ReedSolomon) and
    //cipher works.
    private boolean encryptFile(){

        //check if the blockfile exists and not null
        if(blockFile == null || !blockFile.exists()){

            //log
            MDFSFileCreatorViaRsockNG.logger.log(Level.DEBUG, "Failed to encrypt block# " + blockIdx);

            //return
            return false;
        }

        //encode
        EnCoDer encoder = new EnCoDer(encryptKey, n2, k2, blockFile);
        List<FragmentInfo> fragInfos = encoder.ifYouSmellWhatTheRockIsCooking();   ///takes a file block as input, cipher it and returns bunch of file fragments

        //check if encryption succeeded
        if(fragInfos == null) {return false;}

        // Store the file fragments in local SDCard
        File fragsDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime(),	blockIdx));
        HashSet<Byte> frags = new HashSet<Byte>();

        // Write file fragments to SD Card
        for (FragmentInfo frag : fragInfos) {
            File tmp = IOUtilities.createNewFile(fragsDir, MDFSFileInfo.getFragName(fileInfo.getFileName(), blockIdx, frag.getFragmentNumber()));
            if (tmp != null && IOUtilities.writeObjectToFile(frag, tmp)) {
                frags.add(frag.getFragmentNumber());
            }
        }

        //log
        MDFSFileCreatorViaRsockNG.logger.log(Level.ALL, "block# " + blockIdx + " has been successfully encrypted and partitioned into fragments.");

        return true;
    }

    protected boolean deleteBlockFile(){
        return blockFile.delete();
    }

    private byte parseBlockNum(String fName){
        String str = fName.substring(0, fName.lastIndexOf("__frag__"));
        str = str.substring(str.lastIndexOf("_")+1);
        return Byte.parseByte(str.trim());
    }
    private byte parseFragNum(String fName) {
        return Byte.parseByte(fName.substring(fName.lastIndexOf("_") + 1).trim());
    }

    public byte getBlockIdx(){
        return blockIdx;
    }

}
