package edu.tamu.lenss.mdfs.models;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class JobComplete extends MessageContainer {
	private static final long serialVersionUID = 1L;
	
	public JobComplete(){
		super(MDFSPacketType.JOB_COMPLETE, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
		this.setBroadcast(true);
	}
	
	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}
}
