package edu.tamu.lenss.mdfs.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Pair;
import android.widget.Toast;

import org.apache.log4j.Level;

import edu.tamu.cse.lenss.gnsService.server.GNSServiceUtils;
import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.FragExchangeHelper;
import edu.tamu.lenss.mdfs.MDFSBlockRetriever;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.MDFSLockManager;
import edu.tamu.lenss.mdfs.MDFSNodeStatusMonitor;
import edu.tamu.lenss.mdfs.ScheduledTask;
import edu.tamu.lenss.mdfs.handler.PacketExchanger.DataToObserver;
import edu.tamu.lenss.mdfs.models.AssignTaskReq;
import edu.tamu.lenss.mdfs.models.BlockReply;
import edu.tamu.lenss.mdfs.models.BlockReq;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.FragmentTransferInfo;
import edu.tamu.lenss.mdfs.models.JobReply;
import edu.tamu.lenss.mdfs.models.JobReq;
import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.models.MDFSPacketType;
import edu.tamu.lenss.mdfs.models.MDFSTCPHeader;
import edu.tamu.lenss.mdfs.models.NewFileUpdate;
import edu.tamu.lenss.mdfs.models.NodeInfo;
import edu.tamu.lenss.mdfs.models.TaskResultInfo;
import edu.tamu.lenss.mdfs.models.TopologyDiscovery;
import edu.tamu.lenss.mdfs.network.ConnectionMonitor;
import edu.tamu.lenss.mdfs.network.ConnectionMonitor.ConnectionMonitorListener;
import edu.tamu.lenss.mdfs.network.TCPReceive.TCPReceiverData;
import edu.tamu.lenss.mdfs.pdu.MessageContainer;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;


//this class is the first class that is run as a service.
//this class is basically the execution starting point of mdfs library
public class NetworkObserver extends Service implements Observer {
	private static final String TAG = NetworkObserver.class.getSimpleName();
	private static final String SCREEN_STATE_LISTENER = "screen_state";
	private static final String WIFI_STATE_LISTENER = "wifi_state";
	private boolean firstStarted = false;
	private PacketExchanger pktExchanger;
	private NodeManager nodeManager;
	private FragExchangeHelper fragDownloadHelper = new FragExchangeHelper();
	private TopologyHandler topologyHandler = new TopologyHandler();
	private BlockRequestHandler fileReqHandler = new BlockRequestHandler();
	private BlockReplyHandler fileRepHandler = new BlockReplyHandler();
	private JobProcessingHandler jobHandler = new JobProcessingHandler();
	private DeleteFileHandler deleteFileHandler = new DeleteFileHandler();
	private ScheduledTask scheduledTask = new ScheduledTask();
	private ConnectionMonitor connMonitor;
	private MDFSLockManager lockManager;
	private MDFSNodeStatusMonitor nodeStatusMonitor;
	private SharedPreferences SP;
	
	private ExecutorService pool;	// This pool is shared and used by many classes in MDFS library
	
	private final IBinder mBinder = new LocalBinder();


	/**
	 * Initialize the NetworkObserver.
	 * This function should be called after the service has been started or connected 
	 * because some objects in the init() will also call this Service. If they make the call before
	 * the service is completely started, NullPointer exception occurs 
	 */
	public void init(){
		System.out.println("onetime netObserver gets called!");
		nodeManager = new NodeManager(this);
		EdgeKeeperConstants.my_wifi_ip_temp = AndroidIOUtils.getWifiIP(this);
		pktExchanger = PacketExchanger.getInstance(AndroidIOUtils.getWifiIP(this));
		if(pktExchanger == null){
			stopSelf();
			Logger.e(TAG, "Can't start Network Server");
			return;
		}
		else{
			pktExchanger.addObserver(this);
		}
		pktExchanger.startThread();
		pool = Executors.newCachedThreadPool(); 
		scheduledTask.startAll();
		connMonitor = new ConnectionMonitor(this, connListener);
		connMonitor.startMonitor();
		lockManager = new MDFSLockManager(this);
		//lockManager.enableAllLock();
		nodeStatusMonitor = new MDFSNodeStatusMonitor(this);
		SP = PreferenceManager.getDefaultSharedPreferences(this);
		firstStarted = true;

		//log4j used by rsockJavaAPI and GNS
		try {
			GNSServiceUtils.initLogger(Environment.getExternalStorageDirectory() + "/someLog", Level.ALL);
		} catch (IOException e) {
			System.out.println("Can not create log file probably due to insufficient permission");
		}

		IntentFilter screenStateFilter = new IntentFilter(SCREEN_STATE_LISTENER);
		IntentFilter wifiStateFilter = new IntentFilter(WIFI_STATE_LISTENER);
		screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
		screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
		wifiStateFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
	}
	
