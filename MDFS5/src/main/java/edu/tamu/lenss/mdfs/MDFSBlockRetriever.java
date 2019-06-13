package edu.tamu.lenss.mdfs;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import edu.tamu.lenss.mdfs.crypto.FragmentInfo;
import edu.tamu.lenss.mdfs.crypto.MDFSDecoder;
import edu.tamu.lenss.mdfs.handler.BlockReplyHandler.BlockRepListener;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.models.BlockReply;
import edu.tamu.lenss.mdfs.models.BlockReq;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.network.TCPConnection;
import edu.tamu.lenss.mdfs.network.TCPSend;
import edu.tamu.lenss.mdfs.placement.PlacementHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.JCountDownTimer;
import edu.tamu.lenss.mdfs.utils.Logger;
import edu.tamu.lenss.mdfs.utils.MyPair;


public class MDFSBlockRetriever {
	private static final String TAG = MDFSBlockRetriever.class.getSimpleName();
	private static final String DOWNLOADING_SIGNATURE = "_dOwN__lOaDiNg___";
	private String fileName;
	private long fileId;	// User the file create time currently
	private byte blockIdx;
	private ServiceHelper serviceHelper;
	private Map<Long, List<Byte>> fileFrags = new HashMap<Long, List<Byte>>(); // <IPAddress, List<FragNum>>
	private MDFSFileInfo fileInfo;
	private boolean decoding = false;	// Has the decoding procedure started?
	private final BlockRetrieveLog fileRetLog = new BlockRetrieveLog();

	public MDFSBlockRetriever(String fileName, long fileId, byte blockIndex){
		serviceHelper = ServiceHelper.getInstance();
		this.fileName = fileName;
		this.fileId = fileId;
		this.blockIdx = blockIndex;
		this.fileInfo = serviceHelper.getDirectory().getFileInfo(fileId);
	}

	public MDFSBlockRetriever(MDFSFileInfo fInfo, byte blockIndex){
		this(fInfo.getFileName(), fInfo.getCreatedTime(), blockIndex);
		this.fileInfo = fInfo;
	}

	private byte[] decryptKey;
	/**
	 * Application needs to provide a symmetric key for decryping the block
	 * @param key
	 */
	public void setDecryptKey(byte[] key){
		this.decryptKey = key;
	}

	public void start(){
		serviceHelper.getDirectory().addDownloadingBlock(fileInfo.getCreatedTime(), blockIdx);
		// Check if a decrypted file or encrypted file already exists on my device
		// If it is, returns it immediately.
		final List<FragmentInfo> localFrags = getStoredFrags();
		if(localFrags != null && (byte)localFrags.size() >= fileInfo.getK2()){
			Logger.v(TAG, "Recovering block from local caches");
			/*
			 * decodeFile() is a blocking call.
			 * This is also necessary in order to ensure onComplete() is called from a different thread. (required for notify())
			 */
			serviceHelper.executeRunnableTask(new Runnable(){
				@Override
				public void run() {
					decodeFile(localFrags);
				}
			});
		}
		else{
			sendBlockREQ();
			setUpTimer();
		}
	}

	/**
	 * Non-blocking
	 */
	private void sendBlockREQ() {
		BlockReq req = new BlockReq(fileName, fileId, blockIdx);
		req.setAnyAvailable(true);

		fileRetLog.discStart = System.currentTimeMillis();
		serviceHelper.startBlockRequest(req, fileRepListener);
		listener.statusUpdate("Searching for nearby fragments");
	}

