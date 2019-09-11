package edu.tamu.lenss.mdfs.handleCommands.mkdir;

import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;

public class mkdir {

    public static String mkdir(String mdfsDir){

        //make json object
        JSONObject reqJSON = new JSONObject();

        //send mkdir request
        JSONObject repJSON = EKClient.mkdir(mdfsDir, GNS.ownGUID, Constants.isGlobal);

        //check reply
        if(repJSON!=null){

            //check reply success or error
            try {
                if(repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    return "-mkdir success!";
                }else{

                    return "-mkdir failed! " + repJSON.getString(RequestTranslator.errorMessage);
                }
            } catch (JSONException e) {
                return "Json exception.";
            }
        }else{
            return "Could not connect to local EdgeKeeper.";
        }
    }
}
