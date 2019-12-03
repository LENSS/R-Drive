package edu.tamu.lenss.MDFS.Commands.ls;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.LScommand;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class lsUtils {


    //takes an unmodified ls result in json string format directly from EdgeKeeper,
    //and returns as plain format.
    //the input ls result can contain ownEdgeDir, neighborEdgeDir, or both.
    public static String jsonToPlainString(String lsResult){


        System.out.println();
        //make result string
        String result = "";

        //check for null
        if(lsResult==null){
            return "-ls command failed.";
        }

        //convert json string into obj
        try {
            JSONObject lsResObj = new JSONObject(lsResult);

            //check success or error json
            if(lsResObj.get(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                //check reply type
                if(lsResObj.get(LScommand.lsReplyType).equals(LScommand.lsReplyForOwnEdge)){

                    //parse ls result for own edge
                    result = result + toStringOwnEdgeDir(lsResult);

                }else if(lsResObj.get(LScommand.lsReplyType).equals(LScommand.lsReplyForNeighborEdge)){

                    //parse ls result for neighborEdgeDir
                    result = result + toStringNeighborEdgeDir(lsResult);


                }else if(lsResObj.get(LScommand.lsReplyType).equals(LScommand.lsReplyForBothOwnAndNeighborEdge)){

                    //parse ls result for own edge
                    result = result + toStringOwnEdgeDir(lsResult);

                    //parse ls result for neighborEdgeDir
                    result = result + toStringNeighborEdgeDir(lsResult);
                }

                //return
                return result;

            }else{

                //return error message
                return lsResObj.getString(RequestTranslator.errorMessage);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return "Could not process -ls request.";
        }
    }


    //helper function to parse neighborEdgeDir.
    //takes an unmodified ls reply from edgekeeper, and parses out only the
    //neighborEdgeDir portion, and returns as one string for visual purpose only.
    private static String toStringNeighborEdgeDir(String lsResult) {

        String result = "";

        try {

            //convert string into JSON
            JSONObject lsResObj = new JSONObject(lsResult);

            //get neighborsEdgeDirs and check if its valid
            JSONObject neighborsEdgeDirs = new JSONObject(lsResObj.getString(LScommand.lsReplyForNeighborEdge));
            if (neighborsEdgeDirs.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                //remove success tag
                neighborsEdgeDirs.remove(RequestTranslator.resultField);

                //get all keys, aka masterguids
                Iterator<String> masterguids = neighborsEdgeDirs.keys();

                //for each master guid
                while (masterguids.hasNext()) {

                    //get each master guid
                    String master = masterguids.next();

                    //get ls result for this master.
                    //reminder: this ls result is for one particular directory.
                    JSONObject otherEdgeDir = new JSONObject(neighborsEdgeDirs.getString(master));

                    //get FILES from otherEdgeDir
                    JSONObject FILES = new JSONObject(otherEdgeDir.getString(LScommand.FILES));
                    int fileCount = Integer.parseInt(FILES.getString(LScommand.COUNT));
                    for (int i = 0; i < fileCount; i++) {
                        result = result + FILES.getString(Integer.toString(i)) + "(" + master + ")" + "   ";
                    }

                    //get FOLDERS from otherEdgeDir
                    JSONObject FOLDERS = new JSONObject(otherEdgeDir.getString(LScommand.FOLDERS));
                    int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                    for (int i = 0; i < folderCount; i++) {
                        result = result + File.separator + FOLDERS.getString(Integer.toString(i)) + "(" + master + ")" + "   ";
                    }

                }

            }
        }catch (JSONException e){e.printStackTrace();}

        return result;
    }

    //helper function for parsing ownEdgeDir
    //takes an unmodified ls reply from edgekeeper, and parses out only the
    //ownEdgeDir portion, and returns as one string for visual purpose only.
    private static String toStringOwnEdgeDir(String lsResult){

        String result = "";

        try {
            //convert string into JSON
            JSONObject lsResObj = new JSONObject(lsResult);

            //get ownEdgeDir obj and check if its valid.
            //ownEdgeDir contains ls result for own edge.
            //reminder: this ls result is for one particular directory.
            JSONObject ownEdgeDir = new JSONObject(lsResObj.getString(LScommand.lsReplyForOwnEdge));
            if (ownEdgeDir.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                //get FILES from ownEdgeDir
                JSONObject FILES = new JSONObject(ownEdgeDir.getString(LScommand.FILES));
                int fileCount = Integer.parseInt(FILES.getString(LScommand.COUNT));
                for (int i = 0; i < fileCount; i++) {
                    result = result + FILES.getString(Integer.toString(i)) + "   ";
                }

                //get FOLDERS from ownEdgeDir
                JSONObject FOLDERS = new JSONObject(ownEdgeDir.getString(LScommand.FOLDERS));
                int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                for (int i = 0; i < folderCount; i++) {
                    result = result + File.separator + FOLDERS.getString(Integer.toString(i)) + "   ";
                }

            }
        }catch(JSONException e ){ e.printStackTrace();}

        return result;
    }

    //takes unmodified ls result from EdgeKeeper for neighborEdgeDir,
    // and returns a list of masters.
    //list can be empty.
    public static List<String> getListOfMastersFromNeighborEdgeDirStr(String lsResult){

        List<String> neighborMasters = new ArrayList<>();
        try{

            //convert string into JSON
            JSONObject lsResObj = new JSONObject(lsResult);

            //get neighborsEdgeDirs and check if its valid
            JSONObject neighborsEdgeDirs = new JSONObject(lsResObj.getString(LScommand.lsReplyForNeighborEdge));
            if (neighborsEdgeDirs.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                //remove success tag
                neighborsEdgeDirs.remove(RequestTranslator.resultField);

                //get all keys, aka masterguids
                Iterator<String> masterguids = neighborsEdgeDirs.keys();

                //for each master guid
                while (masterguids.hasNext()) {

                    //get each master guid
                    String master = masterguids.next();

                    //add into list
                    neighborMasters.add(master);

                }
            }

        }catch (JSONException e){
            e.printStackTrace();
        }

        return neighborMasters;
    }

    //takes a ls result in json format,
    //that contains own edge dir info,
    //and returns all tokens as a list.
    public static List<String> jsonToList(String lsResult){

        //check for null
        if(lsResult==null){
            return new ArrayList<>();
        }


        try {

            //convert string into JSON
            JSONObject lsResObj = new JSONObject(lsResult);

            //check success or error json
            if(lsResObj.get(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                //make result list
                List<String> result = new ArrayList<>();

                //get ownEdgeDir obj and check if its valid.
                //ownEdgeDir contains ls result for own edge.
                //reminder: this ls result is for one particular directory.
                JSONObject ownEdgeDir = new JSONObject(lsResObj.getString(LScommand.lsReplyForOwnEdge));
                if(ownEdgeDir.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    //get FILES from ownEdgeDir
                    JSONObject FILES = new JSONObject(ownEdgeDir.getString(LScommand.FILES));
                    int fileCount = Integer.parseInt(FILES.getString(LScommand.COUNT));
                    for(int i=0; i< fileCount; i++){ result.add(FILES.getString(Integer.toString(i)));}

                    //get FOLDERS from ownEdgeDir
                    JSONObject FOLDERS = new JSONObject(ownEdgeDir.getString(LScommand.FOLDERS));
                    int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                    for(int i=0; i< folderCount; i++){ result.add(File.separator+  FOLDERS.getString(Integer.toString(i)));}

                }

                return result;

            }else{

                return new ArrayList<>();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }



}
