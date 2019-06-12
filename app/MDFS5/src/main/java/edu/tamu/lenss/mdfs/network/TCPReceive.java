package edu.tamu.lenss.mdfs.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.network.TCPControlPacket.TCPPacketType;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MyTextUtils;



public class TCPReceive extends Thread {
	private static final String TAG = TCPReceive.class.getSimpleName();
	private ServerSocket tcpSocket;
	private int localPort;
	private String localIp;
	private TCPReceiverListener listener;
	private volatile boolean isRunning = true;
	private ExecutorService pool;
	
	private enum ConnectionType{
		TCPReceive,
		Error
	}
	
	protected TCPReceive(TCPReceiverListener lis, int port){
		this.localPort = port;
		this.listener = lis;
		this.pool = Executors.newCachedThreadPool();
	}	
	
	protected void init() throws IOException{
		Logger.i(TAG, "init() get called");
		localIp = ServiceHelper.getInstance().getNodeManager().getMyIpString();
		tcpSocket = new ServerSocket(localPort);
	}
	
	/**
	 * TCP Connections come in sequentially, and we accept them one by one. 
	 */
	@Override
	public void run(){
		Logger.i(TAG, "TCPReceive thread is running");
		while(isRunning){			
			try {
				final Socket newIncomingSocket = tcpSocket.accept();
				pool.execute(new Runnable(){
					@Override
					public void run() {
						newConnection(newIncomingSocket);
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	

	/**
	 * This is a blocking call. The method waits for the control packet from the sender as well as send back the <br>
	 * control packet. This method should be handled by a thread!
	 * @param clientSocket
	 * @return
	 */
	protected void newConnection(Socket clientSocket) {
		try {

			clientSocket.setSoTimeout(Constants.TCP_RECEIVE_TO);
			clientSocket.setSoLinger(true, 500);

			Logger.i(TAG, "Connection with " + clientSocket.getInetAddress().getHostAddress() +  " is established");
			DataInputStream in = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream out = null;
			
			// Wait for the control message
			ObjectInputStream oin = new ObjectInputStream(in);
			TCPControlPacket packet=null;
			packet = (TCPControlPacket)oin.readObject();
			if(packet == null){
				Logger.e(TAG, "Control Packet is null");
				oin.close();
				in.close();
				clientSocket.close();
			}
			
			ConnectionType type = verifyControlPacket(packet);
			// Send back a control message	
			if(type == ConnectionType.TCPReceive){
				out = new DataOutputStream(clientSocket.getOutputStream());
				sendControlPacket(out, packet.getSourceIP());
				this.listener.onNewConnectioin(new TCPReceiverData(clientSocket, in, out));	// Pass this socket to the app layer
			} 
			else{
				Logger.e(TAG, "TCPControl Packet Verification Error.");
				in.close();
				clientSocket.close();
			}
			
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		isRunning = false;
		pool.shutdown();
		if(!tcpSocket.isClosed()){
			try {
				tcpSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Verify that the TCPControlPacket is valid
	 * @param
	 * @return 
	 */
	private ConnectionType verifyControlPacket(TCPControlPacket packet) {
		ConnectionType type = ConnectionType.Error;
		if(packet == null || MyTextUtils.isEmpty(packet.getDestIP()))
			return ConnectionType.Error;
		
		if(packet.getStatus()==TCPPacketType.CreateRoute){
			if( packet.getDestIP().equalsIgnoreCase(localIp))
				type = ConnectionType.TCPReceive;
			else
				Logger.e(TAG, "Wrong destination. I'm not the destination.");
		}
		return type;
	}
	
	/**
	 * Send the TCPControlPacket to the source.
	 * @param out
	 * @throws IOException 
	 */
	private void sendControlPacket(DataOutputStream out, String destIp) throws IOException{
		TCPControlPacket packet = new TCPControlPacket();
		packet.setSourceIP(ServiceHelper.getInstance().getNodeManager().getMyIpString());
		packet.setDestIP(destIp);
		packet.setStatus(TCPPacketType.RouteEstablished);
		packet.setDestPort(localPort);
		packet.setNextHopPort(localPort);
		
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(packet);
	}
	
	/**
	 * Make sure to call close() after using the data
	 * @author Jay
	 */
	public static class TCPReceiverData{
		private Socket clientSocket;
		private DataInputStream in;
		private DataOutputStream out;
		
		protected TCPReceiverData(Socket s, DataInputStream i, DataOutputStream o){
			this.clientSocket = s;
			this.in = i;
			this.out = o;
		}
		
		public DataInputStream getDataInputStream(){
			return this.in;
		}
		
		public DataOutputStream getDataOutputStream(){
			return this.out;
		}
		
		public void close(){
			try {
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
			if(!clientSocket.isClosed()){
				try {
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Used to send back data to the parent
	 * @author Jay
	 *
	 */
	public static interface TCPReceiverListener{
		public void onNewConnectioin(TCPReceiverData data);
	}

}
