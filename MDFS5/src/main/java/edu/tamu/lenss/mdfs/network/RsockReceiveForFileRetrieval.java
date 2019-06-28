package edu.tamu.lenss.mdfs.network;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.MDFSBlockRetriever;
import edu.tamu.lenss.mdfs.MDFSRsockBlockRetrieval;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.Logger;
import example.*;

import static java.lang.Thread.sleep;


//this class is used for block retrieval via rsock, instead of tcp
public class RsockReceiveForFileRetrieval implements Runnable {

    private static final String TAG = MDFSBlockRetriever.class.getSimpleName();
    private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";

    public RsockReceiveForFileRetrieval() {}

    @Override
    public void run() {

        try {
            //create rsock Interface intrfc
            RSockConstants.intrfc_retrieval = Interface.getInstance(GNS.getGNSInstance().getOwnGuid(), RSockConstants.intrfc_retrieval_appid, 999);

            ReceivedFile receivedFile = null;
            byte[] byteArray = null;
            boolean isRunning = true;
            ByteArrayInputStream bis;
            ObjectInputStream ois;
            while (isRunning) {

                //blocking receive
                ////receivedFile = intrfc.receive(0);
                try { receivedFile = RSockConstants.intrfc_retrieval.receive(0,"hdrRecv");} catch (InterruptedException e) {e.printStackTrace(); }

                //if unblocked, check if received something
                if (receivedFile != null) {

                    //print
                    System.out.println("received file frag which is not null inside RsockReceiveForFileRetrieval");

                    //print byteArray
                    //System.out.println("print: " + Arrays.toString(receivedFile.getFileArray()));

                    //create MDFSRsockBlockRetrieval object from raw byteArray
                    bis = new ByteArrayInputStream(receivedFile.getFileArray());
                    ois = new ObjectInputStream(bis);
                    MDFSRsockBlockRetrieval mdfsrsockblock = (MDFSRsockBlockRetrieval) ois.readObject();
                    bis.close(); ois.close();

                    //parse mdfsrsockblock and get header containing FragmentTransferInfo, destIP etc
                    FragmentTransferInfo header = (FragmentTransferInfo) mdfsrsockblock.fragTransInfoHeader;
                    String destGUID = (String) mdfsrsockblock.destGUID;     //aka own GUID
                    String srcGUID = (String) mdfsrsockblock.srcGUID;       //aka GUID of the node which made the req
                    String fileName = (String) mdfsrsockblock.fileName;
                    long fileId = (long) mdfsrsockblock.fileId;
                    byte blockIdx =(byte)mdfsrsockblock.blockIdx ;
                    byte fragmentIndex = (byte) mdfsrsockblock.fragmentIndex;


                    //create TCP on the same machine
                    TCPSend send = TCPConnection.creatConnection(GNS.gnsServiceClient.getIPbyGUID(destGUID).get(0)); //todo:  dont make tcp fetch the fragment from disk
                    if(send == null){ Logger.e(TAG, "Connection Failed"); return; }

                    //send header
                    ObjectOutputStream oos = new ObjectOutputStream(send.getOutputStream());
                    oos.writeObject(header);

                    //sleep for 10 millisec, allowing the other side to process header and decide whether to close the tcpSocket or nah
                    try { sleep(5); } catch (InterruptedException e) { e.printStackTrace(); }


                    //receive fileFragment from the same machine
                    File tmp0 = null;
                    byte[] buffer = new byte[Constants.TCP_COMM_BUFFER_SIZE];
                    tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex) + DOWNLOADING_SIGNATURE);
                    FileOutputStream fos = new FileOutputStream(tmp0);
                    int readLen = 0;
                    DataInputStream din = send.getInputStream();
                    while ((readLen = din.read(buffer)) >= 0) {
                        fos.write(buffer, 0, readLen);
                    }//this while loop breaks when readLen reads -1 due to other side closing the socket

                    //close all
                    fos.close();
                    oos.close();
                    din.close();
                    send.close();


                    //if fragment was fetched
                    if(tmp0.length()>0){
                        System.out.println("hack tcp success");

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
                        mdfsrsockblock = new MDFSRsockBlockRetrieval(byteArray, (int) tmp0.length(), true, GNS.ownGUID );

                        //convert mdfsrsockblock object into bytearray and do send
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream ooos = null;
                        try {
                            ooos = new ObjectOutputStream(bos);
                            ooos.writeObject(mdfsrsockblock);
                            ooos.flush();
                            byte [] data = bos.toByteArray();

                            //send the object over rsock
                            String uuid = UUID.randomUUID().toString().substring(0,12);
                            RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing","nothing", srcGUID, 0, "hdrRecv", receivedFile.getReplyEndpoint(), "noReply");
                            System.out.println("fragment has been pushed to the rsock daemon (success)");

                            //delete tmp0 file
                            tmp0.delete();


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        System.out.println("hack tcp failed");

                        //now, make MDFSRsockBlockRetrieval object with the fragment byteArray
                        mdfsrsockblock = new MDFSRsockBlockRetrieval(byteArray, (int) tmp0.length(), false, GNS.ownGUID );

                        //convert mdfsrsockblock object into bytearray and do send
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream ooos = null;
                        try {
                            ooos = new ObjectOutputStream(bos);
                            ooos.writeObject(mdfsrsockblock);
                            ooos.flush();
                            byte [] data = bos.toByteArray();

                            //send the object over rsock
                            String uuid = UUID.randomUUID().toString().substring(0,12);
                            RSockConstants.intrfc_retrieval.send(uuid, data, data.length, "nothing","nothing", srcGUID, 0, "hdrRecv", receivedFile.getReplyEndpoint(), "nothing");
                            System.out.println("fragment has been pushed to the rsock daemon (failure)");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }
        }catch(IOException | ClassNotFoundException  e ){e.printStackTrace(); }  ////InterruptedException

    }
}
