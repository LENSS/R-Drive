package edu.tamu.lenss.mdfs.RSock.network;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import edu.tamu.lenss.mdfs.EDGEKEEPER.EdgeKeeper;
import edu.tamu.lenss.mdfs.models.MDFSRsockBlockForFileRetrieve;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import example.*;

import static java.lang.Thread.sleep;


//this class is used for block retrieval via rsock.
//this class receives fragment requests and send the fragment to the requestor.
public class RsockReceiveForFileRetrieval implements Runnable {
    
    private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";
    static boolean isRunning = true;

    public RsockReceiveForFileRetrieval() {}

    @Override
    public void run() {

        try {
            //create rsock Interface intrfc
            RSockConstants.intrfc_retrieval = Interface.getInstance(EdgeKeeper.ownGUID, RSockConstants.intrfc_retrieval_appid, 999);

            ReceivedFile receivedFile = null;
            byte[] byteArray = null;
            ByteArrayInputStream bis;
            ObjectInputStream ois;
            while (isRunning) {

                //blocking on receiving through rsock at a particular endpoint
                try { receivedFile = RSockConstants.intrfc_retrieval.receive(100,"hdrRecv");} catch (InterruptedException e) {e.printStackTrace(); }

                //if unblocked, check if received something
                if (receivedFile != null) {

                    //print
                    System.out.println("received file frag which is not null inside RsockReceiveForFileRetrieval");

                    //print byteArray
                    //System.out.println("print: " + Arrays.toString(receivedFile.getFileArray()));

                    //create MDFSRsockBlockRetrieval object from raw byteArray
                    bis = new ByteArrayInputStream(receivedFile.getFileArray());
                    ois = new ObjectInputStream(bis);
                    MDFSRsockBlockForFileRetrieve mdfsrsockblock = (MDFSRsockBlockForFileRetrieve) ois.readObject();
                    bis.close(); ois.close();

                    //parse mdfsrsockblock and get header containing FragmentTransferInfo, destIP etc
                    FragmentTransferInfo header = (FragmentTransferInfo) mdfsrsockblock.fragTransInfoHeader;
                    String destGUID = (String) mdfsrsockblock.destGUID;     //aka own GUID
                    String srcGUID = (String) mdfsrsockblock.srcGUID;       //aka GUID of the node which made the req
                    String fileName = (String) mdfsrsockblock.fileName;
                    long fileId = (long) mdfsrsockblock.fileId;
                    byte blockIdx =(byte)mdfsrsockblock.blockIdx ;
                    byte fragmentIndex = (byte) mdfsrsockblock.fragmentIndex;


                    //get the file fragment
                    File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(header.getFileName(), header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex()));


                    //if fragment was fetched
                    if(tmp0.length()>0){

                        //convert file tmp0 into byteArray
                        byteArray = new byte[(int) tmp0.length()];
                        try {
                            FileInputStream fileInputStream = new FileInputStream(tmp0);
                            fileInputStream.read(byteArray);
                        } catch (FileNotFoundException e) {
                            System.out.println("File Not Found.");
                            e.printStackTrace();
                        }
                        catch (IOException e1) {
                            System.out.println("Error Reading The File.");
                            e1.printStackTrace();
                        }

                        //print the byteArray
                        //System.out.print("print: " + Arrays.toString(byteArray));

                        //now, make MDFSRsockBlockRetrieval object with the file fragment byteArray
                        mdfsrsockblock = new MDFSRsockBlockForFileRetrieve(byteArray, (int) tmp0.length(), true, EdgeKeeper.ownGUID );

                        //convert mdfsrsockblock object into bytearray and do send
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream ooos = null;
                        try {
                            ooos = new ObjectOutputStream(bos);
                            ooos.writeObject(mdfsrsockblock);
                            ooos.flush();
                            byte [] data = bos.toByteArray();

                            //send the object over rsock and expect no reply
                            String uuid = UUID.randomUUID().toString().substring(0,12);
                            RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing","nothing", srcGUID, 100, "hdrRecv", receivedFile.getReplyEndpoint(), "noReply");
                            System.out.println("fragment has been pushed to the rsock daemon (success)");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        //file frag doesnt exist.
                        //file frag wrong.
                        //file frag invalid.
                        //file frag fetching failed.
                        System.out.println("could not meet fragment retrirval request read file size 0.." + tmp0.getName());

                    }
                }else{
                    //unblocked from rsock client library(roskc java api) due to timeout.
                    //need to check if its time to break out of this while loop.
                }

            }

            //came out of while loop, now close the rsock client library object
            RSockConstants.intrfc_retrieval.close();
            RSockConstants.intrfc_retrieval = null;

        }catch(IOException | ClassNotFoundException  e ){e.printStackTrace(); }

    }

    public static void stop(){
        isRunning = false;
    }
}
