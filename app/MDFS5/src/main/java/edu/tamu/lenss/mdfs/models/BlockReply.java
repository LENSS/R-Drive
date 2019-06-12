package edu.tamu.lenss.mdfs.models;

import java.util.List;

import edu.tamu.lenss.mdfs.pdu.MessageContainer;

public class BlockReply extends MessageContainer {
	private static final long serialVersionUID = 1L;
	private String fileName;
	private long fileCreatedTime;
	private byte blockIdx;
	private List<Byte> fileFragIndex;
	
	public BlockReply(String name,long time, byte blockIndex, long source, long destination){ //source = who is sending back the reply  | destination  = who originally made the request
		super(MDFSPacketType.BLOCK_REPLY, source, destination);
		this.setBroadcast(false);
		this.fileName = name;
		this.fileCreatedTime = time;
		this.blockIdx = blockIndex;
	}
	
	public byte getBlockIdx() {
		return blockIdx;
	}

	public void setBlockIdx(byte blockIdx) {
		this.blockIdx = blockIdx;
	}
	
	public List<Byte> getBlockFragIndex() {
		return fileFragIndex;
	}

	public void setBlockFragIndex(List<Byte> fileFragIndex) {
		this.fileFragIndex = fileFragIndex;
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileCreatedTime() {
		return fileCreatedTime;
	}
	
	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}

}
