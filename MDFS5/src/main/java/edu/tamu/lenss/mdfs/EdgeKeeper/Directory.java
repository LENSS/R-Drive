package edu.tamu.lenss.mdfs.EdgeKeeper;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Directory {

    //Directory variables
    private Directory directory;
    private Root rootDir;

    //private constructors
    private Directory(){
        rootDir = new Root();
    }

    //Diretory getter function
    public Directory getInstance(){
        if(directory==null){
            directory = new Directory();
        }
        return directory;
    }

    //check if a directory in root/subDir exists in MDFS
    //input is /A/B/C/D/
    //output is boolean is A/B/C/D exists or nah
    public boolean dirExists(String dir){

        //check if dir is "/"
        if(dir.equals("/")){return true;}

        //first separate them in tokens by "/"
        String[] dirTokens = dir.split("/");

        //check if this has atleast one token
        if(dirTokens.length==0){return false;}

        //if dirTokens has at least one token, then root dir
        //should have at least one subDir.
        //check if root dir has any subDir at all.
        if(rootDir.listOfSubdirectories==null){ return false;}

        //check if root subDirectory list contains the first token.
        //if not, that means other tokens also doesnt exists
        if(!rootDir.listOfSubdirectories.keySet().contains(dirTokens[0])){
            return false;
        }

        //coming here means first subdirectory A, inside root exists.
        //check for subdirectories B/C/D/ inside that first subDirectory.
        if(dirTokens.length>1){
            return subDirExists(rootDir.listOfSubdirectories.get(dirTokens[0]), dirTokens, 1, true);
        }else{
            //coming here means input was something like "/A/",
            //and folder A exists we already checked.
            return true;
        }
    }

    //helper for dirExists()
    private boolean subDirExists(SubDirectory subDir, String[] dirTokens, int index, boolean prevDirExists){
        if(index<dirTokens.length){
            if(subDir.listOfSubdirectories!= null && subDir.getAllSubDirNames().contains(dirTokens[index])){
                return subDirExists(subDir, dirTokens, index++, true);
            }else{
                return false;
            }
        }else{
            return prevDirExists;
        }
    }

    //todo:
    /*//check if a file exists in a given dir
    public boolean fileExists(long fileID, String dir){
        if(dirExists(dir)){

        }else{
            //the directory itself doesnt exists, so file doesnt exist either
            return false;
        }

        return false;
    }

    public FileMetadata getFileMetadata(long fileID){

    }*/

    //static class for root directory
    private static class Root{
        private String thisDirName;
        private String[] permissionList;
        private long creationTime;
        private String uniqueID;
        private long dirSeqNum;
        private Map<String, SubDirectory> listOfSubdirectories;
        private Map<Long, FileMetadata> listOfFiles;

        public Root(){
            this.thisDirName = "/";
            String[] permission  = {"WORLD"};
            this.permissionList = permission;
            this.creationTime = new Date().getTime();
            this.uniqueID = UUID.randomUUID().toString().substring(0,12);
            this.dirSeqNum = -1;
            this.listOfSubdirectories = new HashMap<>();
            this.listOfFiles = new HashMap<>();
        }


        //just adds a subDir inside root Dir
        public void addSubDirectoryInRoot(String newSubDirName, SubDirectory newSubDir){
            this.listOfSubdirectories.put(newSubDirName, newSubDir);
        }

        //removes a subDir inside root Dir
        public void removeSubDirectoryFromRoot(String newSubDirName){
            this.listOfSubdirectories.remove(newSubDirName);
        }


        //adds a file metadata inside root
        public void addFileInRoot(long fileID, FileMetadata fileMetadata){
            this.listOfFiles.put(fileID, fileMetadata);
        }

        //removes a file metadata from root
        public void removeFileFromRoot(long fileID){
            this.listOfFiles.remove(fileID);
        }

        //get a file metadata from root
        public FileMetadata getFileMetadataFromRoot(long fileID){
            return this.listOfFiles.get(fileID);

        }

    }





    //class for subDirectories
    private class SubDirectory{
        private String thisDirName;
        private String[] permissionList;
        private long creationTime;
        private String uniqueID;
        private long dirSeqNum;
        private Map<String, SubDirectory> listOfSubdirectories;
        private Map<Long, FileMetadata> listOfFiles;

        public SubDirectory(String thisdirname, String[] permList, long creationtime, String uniqueID, long dirSeqNum){
            this.thisDirName = thisdirname;
            this.permissionList = permList;
            this.creationTime = creationtime;
            this.uniqueID = uniqueID;
            this.dirSeqNum = dirSeqNum;
            this.listOfSubdirectories = new HashMap<>();
            this.listOfFiles = new HashMap<>();
        }

        //adds a subDir inside another subDir
        public void addSubDirectoryInSubDir(String newSubDirName, SubDirectory newSubDir){
            this.listOfSubdirectories.put(newSubDirName, newSubDir);
        }

        //removes a subDir inside another subDir
        public void removeSubDirectoryFromSubDir(String newSubDirName){
            this.listOfSubdirectories.remove(newSubDirName);
        }

        //adds a file metadata inside a subDIr
        public void addFileInSubDirectory(long fileID, FileMetadata fileMetadata){
            this.listOfFiles.put(fileID, fileMetadata);
        }

        //removes a file metadata from a subDir
        public void removeFileFromSubDir(long fileID){
            this.listOfFiles.remove(fileID);
        }

        //get a file metadata from subDir
        public FileMetadata getFileMetadataFromSubDir(long fileID){
            return this.listOfFiles.get(fileID);

        }

        //get subDirectory list
        public Set<String> getAllSubDirNames(){
            return listOfSubdirectories.keySet();
        }

    }


}
