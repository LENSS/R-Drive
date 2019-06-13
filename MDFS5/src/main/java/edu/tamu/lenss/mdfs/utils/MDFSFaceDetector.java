package edu.tamu.lenss.mdfs.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.FaceDetector;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;

/**
 * All methods in this class are blocking calls
 * @author Jay
 */
public class MDFSFaceDetector {
	private static final String TAG = MDFSFaceDetector.class.getSimpleName();
	private FaceDetector face_detector;
	private MediaMetadataRetriever retriever;
	//private MediaPlayer mp;
	private Context context;
	
	public MDFSFaceDetector(Context cont) {
		this.context = cont;
	}
	
	
	/**
	 * 
	 * @param mp4File
	 * @param outputDirPath : 		The directory used for storing output image frames
	 * @param compressRate :		The quality of output image frame 0	is the lowest, 100 is the highest quality	
	 * @param samplePerSecond : 	Sample Frequency
	 * @param maxFaces :			Max number of faces that may appear in each frame
	 * @return the number of frames found that contain human faces
	 */
	public int detectFaceFromVideo(File mp4File, String outputDirPath, int compressRate, double samplePerSecond, int maxFaces){
		MediaPlayer mp = MediaPlayer.create(context, Uri.fromFile(mp4File));
		int videoLen = mp.getDuration(); // video length in milliseconds
		mp.release();
		int period = (int) Math.round(1000000/samplePerSecond); // microsecond
		int faceImageCnt = 0;
		int faceCnt = 0;
		String fName = mp4File.getName();
		byte blockIdx = Byte.parseByte(fName.substring(fName.lastIndexOf("blk__")+5));
		
		int idx = fName.lastIndexOf(".mp4");
		if(idx > 0)
			fName = fName.substring(0, idx);
		
		
		
		
		// Extract frame by frame. need to convert millisecond to microsecond
		retriever = new MediaMetadataRetriever();
		retriever.setDataSource(mp4File.getAbsolutePath());
		videoLen *= 1000; // Convert to microsecond
		for(int i=0; i < videoLen; i+=period){
			Bitmap bitmap = retriever.getFrameAtTime(i,MediaMetadataRetriever.OPTION_CLOSEST); // OPTION_CLOSEST_SYNC
			faceCnt = detectFaceFromImage(bitmap, maxFaces);
			
			// save thumbnail
			if(faceCnt > 0){
				faceImageCnt++;
				FileOutputStream out;
				try {
					File tmpF = new File(outputDirPath, fName + "_blk_" + blockIdx + "_" + i + ".jpg" );
					tmpF.createNewFile();
					out = new FileOutputStream(tmpF);
					bitmap.compress(Bitmap.CompressFormat.JPEG, compressRate, out);
					out.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		retriever.release();
		return faceImageCnt;
	}
	
	/**
	 * @param bitmap
	 * @param maxFaces :	Max number of faces that may appear in each frame
	 * @return
	 */
	public int detectFaceFromImage(Bitmap bitmap, int maxFaces){
		face_detector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), maxFaces);
		FaceDetector.Face[] faces = new FaceDetector.Face[maxFaces];
		return face_detector.findFaces(bitmap, faces);
	}
	
	
	/**
	 * Detect human faces in the image file and stored a compressed output to the specified outputDirPath
	 * @param imgFile
	 * @param maxFaces
	 * @param compressRate
	 * @param outputDirPath
	 * @return
	 */
	public int detectFaceFromImage(File imgFile, int maxFaces, int compressRate, String outputDirPath){
		final BitmapFactory.Options options = new BitmapFactory.Options();
		// This reduces the image size significantly already. Don't need to further scale down.
		options.inPreferredConfig=Bitmap.Config.RGB_565;	
    	Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
    	
    	face_detector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), maxFaces);
		FaceDetector.Face[] faces = new FaceDetector.Face[maxFaces];
		int faceCnt = face_detector.findFaces(bitmap, faces);
		if(faceCnt > 0){
			String fName = imgFile.getName();
			int idx = fName.lastIndexOf(".jpg");
			if(idx > 0)
				fName = fName.substring(0, idx);
			File tmpF = new File(outputDirPath, fName + "_0.jpg" );
			try {
				tmpF.createNewFile();
				FileOutputStream out = new FileOutputStream(tmpF);
				bitmap.compress(Bitmap.CompressFormat.JPEG, compressRate, out);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return faceCnt;
	}
	
	
	/**
	 * Covert a Bitmap to RGB_565 format, which is required by Android FaceDetector
	 * @param bitmap
	 * @return
	 */
	private Bitmap convert365(Bitmap bitmap) {
	    Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
	    Canvas canvas = new Canvas(convertedBitmap);
	    Paint paint = new Paint();
	    paint.setColor(Color.BLACK);
	    canvas.drawBitmap(bitmap, 0, 0, paint);
	    return convertedBitmap;
	}

}
