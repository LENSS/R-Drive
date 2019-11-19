package edu.tamu.lenss.MDFS.Handler;

import org.apache.log4j.Level;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.lenss.MDFS.mdfs_api_server.MDFS_API_SERVER;


//this class is basically runs all the other necessary classes
public class StartAll {

	//logger
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StartAll.class);

	private runGNSandRsock rungnsandrsock;
	private ExecutorService pool;
	private Thread mdfs_api_server;

	public StartAll(){

		//start rsock
		this.rungnsandrsock = new runGNSandRsock();

		//start thread pool
		pool = Executors.newCachedThreadPool();

		//start health status update thread
		//new Thread(new HealthStatusUpdate()).start();

		//start MDFS_API thread
		this.mdfs_api_server = new Thread(new MDFS_API_SERVER());
		this.mdfs_api_server.start();

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

