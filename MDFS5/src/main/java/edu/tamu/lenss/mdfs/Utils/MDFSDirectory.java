package edu.tamu.lenss.mdfs.Utils;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.Model.MDFSFileInfo;

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

	//constructor
	public MDFSDirectory() {
		fileInfoMap = new HashMap<Long, MDFSFileInfo>();
		nameToIDMap = new HashMap<String, Long>();
		fileBlockFragMap = new HashMap<Long, HashMap<Byte, HashSet<Byte>>>();
		encryptedFileSet = new HashSet<Long>();
		decryptedFileSet = new HashSet<Long>();
		recentUpdate = new LinkedList<MyPair<Long, Long>>();
		recentDelete = new LinkedList<MyPair<Long, Long>>();
	}


	//Remove this file from the fileInfoMap. This is just for directory purpose,
	//and the actual files in SDCard <Br>
	//and other fragment information are NOT removed <Br>
	//FileId is just the file created time.
	public synchronized void removeFile(long fileId) {
		if (fileInfoMap.containsKey(fileId)) {
			String name = fileInfoMap.get(fileId).getFileName();
			nameToIDMap.remove(name);
		}
		fileInfoMap.remove(fileId);
	}


	public synchronized void removeFileAndBlock(long fileId) {
		fileBlockFragMap.remove(fileId);
	}
	public synchronized void addEncryptedFile(String fileName) {
		Long fileId = nameToIDMap.get(fileName);
		if (fileId != null)
			encryptedFileSet.add(fileId);
	}
	public synchronized void removeEncryptedFile(long fileId) {
		encryptedFileSet.remove(fileId);
	}

	public synchronized void addDecryptedFile(String fileName) {
		Long fileId = nameToIDMap.get(fileName);
		if (fileId != null)
			decryptedFileSet.add(fileId);
	}
	public synchronized void removeDecryptedFile(long fileId) {
		decryptedFileSet.remove(fileId);
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
		
		File rootDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT);
		try {
			IOUtilities.deleteRecursively(rootDir);
		} catch (IOException e) {
			Logger.e(TAG, e.toString());
			e.printStackTrace();
		}
	}

	//delete everything of a file from local mdfs directory
	public void deleteFile(long fileId, String fName) {

		File rootDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT);
		if (!rootDir.exists())
			return;

		synchronized (recentDelete) {
			long curTime = System.currentTimeMillis();
			recentDelete.addFirst(MyPair.create(fileId, curTime));
			while (!recentDelete.isEmpty() && recentDelete.getLast().second < curTime - Constants.FILE_DEL_PERIOD)
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

	//sync local mdfs directory and load at the beginning when application boots up
	public void syncLocal() {
		//sync is the root directory exists
		File rootDir = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT);
		if (!rootDir.exists()){
			return;
		}


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

	//save mdfs directory object on local drive.
	// this function is called periodically and when app is closing.
	public boolean saveDirectory() {
		File tmp0 = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT);
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

	//read local mdfs directory and load at the beginning when application boots up.
	public static MDFSDirectory readDirectory() {
		File tmp = AndroidIOUtils.getExternalFile(Constants.ANDROID_DIR_ROOT + "/"
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

	//get fragment index from name
	public int getFragmentIndexFromName(String name) {
		String[] tokens = name.split("__frag__");

		tokens = delEmptyStr(tokens);

		return Integer.parseInt(tokens[tokens.length-1]);
	}


	//eliminates empty string tokens
	public static String[] delEmptyStr(String[] tokens){
		List<String> newTokens = new ArrayList<>();
		for(String token: tokens){
			if(!token.equals("")){
				newTokens.add(token);
			}
		}

		return newTokens.toArray(new String[0]);
	}
}
