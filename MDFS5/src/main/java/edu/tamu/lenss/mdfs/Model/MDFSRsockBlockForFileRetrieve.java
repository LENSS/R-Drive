package edu.tamu.lenss.mdfs.Model;

import java.io.Serializable;


//This class object is used for fragment request and retrieval.
public class MDFSRsockBlockForFileRetrieve implements Serializable {



    //private variables
    private boolean flipped = false;

    //enum
    public enum Type{ Request, Reply }

    public String uuid;
    public Type type;
    public byte n2;
    public byte k2;
    public String srcGUID;
    public String destGUID;
    public String fileName;
    public long fileId;
    public byte totalNumOfBlocks;
    public byte blockIdx;
    public byte fragmentIndex;
    public String localDir;
    public byte[] fileFrag;

    //default private constructor
    private MDFSRsockBlockForFileRetrieve(){}

    //only public constructor
    public MDFSRsockBlockForFileRetrieve(String uuid, Type type, byte n2, byte k2, String srcGUID, String destGUID, String fileName, long fileId, byte totalNumOfBlocks , byte blockIdx, byte fragmentIndex, String locDir, byte[] fileFrag){
        this.uuid = uuid;
        this.type = type;
        this.n2 = n2;
        this.k2 = k2;
        this.srcGUID = srcGUID;  //this node
        this.destGUID = destGUID;  //other node
        this.fileName = fileName;
        this.fileId = fileId;
        this.totalNumOfBlocks = totalNumOfBlocks;
        this.blockIdx = blockIdx;
        this.fragmentIndex = fragmentIndex;
        this.localDir = locDir;
        this.fileFrag = fileFrag;
    }

    //flips Request object into a Reply object
    public void flipIntoReply(byte[] fileFrag){

        if(type==Type.Request && !flipped){

            //first,change the TYpe variable
            type = Type.Reply;

            //second, flip src and dest guids
            String temp = new String(srcGUID);
            srcGUID = destGUID;
            destGUID = temp;

            //third, add filefrag
            this.fileFrag = fileFrag;

            //fourth, change private variable,
            //so calling this function multiple time doesnt have any effect.
            flipped = true;

        }

    }


}
