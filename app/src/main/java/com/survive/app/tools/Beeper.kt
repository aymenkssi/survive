package com.survive.app.tools

import android.media.AudioManager
import android.media.ToneGenerator

/** Bips sonores sur le flux « alarme » (audible même si le volume média est coupé). */
class Beeper {
    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull()

    fun beep(durationMs: Int, highPitch: Boolean = false) {
        tone?.startTone(if (highPitch) ToneGenerator.TONE_DTMF_D else ToneGenerator.TONE_DTMF_1, durationMs)
    }

    fun stop() { tone?.stopTone() }

    fun release() { tone?.release() }
}
