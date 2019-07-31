package edu.tamu.lenss.mdfs.handler;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import edu.tamu.lenss.mdfs.GNS.GNS;
import edu.tamu.lenss.mdfs.MDFSDirectory;
import edu.tamu.lenss.mdfs.utils.Logger;

import static java.lang.Thread.sleep;


public class ServiceHelper {
	private static final String TAG = ServiceHelper.class.getSimpleName();
	
	/* Global Shared Instances */
	private static ServiceHelper instance = null;
	private static NetworkObserver netObserver;
	private static MDFSDirectory directory;
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

	public static void releaseService(){
		//unregister GNS
		boolean gnsUnreg = GNS.stop();
		if(instance != null ){ 
			Logger.v(TAG, "releaseService");
			directory.saveDirectory();
			if(connected)
				context.unbindService(mConnection);
			Intent intent = new Intent(context, NetworkObserver.class);
			context.stopService(intent);
			instance = null;
		}
	}

	public byte[] getEncryptKey() {
		return encryptKey;
	}

	public void setEncryptKey(byte[] encryptKey) {
		this.encryptKey = encryptKey;
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
