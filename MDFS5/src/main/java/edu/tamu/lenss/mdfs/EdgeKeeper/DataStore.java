package edu.tamu.lenss.mdfs.EdgeKeeper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//data structure used by edgekeeper to store the file metadata
//this class object should be written periodically to the disk
public class DataStore {

    //dataStore instance
    public static DataStore instance;  //todo: write on disk and read from disk before making new instance

    //variables and data structures
    public int longestFileNameLength;                           //only used for printing filenames in a pretty format
    public Map<Long, EdgeKeeperMetadata> fileIDtoMetadataMap;  //contains metadata for each file
    public Map<String, List<String>> GUIDtoGroupNamesMap;       //cointains group information for each node/GUID | only contains the name doesnt contain GROUP: tag | each name's case is as inputted by user
    public List<Long> deletedFiles;                             //contains all the ids of deleted files

    //private constructors
    private DataStore(){
        this.longestFileNameLength = 0;
        this.fileIDtoMetadataMap = new HashMap<>();
        this.GUIDtoGroupNamesMap = new HashMap<>();
        this.deletedFiles = new ArrayList<>();

        //todo: delete these group testing features
        String[] firstArr = {"ABCD", "EFGH"};
        List<String> first = Arrays.asList(firstArr);
        this.GUIDtoGroupNamesMap.put("1BC657F8971A53E9BD90C285EB17C9080EC3EB8E", first);
        String[] secondArr = {"EFGH", "IJKL"};
        List<String> second = Arrays.asList(secondArr);
        this.GUIDtoGroupNamesMap.put("8877417A2CBA0D19636B44702E7DB497B5834559", second);
        String[] thirdArr = {"IJKL", "ABCD"};
        List<String> third = Arrays.asList(thirdArr);
        this.GUIDtoGroupNamesMap.put("D9C6D170C3C5E6032D0C06D8C495C4E0BB769278", third);
    }

    //instance getter function
    public static DataStore getInstance(){
        if(instance==null){
            instance = new DataStore();
        }else{
            return instance;
        }
        return instance;  //dummy return, should never reach here
    }


    //store EdgeKeeper object of a file into map
    public void putFileMetadata(EdgeKeeperMetadata metadata){
        //store metadata without changing command
        fileIDtoMetadataMap.put(metadata.fileID, metadata);
        System.out.println("edgekeeper datastore putting data for fileid: " + metadata.fileID);

        //check if this file has the longest filename yet
        if(metadata.filename.length()>longestFileNameLength){
            longestFileNameLength = metadata.filename.length();
        }
    }

    //get EdgeKeeper metadata from map
    //if metadata doesnt exists, return with cmd = METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST
    //this function does not check for permission but only checks for existence
    public EdgeKeeperMetadata getFileMetadata(long fileid){
        if(fileIDtoMetadataMap.containsKey(fileid)) {
            System.out.println("edgekeeper datastore success retrieving data for fileid: " + fileid);
            //change commands before returning
            EdgeKeeperMetadata metadata = fileIDtoMetadataMap.get(fileid);
            metadata.setCommand(EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_SUCCESS);
            return metadata;
        }else{
            System.out.println("edgekeeper datastore has not metadata for fileID: " + fileid);
            return new EdgeKeeperMetadata(EdgeKeeperConstants.METADATA_WITHDRAW_REPLY_FAILED_FILENOTEXIST, new ArrayList<>(), "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0000, new String[1], new Date().getTime(), "name",  0, (byte)0, (byte)0); //dummy metadata with command FAILED
        }
    }

    //remove metadata of a file
    public void removeFileMetadata(long fileID){
        fileIDtoMetadataMap.remove(fileID);
    }

    //store a list of groups a Node belongs to
    public void putGroupNameBYGUID(String GUID, List<String> GroupNames){
        if(GroupNames!=null){
            if(GroupNames.size()!=0){
                this.GUIDtoGroupNamesMap.put(GUID, GroupNames);  //always value is overwritten so that if a node's group list changes, we keep the latest status
            }
        }
    }

    //get the list of groups a node belongs to and return
    public List<String> getGroupNamesbyGUID(String GUID){
        if(GUIDtoGroupNamesMap.containsKey((GUID))){
            return GUIDtoGroupNamesMap.get(GUID);
        }else{
            return new ArrayList<>();
        }
    }



}
