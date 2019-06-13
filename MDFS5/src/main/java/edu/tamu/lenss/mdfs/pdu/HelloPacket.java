package edu.tamu.lenss.mdfs.pdu;

import java.io.Serializable;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.network.Constants;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class HelloPacket extends Packet implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public HelloPacket() {
		this.pduType = Constants.HELLO_PDU;
		this.setSourceIPLong(ServiceHelper.getInstance().getNodeManager()
				.getMyIP());
		this.setDestinationIPLong(NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
	}
	
	public void update(){
		this.setSourceIPLong(ServiceHelper.getInstance().getNodeManager()
				.getMyIP());
		this.setDestinationIPLong(NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
	}

	@Override
	public byte[] toBytes() {
		return toByteArray(this, HelloPacket.class);
	}

	@Override
	public String toString() {
		return "Hello Packet from" + this.getSourceIPString();
	}

	@Override
	public void parseBytes(byte[] rawPdu) {
		// HelloPacket hello = parseFromByteArray(rawPdu, HelloPacket.class);
	}
}
