package edu.tamu.cse.lenss.MDFS5.Commands.mkdir;

import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.MDFS5.Constants;
import edu.tamu.cse.lenss.MDFS5.EdgeKeeper.EdgeKeeper;
import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class mkdir {

    public static String mkdir(String mdfsDir){

        //make json object
        JSONObject reqJSON = new JSONObject();

        //send mkdir request
        JSONObject repJSON = EKClient.mkdir(mdfsDir, EdgeKeeper.ownGUID, Constants.metadataIsGlobal);

        //check reply
        if(repJSON!=null){

            //check reply success or error
            try {
                if(repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    return "-mkdir success!";
                }else{

                    return "-mkdir failed! " + repJSON.getString(RequestTranslator.messageField);
                }
            } catch (JSONException e) {
                return "Json exception.";
            }
        }else{
            return "Could not connect to local EdgeKeeper.";
        }
    }
}
