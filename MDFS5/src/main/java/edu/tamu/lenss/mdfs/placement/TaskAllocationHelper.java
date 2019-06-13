package edu.tamu.lenss.mdfs.placement;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.JobReply;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.MyPair;

public class TaskAllocationHelper {
	private static final String TAG = TaskAllocationHelper.class.getSimpleName();
	private Set<JobReply> jobReplySet;
	private List<Long> fileList;
	// HashMap<nodeIp, HashMap<fileId, HashSet<blockIdx>>>
	private HashMap<Long, HashMap<Long, HashSet<Byte>>> taskAssignment = new HashMap<Long, HashMap<Long, HashSet<Byte>>>();
	
	private Map<Integer, Long> index2IpMap;
	private Map<Long, JobReply> ip2JobreplyMap;
	private Map<Integer, MyPair<Long, Byte>> blockVirIdx2FileMap;  // Map<BlockVirtualIdx, MyPair<fileId, BlockIdx>>
	private Map<Long, MDFSFileInfo> fileInfoMap;
	private int blockCnt;
	private float fixedProcessingTime;
	
	
	private int[][] distM;		// distance between each pair of nodes
	private float[][] engM;		// each row is one block, and each column is one node. Each element is the processing energy 

	public TaskAllocationHelper(Set<JobReply> jobReplySet, List<Long> fileList){
		this.jobReplySet = jobReplySet; 
		this.fileList = fileList;
		this.index2IpMap = new HashMap<Integer, Long>();
		this.ip2JobreplyMap = new HashMap<Long, JobReply>(jobReplySet.size());
		this.fileInfoMap = new HashMap<Long, MDFSFileInfo>();
		this.blockVirIdx2FileMap = new HashMap<Integer, MyPair<Long, Byte>>();
		this.distM = new int[jobReplySet.size()][jobReplySet.size()];
		this.fixedProcessingTime = (float)Constants.MAX_BLOCK_SIZE/(1024*1024)*1f; // Assume fixed processing time on all devices.
		
		int count = 0;
		for(JobReply reply : jobReplySet){
			index2IpMap.put(count, reply.getSource());
			ip2JobreplyMap.put(reply.getSource(), reply);
			count++;
		}
		
		blockCnt = 0;
		for(Long fileId : fileList){
			fileInfoMap.put(fileId, ServiceHelper.getInstance().getDirectory().getFileInfo(fileId));
			for(int i=0; i<fileInfoMap.get(fileId).getNumberOfBlocks(); i++){
				blockVirIdx2FileMap.put(blockCnt++, MyPair.create(fileId, (byte)i));
			}
		}
		
	}
	
	public void findOptimalAllocation(){
		buildDistMatrix();
		buildRetTimeMatrix();
		calculateAllocation();
	}
	
	
	private JobReply getJobByIdx(int idx){
		return ip2JobreplyMap.get(index2IpMap.get(idx));
	}
	
	private void buildDistMatrix(){
		for(int row = 0; row < jobReplySet.size(); row++){
			for(int col = row; col < jobReplySet.size(); col++){
				if(row == col)
					distM[row][col]=0;
				else{
					distM[row][col] = PlacementHelper.meshHopDistance(index2IpMap.get(row), index2IpMap.get(col));
					distM[col][row] = distM[row][col];
				}
			}
		}
	}
	
	
	private void buildRetTimeMatrix(){
		// Count the number of blocks
		this.engM = new float[blockCnt][jobReplySet.size()];
		
		for(int row = 0; row < blockCnt; row++){
			for(int col = 0; col < jobReplySet.size(); col++){
				// For each block, calculate the number of fragments I need
				engM[row][col] = estimateMinRetrievalCost(row, col);
			}
		}
	}
	
	private float estimateMinRetrievalCost(int blockIdx, int nodeIdx){
		JobReply myNode = getJobByIdx(nodeIdx);
		HashSet<Byte> myFrag = new HashSet<Byte>();
		MyPair<Long, Byte> fileBlock = blockVirIdx2FileMap.get(blockIdx);
		if(myNode.getFileBlockFragMap().containsKey(fileBlock.first)){
			if(myNode.getFileBlockFragMap().get(fileBlock.first).containsKey(fileBlock.second)){
				myFrag = myNode.getFileBlockFragMap().get(fileBlock.first).get(fileBlock.second);
				if(myFrag.size() >= fileInfoMap.get(fileBlock.first).getK2())
					return 0f;
				else{
					// Need fragments from others. Approximate by the avg hop-count (2+4)/2
					return 3*(fileInfoMap.get(fileBlock.first).getK2()-myFrag.size());
				}
			}
		}
		// this node has no fragments at all
		return 3*myFrag.size();
	}
	
