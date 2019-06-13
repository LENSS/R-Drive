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
import edu.tamu.lenss.mdfs.utils.IOUtilities;

/**
 * This is an abstract class for the primitive data received from transport layer. <Br>
 * Currently, there are only HelloPacket, BroadcastPacket, and UserPacket
 * @author Jay
 *
 */
public abstract class Packet implements Serializable{
	protected static final long serialVersionUID = 1L;
	protected byte pduType;	
	public long sourceIP, destinationIp;
	
	public Packet(){
		
	}
	
	public long getSourceIPLong() {
		return sourceIP;
	}
	
	public String getSourceIPString(){
		return IOUtilities.long2Ip(sourceIP);
	}

	public void setSourceIPLong(long sourceIP) {
		this.sourceIP = sourceIP;
	}
	
	public void setSrouceIPString(String sourceIp){
		setSourceIPLong(IOUtilities.ipToLong(sourceIp));
	}

	public long getDestinationIPLong() {
		return destinationIp;
	}
	
	public String getDestIPString(){
		return IOUtilities.long2Ip(destinationIp);
	}

	public void setDestinationIPLong(long destIP) {
		this.destinationIp = destIP;
	}
	
	public void setDestinationIPString(String destIP){
		setDestinationIPLong(IOUtilities.ipToLong(destIP));
	}

	public Packet(byte type){
		this.pduType = type;
	}
	
	public byte getPduType(){
		return this.pduType;
	}
	
	public void setPduType(byte type){
		this.pduType = type;
	}
	
	public abstract byte[] toBytes();
	
	public abstract void parseBytes(byte[] rawPdu);

	//public abstract int getDestinationAddress();
	
	/**
	 * Generic Method
	 * @param packetData
	 * @param type
	 * @return
	 */
	public static <T extends Packet> T parseFromByteArray(byte[] packetData, Class<T> type){
		T packet=null;
		try {
			ByteArrayInputStream byteStr = new ByteArrayInputStream(packetData);
			GZIPInputStream gin = new GZIPInputStream(byteStr); 
			ObjectInputStream input = new ObjectInputStream(gin);
			//ObjectInputStream input = new ObjectInputStream(byteStr);
			packet = type.cast(input.readObject());
			input.close();
			gin.close();
			byteStr.close();
			
		} catch (OptionalDataException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(ClassCastException e){
			e.printStackTrace();
		} 
		return packet;
		//return Node.getInstance().parseFromByteArray(packetData, type);
	}
	
	public <T extends Packet> byte[] toByteArray(T type, Class<T> typeClass){
		ByteArrayOutputStream byteStr = new ByteArrayOutputStream(Constants.UDP_MAX_PACKAGE_SIZE);
		try {
			/*
			 * This is a Bug in KitKat (4.X.X). If calls GZIPOutputStream flush/finish, exception occurs! 
			 * Can be removed in Android 5.0 or older devices
			 */
			GZIPOutputStream gout = new GZIPOutputStream(byteStr, false);
			ObjectOutputStream output = new ObjectOutputStream(gout);
			//ObjectOutputStream output = new ObjectOutputStream(byteStr);
			output.writeObject(type);
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
}
