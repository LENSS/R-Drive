package edu.tamu.lenss.mdfs.handleCommands.get;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MDFSMetadata;
import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileRetrieveNG;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import rsock.Topology;

public class MDFSFileRetrieverViaRsockNG {

    private byte[] decryptKey;
    private MDFSFileInfo fileInfo;
    private MDFSMetadata metadata;
    private String localDir;

    //constructor
    public MDFSFileRetrieverViaRsockNG(MDFSFileInfo fInfo, MDFSMetadata metadata, String localDir, byte[] decryptKey) {
        this.fileInfo = fInfo;
        this.metadata = metadata;
        this.localDir = localDir;
        this.decryptKey = decryptKey;
    }

    //start function.
    //returns SUCCESS or error message.
    public String start(){

        //make a list for block_fragments I dont have
        int[][]missingBlocksAndFrags = new int[fileInfo.getNumberOfBlocks()][fileInfo.getN2()];

        //get/populate missingBlocksAndFrags array for each block
        boolean succeeded  = false;
        for(int i=0; i< fileInfo.getNumberOfBlocks(); i++){
            succeeded = getUtils.getMissingFragmentsOfABlockFromDisk(missingBlocksAndFrags, fileInfo.getFileName(), fileInfo.getCreatedTime(), fileInfo.getN2() ,i);
        }

        //check if fetching missingBlocksAndFrags information succeeded
        if(succeeded) {


            //print before normalize
            print2DArray(missingBlocksAndFrags, "print before normalize");

            //check if this node has already K frags for each block
            if (getUtils.checkEnoughFragsAvailable(missingBlocksAndFrags, fileInfo.getK2())) {
                //logger: merging file without retrieval since all blocks are available
                System.out.println("Enough frags are availabe for each block...doing merge");

                //merge the file in a new thread
                MDFSRsockBlockForFileRetrieveNG mdfsrsockblock = new MDFSRsockBlockForFileRetrieveNG(MDFSRsockBlockForFileRetrieveNG.Type.Reply, fileInfo.getN2(), fileInfo.getK2(), EdgeKeeper.ownGUID, "dummyDEstGUID", fileInfo.getFileName(), fileInfo.getCreatedTime(), fileInfo.getNumberOfBlocks(), (byte)0, (byte)0, localDir, null);
                new FileMerge(mdfsrsockblock).run();
                return "-get Info: File has been retrieved..check directory.";

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
                print2DArray(missingBlocksAndFrags, "print after normalize");

                //choose best nodes for each block, based on metadata.
                //this list contains each entry as block_frag_guid manner.
                List<String> chosenNodes = chooseBestCandidateNodes(missingBlocksAndFrags, blockIndicator);

                //check if chosenNodes are empty
                if (chosenNodes.size() != 0) {

                    //print chosennodes
                    System.out.println("chosen nodes: ");
                    for (int i = 0; i < chosenNodes.size(); i++) {
                        System.out.println(chosenNodes.get(i));
                    }

                    //send out the requests for each tokens
                    for (int i = 0; i < chosenNodes.size(); i++) {
                        sendFragmentRequest(chosenNodes.get(i));
                    }

                    //return success
                    return "-get Info: request has been placed.";

                } else {
                    //logger:
                    return "could not select candidate nodes to request file fragments.";
                }
            }

        }else{

            //logger:
            return "could not fetch local fragments from disk.";
        }
    }


    //this function takes a token of guid__block#__frag#
    //format, and sends out request on by one.
    private boolean sendFragmentRequest(String s) {

        //tokenize the string
        String[] tokens = s.split("__");

        //delete empty string
        tokens = IOUtilities.delEmptyStr(tokens);

        //get the variables
        String destGUID = tokens[0];
        byte blockIdx = (byte) Integer.parseInt(tokens[1]);
        byte fragmentIndex = (byte) Integer.parseInt(tokens[2]);

        //make an object of MDFSRsockBlockRetrieval with request tag
        MDFSRsockBlockForFileRetrieveNG mdfsrsockblock = new MDFSRsockBlockForFileRetrieveNG(MDFSRsockBlockForFileRetrieveNG.Type.Request, fileInfo.getN2(), fileInfo.getK2(), EdgeKeeper.ownGUID, destGUID, fileInfo.getFileName(), fileInfo.getCreatedTime(), fileInfo.getNumberOfBlocks(), blockIdx, fragmentIndex, localDir, null);

        //get byteArray and size of the MDFSRsockBlockRetreival obj
        byte[] data = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            oos = new ObjectOutputStream(bos);
            oos.writeObject(mdfsrsockblock);
            oos.flush();
            data = bos.toByteArray();
        } catch (Exception e) {
            //logger: could not convert object into bytes
        }

