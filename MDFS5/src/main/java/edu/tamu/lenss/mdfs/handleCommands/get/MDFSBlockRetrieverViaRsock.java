package edu.tamu.lenss.mdfs.handleCommands.get;

import org.apache.log4j.Level;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.cipher.FragmentInfo;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.BlockInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.ReedSolomon.DeCoDeR;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.MyPair;
import example.*;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileRetrieveNG;

//rsock imports
import static java.lang.Thread.sleep;


//this class object is made in MDFSFileRetrieverViaRsock.java file
//this class is responsible for fetching each block from other nodes over rsock, instead of tcp
//most of the code are copied from mdfsBlockRetriever.java class which uses tcp
public class MDFSBlockRetrieverViaRsock {
    private static final String TAG = MDFSBlockRetrieverViaRsock.class.getSimpleName();
    private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";
    private String fileName;
    private long fileId;	                    // Uses the file create time currently
    private byte blockIdx;
    private Map<String, List<Byte>> fileFrags = new HashMap<String, List<Byte>>(); // <guid, List<FragNum>>
    private MDFSFileInfo fileInfo;
    private boolean decoding = false;	        // Has the decoding procedure started?
    private final BlockRetrieveLog fileRetLog = new BlockRetrieveLog();
    private AtomicInteger locFragCounter;
    private int initFileFragsCnt = 0;          //dont change it
    private MDFSMetadata metadata;
    private boolean isFinished = false;
    private byte[] decryptKey;

    public MDFSBlockRetrieverViaRsock(String fileName, long fileId, byte blockIndex, MDFSFileInfo fileInfo){
        this.fileName = fileName;
        this.fileId = fileId;
        this.blockIdx = blockIndex;
        this.fileInfo = fileInfo;
    }


    public MDFSBlockRetrieverViaRsock(MDFSFileInfo fileInfo, byte blockIndex, MDFSMetadata metadata){	//RSOCK
        this(fileInfo.getFileName(), fileInfo.getCreatedTime(), blockIndex, fileInfo);
        this.fileInfo = fileInfo;
        this.metadata = metadata;
        this.locFragCounter  = new AtomicInteger();
    }

    //setting decryption key
    public void setDecryptKey(byte[] key){
        this.decryptKey = key;
    }


    public void start(){

        //log
        MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Starting to handle one block# " + blockIdx);

        //get stored fragments of this block in my disk
        List<FragmentInfo> localFrags = getStoredFragsOfABlock();
        if(localFrags.size()!=0 && (byte)localFrags.size() >= fileInfo.getK2()){

            //decode block
            ServiceHelper.getInstance().executeRunnableTask(new Runnable(){
                @Override
                public void run() {
                    decodeBlockFile(localFrags);
                }
            });
        }
        else{
            selectBestNodesToAskForFRagments();
        }
    }

    //this function is ony called by start() function.
    //this function is only called if locally stored fragments are not enough,
    // so we need to fetch fragments from other nodes.
    // this function pulls out all the GUIDs of fragment holders of a file and
    //selects the best nodes among them, and then passes them to the
    //requestFragments() function.
    private void selectBestNodesToAskForFRagments() {

        List<String> nodes = metadata.getAllUniqueFragmentHolders();
        Set<BlockInfo> blockInfoSet = new HashSet<>();

        for(int i=0; i< nodes.size(); i++){
            List<String> blockNums = metadata.getBlockNumbersHeldByNode(nodes.get(i));
            for(int j=0; j<blockNums.size(); j++){

                //get list of fragments for a node(index i) and a blocknum(index j)
                List<String> fragListStr = metadata.getFragmentListByNodeAndBlockNumber(nodes.get(i), blockNums.get(j));

                //convert list<String> in list<byte>
                List<Byte> fragListByte = new ArrayList<>();
                for(int k=0; k< fragListStr.size(); k++){
                    fragListByte.add((byte) Integer.parseInt(fragListStr.get(k)));
                }

                //create BlockInfo object
                BlockInfo blockinfo = new BlockInfo(metadata.getFileName(), metadata.getCreatedTime(), (byte) Integer.parseInt(blockNums.get(j)), nodes.get(i), EdgeKeeper.ownGUID);

                //put fragListByte in blockinfo object
                blockinfo.setBlockFragIndex(fragListByte);

                //add blockinfo object to the list
                blockInfoSet.add(blockinfo);

            }
        }

        requestFRagments(blockInfoSet);
    }


