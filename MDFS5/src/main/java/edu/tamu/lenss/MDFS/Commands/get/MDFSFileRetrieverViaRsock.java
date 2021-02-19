package edu.tamu.lenss.MDFS.Commands.get;

import org.apache.log4j.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.MDFS.EdgeKeeper.EdgeKeeper;
import edu.tamu.lenss.MDFS.Model.MDFSFileInfo;
import edu.tamu.lenss.MDFS.Model.MDFSFragmentForFileRetrieve;
import edu.tamu.lenss.MDFS.RSock.RSockConstants;
import edu.tamu.lenss.MDFS.Utils.IOUtilities;

public class MDFSFileRetrieverViaRsock {

    public static ExecutorService executor = Executors.newSingleThreadExecutor();

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MDFSFileRetrieverViaRsock.class);

    private byte[] decryptKey;
    private MDFSFileInfo fileInfo;
    private MDFSMetadata metadata;
    private String localDir;

    //constructor
    public MDFSFileRetrieverViaRsock(MDFSFileInfo fInfo, MDFSMetadata metadata, String localDir, byte[] decryptKey) {
        this.fileInfo = fInfo;
        this.metadata = metadata;
        this.localDir = localDir;
        this.decryptKey = decryptKey;
    }

    //start function.
    //returns SUCCESS or error message.
    public String start(){

        //log
        logger.log(Level.ALL, "Starting to retrieve file " + fileInfo.getFileName());

        //make a list for block_fragments I dont have

        int[][]missingBlocksAndFrags = new int[fileInfo.getNumberOfBlocks()][fileInfo.getN2()];

        //get/populate missingBlocksAndFrags array for each block
        boolean succeeded  = false;
        for(int i=0; i< fileInfo.getNumberOfBlocks(); i++){
            succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, fileInfo.getFileName(), fileInfo.getFileID(), fileInfo.getN2() ,i);
        }

        //check if fetching missingBlocksAndFrags information succeeded
        if(succeeded) {

            //print before normalize
            // getUtilsGod.print2DArray(missingBlocksAndFrags, "print before normalize");

            //check if this node has already K frags for each block
            if (getUtils.checkEnoughFragsAvailableForWholeFile(missingBlocksAndFrags, fileInfo.getK2())) {

                //make a mdfsfrag of type= ReplyFromOneClientToAnotherForOneFragment
                //merge the file in a new thread
                MDFSFragmentForFileRetrieve mdfsfrag = new MDFSFragmentForFileRetrieve(UUID.randomUUID().toString(), MDFSFragmentForFileRetrieve.Type.ReplyFromOneClientToAnotherForOneFragment, fileInfo.getN2(), fileInfo.getK2(), EdgeKeeper.ownGUID, "dummyDestGUID", fileInfo.getFileName(), metadata.getFilePathMDFS(), fileInfo.getFileID(), fileInfo.getNumberOfBlocks(), 0, 0, localDir, null, true);
                executor.submit(new FileMerge(mdfsfrag));

                //log
                logger.log(Level.ALL, "merged file blocks for filename "  + fileInfo.getFileName() + " without retrieval since all blocks are available");

                //return
                return "File has been retrieved..check directory.";

            }else {

                //make an index array to indicate block numbers which requires retrieval.
                //1 means requires retrieval, 0 means dont need retrieval.
                int[] blockIndicator = new int[fileInfo.getNumberOfBlocks()];
                Arrays.fill(blockIndicator, 1);

                //check for each block, which one already contain K numbers of fragments,
                //mark all the fragments to be present(mark 0).
                for (int i = 0; i < missingBlocksAndFrags.length; i++) {
                    int k = fileInfo.getK2();
                    for (int j = 0; j < missingBlocksAndFrags[i].length; j++) {
                        if (missingBlocksAndFrags[i][j] == 0) {
                            k--;
                        }
                    }

                    //k==0, meaning there are k numbers of
                    //fragments already available for this block,
                    //so we mark all the other missing fragments
                    //as 0(present), because we dont need to retrieve them.
                    //also, mark blockIndicator to 0 for this block;
                    if (k <= 0) {
                        for (int j = 0; j < missingBlocksAndFrags[i].length; j++) {
                            missingBlocksAndFrags[i][j] = 0;
                            blockIndicator[i] = 0;
                        }
                    }

                }

                //print after normalize
                //getUtilsGod.print2DArray(missingBlocksAndFrags, "print after normalize");

                //choose best nodes for each block, based on metadata.
                //this list contains each entry as block_frag_guid manner.
                List<String> chosenNodes = chooseBestCandidateNodes(missingBlocksAndFrags, blockIndicator);

                //check if chosenNodes are empty
                if (chosenNodes.size() != 0) {

                    String chNodes = "";
                    for (int i = 0; i < chosenNodes.size(); i++) {
                        chNodes = chNodes + chosenNodes.get(i) + "\n";
                    }

                    //log
                    logger.log(Level.ALL,"chosen nodes: " + chNodes);

                    //send out the requests for each tokens
                    for (int i = 0; i < chosenNodes.size(); i++) {
                        sendFragmentRequest(chosenNodes.get(i));
                    }

                    //return success
                    return "Request has been placed.";

                } else {
                    //log
                    logger.log(Level.ERROR, "could not select candidate nodes to request file fragments for file " + fileInfo.getFileName());

                    //return
                    return "could not select candidate nodes to request file fragments.";
                }
            }

        }else{

            //log
            logger.log(Level.ERROR, "could not fetch local fragments from disk for file " + fileInfo.getFileName());

            //return
            return "could not fetch local fragments from disk.";
        }
    }


    //this function takes a token of guid__block#__frag#
    //format, and sends out request on by one.
    private boolean sendFragmentRequest(String s) {

        try {
            //tokenize the string
            String[] tokens = s.split("__");

            //delete empty string
            tokens = IOUtilities.delEmptyStr(tokens);

            //get the variables
            String destGUID = tokens[0];
            int blockIdx = Integer.parseInt(tokens[1]);
            int fragmentIndex = Integer.parseInt(tokens[2]);

            //make an object of mdfsfragRetrieval with request tag
            MDFSFragmentForFileRetrieve mdfsfrag = new MDFSFragmentForFileRetrieve(UUID.randomUUID().toString(), MDFSFragmentForFileRetrieve.Type.RequestFromOneClientToAnotherForOneFragment, fileInfo.getN2(), fileInfo.getK2(), EdgeKeeper.ownGUID, destGUID, fileInfo.getFileName(), fileInfo.getFilePathMDFS(), fileInfo.getFileID(), fileInfo.getNumberOfBlocks(), blockIdx, fragmentIndex, localDir, null, true);


            //get byteArray from object and size of the mdfsfragRetreival obj
            byte[] data = IOUtilities.objectToByteArray(mdfsfrag);

            //send request
            if (data != null) {
                String uuid = UUID.randomUUID().toString().substring(0, 12);
                boolean sent = false;
                if (RSockConstants.RSOCK) {
                    sent = RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", destGUID);
                }

                if (sent) {
                    //log
                    logger.log(Level.ALL, "fragment request sent: " + s);

                    //return
                    return sent;
                } else {

                    //log
                    logger.log(Level.ALL, "failed to send fragment request: " + s);
                }
            }

            return false;
        }catch (Exception e){
            logger.log(Level.ERROR, "Exception happened in sendFragmentRequest(), ", e);
        }

        return false;

    }


    //this function chooses the best nodes for sending file retrieval request.
    //each entry in this function return is of guid__block#__fragment# style.
    private List<String> chooseBestCandidateNodes(int[][] missingBlocksAndFrags, int[] blockIndicator) {

        //make return list
        List<String> chosenNodes = new ArrayList<>();

        //iterate through each block.
        for(int i=0; i< blockIndicator.length; i++){

            //check if its a block having less than K fragments.
            if(blockIndicator[i]==1) {

                //for each fragment indicating 1, find its GUIDs
                int k = fileInfo.getK2();
                for(int j=0; j< missingBlocksAndFrags[i].length; j++){

                    //if this fragment is missing
                    if(missingBlocksAndFrags[i][j]==1){

                        //get the guids holding this fragment
                        List<String> fragGUIDs = metadata.getBlock(i).getFragment(j).getAllFragmentHoldersGUID();

                        //remove my guid from the list
                        fragGUIDs.remove(EdgeKeeper.ownGUID);

                        //check if fragGUIDs is empty,
                        //then we take ony the file creator.
                        if(fragGUIDs.size()==0){
                            chosenNodes.add(metadata.getFileCreatorGUID() + "__" + i + "__" + j);
                        } else{
                            //just get the first one from the list for now.
                            //note: this is where a good algorithm
                            // to choose best node would come in play.
                            chosenNodes.add(fragGUIDs.get(0) + "__" + i + "__" + j);
                        }

                        //decrement k
                        k--;

                    }else{
                        //this fragment already
                        // exists so decrease k anyways.
                        k--;
                    }

                    //check for this block we are making k frag request only.
                    if(k==0){ break; }

                }
            }

        }

        return chosenNodes;
    }
}
