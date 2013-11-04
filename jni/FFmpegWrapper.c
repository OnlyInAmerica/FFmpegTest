#include <jni.h>
#include <android/log.h>
#include <string.h>
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"


#define LOG_TAG "FFmpegWrapper"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Current output
const char *outputPath;
const char *outputFormatName = "mp4";
const char *videoCodecName = "h264";
const char *audioCodecName = "aac";
AVFormatContext *outputFormatContext;
AVStream *audioStream;
AVStream *videoStream;
AVCodec *audioCodec;
AVCodec *videoCodec;

AVPacket *packet; // recycled across calls to writeAVPacketFromEncodedData

// Example h264 file:
const char *sampleFilePath = "/sdcard/sample.mp4";

// FFmpeg Utilities

void init(){
    av_register_all();
    avformat_network_init();
    avcodec_register_all();
}

char* stringForAVErrorNumber(int errorNumber){
    char *errorBuffer = malloc(sizeof(char) * AV_ERROR_MAX_STRING_SIZE);

    int strErrorResult = av_strerror(errorNumber, errorBuffer, AV_ERROR_MAX_STRING_SIZE);
    if (strErrorResult != 0) {
        LOGE("av_strerror error: %d", strErrorResult);
        return NULL;
    }
    return errorBuffer;
}

void copyAVFormatContext(AVFormatContext **dest, AVFormatContext **source){
    int numStreams = (*source)->nb_streams;
    LOGI("copyAVFormatContext source has %d streams", numStreams);
    int i;
    for (i = 0; i < numStreams; i++) {
        // Get input stream
        AVStream *inputStream = (*source)->streams[i];
        AVCodecContext *inputCodecContext = inputStream->codec;

        // Add new stream to output with codec from input stream
        LOGI("Attempting to find encoder %s", avcodec_get_name(inputCodecContext->codec_id));
        AVCodec *outputCodec = avcodec_find_encoder(inputCodecContext->codec_id);
        if(outputCodec == NULL){
            LOGE("Unable to find encoder %s", avcodec_get_name(inputCodecContext->codec_id));
        }
        AVStream *outputStream = avformat_new_stream(*dest, outputCodec);
        AVCodecContext *outputCodecContext = outputStream->codec;

        // Copy input stream's codecContext for output stream's codecContext
        avcodec_copy_context(outputCodecContext, inputCodecContext);
        outputCodecContext->strict_std_compliance = FF_COMPLIANCE_UNOFFICIAL;

        LOGI("copyAVFormatContext Copied stream %d with codec %s sample_fmt %s", i, avcodec_get_name(inputCodecContext->codec_id), av_get_sample_fmt_name(inputCodecContext->sample_fmt));

    }
}

// FFInputFile functions
// Using these to deduce codec parameters from test file

AVFormatContext* avFormatContextForInputPath(const char *inputPath, const char *inputFormatString){
    // You can override the detected input format
    AVFormatContext *inputFormatContext = NULL;
    AVInputFormat *inputFormat = NULL;
    //AVDictionary *inputOptions = NULL;

    if (inputFormatString) {
        inputFormat = av_find_input_format(inputFormatString);
        LOGI("avFormatContextForInputPath got inputFormat from string");
    }
    LOGI("avFormatContextForInputPath post av_Find_input_format");
    // It's possible to send more options to the parser
    // av_dict_set(&inputOptions, "video_size", "640x480", 0);
    // av_dict_set(&inputOptions, "pixel_format", "rgb24", 0);
    // av_dict_free(&inputOptions); // Don't forget to free

    LOGI("avFormatContextForInputPath pre avformat_open_input path: %s format: %s", inputPath, inputFormatString);
    int openInputResult = avformat_open_input(&inputFormatContext, inputPath, inputFormat, /*&inputOptions*/ NULL);
    LOGI("avFormatContextForInputPath avformat_open_input result: %d", openInputResult);
    if (openInputResult != 0) {
        LOGE("avformat_open_input failed: %s", stringForAVErrorNumber(openInputResult));
        avformat_close_input(&inputFormatContext);
        return NULL;
    }

    int streamInfoResult = avformat_find_stream_info(inputFormatContext, NULL);
    LOGI("avFormatContextForInputPath avformat_find_stream_info result: %d", streamInfoResult);
    if (streamInfoResult < 0) {
        avformat_close_input(&inputFormatContext);
        LOGE("avformat_find_stream_info failed: %s", stringForAVErrorNumber(openInputResult));
        return NULL;
    }

    LOGI("avFormatContextForInputPath Complete!");
    LOGI("AVInputFormat %s Stream0 codec: %s Stream1 codec: %s", inputFormatContext->iformat->name, avcodec_get_name(inputFormatContext->streams[0]->codec->codec_id), avcodec_get_name(inputFormatContext->streams[1]->codec->codec_id) );
    LOGI("Stream0 time_base: (num: %d, den: %d)", inputFormatContext->streams[0]->codec->time_base.num, inputFormatContext->streams[0]->codec->time_base.den);
    LOGI("Stream1 time_base: (num: %d, den: %d)", inputFormatContext->streams[1]->codec->time_base.num, inputFormatContext->streams[1]->codec->time_base.den);
    return inputFormatContext;
}