	private void calculateAllocation(){
		// use engM to decide allocation
		float[][] engM2 = new float[engM.length][];
		for(int i=0; i<engM.length; i++)
			engM2[i] = engM[i].clone();
		
		
		int[] blockAssign = new int[blockCnt];
		
		for(int row = 0; row < blockCnt; row++){
			// find the minimal of this row
			int minNodeIdx=0; 
			float minVal=Float.MAX_VALUE;
			for(int col = 0; col < jobReplySet.size(); col++){
				if(engM2[row][col] < minVal){
					minVal = engM2[row][col];
					minNodeIdx = col;
				}
			}
			blockAssign[row] = minNodeIdx; 
			
			// Update the entire column
			for(int r = row; r < blockCnt; r++)
				engM2[r][minNodeIdx] = engM2[r][minNodeIdx] + engM[row][minNodeIdx] + fixedProcessingTime;
		}
		
		
		for(JobReply reply : jobReplySet){
			taskAssignment.put(reply.getSource(), new HashMap<Long, HashSet<Byte>>());
		}
		
		// Convert virtual blockIdx to <fileId, blocIdx>
		for(int i=0; i < blockCnt; i++){
			long fileId = blockVirIdx2FileMap.get(i).first;
			byte blockIdx = blockVirIdx2FileMap.get(i).second;
			long nodeId = index2IpMap.get(blockAssign[i]);
			
			if(!taskAssignment.get(nodeId).containsKey(fileId))
				taskAssignment.get(nodeId).put(fileId, new HashSet<Byte>());
			taskAssignment.get(nodeId).get(fileId).add(blockIdx);
		}
	}
	
	public HashMap<Long, HashMap<Long, HashSet<Byte>>> getAllocation(){
		return taskAssignment;
	}
	
	
	
	/*public void findOptimalAllocation(){
		
		 * HashMap<fileId, HashMap<blockIdx, PriorityQueue<MyPair<nodeId, # of frags>>>> 
		 
		HashMap<Long, HashMap<Byte, PriorityQueue<MyPair<Long, Byte>>>> file_block_sortedQ = 
				new HashMap<Long, HashMap<Byte, PriorityQueue<MyPair<Long, Byte>>>>();
		
		// Keep track of the number of tasks each node is assigned
		HashMap<Long, Integer> assignmentCount = new HashMap<Long, Integer>();
		
		// Initialization
		for(Long fileId : fileList){
			file_block_sortedQ.put(fileId, new HashMap<Byte, PriorityQueue<MyPair<Long, Byte>>>());
		}
		
		
		for(JobReply reply : jobReplySet){
			Map<Long, HashMap<Byte, HashSet<Byte>>> fileBlockMap = reply.getFileBlockFragMap();
			for(Long fileId : fileBlockMap.keySet()){
				HashMap<Byte, HashSet<Byte>> blockSet = fileBlockMap.get(fileId);
				for(Byte block : blockSet.keySet()){
					HashSet<Byte> fragSet = blockSet.get(block);
					/////////////////////////////////////////////
					if(!file_block_sortedQ.get(fileId).containsKey(block))
						file_block_sortedQ.get(fileId).put(block, new PriorityQueue<MyPair<Long, Byte>>(15,new PQsort()));
					
					file_block_sortedQ.get(fileId).get(block).add(MyPair.create(reply.getSource(), (byte)fragSet.size()));
					/////////////////////////////////////////////
					for(Byte frag : fragSet){
						StringBuilder str = new StringBuilder();
						str.append("Node " + reply.getSource() );
						str.append(" has File " + fileId );
						str.append(", Block " + block );
						str.append(", Frag " + frag );
						Logger.v(TAG, str.toString());
					}
				}
			}
		}
		
		
		 * taskAssignment: HashMap<fileId, HashMap<blockIdx, PriorityQueue<nodeIp, # of frags>>>
		 * MyPair<nodeIp, # of frags> keeps the number of fragments that the node has
		 * The head of this queue is the node with the most fragments.
		 
		for(JobReply reply : jobReplySet){
			taskAssignment.put(reply.getSource(), new HashMap<Long, HashSet<Byte>>());
			assignmentCount.put(reply.getSource(), 0);
		}
		
		for(Long fileId : file_block_sortedQ.keySet()){
			for(Byte block : file_block_sortedQ.get(fileId).keySet()){
				// For each block, choose the node that has the most fragments
				// MyPair<nodeIp, # of frags>
				MyPair<Long, Byte> pair = file_block_sortedQ.get(fileId).get(block).peek();
				if(!taskAssignment.get(pair.first).containsKey(fileId))
					taskAssignment.get(pair.first).put(fileId, new HashSet<Byte>());
				taskAssignment.get(pair.first).get(fileId).add(block);
				assignmentCount.put(pair.first, assignmentCount.get(pair.first)+1);
			}
		}
		
	}*/
	
	
	
	private class PQsort implements Comparator<MyPair<Long, Byte>> {
		 
		public int compare(MyPair<Long, Byte> one, MyPair<Long, Byte> two) {
			return -(one.second - two.second);
		}
	}
}
