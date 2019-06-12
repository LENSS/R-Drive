package edu.tamu.lenss.mdfs.handler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.tamu.lenss.mdfs.network.Constants;
import edu.tamu.lenss.mdfs.network.UdpReceiver;
import edu.tamu.lenss.mdfs.pdu.BroadcastPacket;
import edu.tamu.lenss.mdfs.pdu.HelloPacket;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.pdu.Packet;
import edu.tamu.lenss.mdfs.pdu.UserDataPacket;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

public class Receiver implements Runnable {
	private static final String TAG = Receiver.class.getSimpleName();
	private static final int BrdLLSize = 20;	// Broadcast LinkedList size
	private Queue<Message> receivedMessages;
	private UdpReceiver udpReceiver;
	private long nodeAddress;
	private Thread receiverThread;
	private UDPReceiverListener udpListener;
	private LinkedList<Integer> rcvBroadcast;	// Used to track the received broadcast UDP
	//private AndroidDataLogger dataLogger;
	

    //private MDFSNode parent;
	private volatile boolean keepRunning = true;

	public Receiver(String nodeIP) {
		//this.parent = parent;
		this.nodeAddress = IOUtilities.ipToLong(nodeIP);
		receivedMessages = new ConcurrentLinkedQueue<Message>();
		udpReceiver = new UdpReceiver(this);
		rcvBroadcast = new LinkedList<Integer>();
		for(int i=0; i < BrdLLSize; i++)
			rcvBroadcast.addFirst(i);
		//dataLogger = parent.getDatalogger();
	}
	
	protected void addListener(UDPReceiverListener list){
		this.udpListener = list;
	}

	protected synchronized void startThread(){
		keepRunning = true;
		udpReceiver.startThread();
		receiverThread = new Thread(this, Receiver.class.getSimpleName());
		receiverThread.start();
	}
	
	/**
	 * Stops the receiver thread.
	 */
	protected synchronized void stopThread() {
		keepRunning = false;
		receivedMessages.clear();
		rcvBroadcast.clear();
		udpReceiver.stopThread();
		receiverThread.interrupt();
	}
	
	public void run() {
		Logger.v(TAG, "Receiver Thread started");
		while(keepRunning) {
			synchronized (receivedMessages) {
				while (receivedMessages.isEmpty()) {
					try {
						receivedMessages.wait();
					} catch (InterruptedException e) {
						//e.printStackTrace();
						Thread.currentThread().interrupt(); 
					}
				}
			}

			Message msg = receivedMessages.poll(); 
			if(msg == null)
				continue;
			else if(msg.sourceIp == nodeAddress){
				Logger.w(TAG, "Receive Packet from myself");
			}
			
			
			switch (msg.pduType) {
			case Constants.HELLO_PDU:
				HelloPacket hello = HelloPacket.parseFromByteArray(msg.packetRawByte, HelloPacket.class);
				helloMessageReceived(hello);
				break;
			case Constants.BROADCAST_PDU:
				BroadcastPacket brdPkt = BroadcastPacket.parseFromByteArray(msg.packetRawByte);
				broadcastPacketReceived(brdPkt);
				break;
			case Constants.USER_DATA_PACKET_PDU:
				UserDataPacket userDataPacket = UserDataPacket.parseFromByteArray(msg.packetRawByte, UserDataPacket.class);
				userDataPacketReceived(userDataPacket, msg.sourceIp);
				break;
			default:
				// The received message is not in the domain of protocol messages
				Logger.w(TAG, "Unrecognized Packet Type");
				break;
			}
		}
	}

	/**
	 * Method used by the lower network layer to queue messages for later processing
	 * 
	 * @param senderNodeAddress Is the address of the node that sent a message
	 * @param msg is an array of bytes which contains the sent data
	 */
	public void addMessage(byte[] msg) { 
		receivedMessages.add(new Message(msg));
		synchronized (receivedMessages) {
			receivedMessages.notify();
		}
		//Logger.v(TAG, "Receiver Message LL size: " + receivedMessages.size());
	}

	private Map<Long, Long> helloMap = new HashMap<Long, Long>();
	/**
	 * Handles a HelloHeader, when such a message is received from a neighbor <br>
	 * @param hello is the HelloHeader message received
	 */
	private void helloMessageReceived(HelloPacket hello) {
		long src = hello.getSourceIPLong();
		long time = System.currentTimeMillis();
		
		helloMap.put(src, time);
	}
	
	
	private void broadcastPacketReceived(BroadcastPacket packet){
		int hashCode = packet.getUUID();
		// Only react if this is a newly received broadcast packet
		if(!rcvBroadcast.contains(hashCode)){
			rcvBroadcast.removeLast();
			rcvBroadcast.addFirst(hashCode);
			if(udpListener != null)
				udpListener.onNewPacket(packet.getSourceIPString(), packet.getMessage());		
		}
	}

	/**
	 * Handles a userDataPacket when received
	 * @param userData is the received packet
	 * @param senderNodeAddress the originator of the message
	 */
	private void userDataPacketReceived(UserDataPacket userData, long senderAddress) {
		// UserDataPacket is used for un-cast communication. 
		if(udpListener != null)
			udpListener.onNewPacket(userData.getSourceIPString(), userData.getMessage());
	}
	
	/**
	 * Used to send back data 
	 */
	public static interface UDPReceiverListener{
		public void onNewPacket(String senderNodeAddess, MessageContainer data);
	}
	

	/**
	 * A class that contains the received raw data from network layer (UDP). Byte[] stores the serialized Packet.class
	 * Objects of this type are stored in a receiving queue for later processing
	 */
	private class Message {
		public byte[] packetRawByte;
		public byte pduType;
		public long sourceIp; // For faster process purpose. This information is available from packetRawByte as well
		
		public Message(byte[] data) {
			packetRawByte = data;
			parseRawData();
		}
		
		public void parseRawData(){
			Packet p = Packet.parseFromByteArray(packetRawByte, Packet.class);
			this.pduType = p.getPduType();
			this.sourceIp = p.getSourceIPLong();
		}
	}
}
