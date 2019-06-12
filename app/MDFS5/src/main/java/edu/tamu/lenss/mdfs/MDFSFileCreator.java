package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Environment;

import com.google.common.io.Files;

import edu.tamu.lenss.mdfs.MDFSBlockCreator.MDFSBlockCreatorListener;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.handler.TopologyHandler.TopologyListener;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.NewFileUpdate;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger.LogFileInfo.LogFileName;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.CallableTask;
import edu.tamu.lenss.mdfs.utils.CallableTask.CallableCallback;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mp4process.SplitVideo;

public class MDFSFileCreator {
	private static final String TAG = MDFSFileCreator.class.getSimpleName();
	private File file;
	private MDFSFileInfo fileInfo;
	private byte[] encryptKey;
	private int blockCount;
	private double encodingRatio;
	private boolean isTopComplete = false;		// Has topology discovery complete yet?
	private boolean isPartComplete = false;
	private boolean deleteWhenComplete = false;

	public MDFSFileCreator(File f, long maxBlockSize, double encodingRatio) {
		this.file = f;
		this.encodingRatio = encodingRatio;
		this.blockCount = (int)Math.ceil((double)file.length()/maxBlockSize);
		this.fileInfo = new MDFSFileInfo(file.getName(), file.lastModified());
		this.fileInfo.setFileSize(file.length());
		this.fileInfo.setNumberOfBlocks((byte)blockCount);
		Logger.w(TAG, "Split file " + f.getName() + " to " + blockCount + " blocks");
	}

	public void setListener(MDFSBlockCreatorListener fileList){
		fileCreatorListener = fileList;
	}

	/**
	 * Whether to delete this file when file creation completes
	 * @param value
	 */
	public void setDeleteWhenComplete(boolean value){
		this.deleteWhenComplete = value;
	}

