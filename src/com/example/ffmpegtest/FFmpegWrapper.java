package com.example.ffmpegtest;

import java.nio.ByteBuffer;

import android.util.Log;

/**
 * Work in Progress!
 * The goal of this class is to mux output from 
 * Android's MediaCodec into various outputs.
 * My specific intent is to create segmented mpegts files
 * and text manfiests suitable for HTTP-HLS
 */
public class FFmpegWrapper {

    static {
        System.loadLibrary("FFmpegWrapper");
    }

    public native void test();		// Successful test that copies an mp4 frame by frame. Relies on hardcoded paths for input / output...

    public native void prepareAVFormatContext(String jOutputPath);
    public native void writeAVPacketFromEncodedData(ByteBuffer jData, int jIsVideo, int jOffset, int jSize, int jFlags, long jPts);
    public native void finalizeAVFormatContext();

}
