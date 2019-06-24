package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;


import android.os.Environment;

import com.google.common.io.Files;

import org.sat4j.pb.tools.INegator;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.NewFileUpdate;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger.LogFileInfo.LogFileName;
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




//this class object is only made in homescreen.java class.
//this class is used only to create file and distribute fragments to other nodes using rsock, instead of tcp.
//most of the codes are copied from MDFSFileCreator.java class.
public class MDFSFileCreatorViaRsock {
    private static final String TAG = MDFSFileCreatorViaRsock.class.getSimpleName();
    private File file;
    private MDFSFileInfo fileInfo;
    private byte[] encryptKey;
    private int blockCount;
    private double encodingRatio;
    private boolean isTopComplete = false;
    private boolean isPartComplete = false;
    private boolean deleteWhenComplete = false;
    private boolean isSending = false;
    List<String> chosenNodes;
    EdgeKeeperMetadata metadata;


    //this constructor is called from the application side HomeScreen.java class
    public MDFSFileCreatorViaRsock(File f, long maxBlockSize, double encodingRatio) {  //RSOCK
        this.file = f;
        this.encodingRatio = encodingRatio;
        this.blockCount = (int)Math.ceil((double)file.length()/maxBlockSize);
        this.fileInfo = new MDFSFileInfo(file.getName(), file.lastModified());
        this.fileInfo.setFileSize(file.length());
        this.fileInfo.setNumberOfBlocks((byte)blockCount);
        Logger.w(TAG, "Split file " + f.getName() + " to " + blockCount + " blocks");
        this.metadata = new EdgeKeeperMetadata('d', GNS.ownGUID, fileInfo.getCreatedTime(), fileInfo.getFileName(), blockCount, (byte)0,(byte) 0);  //d = deposit
    }


    //this function is called from HomeScreen.java class
    //this function sets a listener(listener is used to update app activity )
    public void setListener(MDFSBlockCreatorListenerViaRsock fileList){
        fileCreatorListener = fileList;
    }


    public void start(){

        //first fetch topology from
        fetchTopology();

        if(blockCount > 1){
            partition();
        }
        else{
            // Single block
            File fileBlock = IOUtilities.createNewFile(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                    MDFSFileInfo.getFileDirPath(file.getName(), file.lastModified()) + File.separator +
                    MDFSFileInfo.getBlockName(file.getName(), (byte)0));
            try {
                Files.copy(file, fileBlock);
            } catch (IOException e) {
                e.printStackTrace();
                fileCreatorListener.onError("Copy block fails");
            }

            synchronized(fileInfo){
                isPartComplete = true;
            }
            sendBlocks();
        }
    }

    public void setEncryptKey(byte[] key){
        this.encryptKey = key;
    }


