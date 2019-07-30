package edu.tamu.lenss.mdfs.utils;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.TypedValue;

public class AndroidIOUtils {


	//Create a File handler to the specified path on SD Card
	public static File getExternalFile(String path) {
		return new File(Environment.getExternalStorageDirectory(), path);
	}

	//Return the MAC Address of this device.
	//Note: Require <uses-permission>
	//android:name="android.permission.ACCESS_WIFI_STATE"/>.
	// It only works when WIFI is connected.
	public static String getMyMACAdd(Context cont) {
		WifiManager manager = (WifiManager) cont.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo();
		return info.getMacAddress();
	}

	public static boolean isConnectedToWifi(Context context) {
		// Require permission  android.Manifest.permission.ACCESS_NETWORK_STATE
		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = conMan.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected() && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			return true;
		} else {
			return false;
		}
	}

	public static String getWifiIP(Context context) {
	    WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    if(wifiManager == null)
	    	return null;
	    
	    WifiInfo wInfo = wifiManager.getConnectionInfo(); 
	    if(wInfo == null)
	    	return null;
	    
	    int ipAddress = wInfo.getIpAddress();

	    // Convert little-endian to big-endianif needed
	    if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
	        ipAddress = Integer.reverseBytes(ipAddress);
	    }

	    byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

	    String ipAddressString;
	    try {
	        ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
	    } catch (UnknownHostException ex) {
	        ipAddressString = null;
	    }
	    return ipAddressString;
	}
}