    //this function prepares a list of GUIDs from which fragments will be requested.
    private void requestFRagments(Set<BlockInfo> blockInfos){

        // Retrieve fragments indexes that this node already has
        Set<Byte> myfrags = new HashSet<>();
        List<FragmentInfo> localFrags = getStoredFragsOfABlock();
        for(int i=0; i< localFrags.size(); i++){
            myfrags.add(localFrags.get(i)._fragmentNumber);
        }

        //this is the unique list of fragments to be downloaded for this block.
        Set<Byte> uniqueFrags = new HashSet<Byte>();

        // add fragments from other nodes
        for(BlockInfo info : blockInfos){
            List<Byte> tmpList = info.getBlockFragIndex();
            if(tmpList != null){
                uniqueFrags.addAll(tmpList);
                tmpList.removeAll(myfrags);						// Retain only the fragments that I DO NOT have and
                fileFrags.put(info.getSource(), tmpList);		// add the unique fragments (different from mine) that each storage node has
            }
        }

        locFragCounter.set(myfrags.size());						// Init downloadCounter to the number of file frags I have
        initFileFragsCnt = locFragCounter.get();
        uniqueFrags.addAll(myfrags);							// add my fragments

        if(uniqueFrags.size() < fileInfo.getK2()){
            String s = uniqueFrags.size() + " block fragments";
            listener.onError("Insufficient fragments. " + s, fileInfo);
            return;
        }
        else{
            // Start to download file
            downloadFrags();
        }
    }


