package edu.tamu.lenss.mdfs.Handler;

import org.apache.log4j.Level;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


//this class is basically runs all the other necessary classes
public class StartAll {

	//logger
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StartAll.class);

	private runGNSandRsock rungnsandrsock;
	private ExecutorService pool;

	public StartAll(){

		//start everything
		this.rungnsandrsock = new runGNSandRsock();
		pool = Executors.newCachedThreadPool();

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
		rungnsandrsock.stopAll();

		//log
		logger.log(Level.ALL, "MDFS has been stopped.");
	}


}

