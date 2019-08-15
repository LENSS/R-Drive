package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;


import android.os.Environment;

import com.google.common.io.Files;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.CallableTask;
import edu.tamu.lenss.mdfs.utils.CallableTask.CallableCallback;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mp4process.SplitVideo;

//rsock imports
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock;
import rsock.Topology;
import static java.lang.Thread.sleep;


//this class is used only to create file and distribute fragments to other nodes using rsock, instead of tcp.
//most of the codes are copied from MDFSFileCreator.java class.
public class MDFSFileCreatorViaRsock {
    private static final String TAG = MDFSFileCreatorViaRsock.class.getSimpleName();
    private File file;              //the actual file
    private MDFSFileInfo fileInfo;  //file information
    private byte[] encryptKey;
    private int blockCount;
    private double encodingRatio;
    private boolean isTopComplete = false;
    private boolean isPartComplete = false;
    private boolean isSending = false;
    List<String> chosenNodes;
    String[] permList;              //permList contains entries like WORLD, OWNER, GROUP:<group_name> or GUIDs
    FileMetadata metadata;          //metadata object for this file
    String clientID;                //the client who is making this request
    String filePathMDFS;            //virtual directory path in MDFS in which the file will be saved. if dir doesnt exist, it willbe created first
    String uniqueReqID;             //unique id for file creation req


    //this constructor is called from the application side
    public MDFSFileCreatorViaRsock(File f, String filePathMDFS, long maxBlockSize, double encodingRatio, String[] permList, String clientID) {  //RSOCK
        this.file = f;
        this.filePathMDFS = filePathMDFS;
        this.encodingRatio = encodingRatio;
        this.blockCount = (int)Math.ceil((double)file.length()/maxBlockSize);
        this.fileInfo = new MDFSFileInfo(file.getName(), file.lastModified());
        this.fileInfo.setFileSize(file.length());
        this.fileInfo.setNumberOfBlocks((byte)blockCount);
        this.uniqueReqID = UUID.randomUUID().toString().substring(0,12);
        this.permList = permList;
        this.clientID = clientID;
        this.chosenNodes = new ArrayList<>();
        this.metadata = new FileMetadata(EdgeKeeperConstants.FILE_CREATOR_METADATA_DEPOSIT_REQUEST, EdgeKeeperConstants.getMyGroupName(), GNS.ownGUID, GNS.ownGUID, fileInfo.getCreatedTime(), new String[1], new Date().getTime(), uniqueReqID, fileInfo.getFileName(), fileInfo.getFileSize(), 0000, filePathMDFS ,blockCount, (byte)0,(byte) 0);

    }


    //this function is called from HomeScreen.java class
    //this function sets a listener(listener is used to update app activity )
    public void setListener(MDFSBlockCreatorListenerViaRsock fileListener){
        fileCreatorListener = fileListener;
    }


    public void start(){

        //first fetch topology from olsr
        //then convert permList[] into GUIDs and populates chosenNodes list
        fetchTopologyAndChooseNodes();

        if(blockCount > 1){
            partition();
            System.out.println("blockcount: " + blockCount);
        }else{
            // Single block so we create a new file and copy all the bytes from main file to new file in mdfs directory and operate on that
            File fileBlock = IOUtilities.createNewFile(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + MDFSFileInfo.getFileDirPath(file.getName(), file.lastModified()) + File.separator + MDFSFileInfo.getBlockName(file.getName(), (byte)0));
            try { Files.copy(file, fileBlock); } catch (IOException e) { e.printStackTrace();fileCreatorListener.onError("Copying block from local drive failed", clientID); }

            //no need for partition but make this variable true
            synchronized(fileInfo){
                isPartComplete = true;
            }
            sendBlocks();
        }
    }

    public void setEncryptKey(byte[] key){
        this.encryptKey = key;
    }



