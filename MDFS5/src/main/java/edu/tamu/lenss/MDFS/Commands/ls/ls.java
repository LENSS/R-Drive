package edu.tamu.lenss.MDFS.Commands.ls;

import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;

import edu.tamu.cse.lenss.edgeKeeper.client.EKClient;
import edu.tamu.cse.lenss.edgeKeeper.fileMetaData.command.LScommand;
import edu.tamu.cse.lenss.edgeKeeper.server.RequestTranslator;

public class ls {

    //logger
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ls.class);

    public static String ls(String mdfsDir, String lsRequestType){

        //log
        logger.log(Level.ALL, "Starting to handle -ls command.");

        try {
            //send ls request
            JSONObject repJSON = EKClient.ls(mdfsDir, lsRequestType);

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

}
