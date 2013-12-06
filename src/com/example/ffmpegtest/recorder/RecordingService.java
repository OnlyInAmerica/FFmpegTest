package com.example.ffmpegtest.recorder;

import com.example.ffmpegtest.HWRecorderActivity;
import com.example.ffmpegtest.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
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
    private int NOTIFICATION = R.string.recording_service_started;
    
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

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
    	Log.i(TAG, "onDestroy");
    	stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.recording_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HWRecorderActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.recording_service_label),
                       text, contentIntent);

        // Send the notification.
        //mNM.notify(NOTIFICATION, notification);
        startForeground(1337, notification);
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

}