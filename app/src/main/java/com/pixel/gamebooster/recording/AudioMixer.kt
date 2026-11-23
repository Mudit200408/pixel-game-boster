package com.pixel.gamebooster.recording

/**
 * High-performance 16-bit PCM audio mixer.
 * Combines internal game playback audio and microphone audio with gain weighting
 * and signed 16-bit saturation clamping to prevent digital clipping/distortion.
 */
object AudioMixer {

    /**
     * Mixes two 16-bit PCM stereo sample buffers into an output buffer.
     *
     * @param internalBuf Raw 16-bit PCM samples from internal playback capture.
     * @param micBuf Raw 16-bit PCM samples from microphone capture.
     * @param outBuf Output buffer receiving the mixed samples.
     * @param sampleCount Total number of 16-bit short samples to process.
     * @param internalGain Volume weight for internal game audio (default: 0.85f).
     * @param micGain Volume weight for microphone audio (default: 1.00f).
     */
    fun mixStereo16(
        internalBuf: ShortArray,
        micBuf: ShortArray,
        outBuf: ShortArray,
        sampleCount: Int,
        internalGain: Float = 0.85f,
        micGain: Float = 1.00f,
    ) {
        val limit = minOf(sampleCount, internalBuf.size, micBuf.size, outBuf.size)
        for (i in 0 until limit) {
            val s1 = internalBuf[i] * internalGain
            val s2 = micBuf[i] * micGain
            val sum = (s1 + s2).toInt()
            outBuf[i] = when {
                sum > Short.MAX_VALUE -> Short.MAX_VALUE
                sum < Short.MIN_VALUE -> Short.MIN_VALUE
                else -> sum.toShort()
            }
        }
    }
}
