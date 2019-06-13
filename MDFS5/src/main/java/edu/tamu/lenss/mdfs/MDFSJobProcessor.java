package edu.tamu.lenss.mdfs;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.tamu.lenss.mdfs.handler.JobProcessingHandler.JobRequestListener;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.AssignTaskReq;
import edu.tamu.lenss.mdfs.models.JobReply;
import edu.tamu.lenss.mdfs.models.JobReq;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.placement.TaskAllocationHelper;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger.LogFileInfo.LogFileName;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MyPair;

public class MDFSJobProcessor {
	private static final String TAG = MDFSJobProcessor.class.getSimpleName();
	private ServiceHelper serviceHelper;
	private List<Long> fileList;
	private byte[] decryptKey;
	private long jobId;

	public MDFSJobProcessor(List<Long> fileIds) {
		serviceHelper = ServiceHelper.getInstance();
		fileList = fileIds;
		jobId = System.currentTimeMillis();
	}
	
	public void start(){
		// Setup new directory
		File jobDir = AndroidIOUtils.getExternalFile(Constants.DIR_JOBS + File.separator + jobId);
		jobDir.mkdirs();
		sendJobRequest();
		
		StringBuilder logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", "); 
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("start, ");
		logStr.append(jobId + ", ");
		logStr.append(fileList.size() + ", ");
		for(Long fileId : fileList){
			MDFSFileInfo fInfo = serviceHelper.getDirectory().getFileInfo(fileId);
			if(fInfo == null)
				continue;
			logStr.append(fInfo.getFileName() + ", ");
			logStr.append(fInfo.getFileSize() + ", ");
			logStr.append(fInfo.getNumberOfBlocks()+ ", ");
		}
		logStr.append("\n");
		serviceHelper.getDataLogger().appendSensorData(LogFileName.FILE_PROCESS, logStr.toString());
	}
	
	public void setDecryptKey(byte[] key){
		this.decryptKey = key;
	}
	
	
	private void sendJobRequest(){
		JobReq jobReq = new JobReq(fileList, jobId);
		serviceHelper.broadcastJobRequest(jobReq, jobReqListener);
		serviceHelper.getDirectory().addJob(jobId, jobReq);
	}
	
	private void processorSelection(Set<JobReply> jobReplySet){
		TaskAllocationHelper allocationHelper = new TaskAllocationHelper(jobReplySet, fileList);
		allocationHelper.findOptimalAllocation();
		HashMap<Long, HashMap<Long, HashSet<Byte>>> taskAssignment = allocationHelper.getAllocation();
		
		//long myIp = serviceHelper.getNodeManager().getMyIP();
		for(Long nodeIp : taskAssignment.keySet()){
			AssignTaskReq taskReq = new AssignTaskReq(taskAssignment.get(nodeIp),jobId, nodeIp);
			taskReq.setDecryptKey(decryptKey);
			// send task
			serviceHelper.sendAssignTaskReq(taskReq);
			
			// Print out assignment information
			for(Long fileId : taskAssignment.get(nodeIp).keySet()){
				Iterator<Byte> iter = taskAssignment.get(nodeIp).get(fileId).iterator();
				String str=" blocks ";
				while(iter.hasNext()){
					str+=iter.next() + ", ";
				}
				Logger.v(TAG, "Assign " + IOUtilities.long2Ip(nodeIp) + " file " + fileId + str );
			}
		}
		
		StringBuilder logStr = new StringBuilder();
		logStr.append(System.currentTimeMillis() + ", "); 
		logStr.append(ServiceHelper.getInstance().getNodeManager().getMyMacString() + ", ");
		logStr.append("Assign, ");
		logStr.append(jobId + ", ");
		for(Long nodeIp : taskAssignment.keySet()){
			if(nodeIp == null)
				continue;
			logStr.append(IOUtilities.long2Ip(nodeIp) + ", ");
		}
		logStr.append("\n");
		serviceHelper.getDataLogger().appendSensorData(LogFileName.FILE_PROCESS, logStr.toString());
	}
	
	private JobRequestListener jobReqListener = new JobRequestListener(){
		@Override
		public void onError(String message) {
			Logger.e(TAG, "Job Request Fails. " + message);
		}

		@Override
		public void onComplete(Set<JobReply> jobReplySet) {
			processorSelection(jobReplySet);
		}
	};
	
	private class PQsort implements Comparator<MyPair<Long, Byte>> {
		 
		public int compare(MyPair<Long, Byte> one, MyPair<Long, Byte> two) {
			return -(one.second - two.second);
		}
	}
}