    public void partition(){
        Callable<Boolean> splitJob = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String outputDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                        + edu.tamu.lenss.mdfs.Constants.DIR_ROOT + File.separator
                        + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified());
                SplitVideo splitVideo = new SplitVideo(file.getAbsolutePath(), outputDirPath, blockCount);
                return splitVideo.splitVideo();
            }
        };
        CallableCallback<Boolean> callback = new CallableCallback<Boolean>(){
            @Override
            public void complete(Boolean result) {
                // start MDFS
                if(result){
                    Logger.i(TAG, "Video Split Complete.");
                    fileCreatorListener.statusUpdate("Video Split Complete.");
                    synchronized(fileInfo){
                        isPartComplete = true;
                    }
                    sendBlocks();	// execute from thread that splits the file
                }
                else{
                    Logger.e(TAG, "Fail to split video.");
                    fileCreatorListener.onError("Fail to split video");
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(splitJob, callback));
        fileCreatorListener.statusUpdate("Partitioning video...");
    }


    /**
     * Can be started only after both topology discovery and file partition have completed <Br>
     * Each block is sent with one MDFSFileCreatorViaRsock. All MDFSFileCreatorViaRsock instances are placed <Br>
     * in a Queue, and MDFSFileCreatorViaRsock tries it best to complete all tasks in the queue.
     * Non-blocking call
     */
    private synchronized void sendBlocks(){
        synchronized(fileInfo){
            if(!isTopComplete || !isPartComplete || isSending){
                return;
            }
        }
        isSending = true;
        Logger.i(TAG, "Start to send blocks");
        fileCreatorListener.statusUpdate("Start to send blocks");

        // Read all blocks from the directory
        final Queue<MDFSBlockCreatorViaRsock> uploadQ = new ConcurrentLinkedQueue<MDFSBlockCreatorViaRsock>();

        final File fileDir = AndroidIOUtils.getExternalFile(edu.tamu.lenss.mdfs.Constants.DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(file.getName(), file.lastModified()));

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
                    uploadQ.add(new MDFSBlockCreatorViaRsock(blockF, fileInfo, idx, blockListener, chosenNodes, metadata));
                }
            }

            @Override
            public Boolean call() throws Exception {
                createQueue();
                int reTryLimit = uploadQ.size() * edu.tamu.lenss.mdfs.Constants.FILE_CREATION_RETRIALS;
                synchronized(uploadQ){
                    while(!uploadQ.isEmpty() && reTryLimit > 0){
                        curBlock = uploadQ.poll();
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
                private long sleepPeriod = edu.tamu.lenss.mdfs.Constants.IDLE_BTW_FAILURE;
                @Override
                public void statusUpdate(String status) {
                    Logger.i(TAG, status);
                }

                @Override
                public void onError(String error) {
                    Logger.w(TAG, "Send block failure.  " + error);
                    fileCreatorListener.onError("Retrying...  " + error);
                    try {
                        sleep(sleepPeriod);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized(uploadQ){
                        sleepPeriod += edu.tamu.lenss.mdfs.Constants.IDLE_BTW_FAILURE;
                        uploadQ.add(curBlock);
                        uploadQ.notify();
                    }
                }

                @Override
                public void onComplete() {
                    synchronized(uploadQ){
                        sleepPeriod = edu.tamu.lenss.mdfs.Constants.IDLE_BTW_FAILURE;
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
                    Logger.i(TAG, "Complete sending all blocks of a file");
                    fileCreatorListener.statusUpdate("Complete sending all blocks of a file");
                    updateDirectory();
                }
                else{
                    Logger.e(TAG, "Fail to distribute some blocks");
                    fileCreatorListener.onError("Fail to distribute some blocks");
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(distributeTask, callback));
    }

    //this function fetches a list of nodes from rsock java api (which fetches it from olsrd)
    //about who are the two hops nodes nearby. Then also fetches a list of nodes from GNS
    // about who are the nodes currently running mdfs.
    //then croses the two lists and choses a subset of the nodes as candidate nodes to hold the file fragments.
    private void fetchTopology(){
        //first get my own guid
        String myGUID = GNS.ownGUID;

        //second get peer guids who are running mdfs from GNS
        List<String> peerGUIDsListfromGNS = GNS.gnsServiceClient.getPeerGUIDs("MDFS", "default");
        if(peerGUIDsListfromGNS==null){ System.out.println("GNS Error! called getPeerGUIDs() and returned null."); System.exit(0);}

        //third,get all nearby vertices from Topology.java from rsockJavaAPI(OLSR) and put it in a list
        Set<String> peerGUIDsSetfromOLSR = Topology.getInstance(Constants.intrfc_creation_appid).getVertices();  //todo: check if it returns null first
        List<String> peerGUIDsListfromOLSR = new ArrayList<String>();
        if(peerGUIDsListfromOLSR!=null){
            Iterator<String> it = peerGUIDsSetfromOLSR.iterator();
            while(it.hasNext()){ peerGUIDsListfromOLSR.add(it.next()); }
        }else{
            fileCreatorListener.onError("no neighbors found from OLSR");
            return;
        }


        //fourth, cross the peerGUIDsListfromGNS and peerGUIDsListfromOLSR and get the common ones
        List<String> commonPeerGUIDs = new ArrayList<>();
        for(int i = 0; i < peerGUIDsListfromOLSR.size(); i++){
            if(peerGUIDsListfromGNS.contains(peerGUIDsListfromOLSR.get(i))){
                commonPeerGUIDs.add(peerGUIDsListfromOLSR.get(i));
            }
        }

        //fifth, make a map<guid, pathWeight> for all nodes from ownGUID to each commonPeerGUIDs
        HashMap<String, Double> peerGUIDsWithWeights = new HashMap<>();

        //sixth, call Dijkstra from ownGUID to each of the commonPeerGUIDs and populate peerGUIDsWithWeights map
        for(int i=0; i< commonPeerGUIDs.size(); i++){
           double pathWeight = Topology.getInstance(Constants.intrfc_creation_appid).getShortestPathWeight(myGUID, commonPeerGUIDs.get(i));
            peerGUIDsWithWeights.put(commonPeerGUIDs.get(i), pathWeight);
        }

        //seventh, sort the map by ascending order(less weight to more weight)
        Map<String, Double> peerGUIDsWithWeightsSorted = sortByComparator(peerGUIDsWithWeights,true );

        //eighth, get first MAX_N_VAL(or less) number of nodes in a list(ones with less weight)
        this.chosenNodes = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Double> pair: peerGUIDsWithWeightsSorted.entrySet()) {
            this.chosenNodes.add(pair.getKey());
            count++;
            if(count>= Constants.MAX_N_VAL){break;}
        }

        //print
        System.out.print("debuggg chosen nodes: " );
        for(String node: chosenNodes){System.out.print(node + " , ");}
        System.out.println();

        byte n2; if(chosenNodes.size() >= Constants.MAX_N_VAL){ n2 = (byte)Constants.MAX_N_VAL;} else{ n2 = (byte)chosenNodes.size();}
        byte k2 = (byte) Math.round(n2 * encodingRatio);
        fileInfo.setFragmentsParms(n2, k2);
        System.out.println("n2: " + n2 + "   k2: " + k2);
        this.metadata.setn2(n2);
        this.metadata.setk2(k2);
        fileCreatorListener.statusUpdate("n2:" + n2 + " k2:" + k2);
        if(n2 <= 1 || k2 < 1){
            fileCreatorListener.onError("Insufficeint storage nodes.");
            return;
        }
        synchronized(fileInfo){
            isTopComplete = true;
        }
        sendBlocks();
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



    private void updateDirectory() {
        try { sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }

        ServiceHelper serviceHelper = ServiceHelper.getInstance();
        fileInfo.setCreator(serviceHelper.getNodeManager().getMyMAC());
        NewFileUpdate update = new NewFileUpdate(fileInfo);

        serviceHelper.sendFileUpdate(update);
        Logger.w(TAG, "File Id: " + fileInfo.getCreatedTime());
        // Update my directory as well
        serviceHelper.getDirectory().addFile(fileInfo);

        if(deleteWhenComplete)
            file.delete();

        //now update file metadata to EdgeKeeper
        updateEdgeKeeper();

        fileCreatorListener.onComplete(); //notifies HomeScreen "File Creation Complete"
    }

    //this function sends the
    private void updateEdgeKeeper() {  //todo: only report frag that I have

        //set command
        this.metadata.setCommand('d');

        //add information of all blocks/fragments that I(file creator) have(file creator has all blocks of all fragments)
        for(int i=0; i< blockCount; i++){
            for(int j=0; j<metadata.n2; j++){
                metadata.addInfo(GNS.ownGUID, Integer.toString(i), Integer.toString(j));
            }
        }

        //print metadata
        printMetadata(metadata);

        //create client connection and connect
        client client = new client(Constants.dummy_EdgeKeeper_ip, Constants.dummy_EdgeKeeper_port);
        client.connect();

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

    public void printMetadata(EdgeKeeperMetadata metadata){
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
        public void onError(String error) {}

        @Override
        public void onComplete() {}

    };

}

