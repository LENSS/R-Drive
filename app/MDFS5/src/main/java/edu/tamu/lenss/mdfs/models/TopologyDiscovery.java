package edu.tamu.lenss.mdfs.models;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;

/**
 * A simplest implementation of MessageContainer. Used solely for topology discovery.  
 * @author Jay
 */
public class TopologyDiscovery extends MessageContainer {
	private static final long serialVersionUID = 1L;
	
	public TopologyDiscovery() {
		super(MDFSPacketType.TOPOLOGY_DISCOVERY, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
		this.setBroadcast(true);
	}
	
	@Override
	public byte[] toByteArray() {
		return toByteArray(this);
	}

}
