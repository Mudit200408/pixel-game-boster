package com.pixel.gamebooster.recording

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Dedicated internal playback (and dual mic) audio recorder.
 * Captures raw 16-bit PCM stereo audio via Android's AudioPlaybackCapture API (Android 10+),
 * mixes with microphone if enabled, encodes to AAC via MediaCodec, and writes to an audio MP4 container.
 */
class InternalAudioRecorder(
    private val mediaProjection: MediaProjection,
    private val outputFile: File,
    private val includeMic: Boolean = false,
) {
    companion object {
        private const val TAG = "InternalAudioRecorder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 2
        private const val BIT_RATE = 192000
        private const val TIMEOUT_US = 10000L
    }

    @Volatile
    private var isRecording = false
    private var workerThread: Thread? = null

    fun start(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.e(TAG, "AudioPlaybackCapture requires Android 10 (API 29)+")
            return false
        }

        isRecording = true
        workerThread = Thread({ recordLoop() }, "InternalAudioRecordThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        return true
    }

    fun stop() {
        isRecording = false
        try {
            workerThread?.join(2000)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for audio thread", e)
        }
        workerThread = null
    }

    private fun recordLoop() {
        var internalAudioRecord: AudioRecord? = null
        var micAudioRecord: AudioRecord? = null
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var audioTrackIndex = -1
        var isMuxerStarted = false

        try {
            // 1. Configure AudioPlaybackCaptureConfiguration
            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()

            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(8192)

            internalAudioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(playbackConfig)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(minBufferSize * 2)
                .build()

            if (internalAudioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize internal AudioRecord")
                return
            }

            if (includeMic) {
                try {
                    micAudioRecord = AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(minBufferSize * 2)
                        .build()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to initialize microphone AudioRecord, proceeding with internal audio only", e)
                    micAudioRecord = null
                }
            }

            // 2. Configure AAC MediaCodec Encoder
            val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2)
            }

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            internalAudioRecord.startRecording()
            micAudioRecord?.startRecording()

            val samplesPerRead = minBufferSize / 2
            val internalShorts = ShortArray(samplesPerRead)
            val micShorts = if (includeMic) ShortArray(samplesPerRead) else ShortArray(0)
            val mixedShorts = ShortArray(samplesPerRead)
            val byteBuffer = ByteBuffer.allocateDirect(samplesPerRead * 2).order(ByteOrder.LITTLE_ENDIAN)

            val bufferInfo = MediaCodec.BufferInfo()
            var presentationTimeUs = 0L

            while (isRecording) {
                val readInternal = internalAudioRecord.read(internalShorts, 0, samplesPerRead)
                if (readInternal <= 0) continue

                val targetShorts: ShortArray
                if (includeMic && micAudioRecord != null) {
                    val readMic = micAudioRecord.read(micShorts, 0, samplesPerRead)
                    if (readMic > 0) {
                        AudioMixer.mixStereo16(internalShorts, micShorts, mixedShorts, minOf(readInternal, readMic))
                        targetShorts = mixedShorts
                    } else {
                        targetShorts = internalShorts
                    }
                } else {
                    targetShorts = internalShorts
                }

                byteBuffer.clear()
                byteBuffer.asShortBuffer().put(targetShorts, 0, readInternal)
                val bytesToFeed = readInternal * 2

                // Feed PCM buffer to MediaCodec
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        byteBuffer.position(0)
                        byteBuffer.limit(bytesToFeed)
                        inputBuffer.put(byteBuffer)
                        codec.queueInputBuffer(inputIndex, 0, bytesToFeed, presentationTimeUs, 0)
                        presentationTimeUs += (readInternal * 1_000_000L) / (SAMPLE_RATE * CHANNEL_COUNT)
                    }
                }

                // Drain output from MediaCodec to Muxer
                while (true) {
                    val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                    if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!isMuxerStarted) {
                            audioTrackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            isMuxerStarted = true
                        }
                    } else if (outputIndex >= 0) {
                        val outBuffer = codec.getOutputBuffer(outputIndex)
                        if (isMuxerStarted && outBuffer != null && bufferInfo.size > 0) {
                            muxer.writeSampleData(audioTrackIndex, outBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    } else {
                        break
                    }
                }
            }

            // End-of-Stream signal
            val eosIndex = codec.dequeueInputBuffer(TIMEOUT_US)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(eosIndex, 0, 0, presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Final drain
            var eosReceived = false
            while (!eosReceived) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIndex >= 0) {
                    val outBuffer = codec.getOutputBuffer(outIndex)
                    if (isMuxerStarted && outBuffer != null && bufferInfo.size > 0) {
                        muxer.writeSampleData(audioTrackIndex, outBuffer, bufferInfo)
                    }
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReceived = true
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio recording failed", e)
        } finally {
            try {
                internalAudioRecord?.stop()
                internalAudioRecord?.release()
                micAudioRecord?.stop()
                micAudioRecord?.release()
                codec?.stop()
                codec?.release()
                if (isMuxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Clean-up error in audio recorder", e)
            }
        }
    }
}
