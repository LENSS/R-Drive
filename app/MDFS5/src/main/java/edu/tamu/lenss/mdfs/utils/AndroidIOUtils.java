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
	private static final String TAG = AndroidIOUtils.class.getSimpleName();

	/**
	 * Create a File handler to the specified path on SD Card
	 * 
	 * @param path
	 * @return
	 */
	public static File getExternalFile(String path) {
		return new File(Environment.getExternalStorageDirectory(), path);
	}

	public static boolean isExternalStorageAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
		// return true;
	}

	/**
	 * Return the MAC Address of this device. <Br>
	 * Note: Require <uses-permission
	 * android:name="android.permission.ACCESS_WIFI_STATE"/>. It only works when
	 * WIFI is connected.
	 * 
	 * @param context
	 * @return n
	 */
	public static String getMyMACAdd(Context cont) {
		WifiManager manager = (WifiManager) cont
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo();
		return info.getMacAddress();
	}

	public static boolean isConnectedToWifi(Context context) {
		// Require permission  android.Manifest.permission.ACCESS_NETWORK_STATE
		ConnectivityManager conMan = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = conMan.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()
				&& netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			return true;
		} else {
			return false;
		}
	}

	
	public static boolean isConnectedToMobile(Context context) {
		// Require permission  android.Manifest.permission.ACCESS_NETWORK_STATE
		ConnectivityManager conMan = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo netInfo = conMan.getActiveNetworkInfo();
		if (netInfo == null) Logger.e(TAG, "netInfo is null");
		
		if (netInfo != null && netInfo.isConnected()
				&& netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
			return true;
		} else {
			return false;
		}
	}

	
	/**
	 * Calculate a sample size value that is a power of two based on a target
	 * width and height
	 * 
	 * @param options
	 * @param reqWidth
	 * @param reqHeight
	 * @return
	 */
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
	
	/**
	 * Return a down-scaled Bitmap according to the required width and required height
	 * @param filePath
	 * @param reqWidth : in pixel
	 * @param reqHeight : in pixel
	 * @return
	 */
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
	        int reqWidth, int reqHeight) {

	    // First decode with inJustDecodeBounds=true to check dimensions
	    final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeResource(res, resId, options);

	    // Calculate inSampleSize
	    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

	    // Decode bitmap with inSampleSize set
	    options.inJustDecodeBounds = false;
	    return BitmapFactory.decodeResource(res, resId, options);
	}
	
	/**
	 * Return a down-scaled Bitmap according to the required width and required height
	 * @param filePath
	 * @param reqWidth : in pixel
	 * @param reqHeight : in pixel
	 * @return
	 */
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
	
	
	/**
	 * Rotate a Bitmap image
	 * @param bitmap
	 * @param degree
	 * @return
	 */
	public static Bitmap rotate(Bitmap bitmap, int degree) {
	    int w = bitmap.getWidth();
	    int h = bitmap.getHeight();
	    Matrix mtx = new Matrix();
	    mtx.postRotate(degree);
	    return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
	}
	
	/**
	 * Covert Android UI unit DIP to Pixel
	 * @param context
	 * @param dp
	 * @return
	 */
	public static int dipToPixel(Context context, int dp){
		Resources r = context.getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
		return Math.round(px);
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
	        Logger.e(TAG, "Unable to get host address.");
	        ipAddressString = null;
	    }
	    return ipAddressString;
	}
}
