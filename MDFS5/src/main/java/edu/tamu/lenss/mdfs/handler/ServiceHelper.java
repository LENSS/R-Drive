package edu.tamu.lenss.mdfs.handler;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.GNS.GNSConstants;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.MDFSNodeStatusMonitor;
import edu.tamu.lenss.mdfs.handler.BlockReplyHandler.BlockRepListener;
import edu.tamu.lenss.mdfs.handler.JobProcessingHandler.JobRequestListener;
import edu.tamu.lenss.mdfs.handler.TopologyHandler.TopologyListener;
import edu.tamu.lenss.mdfs.models.AssignTaskReq;
import edu.tamu.lenss.mdfs.models.BlockReq;
import edu.tamu.lenss.mdfs.models.DeleteFile;
import edu.tamu.lenss.mdfs.models.JobReq;
import edu.tamu.lenss.mdfs.models.NewFileUpdate;
import edu.tamu.lenss.mdfs.utils.AndroidDataLogger;
import edu.tamu.lenss.mdfs.utils.Logger;

import static java.lang.Thread.sleep;


public class ServiceHelper {
	private static final String TAG = ServiceHelper.class.getSimpleName();
	
	/* Global Shared Instances */
	private static ServiceHelper instance = null;
	private static NetworkObserver netObserver;
	private static MDFSDirectory directory;
	private static AndroidDataLogger dataLogger;
	private static Context context;
	private byte[] encryptKey = new byte[32];
	
	private static volatile boolean connected = false;
	
	private ServiceHelper(final Context cont) {
		// start service here
		context = cont;
		Intent intent = new Intent(context, NetworkObserver.class);
		context.startService(intent);
		context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		directory = MDFSDirectory.readDirectory();
		directory.syncLocal();
		dataLogger = new AndroidDataLogger();
		dataLogger.init();
	}
	
	public static ServiceHelper getInstance(Context context) {
		if (instance == null) {
			instance = new ServiceHelper(context);
			Logger.v(TAG, "New Instance Created");
		}
		System.out.println("service Helper initialized");
		return instance;
	}
	
	public static synchronized ServiceHelper getInstance() {
		return instance;
	}
	
	//if need directory call this function to get a copy og MDFSDirectory..dont make a new object of MDFSdirectory
	public MDFSDirectory getDirectory() {
		return directory;
	}

	public static void setDirectory(MDFSDirectory directory) {
		ServiceHelper.directory = directory;
	}
	
	public AndroidDataLogger getDataLogger(){
		return dataLogger;
	}

	public static void releaseService(){
		//unregister GNS
		boolean gnsUnreg = GNS.gnsServiceClient.removeService(GNSConstants.GNS_s);
		if(instance != null ){ 
			Logger.v(TAG, "releaseService");
			directory.saveDirectory();
			if(connected)
				context.unbindService(mConnection);
			Intent intent = new Intent(context, NetworkObserver.class);
			context.stopService(intent);
			dataLogger.closeAllFiles();
			instance = null;
		}
	}

	////////////////////////////////////////////////////////////////////////

	public void broadcastJobRequest(JobReq jobReq, JobRequestListener lis){
		netObserver.getJobHandler().broadcastRequest(jobReq, lis);
	}
	
	public void sendAssignTaskReq(AssignTaskReq tasReq){
		netObserver.sendMsgContainer(tasReq);
	}
	
	public void startTopologyDiscovery(TopologyListener lis){
		netObserver.getTopologyHandler().broadcastRequest(lis);
	}
	
	//sends topology discovery requests
	//timeout = time in millisecond that I'm willing to wait for a successful topology reply
	public void startTopologyDiscovery(TopologyListener lis, long timeout){
		netObserver.getTopologyHandler().broadcastRequest(lis, timeout);
	}
	
	public void startBlockRequest(BlockReq request, BlockRepListener lis){
		netObserver.getBlockReplyHandler().sendBlockRequest(request, lis);
	}
	
	public void deleteFiles(DeleteFile files){
		netObserver.getDeleteFileHandler().sendFileDeletionPacket(files);
		netObserver.getDeleteFileHandler().processPacket(files);
	}
	
	public void sendFileUpdate(NewFileUpdate update){
		netObserver.sendMsgContainer(update);
	}

	public byte[] getEncryptKey() {
		return encryptKey;
	}

	public void setEncryptKey(byte[] encryptKey) {
		this.encryptKey = encryptKey;
	}

	public NodeManager getNodeManager(){
		return netObserver.getNodeManager();
	}
	
	public MDFSNodeStatusMonitor getNodeStatusMonitor(){
		return netObserver.getNodeStatusMonitor();
	}
	

	//periodically called by scheduledTask.java to send out NEW_FILE_UPDATES for each files in the directory
	public void broadcastMyDirectory(){
		getDirectory().broadcastMyDirectory();
	}
	
	//submit a task to executorService
	public void executeRunnableTask(Runnable task){
		netObserver.executeRunnableTask(task);
	}
	
	//submit a callable task to ExecutorService
	public Future<?> submitCallableTask(Callable<?> task){
		return netObserver.submitCallableTask(task);
	}
	
	/////////////////////////////////////////////////////////////////////
	
	private static final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			netObserver = ((NetworkObserver.LocalBinder) iBinder).getService();
			Logger.i(TAG, "Service Connected!");
			netObserver.init();
			connected = true;
			//service.init();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			Logger.i(TAG, "Service Disonnected!");
			connected = false;
		}
	};
}
