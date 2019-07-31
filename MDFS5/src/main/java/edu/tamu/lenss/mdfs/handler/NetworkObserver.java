package edu.tamu.lenss.mdfs.handler;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;

import android.os.IBinder;

import edu.tamu.lenss.mdfs.EdgeKeeper.EdgeKeeperConstants;
import edu.tamu.lenss.mdfs.ScheduledTask;
import edu.tamu.lenss.mdfs.models.runGNSandRsock;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;


//this class is the first class that is run as a service.
//this class is basically the networking execution starting point of mdfs library
public class NetworkObserver{
	private static final String TAG = NetworkObserver.class.getSimpleName();
	private boolean firstStarted = false;
	private runGNSandRsock rungnsandrsock;
	private ScheduledTask scheduledTask;
	private ExecutorService pool;

	public NetworkObserver(){
		this.rungnsandrsock = new runGNSandRsock();
		this.scheduledTask = new ScheduledTask();
		pool = Executors.newCachedThreadPool();
		scheduledTask.startAll();
		firstStarted = true;
	}


	protected void executeRunnableTask(Runnable task){
		pool.execute(task);
	}
	
	protected Future<?> submitCallableTask(Callable<?> task){
		return pool.submit(task);
	}

	
	public void shutdown(){
		pool.shutdown();
		scheduledTask.stopAll();
		rungnsandrsock.stopAll();
	}


}

