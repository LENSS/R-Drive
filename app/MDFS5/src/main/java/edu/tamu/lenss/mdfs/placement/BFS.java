package edu.tamu.lenss.mdfs.placement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

public class BFS {
	private HashMap<Integer, GraphNode> nodes = new HashMap<Integer, GraphNode>();
	private boolean[][] adjMatrix;
	
	public BFS(HashMap<Integer, GraphNode> nodes){
		this.nodes = nodes;
	}
	
	public BFS(boolean[][] adjMatrix){
		this.adjMatrix = adjMatrix;
		buildGraphNodes();
	}
	
	public HashMap<Integer, GraphNode> bfs(int rootId){
		GraphNode sourceNode = nodes.get(rootId);
		if(sourceNode == null)
			return null;
		resetNodes();
		// Initialize Source node
		sourceNode.setVisited(true);
		sourceNode.setDistance(sourceNode.getNodeId(), 0);
		sourceNode.setParentId(-1);
		
		Queue<GraphNode> q = new LinkedList<GraphNode>();
		q.add(sourceNode);
		while(!q.isEmpty()){
			GraphNode curNode = q.remove();
			Iterator<Integer> adjNodesIter = curNode.getAdjNodes().iterator();
			while(adjNodesIter.hasNext()){
				int adjId = adjNodesIter.next();
				GraphNode adjNode = nodes.get(adjId);
				if(adjNode == null || adjNode.isVisited() || adjNode.isFinished())
					continue;
				adjNode.setVisited(true);
				adjNode.setDistance(sourceNode.getNodeId(),curNode.getDistance(sourceNode.getNodeId())+1);
				adjNode.setParentId(curNode.getNodeId());
				q.add(adjNode);
			}
			curNode.setFinished(true);
		}
		return nodes;
	}
	
	public int[][] allPairBFS(){
		Iterator<Entry<Integer, GraphNode>> iter = nodes.entrySet().iterator();
		while(iter.hasNext()){
			GraphNode node = iter.next().getValue();
			bfs(node.getNodeId());
		}
		return getDistanceMatrix();
	}
	
	public void resetNodes(){
		Iterator<Entry<Integer, GraphNode>> iter = nodes.entrySet().iterator();
		while(iter.hasNext()){
			GraphNode node = iter.next().getValue();
			node.setVisited(false);
			node.setFinished(false);
			node.setParentId(0);
		}
	}
	
	/**
	 * Need to call allPairBFS() first. Otherwise, the returned values is invalid
	 * @return Return the all pair distance matrix
	 */
	public int[][] getDistanceMatrix(){
		int size = nodes.size();
		int[][] distMatrix = new int[size][size];

		// initialize the distMatrix. Unreachable node has distance size+1
		for(int i=0; i<size; i++){
			for(int j=0; j<size; j++){
				distMatrix[i][j]=size+1;
			}
		}
		
		Iterator<Entry<Integer, GraphNode>> iter = nodes.entrySet().iterator();
		int dist;
		while(iter.hasNext()){
			GraphNode node = iter.next().getValue();
			for(int i=0; i<size; i++){
				if((dist=node.getDistance(i))<0)
					continue;
				distMatrix[node.getNodeId()][i]=dist;
				distMatrix[i][node.getNodeId()]=dist;
			}
		}
		return distMatrix;
	}
	
	private void buildGraphNodes(){
		int size = adjMatrix.length;
		nodes.clear();
		for(int i=0; i<size; i++){
			GraphNode newNode = new GraphNode(i);
			for(int j=0; j<size;j++){
				if(adjMatrix[i][j])
					newNode.addNeighbor(j);
			}
			nodes.put(i, newNode);
		}
	}
	
	public HashMap<Integer, GraphNode> getNodes() {
		return nodes;
	}

	public boolean[][] getAdjMatrix() {
		return adjMatrix;
	}

	public void setAdjMatrix(boolean[][] adjMatrix) {
		this.adjMatrix = adjMatrix;
		buildGraphNodes();
	}

	/**
	 * 
	 * @param trueId	The id used in the application layer
	 * @return
	 */
	public int trueIdToVirtualId(int trueId){
		
		return 0;
	}
	/**
	 * 
	 * @param virtualId	The index(id) used in Graph algorithm
	 * @return
	 */
	public int virtualIdToTrueId(int virtualId){
		
		return 0;
	}
}
