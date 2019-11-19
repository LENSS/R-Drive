package edu.tamu.lenss.MDFS.Model;

import java.io.File;
import java.io.Serializable;

import edu.tamu.lenss.MDFS.Constants;

/**
 * This class is used to store the information of any created file
 * @author Jay
 */
public class MDFSFileInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	private String fileID;
	private String fileName;
	private long fileSize;   //entire file size
	private byte k2, n2;
	private byte numberOfBlocks;
	
	
	public byte getNumberOfBlocks() {
		return numberOfBlocks;
	}

	public void setNumberOfBlocks(byte numberOfBlocks) {
		this.numberOfBlocks = numberOfBlocks;
	}

	//filename fileid
	public MDFSFileInfo(String fileName, String fileID){
		this.fileName = fileName;
		this.fileID = fileID;
	}

	public String getFileID() {
		return fileID;
	}

	public String getFileName() {
		return fileName;
	}

	public byte getK2() {
		return k2;
	}

	public byte getN2() {
		return n2;
	}
	
	public void setFragmentsParms( byte n2, byte k2){
		this.n2 = n2;
		this.k2 = k2;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileLength) {
		this.fileSize = fileLength;
	}


	//returns: /MDFS/test1.jpg__0123/
	public static String getFileDirPath(String fileName, String fileID){
		return Constants.ANDROID_DIR_ROOT + File.separator
				+ getFileDirName(fileName, fileID);
	}
	
	/**
	 * Return the directory name of a file
	 * @param fileName
	 * @param fileID
	 * @return fileName_MMddyyy_HHmmss
	 */
	//returns: test1.jpg__0123/
	public static String getFileDirName(String fileName, String fileID){
		return fileName + "__" + fileID;
	}
	
	/**
	 * Return the directory name of a file block
	 * @param fileName
	 * @param blockIdx
	 * @return
	 */
	//returns: test1.jpg__0
	public static String getBlockDirName(String fileName, byte blockIdx){
		return fileName + "__" + blockIdx;
	}

	//returns: test1.jpg_-blk__0
	public static String getBlockName(String fileName, byte blockIdx){
		return fileName + "__blk__" + blockIdx;
	}
	
	/**
	 * Return the fragment name of a block fragment
	 * @param fileName
	 * @param blockIdx
	 * @param fragIdx
	 * @return
	 */
	//returns: test1.jpg__0__frag__0
	public static String getFragName(String fileName, byte blockIdx, byte fragIdx){
		return fileName + "__" + blockIdx + "__frag__" + fragIdx; 
	}
	
	/**
	 * Return the relative path to a block directory. Starting from the root directory MDFS
	 * @param fileName
	 * @param blockIdx
	 * @return
	 */
	//returns: /MDFS/test1.jpg__0123/test1.jpg__0/
	public static String getBlockDirPath(String fileName, String fileID, byte blockIdx){
		return Constants.ANDROID_DIR_ROOT + File.separator + MDFSFileInfo.getFileDirName(fileName, fileID) + File.separator + MDFSFileInfo.getBlockDirName(fileName, blockIdx);
	}
	
	/**
	 * Return the relative path to a block fragment. Starting from the root directory MDFS
	 * @param fileName
	 * @param blockIdx
	 * @param fragIdx
	 * @return
	 */
	//returns: /MDFS/test1.jpg__0123/test1.jpg__0/test1.jpg__0__frag__0
	public static String getFragmentPath(String fileName, String fileID, byte blockIdx, byte fragIdx){
		return MDFSFileInfo.getBlockDirPath(fileName, fileID, blockIdx)     + File.separator + MDFSFileInfo.getFragName(fileName, blockIdx, fragIdx);
	}
}
