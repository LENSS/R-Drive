package edu.tamu.cse.lenss.MDFS5.Handler;

import android.app.Service;
import android.content.Context;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import edu.tamu.cse.lenss.MDFS5.Constants;
import edu.tamu.cse.lenss.MDFS5.Utils.MDFSDirectory;
import edu.tamu.cse.lenss.edgeKeeper.utils.EKUtils;

public class ServiceHelper {
	static Logger logger = Logger.getLogger(ServiceHelper.class);
	
	//Global Shared Instances
	private static ServiceHelper instance = null;
	private static MDFSDirectory directory;
	private byte[] encryptKey = new byte[32];
	private Context context;
	private static runGNSandRsock rungnsandrsock;


	private ServiceHelper(Context context, byte[] encryptkey) {

		this.context = context;
		this.setEncryptKey(encryptkey);

		//init log
		try {
			EKUtils.initLogger(Constants.MDFS_LOG_PATH, Level.ALL);
		} catch (IOException e) {
			System.out.println("Could not init log ");
		}

		//run gns and rsock
		//this.rungnsandrsock = new runGNSandRsock();


	}
	public static ServiceHelper fetchInstance(){
		return instance;
	}


	public static ServiceHelper getInstance(Context context, byte[] encryptkey) {
		if (instance == null) {
			instance = new ServiceHelper(context, encryptkey);
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

		//stop rsock and edgekeeper
		if(rungnsandrsock!=null){rungnsandrsock.stopAll();}

		//save directory and null servicehelper instance
		if(instance != null ){
			instance = null;
		}
	}

	public byte[] getEncryptKey() {
		return encryptKey;
	}

	private void setEncryptKey(byte[] encryptKey) {
		this.encryptKey = encryptKey;
	}


}
