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
