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
		saveLocalMDFSDirectoryStateToDisk();
	}

	public void stopAll(){
		taskExecutor.shutdown();
	}

	private void saveLocalMDFSDirectoryStateToDisk(){
		taskExecutor.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				ServiceHelper.getInstance().getDirectory().saveDirectory();
			}
		}, SEND_NODEINFO_PERIOD, SEND_NODEINFO_PERIOD, TimeUnit.SECONDS);
	}


}
