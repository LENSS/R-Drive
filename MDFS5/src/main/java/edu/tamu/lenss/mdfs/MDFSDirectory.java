package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Pair;
import edu.tamu.lenss.mdfs.handler.PacketExchanger;
import edu.tamu.lenss.mdfs.models.JobReq;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.NewFileUpdate;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MyPair;

/**
 * This class track the current status of MDFS File System. <br>
 * Available files in the network, local available files, or local available
 * fragments...
 * 
 * @author Jay
 *
 */

//WARNING: this class object is written on the SD card of the phone periodically,
//at the directory  SDCARD/MDFS/mdfs_directory, and fetched everytime the application
//is restarted. IF you add any new feature/code to this file and recompile the application,
// the application may conflict with startup with the old format of mdfs directory.
//So, you must delete the old mdfs directory from SDCARD/MDFS/mdfs_directory and then
//recompile the application with new mdfs_directory, everytime you bring change in this file.
public class MDFSDirectory implements Serializable {

	private static final String TAG = MDFSDirectory.class.getSimpleName();
	private static final long serialVersionUID = 1L;

	//all file fileInfo
	private Map<Long, MDFSFileInfo> fileInfoMap;

	//map from file name to file id
	private Map<String, Long> nameToIDMap;

	//fileID to BlockNum to FragmentNum
	private Map<Long, HashMap<Byte, HashSet<Byte>>> fileBlockFragMap;
																		

	private Set<Long> encryptedFileSet;
	private Set<Long> decryptedFileSet;

	private LinkedList<MyPair<Long, Long>> recentUpdate;
	private LinkedList<MyPair<Long, Long>> recentDelete;
	

	private Map<Long, JobReq> jobMap;
	
	//fileID to BlockID to StartTime
	private Map<Long, HashMap<Byte, Long>> downloadingBlocks;


	//constructor
	public MDFSDirectory() {
		fileInfoMap = new HashMap<Long, MDFSFileInfo>();
		nameToIDMap = new HashMap<String, Long>();
		fileBlockFragMap = new HashMap<Long, HashMap<Byte, HashSet<Byte>>>();
		encryptedFileSet = new HashSet<Long>();
		decryptedFileSet = new HashSet<Long>();
		recentUpdate = new LinkedList<MyPair<Long, Long>>();
		recentDelete = new LinkedList<MyPair<Long, Long>>();
		jobMap = new HashMap<Long, JobReq>();
		downloadingBlocks = new HashMap<Long, HashMap<Byte, Long>>();
	}


	//get fileInfo
	public MDFSFileInfo getFileInfo(long fileId) {
		return fileInfoMap.get(fileId);
	}


	//Check if this file (fileId) was updated within the past FILE_SYNC_PERIOD
	public synchronized boolean isRecentUpdated(long fileId) {
		for (MyPair<Long, Long> pair : recentUpdate) {
			if (pair.first.equals(fileId))
				return true;
		}
		return false;
	}


	//return A List of all available files. The List may be empty
	public synchronized List<MDFSFileInfo> getFileList() {
		List<MDFSFileInfo> list;
		if (!fileInfoMap.isEmpty())
			list = new ArrayList<MDFSFileInfo>(fileInfoMap.values());
		else
			list = new ArrayList<MDFSFileInfo>();
		return list;
	}

	/**
	 * @param name
	 * @return -1 if the the id is not available
	 */
	public synchronized long getFileIdByName(String name) {
		Long id = nameToIDMap.get(name);
		if (id != null)
			return id;
		else
			return -1;
	}

	/**
	 * Return the blocks that I have cached fragments
	 * 
	 * @param fileId
	 * @return null if there is no cache
	 */
	public synchronized Set<Byte> getStoredBlockIndex(long fileId) {
		try {
			return fileBlockFragMap.get(fileId).keySet();
		} catch (NullPointerException o) {
			return null;
		}
	}
	
