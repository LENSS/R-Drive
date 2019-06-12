package edu.tamu.lenss.mdfs.network;

import java.io.IOException;

import edu.tamu.lenss.mdfs.network.TCPReceive.TCPReceiverListener;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 * This class is responsible for managing incoming TCP connection. Both send and receive. 
 * @author Jay
 */
public class TCPConnection {
	private static final String TAG = TCPConnection.class.getSimpleName();
	private TCPReceive tcpReceiver ;
	
	/**
	 * Can't be extended
	 */
	public TCPConnection(TCPReceiverListener listener){
		tcpReceiver = new TCPReceive(listener, Constants.TCP_RCV_PORT);
	}
	
	/**
	 * Once released, the old instance of TCPConnection can't be reused anymore.
	 */
	public void release(){
		if(tcpReceiver != null)
			tcpReceiver.close();
		tcpReceiver = null;
	}
	
	public void init(){
		try {
			tcpReceiver.init();
			tcpReceiver.start();
		} catch (IOException e) {
			e.printStackTrace();
			Logger.e(TAG, e.toString());
		}
	}
	
	/**
	 * Blocking function call
	 * @param ip
	 * @return return null if the connection fails
	 */
	public static TCPSend creatConnection(String ip) {
		TCPSend send = new TCPSend(ip, Constants.TCP_RCV_PORT);
		if(send.init())
			return send;
		else 
			return null;
	}
	
}
