package edu.tamu.lenss.mdfs.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.models.TopologyDiscovery;
import edu.tamu.lenss.mdfs.utils.JCountDownTimer;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MyPair;


/**
 * This class handle send topologyDisocvery request and wait for the responses.
 * @author Jay
 */
public class TopologyHandler {
	private static final String TAG = TopologyDiscovery.class.getSimpleName();
	private boolean waitingReply = false;
	private Map<Long, NodeInfo> nodeInfoMap = new HashMap<Long, NodeInfo>(); // Map from NodeIP to NodeInfo
	private JCountDownTimer timer;
	private long last_discover_time;
	/** <dealine, TopologyListener> */
	private Set<MyPair<Long, TopologyListener>> listenerQueue2 = new HashSet<MyPair<Long, TopologyListener>>();
	
	
	public TopologyHandler() {
		this.timer = new JCountDownTimer(Constants.TOPOLOGY_DISCOVERY_TIMEOUT, Constants.TOPOLOGY_DISCOVERY_TIMEOUT) {
			@Override
			public void onTick(long millisUntilFinished) {}

			@Override
			public synchronized void onFinish() {
				/*
				 * onCompleteCallBack may invoke another job that starts topologyDiscovery 
				 * again (MDFSFileCreation and MDFSBlockCreation). Need to ensure last_discover_time 
				 * and waitingReply are updated BEFORE onCompleteCallBack
				 */
				if (waitingReply) {
					waitingReply = false;
					if (nodeInfoMap.isEmpty()){
						onErrorCallBack("Timeout");
						Logger.e(TAG, "Topology Timeout");
						
						// Restart the timer if there are still listeners in the queue
						if(!listenerQueue2.isEmpty()){
							Logger.w(TAG, "Rebroadcast topology discovery. Queue size: " + listenerQueue2.size());
							broadcastRequest();
						}
					}
					else {
						// Include myself
						long srcIP = ServiceHelper.getInstance().getNodeManager().getMyIP();
						long srcMAC = ServiceHelper.getInstance().getNodeManager().getMyMAC();
						NodeInfo curNode = new NodeInfo(srcIP, srcIP, srcMAC, (byte) 60, (byte) 2);
						nodeInfoMap.put(curNode.getSource(), curNode); 
						
						last_discover_time = System.nanoTime();
						onCompleteCallBack(nodeInfoMap);
					}
				}
			}
		};
	}

	/*
	 * Multiple topology broadcast requests can come in at the same time, but timer is only started once. <Br>
	 * Broadcast requests will be sent out multiple times, but the results will be combined in nodeInfoMap.
	 */
	public synchronized void broadcastRequest(TopologyListener lis){
		broadcastRequest(lis, Constants.TOPOLOGY_DISCOVERY_TIMEOUT);
	}
	
	private void broadcastRequest(){
		TopologyDiscovery top = new TopologyDiscovery();
		PacketExchanger.getInstance().sendMsgContainer(top);
		nodeInfoMap.clear();
		timer.cancel();
		timer.start();
		waitingReply = true;
		Logger.i(TAG, "Start new topology timer");
	}
	
	/**
	 * 
	 * @param lis
	 * @param timeout : the longest time that this node can wait for the reply (millisecond)
	 */
	public synchronized void broadcastRequest(TopologyListener lis, long timeout){
		long curTime = System.nanoTime();
		timeout*=1000000;	// Convert millisecond to nanosecond
		timeout += curTime;
		listenerQueue2.add(MyPair.create(timeout, lis));
		Logger.i(TAG, "Insert time: " + curTime + " Expired time: " + timeout);
		// Use the cache data
		// For some unknown reasons, some thread may get into this function (1st condition true) when we are still
		// waiting for reply. Is the time measurement inaccurate? Add the second condition to prevent this
		if(curTime-last_discover_time < Constants.TOPOLOGY_REBROADCAST_THRESHOLD && !waitingReply){
			Logger.w(TAG, "Return with cache");
			onCompleteCallBack(nodeInfoMap);
			return;
		}
		if(!waitingReply){
			broadcastRequest();
		}
	}

	//this function is called from netObserver.java update() function
	//this function is only called when a reply for a topology discovery has been received
	protected synchronized void receiveNewPacket(NodeInfo info){	
		if(waitingReply){
			nodeInfoMap.put(info.getSource(),info);
			timer.cancel();	// Reset the timer
			timer.start();
		}
	}
	
	private synchronized void onErrorCallBack(String msg){
		long curTime = System.nanoTime();
		
		// look for the expired listener
		for (Iterator<MyPair<Long, TopologyListener>> iter = listenerQueue2.iterator(); iter.hasNext();) {
			MyPair<Long, TopologyListener> pair = iter.next();
			if(curTime > pair.first){ // expired
				Logger.v(TAG, "Remove time: " + curTime);
				pair.second.onError(msg);
				iter.remove();
			}
		}
	}

	//when topology discovery is done and nodeInfoMap is full,this function is being called.
	//this function basically calls the onComplete() function of the TopologyListener inside discoverTopology() function,
	//in MDFSFileCreator.java class.
	private synchronized void onCompleteCallBack(final Map<Long, NodeInfo> topMap){
		final List<NodeInfo> topList = new ArrayList<NodeInfo>(topMap.values());
		Logger.i("TopologyHandler", "Network Size: " + topMap.size());
		
		/*
		 * Problematic! onComplete function may invoke another TopologyDiscovery 
		 * which then calls onCompleteCallBack immediately. This becomes a recursive call. 
		 * So, we need to ensure that all onComplete() passes the same topList
		 */
		
		// make a snapshot of the current queue and reset this queue. This queue will be modified by the recursive call from onComplete
		final List<MyPair<Long, TopologyListener>> listenerList = new ArrayList<MyPair<Long, TopologyListener>>(listenerQueue2);
		listenerQueue2.clear();		
		for(MyPair<Long, TopologyListener> pair : listenerList){
			pair.second.onComplete(topList);
		}
		
	}


	//this function is called from netObserver file
	//this function is called when a topologyDiscovery req has been received,
	//and a reply is being sent using this function.
	//source: reply sender | this node
	//destination: request sender | reply receiver | other node
	protected void receiveTopologyDiscovery(TopologyDiscovery top){
		// Failure Probability Estimation
		NodeInfo info = new NodeInfo();
		info.setSource(ServiceHelper.getInstance().getNodeManager().getMyIP());
		info.setRank((byte)Math.round(Math.random()*100));
		info.setFailureProbability(ServiceHelper.getInstance().getNodeStatusMonitor().getFailureProbability());
		info.setBatteryLevel((byte)Math.round(ServiceHelper.getInstance().getNodeStatusMonitor().getBatteryLevel()*100.0));
		info.setDest(top.getSource());
		PacketExchanger.getInstance().sendMsgContainer(info);
	}
	
	//topology listener interface
	public interface TopologyListener{
		
		/**
		 * Don't do too many works in this callback. It may block the timer thread 
		 * @param topList
		 */
		public void onError(String msg);
		
		/**
		 * Don't do too many works in this callback. It may block the timer thread 
		 * @param topList
		 */
		public void onComplete(List<NodeInfo> topList);
	}
}
