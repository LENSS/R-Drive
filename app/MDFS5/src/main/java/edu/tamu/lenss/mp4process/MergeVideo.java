package edu.tamu.lenss.mp4process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import edu.tamu.lenss.mdfs.utils.IOUtilities;
import edu.tamu.lenss.mdfs.utils.Logger;

public class MergeVideo {
	private static final String TAG = MergeVideo.class.getSimpleName();
	private String outputPath;
	private List<String> videosToMerge;
	
	
	public MergeVideo( List<String> videosToMerge, String outputPath) {
		this.videosToMerge = videosToMerge;
		this.outputPath = outputPath;
	}
	
	/**
	 * This is a blocking class. Need to run in a separate thread
	 * @return true if the operation is successful
	 */
	public boolean mergeVideo(){
		int count = videosToMerge.size();
		try {
			
			Movie[] inMovies = new Movie[count];
			for (int i = 0; i < count; i++) {
				File file = new File(videosToMerge.get(i));
				if(file.exists()) {
					inMovies[i] = MovieCreator.build(videosToMerge.get(i));
				}
				else{
					Logger.e(TAG, "Invalid input video file");
					return false;
				}
			}
			List<Track> videoTracks = new LinkedList<Track>();
			List<Track> audioTracks = new LinkedList<Track>();
			
			for (Movie m : inMovies) {
				for (Track t : m.getTracks()) {
					if (t.getHandler().equals("soun")) {
						audioTracks.add(t);
					}
					if (t.getHandler().equals("vide")) {
						videoTracks.add(t);
					}
					if (t.getHandler().equals("")) {
						
					}
				}
			}
			
			Movie result = new Movie();
			
			if (audioTracks.size() > 0) {
				result.addTrack(new AppendTrack(audioTracks
						.toArray(new Track[audioTracks.size()])));
			}
			if (videoTracks.size() > 0) {
				result.addTrack(new AppendTrack(videoTracks
						.toArray(new Track[videoTracks.size()])));
			}
			Container out = new DefaultMp4Builder().build(result);

			IOUtilities.createNewFile(outputPath);
			RandomAccessFile rndFile = new RandomAccessFile(outputPath, "rw");
	        FileChannel fc = rndFile.getChannel();
	        out.writeContainer(fc);
	        rndFile.close();
	        fc.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
