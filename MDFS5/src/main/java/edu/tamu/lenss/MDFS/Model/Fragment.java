package edu.tamu.lenss.MDFS.Model;
import java.io.Serializable;


// Before storing a fragment in local storage, we wrap the fragment data into this class,
// convert the class object into byteArray,
// and only then write the byteArray into disk.
public class Fragment implements Serializable {

    public String fileName;			//name of the file
    public String filePathMDFS;		//file path in MDFS in which the file will be stored(full path containing filename at the end)
    public byte[] fileFrag;			//the byteArray that contains filefrag
    public String fileID;			//fileiD
    public long entireFileSize;		//size of the entire file
    public int n2;					//n2
    public int k2;					//k2
    public int blockIdx;			//block number of this file
    public int fragmentIdx;			//fragment number of this block
    public int totalNumOfBlocks;    //total number of blocks for this file


    public Fragment(String filename, String filePathMDFS, byte[] filefrag, String fileID, long entirefilesize, int n2, int k2, int blockidx, int fragmentidx, int totalNumOfBlocks) {
        this.fileName = filename;
        this.filePathMDFS = filePathMDFS;
        this.fileFrag = filefrag;
        this.fileID = fileID;
        this.entireFileSize = entirefilesize;
        this.n2 = n2;
        this.k2 = k2;
        this.blockIdx = blockidx;
        this.fragmentIdx = fragmentidx;
        this.totalNumOfBlocks = totalNumOfBlocks;
    }
}
