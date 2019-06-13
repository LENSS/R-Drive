package edu.tamu.lenss.mdfs.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.tamu.lenss.mdfs.Constants;
import edu.tamu.lenss.mdfs.models.BlockReply;
import edu.tamu.lenss.mdfs.models.BlockReq;
import edu.tamu.lenss.mdfs.utils.JCountDownTimer;

//this class is called for sending block req and receiving block reply
public class BlockReplyHandler {
	private Map<Long, BlockRepWaiter> runnerMap;  //<reqTime, BlockRepWaiter object>


	public BlockReplyHandler() {
		runnerMap = new HashMap<Long, BlockRepWaiter>();
	}

	//sends BlockReq
	public synchronized void sendBlockRequest(final BlockReq blockReq, final BlockRepListener lis) {
		//put reqTime and BlockRepWaiter in a map
		runnerMap.put(blockReq.getFileCreatedTime(), new BlockRepWaiter(blockReq, lis));

		//req sending
		PacketExchanger.getInstance().sendMsgContainer(blockReq);
	}

	//this function if called from netObserver.java when BlockReply is received
	//jumps into receiveBlockReply() function
	protected synchronized void receiveNewPacket(BlockReply rep) {
		// Make sure this is indeed the file fragment I'm waiting for
		if(runnerMap.containsKey(rep.getFileCreatedTime())){
			runnerMap.get(rep.getFileCreatedTime()).receiveBlockReply(rep);
		}
	}
	
	private class BlockRepWaiter{
		private BlockReq fileRequest;
		private JCountDownTimer timer;
		private BlockRepListener listener;
		private boolean waitingReply = false;
		private Set<BlockReply> replys = new HashSet<BlockReply>();
		
		
		private BlockRepWaiter(BlockReq fileReq, BlockRepListener lis){
			this.fileRequest = fileReq;
			this.listener = lis;
			this.timer = new JCountDownTimer(Constants.FILE_REQUEST_TIMEOUT, Constants.FILE_REQUEST_TIMEOUT) {
				@Override
				public void onTick(long millisUntilFinished) {}

				@Override
				public synchronized void onFinish() {
					if (waitingReply) {
						if (replys.isEmpty())
							listener.onError("Timeout. No fragments found.");
						else
							listener.onComplete(replys);
					}
					waitingReply = false;
					runnerMap.remove(fileRequest.getFileCreatedTime());
				}
			};
			
			waitingReply = true;
			replys.clear();
			timer.start();
		}
		
		protected synchronized void receiveBlockReply(BlockReply req) {
			replys.add(req);
			timer.cancel(); // Reset the timer
			timer.start(); 
		}
	}


	public interface BlockRepListener {
		public void onError(String msg);
		public void onComplete(Set<BlockReply> fileREPs);
	}
}
