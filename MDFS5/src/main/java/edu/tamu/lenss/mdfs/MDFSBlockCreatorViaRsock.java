package edu.tamu.lenss.mdfs;

import static android.os.Environment.getExternalStoragePublicDirectory;
import static java.lang.Thread.sleep;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.RSock.RSockConstants;
import edu.tamu.lenss.mdfs.crypto.FragmentInfo;
import edu.tamu.lenss.mdfs.crypto.MDFSEncoder;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

//rsock imports


//this class object is made in MDFSFileCreatorViaRsock.java file
//this class is responsible for taking each block and send over rsock, instead of tcp
//most of the code are copied from mdfsBlockCreator.java class which uses tcp
public class MDFSBlockCreatorViaRsock {
    private static final String TAG = MDFSBlockCreatorViaRsock.class.getSimpleName();
    private byte blockIdx;


    private List<String> fileStoragesAddrasGUID;
    private byte[] encryptKey;
    private byte k2, n2;
    private File blockFile;
    private ServiceHelper serviceHelper;
    private MDFSFileInfo fileInfo;
    private boolean fetchTopology, isEncryptComplete;
    private AtomicInteger fragCounter;
    List<String> chosenNodes;       //list of GUIDs who will receive a fragment
    String[] permList;              //permission list for the file
    public boolean isFinished = false;
    String clientID;                //client id who made the file creation request


    public MDFSBlockCreatorViaRsock(File file, MDFSFileInfo info, byte blockIndex, MDFSBlockCreatorListenerViaRsock lis, String[] permlist, List<String> chosenodes, String clientID) {  //RSOCK
        this.blockIdx = blockIndex;
        this.blockFile = file;
        this.listener = lis;
        this.serviceHelper = ServiceHelper.getInstance();
        this.fileInfo = info;
        this.k2 = fileInfo.getK2();
        this.n2 = fileInfo.getN2();
        this.fragCounter = new AtomicInteger();
        this.permList = permlist;
        this.chosenNodes = chosenodes;
        this.clientID = clientID;
    }


    public void start() {
        System.out.println("start gets called aaa");
        encryptFile();
        initTopology();
        distributeFragments();
    }


    //gets the chosenNodes fetched and chosen by MDFSFileCreatorViaRsock.java class
    private void initTopology(){
        fileStoragesAddrasGUID = chosenNodes;
        fetchTopology = true;
    }

    public void setEncryptKey(byte[] key){
        this.encryptKey = key;
    }

    private void encryptFile() {
        isEncryptComplete = false;
        if(blockFile == null || !blockFile.exists())
            return;

        MDFSEncoder encoder = new MDFSEncoder(blockFile, n2, k2);
        if(encryptKey != null)
            encoder.setKey(encryptKey);
        List<FragmentInfo> fragInfos = encoder.encodeNow();   //here we have all the fragments of this blocks

        //if (!encoder.encode()) {
        if(fragInfos == null) {
            listener.onError("File Encryption Failed", clientID);
            return;
        }


        // Store the file fragments in local SDCard
        File fragsDir = AndroidIOUtils.getExternalFile(MDFSFileInfo
                .getBlockDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime(),	blockIdx));

        HashSet<Byte> frags = new HashSet<Byte>();

