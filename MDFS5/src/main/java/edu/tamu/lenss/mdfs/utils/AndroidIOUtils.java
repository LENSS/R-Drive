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



	//Calculate a sample size value that is a power of two based on a target
	//width and height.
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}


	
	//Return a down-scaled Bitmap according to the required width and required height
	public static Bitmap decodeSampledBitmapFromFile(String filePath, 
	        int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(filePath, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeFile(filePath, options);
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
