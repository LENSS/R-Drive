package edu.tamu.lenss.mdfs;

import java.util.LinkedList;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.os.BatteryManager;

public class MDFSNodeStatusMonitor {
	private static final int MY_UID = android.os.Process.myUid();
	private static final int TRAFFIC_Q_SIZE = 100;
	private final Context context;
	private long bytesProcessed; 
	private IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	
	private LinkedList<NetworkCPURecord> loadLL;
	
	public MDFSNodeStatusMonitor(Context context){
		this.context = context;
		loadLL = new LinkedList<NetworkCPURecord>();
		resetLoadRecordQ();
	}
	
	public void resetLoadRecordQ(){
		synchronized(loadLL){
			loadLL.clear();
			for(int i=0; i<TRAFFIC_Q_SIZE; i++)
				loadLL.add(NetworkCPURecord.create(0, 0, 0, 0));
			bytesProcessed = 0;
		}
	}
	
	/**
	 * 
	 * @return  The current battery charge percentage
	 */
	public float getBatteryLevel(){
		Intent batteryStatus = context.registerReceiver(null, batteryFilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		return level / (float)scale;
	}
	
	public synchronized void updateQ(){
		loadLL.removeLast();
		loadLL.addFirst(NetworkCPURecord.create(TrafficStats.getUidTxBytes(MY_UID),
				TrafficStats.getUidRxBytes(MY_UID), bytesProcessed,
				System.currentTimeMillis()));
		//Logger.v("MDFSNodeStatusMonitor", TrafficStats.getUidRxBytes(MY_UID) + ", " + + TrafficStats.getTotalRxBytes());
	}
	
	
	/**
	 * Get the number of bytes sent in the past timePeriod
	 * @param timePeriod
	 * @return
	 */
	public synchronized long getBytesSent(int timePeriod){
		long curSentBytes = TrafficStats.getUidTxBytes(MY_UID);
		int targetIndex = findClosestIndexByTime(timePeriod);
		return curSentBytes-loadLL.get(targetIndex).sentBytes;
	}
	
	/**
	 * Get the number of bytes received in the past timePeriod
	 * @param timePeriod
	 * @return
	 */
	public synchronized long getBytesReceived(int timePeriod){
		long curReceivedBytes = TrafficStats.getUidRxBytes(MY_UID);
		int targetIndex = findClosestIndexByTime(timePeriod);
		return curReceivedBytes-loadLL.get(targetIndex).receivedBytes;
	}
	
	/**
	 * Get the number of bytes processed in the past timePeriod
	 * @param timePeriod
	 * @return
	 */
	public synchronized long getBytesProcessed(int timePeriod){
		int targetIndex = findClosestIndexByTime(timePeriod);
		return bytesProcessed-loadLL.get(targetIndex).processedBytes;
	}
	
	/**
	 * Get the number of bytes received and transmitted in the past timePeriod
	 * @param timePeriod
	 * @return
	 */
	public synchronized long getTotalBytesTransfered(int timePeriod){
		long curReceivedBytes = TrafficStats.getUidRxBytes(MY_UID);
		long curSentBytes = TrafficStats.getUidTxBytes(MY_UID);
		int targetIndex = findClosestIndexByTime(timePeriod);
		
		return curReceivedBytes + curSentBytes - loadLL.get(targetIndex).receivedBytes - loadLL.get(targetIndex).sentBytes;
	}
	
	private int findClosestIndexByTime(long timePeriod){
		long targetTime = System.currentTimeMillis() - timePeriod*1000;
		int i=0;
		for(i=0; i < TRAFFIC_Q_SIZE; i++){
			if(loadLL.get(i).time <= targetTime)
				return i;
		}
		return i-1;	// The last element of the linked list
	}
	
	public void incrementProcessedBytes(long increment){
		bytesProcessed += increment;
	}
	
	public long getBytesProcessed(){
		return bytesProcessed;
	}
		
	private static class NetworkCPURecord{
		public final long sentBytes, receivedBytes, processedBytes, time;
		
		public NetworkCPURecord(long sent, long received, long processed, long t){
			this.sentBytes = sent;
			this.receivedBytes = received;
			this.processedBytes = processed;
			this.time = t;
		}
		
		public static NetworkCPURecord create(long sent, long received, long processed, long t){
			return new NetworkCPURecord(sent, received, processed, t);
		}
	}
	
	/**
	 * Return an integer between 0-100
	 * @return
	 */
	public byte getFailureProbability(){
		// evaluate the probability of failure by the deadline
		// based on the battery level and historical trace (Wi-Fi signal)
		return (byte)Math.round(getBatteryLevel()*100);
		
	}

	@Override
	public String toString() {
		StringBuilder status = new StringBuilder();
		status.append("Process ID: " + MY_UID + "\n");
		status.append("Battery: " + getBatteryLevel() + "\n");
		status.append("Received Bytes: " + getBytesReceived(30) + "\n");
		status.append("Sent Bytes: " + getBytesSent(30) + "\n");
		status.append("Processed Bytes: " + getBytesProcessed(30) + "\n");
		return status.toString();
	}
	
}
