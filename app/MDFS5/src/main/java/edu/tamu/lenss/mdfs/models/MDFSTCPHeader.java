/**
 * 
 */
package edu.tamu.lenss.mdfs.models;

import java.io.Serializable;

/**
 * This is the superclass for all TCP class headers. It essentially identifies the type of TCP header and its source.
 * PacketExchanger uses the parameters in this class to identify the type of TCP connection. 
 * E.g., send fragment, send fragment, send thumbnail...
 * @author Jay
 */
public abstract class MDFSTCPHeader implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final byte TYPE_FRAGMENT = 0;
	public static final byte TYPE_TASK_RESULT = 1;
	
	protected byte tcpHeaderType;
	
	public MDFSTCPHeader() {
	}
	
	public MDFSTCPHeader(byte tcpHeadType) {
		tcpHeaderType = tcpHeadType;
	}
	
	public byte getTcpHeaderType() {
		return tcpHeaderType;
	}
	public void setTcpHeaderType(byte tcpHeaderType) {
		this.tcpHeaderType = tcpHeaderType;
	}

}
