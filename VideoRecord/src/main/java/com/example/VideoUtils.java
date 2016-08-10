package com.example;

import android.content.Context;
import android.media.MediaMetadataRetriever;

import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by zz4760762 on 2015/7/27.
 */
public final class VideoUtils {


    public static void appendVideo(Context context,String saveVideoPath,List<String> videos) throws IOException{
        Movie[] inMovies = new Movie[videos.size()];
        int index = 0;
        for(String video:videos)
        {
            inMovies[index] = MovieCreator.build(video);
            index++;
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
            }
        }

        Movie result = new Movie();

        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }
        Container out = new DefaultMp4Builder().build(result);
        FileChannel fc = new RandomAccessFile(String.format(saveVideoPath), "rw").getChannel();
        out.writeContainer(fc);
        fc.close();
    }

//    protected static long getDuration(Track track) {
//        long duration = 0;
//        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
//            duration += entry.getCount() * entry.getDelta();
//        }
//        return duration;
//    }


    /**
     * 获取视频宽高（分辨率）
     * @param path
     * @return
     */
    public static String[] getVideoWidthHeight(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String video_width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String video_height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        String[] video_width_height = {video_width, video_height};
        return video_width_height;
    }
  /*  private static void printTime(Track track) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            for (int j = 0; j < entry.getCount(); j++) {
                if (Arrays.binarySearch(track.getSyncSamples(),
                        currentSample + 1) >= 0) {
                    // samples always start with 1 but we start with zero
                    // therefore +1
                    timeOfSyncSamples[Arrays.binarySearch(
                            track.getSyncSamples(), currentSample + 1)] = currentTime;
                    System.out.println("currentTime-->" + currentTime);
                }
                currentTime += (double) entry.getDelta()
                        / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
        }

        // System.out.println("size-->"+currentSample);
		*//*
		 * for(int i=0;i<timeOfSyncSamples.length;i++){
		 * System.out.println("data-->"+timeOfSyncSamples[i]); }
		 *//*

		*//*
		 * double previous = 0; for (double timeOfSyncSample :
		 * timeOfSyncSamples) { if (timeOfSyncSample > cutHere) { if (next) {
		 * return timeOfSyncSample; } else { return previous; } } previous =
		 * timeOfSyncSample; } return timeOfSyncSamples[timeOfSyncSamples.length
		 * - 1];
		 *//*
    }*/
}
