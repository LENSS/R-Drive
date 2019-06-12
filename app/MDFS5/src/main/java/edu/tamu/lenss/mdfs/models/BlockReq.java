package edu.tamu.lenss.mdfs.models;

import java.util.List;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;

public class BlockReq extends MessageContainer {

	private static final long serialVersionUID = 1L;
	private String fileName;
	private long fileCreatedTime;
	private byte blockIdx;
	private List<Byte> blockFragIndex;
	private boolean anyAvailable;
	
	/**
	 * This class will be broadcasted to the MDFSNetwork and query for key and file fragments.
	 * @param name
	 * @param time
	 */
	public BlockReq(String name, long time, byte blockIndex){
		super(MDFSPacketType.BLOCK_REQ,
				ServiceHelper.getInstance().getNodeManager().getMyIP(),
				NodeManager.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
		this.setBroadcast(true);
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
		return blockFragIndex;
	}

	public void setBlockFragIndex(List<Byte> fileFragIndex) {
		this.blockFragIndex = fileFragIndex;
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileCreatedTime() {
		return fileCreatedTime;
	}

	
	public boolean isAnyAvailable() {
		return anyAvailable;
	}

	/**
	 * Set this bit if the requester wants Any available fragments of this file.
	 * @param anyAvailable
	 */
	public void setAnyAvailable(boolean anyAvailable) {
		this.anyAvailable = anyAvailable;
	}

	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}

}
