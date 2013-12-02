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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class HWRecorderActivity extends Activity {
    private static final String TAG = "HWRecorderActivity";
    boolean recording = false;
    LiveHLSRecorder liveRecorder;
    
    TextView liveIndicator;
    TextView instructions;
    String broadcastUrl;

    public static GLSurfaceView glSurfaceView;
    GLSurfaceViewRenderer glSurfaceViewRenderer = new GLSurfaceViewRenderer();
    LayoutInflater inflater;

    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_hwrecorder);
        inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        liveIndicator = (TextView) findViewById(R.id.liveLabel);
        instructions = (TextView) findViewById(R.id.instructions);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        glSurfaceView.setRenderer(glSurfaceViewRenderer);
        
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
        	      new IntentFilter(LiveHLSRecorder.INTENT_ACTION));
        
        liveRecorder = new LiveHLSRecorder(getApplicationContext());
        liveRecorder.beginPreparingEncoders();
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.i(TAG, "onPause");
        glSurfaceView.onPause();
        if(recording){
        	liveRecorder.encodeVideoFramesInBackground();
        }
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
        Log.i(TAG, "onResume. Recording: " + recording);
    }
    
    @Override
    protected void onDestroy() {
      // Unregister since the activity is about to be closed.
      LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
      super.onDestroy();
      Log.i(TAG, "onDestroy");
      // TODO: Stop encoder
    }

    public void onRecordButtonClicked(View v){
    	Log.i(TAG, "onRecordButtonClicked");
        if(!recording){
        	instructions.setVisibility(View.INVISIBLE);
        	glSurfaceView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					onRecordButtonClicked(arg0);
				}
        		
        	});
        	broadcastUrl = null;
        	liveRecorder.beginPreparingEncoders();
        	glSurfaceView.queueEvent(new Runnable(){

				@Override
				public void run() {
					liveRecorder.finishPreparingEncoders();
					liveRecorder.startRecording(null);
				}
        		
        	});            	

        }else{
        	instructions.setVisibility(View.VISIBLE);
        	glSurfaceView.setOnClickListener(null);
            liveRecorder.stopRecording();
            glSurfaceView.queueEvent(new Runnable(){

				@Override
				public void run() {
					liveRecorder.encodeVideoFrame();
				}
            	
            });
        	liveIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_to_left));
        }
        recording = !recording;
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
    		shareUrl(broadcastUrl);
    	}
    }
    
    private void shareUrl(String url) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        shareIntent.putExtra(Intent.EXTRA_TEXT, url.replace("%2F", "/"));	// TODO: Fix this in S3 library
        startActivity(Intent.createChooser(shareIntent, "Share Broadcast!"));
    } 
    
    public class GLSurfaceViewRenderer implements GLSurfaceView.Renderer{    	
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.i(TAG, "GLSurfaceView created");
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            
            /*
            if(recording){
            	liveRecorder.beginForegroundRecording();
            }
            */
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
        	Log.i(TAG, "GLSurfaceView changed");
        	gl.glViewport(0, 0, width, height);
            // for a fixed camera, set the projection too
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
            
            if(recording){
            	liveRecorder.saveEGLState();
            	liveRecorder.beginForegroundRecording();
            	Log.i(TAG, "beginForegroundRecording onSurfaceChanged");
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
        	if(recording && !liveRecorder.recordingInBackground){        		
        		//Log.i(TAG, "onDrawFrame");
        		liveRecorder.encodeVideoFrame();
        	}
        }
    }
    

}