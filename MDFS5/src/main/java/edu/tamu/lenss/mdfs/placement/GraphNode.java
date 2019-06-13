package edu.tamu.lenss.mdfs.placement;

import java.util.HashMap;
import java.util.HashSet;

public class GraphNode {
	// Node ID is always positive integer
	private int nodeId;
	private int parentId;
	private boolean visited=false;
	private boolean finished=false;
	
	private HashMap<Integer, Integer> distVector = new HashMap<Integer, Integer>();
	private HashSet<Integer> adjNodes = new HashSet<Integer>();
	
	public GraphNode(){
	}
	public GraphNode(int id){
		this.nodeId = id;
	}
	public int getNodeId() {
		return nodeId;
	}
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	public int getParentId() {
		return parentId;
	}
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}
	public void setDistance(int toNodeId, int distance){
		distVector.put(toNodeId, distance);
	}
	
	/**
	 * @param toNodeId
	 * @return -1 if the distance to the node is unknown
	 */
	public int getDistance(int toNodeId){
		Integer i = distVector.get(toNodeId);
		if(i==null)
			return -1;
		return i;
	}
	
	public void resetDistance(){
		distVector.clear();
	}
	
	public boolean isVisited() {
		return visited;
	}
	public void setVisited(boolean visited) {
		this.visited = visited;
	}
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	public HashSet<Integer> getAdjNodes() {
		return adjNodes;
	}
	public void setAdjNodes(HashSet<Integer> adjNodes) {
		this.adjNodes = adjNodes;
	}
	public void addNeighbor(int neighbor){
		this.adjNodes.add(neighbor);
	}
	public void removeNeighbor(int neighbor){
		this.adjNodes.remove(neighbor);
	}
	public void resetNeirhbors(){
		this.adjNodes.clear();
	}
}
