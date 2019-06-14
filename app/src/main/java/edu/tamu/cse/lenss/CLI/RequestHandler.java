package edu.tamu.cse.lenss.CLI;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.MDFSBlockCreator;
import edu.tamu.lenss.mdfs.MDFSBlockCreatorViaRsock;
import edu.tamu.lenss.mdfs.MDFSFileCreator;
import edu.tamu.lenss.mdfs.MDFSFileCreatorViaRsock;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static java.lang.Thread.sleep;

class RequestHandler implements Runnable{

    private Socket cSocket;

    public RequestHandler( Socket cSocket) {
        super();
        this.cSocket = cSocket;
    }

    public void run() {

        try{
            //Read the request from the CPP daemon
            BufferedReader inBuffer = new BufferedReader(new InputStreamReader( cSocket.getInputStream() ));
            OutputStream os = cSocket.getOutputStream();
            String inMessage = inBuffer.readLine();
            System.out.println("CLIII Incoming message: "+inMessage);

            //Process the message and reply to the Daemon
            String rep = processRequestCpp(inMessage);
            System.out.println("CLIII Reply message = "+rep);
            os.write((rep+"\n\n").getBytes());
            os.flush();

            //Now close the socket.
            inBuffer.close();
            os.close();
            cSocket.close();

        }catch (IOException e) {
            System.out.println("CLIII Problem handelling client socket in RequestHandler"+ e);

        }

    }

    private String processRequestCpp(String inMessage){

        
        //first read file from storage
        File camera = getExternalStoragePublicDirectory("/distressnet/MStorm/StreamFDPic/");
       if (camera != null) {
            System.out.println("CLIII Camera is NOT null");
            int total=10;
            File[] pics = camera.listFiles();
            double min=0;
            double max = pics.length;
            for(int i=0; i< total; i++){
                int index = (int)((Math.random()*((max-min)+1))+min);
                System.out.println("CLIII sending file");
                sendFile(pics[index]);
                try { sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }else{
            System.out.println("CLIII Camera is null");
        }

        //System.out.println("CLIII dir: " + camera.listFiles().length);

        String reply = "reply from cli";
        return reply;
    }

    private void compressAndsendFile(final File orgiginalFile, final String newFileName, final boolean compressed){
        final File renamedFile;
        if(newFileName != null){
            renamedFile = new File(orgiginalFile.getParent()+ File.separator + newFileName);
            orgiginalFile.renameTo(renamedFile);
        }
        else{
            renamedFile = orgiginalFile;
        }

        if(compressed){
            try {
                File tmpF = AndroidIOUtils.getExternalFile(Constants.DIR_CACHE);
                final File compressedFile = IOUtilities.createNewFile(tmpF, newFileName);
                FileOutputStream out = new FileOutputStream(compressedFile);
                AndroidIOUtils.decodeSampledBitmapFromFile(renamedFile.getAbsolutePath(), Constants.COMMON_DEVICE_WIDTH, Constants.COMMON_DEVICE_HEIGHT).compress(Bitmap.CompressFormat.JPEG, 85, out);
                renamedFile.delete();
                sendFile(compressedFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            sendFile(renamedFile);
        }

    }

    private void sendFile(final File file){
        //check whether file creation via rsock or tcp
        if(Constants.file_creation_via_rsock_or_tcp.equals("rsock")){  //RSOCK
            MDFSFileCreatorViaRsock creator = new MDFSFileCreatorViaRsock(file, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO);
            creator.setEncryptKey(ServiceHelper.getInstance().getEncryptKey());
            creator.setListener(fileCreatorListenerviarsock);
            creator.start();
            //try { sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        }else if(Constants.file_creation_via_rsock_or_tcp.equals("tcp")){
            MDFSFileCreator creator = new MDFSFileCreator(file, Constants.MAX_BLOCK_SIZE, Constants.K_N_RATIO);
            creator.setEncryptKey(ServiceHelper.getInstance().getEncryptKey());
            creator.setListener(fileCreatorListener);
            creator.start();
            System.out.println("sendfile thread created tcp");
        }

    }

    //rsock listener
    private MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock fileCreatorListenerviarsock = new MDFSBlockCreatorViaRsock.MDFSBlockCreatorListenerViaRsock(){
        @Override
        public void statusUpdate(String status) {

        }

        @Override
        public void onError(String error) {

        }

        @Override
        public void onComplete() {

        }
    };

    private MDFSBlockCreator.MDFSBlockCreatorListener fileCreatorListener = new MDFSBlockCreator.MDFSBlockCreatorListener(){
        @Override
        public void statusUpdate(String status) {
        }

        @Override
        public void onError(String error) {
        }

        @Override
        public void onComplete() {
        }
    };



}