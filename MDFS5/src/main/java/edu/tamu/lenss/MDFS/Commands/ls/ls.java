package edu.tamu.lenss.MDFS.Commands.ls;

import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.sat4j.pb.tools.INegator;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.LScommand;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class ls {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ls.class);

    public static String ls(String mdfsDir){

        //log
        logger.log(Level.ALL, "Starting to handle -ls command.");

        try {
            //send ls request
            JSONObject repJSON = EKClient.ls(mdfsDir);

            //check reply
            if (repJSON != null) {

                //check reply success or error
                if (repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)) {

                    //log
                    logger.log(Level.ALL, "Received SUCCESS reply from edgeKeeper for -ls command");

                    //return
                    return repJSON.toString();

                } else {

                    //log
                    logger.log(Level.ALL, "Received FAILED reply from edgeKeeper for -ls command");

                    return RequestTranslator.errorJSON("Received FAILED reply from edgeKeeper for ls command").toString();
                }

            } else {

                //log
                logger.log(Level.DEBUG, "Could not connect to local EdgeKeeper to perform -ls command");

                return RequestTranslator.errorJSON("Could not connect to local EdgeKeeper.").toString();
            }
        }catch (Exception e){

            //log
            logger.log(Level.DEBUG, "Exception happened during performing -ls request", e);
        }

        try {
            return RequestTranslator.errorJSON("-ls command failed due to exception").toString();
        } catch (JSONException e) {
            return null;
        }
    }

    //takes a ls result in json format,
    //that contains both edge and neighbor dir info,
    //and returns as plain format.
    public static String jsonToPlainString(String lsResult){

        //check for null
        if(lsResult==null){
            return "-ls command failed.";
        }

        //convert json string into obj
        try {
            JSONObject lsResObj = new JSONObject(lsResult);

            //check success or error json
            if(lsResObj.get(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                //make result string
                String result = "";

                //get ownEdgeDir obj and check if its valid.
                //ownEdgeDir contains ls result for own edge.
                //reminder: this ls result is for one particular directory.
                JSONObject ownEdgeDir = new JSONObject(lsResObj.getString(LScommand.LSRESULTFOROWNEDGE));
                if(ownEdgeDir.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    //get FILES from ownEdgeDir
                    JSONObject FILES = new JSONObject(ownEdgeDir.getString(LScommand.FILES));
                    int fileCount = Integer.parseInt(FILES.getString(LScommand.COUNT));
                    for(int i=0; i< fileCount; i++){ result = result + FILES.getString(Integer.toString(i)) + "   ";}

                    //get FOLDERS from ownEdgeDir
                    JSONObject FOLDERS = new JSONObject(ownEdgeDir.getString(LScommand.FOLDERS));
                    int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                    for(int i=0; i< folderCount; i++){ result = result + File.separator+  FOLDERS.getString(Integer.toString(i)) + "   ";}

                }

                //get neighborsEdgeDirs and check if its valid
                JSONObject neighborsEdgeDirs = new JSONObject(lsResObj.getString(LScommand.LSRESULTFORNEIGHBOREDGE));
                if(neighborsEdgeDirs.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

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
                        for(int i=0; i< fileCount; i++){ result = result + FILES.getString(Integer.toString(i)) + "(" + master + ")" + "   ";}

                        //get FOLDERS from otherEdgeDir
                        JSONObject FOLDERS = new JSONObject(otherEdgeDir.getString(LScommand.FOLDERS));
                        int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                        for(int i=0; i< folderCount; i++){ result = result + File.separator+  FOLDERS.getString(Integer.toString(i)) + "(" + master + ")"  + "   ";}

                    }

                }

                return result;

            }else{

                return lsResObj.getString(RequestTranslator.errorMessage);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return "Could not process -ls request.";
        }

    }

    //takes a ls result in json format,
    //that contains both edge and neighbor dir info,
    //and returns all tokens as a list.
    public static List<String> jsonToList(String lsResult){

        //check for null
        if(lsResult==null){
            return new ArrayList<>();
        }

        //convert json string into obj
        try {
            JSONObject lsResObj = new JSONObject(lsResult);

            //check success or error json
            if(lsResObj.get(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                //make result list
                List<String> result = new ArrayList<>();

                //get ownEdgeDir obj and check if its valid.
                //ownEdgeDir contains ls result for own edge.
                //reminder: this ls result is for one particular directory.
                JSONObject ownEdgeDir = new JSONObject(lsResObj.getString(LScommand.LSRESULTFOROWNEDGE));
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

                //get neighborsEdgeDirs and check if its valid
                JSONObject neighborsEdgeDirs = new JSONObject(lsResObj.getString(LScommand.LSRESULTFORNEIGHBOREDGE));
                if(neighborsEdgeDirs.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

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
                        for(int i=0; i< fileCount; i++){ result.add(FILES.getString(Integer.toString(i)) + "(" + master + ")");}

                        //get FOLDERS from otherEdgeDir
                        JSONObject FOLDERS = new JSONObject(otherEdgeDir.getString(LScommand.FOLDERS));
                        int folderCount = Integer.parseInt(FOLDERS.getString(LScommand.COUNT));
                        for(int i=0; i< folderCount; i++){ result.add(File.separator+  FOLDERS.getString(Integer.toString(i)) + "(" + master + ")");}

                    }

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
