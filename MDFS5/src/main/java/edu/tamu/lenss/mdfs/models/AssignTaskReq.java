package edu.tamu.lenss.mdfs.models;

import java.util.HashSet;
import java.util.Map;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;

public class AssignTaskReq extends MessageContainer {
	private static final long serialVersionUID = 1L;
	private long jobId;
	private byte[] decryptKey;
	private Map<Long, HashSet<Byte>> file2BlockMap;

	public AssignTaskReq(Map<Long, HashSet<Byte>> fileBlockMap, long jobIdentifier, long desti) {
		super(MDFSPacketType.ASSIGN_TASK_REQUEST, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), desti);
		this.setBroadcast(false);
		this.jobId = jobIdentifier;
		this.file2BlockMap = fileBlockMap;
	}

	public byte[] getDecryptKey() {
		return decryptKey;
	}

	public void setDecryptKey(byte[] decryptKey) {
		this.decryptKey = decryptKey;
	}

	public long getJobId() {
		return jobId;
	}


	public void setJobId(long jobId) {
		this.jobId = jobId;
	}


	public Map<Long, HashSet<Byte>> getFile2BlockMap() {
		return file2BlockMap;
	}

	public void setFile2BlockMap(Map<Long, HashSet<Byte>> file2BlockMap) {
		this.file2BlockMap = file2BlockMap;
	}

	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}

}
