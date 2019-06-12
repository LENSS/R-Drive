package edu.tamu.lenss.mdfs.handler;

import android.content.Context;

import com.google.common.collect.HashBiMap;

import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 *  This class keep tracking the map from Mac (unique identifier of each node) to node IP
 * @author Jay
 */
public class NodeManager {
	private static final String TAG = NodeManager.class.getSimpleName();
	private final Context context;
	private long myIP, myMAC;
	private HashBiMap<Long, Long> macIpMap;
	
	public NodeManager(Context cont) {
		this.context = cont;
		this.myIP = IOUtilities.ipToLong(AndroidIOUtils.getWifiIP(context));
		this.myMAC = IOUtilities.mac2Long(AndroidIOUtils.getMyMACAdd(context));
		this.macIpMap = HashBiMap.create();
		Logger.d(TAG, "IP: " + IOUtilities.long2Ip(myIP));
		Logger.d(TAG, "MAC: " + AndroidIOUtils.getMyMACAdd(context));
	}
	
	/**
	 * Create a new entry if it does not exit. Update an entry if it already exists.
	 * @param mac
	 * @param ip
	 */
	public void addEntry(long mac, long ip){
		macIpMap.forcePut(mac, ip);
	}
	
	public String getIpByMacLong(long mac){
		Long ip = macIpMap.get(mac);
		if(ip != null)
			return IOUtilities.long2Ip(ip);
		else
			return null;
	}
	
	public String getMacByIpLong(long ip){
		Long mac = macIpMap.inverse().get(ip);
		if(mac != null)
			return IOUtilities.long2mac(mac);
		else
			return null;
	}
	
	/**
	 * Update my MAC and IP. Clear the entire Map
	 */
	public void resetAll(){
		this.myIP = IOUtilities.ipToLong(AndroidIOUtils.getWifiIP(context));
		this.myMAC = IOUtilities.mac2Long(AndroidIOUtils.getMyMACAdd(context));
		macIpMap.clear();
	}
	
	public long getMyIP() {
		return myIP;
	}
	
	public String getMyIpString(){
		return IOUtilities.long2Ip(myIP);
	}

	public void setMyIP(long myIP) {
		this.myIP = myIP;
	}

	public long getMyMAC() {
		return myMAC;
	}
	
	public String getMyMacString(){
		return IOUtilities.long2mac(myMAC);
	}

	public void setMyMAC(long myMAC) {
		this.myMAC = myMAC;
	}
	
	public static long getClasCBroadcastIpLong(String ipAddress){
		String brdIp = IOUtilities.parsePrefix(ipAddress.trim()) + "255";
		return IOUtilities.ipToLong(brdIp);
	}

}