        // Write file fragments to SD Card
        for (FragmentInfo frag : fragInfos) {
            File tmp = IOUtilities.createNewFile(fragsDir, MDFSFileInfo.getFragName(fileInfo.getFileName(), blockIdx, frag.getFragmentNumber()));

            if (tmp != null && IOUtilities.writeObjectToFile(frag, tmp)) {
                frags.add(frag.getFragmentNumber());
            }
        }
        serviceHelper.getDirectory().addBlockFragments(fileInfo.getCreatedTime(), blockIdx, frags);
        listener.statusUpdate("Encryption Complete");
        Logger.i(TAG + " encryptFile()", "Encryption Complete");
        isEncryptComplete = true;
    }


    //this functions distributes fragments to other nodes
    private void distributeFragments() {
        //check if file encryption and fetch topoplogy succeeded
        if (!fetchTopology || !isEncryptComplete)
            return;
        System.out.println("distributeFragments() is called");

        //check if fileStoragesAddrasGUID only contains one entry ans that is ownGUID
        //that means locally fragment storage is done and needs to call oncomplete() and return.
        if(fileStoragesAddrasGUID.size()==1 && fileStoragesAddrasGUID.contains(GNS.ownGUID)){
            listener.onComplete("fragments were distributed", clientID);
            return;
        }

        // Scan through all files in the folder and upload them
        File fileFragDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime(), blockIdx));
        if(!fileFragDir.exists()){
            Logger.e(TAG, "Can't find fragments directory of the block");
            listener.onError("No block directory", clientID);
            return;
        }

        File[] files = fileFragDir.listFiles();
        String destNode;


        fragCounter.set(0);
        Iterator<String> nodesIter = fileStoragesAddrasGUID.iterator();
        for (File f : files) {
            if (f.getName().contains("__frag__")) {
                // Find the fragment Number
                if (nodesIter != null && nodesIter.hasNext()) {
                    destNode = nodesIter.next();
                    if (destNode.equals(GNS.ownGUID)){
                        fragCounter.incrementAndGet();
                        continue; // Don't need to send to myself again
                    }
                     //upload via rsock by creating new threads
                    //Thread t1 = new Thread(new FragmentUploaderViaRsock(f, fileInfo.getCreatedTime(), destNode, !nodesIter.hasNext()), "t1");
                    //t1.start();

                    //or, upload via executorservice
                    ServiceHelper.getInstance().executeRunnableTask(new FragmentUploaderViaRsock(f, fileInfo.getCreatedTime(), permList, destNode, !nodesIter.hasNext()));

                }
            }
        }

        listener.statusUpdate("Distributing block fragments");
    }

    protected boolean deleteBlockFile(){
        return blockFile.delete();
    }


    //This class is used when a fragment is about to be sent over rsock, instead of tcp
    //this class packs all the required parameters and packs into a MDFSRsockBlockCreator obj,
    //and pushes it to the rsock daemon through rsock api
    //most of the code of this class is copied from MDFSBlockCreator.FragmentUploader class
    //read MDFSBlockCreator.FragmentUploader class to see how that code was used here.
    //how: we first generate a header, pack the header-filefrag and other important params in a
    //MDFSRsockBlockCreator object, convert the objest into byteArray, and send that over rsock.
    class FragmentUploaderViaRsock implements Runnable{
        private File fileFrag;
        private String destGUID;
        private long fileCreatedTime;
        private byte blockIndex;
        private byte fragmentIndex;
        private String[] permList;

        public FragmentUploaderViaRsock(File frag, long fileCreationTime, String[] permlist, String destGUID, boolean last) {
            this.fileFrag = frag;
            this.destGUID = destGUID;
            this.fileCreatedTime = fileCreationTime;
            this.permList = permlist;
            this.blockIndex = parseBlockNum(frag.getName());
            this.fragmentIndex = parseFragNum(frag.getName());
        }

        private byte parseBlockNum(String fName){
            String str = fName.substring(0, fName.lastIndexOf("__frag__"));
            str = str.substring(str.lastIndexOf("_")+1);
            return Byte.parseByte(str.trim());
        }
        private byte parseFragNum(String fName) {
            return Byte.parseByte(fName.substring(fName.lastIndexOf("_") + 1).trim());
        }

        @Override
        public void run() {
            Logger.d(TAG, "FragmentUploaderRsock thread is running");
            boolean success = false;
            try {

                //Header
                FragmentTransferInfo header = new FragmentTransferInfo(fileInfo.getFileName(), fileCreatedTime, blockIndex, fragmentIndex, FragmentTransferInfo.REQ_TO_SEND);
                header.setNeedReply(true);

                //read the content of the filefrag into bytearray
                byte[] byteArray = new byte[(int) fileFrag.length()];
                try {
                    FileInputStream fileInputStream = new FileInputStream(fileFrag);
                    fileInputStream.read(byteArray);
                } catch (FileNotFoundException e) {
                    System.out.println("File Not Found.");
                    e.printStackTrace();
                }
                catch (IOException e1) {
                    System.out.println("Error Reading The File.");
                    e1.printStackTrace();
                }

                System.out.println("sizeee of filefrag send: " + fileFrag.length());
                System.out.println("sizeee of bytearray send: " + byteArray.length);

                //make MDFSRsockBlockCreator obj
                MDFSRsockBlockCreator mdfsrsockblock = new MDFSRsockBlockCreator(header, byteArray, fileInfo.getFileName(), fileFrag.length(), fileInfo.getNumberOfBlocks(), fileInfo.getN2(), fileInfo.getK2(), fileCreatedTime, permList, GNS.ownGUID, destGUID);

                //get byteArray and size of the MDFSRsockBlockCreator obj and do send over rsock
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(bos);
                    oos.writeObject(mdfsrsockblock);
                    oos.flush();
                    byte [] data = bos.toByteArray();


                    //print
                    System.out.println(Arrays.toString(data));


                    //send the object over rsock. only send people who are not me
                    String uuid = UUID.randomUUID().toString().substring(0, 12);
                    RSockConstants.intrfc_creation.send(uuid, data, data.length, "nothing", "nothing", destGUID, 0, "default", "default", "default");
                    System.out.println("fragment has been pushed to the rsock daemon to : " + destGUID);


                } catch (IOException e) {
                    e.printStackTrace();
                }

                success = true;

            } catch (NullPointerException nulp) {
                nulp.printStackTrace();
            }finally {
                if (!success) {
                    //do something
                }
                else if(fragCounter.incrementAndGet() >= n2){	// success!
                    if(isFinished)
                        return;
                    if (fragCounter.get() > k2 || (fragCounter.get()==k2 && n2 == k2 )){
                        Logger.v(TAG, fragCounter.get() + " fragments were distributed");
                        listener.onComplete("fragments were distributed", clientID);

                    }
                    else{
                        // Delete fragments
                        DeleteFile deleteFile = new DeleteFile();
                        deleteFile.setFile(fileInfo.getFileName(), fileInfo.getCreatedTime());
                        ServiceHelper.getInstance().deleteFiles(deleteFile);
                        listener.onError("Fail to distribute file fragments. " + "Only " + fragCounter.get() + " were successfully sent. Please try again later", clientID);
                    }
                    isFinished = true;

                }
            }
        }

    }


    private MDFSBlockCreatorListenerViaRsock listener = new MDFSBlockCreatorListenerViaRsock(){
        @Override
        public void statusUpdate(String status) {}

        @Override
        public void onError(String error, String clientID) {}

        @Override
        public void onComplete(String msg, String clientID) {}

    };

    public interface MDFSBlockCreatorListenerViaRsock {
        public void statusUpdate(String status);

        public void onError(String error, String clientID);

        public void onComplete(String msg, String clientID);
    }

    private class BlockCreationLog{
        public BlockCreationLog(){}
        public long topStart, topEnd, encryStart, encryStop,
                optStart, optStop, distStart, distStop;
        public String getDiff(long l1, long l2){
            return Long.toString(Math.abs(l2-l1));
        }
    }

}

