LOCAL_PATH := $(call my-dir)

APP_PLATFORM := android-10

include $(CLEAR_VARS)

# TODO: Observe $(TARGET_ARCH) and adjust appropriately. For now, we only have armeabi libraries

# Prebuilt FFmpeg

LOCAL_MODULE:= libavcodec
LOCAL_SRC_FILES:= ./ffmpeg/libavcodec-55.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavfilter
LOCAL_SRC_FILES:= ./ffmpeg/libavfilter-3.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavformat
LOCAL_SRC_FILES:= ./ffmpeg/libavformat-55.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavutil
LOCAL_SRC_FILES:= ./ffmpeg/libavutil-52.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libswresample
LOCAL_SRC_FILES:= ./ffmpeg/libswresample-0.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libswscale
LOCAL_SRC_FILES:= ./ffmpeg/libswscale-2.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

# Our Wrapper

include $(CLEAR_VARS)

LOCAL_LDLIBS += -llog -lz
LOCAL_STATIC_LIBRARIES := libavformat libavcodec libswscale libavutil
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_SRC_FILES := FFmpegWrapper.c
LOCAL_CFLAGS := -march=armv7-a -mfloat-abi=softfp -mfpu=neon -g -O0
LOCAL_MODULE := FFmpegWrapper

include $(BUILD_SHARED_LIBRARY)
