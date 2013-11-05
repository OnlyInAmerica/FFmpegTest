# FFmpegTest
An attempt to feed encoded Audio and Video data from Android's MediaCodec to FFmpeg to provide support for formats beyond Android's capabilities (Android's MediaMuxer is currently limited to .mp4). Our deep ulteriour motive is to generate HLS streams on device. 

Included is a build of the ffmpeg 2.0.2 libraries for arm linux with debugging symbols enabled, and no optimizations.

# Overview

Camera frames and Microphone samples are queued into instances of Android's [MediaCodec](http://developer.android.com/reference/android/media/MediaCodec.html), which performs the encoding in hardware. We poll MediaCodec after submitting each audio / video  to dequeue encoded data and pass it to FFmpeg.

There are three JNI methods that bridge the tested Java/Android logic to FFmpeg. Their Java definitions are in FFmpegWrapper.java, their C implementations in FFmpegWrapper.c

+ `prepareAVFormatContext(String outputPath);`
     + Prepares an `AVFormatContext` for output, currently by reading from an mp4 prepared using Android's MediaMuxer and identical codec parameters.
+ `writeAVPacketFromEncodedData(ByteBuffer jData, int jIsVideo, int jOffset, int jSize, int jFlags, long jPts);`
     + Prepares an `AVPacket` from MediaCodec encoder output and submits it to FFmpeg via `av_interleaved_write_frame(...)`, along with the `AVFormatContext` created with the first method.
+ `finalizeAVFormatContext();`
     + Finalizes our `AVFormatContext` with `av_write_trailer(...)`

# Current Output

Playing the output of this app in VLC results in generally correct audio and blank / black video frames. 

ffprobe reports:

	$ ffprobe ffmpeg_1383684257441.mp4
	ffprobe version 1.2.4 Copyright (c) 2007-2013 the FFmpeg developers
	  built on Oct  8 2013 17:01:58 with Apple LLVM version 4.2 (clang-425.0.28) (based on LLVM 3.2svn)
	  configuration: --prefix=/usr/local/Cellar/ffmpeg/1.2.4 --enable-shared --enable-pthreads --enable-gpl --enable-version3 --enable-nonfree --enable-hardcoded-tables --enable-avresample --enable-vda --cc=cc --host-cflags= --host-ldflags= --enable-libx264 --enable-libfaac --enable-libmp3lame --enable-libxvid
	  libavutil      52. 18.100 / 52. 18.100
	  libavcodec     54. 92.100 / 54. 92.100
	  libavformat    54. 63.104 / 54. 63.104
	  libavdevice    54.  3.103 / 54.  3.103
	  libavfilter     3. 42.103 /  3. 42.103
	  libswscale      2.  2.100 /  2.  2.100
	  libswresample   0. 17.102 /  0. 17.102
	  libpostproc    52.  2.100 / 52.  2.100
	[aac @ 0x7ffefa018400] Input buffer exhausted before END element found
	[h264 @ 0x7ffefa010400] no frame!
	[h264 @ 0x7ffefa010400] corrupted macroblock 11 29 (total_coeff=-1)
	[h264 @ 0x7ffefa010400] error while decoding MB 11 29
	[h264 @ 0x7ffefa010400] concealing 78 DC, 78 AC, 78 MV errors in I frame
	Input #0, mov,mp4,m4a,3gp,3g2,mj2, from 'ffmpeg_1383684257441.mp4':
	  Metadata:
	    major_brand     : isom
	    minor_version   : 512
	    compatible_brands: isomiso2avc1mp41
	    encoder         : Lavf55.12.100
	  Duration: 00:00:11.08, start: 0.000000, bitrate: 1061 kb/s
	    Stream #0:0(und): Audio: aac (mp4a / 0x6134706D), 44100 Hz, mono, fltp, 125 kb/s
	    Metadata:
	      handler_name    : SoundHandler
	    Stream #0:1(und): Video: h264 (Baseline) (avc1 / 0x31637661), yuv420p, 640x480, 925 kb/s, 27.44 fps, 180k tbr, 180k tbn, 360k tbc
	    Metadata:
	      handler_name    : VideoHandler

     

# Building

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
