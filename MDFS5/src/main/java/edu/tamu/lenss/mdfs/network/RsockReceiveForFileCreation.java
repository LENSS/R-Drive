package edu.tamu.lenss.mdfs.network;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.MDFSBlockCreator;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.MDFSRsockBlockCreator;
import example.Interface;
import example.ReceivedFile;

import static java.lang.Thread.sleep;


public class RsockReceiveForFileCreation implements Runnable{

    private static final String TAG = MDFSBlockCreator.class.getSimpleName();

    private boolean isTerminated;

    //constructor
    public RsockReceiveForFileCreation(){
        isTerminated = false;
    }


    @Override
    public void run() {
        if(RSockConstants.intrfc_creation==null) {
            RSockConstants.intrfc_creation = Interface.getInstance(GNS.getGNSInstance().getOwnGuid(), RSockConstants.intrfc_creation_appid, 999);
        }
        System.out.println("Rsock receiver thread is running...");
        ReceivedFile rcvdfile = null;
        while(!isTerminated){
            try {
                //blocking on receving through rsock
                try { rcvdfile = RSockConstants.intrfc_creation.receive(0, "default"); } catch (InterruptedException e) {e.printStackTrace(); }
                if(rcvdfile!=null) {
                    System.out.println("new incoming rsock");

                    System.out.println("print: " + Arrays.toString(rcvdfile.getFileArray()));

                    //convert byteArray into MDFSRsockBlockCreator object
                    ByteArrayInputStream bis = new ByteArrayInputStream(rcvdfile.getFileArray());
                    ObjectInputStream ois = new ObjectInputStream(bis);
                    MDFSRsockBlockCreator mdfsrsockblock = (MDFSRsockBlockCreator) ois.readObject();
                    bis.close();
                    ois.close();

                    //convert MDFSRsockBlockCreator obj into FragmentTransferInfo (header), File (fileFrag) etc.
                    FragmentTransferInfo header = (FragmentTransferInfo) mdfsrsockblock.fragTransInfoHeader;
                    byte[] byteArray = (byte[])mdfsrsockblock.fileFrag;
                    long fileFragLength = (long) mdfsrsockblock.fileFragLength;
                    String fileName = (String) mdfsrsockblock.fileName;
                    long fileCreatedTime = (long) mdfsrsockblock.fileCreatedTime;
                    String destGUID = (String) mdfsrsockblock.destGUID;

                    //now save the fileFrag
                    saveTheFileFrag(header, byteArray, destGUID);

                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (NullPointerException | IOException | ClassNotFoundException e) {  ////InterruptedException
                e.printStackTrace();
            }
        }

    }

    //this function basically copied from FragExchangeHelper.java receiveBlockFragment() function
    private void saveTheFileFrag(FragmentTransferInfo header, byte[] byteArray, String destGUID) {  //note: destGUID was never used
        //create file
        File tmp0 = null;
        try{
            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(header.getFileName(),header.getCreatedTime(), header.getBlockIndex()));

            if(!tmp0.exists()){
                if(!tmp0.mkdirs()){
                    Logger.e(TAG, "Fail to create block directory for " + header.getFileName());
                    return;
                }
            }

            //write on file
            tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(header.getFileName(), header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex()));
            FileOutputStream outputStream = new FileOutputStream(tmp0);
            outputStream.write(byteArray);
            outputStream.flush();
            outputStream.close();

            //update own local directory data
            ServiceHelper.getInstance().getDirectory().addBlockFragment(header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex());

            //todo: make a connection to the edgekeeper and send data to edgeKeeper


        }catch(IOException | NullPointerException | SecurityException e){
            e.printStackTrace();
        }
    }

}
