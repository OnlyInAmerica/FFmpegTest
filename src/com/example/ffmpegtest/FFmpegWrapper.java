package com.example.ffmpegtest;

import java.nio.ByteBuffer;

/**
 * Work in Progress
 * First goal is to construct .ts files of constant length
 * given a series of interleaved, encoded audio / video frames
 *
 * Call shutdown() when FFmpegWrapper should not expect any more input
 * Any currently queued jobs will be finished.
 */
public class FFmpegWrapper {

    static {
        System.loadLibrary("FFmpegWrapper");
    }

    /*
    ExecutorService jobService = Executors.newSingleThreadExecutor();

    public void shutdown(){
        jobService.shutdown();
    }

    public void prepareAVFormatContext(final String outputPath){
        jobService.submit(new Runnable() {
            @Override
            public void run() {
                _prepareAVFormatContext(outputPath);
            }
        });
    }

    public void writeAVPacketFromEncodedData(final ByteBuffer data, final boolean isVideo, final int offset, final int size, final int flags, final long presentationTime){
        jobService.submit(new Runnable() {
            @Override
            public void run() {
                _writeAVPacketFromEncodedData(data, isVideo ? 1 : 0, offset, size, flags, presentationTime);
            }
        });
    }

    public void finalizeAVFormatContext(){
        jobService.submit(new Runnable() {
            @Override
            public void run() {
                _finalizeAVFormatContext();
            }
        });
    }

    */

    public native void test();

    // jint jIsVideo, jint jOffset, jint jSize, jint jFlags, jlong presentationTimeUs
    // .TS file generation from encoded data
    public native void prepareAVFormatContext(String jOutputPath);
    public native void writeAVPacketFromEncodedData(ByteBuffer jData, int jIsVideo, int jOffset, int jSize, int jFlags, long jFrameCount);
    public native void finalizeAVFormatContext();

}
