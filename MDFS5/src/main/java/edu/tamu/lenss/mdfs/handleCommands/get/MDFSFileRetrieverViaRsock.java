package edu.tamu.lenss.mdfs.handleCommands.get;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.io.Files;

import android.os.Environment;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.CallableTask;
import edu.tamu.lenss.mdfs.utils.CallableTask.CallableCallback;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mp4process.MergeVideo;

//rsock imports
import edu.tamu.lenss.mdfs.handleCommands.get.MDFSBlockRetrieverViaRsock.BlockRetrieverListenerViaRsock;


//this class object is only made in DirectoryList.java class.
//this class is used only to retrieve file from other nodes using rsock, instead of tcp.
//most of the codes are copied from MDFSFileRetriever.java class.
public class MDFSFileRetrieverViaRsock {
    private static final String TAG = MDFSFileRetrieverViaRsock.class.getSimpleName();
    private byte[] decryptKey;
    private MDFSFileInfo fileInfo;
    private MDFSMetadata metadata;
    private String localDir;

    public MDFSFileRetrieverViaRsock(MDFSFileInfo fInfo, MDFSMetadata metadata, String localDir) {
        this.fileInfo = fInfo;
        this.metadata = metadata;
        this.localDir = localDir;
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
                    //downloadQ being empty means there are no blocks to retrieve,
                    //not empty means there are some block failed and,
                    //they needs to be retrieved again but reTryLimit may exceed.
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
                    System.out.println("xxx" + " complete downlaoding all blocks");
                    fileListener.statusUpdate("Complete downloading all blocks of a file");
                    if(fileInfo.getNumberOfBlocks() > 1){
                        mergeBlocks1();
                    }
                    else{
                        handleSingleBlock();
                    }
                }
                else{
                    Logger.e(TAG, "Fail to download some blocks");
                    fileListener.onError("Fail to download some blocks", fileInfo);
                    return;
                }
            }
        };

        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(downloadTask, callback));
    }


    //goes into the file directory and loads all the files that has "__blk__" tag in it.
    private MergeVideo prepareBlocks(){
        ////storage/emulated/0/MDFS/test1.jpg_0123/
        File fileDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(fileInfo.getFileName(), fileInfo.getCreatedTime()));

        //blockFiles only contains blocks with signature "__blk_-" in them.
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
        File from = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime()) + File.separator + MDFSFileInfo.getBlockName(fileInfo.getFileName(), (byte)0));
        File to = IOUtilities.createNewFile(getDecryptedFilePath());

        try {
            Files.move(from,to);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // update directory
        ServiceHelper.getInstance().getDirectory().addDecryptedFile(fileInfo.getCreatedTime());
        fileListener.onComplete(to, fileInfo);  //this is where execution of this file ends
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

                if(result){
                    Logger.i(TAG, "Video Merge Complete.");
                    fileListener.statusUpdate("Video Merge Complete.");
                    deleteBlocks();
                    // Update directory
                    ServiceHelper.getInstance().getDirectory().addDecryptedFile(fileInfo.getCreatedTime());
                    fileListener.onComplete(AndroidIOUtils.getExternalFile(getDecryptedFilePath()), fileInfo);  //this is where execution of this file ends
                }
                else{
                    fileListener.onError("Fail to merge video.", fileInfo);
                    return;
                }
            }
        };
        ServiceHelper.getInstance().submitCallableTask(new CallableTask<Boolean>(mergeJob, callback));
    }

    private void mergeBlocks1(){
        boolean mergeResult = false;

        //get all the block files from disk
        ///storage/emulated/0/MDFS/test1.jpg_0123/
        File fileDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(fileInfo.getFileName(), fileInfo.getCreatedTime()));  //Isagor0!
        File[] blockFiles = fileDir.listFiles(new FileFilter(){
            @Override
            public boolean accept(File file) {
                if(file.isFile() && file.getName().contains(fileInfo.getFileName() + "__blk__")) {
                    return true;
                }
                return false;
            }
        });


        //create a map with blockName to byte[] mapping
        Map<String, byte[]> fileMap = new HashMap<>();
        for(int i =0; i< blockFiles.length; i++){
            //get the bytes of the block files
            byte[] blockBytes = IOUtilities.fileToByte(blockFiles[i]);

            //get the length of bytes which are actual data (a block file = size_of_data + data)
            int blockLength = ByteBuffer.wrap(blockBytes).getInt();

            //allocate the blockLength amount of size in each array
            byte[] blockData = new byte[blockLength];

            //copy data from blockBytes[] to blockData[]
            System.arraycopy(blockBytes, Integer.BYTES, blockData, 0 ,blockLength);

            //put blockData[] into map
            fileMap.put(blockFiles[i].getName(), blockData);
        }


        File file = new File(getDecryptedFilePath());
        for(int i=0; i< fileMap.size(); i++){
            try {
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write(fileMap.get(fileInfo.getFileName() + "__blk__" + i ));
                fos.close();
            }catch(IOException e){e.printStackTrace();}
        }

        mergeResult = true;

        if(mergeResult){
            //update listener
            fileListener.statusUpdate("File Merge Complete.");
            deleteBlocks();

            // Update directory
            ServiceHelper.getInstance().getDirectory().addDecryptedFile(fileInfo.getCreatedTime());
            fileListener.onComplete(AndroidIOUtils.getExternalFile(getDecryptedFilePath()), fileInfo);  //this is where execution of this file ends
        }else{
            fileListener.onError("Fail to merge file..", fileInfo);
            return;
        }
    }


    private String getDecryptedFilePath(){  //Isagor0!

        //first set the path where the file ought to be saved
        //replace the "/storage/emulated/0" part from user inputted localDir since it will be added again.
        Constants.ANDROID_DIR_DECRYPTED = localDir.replace("/storage/emulated/0/", "/");

        ///storage/emulated/0/MDFS/test1.jpg
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Constants.ANDROID_DIR_DECRYPTED
                + File.separator + fileInfo.getFileName();
    }

    //goes into /storage/emulated/0/MDFS/test1.jpg_0123/ directory and loads all the files.
    //all the files consists of both block directories(like "test1.jpg_0123_0/" that contains fragments)
    // and the block file (with "__blk__" in name) itself.
    //this function only deletes the block files and doesnt affect the block directories.
    private void deleteBlocks(){
        File fileDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(fileInfo.getFileName(), fileInfo.getCreatedTime()));  //Isagor0!
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
