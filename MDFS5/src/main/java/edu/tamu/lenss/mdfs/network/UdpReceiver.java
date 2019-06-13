package edu.tamu.lenss.mdfs.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.tamu.lenss.mdfs.handler.Receiver;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 * This class starts two threads. One listening for UDP broadcast, the other listens for UDP uni-cast packets
 * @author Jay
 * 
 */
public class UdpReceiver {
	private Receiver parent;
	private UdpPacketReceiver udpBroadcastReceiver;
	private TCPThread tcpDataPktReceiver;
	public static final String TAG = UdpReceiver.class.getSimpleName();
	

	public UdpReceiver(Receiver parent){
		this.parent = parent;
		udpBroadcastReceiver = new UdpPacketReceiver(Constants.UDP_BD_PORT);
		tcpDataPktReceiver = new TCPThread(Constants.UDP_RCV_PORT);
	}

	public void startThread() {
		udpBroadcastReceiver.startReceiverthread("Broadcast_" + UdpReceiver.class.getSimpleName());
		tcpDataPktReceiver.startReceiverthread("UdpReceiver_" + UdpReceiver.class.getSimpleName());
	}

	public void stopThread() {
		udpBroadcastReceiver.stopReceiverThread();
		tcpDataPktReceiver.stopReceiverThread();
	}
	

	/**
	 * Use different port to receive broadcast packet.
	 * udppacketreceiver
	 */
	private class UdpPacketReceiver implements Runnable {
		private DatagramSocket rcvdatagramSocket;
		private volatile boolean keepReceiving = true;
		private Thread udpReceiverThread;

		public UdpPacketReceiver(int receiverPort) {
			try {
				rcvdatagramSocket = new DatagramSocket(receiverPort);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		public synchronized void startReceiverthread( String name ) {
			keepReceiving = true;
			udpReceiverThread = new Thread(this, name); 
			udpReceiverThread.start();
			Logger.i(TAG, "UdpReceiver started");
		}

		private synchronized void stopReceiverThread() {
			keepReceiving = false;
			if (!rcvdatagramSocket.isClosed())
				rcvdatagramSocket.close();
			udpReceiverThread.interrupt();
		}
		
		private byte[] bcBuffer = new byte[Constants.UDP_MAX_PACKAGE_SIZE];
		private DatagramPacket datagramPacket = new DatagramPacket(
				bcBuffer, bcBuffer.length);
		private byte[] pktData;
		private long senderIp;
		private int dataLen;

		
		@Override
		public void run() {
			Logger.i(TAG, "UdpPacketReceiver thread is running");
			while (keepReceiving) {
				try {
					if(rcvdatagramSocket == null || rcvdatagramSocket.isClosed()){
						Logger.e(TAG, "Udp socket is null");
						break;
					}
					rcvdatagramSocket.receive(datagramPacket);
					dataLen = datagramPacket.getLength();
					pktData = new byte[dataLen]; 
					System.arraycopy(datagramPacket.getData(), 0, pktData, 0, dataLen);	
					
					senderIp = IOUtilities.ipToLong(datagramPacket.getAddress().getHostAddress()); 
					if(senderIp != ServiceHelper.getInstance().getNodeManager().getMyIP()){
						Logger.d(TAG, "Receive " + dataLen + " bytes of UDP broadcast from " + datagramPacket.getAddress().getHostAddress());
						Logger.d(TAG, "MYIP:  " + IOUtilities.long2Ip(ServiceHelper.getInstance().getNodeManager().getMyIP()));
						parent.addMessage(pktData);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}



	/**
	 * This thread waits for incoming packet and starts a new TCPPacketReceiver thread to receive the data 
	 * @author Jay
	 */
	private class TCPThread implements Runnable {
		private volatile boolean keepReceiving = true;
		private Thread tcpReceiverThread;
		private ServerSocket tcpSocket;
		private ExecutorService pool;
		
		public TCPThread(int tcpRcvPort){
			try {
				tcpSocket = new ServerSocket(tcpRcvPort);
			} catch (IOException e) {
				e.printStackTrace();
			}
			pool = Executors.newCachedThreadPool();
		}
		
		public synchronized void startReceiverthread( String name ) {
			keepReceiving = true;
			tcpReceiverThread = new Thread(this, name); 
			tcpReceiverThread.start();
		}

		private synchronized void stopReceiverThread() {
			keepReceiving = false;
			try {
				if (!tcpSocket.isClosed())
					tcpSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			pool.shutdown();
			tcpReceiverThread.interrupt();
		}

		@Override
		public void run() {
			Logger.i(TAG, "TCPThread thread is running");
			while(keepReceiving) {
				if(tcpSocket == null || tcpSocket.isClosed()){
					Logger.e(TAG, "tcpSocket is null");
					break;
				}
				try {
					Socket socket = tcpSocket.accept();
					Logger.d(TAG, "Receive Connection from " + socket.getRemoteSocketAddress());
					pool.execute(new TcpPacketReceiver(socket));
					
				} catch (IOException e) {
					Logger.e(TAG, e.toString());
				}
			}
		}
	}
	
	/**
	 * A thread class responsible for receiving one data packet 
	 * @author Jay
	 */
	private class TcpPacketReceiver implements Runnable{
		private final Socket tcpSocket;
		
		public TcpPacketReceiver(Socket socket){
			tcpSocket = socket;
		}
		
		@Override
		public void run() {
			Logger.i(TAG, "TcpPacketReceiver thread is running");
			final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			int len;
			byte[] readBuf  = new byte[512];
			
			try {
				while((len = tcpSocket.getInputStream().read(readBuf)) >= 0){
					byteStream.write(readBuf, 0, len);
				}
				byteStream.flush();
				readBuf = byteStream.toByteArray();
				Logger.d(TAG, "Receive Packet of length " + readBuf.length + " bytes from " + tcpSocket.getRemoteSocketAddress());
				parent.addMessage(readBuf);				 
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				try {
					byteStream.close();
					tcpSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
