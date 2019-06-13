package edu.tamu.lenss.mdfs.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.network.TCPControlPacket.TCPPacketType;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 * Call close() after finishing the socket
 * @author Jay
 *
 */
public class TCPSend {
	private Socket tcpSocket; 
	private DataInputStream in;
	private DataOutputStream out;
	private String destIp;
	private int destPort;
	private static final String TAG = TCPSend.class.getSimpleName();
	//private static final int BUFFER_SIZE = Constants.TCP_COMM_BUFFER_SIZE;
	
	protected TCPSend(String ip, int port){
		this.destIp = ip;
		this.destPort = port;
	}
	
	/**
	 * Blocking function call
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	protected boolean init() {
		boolean success = false;
		try{
			tcpSocket = new Socket(destIp, destPort);
			Logger.v(TAG, "Connected with " + destIp);
			
			in = new DataInputStream(tcpSocket.getInputStream());
			out = new DataOutputStream(tcpSocket.getOutputStream());
			tcpSocket.setSoTimeout(Constants.TCP_SEND_READ_TO);
			sendControlPacket(out);
			
			TCPControlPacket ctrPkt=null;
			ObjectInputStream oin = new ObjectInputStream(in);
			ctrPkt = (TCPControlPacket)oin.readObject();
			
			
			if(ctrPkt != null){
				success = verifyControlPacket(ctrPkt);
			}
			else{
				close();
			}
			
		} catch(IOException e){
			e.printStackTrace();
			Logger.w(TAG, "Fail connection to " + destIp);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return success;
	}
	
	/**
	 * Send the TCPControlPacket to the next hop.
	 * @param out
	 * @throws IOException 
	 */
	private void sendControlPacket(DataOutputStream out) throws IOException{
		TCPControlPacket packet = new TCPControlPacket();
		packet.setSourceIP(ServiceHelper.getInstance().getNodeManager().getMyIpString());
		packet.setDestIP(destIp);
		packet.setDestPort(destPort);
		packet.setNextHopPort(destPort);
		packet.setStatus(TCPPacketType.CreateRoute);
		
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(packet);
	}
	
	/**
	 * Verify that the TCPControlPacket is valid
	 * @param packetData
	 * @return
	 */
	private boolean verifyControlPacket(TCPControlPacket packet) {
		if( packet != null &&
			packet.getStatus()==TCPPacketType.RouteEstablished &&
			packet.getSourceIP().equalsIgnoreCase(destIp)){
			
			//Logger.v(TAG, "Packet verified!!!!");
			return true;
		}
		else{
			Logger.e(TAG, "Packet verification fails!!!!");
			return false;
		}
	}
	
	public DataOutputStream getOutputStream(){
		return out;
	}
	
	public DataInputStream getInputStream(){
		return in;
	}
	
	/**
	 * Always close outputstream first. It will call flush() first and close the outputstream and socket. 
	 */
	public void close() {
		try {
			// This delay assure that all data has been transmitted. Otherwise, some packet always lost..
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(out != null){
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		if(in != null){
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(!tcpSocket.isClosed())
			try {
				tcpSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	public boolean isClosed(){
		return tcpSocket.isClosed();
	}

	public Socket getTcpSocket(){
		return tcpSocket;
	}
}
