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
    private String clientID;

    public MDFSFileRetrieverViaRsock(MDFSFileInfo fInfo, FileMetadata metadata, String clientID) {
        this.fileInfo = fInfo;
        this.metadata = metadata;
        this.clientID = clientID;
    }

    public void start(){
        retrieveBlocks();
    }

    public void setDecryptKey(byte[] key){
        this.decryptKey = key;
    }

    public void setListener(BlockRetrieverListenerViaRsock list){
        fileListener = list;
    }


    //this function calls new MDFSBlockRetrieverViaRsock()
    private void retrieveBlocks(){
        fileListener.statusUpdate("Start downloading blocks. Total " + fileInfo.getNumberOfBlocks() + " blocks.", clientID);
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
                        curBlock = new MDFSBlockRetrieverViaRsock(fileInfo, curBlockIdx, metadata, clientID);  //RSOCK
                        curBlock.setListener(blockListener);
                        curBlock.setDecryptKey(decryptKey);
                        curBlock.start();
                        downloadQ.wait();	// one block at a time..execution of this thread waits here until current block is done(succeed or error)
                        reTryLimit--;
                    }
                    Logger.i(TAG, "Finish downloadQ");
                    //downloadQ beung empty means there are no blocks to retrieve,
                    //not empty means there are some block failed and,
                    //they needs to be retrieved again but reTryLimit exceeded.
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
                public void onError(String error, MDFSFileInfo fInfo, String clientID) {
                    //fileListener.onError("Retrieve block unsuccessful. Retrying...  " + error, fInfo, clientID);
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
                public void onComplete(File decryptedFile, MDFSFileInfo fileInfo, String clientID) {
                    Logger.i(TAG, "Complete Retrieving One Block");
                    synchronized(downloadQ){
                        sleepPeriod = Constants.IDLE_BTW_FAILURE;
                        downloadQ.notify();
                    }
                }

                @Override
                public void statusUpdate(String status, String clientID) {
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
                    fileListener.statusUpdate("Complete downloading all blocks of a file", clientID);
                    if(fileInfo.getFileName().contains(".mp4") && fileInfo.getNumberOfBlocks() > 1){
                        mergeBlocks();
                    }
                    else{
                        handleSingleBlock();
                    }
                }
                else{
                    Logger.e(TAG, "Fail to download some blocks");
                    fileListener.onError("Fail to download some blocks", fileInfo, clientID);
                    return;
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
        fileListener.onComplete(to, fileInfo, clientID);  //this is where execution of this file ends

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
                    fileListener.statusUpdate("Video Merge Complete.", clientID);
                    deleteBlocks();
                    // Update directory
                    ServiceHelper.getInstance().getDirectory().addDecryptedFile(fileInfo.getCreatedTime());
                    fileListener.onComplete(AndroidIOUtils.getExternalFile(getDecryptedFilePath()), fileInfo, clientID);  //this is where execution of this file ends

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
                    fileListener.onError("Fail to merge video.", fileInfo, clientID);
                    return;
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(mergeJob, callback));
    }


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



    private BlockRetrieverListenerViaRsock fileListener = new BlockRetrieverListenerViaRsock(){
        @Override
        public void onError(String error, MDFSFileInfo fInfo, String clientID) {
        }
        @Override
        public void onComplete(File decryptedFile, MDFSFileInfo fileInfo, String clientID) {
        }
        @Override
        public void statusUpdate(String status, String clientID) {
        }
    };
}
