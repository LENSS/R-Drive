package edu.tamu.lenss.mdfs.network;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import edu.tamu.lenss.mdfs.handler.ServiceHelper;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.Logger;

public class ConnectionMonitor {
	private static final String TAG = ConnectionMonitor.class.getSimpleName();
	private static final String WIFI_STATE_LISTENER = "wifi_state";
	private static final int WIFI_SIGNAL_LEVEL = 5;
	private boolean isMeshConnected = false;
	private Context context;
	private MyReceiver myReceiver;
	private WifiManager wifiManager;
	private List<ScanResult> wifiScanResultList;
	private PriorityQueue<ScanResult> meshAP; // The strongest Mesh AP is put at the head
	private ConnectionMonitorListener connListener;
	private String bestSSID;
	private int rssiCompCount = 0;		// Count the number of times that one AP is better than mine
	
	
	
	public ConnectionMonitor(Context cont) {
		this(cont, null);
	}
	
	public ConnectionMonitor(Context cont, ConnectionMonitorListener listener){
		this.context = cont;
		myReceiver = new MyReceiver();
		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		meshAP = new PriorityQueue<ScanResult>(10, new PQsort()); // Mesh size is less than 1
		this.connListener = listener;
	}
	
	public void startMonitor(){
		
		IntentFilter wifiStateFilter = new IntentFilter(WIFI_STATE_LISTENER);
		//wifiStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION); // SUPPLICANT_CONNECTION_CHANGE_ACTION, NETWORK_STATE_CHANGED_ACTION
		//wifiStateFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		wifiStateFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		wifiStateFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION); // Scan for neighbor WiFi
		wifiManager.startScan();
		
		context.registerReceiver(myReceiver, wifiStateFilter);
	}
	
	public void stopMonitoring(){
		context.unregisterReceiver(myReceiver);
	}
	
