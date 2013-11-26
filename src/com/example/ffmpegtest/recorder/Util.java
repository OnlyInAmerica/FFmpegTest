package com.example.ffmpegtest.recorder;

import android.opengl.GLES20;
import android.util.Log;

public class Util {
	static final String TAG = "Util";
	
	public static void checkGlError(String msg) {
        int error, lastError = GLES20.GL_NO_ERROR;

        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, msg + ": glError " + error);
            lastError = error;
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(msg + ": glError " + lastError);
        }
    }

}