        //send request and expect reply
        if (data != null) {
            String uuid = UUID.randomUUID().toString().substring(0, 12);
            return RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing", "nothing", destGUID, 0, RSockConstants.fileRetrieveEndpoint, RSockConstants.fileRetrieveEndpoint, RSockConstants.fileRetrieveEndpoint);
        }

        return false;

    }


    public void print2DArray(int[][] missingBlocksAndFrags, String message){

        System.out.println(message);
        for(int i=0; i< missingBlocksAndFrags.length; i++){
            for(int j=0; j< missingBlocksAndFrags[i].length; j++){
                System.out.print(missingBlocksAndFrags[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println();
        System.out.println();
    }



    //this function chooses the best nodes for sending file retrieval request.
    //each entry in this function is of guid__block#__fragment# style.
    private List<String> chooseBestCandidateNodes(int[][] missingBlocksAndFrags, int[] blockIndicator) {

        //print block indicator
        for (int i=0; i< blockIndicator.length; i++){
            System.out.print(blockIndicator[i] + " ");
        }
        System.out.println();

        //make return list
        List<String> chosenNodes = new ArrayList<>();

        //get neighboring nodes from olsr
        Set<String> peerGUIDsSetfromOLSR = null;
        try {
            peerGUIDsSetfromOLSR = Topology.getInstance(RSockConstants.intrfc_creation_appid).getVertices();
        }catch(Exception e ){
            //dont need to handle this error
        }

        //iterate through each block.
        for(int i=0; i< blockIndicator.length; i++){

            //check if its a block having less than K fragments.
            if(blockIndicator[i]==1) {

                //for each fragment indicating 1, find its GUIDs
                int k = fileInfo.getK2()+1;
                for(int j=0; j< missingBlocksAndFrags[i].length; j++){

                    if(missingBlocksAndFrags[i][j]==1){

                        //get the guids holding this fragment
                        List<String> fragGUIDs = metadata.getBlock(i).getFragment(j).getAllFragmentHoldersGUID();

                        //common guids from olsr and fragGUIDs
                        List<String> commonGUIDs = new ArrayList<>();

                        if(peerGUIDsSetfromOLSR!=null) {

                            ///cross olsr GUIDs with fragGUIDs and get the common ones
                            for (int l = 0; l < fragGUIDs.size(); l++) {
                                if (peerGUIDsSetfromOLSR.contains(fragGUIDs.get(l))) {
                                    commonGUIDs.add(fragGUIDs.get(l));
                                }
                            }

                        }else{
                            //olsr returned null or empty,
                            //so we continue without it.
                            commonGUIDs.addAll(fragGUIDs);
                        }

                        //remove my guid from the list
                        commonGUIDs.remove(EdgeKeeper.ownGUID);

                        //check if commonGUIDs is empty,
                        //then we take ony the file creator.
                        //in this case, file creator is not
                        //nearby, but we have no other way.
                        if(commonGUIDs.size()==0){
                            chosenNodes.add(metadata.getFileCreatorGUID() + "__" + i + "__" + j);
                        }
                        //other wise we take the file creator, if it is nearby.
                        else if(commonGUIDs.contains(metadata.getFileCreatorGUID())){
                            chosenNodes.add(metadata.getFileCreatorGUID() + "__" + i + "__" + j);
                        }
                        //otherwise we consider other nodes as candidate,
                        //who are nearby and available.
                        else{
                            chosenNodes.add(commonGUIDs.get(0) + "__" + i + "__" + j);
                        }

                        //decrement k
                        k--;

                    }

                    //check for this block we are making k+1 frag request only.
                    //we do one extra for assurance.
                    if(k==0){ break; }


                }
            }

        }

        return chosenNodes;
    }












}
