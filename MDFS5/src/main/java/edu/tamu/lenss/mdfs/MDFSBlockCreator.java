package edu.tamu.lenss.mdfs;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.lenss.mdfs.crypto.FragmentInfo;
import edu.tamu.lenss.mdfs.crypto.MDFSEncoder;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.handler.TopologyHandler.TopologyListener;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.network.TCPConnection;
import edu.tamu.lenss.mdfs.network.TCPSend;
import edu.tamu.lenss.mdfs.placement.PlacementHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.JCountDownTimer;
import edu.tamu.lenss.mdfs.utils.Logger;


public class MDFSBlockCreator {
    private static final String TAG = MDFSBlockCreator.class.getSimpleName();
    private byte blockIdx;

    private int networkSize;
    private byte k2, n2;
    private File blockFile;
    private ServiceHelper serviceHelper;
    private List<NodeInfo> nodInfoList = new ArrayList<NodeInfo>();
    private MDFSFileInfo fileInfo;
    private boolean isOptComplete, isEncryptComplete;
    private AtomicInteger fragCounter;
    private final BlockCreationLog logger = new BlockCreationLog();

    public MDFSBlockCreator(File file, MDFSFileInfo info, byte blockIndex, MDFSBlockCreatorListener lis) {
        this.blockIdx = blockIndex;
        this.blockFile = file;
        this.listener = lis;
        this.serviceHelper = ServiceHelper.getInstance();
        this.fileInfo = info;
        this.k2 = fileInfo.getK2();
        this.n2 = fileInfo.getN2();
        this.fragCounter = new AtomicInteger();
    }

    /**
     * discoverTopology() is called first. Once it is complete, encrypteFile()
     * and optimizePlacement() <br>
     * are started simultaneously. Both these subroutines need n,k values. Once
     * both <br>
     * encrypteFile() and optimizePlacement() are complete, distributeFragment()
     * is started. <br>
     * Once distributeFragment() is complete, directoryUpdate() is triggered.<br>
     * Non-blocking call
     */
    public void start() {
        discoverTopology();
        setUpTimer();
        serviceHelper.executeRunnableTask(new Runnable() {
            @Override
            public void run() {
                encryptFile();
            }
        });
    }

    private TopologyListener topologyListener;
    /**
     * Non-blocking call
     */
    private void discoverTopology() {
        topologyListener = new TopologyListener() {
            @Override
            public void onError(String msg) {
                listener.onError("Block Topology Disovery Fails. Please try again later");
                Logger.e(TAG, msg);
            }

            @Override
            public void onComplete(List<NodeInfo> topList) {
                logger.topEnd = System.currentTimeMillis();
                nodInfoList = topList;
                for (NodeInfo info : topList) {
                    Logger.v(TAG, "Receive NodeInfo from " + info.getSource());
                }
                listener.statusUpdate("Block Topology Discovery Complete.");

                networkSize = nodInfoList.size();
                serviceHelper.executeRunnableTask(new Runnable() {
                    @Override
                    public void run() {
                        optimizePlacement();
                    }
                });
            }
        };

        listener.statusUpdate("Starting topology Discovery for block " + blockIdx);
        logger.topStart = System.currentTimeMillis();
        serviceHelper.startTopologyDiscovery(topologyListener, Constants.TOPOLOGY_DISCOVERY_RETRIAL_TIMEOUT);
    }


    private byte[] encryptKey;
    /**
     * Application needs to provide a symmetric encryption key
     * @param key
     */
    public void setEncryptKey(byte[] key){
        this.encryptKey = key;
    }

    //private List<KeyShareInfo> keyShares; // Cache the key fragments
    /**
     * Blocking call
     */
    private void encryptFile() {
        isEncryptComplete = false;
        logger.encryStart = System.currentTimeMillis();
        if(blockFile == null || !blockFile.exists())
            return;

        MDFSEncoder encoder = new MDFSEncoder(blockFile, n2, k2);
        if(encryptKey != null)
            encoder.setKey(encryptKey);
        List<FragmentInfo> fragInfos = encoder.encodeNow();

        //if (!encoder.encode()) {
        if(fragInfos == null) {
            listener.onError("File Encryption Failed");
            return;
        }
        logger.encryStop = System.currentTimeMillis();


        // Store the file fragments in local SDCard
        File fragsDir = AndroidIOUtils.getExternalFile(MDFSFileInfo
                .getBlockDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime(),	blockIdx));

