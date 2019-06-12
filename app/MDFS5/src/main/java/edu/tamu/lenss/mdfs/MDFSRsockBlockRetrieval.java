package edu.tamu.lenss.mdfs;

import java.io.Serializable;
import java.io.File;
import java.io.Serializable;

import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;


//This class object is sent from MDFSBlockRetrieval.FragmentUploadreViaRsock class, through rsock,
// and parsed back when received by RsockReceiveForFileRetrieval class.
//This class object is only used when doing block retreival.
//Depending on the type (request or reply), this object is handled differently.
//If it is a request, meaning a node(say A) is asking another node(say B) to send back a block,
// at this point, the fileFrag variable is null,
//if it is a reply,that means a node(say B) previously received a request, processed it, and sent back a block to requester(node A),
//at this point, the fileFrag variable will contain data.
public class MDFSRsockBlockRetrieval implements Serializable {

    //request variables
    public FragmentTransferInfo fragTransInfoHeader;
    public String srcGUID;    //the GUID from which the req has been made | the GUID from which the header has been sent
    public String destGUID;   //the GUID to which the req for retrieving fragment has been made
    public String fileName;
    public long fileId;
    public byte blockIdx;
    public byte fragmentIndex;

    //reply variables
    public byte[] fileFrag;
    public int fileFragLength;
    public boolean fileFragRetrieveSuccess;

    //request constructor
    public MDFSRsockBlockRetrieval(FragmentTransferInfo header, String destGUID, String srcGUID, String filename, long fileid, byte blockidx, byte fragmentindex){  //srcGUID = requester sender GUID |  destGUID = requester receiver GUID
        this.fragTransInfoHeader = header;
        this.destGUID = destGUID;
        this.srcGUID = srcGUID;
        this.fileFrag = null;
        this.fileFragLength = 0;
        this.fileName = filename;
        this.fileId = fileid;
        this.blockIdx = blockidx;
        this.fragmentIndex = fragmentindex;
        fileFragRetrieveSuccess = false;
    }

    //reply constructor
    public MDFSRsockBlockRetrieval(byte[] filefrag, int fraglength, boolean fileSuccess, String myGUID){
        this.fileFrag = filefrag;
        this.fragTransInfoHeader = null;
        this.destGUID = null;
        this.srcGUID = myGUID;
        this.fileFragLength = fraglength;
        this.fileName = null;
        this.fileId = 0;
        this.blockIdx = 0;
        this.fragmentIndex = 0;
        this.fileFragRetrieveSuccess = fileSuccess;
    }
}
