package edu.tamu.lenss.MDFS.Model;

import java.io.Serializable;


//This class object is used for fragment request and retrieval.
public class MDFSRsockBlockForFileRetrieve implements Serializable {

    //private variables
    private boolean flipped = false;

    //enum
    public enum Type{
        RequestFromOneClientToAnotherForOneFragment,
        ReplyFromOneClientToAnotherForOneFragment,
        RequestFromOneClientInOneAEdgeToMasterOfAnotherEdgeForWholeFile,
        RequestFromMasterToClientInSameEdgeForWholeFile
    }

    //public variables
    public String blockRetrieveReqUUID;
    public Type type;
    public byte n2;
    public byte k2;
    public String srcGUID;
    public String destGUID;
    public String fileName;
    public String fileId;
    public byte totalNumOfBlocks;
    public byte blockIdx;
    public byte fragmentIndex;
    public String localDir;
    public byte[] fileFrag;
    public String filePathMDFS;
    public boolean sameEdge;

    //default private constructor
    private MDFSRsockBlockForFileRetrieve(){}


    public MDFSRsockBlockForFileRetrieve(String uuid, Type type, byte n2, byte k2, String srcGUID, String destGUID, String fileName, String filePathMDFS, String fileId, byte totalNumOfBlocks , byte blockIdx, byte fragmentIndex, String locDir, byte[] fileFrag, boolean sameedge){
        this.blockRetrieveReqUUID = uuid;
        this.type = type;
        this.n2 = n2;
        this.k2 = k2;
        this.srcGUID = srcGUID;  //this node
        this.destGUID = destGUID;  //other node
        this.fileName = fileName;
        this.filePathMDFS = filePathMDFS;
        this.fileId = fileId;
        this.totalNumOfBlocks = totalNumOfBlocks;
        this.blockIdx = blockIdx;
        this.fragmentIndex = fragmentIndex;
        this.localDir = locDir;
        this.fileFrag = fileFrag;
        this.sameEdge = sameedge;
    }

    //flips RequestFromOneClientToAnotherForOneFragment object into a ReplyFromOneClientToAnotherForOneFragment object
    public void flipIntoReply(byte[] fileFrag){

        if(type==Type.RequestFromOneClientToAnotherForOneFragment && !flipped){

            //first,change the Type variable
            type = Type.ReplyFromOneClientToAnotherForOneFragment;

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