// FFOutputFile functions

AVFormatContext* avFormatContextForOutputPath(const char *path, const char *formatName){
    AVFormatContext *outputFormatContext;
    LOGI("avFormatContextForOutputPath format: %s path: %s", formatName, path);
    int openOutputValue = avformat_alloc_output_context2(&outputFormatContext, NULL, formatName, path);
    if (openOutputValue < 0) {
        avformat_free_context(outputFormatContext);
    }
    return outputFormatContext;
}

int openFileForWriting(AVFormatContext *avfc, const char *path){
    if (!(avfc->oformat->flags & AVFMT_NOFILE)) {
        LOGI("Opening output file for writing at path %s", path);
        return avio_open(&avfc->pb, path, AVIO_FLAG_WRITE);
    }
    return -42;
}

int writeFileHeader(AVFormatContext *avfc){
    AVDictionary *options = NULL;

    // Write header for output file
    int writeHeaderResult = avformat_write_header(avfc, &options);
    if (writeHeaderResult < 0) {
        LOGE("Error writing header: %s", stringForAVErrorNumber(writeHeaderResult));
        av_dict_free(&options);
    }
    LOGI("Wrote file header");
    av_dict_free(&options);
    return writeHeaderResult;
}

int writeFileTrailer(AVFormatContext *avfc){
    return av_write_trailer(avfc);
}

  /////////////////////
  //  JNI FUNCTIONS  //
  /////////////////////

void Java_com_example_ffmpegtest_FFmpegWrapper_test(JNIEnv *env, jobject obj){
  /* Standalone test copying input file to output, frame by frame */

 // Ensure gdb is connected before proceeding
 int i = 0;
 while(i>0){
	 i++;
 }
  // Init

  init();

  AVFormatContext *inputFormatContext;
  outputPath = "/sdcard/ffmpeg_output.mp4";

  outputFormatContext = avFormatContextForOutputPath(outputPath, outputFormatName);
  LOGI("post avFormatContextForOutputPath");
  inputFormatContext = avFormatContextForInputPath(sampleFilePath, NULL);
  LOGI("post avFormatContextForInputPath");
  copyAVFormatContext(&outputFormatContext, &inputFormatContext);
  LOGI("post copyAVFormatContext");

  int result = openFileForWriting(outputFormatContext, outputPath);
  if(result < 0){
      LOGE("openFileForWriting error: %d", result);
  }

  writeFileHeader(outputFormatContext);

  // Copy input to output frame by frame

  AVPacket *inputPacket;
  inputPacket = av_malloc(sizeof(AVPacket));

  int continueRecording = 1;
  int avReadResult = 0;
  int writeFrameResult = 0;
  int frameCount = 0;
  while(continueRecording == 1){
	  LOGI("pre av_read_frame");
      avReadResult = av_read_frame(inputFormatContext, inputPacket);
      LOGI("post av_read_frame");
      frameCount++;
      if(avReadResult != 0){
        if (avReadResult != AVERROR_EOF) {
        	LOGE("av_read_frame error");
            LOGE("av_read_frame error: %s", stringForAVErrorNumber(avReadResult));
        }else{
            LOGI("End of input file");
        }

        continueRecording = 0;
      }

      AVStream *outStream = outputFormatContext->streams[inputPacket->stream_index];
      LOGI("About to write packet");
      //LOGI("About to write packet. pts: (val: %ld, num: %ls den: %ld), time_base: %d/%d", (long) outStream->pts.val, (long) outStream->pts.num, (long) outStream->pts.den, outStream->time_base.num, outStream->time_base.den);
      writeFrameResult = av_interleaved_write_frame(outputFormatContext, inputPacket);
      LOGI("av_interleaved_write_frame");
      if(writeFrameResult < 0){
          LOGE("av_interleaved_write_frame error: %s", stringForAVErrorNumber(avReadResult));
      }
  }
  LOGI("Finished reading input file. #frames : %d", frameCount);

  // Finalize
  int writeTrailerResult = writeFileTrailer(outputFormatContext);
  if(writeTrailerResult < 0){
      LOGE("av_write_trailer error: %s", stringForAVErrorNumber(writeTrailerResult));
  }
  LOGI("Wrote trailer");
}

