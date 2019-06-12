package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.io.Files;

import android.os.Environment;
import edu.tamu.lenss.mdfs.MDFSBlockRetriever.BlockRetrieverListener;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.CallableTask;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger.LogFileInfo.LogFileName;
import edu.tamu.lenss.mdfs.utils.CallableTask.CallableCallback;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mp4process.MergeVideo;


//this class object is only created from DirectoryList.java class when retrieving a flie via tcp
public class MDFSFileRetriever {
	private static final String TAG = MDFSFileRetriever.class.getSimpleName();
	private byte[] decryptKey;
	private MDFSFileInfo fileInfo;
	private String myIP;
	
	public MDFSFileRetriever(MDFSFileInfo fInfo) {
		this.fileInfo = fInfo;
		this.myIP = "0.0.0.0";
	}

	public MDFSFileRetriever(MDFSFileInfo fInfo, String myip) {
		this.fileInfo = fInfo;
		this.myIP = myip;
	}
	
	public void start(){
		StringBuilder logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", "); 
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("start, ");
		logStr.append(fileInfo.getFileName() + ", ");
		logStr.append(fileInfo.getFileSize() + ", ");
		logStr.append(fileInfo.getNumberOfBlocks()+ ", ");
		logStr.append("\n");
		ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_RETRIEVAL, logStr.toString());

		retrieveBlocks();
	}
	
	public void setDecryptKey(byte[] key){
		this.decryptKey = key;
	}
	
	public void setListener(BlockRetrieverListener list){
		fileListener = list;
	}


	private void retrieveBlocks(){
		Logger.i(TAG, "Start downloading blocks. Total " + fileInfo.getNumberOfBlocks() + " blocks.");
		fileListener.statusUpdate("Start downloading blocks. Total " + fileInfo.getNumberOfBlocks() + " blocks.");
		final Queue<Byte> downloadQ = new ConcurrentLinkedQueue<Byte>();
		for(byte i=0; i < fileInfo.getNumberOfBlocks(); i++){
			downloadQ.add(i);
		}
		
		Callable<Boolean> downloadTask = new Callable<Boolean>() {
			private MDFSBlockRetriever curBlock;
			private byte curBlockIdx;

			@Override
			public Boolean call() throws Exception {
				int reTryLimit = downloadQ.size() * Constants.FILE_RETRIEVAL_RETRIALS;
				synchronized(downloadQ){
					while(!downloadQ.isEmpty() && reTryLimit > 0){
						Logger.i(TAG, "Download one block");
						curBlockIdx = downloadQ.poll();
						curBlock = new MDFSBlockRetriever(fileInfo, curBlockIdx);
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
			
			/**
			 * This listener is shared by all MDFSBlockRetriever thread. Need to be synchronized properly to avoid race condition <Br>
			 * downloadQ and curBlock are the shared objects between different threads.
			 * notify() needs to be called from different thread. Make sure this is the case when callback function makes call
			 */
			BlockRetrieverListener blockListener = new BlockRetrieverListener(){
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
		fileListener.onComplete(to, fileInfo);
		
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
					fileListener.onComplete(AndroidIOUtils.getExternalFile(getDecryptedFilePath()), fileInfo);
					
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
	
	/**
	 * Return the file path to the final storage location of the decrypted file. File may not exist yet
	 * @return
	 */
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
	
	/*
	 * Default FileRetrieverListener. Do nothing.
	 */
	private BlockRetrieverListener fileListener = new BlockRetrieverListener(){
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