	/**
	 * Return the current WiFi signal ratio from 0-4. 0 is the lowest, and 4 is the hightest
	 * @return
	 */
	public int getWifiSignalStrength(){
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo != null)
			return WifiManager.calculateSignalLevel(wifiInfo.getRssi(), WIFI_SIGNAL_LEVEL);
		else
			return -1;
	}
	
	private String getWifiSSID(){
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo != null){
			return wifiInfo.getSSID();
		}
		else
			return null;
	}
	
	/**
	 * Check if a specific AP is already added to this phone. 
	 * @param SSID
	 * @return the ID of this AP if this AP has record, otherwise, return -1 
	 */
	private int isAPadded(String SSID) {
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		if(list != null){
			for (WifiConfiguration i : list) {
				if (i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
					return i.networkId;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Current support only Open AP or AP encrypted with WAP only
	 * @param apSSID
	 * @param apPwd
	 */
	private void connect2AP(String apSSID, String apPwd){
		Logger.v(TAG, "Connecting to " + apSSID);
		int networkId = isAPadded(apSSID);
		if(networkId == -1){
			// A new AP for this phone
			WifiConfiguration conf = new WifiConfiguration();
			conf.SSID = "\"" + apSSID + "\"";   // Please note the quotes. String should contain ssid in quotes
			
			if(apPwd != null){
				conf.wepKeys[0] = "\"" + apPwd + "\""; 
				conf.wepTxKeyIndex = 0;
				conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				conf.preSharedKey = "\""+ apPwd +"\"";
			}
			else{
				conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			}
			networkId=wifiManager.addNetwork(conf);
		}
		
		// Automatically connect to best Mesh AP
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(context);
		if(SP.getBoolean("autoconnect", true)){
			wifiManager.disconnect();
			if(isMeshConnected && connListener != null){
	    		connListener.meshConnectionChanged(false);
	    		isMeshConnected = false;
			}
			wifiManager.enableNetwork(networkId, true);
			wifiManager.reconnect();
		}
	}
	

	/**
	 * This function check if this node needs to connect to a different AP <Br>
	 * If I'm currently connecting to an AP belonging to the Mesh, and this AP has the strongest signal, then do nothing. <Br>
	 * Otherwise, check if there is an recognized AP with good signal that I can connect to. 
	 */
	private void connectionCheck(){
		String curSSID;
		String curBestSSID  = meshAP.peek().SSID;
		boolean wifiConnected = AndroidIOUtils.isConnectedToWifi(context);
		
		if(bestSSID == null) // Initialization
			bestSSID = curBestSSID;
		
		if(wifiConnected){
			curSSID = getWifiSSID();
			Logger.v(TAG, "Current SSID: " + curSSID + "  Best SSID: " + curBestSSID);
			if(curSSID.compareToIgnoreCase(curBestSSID) == 0){
				// Do nothing. We are good
			}
			else {
				// There is a better AP that we can connect to
				int bestRSSI = WifiManager.calculateSignalLevel(meshAP.peek().level, WIFI_SIGNAL_LEVEL);
				Logger.v(TAG, "Current AP SNR: " + getWifiSignalStrength() + " Best AP SNR: " + bestRSSI);
				// If I have seen this good AP repeatedly, than it is truly a stable and good AP
				if(curBestSSID.compareToIgnoreCase(bestSSID) == 0 && bestRSSI > getWifiSignalStrength()){
					if(++rssiCompCount > 1){
						// Re-associate to best SSID
						connect2AP(curBestSSID, null);
						rssiCompCount = 0;
					}				
				}
				else
					rssiCompCount = 0;
			}
		}
		else{
			// Try to connect to the best SSID
			connect2AP(curBestSSID, null);
			rssiCompCount = 0;
		}
		bestSSID = curBestSSID;
	}
	
	private class MyReceiver extends BroadcastReceiver {
		private String lastKnownSSID = null;
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE); 
    	        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
    	        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI && netInfo.isConnected() ) {
    	        	String ssid = getWifiSSID();
    	        	if(lastKnownSSID == null)
    	        		lastKnownSSID = ssid;
    	        	Logger.i(TAG, "Have Wifi Connection");
    	        	Logger.i(TAG, "SSID: " + ssid + ", Signal Level: " + getWifiSignalStrength());
    	        	// Either wifi network is restarted or switched. When WiFi network is switched, the "Don't have Wifi Connection" condtion is NOT called
    	        	if( (!isMeshConnected || ssid.compareTo(lastKnownSSID)!=0) 
    	        			&& connListener != null)
    	        		connListener.meshConnectionChanged(true);
    	        	isMeshConnected = true;
    	        	lastKnownSSID = ssid;
    	        	Logger.i(TAG, "Current IP: " + ServiceHelper.getInstance().getNodeManager().getMyIpString());
					Intent setip = new Intent("current_ip");
					// Adding some data
					setip.putExtra("message", ServiceHelper.getInstance().getNodeManager().getMyIpString());
					LocalBroadcastManager.getInstance(context).sendBroadcast(setip);
					
    	        }
    	        else {
    	        	Logger.w(TAG, "Don't have Wifi Connection");
    	        	if(isMeshConnected && connListener != null)
    	        		connListener.meshConnectionChanged(false);
    	        	
    	        	isMeshConnected = false;
    	        }
	        }
	        else if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
	        	wifiScanResultList = wifiManager.getScanResults();
	        	meshAP.clear();
	        	for(ScanResult oneAP : wifiScanResultList){
	        		if(oneAP.SSID.contains(Constants.MESH_AP_KEYWORD) && !oneAP.SSID.toLowerCase().contains("test")){ 
	        			meshAP.add(oneAP); 
	        		}
	        	}
	        	if(!meshAP.isEmpty())
	        		connectionCheck();
	        }
	    }
	}
	
	
	/**
	 * Priority queue for scanned AP
	 * @author Jay
	 *
	 */
	private class PQsort implements Comparator<ScanResult> {
		 
		public int compare(ScanResult one, ScanResult two) {
			return WifiManager.compareSignalLevel(one.level, two.level)*-1; // Inverse so that head is the biggest (Strongest SSID)
		}
	}
	
	public interface ConnectionMonitorListener{
		public abstract void meshConnectionChanged(boolean isConnected);
	}
}