	/**
	 * Return the fragments of each block that this node carries
	 * @param fileId
	 * @param reCheck  set to true if forcing to inspect the files in external storage <Br> 
	 * and sync with MDFSDirectory. This can take some time.
	 * @return null if there is no cached fragments of this file
	 */
	public synchronized HashMap<Byte, HashSet<Byte>> getStoredFragments(long fileId, boolean reCheck){
		MDFSFileInfo fInfo = getFileInfo(fileId);
		
		if(!reCheck || fInfo == null){
			// no way to verify
			return fileBlockFragMap.get(fileId);
		}
		
		// Verify that there are indeed cached fragments
		try {
			File tmpF = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(fInfo.getFileName(), fileId));
			if(tmpF.exists() && tmpF.isDirectory()){
				HashMap<Byte, HashSet<Byte>> blockAndFrags = fileBlockFragMap.get(fileId);
				for(Iterator<Byte> blkIter = blockAndFrags.keySet().iterator(); blkIter.hasNext();){
					byte block = blkIter.next();					
					tmpF = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fInfo.getFileName(), fileId, block));
					if(tmpF.exists() && tmpF.isDirectory()){
						HashSet<Byte> frags = blockAndFrags.get(block);
						for(Iterator<Byte> fragIter = frags.iterator(); fragIter.hasNext();){
							byte frag = fragIter.next();
							tmpF = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fInfo.getFileName(), fileId, block, frag));
							if(!tmpF.exists() || !tmpF.isFile()){
								//fileBlockFragMap.get(fileId).get(block).remove(frag);
								// We are removing elements while iterating a Collection. Need to use iterator.remove();
								fragIter.remove();
								Logger.w(TAG, "Remove out of sync frag from cache: " 
										+ fInfo.getFileName() + ", block " + block + ", frag " + frag);
							}
						}
					}
					else{
						//fileBlockFragMap.get(fileId).remove(block);
						// We are removing elements while iterating a Collection. Need to use iterator.remove();
						blkIter.remove();
						Logger.w(TAG, "Remove out of sync block from cache: " + fInfo.getFileName() + ", block " + block);
					}
				}
			}
			else{
				fileBlockFragMap.remove(fileId);
				Logger.w(TAG, "Remove out of sync file from cache: " + fInfo.getFileName() );
			}
			
			return fileBlockFragMap.get(fileId);
		} catch (NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return the cahced fragments of a given fileId(file creation time) and block index
	 * 
	 * @param fileId
	 * @param blockIdx
	 * @return null if the file or the block does not exist
	 */
	public synchronized Set<Byte> getStoredFragIndex(long fileId, byte blockIdx) {
		try {
			return fileBlockFragMap.get(fileId).get(blockIdx);
		} catch (NullPointerException o) {
			return null;
		}
	}



	public synchronized void addFile(MDFSFileInfo file) {

		//for any incoming NEW_FILE_UPDATE msg, this block is executed and checks some time-mechanism
		//if check fails, it triggers a DELETE_FILE msg which deletes the file from entire network
		//commenting this block prevents that delete from taking place - Mohammad Sagor
		/*for (MyPair<Long, Long> pair : recentDelete) {
			if (pair.first.equals(file.getCreatedTime())) {
				// This file has been deleted
				DeleteFile deleteFile = new DeleteFile();
				deleteFile.setFile(file.getFileName(), file.getCreatedTime());
				ServiceHelper.getInstance().deleteFiles(deleteFile);
				return;
			}
		}*/

		fileInfoMap.put(file.getCreatedTime(), file);
		nameToIDMap.put(file.getFileName(), file.getCreatedTime());

		long curTime = System.currentTimeMillis();
		recentUpdate.addFirst(MyPair.create(file.getCreatedTime(), curTime));
		// Remove expired items
		while (!recentUpdate.isEmpty() && recentUpdate.getLast().second < (curTime - edu.tamu.lenss.mdfs.network.Constants.FILE_SYNC_PERIOD)) {
			recentUpdate.removeLast();
		}
	}

	/**
	 * Remove this file from the fileInfoMap. This is just for directory purpose,
	 * and the actual files in SDCard <Br>
	 * and other fragment information are NOT removed <Br>
	 * FileId is just the file created time.
	 * 
	 * @param fileId
	 */
	public synchronized void removeFile(long fileId) {
		if (fileInfoMap.containsKey(fileId)) {
			String name = fileInfoMap.get(fileId).getFileName();
			nameToIDMap.remove(name);
		}
		fileInfoMap.remove(fileId);
	}



	/**
	 * Add one fragment of a block to our local cache
	 * 
	 * @param fileId
	 * @param blockIdx
	 * @param fragIdx
	 */
	public synchronized void addBlockFragment(long fileId, byte blockIdx, byte fragIdx) {
		addBlockFragments(fileId, blockIdx, new HashSet<Byte>(Arrays.asList(fragIdx)));
	}


	/**
	 * Add a set of fragments index to our local cache
	 * 
	 * @param fileId
	 * @param blockIdx
	 * @param fragIdxSet
	 */
	public synchronized void addBlockFragments(long fileId, byte blockIdx, HashSet<Byte> fragIdxSet) {
		if (fileBlockFragMap.containsKey(fileId)) {
			if (fileBlockFragMap.get(fileId).containsKey(blockIdx)) {
				fileBlockFragMap.get(fileId).get(blockIdx).addAll(fragIdxSet);
			} else {
				fileBlockFragMap.get(fileId).put(blockIdx, fragIdxSet);
			}
		} else {
			HashMap<Byte, HashSet<Byte>> blockSet = new HashMap<Byte, HashSet<Byte>>();
			blockSet.put(blockIdx, fragIdxSet);
			fileBlockFragMap.put(fileId, blockSet);
		}

	}


	/**
	 * Remove a single cached fragment
	 * 
	 * @param fileId
	 * @param blockIdx
	 */
	public synchronized void removeBlockFragment(long fileId, byte blockIdx,
			byte fragIdx) {
		if (fileBlockFragMap.containsKey(fileId)
				&& fileBlockFragMap.get(fileId).containsKey(blockIdx)) {
			fileBlockFragMap.get(fileId).get(blockIdx).remove(fragIdx);
		}
	}

	/**
	 * Remove a set of cached fragments
	 * 
	 * @param fileId
	 * @param blockIdx
	 * @param fragIdxSet
	 */
	public synchronized void removeBlockFragments(long fileId, byte blockIdx,
			HashSet<Byte> fragIdxSet) {
		if (fileBlockFragMap.containsKey(fileId)
				&& fileBlockFragMap.get(fileId).containsKey(blockIdx)) {
			fileBlockFragMap.get(fileId).get(blockIdx).removeAll(fragIdxSet);
		}
	}

	/**
	 * Remove a block and all its fragments from the cache
	 * 
	 * @param fileId
	 * @param blockIdx
	 */
	public synchronized void removeBlock(long fileId, byte blockIdx) {
		if (fileBlockFragMap.containsKey(fileId)) {
			fileBlockFragMap.get(fileId).remove(blockIdx);
		}
	}

	/**
	 * Completely remove a file and all its blocks from the local cache
	 * 
	 * @param fileId
	 */
	public synchronized void removeFileAndBlock(long fileId) {
		fileBlockFragMap.remove(fileId);
	}


	public synchronized void addEncryptedFile(long fileId) {
		encryptedFileSet.add(fileId);
	}

	public synchronized void addEncryptedFile(String fileName) {
		Long fileId = nameToIDMap.get(fileName);
		if (fileId != null)
			encryptedFileSet.add(fileId);
	}

	public synchronized void removeEncryptedFile(long fileId) {
		encryptedFileSet.remove(fileId);
	}

	public synchronized void removeEncryptedFile(String fileName) {
		Long fileId = nameToIDMap.get(fileName);
		if (fileId != null)
			encryptedFileSet.remove(fileId);
	}

	public synchronized void addDecryptedFile(long fileId) {
		decryptedFileSet.add(fileId);
	}

	public synchronized void addDecryptedFile(String fileName) {
		Long fileId = nameToIDMap.get(fileName);
		if (fileId != null)
			decryptedFileSet.add(fileId);
	}

	public synchronized void removeDecryptedFile(long fileId) {
		decryptedFileSet.remove(fileId);
	}

	public synchronized void removeDecryptedFile(String fileName) {
		Long fileId = nameToIDMap.get(fileName);
		if (fileId != null)
			decryptedFileSet.remove(fileId);
	}

	public boolean isEncryptedFileCached(long fileId) {
		return encryptedFileSet.contains(fileId);
	}

	public boolean isDecryptedFileCached(long fileId) {
		return decryptedFileSet.contains(fileId);
	}
	
	public void addJob(Long jobId, JobReq jobReq){
		jobMap.put(jobId, jobReq);
	}

	public Set<Long> getJobList(){ return jobMap.keySet(); }
	
	public List<JobReq> getJobReqList(){
		List<JobReq> list;
		if (!jobMap.isEmpty())
			list = new ArrayList<JobReq>(jobMap.values());
		else
			list = new ArrayList<JobReq>();
		return list;
	}

	public void clearAll() {
		// Reset the maps
		fileInfoMap.clear();
		nameToIDMap.clear();
		fileBlockFragMap.clear();
		encryptedFileSet.clear();
		decryptedFileSet.clear();
		encryptedFileSet.clear();
		decryptedFileSet.clear();
		recentUpdate.clear();
		recentDelete.clear();
		jobMap.clear();
		
		File rootDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT); 
		try {
			IOUtilities.deleteRecursively(rootDir);
		} catch (IOException e) {
			Logger.e(TAG, e.toString());
			e.printStackTrace();
		}
	}

	//periodically called by scheduledTask.java to send out NEW_FILE_UPDATES for each files in the directory
	//currently turned off from ScheduledTak.java class
	public void broadcastMyDirectory(){
		// Remove expired items
		long curTime = System.currentTimeMillis();
		while (!recentUpdate.isEmpty() && recentUpdate.getLast().second < (curTime - edu.tamu.lenss.mdfs.network.Constants.FILE_SYNC_PERIOD)) {
			recentUpdate.removeLast();
		}

		for(MDFSFileInfo fInfo : getFileList()){
			NewFileUpdate fUpdate = new NewFileUpdate(fInfo);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// The file may be deleted within this delay
			if(getFileInfo(fInfo.getCreatedTime()) != null &&
					!isRecentUpdated(fInfo.getCreatedTime())){
				PacketExchanger.getInstance().sendMsgContainer(fUpdate);
			}			
		}
	}


	//delete everything of a file from local mdfs directory
	public void deleteFile(long fileId, String fName) {
		File rootDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT);
		if (!rootDir.exists())
			return;

		synchronized (recentDelete) {
			long curTime = System.currentTimeMillis();
			recentDelete.addFirst(MyPair.create(fileId, curTime));
			while (!recentDelete.isEmpty() && recentDelete.getLast().second < curTime - edu.tamu.lenss.mdfs.network.Constants.FILE_DEL_PERIOD)
				recentDelete.removeLast();
		}

		// Clean up the MDFSDirectory
		removeDecryptedFile(fileId);
		removeEncryptedFile(fileId);
		removeFile(fileId);
		removeFileAndBlock(fileId);

		// Remove Encrypted File
		File file = new File(rootDir, "encrypted/" + fName);
		if (file.exists()){ file.delete(); }

		// Remove Decrypted File
		file = new File(rootDir, "decrypted/" + fName);
		if (file.exists()){ file.delete();}
		
		//Remove Cahced File
		file = new File(rootDir, "cache/" + "thumb_" + fileId + ".jpg");
		if (file.exists()){ file.delete();}

		// Delete File Folder
		File fileDir = new File(rootDir, MDFSFileInfo.getFileDirName(fName,	fileId));
		if (fileDir.exists() && fileDir.isDirectory()) {
			try {
				IOUtilities.deleteRecursively(fileDir);
				fileDir.delete();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//sync local mdfs directory and load at the beginning when application booots up
	public void syncLocal() {
		File rootDir = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT);
		if (!rootDir.exists())
			return; // Don't need to sync at all
		File[] directories = rootDir.listFiles(); // list file directories

		// Reset the maps
		fileBlockFragMap.clear();
		encryptedFileSet.clear();
		decryptedFileSet.clear();

		String dirName;
		for (File fileDir : directories) {
			if (!fileDir.isDirectory())
				continue; // We only check for directories
			dirName = fileDir.getName();
			if (dirName.equalsIgnoreCase("decrypted")) {
				File[] decFiles = fileDir.listFiles();
				for (File f : decFiles) {
					addDecryptedFile(f.getName().trim());
				}
			} else if (dirName.equalsIgnoreCase("encrypted")) {
				File[] encFiles = fileDir.listFiles();
				for (File f : encFiles) {
					addEncryptedFile(f.getName().trim());
				}
			} else if (dirName.contains("__")) {
				try {
					// String fileDirName;
					// fileDirName = ;
					Long fileId = Long.parseLong(dirName.substring(
							dirName.lastIndexOf("_") + 1).trim());
					if (fileId == null) {
						// This is not a recognizable directory. Delete it
						IOUtilities.deleteRecursively(fileDir);
						fileDir.delete();
						continue;
					}
					// Create a file entry
					fileBlockFragMap.put(fileId,
							new HashMap<Byte, HashSet<Byte>>());
					File[] blockDirs = fileDir.listFiles(); // list block
															// directories
					String blockDirName;
					for (File blockDir : blockDirs) {
						if (!blockDir.isDirectory())
							continue;
						blockDirName = blockDir.getName();
						byte blockIdx = Byte.parseByte(blockDirName.substring(
								blockDirName.lastIndexOf("_") + 1).trim());

						File[] fragDirs = blockDir.listFiles(); // list
																// fragments of
																// a block
						String fragName;
						HashSet<Byte> frags = new HashSet<Byte>(); // A set of
																	// fragments
						for (File frag : fragDirs) {
							if (!frag.isFile())
								continue;

							fragName = frag.getName();
							byte fragIdx = Byte.parseByte(fragName
									.substring(fragName.lastIndexOf("_") + 1));
							if (fragName.contains("__frag__")) {
								frags.add(fragIdx);
							}
						}
						if (!frags.isEmpty())
							fileBlockFragMap.get(fileId).put(blockIdx, frags);
					}
					// If no fragment exists at all
					if (fileBlockFragMap.get(fileId).isEmpty())
						fileBlockFragMap.remove(fileId);
				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (IndexOutOfBoundsException e) {
					e.printStackTrace();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//save mdfs directory object on local drive periodically
	public boolean saveDirectory() {
		File tmp0 = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT);
		File tmp = IOUtilities.createNewFile(tmp0,
				Constants.NAME_MDFS_DIRECTORY);
		if (tmp == null) {
			return false;
		}
		try {
			FileOutputStream fos = new FileOutputStream(tmp);
			ObjectOutputStream output = new ObjectOutputStream(fos);
			output.writeObject(this);
			output.close();
			fos.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	//read local mdfs directory and load at the beginning when application booots up
	public static MDFSDirectory readDirectory() {
		File tmp = AndroidIOUtils.getExternalFile(Constants.DIR_ROOT + "/"
				+ Constants.NAME_MDFS_DIRECTORY);
		// File tmp = IOUtilities.createNewFile(tmp0,
		// Constants.NAME_MDFS_DIRECTORY);

		if (!tmp.exists()) // In case that the file does not exist
			return new MDFSDirectory();
		MDFSDirectory obj;
		try {
			FileInputStream fis = new FileInputStream(tmp);
			ObjectInputStream input = new ObjectInputStream(fis);
			obj = (MDFSDirectory) input.readObject();
			input.close();
			fis.close();
			return obj;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return new MDFSDirectory();
	}

}
