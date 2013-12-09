package com.example.ffmpegtest.recorder;

import com.example.ffmpegtest.HWRecorderActivity;
import com.example.ffmpegtest.R;
import com.example.ffmpegtest.Util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLSurfaceView;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

/**
 * Service to wrap LiveHLSRecorder. By moving the 
 * reference to the recorder here, we're given a 
 * higher priority by the Android system, and 
 * run less of risk of being destroyed due to 
 * memory constraints
 * @author davidbrodsky
 *
 */
public class RecordingService extends Service {
	private static final String TAG = "RecordingService";
	
	// Notification
    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION_ID = R.string.recording_service_name;
    
    // HLSRecorder
	public LiveHLSRecorder hlsRecorder;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public RecordingService getService() {
            return RecordingService.this;
        }
    }

    @Override
    public void onCreate() {
    	Log.i(TAG, "onCreate");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
      	      new IntentFilter(LiveHLSRecorder.INTENT_ACTION));
        // Display a notification about us starting.  We put an icon in the status bar.
        makeForeground();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // If this service is killed, restarting it won't do much good as
        // we'll have lost all the recording state, so return NOT_STICKY.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
    	Log.i(TAG, "onDestroy");
    	stopForeground(true);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Transition the service to foreground status. Prevents killing in all but extreme cases
     */
    private void makeForeground() {
        // The PendingIntent to launch if the user selects this notification
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HWRecorderActivity.class), 0);
        
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContentTitle(getText(R.string.recording_service_label))
        	   .setContentText(getText(R.string.recording_service_ready))
        	   .setSmallIcon(R.drawable.ic_stat_skate)
        	   .setContentIntent(pendingIntent);
        
        // Send the notification.
        startForeground(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * Convenience method for preparing LiveHLSRecorder
     * @param glSurfaceView
     */
    public void prepare(GLSurfaceView glSurfaceView){
        hlsRecorder = new LiveHLSRecorder(getApplicationContext(), glSurfaceView);
        hlsRecorder.setHLSRecorderCallback(new HLSRecorderCallback(){

			@Override
			public void HLSStreamComplete() {
				// Stop this service
				// If created with BIND_AUTO_CREATE, service
				// will remain alive until last client unbinds
				Log.i(TAG, "HLSStreamComplete!");
				RecordingService.this.stopSelf();
			}
        	
        });
        hlsRecorder.beginPreparingEncoders();
    }
    
    /**
     * Starts recording and updates Service
     * notification.
     * @param outputDir
     */
    public void startRecording(String outputDir){
    	hlsRecorder.startRecording(outputDir);
    	
    	PendingIntent pendingContentIntent = PendingIntent.getActivity(RecordingService.this, 0,
	            new Intent(RecordingService.this, HWRecorderActivity.class), 0);
    
	    Notification.Builder builder = new Notification.Builder(getApplicationContext());
	    builder.setContentTitle(getText(R.string.recording_service_label))
	    	   .setSmallIcon(R.drawable.ic_stat_skate)
	    	   .setContentIntent(pendingContentIntent)
	    	   .setContentText(getText(R.string.recording_service_buffering));
	    mNM.notify(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * Stops recording and updates Service
     */
    public void stopRecording(){
    	hlsRecorder.stopRecording();
    	
    	PendingIntent pendingContentIntent = PendingIntent.getActivity(RecordingService.this, 0,
	            new Intent(RecordingService.this, HWRecorderActivity.class), 0);
    	
    	PendingIntent pendingShareIntent = PendingIntent.getActivity(RecordingService.this, 0,
        		Util.createShareChooserIntentWithTitleAndUrl(getApplicationContext(), getString(R.string.recording_service_share), hlsRecorder.getHLSUrl()), 0);
    
	    Notification.Builder builder = new Notification.Builder(getApplicationContext());
	    builder.setContentTitle(getText(R.string.recording_service_label))
	    	   .setSmallIcon(R.drawable.ic_stat_skate)
	    	   .setContentIntent(pendingContentIntent)
	    	   .setContentText(getText(R.string.recording_service_syncing))
	    	   .addAction(R.drawable.ic_action_share, getText(R.string.recording_service_share), pendingShareIntent);
	    mNM.notify(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * We receive broadcasts from LiveHLSRecorder when the recording
     * is live, and when all syncing is complete.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
  	  @Override
  	  public void onReceive(Context context, Intent intent) {  		  
	        String broadcastUrl = intent.getStringExtra("url");

			PendingIntent pendingContentIntent = PendingIntent.getActivity(RecordingService.this, 0,
			            new Intent(RecordingService.this, HWRecorderActivity.class), 0);
	        
	        PendingIntent pendingShareIntent = PendingIntent.getActivity(RecordingService.this, 0,
	        		Util.createShareChooserIntentWithTitleAndUrl(getApplicationContext(), getString(R.string.recording_service_share), broadcastUrl), 0);
 
	        Notification.Builder builder = new Notification.Builder(getApplicationContext());
	        builder.setContentTitle(getText(R.string.recording_service_label))
	        	   .setSmallIcon(R.drawable.ic_stat_skate)
	        	   .setContentIntent(pendingContentIntent)
	        	   .addAction(R.drawable.ic_action_share, getText(R.string.recording_service_share), pendingShareIntent);
	        	   
  		if (LiveHLSRecorder.HLS_STATUS.LIVE ==  (LiveHLSRecorder.HLS_STATUS) intent.getSerializableExtra("status")){
			  	   builder.setContentText(getText(R.string.recording_service_live));  	   
  		}  else if (LiveHLSRecorder.HLS_STATUS.COMPLETE ==  (LiveHLSRecorder.HLS_STATUS) intent.getSerializableExtra("status")){
			builder.setContentText(getText(R.string.recording_service_complete));
  		}
		mNM.notify(NOTIFICATION_ID, builder.build());
  	  }
  };
    

}