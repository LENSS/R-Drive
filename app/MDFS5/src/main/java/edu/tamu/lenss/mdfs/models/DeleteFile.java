package edu.tamu.lenss.mdfs.models;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.lenss.mdfs.handler.NodeManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

/**
 * Delete one or more files in the Distributed File System. Need to provide both <br>
 * file names and fileId of every file needed to be deleted
 * In situation that a device need to clean up some files without the knowledge of MDFSFileInfo, <br> 
 * both fileNames and fileIds are required to locate the directories.
 * @author Jay
 *
 */
public class DeleteFile extends MessageContainer {
	private static final long serialVersionUID = 1L;
	private List<String> fileNames;
	private List<Long> fileIds;
	// A special request to delte all files
	private boolean deleteAll = false; 	
	
	/**
	 * This will be broadcasted to the entire network
	 */
	public DeleteFile(){
		super(MDFSPacketType.DELETE_FILE, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), NodeManager
				.getClasCBroadcastIpLong(ServiceHelper.getInstance().getNodeManager().getMyIpString()));
		this.setBroadcast(true);
	}
	
	/**
	 * If this is sent to a specific node
	 * @param destination
	 * @param broadcast
	 */
	public DeleteFile(long destination){
		super(MDFSPacketType.DELETE_FILE, ServiceHelper.getInstance()
				.getNodeManager().getMyIP(), destination);
		this.setBroadcast(false);
	}
	
	public List<String> getFileNames() {
		return fileNames;
	}

	public List<Long> getFileIds() {
		return fileIds;
	}

	/**
	 * fileNames and fileIds need to have the same size
	 * @param fileNames
	 * @param fileIds
	 * @return
	 */
	public boolean setFiles(List<String> fileNames, List<Long> fileIds) {
		if(fileNames.size() != fileIds.size())
			return false;
		this.fileNames = fileNames;
		this.fileIds = fileIds;
		return true;
	}
	
	public void setFiles(List<MDFSFileInfo> fileList) {
		for(MDFSFileInfo fInfo : fileList){
			setFile(fInfo.getFileName(), fInfo.getCreatedTime());
		}
	}
	
	
	public void setFile(String name, long id){
		if(fileNames == null)
			fileNames = new ArrayList<String>();
		if(fileIds == null)
			fileIds = new ArrayList<Long>();
		
		fileNames.add(name);
		fileIds.add(id);
	}
	
	public boolean isDeleteAll() {
		return deleteAll;
	}

	public void setDeleteAll(boolean deleteAll) {
		this.deleteAll = deleteAll;
	}

	@Override
	public byte[] toByteArray() {
		return toByteArray(this);
	}

}