	//this listener if implemented from BlockReplyHandler.java class
	//this is only used when received a block reply
	private int initFileFragsCnt = 0;
	private BlockRepListener fileRepListener = new BlockRepListener(){
		@Override
		public void onError(String msg) {
			Logger.e(TAG, msg);
			listener.onError("Can't locate any data blocks. ", fileInfo);
		}

		@Override
		public void onComplete(Set<BlockReply> blockREPs) {
			fileRetLog.fileReps = blockREPs;
			fileRetLog.discEnd = System.currentTimeMillis();

			// Retrieve block fragments
			Set<Byte> myfrags = serviceHelper.getDirectory().getStoredFragIndex(fileId, blockIdx);	// Get the fragments I have
			if(myfrags == null)
				myfrags = new HashSet<Byte>();
			Set<Byte> uniqueFrags = new HashSet<Byte>();				// all the available fragments, including mine and others

			// add fragments from other nodes
			for(BlockReply rep : blockREPs){
				List<Byte> tmpList = rep.getBlockFragIndex();
				if(tmpList != null){
					uniqueFrags.addAll(tmpList);
					Logger.v(TAG, "Node " + IOUtilities.long2Ip(rep.getSource()) + " has " + tmpList.size() + " block Frags");
					tmpList.removeAll(myfrags);						// Retain only the fragments that I DO NOT have and
					fileFrags.put(rep.getSource(), tmpList);		// add the unique fragments (different from mine) that each storage node has
				}
			}
			// Note. fileFrags do not include MY cached fragments

			locFragCounter.set(myfrags.size());						// Init downloadCounter to the number of file frags I have
			initFileFragsCnt = locFragCounter.get();
			uniqueFrags.addAll(myfrags);							// add my fragments

			if(uniqueFrags.size() < fileInfo.getK2()){
				String s = uniqueFrags.size() + " block fragments";
				Logger.w(TAG, s + ". Insufficient fragments");
				listener.onError("Insufficient fragments. " + s, fileInfo);
			}
			else{
				// Start to download file
				downloadFrags();
			}
		}
	};

