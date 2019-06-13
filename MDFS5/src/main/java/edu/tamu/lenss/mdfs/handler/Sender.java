package edu.tamu.lenss.mdfs.handler;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.tamu.lenss.mdfs.network.Constants;
import edu.tamu.lenss.mdfs.network.UdpSender;
import edu.tamu.lenss.mdfs.pdu.BroadcastPacket;
import edu.tamu.lenss.mdfs.pdu.HelloPacket;
import edu.tamu.lenss.mdfs.pdu.Packet;
import edu.tamu.lenss.mdfs.pdu.UserDataPacket;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
//import adhoc.etc.AndroidDataLogger;
//import adhoc.etc.AndroidDataLogger.LogFileInfo.LogFileName;

public class Sender {
    private long nodeAddress;
    private ScheduledThreadPoolExecutor broadcastExecutor = new ScheduledThreadPoolExecutor(1);    
    
    private UdpSender udpSender;
    private static final String TAG=Sender.class.getSimpleName();
    
    public Sender(String nodeAddress)  {
        this.nodeAddress = IOUtilities.ipToLong(nodeAddress);
		udpSender = new UdpSender();
        
    }
    
    public void startThread(){
    	//startHelloBroadcast();
    }
    
    public void stopThread(){
    	broadcastExecutor.shutdown();
    	udpSender.closeSoket();
    }
    
    
	private boolean sendUserDataPacket(UserDataPacket packet) {
		if(packet.getDestinationIPLong() == nodeAddress){
			Logger.w(TAG, "Sending packet to myself: " + nodeAddress);
		}
		Logger.e(TAG, "Some one calls sendUserDataPacket()");
		return udpSender.sendPacket(packet.getDestIPString(), packet.toBytes());
	}
    
    
    protected void sendBrdcastPacket(Packet packet){   
    	/*StringBuilder str = new StringBuilder();
		str.append(System.currentTimeMillis() + ", ");
		str.append(packet.getPduType() + ", " + packet.getPduType() + ", " );
		str.append(parent.getNodeId() + ", " + packet.getDestinationAddress());
		str.append("\n");*/
		//dataLogger.appendSensorData(LogFileName.PACKET_SENT, str.toString());
		
		if(packet instanceof HelloPacket || packet instanceof BroadcastPacket){
			packet.setDestinationIPLong(NodeManager.getClasCBroadcastIpLong(
					ServiceHelper.getInstance().getNodeManager().getMyIpString()));
			packet.setSourceIPLong(ServiceHelper.getInstance().getNodeManager().getMyIP());
			Logger.e(TAG, "Some one calls sendBrdcastPacket()");
			udpSender.sendPacket(packet.getDestIPString(), packet.toBytes());
		} 
		else {
			Logger.e(TAG, "Sender queue contains an unknown broadcast message Packet!");
		}
    }
    
    /**
     * This particular function is for Mesh network across multiple subnets. <Br> 
     * By default, UDP broadcast is limited to only one subnet, this function forwarded packets to all other 
     * pre-defined subnets. 
     * Hacky way to handle multiple subnets.
     */
    protected void sendGlobalBrdcastPacket(Packet packet){   
		if(packet instanceof HelloPacket || packet instanceof BroadcastPacket){
			packet.setSourceIPLong(ServiceHelper.getInstance().getNodeManager().getMyIP());
			
			if(Constants.RELAY_BROADCAST){
				String brdIp;
// 				for(int i=1; i <=10; i++){
// 					brdIp = "192.168." + i + ".255";
 					brdIp = "192.168.0.255";
//					multicast seems not working, not sure why. But 192.168.255.255/16 (subnet mask is 255.255.0.0) should do the trick!!!
//					brdIp = "224.0.0.1";//Use multicast now, needs to be verified in a MESH network! by Wei 11/12/17
					packet.setDestinationIPLong(IOUtilities.ipToLong(brdIp));
					udpSender.sendPacket(brdIp, packet.toBytes());
// 				}
			}
			// In case I'm not in a subnet belonging to the Mesh
//			packet.setDestinationIPLong(NodeManager.getClasCBroadcastIpLong(
//					ServiceHelper.getInstance().getNodeManager().getMyIpString()));
//			Logger.e(TAG, "Some one calls sendGlobalBrdcastPacket()");
//			udpSender.sendPacket(packet.getDestIPString(), packet.toBytes());
		} 
		else {
			Logger.e(TAG, "Sender queue contains an unknown broadcast message Packet!");
		}
    }
    
    protected void sendUnicastPacket(UserDataPacket userData){
    	/*StringBuilder str = new StringBuilder();
		str.append(System.currentTimeMillis() + ", ");
		str.append("userdata, " + userData.getUserDataType() + ", ");
		str.append(parent.getNodeId() + ", " + userData.getDestinationAddress() + ", ");
		str.append("\n");*/
		
		if(!sendUserDataPacket(userData)){
			Logger.w(TAG, "Fail to send a UDP Unicast packet");
		} 
		
    }
    
	private void startHelloBroadcast() {
		broadcastExecutor.scheduleAtFixedRate(new Runnable() {
			final HelloPacket hello = new HelloPacket();
			@Override
			public void run() {
				hello.update();
				sendBrdcastPacket(hello);
			}
		}, 0, Constants.BROADCAST_INTERVAL, TimeUnit.MILLISECONDS);
	}
}
