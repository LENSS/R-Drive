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
	
	/* Global Shared Instances */
	private static ServiceHelper instance = null;
	private static NetworkObserver netObserver;
	private static MDFSDirectory directory;
	private byte[] encryptKey = new byte[32];
	
	private ServiceHelper() {
		this.netObserver = new NetworkObserver();
		this.directory = MDFSDirectory.readDirectory();
		this.directory.syncLocal();
	}
	
	public static ServiceHelper getInstance() {
		if (instance == null) {
			instance = new ServiceHelper();
		}
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

		//close netobserver
		netObserver.shutdown();

		//save directory and null servicehelper instance
		if(instance != null ){
			directory.saveDirectory();
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

}
