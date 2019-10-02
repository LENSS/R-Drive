package edu.tamu.lenss.mdfs.Handler;

import org.apache.log4j.Level;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.lenss.mdfs.Commands.rm.rm;
import edu.tamu.lenss.mdfs.Utils.ScheduledTask;


//this class is basically runs all the other necessary classes
public class StartAll {

	//logger
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StartAll.class);

	private runGNSandRsock rungnsandrsock;
	private ScheduledTask scheduledTask;
	private ExecutorService pool;

	public StartAll(){

		//start everything
		this.rungnsandrsock = new runGNSandRsock();
		this.scheduledTask = new ScheduledTask();
		pool = Executors.newCachedThreadPool();
		scheduledTask.startAll();

		//log
		logger.log(Level.ALL, "MDFS has been started.");
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

		//log
		logger.log(Level.ALL, "MDFS has been stopped.");
	}


}

