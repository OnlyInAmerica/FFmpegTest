# FFmpegTest
An attempt to illustrate debugging native libraries in Android. Included is a build of the ffmpeg 2.0.2 libraries for arm linux with debugging symbols enabled, and no optimizations.

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

Current problematic output:

	dbook:ffmpegTest davidbrodsky$ ndk-gdb
	GNU gdb (GDB) 7.3.1-gg2
	Copyright (C) 2011 Free Software Foundation, Inc.
	License GPLv3+: GNU GPL version 3 or later <http://gnu.org/licenses/gpl.html>
	This is free software: you are free to change and redistribute it.
	There is NO WARRANTY, to the extent permitted by law.  Type "show copying"
	and "show warranty" for details.
	This GDB was configured as "--host=x86_64-apple-darwin --target=arm-linux-android".
	For bug reporting instructions, please see:
	<http://source.android.com/source/report-bugs.html>.
	warning: Could not load shared library symbols for 91 libraries, e.g. libstdc++.so.
	Use the "info sharedlibrary" command to see the complete listing.
	Do you need "set solib-search-path" or "set sysroot"?
	warning: Breakpoint address adjusted from 0x4017fb79 to 0x4017fb78.
	0x4013f408 in epoll_wait () from /Users/davidbrodsky/Code/eclipse/ffmpegTest/obj/local/armeabi/libc.so
	(gdb) show solib-search-path
	The search path for loading non-absolute shared library symbol files is ./obj/local/armeabi.
	(gdb) info sources
	No symbol table is loaded.  Use the "file" command.
