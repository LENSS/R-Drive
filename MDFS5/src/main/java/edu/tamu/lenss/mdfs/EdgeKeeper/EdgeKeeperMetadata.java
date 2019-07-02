package edu.tamu.lenss.mdfs.EdgeKeeper;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

//This class is used to send and requests and receive reply from edgeKeeper.
public class EdgeKeeperMetadata implements Serializable {

    //all variables
    public int command;
    public List<String> ownGroupNames;
    public String metadataDepositorGUID;
    public String fileCreatorGUID;
    public String metadataRequesterGUID;
    public String groupConversionRequesterGUID;
    public String filename;
    public long fileID;
    public long timeStamp;
    public int numOfBlocks;
    public byte n2;
    public byte k2;
    public List<Map<String, Map<String, List<String>>>> chosenNodes;  //the main data structure that holds a list that contains mapping from GUID to blockNUm to FRagmentNum
    public List<String> groupOrGUID;
    public String[] permissionList;

    //empty constructors
    public EdgeKeeperMetadata(){}

    //constructor for metadata deposit by file creator with cmd = FILE_CREATOR_METADATA_DEPOSIT_REQUEST (object made by -client, sent to- EdgeKeeper, reason - to deposit metadata after a file creator created a file)
    //constructor for metadata deposit by fragment receiver with cmd = FRAGMENT_RECEIVER_METADATA_DEPOSIT_REQUEST (object made by -client, sent to- EdgeKeeper, reason - to deposit metadata after a fragment of a file has been received)
    //constructor for metadata withdraw reply with cmd = METADATA_WITHDRAW_REPLY_SUCCESS (object made by EdgeKeeper, sent to - client, reason - success to reply with metadata of a file)
    //constructor for metadata withdraw reply with cmd = METADATA_WITHDRAW_REPLY_FAILED (object made by EdgeKeeper, sent to - client, reason - failed to reply with metadata of a file but no metadata found for this file so dummy metadata sent)
    public EdgeKeeperMetadata(int cmd, List<String> owngroupnames, String metadataDepositorGUID, String fileCreatorGUID, long fileid, String[] permList, long timeStamp, String filename, int numofblocks, byte n2, byte k2){
        this.command = cmd;
        this.ownGroupNames = owngroupnames;
        this.metadataDepositorGUID = metadataDepositorGUID;
        this.fileCreatorGUID = fileCreatorGUID;
        this.permissionList = permList;
        this.filename = filename;
        this.fileID = fileid;
        this.timeStamp = timeStamp;
        this. numOfBlocks = numofblocks;
        this.n2 = n2;
        this.k2 = k2;
        this.chosenNodes = new ArrayList<>(); //this list if populated by addInfo() function
    }

    //constructor for metadata withdraw request with cmd  = METADATA_WITHDRAW_REQUEST (object made by -client, sent to - EdgeKeeper, reason -to ask for metadata for a file)
    public EdgeKeeperMetadata(int cmd, List<String> owngroupnames, String metadataRequesterGUID, long fileid, long timestamp, String filename){
        this.command = cmd;
        this.ownGroupNames = owngroupnames;
        this.metadataRequesterGUID = metadataRequesterGUID;
        this.filename = filename;
        this.fileID = fileid;
        this.timeStamp = timestamp;
        this.chosenNodes = new ArrayList<>(); //this list if populated by addInfo() function
    }

    //constructor for GroupName to GUID conversion requests with cmd = GROUP_TO_GUID_CONV_REQUEST  (object made by -client, sent to -EdgeKeeper, reason - asking for GUIDs who belong to a group name)
    //constructor for GroupName to GUID conversion reply with cmd = GROUP_TO_GUID_CONV_REPLY_SUCCESS  (object made by -EdgeKeeper, sent to -Client, reason - success in replying with GUIDs who belong to a group name)
    //constructor for GroupName to GUID conversion reply with cmd = GROUP_TO_GUID_CONV_REPLY_FAILED  (object made by -EdgeKeeper, sent to -Client, reason - failed in replying with GUIDs who belong to a group name)
    public EdgeKeeperMetadata(int cmd, long timeStamp, List<String> owngroupnames, String groupConversionRequesterGUID, List<String> list){
        this.command = cmd;
        this.timeStamp = timeStamp;
        this.ownGroupNames = owngroupnames;
        this.groupConversionRequesterGUID = groupConversionRequesterGUID;
        this.groupOrGUID = list;
        this.chosenNodes = new ArrayList<>(); //this list if populated by addInfo() function
    }


    public void setn2(byte n2){
        this.n2 = n2;
    }

    public void setk2(byte k2){
        this.k2 = k2;
    }

    public void setCommand(int c){
        this.command = c;
    }

    public void setPermission(String[] perm){ this.permissionList = perm; }



    //conversion of EdgeKeeper object to json String
    public String toBuffer(EdgeKeeperMetadata metadata){
        //convert class to json to string
        ObjectMapper Obj = new ObjectMapper();
        String jsonStr = null;
        try {jsonStr = Obj.writeValueAsString(metadata); } catch (IOException e) { e.printStackTrace(); }
        return jsonStr;
    }

    //convertion of json string to EdgeKeeper object
    public static EdgeKeeperMetadata parse(String incoming){
        ObjectMapper Obj = new ObjectMapper();
        EdgeKeeperMetadata metadata = null;
        try { metadata = Obj.readValue(incoming, EdgeKeeperMetadata.class); } catch (IOException e) { e.printStackTrace(); }
        return metadata;
    }

