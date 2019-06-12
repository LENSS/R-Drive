package edu.tamu.lenss.mdfs.handler;

import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;

public class DeleteFileHandler {
	
	public DeleteFileHandler(){
	}


	//called by servicehelper.deletefile() , and netobserver.DELETE_FILE msg
	public void processPacket(final DeleteFile delete){
		if(delete.isDeleteAll()){
			deleteAll();
			return;
		}
		if(delete.getFileIds() == null || delete.getFileNames() == null){
			return;
		}
		
		if(delete.getFileIds().size() != delete.getFileNames().size()){
			return;
		}
		ServiceHelper.getInstance().executeRunnableTask(new Runnable(){
			@Override
			public void run() {
				String fName;
				long fileId; 
				for(int i=0; i<delete.getFileIds().size(); i++){
					fileId =  delete.getFileIds().get(i);
					fName = delete.getFileNames().get(i);
					ServiceHelper.getInstance().getDirectory().deleteFile(fileId, fName);
				}
			}
		});
	}
	
	private void deleteAll(){
		MDFSDirectory dir = ServiceHelper.getInstance().getDirectory();
		dir.clearAll();
	}
	
	public void sendFileDeletionPacket(DeleteFile delete){
		PacketExchanger.getInstance().sendMsgContainer(delete);
	}
	
}
