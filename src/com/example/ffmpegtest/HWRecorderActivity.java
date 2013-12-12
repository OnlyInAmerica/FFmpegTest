/*
 * Copyright (c) 2013, David Brodsky. All rights reserved.
 *
 *	This program is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.ffmpegtest;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.example.ffmpegtest.recorder.LiveHLSRecorder;
import com.example.ffmpegtest.recorder.RecordingService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

public class HWRecorderActivity extends Activity {
    private static final String TAG = "HWRecorderActivity";
    //LiveHLSRecorder liveRecorder;
    
    TextView liveIndicator;
    TextView instructions;
    String broadcastUrl;

    public static GLSurfaceView glSurfaceView;
    GLSurfaceViewRenderer glSurfaceViewRenderer = new GLSurfaceViewRenderer();
    LayoutInflater inflater;
    
    // Service
    private RecordingService mRecordingService;
    private boolean mIsBound;

    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate. changingConfigurations: " + this.getChangingConfigurations() + " callingActivity: " + ( (getCallingActivity() == null) ? "none" : this.getCallingActivity().toString()) );
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_hwrecorder);
        inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        liveIndicator = (TextView) findViewById(R.id.liveLabel);
        instructions = (TextView) findViewById(R.id.instructions);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        //glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        glSurfaceView.setRenderer(glSurfaceViewRenderer);
        
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
        	      new IntentFilter(LiveHLSRecorder.INTENT_ACTION));
        
        if(mRecordingService == null){
            Intent intent = new Intent(this, RecordingService.class);
            startService(intent);
            Log.i(TAG, "called startService");
        }
        if(!mIsBound){
        	doBindService();  	
        }
    }
    
    boolean isPaused = false;
    
    @Override
    public void onPause(){
        super.onPause();
        Log.i(TAG, "onPause");
        if((( mRecordingService == null || mRecordingService.hlsRecorder == null) ? false: mRecordingService.hlsRecorder.isRecording())){
        	mRecordingService.hlsRecorder.encodeVideoFramesInBackground();
        }else{
        	glSurfaceView.onPause();
        }
        isPaused = true;
    }
    
    @Override
    public void onStop(){
        super.onStop();
        Log.i(TAG, "onStop");
    }
    
    @Override
    public void onResume(){
        super.onResume();
        glSurfaceView.onResume();
        Log.i(TAG, "onResume");
        isPaused = false;
    }
    
    @Override
    protected void onDestroy() {
      // Unregister since the activity is about to be closed.
      LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
      super.onDestroy();
      Log.i(TAG, "onDestroy");
      doUnbindService();
      // TODO: Stop encoder
    }

    public void onRecordButtonClicked(View v){
    	Log.i(TAG, "onRecordButtonClicked");
    	boolean isRecording = false;
        if(!mRecordingService.hlsRecorder.isRecording()){
        	isRecording = true;
        	broadcastUrl = null;
        	//liveRecorder.beginPreparingEncoders();
        	glSurfaceView.queueEvent(new Runnable(){

				@Override
				public void run() {
					mRecordingService.hlsRecorder.finishPreparingEncoders();
					mRecordingService.startRecording(null);
				}
        		
        	});            	
        }else{
        	mRecordingService.stopRecording();  
        }
        updateUI(isRecording);
    }
    
    /**
     * Adjusts the UI state based on 
     * whether the underlying recorder is active
     * 
     * Assumed to be called after onCreate
     */
    public void updateUI(boolean isRecording){
    	if(isRecording){
    		// Hide instructions, and set click listener on glSurfaceView
    		instructions.setVisibility(View.INVISIBLE);
        	glSurfaceView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					onRecordButtonClicked(arg0);
				}
        		
        	});
    	}else{
    		// Show instructions, remove glSurfaceView click listener, slide out LIVE badge if visible
    		// NOTE: It seems when GLSurfaceView is re-created, it's z order is modified, blocking instructions
    		instructions.bringToFront();
			liveIndicator.bringToFront();
    		instructions.setVisibility(View.VISIBLE);
        	glSurfaceView.setOnClickListener(null);
        	if(liveIndicator.getVisibility() == View.VISIBLE)
        		liveIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_to_left));
    	}
    }
    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    	    // Get extra data included in the Intent
    		if (LiveHLSRecorder.HLS_STATUS.LIVE ==  (LiveHLSRecorder.HLS_STATUS) intent.getSerializableExtra("status")){
    			broadcastUrl = intent.getStringExtra("url");
    			liveIndicator.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_from_left));
            	liveIndicator.setVisibility(View.VISIBLE);
    		}  
    	  }
    };
    
    public void onUrlLabelClick(View v){
    	Log.i(TAG, "onUrlLabelClick");
    	if(broadcastUrl != null){
    		startActivity(Util.createShareChooserIntentWithTitleAndUrl(getApplicationContext(), "Share Broadcast!", broadcastUrl));
    	}
    }
    
    public class GLSurfaceViewRenderer implements GLSurfaceView.Renderer{    
    	boolean surfaceJustCreated = false;
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        	surfaceJustCreated = true;
            Log.i(TAG, "GLSurfaceView created");
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
        	//Log.i(TAG, "GLSurfaceView changed. HLSRecorder set : " + (mRecordingService != null && mRecordingService.hlsRecorder != null));
        	Log.i(TAG, "GLSurfaceView changed. isPaused: " + isPaused);
        	gl.glViewport(0, 0, width, height);
            // for a fixed camera, set the projection too
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);

            if( !isPaused && ((mRecordingService == null || mRecordingService.hlsRecorder == null) ? false : mRecordingService.hlsRecorder.isRecording())){
            	Log.i(TAG, "beginForegroundRecording onSurfaceChanged. isPaused: " + isPaused + " surfaceJustCreated: " + surfaceJustCreated);
            	mRecordingService.hlsRecorder.beginForegroundRecording(surfaceJustCreated);
            	if(surfaceJustCreated){
            		HWRecorderActivity.this.runOnUiThread(new Runnable(){

						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "Currently unable to display frames after screen off. Broadcast is still live.", Toast.LENGTH_LONG).show();
						}
            			
            		});
            	}
            }
            surfaceJustCreated = false;
        }

        @Override
        public void onDrawFrame(GL10 gl) {
    		//Log.i(TAG, "onDrawFrame"); // bg? " +((mRecordingService == null || mRecordingService.hlsRecorder == null) ? false : mRecordingService.hlsRecorder.recordingInBackground));
        	if( ((mRecordingService == null || mRecordingService.hlsRecorder == null) ? false : ( mRecordingService.hlsRecorder.isRecording() && !mRecordingService.hlsRecorder.recordingInBackground ) )){        		
        		mRecordingService.hlsRecorder.encodeVideoFrame();
        	}
        }
    }
    
    // Service
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mRecordingService = ((RecordingService.LocalBinder)service).getService();
            if( (mRecordingService.hlsRecorder == null) ? true : !mRecordingService.hlsRecorder.isRecording()){
            	Log.i(TAG, "Preparing mRecordingService encoder with glSurfaceView");
            	mRecordingService.prepare(glSurfaceView);		// creates and prepares HLSRecorder
            }
            Log.i(TAG, "Recording service connected");
            updateUI(mRecordingService.hlsRecorder.isRecording());
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mRecordingService = null;
            Log.i(TAG, "Recording service disconnected");
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(HWRecorderActivity.this, 
                RecordingService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    	Log.i(TAG, "called bindService");
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            
            // If we're bound to service
            // but not recording, safe to stop service now
            if( (mRecordingService == null || mRecordingService.hlsRecorder == null) ? false : !mRecordingService.hlsRecorder.isRecording()){
            	stopService(new Intent(this, RecordingService.class));
            }
        }
    }
    

}