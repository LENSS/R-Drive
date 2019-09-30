package edu.tamu.lenss.mdfs.Commands.ls;

import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class ls {
    public static String ls(String mdfsDir){

        //send ls request
        JSONObject repJSON = EKClient.ls(mdfsDir);

        //check reply
        if(repJSON!=null){

            //check reply success or error
            try {
                if(repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){
                    return repJSON.getString(RequestTranslator.MDFSLsReply);
                }else{
                    return "-ls failed! " + repJSON.getString(RequestTranslator.messageField);
                }
            } catch (JSONException e) {
                return "Json exception.";
            }
        }else{
            return "Could not connect to local EdgeKeeper.";
        }
    }
}
