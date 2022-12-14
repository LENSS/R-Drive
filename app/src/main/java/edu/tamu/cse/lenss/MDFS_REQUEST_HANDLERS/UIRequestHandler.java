package edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS;

import android.content.Context;

import edu.tamu.lenss.MDFS.Constants;

//this class only takes requests from phone's UI.
public class UIRequestHandler{

    String command;
    Context context;

    public UIRequestHandler(String command, Context context){
        this.command = command;
        this.context = context;
    }


    public String run(){

        String ret = edu.tamu.cse.lenss.MDFS_REQUEST_HANDLERS.ProcessOneRequest.processRequest(Constants.NON_CLI_CLIENT, command);
        return ret;
    }



}