	private AtomicInteger locFragCounter = new AtomicInteger();
	private JCountDownTimer timer;
	private void setUpTimer(){
		// Assume throughput is 800kBps
		timer = new JCountDownTimer(Constants.FRAGMENT_RETRIEVAL_TIMEOUT_INTERVAL, Constants.FRAGMENT_RETRIEVAL_TIMEOUT_INTERVAL){
			@Override
			public synchronized void onFinish() {
				/*
				 * onFinish() may be called multiple times by different FragmentDownloader thread,
				 * but decodeFile() will only execute once
				 */
				if (locFragCounter.get() >= fileInfo.getK2() ){
					Logger.v(TAG, (locFragCounter.get()-initFileFragsCnt) + " file fragments were downloaded");
					fileRetLog.retEnd = System.currentTimeMillis();
					decodeFile();
				}
				else{
					serviceHelper.getDirectory().removeDownloadingBlock(fileInfo.getCreatedTime(), blockIdx);
					listener.onError("Fail to download file fragments. " +
							"Only " + (locFragCounter.get()-initFileFragsCnt) +
							" were successfully downloaded.", fileInfo);
				}
			}
			@Override
			public synchronized void onTick(long millisUntilFinished) {
			}
		};
	}
	/**
	 * Non-blocking
	 */
	private void downloadFrags(){
		// Create a folder for fragments
		File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileId, blockIdx));
		if(!tmp0.exists()){
			if(!tmp0.mkdirs()){
				listener.onError("File IO Error. Can't save file locally", fileInfo);
				return;
			}
		}

		listener.statusUpdate("Downloading fragments");
		// If I have enough fragments already
		if(locFragCounter.get() >= fileInfo.getK2()){
			decodeFile();
			return;
		}
		int requiredDownloadCnt = fileInfo.getK2() + 1 - locFragCounter.get();	// Download one more fragment. Just in case...

		// Search for the best(closest) storages
		List<Long> storageNodes = new ArrayList<Long>(fileFrags.keySet());
		Collections.sort(storageNodes, new SortByHopCount());
		Set<Byte> requestedFrag = new HashSet<Byte>();	// Record the downloaded frag. Do not download duplicates.
		for(Long sNode : storageNodes){
			List<Byte> nodeFrags = fileFrags.get(sNode);
			for(Byte fragNum : nodeFrags){
				if(!requestedFrag.contains(fragNum)){
					requestedFrag.add(fragNum);
					serviceHelper.executeRunnableTask(new FragmentDownloadloader(sNode, blockIdx ,fragNum));
					if(--requiredDownloadCnt < 1){
						timer.start();
						return;
					}
				}
			}
		}
		//fileRetLog.retStart = System.currentTimeMillis(); Logger.v(TAG, "Retrieval Start: " + fileRetLog.retStart );
		timer.start();
	}

	/**
	 *	Distance from me; the closer to me, the shorter the disatance is
	 */
	class SortByHopCount implements Comparator<Long>{
		@Override
		public int compare(Long nodeId1, Long nodeId2) {
			return PlacementHelper.meshHopDistance(serviceHelper.getNodeManager().getMyIP(), nodeId2);
		}
	}

	private List<FragmentInfo> getStoredFrags(){
		Set<Byte> cachedFrags = serviceHelper.getDirectory().getStoredFragIndex(fileId, blockIdx);
		List<FragmentInfo> blockFragments = new ArrayList<FragmentInfo>();
		if(cachedFrags != null){
			// Check stored fragments
			File blockDir = AndroidIOUtils.getExternalFile(MDFSFileInfo.getBlockDirPath(fileName, fileId, blockIdx));
			if(blockDir.isDirectory()){
				File[] files = blockDir.listFiles(new FileFilter(){
					@Override
					public boolean accept(File f) {
						// one fragment maybe still being downloaded. We assume each fragment is at least 5k large
						return (f.isFile()
								&& !f.getName().contains(DOWNLOADING_SIGNATURE)
								&& f.getName().contains(fileName + "__" + blockIdx + "__frag__")
								&& f.length() > 1024*5);
					}
				});
				for (File f : files) {
					FragmentInfo frag; frag = IOUtilities.readObjectFromFile(f, FragmentInfo.class);
					if(frag != null && cachedFrags.contains(frag.getFragmentNumber()))
						blockFragments.add(frag);
				}
			}
		}
		return blockFragments;
	}

	/**
	 * This function can only be called when enough file and key fragments are available <br>
	 * Blocking call
	 */
	private synchronized void decodeFile(List<FragmentInfo> blockFragments){
		// Final Check. Make sure enough fragments are available
		if(blockFragments.size() < fileInfo.getK2()){
			String s = blockFragments.size() + " block fragments are available locally.";
			listener.onError("Insufficient fragments. " + s, fileInfo);
			return;
		}

		/*
		 * Ensure that a block decoded once.
		 * decodeFile() method may be called by different FragmentDownloadloader threads multiple times
		 */
		if(decoding){
			return;
		}
		decoding = true;
		listener.statusUpdate("Decoding fragments");
		fileRetLog.decryStart = System.currentTimeMillis();

		if(decryptKey == null){
			Logger.e(TAG, "No Decryption Key");
			return;
		}
		MDFSDecoder decoder = new MDFSDecoder(fileInfo.getK2(), fileInfo.getN2(), decryptKey);
		File tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFileDirPath(fileInfo.getFileName(), fileInfo.getCreatedTime()));
		File tmp = IOUtilities.createNewFile(tmp0, MDFSFileInfo.getBlockName(fileInfo.getFileName(), blockIdx));

		if(decoder.decodeNow(blockFragments, tmp.getAbsolutePath())){
			Logger.i(TAG, "Block Decryption Complete");
			listener.onComplete(tmp, fileInfo);
		}
		else{
			listener.onError("Fail to decode the fragments. You may try again", fileInfo);
			return;
		}
		serviceHelper.getDirectory().removeDownloadingBlock(fileInfo.getCreatedTime(), blockIdx);
	}

	/**
	 * This function can only be called when enough file and key fragments are available <br>
	 * Blocking call
	 */
	private void decodeFile(){
		decodeFile(getStoredFrags());
	}

	private void writeLog(){
		// Log Data
		//DataLogger dataLogger = ServiceHelper.getInstance().getDataLogger();
		long nodeMac = ServiceHelper.getInstance().getNodeManager().getMyMAC();
		StringBuilder str = new StringBuilder();
		str.append(System.currentTimeMillis() + ", ");
		str.append(fileInfo.getCreator() + ", ");
		str.append(fileInfo.getFileName() + ", ");
		str.append(fileInfo.getFileSize() + ", ");
		str.append(fileRetLog.getDiff(fileRetLog.discStart, fileRetLog.discEnd-Constants.FILE_REQUEST_TIMEOUT) + ", ");
		str.append(fileRetLog.getDiff(fileRetLog.decryStart, fileRetLog.decryEnd) + ", ");
		str.append(fileRetLog.getDiff(fileRetLog.retStart, fileRetLog.retEnd) + ", ");
		str.append("\n");

		String tmpStr = "	";
		Iterator<BlockReply> iter = fileRetLog.fileReps.iterator();
		while(iter.hasNext()){
			BlockReply rep = iter.next();
			if(rep.getFileName() != null){
				tmpStr += rep.getSource() + ", ";
			}
		}
		str.append(tmpStr + "\n");

		tmpStr = "	";
		for(MyPair<Long, Byte> pair : fileRetLog.fileSources){
			tmpStr += pair.first + ", " + pair.second + ", ";
		}
		str.append(tmpStr);
		str.append("\n");
		//dataLogger.appendSensorData(LogFileName.FILE_RETRIEVAL, str.toString());

		// File Discovery
		str.delete(0, str.length()-1);
		str.append(nodeMac + ", ");
		str.append("FileDiscovery, ");
		str.append(fileRetLog.fileReps.size() + ", ");
		str.append(fileInfo.getN2() + ", " + fileInfo.getK2() + ", ");
		str.append(fileRetLog.discStart + ", ");
		str.append(fileRetLog.getDiff(fileRetLog.discStart, fileRetLog.discEnd) + ", ");
		str.append("\n");
		//dataLogger.appendSensorData(LogFileName.TIMES, str.toString());

		// File Retrieval
		str.delete(0, str.length()-1);
		str.append(nodeMac + ", ");
		str.append("FileRetrieval, ");
		str.append(fileRetLog.fileReps.size() + ", ");
		str.append(fileInfo.getN2() + ", " + fileInfo.getK2() + ", ");
		str.append(fileInfo.getFileSize() + ", ");
		str.append(fileRetLog.retStart + ", ");
		str.append(fileRetLog.getDiff(fileRetLog.retStart, fileRetLog.retEnd) + ", ");
		str.append("\n\n");
		//dataLogger.appendSensorData(LogFileName.TIMES, str.toString());
	}

	/**
	 * Start one thread to download each fragment. At the end of each thread, check the number of fragments that
	 * have been successfully downloaded. If there is enough fragments, we start to decode (by calling onFinish());
	 * otherwise, we restart the timer and wait for the next fragment to be downloaded
	 * @author Jay
	 */
	class FragmentDownloadloader implements Runnable{
		private long destIP;
		private byte blockIdx;
		private byte fragmentIndex;

		private FragmentDownloadloader(long destination, byte blockIndex, byte fragmentIdx){
			this.destIP = destination;
			this.fragmentIndex = fragmentIdx;
			this.blockIdx = blockIndex;
		}

		@Override
		public void run() {
			boolean success = false;
			byte[] buffer = new byte[Constants.TCP_COMM_BUFFER_SIZE];
			final String IPAdd = IOUtilities.long2Ip(destIP);

			TCPSend send = TCPConnection.creatConnection(IPAdd);
			if(send == null){
				Logger.e(TAG, "Connection Failed");
				return;
			}
			// Handshake
			FragmentTransferInfo header = new FragmentTransferInfo(fileName,
					fileId, blockIdx, fragmentIndex, FragmentTransferInfo.REQ_TO_RECEIVE);
			//header.setFragmented(true);
			header.setNeedReply(false);


			// Maybe we should wait for another handshake response?
			File tmp0=null;
			try {
				ObjectOutputStream oos = new ObjectOutputStream(send.getOutputStream());
				oos.writeObject(header);

				// Start to download and save the file fragment
				Logger.v(TAG, "Strat downloading frag " + fragmentIndex + " from " + IPAdd);
				tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex) +DOWNLOADING_SIGNATURE );
				FileOutputStream fos = new FileOutputStream(tmp0);
				int readLen=0;
				DataInputStream din = send.getInputStream();
				while ((readLen = din.read(buffer)) >= 0) {
					fos.write(buffer, 0, readLen);
				}
				if(tmp0.length() > 0)
					Logger.v(TAG, "Finish downloading fragment " + fragmentIndex + " from node " + IPAdd);
				else
					Logger.w(TAG, "Zero bytes file fragment " + fragmentIndex + " from node " + IPAdd);

				fos.close();
				oos.close();
				din.close();
				send.close();

				// Rename the fragment after it is completely downloaded to avoid decoder using a fragment that is still being downloaded
				success = IOUtilities.renameFile(tmp0, MDFSFileInfo.getFragName(fileName, blockIdx, fragmentIndex));
				/*if(success)
					Logger.v(TAG, "Rename fragment " + fragmentIndex + " from node " + IPAdd + " successfully");*/
				tmp0 = AndroidIOUtils.getExternalFile(MDFSFileInfo.getFragmentPath(fileName, fileId, blockIdx, fragmentIndex));
			} catch (IOException e) {
				e.printStackTrace();
			} finally{
				timer.cancel();
				if(success && tmp0.length() > 0){	// Hacky way to avoid 0 byte file
					// update directory
					serviceHelper.getDirectory().addBlockFragment(
							header.getCreatedTime(), header.getBlockIndex(), header.getFragIndex());
					locFragCounter.incrementAndGet();
				}
				else if(tmp0 != null){
					tmp0.delete();
				}

				/*
				 * Timer is unnecessary. Decoding procedure has started.
				 * This may happen when the last fragment finishes after the decode() procedure has started
				 */
				// Early termination.
				timer.cancel();		// prepare to restart the timer
				if(locFragCounter.get() >= fileInfo.getK2()){
					timer.onFinish();		// may be called multiple times by different FragmentDownloader thread
				}
				else{
					timer.cancel();
				}
			}
		}
	}

	public BlockRetrieverListener getListener() {
		return listener;
	}

	public void setListener(BlockRetrieverListener listener) {
		this.listener = listener;
	}

	/*
	 * Default FileRetrieverListener. Do nothing.
	 */
	private BlockRetrieverListener listener = new BlockRetrieverListener(){
		@Override
		public void onError(String error, MDFSFileInfo fileInfo) {
		}
		@Override
		public void onComplete(File decryptedFile, MDFSFileInfo fileInfo) {
		}
		@Override
		public void statusUpdate(String status) {
		}
	};

	public interface BlockRetrieverListener{
		public void onError(String error, MDFSFileInfo fileInfo);
		public void statusUpdate(String status);
		public void onComplete(File decryptedFile, MDFSFileInfo fileInfo);
	}

	private class BlockRetrieveLog{
		public BlockRetrieveLog(){}
		public long discStart, discEnd, retStart, retEnd, decryStart, decryEnd;
		public List<MyPair<Long, Byte>> fileSources = new ArrayList<MyPair<Long, Byte>>(); // <NodeIp, FragNum>
		public Set<BlockReply> fileReps;
		public String getDiff(long l1, long l2){
			return Long.toString(Math.abs(l2-l1));
		}
	}
}
