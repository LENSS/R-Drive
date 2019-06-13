package edu.tamu.lenss.mdfs.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;

public class JobReply extends MessageContainer {
	private static final long serialVersionUID = 1L;
	private long jobId;
	private Map<Long, HashMap<Byte, HashSet<Byte>>> fileBlockFragMap;
	

	public JobReply(long destIp) {
		super(MDFSPacketType.JOB_REPLY, 
				ServiceHelper.getInstance().getNodeManager().getMyIP(), destIp);
		this.setBroadcast(false);
		fileBlockFragMap = new HashMap<Long, HashMap<Byte, HashSet<Byte>>>();
	}
	
	public JobReply(Map<Long, HashMap<Byte, HashSet<Byte>>> fileMap, long jobIdentifier, long destIp) {
		super(MDFSPacketType.JOB_REPLY, 
				ServiceHelper.getInstance().getNodeManager().getMyIP(), destIp);
		this.setBroadcast(false);
		fileBlockFragMap = fileMap;
		jobId = jobIdentifier;
	}
	
	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	
	public Map<Long, HashMap<Byte, HashSet<Byte>>> getFileBlockFragMap() {
		return fileBlockFragMap;
	}

	public void setFileBlockFragMap(
			Map<Long, HashMap<Byte, HashSet<Byte>>> fileBlockFragMap) {
		this.fileBlockFragMap = fileBlockFragMap;
	}

	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}

}
