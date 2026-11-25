package com.survive.app.tools

import kotlinx.coroutines.delay

/** Émet une suite d'impulsions Morse sur les sorties choisies (flash, son, écran). */
suspend fun playPulses(
    pulses: List<Pair<Boolean, Int>>,
    unitMs: Long,
    onChange: (Boolean) -> Unit,
    beeper: Beeper?,
) {
    try {
        for ((on, units) in pulses) {
            val ms = units * unitMs
            onChange(on)
            if (on) beeper?.beep(ms.toInt())
            delay(ms)
        }
    } finally {
        onChange(false)
        beeper?.stop()
    }
}