	/**
	 * MDFSFileCreator first starts a topology discovery and partitioning file concurrently. <Br>
	 * Topology discovery result helps determine the size of the network and proper k,n value. <Br>
	 * Once the both discovery and file partition have completed, it starts to send block. <Br>
	 * Each block is assumed to use the same (k,n) value.
	 *
	 * sendBlock() is triggered by topologyDiscovery.
	 */
	public void start(){
		Logger.i(TAG, "start() get called!");
		StringBuilder logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", ");
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("start, ");
		logStr.append(file.getName() + ", ");
		logStr.append(file.length() + ", ");
		logStr.append(blockCount + ", ");
		logStr.append("\n");
		ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_CREATION, logStr.toString());
		discoverTopology();
		if(blockCount > 1){
			partition();
		}
		else{
			// Single block
			File fileBlock = IOUtilities.createNewFile(
					Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
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
						+ Constants.DIR_ROOT + File.separator
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

	private boolean isSending = false;
	/**
	 * Can be started only after both topology discovery and file partition have completed <Br>
	 * Each block is sent with one MDFSBlockCreator. All MDFSBlockCreator instances are placed <Br>
	 * in a Queue, and MDFSFileCreator tries it best to complete all tasks in the queue.
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
		final Queue<MDFSBlockCreator> uploadQ = new ConcurrentLinkedQueue<MDFSBlockCreator>();
		final File fileDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT + File.separator
				+ MDFSFileInfo.getFileDirName(file.getName(), file.lastModified()));
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
			private MDFSBlockCreator curBlock;

			private void createQueue(){
				String fName;
				for(File blockF : blocks){
					fName = blockF.getName();
					byte idx = Byte.parseByte(fName.substring((fName.lastIndexOf("_")+1)));
					uploadQ.add(new MDFSBlockCreator(blockF, fileInfo, idx, blockListener));
				}
			}

			@Override
			public Boolean call() throws Exception {
				createQueue();
				int reTryLimit = uploadQ.size() * Constants.FILE_CREATION_RETRIALS;
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
			 * This listener is shared by all MDFSBlockCreator thread. Need to be synchronized properly to avoid race condition. <Br>
			 * uploadQ and curBlock are shared objects between different threads
			 */
			MDFSBlockCreatorListener blockListener = new MDFSBlockCreatorListener(){
				private long sleepPeriod = Constants.IDLE_BTW_FAILURE;
				@Override
				public void statusUpdate(String status) {
					Logger.i(TAG, status);
				}

				@Override
				public void onError(String error) {
					Logger.w(TAG, "Send block failure.  " + error);
					fileCreatorListener.onError("Retrying...  " + error);
					try {
						Thread.sleep(sleepPeriod);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					synchronized(uploadQ){
						sleepPeriod += Constants.IDLE_BTW_FAILURE;
						uploadQ.add(curBlock);
						uploadQ.notify();
					}
				}

				@Override
				public void onComplete() {
					synchronized(uploadQ){
						sleepPeriod = Constants.IDLE_BTW_FAILURE;
						curBlock.deleteBlockFile();
						uploadQ.notify();
					}
				}
			};
		};
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

	/*
	 * NOn-blocking call
	 */
	private void discoverTopology() {
		Logger.i(TAG, "discoverTopology() get called!");
		TopologyListener topologyListener = new TopologyListener() {
			@Override
			public void onError(String msg) {
				Logger.e(TAG, msg);
				fileCreatorListener.onError("Topology Discovery fails");
			}

			@Override
			public void onComplete(List<NodeInfo> topList) {
				// Configure some global parameters
				// We don't want to use all of the nodes for storage. It is difficult to reach all the nodes.
				int numOfStorages = (int) Math.ceil(topList.size()*Constants.STORAGE_NODES_RATIO);
				Math.min(numOfStorages, Constants.MAX_N_VAL);
				byte n2 ;
				if(numOfStorages >= Constants.MAX_N_VAL)
					n2 = (byte)Constants.MAX_N_VAL;
				else
					n2 = (byte)numOfStorages;
				byte k2 = (byte) Math.round(n2 * encodingRatio);
				fileInfo.setFragmentsParms(n2, k2);
				Logger.i(TAG,"n2:" + n2 + " k2:" + k2);
				Logger.i(TAG, "Topology Discovery Complete");
				fileCreatorListener.statusUpdate("n2:" + n2 + " k2:" + k2);
				if(n2 <= 1 || k2 < 1){
					fileCreatorListener.onError("Insufficeint storage nodes.");
					return;
				}

				synchronized(fileInfo){
					isTopComplete = true;
				}
				sendBlocks(); // Execute from the timer ins topology discovery handler
			}
		};

		ServiceHelper.getInstance().startTopologyDiscovery(topologyListener, Constants.TOPOLOGY_DISCOVERY_RETRIAL_TIMEOUT);
		fileCreatorListener.statusUpdate("Start Topology Discovery");
	}


	private void updateDirectory() {


		ServiceHelper serviceHelper = ServiceHelper.getInstance();
		fileInfo.setCreator(serviceHelper.getNodeManager().getMyMAC());
		NewFileUpdate update = new NewFileUpdate(fileInfo);

		serviceHelper.sendFileUpdate(update);
		Logger.w(TAG, "File Id: " + fileInfo.getCreatedTime());
		// Update my directory as well
		serviceHelper.getDirectory().addFile(fileInfo);

		if(deleteWhenComplete)
			file.delete();

		StringBuilder logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", ");
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("end, ");
		logStr.append(fileInfo.getFileName() + ", ");
		logStr.append(fileInfo.getFileSize() + ", ");
		logStr.append(fileInfo.getK2() + ", ");
		logStr.append(fileInfo.getN2() + ", ");
		logStr.append(fileInfo.getNumberOfBlocks() + ", ");
		logStr.append("\n\n");
		ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_CREATION, logStr.toString());
		fileCreatorListener.onComplete();
	}

	/*
	 * Default MDFSBlockCreatorListener. Do nothing.
	 */
	private MDFSBlockCreatorListener fileCreatorListener = new MDFSBlockCreatorListener(){
		@Override
		public void statusUpdate(String status) {}

		@Override
		public void onError(String error) {}

		@Override
		public void onComplete() {}

	};
}
