package edu.tamu.lenss.mdfs.placement;


public class LPItem {
	/**
	 * 
	 * @author Jay
	 *
	 */
	public enum VarType{
		/**
		 * Indicate whether this node servers as a storage node
		 */
		node,
		/**
		 * Indicate whether node j is a potential storage node for node i
		 */
		storage
	}
	private VarType type;
	private int nodeIndex;		// i
	private int storageIndex;	// j
	
	public LPItem(VarType varType){
		this.type = varType;
		if(type==VarType.node){
			this.storageIndex = -1;
		}
	}
	
	public VarType getType() {
		return type;
	}

	public void setType(VarType type) {
		this.type = type;
	}

	public int getNodeIndex() {
		return nodeIndex;
	}

	public void setNodeIndex(int nodeIndex) {
		this.nodeIndex = nodeIndex;
	}

	public int getStorageIndex() {
		return storageIndex;
	}

	public void setStorageIndex(int storageIndex) {
		this.storageIndex = storageIndex;
	}
}
