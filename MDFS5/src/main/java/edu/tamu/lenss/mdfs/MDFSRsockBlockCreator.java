package edu.tamu.lenss.mdfs;

import java.io.Serializable;

import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;

//this object is sent through rsock api and parsed back when received
//this class object is only used during file creation
public class MDFSRsockBlockCreator implements Serializable {  				//RSOCK
	public FragmentTransferInfo fragTransInfoHeader;
	public byte[] fileFrag;			//the byteArray that contains filefrag
	public long fileFragLength;		//num of bytes in file frag array
	public String fileName;			//filename
	public long filesize;			//size of the entire file
	public long creatorMAC;			//MAC address of the node who created the file in MDFS
	public String filePathMDFS;		//file path in MDFS in which the file will be stored
	public long fileCreatedTime;	//time the file has been created, currently known as FileID
	public String fileCreatorGUID;  //the guid/node which created the file
	public String destGUID;			//the guid /node which will receive the file fragment
	public byte blockCount;			//number of blocks for this file
	public byte n2;					//n2
	public byte k2;					//k2
	public String[] permList;		//permission list for this file
	public String uniqueReqID;


	public MDFSRsockBlockCreator(FragmentTransferInfo fragtransinfo, byte[] file, String filename, long filesize, long creatorMAC, String filePathMDFS,  long filelength, byte blockcount, byte n2, byte k2, long filecreatedtime, String[] permlist, String uniquereqid, String fileCreatorGUID, String destGUID){
		this.fragTransInfoHeader = fragtransinfo;
		this.fileName = filename;
		this.filesize = filesize;
		this.creatorMAC = creatorMAC;
		this.filePathMDFS = filePathMDFS;
		this.fileFragLength = filelength;
		this.fileCreatedTime = filecreatedtime;
		this.fileCreatorGUID = fileCreatorGUID;
		this.destGUID = destGUID;
		this.fileFrag = file;
		this.blockCount = blockcount;
		this.n2 = n2;
		this.k2 = k2;
		this.permList = permlist;
		this.uniqueReqID = uniquereqid;

	}

}
