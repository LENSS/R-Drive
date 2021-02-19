package edu.tamu.lenss.MDFS.Commands.ls;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.MetaDataHandler;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.LScommand;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;


//this class is all about parsing strings.
//this class is used for parsing LS command resumts from EdgeKeeper,
//and parse them.
//any GUID or NAME is this class, relates to the master of the edge(either own edge or neighbor edge)
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
    //takes an unmodified ls reply from edgekeeper for lsRequestForNeighborEdge, or lsRequestForBothOwnAndNeighborEdge
    // and parses out only the neighborEdgeDir portion,
    // and returns as one string for visual purpose only.
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

                    //get one master guid
                    String master = masterguids.next();

                    //get ls result for this master.
                    //reminder: this ls result is for one particular directory.
                    JSONObject otherEdgeDir = new JSONObject(neighborsEdgeDirs.getString(master));


                    //get mastername
                    String masterName = otherEdgeDir.getString(MetaDataHandler.MASTERNAME);

                    //get FILES from otherEdgeDir
                    JSONObject FILES = new JSONObject(otherEdgeDir.getString(LScommand.FILES));
                    int fileCount = Integer.parseInt(FILES.getString(LScommand.COUNT));
                    for (int i = 0; i < fileCount; i++) {
                        result = result + FILES.getString(Integer.toString(i)) + "(" + masterName + ")" + "   ";
                    }

                    //get FOLDERS from otherEdgeDir
                    JSONObject FOLDERS = new JSONObject(otherEdgeDir.getString(LScommand.FOLDERS));
                    int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                    for (int i = 0; i < folderCount; i++) {
                        result = result + File.separator + FOLDERS.getString(Integer.toString(i)) + "(" + masterName + ")" + "   ";
                    }

                }

            }
        }catch (JSONException e){e.printStackTrace();}

        return result;
    }

    //helper function for parsing ownEdgeDir
    //takes an unmodified ls reply from edgekeeper for lsRequestForOwnEdge, or lsRequestForBothOwnAndNeighborEdge
    // and parses out only the ownEdgeDir portion,
    // and returns as one string for visual purpose only.
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

    //takes unmodified ls result from EdgeKeeper for lsRequestForNeighborEdge,
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

    //takes a ls result from edgekeeper for lsRequestForOwnEdge
    //and returns all tokens as a list.
    //lsReplyForOwnEdge is used for doing ls for a
    //particular directory for own edge directory.
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

    //takes a ls results from edgekeeper for lsRequestForAllDirectoryiesOfAllNeighborEdges,
    //and return a list of neighbor masters guids.
    //returns empty list if no masters available().
    public static List<String> getListOfMastersGUIDsFromAllNeighborEdgeDirStr(String allNeighborEdgeDirsCache){

        //make a list
        List<String> masters = new ArrayList<>();

        try{

           //convert string into json
            JSONObject allneighDirsObj = new JSONObject(allNeighborEdgeDirsCache);

            //check null
            if(allneighDirsObj!=null){

                //check success or error
                if(allneighDirsObj.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    //get reply type
                    String replyType =  allneighDirsObj.getString(LScommand.lsReplyType);

                    //get DirsStr
                    String DirsStr =  allneighDirsObj.getString(replyType);

                    //get DirsObj
                    JSONObject DirsObj = new JSONObject(DirsStr);

                    //check success tag
                    if(DirsObj.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                        //remove success tag
                        DirsObj.remove(RequestTranslator.resultField);

                        //get all keys, aka masterguids
                        Iterator<String> masterguids = DirsObj.keys();

                        //for each master guid
                        while (masterguids.hasNext()) {

                            //get each master guid
                            String master = masterguids.next();

                            //add into list
                            masters.add(master);

                        }
                    }

                }

            }

        }catch (JSONException e){
            e.printStackTrace();
        }

        System.out.println("2020: " + masters);

        return masters;
    }

    //takes a ls results from edgekeeper for lsRequestForAllDirectoryiesOfAllNeighborEdges,
    //and return a list of neighbor masters names.
    //returns empty list if no masters available.
    public static List<String> getListOfMastersNAMEsFromAllNeighborEdgeDirStr(String allNeighborEdgeDirsCache){

        //make a resultant list
        List<String> mastersNAMEs = new ArrayList<>();

        try{

            //first get the list of all unique master's GUIDs
            List<String> mastersGUIDs = getListOfMastersGUIDsFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);

            //check empty
            if(mastersGUIDs!= null && mastersGUIDs.size()>0){

                for(String master: mastersGUIDs){

                    //fetch dir object for this particular master
                    JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(master, allNeighborEdgeDirsCache);

                    //check null
                    if (particularMasterDirsObject != null) {

                        //get master name
                        String masterName = particularMasterDirsObject.getString(MetaDataHandler.MASTERNAME);

                        //add master name to result list
                        mastersNAMEs.add(masterName);

                    }
                }
            }



        }catch (Exception e){
            e.printStackTrace();
        }

        return mastersNAMEs;
    }

    //takes one master name and returns its GUID
    public static String nameToGUID(String name, String allNeighborEdgeDirsCache){

        //first get all the masters GUIDs
        List<String> mastersGUIDs = getListOfMastersGUIDsFromAllNeighborEdgeDirStr(allNeighborEdgeDirsCache);

        for(String guid: mastersGUIDs){
            String temp = guidTOname(guid, allNeighborEdgeDirsCache);

            if(temp!=null && temp.equals(name)){
                return guid;
            }
        }

        return null;
    }

    //takes one master guid and returns its name
    //may return null.
    public static String guidTOname(String guid, String allNeighborEdgeDirsCache){

        //fetch dir object for this particular master
        JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(guid, allNeighborEdgeDirsCache);

        //get the name
        try {
            String name = particularMasterDirsObject.getString(MetaDataHandler.MASTERNAME);
            return name;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    //takes a list of master guids and returns their names.
    //strip out ".distressnet.org" if boolean is true.
    //the list may be empty, if no conversion happens.
    public static List<String> masterGUIDsToNAMEs(List<String> mastersGUIDs, String allNeighborEdgeDirsCache, boolean stripped){

        //make a resultant list
        List<String> mastersNAMEs = new ArrayList<>();

        try{

            //check empty
            if(mastersGUIDs!= null && mastersGUIDs.size()>0){

                for(String master: mastersGUIDs){

                    //fetch dir object for this particular master
                    JSONObject particularMasterDirsObject = lsUtils.parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(master, allNeighborEdgeDirsCache);

                    //check null
                    if (particularMasterDirsObject != null) {

                        //parse master name
                        String masterName = particularMasterDirsObject.getString(MetaDataHandler.MASTERNAME);

                        //add master name to result list
                        if(stripped){
                            mastersNAMEs.add(masterName.replace(".distressnet.org",""));
                        }else {
                            mastersNAMEs.add(masterName);
                        }
                    }
                }
            }



        }catch (Exception e){
            e.printStackTrace();
        }

        return mastersNAMEs;
    }

    //takes ls result for lsRequestForAllDirectoriesOfAllNeighborEdges,
    //a master GUID and returns a JSONObject for directory structure for
    //this particular master.
    //note: the return JUSONObject contains directory strings as keys,
    //and for each key, there is another json that contains
    // two jsons, those are FILES and FOLDERS json inside it.
    //returns JSONObject or null.
    public static JSONObject parseParticularMasterDirectoryFromAllNeighborEdgeDirStr(String masterguid, String lsResult){
        try{

            //convert string into json
            JSONObject allneighDirsObj = new JSONObject(lsResult);

            //check null
            if(allneighDirsObj!=null){

                //check success or error
                if(allneighDirsObj.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    //get reply type
                    String replyType =  allneighDirsObj.getString(LScommand.lsReplyType);

                    //get DirsStr
                    String DirsStr =  allneighDirsObj.getString(replyType);

                    //get DirsObj
                    JSONObject DirsObj = new JSONObject(DirsStr);

                    //check success tag
                    if(DirsObj.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                        //remove success tag
                        DirsObj.remove(RequestTranslator.resultField);

                        //get particular directory for particular master
                        String particularMasterDirsStr = DirsObj.getString(masterguid);

                        //check null
                        if(particularMasterDirsStr!=null){

                            //convert string into json
                            JSONObject particularMasterDirsObj = new JSONObject(particularMasterDirsStr);

                            //check null
                            if(particularMasterDirsObj!=null){

                                //at this point particularMasterDirsObj contains all keys as directory string,
                                //and for each key, there are FILES and FOLDERS json in it.
                                return  particularMasterDirsObj;

                            }
                        }
                    }

                }

            }



        }catch (JSONException e){
            e.printStackTrace();
        }

        return null;
    }



}
