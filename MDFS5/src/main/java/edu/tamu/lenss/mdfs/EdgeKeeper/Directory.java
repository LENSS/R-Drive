package edu.tamu.lenss.mdfs.EdgeKeeper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Directory {

    //Directory variables
    private Directory directory;
    private Root rootDir;
    public static String[] PERMISSION  = {"WORLD"};

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
    //input is /A/B/C/D/ or /A/B/C/D
    //output is boolean if dir exists or nah
    public boolean dirExists(String dir){
        //check if the first char is "/",
        //otherwise return false
        if(dir.charAt(0)!='/'){return false;}

        //check if dir is "/"
        if(dir.equals("/")){return true;}

        //first separate them in tokens by "/"
        String[] dirTokens = dir.split("/");

        //check if this dir has at least one token
        if(dirTokens.length==0){return false;}

        //if dirTokens has at least one token, then root dir
        //should have at least one subDir.
        //check if root dir has any subDir at all.
        if(rootDir.listOfSubdirectories==null){ return false;}

        //check if root subDirectory list contains the first token.
        //if not, that means other tokens also doesnt exists
        if(!rootDir.getAllSubDirNames().contains(dirTokens[0])){
            return false;
        }

        //coming here means first subdirectory A, inside root exists.
        //check for subdirectories B/C/D/ inside that first subDirectory.
        if(dirTokens.length>1){
            return subDirExists(rootDir.getSubDirectoryFromRoot(dirTokens[0]), dirTokens, 1, true);
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


    //returns a file metadata if it exists.
    //input is filename such as xyz.jpg, and dir such as /A/B/C/ or /A/B/C.
    //output is FileMetadata or null.
    public FileMetadata getFileMetadata(String filename, String dir){
        //check if directory exists
        if(dirExists(dir)){

            //check if dir is only "/" aka root
            if(dir.equals("/")){
                //check if root dir has any file in it at all.
                if(rootDir.listOfFiles==null){ return null;}

                //if not, check if root has the file
                if(rootDir.getAllFileNames().contains(filename)){
                    return rootDir.getFileMetadataFromRoot(filename);
                }else{
                    return null;
                }
            }else{
                //separate the dir in tokens
                String[] dirTokens = dir.split("/");

                //check if this dir has atleast one token
                if(dirTokens.length==0){return null;}


                //iterate
                SubDirectory subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
                if(dirTokens.length==1){
                    //this is the first subDir after root
                    //check if subDir has the file
                    if(subDir.listOfFiles!=null && subDir.getAllFileNames().contains(filename)){
                        return subDir.getFileMetadataFromSubDir(filename);
                    }
                }else{
                    for(int i=1; i< dirTokens.length; i++){
                        subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
                    }

                    //check if subDir has any file at all
                    if(subDir.listOfFiles==null){return null;}

                    //if not, check if that subDir has the file in it.
                    if(subDir.getAllFileNames().contains(filename)){ return subDir.getFileMetadataFromSubDir(filename);}
                    else {return null;}
                }

            }

        }else{
            //the directory itself doesnt exists, so file doesnt exist either
            return null;
        }
        return null;
    }

    //check if a file exists in a given dir.
    //input is filename such as xyz.jpg, and dir such as /A/B/C/ or /A/B/C.
    //output is FileMetadata or null.
    public boolean fileExists(String filename, String dir){
        if(getFileMetadata(filename, dir)!=null){return true;}
        else{return false;}
    }

    //removes a FileMetadata object from a directory if the directory
    //and file exists.
    public boolean removefileMetadata(String filename, String dir){
        //check if the directory exists
        if(dirExists(dir)){
            //check if the directory is the root
            if(dir.equals("/")){
                //check if the root directory has any file at all,
                //if it does then delete.
                if(rootDir.listOfFiles!=null && rootDir.getAllFileNames().contains(filename)){
                    rootDir.removeFileFromRoot(filename);
                    return true;
                }
            }else{
                //separate the dir in tokens
                String[] dirTokens = dir.split("/");

                //check if the dirTokens has at least oen token
                if(dirTokens.length>0) {
                    //iterate to the correct subDir
                    SubDirectory subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
                    if (dirTokens.length == 1) {
                        //this is the first subDir after root
                        //check if subDir has the file
                        if (subDir.listOfFiles != null && subDir.getAllFileNames().contains(filename)) {
                            subDir.removeFileFromSubDir(filename);
                            return true;
                        }
                    } else {
                        for (int i = 1; i < dirTokens.length; i++) {
                            subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
                        }
                        subDir.removeFileFromSubDir(filename);
                        return true;
                    }
                }
            }
        }else{
            //do nothing, directory doesnt exists to begin with.
            return false;
        }

        return false;
    }

    //get all filenames in a given directory
    //output is arrayList which can be empty.
    public List<String> getAllFileNames(String dir){
        //first check if the dir exists
        if(dirExists(dir)){
            //check if dir is only "/" aka root
            if(dir.equals("/")){
                //check if root dir has any file in it at all.
                if(rootDir.listOfFiles==null){ return new ArrayList<>();}

                //if not, get all file names in a list and send
                return new ArrayList<>(rootDir.getAllFileNames());

            }else{
                //separate the dir in tokens
                String[] dirTokens = dir.split("/");

                //check if this dir has atleast one token
                if(dirTokens.length==0){return new ArrayList<>();}

                //iterate to the correct subDir
                SubDirectory subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
                if(dirTokens.length==1){
                    //this is the first subDir after root
                    //check if subDir has any file at all
                    if(subDir.listOfFiles==null){return new ArrayList<>();}

                    //otherwise, get all the file names and send
                    return new ArrayList<>(subDir.getAllFileNames());
                }else{
                    for(int i=1; i< dirTokens.length; i++){
                        subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
                    }
                    //check if the subDir has any file at all
                    if(subDir.listOfFiles==null){ return new ArrayList<>();}

                    //if not, return all the filenames in this dir
                    new ArrayList<>(subDir.getAllFileNames());
                }

            }

        }else{
            //the directory doesnt exists to begin with,
            //so there is no file.
            return new ArrayList<>();
        }

        return new ArrayList<>();
    }

    //put a file metadata in directory
    //if the directory doesnt exists then create it
    public void putFileMetadata(){
        //first parse the directories int
    }

    //create a directory recursively
    public boolean addDirectory(String dir){
        //check if the dir starts with "/"
        if(dir.charAt(0)!='/'){return false;}

        //parse the dir into tokens
        String[] dirTokens = dir.split("/");

        //check if dirTokens has at least one entry
        if(dirTokens.length==0){ return false;}

        //check if root dir has the first token already
        //if not add first token.
        SubDirectory subDir;
        if(rootDir.listOfSubdirectories!=null){
            if(rootDir.getAllSubDirNames().contains(dirTokens[0])){
                //root already contains first token so load the directory.
                subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
            }else{
                //root contains many other directoy but not this so add the first token as directory and load it.
                rootDir.addSubDirectoryInRoot(dirTokens[0], new SubDirectory(dirTokens[0], PERMISSION, new Date().getTime(), UUID.randomUUID().toString().substring(0,12), 0000));
                subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
            }
        }else{
            //root doesnt contain any directory, so add the first token as directory and load it.
            rootDir.addSubDirectoryInRoot(dirTokens[0], new SubDirectory(dirTokens[0], PERMISSION, new Date().getTime(), UUID.randomUUID().toString().substring(0,12), 0000));
            subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
        }

        //check if dirTokens has more than one entry for subDir
        if(dirTokens.length>1){
            for(int i=1; i< dirTokens.length; i++){
                //check if subDir contains any dir at all
                if(subDir!=null){
                    //check if subdirectory contains the dirToken[i]
                    if(subDir.getAllSubDirNames().contains(dirTokens[i])){
                        //do nothing, just load the subDir for next iteration
                        subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
                    }else{
                        //subDir contains many other directories but not dirTokens[i] so add and load it
                        subDir.addSubDirectoryInSubDir(dirTokens[i], new SubDirectory(dirTokens[i], PERMISSION, new Date().getTime(), UUID.randomUUID().toString().substring(0,12), 0000));
                        subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
                    }
                }else{
                    //subDir doesnt have any directory.
                    // add dirTokens[i] as a subDirectory and load.
                    subDir.addSubDirectoryInSubDir(dirTokens[i], new SubDirectory(dirTokens[i], PERMISSION, new Date().getTime(), UUID.randomUUID().toString().substring(0,12), 0000));
                    subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
                }
            }
        }

        //now check if the directory exists
        return dirExists(dir);
    }

    //remove a directory and its contents from directory
    //if an input is /A/B/C Or /A/B/C/ then we remove /C
    //and everything in it.
    public boolean removeDirectory(String dir){
        //check if the directory is "/" then cannot delete it
        if(dir.equals("/")){return false; }

        //check if dir first char is "/"
        if(dir.charAt(0)!='/'){return false;}

        //check if directory exists to begin with
        if(!dirExists(dir)){ return false; }

        //remove the last "/" symbol from dir if it exists
        //note: unnecessary
        if(dir.charAt(dir.length()-1)=='/'){
            dir = dir.substring(0,dir.length()-1);
        }

        //parse the directory by tokens
        String[] dirTokens = dir.split("/");

        //check if the dirTokens has at least one entry
        SubDirectory subDir;
        if(dirTokens.length!=0){
            //get the first subDir in root
            subDir = rootDir.getSubDirectoryFromRoot(dirTokens[0]);
        }else{
            //for some reason dirToken length is 0, this shouldnt be.
            return false;
        }

        //iterate the following directories and load them
        if(dirTokens.length>1) {
            for (int i = 1; i < dirTokens.length-1; i++) {
                subDir = subDir.getSubDirectoryFromSubDir(dirTokens[i]);
            }
        }

        //now delete the dirToken[last] element from subDir
        subDir.removeSubDirectoryFromSubDir(dirTokens[dirTokens.length-1]);

        //check if directory still exists
        if(dirExists(dir)){return false;}
        else{return true;}
    }




    //=============================================================================================

    //static class for root directory
    private static class Root{
        private String thisDirName;
        private String[] permissionList;
        private long creationTime;
        private String uniqueID;
        private long dirSeqNum;
        private Map<String, SubDirectory> listOfSubdirectories;  //subDirName , subDir
        private Map<String, FileMetadata> listOfFiles;  //filename, fileMetadata

        public Root(){
            this.thisDirName = "/";
            this.permissionList = PERMISSION;
            this.creationTime = new Date().getTime();
            this.uniqueID = UUID.randomUUID().toString().substring(0,12);
            this.dirSeqNum = -1;
        }


        //adds a subDir inside root Dir
        public void addSubDirectoryInRoot(String newSubDirName, SubDirectory newSubDir){
            if(this.listOfSubdirectories==null){this.listOfSubdirectories = new HashMap<>();}
            this.listOfSubdirectories.put(newSubDirName, newSubDir);
        }

        //removes a subDir inside root Dir
        public void removeSubDirectoryFromRoot(String SubDirName){
            this.listOfSubdirectories.remove(SubDirName);
            if(this.listOfSubdirectories.size()==0){
                this.listOfSubdirectories = null;
            }
        }

        //get a subDirectory from root dir
        public SubDirectory getSubDirectoryFromRoot(String subDirName){
            return listOfSubdirectories.get(subDirName);
        }

        //adds a file metadata inside root
        public void addFileInRoot(String filename, FileMetadata fileMetadata){
            if(this.listOfFiles==null){this.listOfFiles = new HashMap<>();}
            this.listOfFiles.put(filename, fileMetadata);
        }

        //removes a file metadata from root
        public void removeFileFromRoot(String filename){
            this.listOfFiles.remove(filename);
            if(this.listOfFiles.size()==0){
                this.listOfFiles = null;
            }
        }

        //get a file metadata from root
        public FileMetadata getFileMetadataFromRoot(String filename){
            return this.listOfFiles.get(filename);

        }

        //get subDirectory list
        public Set<String> getAllSubDirNames(){
            return listOfSubdirectories.keySet();
        }

        //get file list
        public Set<String> getAllFileNames(){
            return listOfFiles.keySet();
        }

    }


    //=============================================================================================

    //class for subDirectories
    private class SubDirectory{
        private String thisDirName;
        private String[] permissionList;
        private long creationTime;
        private String uniqueID;
        private long dirSeqNum;
        private Map<String, SubDirectory> listOfSubdirectories;  //subDirName, subDir
        private Map<String, FileMetadata> listOfFiles;  //filename, fileMetadata

        public SubDirectory(String thisdirname, String[] permList, long creationtime, String uniqueID, long dirSeqNum){
            this.thisDirName = thisdirname;
            this.permissionList = permList;
            this.creationTime = creationtime;
            this.uniqueID = uniqueID;
            this.dirSeqNum = dirSeqNum;
        }

        //adds a subDir inside another subDir
        public void addSubDirectoryInSubDir(String newSubDirName, SubDirectory newSubDir){
            if(this.listOfSubdirectories==null){this.listOfSubdirectories = new HashMap<>();}
            this.listOfSubdirectories.put(newSubDirName, newSubDir);
        }

        //removes a subDir inside another subDir
        public void removeSubDirectoryFromSubDir(String SubDirName){
            this.listOfSubdirectories.remove(SubDirName);
            if(this.listOfSubdirectories.size()==0){
                this.listOfSubdirectories = null;
            }
        }

        //get a subDirectory
        public SubDirectory getSubDirectoryFromSubDir(String subDirName){
            return listOfSubdirectories.get(subDirName);
        }

        //adds a file metadata inside a subDIr
        public void addFileInSubDirectory(String filename, FileMetadata fileMetadata){
            if(this.listOfFiles==null){this.listOfFiles = new HashMap<>();}
            this.listOfFiles.put(filename, fileMetadata);
        }

        //removes a file metadata from a subDir
        public void removeFileFromSubDir(String filename){
            this.listOfFiles.remove(filename);
            if(this.listOfFiles.size()==0){
                this.listOfFiles = null;
            }
        }

        //get a file metadata from subDir
        public FileMetadata getFileMetadataFromSubDir(String filename){
            return this.listOfFiles.get(filename);

        }

        //get subDirectory list
        public Set<String> getAllSubDirNames(){
            return listOfSubdirectories.keySet();
        }

        //get file list
        public Set<String> getAllFileNames(){
            return listOfFiles.keySet();
        }

    }


}