    //divides a file into multiple blocks
    public void partition(){
        Callable<Boolean> splitJob = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String outputDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified());
                SplitVideo splitVideo = new SplitVideo(file.getAbsolutePath(), outputDirPath, blockCount);
                return splitVideo.splitVideo();
            }
        };
        CallableCallback<Boolean> callback = new CallableCallback<Boolean>(){
            @Override
            public void complete(Boolean result) {
                // start MDFS
                if(result){
                    Logger.i(TAG, "File Split Complete.");
                    fileCreatorListener.statusUpdate("FIle Split Complete.");
                    synchronized(fileInfo){
                        isPartComplete = true;
                    }
                    sendBlocks();	// execute from thread that splits the file
                }
                else{
                    Logger.e(TAG, "Fail to split file.");
                    fileCreatorListener.onError("Fail to split file", clientID);
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(splitJob, callback));
        fileCreatorListener.statusUpdate("Partitioning file...");
    }



     //Can be started only after both topology discovery and file partition have completed <Br>
     // Each block is sent with one MDFSFileCreatorViaRsock. All MDFSFileCreatorViaRsock instances are placed <Br>
     //in a Queue, and MDFSFileCreatorViaRsock tries it best to complete all tasks in the queue.
     //Non-blocking call
    private synchronized void sendBlocks(){

        //first check if all the pre-works are done
        synchronized(fileInfo){
            if(!isTopComplete || !isPartComplete || isSending){
                return;
            }
        }

        isSending = true;

        // Read all blocks from the directory
        final Queue<MDFSBlockCreatorViaRsock> uploadQ = new ConcurrentLinkedQueue<MDFSBlockCreatorViaRsock>();

        //load the block directory
        final File fileDir = AndroidIOUtils.getExternalFile(edu.tamu.lenss.mdfs.Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified()));

        //array of blocks
        final File[] blocks = fileDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File f) {
                return f.isFile()
                        && f.length()>0
                        && f.getName().contains(file.getName());
            }
        });

        /*
         * This callable task is responsible for sending all the blocks. Block is sent one by one. <Br>
         * If one MDFSBlockCreation task fails, it will be inserted back to uploadQ and try again later
         * Non-blocking call
         */
        Callable<Boolean> distributeTask = new Callable<Boolean>() {
            private MDFSBlockCreatorViaRsock curBlock;

            private void createQueue(){
                String fName;
                for(File blockF : blocks){
                    fName = blockF.getName();
                    byte idx = Byte.parseByte(fName.substring((fName.lastIndexOf("_")+1)));   //idx = block number
                    uploadQ.add(new MDFSBlockCreatorViaRsock(blockF, filePathMDFS, fileInfo, idx, blockListener, permList, uniqueReqID, chosenNodes, clientID));
                }
            }

            @Override
            public Boolean call() throws Exception {
                createQueue();
                int reTryLimit = uploadQ.size() * edu.tamu.lenss.mdfs.Constants.FILE_CREATION_RETRIALS;
                synchronized(uploadQ){
                    while(!uploadQ.isEmpty() && reTryLimit > 0){
                        curBlock = uploadQ.poll(); //retrieve and remove
                        curBlock.setEncryptKey(encryptKey);
                        curBlock.start();
                        uploadQ.wait();	// Current thread releases the lock when wait() is called and Regain the lock once it is notified.
                        reTryLimit--;
                    }
                    if(uploadQ.isEmpty())
                        return true;
                    else
                        return false;
                }
            }

            /**
             * This listener is shared by all MDFSFileCreatorViaRsock thread. Need to be synchronized properly to avoid race condition. <Br>
             * uploadQ and curBlock are shared objects between different threads
             */
            MDFSBlockCreatorListenerViaRsock blockListener = new MDFSBlockCreatorListenerViaRsock(){
                private long sleepPeriod = Constants.IDLE_BTW_FAILURE;
                @Override
                public void statusUpdate(String status) {
                    Logger.i(TAG, status);
                }

                @Override
                public void onError(String error, String clientID) {
                    Logger.w(TAG, "Send block failure.  " + error);
                    fileCreatorListener.onError("Failed to send some blocks...cause: " + error, clientID);
                    try {
                        sleep(sleepPeriod);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized(uploadQ){
                        uploadQ.add(curBlock);
                        uploadQ.notify();
                    }
                }

                @Override
                public void onComplete(String msg, String clientID) {
                    synchronized(uploadQ){
                        sleepPeriod = Constants.IDLE_BTW_FAILURE;
                        curBlock.deleteBlockFile();
                        uploadQ.notify();
                    }
                }
            };
        };

        //this callback is called when all blocks have been sent
        //aka all calls to MDFSFileCreatorViaRsock has completed
        //aka, distributeTask callable is completed
        CallableCallback<Boolean> callback = new CallableCallback<Boolean>(){
            @Override
            public void complete(Boolean result) {
                if(result){
                    // Directory update
                    fileCreatorListener.statusUpdate("Complete sending all blocks of a file");
                    updateDirectory();
                }
                else{
                    fileCreatorListener.onError("Fail to distribute some blocks", clientID);
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(distributeTask, callback));
    }

    // this function basically populates chosenNodes list with GUIDs, and
    //decides n and k value for fragment distribution.
    //todo; this function will be changed later when ACL is implemented
    private void fetchTopologyAndChooseNodes(){

        if(permList.length==1 && permList[0].equals("WORLD")){

            //get peer guids who are running mdfs from GNS
            List<String> peerGUIDsListfromGNS = GNS.gnsServiceClient.getPeerGUIDs("MDFS", "default");
            if(peerGUIDsListfromGNS==null){ fileCreatorListener.onError("GNS Error! called getPeerGUIDs() and returned null.", clientID); return;}
            if(peerGUIDsListfromGNS.size()==0){ fileCreatorListener.onError("no other MDFS peer registered to GNS.", clientID); return; }

            //get all nearby vertices from Topology.java from rsockJavaAPI(OLSR) and put it in a list
            Set<String> peerGUIDsSetfromOLSR = Topology.getInstance(RSockConstants.intrfc_creation_appid).getVertices();
            if(peerGUIDsSetfromOLSR==null){fileCreatorListener.onError("    Topology fetch from OLSR Error! called getNeighbors() and returned null.", clientID);return;}
            if(peerGUIDsSetfromOLSR.size()==0){fileCreatorListener.onError("No neighbors found from OLSR", clientID);return;}
            List<String> peerGUIDsListfromOLSR = new ArrayList<String>(peerGUIDsSetfromOLSR);

            //cross the peerGUIDsListfromGNS and peerGUIDsListfromOLSR and get the common ones
            List<String> commonPeerGUIDs = new ArrayList<>();
            for(int i = 0; i < peerGUIDsListfromOLSR.size(); i++){
                if(peerGUIDsListfromGNS.contains(peerGUIDsListfromOLSR.get(i))){
                    commonPeerGUIDs.add(peerGUIDsListfromOLSR.get(i));
                }
            }

            //make a map<guid, pathWeight> for all nodes from ownGUID to each commonPeerGUIDs
            HashMap<String, Double> peerGUIDsWithWeights = new HashMap<>();

            //call Dijkstra from ownGUID to each of the commonPeerGUIDs and populate peerGUIDsWithWeights map
            for(int i=0; i< commonPeerGUIDs.size(); i++){
                double pathWeight = Topology.getInstance(RSockConstants.intrfc_creation_appid).getShortestPathWeight(GNS.ownGUID, commonPeerGUIDs.get(i));
                peerGUIDsWithWeights.put(commonPeerGUIDs.get(i), pathWeight);
            }

            //sort the map by ascending order(less weight to more weight)
            Map<String, Double> peerGUIDsWithWeightsSorted = sortByComparator(peerGUIDsWithWeights,true );

            //get first MAX_N_VAL(or less) number of nodes in a list(ones with less weight)
            int count = 0;
            for (Map.Entry<String, Double> pair: peerGUIDsWithWeightsSorted.entrySet()) {
                this.chosenNodes.add(pair.getKey());
                count++;
                if(count>= Constants.MAX_N_VAL){break;}
            }

        }

        populateChosenNodes();
    }


    //this function populates the choseNodes list, and selects n2, k2 values
    private void populateChosenNodes(){
        //print
        System.out.print("debuggg chosen nodes: " );
        for(String node: chosenNodes){System.out.print(node + " , ");}
        System.out.println();

        //set n2 , k2
        byte n2; if(chosenNodes.size() >= Constants.MAX_N_VAL){ n2 = (byte)Constants.MAX_N_VAL;} else{ n2 = (byte)chosenNodes.size();}
        byte k2 = (byte) Math.round(n2 * encodingRatio);
        fileInfo.setFragmentsParms(n2, k2);
        System.out.println("n2: " + n2 + "   k2: " + k2);
        this.metadata.setn2(n2);
        this.metadata.setk2(k2);
        if(n2 < 1 || k2 < 1){
            fileCreatorListener.onError("insufficient storage nodes. maybe permission denied Or N to K ratio is wrong!",clientID);
            return;
        }
        synchronized(fileInfo){
            isTopComplete = true;
        }
        sendBlocks();
    }

    //this function takes a list of group names, makes a call to edgekeeper, and returns a list of GUIDs
    //this list may return empty due to conversion failure(or due to disconnection etc)
    private List<String> groupToGUIDConvert(List<String> groups){
        //create client connection
        client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);

        //connect
        boolean connected = client.connect();

        //check if connection succeeded..if not, return with empty list
        //dont put in client.putInDTNQueue().
        if(!connected){ return new ArrayList<>();}

        //if connected, set socket read timeout(necessary here as we are expected reply in time)
        client.setSocketReadTimeout();

        //make EdgeKeeper object with cmd = GROUP_TO_GUID_CONV_REQUEST
        FileMetadata metadataReq = new FileMetadata(EdgeKeeperConstants.GROUP_TO_GUID_CONV_REQUEST, new Date().getTime(), EdgeKeeperConstants.getMyGroupName() , GNS.ownGUID, groups);

        //convert into json string
        String str = metadataReq.toBuffer(metadataReq);

        //create bytebuffer
        ByteBuffer sendBuf = ByteBuffer.allocate(str.length());
        sendBuf.order(ByteOrder.LITTLE_ENDIAN);
        sendBuf.clear();

        //put str in sendBuf and flip
        sendBuf.put(str.getBytes());
        sendBuf.flip();

        //send metadata req
        client.send(sendBuf);

        //get return
        ByteBuffer recvBuf = client.receive();

        //check if receive value is null or nah(can be null due to timeout)
        if(recvBuf==null){ client.close(); return new ArrayList<>();}

        //close client socket
        client.close();

        //get data from recvBuf and make string
        StringBuilder bd = new StringBuilder();
        while (recvBuf.remaining() > 0){ bd.append((char)recvBuf.get());}
        String str1 = bd.toString();

        //make metadata from str1
        FileMetadata metadataRet = FileMetadata.parse(str1);

        //check cmd = GROUP_TO_GUID_CONV_REPLY_SUCCESS, then success)
        if(metadataRet.command == EdgeKeeperConstants.GROUP_TO_GUID_CONV_REPLY_SUCCESS){
            return metadataRet.groupOrGUID;
        }else{
            return new ArrayList<>();
        }
    }

    //utility function
    //takes a hashmap and returns sorted map by value
    //order = true, results in ascending order, false= descending order
    private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap, final boolean order) {
        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if (order) { return o1.getValue().compareTo(o2.getValue()); }
                else { return o2.getValue().compareTo(o1.getValue()); }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> entry : list) { sortedMap.put(entry.getKey(), entry.getValue()); }
        return sortedMap;
    }



    //this function updates own local directory about the created file
    //and then calls updateEdgeKeeper() to send the update to edgekeeper
    private void updateDirectory() {

        //set file creator
        ServiceHelper serviceHelper = ServiceHelper.getInstance();

        //NewFileUpdate update = new NewFileUpdate(fileInfo);
        //serviceHelper.sendFileUpdate(update);  (commented by mohammad sagor/// reason: no need to send update dir commands to other nodes)

        // Update my local directory
        serviceHelper.getDirectory().addFile(fileInfo);

        //now update file metadata to EdgeKeeper
        updateEdgeKeeper();

        fileCreatorListener.onComplete("File creation complete.",clientID); //notifies handleCreateCommand
    }

    //this function sends the update to the edgekeeper
    //if it cannot connect to the edgekeeper, then it puts the data in DTNQueue
    private void updateEdgeKeeper() {

        //add information of all blocks/fragments that I(file creator) have(file creator has all blocks of all fragments)
        for(int i=0; i< blockCount; i++){
            for(int j=0; j<metadata.n2; j++){
                metadata.addInfo(GNS.ownGUID, Integer.toString(i), Integer.toString(j));
            }
        }

        //add permList to the metadata
        metadata.setPermission(permList);  ///permList contains WORLD, OWNER, GROUP:<group_name> or GUIDs

        //print metadata
        printMetadata(metadata);

        //create client connection and connect
        client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);
        boolean connected = client.connect();

        if(!connected){
            //if couldnt connect to EdgeKeeper, we put in DTN queue
            client.putInDTNQueue(metadata, 3);
            return;
        }

        //set socket read timeout(unnecessary here)
        client.setSocketReadTimeout();

        //get json string
        String str= metadata.toBuffer(metadata);

        //make sendBuf
        ByteBuffer sendBuf = ByteBuffer.allocate(str.length());
        sendBuf.order(ByteOrder.LITTLE_ENDIAN);
        sendBuf.clear();

        //put data in sendBuf
        sendBuf.put(str.getBytes());
        sendBuf.flip();

        //send
        client.send(sendBuf);

        //sleep 1 second
        try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

        //close connection
        client.close();
    }

    public void printMetadata(FileMetadata metadata){
        System.out.println("metadataaaa command return: " + metadata.command);
        System.out.println("metadataaaa chosennodes size: " + metadata.getAllUniqueFragmentHolders().size());

        //print all unique nodes
        List<String> allUNiqueNodes = metadata.getAllUniqueFragmentHolders();
        System.out.print("metadataaa all unique nodes are : ");
        for(int i=0; i< allUNiqueNodes.size(); i++){System.out.print(allUNiqueNodes.get(i) + " ");}
        System.out.println();
        System.out.println();

        //print all blocksnums by each node
        for(int i=0; i< allUNiqueNodes.size();i++){
            List<String> blocksnums = metadata.getBlockNumbersHeldByNode(allUNiqueNodes.get(i));
            System.out.print("metadataaa " +  allUNiqueNodes.get(i) + " has blocknumbers ");
            for(int j=0; j< blocksnums.size(); j++){System.out.print(blocksnums.get(j) + " ");}
            System.out.println();
        }
        System.out.println();
        System.out.println();

        //print all fragment number by each user
        for(int i=0; i< allUNiqueNodes.size(); i++){
            List<String> blocksnums = metadata.getBlockNumbersHeldByNode(allUNiqueNodes.get(i));
            //print num of blocks
            System.out.println("metadataaaa num of blocks: " + metadata.numOfBlocks + " " + blocksnums);
            for(int j=0; j< blocksnums.size(); j++){
                List<String> frags = metadata.getFragmentListByNodeAndBlockNumber(allUNiqueNodes.get(i), blocksnums.get(j));
                System.out.println("metadataaa " + allUNiqueNodes.get(i) + " has blocknum " + blocksnums.get(j) + " containing fragment " + frags);

            }
        }
        System.out.println();
        System.out.println();

        //print all the nodes containing a block
        int blocknumba = 0;
        List<String> nodes = metadata.getNodesContainingFragmentsOfABlock(Integer.toString(blocknumba));
        System.out.println("metadataaa size of nodes containing blocknum " + blocknumba + " is: " + nodes.size());
        System.out.print("metadataaa nodes containing blocknum " + blocknumba + " are: ");
        for(int i=0; i< nodes.size(); i++){
            System.out.print(nodes.get(i) + " ");
        }
        System.out.println();
        System.out.println();
    }



    /*
     * Default MDFSBlockCreatorListenerViaRsock. Do nothing.
     */
    private MDFSBlockCreatorListenerViaRsock fileCreatorListener = new MDFSBlockCreatorListenerViaRsock(){
        @Override
        public void statusUpdate(String status) {}

        @Override
        public void onError(String error, String clientID) {}

        @Override
        public void onComplete(String msg, String clientID) {}

    };

}