	protected NodeManager getNodeManager(){
		return nodeManager;
	}
	
	protected MDFSNodeStatusMonitor getNodeStatusMonitor(){
		return nodeStatusMonitor;
	}
	
	protected TopologyHandler getTopologyHandler(){
		return topologyHandler;
	}
	
	protected JobProcessingHandler getJobHandler(){
		return jobHandler;
	}
	
	
	protected BlockReplyHandler getBlockReplyHandler(){
		return fileRepHandler;
	}
	
	protected DeleteFileHandler getDeleteFileHandler(){
		return deleteFileHandler;
	}
	
	protected ConnectionMonitor getConnectionMonitor(){
		return connMonitor;
	}

	
	/**
	 * Use PacketExchanger to send a UDP packet
	 * @param packet
	 */
	protected void sendMsgContainer(MessageContainer packet){
		pktExchanger.sendMsgContainer(packet);
	}
	
	
	/**
	 * Submit a task to ExecutorService
	 * @param
	 */
	protected void executeRunnableTask(Runnable task){
		pool.execute(task);
	}
	
	protected Future<?> submitCallableTask(Callable<?> task){
		return pool.submit(task);
	}


	@Override
	public void update(Observable observable, Object arg) {
		/*
		 * The work in this update() method should be completed ASAP. It blocks the Observable object.
		 * Should execute every task in a Thread.
		 */
		if( arg instanceof TCPReceiverData){
			final TCPReceiverData data = (TCPReceiverData)arg;
			Logger.v(TAG, "New incoming TCP");
			pool.execute(new Runnable(){
				@Override
				public void run() {
					try {
						// We assume the input/output stream will be closed properly
						ObjectInputStream ois = new ObjectInputStream(data.getDataInputStream());
						MDFSTCPHeader header = (MDFSTCPHeader)ois.readObject();
						switch(header.getTcpHeaderType()){
						case MDFSTCPHeader.TYPE_FRAGMENT:
							fragDownloadHelper.newFragTransfer(data, (FragmentTransferInfo)header);
							break;
						case MDFSTCPHeader.TYPE_TASK_RESULT:
							jobHandler.receiveTaskResult(data,(TaskResultInfo)header );
							break;
						default:
							Logger.w(TAG, "Unrecognized Incoming TCP Packet");
						}
						ois.close();
					} catch (StreamCorruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} finally{
						
					}
				}
			});
		}
		else if (arg instanceof DataToObserver){
			try{
				DataToObserver msg = (DataToObserver)arg;
				MessageContainer container = (MessageContainer)msg.getContainedData();
				Logger.v(TAG, "New incoming UDP Packet Type: " + container.getPacketTypeReadable());
				switch(container.getPacketType()){
				case MDFSPacketType.JOB_REQUEST:
					final JobReq jobReq = (JobReq)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							jobHandler.processRequest(jobReq);
						}
					});
					break;
				case MDFSPacketType.JOB_REPLY:
					final JobReply jobReply = (JobReply)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							jobHandler.processReply(jobReply);
						}
					});
					break;
				case MDFSPacketType.ASSIGN_TASK_REQUEST:
					final AssignTaskReq assignReq = (AssignTaskReq)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							jobHandler.processAssignTask(getBaseContext(), assignReq);
						}
					});
					break;	
				case MDFSPacketType.TOPOLOGY_DISCOVERY:
					final TopologyDiscovery top = (TopologyDiscovery)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							topologyHandler.receiveTopologyDiscovery(top);
						}
					});
					showToast("TopDisc: from " + IOUtilities.long2Ip(top.getSource()));
					Logger.i(TAG, "TopDisc: from " + IOUtilities.long2Ip(top.getSource()));
					break;
				case MDFSPacketType.NODE_INFO:
					final NodeInfo info = (NodeInfo)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							topologyHandler.receiveNewPacket(info);
						}
					});
					showToast("NodeInfo: from " + IOUtilities.long2Ip(info.getSource()));
					Logger.i(TAG, "NodeInfo: from " + IOUtilities.long2Ip(info.getSource()));
					break;
				case MDFSPacketType.NEW_FILE_UPDATE:
					final NewFileUpdate dirUpdate = (NewFileUpdate)msg.getContainedData();
					final MDFSDirectory dir = ServiceHelper.getInstance().getDirectory();
					dir.addFile(dirUpdate.getFileInfo());
					// update directory  //todo: comment this block
					if(SP.getBoolean("aggressivenode", false)){
						pool.execute(new Runnable(){
							@Override
							public void run() {
								Set<Pair<Long, Byte>> blocks = dir.getUncachedBlocks();
								Logger.i(TAG, blocks.size() + " uncached blocks");
								for(Pair<Long, Byte> pair : blocks){
									if(dir.isBlockDownloading(pair.first, pair.second))
										continue;
									MDFSFileInfo fInfo = dir.getFileInfo(pair.first);
									if(fInfo == null)
										continue;
									MDFSBlockRetriever retriever = new MDFSBlockRetriever(fInfo, pair.second);
									retriever.setDecryptKey(ServiceHelper.getInstance().getEncryptKey());
									retriever.start();
									Logger.i(TAG, "Start downloading uncached blocks");
									try {
										long blkSize = (long)((double)fInfo.getFileSize()/fInfo.getNumberOfBlocks());
										Thread.sleep(blkSize*8); // Assume each byte takes 8 milliseconds. 
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
						});
					}
					Logger.i(TAG, "dirUpdate: from " + IOUtilities.long2Ip(dirUpdate.getSource()));
					break;
				case MDFSPacketType.BLOCK_REQ:
					final BlockReq blockReq = (BlockReq)msg.getContainedData();
					Logger.i(TAG, "BlockRep: from " + IOUtilities.long2Ip(blockReq.getSource()));
					pool.execute(new Runnable(){
						@Override
						public void run() {
							fileReqHandler.processRequest(blockReq);
						}
					});
					break;
				case MDFSPacketType.BLOCK_REPLY:
					final BlockReply blockRep = (BlockReply)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							fileRepHandler.receiveNewPacket(blockRep);
						}
					});
					showToast("BlockRep: from " + IOUtilities.long2Ip(blockRep.getSource()));
					Logger.i(TAG, "BlockRep: from " + IOUtilities.long2Ip(blockRep.getSource()));
					break;
				case MDFSPacketType.DELETE_FILE:
					final DeleteFile deleteFile = (DeleteFile)msg.getContainedData();
					pool.execute(new Runnable(){
						@Override
						public void run() {
							deleteFileHandler.processPacket(deleteFile);
						}
					});
					showToast("DelFile: from " + IOUtilities.long2Ip(deleteFile.getSource()));
					Logger.i(TAG, "DelFile: from " + IOUtilities.long2Ip(deleteFile.getSource()));

					break;
				default:
					Logger.w(TAG, "Unrecognized Incoming UDP Packet");
					return;
				}
			}
			catch(ClassCastException e){
				e.printStackTrace();
				Logger.e(TAG, e.toString());
			}
		}
	}
	
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
		//init() will be called by the process that starts the service.
		return START_STICKY;
	}
	
	public void shutdown(){
		pktExchanger.deleteObserver(this);
		pktExchanger.shutdown(); // It calls TCPConnection.stopAllTCP();
		pool.shutdown();
		scheduledTask.stopAll();
		connMonitor.stopMonitoring();
		lockManager.disableAllLock();
	}
	
	public void stopNetwork(){
		pktExchanger.deleteObserver(this);
		pktExchanger.stopThread();	// It calls TCPConnection.stopAllTCP();
		scheduledTask.stopAll();
	}
	
	public void restartNetwork(){
		nodeManager.resetAll();
		pktExchanger.resetAll();
		pktExchanger.addObserver(this);	// The listener may has been removed by stopNetwork();
		scheduledTask.stopAll();
		scheduledTask.startAll();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class LocalBinder extends Binder {
    	NetworkObserver getService() {
            return NetworkObserver.this;
        }
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
		shutdown();
		Logger.i(TAG, "NetworkObserver Service terminates");
	}
	
	private ConnectionMonitorListener connListener = new ConnectionMonitorListener() {
		@Override
		public void meshConnectionChanged(boolean isConnected) {
			if(isConnected){
				// re-start network activity
				if(!firstStarted)
					restartNetwork();
				firstStarted = false;
			}
			else{
				// Stop network activity
				stopNetwork();
			}
		}
	};
	
	private Handler uiHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			String str = (String)msg.obj;
			Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
		}
	};
	private void showToast(String msg){
		
		if(SP.getBoolean("debugtoast", false)){
			Message m = new Message();
			m.obj = msg;
			uiHandler.sendMessage(m);
		}
		// Logger.i(TAG, msg)
	}
	
}

