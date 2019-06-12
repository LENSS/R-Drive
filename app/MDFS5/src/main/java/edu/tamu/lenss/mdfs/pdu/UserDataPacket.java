package edu.tamu.lenss.mdfs.pdu;

import java.io.Serializable;

import edu.tamu.lenss.mdfs.network.Constants;

public class UserDataPacket extends Packet implements Serializable{
	private static final long serialVersionUID = 1L;
	//private static final String TAG = UserDataPacket.class.getSimpleName();
	private MessageContainer msgContainer;	
	
	public UserDataPacket(){
		this.pduType = Constants.USER_DATA_PACKET_PDU;
	}
	
	public UserDataPacket(long sourceAddress, long destinationAddress, MessageContainer data){
		this.pduType = Constants.USER_DATA_PACKET_PDU;
		this.destinationIp = destinationAddress;
		this.msgContainer = data;
		this.sourceIP = sourceAddress;
	}
	
	/**
	 * This is a hack used to distinguish new AODVDataContainer and old userData ....bad...
	 * @param type
	 */
	public void setPDUType(byte type){
		this.pduType = type;
	}
	
	public byte getPDUType(){
		return this.pduType;
	}
	
	
	public MessageContainer getMessage(){
		return msgContainer;
	}
	
	public int getUserDataType(){
		return msgContainer.getPacketType();
	}
	
	@Override
	public byte[] toBytes() {
		return toByteArray(this, UserDataPacket.class);
	}

	@Override
	public String toString(){
		return "UserDataPacket:"+"From: "+this.getSourceIPString()+" To: "+this.getDestIPString();
	}
	
	
	@Override
	public void parseBytes(byte[] packetData) {
		
		UserDataPacket packet = parseFromByteArray(packetData, UserDataPacket.class);
		if(packet != null){
			this.msgContainer = packet.getMessage();
			this.pduType = Constants.USER_DATA_PACKET_PDU;
		}
	}

}