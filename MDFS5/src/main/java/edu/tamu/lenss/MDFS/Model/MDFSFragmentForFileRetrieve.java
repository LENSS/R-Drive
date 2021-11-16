package edu.tamu.lenss.MDFS.Model;

import java.io.Serializable;


//This class object is used for fragment request and retrieval.
public class MDFSFragmentForFileRetrieve implements Serializable {

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
    public int n2;
    public int k2;
    public String srcGUID;
    public String destGUID;
    public String fileName;
    public String fileId;
    public int totalNumOfBlocks;
    public int blockIdx;
    public int fragmentIndex;
    public String outputDir;
    public byte[] fileFrag;
    public String filePathMDFS;
    public boolean sameEdge;
    public long entireFileSize;
    public long fragSendingTime; //the timestamp when a fragment is sent from one device to another.

    //default private constructor
    private MDFSFragmentForFileRetrieve(){}


    public MDFSFragmentForFileRetrieve(String uuid, Type type, int n2, int k2, String srcGUID, String destGUID, String fileName, String filePathMDFS, String fileId, int totalNumOfBlocks , int blockIdx, int fragmentIndex, String outputDir, byte[] fileFrag, long entireFileSize, boolean sameedge){
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
        this.outputDir = outputDir;
        this.fileFrag = fileFrag;
        this.entireFileSize = entireFileSize;
        this.sameEdge = sameedge;
    }

    //flips RequestFromOneClientToAnotherForOneFragment object into a ReplyFromOneClientToAnotherForOneFragment object
    public void flipIntoReply(byte[] fileFrag){

        if(type== Type.RequestFromOneClientToAnotherForOneFragment && !flipped){

            //first,change the Type variable
            type = Type.ReplyFromOneClientToAnotherForOneFragment;

            //second, flip src and dest guids
            String temp = new String(srcGUID);
            srcGUID = destGUID;
            destGUID = temp;

            //third, add filefrag
            this.fileFrag = fileFrag;

            //fourth, attach timestamp so receiver can know how long it took for the fragment to travel one way in network via rsock.
            //assuming all devices in edge are time synchronized.
            this.fragSendingTime = System.currentTimeMillis();

            //fifth, change private variable,
            //so calling this function multiple time doesnt have any effect.
            flipped = true;

        }

    }


}
