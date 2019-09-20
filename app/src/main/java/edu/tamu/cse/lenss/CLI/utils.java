package edu.tamu.cse.lenss.CLI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


//this class contains utility functions for commands passed through cli_processor.java class
public class utils {

    ///takes a filename and checks if the filename contains the desired file extension
    public static boolean checkFileExtension(String filename){

        //actual check
        //if(filename.contains(".jpg")  || filename.contains(".mp4") || filename.contains(".txt") || filename.contains(".pdf") || filename.contains(".jar") || filename.contains(".tar")){
        if(filename.contains(".")){
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
    //explicitly for phones is because android host filesystem(sdcard),
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
