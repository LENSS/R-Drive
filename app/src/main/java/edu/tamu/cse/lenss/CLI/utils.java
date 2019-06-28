package edu.tamu.cse.lenss.CLI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNSConstants;


//this class contains utility functions for commands passed through cli_processor.java class
public class utils {


    //this function checks if the user inputted permission list has valid entries.
    //if "world", then other entries doesnt matter..everyone who is running MDFS will hav r/w/d permission.
    //if "owner", then other entries doesnt matter, only owner of the file will have r/w/d permission.
    //else, they listed nodes will have permission.
    //if no other entries are valid the only permission is OWNER
    public static String[] checkPermittedNodes(String[] perm){
        //resultant list
        List<String> permittedList = new ArrayList<>();

        List<String> permList = Arrays.asList(perm);
        Set<String> permSet = new HashSet<>(permList);

        //if permission list contains owner or world, then other permissions doesnt matter
        if(permSet.contains("OWNER") || permSet.contains("owner")){
            permittedList.add("OWNER");
            return permittedList.toArray(new String[permittedList.size()]);
        }else if(permSet.contains("WORLD") || permSet.contains("world")){
            permittedList.add("WORLD");
            return permittedList.toArray(new String[permittedList.size()]);
        }

        //if permittedSet doesnt contain WORLD or OWNER, then the only thing it might have are GUIDs or GROUP:<group_name>
        for(int i=0; i< perm.length; i++){
            if (!perm[i].equals("") && perm[i].toLowerCase().contains("group:") && perm[i].toLowerCase().substring(0,6).equals("group:")) {
                permittedList.add(perm[i].toUpperCase());  //note: add with GROUP: tag | do uppercase
            } else if (!perm[i].equals("") && perm[i].toLowerCase().length() == GNSConstants.GUID_LENGTH) {
                permittedList.add(perm[i]);  //note: dont lowercase or uppercase GUIDs, keep them as is
            }else{
                //entry is either empty string, or is not 40 bytes guid, or not Group name and some sort of garbage entry
                continue;
            }
        }

        //note: all entries in this list are upper case
        return permittedList.toArray(new String[permittedList.size()]);
    }

}
