package edu.tamu.lenss.mdfs.pdu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.tamu.lenss.mdfs.network.Constants;

public class BroadcastPacket extends Packet implements Serializable {
	private static final long serialVersionUID = 1L;
	private MessageContainer data;
	private int uuid;

	public BroadcastPacket(){
		this.pduType = Constants.BROADCAST_PDU;
	}
	
	/**
	 * @param src		The ID of the source node
	 * @param inData	The data in this broadcast message
	 */
	public BroadcastPacket(long srcIp, long destIp, MessageContainer inData){
		this.sourceIP = srcIp;
		this.destinationIp = destIp;
		this.data = inData;
		this.pduType = Constants.BROADCAST_PDU;
		this.uuid = this.hashCode();
	}
	
	public int getUUID(){
		return uuid;
	}
	
	public MessageContainer getMessage() {
		return data;
	}

	public void setData(MessageContainer data) {
		this.data = data;
	}
	

	@Override
	public byte[] toBytes() {
		return toByteArray(this);
	}

	@Override
	public void parseBytes(byte[] rawPdu){
	}
	
	@Override
	public String toString(){
		return "Broadcast Packet from" + this.getSourceIPString();
	}

	/**
	 * 
	 * @param packet
	 * @return null if fails to serialize
	 */
	public static byte[] toByteArray(BroadcastPacket packet){
		ByteArrayOutputStream byteStr = new ByteArrayOutputStream(Constants.UDP_MAX_PACKAGE_SIZE);
		
		try {
			//ObjectOutputStream output = new ObjectOutputStream(byteStr);
			/*
			 * This is a Bug in KitKat (4.X.X). If calls GZIPOutputStream flush/finish, exception occurs! 
			 * Can be removed in Android 5.0 or older devices
			 */
			GZIPOutputStream gout = new GZIPOutputStream(byteStr, false);
			ObjectOutputStream output = new ObjectOutputStream(gout);
			output.writeObject(packet);
			output.flush(); 
			gout.finish();
			gout.flush();
			byteStr.flush();
			byte[] byteData = byteStr.toByteArray();
			output.close(); 
			gout.close();
			byteStr.close();
			return byteData;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	

	/**
	 * 
	 * @param packetData
	 * @return null if fails to parse
	 */
	public static BroadcastPacket parseFromByteArray(byte[] packetData){
		BroadcastPacket packet=null;
		try {
			ByteArrayInputStream byteStr = new ByteArrayInputStream(packetData);
			//ObjectInputStream input = new ObjectInputStream(byteStr);
			GZIPInputStream gin = new GZIPInputStream(byteStr);
			ObjectInputStream input = new ObjectInputStream(gin);
			packet = (BroadcastPacket) input.readObject();
			input.close();
			gin.close();
			byteStr.close();
		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassCastException e){
			e.printStackTrace();
		}
		return packet;
	}
}
