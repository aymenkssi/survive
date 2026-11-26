package com.survive.app.tools

import java.util.Locale
import kotlin.math.abs

/** Mise en forme de coordonnées GPS, pour les lire à voix haute aux secours ou les noter sur papier. */
object Coordinates {
    /** Degrés décimaux, 5 décimales (précision ≈ 1 m) : "36.80650, 10.18150". */
    fun decimal(lat: Double, lon: Double): String = String.format(Locale.ROOT, "%.5f, %.5f", lat, lon)

    /** Degrés, minutes, secondes : 36°48'23.4"N 10°10'53.4"E. Les lettres N/S/E/W sont la norme internationale. */
    fun dms(lat: Double, lon: Double): String = "${dmsPart(lat, 'N', 'S')} ${dmsPart(lon, 'E', 'W')}"

    private fun dmsPart(value: Double, pos: Char, neg: Char): String {
        val a = abs(value)
        var deg = a.toInt()
        var min = ((a - deg) * 60).toInt()
        var sec = (a - deg - min / 60.0) * 3600
        // Arrondi au dixième sans afficher 60.0"
        if (String.format(Locale.ROOT, "%.1f", sec) == "60.0") { sec = 0.0; min++ }
        if (min == 60) { min = 0; deg++ }
        return String.format(Locale.ROOT, "%d°%02d'%04.1f\"%c", deg, min, sec, if (value >= 0) pos else neg)
    }
}
