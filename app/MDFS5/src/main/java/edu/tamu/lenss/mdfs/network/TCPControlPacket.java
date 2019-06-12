package edu.tamu.lenss.mdfs.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.ArrayList;

public class TCPControlPacket implements Serializable{
	private static final long serialVersionUID = 1L;
	//private static final String TAG = TCPControlPacket.class.getSimpleName();
	private static final int BUFFER_SIZE = 4096;
	private TCPPacketType status;

	private String sourceIP="";
	private String nextHopIP="";
	private String destIP="";
	
	private int destPort;
	private int nextHopPort;
	
	private ArrayList<String> interNodes = new ArrayList<String>();
	
	public enum TCPPacketType{
		CreateRoute,
		RouteEstablished,
		RouteError		
	}
	
	public static final String CONNECTION_ESTABLISHED = "CONNECTION_ESTABLISHED";
	public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
	
	public String getSourceIP() {
		return sourceIP;
	}

	public void setSourceIP(String sourceIP) {
		this.sourceIP = sourceIP;
	}

	public String getDestIP() {
		return destIP;
	}

	public void setDestIP(String destIP) {
		this.destIP = destIP;
	}

	public String getNextHopIP() {
		return nextHopIP;
	}

	public void setNextHopIP(String nextHopIP) {
		this.nextHopIP = nextHopIP;
	}

	public String[] getInterNodes() {
		return (String[]) interNodes.toArray();
	}

	public void addInterNodes(String interNodeIP) {
		this.interNodes.add(interNodeIP);
	}
	
	public TCPPacketType getStatus() {
		return status;
	}

	public void setStatus(TCPPacketType status) {
		this.status = status;
	}
	
	public int getDestPort() {
		return destPort;
	}

	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}

	public int getNextHopPort() {
		return nextHopPort;
	}

	public void setNextHopPort(int nextHopPort) {
		this.nextHopPort = nextHopPort;
	}

	
	/**
	 * @return null if the previous node does not set this field properly
	 */
	public String getLastNode(){
		if(interNodes.isEmpty()){
			return sourceIP;
		}
		else
			return interNodes.get(interNodes.size()-1);
	}
	
	/**
	 * 
	 * @param packet
	 * @return null if fails to serialize
	 */
	public static byte[] toByteArray(TCPControlPacket packet){
		ByteArrayOutputStream byteStr = new ByteArrayOutputStream(BUFFER_SIZE);
		
		try {
			ObjectOutputStream output = new ObjectOutputStream(byteStr);
			output.writeObject(packet);
			byte[] byteData = byteStr.toByteArray();
			byteStr.close();
			output.close();
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
	public static TCPControlPacket parseFromByteArray(byte[] packetData){
		TCPControlPacket packet=null;
		try {
			ByteArrayInputStream byteStr = new ByteArrayInputStream(packetData);
			ObjectInputStream input = new ObjectInputStream(byteStr);
			packet = (TCPControlPacket) input.readObject();
			
		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return packet;
	}
	
}
