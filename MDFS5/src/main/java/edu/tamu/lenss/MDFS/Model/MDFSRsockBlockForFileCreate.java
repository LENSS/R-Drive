package edu.tamu.lenss.MDFS.Model;

import java.io.Serializable;

//this object is sent through rsock api and parsed back when received
//this class object is only used during file creation
public class MDFSRsockBlockForFileCreate implements Serializable {

	public String fileName;			//name of the file
	public String filePathMDFS;		//file path in MDFS in which the file will be stored(full path containing filename at the end)
	public byte[] fileFrag;			//the byteArray that contains filefrag
	public String fileID;				//fileiD
	public long entireFileSize;		//size of the entire file
	public byte n2;					//n2
	public byte k2;					//k2
	public byte blockIdx;			//block number of this file
	public byte fragmentIdx;		//fragment number of this block
	public String fileCreatorGUID;  //the guid/node which created the file
	public String uniqueReqID;		//unique blockRetrieveReqUUID using which this file metadata is identified
	public boolean isGlobal;        //is this file globally accessible or nah


	public MDFSRsockBlockForFileCreate(String filename, String filePathMDFS, byte[] filefrag, String fileID, long entirefilesize, byte n2, byte k2, byte blockidx, byte fragmentidx, String filecreatorguid, String uniquereqid, boolean isglobal){
		this.fileName = filename;
		this.filePathMDFS = filePathMDFS;
		this.fileFrag = filefrag;
		this.fileID = fileID;
		this.entireFileSize = entirefilesize;
		this.n2 = n2;
		this.k2 = k2;
		this.blockIdx = blockidx;
		this.fragmentIdx = fragmentidx;
		this.fileCreatorGUID = filecreatorguid;
		this.uniqueReqID = uniquereqid;
		this.isGlobal = isglobal;

	}

}
