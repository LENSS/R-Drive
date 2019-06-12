package edu.tamu.lenss.mp4process;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import edu.tamu.lenss.mdfs.models.MDFSFileInfo;
import edu.tamu.lenss.mdfs.utils.AndroidIOUtils;
import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

public class SplitVideo {
	private static final String TAG = SplitVideo.class.getSimpleName();
	private String mediaPath, outputDirPath;
	private int splitCnt;
	private double movieEndTime;
	private double[] splitTime;
	private long[][] sampleTime;
	private File movieFile;

	public SplitVideo(String mediaPath, String outputDirPath, int numberOfBlocks) {
		this.mediaPath = mediaPath;
		this.outputDirPath = outputDirPath;
		this.splitCnt = numberOfBlocks;
		this.splitTime = new double[splitCnt+1];
		this.movieFile = new File(mediaPath);
	}
	
	/**
	 * This is a blocking class. Need to run in a separate thread
	 * @return true if the operation is successful
	 */
	public boolean splitVideo() {
		if(!movieFile.exists() || !movieFile.getName().toLowerCase().contains(".mp4")){
			Logger.e(TAG, "Invalid input media file");
			return false;
		}
		try {  
			Movie movie = MovieCreator.build(mediaPath); 
			movieEndTime = getMovieDuration(movie);
			Log.i(TAG, "Movie Duration: " + movieEndTime);
			splitTime[0] = 0;
			double gap = movieEndTime/(double)splitCnt;
			for(int i=1; i < splitCnt+1; i++){
				splitTime[i] = splitTime[i-1] + gap;
				//Log.v(TAG, "Desired Split time:" + splitTime[i]);
			}
			
			List<Track> tracks = movie.getTracks();
			sampleTime = new long[tracks.size()][splitCnt+1];
			
			movie.setTracks(new LinkedList<Track>());   
			boolean timeCorrected = false;

	        // Here we try to find a track that has sync samples. Since we can only start decoding
	        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
	        // such a frame
			for (Track track : tracks) {
	            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
	                if (timeCorrected) {
	                    // This exception here could be a false positive in case we have multiple tracks
	                    // with sync samples at exactly the same positions. E.g. a single movie containing
	                    // multiple qualities of the same video (Microsoft Smooth Streaming file)
	                    throw new RuntimeException("The startTime has already been corrected "
	                    		+ "by another track with SyncSample. Not Supported.");
	                }
	                
	                for(int i=0; i < splitCnt+1; i++){
	                	splitTime[i] = correctTimeToSyncSample(track, splitTime[i], (i==splitCnt)?true:false);
	                	Log.v(TAG, "Split time:" + splitTime[i]);
	                }
	                timeCorrected = true;
	            }
	        }
			int trackCnt = 0;
			for (Track track : tracks) {
				long currentSample = 0;
				double currentTime = 0;
				double lastTime = -1;

				for (int i = 0; i < track.getSampleDurations().length; i++) {
					long delta = track.getSampleDurations()[i];

					for(int j=0; j < splitCnt+1; j++){
						if (currentTime > lastTime && currentTime <= splitTime[j]) {
							sampleTime[trackCnt][j] = currentSample;
						}
					}
					
					lastTime = currentTime;
					currentTime += (double)delta / (double)track.getTrackMetaData().getTimescale();
					currentSample++;
				}
				trackCnt++;
			}
			
			/**
			 * Create video blocks one by one 
			 */
			for(int j=0; j < splitCnt; j++){
				movie.setTracks(new LinkedList<Track>());
				trackCnt = 0;
				for (Track track : tracks) {
					movie.addTrack(new AppendTrack(new CroppedTrack(track,
							sampleTime[trackCnt][j], sampleTime[trackCnt][j+1])));
					trackCnt++;
				}
				Log.v(TAG, "Finish movie " + j);
				Container out = new DefaultMp4Builder().build(movie);
				File tmpF = IOUtilities.createNewFile(outputDirPath + File.separator 
						+ MDFSFileInfo.getBlockName(movieFile.getName(), (byte)j));
				
				FileOutputStream fos = new FileOutputStream(tmpF);
				FileChannel fc = fos.getChannel();
				out.writeContainer(fc);
				fc.close();
				fos.close();
			}
		} catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private double getMovieDuration(Movie m){
		double movieDuration = 0;
        for (Track track : m.getTracks()) {
            movieDuration = Math.max((double) track.getDuration() / track.getTrackMetaData().getTimescale(), movieDuration);
        }
        return movieDuration;
	}
	
	private double correctTimeToSyncSample(final Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

}