void Java_com_example_ffmpegtest_FFmpegWrapper_prepareAVFormatContext(JNIEnv *env, jobject obj, jstring jOutputPath){
    init();

    AVFormatContext *inputFormatContext;
    outputPath = (*env)->GetStringUTFChars(env, jOutputPath, NULL);

    outputFormatContext = avFormatContextForOutputPath(outputPath, outputFormatName);
    LOGI("post avFormatContextForOutputPath");

    inputFormatContext = avFormatContextForInputPath(sampleFilePath, outputFormatName);
    LOGI("post avFormatContextForInputPath");
    copyAVFormatContext(&outputFormatContext, &inputFormatContext);
    LOGI("post copyAVFormatContext");


    int result = openFileForWriting(outputFormatContext, outputPath);
    if(result < 0){
        LOGE("openFileForWriting error: %d", result);
    }

    writeFileHeader(outputFormatContext);
}

void Java_com_example_ffmpegtest_FFmpegWrapper_writeAVPacketFromEncodedData(JNIEnv *env, jobject obj, jobject jData, jint jIsVideo, jint jOffset, jint jSize, jint jFlags, jlong jFrameCount){
    if(packet == NULL){
        packet = av_malloc(sizeof(AVPacket));
        LOGI("av_malloc packet");
    }

    // jData is a ByteBuffer managed by Android's MediaCodec: a wrapper around the OMX interface
    uint8_t *data = (*env)->GetDirectBufferAddress(env, jData);
    LOGI("writeAVPacketFromEncodedData video: %d length %d", (int) jIsVideo, (int) jSize);

    av_init_packet(packet);

    packet->size = (int) jSize;
    packet->data = data;
    packet->pts = (int) jFrameCount;
    //packet->dts = (int) jFrameCount;

    // av_packet_from_data doesn't seem appropriate, as I don't want FFmpeg to manage data

    if( ((int) jIsVideo) == JNI_TRUE){
        LOGI("pre write_frame video");
        packet->stream_index = 1; // TODO
        // TODO: Apply bitstream filter
    }else{
        LOGI("pre write_frame audio");
        packet->stream_index = 0; // TODO
    }
    LOGI("pre av_interleaved_write_frame");
    int writeFrameResult = av_interleaved_write_frame(outputFormatContext, packet);
    // Crash here:
    // A/libc﹕ Fatal signal 8 (SIGFPE)
    if(writeFrameResult < 0){
        LOGE("av_interleaved_write_frame error: %s", stringForAVErrorNumber(writeFrameResult));
    }
    LOGI("post write_frame");
}

void Java_com_example_ffmpegtest_FFmpegWrapper_finalizeAVFormatContext(JNIEnv *env, jobject obj){
    LOGI("finalizeAVFormatContext");
    // Write file trailer (av_write_trailer)
    int writeTrailerResult = writeFileTrailer(outputFormatContext);
    if(writeTrailerResult < 0){
        LOGE("av_write_trailer error: %d", writeTrailerResult);
    }
}
