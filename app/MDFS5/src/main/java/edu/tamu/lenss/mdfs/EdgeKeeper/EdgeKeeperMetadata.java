package edu.tamu.lenss.mdfs.EdgeKeeper;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class EdgeKeeperMetadata implements Serializable {
    public char command; //deposit or withdraw
    public String fileCreatorGUID;
    public String filename;
    public long fileID;
    public int numOfBlocks;
    public byte n2;
    public byte k2;
    public List<Map<String, Map<String, List<String>>>> chosenNodes;

    public EdgeKeeperMetadata(){}

    public EdgeKeeperMetadata(char cmd, String fileCreatorguid, long fileid, String name, int numofblocks, byte n2, byte k2){
        this.command = cmd;
        this.fileCreatorGUID = fileCreatorguid;
        this.filename = name;
        this.fileID = fileid;
        this. numOfBlocks = numofblocks;
        this.n2 = n2;
        this.k2 = k2;
        this.chosenNodes = new ArrayList<>();
    }

    public void setn2(byte n2){
        this.n2 = n2;
    }

    public void setk2(byte k2){
        this.k2 = k2;
    }

    public void setCommand(char c){
        this.command = c;
    }



    public String toBuffer(EdgeKeeperMetadata metadata){
        //convert class to json to string
        ObjectMapper Obj = new ObjectMapper();
        String jsonStr = null;
        try {jsonStr = Obj.writeValueAsString(metadata); } catch (IOException e) { e.printStackTrace(); }
        return jsonStr;
    }

    public static EdgeKeeperMetadata parse(String incoming){
        ObjectMapper Obj = new ObjectMapper();
        EdgeKeeperMetadata metadata = null;
        try { metadata = Obj.readValue(incoming, EdgeKeeperMetadata.class); } catch (IOException e) { e.printStackTrace(); }
        return metadata;
    }

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

        List<String> nodeList = new ArrayList<>();
        Iterator<String> it = nodeSet.iterator();
        while(it.hasNext()){
            nodeList.add(it.next());
        }

        return nodeList;
    }


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

        List<String> blockNumList = new ArrayList<>();
        Iterator<String> it = blockNumSet.iterator();
        while(it.hasNext()){
            blockNumList.add(it.next());
        }

        return blockNumList;
    }

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

    public List<Map<String, Map<String, List<String>>>> addGUIDMapToChosenList(List<Map<String, Map<String, List<String>>>> chosenNodes, Map<String, Map<String, List<String>>> guidmap, String guid, Map<String, List<String>> blockmap, String blocknum, List<String> fraglist, String fragment){
        Map<String, Map<String, List<String>>> guidmapRet = addBlockmapToGUIDmap(guidmap, guid, blockmap, blocknum, fraglist, fragment);
        chosenNodes.add(guidmapRet);
        return chosenNodes;
    }

    public Map<String, Map<String, List<String>>> addBlockmapToGUIDmap(Map<String, Map<String, List<String>>> guidmap, String guid, Map<String, List<String>> blockmap, String blocknum, List<String> fraglist, String fragment){
        Map<String, List<String>> blockmapRet = addFragListToBlockMap(blockmap, blocknum, fraglist, fragment);
        guidmap.put(guid, blockmapRet);
        return guidmap;
    }

    public Map<String, List<String>> addFragListToBlockMap(Map<String, List<String>> blockmap, String blocknum, List<String> fraglist, String fragment){
        List<String> fraglistRet = addFragmentToFragList(fraglist, fragment);
        blockmap.put(blocknum, fraglistRet);
        return blockmap;
    }

    public List<String> addFragmentToFragList(List<String> fraglist, String fragment){
        fraglist.add(fragment);
        return fraglist;
    }

}