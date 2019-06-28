package edu.tamu.lenss.mdfs.handler;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSBlockRetriever;
import edu.tamu.lenss.mdfs.models.AssignTaskReq;
import edu.tamu.lenss.mdfs.models.JobReply;
import edu.tamu.lenss.mdfs.models.JobReq;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.TaskResultInfo;
import edu.tamu.lenss.mdfs.network.TCPConnection;
import edu.tamu.lenss.mdfs.network.TCPReceive.TCPReceiverData;
import edu.tamu.lenss.mdfs.network.TCPSend;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.JCountDownTimer;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MDFSFaceDetector;
import edu.tamu.lenss.mdfs.utils.MyPair;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger.LogFileInfo.LogFileName;

/**
 * This class is responsible for handling all requests for job processing. <Br>
 * All methods in this class are blocking calls.  
 * @author Jay
 */
public class JobProcessingHandler {
	private static final String TAG = JobProcessingHandler.class.getSimpleName();
	private Map<Long, JobReplyWaiter> runnerMap;

	public JobProcessingHandler() {
		runnerMap = new HashMap<Long, JobReplyWaiter>();
	}
	
	/**
	 * Process the incoming job reply
	 * @param jobReply
	 */
	protected synchronized void processReply(JobReply jobReply){
		// Make sure this is indeed the file fragment I'm waiting for
		if(runnerMap.containsKey(jobReply.getJobId())){
			runnerMap.get(jobReply.getJobId()).receiveJobReply(jobReply);
		}
	}
	
	/**
	 * Process the incoming job request and uni-cast back to a jobReply if I can handle the job
	 * @param jobReq
	 */
	protected synchronized void processRequest(JobReq jobReq){
		JobReply jobReply = generateJobReply(jobReq);
		if(jobReply != null)
			PacketExchanger.getInstance().sendMsgContainer(jobReply);
	}
	
	private JobReply generateJobReply(JobReq jobReq){
		// Return the number of fragments I have for the requested file
		Map<Long, HashMap<Byte, HashSet<Byte>>> fileBlockFragMap = new HashMap<Long, HashMap<Byte, HashSet<Byte>>>();
		List<Long>fileList = jobReq.getFileList();
		for(Long fileId : fileList){
			HashMap<Byte, HashSet<Byte>> blockAndFrags = ServiceHelper.getInstance().getDirectory().getStoredFragments(fileId, true);
			if(blockAndFrags != null){
				fileBlockFragMap.put(fileId, blockAndFrags);
			}
		}
		if(!fileBlockFragMap.isEmpty()){
			return new JobReply(fileBlockFragMap, jobReq.getJobId(), jobReq.getSource());
		}
		else 
			return null;
	}

	
	protected synchronized void broadcastRequest(JobReq jobReq, JobRequestListener lis){
		// create a JobReplyWaiter for each new job. JobReplyWaiter starts a timer thread and wait for replies
		runnerMap.put(jobReq.getJobId(), new JobReplyWaiter(jobReq, lis));
		
		// include myself. create a JobReply and insert 
		processReply(generateJobReply(jobReq));
		
		// Broadcast the message
		PacketExchanger.getInstance().sendMsgContainer(jobReq);
	}
	
	private long processingTime = 0;	// measure the data processing time
	/**
	 * This method can take a long time, so do not make it synchronized. <Br>
	 * It may block other synchronized methods to be executed.
	 * @param taskReq
	 * @param context
	 */
	protected void processAssignTask(Context context, AssignTaskReq taskReq){
		StringBuilder logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", "); 
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("start, ");
		
		// Start processing tasks
		Queue<MyPair<Long, Byte>> downloadProcessQ = new ConcurrentLinkedQueue<MyPair<Long, Byte>>(); // <FileId, BlockIdx>
		Map<Long, HashSet<Byte>> fileMap = taskReq.getFile2BlockMap();
		ServiceHelper serviceHelper = ServiceHelper.getInstance();
		
