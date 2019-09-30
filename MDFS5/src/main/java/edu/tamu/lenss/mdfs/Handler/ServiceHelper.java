package edu.tamu.lenss.mdfs.Handler;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;
import edu.tamu.lenss.mdfs.Utils.MDFSDirectory;

public class ServiceHelper {
	static Logger logger = Logger.getLogger(ServiceHelper.class);
	
	//Global Shared Instances
	private static ServiceHelper instance = null;
	private static StartAll startAll;
	private static MDFSDirectory directory;
	private byte[] encryptKey = new byte[32];
	
	private ServiceHelper() {

		//init log
		try {
			EKUtils.initLogger("/storage/emulated/0/MDFS/mdfs_log.log", Level.ALL);
		} catch (IOException e) {
			System.out.println("Could not init log ");
		}

		//start all
		this.startAll = new StartAll();
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

		//close netobserver
		startAll.shutdown();

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
		startAll.executeRunnableTask(task);
	}
	
	//submit a callable task to ExecutorService
	public Future<?> submitCallableTask(Callable<?> task){
		return startAll.submitCallableTask(task);
	}

}
