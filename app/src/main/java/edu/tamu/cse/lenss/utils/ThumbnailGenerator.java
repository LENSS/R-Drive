package edu.tamu.cse.lenss.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;

public class ThumbnailGenerator {
	private static final String TAG = ThumbnailGenerator.class.getSimpleName();

	private ThumbnailGenerator() {
	}
	
	/**
	 * Create a png humbnail file of the video
	 * @param inFilePath
	 * @param
	 * @return a bitmap if the video file is successfully compressed
	 */
	public static Bitmap createVideoThumbnail(String inFilePath, String outFilePath) {
		Bitmap bmp = ThumbnailUtils.createVideoThumbnail(inFilePath, MediaStore.Video.Thumbnails.MICRO_KIND);
		if(bmp == null){
			Log.e(TAG, "decode file " + inFilePath + " to thumbnail failed");
			return null;
		}
		boolean success = false;
		try {
			FileOutputStream out = new FileOutputStream(outFilePath);
			success = bmp.compress(Bitmap.CompressFormat.JPEG, 75, out);
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} finally{
			if(success)
				return bmp; 			
		}
		return null;
	}
	
	public static Bitmap creatImageThumbnail(String inFilePath, String outFilePath){
		final BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(inFilePath, options);
		
	    // Match the size of MediaStore.Video.Thumbnails.MICRO_KIND as well as save memory
		Bitmap inputBmp = AndroidIOUtils.decodeSampledBitmapFromFile(inFilePath,
				96, Math.round(96*((float)options.outHeight/options.outWidth)));
		if(inputBmp == null){
			Log.e(TAG, "decode file " + inFilePath + " to thumbnail failed");
			return null;
		}
		
		boolean success = false;
		Bitmap bmp = ThumbnailUtils.extractThumbnail(inputBmp, 
				96, Math.round(96*((float)options.outHeight/options.outWidth)), ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		
		try {
			FileOutputStream out = new FileOutputStream(outFilePath);
			success = bmp.compress(Bitmap.CompressFormat.JPEG, 75, out);
			out.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} finally{
			if(success)
				return bmp; 			
		}
		return null;
	}
}
