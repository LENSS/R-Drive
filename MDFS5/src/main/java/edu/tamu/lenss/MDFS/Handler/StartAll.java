package edu.tamu.lenss.MDFS.Handler;

import org.apache.log4j.Level;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.tamu.lenss.MDFS.PeerFetcher.PeerFetcher;


//this class is basically runs all the other necessary classes
public class StartAll {

	//logger
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(StartAll.class);

	private runGNSandRsock rungnsandrsock;
	private ExecutorService pool;
	private PeerFetcher PF;


	public StartAll(){

		//starts GNS(edgekeeper), and then rsock
		this.rungnsandrsock = new runGNSandRsock();

		//start thread pool
		pool = Executors.newCachedThreadPool();

		//start peer fetch thread
		PF = new PeerFetcher();
		PF.start();

		//start health status update thread
		//new Thread(new HealthStatusUpdate()).start();

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
		PF.interrupt();
		rungnsandrsock.stopAll();

		//log
		logger.log(Level.ALL, "MDFS has been stopped.");
	}


}

