package edu.tamu.lenss.mdfs.models;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

public class NewFileUpdate extends MessageContainer {

	private static final long serialVersionUID = 1L;
	private MDFSFileInfo fileInfo;

	public NewFileUpdate(MDFSFileInfo file){
		super(MDFSPacketType.NEW_FILE_UPDATE, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
		this.setBroadcast(true);
		this.fileInfo = file;
	}
	
	public MDFSFileInfo getFileInfo() {
		return fileInfo;
	}

	@Override
	public byte[] toByteArray() {
		return super.toByteArray(this);
	}
}
