package edu.tamu.lenss.mdfs.handler;

import android.os.Environment;

import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.EdgeKeeper.server;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.handler.Receiver.UDPReceiverListener;
import edu.tamu.lenss.mdfs.network.TCPConnection;
import edu.tamu.lenss.mdfs.network.TCPReceive.TCPReceiverData;
import edu.tamu.lenss.mdfs.network.TCPReceive.TCPReceiverListener;
import edu.tamu.lenss.mdfs.pdu.BroadcastPacket;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.pdu.UserDataPacket;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.network.RsockReceiveForFileCreation;   //ROSK
import edu.tamu.lenss.mdfs.network.RsockReceiveForFileRetrieval;  //ROSK

import static java.lang.Thread.sleep;

/**
 * PacketExchanger is is responsible starting/stopping the UDP/TCP/Rsock sender and receiver threads. <Br>
 * The received packets/data from various sources are all collected here and forwarded to the observer class, NetworkObserver. 
 *
 * @author Jay
 */
public class PacketExchanger extends Observable {
	private static final String TAG = PacketExchanger.class.getSimpleName();
	private TCPConnection tcpConnection;
	private Sender sender;
	private Receiver receiver;
	private Queue<DataToObserver> messagesForObservers;
	private volatile boolean keepRunning = true;
	private static PacketExchanger instance = null;
	private MessageQueueChecker checker;


	/**
	 * Creates an instance of the Node class
	 * @param nodeAddress
	 */
	private PacketExchanger(String nodeAddress) {
		messagesForObservers = new ConcurrentLinkedQueue<DataToObserver>();
		sender = new Sender(nodeAddress);
		receiver = new Receiver(nodeAddress);
		tcpConnection = new TCPConnection(tcpRcvListener);
		checker = new MessageQueueChecker();

		//if file creation or retrieval is via rsock, then we need gns, so init gns first in GNS.java file		//RSOCK
		if(Constants.file_creation_via_rsock_or_tcp.equals("rsock") || Constants.file_retrieval_via_rsock_or_tcp.equals("rsock")) {
			GNS.getGNSInstance();
			GNS.gnsServiceClient.addService(Constants.GNS_s, Constants.GNS_s1);
		}

		try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }

		//if file creation via rsock is enabled in Constants file, start rsock in a thread   //RSOCK
		if(Constants.file_creation_via_rsock_or_tcp.equals("rsock")){
			Thread t1 = new Thread(new RsockReceiveForFileCreation());
			t1.start();
		}

		//if file retrieval via rsock is enabled in Constants file, start rsock in a thread   //RSOCK
		if(Constants.file_retrieval_via_rsock_or_tcp.equals("rsock")){
			Thread t1 = new Thread(new RsockReceiveForFileRetrieval());
			t1.start();
		}

		//check if I am the dummy EdgeKeeper server.
		if(Constants.dummy_EdgeKeeper_ip.equals(Constants.my_wifi_ip_temp)){
			server server = new server(Constants.dummy_EdgeKeeper_port);
			server.start();
		}


	}

	/**
	 * Allow only specific class to use this singleton
	 */
	protected static synchronized PacketExchanger getInstance(String localIp) {
		if (instance == null) {
			if(IOUtilities.validateIP(localIp)){
				instance = new PacketExchanger(localIp);
				Logger.v(TAG, "New " + PacketExchanger.class.getSimpleName() + " is created");
			}
			else{
				Logger.e(TAG, "Invalid IP. Fail to initialize PacketExchanger");
			}
		}
		return instance;
	}

	public static synchronized PacketExchanger getInstance(){
		return instance;
	}

	/**
	 * Starts executing the AODV routing protocol
	 */
	protected synchronized void startThread(){
		keepRunning = true;
		sender.startThread();
		receiver.addListener(udpRcvListener);
		receiver.startThread();
		tcpConnection.init();
		//checker = new MessageQueueChecker();
		checker.start();
		Logger.i(TAG, "All TCP/UDP/RSOCK threads are running");  //RSOCK
	}

	protected synchronized void resetAll(){
		stopThread();
		try {
			Thread.sleep(1200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sender = new Sender(ServiceHelper.getInstance().getNodeManager().getMyIpString());
		receiver = new Receiver(ServiceHelper.getInstance().getNodeManager().getMyIpString());
		tcpConnection = new TCPConnection(tcpRcvListener);
		checker = new MessageQueueChecker();
		startThread();
	}

	/**
	 * Stops the UDP/TCP communication and release the Node singleton
	 * Note: using this method tells the running threads to terminate.
	 * This means that it does not ensure that any remaining userpackets is sent before termination.
	 * Such behavior can be achieved by monitoring the notifications by registering as an observer.
	 */
	protected synchronized void stopThread(){
		keepRunning = false;
		receiver.stopThread();
		sender.stopThread();
		checker.interrupt();
		tcpConnection.release();
		messagesForObservers.clear();
		Logger.i(TAG, "All TCP/UDP threads stop");
	}

	protected void shutdown(){
		stopThread();
		instance=null;
	}


	//SENDING
	public void sendMsgContainer(MessageContainer packet){
		if(packet.isBroadcast()){
			BroadcastPacket pkt = new BroadcastPacket(packet.getSource(), packet.getDest(), packet);
			Logger.i(TAG, "Packet type " + packet.getPacketTypeReadable() + " is broadcasted");
			sender.sendGlobalBrdcastPacket(pkt); // Hacky way. To handle multiple cast in Mesh Network
		}
		else{
			UserDataPacket pkt = new UserDataPacket(packet.getSource(), packet.getDest(), packet);
			Logger.i(TAG, "Packet type " + packet.getPacketTypeReadable() + " is sent to " + packet.getDestReadable());
			sender.sendUnicastPacket(pkt);
		}
	}


	private UDPReceiverListener udpRcvListener = new UDPReceiverListener() {

		@Override
		public void onNewPacket(String senderNodeAddess, MessageContainer data) {
			messagesForObservers.add(new DataToObserver(DataToObserver.DataToObserver, data));
			synchronized (messagesForObservers) {
				messagesForObservers.notify();
			}
		}
	};

	private TCPReceiverListener tcpRcvListener = new TCPReceiverListener(){
		@Override
		public void onNewConnectioin(TCPReceiverData data) {
			setChanged();
			notifyObservers(data);
		}
	};



	/**
	 * This Object is only used to wrap the MessageContainer and send to the application layer(NETWORK OBSERVER)
	 * @author Jay
	 */
	public static class DataToObserver {
		public static final int DataToObserver = 7;
		private MessageContainer data;
		private int type;

		public DataToObserver(int t, MessageContainer d){
			this.type = t;
			this.data = d;
		}

		public int getMessageType() {
			return type;
		}

		public MessageContainer getContainedData() {
			return data;
		}

	}

	/**
	 * This private class is only used to check the mesageForObsers queue
	 * @author Jay
	 */
	private class MessageQueueChecker extends Thread{
		private MessageQueueChecker(){
			super("MessageQueueChecker");
		}

		@Override
		public void run() {
			while(keepRunning){
				try{
					synchronized (messagesForObservers) {
						while(messagesForObservers.isEmpty()){
							messagesForObservers.wait();
						}
					}
					setChanged();
					notifyObservers(messagesForObservers.poll());
					//Logger.i(TAG, "Packet  is sent to Observer");
				}catch (InterruptedException e) {
					// thread stopped
				}
			}
		}
	}
}
