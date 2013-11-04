# FFmpegTest
An attempt to feed encoded Audio and Video data from Android's MediaCodec to FFmpeg in order to generate an HLS stream. Included is a build of the ffmpeg 2.0.2 libraries for arm linux with debugging symbols enabled, and no optimizations.

# Procedure

In Eclipse with the [NDK plugin](http://tools.android.com/recent/usingthendkplugin).

   1. Import the project
   2. Follow the [NDK plugin setup instructions](http://tools.android.com/recent/usingthendkplugin)
   3. Debug as Native Android Application
   
Manually:

    $ cd ./jni
    $ ndk-build NDK_DEBUG=1
    # build and send the apk to device
    $ cd ../  # The project root
    $ ndk-gdb
