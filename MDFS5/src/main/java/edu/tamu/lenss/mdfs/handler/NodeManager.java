package edu.tamu.lenss.mdfs.handler;

import android.content.Context;

import com.google.common.collect.HashBiMap;

import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

/**
 *  This class keep tracking the map from Mac to node IP
 * @author Jay
 */
public class NodeManager {
	private static final String TAG = NodeManager.class.getSimpleName();
	private final Context context;
	private long myIP, myMAC;
	
	public NodeManager(Context cont) {
		this.context = cont;
		this.myIP = IOUtilities.ipToLong(AndroidIOUtils.getWifiIP(context));
		this.myMAC = IOUtilities.mac2Long(AndroidIOUtils.getMyMACAdd(context));
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

	public static long getClasCBroadcastIpLong(String ipAddress){
		String brdIp = IOUtilities.parsePrefix(ipAddress.trim()) + "255";
		return IOUtilities.ipToLong(brdIp);
	}

}
