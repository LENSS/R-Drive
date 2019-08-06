package edu.tamu.lenss.mdfs;

//this is a wrapper class that contains an object of either MDFSRsockBlockCreator or MDFSRsockBlockRetriever.
//this class object is received by a while loop on the receiver side.
//depending on the type of object this class contains, different job is done.
//see MDFSRsockBlockCreator.java and MDFSRsockBlockRetriever.java files for more details.

import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileCreate;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileRetrieve;

public class MDFSRsockBlock {
    public String type;
    public MDFSRsockBlockForFileCreate mdfsrsockblockcreator;
    public MDFSRsockBlockForFileRetrieve mdfsrsockblockretrieval;

    //constructor for file creation/block uploading
    public MDFSRsockBlock(String type, MDFSRsockBlockForFileCreate mdfsrsockblockcreator){
        this.type = type;
        this.mdfsrsockblockcreator = mdfsrsockblockcreator;
        this.mdfsrsockblockretrieval = null;
    }

    //constructor for file retrieval/ block downloading
    public MDFSRsockBlock(String type, MDFSRsockBlockForFileRetrieve mdfsrsockblockretrieval ){
        this.type = type;
        this.mdfsrsockblockcreator = null;
        this.mdfsrsockblockretrieval = mdfsrsockblockretrieval;
    }
}
