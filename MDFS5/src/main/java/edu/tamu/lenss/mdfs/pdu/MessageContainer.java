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
import edu.tamu.lenss.mdfs.models.MDFSPacketType;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

/**
 * This is an abstract class. The application layer extends this class to create a customized packet. <Br>
 * The packetType defines the type in application sense.
 * @author Jay
 */
public abstract class MessageContainer implements Serializable{
	private static final long serialVersionUID = 1L;
	private byte packetType; 
	private long dest, source;
	private boolean broadcast=false;
	
	public MessageContainer(){
	}
	
	public MessageContainer(byte pType){
		this.packetType = pType;
	}
	
	public MessageContainer(byte pType, long src, long desti){
		this.packetType = pType;
		this.source = src;
		this.dest = desti;
	}
	
	
	public String getPacketTypeReadable() {
		return MDFSPacketType.TypeName[packetType];
	}
	
	public byte getPacketType() {
		return packetType;
	}
	
	public void setPacketType(byte packetType) {
		this.packetType = packetType;
	}

	public long getDest() {
		return dest;
	}
	
	public String getDestReadable() {
		return IOUtilities.long2Ip(dest);
	}
	
	public void setDest(long dest) {
		this.dest = dest;
	}

	public long getSource() {
		return source;
	}
	
	public String getSourceReadable() {
		return IOUtilities.long2Ip(source);
	}
	
	public void setSource(long source) {
		this.source = source;
	}


	public boolean isBroadcast() {
		return broadcast;
	}

	public void setBroadcast(boolean broadcast) {
		this.broadcast = broadcast;
	}

	public <T extends MessageContainer> byte[] toByteArray(T type){
		ByteArrayOutputStream byteStr = new ByteArrayOutputStream(8192);
		try {
			//ObjectOutputStream output = new ObjectOutputStream(byteStr);
			/*
			 * This is a Bug in KitKat (4.X.X). If calls GZIPOutputStream flush/finish, exception occurs! 
			 * Can be removed in Android 5.0 or older devices
			 */
			GZIPOutputStream gout = new GZIPOutputStream(byteStr, false);
			ObjectOutputStream output = new ObjectOutputStream(gout);
			output.writeObject(type);
			output.flush();
			gout.finish();
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
	 * This method has to be overrode by children
	 * @return
	 */
	public abstract byte[] toByteArray();
	
	
	/**
	 * Generic Method
	 * @param packetData
	 * @param type
	 * @return
	 */
	public static <T extends MessageContainer> T parseFromByteArray(byte[] packetData, Class<T> type){
		T packet=null;
		try {
			ByteArrayInputStream byteStr = new ByteArrayInputStream(packetData);
			//ObjectInputStream input = new ObjectInputStream(byteStr);
			GZIPInputStream gin = new GZIPInputStream(byteStr);
			ObjectInputStream input = new ObjectInputStream(gin);
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
	}
}