    //non blocking call
    private void downloadFrags(){
        // Create a folder for fragments of this block
        File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileId, blockIdx));
        if(!tmp0.exists()){
            if(!tmp0.mkdirs()){
                listener.onError("File IO Error. Can't create/save file locally", fileInfo);
                return;
            }
        }

        listener.statusUpdate("Downloading fragments");
        // If I have enough fragments already
        if(locFragCounter.get() >= fileInfo.getK2()){
            decodeBlockFile();
            return;
        }
        int requiredDownloadCnt = fileInfo.getK2() + 1 - locFragCounter.get();	// Download one more fragment. Just in case...

        // Search for the best(closest) storages
        List<String> storageNodes = new ArrayList<String>(fileFrags.keySet());
        Collections.sort(storageNodes, new SortByHopCount());
        Set<Byte> requestedFrag = new HashSet<Byte>();	// Record the downloaded frag. Do not download duplicates.
        for(String sNode : storageNodes){
            List<Byte> nodeFrags = fileFrags.get(sNode);
            for(Byte fragNum : nodeFrags){
                if(!requestedFrag.contains(fragNum)){
                    requestedFrag.add(fragNum);


                    Thread thread = new Thread(new FragmentDownloaderViaRsock(sNode, blockIdx, fragNum));
                    thread.start();

                    //new FragmentDownloaderViaRsock(sNode, blockIdx, fragNum, UUID.randomUUID().toString().substring(0, 7)).run();

                    if(--requiredDownloadCnt < 1){
                        return;
                    }
                }
            }
        }
    }


    //This class is used when a fragment is about to be retrieved over rsock, instead of tcp
    //most of the code of this class is copied from MDFSBlockRetriever.FragmentDownloader class
    //read MDFSBlockRetriever.FragmentDownloader class to see how that code was used here.
    //how: we first generate a header, pack the header and other important params in a
    //MDFSRsockBlockRetriever object, convert the object into byteArray, and send that over rsock.
    //when other side receives the packet, and gets the fragment request, it sends back a MDFSRsockBlockRetriever
    //object containing file fragment. when fileFrag is received, it is written on disk, and checked if the entire file is retrieve-able.
    class FragmentDownloaderViaRsock implements Runnable{
        private String destGUID;
        private byte blockIdx;
        private byte fragmentIndex;

        private FragmentDownloaderViaRsock(String destination, byte blockIndex, byte fragmentIdx){
            this.destGUID = destination;
            this.fragmentIndex = fragmentIdx;
            this.blockIdx = blockIndex;
        }

        @Override
        public void run(){
            System.out.println("xxx a thread is born");
            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Downloading block# " + blockIdx + ", fragment# " + fragmentIndex);
            int block = 0;
            int total_pooling_time = 0;
            int interval = 1000;
            boolean success = false;
            File tmp0 = null;
            boolean fileFragRetrieveSuccess;
            try {

                //make an object of MDFSRsockBlockRetrieval
                MDFSRsockBlockForFileRetrieveNG mdfsrsockblock = null; //new MDFSRsockBlockForFileRetrieve(destGUID, EdgeKeeper.ownGUID, fileName, fileId, blockIdx, fragmentIndex, MDFSRsockBlockForFileRetrieve.Type.Request);

                //get byteArray and size of the MDFSRsockBlockRetreival obj
                byte[] data;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                oos = new ObjectOutputStream(bos);
                oos.writeObject(mdfsrsockblock);
                oos.flush();
                data = bos.toByteArray();

                //send request and expect reply
                String uuid = UUID.randomUUID().toString().substring(0, 12);
                RSockConstants.intrfc_retrieval.send(uuid, data, data.length,"nothing", "nothing", destGUID,0,RSockConstants.fileRetrieveEndpoint , RSockConstants.fileRetrieveEndpoint, RSockConstants.fileRetrieveEndpoint);

                //log
                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "fragment# " + fragmentIndex +  " of block# " + blockIdx +" has been requested from guid: " + destGUID + "with request uuid: " + uuid);


                //now waiting for reply with fileFrag.
                ReceivedFile receivedFile = null;
                while(true){

                    System.out.println("xxx blocking on while loop...");

                    //check if blocking time expired then break out of while(whether or not fragment received))
                    if(total_pooling_time>= Constants.FRAGMENT_RETRIEVAL_TIMEOUT_INTERVAL){
                        RSockConstants.intrfc_retrieval.deleteEndpoint(RSockConstants.fileRetrieveEndpoint);
                        System.out.println("xxx unblocking from while loop.");
                        break;
                    }

                    //or something has been received
                    try { receivedFile = RSockConstants.intrfc_retrieval.receive(interval, RSockConstants.fileRetrieveEndpoint); } catch (InterruptedException e) {e.printStackTrace(); }
                    total_pooling_time = total_pooling_time + interval;
                    if(receivedFile!=null) {

                        //coming here means we received a reply for file fragment request
                        RSockConstants.intrfc_retrieval.deleteEndpoint(RSockConstants.fileRetrieveEndpoint);


                        //get the byteArray[] from the receivedFile obj and convert into MDFSRsockBlockRetrieval object
                        ByteArrayInputStream bis = new ByteArrayInputStream(receivedFile.getFileArray());
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        mdfsrsockblock = (MDFSRsockBlockForFileRetrieveNG) ois.readObject();
                        bis.close();
                        ois.close();


                        //parse mdfsrsockblock object
                        int fragLength = 0; //(int) mdfsrsockblock.fileFragLength;

                        //get the fileFrag as byteArray
                        byte[] byteArray = (byte[]) mdfsrsockblock.fileFrag;

                        //check if the receive is legit
                        fileFragRetrieveSuccess = true; // (boolean) mdfsrsockblock.fileFragRetrieveSuccess;
                        if(fileFragRetrieveSuccess){

                            //coming here means the received file fragment reply indeed contains a file fragment

                            //create fileFrag from byteArray
                            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0_dOwN__lOaDiNg___   (file)
                            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex) + DOWNLOADING_SIGNATURE );
                            FileOutputStream outputStream = new FileOutputStream(tmp0);
                            outputStream.write(byteArray);
                            outputStream.flush();
                            outputStream.close();

                            // Rename the fragment after it is completely downloaded to avoid decoder using a fragment that is still being downloaded
                            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0 (file)
                            success = IOUtilities.renameFile(tmp0, MDFSFileInfo.getFragName(fileName, blockIdx, fragmentIndex));

                            ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/test1.jpg_0123__frag__0 (file)
                            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex));
                            try { sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

                            //log
                            MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Finish downloading fragment " + fragmentIndex + " from node " + destGUID );

                        }else{
                            //this means other side received fragment request, other side processed fragment request, but other side did not have the fragment, so it sent back an empty file.
                        }
                    }else{
                        //returned with null, did not received any reply from other side.
                    }
                }
            }catch(IOException | ClassNotFoundException | NullPointerException e){

                //log
                MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Exception is block retrieval, " , e);

            }finally{
                if(success && tmp0!=null && tmp0.length() > 0){

                    //log
                    MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Fragment#" + fragmentIndex + " of Block# " + blockIdx + " retrieve success.");

                    // success! so update directory
                    ServiceHelper.getInstance().getDirectory().addBlockFragment(fileId, blockIdx, fragmentIndex);

                    //update local fragment counter for this block

                    locFragCounter.incrementAndGet();
                }else if(tmp0 != null){

                    //log
                    MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Failed to download fragment# " + fragmentIndex + " of block# " + blockIdx + ".");

                    //something was wrong
                    tmp0.delete();

                }

                //check if enough fragments are available after fragment download
                if(locFragCounter.get() >= fileInfo.getK2()){

                    //log
                    MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Enough fragments of block# " + blockIdx + " is available now.");

                    //decode the fragments into a block
                    decodeBlockFile();
                }

            }

            System.out.println("xxx a thread is dying");
        }

    }


    //this function is directly called from start() when all fagments of a block are locally available,
    //and no need to download fragmentss from other nodes.
    //or called by decodeFIle() function after fetching of fragments is done.
    private synchronized void decodeBlockFile(List<FragmentInfo> blockFragments){

        MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Start to decode block.");
        if(!isFinished) {
            // Final Check. Make sure enough fragments are available
            //decodeBlockFile() execution will only proceed further if this block of code passes
            if (blockFragments.size() < fileInfo.getK2()) {
                String s = blockFragments.size() + " block fragments are available locally.";

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "block# " + blockIdx + " decryption failed, insufficient fragments.");
                listener.onError("Insufficient fragments." + s, fileInfo);
                return;
            }

            //check if file already being decoded
            if (decoding) {
                return;
            } else {
                decoding = true;
            }

            //listener and log update
            listener.statusUpdate("Decoding fragments");

            //if decryptkey is null
            if (decryptKey == null) {

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Block# " + blockIdx + " decryption failed, no decryption key found.");
                listener.onError("No decryption key found", fileInfo);
                return;
            }

            //if enough fragments available, isFinished is False, decoding is False and decryptKey is valid, then decode and store the file
            //tmp0 = /storage/emulated/0/MDFS/test1.jpg__0123/ (directory)
            //tmp = /storage/emulated/0/MDFS/test1.jpg__0123/test2.jpg_0123__blk__0 (file)
            File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime()));
            File tmp = IOUtilities.createNewFile(tmp0, MDFSFileInfo.getBlockName(fileInfo.getFileName(), blockIdx));

            //make decoder object
            DeCoDeR decoder = new DeCoDeR(decryptKey, fileInfo.getN2(), fileInfo.getK2(), blockFragments, tmp.getAbsolutePath());

            //check if decoding completed
            if (decoder.ifYouSmellWhatTheRockIsCooking()) {  //takes bunch of file fragments and returns file block

                //log
                MDFSFileRetrieverViaRsock.logger.log(Level.ALL, "Block# " + blockIdx +" Decryption Complete");

                isFinished = true;
                listener.onComplete(tmp, fileInfo);
                return;
            } else {

                //log and listener
                MDFSFileRetrieverViaRsock.logger.log(Level.DEBUG, "Failed to decode some fragments of block# " + blockIdx);
                listener.onError("Fail to decode the fragments. You may try again", fileInfo);

                isFinished = false;
                decoding = false;
                return;
            }
        }
    }

    //this function is only called when all blocks are not locally available,
    //and needed to download blocks from other nodes.
    //this function is called only AFTER all the required blocks hav been downloaded
    //this function eventually calls the other decodeBlockFile(List<FragmentInfo> blockFragments) function
    private void decodeBlockFile(){
        decodeBlockFile(getStoredFragsOfABlock());
    }

    //Distance from me; the closer to me, the shorter the disatance is
    class SortByHopCount implements Comparator<String>{
        @Override
        public int compare(String nodeId1, String nodeId2) {
            return 0;
        }
    }

    //get stored fragments for this block in this device.
    //never returns null, always returns a list that is either empty
    // or has elements in it.
    private List<FragmentInfo> getStoredFragsOfABlock(){


        //create a list fo fragmentInfo object to return
        List<FragmentInfo> blockFragments = new ArrayList<FragmentInfo>();


        // Check stored fragments
        ///storage/emulated/0/MDFS/test1.jpg_0123/test1.jpg_0123__0/  (directory)
        File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileId, blockIdx));

        //check if its a directory
        if(blockDir.isDirectory()){

            //get all the files in the directory
            File[] files = blockDir.listFiles(new FileFilter(){
                @Override
                public boolean accept(File f) {

                    //filter
                    return ( f.isFile() && f.getName().contains(fileName + "__" + blockIdx + "__frag__") );
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

    //MDFSFileRetriever listener setter and getter function
    public BlockRetrieverListenerViaRsock getListener() {
        return listener;
    }

    public void setListener(BlockRetrieverListenerViaRsock listener) {
        this.listener = listener;
    }

    //Default FileRetrieverListener. Do nothing.
    private BlockRetrieverListenerViaRsock listener = new BlockRetrieverListenerViaRsock(){
        @Override
        public void onError(String error, MDFSFileInfo fileInfo) {
        }
        @Override
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
        }
        @Override
        public void statusUpdate(String status) {
        }
    };//

    //interface for BlockRetrieverListenerViaRsock
    public interface BlockRetrieverListenerViaRsock{
        public void onError(String error, MDFSFileInfo fileInfo);
        public void statusUpdate(String status);
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo);
    }

    //some kind of logging
    private class BlockRetrieveLog{
        public BlockRetrieveLog(){}
        public long discStart, discEnd, retStart, retEnd, decryStart, decryEnd;
        public List<MyPair<Long, Byte>> fileSources = new ArrayList<MyPair<Long, Byte>>(); // <NodeIp, FragNum>
        public Set<BlockInfo> fileReps;
        public String getDiff(long l1, long l2){
            return Long.toString(Math.abs(l2-l1));
        }
    }

    public byte getBlockIdx(){
        return blockIdx;
    }


}


