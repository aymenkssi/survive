package com.survive.app.tools

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/*
 * Décodage du Morse à l'oreille, en trois étages indépendants (pur Kotlin, testables sur JVM) :
 *   1. ToneDetector  : niveau d'un bip dans une trame audio (banc de filtres de Goertzel 300–3000 Hz).
 *   2. SignalTracker : seuil adaptatif au bruit ambiant -> durées « bip » / « silence ».
 *   3. MorseDecoder  : durées -> points, traits, lettres et mots, avec estimation automatique de la vitesse.
 */

/** Mesure l'intensité du bip le plus fort, quelle que soit sa hauteur (sifflet, buzzer, radio…). */
class ToneDetector(sampleRate: Int, private val frameSize: Int) {
    private val coeffs: FloatArray = (300..3000 step 100)
        .map { f -> (2 * cos(2 * PI * f / sampleRate)).toFloat() }
        .toFloatArray()

    /** Renvoie l'amplitude du filtre le plus fort pour la trame (frameSize échantillons 16 bits). */
    fun level(frame: ShortArray, length: Int = frame.size): Float {
        var best = 0f
        for (c in coeffs) {
            var s1 = 0f
            var s2 = 0f
            for (i in 0 until length) {
                val s0 = frame[i] + c * s1 - s2
                s2 = s1
                s1 = s0
            }
            val power = s1 * s1 + s2 * s2 - c * s1 * s2
            if (power > best) best = power
        }
        return sqrt(best) / frameSize
    }
}

/**
 * Transforme une suite de niveaux (une valeur par trame) en impulsions.
 * Le seuil suit le bruit de fond et le niveau du signal ; une hystérésis et un anti-rebond
 * de 2 trames évitent les faux déclenchements.
 */
class SignalTracker(private val frameMs: Int, private val listener: Listener) {
    interface Listener {
        fun onTone(durationMs: Long)
        fun onGap(durationMs: Long)
    }

    private var floor = -1f
    private var peak = 0f
    var isOn = false
        private set
    private var candidateFrames = 0
    private var framesInState = 0
    private var started = false

    /** Durée du silence en cours (pour terminer une lettre quand l'émetteur s'arrête). */
    val currentSilenceMs: Long get() = if (!isOn && started) framesInState.toLong() * frameMs else 0

    /** Rapport signal / bruit actuel, utile pour un vu-mètre. */
    var snr = 0f
        private set

    fun feed(level: Float) {
        if (floor < 0) { floor = level; peak = level }
        peak = maxOf(level, peak * 0.998f, floor)
        if (!isOn) floor = if (level < floor) floor * 0.8f + level * 0.2f else floor * 0.995f + level * 0.005f
        snr = if (floor > 0f) level / floor else 0f

        val strong = peak > floor * 4f
        val onThreshold = floor + (peak - floor) * 0.5f
        val offThreshold = floor + (peak - floor) * 0.3f
        val wantOn = strong && if (isOn) level > offThreshold else level > onThreshold

        framesInState++
        if (wantOn != isOn) {
            candidateFrames++
            if (candidateFrames >= 2) {
                // La transition a eu lieu au début de la période candidate.
                val duration = (framesInState - candidateFrames).toLong() * frameMs
                if (isOn) listener.onTone(duration) else if (started) listener.onGap(duration)
                started = true
                isOn = wantOn
                framesInState = candidateFrames
                candidateFrames = 0
            }
        } else {
            candidateFrames = 0
        }
    }
}

/** Reconstitue le texte à partir des durées de bips et de silences. */
class MorseDecoder(initialUnitMs: Float = 120f) : SignalTracker.Listener {
    private val reverse: Map<String, Char> = Morse.table.entries.associate { (k, v) -> v to k }
    private val history = ArrayDeque<Long>()
    private val letterTones = mutableListOf<Long>()
    private val out = StringBuilder()

    /** Durée estimée d'un point, en millisecondes. */
    var unitMs = initialUnitMs
        private set

    /** Vitesse estimée en mots par minute (norme PARIS : 1200 / durée du point). */
    val wpm: Int get() = (1200f / unitMs).toInt()

    val text: String get() = out.toString()

    /** Points et traits de la lettre en cours de réception. */
    val pending: String get() = letterTones.joinToString("") { if (it > 2 * unitMs) "-" else "." }

    override fun onTone(durationMs: Long) {
        letterTones += durationMs
        remember(durationMs)
    }

    override fun onGap(durationMs: Long) {
        if (durationMs < 3000) remember(durationMs)
        if (durationMs >= 2 * unitMs) flushLetter()
        if (durationMs >= 5 * unitMs) space()
    }

    /** À appeler régulièrement pendant un silence : affiche la dernière lettre sans attendre le bip suivant. */
    fun tick(silenceMs: Long) {
        if (silenceMs >= 3 * unitMs) flushLetter()
        if (silenceMs >= 7 * unitMs) space()
    }

    fun clear() {
        out.clear(); letterTones.clear(); history.clear()
    }

    /**
     * Le point dure 1 unité, comme les silences entre points et traits d'une même lettre :
     * les plus petites durées observées donnent donc l'unité, quelle que soit la vitesse.
     */
    private fun remember(ms: Long) {
        history.addLast(ms)
        if (history.size > 30) history.removeFirst()
        val sorted = history.sorted()
        val ref = sorted[minOf(1, sorted.lastIndex)].toFloat()
        val small = sorted.filter { it <= 2 * ref }
        if (sorted.size < 2) return // pas assez d'information
        unitMs = small.average().toFloat().coerceIn(20f, 1000f)
    }

    private fun flushLetter() {
        if (letterTones.isEmpty()) return
        val code = pending
        letterTones.clear()
        when {
            code == Morse.SOS -> out.append("SOS")
            else -> out.append(reverse[code] ?: '?')
        }
    }

    private fun space() {
        if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
    }
}
