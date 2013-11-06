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

import com.example.ffmpegtest.recorder.ChunkedHWRecorder;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HWRecorderActivity extends Activity {
    private static final String TAG = "HWRecorderActivity";
    boolean recording = false;
    ChunkedHWRecorder chunkedHWRecorder;

    //GLSurfaceView glSurfaceView;
    //GlSurfaceViewRenderer glSurfaceViewRenderer = new GlSurfaceViewRenderer();

    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hwrecorder);
        //glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        //glSurfaceView.setRenderer(glSurfaceViewRenderer);
    }

    @Override
    public void onPause(){
        super.onPause();
        //glSurfaceView.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        //glSurfaceView.onResume();
    }

    public void onRecordButtonClicked(View v){
        if(!recording){
            try {
                chunkedHWRecorder = new ChunkedHWRecorder();
                chunkedHWRecorder.startRecording(null);
                recording = true;
                ((Button) v).setText("Stop Recording");
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }else{
            chunkedHWRecorder.stopRecording();
            recording = false;
            ((Button) v).setText("Start Recording");
        }
    }

    /*
    static EGLContext context;

    public class GlSurfaceViewRenderer implements GLSurfaceView.Renderer{

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.i(TAG, "GLSurfaceView created");
            context = EGL14.eglGetCurrentContext();
            if(context == EGL14.EGL_NO_CONTEXT)
                Log.e(TAG, "failed to get valid EGLContext");

           EGL14.eglMakeCurrent(EGL14.eglGetCurrentDisplay(), EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {
        }
    }
    */

}