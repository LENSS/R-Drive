package edu.tamu.lenss.mdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;

//this object is sent through rsock api and parsed back when received
//this class object is only used during file creation
public class MDFSRsockBlockCreator implements Serializable {  				//RSOCK
	public FragmentTransferInfo fragTransInfoHeader;
	public byte[] fileFrag;
	public long fileFragLength;
	public String fileName;
	public long fileCreatedTime;
	public String destGUID;


	public MDFSRsockBlockCreator(FragmentTransferInfo fragtransinfo, byte[] file, String name, long filelength, long filecreatedtime, String destGUID){
		this.fragTransInfoHeader = fragtransinfo;
		this.fileName = name;
		this.fileFragLength = filelength;
		this.fileCreatedTime = filecreatedtime;
		this.destGUID = destGUID;
		this.fileFrag = file;

	}

}
