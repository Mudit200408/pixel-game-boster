package com.pixel.gamebooster.recording

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Fast dual-track media muxer.
 * Merges a video-only MP4 file and an audio-only MP4/AAC file into a consolidated MP4
 * by copying sample buffers directly without re-encoding (typically completes in < 300ms).
 */
object ScreenRecordingMuxer {

    private const val TAG = "ScreenRecordingMuxer"
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB buffer

    fun mux(videoFile: File, audioFile: File, outputFile: File): Boolean {
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var isMuxerStarted = false

        return try {
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Locate video track
            var videoTrackIndex = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    break
                }
            }

            // Locate audio track
            var audioTrackIndex = -1
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex < 0) {
                Log.e(TAG, "No video track found in ${videoFile.name}")
                return false
            }

            videoExtractor.selectTrack(videoTrackIndex)
            val muxerVideoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrackIndex))

            var muxerAudioTrack = -1
            if (audioTrackIndex >= 0) {
                audioExtractor.selectTrack(audioTrackIndex)
                muxerAudioTrack = muxer.addTrack(audioExtractor.getTrackFormat(audioTrackIndex))
            }

            muxer.start()
            isMuxerStarted = true

            var buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()

            // 1. Copy video samples
            while (true) {
                val sampleSize = videoExtractor.sampleSize
                if (sampleSize > buffer.capacity()) {
                    buffer = ByteBuffer.allocate(sampleSize.toInt())
                }
                bufferInfo.offset = 0
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                bufferInfo.flags = mapExtractorFlags(videoExtractor.sampleFlags)
                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // 2. Copy audio samples (if available)
            if (muxerAudioTrack >= 0) {
                while (true) {
                    val sampleSize = audioExtractor.sampleSize
                    if (sampleSize > buffer.capacity()) {
                        buffer = ByteBuffer.allocate(sampleSize.toInt())
                    }
                    bufferInfo.offset = 0
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    bufferInfo.flags = mapExtractorFlags(audioExtractor.sampleFlags)
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            isMuxerStarted = false

            // Delete temporary files after muxer.stop() succeeds
            videoFile.delete()
            audioFile.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Muxing failed", e)
            false
        } finally {
            if (isMuxerStarted) {
                try {
                    muxer?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping muxer", e)
                }
            }
            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing muxer", e)
            }
            videoExtractor?.release()
            audioExtractor?.release()
        }
    }

    private fun mapExtractorFlags(sampleFlags: Int): Int {
        var codecFlags = 0
        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return codecFlags
    }
}
