package edu.tamu.lenss.mdfs.Commands.ls;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;
import edu.tamu.lenss.mdfs.Commands.help.help;
import edu.tamu.lenss.mdfs.RSock.network.RsockReceiveForFileDeletion;

public class ls {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ls.class);

    public static String ls(String mdfsDir){

        //log
        logger.log(Level.ALL, "Starting to handle -ls command.");

        //send ls request
        JSONObject repJSON = EKClient.ls(mdfsDir);

        //check reply
        if(repJSON!=null){

            //check reply success or error
            try {
                if(repJSON.getString(RequestTranslator.resultField).equals(RequestTranslator.successMessage)){

                    //log
                    logger.log(Level.ALL, "Received SUCCESS reply from edgeKeeper for ls command");

                    return repJSON.getString(RequestTranslator.MDFSLsReply);
                }else{

                    //log
                    logger.log(Level.ALL, "Received FAILED reply from edgeKeeper for ls command");

                    return "-ls failed! " + repJSON.getString(RequestTranslator.messageField);
                }
            } catch (JSONException e) {
                return "Json exception.";
            }
        }else{

            //log
            logger.log(Level.DEBUG, "Could not connect to local EdgeKeeper to perform ls command");

            return "Could not connect to local EdgeKeeper.";
        }
    }
}
