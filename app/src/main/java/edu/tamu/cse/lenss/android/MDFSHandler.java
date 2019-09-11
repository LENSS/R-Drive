package edu.tamu.cse.lenss.android;


import edu.tamu.cse.lenss.CLI.cli_processor;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;


//this is the mdfs handler that starts the command line interface(cli) and the MDFS library.

public class MDFSHandler extends Thread {

    public cli_processor cli;

    public MDFSHandler() {}

    @Override
    public void run(){
        //note: MDFS must start before CLI
        startMDFS();
        startCLI();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        ServiceHelper.releaseService();
        cli.interrupt();
    }

    public void startMDFS(){

        //init serviceHelper
        ServiceHelper.getInstance();
        try {
            //Set encryption key
            byte[] encryptKey = new byte[32];
            int[] keyValues = {121, 108, 85, 100, -17, 52, 31, 65, -106, 82, 116, -94, -71, 50, -80, -90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int index = 0;
            for(int i: keyValues){encryptKey[index] = (byte)i; index++;}
            ServiceHelper.getInstance().setEncryptKey(encryptKey);

        } catch (NullPointerException  e) {
            e.printStackTrace();
        }

    }


    private void startCLI() {
        cli = new cli_processor();
        cli.start();
    }




}
