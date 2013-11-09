package com.example.ffmpegtest.recorder;

import java.io.File;
import java.util.UUID;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.example.ffmpegtest.HLSFileObserver;
import com.example.ffmpegtest.HLSFileObserver.HLSCallback;
import com.example.ffmpegtest.S3Client;
import com.example.ffmpegtest.S3Client.S3Callback;
import com.example.ffmpegtest.SECRETS;
import com.readystatesoftware.simpl3r.Uploader;
import com.readystatesoftware.simpl3r.Uploader.UploadProgressListener;

public class LiveHLSRecorder extends HLSRecorder{
	static final String TAG = "LiveHLSRecorder";
	static final boolean VERBOSE = true; 				// lots of logging
	
	Context c;
	String uuid;										// Recording UUID
	HLSFileObserver observer;							// Must hold reference to observer to continue receiving events
	
	public static final String INTENT_ACTION = "HLS";	// Intent action broadcast to LocalBroadcastManager
	public enum HLS_STATUS { OFFLINE, LIVE };
	
	boolean sentIsLiveBroadcast = false;				// Only send "broadcast is live" intent once per recording
	
	// Amazon S3
	static final String S3_BUCKET = "openwatch-livestreamer";
	S3Client s3Client;
	S3Callback segmentUploadedCallback = new S3Callback(){

		@Override
		public void onProgress(ProgressEvent progressEvent, long bytesUploaded,
				int percentUploaded) {
			if (VERBOSE) Log.i(TAG, String.format(".ts segment upload progress: %d", percentUploaded));
			if(progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
				if (VERBOSE) Log.i(TAG, ".ts segment upload success");
			} else if(progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE){
				if (VERBOSE) Log.i(TAG, ".ts segment upload failed");
			}
		}
		
	};
	
	S3Callback manifestUploadedCallback = new S3Callback(){

		@Override
		public void onProgress(ProgressEvent progressEvent, long bytesUploaded,
				int percentUploaded) {
			if (VERBOSE) Log.i(TAG, String.format(".m3u8 upload progress: %d", percentUploaded));
			if(progressEvent.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
				if (VERBOSE) Log.i(TAG, ".m3u8 upload success");
			} else if(progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE){
				if (VERBOSE) Log.i(TAG, ".m3u8 upload failed");
			}
		}
		
	};
	
	public LiveHLSRecorder(Context c){
		super(c);
		s3Client = new S3Client(c, SECRETS.AWS_KEY, SECRETS.AWS_SECRET);
		s3Client.setBucket(S3_BUCKET);
		this.c = c;
	}
	
	@Override
	public void startRecording(final String outputDir){
		super.startRecording(outputDir);
		sentIsLiveBroadcast = false;
        observer = new HLSFileObserver(getOutputPath(), new HLSCallback(){

			@Override
			public void onSegmentComplete(String path) {
				if (VERBOSE) Log.i(TAG, ".ts segment written: " + path);
				File file = new File(path);
				String url = s3Client.upload(getUUID() + File.separator + file.getName(), file, segmentUploadedCallback);
				if (VERBOSE) Log.i(TAG, ".ts segment destination url received: " + url);
			}

			@Override
			public void onManifestUpdated(String path) {
				if (VERBOSE) Log.i(TAG, ".m3u8 written: " + path);
				File file = new File(path);
				String url = s3Client.upload(getUUID() + File.separator + file.getName(), file, manifestUploadedCallback);
				if (VERBOSE) Log.i(TAG, ".m3u8 destination url received: " + url);
				if(!sentIsLiveBroadcast){
					broadcastRecordingIsLive(url);
					sentIsLiveBroadcast = true;
				}
			}
        	
        });
        observer.startWatching();
        Log.i(TAG, "Watching " + getOutputPath() + " for changes");
	}
	
	/**
	 * Broadcasts a message to the LocalBroadcastManager
	 * indicating the HLS stream is live.
	 * This message is receivable only within the 
	 * hosting application
	 * @param url address of the HLS stream
	 */
	private void broadcastRecordingIsLive(String url) {
		  Log.d(TAG, "Broadcasting Live HLS link");
		  Intent intent = new Intent(INTENT_ACTION);
		  intent.putExtra("url", url);
		  intent.putExtra("status", HLS_STATUS.LIVE);
		  LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
	}
}
