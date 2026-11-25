package com.survive.app.tools

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Fabrique un vrai signal audio Morse (bip + bruit) et vérifie que la chaîne d'écoute le retraduit. */
class MorseListenerTest {
    private val rate = 16_000
    private val frame = 160 // 10 ms

    private fun synth(
        text: String, unitMs: Int, freqs: List<Double> = listOf(800.0),
        noise: Double = 800.0, jitter: Double = 0.0, seed: Int = 1,
    ): ShortArray {
        val rnd = Random(seed)
        val samples = ArrayList<Short>()
        fun add(ms: Int, on: Boolean) {
            val n = ms * rate / 1000
            repeat(n) {
                val t = samples.size.toDouble() / rate
                var v = rnd.nextDouble(-noise, noise)
                if (on) freqs.forEach { f -> v += 7000.0 / freqs.size * sin(2 * PI * f * t) }
                samples += v.coerceIn(-32767.0, 32767.0).toInt().toShort()
            }
        }
        add(600, false)
        for ((on, units) in Morse.toPulses(Morse.encode(text))) {
            val j = if (jitter > 0) 1 + rnd.nextDouble(-jitter, jitter) else 1.0
            add((units * unitMs * j).roundToInt(), on)
        }
        add(unitMs * 10 + 300, false)
        return samples.toShortArray()
    }

    private fun listen(audio: ShortArray): MorseDecoder {
        val decoder = MorseDecoder()
        val tracker = SignalTracker(10, decoder)
        val detector = ToneDetector(rate, frame)
        var i = 0
        while (i + frame <= audio.size) {
            tracker.feed(detector.level(audio.copyOfRange(i, i + frame)))
            decoder.tick(tracker.currentSilenceMs)
            i += frame
        }
        return decoder
    }

    @Test fun decodesNormalSpeed() =
        assertEquals("SOS AIDE 112", listen(synth("SOS aide 112", 100)).text.trim())

    @Test fun decodesSlowSpeedFromFirstLetter() =
        assertEquals("SOS EAU", listen(synth("SOS eau", 250)).text.trim())

    @Test fun decodesFastSpeed() =
        assertEquals("PARIS PARIS", listen(synth("paris paris", 60)).text.trim())

    @Test fun decodesDualToneBeepWithHumanJitter() =
        assertEquals("BLESSE ICI", listen(synth("blessé ici", 120, listOf(697.0, 1209.0), jitter = 0.2, seed = 7)).text.trim())

    @Test fun decodesInNoisyEnvironment() {
        for (seed in 1..5) {
            val decoded = listen(synth("sos nous sommes 4", 150, noise = 3000.0, jitter = 0.15, seed = seed)).text.trim()
            assertEquals("graine $seed", "SOS NOUS SOMMES 4", decoded)
        }
    }

    @Test fun estimatesSpeed() {
        val d = listen(synth("paris paris paris", 100))
        assertEquals(12, d.wpm)
    }

    @Test fun decodesWrittenCode() {
        assertEquals("SOS AIDE", Morse.decode("...---... / .- .. -.. ."))
        assertEquals("EAU", Morse.decode("•  •—  ••—".replace("  ", " ")))
        assertEquals("E T", Morse.decode(".   -"))
    }
}
