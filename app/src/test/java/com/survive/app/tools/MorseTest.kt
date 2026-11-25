package com.survive.app.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseTest {
    @Test fun encodesSosAsOneBlock() {
        assertEquals("...---...", Morse.encode("sos"))
    }

    @Test fun encodesWordsAndAccents() {
        assertEquals(".- .. -.. . / .--- --- .", Morse.encode("Aidé  JOE"))
    }

    @Test fun pulsesFollowStandardTiming() {
        // "E T" -> point, pause de mot (7), trait
        assertEquals(listOf(true to 1, false to 7, true to 3), Morse.toPulses(Morse.encode("E T")))
        // "ET" -> point, pause de lettre (3), trait
        assertEquals(listOf(true to 1, false to 3, true to 3), Morse.toPulses(Morse.encode("ET")))
        // "A" -> point, pause intra-lettre (1), trait
        assertEquals(listOf(true to 1, false to 1, true to 3), Morse.toPulses(".-"))
    }
}
