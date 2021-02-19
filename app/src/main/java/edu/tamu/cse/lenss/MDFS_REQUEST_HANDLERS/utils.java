package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;


import android.os.Environment;

import java.io.File;

//this class contains utility functions for commands passed through cli_processor.java class
public class utils {

    ///takes a filename and checks if the filename contains the desired file extension
    public static boolean checkFileExtension(String filename){

        //actual check
        //if(filename.contains(".jpg")  || filename.contains(".mp4") || filename.contains(".txt") || filename.contains(".pdf") || filename.contains(".jar") || filename.contains(".tar")  || filename.contains(".iso")){
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
    public static String isValidLocalDirInAndroidPhone(String dir){

        //check if dir length is at least /storage/emulated/0/
        if(dir.length()< (Environment.getExternalStorageDirectory().toString() + File.separator).length()){ return "directory path must start with " + Environment.getExternalStorageDirectory().toString() + File.separator;}

        //check if dir starts with /storage/emulated/0/
        else if(!dir.substring(0, new StringBuilder(Environment.getExternalStorageDirectory().toString() + File.separator).toString().length()).equals(Environment.getExternalStorageDirectory().toString() + File.separator)){
            return "directory path must start with " + Environment.getExternalStorageDirectory().toString() + File.separator;
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
