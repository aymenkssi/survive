package com.survive.app.ui

import com.survive.app.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocalizationTest {
    @Test fun automaticFollowsThePhone() {
        assertEquals("fr", resolveLanguage(AppLanguage.SYSTEM, Locale.FRANCE))
        assertEquals("fr", resolveLanguage(AppLanguage.SYSTEM, Locale.CANADA_FRENCH))
        assertEquals("en", resolveLanguage(AppLanguage.SYSTEM, Locale.US))
    }

    @Test fun unsupportedLanguagesFallBackToEnglish() {
        assertEquals("en", resolveLanguage(AppLanguage.SYSTEM, Locale.forLanguageTag("ar")))
        assertEquals("en", resolveLanguage(AppLanguage.SYSTEM, Locale.GERMANY))
    }

    @Test fun explicitChoiceWins() {
        assertEquals("en", resolveLanguage(AppLanguage.EN, Locale.FRANCE))
        assertEquals("fr", resolveLanguage(AppLanguage.FR, Locale.US))
    }
}
