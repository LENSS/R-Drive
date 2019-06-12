package edu.tamu.lenss.mdfs.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 * This is a single UDPSender. All data, including broadcast, are sent through a single port though the same thread 
 */
public class UdpSender {
 	private DatagramSocket datagramSocket;
//	private MulticastSocket datagramSocket;
	private ExecutorService pool;
	private static final String TAG = UdpSender.class.getSimpleName();
	
	public UdpSender(){
	    try {
 			datagramSocket = new DatagramSocket(Constants.UDP_SNT_PORT); 
//			datagramSocket = new MulticastSocket(Constants.UDP_SNT_PORT); 
		} catch (IOException e) {
			e.printStackTrace();
		}
	   this.pool = Executors.newCachedThreadPool();
	}

	
	private InetAddress ipAddress;
	private byte[] data = new byte[Constants.UDP_MAX_PACKAGE_SIZE];
	private DatagramPacket thePacket = new DatagramPacket(data, data.length, ipAddress, Constants.UDP_BD_PORT);
	
	
	/**
	 * Send broadcast packet using UDP, and unicast packet using TCP
	 * Non-blocking call
	 * @param destinationNodeID indicates the ID of the receiving node. Should be a positive integer.
	 * @param data is the message which is to be sent. It is serialized from Packet type
	 */
	public boolean sendPacket(final String destinationIP, final byte[] data)	{
		Future<Boolean> result = pool.submit(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				return sendPacketBlock(destinationIP, data);
			}
		});
		try {
			return result.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Blocking call for sending a UDP broadcast packet
	 * @param destinationIP
	 * @param data
	 * @return
	 */
	private boolean sendPacketBlock(String destinationIP, final byte[] data)	{
		// do we have a packet to be broadcasted?
		try {
			int ipLastSeg = IOUtilities.parseNodeNumber(destinationIP);
//			int ipSeg = IOUtilities.parseNetSegment(destinationIP);
//			Logger.i(TAG + "sendPacketBlock()", "" + ipSeg);
			ipAddress = InetAddress.getByName(destinationIP);
			if (ipLastSeg == Constants.BROADCAST_ADDRESS){ 
//			if (ipSeg >= Constants.MULTICAST_ADDRESS_START && ipSeg <= Constants.MULTICAST_ADDRESS_END){ 
 				datagramSocket.setBroadcast(true);
				thePacket.setPort(Constants.UDP_BD_PORT);		
				thePacket.setData(data);
				thePacket.setAddress(ipAddress);	
				// Hack: Increase broadcast reliability. Probably not helpful

 				for(int i=0; i < 3; i++){
					datagramSocket.send(thePacket); 
 				}
				//Logger.v(TAG, "UDP data of length: " + data.length	+ " bytes is broadcasted");

			}
			else/* if (ipSeg > 0)*/{
				//sendPacket.setPort(Constants.UDP_RCV_PORT);
				// TCP
				pool.execute(new TCPPacketSender(destinationIP, data));
			}
//			else {
//				Logger.e(TAG + "sendPacketBlock()", "Not a valid IP address");
//				return false;
//			}
			
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	public void closeSoket(){
		if(!datagramSocket.isClosed())
			datagramSocket.close();
		pool.shutdown();
	}
	
	private class TCPPacketSender implements Runnable{
		private byte[] data;
		private String destIPStr;
		public TCPPacketSender(String destinationIP, final byte[] dataIn){
			this.destIPStr = destinationIP;
			this.data = dataIn;
		}
		
		@Override
		public void run() {
			Socket tcpSocket=null;
			try {
				InetAddress destIP = InetAddress.getByName(destIPStr);
				tcpSocket = new Socket(destIP, Constants.UDP_RCV_PORT);
				tcpSocket.getOutputStream().write(data);
				//Logger.v(TAG, "data of length: " + data.length + " bytes is sent to " + tcpSocket.getInetAddress());
			} catch (IOException e) {
				e.printStackTrace();
				Logger.v(TAG, " Error in TCPSocket in UDPSender when write");

			} finally{
				if(tcpSocket != null){
					try {
						tcpSocket.getOutputStream().flush();
						tcpSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
						Logger.e(TAG, " Error in TCPSocket  in UDPSender when close");
					}
				}
			}
		}
	}
}
