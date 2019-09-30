package edu.tamu.lenss.mdfs.Handler;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.lenss.mdfs.Utils.ScheduledTask;


//this class is basically runs all the other necessary classes
public class StartAll {
	private runGNSandRsock rungnsandrsock;
	private ScheduledTask scheduledTask;
	private ExecutorService pool;

	public StartAll(){
		this.rungnsandrsock = new runGNSandRsock();
		this.scheduledTask = new ScheduledTask();
		pool = Executors.newCachedThreadPool();
		scheduledTask.startAll();
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

