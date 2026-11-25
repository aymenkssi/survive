package com.survive.app.tools

import java.text.Normalizer

/** Code Morse international (UIT-R M.1677). Pur Kotlin : testable sur JVM. */
object Morse {
    val table: Map<Char, String> = linkedMapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", ':' to "---...", '=' to "-...-",
        '+' to ".-.-.", '-' to "-....-", '@' to ".--.-.",
    )

    /** Le signal de détresse SOS s'émet d'un seul bloc, sans espace entre les lettres. */
    const val SOS = "...---..."

    /** Supprime les accents et met en majuscules (é -> E). */
    fun clean(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "").uppercase()

    /** Texte -> code lisible, lettres séparées par un espace, mots par " / ". */
    fun encode(text: String): String =
        clean(text).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" / ") { word ->
            if (word == "SOS") SOS else word.mapNotNull { table[it] }.joinToString(" ")
        }

    private val reverse: Map<String, Char> by lazy { table.entries.associate { (k, v) -> v to k } }

    /**
     * Code -> texte. Accepte « . » ou « • » pour le point, « - », « — » ou « _ » pour le trait,
     * un espace entre les lettres et « / » (ou plusieurs espaces) entre les mots.
     */
    fun decode(code: String): String =
        code.replace('•', '.').replace('·', '.').replace('—', '-').replace('_', '-').replace('–', '-')
            .trim().split(Regex("\\s*/\\s*|\\s{2,}")).filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.trim().split(Regex("\\s+")).joinToString("") { letter ->
                    if (letter == SOS) "SOS" else (reverse[letter]?.toString() ?: "?")
                }
            }

    /**
     * Code -> suite d'impulsions (allumé ?, durée en unités).
     * Point = 1 unité, trait = 3, espace intra-lettre = 1, entre lettres = 3, entre mots = 7.
     */
    fun toPulses(code: String): List<Pair<Boolean, Int>> {
        val out = mutableListOf<Pair<Boolean, Int>>()
        fun gap(units: Int) {
            if (out.isEmpty()) return
            val last = out.last()
            if (!last.first) out[out.lastIndex] = false to maxOf(last.second, units) else out += false to units
        }
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith(" / ", i) -> { gap(7); i += 3; continue }
                code[i] == ' ' -> gap(3)
                code[i] == '.' -> { gap(1); out += true to 1 }
                code[i] == '-' -> { gap(1); out += true to 3 }
            }
            i++
        }
        return out
    }
}
