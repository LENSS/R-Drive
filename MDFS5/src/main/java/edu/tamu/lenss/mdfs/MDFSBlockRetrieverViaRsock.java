package edu.tamu.lenss.mdfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.cipher.FragmentInfo;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.BlOcKrEpLy;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.testCrypto.DeCoDeR;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MyPair;
import example.*;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileRetrieve;

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
    private ServiceHelper serviceHelper;
    private Map<String, List<Byte>> fileFrags = new HashMap<String, List<Byte>>(); // <guid, List<FragNum>>
    private MDFSFileInfo fileInfo;
    private boolean decoding = false;	        // Has the decoding procedure started?
    private final BlockRetrieveLog fileRetLog = new BlockRetrieveLog();
    private AtomicInteger locFragCounter;
    private int initFileFragsCnt = 0;          //dont change it
    private FileMetadata metadata;
    private boolean isFinished = false;
    private String clientID;
    private byte[] decryptKey;

    public MDFSBlockRetrieverViaRsock(String fileName, long fileId, byte blockIndex, String clientID, MDFSFileInfo fileInfo){
        serviceHelper = ServiceHelper.getInstance();
        this.fileName = fileName;
        this.fileId = fileId;
        this.blockIdx = blockIndex;
        this.fileInfo = fileInfo;
        this.clientID = clientID;
    }


    public MDFSBlockRetrieverViaRsock(MDFSFileInfo fInfo, byte blockIndex, FileMetadata metadata, String clientID, MDFSFileInfo fileInfo){	//RSOCK
        this(fInfo.getFileName(), fInfo.getCreatedTime(), blockIndex, clientID, fileInfo);
        this.fileInfo = fInfo;
        this.metadata = metadata;
        this.locFragCounter  = new AtomicInteger();
    }

    //setting decryption key
    public void setDecryptKey(byte[] key){
        this.decryptKey = key;
    }


    public void start(){

        System.out.println("xxx" + " inside block ret start");

        // Check if a decrypted file or encrypted file already exists on my device
        // If it is, returns it immediately.
        final List<FragmentInfo> localFrags = getStoredFrags();

        if(localFrags != null && (byte)localFrags.size() >= fileInfo.getK2()){
            serviceHelper.executeRunnableTask(new Runnable(){
                @Override
                public void run() {
                    decodeFile(localFrags);
                }
            });
        }
        else{
            doTheThing();
        }
    }

    //this function is ony called by start() function.
    //this function is only called if locally stored fragments are not enough,
    // so we need to fetch fragments from other nodes.
    // this function pulls out all the GUIDs of fragment holders of a file and
    //selects the best nodes among them, and then passes them to the
    //doAnotherThing() function.
    private void doTheThing() {

        List<String> nodes = metadata.getAllUniqueFragmentHolders();
        Set<BlOcKrEpLy> blockrepdumbSet = new HashSet<>();

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

                //create BlockReplyDumb object
                BlOcKrEpLy blockReplyDumb = new BlOcKrEpLy(metadata.filename, metadata.fileID, (byte) Integer.parseInt(blockNums.get(j)), nodes.get(i), GNS.ownGUID);

                //put fragListByte in blockReplyDumb object
                blockReplyDumb.setBlockFragIndex(fragListByte);

                //add blockReplyDumb object to the list
                blockrepdumbSet.add(blockReplyDumb);

            }
        }

        doAnotherThing(blockrepdumbSet);
    }


    //this function prepares a list of GUIDs from which fragments will be requested.
    private void doAnotherThing(Set<BlOcKrEpLy> blockREPs){

        // Retrieve block fragments info
        Set<Byte> myfrags = serviceHelper.getDirectory().getStoredFragIndex(fileId, blockIdx);	// Get the fragments I have
        if(myfrags == null){ myfrags = new HashSet<Byte>();}
        Set<Byte> uniqueFrags = new HashSet<Byte>();				// all the available fragments, including mine and others

        // add fragments from other nodes
        for(BlOcKrEpLy rep : blockREPs){
            List<Byte> tmpList = rep.getBlockFragIndex();
            if(tmpList != null){
                uniqueFrags.addAll(tmpList);
                tmpList.removeAll(myfrags);						// Retain only the fragments that I DO NOT have and
                fileFrags.put(rep.getSource(), tmpList);		// add the unique fragments (different from mine) that each storage node has
            }
        }
        // Note. fileFrags do not include MY cached fragments

        locFragCounter.set(myfrags.size());						// Init downloadCounter to the number of file frags I have
        initFileFragsCnt = locFragCounter.get();
        uniqueFrags.addAll(myfrags);							// add my fragments

        if(uniqueFrags.size() < fileInfo.getK2()){
            String s = uniqueFrags.size() + " block fragments";
            listener.onError("Insufficient fragments. " + s, fileInfo, clientID);
            return;
        }
        else{
            // Start to download file
            downloadFrags();
        }
    }


    //non blocking call
    private void downloadFrags(){
        System.out.println("xxx" + " inside download frags");

        // Create a folder for fragments of this block
        File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileId, blockIdx));
        if(!tmp0.exists()){
            if(!tmp0.mkdirs()){
                listener.onError("File IO Error. Can't create/save file locally", fileInfo, clientID);
                return;
            }
        }

        listener.statusUpdate("Downloading fragments", clientID);
        // If I have enough fragments already
        System.out.println("xxx locfragcount: " + locFragCounter.get());
        if(locFragCounter.get() >= fileInfo.getK2()){
            System.out.println("xxx decodefile is being called from downloadfrags");
            decodeFile();
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


                    Thread thread = new Thread(new FragmentDownloaderViaRsock(sNode, blockIdx, fragNum, UUID.randomUUID().toString().substring(0, 7)));
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
        private String sendAndReplyEndpoint;

        private FragmentDownloaderViaRsock(String destination, byte blockIndex, byte fragmentIdx, String sendAndReplyEndpoint){
            this.destGUID = destination;
            this.fragmentIndex = fragmentIdx;
            this.blockIdx = blockIndex;
            this.sendAndReplyEndpoint = sendAndReplyEndpoint;
        }

        @Override
        public void run(){
            System.out.println("xxx a thread is born");
            System.out.println("ifff downloading blocknum " + blockIdx + " fragmentnum " + fragmentIndex);
            int block = 0;
            int total_pooling_time = 0;
            int interval = 1000;
            boolean success = false;
            File tmp0 = null;
            boolean fileFragRetrieveSuccess;
            FragmentTransferInfo header = null;
            try {
                // make header
                header = new FragmentTransferInfo(fileName, fileId, blockIdx, fragmentIndex, FragmentTransferInfo.REQ_TO_RECEIVE);
                header.setNeedReply(false);

                //make an object of MDFSRsockBlockRetrieval
                MDFSRsockBlockForFileRetrieve mdfsrsockblock = new MDFSRsockBlockForFileRetrieve(header, destGUID, GNS.ownGUID, fileName, fileId, blockIdx, fragmentIndex);

                //get byteArray and size of the MDFSRsockBlockRetreival obj
                byte[] data;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                oos = new ObjectOutputStream(bos);
                oos.writeObject(mdfsrsockblock);
                oos.flush();
                data = bos.toByteArray();

                //print of byteArray size for testing
                System.out.println("print: " + 	Arrays.toString(data));

                //send happening here
                String uuid = UUID.randomUUID().toString().substring(0, 12);
                RSockConstants.intrfc_retrieval.send(uuid, data, data.length,"nothing", "nothing", destGUID,0, sendAndReplyEndpoint, "hdrRecv", sendAndReplyEndpoint);


                //now waiting for reply with fileFrag
                ReceivedFile receivedFile = null;
                while(true){
                    //block only for this amount of time
                    System.out.println("xxx blocking on while " + (++block));

                    //check if blocking time expired then break out of while(whether or not fragment received))
                    if(total_pooling_time>= Constants.FRAGMENT_RETRIEVAL_TIMEOUT_INTERVAL){
                        RSockConstants.intrfc_retrieval.deleteEndpoint(sendAndReplyEndpoint);
                        System.out.println("xxx un blocking on while ");
                        break;
                    }

                    //or something has been received
                    try { receivedFile = RSockConstants.intrfc_retrieval.receive(interval, sendAndReplyEndpoint); } catch (InterruptedException e) {e.printStackTrace(); }
                    total_pooling_time = total_pooling_time + interval;
                    if(receivedFile!=null) {
                        //coming here means we received a reply for file fragment request
                        System.out.println("xxx received file frag which is not null inside MDFSBlockRetrieval (success)");
                        RSockConstants.intrfc_retrieval.deleteEndpoint(sendAndReplyEndpoint);

                        //get the byteArray[] from the receivedFile obj and convert into MDFSRsockBlockRetrieval object
                        ByteArrayInputStream bis = new ByteArrayInputStream(receivedFile.getFileArray());
                        ObjectInputStream ois = new ObjectInputStream(bis);
                        mdfsrsockblock = (MDFSRsockBlockForFileRetrieve) ois.readObject();
                        bis.close();
                        ois.close();


                        //parse mdfsrsockblock object
                        System.out.println("casting the file");
                        int fragLength = (int) mdfsrsockblock.fileFragLength;

                        //get the fileFrag as byteArray
                        System.out.println("getting byteArray");
                        byte[] byteArray = (byte[]) mdfsrsockblock.fileFrag;

                        //print the byteArray
                        //System.out.print("print: " + Arrays.toString(byteArray));

                        //check if the receive is legit
                        fileFragRetrieveSuccess = (boolean) mdfsrsockblock.fileFragRetrieveSuccess;
                        if(fileFragRetrieveSuccess){

                            //coming here means the received file fragment reply indeed contains a file fragment
                            System.out.println("file signature is true");

                            //create fileFrag from byteArray
                            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex) + DOWNLOADING_SIGNATURE );
                            FileOutputStream outputStream = new FileOutputStream(tmp0);
                            outputStream.write(byteArray);
                            outputStream.flush();
                            outputStream.close();

                            // Rename the fragment after it is completely downloaded to avoid decoder using a fragment that is still being downloaded
                            success = IOUtilities.renameFile(tmp0, MDFSFileInfo.getFragName(fileName, blockIdx, fragmentIndex));

                            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex));
                            try { sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }

                            Logger.v(TAG, "Finish downloading fragment " + fragmentIndex + " from node " + destGUID );
                        }else{
                            System.out.println("file signature is not true");
                            //this means other side received fragment request, other side processed fragment request, but other side did not have the fragment, so it sent back an empty file.
                        }
                        break;
                    }else{
                        //returned with null, did not received any reply from other side.

                    }
                }

            }catch(IOException | ClassNotFoundException | NullPointerException e){
                System.out.println("EXCEPTION!");
                e.printStackTrace();
            }finally{
                if(success && tmp0.length() > 0){	// Hacky way to avoid 0 byte file
                    // success! so update directory
                    serviceHelper.getDirectory().addBlockFragment(header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex());
                    locFragCounter.incrementAndGet();
                }else if(tmp0 != null){
                    //something was wrong
                    tmp0.delete();
                }

                if(locFragCounter.get() >= fileInfo.getK2()){
                    decodeFile();
                }

            }

            System.out.println("xxx a thread is dying");
        }

    }


    //this function is directly called from start() when all fagments of a block are locally available,
    //and no need to download fragmentss from other nodes.
    //or called by decodeFIle() function after fetching of fragments is done.
    private synchronized void decodeFile(List<FragmentInfo> blockFragments){

        System.out.println("xxx inside decodefile");
        if(!isFinished) {
            // Final Check. Make sure enough fragments are available
            //decodeFile() execution will only proceed further if this block of code passes
            if (blockFragments.size() < fileInfo.getK2()) {
                String s = blockFragments.size() + " block fragments are available locally.";
                System.out.println("xxx inside decodefile insufficient fragments");
                listener.onError("Insufficient fragments. " + s, fileInfo, clientID);
                return;
            }

            //check if file already being decoded
            if (decoding) {
                return;
            } else {
                decoding = true;
            }

            //listener and log update
            listener.statusUpdate("Decoding fragments", clientID);

            //if decryptkey is null
            if (decryptKey == null) {
                listener.onError("No decryption key found", fileInfo, clientID);
                return;
            }

            //if enough fragments available, isFinished is False, decoding is False and decryptKey is valid, then
            //decode and store the file
            File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime()));
            File tmp = IOUtilities.createNewFile(tmp0, MDFSFileInfo.getBlockName(fileInfo.getFileName(), blockIdx));

            //make decoder object
            DeCoDeR decoder = new DeCoDeR(decryptKey, fileInfo.getN2(), fileInfo.getK2(), blockFragments, tmp.getAbsolutePath() );

            //check if decoding completed
            if (decoder.ifYouSmellWhatTheRockIsCooking()) {  //takes bunch of file fragments and returns file block
                System.out.println("xxx Block Decryption Complete");
                isFinished = true;
                listener.onComplete(tmp, fileInfo, clientID);
                return;
            } else {
                isFinished = false;
                decoding = false;
                listener.onError("Fail to decode the fragments. You may try again", fileInfo, clientID);
                return;
            }
        }
    }

    //this function is only called when all blocks are not locally available,
    //and needed to download blocks from other nodes.
    //this function is called only AFTER all the required blocks hav been downloaded
    //this function eventually calls the other decodeFile(List<FragmentInfo> blockFragments) function
    private void decodeFile(){
        decodeFile(getStoredFrags());
    }

    //Distance from me; the closer to me, the shorter the disatance is
    class SortByHopCount implements Comparator<String>{
        @Override
        public int compare(String nodeId1, String nodeId2) {
            return 0;
        }
    }

    //get stored fragments
    private List<FragmentInfo> getStoredFrags(){
        Set<Byte> cachedFrags = serviceHelper.getDirectory().getStoredFragIndex(fileId, blockIdx);

        List<FragmentInfo> blockFragments = new ArrayList<FragmentInfo>();
        if(cachedFrags != null){
            // Check stored fragments
            File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileId, blockIdx));
            if(blockDir.isDirectory()){
                File[] files = blockDir.listFiles(new FileFilter(){
                    @Override
                    public boolean accept(File f) {
                        // one fragment maybe still being downloaded. We assume each fragment is at least 5k large
                        return (f.isFile()
                                //&& !f.getName().contains(DOWNLOADING_SIGNATURE)
                                && f.getName().contains(fileName + "__" + blockIdx + "__frag__")
                                //&& f.length() > 1024*5
                                );
                    }
                });

                System.out.println("xxx list of files " + files.length);
                for (File f : files) {
                    FragmentInfo frag;
                    frag = IOUtilities.readObjectFromFile(f, FragmentInfo.class);
                    if(frag != null && cachedFrags.contains(frag.getFragmentNumber()))
                        blockFragments.add(frag);
                }
            }
        }

        System.out.println("xxx local frag count:  " + blockFragments.size());
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
        public void onError(String error, MDFSFileInfo fileInfo, String clientID) {
        }
        @Override
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo, String clientID) {
        }
        @Override
        public void statusUpdate(String status, String clientID) {
        }
    };//

    //interface for BlockRetrieverListenerViaRsock
    public interface BlockRetrieverListenerViaRsock{
        public void onError(String error, MDFSFileInfo fileInfo, String clientID);
        public void statusUpdate(String status, String clientID);
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo, String clientID);
    }

    //some kind of logging
    private class BlockRetrieveLog{
        public BlockRetrieveLog(){}
        public long discStart, discEnd, retStart, retEnd, decryStart, decryEnd;
        public List<MyPair<Long, Byte>> fileSources = new ArrayList<MyPair<Long, Byte>>(); // <NodeIp, FragNum>
        public Set<BlOcKrEpLy> fileReps;
        public String getDiff(long l1, long l2){
            return Long.toString(Math.abs(l2-l1));
        }
    }


}


