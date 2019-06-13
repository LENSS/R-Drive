package edu.tamu.lenss.mdfs.EdgeKeeper;

import java.util.HashMap;
import java.util.Map;

//data structure used by edgekeeper regarding how to store the file metadata
public class DataStore {
    public Map<Long, EdgeKeeperMetadata> fileIDtoMetadataMap;

    public DataStore(){
        this.fileIDtoMetadataMap = new HashMap<>();
    }


    public void putFileMetadata(EdgeKeeperMetadata metadata){
        fileIDtoMetadataMap.put(metadata.fileID, metadata);
        System.out.println("edgekeeper datastore putting data for fileid: " + metadata.fileID);
    }

    public EdgeKeeperMetadata getFileMetadata(long fileid){
        if(fileIDtoMetadataMap.containsKey(fileid)) {
            System.out.println("edgekeeper datastore success retrieving data for fileid: " + fileid);
            return fileIDtoMetadataMap.get(fileid);
        }else{
            System.out.println("edgekeeper datastore failed retrieving data for fileid: " + fileid);
            return new EdgeKeeperMetadata('f',"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX", 0000,"name",  0, (byte)0, (byte)0); //dummy metadata with command f(failed)..otherwise command is r(return)
        }
    }

}
