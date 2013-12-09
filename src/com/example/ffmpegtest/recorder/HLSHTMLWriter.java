package com.example.ffmpegtest.recorder;

import java.io.File;

/**
 * Quick and Dirty HTML video player
 * @author davidbrodsky
 *
 */
public class HLSHTMLWriter {
	public static final String header = "<html><head><title>KickFlip Live Broadcast</title><style>body{background-color: #47A447;}</style><body><center><div><video controls autoplay><source src=";
	public static final String footer = " type=application/x-mpegURL></source></video><br><br><a href=kickflip.io><img width=200px src=http://i.imgur.com/e27s8uU.png></a></div></center>";
	
	public static String generateHTMLWithVideoURL(String videoUrl){
		return header + videoUrl + footer;
	}
}
