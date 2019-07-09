/*
package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.io.Files;

import android.os.Environment;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.EdgeKeeper.FileMetadata;
import edu.tamu.lenss.mdfs.EdgeKeeper.client;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.GNS.GNSConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.CallableTask;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger.LogFileInfo.LogFileName;
import edu.tamu.lenss.mdfs.utils.CallableTask.CallableCallback;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mp4process.MergeVideo;

//rsock imports
import edu.tamu.lenss.mdfs.MDFSBlockRetrieverViaRsock.BlockRetrieverListenerViaRsock;


//this class object is only made in DirectoryList.java class.
//this class is used only to retrieve file from other nodes using rsock, instead of tcp.
//most of the codes are copied from MDFSFileRetriever.java class.
public class MDFSFileRetrieverViaRsock {
    private static final String TAG = MDFSFileRetrieverViaRsock.class.getSimpleName();
    private byte[] decryptKey;
    private MDFSFileInfo fileInfo;
    private FileMetadata metadata;

    public MDFSFileRetrieverViaRsock(MDFSFileInfo fInfo) {
        this.fileInfo = fInfo;
    }

    public void start(){
        fetchFileMetadataFromEdgeKeeper();

        //check if metadata is valid
        if(metadata.command == EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST){
            fileListener.onError("Failed, EdgeKeeper has no metadata for this file.", fileInfo);
        }else if(metadata.command == EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_PERMISSIONDENIED){
            fileListener.onError("Failed, File permission denied", fileInfo);
        }else if(metadata.command == EdgeKeeperConstants.EDGEKEEPER_CONNECTION_FAILED){
            fileListener.onError("Failed, could not connect to EdgeKeeper.", fileInfo);
        }else if(metadata.command ==  EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_SUCCESS){
            retrieveBlocks();
        }

    }

    private void fetchFileMetadataFromEdgeKeeper() {
        //create client connection
        client client = new client(EdgeKeeperConstants.dummy_EdgeKeeper_ip, EdgeKeeperConstants.dummy_EdgeKeeper_port);

        //connect
        boolean connected = client.connect();

        //if could not connect to edgeKeeper
        if(!connected){
            //return with dummy metadata with cmd = EDGEKEEPER_CONNECTION_FAILED and other dummy information
            //dont put it in client.putInDTNQueue() function
            this.metadata = new FileMetadata(EdgeKeeperConstants.EDGEKEEPER_CONNECTION_FAILED, EdgeKeeperConstants.getMyGroupName(), GNSConstants.dummyGUID, GNSConstants.dummyGUID, 0000, new String[1], new Date().getTime(), "dummyuniqueid", "filename", 0, 0, "filePathMDFS",  0, (byte)0, (byte)0); //dummy metadata
            return;
        }

        //if connected set socket read timeout
        client.setSocketReadTimeout();

        //make request metadata object
        FileMetadata metadataReq = new FileMetadata(EdgeKeeperConstants.METADATA_WITHDRAW_REQUEST, EdgeKeeperConstants.getMyGroupName(), GNS.ownGUID, fileInfo.getCreatedTime(), new Date().getTime(), fileInfo.getFileName());

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

        //check if receive value is null or nah(can be null due to timeout),
        //then return dummy object
        if(recvBuf==null){
            client.close();
            return;
        }

        //close client socket
        client.close();

        //get data from recvBuf and make string
        StringBuilder bd = new StringBuilder();
        while (recvBuf.remaining() > 0){ bd.append((char)recvBuf.get());}
        String str1 = bd.toString();

        //make metadata from str1
        this.metadata = FileMetadata.parse(str1);

        printMetadata(metadata);

    }

    //testing purpose
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

    public void setDecryptKey(byte[] key){
        this.decryptKey = key;
    }

    public void setListener(BlockRetrieverListenerViaRsock list){
        fileListener = list;
    }


    //this function calls new MDFSBlockRetrieverViaRsock()
    private void retrieveBlocks(){
        Logger.i(TAG, "Start downloading blocks. Total " + fileInfo.getNumberOfBlocks() + " blocks.");
        fileListener.statusUpdate("Start downloading blocks. Total " + fileInfo.getNumberOfBlocks() + " blocks.");
        final Queue<Byte> downloadQ = new ConcurrentLinkedQueue<Byte>();
        for(byte i=0; i < fileInfo.getNumberOfBlocks(); i++){
            downloadQ.add(i);
        }

        Callable<Boolean> downloadTask = new Callable<Boolean>() {
            private MDFSBlockRetrieverViaRsock curBlock;
            private byte curBlockIdx;

            @Override
            public Boolean call() throws Exception {
                int reTryLimit = downloadQ.size() * Constants.FILE_RETRIEVAL_RETRIALS;
                synchronized(downloadQ){
                    while(!downloadQ.isEmpty() && reTryLimit > 0){
                        curBlockIdx = downloadQ.poll();
                        curBlock = new MDFSBlockRetrieverViaRsock(fileInfo, curBlockIdx, metadata);  //RSOCK
                        curBlock.setListener(blockListener);
                        curBlock.setDecryptKey(decryptKey);
                        curBlock.start();
                        downloadQ.wait();	// one block at a time..execution of this thread waits here until current block is done(succeed or error)
                        reTryLimit--;
                    }
                    Logger.i(TAG, "Finish downloadQ");
                    if(downloadQ.isEmpty())
                        return true;
                    else
                        return false;
                }
            }


            //This listener is shared by all MDFSBlockRetrieverViaRsock thread. Need to be synchronized properly to avoid race condition <Br>
            //downloadQ and curBlock are the shared objects between different threads.notify() needs to be called from different thread. Make sure this is the case when callback function makes call
            BlockRetrieverListenerViaRsock blockListener = new BlockRetrieverListenerViaRsock(){
                private long sleepPeriod = Constants.IDLE_BTW_FAILURE;
                @Override
                public void onError(String error, MDFSFileInfo fInfo) {
                    Logger.w(TAG, "Retrieve block failure.  " + error);
                    fileListener.onError("Retrieve block unsuccessful. Retrying...  " + error, fInfo);
                    try {
                        Thread.sleep(sleepPeriod);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized(downloadQ){
                        sleepPeriod += Constants.IDLE_BTW_FAILURE;
                        downloadQ.add(curBlockIdx);
                        downloadQ.notify();
                    }
                }

                @Override
                public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
                    Logger.i(TAG, "Complete Retrieving One Block");
                    synchronized(downloadQ){
                        sleepPeriod = Constants.IDLE_BTW_FAILURE;
                        downloadQ.notify();
                    }
                }

                @Override
                public void statusUpdate(String status) {
                    Logger.i(TAG, status);
                }
            };
        };

        //this callback is called after all blocks have been downloaded
        CallableCallback<Boolean> callback = new CallableCallback<Boolean>(){
            @Override
            public void complete(Boolean result) {
                if(result){
                    // Directory update
                    Logger.i(TAG, "Complete downloading all blocks of a file");
                    fileListener.statusUpdate("Complete downloading all blocks of a file");
                    if(fileInfo.getFileName().contains(".mp4") && fileInfo.getNumberOfBlocks() > 1){
                        mergeBlocks();
                    }
                    else{
                        handleSingleBlock();
                    }
                }
                else{
                    Logger.e(TAG, "Fail to download some blocks");
                    fileListener.onError("Fail to download some blocks", fileInfo);
                }
            }
        };

        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(downloadTask, callback));
    }

    private MergeVideo prepareBlocks(){
        File fileDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT + File.separator
                + MDFSFileInfo.getFileDirName(fileInfo.getFileName(), fileInfo.getCreatedTime()));
        File[] blockFiles = fileDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                if(file.isFile() && file.getName().contains(fileInfo.getFileName() + "__blk__")) {
                    return true;
                }
                return false;
            }
        });
        if(blockFiles.length < 1){
            return null;
        }

        List<String> blockList = new ArrayList<String>();
        for(File f : blockFiles){
            blockList.add(f.getAbsolutePath());
        }

        String outputDir = getDecryptedFilePath();
        return new MergeVideo(blockList, outputDir);
    }

    private void handleSingleBlock(){
        // move block to the decrypted directory and rename
        File from = AndroidIOUtils.getExternalFile(
                MDFSFileInfo.getFileDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime()) + File.separator
                        + MDFSFileInfo.getBlockName(fileInfo.getFileName(), (byte)0));
        File to = IOUtilities.createNewFile(getDecryptedFilePath());

        try {
            Files.move(from,to);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // update directory
        ServiceHelper.getInstance().getDirectory().addDecryptedFile(fileInfo.getCreatedTime());
        fileListener.onComplete(to, fileInfo);  //this is where execution of this file ends

        StringBuilder logStr = new StringBuilder();
        logStr.append(System.currentTimeMillis() + ", ");
        logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
        logStr.append("end, ");
        logStr.append(fileInfo.getFileName() + ", ");
        logStr.append(fileInfo.getFileSize() + ", ");
        logStr.append(fileInfo.getNumberOfBlocks()+ ", ");
        logStr.append("\n\n");
        ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_RETRIEVAL, logStr.toString());
    }

    private void mergeBlocks(){
        Callable<Boolean> mergeJob = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final MergeVideo mergeVideo = prepareBlocks();
                if(mergeVideo == null)
                    return false;
                return mergeVideo.mergeVideo();
            }
        };

        CallableCallback<Boolean> callback = new CallableCallback<Boolean>(){
            @Override
            public void complete(Boolean result) {
                // start MDFS
                if(result){
                    Logger.i(TAG, "Video Merge Complete.");
                    fileListener.statusUpdate("Video Merge Complete.");
                    deleteBlocks();
                    // Update directory
                    ServiceHelper.getInstance().getDirectory().addDecryptedFile(fileInfo.getCreatedTime());
                    fileListener.onComplete(AndroidIOUtils.getExternalFile(getDecryptedFilePath()), fileInfo);  //this is where execution of this file ends

                    StringBuilder logStr = new StringBuilder();
                    logStr.append(System.currentTimeMillis() + ", ");
                    logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
                    logStr.append("end, ");
                    logStr.append(fileInfo.getFileName() + ", ");
                    logStr.append(fileInfo.getFileSize() + ", ");
                    logStr.append(fileInfo.getNumberOfBlocks()+ ", ");
                    logStr.append("\n");
                    ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_RETRIEVAL, logStr.toString());
                }
                else{
                    Logger.e(TAG, "Fail to merge video.");
                    fileListener.onError("Fail to merge video.", fileInfo);
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(mergeJob, callback));
    }

    */
/**
     * Return the file path to the final storage location of the decrypted file. File may not exist yet
     * @return
     *//*

    private String getDecryptedFilePath(){
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Constants.DIR_DECRYPTED
                + File.separator + fileInfo.getFileName();
    }

    private void deleteBlocks(){
        File fileDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT + File.separator
                + MDFSFileInfo.getFileDirName(fileInfo.getFileName(), fileInfo.getCreatedTime()));
        File[] blockFiles = fileDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                if(file.isFile() && file.getName().contains(fileInfo.getFileName() + "__blk__")) {
                    return true;
                }
                return false;
            }
        });
        for(File f : blockFiles)
            f.delete();
    }

    */
/*
     * Default FileRetrieverListener. Do nothing.
     *//*

    private BlockRetrieverListenerViaRsock fileListener = new BlockRetrieverListenerViaRsock(){
        @Override
        public void onError(String error, MDFSFileInfo fInfo) {
        }
        @Override
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
        }
        @Override
        public void statusUpdate(String status) {
        }
    };
}
*/
