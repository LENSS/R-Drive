package edu.tamu.lenss.mdfs.models;

import java.io.Serializable;

import edu.tamu.lenss.mdfs.models.MDFSTCPHeader;

public class FragmentTransferInfo extends MDFSTCPHeader implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final byte REQ_TO_SEND = 0;
	public static final byte REQ_TO_RECEIVE = 1;
	
	private String fileName;
	private long createdTime;  //aka fileid
	private byte blockIndex, fragIndex, reqType;
	private boolean needReply=false, ready=false;
	
	public FragmentTransferInfo(String fileName, long creationTime, byte blockIdx, byte fragIdx, byte type){
		super(MDFSTCPHeader.TYPE_FRAGMENT);
		this.fileName = fileName;
		this.createdTime = creationTime;
		this.blockIndex = blockIdx;
		this.fragIndex = fragIdx;
		this.reqType = type;
	}

	public boolean isNeedReply() {
		return needReply;
	}

	public void setNeedReply(boolean needReply) {
		this.needReply = needReply;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean readyToReceive) {
		this.ready = readyToReceive;
	}

	/**
	 * File name of the fragment
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public byte getBlockIndex() {
		return blockIndex;
	}

	public void setBlockIndex(byte blockIndex) {
		this.blockIndex = blockIndex;
	}

	public byte getFragIndex() {
		return fragIndex;
	}

	public byte getReqType() {
		return reqType;
	}

}
