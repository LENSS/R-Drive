package edu.tamu.lenss.mdfs.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.tamu.lenss.mdfs.models.BlockReply;
import edu.tamu.lenss.mdfs.models.BlockReq;


//this class is called when a block request has been received
public class BlockRequestHandler {
	private ServiceHelper serviceHelper;
	
	public BlockRequestHandler(){}
	

	public void processRequest(BlockReq fileReq){
		serviceHelper = ServiceHelper.getInstance();
		
		BlockReply reply = new BlockReply(fileReq.getFileName(), fileReq.getFileCreatedTime(), fileReq.getBlockIdx(), serviceHelper.getNodeManager().getMyIP(), fileReq.getSource());
		
		Set<Byte> fileSet = serviceHelper.getDirectory().getStoredFragIndex(fileReq.getFileCreatedTime(), fileReq.getBlockIdx());
		
		// Reply with whatever fragments I have
		if(fileReq.isAnyAvailable()){
			if(fileSet != null)
				reply.setBlockFragIndex(new ArrayList<Byte>(fileSet));
		}
		// Reply with only the fragments specified in the request
		else{
			// Process FileFrag
			List<Byte> fileFrags = fileReq.getBlockFragIndex();
			if(fileFrags != null && fileFrags.size() > 0){
				if(fileSet != null){
					fileFrags.retainAll(fileSet);
					if(!fileFrags.isEmpty())
						reply.setBlockFragIndex(fileFrags);
				}
			}
			
		}
		
		// Send back the reply
		if(reply.getBlockFragIndex() != null ) {
			PacketExchanger.getInstance().sendMsgContainer(reply);
		}else{
		}
	}
}