		int blockCnt = 0;
		for(Long fileId : fileMap.keySet()){
			logStr.append(fileId + ", ");
			for(Byte block : fileMap.get(fileId)){
				//Logger.v(TAG, "I am assigned task of file " + fileId + ", block " + block);
				downloadProcessQ.add(MyPair.create(fileId, block));
				logStr.append(block + ", ");
				blockCnt++;
			}
		}
		logStr.append(blockCnt + "\n");
		ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_PROCESS, logStr.toString());
		
		File jobDir = AndroidIOUtils.getExternalFile(Constants.DIR_JOBS + File.separator + taskReq.getJobId());
		jobDir.mkdirs();
		downloadAndProcess(context, downloadProcessQ, jobDir.getAbsolutePath(), taskReq.getDecryptKey());
		
		logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", "); 
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("End, ");
		logStr.append(processingTime + ",\n\n");
		ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_PROCESS, logStr.toString());
		
		// send result back to the jobRequester
		File[] resultFiles = jobDir.listFiles(new FileFilter(){
			@Override
			public boolean accept(File fName) {
				return fName.getName().contains(".jpg");
			}
		});
		
		if(resultFiles != null && taskReq.getSource() != serviceHelper.getNodeManager().getMyIP()){
			for(File f : resultFiles){
				sendTasResult(f, taskReq);
			}
		}
	}
	
	
	/**
	 * Send the processing result (image frames with human faces) back to the jobRequester
	 * Blocking call.
	 * @param resultFile : image file to be sent
	 * @param taskReq 
	 * @return
	 */
	protected boolean sendTasResult(File resultFile, AssignTaskReq taskReq){
		TCPSend send = TCPConnection
				.creatConnection(IOUtilities.long2Ip(taskReq.getSource()));
		if( send == null){
			Logger.w(TAG, "Fail to establish conneciton with " + taskReq.getSource());
			return false;
		}
		// Handshake
		TaskResultInfo header = new TaskResultInfo(resultFile.getName(), taskReq.getJobId());
		
		try {
			ObjectOutputStream oos = new ObjectOutputStream(send.getOutputStream());
			oos.writeObject(header);
			
			ObjectInputStream ois = new ObjectInputStream(
					send.getInputStream());
			header = (TaskResultInfo) ois.readObject();
			if(!header.isReady()){
				Logger.e(TAG, "Destination reject to receive task result");
				return false;
			}
			
			byte[] mybytearray = new byte[Constants.TCP_COMM_BUFFER_SIZE];
			int readLen = 0;
			FileInputStream fis = new FileInputStream(resultFile);
			BufferedInputStream bis = new BufferedInputStream(fis);
			DataOutputStream out = send.getOutputStream();
			while ((readLen = bis.read(mybytearray, 0, Constants.TCP_COMM_BUFFER_SIZE)) >= 0) {
				out.write(mybytearray, 0, readLen);
			}
			
			send.close();
			fis.close();
			ois.close();
			oos.close();
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Receive processing result from an processor
	 * Blocking call
	 * @param data
	 * @param header
	 * @return
	 */
	protected boolean receiveTaskResult(TCPReceiverData data, TaskResultInfo header){
		try {
			ObjectOutputStream oos = new ObjectOutputStream(data.getDataOutputStream());
			header.setReady(true);
			oos.writeObject(header);

			byte[] buffer = new byte[Constants.TCP_COMM_BUFFER_SIZE];
			File tmp0 = AndroidIOUtils.getExternalFile(
					Constants.DIR_JOBS + File.separator + header.getJobId() + File.separator + header.getResultFileName());
			IOUtilities.createNewFile(tmp0);
			if(!tmp0.exists()){
				Logger.e(TAG, "Fail to create task result file for " + header.getResultFileName());
				return false;
			}
			
			FileOutputStream fos = new FileOutputStream(tmp0);
			int readLen=0;
			DataInputStream in = data.getDataInputStream();
			while ((readLen = in.read(buffer)) >= 0) {
                fos.write(buffer, 0, readLen);
			}
			Logger.v(TAG, "Finish downloading task result file of " + header.getResultFileName());
			
			fos.close();
			data.close();
			oos.close();
			return true;
		} 
		catch (StreamCorruptedException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * A blocking call to download blocks and processing blocks
	 * @param context :
	 * @param downloadQ :
	 * @param outputDirPath :
	 * @return true if all blocks are successfully downloaded and processed
	 */
	private boolean downloadAndProcess(final Context context, final Queue<MyPair<Long, Byte>> downloadQ,
			final String outputDirPath, final byte[] decryptKey ){
		MDFSBlockRetriever.BlockRetrieverListener blockListener = new MDFSBlockRetriever.BlockRetrieverListener(){
			private long sleepPeriod = Constants.IDLE_BTW_FAILURE;
			
			@Override
			public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
				long startT = System.currentTimeMillis();
				Logger.i(TAG, "One block is saved to " + decryptedFile.getPath());
				// process the decrypted file
				MDFSFaceDetector faceDetector = new MDFSFaceDetector(context);
				if(fileInfo.getFileName().contains(".mp4")){
					int frameCnt = faceDetector.detectFaceFromVideo(decryptedFile, outputDirPath, 
							Constants.COMPRESS_RATE, Constants.SAMPLE_PER_SECOND, Constants.MAX_FACE_PER_FRAME);
					Logger.v(TAG, "Found " + frameCnt + " frames with faces " + " from video" + decryptedFile.getPath());
				}
				else{
					int faceCnt = faceDetector.detectFaceFromImage(decryptedFile, 
							Constants.MAX_FACE_PER_FRAME, Constants.COMPRESS_RATE, outputDirPath);
					Logger.v(TAG, "Found " + faceCnt + " faces " + " from image" + decryptedFile.getPath());
				}
				// File processing
				ServiceHelper.getInstance().getNodeStatusMonitor().incrementProcessedBytes(decryptedFile.length());
				long endT = System.currentTimeMillis();
				processingTime+= (endT-startT);
				synchronized(downloadQ){
					sleepPeriod = Constants.IDLE_BTW_FAILURE;
					downloadQ.poll();
					downloadQ.notify();  
				}
			}
			
			@Override
			public void onError(String error, MDFSFileInfo fInfo) {
				Logger.w(TAG, "Retrieve block failure.  " + error);
				//fileListener.onError("Retrieve block failure.  " + error, fInfo);
				try {
					Thread.sleep(sleepPeriod);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// File processing
				ServiceHelper.getInstance().getNodeStatusMonitor().incrementProcessedBytes(
						(long)Math.round(fInfo.getFileSize()/(double)fInfo.getNumberOfBlocks()));
				synchronized(downloadQ){
					if(!downloadQ.isEmpty()){ // downloadQ.poll may return null...
						sleepPeriod += Constants.IDLE_BTW_FAILURE;
						downloadQ.add(downloadQ.poll());	// Retrieve from tail and add back to head
						downloadQ.notify();	
					}
				}
			}
			
			@Override
			public void statusUpdate(String status) {
				Logger.i(TAG, status);
			}
		};
		
		
		int retryLimit = downloadQ.size()*Constants.BLOCK_RETRIEVAL_RETRIALS;
		MDFSBlockRetriever curBlock;
		MyPair<Long, Byte> blockPair;
		MDFSFileInfo fInfo;
		synchronized(downloadQ){
			while(!downloadQ.isEmpty() && retryLimit > 0){
				Logger.i(TAG, "Download one block");
				blockPair = downloadQ.peek();
				fInfo = ServiceHelper.getInstance().getDirectory().getFileInfo(blockPair.first);
				if(fInfo == null)
					continue;
				curBlock = new MDFSBlockRetriever(fInfo, blockPair.second);
				curBlock.setDecryptKey(decryptKey);
				curBlock.setListener(blockListener);
				curBlock.start();
				try {
					// Current thread releases the lock when wait() is called and Regain the lock once it is notified.
					downloadQ.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}	
				retryLimit--;
			}
			Logger.i(TAG, "Finish downloadQ");
			
			// log
			StringBuilder logStr = new StringBuilder();
			logStr.append(System.currentTimeMillis() + ", "); 
			logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
			logStr.append("Download End, ");
			logStr.append("\n");
			ServiceHelper.getInstance().getDataLogger().appendSensorData(LogFileName.FILE_PROCESS, logStr.toString());
			
			if(downloadQ.isEmpty())
				return true;
			else
				return false;
		}		
	}
	
	
	
	private class JobReplyWaiter{
		private JobReq jobRequest;
		private JCountDownTimer timer;
		private JobRequestListener listener;
		private boolean waitingReply = false;
		private Set<JobReply> replys = new HashSet<JobReply>();
		
		
		private JobReplyWaiter(JobReq fileReq, JobRequestListener lis){
			this.jobRequest = fileReq;
			this.listener = lis;
			this.timer = new JCountDownTimer(Constants.JOB_REQUEST_TIMEOUT,
					Constants.JOB_REQUEST_TIMEOUT) {
				@Override
				public void onTick(long millisUntilFinished) {}

				@Override
				public synchronized void onFinish() {
					if (waitingReply) {
						if (replys.isEmpty())
							listener.onError("Timeout. No processor nodes in the network.");
						else
							listener.onComplete(replys);
					}
					waitingReply = false;
					runnerMap.remove(jobRequest.getJobId());
				}
			};
			
			waitingReply = true;
			replys.clear();
			timer.start();
		}
		
		protected synchronized void receiveJobReply(JobReply req) {
			replys.add(req);
			timer.cancel(); // Reset the timer
			timer.start(); 
		}
	}
	
	public static interface JobRequestListener{
		public void onError(String message);
		
		public void onComplete(Set<JobReply> jobReplySet);
	}

}
