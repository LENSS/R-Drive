package edu.tamu.lenss.mdfs.models;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.lenss.mdfs.pdu.MessageContainer;

public class NodeInfo extends MessageContainer {
	private static final long serialVersionUID = 1L;
	private byte batteryLevel, rank, failureProbability;
	private long macAdd;
	
	private List<Long> neighborsList = new ArrayList<Long>();
	
	public NodeInfo(){
		super(MDFSPacketType.NODE_INFO);
		this.setBroadcast(false);
	}
	
	public NodeInfo(long source, long destination, long macAddress){
		super(MDFSPacketType.NODE_INFO, source, destination);
		this.setBroadcast(false);
		this.macAdd = macAddress;
	}
	
	public NodeInfo(long source, long destination, long macAddress, byte battery, byte rank){
		super(MDFSPacketType.NODE_INFO, source, destination);
		this.batteryLevel = battery;
		this.rank = rank;
		this.macAdd = macAddress;		
		this.setBroadcast(false);
	}


	public byte getBatteryLevel() {
		return batteryLevel;
	}

	public void setBatteryLevel(byte batteryLevel) {
		this.batteryLevel = batteryLevel;
	}

	public byte getRank() {
		return rank;
	}

	public void setRank(byte rank) {
		this.rank = rank;
	}
	
	public long getMacAdd() {
		return macAdd;
	}

	public void setMacAdd(long macAdd) {
		this.macAdd = macAdd;
	}

	public List<Long> getNeighborsList() {
		return neighborsList;
	}

	public void setNeighborsList(ArrayList<Long> neighborsList) {
		this.neighborsList = neighborsList;
	}
	
	public void addNeighbor(long neighbor){
		this.neighborsList.add(neighbor);
	}
	
	public byte getFailureProbability() {
		return failureProbability;
	}

	public void setFailureProbability(byte failureProbability) {
		this.failureProbability = failureProbability;
	}
	
	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}

}
