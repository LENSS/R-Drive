package edu.tamu.lenss.mdfs.placement;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.MyPair;

//this class takes a list of nodes, and returns a list of nodes based on the least distance
public class PlacementHelper {
	private int k2Val, n2Val;
	private int networkSize;
	private long myIp;
	private Set<NodeInfo> nodeInfoSet;
	private Queue<MyPair<Long, Double>> distFromMe;		// Virtual distance from my node, <IP, Distance>
	private Map<Long, Integer> offerCountMap;			// Number of fragments that a node can store
	
	public PlacementHelper(Set<NodeInfo> infoSet, int n2, int k2){
		this.nodeInfoSet = infoSet;
		this.n2Val = n2;
		this.k2Val = k2;
		this.networkSize = nodeInfoSet.size();
		this.distFromMe = new PriorityQueue<MyPair<Long, Double>>(networkSize, new PQsort());
		this.offerCountMap = new HashMap<Long, Integer>();
		myIp = ServiceHelper.getInstance().getNodeManager().getMyIP();
	}
	
	public void findOptimalLocations(){
		estiamteVirtualDistance();
		allocateFrags();
	}
	
	public Map<Long, Integer> getAllocation(){
		return offerCountMap;				
	}
	
	private void estiamteVirtualDistance(){
		for(NodeInfo node : nodeInfoSet){
			double meshHopDist = (double) meshHopDistance(myIp, node.getSource());
			// perform more complicated distance calculation here
			
			distFromMe.add(MyPair.create(node.getSource(), meshHopDist));
		}
	}
	
	private void allocateFrags(){
		int cnt=0;
		while(!distFromMe.isEmpty() && cnt < n2Val){
			MyPair<Long, Double> pair = distFromMe.poll();
			offerCountMap.put(pair.first, 1);  // (ip, 1)
			cnt++;
		}
	}
	

	/**
	 * For Mesh network, if two nodes are in the same subnet, hop-count is 2, otherwise, hop-count is 4
	 * @param ip1 :
	 * @param ip2 :
	 * @return
	 */
	public static int meshHopDistance(long ip1, long ip2){
		if(ip1 == ip2)
			return 0;
		
		String subnet1 = IOUtilities.parsePrefix(IOUtilities.long2Ip(ip1));
		String subnet2 = IOUtilities.parsePrefix(IOUtilities.long2Ip(ip2));
		if(subnet1.compareToIgnoreCase(subnet2) == 0){
			return 2;
		}
		else
			return 4;
	}

	
	private class PQsort implements Comparator<MyPair<Long, Double>> {

		@Override
		public int compare(MyPair<Long, Double> one, MyPair<Long, Double> two) {
			if (one.second - two.second > 0)
				return 1;
			else if (one.second - two.second < 0)
				return -1;
			else
				return 0;
		}
	}
	
}
