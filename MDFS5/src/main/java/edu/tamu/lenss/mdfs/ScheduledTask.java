package edu.tamu.lenss.mdfs;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 * This class is responsible for periodical background task
 * @author Jay
 */
public class ScheduledTask {
	private static final String TAG = ScheduledTask.class.getSimpleName();
	private ScheduledThreadPoolExecutor taskExecutor;
	public static final int SEND_NODEINFO_PERIOD = 30; 
	public static final int SEND_NODESTATUS_PERIOD = 60;
	public ScheduledTask(){
		
	}
	
	public void startAll(){		
		taskExecutor = new ScheduledThreadPoolExecutor(2);
		broadcastNodeInfo();
		updateNodeStatus();
	}
	
	public void stopAll(){
		taskExecutor.shutdown();
		Logger.i(TAG, "All scheduled tasks stopped");
	}
	
	private void broadcastNodeInfo(){
		Logger.i(TAG, "Start broadcast schedule");
		taskExecutor.scheduleAtFixedRate(new Runnable(){
    		@Override
    		public void run() {
    			ServiceHelper.getInstance().broadcastMyDirectory();
    			ServiceHelper.getInstance().getDirectory().saveDirectory();
    			//Logger.v(TAG, "Directory broadcasted");
    		}
    	}, SEND_NODEINFO_PERIOD, SEND_NODEINFO_PERIOD, TimeUnit.SECONDS);
	}
	
	private void updateNodeStatus(){
		Logger.i(TAG, "Start node status update");
		taskExecutor.scheduleAtFixedRate(new Runnable(){
    		@Override
    		public void run() {
    			ServiceHelper.getInstance().getNodeStatusMonitor().updateQ();
    			//Logger.v(TAG, ServiceHelper.getInstance().getNodeStatusMonitor().toString());
    		}
    	}, SEND_NODESTATUS_PERIOD, SEND_NODESTATUS_PERIOD, TimeUnit.SECONDS);
	}
}
