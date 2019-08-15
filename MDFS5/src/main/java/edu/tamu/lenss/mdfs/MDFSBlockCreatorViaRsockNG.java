package edu.tamu.lenss.mdfs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.ReedSolomon.EnCoDer;
import edu.tamu.lenss.mdfs.cipher.FragmentInfo;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileCreate;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class MDFSBlockCreatorViaRsockNG{

    private byte blockIdx;
    private List<String> fileStoragesAddrasGUID;
    private byte[] encryptKey;
    private byte k2, n2;
    private File blockFile;
    private ServiceHelper serviceHelper;
    private MDFSFileInfo fileInfo;
    private boolean fetchTopology, isEncryptComplete;
    private AtomicInteger fragCounter;
    String[] permList;              //permission list for the file
    String uniqueReqID;             //unique req id fr file creation job
    public boolean isFinished = false;  //this becomes true when the last thread pushes fragment to the rsock java api
    String clientID;                //client id who made the file creation request
    String filePathMDFS;            //mdfs directory in which the file will be virtually stored

    //constructor
    public MDFSBlockCreatorViaRsockNG(File file, String filePathMDFS, MDFSFileInfo info, byte blockIndex, String[] permlist, String uniquereqid, List<String> chosenodes, String clientID, byte[] key) {  //RSOCK
        this.blockIdx = blockIndex;
        this.blockFile = file;
        this.serviceHelper = ServiceHelper.getInstance();
        this.fileInfo = info;
        this.k2 = fileInfo.getK2();
        this.n2 = fileInfo.getN2();
        this.fragCounter = new AtomicInteger();
        this.permList = permlist;
        this.uniqueReqID = uniquereqid;
        this.fileStoragesAddrasGUID = chosenodes;
        this.clientID = clientID;
        this.filePathMDFS = filePathMDFS;
        this.encryptKey = key;
    }

    public String start() {
        if(encryptFile()){
            //check if the only mdfs nod eis me or there are other nodes
            if(fileStoragesAddrasGUID.size()==1 && fileStoragesAddrasGUID.get(0).equals(GNS.ownGUID)){
                //there is no other nodes to send the fragments to so return.
                return "SUCCESS";
            }else{
                //distribute fragments among other nodes
                return distributeFragments();

            }
        }else{
            return "Block encryption Failed.";

        }
    }

    //this method is only called when needs to distribute fragments among other nodes.
    //returns SUCCESS if all succeeds, Error message if not.
    private String distributeFragments() {
        //result to return
        boolean result = true;

        // Scan through all files in the folder and load them
        File fileFragDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime(), blockIdx));
        if(!fileFragDir.exists()){ return "No block directory found."; }
        File[] allFrags = fileFragDir.listFiles();

        //iterate through all fragments and send each to each destGUIDs except myself
        Iterator<String> nodesIter = fileStoragesAddrasGUID.iterator();
        for (File oneFrag : allFrags) {
            if (oneFrag.getName().contains("__frag__")) {
                // Find the fragment Number
                if (nodesIter != null && nodesIter.hasNext()) {
                    String destNode = nodesIter.next();
                    if (!destNode.equals(GNS.ownGUID)){
                        result = result && fragmentUploaderViaRsock(oneFrag, filePathMDFS, fileInfo.getCreatedTime(), permList, destNode);
                    }
                }
            }
        }

        if(result){return "SUCCESS";}
        else{return "unknown error.";}
    }

    //this function sends the fragments to each destinations
    private boolean fragmentUploaderViaRsock(File fileFrag, String filePathMDFS, long fileCreationTime, String[] permList, String destGUID){

        //return variable
        boolean result = false;

        //get the block and fragment number
        byte blockIndex = parseBlockNum(fileFrag.getName());
        byte fragmentIndex = parseFragNum(fileFrag.getName());

        //Header
        FragmentTransferInfo header = new FragmentTransferInfo(fileInfo.getFileName(), fileCreationTime, blockIndex, fragmentIndex, FragmentTransferInfo.REQ_TO_SEND);
        header.setNeedReply(true);

        //read the content of the filefrag into bytearray
        byte[] byteArray = new byte[(int) fileFrag.length()];
        try {
            FileInputStream fileInputStream = new FileInputStream(fileFrag);
            fileInputStream.read(byteArray);
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found.");
            e.printStackTrace();
        }
        catch (IOException e1) {
            System.out.println("Error Reading The File.");
            e1.printStackTrace();
        }

        //make MDFSRsockBlockCreator obj
        MDFSRsockBlockForFileCreate mdfsrsockblock = new MDFSRsockBlockForFileCreate(header, byteArray, fileInfo.getFileName(), fileInfo.getFileSize(), 0000, filePathMDFS, fileFrag.length(), fileInfo.getNumberOfBlocks(), fileInfo.getN2(), fileInfo.getK2(), fileCreationTime, permList, uniqueReqID,  GNS.ownGUID, destGUID);

        //get byteArray and size of the MDFSRsockBlockCreator obj and do send over rsock
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(mdfsrsockblock);
            oos.flush();
            byte [] data = bos.toByteArray();

            //send the object over rsock.
            String uuid = UUID.randomUUID().toString().substring(0, 12);
            result = RSockConstants.intrfc_creation.send(uuid, data, data.length, "nothing", "nothing", destGUID, 0, "default", "default", "default");
            System.out.println("fragment has been pushed to the rsock daemon to : " + destGUID);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    //encrypt a blockfile and store in the disk.
    //this function does erasure coding(ReedSolomon) and
    //cipher works.
    private boolean encryptFile(){

        //check if the blockfile exists and not null
        if(blockFile == null || !blockFile.exists()){return false;}

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

        //add fragments information in my local directory
        serviceHelper.getDirectory().addBlockFragments(fileInfo.getCreatedTime(), blockIdx, frags);

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

}