        HashSet<Byte> frags = new HashSet<Byte>();
        // Write file fragments to SD Card
        for (FragmentInfo frag : fragInfos) {
            //Logger.v(TAG, "Directory: " + fragsDir.getPath() + " File Name: " + MDFSFileInfo.getFragName(fileInfo.getFileName(), blockIdx, frag.getFragmentNumber()));
            File tmp = IOUtilities.createNewFile(fragsDir, MDFSFileInfo.getFragName(fileInfo.getFileName(), blockIdx, frag.getFragmentNumber()));

            if (tmp != null && IOUtilities.writeObjectToFile(frag, tmp)) {
                frags.add(frag.getFragmentNumber());
            }
        }
        serviceHelper.getDirectory().addBlockFragments(fileInfo.getCreatedTime(), blockIdx, frags);
        listener.statusUpdate("Encryption Complete");
        Logger.i(TAG + " encryptFile()", "Encryption Complete");
        isEncryptComplete = true;
        distributeFragments();
    }

    private List<Long> fileStorages;
    /**
     * Blocking function call
     */
    private void optimizePlacement() {
        listener.statusUpdate("Start Placement Optimization for block " + blockIdx);
        isOptComplete = false;
        logger.optStart = System.currentTimeMillis();


        /////////////////////////////////////////////////////////////////
        /**
         * To be replaced. SimplePlacement
         */
		/*
		fileStorages = new ArrayList<Long>();
		for(int i=0; i<n2; i++){
			fileStorages.add(nodInfoList.get(i).getSource());
		}*/

        if(nodInfoList.size() < n2){
            listener.onError("Insufficient Storage nodes to distribute fragments");
            return;
        }

        PlacementHelper helper = new PlacementHelper(new HashSet<NodeInfo>(
                nodInfoList), n2, k2);
        helper.findOptimalLocations();
        fileStorages = new ArrayList<Long>(helper.getAllocation().keySet());

        //////////////////////////////////////////////////////////////////

        StringBuilder str = new StringBuilder("Block Storages:");
        for (Long ip : fileStorages) {
            str.append(IOUtilities.long2Ip(ip) + ", ");
        }
        Logger.v(TAG, str.toString());
        listener.statusUpdate("Placement Optimization Complete");
        isOptComplete = true;
        distributeFragments();
    }
    private JCountDownTimer jTimer;

    private void setUpTimer(){
        jTimer = new JCountDownTimer(Constants.FRAGMENT_CREATION_TIMEOUT_INTERVAL, Constants.FRAGMENT_CREATION_TIMEOUT_INTERVAL){
            private boolean isFinished = false;	// Ensure that isFinish() will only execute once
            @Override
            public synchronized void onFinish() {
                if(isFinished)
                    return;
                if (fragCounter.get() > k2 || (fragCounter.get()==k2 && n2 == k2 )){
                    Logger.v(TAG, fragCounter.get() + " fragments were distributed");
                    logger.distStop = System.currentTimeMillis();
                    listener.onComplete();
                    //updateDirectory();
                }
                else{
                    // Delete fragments
                    DeleteFile deleteFile = new DeleteFile();
                    deleteFile.setFile(fileInfo.getFileName(), fileInfo.getCreatedTime());
                    ServiceHelper.getInstance().deleteFiles(deleteFile);
                    listener.onError("Fail to distribute file fragments. " +
                            "Only " + fragCounter.get() + " were successfully sent. Please try again later");
                }
                isFinished = true;
            }

            @Override
            public synchronized void onTick(long millisUntilFinished) {
            }
        };
    }

    /**
     * Can only start after both optimizePlacement() and encryptFile() have
     * completed
     */
    private void distributeFragments() {
        if (!isOptComplete || !isEncryptComplete)
            return;
        Logger.d(TAG, "distributeFragments() is called");
        // Scan through all files in the folder and upload them
        File fileFragDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileInfo.getFileName(),
                fileInfo.getCreatedTime(), blockIdx));
        if(!fileFragDir.exists()){
            Logger.e(TAG, "Can't find fragments directory of the block");
            listener.onError("No block directory");
            return;
        }
        File[] files = fileFragDir.listFiles();
        long destNode;

        logger.distStart = System.currentTimeMillis();
        fragCounter.set(0);
        Iterator<Long> nodesIter = fileStorages.iterator();
        for (File f : files) {
            if (f.getName().contains("__frag__")) {
                // Find the fragment Number
                if (nodesIter != null && nodesIter.hasNext()) {
                    destNode = nodesIter.next();
                    if (destNode == ServiceHelper.getInstance().getNodeManager().getMyIP()){
                        fragCounter.incrementAndGet();
                        continue; // Don't need to send to myself again
                    }
                    serviceHelper.executeRunnableTask(new FragmentUploader(f, fileInfo.getCreatedTime(), destNode, !nodesIter.hasNext()));
                }
            }
        }

        jTimer.start();
        listener.statusUpdate("Distributing block fragments");
    }



    private void writeLog(){
        // Log data
        //AndroidDataLogger dataLogger = ServiceHelper.getInstance().getDataLogger();
        long nodeMac = ServiceHelper.getInstance().getNodeManager().getMyMAC();
        StringBuilder str = new StringBuilder();
        str.append(System.currentTimeMillis() + ", ");
        str.append(fileInfo.getCreator() + ", ");
        str.append(fileInfo.getFileName() + ", ");
        str.append(fileInfo.getFileSize() + ", ");
        str.append(networkSize + ", ");
        str.append(fileInfo.getN2() + ", ");
        str.append(fileInfo.getK2() + ", ");
        str.append(logger.getDiff(logger.topStart, logger.topEnd-Constants.TOPOLOGY_DISCOVERY_TIMEOUT)+ ", ");
        str.append(logger.getDiff(logger.optStart, logger.optStop) + ", ");
        str.append(logger.getDiff(logger.encryStart, logger.encryStop) + ", ");
        str.append(logger.getDiff(logger.distStart, logger.distStop) + "\n");

        String tmp="	";
        str.append(tmp);

        tmp="	";
        for(Long i : fileStorages)
            tmp += IOUtilities.long2Ip(i) + ",";
        tmp += "\n";
        str.append(tmp);
        //dataLogger.appendSensorData(LogFileName.FILE_CREATION, str.toString());

        // Topology Discovery
        str.delete(0, str.length()-1);
        str.append(nodeMac + ", ");
        str.append("TopologyDisc, ");
        str.append(networkSize + ", " + n2 + ", " + k2 + ", ");
        str.append(logger.topStart + ", ");
        str.append(logger.getDiff(logger.topStart, logger.topEnd-Constants.TOPOLOGY_DISCOVERY_TIMEOUT ) + ", ");
        str.append("\n");
        //dataLogger.appendSensorData(LogFileName.TIMES, str.toString());

        // Optimization
        str.delete(0, str.length()-1);
        str.append(nodeMac + ", ");
        str.append("Optimization, ");
        str.append(networkSize + ", " + n2 + ", " + k2 + ", ");
        str.append(logger.optStart + ", ");
        str.append(logger.getDiff(logger.optStart, logger.optStop) + ", ");
        str.append("\n");
        //dataLogger.appendSensorData(LogFileName.TIMES, str.toString());


        // File Distribution
        str.delete(0, str.length()-1);
        str.append(nodeMac + ", ");
        str.append("FileDistribution, ");
        str.append(networkSize + ", " + n2 + ", " + k2 + ", ");
        str.append(fileInfo.getFileSize() + ", ");
        str.append(logger.distStart + ", ");
        str.append(logger.getDiff(logger.distStart, logger.distStop) + ", ");
        str.append("\n\n");
        //dataLogger.appendSensorData(LogFileName.TIMES, str.toString());
    }

    protected boolean deleteBlockFile(){
        return blockFile.delete();
    }

    /**
     * Each fragment is uploaded by one thread. MDFSBlockCreator ends (onFinish())
     * when all fragments have been successfully uploaded or timeout occurs. As a result,
     * onFinish() may be called two times.
     * @author Jay
     *
     */
    class FragmentUploader implements Runnable {
        private File fileFrag;
        private long destId;
        private long fileCreatedTime;
        private byte blockIndex;
        private byte fragmentIndex;

        /**
         *
         * @param frag
         *            The Fragment File stored on SDCard
         * @param fileCreationTime
         *            The file_creation time of the original plain file
         * @param dest
         *            Destination
         * @param last
         *            Is this is the last fragment to send? It is used to
         *            trigger the next step, directoryUpdate();
         */
        public FragmentUploader(File frag, long fileCreationTime, long dest, boolean last) {
            this.fileFrag = frag;
            this.destId = dest;
            this.fileCreatedTime = fileCreationTime;
            this.blockIndex = parseBlockNum(frag.getName());
            this.fragmentIndex = parseFragNum(frag.getName());
        }

        private byte parseBlockNum(String fName){
            String str = fName.substring(0, fName.lastIndexOf("__frag__"));
            str = str.substring(str.lastIndexOf("_")+1);
            return Byte.parseByte(str.trim());
        }
        private byte parseFragNum(String fName) {
            return Byte.parseByte(fName.substring(fName.lastIndexOf("_") + 1).trim());
        }

        @Override
        public void run() {
            Logger.d(TAG, "FragmentUploader thread is running");
            boolean success = false;
            try {
                //create tcp
                TCPSend send = TCPConnection.creatConnection(IOUtilities.long2Ip(destId));
                if (send == null) {
                    Logger.e(TAG, "Connection Failed");
                    return;
                }

                // Handshake by sending header
                FragmentTransferInfo header = new FragmentTransferInfo(fileInfo.getFileName(), fileCreatedTime, blockIndex, fragmentIndex, FragmentTransferInfo.REQ_TO_SEND);
                header.setNeedReply(true);
                ObjectOutputStream oos = new ObjectOutputStream(send.getOutputStream());
                oos.writeObject(header);

                //receive response
                ObjectInputStream ois = new ObjectInputStream(send.getInputStream());
                header = (FragmentTransferInfo) ois.readObject();

                //check if receiver is good to receive fragment
                if (!header.isReady()) {
                    Logger.e(TAG, "Destination reject to receive");
                    return;
                }

                //doing actual send
                byte[] mybytearray = new byte[Constants.TCP_COMM_BUFFER_SIZE];
                int readLen = 0;
                FileInputStream fis = new FileInputStream(fileFrag);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataOutputStream out = send.getOutputStream();
                while ((readLen = bis.read(mybytearray, 0, Constants.TCP_COMM_BUFFER_SIZE)) >= 0) {
                    out.write(mybytearray, 0, readLen);
                }

                send.close();
                fis.close();
                ois.close();
                oos.close();
                success = true;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                jTimer.cancel();	// Reset the timer
                if (!success) {
                    jTimer.start();
                }
                else if(fragCounter.incrementAndGet() >= n2){	// all fragments have been sent
                    jTimer.onFinish();
                }
                else{
                    jTimer.start();
                }
            }
        }
    }

    /*
     * Default MDFSBlockCreatorListener. Do nothing.
     */
    private MDFSBlockCreatorListener listener = new MDFSBlockCreatorListener(){
        @Override
        public void statusUpdate(String status) {}

        @Override
        public void onError(String error) {}

        @Override
        public void onComplete() {}

    };

    public interface MDFSBlockCreatorListener {
        public void statusUpdate(String status);

        public void onError(String error);

        public void onComplete();
    }

    private class BlockCreationLog{
        public BlockCreationLog(){}
        public long topStart, topEnd, encryStart, encryStop,
                optStart, optStop, distStart, distStop;
        public String getDiff(long l1, long l2){
            return Long.toString(Math.abs(l2-l1));
        }
    }
}
