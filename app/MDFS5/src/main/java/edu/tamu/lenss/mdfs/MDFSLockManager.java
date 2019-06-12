package edu.tamu.lenss.mdfs;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.PowerManager;

public class MDFSLockManager {
	private PowerManager pm; 
	private PowerManager.WakeLock wl = null;
	private WifiManager wifiManager;
	private WifiManager.WifiLock wifiLock = null;
	private WifiManager.MulticastLock multicastLock = null;
	
	public MDFSLockManager(Context context){
		
		pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MDFS_SERVICE"); // SCREEN_DIM_WAKE_LOCK PARTIAL_WAKE_LOCK
		
		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		wifiLock = wifiManager.createWifiLock("MDFS_WIFI_LOCK");
		wifiLock.setReferenceCounted(false);
		multicastLock = wifiManager.createMulticastLock("MDFS_MULTICAST_LOCK");
		multicastLock.setReferenceCounted(false);
	}
	
	public void enableAllLock(){
		wl.acquire();
		wifiLock.acquire();
		multicastLock.acquire();
	}
	
	public void disableAllLock(){
		if(wl != null && wl.isHeld())
			wl.release(); 
		if(wifiLock != null && wifiLock.isHeld())
			wifiLock.release();
		if(multicastLock != null && multicastLock.isHeld())
			multicastLock.release();
	}

}