    //returns list of GUIDs who has any fragment of a block number
    public List<String> getNodesContainingFragmentsOfABlock(String blocknum){
        List<String> allUniqueNodes = getAllUniqueFragmentHolders();
        List<String>  guidsList = new ArrayList<>();

        for(int i=0 ;i < allUniqueNodes.size(); i++){
            for(int j=0; j< chosenNodes.size(); j ++){
                if(chosenNodes.get(j).containsKey(allUniqueNodes.get(i))){
                    if(chosenNodes.get(j).get(allUniqueNodes.get(i)).containsKey(blocknum)){
                        guidsList.add(allUniqueNodes.get(i));
                    }
                }
            }
        }

        return guidsList;
    }


    //returns a list of unique GUIDs who have one/any fragment of this file
    public List<String> getAllUniqueFragmentHolders(){
        Set<String> nodeSet = new HashSet<>();
        for(int i=0; i< chosenNodes.size(); i++){
            Set<String> setOfGuid = chosenNodes.get(i).keySet();
            Iterator<String> it = setOfGuid.iterator();
            while(it.hasNext()){
                String temp = it.next();
                if(!nodeSet.contains(temp)){
                    nodeSet.add(temp);
                }
            }
        }

        List<String> nodeList = new ArrayList<>(nodeSet);
        return nodeList;
    }


    //takes a GUID and returns the blockNumbers carried by this GUID for this file
    public List<String> getBlockNumbersHeldByNode(String guid){
        Set<String> blockNumSet = new HashSet<>();
        for(int i=0; i< chosenNodes.size(); i++){
            if(chosenNodes.get(i).containsKey(guid)){
                Set<String> setOfBlockNum = chosenNodes.get(i).get(guid).keySet();
                Iterator<String> it = setOfBlockNum.iterator();
                while(it.hasNext()){
                    String temp = it.next();
                    if(!blockNumSet.contains(temp)){
                        blockNumSet.add(temp);
                    }
                }
            }
        }

        List<String> blockNumList = new ArrayList<>(blockNumSet);
        return blockNumList;
    }

    //takes a GUID and a block number and returns the list of fragments of this block this node contains for this file
    public List<String> getFragmentListByNodeAndBlockNumber(String guid, String blocknum){
        for(int i=0; i< chosenNodes.size(); i++){
            if(chosenNodes.get(i).containsKey(guid)){
                if(chosenNodes.get(i).get(guid).containsKey(blocknum)){
                    return chosenNodes.get(i).get(guid).get(blocknum);
                }
            }
        }

        return new ArrayList<>();
    }


    //add information to the metadata object
    //this function populates the chosenNodes list
    public void addInfo(String guid, String blocknum, String fragmentnum){
        boolean done = false;

        //first check if entry already exists
        for(int i=0; i< chosenNodes.size(); i++){
            if(chosenNodes.get(i).containsKey(guid)){
                if(chosenNodes.get(i).get(guid).containsKey(blocknum)){
                    boolean contains = false;
                    for(int j=0;j<chosenNodes.get(i).get(guid).get(blocknum).size(); j++){
                        if(chosenNodes.get(i).get(guid).get(blocknum).get(j).equals(fragmentnum)){
                            contains = true;
                            done = true;
                        }
                    }
                    if(!contains) {
                        chosenNodes.get(i).get(guid).get(blocknum).add(fragmentnum);
                        done = true;
                    }
                }else{
                    List<String> fraglist = new ArrayList<>();
                    fraglist.add(fragmentnum);
                    chosenNodes.get(i).get(guid).put(blocknum,fraglist);
                    done = true;
                }
            }
        }

        //if not done, that means need to add a totally new entry to chosenNodes list
        //this line is usually executed each time a new Map<guid,Map<blocknum, fragmentList>> is added to the chosenNodes
        if(!done){
            chosenNodes = addGUIDMapToChosenList(chosenNodes, new HashMap<>(), guid, new HashMap<>(), blocknum, new ArrayList<>(), fragmentnum);
        }

    }

    private List<Map<String, Map<String, List<String>>>> addGUIDMapToChosenList(List<Map<String, Map<String, List<String>>>> chosenNodes, Map<String, Map<String, List<String>>> guidmap, String guid, Map<String, List<String>> blockmap, String blocknum, List<String> fraglist, String fragment){
        Map<String, Map<String, List<String>>> guidmapRet = addBlockmapToGUIDmap(guidmap, guid, blockmap, blocknum, fraglist, fragment);
        chosenNodes.add(guidmapRet);
        return chosenNodes;
    }

    private Map<String, Map<String, List<String>>> addBlockmapToGUIDmap(Map<String, Map<String, List<String>>> guidmap, String guid, Map<String, List<String>> blockmap, String blocknum, List<String> fraglist, String fragment){
        Map<String, List<String>> blockmapRet = addFragListToBlockMap(blockmap, blocknum, fraglist, fragment);
        guidmap.put(guid, blockmapRet);
        return guidmap;
    }

    private Map<String, List<String>> addFragListToBlockMap(Map<String, List<String>> blockmap, String blocknum, List<String> fraglist, String fragment){
        List<String> fraglistRet = addFragmentToFragList(fraglist, fragment);
        blockmap.put(blocknum, fraglistRet);
        return blockmap;
    }

    private List<String> addFragmentToFragList(List<String> fraglist, String fragment){
        fraglist.add(fragment);
        return fraglist;
    }

}