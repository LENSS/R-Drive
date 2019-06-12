package edu.tamu.lenss.mdfs.models;

public final class MDFSPacketType {
	public static final byte TOPOLOGY_DISCOVERY = 1;
	public static final byte NODE_INFO = 2;
	public static final byte KEY_FRAG_PACKET = 3;
	public static final byte BLOCK_REQ = 4;
	public static final byte BLOCK_REPLY = 5;
	public static final byte NEW_FILE_UPDATE = 6;
	public static final byte DELETE_FILE = 7;
	public static final byte JOB_REQUEST = 8;
	public static final byte JOB_REPLY = 9;
	public static final byte ASSIGN_TASK_REQUEST = 10;
	
	public static final String TypeName[] = {
		"",
		"TOPOLOGY_DISCOVERY",
		"NODE_INFO", 
		"KEY_FRAG_PACKET",
		"BLOCK_REQ",
		"BLOCK_REPLY",
		"NEW_FILE_UPDATE",
		"DELETE_FILE",
		"JOB_REQUEST",
		"JOB_REPLY",
		"ASSIGN_TASK_REQUEST",
		};

	
	public static final byte JOB_SCHEDULE = 108;
	public static final byte JOB_COMPLETE = 109;
	public static final byte JOB_RESULT = 100;
	
	
}
