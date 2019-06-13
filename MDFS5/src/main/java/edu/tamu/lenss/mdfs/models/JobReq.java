package edu.tamu.lenss.mdfs.models;

import java.util.List;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class JobReq extends MessageContainer {
	private static final long serialVersionUID = 1L;
	private boolean isComplete = false;		// Indicate if this job is complete
	
	private long jobId;
	private List<Long> fileList;

	public JobReq(List<Long> files, long jobIdentifier) {
		//super(MDFSPacketType.JOB_REQUEST);
		super(MDFSPacketType.JOB_REQUEST, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
		this.setBroadcast(true);
		this.fileList = files;
		this.jobId = jobIdentifier;
	}

	public boolean isComplete() {
		return isComplete;
	}

	public void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public List<Long> getFileList() {
		return fileList;
	}

	public void setFileList(List<Long> fileList) {
		this.fileList = fileList;
	}

	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}

}
