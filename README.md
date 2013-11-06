# FFmpegTest
An attempt to feed encoded Audio and Video data from Android's MediaCodec to FFmpeg to provide support for formats beyond Android's capabilities (Android's MediaMuxer is currently limited to .mp4). Our ultimate goal is to allow an Android device to act as an HLS server. 

Included is a build of the ffmpeg 2.0.2 libraries for arm linux with debugging symbols enabled, and no optimizations.

# Overview

Camera frames and Microphone samples are queued into instances of Android's [MediaCodec](http://developer.android.com/reference/android/media/MediaCodec.html), which performs the encoding in hardware. We poll MediaCodec after submitting each audio / video  to dequeue encoded data and pass it to FFmpeg.

There are three JNI methods that bridge the tested Java/Android logic to FFmpeg. Their Java definitions are in [FFmpegWrapper.java](https://github.com/OnlyInAmerica/FFmpegTest/blob/master/src/com/example/ffmpegtest/FFmpegWrapper.java), and their C implementations in [FFmpegWrapper.c](https://github.com/OnlyInAmerica/FFmpegTest/blob/master/jni/FFmpegWrapper.c).

+ `prepareAVFormatContext(String outputPath);`
     + Prepares an `AVFormatContext` for output, currently by reading from an mp4 prepared using Android's MediaMuxer and identical codec parameters.
+ `writeAVPacketFromEncodedData(ByteBuffer jData, int jIsVideo, int jOffset, int jSize, int jFlags, long jPts);`
     + Prepares an `AVPacket` from MediaCodec encoder output and submits it to FFmpeg via `av_interleaved_write_frame(...)`, along with the `AVFormatContext` created with the first method.
+ `finalizeAVFormatContext();`
     + Finalizes our `AVFormatContext` with `av_write_trailer(...)`

# Current Output

A video produced with the latest version of this application is available [here](https://s3.amazonaws.com/dbro/h264_madness/ffmpeg_1383772856149.ts).

Playing the output of this app in VLC results in generally correct audio and video.

ffprobe reports:

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
	[h264 @ 0x7ffb29018400] corrupted macroblock 11 29 (total_coeff=-1)
	[h264 @ 0x7ffb29018400] error while decoding MB 11 29
	[h264 @ 0x7ffb29018400] concealing 78 DC, 78 AC, 78 MV errors in I frame
	Input #0, mpegts, from '/Users/davidbrodsky/Desktop/HWEncodingExperiments/ffmpeg/ffmpeg_1383766999385.mp4':
	  Duration: 00:00:03.85, start: 0.000000, bitrate: 1214 kb/s
	  Program 1 
	    Metadata:
	      service_name    : Service01
	      service_provider: FFmpeg
	    Stream #0:0[0x100]: Video: h264 (Baseline) ([27][0][0][0] / 0x001B), yuv420p, 640x480, 90k tbr, 90k tbn, 180k tbc
	    Stream #0:1[0x101]: Audio: aac ([15][0][0][0] / 0x000F), 44100 Hz, mono, fltp, 137 kb/s

     

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


# License


	Software License Agreement (GPLv3+)
	
	Copyright (c) 2013, David Brodsky. All rights reserved.
	
	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.


This software additionally references or incorporates the following sources
of intellectual property, the license terms for which are set forth
in the sources themselves:

[FFmpeg](http://www.ffmpeg.org/legal.html) - Used for muxing and processing of encoded data