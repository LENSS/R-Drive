package edu.tamu.lenss.mdfs.RSock.network;


import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import example.Interface;
import example.ReceivedFile;

//this class is used for receiving packets that contains file information
// to delete from a  device completely.
public class RsockReceiveForFileDeletion implements Runnable{


    private static boolean isTerminated = false;

    //constructor
    public RsockReceiveForFileDeletion(){

    }

    @Override
    public void run() {
        if(RSockConstants.intrfc_deletion==null) {
            RSockConstants.intrfc_deletion = Interface.getInstance(EdgeKeeper.ownGUID, RSockConstants.intrfc_deletion_appid, 999);
        }

        ReceivedFile rcvdfile = null;
        String[] tokens = null;
        String fileInformation;
        String fileName;
        long fileID;
        while(!isTerminated){

            //blocking on receiving through rsock
            try {
                rcvdfile = RSockConstants.intrfc_deletion.receive(100, "default"); }
            catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(rcvdfile!=null) {

                //get file information and parse
                fileInformation = new String(rcvdfile.getFileArray());
                tokens = fileInformation.split(RSockConstants.deletion_tag);
                tokens = IOUtilities.delEmptyStr(tokens);
                fileName = tokens[0];
                fileID = Long.parseLong(tokens[1]);

                //trigger delete
                try {
                    ServiceHelper.getInstance().getDirectory().deleteFile(fileID, fileName);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        }

        //execution only comes here when the above while loop is broken.
        //above while loop is only broken when mdfs is closing.
        //came out of while loop, now close the rsock client library object.
        RSockConstants.intrfc_deletion.close();
        RSockConstants.intrfc_deletion = null;
    }

    public static void stop(){
        isTerminated = true;
    }

}