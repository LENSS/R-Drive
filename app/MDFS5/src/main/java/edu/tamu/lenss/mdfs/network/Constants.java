package edu.tamu.lenss.mdfs.network;


public class Constants {
	
	//Valid node address interval
	public static final int MAX_VALID_NODE_ADDRESS = 254;
	public static final int MIN_VALID_NODE_ADDRESS = 0;
	public static final int BROADCAST_ADDRESS = 255;
	public static final int MULTICAST_ADDRESS_START = 224;
	public static final int MULTICAST_ADDRESS_END = 239;
	
	//Broadcast ID
	/*public static final int MAX_BROADCAST_ID = Integer.MAX_VALUE;
	public static final int FIRST_BROADCAST_ID = 0;
	public static final int BROADCAST_CACHE_SIZE = 15;*/
	
	// user package type
	public static final byte USER_DATA_PACKET_PDU = 0;
	
	// hello package type
	public static final byte HELLO_PDU = 6;
	
	public static final byte BROADCAST_PDU = 7;
	
	//the time to wait between each hello message sent
	public static final int BROADCAST_INTERVAL = 30000;
	
	// user package max size equivalent 54kb
	public static final int UDP_MAX_PACKAGE_SIZE = 1500;
	
	//alive time for a route. The value depends on the mobility 
	//public static int ROUTE_ALIVETIME = (int) (BROADCAST_INTERVAL*1.1);	// 10% time skew.  	
	//public static final int MAX_NUMBER_OF_RREQ_RETRIES = 2;
	
	//the amount of time to store a RREQ entry before the entry dies
	//public static int PATH_DESCOVERY_TIME = 1000*10;
	
	public static final int FILE_SYNC_PERIOD = 1*60000;	// Each file has to be synchronized at least every 1 min
	public static final int FILE_DEL_PERIOD = 60*60000; //  The file delete record is kept for an hour
	public static final int MAX_CONNECTIONS = 10;
	public static final long SOCKET_MAX_ALIVE_TIME = 120000;
	
	public static final int UDP_SNT_PORT = 8881;
	public static final int UDP_RCV_PORT = 8888;
	public static final int UDP_BD_PORT = 1357;	// Broadcast Port
	public static final int UDP_BUFFER_SIZE = 1500;
	
	public static final int TCP_RCV_PORT = 8877;
	public static final int TCP_COMM_BUFFER_SIZE = 128;
	public static final int TCP_RECEIVE_TO = 30000;	// TCPSend Read TimeOut
	public static final int TCP_SEND_READ_TO = 30000;	// TCPSend Read TimeOut
	public static final int TCP_FORWARD_READ_TO = 60000;	// TCPSend Read TimeOut
	public static final int TCP_PACKET_TIMEOUT = 4000;
	
	public static final String DIR_LOG = "MDFSLog";
	
	public static final String MESH_AP_KEYWORD = "324-Amin-router";
	
	public static final boolean RELAY_BROADCAST = true;
	
	//public static final String[] SUBNET_ADDS = {"192.168.1.255"};
	
	/*
	 * Hack
	 */
	/*public static void setRREQTimeout(int t){
		PATH_DESCOVERY_TIME = t;
	}
	
	public static void setRouteTimeout(int t){
		ROUTE_ALIVETIME = t;
	}*/
	
}
