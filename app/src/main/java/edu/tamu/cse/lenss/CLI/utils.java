package edu.tamu.cse.lenss.CLI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


//this class contains utility functions for commands passed through cli_processor.java class
public class utils {


    //this function checks if the user inputted permission list has valid entries.
    //the input array is expected to have four types of entry "WORLD", "OWNER", GUID, or GROUP:<group_name>.
    //if "WORLD", then other entries doesnt matter..everyone who is running MDFS will hav r/w/d permission.
    //if "OWNER", then other entries doesnt matter, only owner of the file will have r/w/d permission.
    //else, the listed nodes will have permission.
    //returned String[] contains either "WORLD" in uppercase, or
    //"OWNER" in uppercase, or
    //GROUP:<group_name> , containing GROUP: tag in uppercase and <group_name> as passed by user
    //GUIDs which can be either uppercase or lowercase, as got from GNS.
    public static String[] checkPermittedNodes(String[] perm){
        //resultant list
        List<String> permittedList = new ArrayList<>();

        List<String> permList = Arrays.asList(perm);
        Set<String> permSet = new HashSet<>(permList);

        //if permission list contains owner or world, then other permissions dont matter
        if(permSet.contains("OWNER") || permSet.contains("owner")){
            permittedList.add("OWNER");
            return permittedList.toArray(new String[permittedList.size()]);
        }else if(permSet.contains("WORLD") || permSet.contains("world")){
            permittedList.add("WORLD");
            return permittedList.toArray(new String[permittedList.size()]);
        }

        //if permittedSet doesnt contain WORLD or OWNER, then the only thing it might have are GUIDs or GROUP:<group_name>
        //we add group with GROUP: tag along with <group_name>, the GROUP: Tag is uppercase.
        //we add GUIDs as is , can be either uppercase or lowercase.
        for(int i=0; i< perm.length; i++){
            if( (!perm[i].equals("")) && (perm[i].toLowerCase().equals("group:")) && (perm[i].length() == "group:".length())){
                //user inputted only GROUP: tag without any <group_name> so ignore
                continue;
            }else if (!perm[i].equals("") && perm[i].toLowerCase().contains("group:") && perm[i].toLowerCase().substring(0,6).equals("group:")) {
                String groupVal = "GROUP:" + perm[i].substring("GROUP:".length());  //GROUP: + <group_name>
                permittedList.add(groupVal);
            }else if (!perm[i].equals("") && perm[i].length() == CLIConstants.GUID_LENGTH) {
                permittedList.add(perm[i]);  //GUID, dont change case
            }else{
                //entry is either empty string, or is not 40 bytes guid, or some sort of garbage entry
                continue;
            }
        }

        //note: all entries in this list are upper case
        return permittedList.toArray(new String[permittedList.size()]);
    }

    //this is a dummy checkPermittedNodes() function.
    //the only permission it returns is WORLD.
    public static String[] checkPermittedNodes(String world){
        String[] ret = {"WORLD"};
        return ret;
    }


    ///takes a filename and checks if the filename contains the desired file extension
    public static boolean checkFileExtension(String filename){

        //actual check
        if(filename.contains(".jpg")  || filename.contains(".mp4") || filename.contains(".txt") || filename.contains(".pdf") || filename.contains(".jar") || filename.contains(".tar")){
            return true;
        }else{
            return false;
        }
    }

    //check if a MDFS dir is valid
    //returns "OK" if correct,
    //else returns the reason why the dir is invalid.
    public static String isValidMDFSDir(String dir){

        //check if dir is empty string
        if(dir.equals("")){return "dir is empty.";}

        //check if dir is just root
        if(dir.equals("/")){return "OK";}

        //check if the dir starts with "/"
        if(dir.charAt(0)!='/'){ return "dir must start with root /";}

        //check for more than one subsequent slash
        if(dir.length()>1){
            for(int i=0; i< dir.length()-1; i++){
                if(dir.charAt(i)=='/' && (dir.charAt(i)==dir.charAt(i+1))){
                    return "dir contains more that one subsequent slashes /";
                }
            }
        }

        return "OK";
    }

    //check if local dir is valid in android linux system
    //returns "OK" if correct,
    //else returns he reason the local dir is not valid.
    //note: the reason this function checks linux directory,
    //explicitly for phones is because android host filesystem,
    //is a bit different that the usual linux(like ubuntu) file system.
    public static String isValidLocalDirInAndroidPhone(String dir){

        //check if dir length is enough
        if(dir.length()<"/storage/emulated/0/".length()){ return "directory path must start with /storage/emulated/0/";}

        //check if dir starts with /storage/emulated/0/
        else if(!dir.substring(0, new StringBuilder("/storage/emulated/0/").toString().length()).equals("/storage/emulated/0/")){
            return "directory path must start with /storage/emulated/0/";
        }

        else{ return "OK"; }
    }

    //check if the local dir is valid in typical linux system
    public static String isValidLocalDirInLinux(String dir){

        //check if the first char is root
        if(dir.charAt(0)!='/'){
            return "dir must start with root.";
        }

        return "OK";
    }

}
